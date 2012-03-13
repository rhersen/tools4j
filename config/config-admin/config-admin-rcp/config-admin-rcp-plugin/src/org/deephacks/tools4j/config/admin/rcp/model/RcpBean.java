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
package org.deephacks.tools4j.config.admin.rcp.model;

import org.deephacks.tools4j.config.model.Bean;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Represents a Bean node in the admin tree.
 */
public class RcpBean implements IAdaptable {
    public static final String ICON = "icons/bean.png";
    private Bean bean;
    private RcpSchema parent;

    public RcpBean(RcpSchema schema, Bean bean) {
        this.bean = bean;
        this.parent = schema;
    }

    public Object getAdapter(Class arg0) {
        return null;
    }

    public String getName() {
        return bean.getId().getInstanceId();
    }

    public RcpSchema getParent() {
        return parent;
    }

    public Bean getBean() {
        return bean;
    }

}
