package org.jboss.arquillian.android.enricher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.android.api.AndroidDevice;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.impl.TestInstanceEnricher;
import org.jboss.arquillian.test.impl.enricher.resource.ArquillianResourceTestEnricher;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.context.SuiteContext;
import org.jboss.arquillian.test.spi.context.TestContext;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.arquillian.test.test.AbstractTestTestBase;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
// @Ignore
public class AndroidDeviceEnricherTestCase extends AbstractTestTestBase
{
    @Mock
    private ServiceLoader serviceLoader;

    @Mock
    private AndroidDevice runningDevice;

    @Inject
    Instance<Injector> injector;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(TestInstanceEnricher.class);
        extensions.add(ArquillianResourceTestEnricher.class);
        extensions.add(AndroidDeviceResourceProvider.class);
    }

    @org.junit.Before
    public void setMocks() {
        ArquillianDescriptor desc = Descriptors.create(ArquillianDescriptor.class);

        TestEnricher enricher = new ArquillianResourceTestEnricher();
        enricher = injector.get().inject(enricher);

        ResourceProvider provider = new AndroidDeviceResourceProvider();
        provider = injector.get().inject(provider);

        Mockito.when(serviceLoader.all(TestEnricher.class)).thenReturn(
                Arrays.asList(enricher));
        Mockito.when(serviceLoader.all(ResourceProvider.class)).thenReturn(
                Arrays.asList(provider));
        Mockito.when(runningDevice.getAvdName()).thenReturn("mockedAndroid");

        bind(ApplicationScoped.class, ServiceLoader.class, serviceLoader);
        bind(ApplicationScoped.class, ArquillianDescriptor.class, desc);
        bind(SuiteScoped.class, AndroidDevice.class, runningDevice);

    }

    @Test
    public void deviceFoobarWasCreated() throws Exception {

        DummyClass instance = new DummyClass();
        getManager().getContext(ClassContext.class).activate(DummyClass.class);
        Method testMethod = DummyClass.class.getMethod("testMe");
        getManager().getContext(TestContext.class).activate(instance);
        fire(new BeforeSuite());

        AndroidDevice device = getManager().getContext(SuiteContext.class).getObjectStore().get(AndroidDevice.class);

        Assert.assertNotNull("Device was created", device);

        fire(new BeforeClass(DummyClass.class));
        // this is where enricher applies
        fire(new Before(instance, testMethod));

        Assert.assertNotNull("Device was injected", instance.getDevice());

        fire(new After(instance, testMethod));
        fire(new AfterClass(DummyClass.class));

        fire(new AfterSuite());

    }

    static class DummyClass {

        @ArquillianResource
        AndroidDevice device;

        public void testMe() {

        }

        public AndroidDevice getDevice() {
            return device;
        }

        public String getAvdName() {
            return device.getAvdName();
        }
    }

}
