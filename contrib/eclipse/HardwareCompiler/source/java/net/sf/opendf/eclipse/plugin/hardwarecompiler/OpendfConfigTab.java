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
package net.sf.opendf.eclipse.plugin.hardwarecompiler;

import net.sf.opendf.config.AbstractConfig;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.eclipse.plugin.config.ConfigModificationListener;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.UpdatableControlIF;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.*;

public abstract class OpendfConfigTab extends AbstractLaunchConfigurationTab
{
    /**
     * This instance of the ConfigGroup is used ONLY for obtaining the set 
     * of configuration options and storing default/current values.  This group
     * is not actually used for configuring synthesis, instead the data is passed 
     * via the launchconfiguration to the synthesis process.  The correlation of 
     * these two ConfigGroup instances is ensured by their common use of the 
     * ConfigGroup push/updateConfig methods via the UpdateConfigWrapper. 
     */
    private final ConfigGroup configs;
    private final Map<String, UpdatableControlIF> controls = new LinkedHashMap<String, UpdatableControlIF>();

    public OpendfConfigTab ()
    {
        super();
        this.configs = new ConfigGroup();
        setDefaultConfigs(configs);
    }

    /**
     * A listener that is used to update the tab on any change to a control in the tab.  This 
     * allows revert and apply buttons to be set accordingly.
     */
    private final ConfigModificationListener modifyListener = new ConfigModificationListener(){
        public void registerModification (int type) {
            OpendfConfigTab.this.updateLaunchConfigurationDialog();
        } 
    };

    /**
     * This method should be overriden by any implementing class which requires non
     * standard default configuration values. 
     * @param configs
     */
    protected void setDefaultConfigs (ConfigGroup configs)
    {
        // Any non standard default settings for the configs should go here.
    }
    
    protected void addControl (String key, UpdatableControlIF control)
    {
        this.controls.put(key, control);
        // Register a listener to detect when a control has changed and cause the tab status 
        // to be updated.  This will allow the "apply" and "revert" buttons to work as expected
        control.addModifyListener(modifyListener);
    }
    
    protected ConfigGroup getConfigs  ()
    {
        return this.configs;
    }
    
    protected Button getDefaultButton (Composite parent)
    {
        final Button defaults = new Button(parent, SWT.PUSH | SWT.CENTER); 
        defaults.setText("set defaults");
        defaults.setToolTipText("Restore default values for configuration values.");
        
        // Add the listener which will revert all configs to their default values.
        setDefaultConfigs(configs);
        defaults.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e)
            {
                for (String key : OpendfConfigTab.this.controls.keySet())
                {
                    OpendfConfigTab.this.configs.get(key).unset();
                }
                for (UpdatableControlIF control : controls.values())
                    control.updateValue();
                OpendfConfigTab.this.updateLaunchConfigurationDialog();
            }
        });
        return defaults;
    }
    
    /**
     * Declared as final so that the modificationListener created in this 
     * class obtains the correct method. 
     */
    public final void updateLaunchConfigurationDialog ()
    {
        super.updateLaunchConfigurationDialog();
    }

    @Override
    public void initializeFrom (ILaunchConfiguration configuration)
    {
        // First, update the configs.  Then push it to the controls
        this.configs.updateConfig(new ConfigUpdateWrapper(configuration), this.controls.keySet());
        
        for (UpdatableControlIF controlIF : this.controls.values())
        {
            controlIF.updateValue();
        }
    }
    
    @Override
    public void performApply (ILaunchConfigurationWorkingCopy configuration)
    {
        this.configs.pushConfig(new ConfigUpdateWrapper(configuration), this.controls.keySet());
    }
    
    @Override
    public void setDefaults (ILaunchConfigurationWorkingCopy configuration)
    {
        // May be called before the control is created
        this.configs.pushConfig(new ConfigUpdateWrapper(configuration), this.controls.keySet());
    }

    @Override
    public boolean isValid (ILaunchConfiguration config)
    {
        // Test the specified configuration for errors
        if (!super.isValid(config)) return false;

        // Construct a new configuration view for testing
        ConfigGroup group = new ConfigGroup();
        // Set the specified launch config values into the test config
        group.updateConfig(new ConfigUpdateWrapper(config), this.controls.keySet());
        // Canonicalize to ensure that any values are pushed as far as they can go
        group = group.canonicalize();
        // Test for errors
        List<ConfigError> errors = new ArrayList();
        for (String key : this.controls.keySet())
        {
            AbstractConfig cfg = this.configs.get(key);
            if (!cfg.validate())
            {
                errors.addAll(cfg.getErrors());
            }
        }
        
        if (errors.isEmpty())
            setErrorMessage(null);
        else
        {
            for (ConfigError err : errors)
                Logging.dbg().info(err.toString());
            
            String msg = "There " + (errors.size() == 1 ? "is ":"are ") + errors.size() + " error" + 
                    (errors.size() == 1 ? "":"s") + ":  ";
            msg += errors.get(0).getMessage();
            setErrorMessage(msg);
        }
        
        return errors.isEmpty();
    }

    
}
