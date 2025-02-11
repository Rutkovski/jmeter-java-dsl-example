package ru.rustore.loadtests;

public abstract class BaseSamplerFactory {
    protected final String BASE_URL;

    public BaseSamplerFactory(String baseUrlPath) {
        this.BASE_URL = baseUrlPath;
    }
}
