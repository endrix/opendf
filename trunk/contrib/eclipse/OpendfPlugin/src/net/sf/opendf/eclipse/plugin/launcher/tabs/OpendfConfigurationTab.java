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

package net.sf.opendf.eclipse.plugin.launcher.tabs;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import net.sf.opendf.eclipse.plugin.*;

import java.util.*;

public abstract class OpendfConfigurationTab extends AbstractLaunchConfigurationTab
{
  private String name;
  
  private Map<String,String> properties;
  
  public Image errorImage;
  
  private static String uniqueId()
  {
    return OpendfConstants.ID_PLUGIN;
  }

  public static String Export( String tabName, String key )
  {
    return uniqueId() + "." + tabName + "." + key;
  }
  
  public OpendfConfigurationTab( String name )
  {
    super();
    this.name  = name;

    properties = new HashMap< String, String >();
    errorImage = OpendfPlugin.getDefault().getImage( OpendfPlugin.IMAGE_error );
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

  // promote to public so that controls can do this
  public void updateLaunchConfigurationDialog()
  {
    super.updateLaunchConfigurationDialog();
  }

  // Return all keys matching a qualifier
  public java.util.Set<String> getKeys( )
  {
    assert( properties != null );

    return properties.keySet();
  }

  // Return all keys matching a qualifier
  public void clearKeys()
  {
    assert( properties != null );

    properties.clear();
  }
  
  // Return all keys matching a qualifier
  public java.util.List<String> getKeys( String qualifier )
  {
    java.util.List<String> keys = new java.util.ArrayList<String>();
    String dottedQualifier = qualifier + ".";

    for( String key : getKeys() )
    {
      if( key.startsWith( dottedQualifier ) )
        keys.add( key.substring( dottedQualifier.length() ) );
    }

    return keys;
  }
  
  public String getName()
  {
    return name;
  }

  // Base key for all items managed by this tab 
  public String export()
  {
    return uniqueId() + "." + name;
  }
  
  // Fully qualified key
  public String export( String key )
  {
    return export() + "." + key;
  }
  
  public void clearConfiguration( ILaunchConfigurationWorkingCopy conf )
  {
    // null out this tab's keys
    try
    {
      String qualifier = export() + ".";
      for( Object obj : conf.getAttributes().keySet() )
      {
        String key = (String) obj;
        if( key.startsWith( qualifier ) )
          conf.setAttribute( key, (String) null );
      }
    }
    catch( CoreException e )
    { 
      OpendfPlugin.logErrorMessage( "Exception in setDefaults() for tab " + name, e );
    }
  }
  
  // called potentially before controls exist
  public void setDefaults( ILaunchConfigurationWorkingCopy conf )
  {
    clearConfiguration( conf );
  }
  
  // this is called to set the tab the first time
  public void initializeFrom( ILaunchConfiguration conf )
  {
    clearKeys();
    
    // import all relevant attributes
    try
    {
      String qualifier = export() + ".";
      for( Object obj : conf.getAttributes().keySet() )
      {
        String key = (String) obj;
        if( key.startsWith( qualifier ) )
        {
          setProperty( key.substring( qualifier.length() ), 
                       conf.getAttribute( key, (String) null ) );
          // System.out.println("importing " + key + " = " + conf.getAttribute( key, (String) null ) );
        }
      }
    }
    catch( CoreException e )
    { 
      OpendfPlugin.logErrorMessage( "Exception in initializeFrom() for tab " + name, e );
    }
  }

  // this is called for every update
  public void performApply( ILaunchConfigurationWorkingCopy conf )
  {
    // export all attributes
    for( String key: getKeys() )
    {
      conf.setAttribute( export( key ), getProperty( key ) );
    }
  }
}
