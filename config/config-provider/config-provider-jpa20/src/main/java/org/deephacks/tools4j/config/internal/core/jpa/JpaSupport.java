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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.deephacks.tools4j.config.model.Bean.BeanId;

/**
 * JpaSupport provide various utility methods used by query methods.
 */
public class JpaSupport {
    /**
     * Beans with different schemaName may have same instance id.
     *   
     * The IN search query is greedy, finding instances that
     * match any combination of instance id and schemaName. Hence, 
     * the query may find references belonging to wrong schema 
     * so filter those out. 
     */
    static void filterUnwantedReferences(List<JpaRef> result, Collection<JpaBean> queryBeans) {
        Map<BeanId, JpaBean> query = index(queryBeans);
        ListIterator<JpaRef> it = result.listIterator();
        while (it.hasNext()) {
            // remove reference from result that was not part of the query
            BeanId found = it.next().getSource();
            if (!query.containsKey(found)) {
                it.remove();
            }
        }
    }

    private static Map<BeanId, JpaBean> index(Collection<JpaBean> beans) {
        Map<BeanId, JpaBean> map = new HashMap<BeanId, JpaBean>();
        for (JpaBean jpaBean : beans) {
            map.put(jpaBean.getId(), jpaBean);
        }
        return map;
    }

    /**
     * Beans with different schemaName may have same instance id.
     *   
     * The IN search query is greedy, finding instances that
     * match any combination of instance id and schemaName. Hence, 
     * the query may find references belonging to wrong schema 
     * so filter those out. 
     */
    static void filterUnwantedReferences(List<JpaProperty> result, Set<BeanId> query) {
        ListIterator<JpaProperty> it = result.listIterator();
        while (it.hasNext()) {
            // remove property from result that was not part of the query
            JpaProperty found = it.next();
            if (!query.contains(found.getId())) {
                it.remove();
            }
        }
    }

    static List<JpaBean> toJpaBeans(List<JpaProperty> properties) {
        Map<BeanId, JpaBean> jpaBeans = new HashMap<BeanId, JpaBean>();
        for (JpaProperty jpaProperty : properties) {
            JpaBean jpaBean = jpaBeans.get(jpaProperty.getId());
            if (jpaBean == null) {
                jpaBean = new JpaBean(new JpaBeanPk(jpaProperty.getId()));
                jpaBeans.put(jpaProperty.getId(), jpaBean);
            }
        }
        return new ArrayList<JpaBean>(jpaBeans.values());
    }

}
