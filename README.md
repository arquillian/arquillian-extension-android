Arquillian Extension for the Android Platform
=============================================

This extensions allows you to bring Arquillian Drone WebDriver based testing to Android devices.
Extensions currently supports:

* Creating new Android Virtual Devices
* Reusing already existing Android Virtual Devices
* Connecting to real devices

Usage
-----

You have to do following steps, expecting your project was already set up to use Drone

0. Download Android SDK from http://developer.android.com/sdk/index.html and point ANDROID_HOME system variable to directory
where you extracted it. You should also update it via running `android` and navigating in the GUI.

1. Add Android extension to dependencies

        <dependency>
            <groupId>org.jboss.arquillian.extension</groupId>
            <artifactId>arquillian-android-depchain</artifactId>
            <version>1.0.0.Alpha1-SNAPSHOT</version>
            <type>pom</type>
            <scope>test</scope>
        </dependency>

    *Note: Make sure you have **NOT** Arquillian Drone Selenium Server on the classpath, as it will collide
    unless configured to a different port. If you cannot remove it from classpath, you should disable it in `arquillian.xml`.

        <extension qualifier="selenium-server">
            <!-- this must be skipped, we run /wd/hub on emulator -->
            <property name="skip">true</property>
        </extension>

2. Download Android Server APK to be installed to you mobile device from http://code.google.com/p/selenium/downloads/list
   Use `android-server-2.6.0.apk` for devices including Android 2.3.4, latest version for Android 3.0 and newer. 

3. Set up WebDriver in arquillian.xml

        <extension qualifier="webdriver">
            <!-- this is optional if you set -->
            <property name="implementationClass">org.openqa.selenium.android.AndroidDriver</property>
            <!-- this makes WebDriver connect hub on Android device -->
            <property name="remoteAddress">http://localhost:14444/wd/hub</property>
        </extension>

4. Set up Android in arquillian.xml

    You should be aware that following might change in the future. You've been warned! 

        <extension qualifier="android">
            <!-- this is optional, can be set via ANDROID_HOME property -->
            <property name="home">/home/kpiwko/apps/android-sdk-linux_x86</property>
            <!-- Nexus S -->
            <!-- <property name="serialId">3233E8EDB21700EC</property>-->

            <property name="apiLevel">13</property>
            <property name="avdName">SnapshotEnabled</property>
            <property name="emulatorBootupTimeoutInSeconds">180</property>
        </extension>
    
    Properties explained, required in **bold**:

    - **home** - ANDROID_HOME, can be ommited if set via ANDROID_HOME property
    - **avdName** - name of the Android Virtual Device. It will be either created or reused
    - apiLevel - (13) denotates API level, use `android list target` to get more variants
    - serialId - replaces avdName if set and availabel, represents a real device. Use `adb devics` to get the list
    - skip - (false) skip execution    
    - force - (false) force emulator recreationg
    - sdSize - (128M) SD card size for emulator 
    - emulatorBootupTimeoutInSeconds - (180) maximal time to get emulator started, use Snapshot enabled device if it takes too long
    - emulatorOptions - emulator options

    Emulators are created by default in `${basedir}/${avdName}`.

5. Set up Android Drone in arquillian.xml

    You should be aware that following might change in the future. You've been warned! 
    
        <extension qualifier="android-drone">
            <property name="androidServerApk">android-server-2.16.apk</property>
        </extension> 

    Properties explained, required in **bold**:

    - **androidServerApk** - path to the Android Server APK you've downloaded
    - skip - (false) skip execution
    - webdriverPortHost - (14444) port on Host connected with port on device
    - webdriverPortGuest - (8080) port on Guest connected with port on Host
 
    
Logging
-------

If you need to have more detailed logging, you have to provide logging.properties file as well as set it in Surefire plugin.
Logging file can look like:

	handlers= java.util.logging.ConsoleHandler

	.level= FINEST

	java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
	java.util.logging.SimpleFormatter.format = %4$s: %5$s
	java.util.logging.ConsoleHandler.level = FINEST
    
If you placed this file in `src/test/resources/logging.propreties`, Surefire execution will pick it up if following is defined:

	<build>
	    ...
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <systemPropertyVariables>
                        <java.util.logging.config.file>${project.build.testOutputDirectory}/logging.properties</java.util.logging.config.file>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
        ...
    </build>


