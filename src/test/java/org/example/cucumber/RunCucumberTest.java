package org.example.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Entry point for Maven Surefire: runs every .feature file under org/example/cucumber
 * with the step definitions in this package.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("org.example.cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.example.cucumber")
public class RunCucumberTest {
}
