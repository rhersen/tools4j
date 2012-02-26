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
package org.deephacks.tools4j.config.internal.core.runtime;

import static org.deephacks.tools4j.config.internal.core.ConfigCore.lookupBeanManager;
import static org.deephacks.tools4j.config.internal.core.ConfigCore.lookupSchemaManager;
import static org.deephacks.tools4j.config.internal.core.ConfigCore.setSchema;

import java.util.List;
import java.util.Map;

import org.deephacks.tools4j.config.Config;
import org.deephacks.tools4j.config.Id;
import org.deephacks.tools4j.config.RuntimeContext;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Events;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRef;
import org.deephacks.tools4j.config.spi.BeanManager;
import org.deephacks.tools4j.config.spi.SchemaManager;
import org.deephacks.tools4j.config.spi.ValidationManager;
import org.deephacks.tools4j.support.ServiceProvider;
import org.deephacks.tools4j.support.conversion.Conversion;
import org.deephacks.tools4j.support.lookup.Lookup;
import org.deephacks.tools4j.support.reflections.ClassIntrospector;
import org.deephacks.tools4j.support.reflections.ClassIntrospector.FieldWrap;

import com.google.common.collect.Lists;

/**
 * RuntimeCoreContext is responsible for separating the admin, runtime and spi 
 * context so that no dependencies (compile nor runtime) exist between them.
 */
@ServiceProvider(service = RuntimeContext.class)
public class RuntimeCoreContext extends RuntimeContext {
    private Conversion conversion;
    private SchemaManager schemaManager;
    private BeanManager beanManager;
    private ValidationManager validationManager;

    public RuntimeCoreContext() {
    }

    @Override
    public void register(Class<?>... configurable) {
        doLookup();
        for (Class<?> clazz : configurable) {
            Schema schema = conversion.convert(clazz, Schema.class);
            schemaManager.regsiterSchema(schema);
            if (schema.getId().isSingleton()) {
                beanManager.createSingleton(getSingletonId(schema, clazz));
            }
            // ok to not have validation manager available
            if (validationManager != null) {
                validationManager.register(schema.getName(), clazz);
            }
        }
    }

    @Override
    public void unregister(Class<?>... configurable) {
        doLookup();
        for (Class<?> clazz : configurable) {
            Schema schema = conversion.convert(clazz, Schema.class);
            schemaManager.removeSchema(schema.getName());
            // ok to not have validation manager available
            if (validationManager != null) {
                validationManager.unregister(schema.getName());
            }
        }
    }

    @Override
    public <T> T singleton(Class<T> configurable) {
        doLookup();
        Schema schema = conversion.convert(configurable, Schema.class);
        BeanId singleton = getSingletonId(schema, configurable);
        Map<String, Schema> schemas = schemaManager.getSchemas();
        Bean bean = beanManager.getEager(singleton);
        bean.set(schema);
        setSingletonReferences(bean, schemas);
        return conversion.convert(bean, configurable);
    }

    @Override
    public <T> List<T> all(Class<T> clazz) {
        doLookup();
        Schema s = schemaManager.getSchema(clazz.getAnnotation(Config.class).name());
        Map<String, Schema> schemas = schemaManager.getSchemas();
        Map<BeanId, Bean> beans = beanManager.list(s.getName());
        setSchema(schemas, beans);
        for (Bean bean : beans.values()) {
            setSingletonReferences(bean, schemas);
        }
        return Lists.newArrayList(conversion.convert(beans.values(), clazz));
    }

    @Override
    public <T> T get(String id, Class<T> clazz) {
        doLookup();
        Schema s = schemaManager.getSchema(clazz.getAnnotation(Config.class).name());
        Map<String, Schema> schemas = schemaManager.getSchemas();
        BeanId beanId = BeanId.create(id, s.getName());
        Bean bean = beanManager.getEager(beanId);
        if (bean == null) {
            throw Events.CFG304_BEAN_DOESNT_EXIST(beanId);
        }
        setSchema(bean, schemas);
        setSingletonReferences(bean, schemas);
        return conversion.convert(bean, clazz);
    }

    private BeanId getSingletonId(Schema s, Class<?> configurable) {
        try {
            ClassIntrospector introspector = new ClassIntrospector(configurable);
            FieldWrap<Id> id = introspector.getFieldList(Id.class).get(0);
            String instanceId = id.getStaticValue().toString();
            return BeanId.createSingleton(instanceId, s.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException("Configurable class [" + configurable
                    + "] is not a singleton.", e);
        }
    }

    private void setSingletonReferences(Bean bean, Map<String, Schema> schemas) {
        Schema s = bean.getSchema();
        for (SchemaPropertyRef ref : s.get(SchemaPropertyRef.class)) {
            if (ref.isSingleton()) {
                Schema singletonSchema = schemas.get(ref.getSchemaName());
                Bean singleton = beanManager.getSingleton(ref.getSchemaName());
                singleton.set(singletonSchema);
                BeanId singletonId = singleton.getId();
                singletonId.setBean(singleton);
                // recursive call.
                setSingletonReferences(singleton, schemas);
                bean.setReference(ref.getName(), singletonId);
            }
        }
    }

    public void doLookup() {
        if (conversion == null) {
            conversion = Conversion.get();
            conversion.register(new ClassToSchemaConverter());
            conversion.register(new FieldToSchemaPropertyConverter());
            conversion.register(new BeanToObjectConverter());

        }
        if (beanManager == null) {
            beanManager = lookupBeanManager();
        }
        if (schemaManager == null) {
            schemaManager = lookupSchemaManager();
        }
        if (validationManager == null) {
            validationManager = Lookup.get().lookup(ValidationManager.class);
        }

    }

}
