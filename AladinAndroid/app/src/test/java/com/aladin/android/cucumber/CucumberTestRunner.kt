package com.aladin.android.cucumber

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/resources/features"],
    glue = ["com.aladin.android.cucumber"],
    plugin = ["pretty", "html:build/reports/cucumber.html"]
)
class CucumberTestRunner
