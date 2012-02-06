package org.jboss.arquillian.android.configuration;

public class AndroidSdkConfiguration {

    private boolean skip;

    private boolean verbose;

    private boolean force;

    private String serialId;

    private String avdName;

    private String emulatorOptions;

    private String sdSize = "128M";

    private long emulatorStartupTimeout = 50000L;

    private String androidServerApk = "android-server.apk";

    private int webdriverPortHost = 14444;

    private int webdriverPortGuest = 8080;

    private String home = System.getenv("ANDROID_HOME");

    private String apiLevel = "13";

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

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public long getEmulatorStartupTimeout() {
        return emulatorStartupTimeout;
    }

    public void setEmulatorStartupTimeout(long emulatorStartupTimeout) {
        this.emulatorStartupTimeout = emulatorStartupTimeout;
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

    public String getAndroidServerApk() {
        return androidServerApk;
    }

    public void setAndroidServerApk(String androidServerApk) {
        this.androidServerApk = androidServerApk;
    }

    public int getWebdriverPortGuest() {
        return webdriverPortGuest;
    }

    public void setWebdriverPortGuest(int webdriverPortGuest) {
        this.webdriverPortGuest = webdriverPortGuest;
    }

    public int getWebdriverPortHost() {
        return webdriverPortHost;
    }

    public void setWebdriverPortHost(int webdriverPortHost) {
        this.webdriverPortHost = webdriverPortHost;
    }
}
