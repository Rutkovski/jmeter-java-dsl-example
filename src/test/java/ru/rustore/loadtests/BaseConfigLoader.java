package ru.rustore.loadtests;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class BaseConfigLoader {
    private static final String BASE_CONFIG = "application.properties";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    protected Map<String, String> environmentVariables;

    public BaseConfigLoader() {
        loadEnvironmentVariables();
    }

    public void loadEnvironmentVariables() {
        environmentVariables = new HashMap<>();
        Configuration config = loadConfiguration(BASE_CONFIG);
        config.getKeys().forEachRemaining(key -> environmentVariables.put(key, config.getString(key)));
        dotenv.entries().forEach(entry -> environmentVariables.put(entry.getKey(), entry.getValue()));
        environmentVariables.putAll(System.getenv());
        System.getProperties().forEach((key, value) -> environmentVariables.put((String) key, (String) value));
    }


    public static Configuration loadConfiguration(String filePath) {
        try (InputStream inputStream = BaseConfigLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + filePath);
            }
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.read(reader);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration from file: " + filePath, e);
        }
    }

    public String getVar(String key) {
        return environmentVariables.get(key);
    }

}