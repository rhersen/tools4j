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

import org.deephacks.tools4j.config.admin.AdminContext;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.support.lookup.Lookup;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Represent the complete tree of nodes consisting of schema and beans.
 */
public class RcpTreeRoot implements IAdaptable {
    private List<RcpSchema> schemas;
    public static final String LABEL = "Admin Tree Root";

    public RcpTreeRoot() {
        AdminContext admin = Lookup.get().lookup(AdminContext.class);
        schemas = new ArrayList<RcpSchema>();
        for (Schema schema : admin.getSchemas().values()) {
            RcpSchema rcpSchema = new RcpSchema(this, schema);
            for (Bean bean : admin.list(schema.getName())) {
                RcpBean rcpAdminBean = new RcpBean(rcpSchema, bean);
                rcpSchema.addChild(rcpAdminBean);
            }
            schemas.add(rcpSchema);
        }
    }

    public Object getAdapter(Class arg0) {
        return null;
    }

    public Object getParent() {
        return null;
    }

    public List<RcpSchema> getChildren() {
        return schemas;
    }
}
