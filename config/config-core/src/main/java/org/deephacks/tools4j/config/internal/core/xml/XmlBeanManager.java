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
package org.deephacks.tools4j.config.internal.core.xml;

import static org.deephacks.tools4j.config.model.Events.CFG202_XML_SCHEMA_FILE_MISSING;
import static org.deephacks.tools4j.config.model.Events.CFG301_MISSING_RUNTIME_REF;
import static org.deephacks.tools4j.config.model.Events.CFG302_CANNOT_DELETE_BEAN;
import static org.deephacks.tools4j.config.model.Events.CFG303_BEAN_ALREADY_EXIST;
import static org.deephacks.tools4j.config.model.Events.CFG304_BEAN_DOESNT_EXIST;
import static org.deephacks.tools4j.config.model.Events.CFG307_SINGELTON_REMOVAL;
import static org.deephacks.tools4j.config.model.Events.CFG308_SINGELTON_CREATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.deephacks.tools4j.config.internal.core.xml.XmlBeanAdapter.XmlBeans;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Events;
import org.deephacks.tools4j.config.spi.BeanManager;
import org.deephacks.tools4j.support.ServiceProvider;
import org.deephacks.tools4j.support.SystemProperties;
import org.deephacks.tools4j.support.event.AbortRuntimeException;

import com.google.common.io.Files;

/**
 * Bean manager storing config bean instances in XML format.
 * 
 * This class should be considered a test facility. It have not been optimized for performance
 * or robustness. 
 * 
 */
@ServiceProvider(service = BeanManager.class)
public class XmlBeanManager extends BeanManager {
    public static final String XML_BEAN_FILE_STORAGE_DIR_PROP = "config.spi.bean.xml.dir";
    public static final String XML_BEAN_FILE_NAME = "bean.xml";
    private static final SystemProperties PROP = SystemProperties.createDefault();
    private static final long serialVersionUID = -4292817727054404604L;

    @Override
    public Bean getEager(BeanId id) {
        Map<BeanId, Bean> all = readValuesAsMap();
        return getEagerly(id, all);
    }

    private Bean getEagerly(BeanId id, Map<BeanId, Bean> all) {
        Bean result = all.get(id);
        if (result == null) {
            throw CFG304_BEAN_DOESNT_EXIST(id);
        }
        // bean found, initalize references.
        for (BeanId ref : result.getReferences()) {
            if (ref.getBean() != null) {
                continue;
            }
            Bean refBean = all.get(ref);
            if (refBean == null) {
                throw CFG301_MISSING_RUNTIME_REF(result.getId(), ref);
            }
            ref.setBean(refBean);
            getEagerly(ref, all);
        }
        return result;
    }

    @Override
    public Bean getLazy(BeanId id) throws AbortRuntimeException {
        Map<BeanId, Bean> all = readValuesAsMap();
        Bean bean = all.get(id);
        if (bean == null) {
            throw CFG304_BEAN_DOESNT_EXIST(id);
        }
        for (BeanId ref : bean.getReferences()) {
            Bean refBean = all.get(ref);
            if (bean == null) {
                throw CFG301_MISSING_RUNTIME_REF(ref);
            }
            ref.setBean(refBean);
        }
        return bean;
    }

    /**
     * The direct, but no further, successors that references this bean will also be 
     * fetched and initalized with their direct, but no further, predecessors.
     */
    @Override
    public Map<BeanId, Bean> getBeanToValidate(Bean bean) throws AbortRuntimeException {
        Map<BeanId, Bean> predecessors = new HashMap<BeanId, Bean>();
        // beans read from xml storage will only have their basic properties initalized...  
        Map<BeanId, Bean> all = readValuesAsMap();
        // ... but we also need set the direct references/predecessors for beans to validate
        Map<BeanId, Bean> beansToValidate = getDirectSuccessors(bean, all);
        beansToValidate.put(bean.getId(), bean);
        for (Bean toValidate : beansToValidate.values()) {
            predecessors.putAll(getDirectPredecessors(toValidate, all));
        }

        for (Bean predecessor : predecessors.values()) {
            for (BeanId ref : predecessor.getReferences()) {
                Bean b = all.get(ref);
                if (b == null) {
                    throw CFG301_MISSING_RUNTIME_REF(predecessor.getId());
                }
                ref.setBean(b);
            }
        }
        for (Bean toValidate : beansToValidate.values()) {
            // all references of beansToValidate should now 
            // be available in predecessors.
            for (BeanId ref : toValidate.getReferences()) {
                Bean predecessor = predecessors.get(ref);
                if (predecessor == null) {
                    throw new IllegalStateException("Bug in algorithm. Reference [" + ref
                            + "] of [" + toValidate.getId()
                            + "] should be available in predecessors.");
                }
                ref.setBean(predecessor);
            }
        }
        beansToValidate.putAll(predecessors);
        return beansToValidate;
    }

    private Map<BeanId, Bean> getDirectPredecessors(Bean bean, Map<BeanId, Bean> all) {
        Map<BeanId, Bean> predecessors = new HashMap<BeanId, Bean>();
        for (BeanId ref : bean.getReferences()) {
            Bean predecessor = all.get(ref);
            if (predecessor == null) {
                throw CFG304_BEAN_DOESNT_EXIST(ref);
            }
            predecessors.put(predecessor.getId(), predecessor);
        }
        return predecessors;
    }

    private Map<BeanId, Bean> getDirectSuccessors(Bean bean, Map<BeanId, Bean> all) {
        Map<BeanId, Bean> successors = new HashMap<BeanId, Bean>();
        for (Bean b : all.values()) {
            List<BeanId> refs = b.getReferences();
            if (refs.contains(bean.getId())) {
                successors.put(b.getId(), b);
            }
        }
        return successors;
    }

    @Override
    public Bean getSingleton(String schemaName) throws IllegalArgumentException {
        Map<BeanId, Bean> all = readValuesAsMap();
        for (Bean bean : all.values()) {
            if (bean.getId().getSchemaName().equals(schemaName)) {
                if (!bean.getId().isSingleton()) {
                    throw new IllegalArgumentException("Schema [" + schemaName
                            + "] is not a singleton.");
                }
                BeanId singletonId = bean.getId();
                Bean singleton = getEagerly(singletonId, all);
                if (singleton == null) {
                    throw CFG304_BEAN_DOESNT_EXIST(singletonId);
                }
                return singleton;
            }
        }
        return null;
    }

    @Override
    public Map<BeanId, Bean> list(String name) {
        Map<BeanId, Bean> all = readValuesAsMap();
        Map<BeanId, Bean> result = new HashMap<BeanId, Bean>();
        for (Bean b : all.values()) {
            if (b.getId().getSchemaName().equals(name)) {
                Bean bean = getEagerly(b.getId(), all);
                result.put(bean.getId(), bean);
            }
        }
        return result;
    }

    @Override
    public void create(Bean bean) {
        Map<BeanId, Bean> values = readValuesAsMap();
        checkReferencesExist(bean, values);
        checkCreateSingleton(bean, values);
        checkUniquness(bean, values);
        values.put(bean.getId(), bean);
        writeValues(values);
    }

    @Override
    public void create(Collection<Bean> set) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        // first check uniquness towards storage
        for (Bean bean : set) {
            checkUniquness(bean, beans);
            checkCreateSingleton(bean, beans);
        }
        // TODO: check that provided beans are unique among themselves.

        // references may not exist in storage, but are provided 
        // as part of the transactions, so add them before validating references.
        for (Bean bean : set) {
            beans.put(bean.getId(), bean);
        }
        for (Bean bean : set) {
            checkReferencesExist(bean, beans);
        }
        writeValues(beans);

    }

    @Override
    public void createSingleton(BeanId singleton) {
        Map<BeanId, Bean> values = readValuesAsMap();
        Bean bean = Bean.create(singleton);
        try {
            checkUniquness(bean, values);
        } catch (AbortRuntimeException e) {
            // ignore and return silently.
            return;
        }
        values.put(singleton, bean);
        writeValues(values);
    }

    @Override
    public void set(Bean bean) {
        Map<BeanId, Bean> values = readValuesAsMap();
        Bean existing = values.get(bean.getId());
        if (existing == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

        }
        checkReferencesExist(bean, values);
        checkInstanceExist(bean, values);
        values.put(bean.getId(), bean);
        writeValues(values);
    }

    @Override
    public void set(Collection<Bean> set) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        // TODO: check that provided beans are unique among themselves.

        // references may not exist in storage, but are provided 
        // as part of the transactions, so add them before validating references.
        for (Bean bean : set) {
            Bean existing = beans.get(bean.getId());
            if (existing == null) {
                throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
            }
            beans.put(bean.getId(), bean);
        }
        for (Bean bean : set) {
            checkReferencesExist(bean, beans);
        }

        writeValues(beans);
    }

    @Override
    public void merge(Bean bean) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        Bean b = beans.get(bean.getId());
        if (b == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
        }
        replace(b, bean, beans);
        writeValues(beans);
    }

    @Override
    public void merge(Collection<Bean> bean) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        for (Bean replace : bean) {
            Bean target = beans.get(replace.getId());
            if (target == null) {
                throw Events.CFG304_BEAN_DOESNT_EXIST(replace.getId());
            }
            replace(target, replace, beans);
        }
        writeValues(beans);
    }

    private void replace(Bean target, Bean replace, Map<BeanId, Bean> all) {
        if (target == null) {
            // bean did not exist in storage, create it.
            target = replace;
        }
        checkReferencesExist(replace, all);
        for (String name : replace.getPropertyNames()) {
            List<String> values = replace.getValues(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setProperty(name, replace.getValues(name));
            }

        }

        for (String name : replace.getReferenceNames()) {
            List<BeanId> values = replace.getReference(name);
            if (values == null || values.size() == 0) {
                // null/empty indicates a remove/reset-to-default op
                target.remove(name);
            } else {
                target.setReferences(name, values);
            }

        }
    }

    @Override
    public void delete(BeanId id) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        checkNoReferencesExist(id, beans);
        checkDeleteSingleton(beans.get(id));
        beans.remove(id);
        writeValues(beans);
    }

    @Override
    public void delete(String schemaName, Collection<String> instanceIds) {
        Map<BeanId, Bean> beans = readValuesAsMap();
        for (String instance : instanceIds) {
            checkDeleteSingleton(beans.get(BeanId.create(instance, schemaName)));
            checkNoReferencesExist(BeanId.create(instance, schemaName), beans);
            beans.remove(BeanId.create(instance, schemaName));
        }
        writeValues(beans);
    }

    private List<Bean> readValues() {
        String dirValue = PROP.get(XML_BEAN_FILE_STORAGE_DIR_PROP);
        if (dirValue == null || "".equals(dirValue)) {
            dirValue = System.getProperty("java.io.tmpdir");
        }
        File file = new File(new File(dirValue), XML_BEAN_FILE_NAME);
        try {
            if (!file.exists()) {
                Files.write("<bean-xml></bean-xml>", file, Charset.defaultCharset());
            }
            FileInputStream in = new FileInputStream(file);
            JAXBContext context = JAXBContext.newInstance(XmlBeans.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            XmlBeans beans = (XmlBeans) unmarshaller.unmarshal(in);
            return beans.getBeans();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw CFG202_XML_SCHEMA_FILE_MISSING(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<BeanId, Bean> readValuesAsMap() {
        List<Bean> beans = readValues();
        Map<BeanId, Bean> map = new HashMap<BeanId, Bean>();
        for (Bean bean : beans) {
            map.put(bean.getId(), bean);
        }
        return map;

    }

    private void writeValues(Map<BeanId, Bean> map) {
        writeValues(new ArrayList<Bean>(map.values()));
    }

    private void writeValues(List<Bean> beans) {
        String dirValue = PROP.get(XML_BEAN_FILE_STORAGE_DIR_PROP);
        if (dirValue == null || "".equals(dirValue)) {
            dirValue = System.getProperty("java.io.tmpdir");
        }
        File dir = new File(dirValue);
        if (!dir.exists()) {
            try {
                dir.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        File file = new File(dir, XML_BEAN_FILE_NAME);
        PrintWriter pw = null;
        try {
            XmlBeans xmlbeans = new XmlBeans(beans);
            pw = new PrintWriter(file, "UTF-8");
            JAXBContext context = JAXBContext.newInstance(XmlBeans.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(xmlbeans, pw);
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
        }

    }

    private static void checkNoReferencesExist(BeanId deleted, Map<BeanId, Bean> storage) {
        Collection<BeanId> hasReferences = new ArrayList<BeanId>();
        for (Bean b : storage.values()) {
            if (hasReferences(b, deleted)) {
                hasReferences.add(b.getId());
            }
        }
        if (hasReferences.size() > 0) {
            throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(deleted));
        }
    }

    private static void checkReferencesExist(final Bean bean, final Map<BeanId, Bean> storage) {

        ArrayList<BeanId> allRefs = new ArrayList<BeanId>();
        for (String name : bean.getReferenceNames()) {
            if (bean.getReference(name) == null) {
                // the reference is about to be removed.
                continue;
            }
            for (BeanId beanId : bean.getReference(name)) {
                allRefs.add(beanId);
            }
        }

        Collection<BeanId> missingReferences = new ArrayList<BeanId>();

        for (BeanId beanId : allRefs) {
            if (beanId.getInstanceId() == null) {
                continue;
            }
            Bean b = storage.get(beanId);
            if (b == null) {
                missingReferences.add(beanId);
            }
        }
        if (missingReferences.size() > 0) {
            throw CFG301_MISSING_RUNTIME_REF(bean.getId(), missingReferences);
        }
    }

    private static void checkInstanceExist(Bean bean, Map<BeanId, Bean> storage) {
        Collection<Bean> beans = storage.values();
        for (Bean existingBean : beans) {
            if (existingBean.getId().equals(bean.getId())) {
                return;
            }
        }
        throw CFG304_BEAN_DOESNT_EXIST(bean.getId());

    }

    private static void checkUniquness(Bean bean, Map<BeanId, Bean> storage) {
        Collection<Bean> beans = storage.values();

        for (Bean existing : beans) {
            if (bean.getId().equals(existing.getId())) {
                throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
            }
        }
    }

    private static void checkCreateSingleton(Bean bean, Map<BeanId, Bean> storage) {
        for (Bean b : storage.values()) {
            if (bean.getId().getSchemaName().equals(b.getId().getSchemaName())) {
                if (b.getId().isSingleton()) {
                    throw CFG308_SINGELTON_CREATION(bean.getId());
                }
            }
        }
    }

    private static void checkDeleteSingleton(Bean bean) {
        if (bean == null) {
            return;
        }
        if (bean.getId().isSingleton()) {
            throw CFG307_SINGELTON_REMOVAL(bean.getId());
        }
    }

    /**
     * Returns the a list of property names of the target bean that have 
     * references to the bean id.
     */
    private static boolean hasReferences(Bean target, BeanId reference) {
        for (String name : target.getReferenceNames()) {
            for (BeanId ref : target.getReference(name)) {
                if (ref.equals(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

}
