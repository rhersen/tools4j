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

import java.util.ArrayList;
import java.util.List;

import org.deephacks.tools4j.config.model.Schema;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Represents a Schema node in the admin tree.
 */
public class RcpSchema implements IAdaptable {
    public static final String ICON = "icons/schema.png";
    private Schema schema;
    private List<RcpBean> children;
    private RcpTreeRoot parent;

    public RcpSchema(RcpTreeRoot parent, Schema schema) {
        this.schema = schema;
        this.children = new ArrayList<RcpBean>();
        this.parent = parent;
    }

    public void addChild(RcpBean bean) {
        children.add(bean);
    }

    public String getName() {
        return schema.getName();
    }

    public Object getAdapter(Class arg0) {
        return null;
    }

    public RcpTreeRoot getParent() {
        return parent;
    }

    public List<RcpBean> getChildren() {
        return children;
    }

    public Schema getSchema() {
        return schema;
    }
}
