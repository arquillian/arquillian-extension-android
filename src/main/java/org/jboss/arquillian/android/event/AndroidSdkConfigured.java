/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.android.event;

import org.jboss.arquillian.android.configuration.AndroidSdk;
import org.jboss.arquillian.android.configuration.AndroidSdkConfiguration;

/**
 * An event to inform other components that a drone instance was configured
 * 
 * @author <a href="kpiwko@redhat.com>Karel Piwko</a>
 * 
 */
public class AndroidSdkConfigured {
    private AndroidSdkConfiguration configuration;
    private AndroidSdk sdk;

    public AndroidSdkConfigured(AndroidSdkConfiguration configuration, AndroidSdk sdk) {
        this.configuration = configuration;
        this.sdk = sdk;
    }

    public AndroidSdkConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(AndroidSdkConfiguration configuration) {
        this.configuration = configuration;
    }

    public AndroidSdk getSdk() {
        return sdk;
    }

    public void setSdk(AndroidSdk sdk) {
        this.sdk = sdk;
    }
}
