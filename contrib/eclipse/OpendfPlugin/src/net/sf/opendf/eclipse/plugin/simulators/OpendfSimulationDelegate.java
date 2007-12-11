package net.sf.opendf.eclipse.plugin.simulators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

public class OpendfSimulationDelegate implements ILaunchConfigurationDelegate {

  public void launch(ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor) throws CoreException 
  {
    // TODO Auto-generated method stub
    System.out.println("OpendfSimulationDelegate");
  }

}
