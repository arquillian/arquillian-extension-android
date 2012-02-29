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
package org.jboss.arquillian.android.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.jboss.arquillian.android.api.AndroidBridge;
import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.android.api.AndroidExecutionException;
import org.jboss.arquillian.android.configuration.AndroidExtensionConfiguration;
import org.jboss.arquillian.android.configuration.AndroidSdk;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;

/**
 * Starts an emulator and either connects to an existing device or creates one
 *
 * Observes:
 * <ul>
 * <li>{@link AndroidVirtualDeviceEvent}</li>
 * </ul>
 *
 * Creates:
 * <ul>
 * <li>{@link AndroidEmulator}</li>
 * <li>{@link AndroidDevice}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidDeviceReady}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class EmulatorStartup {
    private static final Logger log = Logger.getLogger(EmulatorStartup.class.getName());

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidEmulator> androidEmulator;

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidDevice> androidDevice;

    @Inject
    private Event<AndroidDeviceReady> androidDeviceReady;

    public void createAndroidVirtualDeviceAvailable(@Observes AndroidVirtualDeviceEvent event, AndroidBridge bridge,
            AndroidExtensionConfiguration configuration, AndroidSdk sdk, ProcessExecutor executor)
            throws AndroidExecutionException, IOException {

        if (!bridge.isConnected()) {
            throw new IllegalStateException("Android debug bridge must be connected in order to spawn emulator");
        }

        String name = configuration.getAvdName();
        AndroidDevice running = null;
        for (AndroidDevice device : bridge.getDevices()) {
            if (equalsIgnoreNulls(name, device.getAvdName())) {
                running = device;
                break;
            }

        }

        if (running == null) {

            log.info("Starting avd named: " + name);

            // discover what device was added here
            DeviceDiscovery deviceDiscovery = new DeviceDiscovery();
            AndroidDebugBridge.addDeviceChangeListener(deviceDiscovery);

            // construct emulator command
            List<String> emulatorCommand = new ArrayList<String>(Arrays.asList(sdk.getEmulatorPath(), "-avd", name));
            emulatorCommand = getEmulatorOptions(emulatorCommand, configuration.getEmulatorOptions());
            // execute emulator
            Process emulator = executor.spawn(emulatorCommand.toArray(new String[0]));
            androidEmulator.set(new AndroidEmulator(emulator));

            waitUntilBootUpIsComplete(deviceDiscovery, executor, sdk, configuration.getEmulatorBootupTimeoutInSeconds());
            running = deviceDiscovery.getDiscoveredDevice();

            AndroidDebugBridge.removeDeviceChangeListener(deviceDiscovery);

        } else {
            log.info("Emulator for device " + name + " is already started, device serial is " + running.getSerialNumber()
                    + ". Emulator will not be reinitialized.");
        }

        // fire event that we have a device ready
        androidDevice.set(running);
        androidDeviceReady.fire(new AndroidDeviceReady(running));
    }

    private void waitUntilBootUpIsComplete(DeviceDiscovery deviceDiscovery, ProcessExecutor executor, AndroidSdk sdk,
            long timeout) throws AndroidExecutionException, IOException {

        log.info("Waiting " + timeout + " seconds until the device is connected");

        // wait until the device is connected to ADB
        long timeLeft = timeout * 1000;
        while (true) {
            long started = System.currentTimeMillis();
            if (deviceDiscovery.isOnline()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeLeft -= System.currentTimeMillis() - started;
            if (timeLeft <= 0) {
                throw new IllegalStateException(
                        "No emulator device was brough online during "
                                + timeout
                                + " seconds to Android Debug Bridge. Please increase the time limit in order to get emulator connected.");
            }
        }
        // device is connected to ADB
        AndroidDevice connectedDevice = deviceDiscovery.getDiscoveredDevice();
        // wait until the device is started completely
        while (true) {
            long started = System.currentTimeMillis();
            // if (connectedDevice.getProperty("ro.runtime.firstboot") != null) {
            // return;
            // }

            List<String> props = executor
                    .execute(sdk.getAdbPath(), "-s", connectedDevice.getSerialNumber(), "shell", "getprop");
            for (String line : props) {
                if (line.contains("[ro.runtime.firstboot]")) { // boot is completed
                    log.info("Device boot completed after " + (timeout * 1000 - timeLeft) + " milliseconds");
                    return;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeLeft -= System.currentTimeMillis() - started;
            if (timeLeft <= 0) {
                throw new IllegalStateException("Emulator device hasn't started properly in " + timeout
                        + " seconds. Please increase the time limit in order to get emulator booted.");
            }
        }
    }

    private List<String> getEmulatorOptions(List<String> properties, String valueString) {
        if (valueString == null) {
            return properties;
        }

        // FIXME this should accept properties encapsulated in quotes as well
        StringTokenizer tokenizer = new StringTokenizer(valueString, " ");
        while (tokenizer.hasMoreTokens()) {
            String property = tokenizer.nextToken().trim();
            properties.add(property);
        }

        return properties;
    }

    private boolean equalsIgnoreNulls(String current, String other) {
        if (current == null && other == null) {
            return false;
        } else if (current == null && other != null) {
            return false;
        } else if (current != null && other == null) {
            return false;
        }

        return current.equals(other);
    }

    private class DeviceDiscovery implements IDeviceChangeListener {

        private IDevice discoveredDevice;

        private boolean online;

        @Override
        public void deviceChanged(IDevice device, int changeMask) {
            if (discoveredDevice.equals(device) && (changeMask & IDevice.CHANGE_STATE) == 1) {
                if (device.isOnline()) {
                    this.online = true;
                }
            }
        }

        @Override
        public void deviceConnected(IDevice device) {
            this.discoveredDevice = device;
            log.fine("Discovered an emulator device connected to ADB bus");
        }

        @Override
        public void deviceDisconnected(IDevice device) {
        }

        public AndroidDevice getDiscoveredDevice() {
            return new AndroidDeviceImpl(discoveredDevice);
        }

        public boolean isOnline() {
            return online;
        }
    }
}
