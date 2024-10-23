package com.example;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CustomTimeScaleSinkConnector extends SinkConnector {

    private Map<String, String> configProps;

    @Override
    public void start(Map<String, String> props) {
        this.configProps = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        // Retorna a classe da tarefa que será usada (no seu caso, CustomTimeScaleSinkTask)
        return CustomTimeScaleSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Cria uma configuração para cada tarefa (maxTasks define quantas instâncias paralelas)
        List<Map<String, String>> configs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            configs.add(configProps);  // Repasse as configurações para cada tarefa
        }
        return configs;
    }

    @Override
    public void stop() {
        // Fechar recursos se necessário
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef(); // Aqui você pode adicionar validações para as configurações, se necessário
    }

    @Override
    public String version() {
        return "1.0"; // Versão do conector
    }
}