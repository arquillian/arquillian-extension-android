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
package org.jboss.arquillian.android.configuration;

/**
 * Configuration for Android extension in Arquillian
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidExtensionConfiguration {

    private boolean skip;

    private boolean force;

    private String serialId;

    private String avdName;

    private String emulatorOptions;

    private String sdSize = "128M";

    private long emulatorBootupTimeoutInSeconds = 120L;

    private long emulatorShutdownTimeoutInSeconds = 60L;

    private String home = System.getenv("ANDROID_HOME");

    // Android 2.3.3 is the default
    private String apiLevel = "10";

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getAvdName() {
        return avdName;
    }

    public void setAvdName(String avdName) {
        this.avdName = avdName;
    }

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    public String getEmulatorOptions() {
        return emulatorOptions;
    }

    public void setEmulatorOptions(String emulatorOptions) {
        this.emulatorOptions = emulatorOptions;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public long getEmulatorBootupTimeoutInSeconds() {
        return emulatorBootupTimeoutInSeconds;
    }

    public void setEmulatorBootupTimeoutInSeconds(long emulatorBootupTimeoutInSeconds) {
        this.emulatorBootupTimeoutInSeconds = emulatorBootupTimeoutInSeconds;
    }

    public String getApiLevel() {
        return apiLevel;
    }

    public void setApiLevel(String apiLevel) {
        this.apiLevel = apiLevel;
    }

    public String getSdSize() {
        return sdSize;
    }

    public void setSdSize(String sdSize) {
        this.sdSize = sdSize;
    }

    public long getEmulatorShutdownTimeoutInSeconds() {
        return emulatorShutdownTimeoutInSeconds;
    }

    public void setEmulatorShutdownTimeoutInSeconds(long emulatorShutdownTimeoutInSeconds) {
        this.emulatorShutdownTimeoutInSeconds = emulatorShutdownTimeoutInSeconds;
    }

}