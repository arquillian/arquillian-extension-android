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
package org.jboss.arquillian.android.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Representation of Android Device
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public interface AndroidDevice {

    /**
     * Returns serial number of device
     *
     * @return Serial number
     */
    public String getSerialNumber();

    /**
     * Returns name of Android Virtual Device
     *
     * @return Either a virtual device name or {@code null} if device is not an emulator
     */
    public String getAvdName();

    /**
     * Returns a map of properties available for the device. These properties are cached.
     *
     * @return A properties map
     */
    public Map<String, String> getProperties();

    /**
     * Returns a value of property with given name
     *
     * @param name A key
     * @return Value of property or {@code null} if not present
     * @throws IOException
     * @throws AndroidExecutionException
     */
    public String getProperty(String name) throws IOException, AndroidExecutionException;

    /**
     * Checks if the device is online
     *
     * @return {@code true} if device is online, {@code false} otherwise
     */
    public boolean isOnline();

    /**
     * Checks if the device is an emulator
     *
     * @return {@code true} if device is an emulator, {@code false} otherwise
     */
    public boolean isEmulator();

    /**
     * Returns if the device is offline
     *
     */
    public boolean isOffline();

    /**
     * Executes a shell command on the device
     *
     * @param command The command to be executed
     * @param reciever A processor to process command output
     * @throws AndroidExecutionException
     * @throws IOException
     */
    public void executeShellCommand(String command, AndroidDeviceOutputReciever reciever) throws AndroidExecutionException,
            IOException;

    /**
     * Creates a port forwarding between a local and a remote port.
     *
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     */
    public void createForward(int localPort, int remotePort) throws AndroidExecutionException, IOException;

    /**
     * Removes a port forwarding between a local and a remote port.
     *
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     */
    public void removeForward(int localPort, int remotePort) throws AndroidExecutionException, IOException;

    /**
     * Installs an Android application on device. This is a helper method that combines the syncPackageToDevice,
     * installRemotePackage, and removePackage steps
     *
     * @param packageFilePath the absolute file system path to file on local host to install
     * @param reinstall set to <code>true</code> if re-install of app should be performed
     * @param extraArgs optional extra arguments to pass. See 'adb shell pm install --help' for available options.
     * @return a {@link String} with an error code, or <code>null</code> if success.
     */
    public void installPackage(File packageFilePath, boolean reinstall, String... extraArgs) throws AndroidExecutionException;

    /**
     * Uninstalls an package from the device.
     *
     * @param packageName the Android application package name to uninstall
     */
    public void uninstallPackage(String packageName) throws AndroidExecutionException;

}
