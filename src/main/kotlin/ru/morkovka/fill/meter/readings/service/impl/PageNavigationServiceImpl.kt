package ru.morkovka.fill.meter.readings.service.impl

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import ru.morkovka.fill.meter.readings.entity.User
import ru.morkovka.fill.meter.readings.service.EncryptorService
import ru.morkovka.fill.meter.readings.service.PageNavigationService

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
    override fun sendReadings(user: User) {
        var password = ""
        if (!StringUtils.isEmpty(user.password)) {
            password = encryptorService.decrypt(user.password) ?: ""
        }

        val driver = ChromeDriver()
        driver.get("https://room.comfort-group.ru/login/")
        driver.findElement(By.cssSelector("input[name='account']")).sendKeys(user.login)
        driver.findElement(By.cssSelector("input[name='password']")).sendKeys(password)
        driver.findElement(By.cssSelector("button[type='submit']")).click()
        driver.pageSource.contains("Текущий баланс", true)
        logger.info("[user.id = ${user.id}]\t Login successful")

        driver.findElement(By.cssSelector("[title='Счетчики'] span")).click()
        driver.pageSource.contains("Передать показания / Вода", true)
        driver.pageSource.contains("Передать показания / Отопление", true)
        driver.findElement(By.cssSelector("[title='Передать показания / Вода'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Вода)", true)
        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[0].sendKeys(user.cold)
        driver.findElements(By.cssSelector("tr td .form-group input[type='text']"))[1].sendKeys(user.hot)
        Thread.sleep(5 * 1000)
        //driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        //driver.pageSource.contains("успешно", true)
        logger.info("[user.id = ${user.id}]\t Water saved")

        driver.findElement(By.cssSelector("[title='Передать показания / Отопление'] span")).click()
        driver.pageSource.contains("Отправить показания счетчиков (Отопление)", true)
        driver.findElement(By.cssSelector("tr td .form-group input[type='text']")).sendKeys(user.heat)
        Thread.sleep(5 * 1000)
        //driver.findElement(By.cssSelector("input[value='Отправить показания']")).click()
        //driver.pageSource.contains("успешно", true)
        logger.info("[user.id = ${user.id}]\t Heat saved")

        driver.quit()
    }
}