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
package org.deephacks.tools4j.support.lookup;

import java.lang.reflect.Field;
import java.util.Collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * MockLookup is responsible for mocking lookups.
 */
public class MockLookup extends Lookup {
    private Multimap<Class<?>, Object> instances = ArrayListMultimap.create();

    @Override
    public <T> T lookup(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Collection<T> result = (Collection<T>) instances.get(clazz);
        if (result.size() != 0) {
            return result.iterator().next();
        }
        return super.lookup(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> lookupAll(Class<T> clazz) {
        Collection<T> result = (Collection<T>) instances.get(clazz);
        if (result.size() != 0) {
            return result;
        }
        return super.lookupAll(clazz);
    }

    public static void setMockInstances(Class<?> clazz, Object... instances) {
        overrideDefault();
        MockLookup thisMockLookup = (MockLookup) Lookup.get();
        thisMockLookup.instances.clear();
        for (Object object : instances) {
            thisMockLookup.instances.put(clazz, object);
        }
    }

    public static void addMockInstances(Class<?> clazz, Object... instances) {
        overrideDefault();
        MockLookup thisMockLookup = (MockLookup) Lookup.get();
        for (Object object : instances) {
            thisMockLookup.instances.put(clazz, object);
        }
    }

    private static void overrideDefault() throws ExceptionInInitializerError {
        try {
            System.setProperty(Lookup.class.getName(), MockLookup.class.getName());
            if (Lookup.get().getClass() != MockLookup.class) {
                // Someone else initialized lookup first. Try to force our way.
                Field defaultLookup = Lookup.class.getDeclaredField("LOOKUP");
                defaultLookup.setAccessible(true);
                defaultLookup.set(null, null);
            }

            if (!Lookup.get().getClass().equals(MockLookup.class))
                throw new RuntimeException("Could not set MockLookup");
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    public static void resetToDefault() throws ExceptionInInitializerError {
        try {
            System.setProperty(Lookup.class.getName(), "");
            Field defaultLookup = Lookup.class.getDeclaredField("LOOKUP");
            defaultLookup.setAccessible(true);
            defaultLookup.set(null, null);
            if (!Lookup.get().getClass().equals(Lookup.class))
                throw new RuntimeException("Could reset to default lookup");

        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

}
