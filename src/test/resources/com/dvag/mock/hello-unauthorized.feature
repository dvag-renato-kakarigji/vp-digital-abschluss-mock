Feature: Tests für die Actuator-Endpunkte

  Scenario: GET hello
    Given url host
    And path 'rest/v1/hello'
    And header Content-Type = 'application/json'
    When method get
    Then status 403

  Scenario: GET hello/vb
    Given url host
    And path 'rest/v1/hello/vb'
    And header Content-Type = 'application/json'
    When method get
    Then status 403

  Scenario: GET hello/mitbenutzer
    Given url host
    And path 'rest/v1/hello/mitbenutzer'
    And header Content-Type = 'application/json'
    When method get
    Then status 403

  Scenario: GET hello/vertreter
    Given url host
    And path 'rest/v1/hello/vertreter'
    And header Content-Type = 'application/json'
    When method get
    Then status 403

  Scenario: GET hello/innendienst
    Given url host
    And path 'rest/v1/hello/innendienst'
    And header Content-Type = 'application/json'
    When method get
    Then status 403

  Scenario: GET hello/kunde
    Given url host
    And path 'rest/v1/hello/kunde'
    And header Content-Type = 'application/json'
    When method get
    Then status 403