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
import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.arquillian.android.configuration.ConfigurationMapper;
import org.jboss.arquillian.android.drone.configuration.AndroidDroneConfiguration;
import org.jboss.arquillian.android.drone.event.AndroidDroneConfigured;
import org.jboss.arquillian.android.spi.event.AndroidExtensionConfigured;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

/**
 * Creator of Arquillian Drone configuration. Note that this observer has a higher priority so it is executed before the rest of
 * Android Extension.
 *
 * Observes:
 * <ul>
 * <li>{@link AndroidExtensionConfigured}</li>
 * </ul>
 *
 * Creates:
 * <ul>
 * <li>{@link AndroidDroneConfiguration}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidDroneConfigured}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidDroneConfigurator {
    private static final Logger log = Logger.getLogger(AndroidDroneConfigurator.class.getName());

    public static final String ANDROID_DRONE_EXTENSION_NAME = "android-drone";

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidDroneConfiguration> androidDroneConfiguration;

    @Inject
    private Event<AndroidDroneConfigured> afterConfiguration;

    // we need to configure Android Drone extension before AndroidBridge is initialized
    public void configureAndroidDrone(@Observes(precedence = 10) AndroidExtensionConfigured event,
            ArquillianDescriptor descriptor) {

        AndroidDroneConfiguration configuration = new AndroidDroneConfiguration();
        boolean configured = false;

        for (ExtensionDef extensionDef : descriptor.getExtensions()) {
            if (ANDROID_DRONE_EXTENSION_NAME.equals(extensionDef.getExtensionName())) {
                ConfigurationMapper.fromArquillianDescriptor(descriptor, configuration, extensionDef.getExtensionProperties());
                configured = true;
                log.fine("Configured Android Drone extension from Arquillian configuration file");
            }
        }

        if (configured && configuration.isSkip() != true) {
            Validate.isReadable(configuration.getAndroidServerApk(), "You must provide a valid path to Android Server APK: "
                    + configuration.getAndroidServerApk());

            File webdriverLog = configuration.getWebdriverLogFile();

            Validate.notNull(webdriverLog, "You must provide a valid path to Android Webdriver Monkey log file: "
                    + configuration.getWebdriverLogFile());

            // create the log file if not present
            try {
                webdriverLog.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create Android Webdriver Monkey log file at "
                        + webdriverLog.getAbsolutePath(), e);
            }

            androidDroneConfiguration.set(configuration);
            afterConfiguration.fire(new AndroidDroneConfigured());
        }
    }
}
