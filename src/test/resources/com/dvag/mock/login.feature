@ignore
Feature: ZOB-Login

  Scenario: POST loginwithoutotp
    Given url zobHost
    And path '/zob/services/authentication/loginwithoutotp'
    And header Content-Type = 'application/json'
    And request {userIdOrAlias:#(zobUser), password:#(zobPassword), clientTyp:IPAD}
    When method post
    Then status 200
    And def ssoid = response