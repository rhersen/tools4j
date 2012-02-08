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

import static com.google.common.io.Resources.readLines;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

import com.google.common.base.Charsets;

public class OsgiSupport {

    public static Set<OsgiServiceLoader> getServiceLoaders(Bundle bundle) {
        return OsgiServiceLoader.createServiceLoaders(bundle);
    }

    public static class OsgiServiceLoader {
        private static final String META_INF_SERVICES = "/META-INF/services";
        private Class<?> serviceClass;
        private Set<Object> providers = new HashSet<Object>();
        private Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
        private Bundle bundle;

        public static Set<OsgiServiceLoader> createServiceLoaders(Bundle bundle) {
            Enumeration<URL> spiUrls = bundle.findEntries(META_INF_SERVICES, "*", true);
            Set<OsgiServiceLoader> serviceLoaders = new HashSet<OsgiServiceLoader>();
            if (spiUrls == null) {
                return serviceLoaders;
            }

            while (spiUrls.hasMoreElements()) {
                serviceLoaders.add(new OsgiServiceLoader(bundle, spiUrls.nextElement()));
            }
            return serviceLoaders;
        }

        private OsgiServiceLoader(Bundle bundle, URL url) {
            this.bundle = bundle;
            String filePath = url.getFile();
            String serviceClassname = filePath.substring(
                    filePath.lastIndexOf(File.separatorChar) + 1, filePath.length());
            try {
                for (String providerClassname : readLines(url, Charsets.UTF_8)) {
                    Class<?> providerClass = bundle.loadClass(providerClassname);
                    serviceClass = providerClass.getClassLoader().loadClass(serviceClassname);
                    providers.add(providerClass.newInstance());
                }
            } catch (Exception e) {
                // nothing more we can do really.
                throw new IllegalArgumentException("Error making bundle as ServiceLoaderBundle ["
                        + bundle + "]", e);
            }
        }

        public Bundle getBundle() {
            return bundle;
        }

        public void registerProviders() {
            for (Object provider : getProviders()) {
                ServiceRegistration registration = bundle.getBundleContext().registerService(
                        getServiceName(), provider, new Hashtable());
                registrations.add(registration);
            }
        }

        public void unregisterProviders() {
            for (ServiceRegistration reg : registrations) {
                try {
                    reg.unregister();
                } catch (IllegalStateException e) {
                    // will be thrown is provider have already been removed
                    // ignore and continue with next.
                }
            }
        }

        public Class<?> getServiceClass() {
            return serviceClass;
        }

        public String getServiceName() {
            return getServiceClass().getName();
        }

        public Set<Object> getProviders() {
            return providers;
        }

        public boolean hasProviders() {
            return providers.size() > 0;
        }
    }
}
