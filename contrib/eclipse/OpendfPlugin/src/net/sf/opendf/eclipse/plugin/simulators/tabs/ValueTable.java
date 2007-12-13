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

import org.eclipse.swt.widgets.Composite;
import java.util.Comparator;
import java.util.Collections;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.layout.*;

public class ValueTable
{
  private static final String LABEL_NAME          = "Name";
  private static final String LABEL_TYPE          = "Type";
  private static final String LABEL_VALUE         = "Value";
  private static final String LABEL_EDIT          = "Edit...";

  private static final int PUSHBUTTON_WIDTH = 100;
  private static final int NAME_INDEX  = 0;
  private static final int TYPE_INDEX  = 1;
  private static final int VALUE_INDEX = 2;

  private static final String MSG_MISSINGPARAMS = "Missing model parameter values";
 
  private Button editButton;
  private Table table;
  
  private OpendfConfigurationTab tab;
  private String nameKey;
  private String typeKey;
  private String errorKey;
 
  private static final String[] columnLabels = { LABEL_NAME, LABEL_TYPE, LABEL_VALUE };

  public ValueTable( OpendfConfigurationTab t, Composite parent, String label, String nKey, String tKey, String eKey )
  {    
    tab = t;
    nameKey = nKey;
    typeKey = tKey;
    errorKey = eKey;
    
    Group group = new Group( parent, SWT.NONE );
    group.setText( label );
    group.setLayoutData( new GridData( GridData.FILL_BOTH ) );
    group.setLayout( new GridLayout( 2, false ) );

    Composite tableHolder = new Composite( group, SWT.NONE );
    tableHolder.setLayoutData( new GridData( GridData.FILL_BOTH ) );
    TableColumnLayout columnLayout = new TableColumnLayout();
      
    table = new Table( tableHolder, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION );
    table.setLinesVisible( false );
    table.setHeaderVisible( true );
      
    TableColumn[] columns = new TableColumn[ columnLabels.length ];
    for( int i = 0; i < columnLabels.length ; i++ )
    {
      columns[i] = new TableColumn( table, SWT.LEFT, i );
      columns[i].setText( columnLabels[i] );
      columnLayout.setColumnData( columns[i], new ColumnWeightData( i == 2 ? 80 : 10, PUSHBUTTON_WIDTH ) );
    }
    
    tableHolder.setLayout( columnLayout );
       
    // parameter edit button
    editButton = new Button( group, SWT.PUSH );
    GridData buttonData = new GridData( );
    buttonData.widthHint = PUSHBUTTON_WIDTH;
    editButton.setLayoutData( buttonData );
    editButton.setText( LABEL_EDIT );
    editButton.setEnabled( false );
    editButton.addSelectionListener
    ( 
      new SelectionAdapter()
      {
        public void widgetSelected( SelectionEvent e )
        {
          int i = table.getSelectionIndex();
            
          if( i < 0 ) return;
            
          String name = table.getItem( i ).getText( 0 );

          EditValueDialog dialog = new EditValueDialog( editButton.getShell(), tab, name, 
                typeKey, nameKey, nameKey );
            
          String value = (String) dialog.open();
          if( value != null )
          { 
            tab.setProperty( nameKey, name, value );
            tab.updateLaunchConfigurationDialog();
          }
        }
      }
    );
  }

  private void clear()
  {
    table.clearAll();
  }
  
  public void update()
  {
    TableItem[] items = table.getItems();
    String errorMsg = null;
    
    for( int i = 0; i < items.length; i++ )
    {
      String value = tab.getProperty( nameKey, items[i].getText( NAME_INDEX ) );
      if( value != null )
      {
        items[i].setText( VALUE_INDEX, value );
        items[i].setImage( VALUE_INDEX, null );
      }
      else
      {
        items[i].setText( VALUE_INDEX, "" );
        items[i].setImage( VALUE_INDEX, tab.errorImage );
        errorMsg = MSG_MISSINGPARAMS;
      }
    }
    
    tab.setProperty( errorKey, errorMsg );
  }
  
  public void load()
  {
    clear();
    
    java.util.List<String> names = tab.getKeys( nameKey );
    Collections.sort
    ( 
      names, 
      new Comparator<String>() { public int compare( String a, String b ) { return a.compareTo( b ); } }
    );

    for( String name : names )
    {
      String type = tab.getProperty( typeKey, name );
      TableItem item = new TableItem( table, SWT.None );
      item.setText( NAME_INDEX, name );
      if( type != null )
        item.setText( TYPE_INDEX, type );
    }
    
    // fill in any values that exist
    update();
  }
}
