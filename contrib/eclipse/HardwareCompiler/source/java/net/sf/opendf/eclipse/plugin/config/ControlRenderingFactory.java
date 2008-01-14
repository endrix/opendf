package net.sf.opendf.eclipse.plugin.config;

import java.io.File;
import java.util.*;

import net.sf.opendf.config.AbstractConfig;
import net.sf.opendf.config.ConfigBoolean;
import net.sf.opendf.config.ConfigFile;
import net.sf.opendf.config.ConfigList;
import net.sf.opendf.config.ConfigString;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class ControlRenderingFactory
{

    public static UpdatableControlIF renderConfig (AbstractConfig config, Composite parent)
    {
        if (config.getType() == AbstractConfig.TYPE_FILE) return renderConfig((ConfigFile)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_STRING) return renderConfig((ConfigString)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_BOOL) return renderConfig((ConfigBoolean)config, parent);
        else if (config.getType() == AbstractConfig.TYPE_LIST) return renderConfig((ConfigList)config, parent);
        return null;
    }
    
    public static UpdatableControlIF renderConfig (ConfigList config, Composite parent)
    {
        final ConfigList configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName() + " (enter one per line)");
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );
        final Text textBox = new Text( group, SWT.LEFT | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        textBox.setEnabled(true);
        textBox.setToolTipText(config.getDescription());
        textBox.setLayoutData(new GridData( GridData.FILL_HORIZONTAL));

        final ModifyListener modListener = new ModifyListener() {
            public void modifyText (ModifyEvent e) {
                if (textBox.getText() == null || textBox.getText().length() == 0) {
                    configHandle.unset();
                } else {
                    StringTokenizer st = new StringTokenizer(textBox.getText(), Text.DELIMITER);
                    List list = new ArrayList();
                    while (st.hasMoreTokens())
                        list.add(st.nextToken());
                    configHandle.setValue(list, true);
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

        addFocusListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);
        
        return cif;
    }
    
    public static UpdatableControlIF renderConfig (ConfigFile config, Composite parent)
    {
        final ConfigFile configHandle = config;
        
        final Group group = new Group(parent, SWT.SHADOW_IN);
        group.setText(configHandle.getName());
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group.setLayout( new GridLayout( 1, false ) );
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
                textBox.setText(configHandle.getValue()); 
                textBox.addModifyListener(modListener);
                };
        };
        
        cif.updateValue();
        
        addFocusListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);

        return cif;
    }
    
    public static UpdatableControlIF renderConfigFileSelect (ConfigFile config, Composite parent)
    {
        final ConfigFile configHandle = config;
        
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
                if (configHandle.getType() == AbstractConfig.TYPE_DIR)
                    textBox.setText(configHandle.getValue());
                else
                    textBox.setText(configHandle.getValueFile().getName()); 
                textBox.addModifyListener(modListener);
                };
        };
        cif.updateValue();
        
        addFocusListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);

        final Button browse = new Button(group, SWT.PUSH | SWT.CENTER);
        browse.setText("Browse");
        browse.setToolTipText("Browse file system...");
        browse.addSelectionListener(
                new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {}
                    public void widgetSelected(SelectionEvent e) {
                        File selected = configHandle.getValueFile();
                        System.out.println("The selected file is " + selected);
                        String selectedName = selected.getName();
                        String absoluteFile = null;
                        
                        if (configHandle.getType() == AbstractConfig.TYPE_FILE)
                        {
                            FileDialog fileSelector = new FileDialog(group.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );
                            if (configHandle.isUserSpecified())
                            {
                                fileSelector.setFileName(selectedName);
                                fileSelector.setFilterPath(selected.getAbsoluteFile().getParent());
                                String preferredExt = null;
                                // Only set a preferred extension if it is a valid selection
                                if (configHandle.validate() && selectedName.lastIndexOf('.') > 0) 
                                    preferredExt = "*" + selectedName.substring(selectedName.lastIndexOf('.'));
                                setDialogExtensions(fileSelector, configHandle, preferredExt);
                            } else {
                                setDialogExtensions(fileSelector, configHandle, null);
                            }
                            absoluteFile = fileSelector.open();
                        } else {
                            DirectoryDialog dirDialog = new DirectoryDialog(group.getShell());
                            if (configHandle.isUserSpecified())
                            {
                                dirDialog.setFilterPath(selected.getAbsoluteFile().getParent());
                            }
                            absoluteFile = dirDialog.open();
                        }
                        if (absoluteFile != null) // null if 'cancel' from dialog
                        {
                            configHandle.setValue(absoluteFile, true);
                            cif.updateValue();
                            cif.modificationNotify(ConfigModificationListener.TEXT_MODIFICATION);
                        }
                    } } );
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
        
        addFocusListener(select, cif, ConfigModificationListener.BUTTON_SELECTION);

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
        
        addFocusListener(textBox, cif, ConfigModificationListener.TEXT_MODIFICATION);

        /*
        textBox.addVerifyListener(
                new VerifyListener(){
                    public void verifyText (VerifyEvent e){}
                });
        */
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
    private static void addFocusListener (Control control, ConfigUpdatableControl cif, int type)
    {
        final ConfigUpdatableControl cifHandle = cif;
        final int typeHandle = type;
        control.addFocusListener(new FocusListener(){
            public void focusGained (FocusEvent e){}
            public void focusLost (FocusEvent e){
                cifHandle.modificationNotify(typeHandle);
            }});
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
