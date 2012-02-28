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

import java.util.HashMap;
import java.util.Map;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.support.conversion.Conversion;
import org.deephacks.tools4j.support.conversion.Converter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class JpaBeanToBeanConverter implements Converter<JpaBean, Bean> {
    private Conversion conversion = Conversion.get();

    @Override
    public Bean convert(JpaBean source, Class<? extends Bean> specificType) {
        Bean result = convertProperties(source);
        Multimap<BeanId, JpaBean> refs = HashMultimap.create();
        collectRefs(source, refs);
        Map<BeanId, Bean> beans = convertBeans(refs);
        for (Bean bean : beans.values()) {
            for (BeanId id : bean.getReferences()) {
                if (beans.get(id) == null) {
                    throw new IllegalStateException(
                            "Bean ["
                                    + id
                                    + "] is not available and this is a bug. Bean reference must be available.");
                }
                id.setBean(beans.get(id));
            }
        }
        for (JpaRef ref : source.getReferences()) {
            BeanId id = ref.getTarget();
            id.setBean(beans.get(id));
            result.addReference(ref.getPropertyName(), id);
        }
        return result;
    }

    private Map<BeanId, Bean> convertBeans(Multimap<BeanId, JpaBean> refs) {
        Map<BeanId, Bean> beans = new HashMap<BeanId, Bean>();
        for (JpaBean jpaBean : refs.values()) {
            if (beans.get(jpaBean.getId()) != null) {
                continue;
            }
            Bean bean = convertProperties(jpaBean);
            for (JpaRef ref : jpaBean.getReferences()) {
                bean.addReference(ref.getPropertyName(), ref.getTarget());
            }
            beans.put(jpaBean.getId(), bean);
        }
        return beans;
    }

    private static Bean convertProperties(JpaBean source) {
        BeanId id = BeanId.create(source.getPk().id, source.getPk().schemaName);
        Bean bean = Bean.create(id);
        for (JpaProperty prop : source.getProperties()) {
            bean.addProperty(prop.getPropertyName(), prop.getValue());
        }
        return bean;
    }

    private static void collectRefs(JpaBean jpaBean, Multimap<BeanId, JpaBean> result) {
        if (result.containsKey(jpaBean.getId())) {
            // break redundant selects and circular recursion
            return;
        }

        for (JpaRef jpaRef : jpaBean.getReferences()) {
            result.put(jpaBean.getId(), jpaRef.getTargetBean());
            collectRefs(jpaRef.getTargetBean(), result);
        }
    }
}
