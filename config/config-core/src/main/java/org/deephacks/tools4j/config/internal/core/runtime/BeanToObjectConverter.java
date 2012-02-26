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

import static org.deephacks.tools4j.support.reflections.Reflections.findFields;
import static org.deephacks.tools4j.support.reflections.Reflections.forName;
import static org.deephacks.tools4j.support.reflections.Reflections.newInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.deephacks.tools4j.config.Id;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.config.model.Schema.SchemaProperty;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyList;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRef;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRefList;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRefMap;
import org.deephacks.tools4j.support.conversion.Conversion;
import org.deephacks.tools4j.support.conversion.Converter;

public class BeanToObjectConverter implements Converter<Bean, Object> {
    private Conversion conversion = Conversion.get();

    @Override
    public Object convert(Bean source, Class<? extends Object> specificType) {
        Object instance = null;
        try {
            instance = newInstance(specificType);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
        Map<String, Object> valuesToInject = new HashMap<String, Object>();
        Map<BeanId, Object> instanceCache = new HashMap<BeanId, Object>();
        return convert(source, instance, valuesToInject, instanceCache);
    }

    private Object convert(Bean source, Object instance, Map<String, Object> valuesToInject,
            Map<BeanId, Object> instanceCache) {
        instanceCache.put(source.getId(), instance);
        convertProperty(source, valuesToInject);
        convertPropertyList(source, valuesToInject);
        convertPropertyRef(source, valuesToInject, instanceCache);
        convertPropertyRefList(source, valuesToInject, instanceCache);
        convertPropertyRefMap(source, valuesToInject, instanceCache);
        Schema schema = source.getSchema();
        if (!schema.getId().isSingleton()) {
            // do not try to inject singleton id: the field is static final
            valuesToInject.put(getIdField(instance.getClass()), source.getId().getInstanceId());
        }
        inject(instance, valuesToInject);
        return instance;
    }

    private void inject(Object instance, Map<String, Object> values) {
        List<Field> fields = findFields(instance.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = values.get(field.getName());
            if (value == null) {
                continue;
            }
            try {
                field.set(instance, value);
            } catch (IllegalArgumentException e) {
                throw new UnsupportedOperationException(e);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private void convertPropertyRefMap(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRefMap prop : source.getSchema().get(SchemaPropertyRefMap.class)) {
            List<BeanId> beans = source.getReference(prop.getName());
            if (beans == null) {
                continue;
            }
            Map<Object, Object> c = newMap(forName(prop.getMapType()));
            for (BeanId beanId : beans) {
                Bean b = beanId.getBean();
                if (b != null) {
                    Object beanInstance = instanceCache.get(beanId);
                    if (beanInstance == null) {
                        try {
                            beanInstance = newInstance(forName(b.getSchema().getType()));
                        } catch (Exception e) {
                            throw new UnsupportedOperationException(e);
                        }
                        beanInstance = convert(b, beanInstance, new HashMap<String, Object>(),
                                instanceCache);
                    }
                    c.put(beanId.getInstanceId(), beanInstance);
                }
            }
            values.put(prop.getFieldName(), c);
        }
    }

    private void convertPropertyRefList(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRefList prop : source.getSchema().get(SchemaPropertyRefList.class)) {
            List<BeanId> references = source.getReference(prop.getName());
            if (references == null) {
                continue;
            }
            Collection<Object> c = newCollection(forName(prop.getCollectionType()));
            for (BeanId beanId : references) {
                Bean b = beanId.getBean();
                if (b != null) {
                    Object beanInstance = instanceCache.get(beanId);
                    if (beanInstance == null) {
                        String type = b.getSchema().getType();
                        try {
                            beanInstance = newInstance(forName(type));
                        } catch (Exception e) {
                            throw new UnsupportedOperationException(e);
                        }
                        beanInstance = convert(b, beanInstance, new HashMap<String, Object>(),
                                instanceCache);
                    }
                    c.add(beanInstance);
                }
            }
            values.put(prop.getFieldName(), c);
        }
    }

    private void convertPropertyRef(Bean source, Map<String, Object> values,
            Map<BeanId, Object> instanceCache) {
        for (SchemaPropertyRef prop : source.getSchema().get(SchemaPropertyRef.class)) {
            BeanId id = source.getFirstReference(prop.getName());
            if (id == null) {
                continue;
            }
            Bean ref = id.getBean();
            if (ref == null) {
                continue;
            }
            Schema refSchema = ref.getSchema();
            SchemaPropertyRef schemaRef = source.getSchema().get(SchemaPropertyRef.class,
                    prop.getName());
            Object beanInstance = instanceCache.get(id);
            if (beanInstance == null) {
                try {
                    beanInstance = newInstance(forName(refSchema.getType()));
                } catch (Exception e) {
                    throw new UnsupportedOperationException(e);
                }
                beanInstance = convert(ref, beanInstance, new HashMap<String, Object>(),
                        instanceCache);
            }

            values.put(schemaRef.getFieldName(), beanInstance);

        }
    }

    private void convertPropertyList(Bean source, Map<String, Object> values) {
        for (SchemaPropertyList prop : source.getSchema().get(SchemaPropertyList.class)) {
            List<String> vals = source.getValues(prop.getName());
            String field = prop.getFieldName();

            if (vals == null) {
                continue;
            }
            Collection<Object> c = newCollection(forName(prop.getCollectionType()));
            for (String val : vals) {
                Object converted = conversion.convert(val, forName(prop.getType()));
                c.add(converted);
            }

            values.put(field, c);
        }
    }

    private void convertProperty(Bean source, Map<String, Object> values) {
        for (SchemaProperty prop : source.getSchema().get(SchemaProperty.class)) {
            String value = source.getSingleValue(prop.getName());
            String field = prop.getFieldName();
            Object converted = conversion.convert(value, forName(prop.getType()));
            values.put(field, converted);
        }
    }

    private static String getIdField(Class<?> clazz) {
        for (Field field : findFields(clazz)) {
            field.setAccessible(true);
            Annotation annotation = field.getAnnotation(Id.class);
            if (annotation != null) {
                return field.getName();
            }
        }
        throw new RuntimeException("Class [" + clazz + "] does not decalare @Id.");
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> newCollection(Class<?> clazz) {
        if (!clazz.isInterface()) {
            try {
                return (Collection<Object>) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (List.class.isAssignableFrom(clazz)) {
            return new ArrayList<Object>();
        } else if (Set.class.isAssignableFrom(clazz)) {
            return new HashSet<Object>();
        }
        throw new UnsupportedOperationException("Class [" + clazz + "] is not supported.");
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> newMap(Class<?> clazz) {
        if (!clazz.isInterface()) {
            try {
                return (Map<Object, Object>) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return new HashMap<Object, Object>();
        } else if (ConcurrentMap.class.isAssignableFrom(clazz)) {
            return new ConcurrentHashMap<Object, Object>();
        }
        throw new UnsupportedOperationException("Class [" + clazz + "] is not supported.");
    }
}
