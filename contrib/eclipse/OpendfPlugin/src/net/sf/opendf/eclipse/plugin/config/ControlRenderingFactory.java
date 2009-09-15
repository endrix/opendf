/* 
BEGINCOPYRIGHT X
  
  Copyright (c) 2008, Xilinx Inc.
  All rights reserved.
  
  Redistribution and use in source and binary forms, 
  with or without modification, are permitted provided 
  that the following conditions are met:
  - Redistributions of source code must retain the above 
    copyright notice, this list of conditions and the 
    following disclaimer.
  - Redistributions in binary form must reproduce the 
    above copyright notice, this list of conditions and 
    the following disclaimer in the documentation and/or 
    other materials provided with the distribution.
  - Neither the name of the copyright holder nor the names 
    of its contributors may be used to endorse or promote 
    products derived from this software without specific 
    prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
ENDCOPYRIGHT
*/
package net.sf.opendf.eclipse.plugin.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.opendf.config.AbstractConfig;
import net.sf.opendf.config.ConfigBoolean;
import net.sf.opendf.config.ConfigFile;
import net.sf.opendf.config.ConfigInt;
import net.sf.opendf.config.ConfigList;
import net.sf.opendf.config.ConfigMap;
import net.sf.opendf.config.ConfigString;
import net.sf.opendf.config.ConfigStringPickOne;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * A utility class for rendering AbstractConfig objects into appropriate UI widgets on a 
 * configuration tab.
 * 
 * @author imiller
 *
 */
@SuppressWarnings("unchecked")
public class ControlRenderingFactory
{

    /**
     * Render the specified config into an appropriate type of {@link Control} widget.
     * @param config
     * @param parent
     * @return
     */
    public static UpdatableControlIF renderConfig (AbstractConfig config, Composite parent)
    {
        if (config.getType() == AbstractConfig.TYPE_FILE) return renderConfig((ConfigFile)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_DIR) return renderConfig((ConfigFile)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_PICKONE) return renderConfig((ConfigStringPickOne)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_STRING) return renderConfig((ConfigString)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_BOOL) return renderConfig((ConfigBoolean)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_LIST) return renderConfig((ConfigList)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_MAP) return renderConfig((ConfigMap)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_INT) return renderConfig((ConfigInt)config, parent);
        return null;
    }

    public static UpdatableControlIF renderConfig (ConfigMap config, Composite parent)
    {
        final ConfigMap configHandle = config;
        final int nameKey = 0;
        final int valueKey = 1;
        
        // A group to provide the border and config name
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        group.setLayout( new GridLayout( 1, false ) );
        
        // A composite to hold the table layout
        final Composite tableHolder = new Composite( group, SWT.NONE );
        tableHolder.setLayoutData(new GridData( GridData.FILL_BOTH ));
        TableColumnLayout columnLayout = new TableColumnLayout();
        tableHolder.setLayout( columnLayout );
          
        // The table
        final Table table = new Table( tableHolder, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL );
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setToolTipText(config.getDescription());
        table.setLinesVisible( false );
        table.setHeaderVisible( true );
          
        final String[] columnLabels = { "Parameter", "Value"};
        TableColumn[] columns = new TableColumn[ columnLabels.length ];
        for( int i = 0; i < columnLabels.length ; i++ )
        {
          columns[i] = new TableColumn( table, SWT.LEFT, i );
          columns[i].setText( columnLabels[i] );
          columnLayout.setColumnData( columns[i], new ColumnWeightData( i == 1 ? 80 : 10, 100) );
        }
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return tableHolder; }
            // The method called when the backing config has changed, in order to update the table.
            // Removes all entries and re-adds them (after sorting by string).
            public void updateValue () {
                table.removeAll();
                Map<String, String> params = configHandle.getValue();
                // Ensuring that there is at least 1 row ensures a minimum size for the table
                // Not sure how else to ensure this...
                if (params.isEmpty()) params = Collections.singletonMap("", " ");
                ArrayList<String> keys = new ArrayList(params.keySet());
                Collections.sort(keys);
                for (String key : keys)
                {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setText(new String[]{key, params.get(key)});
                }

                table.getColumn(nameKey).pack();
                table.getColumn(valueKey).pack();
            };
        };
        
        // The listener which is responsible for updating the config based on changes to the table
        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                Map map = new LinkedHashMap();
                TableItem[] items = table.getItems();
                for (int i=0; i < items.length; i++)
                {
                    if (items[i].getText(nameKey).equals("")) continue;
                    map.put(items[i].getText(nameKey), items[i].getText(valueKey));
                }
                configHandle.setValue(map, true);
                // Notify the context that a change to the control has occurred
                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
            }
        };

        // Call update to display the initial value
        cif.updateValue();

        // Generate a cell editor to allow parameter values to be directly specified
        final TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;
        
        table.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected (SelectionEvent e) {}
            public void widgetSelected (SelectionEvent e)
            {
                // Clean up any previous editor control
                Control oldEditor = editor.getEditor();
                if (oldEditor != null) oldEditor.dispose();

                // Identify the selected row
                TableItem item = (TableItem)e.item;
                if (item == null) return;
                // Disallow editing of an empty row (occurs when there are no parameters)
                if (item.getText(nameKey).equals("")) return;

                // The control that will be the editor must be a child of the Table
                Text newEditor = new Text(table, SWT.NONE);
                newEditor.setText(item.getText(valueKey));
                newEditor.addModifyListener(new ModifyListener() {
                    public void modifyText(ModifyEvent e) {
                        editor.getItem().setText(valueKey, ((Text)editor.getEditor()).getText());
                    }
                });
                newEditor.addModifyListener(modListener);
                newEditor.selectAll();
                newEditor.setFocus();
                editor.setEditor(newEditor, item, valueKey);
                
                newEditor.addFocusListener(new FocusListener(){
                    public void focusGained(FocusEvent e) {} 
                    public void focusLost(FocusEvent e) {
                        // When we lose focus, ditch the editor
                        if (editor.getEditor() != null)
                            editor.getEditor().dispose();
                    } });
            }
        });

        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigList config, Composite parent)
    {
        final ConfigList configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName() + " (one entry per line)");
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );
        final Text textBox = new Text( group, SWT.LEFT | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        textBox.setEnabled(true);
        textBox.setToolTipText(config.getDescription());
        textBox.setLayoutData(new GridData( GridData.FILL_HORIZONTAL));

        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () { 
                Iterator iter = configHandle.getValue().iterator();
                String s = iter.hasNext() ? iter.next().toString():"";
                while (iter.hasNext())
                    s += Text.DELIMITER + iter.next().toString();
                textBox.setText(s);
            };
        };
        
        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                StringTokenizer st = new StringTokenizer(textBox.getText(), Text.DELIMITER);
                List list = new ArrayList();
                while (st.hasMoreTokens())
                    list.add(st.nextToken());
                configHandle.setValue(list, true);
                // update the context when this control has changed
                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        cif.updateValue();

        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigFile config, Composite parent)
    {
        return renderConfigFileSelect(config, parent, false, false);
    }
    
    public static UpdatableControlIF renderConfigFileSelect (ConfigFile config, Composite parent, 
            boolean withBrowse, boolean showFullPath)
    {
        final ConfigFile configHandle = config;
        final boolean showFull = showFullPath;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 2, false ) );
        final Text textBox = new Text( group, SWT.LEFT | SWT.SINGLE | SWT.BORDER );
        textBox.setEnabled(true);
        textBox.setToolTipText(config.getDescription());
        textBox.setLayoutData(new GridData( GridData.FILL_HORIZONTAL));

        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () {
                if (showFull) textBox.setText(configHandle.getValue());
                else textBox.setText(configHandle.getValueFile().getName());
                };
        };

        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                configHandle.setValue(textBox.getText(), true);
                // update the context when this control has changed
                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        cif.updateValue();

        // Conditionally generate a file browsing button
        if (withBrowse)
        {
            final Button browse = new Button(group, SWT.PUSH | SWT.CENTER);
            browse.setText("Browse");
            browse.setToolTipText("Browse file system...");
            browse.addSelectionListener(
                    new SelectionListener(){
                        public void widgetDefaultSelected(SelectionEvent e) {}
                        public void widgetSelected(SelectionEvent e) {
                            File selected = configHandle.getValueFile();
                            String selectedName = selected.getName();
                            String absoluteFile = null;

                            if (configHandle.getType() == AbstractConfig.TYPE_FILE)
                            {
                                FileDialog fileSelector = new FileDialog(group.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );

                                String preferredExt = null;
                                // Only set a preferred extension if it is a valid selection
                                if (configHandle.isUserSpecified() && configHandle.validate() && selectedName.lastIndexOf('.') > 0)  
                                    preferredExt = "*" + selectedName.substring(selectedName.lastIndexOf('.'));
                                setDialogExtensions(fileSelector, configHandle, preferredExt);

                                if (selected.getAbsoluteFile().getParent() != null)
                                {
                                    fileSelector.setFilterPath(selected.getAbsoluteFile().getParent());
                                }
                                absoluteFile = fileSelector.open();
                            } else {
                                DirectoryDialog dirDialog = new DirectoryDialog(group.getShell());
                                if (selected.getAbsoluteFile().getParent() != null)
                                    dirDialog.setFilterPath(selected.getAbsoluteFile().getParent());
                                absoluteFile = dirDialog.open();
                            }
                            if (absoluteFile != null) // null if 'cancel' from dialog
                            {
                                configHandle.setValue(absoluteFile, true);
                                cif.updateValue();
                                // The user has specified a new value for the config.  Notify the context that 
                                // the change has occurred
                                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
                            }
                        } } );
        }
        /*
        textBox.addVerifyListener(
                new VerifyListener(){
                    public void verifyText (VerifyEvent e){}
                });
         */
        return cif;
    }

    /**
     * Generates a checkbox control for the element.
     * @param config
     * @param parent
     * @return
     */
    public static UpdatableControlIF renderConfig (ConfigBoolean config, Composite parent)
    {
        final ConfigBoolean configHandle = config;
        final Button select = new Button(parent, SWT.CHECK);
        select.setText(configHandle.getName());
        select.setEnabled(true);
        select.setToolTipText(config.getDescription());

        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return select; }
            public void updateValue () {
                select.setSelection(configHandle.getValue().booleanValue());
                };
        };
        
        final SelectionListener selListener = new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                configHandle.setValue(select.getSelection(), true);
                cif.modificationNotify(ConfigModificationListener.BUTTON_SELECTION);
            } 
        };

        select.addSelectionListener(selListener);
        
        cif.updateValue();        
        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigString config, Composite parent)
    {
        final ConfigString configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );
        final Text textBox = new Text( group, SWT.LEFT | SWT.SINGLE | SWT.BORDER );
        textBox.setEnabled(true);
        textBox.setToolTipText(config.getDescription());
        textBox.setLayoutData(new GridData( GridData.FILL_HORIZONTAL));
        textBox.setText(configHandle.getValue());

        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () { textBox.setText(configHandle.getValue()); };
        };
        
        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                configHandle.setValue(textBox.getText(), true);
                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        /*
        textBox.addVerifyListener(
                new VerifyListener(){
                    public void verifyText (VerifyEvent e){}
                });
        */
        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigInt config, Composite parent)
    {
        final ConfigInt configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );

        // The spinner class was fixed in the Eclipse build on 20080115 to allow
        // negative values.  But, our base version is 3.2 for now.
        //final Spinner textBox = new Spinner(group, SWT.NONE);
        //textBox.setMaximum(Integer.MAX_VALUE);
        //textBox.setMinimum(-1);
        final Text textBox = new Text( group, SWT.LEFT | SWT.SINGLE | SWT.BORDER );
        textBox.setEnabled(true);
        textBox.setTextLimit(11); // Enough for +/- Integer.MAX_VALUE in radix 10
        textBox.setToolTipText(config.getDescription());
        textBox.setLayoutData(new GridData( GridData.FILL_HORIZONTAL));
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () {
                    textBox.setText(configHandle.getValueString());
                };
        };
        
        cif.updateValue();
        
        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                try {
                    configHandle.setValue(textBox.getText(), true);
                }
                catch (Exception ie) {}
                cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        /*
        textBox.addVerifyListener(
                new VerifyListener(){
                    public void verifyText (VerifyEvent e){}
                });
        */
        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigStringPickOne config, Composite parent)
    {
        final ConfigStringPickOne configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );

        final Combo dropDown = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        dropDown.setEnabled(true);
        dropDown.setText(config.getName());
        for (String element : config.getAllowable())
            dropDown.add(element);
        dropDown.pack(true);
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () {
                dropDown.clearSelection();
                dropDown.select(configHandle.getAllowable().indexOf(configHandle.getValue()));
            }
        };

        dropDown.addSelectionListener(
                new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {}
                    public void widgetSelected(SelectionEvent e) {
                        configHandle.setValue(dropDown.getItems()[dropDown.getSelectionIndex()],true);
                        cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
                    }}
        );
        
        cif.updateValue();

        return cif;
    }
    
    public static UpdatableControlIF fileSelectButton (Composite parent, String text, boolean isDir, ConfigFile config)
    {
        final ConfigFile configHandle = config;
        final Composite parentHandle = parent;

        
        final Button browse = new Button(parent, SWT.PUSH | SWT.CENTER);
        browse.setText(text);
        browse.setToolTipText("Browse file system...");
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return browse; }
            public void updateValue () { } // nothing updatable
        };
                
        browse.addSelectionListener(
                new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {}
                    public void widgetSelected(SelectionEvent e) {
                        File selected = configHandle.getValueFile();
                        String selectedName = selected.getName();
                        String absoluteFile = null;

                        if (configHandle.getType() == AbstractConfig.TYPE_FILE)
                        {
                            FileDialog fileSelector = new FileDialog(parentHandle.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );

                            String preferredExt = null;
                            // Only set a preferred extension if it is a valid selection
                            if (configHandle.isUserSpecified() && configHandle.validate() && selectedName.lastIndexOf('.') > 0)  
                                preferredExt = "*" + selectedName.substring(selectedName.lastIndexOf('.'));
                            setDialogExtensions(fileSelector, configHandle, preferredExt);

                            if (selected.getAbsoluteFile().getParent() != null)
                            {
                                String filterPath = selected.getAbsoluteFile().getParent();
                                fileSelector.setFilterPath(filterPath);
                            }
                            absoluteFile = fileSelector.open();
                        } else {
                            DirectoryDialog dirDialog = new DirectoryDialog(parentHandle.getShell());
                            if (selected.getAbsoluteFile().getParent() != null)
                                dirDialog.setFilterPath(selected.getAbsoluteFile().getParent());
                            absoluteFile = dirDialog.open();
                        }
                        if (absoluteFile != null) // null if 'cancel' from dialog
                        {
                            configHandle.setValue(absoluteFile, true);
                            cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
                        }
                    } } );
        

        return cif;
    }
    
    
    /**
     * Queries the config for a list of valid extensions, then orders them according to the
     * preferred extension (making sure it is first if it is non null).  To be nice to users
     * the all files (*.*) filter is added.
     * 
     * @param dialog
     * @param config
     * @param preferredExtension, may be null if there is no preferred extension
     */
    private static void setDialogExtensions (FileDialog dialog, ConfigFile config, String preferredExtension)
    {
        Map<String, String> filters = config.getFilters();
        List<String> allExtensions = new ArrayList(filters.keySet());
        allExtensions.remove(preferredExtension); // OK to remove 'null'
        
        List<String> names = new ArrayList();
        for (String key : allExtensions)
            names.add(filters.get(key) + " (" + key + ")");
        
        if (preferredExtension != null)
        {
            allExtensions.add(0, preferredExtension);
            String prefName = filters.containsKey(preferredExtension) ? filters.get(preferredExtension):preferredExtension;   
            names.add(0, prefName + " ("+preferredExtension+")");
        }

        // always be nice to the user
        if (!allExtensions.contains("*.*"))
        {
            if (preferredExtension == null)
            {
                allExtensions.add(0,"*.*");
                names.add(0,"All Files (*.*)");
            } else {
                allExtensions.add("*.*");
                names.add("All Files (*.*)");
            }
        }
        
        dialog.setFilterExtensions(allExtensions.toArray(new String[0]));
        dialog.setFilterNames(names.toArray(new String[0]));
    }
    
    private static abstract class ConfigUpdatableControl implements UpdatableControlIF
    {
        private List<ConfigModificationListener> listeners = new ArrayList();
        public void addModifyListener (ConfigModificationListener listener)
        {
            if (!this.listeners.contains(listener))
                this.listeners.add(listener);
        }
        public void modificationNotify (int type)
        {
            for (ConfigModificationListener listener : this.listeners)
                listener.registerModification(type);
        }
    }
}
