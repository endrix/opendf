package net.sf.opendf.eclipse.plugin.simulators.tabs;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
// import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

import net.sf.opendf.eclipse.plugin.*;
import java.util.*;

public abstract class OpendfConfigurationTab extends AbstractLaunchConfigurationTab
{
  private String name;
  
  private Map<String,String> properties;
 // private Map<String,String> defaults;
  
  public OpendfConfigurationTab( String name /* , Map<String,String> defaultProperties */ )
  {
    super();
    this.name  = name;
    properties = new HashMap<String,String> ();
   // defaults   = defaultProperties;
  }
  
  public static String valueReplacement = "$value$";
  
  public static String valueSuggestion( String name, String type )
  {
    // scalar value suggestion
    if( type == null || ! type.contains( "type:" ) ) return valueReplacement;
    
    // list value suggestion
    return  "[ " + valueReplacement + " : for i in 1 .. #" + name + "]";
  }
  
  protected void setProperty( String key, String value )
  {
    if( value != null && value.length() == 0 )
      properties.put( key, null );
    else
      properties.put( key, value );
  }

  protected String getProperty( String key )
  {
    return properties.get( key );
  }

  // Remove all keys matching a qualifier that do not appear in a keep list.
  protected void pruneProperties( String qualifier, List<String> keep )
  {
    String dottedQualifier = qualifier + ".";
    int qualifierLength = dottedQualifier.length();
    Iterator<String> i = properties.keySet().iterator();
    
    while( i.hasNext() )
    {
      String key = i.next();
      if( key.startsWith( dottedQualifier ) && ! keep.contains( key.substring( qualifierLength ) ) )
      {
        i.remove();
      }
    }
  }
  
  public String getName()
  {
    return name;
  }
  
  public void initializeFrom(ILaunchConfiguration configuration) {
    // TODO Auto-generated method stub

  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    // TODO Auto-generated method stub

  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    // TODO Auto-generated method stub

  }
  
  public void setErrorMessage( String message )
  {
    super.setErrorMessage( message );
    updateLaunchConfigurationDialog();
  }
  
}
