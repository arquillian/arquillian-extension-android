/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.android.drone.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.android.api.AndroidDeviceOutputReciever;
import org.jboss.arquillian.android.api.AndroidExecutionException;
import org.jboss.arquillian.android.drone.configuration.AndroidDroneConfiguration;
import org.jboss.arquillian.android.drone.event.AndroidWebDriverHubRunning;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Configurator of Android Web Driver Hub. Installs Android WebDriver Hub on the device and sets port forwarding.
 *
 * Observes:
 * <ul>
 * <li>{@link AndroidDeviceReady}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidWebDriverHubRunning}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidWebDriverSupport {
    private static final Logger log = Logger.getLogger(AndroidWebDriverSupport.class.getName());

    private static final String START_WEBDRIVER_HUB_CMD = "am start -a android.intent.action.MAIN -n org.openqa.selenium.android.app/.MainActivity";
    private static final String TOP_CMD = "top -n 1";
    private static final String WEBDRIVER_HUB_NAME = "org.openqa.selenium.android.app";

    @Inject
    private Event<AndroidWebDriverHubRunning> androidWebDriverHubRunning;

    public void prepareWebDriverEnvironment(@Observes AndroidDeviceReady event, AndroidDroneConfiguration configuration,
            AndroidDevice device) throws AndroidExecutionException, IOException {

        log.info("Installing Android Server APK for WebDriver support");
        device.installPackage(configuration.getAndroidServerApk(), true);

        // start selenium server
        WebDriverMonkey monkey = new WebDriverMonkey(configuration.getWebdriverLogFile());
        device.executeShellCommand(START_WEBDRIVER_HUB_CMD, monkey);

        // check the process of selenium server is present
        waitUntilSeleniumStarted(device, monkey);

        // forward ports
        log.log(Level.INFO, "Creating port forwaring from {0} to {1} for WebDriver support",
                new Object[] { configuration.getWebdriverPortHost(), configuration.getWebdriverPortGuest() });
        device.createPortForwarding(configuration.getWebdriverPortHost(), configuration.getWebdriverPortGuest());

        androidWebDriverHubRunning.fire(new AndroidWebDriverHubRunning());
    }

    private void waitUntilSeleniumStarted(AndroidDevice device, WebDriverMonkey monkey) throws IOException,
            AndroidExecutionException {

        log.info("Starting Web Driver Hub on Android device");
        for (int i = 0; i < 5; i++) {
            device.executeShellCommand(TOP_CMD, monkey);
            if (monkey.isWebDriverHubStarted()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new AndroidExecutionException("Unable to start Android Server for WebDriver support.");
    }

    private static class WebDriverMonkey implements AndroidDeviceOutputReciever {
        private static final Logger log = Logger.getLogger(WebDriverMonkey.class.getName());

        private final Writer output;

        private boolean started = false;

        public WebDriverMonkey(File output) throws IOException {
            this.output = new FileWriter(output);
        }

        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                try {
                    log.log(Level.FINEST, "WebDriveMonkey outputs: ", line);
                    output.append(line).append("\n").flush();
                } catch (IOException e) {
                    // ignore output
                }
                if (line.contains(WEBDRIVER_HUB_NAME)) {
                    this.started = true;
                }
            }

        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        public boolean isWebDriverHubStarted() {
            return started;
        }

    }

}
