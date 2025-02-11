package ru.rustore.loadtests.googletest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.rustore.loadtests.BaseLoadTest;

import static ru.rustore.loadtests.googletest.VarsInstaller.getSpecialElements;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

@Tag("google")
public class AppTests extends BaseLoadTest {

    private static final String APP_NAME = "googletest";
    private final SamplerFactory samplers;

    public AppTests() {
        super(APP_NAME);
        this.samplers = new SamplerFactory("http://google.com");
        TEST_PLAN_ELEMENTS.addAll(getSpecialElements(CONFIG));
    }

    @Test
    @Tag(APP_NAME + "-smoke")
    public void smokeTest() throws Exception {
        runStdSmokeTest(samplers);
    }


    @Test
    @Tag(APP_NAME + "-debug")
    public void debugTest() throws Exception {
        TEST_PLAN_ELEMENTS.add(threadGroup("debug", 1, 1,
                samplers.getGooogleCom()
        ));
        runStdDebugTest();
    }
}