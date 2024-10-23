package com.example;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;

public class TTNSourceConnector extends SourceConnector {

    private Map<String, String> configProps;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.configProps = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return TTNSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Define como distribuir as tarefas
        return List.of(configProps);
    }

    @Override
    public void stop() {
        // Método para parar o conector
    }

    @Override
    public ConfigDef config() {
        return TTNSourceConnectorConfig.conf();
    }
}