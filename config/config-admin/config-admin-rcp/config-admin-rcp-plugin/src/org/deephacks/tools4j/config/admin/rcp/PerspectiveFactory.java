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
package org.deephacks.tools4j.config.admin.rcp;

import org.deephacks.tools4j.config.admin.rcp.view.FormView;
import org.deephacks.tools4j.config.admin.rcp.view.TreeView;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactory implements IPerspectiveFactory {

    public void createInitialLayout(IPageLayout layout) {

        layout.setEditorAreaVisible(false);

        layout.addStandaloneView(TreeView.ID, false, IPageLayout.LEFT, 0.5f, layout.getEditorArea());
        layout.getViewLayout(TreeView.ID).setCloseable(false);
        layout.getViewLayout(TreeView.ID).setMoveable(false);

        layout.addStandaloneView(FormView.ID, false, IPageLayout.RIGHT, 0.5f,
                layout.getEditorArea());
        layout.getViewLayout(FormView.ID).setCloseable(false);
        layout.getViewLayout(FormView.ID).setMoveable(false);

    }
}
