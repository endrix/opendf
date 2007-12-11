package net.sf.opendf.eclipse.plugin.simulators.tabs;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;

public class SimulationTabGroup extends OpendfConfigurationTabGroup
{

  public void createTabs( ILaunchConfigurationDialog dialog, String mode  )
  {
    OpendfConfigurationTab[] tabs =
    {
      new SimulationMainTab()
    };
    
    super.createTabs( dialog, mode, tabs );
  }
  
}
