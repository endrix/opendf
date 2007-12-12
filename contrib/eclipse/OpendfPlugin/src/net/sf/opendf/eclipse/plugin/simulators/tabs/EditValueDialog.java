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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.layout.*;
import org.eclipse.jface.viewers.ColumnWeightData;

import net.sf.opendf.eclipse.plugin.*;
import net.sf.opendf.util.source.SourceLoader;
import net.sf.opendf.nl.util.NLLoader;
import net.sf.opendf.cal.util.CalLoader;
import net.sf.opendf.util.source.XDFLoader;
import java.io.*;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import static net.sf.opendf.util.xml.Util.xpathEvalElements;
import net.sf.opendf.cal.util.CalWriter;
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
            if( SourceReader.parseExpr( valueField.getText() ) != null )
              accept.setEnabled( true );
          }
          catch( Exception e ) { accept.setEnabled( false ); }
        }
      }
    );
    Composite buttonHolder = new Composite( shell, SWT.NONE );
    buttonHolder.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    buttonHolder.setLayout( new GridLayout( 4, false ) );

    if( environmentKeyQualifier == null )
    {
      // No variable inserter
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
            
            VariableDialog dialog = new VariableDialog( parent, variables );

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
    
  public static final int PUSHBUTTON_WIDTH = 100;  
    
  public static final String LABEL_TOPMODEL      = "Top Level Model:";
  public static final String LABEL_BROWSE        = "Browse...";
  public static final String LABEL_MODELDIALOG   = "Select Top Level Model";
  public static final String LABEL_MODELPATH     = "Model Path:";
  public static final String LABEL_DEFAULT       = "Default";
  public static final String LABEL_SPECIFY       = "Specify";
  public static final String LABEL_PARAMETERS    = "Model Parameters:";
  public static final String LABEL_NAME          = "Name";
  public static final String LABEL_TYPE          = "Type";
  public static final String LABEL_VALUE         = "Value";
  public static final String LABEL_EDIT          = "Edit...";
  public static final String LABEL_PATHSEPARATOR = "path separator is ";
  public static final String LABEL_EDITPARAMETER = "Edit Parameter ";
  public static final String LABEL_APPLY         = "Apply";
  public static final String LABEL_REVERT        = "Revert";
  public static final String LABEL_VARIABLE      = "Variable...";
  public static final String LABEL_INSERTVARIABLE= "Insert Variable Reference";
  public static final String LABEL_INSERT        = "Insert";
  public static final String LABEL_CANCEL        = "Cancel";
  
  private class VariableDialog extends org.eclipse.swt.widgets.Dialog
  {

    private java.util.List<String> variables;
    private Shell parent;
    private Shell shell;
    
    public VariableDialog( Shell parent, java.util.List<String> variables )
    {
      super( parent );
    
      this.parent = parent;
      this.variables = variables;
    }
 
    private String returnValue;
    private List list;
    
    public Object open()
    {
      shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
      shell.setText( LABEL_INSERTVARIABLE );
      GridData shellData = new GridData();
      shellData.widthHint = 250;
      shell.setLayoutData( shellData );
      shell.setLayout( new GridLayout( 1, false ) );
      
      list = new List( shell, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER );
      list.setLayoutData( new GridData( GridData.FILL_BOTH ) );
      for( String name : variables )
        list.add( name );
      list.setSelection( 0 );
      list.pack();
 
      Composite buttonHolder = new Composite( shell, SWT.NONE );
      buttonHolder.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      buttonHolder.setLayout( new GridLayout( 2, false ) );

      Button insert = new Button( buttonHolder, SWT.PUSH );
      GridData insertData = new GridData();
      insertData.widthHint = PUSHBUTTON_WIDTH;
      insert.setLayoutData( insertData );
      insert.setText( LABEL_INSERT );
      insert.setEnabled( true );
      insert.addSelectionListener
      ( 
        new SelectionListener()
        {
          public void widgetDefaultSelected( SelectionEvent e ) {}

          public void widgetSelected( SelectionEvent e )
          {
            int i = list.getSelectionIndex();
            returnValue = (i >= 0 ? list.getItem( i ): null);
            shell.close();
          }
        }
      );

      Button cancel = new Button( buttonHolder, SWT.PUSH );
      GridData cancelData = new GridData();
      cancelData.widthHint = PUSHBUTTON_WIDTH;
      cancel.setLayoutData( cancelData );
      cancel.setText( LABEL_CANCEL );
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
  }
}
