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

import org.deephacks.tools4j.config.admin.rcp.model.Events.DeleteBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.PreMergeBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.view.support.EventBus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class RcpBeanActions implements IMenuListener, IDoubleClickListener,
        ISelectionChangedListener {
    private static final String DELETE_TEXT = "Delete bean";
    private static final String VIEW_TEXT = "View bean";
    private RcpBean actionTarget;

    public RcpBeanActions() {
    }

    /**
     * Responsible for showing context menu when right clicking bean in tree.
     */
    public void menuAboutToShow(IMenuManager mgr) {
        if (actionTarget == null) {
            return;
        }
        mgr.add(viewAction());
        mgr.add(deleteAction());
    }

    public Action deleteAction() {
        return new Action(DELETE_TEXT) {

            public void run() {
                EventBus.fire(new DeleteBeanEvent(actionTarget.getBean().getId()));
            }
        };
    }

    public Action viewAction() {
        return new Action(VIEW_TEXT) {
            public void run() {
                EventBus.fire(new PreMergeBeanEvent(actionTarget.getBean()));
            }

        };
    }

    public void doubleClick(DoubleClickEvent event) {
        actionTarget = getSelection(event.getSelection(), RcpBean.class);
        if (actionTarget == null) {
            return;
        }
        viewAction().run();
    }

    public void selectionChanged(SelectionChangedEvent event) {
        actionTarget = getSelection(event.getSelection(), RcpBean.class);
    }

}
