package org.jboss.arquillian.android.impl;

public class AndroidEmulator {

    private Process process;

    public AndroidEmulator(Process process) {
        this.process = process;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }
}
