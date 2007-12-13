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

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;

public class BrowseButton

{
  private static final String LABEL_BROWSE        = "Browse...";
  private static final String LABEL_MODELDIALOG   = "Select Top Level Model";

  private static final int PUSHBUTTON_WIDTH = 100;
  
  private String[] filters;
  private String[] filterNames;
   
  private OpendfConfigurationTab tab;
  private String fileKey;
  private String dirKey;
  private Composite parent;  
  
  public BrowseButton( OpendfConfigurationTab t, Composite par, String fKey, String dKey, String[] f, String[] fn )
  {        
    tab = t;
    fileKey = fKey;
    dirKey  = dKey;
    
    parent = par;
    
    filters = f;
    filterNames = fn;

    Button browseButton = new Button( parent, SWT.PUSH );
    browseButton.setText( LABEL_BROWSE );
    browseButton.setEnabled( true );
    GridData gd = new GridData();
    gd.widthHint = PUSHBUTTON_WIDTH;
    browseButton.setLayoutData( gd );
    browseButton.addSelectionListener
    (
      new SelectionAdapter()
      {
        public void widgetSelected( SelectionEvent e )
        {
          String file = tab.getProperty( fileKey );
            
          FileDialog dialog = new FileDialog( parent.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );
          dialog.setText( LABEL_MODELDIALOG );
          dialog.setFilterPath( tab.getProperty( dirKey ) );
          dialog.setFileName( file ); // null OK
            
          // Shuffle the filters so we start where we left off last time
          String[] f_shuffle = new String[ filters.length ];
          String[] fn_shuffle = new String[ filters.length ];
          
          // The default will be to look for the first extension, if no file was found before
          // If the extension does not match any on the list, use the last one (usually *.*)
          int found = 0;
          while( file != null && found != filters.length - 1 )
            if( file.endsWith( filters[found].substring( filters[found].indexOf( '.' ) ) ) )
              break;
          
          f_shuffle [0] = filters[found];
          fn_shuffle[0] = filterNames[found];
          int i = 1;
          
          for( int j = 0; j < filters.length; j ++ )
            if( j != found )
            {
              f_shuffle [i] = filters[j];
              fn_shuffle[i] = filterNames[j];
              i ++;
            }
          
          dialog.setFilterExtensions( f_shuffle );
          dialog.setFilterNames( fn_shuffle );

          file = dialog.open() ;
              
          if( file != null )
          {
            String dir  = dialog.getFilterPath();
            int dl = dir.length() + 1;
              
            // Get the base filename
            if( file.length() > dl ) file = file.substring( dl );
            else dir = "";    // ??
                
            tab.setProperty( fileKey, file );
            tab.setProperty( dirKey , dir );

            // The affected widgets are not updated here. One of the tab life-cycle methods
            // will read the property map and do the right thing.
            tab.updateLaunchConfigurationDialog();
          }
        }
      }
    );
  }
}
