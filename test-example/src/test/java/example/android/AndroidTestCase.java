package example.android;

import java.io.File;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.android.AndroidDriver;

import com.google.common.io.Files;

@RunWith(Arquillian.class)
public class AndroidTestCase {

    @Drone
    AndroidDriver driver;

    @Test
    public void testGoogle() throws Exception {
        // And now use this to visit Google
        driver.get("http://www.google.com");

        // Find the text input element by its name
        WebElement element = driver.findElement(By.name("q"));
        Files.copy(driver.getScreenshotAs(OutputType.FILE), new File("target/screen.png"));

        // Enter something to search for
        element.sendKeys("Cheese!");

        // Now submit the form. WebDriver will find the form for us from the element
        element.submit();

        Files.copy(driver.getScreenshotAs(OutputType.FILE), new File("target/screen2.png"));

    }
}
