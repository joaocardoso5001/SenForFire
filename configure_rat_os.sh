#!/bin/sh

echo "Configurando o Kafka Connect para usar o conector MirrorMaker..."

curl -X POST -H "Content-Type: application/json" --data '{
  "name": "rat-eos-pc-mirror-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "topics": "rat-eos-pc",
    "source.cluster.alias": "source-cluster",
    "target.cluster.alias": "target-cluster",
    "tasks.max": "1",
    "replication.factor": "1",
    "source.cluster.bootstrap.servers": "atnog-io-iot4fire.av.it.pt:9092",
    "source.cluster.security.protocol": "SASL_PLAINTEXT",
    "source.cluster.sasl.mechanism": "PLAIN",
    "source.cluster.sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"rat_eos_pc\" password=\"5e961dc4-be98-4788-82a6-f353147c67b\";",
    "auto.offset.reset": "latest",
    "target.cluster.bootstrap.servers": "kafka:9092",
    "target.cluster.security.protocol": "PLAINTEXT",
    "sync.acls.enabled": "false"
  }
}' http://localhost:8083/connectors

echo "Conector MirrorMaker configurado com sucesso."