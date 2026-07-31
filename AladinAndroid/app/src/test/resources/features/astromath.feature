Feature: AstroMath Formats

  Scenario: Format Right Ascension in HMS
    Given I have a RA degree of 15.0
    When I format it as HMS
    Then the result should be "01h00m00.00s"
