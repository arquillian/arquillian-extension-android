package org.jboss.arquillian.android.impl;

import java.util.logging.Logger;

import org.jboss.arquillian.android.configuration.AndroidSdk;
import org.jboss.arquillian.android.configuration.AndroidSdkConfiguration;
import org.jboss.arquillian.android.event.AndroidDebugBridgeInitialized;
import org.jboss.arquillian.android.event.AndroidDeviceShutdownEvent;
import org.jboss.arquillian.android.event.AndroidDeviceStartupEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

import com.android.ddmlib.AndroidDebugBridge;

/**
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidDebugBridgeConnector {
    private static final Logger log = Logger.getLogger(AndroidDebugBridgeConnector.class.getName());

    /** Android Debug Bridge initialisation timeout in milliseconds. */
    private static final long ADB_TIMEOUT_MS = 60L * 1000;

    @Inject
    private Instance<AndroidSdk> androidSdk;

    @Inject
    private Instance<AndroidSdkConfiguration> androidSdkConfiguration;

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidDebugBridge> androidDebugBridge;

    @Inject
    private Event<AndroidDebugBridgeInitialized> adbInitialized;

    public void initAndroidDebugBridge(@Observes AndroidDeviceStartupEvent device) {
        AndroidSdkConfiguration configuration = androidSdkConfiguration.get();
        AndroidSdk sdk = androidSdk.get();

        Validate.notNull(configuration, "Android SDK configuration must not be null");
        Validate.notNull(sdk, "Android SDK must not be null");

        long start = System.currentTimeMillis();
        log.info("Initializing Android Debug Bridge");
        AndroidDebugBridge.init(false);
        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(sdk.getAdbPath(), configuration.isForce());
        waitUntilConnected(bridge);
        waitForInitialDeviceList(bridge);

        androidDebugBridge.set(bridge);
        long delta = System.currentTimeMillis() - start;
        log.info("Android debug Bridge was initialized in " + delta + "ms");
        adbInitialized.fire(new AndroidDebugBridgeInitialized(bridge));
    }

    public void destroyAndroidDebugBridge(@Observes AndroidDeviceShutdownEvent event) {
        AndroidDebugBridge.disconnectBridge();
        AndroidDebugBridge.terminate();
    }

    /**
     * Run a wait loop until adb is connected or trials run out. This method seems to work more reliably then using a listener.
     * 
     * @param adb
     */
    private void waitUntilConnected(AndroidDebugBridge adb) {
        int trials = 10;
        while (trials > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (adb.isConnected()) {
                break;
            }
            trials--;
        }
    }

    /**
     * Wait for the Android Debug Bridge to return an initial device list.
     * 
     * @param adb
     */
    protected void waitForInitialDeviceList(AndroidDebugBridge adb) {
        if (!adb.hasInitialDeviceList()) {
            log.fine("Waiting for initial device list from the Android Debug Bridge");
            long limitTime = System.currentTimeMillis() + ADB_TIMEOUT_MS;
            while (!adb.hasInitialDeviceList() && (System.currentTimeMillis() < limitTime)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for initial device list from Android Debug Bridge");
                }
            }
            if (!adb.hasInitialDeviceList()) {
                log.severe("Did not receive initial device list from the Android Debug Bridge.");
            }
        }
    }

}
