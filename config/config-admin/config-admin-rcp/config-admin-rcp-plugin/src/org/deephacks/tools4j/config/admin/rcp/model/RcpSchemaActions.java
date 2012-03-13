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

import static org.deephacks.tools4j.config.admin.rcp.view.TreeView.getSelection;

import org.deephacks.tools4j.config.admin.rcp.model.Events.PreCreateBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.view.support.EventBus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class RcpSchemaActions implements IMenuListener, IDoubleClickListener,
        ISelectionChangedListener {
    private static final String VIEW_TEXT = "View schema";
    private static final String CREATE_TEXT = "Create bean";
    public RcpSchema actionTarget;

    public RcpSchemaActions() {

    }

    /**
     * Responsible for showing rightclick contextmenu in tree.
     */
    public void menuAboutToShow(IMenuManager mgr) {
        if (actionTarget == null) {
            return;
        }
        mgr.add(viewAction());
        mgr.add(createAction());
    }

    public Action createAction() {
        return new Action(CREATE_TEXT) {
            public void run() {
                EventBus.fire(new PreCreateBeanEvent(actionTarget.getSchema()));
            }
        };
    }

    public Action viewAction() {
        return new Action(VIEW_TEXT) {
            public void run() {
                EventBus.fire(new PreCreateBeanEvent(actionTarget.getSchema()));
            }

        };
    }

    public void doubleClick(DoubleClickEvent event) {
        actionTarget = getSelection(event.getSelection(), RcpSchema.class);
        if (actionTarget == null) {
            return;
        }
        viewAction().run();
    }

    public void selectionChanged(SelectionChangedEvent event) {
        actionTarget = getSelection(event.getSelection(), RcpSchema.class);
    }

}
