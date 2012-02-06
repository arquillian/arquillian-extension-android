package org.jboss.arquillian.android.event;

import com.android.ddmlib.IDevice;

public class AndroidDeviceReady {

    private IDevice device;

    public AndroidDeviceReady(IDevice device) {
        this.device = device;
    }

    public IDevice getDevice() {
        return device;
    }
}
