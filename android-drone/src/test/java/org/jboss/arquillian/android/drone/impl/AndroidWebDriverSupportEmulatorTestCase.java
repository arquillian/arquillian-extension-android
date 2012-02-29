/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import java.lang.reflect.Method;
import java.util.List;

import org.jboss.arquillian.android.api.AndroidBridge;
import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.android.spi.event.AndroidDeviceShutdown;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.context.SuiteContext;
import org.jboss.arquillian.test.spi.context.TestContext;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.arquillian.test.test.AbstractTestTestBase;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests Destroyer activation when no context was created (no @Drone) won't fail
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AndroidWebDriverSupportEmulatorTestCase extends AbstractTestTestBase {

    @Mock
    private ServiceLoader serviceLoader;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(AndroidWebDriverSupport.class);
    }

    @org.junit.Before
    public void setMocks() {
        ArquillianDescriptor desc = Descriptors.create(ArquillianDescriptor.class).extension("android")
                .property("force", "false").property("verbose", "true").property("avdName", "webdriver-emulator")
                .property("apiLevel", "9").property("emulatorBootupTimeoutInSeconds", "120").property("sdSize", "256M")
                .extension(AndroidDroneConfigurator.ANDROID_DRONE_EXTENSION_NAME)
                .property("androidServerApk", "android-server-2.6.0.apk");

        bind(ApplicationScoped.class, ServiceLoader.class, serviceLoader);
        bind(ApplicationScoped.class, ArquillianDescriptor.class, desc);
    }

    @org.junit.After
    public void disposeMocks() {
        // emulator shutdown send an event so this is called automatically
        // AndroidDebugBridge.disconnectBridge();
        // AndroidDebugBridge.terminate();
    }

    @Test
    public void webDriverServerInitialized() throws Exception {
        getManager().getContext(ClassContext.class).activate(DummyClass.class);

        Object instance = new DummyClass();
        Method testMethod = DummyClass.class.getMethod("testDummyMethod");

        getManager().getContext(TestContext.class).activate(instance);
        fire(new BeforeSuite());

        AndroidBridge adb = getManager().getContext(SuiteContext.class).getObjectStore().get(AndroidBridge.class);

        Assert.assertTrue("AndroidDebugBridge is connected", adb.isConnected());
        Assert.assertFalse("At least one device is connected to AndroidDebugBridge", adb.getDevices().isEmpty());

        Thread.sleep(10000);

        fire(new BeforeClass(DummyClass.class));
        fire(new Before(instance, testMethod));

        fire(new After(instance, testMethod));
        fire(new AfterClass(DummyClass.class));

        fire(new AfterSuite());

        assertEventFired(AndroidBridgeInitialized.class, 1);
        assertEventFired(AndroidDeviceReady.class, 1);
        assertEventFired(AndroidDeviceShutdown.class, 1);
    }

    static class DummyClass {
        public void testDummyMethod() {
        }
    }
}
