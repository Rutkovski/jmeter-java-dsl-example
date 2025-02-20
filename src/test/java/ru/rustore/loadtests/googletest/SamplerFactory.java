package ru.rustore.loadtests.googletest;

import ru.rustore.loadtests.BaseSamplerFactory;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;

/**
 * Класс для построения сэмплеров. Условно - библиотека готовых запросов к сервису
 * которые потом можно будет использовать при построении тестов
 */

public class SamplerFactory extends BaseSamplerFactory {

    public SamplerFactory(String baseUrlPath) {
        super(baseUrlPath);
    }

    public DslHttpSampler getAutomationpracticeCom() {
        return httpSampler("GET http://automationpractice.com/",BASE_URL + "/");
    }


}

