package com.example;

import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.*;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.everit.json.schema.ObjectSchema;

public class CustomTimeScaleSinkTask extends SinkTask {

    private Connection dbConnection;
    private Schema jsonSchema;
    private PreparedStatement preparedStatement;
    private Set<String> optionalFields;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        // Leitura do JSON Schema
        try {

            String schemaPath = props.get("schema.path");
            String schemaContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(schemaPath)));
            JSONObject rawSchema = new JSONObject(new JSONTokener(schemaContent));
            jsonSchema = SchemaLoader.load(rawSchema);
            optionalFields = getOptionalFieldsFromSchema();
        } catch (Exception e) {
            throw new ConnectException("Erro ao carregar o schema JSON: " + e.getMessage(), e);
        }

        // Conexão com TimeScaleDB
        try {
            dbConnection = DriverManager.getConnection(props.get("db.url"), props.get("db.user"), props.get("db.password"));
            dbConnection.setAutoCommit(false); // Usar inserção em transações
            preparedStatement = dbConnection.prepareStatement(
                    "INSERT INTO sensor_data (device_id, metric, value, created, source) VALUES (?, ?, ?, ?, ?)");
        } catch (SQLException e) {
            throw new ConnectException("Erro ao conectar ao TimeScaleDB", e);
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        for (SinkRecord record : records) {
            try {
                if (record.value() instanceof Map) {
                    JSONObject message = new JSONObject((Map) record.value());

                    // Verifica se a mensagem contém uma lista de mensagens
                    if (message.has("messages") && message.get("messages") instanceof JSONArray) {
                        JSONArray messages = message.getJSONArray("messages");

                        // Processa cada mensagem individualmente
                        for (int i = 0; i < messages.length(); i++) {
                            JSONObject individualMessage = messages.getJSONObject(i);

                        try {
                            // Valida e insere cada mensagem individualmente
                            validateMessageWithSchema(individualMessage);
                            insertMessageIntoDB(individualMessage);
                        } catch (Exception e) {
                            logError(record, "Erro ao processar a mensagem individual: " + e.getMessage());
                            continue; // Continua com a próxima mensagem no array
                        }
                        }
                    } else {
                        // Mensagem única (não é uma lista)
                        validateMessageWithSchema(message);
                        insertMessageIntoDB(message);
                    }
                } else {
                    logError(record, "Mensagem não é um JSON válido");
                }
            } catch (Exception e) {
                logError(record, "Erro ao processar a mensagem: " + e.getMessage());
                continue; // Continua com a próxima mensagem
            }
        }

    // Commit da transação após processar todas as mensagens
    try {
        preparedStatement.executeBatch();
        dbConnection.commit();
    } catch (SQLException e) {
        logError(null, "Erro ao fazer commit no banco de dados: " + e.getMessage());
    }
}

    private void validateMessageWithSchema(JSONObject message) throws Exception {
        try {
            jsonSchema.validate(message);
        } catch (Exception e) {
            throw new Exception("Mensagem inválida conforme o schema JSON: " + e.getMessage());
        }
    }

    private void insertMessageIntoDB(JSONObject message) throws SQLException {
        String deviceId = message.getString("deviceId");
        //UUID deviceUUID = UUID.fromString(deviceId);  // Converte para UUID

        String source = message.getString("source");

        String metric = ""; 
        double value = 0.0;

        Timestamp createdTimestamp = Timestamp.from(Instant.now());
        String dateObserved = message.optString("dateObserved", null);
        if (dateObserved != null && !dateObserved.isEmpty()) {
            createdTimestamp = Timestamp.from(ZonedDateTime.parse(dateObserved).toInstant());
        }

        for (String key : message.keySet()) {
            if (optionalFields.contains(key)) {
                Object valueObj = message.get(key);
                    if (valueObj instanceof Number) {
                    metric = key;
                    value = message.getDouble(key);
                    preparedStatement.setString(1, deviceId);
                    preparedStatement.setString(2, metric);
                    preparedStatement.setDouble(3, value);
                    preparedStatement.setTimestamp(4, createdTimestamp);
                    preparedStatement.setString(5, source);
                    preparedStatement.addBatch(); // Adiciona ao batch para inserir depois
                }
            }
        }
    }

    @Override
    public void stop() {
        try {
            if (preparedStatement != null) preparedStatement.close();
            if (dbConnection != null) dbConnection.close();
        } catch (SQLException e) {
            // Logar se houver erro ao fechar a conexão
        }
    }

    private void logError(SinkRecord record, String errorMessage) {
        // Logar o erro para uma fila de dead-letter ou apenas no log
        System.err.println("Erro: " + errorMessage);
        if (record != null) {
            System.err.println("Registro Kafka: " + record.toString());
        }
    }

    // Nova função para obter os atributos opcionais do JSON schema
    private Set<String> getOptionalFieldsFromSchema() throws Exception {
        Set<String> optionalFields = new HashSet<>();
         Schema schemaObject = jsonSchema; // Usa o schema diretamente

        JSONObject rawSchema = new JSONObject(schemaObject.toString());
        
        // Obtém as propriedades definidas no schema
        JSONObject properties = rawSchema.getJSONObject("properties");

        // Verifica quais são as propriedades obrigatórias
        Set<String> requiredFields = new HashSet<>();
        if (rawSchema.has("required")) {
            JSONArray requiredArray = rawSchema.getJSONArray("required");
            for (int i = 0; i < requiredArray.length(); i++) {
                requiredFields.add(requiredArray.getString(i));
            }
        }

        // Adiciona ao conjunto os campos que NÃO são obrigatórios
        for (String key : properties.keySet()) {
            if (!requiredFields.contains(key)) {
                optionalFields.add(key);
            }
        }

    return optionalFields;
    }
}
