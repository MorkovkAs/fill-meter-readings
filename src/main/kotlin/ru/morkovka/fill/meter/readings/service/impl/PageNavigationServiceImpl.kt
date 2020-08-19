package ru.morkovka.fill.meter.readings.service.impl

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import ru.morkovka.fill.meter.readings.entity.User
import ru.morkovka.fill.meter.readings.service.EncryptorService
import ru.morkovka.fill.meter.readings.service.PageNavigationService
import java.io.File

@Service
class PageNavigationServiceImpl : PageNavigationService {

    @Autowired
    private lateinit var encryptorService: EncryptorService

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        WebDriverManager.chromedriver().setup()
    }

    //вспомнить, что эта штука возможно будет оптимальнее в одном потоке
    @Synchronized
    override fun sendReadings(user: User): MutableList<File> {
        val list = mutableListOf<File>()
        val driver = ChromeDriver()

        login(user, driver)
        openMetersBlock(driver)
        list.add(fillWater(user, driver))
        list.add(fillHeat(user, driver))

        driver.quit()

        return list
    }

    private fun login(user: User, driver: WebDriver) {
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

    private fun openMetersBlock(driver: WebDriver) {
        driver.findElement(By.cssSelector("[title='Счетчики'] span")).click()
        driver.pageSource.contains("Передать показания / Вода", true)
        driver.pageSource.contains("Передать показания / Отопление", true)
    }

    private fun fillWater(user: User, driver: WebDriver): File {
        driver.findElement(By.cssSelector("[title='Передать показания / Вода'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Вода)", true)
        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[0].sendKeys(user.cold)
        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[1].sendKeys(user.hot)
        Thread.sleep(5 * 1000)
        //driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        //driver.pageSource.contains("успешно", true)

        val scrWaterFile: File = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        //FileUtils.copyFile(scrWaterFile, File("c:\\tmp\\waterFile.png"))
        logger.info("[user.id = ${user.id}]\t Water saved")

        return scrWaterFile
    }

    private fun fillHeat(user: User, driver: WebDriver): File {
        driver.findElement(By.cssSelector("[title='Передать показания / Отопление'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Отопление)", true)
        driver.findElement(By.cssSelector("tr td .form-group input[type='text']")).sendKeys(user.heat)
        Thread.sleep(5 * 1000)
        //driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        //driver.pageSource.contains("успешно", true)

        val scrHeatFile: File = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        //FileUtils.copyFile(scrHeatFile, File("c:\\tmp\\heatFile.png"))
        logger.info("[user.id = ${user.id}]\t Heat saved")

        return scrHeatFile
    }
}