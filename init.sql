-- Criação da extensão PostGIS
CREATE EXTENSION postgis;

-- Criação da tabela User
CREATE TABLE IF NOT EXISTS "User" (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Criação da tabela Group
CREATE TABLE IF NOT EXISTS "Group" (
    group_id SERIAL PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    location geography(Point, 4326) -- Armazenamento de um ponto geográfico (latitude, longitude)
);

-- Criação da tabela User_Group
CREATE TABLE IF NOT EXISTS "User_Group" (
    user_id INT NOT NULL,
    group_id INT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES "User" (user_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES "Group" (group_id) ON DELETE CASCADE
);

-- Criação da tabela Geographic_zones
CREATE TABLE IF NOT EXISTS Geographic_zones (
    zone_id SERIAL PRIMARY KEY,
    zone_name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    region VARCHAR(100),
    country VARCHAR(100),
    city VARCHAR(100),
    description TEXT
);

-- Criação da tabela Sensor
CREATE TABLE IF NOT EXISTS Sensor (
    sensor_id SERIAL PRIMARY KEY,
    sensor_name VARCHAR(100) NOT NULL,
    sensor_type VARCHAR(100),
    installation_date DATE,
    sensor_model VARCHAR(100),
    sensor_manufacturer VARCHAR(100),
    specifications TEXT,
    status VARCHAR(50),
    location geography(Point, 4326), -- Armazenamento de um ponto geográfico (latitude, longitude)
    connection_type VARCHAR(50),
    zone_id INT,
    group_id INT,
    FOREIGN KEY (zone_id) REFERENCES Geographic_zones (zone_id) ON DELETE SET NULL,
    FOREIGN KEY (group_id) REFERENCES "Group" (group_id) ON DELETE SET NULL
);

-- Criação da tabela sensor_data
CREATE TABLE IF NOT EXISTS sensor_data (
  created TIMESTAMPTZ DEFAULT NOW(),
  device_id TEXT NOT NULL,
  metric TEXT NOT NULL,
  value DOUBLE PRECISION NOT NULL,
  source TEXT 
);

-- Tornar a tabela sensor_data uma hypertable
SELECT create_hypertable('sensor_data', 'created');