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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deephacks.tools4j.config.admin.rcp.Activator;
import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Schema;
import org.deephacks.tools4j.config.model.Schema.AbstractSchemaProperty;
import org.deephacks.tools4j.config.model.Schema.SchemaProperty;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyList;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRef;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRefList;
import org.deephacks.tools4j.config.model.Schema.SchemaPropertyRefMap;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/**
 * Layout a form of widgets (combo, text, checkbox etc) in a composite according to a 
 * particular schema.   
 */
public class SchemaFormHandler {
    private static final int TEXT_FIELD_WIDTH = 200;

    public static Map<String, Widget> addSchemaWidgets(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        inputs.putAll(layoutId(content, schema));
        inputs.putAll(layoutSingle(content, schema));
        inputs.putAll(layoutList(content, schema));
        inputs.putAll(layoutRef(content, schema));
        inputs.putAll(layoutRefList(content, schema));
        inputs.putAll(layoutRefMap(content, schema));
        return inputs;
    }

    public static Map<String, Widget> addSchemaWidgets(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();

        inputs.putAll(layoutId(content, bean));
        inputs.putAll(layoutSingle(content, bean));
        inputs.putAll(layoutList(content, bean));
        inputs.putAll(layoutRef(content, bean));
        inputs.putAll(layoutRefList(content, bean));
        inputs.putAll(layoutRefMap(content, bean));
        return inputs;
    }

    public static Bean getBean(Map<String, Widget> inputs, Schema schema) {
        Widget id = inputs.get(schema.getId().getName());
        String instanceId = null;
        if (id instanceof Label) {
            // merge
            instanceId = ((Label) id).getText();
        } else if (id instanceof Text) {
            // create
            instanceId = ((Text) id).getText();
        }
        Bean bean = Bean.create(BeanId.create(instanceId, schema.getName()));
        for (SchemaProperty prop : schema.get(SchemaProperty.class)) {
            String value = ((Text) inputs.get(prop.getName())).getText();
            if (value != null && !"".equals(value)) {
                bean.addProperty(prop.getName(), value);
            }
        }
        for (SchemaPropertyRef prop : schema.get(SchemaPropertyRef.class)) {
            String value = ((Text) inputs.get(prop.getName())).getText();
            if (value != null && !"".equals(value)) {
                bean.addReference(prop.getName(), BeanId.create(value, prop.getSchemaName()));
            }
        }
        for (SchemaPropertyList prop : schema.get(SchemaPropertyList.class)) {
            Combo combo = (Combo) inputs.get(prop.getName());
            String[] items = combo.getItems();
            if (items == null || items.length == 0) {
                if (combo.getData("dirty") != null) {
                    bean.setProperty(prop.getName(), (List<String>) null);
                }
            } else {
                for (String value : items) {
                    bean.addProperty(prop.getName(), value);
                }
            }
        }
        for (SchemaPropertyRefList prop : schema.get(SchemaPropertyRefList.class)) {
            Combo combo = (Combo) inputs.get(prop.getName());
            String[] items = combo.getItems();
            if (items == null || items.length == 0) {
                if (combo.getData("dirty") != null) {
                    bean.setReferences(prop.getName(), (List<BeanId>) null);
                }
            } else {
                for (String value : items) {
                    bean.addReference(prop.getName(), BeanId.create(value, prop.getSchemaName()));
                }
            }
        }
        for (SchemaPropertyRefMap prop : schema.get(SchemaPropertyRefMap.class)) {
            Combo combo = (Combo) inputs.get(prop.getName());
            String[] items = combo.getItems();
            if (items == null || items.length == 0) {
                if (combo.getData("dirty") != null) {
                    bean.setReferences(prop.getName(), (List<BeanId>) null);
                }
            } else {
                for (String value : items) {
                    bean.addReference(prop.getName(), BeanId.create(value, prop.getSchemaName()));
                }
            }
        }

        bean.set(schema);
        return bean;
    }

    private static Map<String, Widget> layoutId(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Label label = new Label(content, SWT.NONE);
        String propName = schema.getId().getName();
        label.setText(propName);
        Text text = new Text(content, SWT.BORDER | SWT.NONE);
        setTextFieldWidth(text);
        text.setData(propName);
        inputs.put(propName, text);
        return inputs;
    }

    private static Map<String, Widget> layoutId(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        String propName = bean.getSchema().getId().getName();
        Label label = new Label(content, SWT.NONE);
        label.setText(propName);
        Label text = new Label(content, SWT.NONE);

        text.setText(bean.getId().getInstanceId());
        text.setData(propName);
        inputs.put(propName, text);
        return inputs;
    }

    private static Map<String, Widget> layoutSingle(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaProperty> props = schema.get(SchemaProperty.class);
        for (SchemaProperty schemaProp : props) {
            Text text = layoutText(content, schemaProp, null);
            text.setToolTipText(getToolTip(schemaProp.getType(), schemaProp));
            inputs.put(schemaProp.getName(), text);

        }
        return inputs;
    }

    private static Map<String, Widget> layoutSingle(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaProperty> props = bean.getSchema().get(SchemaProperty.class);
        for (SchemaProperty schemaProp : props) {
            String propName = schemaProp.getName();
            Text text = layoutText(content, schemaProp, bean.getSingleValue(propName));
            text.setToolTipText(getToolTip(schemaProp.getType(), schemaProp));
            inputs.put(propName, text);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutList(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyList> propsList = schema.get(SchemaPropertyList.class);
        for (SchemaPropertyList schemaProp : propsList) {
            String propName = schemaProp.getName();
            Combo combo = layoutCombo(content, schemaProp, null);
            combo.setToolTipText(getToolTip("List of " + schemaProp.getType(), schemaProp));
            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutList(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyList> propsList = bean.getSchema().get(SchemaPropertyList.class);
        for (SchemaPropertyList schemaProp : propsList) {
            String propName = schemaProp.getName();
            Combo combo = layoutCombo(content, schemaProp, bean.getValues(propName));
            combo.setToolTipText(getToolTip("List of " + schemaProp.getType(), schemaProp));
            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRef(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRef> props = schema.get(SchemaPropertyRef.class);
        for (SchemaPropertyRef schemaProp : props) {
            String propName = schemaProp.getName();
            Text text = layoutText(content, schemaProp, null);
            text.setToolTipText(getToolTip(schemaProp.getSchemaName(), schemaProp));
            inputs.put(propName, text);
            Class.class.getSimpleName();
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRef(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRef> props = bean.getSchema().get(SchemaPropertyRef.class);
        for (SchemaPropertyRef schemaProp : props) {
            String propName = schemaProp.getName();
            BeanId id = bean.getFirstReference(propName);
            String value = null;
            if (id != null) {
                value = id.getInstanceId();
            }
            Text text = layoutText(content, schemaProp, value);
            text.setToolTipText(getToolTip(schemaProp.getSchemaName(), schemaProp));
            inputs.put(propName, text);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRefList(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRefList> propsRefList = schema.get(SchemaPropertyRefList.class);
        for (SchemaPropertyRefList schemaProp : propsRefList) {
            String propName = schemaProp.getName();
            Combo combo = layoutCombo(content, schemaProp, null);
            combo.setToolTipText(getToolTip("List of " + schemaProp.getSchemaName(), schemaProp));

            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRefList(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRefList> propsRefList = bean.getSchema().get(SchemaPropertyRefList.class);
        for (SchemaPropertyRefList schemaProp : propsRefList) {
            String propName = schemaProp.getName();
            List<BeanId> ids = bean.getReference(propName);
            Combo combo = layoutIdCombo(content, schemaProp, ids);
            combo.setToolTipText(getToolTip("List of " + schemaProp.getSchemaName(), schemaProp));
            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRefMap(Composite content, Schema schema) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRefMap> propsRefMap = schema.get(SchemaPropertyRefMap.class);
        for (SchemaPropertyRefMap schemaProp : propsRefMap) {
            String propName = schemaProp.getName();
            Combo combo = layoutCombo(content, schemaProp, null);
            combo.setToolTipText(getToolTip("List of " + schemaProp.getSchemaName(), schemaProp));
            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Map<String, Widget> layoutRefMap(Composite content, Bean bean) {
        Map<String, Widget> inputs = new HashMap<String, Widget>();
        Set<SchemaPropertyRefMap> propsRefMap = bean.getSchema().get(SchemaPropertyRefMap.class);
        for (SchemaPropertyRefMap schemaProp : propsRefMap) {
            String propName = schemaProp.getName();
            List<BeanId> values = bean.getReference(propName);
            Combo combo = layoutIdCombo(content, schemaProp, values);
            combo.setToolTipText(getToolTip("List of " + schemaProp.getSchemaName(), schemaProp));
            inputs.put(propName, combo);
        }
        return inputs;
    }

    private static Text layoutText(Composite content, AbstractSchemaProperty prop, String value) {
        Label label = new Label(content, SWT.NONE);
        label.setText(prop.getName());

        Text text = new Text(content, SWT.BORDER | SWT.NONE);
        text.computeSize(300, 300);
        setTextFieldWidth(text);
        text.pack();
        // name that identify the property when data is submitted
        text.setData(prop.getName());
        if (value != null) {
            text.setText(value);
        }
        return text;
    }

    private static Combo layoutIdCombo(Composite content, AbstractSchemaProperty prop,
            List<BeanId> ids) {
        List<String> values = new ArrayList<String>();
        if (ids != null) {
            for (BeanId id : ids) {
                values.add(id.getInstanceId());
            }
        }
        return layoutCombo(content, prop, values);
    }

    private static Combo layoutCombo(Composite content, final AbstractSchemaProperty prop,
            List<String> values) {
        Label label = new Label(content, SWT.NONE);
        label.setText(prop.getName());
        Composite buttons = new Composite(content, SWT.NONE);
        RowLayout row = new RowLayout();
        buttons.setLayout(row);
        // name that identify the property when data is submitted
        final Combo combo = new Combo(buttons, SWT.READ_ONLY);
        combo.setSize(200, 200);
        combo.setTouchEnabled(false);
        combo.setLayoutData(new RowData(TEXT_FIELD_WIDTH + 5, combo.getTextHeight()));
        combo.setData(prop.getName());
        if (values != null) {
            for (String value : values) {
                combo.add(value);
            }
        }

        Label add = new Label(buttons, SWT.NONE);
        add.setImage(Activator.getImage(Activator.ADD_IMAGE));
        add.addMouseListener(new MouseListener() {

            public void mouseUp(MouseEvent arg0) {

            }

            public void mouseDown(MouseEvent arg0) {
                InputDialog dialog = new InputDialog(prop);
                int value = dialog.open();

                if (value == Window.OK) {
                    combo.add(dialog.getInput());
                    combo.select(0);
                    combo.setData("dirty", "true");
                    combo.redraw();
                }

            }

            public void mouseDoubleClick(MouseEvent arg0) {

            }
        });

        Label delete = new Label(buttons, SWT.NONE);
        delete.setImage(Activator.getImage(Activator.DELETE_IMAGE));

        delete.addMouseListener(new MouseListener() {

            public void mouseUp(MouseEvent arg0) {

            }

            public void mouseDown(MouseEvent arg0) {
                combo.remove(combo.getSelectionIndex());
                combo.setData("dirty", "true");
                combo.select(0);

            }

            public void mouseDoubleClick(MouseEvent arg0) {

            }
        });

        combo.select(0);
        return combo;
    }

    public static class InputDialog extends TitleAreaDialog {
        private Text input;
        private String inputData;

        private AbstractSchemaProperty prop;

        public InputDialog(AbstractSchemaProperty prop) {
            super(Display.getCurrent().getActiveShell());
            this.prop = prop;
        }

        public void create() {
            super.create();
            setTitle("Add a " + prop.getName() + " value.");
            setMessage(prop.getDesc(), IMessageProvider.INFORMATION);

        }

        @Override
        protected Control createDialogArea(Composite parent) {
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            // layout.horizontalAlignment = GridData.FILL;
            parent.setLayout(layout);

            // The text fields will grow with the size of the dialog
            GridData gridData = new GridData();
            gridData.grabExcessHorizontalSpace = true;
            gridData.horizontalAlignment = GridData.FILL;

            Label label1 = new Label(parent, SWT.NONE);
            label1.setText(prop.getName());

            input = new Text(parent, SWT.BORDER);
            input.setLayoutData(gridData);

            return parent;
        }

        public String getInput() {
            return inputData;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            GridData gridData = new GridData();
            gridData.horizontalSpan = 2;
            parent.setLayoutData(gridData);

            Button ok = createButton(parent, OK, "Ok", false);
            ok.setImage(Activator.getImage(Activator.ACCEPT_IMAGE));
            ok.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    okPressed();
                }
            });

            Button cancel = createButton(parent, CANCEL, "Cancel", false);
            cancel.setImage(Activator.getImage(Activator.CANCEL_IMAGE));
            cancel.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    close();
                }
            });
        }

        protected void okPressed() {
            // the UI gets disposed and the Text Fields are not 
            // accessible any more so transfer data to strings. 
            inputData = input.getText();
            super.okPressed();
        }

    }

    private static void setTextFieldWidth(Text text) {
        text.setLayoutData(new GridData(TEXT_FIELD_WIDTH, text.getLineHeight()));
    }

    private static String getToolTip(String type, AbstractSchemaProperty prop) {
        return prop.getDesc() + System.getProperty("line.separator") + "Datatype: "
                + type.substring(type.lastIndexOf(".") + 1);
    }
}
