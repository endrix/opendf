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

import java.util.*;

import net.sf.opendf.config.*;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.eclipse.plugin.config.ConfigModificationListener;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.ControlRenderingFactory;
import net.sf.opendf.eclipse.plugin.config.TopModelParamParse;
import net.sf.opendf.eclipse.plugin.config.UpdatableControlIF;
import net.sf.opendf.eclipse.plugin.config.TopModelParamParse.ModelParameter;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;


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

        // Add the relevant controls for selecting parameters
        final UpdatableControlIF topFile = ControlRenderingFactory.fileSelectButton(tab, "Set top model from file selection", false, (ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE));
        
        final Composite group2 = new Composite(tab, SWT.SHADOW_IN);
        //group2.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        group2.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        group2.setLayout( new GridLayout( 2, true ) );
        
        final Composite leftCol = new Composite(group2, SWT.NONE);
        leftCol.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        leftCol.setLayout( new GridLayout( 1, true ) );

        final Composite rightCol = new Composite(group2, SWT.NONE);
        rightCol.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        rightCol.setLayout( new GridLayout( 1, true ) );

        final UpdatableControlIF runDir = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.RUN_DIR), leftCol, true, true); 
        final UpdatableControlIF topName = ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.TOP_MODEL_NAME), leftCol); 
        final UpdatableControlIF oFile = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.OUTPUT_FILE), leftCol, false, true); 
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.ACTOR_OUTPUT_DIR), leftCol));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.CACHE_DIR), leftCol));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.GEN_HDL_SIM_MODEL), leftCol));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.ENABLE_ASSERTIONS), leftCol));

        final UpdatableControlIF modelPath = ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.MODEL_PATH), rightCol); 
        final UpdatableControlIF modelParams = ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.TOP_MODEL_PARAMS), rightCol); 
        
        controls.add(topFile);
        controls.add(modelPath);
        controls.add(runDir);
        controls.add(oFile);
        controls.add(topName);
        controls.add(modelParams);

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
                ConfigFile runDirConfig = (ConfigFile)configs.get(ConfigGroup.RUN_DIR);
                if (forceUpdate || !runDirConfig.isUserSpecified())
                    runDirConfig.setValue(topFileConfig.getValueFile().getParent(), true);
                runDir.updateValue();

                // Update the top level model name
                ConfigString topNameConfig = (ConfigString)configs.get(ConfigGroup.TOP_MODEL_NAME);
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
                
                // Ensure that the model path contains the run directory
                ConfigList modelPathConfig = (ConfigList)configs.get(ConfigGroup.MODEL_PATH);
                if (!modelPathConfig.getValue().contains(runDirConfig.getValue()))
                {
                    // The contract for setValue on collections is to append
                    modelPathConfig.addValue(Collections.singletonList(runDirConfig.getValue()), modelPathConfig.isUserSpecified());
                }
                modelPath.updateValue();
                
                // Update the model parameters
                ConfigMap paramsConfig = (ConfigMap)configs.get(ConfigGroup.TOP_MODEL_PARAMS);
                if (forceUpdate || !paramsConfig.isUserSpecified())
                {
                    String[] modelPathArr = (String[])modelPathConfig.getValue().toArray(new String[0]);
                    try {
                        List<TopModelParamParse.ModelParameter> params = TopModelParamParse.parseModel(topNameConfig.getValue(), modelPathArr);
                        Map map = new HashMap();
                        for (ModelParameter mp : params)
                            map.put(mp.getName(), mp.getValue());
                        paramsConfig.setValue(map, false);
                    } catch (TopModelParamParse.ModelAnalysisException exc) {
                        Logging.dbg().severe("Error loading top model " + exc);
                    }
                }
                modelParams.updateValue();
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
        return "HDL Compile";
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
    }
    
    @Override
    public void setDefaults (ILaunchConfigurationWorkingCopy configuration)
    {
        // May be called before the control is created
        configs.pushConfig(new ConfigUpdateWrapper(configuration));
    }

    @Override
    public boolean isValid (ILaunchConfiguration config)
    {
        // Test the specified configuration for errors
        if (!super.isValid(config)) return false;

        // Construct a new configuration view for testing
        ConfigGroup group = new ConfigGroup();
        // Set the specified launch config values into the test config
        group.updateConfig(new ConfigUpdateWrapper(config));
        // Canonicalize to ensure that any values are pushed as far as they can go
        group = group.canonicalize();
        // Test for errors
        List<ConfigError> errors = new ArrayList();
        for (AbstractConfig cfg : group.getConfigs().values())
        {
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
