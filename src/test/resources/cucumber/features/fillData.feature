Feature: Fill data

  Scenario Outline: Login and fill water and heat data in comfort-group page
    Given I am on the comfort-group login page
    Then I should see 'Вход в личный кабинет'
    When I fill login <login> and password <password>
    And I click login button
    Then I should see 'Главная'
    And I should see 'Текущий баланс'

    When I click devices
    Then I should see 'Передать показания / Вода'
    And I should see 'Передать показания / Отопление'
    When I click water
    Then I should see 'Отправить показания счетчиков (Вода)'
    And I should see 'Холодное водоснабжение'
    And I should see 'ХВС для ГВС'
    When I fill water cold: <cold> and hot: <hot>
    And I wait for 3 seconds
    #And I click save button
    And I wait for 5 seconds

    When I click heat
    Then I should see 'Отправить показания счетчиков (Отопление)'
    When I fill heat: <heat>
    And I wait for 3 seconds
    #And I click save button
    And I wait for 5 seconds


    Examples:
      | login | password | cold  | hot   | heat    |
      | ''    | ''       | '1.1' | '2.1' | '0.589' |