package net.sf.opendf.eclipse.plugin.simulators.tabs;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.swt.widgets.*;
import java.util.*;

public abstract class OpendfConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup
{
  private OpendfConfigurationTab[] tabs;
  
  public void createTabs( ILaunchConfigurationDialog dialog, String mode, OpendfConfigurationTab[] tabs  )
  {
    this.tabs = tabs;
    
    ILaunchConfigurationTab[] launchTabs = new ILaunchConfigurationTab[ tabs.length + 1];
    int i;
    
    for( i = 0; i < tabs.length; i++ )
    {
      launchTabs[ i ] = tabs[ i ];
    };
    
    launchTabs[ i ] =  new CommonTab();
    super.setTabs( launchTabs );
    
  }
  
  private class tabListener implements Listener
  {
    public void handleEvent( Event e )
    {
      SimulationMainTab tab = (SimulationMainTab) e.widget.getData();
      if( tab != null )
        System.out.println( "Event " + e.type + " in tab " + tab.TAB_NAME );
    }
  }
  
}
