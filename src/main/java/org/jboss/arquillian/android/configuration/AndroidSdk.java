/*
 * Copyright (C) 2009, 2010 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.android.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.AndroidConfigurationException;

/**
 * Represents an Android SDK.
 * 
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidSdk {

    private static final Logger log = Logger.getLogger(AndroidSdk.class.getName());

    /**
     * property file in each platform folder with details about platform.
     */
    private static final String SOURCE_PROPERTIES_FILENAME = "source.properties";
    /**
     * property name for platform version in sdk source.properties file.
     */
    private static final String PLATFORM_VERSION_PROPERTY = "Platform.Version";
    /**
     * property name for api level version in sdk source.properties file.
     */
    private static final String API_LEVEL_PROPERTY = "AndroidVersion.ApiLevel";

    /**
     * folder name for the sdk sub folder that contains the different platform versions.
     */
    private static final String PLATFORMS_FOLDER_NAME = "platforms";

    /**
     * folder name for the sdk sub folder that contains the platform tools.
     */
    private static final String PLATFORM_TOOLS_FOLDER_NAME = "platform-tools";

    private static final class Platform implements Comparable<Platform> {
        final String name;
        final String apiLevel;
        final String path;

        public Platform(String name, String apiLevel, String path) {
            super();
            this.name = name;
            this.apiLevel = apiLevel;
            this.path = path;
        }

        @Override
        public int compareTo(Platform o) {

            // try to do a numeric comparision
            try {
                Integer current = Integer.parseInt(apiLevel);
                Integer other = Integer.parseInt(o.apiLevel);
                return current.compareTo(other);
            } catch (NumberFormatException e) {

            }

            // failed, try to compare as strings
            return apiLevel.compareTo(o.apiLevel);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((apiLevel == null) ? 0 : apiLevel.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Platform other = (Platform) obj;
            if (apiLevel == null) {
                if (other.apiLevel != null)
                    return false;
            } else if (!apiLevel.equals(other.apiLevel))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Platform: ");
            sb.append(name).append("/API level ").append(apiLevel).append(" at ").append(path);
            return sb.toString();
        }
    }

    private final File sdkPath;
    private final Platform platform;

    private Set<Platform> availablePlatforms;

    public AndroidSdk(AndroidSdkConfiguration configuration) throws AndroidConfigurationException {

        Validate.notNull(configuration, "AndroidSdk configuration must be provided");
        Validate.isReadableDirectory(configuration.getHome(),
                "Unable to read Android SDK from directory " + configuration.getHome());
        Validate.notNullOrEmpty(configuration.getApiLevel(), "Platform or API level for Android SDK must be specified");

        this.sdkPath = new File(configuration.getHome());
        this.availablePlatforms = findAvailablePlatforms();

        platform = findPlatformByApiLevel(configuration.getApiLevel());
        if (platform == null) {

            StringBuilder sb = new StringBuilder();
            for (Platform p : availablePlatforms) {
                sb.append("API level: ").append(p.apiLevel).append("(").append(p.name).append("), ");
            }
            if (sb.length() > 0) {
                sb.delete(sb.lastIndexOf(","), sb.length());
            }

            throw new AndroidConfigurationException(
                    "Invalid SDK: API level "
                            + configuration.getApiLevel()
                            + " is not available. Available platforms are: "
                            + sb.toString()
                            + ". Use either Platform identification or API level in Arquillian configuration to identify your platform.");
        }
    }

    private Platform findPlatformByApiLevel(String apiLevel) {
        for (Platform p : availablePlatforms) {
            if (p.apiLevel.equals(apiLevel)) {
                return p;
            }
        }
        return null;
    }

    public enum Layout {
        LAYOUT_1_5, LAYOUT_2_3
    }

    public Layout getLayout() throws AndroidConfigurationException {

        Validate.isReadableDirectory(sdkPath, "Unable to read Android SDK from directory " + sdkPath);

        final File platformTools = new File(sdkPath, PLATFORM_TOOLS_FOLDER_NAME);
        if (platformTools.exists() && platformTools.isDirectory()) {
            return Layout.LAYOUT_2_3;
        }

        final File platforms = new File(sdkPath, PLATFORMS_FOLDER_NAME);
        if (platforms.exists() && platforms.isDirectory()) {
            return Layout.LAYOUT_1_5;
        }

        throw new AndroidConfigurationException("Android SDK could not be identified from path \"" + sdkPath + "\". ");
    }

    /**
     * Returns the complete path for a tool, based on this SDK.
     * 
     * @param tool which tool, for example <code>adb</code> or <code>dx.jar</code>.
     * @return the complete path as a <code>String</code>, including the tool's filename.
     */
    public String getPathForTool(String tool) {

        String[] possiblePaths = { sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool,
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool + ".exe",
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/" + tool + ".bat",
                sdkPath + "/" + PLATFORM_TOOLS_FOLDER_NAME + "/lib/" + tool, getPlatform() + "/tools/" + tool,
                getPlatform() + "/tools/" + tool + ".exe", getPlatform() + "/tools/" + tool + ".bat",
                getPlatform() + "/tools/lib/" + tool, sdkPath + "/tools/" + tool, sdkPath + "/tools/" + tool + ".exe",
                sdkPath + "/tools/" + tool + ".bat", sdkPath + "/tools/lib/" + tool };

        for (String possiblePath : possiblePaths) {
            File file = new File(possiblePath);
            if (file.exists() && !file.isDirectory()) {
                return file.getAbsolutePath();
            }
        }

        throw new RuntimeException("Could not find tool '" + tool
                + "'. Please ensure you've set it properly in Arquillian configuration");
    }

    /**
     * Get the emulator path.
     * 
     * @return
     */
    public String getEmulatorPath() {
        return getPathForTool("emulator");
    }

    /**
     * Get the android debug tool path (adb).
     * 
     * @return
     */
    public String getAdbPath() {
        return getPathForTool("adb");
    }

    /**
     * Get the android tool path
     * 
     * @return
     */
    public String getAndroidPath() {
        return getPathForTool("android");
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     * 
     * @return the complete path as a <code>String</code>, including the filename.
     * @throws AndroidConfigurationException
     */
    public String getPathForFrameworkAidl() throws AndroidConfigurationException {
        final Layout layout = getLayout();
        switch (layout) {
            case LAYOUT_1_5: // intentional fall-through
            case LAYOUT_2_3:
                return getPlatform() + "/framework.aidl";
            default:
                throw new AndroidConfigurationException("Unsupported layout \"" + layout + "\"!");
        }
    }

    public File getPlatform() {
        Validate.isReadableDirectory(sdkPath, "Unable to read Android SDK from directory " + sdkPath);

        final File platformsDirectory = new File(sdkPath, PLATFORMS_FOLDER_NAME);
        Validate.isReadableDirectory(platformsDirectory, "Unable to read Android SDK Platforms directory from directory "
                + platformsDirectory);

        if (platform == null) {
            final File[] platformDirectories = platformsDirectory.listFiles();
            Arrays.sort(platformDirectories);
            return platformDirectories[platformDirectories.length - 1];
        } else {
            final File platformDirectory = new File(platform.path);
            Validate.isReadableDirectory(platformsDirectory, "Unable to read Android SDK Platforms directory from directory "
                    + platformsDirectory);
            return platformDirectory;
        }
    }

    /**
     * Initialize the maps matching platform and api levels form the source properties files.
     * 
     * @throws AndroidConfigurationException
     * 
     */
    private Set<Platform> findAvailablePlatforms() throws AndroidConfigurationException {
        List<Platform> availablePlatforms = new ArrayList<Platform>();

        List<File> platformDirectories = getPlatformDirectories();
        for (File pDir : platformDirectories) {
            File propFile = new File(pDir, SOURCE_PROPERTIES_FILENAME);
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(propFile));
            } catch (IOException e) {
                throw new AndroidConfigurationException(
                        "Unable to read platform directory details from its configuration file " + propFile.getAbsoluteFile());
            }
            if (properties.containsKey(PLATFORM_VERSION_PROPERTY) && properties.containsKey(API_LEVEL_PROPERTY)) {
                String platform = properties.getProperty(PLATFORM_VERSION_PROPERTY);
                String apiLevel = properties.getProperty(API_LEVEL_PROPERTY);

                Platform p = new Platform(platform, apiLevel, pDir.getAbsolutePath());
                availablePlatforms.add(p);
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Found available platform: " + p.toString());
                }
            }
        }

        Collections.sort(availablePlatforms);
        return new LinkedHashSet<AndroidSdk.Platform>(availablePlatforms);
    }

    /**
     * Gets the source properties files from all locally installed platforms.
     * 
     * @return
     */
    private List<File> getPlatformDirectories() {
        List<File> sourcePropertyFiles = new ArrayList<File>();

        final File platformsDirectory = new File(sdkPath, PLATFORMS_FOLDER_NAME);
        Validate.isReadableDirectory(platformsDirectory, "Unable to read Android SDK Platforms directory from directory "
                + platformsDirectory);

        final File[] platformDirectories = platformsDirectory.listFiles();
        for (File file : platformDirectories) {
            // only looking in android- folder so only works on reasonably new sdk revisions..
            if (file.isDirectory() && file.getName().startsWith("android-")) {
                sourcePropertyFiles.add(file);
            }
        }
        return sourcePropertyFiles;
    }

}