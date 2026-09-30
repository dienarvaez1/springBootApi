package org.example.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * End-to-end suite, run by Maven Failsafe during {@code ./mvnw verify}. Starts the whole
 * application on a random port against the local Postgres (database springboot_app_e2e),
 * so Postgres must be running on localhost:5432.
 * Kept in its own package so it has its own Spring context, separate from the MockMvc tests.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("org.example.e2e")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.example.e2e")
public class RunE2EIT {
}
