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

public class MqttSourceTask extends SourceTask implements MqttCallback {

    private MqttClient mqttClient;
    private String topic;
    private MqttSourceConnectorConfig config;
    private final BlockingQueue<SourceRecord> messageQueue = new LinkedBlockingQueue<>();

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.config = new MqttSourceConnectorConfig(props);
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
        
            // Converter o payload diretamente em um JSONObject
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        JSONObject jsonMessage;
        
        try {
            jsonMessage = new JSONObject(payload);
        } catch (Exception e) {
            System.err.println("Erro ao processar JSON: " + e.getMessage());
            return;
        }
        
        // Converter o JSONObject para Map diretamente, sem transformar em string
        Map<String, Object> jsonMap = jsonMessage.toMap();
        jsonMap = convertBigDecimalToDouble(jsonMap);
        Map<String, String> sourcePartition = Collections.singletonMap("topic", topic);
        Map<String, String> sourceOffset = Collections.singletonMap("offset", String.valueOf(System.currentTimeMillis()));

        // Enviar o JSON diretamente como objeto para Kafka
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