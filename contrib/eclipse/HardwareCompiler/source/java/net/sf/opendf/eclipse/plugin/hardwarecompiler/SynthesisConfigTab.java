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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.opendf.config.*;
import net.sf.opendf.eclipse.plugin.config.ConfigModificationListener;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.ControlRenderingFactory;
import net.sf.opendf.eclipse.plugin.config.UpdatableControlIF;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;

public class SynthesisConfigTab extends AbstractLaunchConfigurationTab
{
    /**
     * This instance of the ConfigGroup is used ONLY for obtaining the set 
     * of configuration options and storing default/current values.  This group
     * is not actually used for configuring synthesis, instead the data is passed 
     * via the launchconfiguration to the synthesis process.
     */
    private ConfigGroup configs;
    private List<UpdatableControlIF> controls = new ArrayList<UpdatableControlIF>();
    
    public SynthesisConfigTab()
    {
        super();
        this.configs = new ConfigGroup();
    }

    @Override
    public void createControl (Composite parent)
    {
        //IDM TODO:
            /*
             * Create the -D config and a renderer which takes in values
             * Copy over the calml parsing
             */
        // Create a scrolling composite in which to put all the controls.  You can organize it however you would like.
        ScrolledComposite tabScroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        tabScroller.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        tabScroller.setLayout( new GridLayout( 1, false ) );
        tabScroller.setExpandHorizontal(true);
        tabScroller.setExpandVertical(true);
        Composite tab = new Composite(tabScroller, SWT.NONE);
        tab.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        tab.setLayout( new GridLayout( 1, false ) );
        
        setControl( tabScroller );
        tabScroller.setContent(tab);

        final Composite group1 = new Composite(tab, SWT.SHADOW_IN);
        group1.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group1.setLayout( new GridLayout( 1, false ) );
        final Composite group2 = new Composite(tab, SWT.SHADOW_IN);
        //group2.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group2.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        group2.setLayout( new GridLayout( 2, true ) );

        // Add the relevant controls for selecting parameters
        final UpdatableControlIF topFile = ControlRenderingFactory.fileSelectButton(group1, "Set top model from file selection", false, (ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE));
        
        final UpdatableControlIF runDir = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.RUN_DIR), group2, true, true); 
        final UpdatableControlIF modelPath = ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.MODEL_PATH), group2); 
        final UpdatableControlIF topName = ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.TOP_MODEL_NAME), group2); 
        final UpdatableControlIF oFile = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.OUTPUT_FILE), group2, false, true); 
        
        controls.add(modelPath);
        controls.add(runDir);
        controls.add(oFile);
        controls.add(topName);
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.CACHE_DIR), group2));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.ACTOR_OUTPUT_DIR), group2));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.GEN_HDL_SIM_MODEL), group2));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.ENABLE_ASSERTIONS), group2));

        // If the user uses the button to set the model by file selection, overide any values in 
        // the relevent fields
        topFile.addModifyListener(new ConfigModificationListener(){
            public void registerModification (int type) {
                                // Set to false to only update in case of no user setting.
                final boolean forceUpdate = true;
                
                ConfigFile topFileConfig = (ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE);
                // In case of spurious events ignore them (possible?)
                if (!topFileConfig.isUserSpecified())
                    return;
                
                // Update the run directory whenever the top file is updated
                AbstractConfig runDirConfig = configs.get(ConfigGroup.RUN_DIR);
                if (forceUpdate || !runDirConfig.isUserSpecified())
                    runDirConfig.setValue(topFileConfig.getValueFile().getParent(), true);
                runDir.updateValue();

                AbstractConfig topNameConfig = configs.get(ConfigGroup.TOP_MODEL_NAME);
                if (forceUpdate || !topNameConfig.isUserSpecified())
                {
                    String name = topFileConfig.getValueFile().getName();
                    name = name.indexOf('.') > 0 ? name.substring(0, name.lastIndexOf('.')):name;
                    topNameConfig.setValue(name, true);
                }
                topName.updateValue();
                
                // Update the output file name
                ConfigFile oFileConfig = (ConfigFile)configs.get(ConfigGroup.OUTPUT_FILE);
                if (forceUpdate || !oFileConfig.isUserSpecified())
                {
                    // Convert the file name to the right extension
                    String fileName = topFileConfig.getValueFile().getName();
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String ext = oFileConfig.getFilters().isEmpty() ? "":oFileConfig.getFilters().keySet().iterator().next();
                    fileName += ext.substring(ext.lastIndexOf('.')); 
                    oFileConfig.setValue(fileName, true);
                }
                oFile.updateValue();
                
                // Ensure that the model path contains '.'
                ConfigList modelPathConfig = (ConfigList)configs.get(ConfigGroup.MODEL_PATH);
                if (!modelPathConfig.getValue().contains("."))
                {
                    // The contract for setValue on collections is to append
                    modelPathConfig.addValue(Collections.singletonList("."), modelPathConfig.isUserSpecified());
                }
                modelPath.updateValue();
            }
        });
        
        // Register a listener to detect when a control has changed and cause the tab status 
        // to be updated.  This will allow the "apply" and "revert" buttons to work as expected
        ConfigModificationListener modifyListener = new ConfigModificationListener(){
            public void registerModification (int type) {
                SynthesisConfigTab.this.updateLaunchConfigurationDialog();
            } };
        for (UpdatableControlIF control : controls)
        {
            control.addModifyListener(modifyListener);
        }

        tabScroller.setMinSize(tab.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    @Override
    public String getName ()
    {
        return "Synthesis";
    }

    @Override
    public void initializeFrom (ILaunchConfiguration configuration)
    {
        // First, update the configs.  Then push it to the controls
        configs.updateConfig(new ConfigUpdateWrapper(configuration));
        
        for (UpdatableControlIF controlIF : controls)
        {
            controlIF.updateValue();
        }
    }

    @Override
    public void performApply (ILaunchConfigurationWorkingCopy configuration)
    {
        configs.pushConfig(new ConfigUpdateWrapper(configuration));
        configs.debug(System.out);
    }

    @Override
    public void setDefaults (ILaunchConfigurationWorkingCopy configuration)
    {
        // May be called before the control is created
        // TODO Auto-generated method stub
    }

}
