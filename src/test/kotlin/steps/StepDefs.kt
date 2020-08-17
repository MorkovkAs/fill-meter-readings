package steps

import io.cucumber.java8.En
import io.cucumber.java8.Scenario
import org.openqa.selenium.By.cssSelector
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

class StepDefs : En {

    lateinit var driver: WebDriver

    init {
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\driver\\chromedriver.exe")
        driver = ChromeDriver()

        Given("I am on the comfort-group login page") {
            println("Hello, we are opening a browser")
            driver.get("https://room.comfort-group.ru/login/")
        }

        Then("I should see {string}") { text: String ->
            driver.pageSource.contains(text, true)
        }

        When("I fill login {string} and password {string}") { login: String, password: String ->
            driver.findElement(cssSelector("input[name='account']")).sendKeys(login)
            driver.findElement(cssSelector("input[name='password']")).sendKeys(password)
        }

        When("I click login button") {
            println("Trying to login")
            driver.findElement(cssSelector("button[type='submit']")).click()
        }

        When("I click devices") {
            println("Clicking devices button")
            driver.findElement(cssSelector("[title='Счетчики'] span")).click()
        }

        When("I click water") {
            println("Clicking water button")
            driver.findElement(cssSelector("[title='Передать показания / Вода'] span")).click()
        }

        When("I click heat") {
            println("Clicking heat button")
            driver.findElement(cssSelector("[title='Передать показания / Отопление'] span")).click()
        }

        When("I fill water cold: {string} and hot: {string}") { cold: String, hot: String ->
            println("Filling water data: [cold: $cold; hot: $hot]")
            driver.findElements(cssSelector("tr td .form-group input[type='text']"))[0].sendKeys(cold)
            driver.findElements(cssSelector("tr td .form-group input[type='text']"))[1].sendKeys(hot)
        }

        When("I fill heat: {string}") { heat: String ->
            println("Filling heat data: [heat: $heat]")
            driver.findElement(cssSelector("tr td .form-group input[type='text']")).sendKeys(heat)
        }

        When("I click save button") {
            println("Saving")
            driver.findElement(cssSelector("input[value='Отправить показания']")).click()
        }

        Then("I wait for {int} seconds") { n: Long ->
            Thread.sleep(n * 1000)
        }

        After { _: Scenario ->
            driver.quit()
        }
    }
}