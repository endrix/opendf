package net.sf.opendf.eclipse.plugin.hardwarecompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.opendf.config.*;
import net.sf.opendf.eclipse.plugin.config.ConfigModificationListener;
import net.sf.opendf.eclipse.plugin.config.ControlRenderingFactory;
import net.sf.opendf.eclipse.plugin.config.UpdatableControlIF;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
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
        //IDM TODO:
            /*
             * Create the -D config and a renderer which takes in values
             * Copy over the calml parsing
             * Update the compilation delegate to use the config values
             */
        // Create a composite in which to put all the controls.  You can organize it however you would like.
        Composite tab = new Composite(parent, SWT.NONE);
        setControl( tab );
        tab.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        tab.setLayout( new GridLayout( 2, false ) );

        // Add the relevant controls for selecting parameters
        // Top model name is derived from the top model file.
        //controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.TOP_MODEL_NAME), tab));
        final UpdatableControlIF topFile = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE), tab); 
        final UpdatableControlIF runDir = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.RUN_DIR), tab); 
        final UpdatableControlIF oFile = ControlRenderingFactory.renderConfigFileSelect((ConfigFile)configs.get(ConfigGroup.OUTPUT_FILE_NAME), tab); 
        controls.add(topFile);
        controls.add(runDir);
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.GEN_HDL_SIM_MODEL), tab));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.CACHE_DIR), tab));
        controls.add(oFile);
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.ACTOR_OUTPUT_DIR), tab));
        controls.add(ControlRenderingFactory.renderConfig(configs.get(ConfigGroup.MODEL_PATH), tab));

        topFile.addModifyListener(new ConfigModificationListener(){
            public void registerModification (int type) {
                // Update the run directory whenever the top file is updated
                AbstractConfig runDirConfig = configs.get(ConfigGroup.RUN_DIR);
                if (!runDirConfig.isUserSpecified())
                    runDirConfig.setValue(((ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE)).getValueFile().getParent(), false);
                runDir.updateValue();
                
                // Update the output file name
                ConfigFile oFileConfig = (ConfigFile)configs.get(ConfigGroup.OUTPUT_FILE_NAME);
                if (!oFileConfig.isUserSpecified())
                {
                    // Convert the file name to the right extension
                    String fileName = ((ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE)).getValueFile().getAbsolutePath();
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String ext = oFileConfig.getFilters().isEmpty() ? "":oFileConfig.getFilters().keySet().iterator().next();
                    fileName += ext.substring(ext.lastIndexOf('.')); 
                    oFileConfig.setValue(fileName, false);
                }
                oFile.updateValue();
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
        try {
            configs.updateConfig(configuration, false);
        }catch (CoreException ce) {
            System.out.println("Could not update configuration due to: " + ce);
        }
        
        for (UpdatableControlIF controlIF : controls)
        {
            controlIF.updateValue();
        }
    }

    @Override
    public void performApply (ILaunchConfigurationWorkingCopy configuration)
    {
        Map<String, AbstractConfig> configMap = configs.getConfigs();
        for (String key : configMap.keySet())
        {
            // Only update the configs with user specified values
            if (!configMap.get(key).isUserSpecified())
                continue;
            
            if (configMap.get(key).getType() == AbstractConfig.TYPE_BOOL) 
                configuration.setAttribute(key, ((ConfigBoolean)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_FILE) 
                configuration.setAttribute(key, ((ConfigFile)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_DIR) 
                configuration.setAttribute(key, ((ConfigFile.Dir)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_STRING) 
                configuration.setAttribute(key, ((ConfigString)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_INT)
                configuration.setAttribute(key, ((ConfigInt)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_LIST) 
                configuration.setAttribute(key, ((ConfigList)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_MAP) 
                configuration.setAttribute(key, ((ConfigMap)configMap.get(key)).getValue());
            else if (configMap.get(key).getType() == AbstractConfig.TYPE_SET) 
                // The configuration does not have a Set attribute.  Repack it as a list
                configuration.setAttribute(key, new ArrayList(((ConfigSet)configMap.get(key)).getValue()));
            else
                throw new RuntimeException("Unknown configuration type " + configMap.get(key).getType() + " for " + key);
        }
    }

    @Override
    public void setDefaults (ILaunchConfigurationWorkingCopy configuration)
    {
        // May be called before the control is created
        // TODO Auto-generated method stub
    }

}
