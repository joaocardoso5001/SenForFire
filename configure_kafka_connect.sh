#!/bin/sh

echo "Configurando o Kafka Connect para usar o conector personalizado com TimeScaleDB..."

curl -X POST -H "Content-Type: application/json" --data '{
  "name": "custom-timescale-sink",
  "config": {
    "connector.class": "com.example.CustomTimeScaleSinkConnector", 
    "tasks.max": "1",
    "topics": "sensor-data", 
    "schema.path": "/etc/kafka-connect/jars/schema.json", 
    "db.url": "jdbc:postgresql://timescaledb:5432/senforfire", 
    "db.user": "postgres",
    "db.password": "password",
    "errors.tolerance": "all",  
    "errors.log.enable": "true",  
    "errors.log.include.messages": "true",  
    "errors.deadletterqueue.topic.name": "dead-letter-queue", 
    "errors.deadletterqueue.context.headers.enable": "true", 
    "errors.deadletterqueue.topic.replication.factor": 1
  }
}' http://localhost:8083/connectors

echo "Conector personalizado configurado com sucesso."

# Configurar o conector MQTT para Kafka
echo "Configurando o Kafka Connect para usar o conector MQTT com Kafka..."

curl -X POST -H "Content-Type: application/json" --data '{
  "name": "mqtt-kafka-connector",
  "config": {
    "connector.class": "com.example.MqttSourceConnector", 
    "tasks.max": "1",
    "mqtt.broker.url": "tcp://mqtt-broker:1883", 
    "mqtt.topic": "sensor-mqtt", 
    "mqtt.username": "senforfire", 
    "mqtt.password": "k3.765!H",
    "kafka.topic": "sensor-data", 
    "errors.tolerance": "all",  
    "errors.log.enable": "true",  
    "errors.log.include.messages": "true",  
    "value.converter.schemas.enable": "false",
    "errors.deadletterqueue.topic.name": "dead-letter-queue", 
    "errors.deadletterqueue.context.headers.enable": "true", 
    "errors.deadletterqueue.topic.replication.factor": 1
  }
}' http://localhost:8083/connectors

echo "Conector MQTT configurado com sucesso."

curl -X POST -H "Content-Type: application/json" --data '{
  "name": "ttn-ray-kafka-connector",
  "config": {
    "connector.class": "com.example.TTNSourceConnector", 
    "tasks.max": "1",
    "mqtt.broker.url": "ssl://eu1.cloud.thethings.network:8883", 
    "mqtt.topic": "#", 
    "mqtt.username": "sensors-ray-senforfire@ttn", 
    "mqtt.password": "NNSXS.DBHHP4DHB7G7FCPBRCPVHRUYF2HQLF3IZHZ6GOI.X7FI3RHB5CKQJ5DQO6V34WR2GESSWXBR4DTX3F5WDRF2MYCAY6XQ",
    "kafka.topic": "sensor-data", 
    "errors.tolerance": "all",  
    "errors.log.enable": "true",  
    "errors.log.include.messages": "true",  
    "value.converter.schemas.enable": "false",
    "errors.deadletterqueue.topic.name": "dead-letter-queue", 
    "errors.deadletterqueue.context.headers.enable": "true", 
    "errors.deadletterqueue.topic.replication.factor": 1
  }
}' http://localhost:8083/connectors

echo "Conector MQTT configurado com sucesso."

echo "Conector MQTT configurado com sucesso."

curl -X POST -H "Content-Type: application/json" --data '{
  "name": "ttn-co2-kafka-connector",
  "config": {
    "connector.class": "com.example.TTNSourceConnector", 
    "tasks.max": "1",
    "mqtt.broker.url": "ssl://eu1.cloud.thethings.network:8883", 
    "mqtt.topic": "#", 
    "mqtt.username": "sensor-co2-senforfire1@ttn", 
    "mqtt.password": "NNSXS.GIAQ6UJ7CDNKQXXHBDYFZMIM6QGJH47A2FCX4VI.NM3DU3QPS4BPZZVOZ5XAYZWPLUT24G7RXTKDV5DVURRH7DRP6GOQ",
    "kafka.topic": "sensor-data", 
    "errors.tolerance": "all",  
    "errors.log.enable": "true",  
    "errors.log.include.messages": "true",  
    "value.converter.schemas.enable": "false",
    "errors.deadletterqueue.topic.name": "dead-letter-queue", 
    "errors.deadletterqueue.context.headers.enable": "true", 
    "errors.deadletterqueue.topic.replication.factor": 1
  }
}' http://localhost:8083/connectors

echo "Conector MQTT configurado com sucesso."