package com.example;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class MqttSourceConnectorConfig extends AbstractConfig {

    public static final String MQTT_BROKER_URL = "mqtt.broker.url";
    public static final String MQTT_TOPIC = "mqtt.topic";
    public static final String KAFKA_TOPIC = "kafka.topic";

    public MqttSourceConnectorConfig(Map<String, ?> originals) {
        super(conf(), originals);
    }

    public static ConfigDef conf() {
        return new ConfigDef()
            .define(MQTT_BROKER_URL, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "URL do broker MQTT")
            .define(MQTT_TOPIC, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Tópico MQTT a ser consumido")
            .define(KAFKA_TOPIC, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Tópico no Kafka"); // Define o tópico Kafka
    }

    public String getMqttBrokerUrl() {
        return this.getString(MQTT_BROKER_URL);
    }

    public String getMqttTopic() {
        return this.getString(MQTT_TOPIC);
    }

    // Método para obter o tópico Kafka
    public String getKafkaTopic() {
        return this.getString(KAFKA_TOPIC);
    }
}