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

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaProperty.deleteProperties;
import static org.deephacks.tools4j.config.internal.core.jpa.JpaRef.deleteReferences;
import static org.deephacks.tools4j.config.model.Events.CFG304_BEAN_DOESNT_EXIST;
import static org.deephacks.tools4j.support.web.jpa.ThreadLocalEntityManager.getEm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;

import com.google.common.base.Objects;

/**
 * JpaBean is a jpa entity that represent a Bean.
 * 
 * @author Kristoffer Sjogren
 */
@Entity
@Table(name = "CONFIG_BEAN")
@NamedQueries({
        @NamedQuery(name = JpaBean.FIND_BEAN_FROM_BEANID_NAME,
                query = JpaBean.FIND_BEAN_FROM_BEANID),
        @NamedQuery(name = JpaBean.FIND_BEANS_FROM_SCHEMA_NAME,
                query = JpaBean.FIND_BEANS_FROM_SCHEMA),
        @NamedQuery(name = JpaBean.DELETE_BEAN_USING_BEANID_NAME,
                query = JpaBean.DELETE_BEAN_USING_BEANID) })
public class JpaBean implements Serializable {
    private static final long serialVersionUID = -4097243985344046349L;
    @EmbeddedId
    private JpaBeanPk pk;

    protected static final String FIND_BEAN_FROM_BEANID = "SELECT DISTINCT e FROM JpaBean e WHERE e.pk.id = ?1 AND e.pk.schemaName= ?2";
    protected static final String FIND_BEAN_FROM_BEANID_NAME = "FIND_BEAN_FROM_BEANID_NAME";

    public static JpaBean findEagerJpaBean(BeanId id) {
        JpaBean result = getJpaBeanAndProperties(id);
        if (result == null) {
            // bean does not exist
            return null;
        }
        Map<BeanId, JpaBean> refs = new HashMap<BeanId, JpaBean>();
        // collect recursivley
        collectRefs(result, refs);
        // now ready to associate references with correct JpaBean
        for (JpaBean jpaBean : refs.values()) {
            for (JpaRef ref : jpaBean.getReferences()) {

                JpaBean bean = refs.get(ref.getTarget());
                ref.setTargetBean(bean);
            }
        }
        return result;
    }

    private static void collectRefs(JpaBean jpaBean, Map<BeanId, JpaBean> result) {
        if (result.containsKey(jpaBean.getId())) {
            // break redundant selects and circular references
            return;
        }
        result.put(jpaBean.getId(), jpaBean);
        for (JpaRef jpaRef : JpaRef.findReferences(jpaBean.getId())) {

            collectRefs(getJpaBeanAndProperties(jpaRef.getTarget()), result);
            jpaBean.addReference(jpaRef);
        }
    }

    public static JpaBean findLazyJpaBean(BeanId id) {
        JpaBean bean = getJpaBeanAndProperties(id);
        if (bean == null) {
            return null;
        }
        List<JpaRef> refs = JpaRef.findReferences(bean.getId());
        for (JpaRef jpaRef : refs) {
            JpaBean target = getJpaBeanAndProperties(jpaRef.getTarget());
            jpaRef.setTargetBean(target);
        }
        bean.references.addAll(refs);
        return bean;
    }

    protected static final String FIND_BEANS_FROM_SCHEMA = "SELECT DISTINCT e FROM JpaBean e WHERE e.pk.schemaName= ?1";
    protected static final String FIND_BEANS_FROM_SCHEMA_NAME = "FIND_BEANS_FROM_SCHEMA_NAME";

    @SuppressWarnings("unchecked")
    public static List<JpaBean> findJpaBeans(String schemaName) {
        Query query = getEm().createNamedQuery(FIND_BEANS_FROM_SCHEMA_NAME);
        query.setParameter(1, schemaName);
        List<JpaBean> beans = (List<JpaBean>) query.getResultList();
        List<JpaBean> result = new ArrayList<JpaBean>();
        for (JpaBean bean : beans) {
            result.add(findEagerJpaBean(bean.getId()));
        }
        return result;
    }

    protected static final String FIND_BEANS_DEFAULT = "SELECT e FROM JpaBean e WHERE (e.pk.id IN :ids AND e.pk.schemaName IN :schemaNames)";
    protected static final String FIND_BEANS_HIBERNATE = "SELECT e FROM JpaBean e WHERE (e.pk.id IN (:ids) AND e.pk.schemaName IN (:schemaNames))";

    @SuppressWarnings("unchecked")
    public static List<JpaBean> findJpaBeans(List<BeanId> beanIds) {
        String queryString = FIND_BEANS_DEFAULT;
        if (getEm().getClass().getName().contains("hibernate")) {
            /**
             * Hibernate and EclipseLink treat IN queries differently. 
             * EclipseLink mandates NO brackets, while hibernate mandates WITH brackets.
             * In order to support both, this ugly hack is needed.
             */
            queryString = FIND_BEANS_HIBERNATE;
        }
        Query query = getEm().createQuery(queryString);
        Collection<String> ids = new ArrayList<String>();
        Collection<String> schemaNames = new ArrayList<String>();
        for (BeanId id : beanIds) {
            ids.add(id.getInstanceId());
            schemaNames.add(id.getSchemaName());
        }
        query.setParameter("ids", ids);
        query.setParameter("schemaNames", schemaNames);
        List<JpaBean> beans = (List<JpaBean>) query.getResultList();
        List<JpaProperty> props = JpaProperty.findProperties(beanIds);
        for (JpaProperty jpaProperty : props) {
            for (JpaBean jpaBean : beans) {
                if (jpaBean.getId().equals(jpaProperty.getId())) {
                    jpaBean.addProperty(jpaProperty);
                }
            }
        }
        return beans;
    }

    /**
     * Will return the target bean and its direct predecessors for validation
     */
    @SuppressWarnings("unchecked")
    public static Set<JpaBean> getBeanToValidate(Bean targetBean) {
        Map<BeanId, JpaBean> beansToValidate = new HashMap<BeanId, JpaBean>();
        List<JpaRef> targetPredecessors = JpaRef.getDirectPredecessors(targetBean.getId());
        // predecessors of target
        for (JpaRef targetPredecessorRef : targetPredecessors) {
            JpaBean targetPredecessor = beansToValidate.get(targetPredecessorRef.getSource());
            if (targetPredecessor == null) {
                targetPredecessor = getJpaBeanAndProperties(targetPredecessorRef.getSource());
                initReferences(targetPredecessor, 2);
            }
            beansToValidate.put(targetPredecessorRef.getSource(), targetPredecessor);
        }
        JpaBean target = beansToValidate.get(targetBean.getId());
        if (target == null) {
            target = getJpaBeanAndProperties(targetBean.getId());
            if (target == null) {
                throw CFG304_BEAN_DOESNT_EXIST(targetBean.getId());
            }
            initReferences(target, 2);
        }
        beansToValidate.put(targetBean.getId(), target);
        return new HashSet<JpaBean>(beansToValidate.values());
    }

    private static void initReferences(JpaBean bean, int successors) {
        if (--successors < 0) {
            return;
        }
        List<JpaRef> refs = JpaRef.findReferences(bean.getId());
        for (JpaRef ref : refs) {
            JpaBean refBean = getJpaBeanAndProperties(ref.getTarget());
            if (refBean == null) {
                throw CFG304_BEAN_DOESNT_EXIST(ref.getTarget());
            }
            ref.setTargetBean(refBean);
            bean.references.add(ref);
            initReferences(refBean, successors);
        }
    }

    private static JpaBean getJpaBeanAndProperties(BeanId id) {
        Query query = getEm().createNamedQuery(FIND_BEAN_FROM_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        JpaBean bean;
        try {
            bean = (JpaBean) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        List<JpaProperty> props = JpaProperty.findProperties(bean.getId());
        bean.properties.addAll(props);
        return bean;
    }

    protected static final String DELETE_BEAN_USING_BEANID = "DELETE FROM JpaBean e WHERE e.pk.id = ?1 AND e.pk.schemaName= ?2";
    protected static final String DELETE_BEAN_USING_BEANID_NAME = "DELETE_BEAN_USING_BEANID_NAME";

    public static void deleteJpaBean(BeanId id) {
        deleteProperties(id);
        deleteReferences(id);
        Query query = getEm().createNamedQuery(DELETE_BEAN_USING_BEANID_NAME);
        query.setParameter(1, id.getInstanceId());
        query.setParameter(2, id.getSchemaName());
        query.executeUpdate();
    }

    @Transient
    private Set<JpaRef> references = new HashSet<JpaRef>();
    @Transient
    private Set<JpaProperty> properties = new HashSet<JpaProperty>();

    public JpaBean() {

    }

    JpaBean(Bean b) {
        this.pk = new JpaBeanPk(b.getId());
    }

    JpaBean(JpaBeanPk pk) {
        this.pk = pk;
    }

    public JpaBeanPk getPk() {
        return pk;
    }

    public BeanId getId() {
        return BeanId.create(pk.id, pk.schemaName);
    }

    public Set<JpaRef> getReferences() {
        return references;
    }

    public void addReference(JpaRef ref) {
        references.add(ref);
    }

    public Set<JpaProperty> getProperties() {
        return properties;
    }

    public void addProperty(String name, List<String> values) {
        for (String value : values) {
            properties.add(new JpaProperty(getId(), name, value));
        }
    }

    public void addProperty(JpaProperty prop) {
        properties.add(prop);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JpaBean)) {
            return false;
        }
        JpaBean o = (JpaBean) obj;
        return equal(pk, o.pk);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pk);
    }

    @Override
    public String toString() {
        return toStringHelper(JpaBean.class).add("pk", pk).toString();
    }

}
