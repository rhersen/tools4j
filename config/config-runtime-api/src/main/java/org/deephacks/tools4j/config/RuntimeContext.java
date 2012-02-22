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
package org.deephacks.tools4j.config;

import java.util.List;

/**
 * <p> 
 * Central interface for providing configuration to applications. 
 * <p>
 * Applications use this interface for registering {@link Config} classes with this runtime context 
 * in order to make them visible and available for provisioning in an administrative context. Configuration 
 * instances should not be cached, unless applications have very specific caching needs.
 * </p>
 * <p>
 * This interfaces is looked up using {@link org.deephacks.tools4j.support.lookup.Lookup}.
 * </p>
 * @author Kristoffer Sjogren
 */
public abstract class RuntimeContext {
    private static final String CORE_IMPL = "org.deephacks.tools4j.config.internal.core.runtime.RuntimeCoreContext";

    protected RuntimeContext() {
        // only core should implement this class
        if (!getClass().getName().equals(CORE_IMPL)) {
            throw new IllegalArgumentException("Only RuntimeCoreContext is allowed to"
                    + "implement this interface.");
        }
    }

    /**
     * Register a configurable class and make it visible and available for provisioning in 
     * an administrative context.
     * 
     * <p>
     * If the same class is registered multiple times, the former class is 
     * simply replaced (upgraded). 
     * <p>
     * Be cautious of registering a new version of a class that is not compatible with 
     * earlier versions of data of the same class.
     * </p>
     * 
     * @param configurable {@link Config} classes.
     */
    public abstract void register(Class<?>... configurable);

    /**
     * Remove a configurable class. This will make the schema unavailable for provisioning 
     * in an administrative context.
     * <p>
     * Data will never be removed when unregistering a configurable class.
     * </p>
     * 
     * @param configurable {@link Config} classes.
     */
    public abstract void unregister(Class<?>... configurable);

    /**
     * Read a singleton instance. This requires the configurable to have a 
     * <b>static</b> <b>final</b> {@link Id} with a default value assigned.  
     * 
     * <p>
     * Trying to read instances that are not singletons will result in an error.
     * </p>
     * 
     * @param configurable {@link Config} class.
     * @return The singleton instance of {@link Config} T class.
     */
    public abstract <T> T singleton(Class<T> configurable);

    /**
     * Fetch all instances of the same type. All references and properties
     * will be traversed and fecthed eagerly.
     *  
     * @param configurable {@link Config} class.
     * @return all instances of {@link Config} T class.
     */
    public abstract <T> List<T> all(Class<T> configurable);

    /**
     * Get a specific instance with respect to its {@link Id}. All references 
     * and properties will be traversed and fecthed eagerly.
     *  
     * @param id of the instance as specfied by {@link Id}.
     * 
     * @param configurable {@link Config} class.
     * @return an instance of {@link Config} T class.
     */
    public abstract <T> T get(String id, Class<T> configurable);

}
