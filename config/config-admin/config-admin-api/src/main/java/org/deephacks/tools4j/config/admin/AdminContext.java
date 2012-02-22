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
package org.deephacks.tools4j.config.admin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.support.event.AbortRuntimeException;

/**
 * <p> 
 * Central interface for provisioning configuration to applications. 
 * <p>
 * Configuration is read-only from an application runtime perspective and can only be 
 * changed using this interface. Configuration changes will be reloaded automatically by 
 * applications at runtime.
 * </p>
 * <p>
 * Read operations will return {@link Bean} that will always have schema initialized, including 
 * properties and references traversed and fetched eagerly. 
 * <p>
 * Provisioning operations are relieved from having {@link Bean} schema initalized, nor 
 * must references be set recusivley, {@link BeanId} is enough to indicate references.
 * </p>
 * <p>
 * This interfaces is looked up using {@link org.deephacks.tools4j.support.lookup.Lookup}.
 * </p>
 * 
 * @author Kristoffer Sjogren
 */
public abstract class AdminContext {
    private static final String CORE_IMPL = "org.deephacks.tools4j.config.internal.core.admin.AdminCoreContext";

    protected AdminContext() {
        // only core should implement this class
        if (!getClass().getName().equals(CORE_IMPL)) {
            throw new IllegalArgumentException("Only AdminCoreContext is allowed to"
                    + "implement this interface.");
        }
    }

    /**
     * Get a single bean as identified by the id. 
     *
     * @param beanId id of the bean to be fetched.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract Bean get(BeanId beanId) throws AbortRuntimeException;

    /**
     * List all bean instances of particular schema. 
     * 
     * @param schemaName of beans to be listed.
     * @return beans matching the schema.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract List<Bean> list(String schemaName) throws AbortRuntimeException;

    /**
     * List a sepecific set of bean instances of particular schema. 
     * 
     * @param schemaName of beans to be listed.
     * @param instanceIds the ids that should be listed.
     * @return bean of matching type.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract List<Bean> list(String schemaName, Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Create a bean. 
     * 
     * @param bean to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void create(Bean bean) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#create(Bean)}.  
     * 
     * @param beans to be created.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void create(Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Overwrite/set existing bean instances with provided data.
     *
     * <p>
     * Already persisted properties associated with the instance 
     * will be removed if they are missing from the provided bean instances.
     * </p>
     * 
     * @param bean with values to be written.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void set(Bean bean) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#set(Bean)}.  
     * 
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void set(Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * <p>
     * Merges the provided bean properties with an already existing instance.
     * <p>
     * Properties not provided will remain untouched in storage, hence this method can 
     * be used to set or delete a single property. 
     * </p>
     * 
     * @param bean to be merged.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void merge(Bean bean) throws AbortRuntimeException;

    /**
     * Collection variant of {@link AdminContext#merge(Bean)}.  
     *  
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void merge(Collection<Bean> beans) throws AbortRuntimeException;

    /**
     * Delete a bean. 
     * <p>
     * Beans are only allowed to be deleted if they are not referenced by other beans, 
     * in order to enforce referential integrity.
     * </p>      
     * <p>
     * Delete operations are not cascading, which means that a bean's references 
     * are not deleted along with the bean itself.
     * </p>.
     * 
     * @param bean to be deleted
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void delete(BeanId bean) throws AbortRuntimeException;

    /**
     * This is the collection variant of {@link AdminContext#delete(Bean)}.
     * 
     * @param schemaName the name of the schema that covers all instance ids.
     * @param instanceIds instance ids to be deleted.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract void delete(String schemaName, Collection<String> instanceIds)
            throws AbortRuntimeException;

    /**
     * Get all schemas available in the system. The keys of the map is the name
     * of the schema. This method can be useful for dynamic schema discovery and
     * display.
     * 
     * @return a map of schemas indexed on schema name.
     * @throws AbortRuntimeException is thrown when the system itself cannot 
     * recover from a certain event and must therefore abort execution, see 
     * {@link org.deephacks.tools4j.config.model.Events}.
     */
    public abstract Map<String, Schema> getSchemas();

}
