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
        // Инициализируем карту переменных окружения из файла конфигурации
        environmentVariables = new HashMap<>();
        Configuration config = loadConfiguration(BASE_CONFIG);
        // Добавляем переменные из файла конфигурации с самым низким приоритетом
        config.getKeys().forEachRemaining(key -> environmentVariables.put(key, config.getString(key)));
        // Добавляем переменные из .env и обновляем
        dotenv.entries().forEach(entry -> environmentVariables.put(entry.getKey(), entry.getValue()));
        // Добавляем системные переменные окружения и обновляем
        environmentVariables.putAll(System.getenv());
        // Загружаем переменные свойств, переданные при запуске, и обновляем
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

    // Метод для получения значения переменной окружения по ключу
    public String getVar(String key) {
        return environmentVariables.get(key);
    }

}