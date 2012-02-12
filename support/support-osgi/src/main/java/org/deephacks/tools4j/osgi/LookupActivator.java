/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.osgi;

import static org.deephacks.tools4j.osgi.OsgiSupport.getServiceLoaders;

import java.util.HashSet;
import java.util.Set;

import org.deephacks.tools4j.osgi.OsgiSupport.OsgiServiceLoader;
import org.deephacks.tools4j.support.lookup.Lookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LookupActivator is responsible for register SPI provider bundles as OSGi services and 
 * make OsgiLookup aware of these bundles.
 * 
 * @author Kristoffer Sjögren
 */
public class LookupActivator implements BundleActivator {
    private static OsgiLookup OSGI_LOOKUP = new OsgiLookup();
    private BundleTracker bundleTracker;
    private Logger logger = LoggerFactory.getLogger(LookupActivator.class);

    public void start(BundleContext context) throws Exception {
        Lookup.get().registerLookup(OSGI_LOOKUP);
        bundleTracker = new BundleTracker(context, BundleEvent.RESOLVED,
                new LookupBundleTrackerCustomizer());
        bundleTracker.open();

    }

    public void stop(BundleContext context) throws Exception {
        bundleTracker.close();
        Lookup.get().unregisterLookup(OSGI_LOOKUP);

    }

    private static class LookupBundleTrackerCustomizer implements BundleTrackerCustomizer {
        private Logger logger = LoggerFactory.getLogger(LookupActivator.class);

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
            for (OsgiServiceLoader osgiServiceLoader : OSGI_LOOKUP.getServiceLoaders()) {
                osgiServiceLoader.unregisterProviders();
            }
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
            removedBundle(bundle, event, object);
            addingBundle(bundle, event);
        }

        @Override
        public Object addingBundle(Bundle bundle, BundleEvent event) {
            Set<OsgiServiceLoader> serviceLoaders = new HashSet<OsgiServiceLoader>();
            try {
                serviceLoaders = getServiceLoaders(bundle);
            } catch (Exception e) {
                logger.warn("Could not create Service Loader for bundle [" + bundle + "]", e);
            }

            if (serviceLoaders.isEmpty()) {
                return null;
            }
            for (OsgiServiceLoader serviceLoader : serviceLoaders) {
                serviceLoader.registerProviders();
                logger.debug("Registered [" + serviceLoader.getServiceName()
                        + "] providers {} for bundle {}.", serviceLoader.getProviders().toString(),
                        bundle);
            }

            OSGI_LOOKUP.register(serviceLoaders);
            return bundle;
        }
    }

}
