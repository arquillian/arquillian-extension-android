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
 *
 * Copyright (C) 2009, 2010 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jboss.arquillian.android.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.api.AndroidBridge;
import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.android.api.AndroidExecutionException;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

/**
 * Implementation of Android Bridge
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
class AndroidBridgeImpl implements AndroidBridge {
    private static final Logger log = Logger.getLogger(AndroidBridgeImpl.class.getName());

    /** Android Debug Bridge initialization timeout in milliseconds. */
    private static final long ADB_TIMEOUT_MS = 60L * 1000;

    private AndroidDebugBridge delegate;

    private final File adbLocation;

    private final boolean forceNewBridge;

    private final ProcessExecutor executor;

    AndroidBridgeImpl(final File adbLocation, final boolean forceNewBridge, final ProcessExecutor executor) {
        Validate.isReadable(adbLocation, "ADB location does not represent a readable file (" + adbLocation + ")");
        this.adbLocation = adbLocation;
        this.forceNewBridge = forceNewBridge;
        this.executor = executor;
    }

    @Override
    public List<AndroidDevice> getDevices() {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");

        IDevice[] idevices = delegate.getDevices();
        List<AndroidDevice> devices = new ArrayList<AndroidDevice>(idevices.length);
        for (IDevice d : idevices) {
            devices.add(new AndroidDeviceImpl(d));
        }

        return devices;

    }

    @Override
    public void connect() throws AndroidExecutionException {
        AndroidDebugBridge.init(false);
        this.delegate = AndroidDebugBridge.createBridge(adbLocation.getAbsolutePath(), forceNewBridge);
        waitUntilConnected();
        waitForInitialDeviceList();
    }

    public void destroyAndroidDebugBridge() {
        AndroidDebugBridge.disconnectBridge();
        AndroidDebugBridge.terminate();
    }

    @Override
    public boolean isConnected() {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");

        return delegate.isConnected();
    }

    @Override
    public void disconnect() throws AndroidExecutionException {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");

        AndroidDebugBridge.disconnectBridge();
        AndroidDebugBridge.terminate();
    }

    /**
     * Run a wait loop until adb is connected or trials run out. This method seems to work more reliably then using a listener.
     *
     * @param adb
     */
    private void waitUntilConnected() {

        try {
            executor.scheduleUntilTrue(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return isConnected();
                }
            }, 500, 50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.log(Level.WARNING, "Interupted while waiting for device to be connected", e);
        } catch (ExecutionException e) {
            log.log(Level.WARNING, "Failed while waiting for device to be connected", e);
        }

    }

    /**
     * Wait for the Android Debug Bridge to return an initial device list.
     *
     * @param adb
     */
    private void waitForInitialDeviceList() {

        if (!delegate.hasInitialDeviceList()) {
            try {
                executor.scheduleUntilTrue(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return delegate.hasInitialDeviceList();
                    }
                }, ADB_TIMEOUT_MS, 1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for initial device list from Android Debug Bridge");
            } catch (ExecutionException e) {
                throw new RuntimeException("Unable to get initial list of devices connected to Android Debug Bridge", e);
            }
        }

        if (!delegate.hasInitialDeviceList()) {
            log.severe("Did not receive initial device list from the Android Debug Bridge.");
        }

    }

}
