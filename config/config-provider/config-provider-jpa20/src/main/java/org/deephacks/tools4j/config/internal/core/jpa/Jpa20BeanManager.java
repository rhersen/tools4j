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
package org.deephacks.tools4j.config.internal.core.jpa;

import static org.deephacks.tools4j.config.internal.core.jpa.ExceptionTranslator.translateDelete;
import static org.deephacks.tools4j.config.internal.core.jpa.ExceptionTranslator.translateMerge;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBean.deleteJpaBean;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBean.exists;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBean.findEager;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBean.findEagerJpaBean;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBean.findLazyJpaBean;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaBeanSingleton.isJpaBeanSingleton;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaProperty.deleteProperties;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaProperty.deleteProperty;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaRef.deleteReference;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaRef.deleteReferences;
import static org.deephacks.tools4j.config.model.BeanUtils.uniqueIndex;
import static org.deephacks.tools4j.config.model.Events.CFG301_MISSING_RUNTIME_REF;
import static org.deephacks.tools4j.config.model.Events.CFG302_CANNOT_DELETE_BEAN;
import static org.deephacks.tools4j.config.model.Events.CFG303_BEAN_ALREADY_EXIST;
import static org.deephacks.tools4j.config.model.Events.CFG304_BEAN_DOESNT_EXIST;
import static org.deephacks.tools4j.config.model.Events.CFG307_SINGELTON_REMOVAL;
import static org.deephacks.tools4j.config.model.Events.CFG308_SINGELTON_CREATION;
import static org.deephacks.tools4j.support.web.jpa.ThreadLocalEntityManager.begin;
import static org.deephacks.tools4j.support.web.jpa.ThreadLocalEntityManager.commit;
import static org.deephacks.tools4j.support.web.jpa.ThreadLocalEntityManager.getEm;
import static org.deephacks.tools4j.support.web.jpa.ThreadLocalEntityManager.rollback;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.spi.BeanManager;
import org.deephacks.tools4j.support.ServiceProvider;
import org.deephacks.tools4j.support.conversion.Conversion;
import org.deephacks.tools4j.support.event.AbortRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * JpaBeanManager is responsible for driving transactions, joining/starting and rolling them back.
 * 
 * At the moment this Bean Manager is compatible with EclipseLink+Hibernate and MySQL+Postgresql and can
 * be configured to run in any such combination.
 * 
 *  TODO: Mention container-managed vs standalone deployment. Datasource integration and JTA setups.
 */
@ServiceProvider(service = BeanManager.class)
public class Jpa20BeanManager extends BeanManager {
    private static final long serialVersionUID = -1356093069248894779L;
    private Logger log = LoggerFactory.getLogger(Jpa20BeanManager.class);
    private Conversion conversion;

    public Jpa20BeanManager() {
        conversion = Conversion.get();
        conversion.register(new JpaBeanToBeanConverter());
    }

    @Override
    public void create(Bean bean) {
        try {
            begin();
            if (isJpaBeanSingleton(bean.getId().getSchemaName())) {
                throw CFG308_SINGELTON_CREATION(bean.getId());
            }
            createJpaBean(bean);
            createJpaRefs(bean);
            commit();
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public void create(Collection<Bean> beans) {
        try {
            begin();
            for (Bean bean : beans) {
                if (isJpaBeanSingleton(bean.getId().getSchemaName())) {
                    throw CFG308_SINGELTON_CREATION(bean.getId());
                }
                createJpaBean(bean);
            }
            getEm().flush();
            for (Bean bean : beans) {
                createJpaRefs(bean);
            }
            commit();
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public void createSingleton(BeanId singleton) {
        try {
            begin();
            JpaBean jpaBean = findEagerJpaBean(singleton);
            if (jpaBean != null) {
                // return silently.
                return;
            }
            JpaBeanSingleton jpaBeanSingleton = new JpaBeanSingleton(singleton.getSchemaName());
            getEm().persist(jpaBeanSingleton);

            jpaBean = new JpaBean(new JpaBeanPk(singleton));
            createJpaBean(Bean.create(singleton));
            commit();
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public Bean getSingleton(String schemaName) throws IllegalArgumentException {
        try {
            begin();
            if (!isJpaBeanSingleton(schemaName)) {
                throw new IllegalArgumentException("Schema [" + schemaName
                        + "] is not a singleton.");
            }
            List<Bean> singleton = findEager(schemaName);
            if (singleton.isEmpty()) {
                throw new IllegalArgumentException(
                        "There is no singleton instance, which is not allowed.");
            }
            if (singleton.size() > 1) {
                throw new IllegalArgumentException(
                        "There are several singleton instances, which is not allowed.");
            }
            commit();
            return singleton.get(0);
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    private JpaBean createJpaBean(Bean bean) {
        if (exists(bean.getId())) {
            throw CFG303_BEAN_ALREADY_EXIST(bean.getId());
        }
        JpaBean jpaBean = new JpaBean(bean);
        getEm().persist(jpaBean);
        JpaProperty.markBeanWithProperty(bean);
        createJpaProperties(bean);
        // This is a by-reference call. Clean the property 
        // to avoid side-effects in client.
        JpaProperty.unmarkBeanWithProperty(bean);
        return jpaBean;
    }

    private void createJpaRefs(Bean bean) {
        for (String name : bean.getReferenceNames()) {
            List<BeanId> refs = bean.getReference(name);
            if (refs == null) {
                continue;
            }
            for (BeanId id : refs) {
                JpaBean target = findEagerJpaBean(id);
                if (target == null) {
                    throw CFG301_MISSING_RUNTIME_REF(bean.getId(), id);
                }
                getEm().persist(new JpaRef(bean.getId(), target.getId(), name));
            }
        }
    }

    private void createJpaProperties(Bean bean) {
        for (String name : bean.getPropertyNames()) {
            List<String> values = bean.getValues(name);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                getEm().persist(new JpaProperty(bean.getId(), name, value));
            }
        }
    }

    @Override
    public void delete(BeanId id) {
        try {
            begin();
            if (isJpaBeanSingleton(id.getSchemaName())) {
                throw CFG307_SINGELTON_REMOVAL(id);
            }
            deleteJpaBean(id);
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            try {
                rollback();
            } catch (NullPointerException e1) {
                /**
                 * Postgres+EclipseLink 2.2.0 have bug that fails with a NPE when trying to rolback an 
                 * exception when a constraint is violated. 
                 * 
                 * Probably idiotic thing to do but this is only for making TCK test pass until 
                 * bug have been fixed.  
                 * 
                 * TODO: https://bugs.eclipse.org/bugs/show_bug.cgi?id=289191
                 */
                throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(id));
            }
            translateDelete(Arrays.asList(id), e);
        }
    }

    @Override
    public void delete(String schemaName, Collection<String> ids) {
        BeanId beanId = null;
        try {
            begin();
            for (String id : ids) {
                beanId = BeanId.create(id, schemaName);
                if (isJpaBeanSingleton(schemaName)) {
                    throw CFG307_SINGELTON_REMOVAL(beanId);
                }
                deleteJpaBean(beanId);
            }
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            try {
                rollback();
            } catch (NullPointerException e1) {
                /**
                 * Postgres+EclipseLink 2.2.0 have bug that fails with a NPE when trying to rolback an 
                 * exception when a constraint is violated. 
                 * 
                 * Probably idiotic thing to do but this is only for making TCK test pass until 
                 * bug have been fixed.  
                 * 
                 * TODO: https://bugs.eclipse.org/bugs/show_bug.cgi?id=289191
                 */
                throw CFG302_CANNOT_DELETE_BEAN(Arrays.asList(beanId));
            }
            translateDelete(ids, schemaName, e);
        }
    }

    @Override
    public Bean getEager(BeanId id) {
        try {
            begin();
            List<Bean> beans = findEager(Sets.newHashSet(id));
            if (beans.size() == 0) {
                throw CFG304_BEAN_DOESNT_EXIST(id);
            }
            commit();
            return beans.get(0);
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public Bean getLazy(BeanId id) throws AbortRuntimeException {
        try {
            begin();
            JpaBean bean = findLazyJpaBean(id);
            if (bean == null) {
                throw CFG304_BEAN_DOESNT_EXIST(id);
            }
            commit();
            return conversion.convert(bean, Bean.class);
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    /**
     * <p>
     * Get a specific instance of a particular schema type with its basic properties initialized. 
     * </p>
     * 
     * <p>
     * The direct, but no further, successors that references this bean will also be 
     * fetched and initalized with their direct, but no further, predecessors.
     * </p>
     * Fetching 1.1.2 in the example below will also fetch its successors 1.1.2.1, 1.1.2.2, but 
     * not 1.1.2.1.1, 1.1.2.1.2. Also its predecessor 1.1 and its direct successors 1.1.1, 1.1.3, but not
     * 1.1.3.1. 
     * <pre>
     *
     * 1
     * |-- 1.1
     * |   |-- 1.1.1
     * |   |-- 1.1.2
     * |   |   |-- 1.1.2.1
     * |   |   |   |-- 1.1.2.1.1
     * |   |   |   `-- 1.1.2.1.2
     * |   |   `-- 1.1.2.2
     * |   `-- 1.1.3
     * |       `-- 1.1.3.1
     * |-- 1.2
     * |   |-- 1.2.1
     * |   |   |-- 1.2.1.1
     * |   |   `-- 1.2.1.2
     * |   `-- 1.2.1
     * `-- 1.3
     * `-- 1.4
     *  </pre>
     * @see <a href="http://en.wikipedia.org/wiki/Graph_%28mathematics%29#Directed_graph">Directed graph</a> 
     * @param id targeted bean.
     */

    @Override
    public Map<BeanId, Bean> getBeanToValidate(Bean bean) throws AbortRuntimeException {
        try {
            begin();
            Set<JpaBean> beansToValidate = JpaBean.getBeanToValidate(bean);
            commit();
            Map<BeanId, Bean> beans = new HashMap<BeanId, Bean>();
            for (Bean b : conversion.convert(beansToValidate, Bean.class)) {
                beans.put(b.getId(), b);
            }
            return beans;
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public Map<BeanId, Bean> list(String schemaName) {
        try {
            begin();
            List<Bean> beans = findEager(schemaName);
            commit();
            return uniqueIndex(beans);
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    @Override
    public void merge(Bean bean) {
        try {
            begin();
            mergeJpaBean(bean);
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            translateMerge(bean.getId(), e);
        }
    }

    @Override
    public void merge(Collection<Bean> beans) {
        try {
            begin();
            for (Bean bean : beans) {
                mergeJpaBean(bean);
            }
            commit();
        } catch (AbortRuntimeException e) {
            rollback();
            throw e;
        } catch (Throwable e) {
            rollback();
            throw e;
        }

    }

    private void mergeJpaBean(Bean bean) {
        JpaBean stored = findEagerJpaBean(bean.getId());
        if (stored == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
        }
        getEm().detach(stored);
        mergeProperties(bean, stored);
        mergeReferences(bean, stored);
    }

    private void mergeReferences(Bean bean, JpaBean stored) {
        for (String name : bean.getReferenceNames()) {
            deleteReference(stored.getId(), name);
            List<BeanId> refs = bean.getReference(name);
            if (refs == null) {
                continue;
            }
            for (BeanId beanId : refs) {
                JpaBean target = findEagerJpaBean(beanId);
                if (target == null) {
                    throw CFG301_MISSING_RUNTIME_REF(bean.getId(), beanId);
                }
                getEm().persist(new JpaRef(bean.getId(), target.getId(), name));
            }
        }
    }

    private void mergeProperties(Bean bean, JpaBean stored) {
        for (String name : bean.getPropertyNames()) {
            deleteProperty(stored.getId(), name);
            List<String> values = bean.getValues(name);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                getEm().persist(new JpaProperty(stored.getId(), name, value));
            }
        }
    }

    @Override
    public void set(Bean bean) {
        try {
            begin();
            setJpaBean(bean);
            commit();
        } catch (Throwable e) {
            rollback();
            throw e;
        }

    }

    @Override
    public void set(Collection<Bean> beans) {
        try {
            begin();
            for (Bean bean : beans) {
                setJpaBean(bean);
            }
            commit();
        } catch (Throwable e) {
            rollback();
            throw e;
        }
    }

    private void setJpaBean(Bean bean) {
        JpaBean stored = findEagerJpaBean(bean.getId());
        if (stored == null) {
            throw CFG304_BEAN_DOESNT_EXIST(bean.getId());
        }
        deleteProperties(bean.getId());
        deleteReferences(bean.getId());
        createJpaProperties(bean);
        createJpaRefs(bean);
    }

}
