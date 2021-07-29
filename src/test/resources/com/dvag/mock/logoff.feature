@ignore
Feature: ZOB-Logoff

  Scenario: DELETE logoff
    Given url zobHost
    And path '/zob/services/authentication/logoff'
    And header Content-Type = 'application/json'
    When method delete
    Then status 200