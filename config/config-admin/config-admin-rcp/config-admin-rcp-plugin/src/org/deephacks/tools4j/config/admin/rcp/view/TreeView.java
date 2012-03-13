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

import org.deephacks.tools4j.config.admin.rcp.model.Events.BeansChangedEvent;
import org.deephacks.tools4j.config.admin.rcp.model.RcpBean;
import org.deephacks.tools4j.config.admin.rcp.model.RcpBeanActions;
import org.deephacks.tools4j.config.admin.rcp.model.RcpSchema;
import org.deephacks.tools4j.config.admin.rcp.model.RcpSchemaActions;
import org.deephacks.tools4j.config.admin.rcp.model.RcpTreeRoot;
import org.deephacks.tools4j.config.admin.rcp.view.support.EventBus;
import org.deephacks.tools4j.config.admin.rcp.view.support.Observes;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

public class TreeView extends ViewPart {
    public static final String ID = "org.deephacks.tools4j.config.admin.rcp.treeview";
    private TreeViewer treeViewer;
    private RcpBeanAdapterFactory beanfactory = new RcpBeanAdapterFactory();
    private RcpTreeRootAdapterFactory treefactory = new RcpTreeRootAdapterFactory();
    private RcpSchemaAdapterFactory schemafactory = new RcpSchemaAdapterFactory();

    public TreeView() {
    }

    /**
     * The meat of setting up a the TreeAdminView.
     */
    public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        treeViewer.setContentProvider(new BaseWorkbenchContentProvider());

        /**
         * Adapters for nodes in tree
         */
        Platform.getAdapterManager().registerAdapters(beanfactory, RcpBean.class);
        Platform.getAdapterManager().registerAdapters(treefactory, RcpTreeRoot.class);
        Platform.getAdapterManager().registerAdapters(schemafactory, RcpSchema.class);

        RcpBeanActions beanActions = new RcpBeanActions();
        RcpSchemaActions schemaActions = new RcpSchemaActions();
        /**
         * Double Click actions
         */
        treeViewer.addDoubleClickListener(beanActions);
        treeViewer.addDoubleClickListener(schemaActions);
        /**
         * Right Click actions
         */
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(beanActions);
        menuMgr.addMenuListener(schemaActions);
        Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, treeViewer);
        /**
         * Selection listener
         */
        treeViewer.addSelectionChangedListener(beanActions);
        treeViewer.addSelectionChangedListener(schemaActions);
        getSite().setSelectionProvider(treeViewer);
        /**
         * Inject inital data into tree
         */
        RcpTreeRoot tree = new RcpTreeRoot();
        treeViewer.setInput(tree);
        EventBus.registerListener(this);

    }

    public void reactTo(@Observes BeansChangedEvent event) {
        RcpTreeRoot tree = new RcpTreeRoot();
        treeViewer.setInput(tree);
        treeViewer.refresh();
        treeViewer.getControl().setFocus();
    }

    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        Platform.getAdapterManager().unregisterAdapters(beanfactory, RcpBean.class);
        Platform.getAdapterManager().unregisterAdapters(treefactory, RcpTreeRoot.class);
        Platform.getAdapterManager().unregisterAdapters(schemafactory, RcpSchema.class);
        super.dispose();
    }

    public static <T> T getSelection(ISelection selection, Class<T> clazz) {
        if (!ITreeSelection.class.isInstance(selection)) {
            return null;
        }
        Object o = ((ITreeSelection) selection).getFirstElement();
        if (!clazz.isInstance(o)) {
            return null;
        }
        return clazz.cast(o);
    }
}
