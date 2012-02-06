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
package org.jboss.arquillian.android;

import org.jboss.arquillian.android.impl.AndroidDebugBridgeConnector;
import org.jboss.arquillian.android.impl.AndroidSdkConfigurator;
import org.jboss.arquillian.android.impl.AndroidVirtualDeviceCreator;
import org.jboss.arquillian.android.impl.AndroidWebDriverSupport;
import org.jboss.arquillian.android.impl.EmulatorShutdown;
import org.jboss.arquillian.android.impl.EmulatorStartup;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 */
public class AndroidSdkExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.observer(AndroidSdkConfigurator.class);
        builder.observer(AndroidVirtualDeviceCreator.class);
        builder.observer(AndroidDebugBridgeConnector.class);
        builder.observer(EmulatorStartup.class);
        builder.observer(AndroidWebDriverSupport.class);
        builder.observer(EmulatorShutdown.class);
    }
}
