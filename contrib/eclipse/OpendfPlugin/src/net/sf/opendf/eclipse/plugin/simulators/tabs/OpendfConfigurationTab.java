/* 
BEGINCOPYRIGHT X
  
  Copyright (c) 2007, Xilinx Inc.
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

package net.sf.opendf.eclipse.plugin.simulators.tabs;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;


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
  
  public Image errorImage;
  
  private String uniqueId;
  
  public OpendfConfigurationTab( String name )
  {
    super();
    this.name  = name;

    properties = new HashMap< String, String >();
    errorImage = OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_error );
    uniqueId = OpendfPlugin.ID;
  }
  
  public static String valueReplacement = "$value$";
  
  public static String valueSuggestion( String name, String type )
  {
    // scalar value suggestion
    if( type == null || ! type.contains( "type:" ) ) return valueReplacement;
    
    // list value suggestion
    return  "[ " + valueReplacement + " : for i in 1 .. #" + name + "]";
  }
  
  public void setProperty( String key, String value )
  {
    assert( properties != null );
    if( value != null && value.length() == 0 )
      properties.put( key, null );
    else
      properties.put( key, value );
  }

  public void setProperty( String qualifier, String name, String value )
  {
    setProperty( qualifier + "." + name, value );
  }

  public void touchProperty( String qualifier, String name )
  {
    String fullKey = qualifier + "." + name;
    
    if(! properties.containsKey( fullKey ) )
      setProperty( fullKey, null );
  }

  public String getProperty( String key )
  {
    assert( properties != null );
    
    return properties.get( key );
  }
  
  public String getProperty( String qualifier, String name )
  {
    return getProperty( qualifier + "." + name );
  }

  // Remove all keys matching a qualifier that do not appear in a keep list.
  public void pruneProperties( String qualifier, List<String> keep )
  {
    assert( properties != null );

    String dottedQualifier = qualifier + ".";
    int qualifierLength = dottedQualifier.length();
    Iterator<String> i = properties.keySet().iterator();
    
    while( i.hasNext() )
    {
      String key = i.next();
      if( ! key.startsWith( dottedQualifier ) ) continue;
      
      if( keep == null || ! keep.contains( key.substring( qualifierLength ) ) )
        i.remove();
    }
  }

  public void updateLaunchConfigurationDialog()
  {
    super.updateLaunchConfigurationDialog();
  }
    
  // Return all keys matching a qualifier
  public java.util.List<String> getKeys( String qualifier )
  {
    assert( properties != null );

    java.util.List<String> keys = new java.util.ArrayList<String>();
 
    Iterator<String> i = properties.keySet().iterator();

    String dottedQualifier = qualifier + ".";

    while( i.hasNext() )
    {
      String key = i.next();
      if( key.startsWith( dottedQualifier ) )
        keys.add( key.substring( dottedQualifier.length() ) );
    }

    Collections.sort( keys, 
        new Comparator<String>()
        {
          public int compare( String a, String b )
          {
            return a.compareTo( b );
          }
        }
    );

    return keys;
  }
  
  public String getName()
  {
    return name;
  }

  public String export()
  {
    return uniqueId;
  }
  
  public String export( String key )
  {
    return uniqueId + "." + name + "." + key;
  }

  
  public String export( String tabName, String key )
  {
    return uniqueId + "." + tabName + "." + key;
  }

}
