package org.jboss.arquillian.android.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.android.api.AndroidDeviceOutputReciever;
import org.jboss.arquillian.android.api.AndroidExecutionException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class AndroidApkInstallationTestCase {

    private final String INSTALLED_PACKAGES_CMD = "pm list packages";
    private final String CALCULATOR_APP = "com.calculator";

    @ArquillianResource
    AndroidDevice device;

    @Test
    public void installAndUninstallApk() throws AndroidExecutionException {
        device.installPackage(new File("src/test/apk/calculator.apk"), true);

        List<String> installedApps = getInstalledPackages(device);

        Assert.assertTrue("Calculator app was installed", installedApps.contains(CALCULATOR_APP));

        device.uninstallPackage(CALCULATOR_APP);

        installedApps = getInstalledPackages(device);
        Assert.assertFalse("Calculator app was uninstalled", installedApps.contains(CALCULATOR_APP));
    }

    public List<String> getInstalledPackages(AndroidDevice device) throws AndroidExecutionException {
        final List<String> output = new ArrayList<String>();
        device.executeShellCommand(INSTALLED_PACKAGES_CMD, new AndroidDeviceOutputReciever() {

            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    output.add(line.replaceFirst("package:", ""));
                }
            }

            @Override
            public boolean isCancelled() {
                // TODO Auto-generated method stub
                return false;
            }
        });
        return output;
    }

}
