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

import org.jboss.arquillian.android.event.AndroidDeviceShutdownEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import com.android.ddmlib.IDevice;

/**
 * 
 * @author kpiwko
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class EmulatorShutdown {
    private static final Logger log = Logger.getLogger(EmulatorShutdown.class.getName());

    @Inject
    private Event<AndroidDeviceShutdownEvent> androidDeviceShutdown;

    @Inject
    private Instance<AndroidEmulator> androidEmulator;

    public void shutdownEmulator(@Observes AfterSuite event, IDevice device, ProcessExecutor executor) {

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

        androidDeviceShutdown.fire(new AndroidDeviceShutdownEvent());

    }

    /**
     * This method contains the code required to stop an emulator
     * 
     * @param device The device to stop
     */
    private void stopEmulator(IDevice device) {
        int devicePort = extractPortFromDevice(device);
        if (devicePort == -1) {
            log.info("Unable to retrieve port to stop emulator " + device.getSerialNumber());
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
    private int extractPortFromDevice(IDevice device) {
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
