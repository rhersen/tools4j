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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.deephacks.tools4j.config.admin.rcp.view.SchemaFormHandler.addSchemaWidgets;
import static org.deephacks.tools4j.config.admin.rcp.view.SchemaFormHandler.getBean;

import java.util.HashMap;
import java.util.Map;

import org.deephacks.tools4j.config.admin.AdminContext;
import org.deephacks.tools4j.config.admin.rcp.Activator;
import org.deephacks.tools4j.config.admin.rcp.model.Events.BeansChangedEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.DeleteBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.ErrorEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.PostCreateBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.PostMergeBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.PreCreateBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.PreMergeBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.model.Events.ViewBeanEvent;
import org.deephacks.tools4j.config.admin.rcp.view.support.EventBus;
import org.deephacks.tools4j.config.admin.rcp.view.support.Observes;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.support.event.AbortRuntimeException;
import org.deephacks.tools4j.support.lookup.Lookup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormView extends ViewPart {
    public static final String ID = "org.deephacks.tools4j.config.admin.rcp.formview";
    private Composite stack;
    // contain input given from form
    private Map<String, Widget> inputs = new HashMap<String, Widget>();
    private ScrolledComposite scroll = null;
    private AdminContext admin = Lookup.get().lookup(AdminContext.class);
    private Logger logger = LoggerFactory.getLogger(FormView.class);

    public FormView() {

    }

    @Override
    public void createPartControl(Composite parent) {
        scroll = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        stack = new Composite(scroll, SWT.NONE);
        stack.setLayout(new StackLayout());
        stack.setSize(500, 200);
        scroll.setContent(stack);
        scroll.setMinSize(stack.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        ((StackLayout) stack.getLayout()).topControl = layoutEmptyForm(stack);
        stack.layout();
        EventBus.registerListener(this);

    }

    public void reactTo(@Observes PreCreateBeanEvent event) {
        layoutBeanCreateForm(event.getSchema());
    }

    public void reactTo(@Observes PostCreateBeanEvent event) {
        try {
            admin.create(event.getBean());
        } catch (Exception e) {
            EventBus.fire(new ErrorEvent(e));
            return;
        }
        EventBus.fire(new BeansChangedEvent());
    }

    public void reactTo(@Observes PreMergeBeanEvent event) {
        layoutBeanMergeForm(event.getBean());
    }

    public void reactTo(@Observes PostMergeBeanEvent event) {
        try {
            admin.merge(event.getBean());
        } catch (AbortRuntimeException e) {
            EventBus.fire(new ErrorEvent(e));
            return;
        }
        EventBus.fire(new BeansChangedEvent());
    }

    public void reactTo(@Observes DeleteBeanEvent event) {
        try {
            admin.delete(event.getId());
        } catch (AbortRuntimeException e) {
            EventBus.fire(new ErrorEvent(e));
            return;
        }
        EventBus.fire(new BeansChangedEvent());
    }

    public void reactTo(@Observes ViewBeanEvent event) {
        layoutBeanCreateForm(event.getBean().getSchema());
    }

    public void reactTo(@Observes BeansChangedEvent event) {
        // layout empty form if beans changed.
        ((StackLayout) stack.getLayout()).topControl = layoutEmptyForm(stack);
        stack.layout();
    }

    public void reactTo(@Observes ErrorEvent event) {
        Throwable t = event.getThrowable();
        String msg = t.getMessage();
        if (t instanceof AbortRuntimeException) {
            AbortRuntimeException ex = ((AbortRuntimeException) t);
            msg = ex.getEvent().getMessage();
            logger.trace("Configuration error.", ex);
            MessageDialog.openError(null, "Configuration error.", msg);
            return;
        }
        logger.debug("Unexpected error occured.", t);
        MessageDialog.openError(null, "Unexpected error.", "See DEBUG log for more information.");
    }

    @Override
    public void setFocus() {
    }

    public void dispose() {
        super.dispose();
    }

    public Composite layoutEmptyForm(Composite parent) {
        inputs = new HashMap<String, Widget>();
        Composite content = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout(2, false);
        content.setLayout(layout);
        return content;
    }

    public Composite layoutBeanCreateForm(final Schema schema) {
        checkNotNull(schema);
        Composite content = layoutEmptyForm(stack);
        // layout schema and grab all input widgets
        inputs.putAll(addSchemaWidgets(content, schema));
        GridData data = new GridData(SWT.NONE);
        data.horizontalSpan = 2;
        Composite c = new Composite(content, SWT.NONE);
        c.setLayoutData(data);
        RowLayout rowlayout = new RowLayout();
        c.setLayout(rowlayout);
        Button button = new Button(c, SWT.NONE);
        button.setText("Create");
        Image accept = Activator.getImage(Activator.ACCEPT_IMAGE);
        button.setImage(accept);
        button.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent event) {

            }

            public void widgetSelected(SelectionEvent event) {
                try {
                    Bean bean = getBean(inputs, schema);
                    EventBus.fire(new PostCreateBeanEvent(bean));
                } catch (Exception e) {
                    EventBus.fire(new ErrorEvent(e));
                    return;
                }
            }
        });

        Button cancel = new Button(c, SWT.NONE);
        cancel.setText("Reset");
        Image cancelImage = Activator.getImage(Activator.CANCEL_IMAGE);
        cancel.setImage(cancelImage);

        cancel.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent event) {

            }

            public void widgetSelected(SelectionEvent event) {
                EventBus.fire(new PreCreateBeanEvent(schema));
            }
        });
        scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        ((StackLayout) stack.getLayout()).topControl = content;
        stack.layout();
        return content;
    }

    public Composite layoutBeanMergeForm(final Bean bean) {
        checkNotNull(bean);
        Composite content = layoutEmptyForm(stack);
        // layout schema and grab all input widgets
        inputs.putAll(addSchemaWidgets(content, bean));
        GridData data = new GridData(SWT.NONE);
        data.horizontalSpan = 2;
        Composite c = new Composite(content, SWT.NONE);
        c.setLayoutData(data);
        RowLayout rowlayout = new RowLayout();
        c.setLayout(rowlayout);
        Button button = new Button(c, SWT.NONE);
        button.setText("Update");
        Image accept = Activator.getImage(Activator.ACCEPT_IMAGE);
        button.setImage(accept);

        button.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent event) {

            }

            public void widgetSelected(SelectionEvent event) {
                try {
                    Bean merge = getBean(inputs, bean.getSchema());
                    EventBus.fire(new PostMergeBeanEvent(merge));
                } catch (Exception e) {
                    EventBus.fire(new ErrorEvent(e));
                    return;
                }

            }
        });

        Button cancel = new Button(c, SWT.NONE);
        cancel.setText("Undo");
        Image cancelImage = Activator.getImage(Activator.CANCEL_IMAGE);
        cancel.setImage(cancelImage);

        cancel.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent event) {

            }

            public void widgetSelected(SelectionEvent event) {
                EventBus.fire(new PreMergeBeanEvent(bean));
            }
        });

        scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        ((StackLayout) stack.getLayout()).topControl = content;
        stack.layout();
        return content;
    }

}
