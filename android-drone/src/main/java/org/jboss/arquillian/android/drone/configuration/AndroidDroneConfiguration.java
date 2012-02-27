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
package org.jboss.arquillian.android.drone.configuration;

import java.io.File;

/**
 * Configuration for Android Drone in Arquillian
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidDroneConfiguration {

    private File androidServerApk = new File("android-server.apk");

    private File webdriverLogFile = new File("target/android-webdriver-monkey.log");

    private int webdriverPortHost = 14444;

    private int webdriverPortGuest = 8080;

    private boolean verbose = false;

    private boolean skip = false;

    /**
     * @return the verbose
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the androidServerApk
     */
    public File getAndroidServerApk() {
        return androidServerApk;
    }

    /**
     * @param androidServerApk the androidServerApk to set
     */
    public void setAndroidServerApk(File androidServerApk) {
        this.androidServerApk = androidServerApk;
    }

    /**
     * @return the webdriverLogFile
     */
    public File getWebdriverLogFile() {
        return webdriverLogFile;
    }

    /**
     * @param webdriverLogFile the webdriverLogFile to set
     */
    public void setWebdriverLogFile(File webdriverLogFile) {
        this.webdriverLogFile = webdriverLogFile;
    }

    /**
     * @return the webdriverPortHost
     */
    public int getWebdriverPortHost() {
        return webdriverPortHost;
    }

    /**
     * @param webdriverPortHost the webdriverPortHost to set
     */
    public void setWebdriverPortHost(int webdriverPortHost) {
        this.webdriverPortHost = webdriverPortHost;
    }

    /**
     * @return the webdriverPortGuest
     */
    public int getWebdriverPortGuest() {
        return webdriverPortGuest;
    }

    /**
     * @param webdriverPortGuest the webdriverPortGuest to set
     */
    public void setWebdriverPortGuest(int webdriverPortGuest) {
        this.webdriverPortGuest = webdriverPortGuest;
    }

    /**
     * @return the skip
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * @param skip the skip to set
     */
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

}
