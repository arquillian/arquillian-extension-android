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

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.arquillian.android.AndroidConfigurationException;
import org.jboss.arquillian.android.configuration.AndroidSdk;
import org.jboss.arquillian.android.configuration.AndroidSdkConfiguration;
import org.jboss.arquillian.android.event.AndroidSdkConfigured;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

/**
 * 
 * @author <a href="kpiwko@redhat.com>Karel Piwko</a>
 * 
 */
public class AndroidSdkConfigurator {
    private static Logger log = Logger.getLogger(AndroidSdkConfigurator.class.getName());

    public static final String ANDROID_SDK_EXTENSION_NAME = "android-sdk";

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidSdkConfiguration> androidSdkConfiguration;

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidSdk> androidSdk;

    @Inject
    @SuiteScoped
    private InstanceProducer<ProcessExecutor> executor;

    @Inject
    private Event<AndroidSdkConfigured> afterConfiguration;

    public void configureAndroidSdk(@Observes BeforeSuite event, ArquillianDescriptor descriptor)
            throws AndroidConfigurationException {

        AndroidSdkConfiguration configuration = new AndroidSdkConfiguration();
        boolean configured = false;

        for (ExtensionDef extensionDef : descriptor.getExtensions()) {
            if (ANDROID_SDK_EXTENSION_NAME.equals(extensionDef.getExtensionName())) {
                ConfigurationMapper.fromArquillianDescriptor(descriptor, configuration, extensionDef.getExtensionProperties());
                configured = true;
                log.fine("Configured Android extension from Arquillian configuration file");
            }
        }

        if (configured && configuration.isSkip() != true) {

            // validate configuration
            if (configuration.getAvdName() == null && configuration.getSerialId() == null) {
                throw new AndroidConfigurationException(
                        "You must provide either \"avdName\" if you want to use an emulator, or \"serialId\" property if you want to use a real device.");
            }
            if (configuration.getAvdName() != null && configuration.getSerialId() != null) {
                log.warning("Both \"avdName\" and \"serialId\" properties are defined, the device specified by \"serialId\" will get priority");
            }

            AndroidSdk sdk = new AndroidSdk(configuration);
            androidSdkConfiguration.set(configuration);
            androidSdk.set(sdk);
            executor.set(new ProcessExecutor(configuration));
            afterConfiguration.fire(new AndroidSdkConfigured(configuration, sdk));
        }
    }
}

class ConfigurationMapper {

    /**
     * Maps Android configuration using Arquillian Descriptor file
     * 
     * @param descriptor Arquillian Descriptor
     * @param configuration Configuration object
     * @param properties A map of name-value pairs
     * @return Configured configuration
     */
    public static AndroidSdkConfiguration fromArquillianDescriptor(ArquillianDescriptor descriptor,
            AndroidSdkConfiguration configuration, Map<String, String> properties) {
        Validate.notNull(descriptor, "Descriptor must not be null");
        Validate.notNull(configuration, "Configuration must not be null");

        List<Field> fields = SecurityActions.getAccessableFields(AndroidSdkConfiguration.class);
        for (Field f : fields) {
            if (properties.containsKey(f.getName())) {
                try {
                    f.set(configuration, convert(box(f.getType()), properties.get(f.getName())));
                } catch (Exception e) {
                    throw new RuntimeException("Could not map Android configuration from Arquillian Descriptor", e);
                }
            }
        }
        return configuration;

    }

    /**
     * A helper boxing method. Returns boxed class for a primitive class
     * 
     * @param primitive A primitive class
     * @return Boxed class if class was primitive, unchanged class in other cases
     */
    private static Class<?> box(Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            return primitive;
        }

        if (int.class.equals(primitive)) {
            return Integer.class;
        } else if (long.class.equals(primitive)) {
            return Long.class;
        } else if (float.class.equals(primitive)) {
            return Float.class;
        } else if (double.class.equals(primitive)) {
            return Double.class;
        } else if (short.class.equals(primitive)) {
            return Short.class;
        } else if (boolean.class.equals(primitive)) {
            return Boolean.class;
        } else if (char.class.equals(primitive)) {
            return Character.class;
        } else if (byte.class.equals(primitive)) {
            return Byte.class;
        }

        throw new IllegalArgumentException("Unknown primitive type " + primitive);
    }

    /**
     * A helper converting method.
     * 
     * Converts string to a class of given type
     * 
     * @param <T> Type of returned value
     * @param clazz Type of desired value
     * @param value String value to be converted
     * @return Value converted to a appropriate type
     */
    private static <T> T convert(Class<T> clazz, String value) {
        if (String.class.equals(clazz)) {
            return clazz.cast(value);
        } else if (Integer.class.equals(clazz)) {
            return clazz.cast(Integer.valueOf(value));
        } else if (Double.class.equals(clazz)) {
            return clazz.cast(Double.valueOf(value));
        } else if (Long.class.equals(clazz)) {
            return clazz.cast(Long.valueOf(value));
        } else if (Boolean.class.equals(clazz)) {
            return clazz.cast(Boolean.valueOf(value));
        } else if (URL.class.equals(clazz)) {
            try {
                return clazz.cast(new URI(value).toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Unable to convert value " + value + " to URL", e);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to convert value " + value + " to URL", e);
            }
        } else if (URI.class.equals(clazz)) {
            try {
                return clazz.cast(new URI(value));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to convert value " + value + " to URL", e);
            }
        }

        throw new IllegalArgumentException("Unable to convert value " + value + "to a class: " + clazz.getName());
    }
}
