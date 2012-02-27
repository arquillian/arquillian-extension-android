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
 */
package org.jboss.arquillian.android.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.android.spi.event.AndroidDeviceShutdown;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

/**
 * Brings Android Emulator down
 *
 * Observes:
 * <ul>
 * <li>{@link AfterSuite}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidDeviceShutdown}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class EmulatorShutdown {
    private static final Logger log = Logger.getLogger(EmulatorShutdown.class.getName());

    @Inject
    private Event<AndroidDeviceShutdown> androidDeviceShutdown;

    @Inject
    private Instance<AndroidEmulator> androidEmulator;

    public void shutdownEmulator(@Observes AfterSuite event, AndroidDevice device, ProcessExecutor executor) {

        AndroidEmulator emulator = androidEmulator.get();

        // we created the emulator, test shut it down
        if (emulator != null && device.isEmulator()) {
            stopEmulator(device);

            final Process p = emulator.getProcess();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<Integer> status = executorService.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    return p.waitFor();
                }
            });

            try {
                // check if the process was killed
                status.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                log.warning("Unable to close Android emulator, forcing kill. This will probably leave its state corrupt");
                p.destroy();
            } catch (ExecutionException e1) {
                log.warning("Unable to close Android emulator, forcing kill. This will probably leave its state corrupt");
                p.destroy();
            } catch (TimeoutException e1) {
                log.warning("Unable to close Android emulator, forcing kill. This will probably leave its state corrupt");
                p.destroy();
            }

            executor.removeShutdownHook(p);
        }

        androidDeviceShutdown.fire(new AndroidDeviceShutdown(device));

    }

    /**
     * This method contains the code required to stop an emulator
     *
     * @param device The device to stop
     */
    private void stopEmulator(AndroidDevice device) {
        int devicePort = extractPortFromDevice(device);
        if (devicePort == -1) {
            log.warning("Unable to retrieve port to stop emulator " + device.getSerialNumber());
        } else {
            log.info("Stopping emulator " + device.getSerialNumber());

            sendEmulatorCommand(devicePort, "avd stop");
            boolean killed = sendEmulatorCommand(devicePort, "kill");
            if (!killed) {
                log.warning("Emulator failed to stop " + device.getSerialNumber());
            } else {
                log.info("Emulator stopped successfully " + device.getSerialNumber());
            }
        }
    }

    /**
     * This method extracts a port number from the serial number of a device. It assumes that the device name is of format
     * [xxxx-nnnn] where nnnn is the port number.
     *
     * @param device The device to extract the port number from.
     * @return Returns the port number of the device
     */
    private int extractPortFromDevice(AndroidDevice device) {
        String portStr = device.getSerialNumber().substring(device.getSerialNumber().lastIndexOf("-") + 1);
        if (portStr != null && portStr.length() > 0) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // If the port is not available then return -1
        return -1;
    }

    /**
     * Sends a user command to the running emulator via its telnet interface.
     *
     * @param port The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @return Whether sending the command succeeded.
     */
    private boolean sendEmulatorCommand(
    // final Launcher launcher,
    // final PrintStream logger,
            final int port, final String command) {
        Callable<Boolean> task = new Callable<Boolean>() {
            public Boolean call() throws IOException {
                Socket socket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    socket = new Socket("127.0.0.1", port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (in.readLine() == null) {
                        return false;
                    }

                    out.write(command);
                    out.write("\r\n");
                } finally {
                    try {
                        out.close();
                        in.close();
                        socket.close();
                    } catch (Exception e) {
                        // Do nothing
                    }
                }

                return true;
            }

        };

        boolean result = false;
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(task);
            result = future.get();
        } catch (Exception e) {
            log.warning(String.format("Failed to execute emulator command '%s': %s", command, e));
        }

        return result;
    }
}
