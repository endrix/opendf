/* 
BEGINCOPYRIGHT X

  Copyright (c) 2007-2008, Xilinx Inc.
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
package net.sf.opendf.eclipse.plugin.launcher.tabs;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import net.sf.opendf.eclipse.plugin.config.*;
import net.sf.opendf.eclipse.plugin.config.TopModelParamParse.ModelParameter;
import net.sf.opendf.util.logging.Logging;
import java.util.*;

import net.sf.opendf.config.*;

public class SimulationModelTab extends OpendfConfigTab
{
    public SimulationModelTab()
    {
        super(new SimulationConfigGroup());
    }

    public String getName ()
    {
        return "Model";
    }

    public void createControl( Composite parent )
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

        Composite buttons = new Composite(tab, SWT.NONE);
        buttons.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_CENTER ) );
        buttons.setLayout( new GridLayout( 3, false ) );

        setControl( tabScroller );
        tabScroller.setContent(tab);

        // Add the relevant controls for selecting parameters
        final UpdatableControlIF topFile = ControlRenderingFactory.fileSelectButton(buttons, "Set top model from file selection", false, (ConfigFile)getConfigs().get(ConfigGroup.TOP_MODEL_FILE));
        final Button defaults = this.getDefaultButton(buttons);
        final Button plugins = PluginButton.getButton(parent, buttons);

        final Composite group2 = new Composite(tab, SWT.SHADOW_IN);
        group2.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        group2.setLayout( new GridLayout( 2, true ) );

        final Composite leftCol = new Composite(group2, SWT.NONE);
        leftCol.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        leftCol.setLayout( new GridLayout( 1, true ) );

        final Composite rightCol = new Composite(group2, SWT.NONE);
        rightCol.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        rightCol.setLayout( new GridLayout( 1, true ) );

        final UpdatableControlIF runDir = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)getConfigs().get(ConfigGroup.RUN_DIR), leftCol, true, true); 
        final UpdatableControlIF topName = ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.TOP_MODEL_NAME), leftCol); 
        addControl(ConfigGroup.CACHE_DIR, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.CACHE_DIR), leftCol));

        final Composite leftColShort = new Composite(leftCol, SWT.NONE);
        leftColShort.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        leftColShort.setLayout( new GridLayout( 2, true ) );
        addControl(ConfigGroup.SIM_TIME, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_TIME), leftColShort));
        addControl(ConfigGroup.SIM_STEPS, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_STEPS), leftColShort));
        addControl(ConfigGroup.SIM_MAX_ERRORS, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_MAX_ERRORS), leftColShort));
        addControl(ConfigGroup.SIM_BUFFER_SIZE_WARNING, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_BUFFER_SIZE_WARNING), leftColShort));
        
        addControl(ConfigGroup.ELABORATE_TOP, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.ELABORATE_TOP), leftColShort));
        addControl(ConfigGroup.ENABLE_ASSERTIONS, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.ENABLE_ASSERTIONS), leftColShort));
        addControl(ConfigGroup.SIM_BUFFER_IGNORE, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_BUFFER_IGNORE), leftColShort));
        addControl(ConfigGroup.SIM_BUFFER_RECORD, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_BUFFER_RECORD), leftColShort));
        addControl(ConfigGroup.SIM_TRACE, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_TRACE), leftColShort));
        addControl(ConfigGroup.SIM_TYPE_CHECK, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.SIM_TYPE_CHECK), leftColShort));
        
        addControl(ConfigGroup.SIM_INPUT_FILE, ControlRenderingFactory.renderConfigFileSelect((ConfigFile)getConfigs().get(ConfigGroup.SIM_INPUT_FILE), rightCol, true, true));
        addControl(ConfigGroup.SIM_OUTPUT_FILE, ControlRenderingFactory.renderConfigFileSelect((ConfigFile)getConfigs().get(ConfigGroup.SIM_OUTPUT_FILE), rightCol, true, true));
        final UpdatableControlIF modelPath = ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.MODEL_PATH), rightCol); 
        final UpdatableControlIF modelParams = ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.TOP_MODEL_PARAMS), rightCol); 
        addControl(ConfigGroup.MESSAGE_SUPPRESS_IDS, ControlRenderingFactory.renderConfig((ConfigList)getConfigs().get(ConfigGroup.MESSAGE_SUPPRESS_IDS), rightCol));

        addControl(ConfigGroup.MODEL_PATH, modelPath);
        addControl(ConfigGroup.RUN_DIR, runDir);
        addControl(ConfigGroup.TOP_MODEL_NAME, topName);
        addControl(ConfigGroup.TOP_MODEL_PARAMS, modelParams);

        // If the user uses the button to set the model by file selection, overide any values in 
        // the relevent fields
        topFile.addModifyListener(new ConfigModificationListener(){
            public void registerModification (int type) {                
                // Set to false to only update in case of no user setting.
                final boolean forceUpdate = true;

                ConfigFile topFileConfig = (ConfigFile)getConfigs().get(ConfigGroup.TOP_MODEL_FILE);
                // In case of spurious events ignore them (possible?)
                if (!topFileConfig.isUserSpecified())
                    return;

                // Update the run directory whenever the top file is updated
                ConfigFile runDirConfig = (ConfigFile)getConfigs().get(ConfigGroup.RUN_DIR);
                if (forceUpdate || !runDirConfig.isUserSpecified())
                    runDirConfig.setValue(topFileConfig.getValueFile().getParent(), true);
                runDir.updateValue();

                // Update the top level model name
                ConfigString topNameConfig = (ConfigString)getConfigs().get(ConfigGroup.TOP_MODEL_NAME);
                if (forceUpdate || !topNameConfig.isUserSpecified())
                {
                    String name = topFileConfig.getValueFile().getName();
                    name = name.indexOf('.') > 0 ? name.substring(0, name.lastIndexOf('.')):name;
                    topNameConfig.setValue(name, true);
                }
                topName.updateValue();

                // Ensure that the model path contains the run directory
                ConfigList modelPathConfig = (ConfigList)getConfigs().get(ConfigGroup.MODEL_PATH);
                if (!modelPathConfig.getValue().contains(runDirConfig.getValue()))
                {
                    // The contract for setValue on collections is to append
                    modelPathConfig.addValue(Collections.singletonList(runDirConfig.getValue()), modelPathConfig.isUserSpecified());
                }
                modelPath.updateValue();

                // Update the model parameters
                ConfigMap paramsConfig = (ConfigMap)getConfigs().get(ConfigGroup.TOP_MODEL_PARAMS);
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

        // The topFile control must be added AFTER the above modificationListener is added
        // so that the changes instituted there will be reflected in the call to updateLaunchConfigurationDialog().
        addControl(ConfigGroup.TOP_MODEL_FILE, topFile);

        tabScroller.setMinSize(tab.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

}
