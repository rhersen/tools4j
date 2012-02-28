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
package org.deephacks.tools4j.config.internal.core;

import static org.deephacks.tools4j.config.model.Events.CFG101_SCHEMA_NOT_EXIST;

import java.util.Collection;
import java.util.Map;

import org.deephacks.tools4j.config.internal.core.xml.XmlBeanManager;
import org.deephacks.tools4j.config.internal.core.xml.XmlSchemaManager;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.config.spi.BeanManager;
import org.deephacks.tools4j.config.spi.SchemaManager;
import org.deephacks.tools4j.support.SystemProperties;
import org.deephacks.tools4j.support.lookup.Lookup;

/**
 * ConfigCore implements functionality that is common to both admin and runtime context.
 */
public class ConfigCore {
    public static void setSchema(Map<String, Schema> schemas, Collection<Bean> beans) {
        for (Bean bean : beans) {
            setSchema(bean, schemas);
        }
    }

    public static void setSchema(Bean b, Map<String, Schema> schemas) {
        Schema s = schemas.get(b.getId().getSchemaName());
        if (s == null) {
            throw CFG101_SCHEMA_NOT_EXIST(b.getId().getSchemaName());
        }
        b.set(s);
        for (BeanId id : b.getReferences()) {
            Bean ref = id.getBean();
            if (ref != null && ref.getSchema() == null) {
                setSchema(ref, schemas);
            }
        }
    }

    public static void setSchema(Map<String, Schema> schemas, Map<BeanId, Bean> beans) {
        for (Bean b : beans.values()) {
            setSchema(b, schemas);
        }
    }

    public static BeanManager lookupBeanManager() {
        Collection<BeanManager> beanManagers = Lookup.get().lookupAll(BeanManager.class);
        if (beanManagers.size() == 1) {
            return beanManagers.iterator().next();
        }
        String preferedBeanManager = SystemProperties.createDefault().get("config.beanmanager");
        if (preferedBeanManager == null || "".equals(preferedBeanManager)) {
            return new XmlBeanManager();
        }
        for (BeanManager beanManager : beanManagers) {
            if (beanManager.getClass().getName().equals(preferedBeanManager)) {
                return beanManager;
            }
        }
        return new XmlBeanManager();
    }

    public static SchemaManager lookupSchemaManager() {
        Collection<SchemaManager> schemaManagers = Lookup.get().lookupAll(SchemaManager.class);
        if (schemaManagers.size() == 1) {
            return schemaManagers.iterator().next();
        }
        String preferedSchemaManager = SystemProperties.createDefault().get("config.schemamanager");
        if (preferedSchemaManager == null || "".equals(preferedSchemaManager)) {
            return new XmlSchemaManager();
        }
        for (SchemaManager schemaManager : schemaManagers) {
            if (schemaManager.getClass().getName().equals(preferedSchemaManager)) {
                return schemaManager;
            }
        }
        return new XmlSchemaManager();
    }

}
