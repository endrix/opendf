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
import java.util.*;

import net.sf.opendf.config.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * A utility class for rendering AbstractConfig objects into appropriate UI widgets on a 
 * configuration tab.
 * 
 * @author imiller
 *
 */
public class ControlRenderingFactory
{

    public static UpdatableControlIF renderConfig (AbstractConfig config, Composite parent)
    {
        if (config.getType() == AbstractConfig.TYPE_FILE) return renderConfig((ConfigFile)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_DIR) return renderConfig((ConfigFile)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_STRING) return renderConfig((ConfigString)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_BOOL) return renderConfig((ConfigBoolean)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_LIST) return renderConfig((ConfigList)config, parent);
        return null;
    }

    public static UpdatableControlIF renderConfig (ConfigMap config, Composite parent)
    {
        return null;
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

        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                if (textBox.getText() == null || textBox.getText().length() == 0)
                    configHandle.unset();
                else
                {
                    System.out.println("Config value (pre) is " + configHandle.getValue());
                    System.out.println("Config list text box is " + textBox.getText());
                    StringTokenizer st = new StringTokenizer(textBox.getText(), Text.DELIMITER);
                    List list = new ArrayList();
                    while (st.hasMoreTokens())
                        list.add(st.nextToken());
                    configHandle.setValue(list, true);
                    System.out.println("Config value (post) is " + configHandle.getValue());
                } } 
        };
 
        textBox.addModifyListener(modListener);
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () { 
                Iterator iter = configHandle.getValue().iterator();
                String s = iter.hasNext() ? iter.next().toString():"";
                while (iter.hasNext())
                    s += Text.DELIMITER + iter.next().toString();
                // Remove the listener to avoid updating the config based on the update
                textBox.removeModifyListener(modListener);
                textBox.setText(s);
                textBox.addModifyListener(modListener);
            };
        };
        
        cif.updateValue();

        addChangeListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);
        
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

        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                if (textBox.getText() == null || textBox.getText().length() == 0)
                    configHandle.unset();
                else
                    configHandle.setValue(textBox.getText(), true);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () {
                // Remove the listener to avoid updating the config based on the update
                textBox.removeModifyListener(modListener);
                if (showFull)
                    textBox.setText(configHandle.getValue());
                else
                    textBox.setText(configHandle.getValueFile().getName());
                textBox.addModifyListener(modListener);
                };
        };
        cif.updateValue();
        
        addChangeListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);

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
                                    String filterPath = selected.getAbsoluteFile().getParent();
                                    fileSelector.setFilterPath(filterPath);
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

    public static UpdatableControlIF renderConfig (ConfigBoolean config, Composite parent)
    {
        final ConfigBoolean configHandle = config;
        final Button select = new Button(parent, SWT.CHECK);
        select.setText(configHandle.getName());
        select.setEnabled(true);
        select.setToolTipText(config.getDescription());
        select.setSelection(configHandle.getValue());

        final SelectionListener selListener = new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                configHandle.setValue(select.getSelection(), true);
            } 
        };
 
        select.addSelectionListener(selListener);
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return select; }
            public void updateValue () {
                select.removeSelectionListener(selListener);
                select.setSelection(configHandle.getValue()); 
                select.addSelectionListener(selListener);
                };
        };
        
        addChangeListener(select, cif, ConfigModificationListener.BUTTON_SELECTION);

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

        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                if (textBox.getText() == null || textBox.getText().length() == 0)
                    configHandle.unset();
                else
                    configHandle.setValue(textBox.getText(), true);
            } 
        };
 
        textBox.addModifyListener(modListener);
        
        final ConfigUpdatableControl cif = new ConfigUpdatableControl()
        {
            public Control getControl () { return group; }
            public void updateValue () { 
                textBox.removeModifyListener(modListener);
                textBox.setText(configHandle.getValue()); 
                textBox.addModifyListener(modListener);
                };
        };
        
        addChangeListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);

        /*
        textBox.addVerifyListener(
                new VerifyListener(){
                    public void verifyText (VerifyEvent e){}
                });
        */
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
                            cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
                            configHandle.setValue(absoluteFile, true);
                        }
                    } } );
        

        return cif;
    }
    
    
    /**
     *  FocusListeners are used to detect when the control widget loses focus.  This event 
     *  is taken as the opportunity to notify entities that are listening for changes to that 
     *  widget. 
     *  
     * @param control
     * @param cif
     * @param type
     */
    private static void addChangeListener (Control control, ConfigUpdatableControl cif, int type)
    {
        final ConfigUpdatableControl cifHandle = cif;
        final int typeHandle = type;
        control.addFocusListener(new FocusListener(){
            public void focusGained (FocusEvent e){}
            public void focusLost (FocusEvent e){
                cifHandle.modificationNotify(typeHandle);
            }});
        if (control instanceof Text)
        {
            ((Text)control).addModifyListener(new ModifyListener(){
                public void modifyText (ModifyEvent e){
                    cifHandle.modificationNotify(typeHandle);
                }});
        }
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
        @Override
        public void addModifyListener (ConfigModificationListener listener)
        {
            this.listeners.add(listener);
        }
        public void modificationNotify (int type)
        {
            for (ConfigModificationListener listener : this.listeners)
                listener.registerModification(type);
        }
    }
}
