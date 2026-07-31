package com.aladin.android.cucumber

import com.aladin.android.sky.AstroMath
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals

class AstroMathSteps {
    private var raDeg: Double = 0.0
    private var result: String = ""

    @Given("I have a RA degree of {double}")
    fun i_have_a_ra_degree_of(ra: Double) {
        this.raDeg = ra
    }

    @When("I format it as HMS")
    fun i_format_it_as_hms() {
        this.result = AstroMath.formatRaHms(raDeg)
    }

    @Then("the result should be {string}")
    fun the_result_should_be(expected: String) {
        assertEquals(expected, result)
    }
}
