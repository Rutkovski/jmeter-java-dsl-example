package ru.rustore.loadtests;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.JtlWriter;
import us.abstracta.jmeter.javadsl.core.threadgroups.BaseThreadGroup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.*;
import static us.abstracta.jmeter.javadsl.core.listeners.AutoStopListener.AutoStopCondition.errors;
import static us.abstracta.jmeter.javadsl.core.listeners.AutoStopListener.AutoStopCondition.sampleTime;


public abstract class BaseLoadTest {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseLoadTest.class);
    protected final String REPORT_PATH = Paths.get("build", "reports", "jmeterReports").toString();
    protected final AppConfigLoader CONFIG;
    protected final String TEST_ENV;
    protected final String TITLE;
    protected final String INFLUX_URL;
    protected final List<DslTestPlan.TestPlanChild> TEST_PLAN_ELEMENTS = new ArrayList<>();


    public BaseLoadTest(String appName) {
        this.CONFIG = AppConfigLoader.fromPropertiesFiles(String.format("config/%s.properties", appName));
        this.TITLE = appName;
        this.TEST_ENV = CONFIG.getVar("TEST_ENV");
        this.INFLUX_URL = CONFIG.getVar("INFLUX_URL");
    }

    @BeforeEach
    public void setUp() throws IOException {
        var directory = new File(REPORT_PATH);
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        var jtlFilePath = Paths.get(REPORT_PATH, "httpReport" + File.separator + "statistics.json");
        var resultCompareFilePath = Paths.get(REPORT_PATH, File.separator + "compare-report.json");
        var outputFilePath = Paths.get(REPORT_PATH, "aggregate.csv");
        if (Files.exists(jtlFilePath)) {
            LOG.info("JSON file in path {}, start conversion. ", jtlFilePath);
//            JsonToCSVConverter.processJsonFile(jtlFilePath.toString(), outputFilePath.toString());
            LOG.info("JSON file in path {}, finish conversion. ", jtlFilePath);
        } else {
            LOG.warn("JSON file not found in path {}, skipping conversion. ", jtlFilePath);
        }


    }

    protected ArrayList<DslTestPlan.TestPlanChild> commonElements(String application, String influxUrl) {
        var testPlanChildren = new ArrayList<DslTestPlan.TestPlanChild>();
//add default values for http client
        testPlanChildren.add(httpDefaults().encoding(StandardCharsets.UTF_8).connectionTimeout(Duration.ofSeconds(10)).responseTimeout(Duration.ofMinutes(1)));
        testPlanChildren.add(httpCache().disable());
        testPlanChildren.add(httpCookies().disable());
//add listeners
//        testPlanChildren.add(influxDbListener(influxUrl).application(application));
        testPlanChildren.add(htmlReporter(REPORT_PATH, "httpReport"));
        testPlanChildren.add(jtlWriter(REPORT_PATH, "JtlResults.jtl").withAssertionResults(false).withSampleAndErrorCounts(true).withHostname(true));
        testPlanChildren.add(jtlWriter(REPORT_PATH, "JtlErrors.jtl").logOnly(JtlWriter.SampleStatus.ERROR).saveAsXml(true).withResponseData(true));
//add autoStop criteria
        testPlanChildren.add(autoStop("autoStopPercentOfErrors").when(errors().percent().every(Duration.ofSeconds(20)).greaterThan(10.0).holdsFor(Duration.ofSeconds(60))));
        testPlanChildren.add(autoStop("autoStopSamplerTime90%").when(sampleTime().percentile(90.0).every(Duration.ofSeconds(20)).greaterThan(Duration.ofSeconds(4)).holdsFor(Duration.ofSeconds(60))));
        return testPlanChildren;
    }

    protected void assertSmokeTest(TestPlanStats stats) {
        assertThat(stats.overall().sampleTimePercentile99()).as("99th percentile response time > 10s").isLessThan(Duration.ofSeconds(10));
        assertThat(stats.overall().errorsCount()).as("Count of errors > 0").isLessThan(1);
    }

    protected void assertStabilityTest(TestPlanStats stats) {
        assertThat(stats.overall().sampleTimePercentile99()).as("99th percentile response time > 10s").isLessThan(Duration.ofSeconds(10));
        assertThat(stats.overall().sampleTime().perc90()).as("90th percentile response time > 5s").isLessThan(Duration.ofSeconds(5));
        var totalRequests = stats.overall().samplesCount();
        var errorCount = stats.overall().errorsCount();
        var errorPercentage = (double) errorCount / totalRequests * 100;
        assertThat(errorPercentage).as("Count of errors > 10%").isLessThan(10.0);
    }

    protected void validatePercentageSum(Map<String, Float> percentages) {
        var sum = percentages.values().stream().reduce(0.0f, Float::sum);
        if (sum > 102 || sum < 98) {
            LOG.error("Checking the sum of requests distribution percentages, it should be between 98% and 102%. Current value: {}%. Test was stopped", sum);
            Assertions.fail("The sum of requests distribution percentages is out of the acceptable range: " + sum + "%");
        } else {
            LOG.info("Checking sum of of requests distribution percentages for the app in {}.properties is: {}%", TITLE, sum);
        }
    }

    protected List<BaseThreadGroup.ThreadGroupChild> getMethodsForTest(BaseSamplerFactory samplers, boolean isTestStable) {
        if (isTestStable) {
            validatePercentageSum(CONFIG.getPercentage());
        }
        var listOfMethods = new ArrayList<BaseThreadGroup.ThreadGroupChild>();
        for (var entry : CONFIG.getPercentage().entrySet()) {
            try {
                var method = samplers.getClass().getMethod(entry.getKey());
                var child = (BaseThreadGroup.ThreadGroupChild) method.invoke(samplers);
                if (isTestStable) {
                    child = percentController(entry.getValue(), child);
                }
                listOfMethods.add(child);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Method not found for key: " + entry.getKey(), e);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking method for key: " + entry.getKey(), e);
            }
        }
        return listOfMethods;
    }

    protected void validateSamplerMethodsExist(BaseSamplerFactory samplers) {
        var missingMethods = new ArrayList<String>();
        for (var entry : CONFIG.getPercentage().entrySet()) {
            try {
                samplers.getClass().getMethod(entry.getKey());
            } catch (NoSuchMethodException e) {
                missingMethods.add(entry.getKey());
            }
        }
        if (!missingMethods.isEmpty()) {
            LOG.error("Missing methods in SamplerFactory for keys: {}", missingMethods);
            Assumptions.assumeTrue(missingMethods.isEmpty(), "Test skipped due to missing methods in SamplerFactory: " + missingMethods);
        } else {
            LOG.info("Checking the consistency of methods in SampleFabric with {}.properties has been completed successfully!", TITLE);
        }
    }

    protected void runStdSmokeTest(BaseSamplerFactory samplerFactory) throws IOException {
        validateSamplerMethodsExist(samplerFactory);
        TEST_PLAN_ELEMENTS.addAll(commonElements(TITLE, INFLUX_URL));
        var methodsForSmokeTest = getMethodsForTest(samplerFactory, false);
        TEST_PLAN_ELEMENTS.add(threadGroup("smoke", 3, Duration.ofSeconds(10), methodsForSmokeTest.toArray(new BaseThreadGroup.ThreadGroupChild[0])));
        var stats = testPlan(TEST_PLAN_ELEMENTS.toArray(DslTestPlan.TestPlanChild[]::new)).run();
        assertSmokeTest(stats);
    }

    protected void runStdSmokeTest(BaseSamplerFactory samplerFactory, BaseThreadGroup.ThreadGroupChild... samplerMethods) throws IOException {
        validateSamplerMethodsExist(samplerFactory);
        TEST_PLAN_ELEMENTS.addAll(commonElements(TITLE, INFLUX_URL));
        TEST_PLAN_ELEMENTS.add(threadGroup("smoke", 3, Duration.ofSeconds(30), samplerMethods));
        var stats = testPlan(TEST_PLAN_ELEMENTS.toArray(DslTestPlan.TestPlanChild[]::new)).run();
        assertSmokeTest(stats);
    }

    protected void runStdStableTest(BaseSamplerFactory samplerFactory) throws IOException {
        validateSamplerMethodsExist(samplerFactory);
        TEST_PLAN_ELEMENTS.addAll(commonElements(TITLE, INFLUX_URL));
        var methodsForStableTest = getMethodsForTest(samplerFactory, true);
        TEST_PLAN_ELEMENTS.add(
                rpsThreadGroup("stable")
                        .maxThreads(Integer.parseInt(CONFIG.getVar("MAX_THREAD_COUNT")))
                        .rampToAndHold(
                                Integer.parseInt(CONFIG.getVar("LOAD_RPS")),
                                Duration.ofMinutes(Integer.parseInt(CONFIG.getVar("RUMP_UP_PERIOD_MIN"))),
                                Duration.ofMinutes(Integer.parseInt(CONFIG.getVar("HOLD_LOAD_MIN")))
                        )
                        .children(methodsForStableTest.toArray(new BaseThreadGroup.ThreadGroupChild[0]))
        );
        var stats = testPlan(TEST_PLAN_ELEMENTS.toArray(DslTestPlan.TestPlanChild[]::new)).run();
        assertStabilityTest(stats);
    }

    protected void runStdStableTest(BaseSamplerFactory samplerFactory, BaseThreadGroup.ThreadGroupChild... samplerMethods) throws IOException {
        validateSamplerMethodsExist(samplerFactory);
        TEST_PLAN_ELEMENTS.addAll(commonElements(TITLE, INFLUX_URL));
        TEST_PLAN_ELEMENTS.add(
                rpsThreadGroup("stable")
                        .maxThreads(Integer.parseInt(CONFIG.getVar("MAX_THREAD_COUNT")))
                        .rampToAndHold(
                                Integer.parseInt(CONFIG.getVar("LOAD_RPS")),
                                Duration.ofMinutes(Integer.parseInt(CONFIG.getVar("RUMP_UP_PERIOD_MIN"))),
                                Duration.ofMinutes(Integer.parseInt(CONFIG.getVar("HOLD_LOAD_MIN")))
                        )
                        .children(samplerMethods)
        );
        var stats = testPlan(TEST_PLAN_ELEMENTS.toArray(DslTestPlan.TestPlanChild[]::new)).run();
        assertStabilityTest(stats);
    }

    protected void runStdDebugTest() throws IOException {
        TEST_PLAN_ELEMENTS.addAll(commonElements(TITLE, INFLUX_URL));
        TEST_PLAN_ELEMENTS.add(jtlWriter(REPORT_PATH, "JtlResultsSuccessDebug.jtl")
                .logOnly(JtlWriter.SampleStatus.SUCCESS)
                .saveAsXml(true)
                .withVariables()
                .withResponseData(true));
        TEST_PLAN_ELEMENTS.add(resultsTreeVisualizer());
        var stats = testPlan(TEST_PLAN_ELEMENTS.toArray(DslTestPlan.TestPlanChild[]::new)).run();
        assertSmokeTest(stats);
    }
}
