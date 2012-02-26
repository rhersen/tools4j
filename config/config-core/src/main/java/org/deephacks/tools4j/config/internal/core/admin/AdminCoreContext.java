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
package org.deephacks.tools4j.config.internal.core.admin;

import static org.deephacks.tools4j.config.internal.core.ConfigCore.lookupBeanManager;
import static org.deephacks.tools4j.config.internal.core.ConfigCore.lookupSchemaManager;
import static org.deephacks.tools4j.config.internal.core.ConfigCore.setSchema;
import static org.deephacks.tools4j.config.internal.core.admin.SchemaValidator.validateSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deephacks.tools4j.config.admin.AdminContext;
import org.deephacks.tools4j.config.internal.core.runtime.BeanToObjectConverter;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.BeanUtils;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRef;
import org.deephacks.tools4j.config.spi.BeanManager;
import org.deephacks.tools4j.config.spi.SchemaManager;
import org.deephacks.tools4j.config.spi.ValidationManager;
import org.deephacks.tools4j.support.ServiceProvider;
import org.deephacks.tools4j.support.conversion.Conversion;
import org.deephacks.tools4j.support.lookup.Lookup;

import com.google.common.base.Preconditions;

/**
 * AdminCoreContext is responsible for separating the admin and runtime 
 * context so that no dependencies (compile nor runtime) exist between them.
 *   
 */
@ServiceProvider(service = AdminContext.class)
public class AdminCoreContext extends AdminContext {
    private BeanManager beanManager;
    private SchemaManager schemaManager;
    private ValidationManager validationManager;
    private Conversion conversion;

    public AdminCoreContext() {

    }

    @Override
    public List<Bean> list(String schemaName) {
        Preconditions.checkNotNull(schemaName);
        doLookup();
        Map<BeanId, Bean> beans = beanManager.list(schemaName);
        setSchema(schemaManager.getSchemas(), beans);
        return new ArrayList<Bean>(beans.values());
    }

    @Override
    public List<Bean> list(String schemaName, Collection<String> instanceIds) {
        Preconditions.checkNotNull(schemaName);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return new ArrayList<Bean>();
        }
        doLookup();
        Map<BeanId, Bean> beans = beanManager.list(schemaName);
        Map<BeanId, Bean> result = new HashMap<BeanId, Bean>();
        for (String instanceId : instanceIds) {
            Bean b = beans.get(BeanId.create(instanceId, schemaName));
            result.put(b.getId(), b);
        }
        Map<String, Schema> schemas = schemaManager.getSchemas();
        setSchema(schemas, result);
        return new ArrayList<Bean>(result.values());
    }

    @Override
    public Bean get(BeanId beanId) {
        Preconditions.checkNotNull(beanId);
        doLookup();
        Bean bean = beanManager.getEager(beanId);
        Map<String, Schema> schemas = schemaManager.getSchemas();
        setSchema(schemas, bean);
        setSingletonReferences(bean, schemas);
        return bean;
    }

    @Override
    public void create(Bean bean) {
        Preconditions.checkNotNull(bean);
        doLookup();
        setSchema(schemaManager.getSchemas(), bean);
        validateSchema(bean);
        if (validationManager != null) {
            initReferences(Arrays.asList(bean));
            validationManager.validate(Arrays.asList(bean));
        }
        beanManager.create(bean);
    }

    @Override
    public void create(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        setSchema(schemaManager.getSchemas(), beans);
        validateSchema(beans);
        if (validationManager != null) {
            initReferences(beans);
            validationManager.validate(beans);
        }
        beanManager.create(beans);
    }

    @Override
    public void set(Bean bean) {
        Preconditions.checkNotNull(bean);
        doLookup();
        setSchema(schemaManager.getSchemas(), bean);
        validateSchema(bean);
        if (validationManager != null) {
            initReferences(Arrays.asList(bean));
            validateSet(bean);
        }
        beanManager.set(bean);
    }

    @Override
    public void set(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        setSchema(schemaManager.getSchemas(), beans);
        validateSchema(beans);

        if (validationManager != null) {
            initReferences(beans);
            for (Bean bean : beans) {
                validateSet(bean);
            }
        }
        beanManager.set(beans);
    }

    @Override
    public void merge(Bean bean) {
        Preconditions.checkNotNull(bean);
        doLookup();
        setSchema(schemaManager.getSchemas(), bean);
        validateSchema(bean);
        if (validationManager != null) {
            validateMerge(bean);
        }
        beanManager.merge(bean);
    }

    @Override
    public void merge(Collection<Bean> beans) {
        if (beans == null || beans.isEmpty()) {
            return;
        }
        doLookup();
        setSchema(schemaManager.getSchemas(), beans);
        validateSchema(beans);
        // ok to not have validation manager available
        if (validationManager != null) {
            for (Bean bean : beans) {
                validateMerge(bean);
            }
        }
        beanManager.merge(beans);
    }

    @Override
    public void delete(BeanId beanId) {
        Preconditions.checkNotNull(beanId);
        doLookup();
        beanManager.delete(beanId);
    }

    @Override
    public void delete(String name, Collection<String> instances) {
        Preconditions.checkNotNull(name);
        if (instances == null || instances.isEmpty()) {
            return;
        }
        doLookup();
        beanManager.delete(name, instances);
    }

    @Override
    public Map<String, Schema> getSchemas() {
        doLookup();
        Map<String, Schema> schemas = schemaManager.getSchemas();
        return schemas;
    }

    private void initReferences(Collection<Bean> beans) {
        Map<BeanId, Bean> indexed = BeanUtils.uniqueIndex(beans);
        for (Bean bean : beans) {
            for (String name : bean.getReferenceNames()) {
                List<BeanId> ids = bean.getReference(name);
                for (BeanId id : ids) {
                    Bean ref = indexed.get(id);
                    if (ref == null) {
                        ref = beanManager.getLazy(id);
                        setSchema(schemaManager.getSchemas(), ref);
                    }
                    id.setBean(ref);
                }
            }
        }
    }

    private void validateMerge(Bean mergebean) {
        Map<BeanId, Bean> beansToValidate = beanManager.getBeanToValidate(mergebean);
        setSchema(schemaManager.getSchemas(), beansToValidate);
        // since we are validating mergebean predecessors, we need to make sure
        // that they see a merged reference (not unmerged reference currently in storage)
        // before validation can proceed.
        ArrayList<Bean> mergeBeanReferences = new ArrayList<Bean>();
        ArrayList<Bean> checked = new ArrayList<Bean>();
        findReferences(mergebean.getId(), beansToValidate.values(), mergeBeanReferences, checked);
        // merge all references
        merge(mergeBeanReferences, mergebean);
        // ready to validate
        validationManager.validate(beansToValidate.values());
    }

    private void validateSet(Bean setbean) {
        Map<BeanId, Bean> beansToValidate = beanManager.getBeanToValidate(setbean);
        setSchema(schemaManager.getSchemas(), beansToValidate);
        // since we are validating setbean predecessors, we need to make sure
        // that they see a replaced/set reference (not old reference currently in storage)
        // before validation can proceed.
        ArrayList<Bean> setBeanReferences = new ArrayList<Bean>();
        ArrayList<Bean> checked = new ArrayList<Bean>();
        findReferences(setbean.getId(), beansToValidate.values(), setBeanReferences, checked);
        for (Bean ref : setBeanReferences) {
            // clearing and then merging have same 
            // effect as a 'set' operation
            ref.clear();
        }
        merge(setBeanReferences, setbean);
        validationManager.validate(beansToValidate.values());
    }

    /**
     * Does a recursive check if predecessor have a particular reference and if
     * so return those predecessor references.
     */
    private List<Bean> findReferences(BeanId reference, Collection<Bean> predecessors,
            ArrayList<Bean> matches, ArrayList<Bean> checked) {

        for (Bean predecessor : predecessors) {
            findReferences(reference, predecessor, matches, checked);
        }
        return matches;
    }

    private void findReferences(BeanId reference, Bean predecessor, ArrayList<Bean> matches,
            ArrayList<Bean> checked) {
        if (checked.contains(predecessor)) {
            return;
        }
        checked.add(predecessor);
        if (reference.equals(predecessor.getId()) && !matches.contains(predecessor)) {
            matches.add(predecessor);
        }
        for (BeanId ref : predecessor.getReferences()) {
            if (ref.getBean() == null) {
                continue;
            }
            if (matches.contains(ref.getBean())) {
                continue;
            }
            if (ref.equals(reference)) {
                matches.add(ref.getBean());
            }

            findReferences(reference, ref.getBean(), matches, checked);
        }

    }

    private void merge(List<Bean> sources, Bean mergeBean) {
        HashMap<BeanId, Bean> cache = new HashMap<BeanId, Bean>();
        for (Bean source : sources) {
            for (String name : mergeBean.getPropertyNames()) {
                List<String> values = mergeBean.getValues(name);
                if (values == null || values.size() == 0) {
                    continue;
                }
                source.setProperty(name, values);
            }
            for (String name : mergeBean.getReferenceNames()) {
                List<BeanId> refs = mergeBean.getReference(name);
                if (refs == null || refs.size() == 0) {
                    source.setReferences(name, refs);
                    continue;
                }
                for (BeanId beanId : refs) {
                    Bean bean = cache.get(beanId);
                    if (bean == null) {
                        bean = beanManager.getLazy(beanId);
                        setSchema(schemaManager.getSchemas(), bean);
                        cache.put(beanId, bean);
                    }
                    beanId.setBean(bean);
                }
                source.setReferences(name, refs);
            }

        }
    }

    /**
     * Used for setting or creating a single bean.
     */
    private void initalizeReferences(Bean bean) {
        for (String name : bean.getReferenceNames()) {
            List<BeanId> values = bean.getReference(name);
            if (values == null) {
                continue;
            }
            for (BeanId beanId : values) {
                Bean ref = beanManager.getEager(beanId);
                beanId.setBean(ref);
                setSchema(schemaManager.getSchemas(), beanId.getBean());
            }
        }
    }

    /**
     * Used for setting or creating a multiple beans. 
     * 
     * We must consider that the operation may include beans that have 
     * references betewen eachother. User provided beans are 
     * prioritized and the storage is secondary for looking up references.  
     */
    private void initalizeReferences(Collection<Bean> beans) {
        Map<BeanId, Bean> userProvided = BeanUtils.uniqueIndex(beans);
        for (Bean bean : beans) {
            for (String name : bean.getReferenceNames()) {
                List<BeanId> values = bean.getReference(name);
                if (values == null) {
                    continue;
                }
                for (BeanId beanId : values) {
                    // the does not exist in storage, but may exist in the
                    // set of beans provided by the user.
                    Bean ref = userProvided.get(beanId);
                    if (ref == null) {
                        ref = beanManager.getEager(beanId);
                    }
                    beanId.setBean(ref);
                    setSchema(schemaManager.getSchemas(), beanId.getBean());

                }
            }
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

    private void doLookup() {
        if (conversion == null) {
            conversion = Conversion.get();
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
