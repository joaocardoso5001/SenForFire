package com.example;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import java.math.BigDecimal;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;

public class TTNSourceTask extends SourceTask implements MqttCallback {

    private MqttClient mqttClient;
    private String topic;
    private TTNSourceConnectorConfig config;
    private final BlockingQueue<SourceRecord> messageQueue = new LinkedBlockingQueue<>();

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.config = new TTNSourceConnectorConfig(props);
        String brokerUrl = props.get("mqtt.broker.url");
        String username = props.get("mqtt.username");  
        String password = props.get("mqtt.password");  
        this.topic = props.get("mqtt.topic");

        try {
            mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId());
            mqttClient.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setSocketFactory(SSLSocketFactory.getDefault());
            mqttClient.connect(options);
            mqttClient.subscribe(topic);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();
        messageQueue.drainTo(records); // Consome todas as mensagens da fila
        return records;
    }

    @Override
    public void stop() {
        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Conexão com o broker MQTT perdida: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String kafkaTopic = this.config.getString("kafka.topic");
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        JSONObject jsonMessage;
        
        try {
            jsonMessage = new JSONObject(payload);
        } catch (Exception e) {
            System.err.println("Erro ao processar JSON: " + e.getMessage());
            return;
        }

        
        if (!jsonMessage.has("uplink_message")) {
            return;  // Ignorar a mensagem
        }


        JSONObject transformedMessage = new JSONObject();

        JSONObject uplinkMessage = jsonMessage.getJSONObject("uplink_message");
        JSONObject endDeviceIds = jsonMessage.getJSONObject("end_device_ids");

        transformedMessage.put("deviceId", endDeviceIds.getString("dev_eui"));
        transformedMessage.put("type", "ForestFireObservation");

        // Definir data e hora de observação
        if (uplinkMessage.has("settings") && uplinkMessage.getJSONObject("settings").has("time")) {
        transformedMessage.put("dateObserved", uplinkMessage.getJSONObject("settings").getString("time"));
        } else {
        transformedMessage.put("dateObserved", uplinkMessage.getString("received_at"));  // Fallback, se o "time" não estiver disponível
        }
        // Localização
        JSONObject location = new JSONObject();
        location.put("type", "Point");
        
        // Extrair coordenadas
        JSONArray coordinates = new JSONArray();
        boolean locationAvailable = false;

        if (uplinkMessage.has("rx_metadata")) {
            JSONArray rxMetadataArray = uplinkMessage.getJSONArray("rx_metadata");
            if (rxMetadataArray.length() > 0) {
                JSONObject metadata = rxMetadataArray.getJSONObject(0);  
                if (metadata.has("location")) {
                    JSONObject metadataLocation = metadata.getJSONObject("location");
                    double longitude = metadataLocation.optDouble("longitude", Double.NaN); 
                    double latitude = metadataLocation.optDouble("latitude", Double.NaN);   
                    if (!Double.isNaN(longitude) && !Double.isNaN(latitude)) {
                        coordinates.put(longitude);
                        coordinates.put(latitude);
                        location.put("coordinates", coordinates);
                        locationAvailable = true;  
                    }
                } 
            } 
        } 

        if (locationAvailable) {
            location.put("description", "Sensor location from metadata"); // Descrição
            transformedMessage.put("location", location);
        } 

        // Adicionar a fonte usando o application_id
        JSONObject applicationIds = endDeviceIds.getJSONObject("application_ids");
        String applicationId = applicationIds.optString("application_id", "Unknown");  // Fallback para "Unknown" caso não exista
        transformedMessage.put("source", applicationId);

        // Agora você mapeia as medições específicas do payload decodificado
        JSONObject decodedPayload = uplinkMessage.getJSONObject("decoded_payload");
        Map<String, String> fieldMapping = new HashMap<>();

        if (applicationId.equalsIgnoreCase("sensor-co2-senforfire1")) {
            // Mapeamento de campos para o dispositivo SENSOR-CO2-SENFORFIRE1
            fieldMapping.put("field1", "CO2");
            fieldMapping.put("field2", "temperature");
            fieldMapping.put("field3", "humidity");
            fieldMapping.put("field4", "CO");
            fieldMapping.put("field5", "NH3");
            fieldMapping.put("field6", "NO2");
        } else {
            // Mapeamento de campos para o outro dispositivo
            fieldMapping.put("field1", "temperature");
            fieldMapping.put("field2", "humidity");
            fieldMapping.put("field3", "MICS_2714_OX");
            fieldMapping.put("field4", "MICS_4514_RED");
            fieldMapping.put("field5", "MICS_4514_OX");
            fieldMapping.put("field6", "MICS_6814_RED");
            fieldMapping.put("field7", "MICS_6814_OX");
            fieldMapping.put("field8", "MICS_6814_NH3");
            fieldMapping.put("field9", "SP3_61_OZONE");
        }

        // Preencher o transformedMessage com base no mapeamento definido
        for (String fieldKey : fieldMapping.keySet()) {
            if (decodedPayload.has(fieldKey)) {
                transformedMessage.put(fieldMapping.get(fieldKey), decodedPayload.getDouble(fieldKey));
            }
        }
                
        // Converter o JSONObject para Map diretamente, sem transformar em string
        Map<String, Object> jsonMap = transformedMessage.toMap();
        jsonMap = convertBigDecimalToDouble(jsonMap);


        Map<String, String> sourcePartition = Collections.singletonMap("topic", topic);
        Map<String, String> sourceOffset = Collections.singletonMap("offset", String.valueOf(System.currentTimeMillis()));

        // Enviar o JSON diretamente como objeto para o Kafka
        SourceRecord record = new SourceRecord(
            sourcePartition,
            sourceOffset,
            kafkaTopic,
            null,
            jsonMap 
        );

        messageQueue.add(record);
}
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Não é necessário para o SourceTask
    }

    public Map<String, Object> convertBigDecimalToDouble(Map<String, Object> map) {
    map.replaceAll((key, value) -> {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (value instanceof Map) {
            return convertBigDecimalToDouble((Map<String, Object>) value);
        } else if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(item -> item instanceof Map ? convertBigDecimalToDouble((Map<String, Object>) item) : item)
                .collect(Collectors.toList());
        }
        return value;
    });
    return map;
}
}