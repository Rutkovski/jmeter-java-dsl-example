package ru.rustore.loadtests.googletest;

import ru.rustore.loadtests.AppConfigLoader;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;

import java.util.ArrayList;

public class VarsInstaller {

    public static final String DEVICE_ID = "a1b2c3d4e5f6g7h8--100000000";

    protected static ArrayList<DslTestPlan.TestPlanChild> getSpecialElements(AppConfigLoader CONFIG) {
//        some elements
        return new ArrayList<DslTestPlan.TestPlanChild>();
    }

}
