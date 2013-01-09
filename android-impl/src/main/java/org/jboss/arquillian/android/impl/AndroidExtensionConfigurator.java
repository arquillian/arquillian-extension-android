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
package org.jboss.arquillian.android.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.configuration.AndroidConfigurationException;
import org.jboss.arquillian.android.configuration.AndroidExtensionConfiguration;
import org.jboss.arquillian.android.configuration.AndroidSdk;
import org.jboss.arquillian.android.configuration.ConfigurationMapper;
import org.jboss.arquillian.android.spi.event.AndroidExtensionConfigured;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

/**
 * Configurator of Android extension for Arquillian.
 *
 * Observes:
 * <ul>
 * <li>{@link BeforeSuite}</li>
 * </ul>
 *
 * Creates:
 * <ul>
 * <li>{@link AndroidExtensionConfiguration}</li>
 * <li>{@link AndroidSdk}</li>
 * <li>{@link ProcessExecutor}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidExtensionConfigured}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidExtensionConfigurator {
    private static Logger log = Logger.getLogger(AndroidExtensionConfigurator.class.getName());

    public static final String ANDROID_EXTENSION_NAME = "android";

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidExtensionConfiguration> androidExtensionConfiguration;

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidSdk> androidSdk;

    @Inject
    @SuiteScoped
    private InstanceProducer<ProcessExecutor> executor;

    @Inject
    private Event<AndroidExtensionConfigured> afterConfiguration;

    public void configureAndroidSdk(@Observes BeforeSuite event, ArquillianDescriptor descriptor)
            throws AndroidConfigurationException {

        AndroidExtensionConfiguration configuration = new AndroidExtensionConfiguration();
        boolean configured = false;

        for (ExtensionDef extensionDef : descriptor.getExtensions()) {
            if (ANDROID_EXTENSION_NAME.equals(extensionDef.getExtensionName())) {
                ConfigurationMapper.fromArquillianDescriptor(descriptor, configuration, extensionDef.getExtensionProperties());
                configured = true;
                log.fine("Configured Android extension from Arquillian configuration file");
            }
        }

        if (configured && configuration.isSkip() != true) {

            Validate.isReadableDirectory(
                    configuration.getHome(),
                    "You must provide Android SDK Home. The value you've provided is not valid ("
                            + (configuration.getHome() == null ? "" : configuration.getHome())
                            + "). You can either set it via an environment variable ANDROID_HOME or via a property called \"home\" in Arquillian configuration.");

            Validate.notAllNullsOrEmpty(
                    new String[] { configuration.getAvdName(), configuration.getSerialId() },
                    "You must provide either \"avdName\" if you want to use an emulator, or \"serialId\" property if you want to use a real device.");

            if (configuration.getAvdName() != null && configuration.getSerialId() != null) {
                log.log(Level.WARNING,
                        "Both \"avdName\"({0}) and \"serialId\"({1}) properties are defined, the device specified by \"serialId\" will get priority if connected.",
                        new Object[] { configuration.getAvdName(), configuration.getSerialId() });
            }

            AndroidSdk sdk = new AndroidSdk(configuration);
            androidExtensionConfiguration.set(configuration);
            androidSdk.set(sdk);
            executor.set(new ProcessExecutor());
            afterConfiguration.fire(new AndroidExtensionConfigured());
        }
    }
}
