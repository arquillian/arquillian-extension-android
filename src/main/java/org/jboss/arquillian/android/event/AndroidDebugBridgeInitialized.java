package org.jboss.arquillian.android.event;

import com.android.ddmlib.AndroidDebugBridge;

public class AndroidDebugBridgeInitialized {
    private AndroidDebugBridge bridge;

    public AndroidDebugBridgeInitialized(AndroidDebugBridge bridge) {
        this.bridge = bridge;
    }

    public AndroidDebugBridge getBridge() {
        return bridge;
    }

    public void setBridge(AndroidDebugBridge bridge) {
        this.bridge = bridge;
    }
}
