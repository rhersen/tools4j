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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.deephacks.tools4j.osgi.OsgiSupport.OsgiServiceLoader;
import org.deephacks.tools4j.support.lookup.LookupProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * OsgiLookup is an extension that provide Lookup with the capability of finding 
 * OSGi services and still be unware of the OSGi runtime and its dependencies. 
 */
public class OsgiLookup extends LookupProvider {
    private static Multimap<Bundle, OsgiServiceLoader> LOADERS = ArrayListMultimap.create();

    @Override
    public <T> T lookup(Class<T> clazz) {
        Collection<Object> services = new ArrayList<Object>();
        for (Bundle bundle : LOADERS.keySet()) {
            Collection<OsgiServiceLoader> loaders = LOADERS.get(bundle);
            for (OsgiServiceLoader loader : loaders) {
                ServiceReference ref = bundle.getBundleContext().getServiceReference(
                        loader.getServiceName());
                // return first found service
                return (T) bundle.getBundleContext().getService(ref);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> lookupAll(Class<T> clazz) {
        Collection<Object> services = new ArrayList<Object>();
        for (Bundle bundle : LOADERS.keySet()) {
            Collection<OsgiServiceLoader> loaders = LOADERS.get(bundle);
            for (OsgiServiceLoader loader : loaders) {
                ServiceReference ref = bundle.getBundleContext().getServiceReference(
                        loader.getServiceName());
                services.add(bundle.getBundleContext().getService(ref));
            }
        }
        return (Collection<T>) services;
    }

    void register(Set<OsgiServiceLoader> loaders) {
        for (OsgiServiceLoader loader : loaders) {
            LOADERS.put(loader.getBundle(), loader);
        }
    }

    void unregister(OsgiServiceLoader loader) {
        LOADERS.remove(loader.getBundle(), loader);
    }

    Set<OsgiServiceLoader> getServiceLoaders() {
        return new HashSet<>(LOADERS.values());
    }

}
