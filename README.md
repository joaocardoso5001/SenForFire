# SenForFire

The SenForFire project integrates wireless sensor networks with Kafka, MQTT, and Grafana for forest fire prevention and early detection. This repository contains all the necessary configurations, connectors, and plugins required to run the system using Kafka Connect to stream data via MQTT, validate it through JSON schemas, and visualize the results in Grafana: https://senforfire.xdi.uevora.pt/

## Project Structure
### Directories
1. kafka-connect-json-schema-validator/kafka-connect-json-schema-validator

* Contains the main class that accesses the topic where all the sensor data is sent. This class validates the incoming data against a predefined JSON schema and inserts it into a database. The data could be a list of messages, a single message, or multiple measurements within the same message.

2. kafka-connect-mqtt/kafka-connect-mqtt

* This folder holds the class that connects to Mosquitto, an MQTT broker, enabling data ingestion via MQTT when data is sent to your system.

3. kafka-connect-mqtt-ttn/kafka-connect-mqtt-ttn

* Contains the connector that links to The Things Network (TTN) via MQTT, enabling the ingestion of LoRaWAN-based sensor data from TTN into Kafka.
  
4. kafka-connect-plugins

* Contains Kafka Connect plugins, including custom or third-party connectors, used to process and manage the data flowing through the Kafka ecosystem. These plugins handle tasks such as data transformation and serialization.

### Files

1. README.md

* This file provides an overview and documentation of the project.

2. configure_kafka_connect.sh

* A shell script to automate the setup and configuration of Kafka Connect. It contains commands to install and configure connectors, ensuring everything is set up correctly for data streaming.

configure_rat_os.sh

* This script is used to receive data on a Kafka topic from another Kafka topic in a different project. 

3. docker-compose.yml

* The Docker Compose file orchestrates the various services required for the project, such as Kafka, Zookeeper, Grafana, Mosquitto (MQTT broker), Kafka-Connect. This file automates the deployment of containers in an environment.

4. grafana

* This file defines the port where Grafana is running.

5. init.sql

* A SQL script that sets up the initial database schema or tables used by Kafka Connect or Grafana to store data for reporting and analytics.

6. mosquitto.conf

* Configuration file for Mosquitto, the MQTT broker. It defines the broker settings, including SSL/TLS configurations, ports, and authentication details, to securely receive data from sensors.

7. swagger.yaml

* This file provides a Swagger/OpenAPI specification that shows the data model based on Smart Data Models, providing an overview of the JSON Schema structure used in the SenForFire project. This is useful for defining and documenting how data should be structured when interacting with the system. https://senforfire.xdi.uevora.pt/swagger-ui/
