package acceptance;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "test/acceptance/features",
    glue = "steps"
)
public class RunCucumberTest {
}
