Feature: Tests für die Actuator-Endpunkte

  Scenario: GET Info
    Given url host
    And path 'actuator/info'
    When method get
    Then status 200
    And match response.version == '#notnull'

  Scenario: GET Health
    Given url host
    And path 'actuator/health'
    When method get
    Then status 200
    And match response.status == 'UP'
    And match response.details.gracefulShutdownHealthCheck.status == 'UP'
    And match response.details.diskSpace.status == 'UP'
