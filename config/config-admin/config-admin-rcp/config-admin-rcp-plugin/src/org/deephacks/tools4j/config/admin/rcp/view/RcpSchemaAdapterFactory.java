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
package org.deephacks.tools4j.config.admin.rcp.view;

import org.deephacks.tools4j.config.admin.rcp.Application;
import org.deephacks.tools4j.config.admin.rcp.model.RcpBean;
import org.deephacks.tools4j.config.admin.rcp.model.RcpSchema;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class RcpSchemaAdapterFactory implements IAdapterFactory {

    private IWorkbenchAdapter groupAdapter = new IWorkbenchAdapter() {
        public Object getParent(Object o) {
            return ((RcpSchema) o).getParent();
        }

        public String getLabel(Object o) {
            return ((RcpSchema) o).getName();
        }

        public ImageDescriptor getImageDescriptor(Object object) {
            return AbstractUIPlugin
                    .imageDescriptorFromPlugin(Application.PLUGIN_ID, RcpSchema.ICON);
        }

        public Object[] getChildren(Object o) {
            RcpSchema schema = (RcpSchema) o;
            RcpBean[] beans = schema.getChildren().toArray(new RcpBean[0]);
            return beans;
        }
    };

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IWorkbenchAdapter.class && adaptableObject instanceof RcpSchema)
            return groupAdapter;
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { IWorkbenchAdapter.class };
    }
}
