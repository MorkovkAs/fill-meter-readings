package ru.morkovka.fill.meter.readings.service.impl

import io.github.bonigarcia.wdm.WebDriverManager
import org.apache.commons.lang3.exception.ExceptionUtils
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.events.AbstractWebDriverEventListener
import org.openqa.selenium.support.events.EventFiringWebDriver
import org.openqa.selenium.support.events.WebDriverEventListener
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import ru.morkovka.fill.meter.readings.entity.ComfortUser
import ru.morkovka.fill.meter.readings.entity.ResultFill
import ru.morkovka.fill.meter.readings.service.EncryptorService
import ru.morkovka.fill.meter.readings.service.PageNavigationService
import java.io.File

@Service
class PageNavigationServiceImpl : PageNavigationService {

    @Autowired
    private lateinit var encryptorService: EncryptorService

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    lateinit var wait: WebDriverWait

    init {
        WebDriverManager.chromedriver().setup()
    }

    //помнить, что в текущей реализации эта штука будет оптимальнее в одном потоке
    @Synchronized
    override fun sendReadings(user: ComfortUser): ResultFill {
        val result = ResultFill()
        var driver: WebDriver? = null
        try {
            val options = ChromeOptions()
            options.addArguments("--headless")
            options.addArguments("--disable-gpu")
            val url = System.getenv("GOOGLE_CHROME_SHIM")
            url?.let { options.setBinary(url) }
            driver = EventFiringWebDriver(ChromeDriver(options))

            val errorListener: WebDriverEventListener = object : AbstractWebDriverEventListener() {
                override fun onException(throwable: Throwable, driver: WebDriver) {
                    logger.warn("[user.id = ${user.id}]\t Something went wrong on WebDriverEventListener")
                    val scrExceptionFile: File = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
                    result.error = scrExceptionFile

                    throw throwable
                }
            }
            driver.register(errorListener)
            wait = WebDriverWait(driver, 1)

            login(user, driver)
            openMetersBlock(user, driver)
            //result.srcWater = fillWater(user, driver)
            result.srcHeat = fillHeat(user, driver)
        } catch (ex: Exception) {
            logger.warn("[user.id = ${user.id}]\t Something went wrong")
            logger.warn(ExceptionUtils.getStackTrace(ex))
        } finally {
            driver?.let { driver.quit() }
        }

        return result
    }

    private fun login(user: ComfortUser, driver: WebDriver) {
        var password = ""
        if (!StringUtils.isEmpty(user.password)) {
            password = encryptorService.decrypt(user.password) ?: ""
        }

        driver.get("https://room.comfort-group.ru/login/")
        driver.findElement(By.cssSelector("input[name='account']")).sendKeys(user.login)
        driver.findElement(By.cssSelector("input[name='password']")).sendKeys(password)
        driver.findElement(By.cssSelector("button[type='submit']")).click()
        driver.pageSource.contains("Текущий баланс", true)
        logger.info("[user.id = ${user.id}]\t Login successful")
    }

    private fun openMetersBlock(user: ComfortUser, driver: WebDriver) {
        driver.findElement(By.cssSelector("[title='Счетчики'] span")).click()
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.elementToBeClickable(By.cssSelector("[title='Передать показания / Вода'] span"))))
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.elementToBeClickable(By.cssSelector("[title='Передать показания / Отопление'] span"))))
        driver.pageSource.contains("Передать показания / Вода", true)
        driver.pageSource.contains("Передать показания / Отопление", true)
        logger.info("[user.id = ${user.id}]\t openMetersBlock successful")
    }

    private fun fillWater(user: ComfortUser, driver: WebDriver): File {
        driver.findElement(By.cssSelector("[title='Передать показания / Вода'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Вода)", true)
        logger.info("[user.id = ${user.id}]\t fillWater opened successful")

        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[0].sendKeys(user.cold)
        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[1].sendKeys(user.hot)
        driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        driver.pageSource.contains("Данные успешно отправлены", true)

        val scrWaterFile: File = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        //FileUtils.copyFile(scrWaterFile, File("c:\\tmp\\waterFile.png"))
        logger.info("[user.id = ${user.id}]\t Water saved")

        return scrWaterFile
    }

    private fun fillHeat(user: ComfortUser, driver: WebDriver): File {
        driver.findElement(By.cssSelector("[title='Передать показания / Отопление'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Отопление)", true)
        logger.info("[user.id = ${user.id}]\t fillHeat opened successful")

        driver.findElement(By.cssSelector("tr td .form-group input[type='text']")).sendKeys(user.heat)
        driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        driver.pageSource.contains("Данные успешно отправлены", true)

        val scrHeatFile: File = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        //FileUtils.copyFile(scrHeatFile, File("c:\\tmp\\heatFile.png"))
        logger.info("[user.id = ${user.id}]\t Heat saved")

        return scrHeatFile
    }
}