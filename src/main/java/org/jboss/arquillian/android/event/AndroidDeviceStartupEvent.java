package org.jboss.arquillian.android.event;

public class AndroidDeviceStartupEvent {
    protected final String name;

    public AndroidDeviceStartupEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
