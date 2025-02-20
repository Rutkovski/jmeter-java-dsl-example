package ru.rustore.loadtests;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Getter
@Setter
public class AppConfigLoader extends BaseConfigLoader {
    private Map<String, Float> percentage = new HashMap<>();

    public static AppConfigLoader fromPropertiesFiles(String appFilePath) {
        AppConfigLoader loadAppConfig = new AppConfigLoader();
        Configuration config = loadConfiguration(appFilePath);
        loadAppConfig.loadConfigValues(config);
        return loadAppConfig;
    }

    private void loadConfigValues(Configuration config) {
        for (Iterator<String> it = config.getKeys(); it.hasNext(); ) {
            String key = it.next();
            environmentVariables.put(key, config.getString(key));
        }
        loadPercentage(config.subset("percentage"));
    }

    private void loadPercentage(Configuration subset) {
        subset.getKeys().forEachRemaining(key -> {
            String fullKey = "percentage." + key;
            String value = environmentVariables.get(fullKey);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    this.percentage.put(key, Float.parseFloat(value));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid float value for key " + fullKey + ": " + value);
                }
            } else {
                System.err.println("Missing or empty value for key " + fullKey);
            }
        });
    }

}