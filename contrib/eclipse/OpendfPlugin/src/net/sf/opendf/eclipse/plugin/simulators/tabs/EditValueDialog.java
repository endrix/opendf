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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import net.sf.opendf.cal.util.SourceReader;

public class EditValueDialog extends org.eclipse.swt.widgets.Dialog
{
  private Shell parent;
  private OpendfConfigurationTab tab;

  private String variableName;
  private String typeKeyQualifier;
  private String valueKeyQualifier;
  private String environmentKeyQualifier;

  private String returnValue;

  public EditValueDialog( Shell parent, OpendfConfigurationTab tab, String variableName, String typeKeyQualifier, 
                        String valueKeyQualifier, String environmentKeyQualifier )
  {
    super( parent );
    
    this.parent = parent;
    this.tab = tab;
    this.variableName = variableName;
    this.typeKeyQualifier = typeKeyQualifier;
    this.valueKeyQualifier = valueKeyQualifier;
    this.environmentKeyQualifier = environmentKeyQualifier;
  }
    
  private Shell shell;
  private Text nameField;
  private Text valueField;
  private Button accept;
    
  public Object open()
  {
    // Get existing variable info
    String type  = tab.getProperty( typeKeyQualifier , variableName );
    String value = tab.getProperty( valueKeyQualifier, variableName );
      
    boolean validValue = false;
      
    if( value != null )
    {
      try
      {
        if( SourceReader.parseExpr( value ) != null )
          validValue = true;
      }
      catch( Exception e ) {}
    }
      
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
    shell.setText( LABEL_EDITPARAMETER + variableName );
    shell.setLayout( new GridLayout( 1, false ) );
      
    Composite textHolder = new Composite( shell, SWT.NONE );
    textHolder.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    textHolder.setLayout( new GridLayout( 2, false ) );
      
    Text nameLabel = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY );
    nameLabel.setText( LABEL_NAME );
    nameField = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY );
    nameField.setText( variableName );
    nameField.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      
    Text typeLabel = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY );
    typeLabel.setText( LABEL_TYPE );
    Text typeField = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY );
    if( type != null ) typeField.setText( type );
    typeField.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      
    Text valueLabel = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.READ_ONLY );
    valueLabel.setText( LABEL_VALUE );
    valueField = new Text( textHolder, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    if( value == null )
    {
      String suggestion = OpendfConfigurationTab.valueSuggestion( variableName, type );
      int start = suggestion.indexOf( OpendfConfigurationTab.valueReplacement );
      int end = start + OpendfConfigurationTab.valueReplacement.length();
      valueField.setText( suggestion );
      valueField.setSelection( start, end );
    }
    else
    { 
      valueField.setText( value );
      valueField.setSelection(0);
    }
    GridData gd = new GridData( GridData.FILL_HORIZONTAL );
    gd.minimumWidth = 250;
    valueField.setLayoutData( gd );
    valueField.pack();
    valueField.setEnabled( true );
    valueField.setFocus();
    valueField.addModifyListener
    (
      new ModifyListener()
      {
        public void modifyText( ModifyEvent event )
        {
          try
          {
            String value = valueField.getText();
            
            // accept zero-length string, or valid CAL expression
            if( value != null && ( value.length() == 0 || SourceReader.parseExpr( valueField.getText() ) != null ) )
              accept.setEnabled( true );
          }
          catch( Exception e ) { accept.setEnabled( false ); }
        }
      }
    );
    Composite buttonHolder = new Composite( shell, SWT.NONE );
    buttonHolder.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    buttonHolder.setLayout( new GridLayout( 4, false ) );

    if( environmentKeyQualifier == null || tab.getKeys( environmentKeyQualifier ).size() < 2 )
    {
      // No variable inserter requested
      new Text( buttonHolder, SWT.SINGLE | SWT.READ_ONLY );
    }
    else
    {
      Button insert = new Button( buttonHolder, SWT.PUSH );
      GridData insertData = new GridData();
      insertData.widthHint = PUSHBUTTON_WIDTH;
      insert.setLayoutData( insertData );
      insert.setText( LABEL_VARIABLE );
      insert.setEnabled( true );
      insert.addSelectionListener
      ( 
        new SelectionListener()
        {
          public void widgetDefaultSelected( SelectionEvent e ) {}

          public void widgetSelected( SelectionEvent e )
          {
            java.util.List<String> variables = tab.getKeys( environmentKeyQualifier );
            
            VariableInsertionDialog dialog = new VariableInsertionDialog( parent, variables, variableName );

            String name = (String) dialog.open();
            if( name != null && name.length() > 0 )
              valueField.insert( name );
          }
        }
      );
     
    }
    Text spacer = new Text( buttonHolder, SWT.SINGLE | SWT.READ_ONLY ); // spacer
    spacer.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      
    returnValue = null;
      
    accept = new Button( buttonHolder, SWT.PUSH );
    GridData acceptData = new GridData();
    acceptData.widthHint = PUSHBUTTON_WIDTH;
    accept.setLayoutData( acceptData );
    accept.setText( LABEL_APPLY );
    accept.setEnabled( validValue );
    accept.addSelectionListener
    ( 
      new SelectionListener()
      {
        public void widgetDefaultSelected( SelectionEvent e ) {}

        public void widgetSelected( SelectionEvent e )
        {
          returnValue = valueField.getText();
          tab.setProperty( valueKeyQualifier, variableName, returnValue );
          tab.setDirty( true );
          shell.close();
        }
      }
    );

    Button cancel = new Button( buttonHolder, SWT.PUSH );
    GridData cancelData = new GridData();
    cancelData.widthHint = PUSHBUTTON_WIDTH;
    cancel.setLayoutData( cancelData );
    cancel.setText( LABEL_REVERT );
    cancel.setEnabled( true );
    cancel.addSelectionListener
    ( 
      new SelectionListener()
      {
        public void widgetDefaultSelected( SelectionEvent e ) {}
        public void widgetSelected( SelectionEvent e ) { shell.close(); }
      }
    );

    shell.pack();
    shell.open();
    Display display = parent.getDisplay();
    while( !shell.isDisposed() )
    {
      if( !display.readAndDispatch() ) display.sleep();
    }
      
    return returnValue;
  }
    
  private static final int PUSHBUTTON_WIDTH = 100;  
  private static final String LABEL_NAME          = "Name";
  private static final String LABEL_TYPE          = "Type";
  private static final String LABEL_VALUE         = "Value";
  private static final String LABEL_EDITPARAMETER = "Edit Parameter ";
  private static final String LABEL_APPLY         = "Apply";
  private static final String LABEL_REVERT        = "Revert";
  private static final String LABEL_VARIABLE      = "Variable...";
}
