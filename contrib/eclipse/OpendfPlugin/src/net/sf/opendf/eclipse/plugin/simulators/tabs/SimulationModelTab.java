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

public class SimulationModelTab extends OpendfConfigurationTab

{
  public static final String TAB_NAME          = "Model";
  
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
  public static final String LABEL_EDITPARAMETER = "Edit parameter ";
  public static final String LABEL_ACCEPT        = "Apply";
  public static final String LABEL_CANCEL        = "Revert";
  
  public static final String KEY_MODELFILE        = "FILE";
  public static final String KEY_MODELDIR         = "DIR";
  public static final String KEY_MODELSEARCHPATH  = "PATH";
  public static final String KEY_USEDEFAULTPATH   = "USEDIR";
  public static final String KEY_PARAMETER        = "PARAM.NAME";
  public static final String KEY_PARAMETERTYPE    = "PARAM.TYPE";
  public static final String KEY_INPUT            = "INPUT.NAME";
  public static final String KEY_INPUTTYPE        = "INPUT.TYPE";
  public static final String KEY_OUTPUT           = "OUTPUT.NAME";
  public static final String KEY_OUTPUTTYPE       = "OUTPUT.TYPE";
 
  public static final int PUSHBUTTON_WIDTH = 100;
  public static final int NAME_INDEX  = 0;
  public static final int TYPE_INDEX  = 1;
  public static final int VALUE_INDEX = 2;
  
  
  // Standard file dialog does not remember last filter used!
  public static final String[] filters_nl     = { "*.nl"           , "*.cal"         , "*.xdf"                      };
  public static final String[] filterNames_nl = { "Networks (*.nl)", "Actors (*.cal)", "Structural netlist (*.xdf)" };
  
  public static final String[] filters_cal     = { "*.cal"         , "*.nl"           , "*.xdf"                      };
  public static final String[] filterNames_cal = { "Actors (*.cal)", "Networks (*.nl)", "Structural netlist (*.xdf)" };
  
  public static final String[] filters_xdf     = { "*.xdf"                     , "*.nl"           , "*.cal"          };
  public static final String[] filterNames_xdf = { "Structural netlist (*.xdf)", "Networks (*.nl)", "Actors (*.cal)" };

  Composite fParent;
  
  public SimulationModelTab()
  {
    super( TAB_NAME );
    
    thisTab = this;
  }
  
  private Text modelName;
  private Text defaultPath;
  private Button defaultButton;
  private Button specifyButton;
  private Text specifyPath;
  private Table parameterTable;
  private TableColumn[] parameterColumns;
  private Button editButton;
  
  private OpendfConfigurationTab thisTab;
  
  public void createControl( Composite parent )
  {    
    fParent = parent;
    
    Composite tab = new Composite(parent, SWT.NONE);
    setControl( tab );
    tab.setLayout( new GridLayout( 1, false ) );

    // Top Level Model Selection
    Group modelGroup = new Group( tab, SWT.NONE );
    modelGroup.setText( LABEL_TOPMODEL );
    modelGroup.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    modelGroup.setLayout( new GridLayout( 2, false ) );
    
      // Model name display
      modelName = new Text( modelGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY );
      modelName.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      
      // Browse button
      Button browseButton = new Button( modelGroup, SWT.PUSH );
      browseButton.setText( LABEL_BROWSE );
      browseButton.addSelectionListener
      (
        new SelectionListener()
        {
          public void widgetDefaultSelected( SelectionEvent e ) {}

          public void widgetSelected( SelectionEvent e )
          {
            String file = getProperty( KEY_MODELFILE );
            
            FileDialog dialog = new FileDialog( fParent.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );
            dialog.setText( LABEL_MODELDIALOG );
            dialog.setFilterPath( getProperty( KEY_MODELDIR ) );
            dialog.setFileName( file );
            
            // Shuffle the filters so we start where we left off last time
            if( file == null || file.endsWith( ".nl" ) )
            {
              dialog.setFilterExtensions( filters_nl );
              dialog.setFilterNames( filterNames_nl );
            }          
            else if( file.endsWith( ".cal" ) )
            {
              dialog.setFilterExtensions( filters_cal );
              dialog.setFilterNames( filterNames_cal );
            }          
            else
            {
              dialog.setFilterExtensions( filters_xdf );
              dialog.setFilterNames( filterNames_xdf );
            }

            file = dialog.open() ;
              
            if( file != null )
            {
              String dir  = dialog.getFilterPath();
              int dl = dir.length() + 1;
              
              // Get the base filename
              if( file.length() > dl ) file = file.substring( dl );
              else dir = "";    // ??
                
              modelName.setText( file );
              setProperty( KEY_MODELFILE, file );
                
              defaultPath.setText( dir );
              setProperty( KEY_MODELDIR , dir );
              
              parseModel();
            }
          }
        }
      );
      
    // Model Path Selection
    Group pathGroup = new Group( tab, SWT.NONE );
    pathGroup.setText( LABEL_MODELPATH );
    pathGroup.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    pathGroup.setLayout( new GridLayout( 2, false ) );

      // choose default model path
      defaultButton = new Button( pathGroup, SWT.RADIO );
      defaultButton.setText( LABEL_DEFAULT );
      defaultButton.setData( new Integer( 0 ) );
      defaultButton.setSelection( true );
      defaultButton.addSelectionListener( new radioListener() );
      
      // display default model path
      defaultPath = new Text( pathGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY );
      defaultPath.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

      // choose custom model path
      specifyButton = new Button( pathGroup, SWT.RADIO );
      specifyButton.setText( LABEL_SPECIFY );
      specifyButton.setData( new Integer( 1 ) );
      specifyButton.setSelection( false );
      specifyButton.addSelectionListener( new radioListener() );
      
      // specify custom model path
      specifyPath = new Text( pathGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER );
      specifyPath.setEnabled( false );
      specifyPath.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

      // path separator indication
      new Text( pathGroup, SWT.SINGLE | SWT.READ_ONLY ); // spacer
      Text pathSeparator = new Text( pathGroup, SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY );
      pathSeparator.setText( LABEL_PATHSEPARATOR + "\"" + File.pathSeparator + "\"" );
      pathSeparator.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
      
    // Model Parameters
    Group parameterGroup = new Group( tab, SWT.NONE );
    parameterGroup.setText( LABEL_PARAMETERS );
    parameterGroup.setLayoutData( new GridData( GridData.FILL_BOTH ) );
    parameterGroup.setLayout( new GridLayout( 2, false ) );
    
      // parameter display table
      Composite tableHolder = new Composite( parameterGroup, SWT.NONE );
      tableHolder.setLayoutData( new GridData( GridData.FILL_BOTH ) );
      TableColumnLayout columnLayout = new TableColumnLayout();
      
      parameterTable = new Table( tableHolder, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION );
      parameterTable.setLinesVisible( true );
      parameterTable.setHeaderVisible( true );
      
      String[] columnLabels = { LABEL_NAME, LABEL_TYPE, LABEL_VALUE };
      parameterColumns = new TableColumn[ 3 ];
      for( int i = 0; i < 3; i++ )
      {
        parameterColumns[i] = new TableColumn( parameterTable, SWT.LEFT, i );
        parameterColumns[i].setText( columnLabels[i] );
        // parameterColumns[i].pack();
        columnLayout.setColumnData( parameterColumns[i], new ColumnWeightData( i == 2 ? 80 : 10, PUSHBUTTON_WIDTH ) );
      }
      tableHolder.setLayout( columnLayout );
      // parameterTable.pack();
       
      // parameter edit button
      editButton = new Button( parameterGroup, SWT.PUSH );
      GridData buttonData = new GridData( );
      buttonData.widthHint = PUSHBUTTON_WIDTH;
      editButton.setLayoutData( buttonData );
      editButton.setText( LABEL_EDIT );
      editButton.setEnabled( false );
      editButton.addSelectionListener
      ( new SelectionListener()
        {
          public void widgetSelected( SelectionEvent e )
          {
            int i = parameterTable.getSelectionIndex();
            
            if( i < 0 ) return;
            
            String name = parameterTable.getItem( i ).getText( 0 );

            EditValueDialog dialog = new EditValueDialog( editButton.getShell(), thisTab, name, 
                KEY_PARAMETERTYPE, KEY_PARAMETER, KEY_PARAMETER );
            
            String value = (String) dialog.open();
            if( value != null )
              parameterTable.getItem( i ).setText( VALUE_INDEX, value );
          }
                                             
          public void widgetDefaultSelected( SelectionEvent e ) {}
        }
      );

      parseModel();
   }

  private class radioListener implements SelectionListener
  {
    
    public void widgetDefaultSelected( SelectionEvent e ) {}

    public void widgetSelected( SelectionEvent e )
    {
      int i = (Integer) e.widget.getData();
      
      boolean useDefault = i == 0;
      defaultButton.setSelection( useDefault );
      specifyButton.setSelection( ! useDefault );
      specifyPath.setEnabled( ! useDefault );
    }
  }
 
  private boolean parseModel()
  {
    String file = getProperty( KEY_MODELFILE );
    String dir  = getProperty( KEY_MODELDIR );

    if( file == null || dir == null )
    {
      setErrorMessage( "No top level model selected" );
      return false;
    }
    
    SourceLoader loader;
    
    if( file.endsWith(".nl" ) )
      loader = new NLLoader();
    else if( file.endsWith(".cal" ) )
      loader = new CalLoader();
    else if( file.endsWith(".xdf" ) )
      loader = new XDFLoader();
    else
    {
      setErrorMessage("Invalid top level model type");
      return false;
    }

    InputStream is;
    
    try
    {
      is = new FileInputStream( new File( dir, file ) );
    }
    catch( Exception e )
    {
      setErrorMessage("Can't read top level model");
      return false;
    }
    
    java.util.List<Element> parameters;
    java.util.List<Element> inputs;
    java.util.List<Element> outputs;
    
    try
    {
      Node document = loader.load( is );
 
      if( xpathEvalElements("//Note[@kind='Report'][@severity='Error']", document ).size() > 0 )
      {
        setErrorMessage( "Top level model has errors" );
        return false;
      }

      parameters = xpathEvalElements( "(Actor|Network)/Decl[@kind='Parameter']|XDF/Parameter", document );
      inputs     = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Input']"              , document );
      outputs    = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Output']"             , document );
      
    }
    catch( Exception e )
    {
      setErrorMessage("Top level model has errors");
      return false;
    }
    
    parameterTable.removeAll();
    Collections.sort( parameters, 
                      new Comparator<Element>()
                      {
                        public int compare( Element a, Element b )
                        {
                          return a.getAttribute( "name" ).compareTo( b.getAttribute( "name" ) );
                        }
                      }
    );
    
    java.util.List<String> keep = new ArrayList<String>();
    
    // Process the parameters, set up tab properties
    for( Element p: parameters )
    {
      Node n;
      for( n = ((Node )p).getFirstChild(); n != null; n = n.getNextSibling() )
        if( n.getNodeName().equals("Type") ) break;
      
      TableItem item = new TableItem( parameterTable, SWT.None );
      
      String name = p.getAttribute("name");
      String type = n == null ? null : CalWriter.CalmlToString( n ) ;

      keep.add( name );
      setProperty( KEY_PARAMETERTYPE + "." + name, type );
      
      item.setText( NAME_INDEX, name );
      if( type != null )
        item.setText( TYPE_INDEX, type );
      
      String value = getProperty( KEY_PARAMETER + "." + name );
      if( value != null )
        item.setText( VALUE_INDEX, value );
    }
    
    // Remove any properties that are no longer applicable
    pruneProperties( KEY_PARAMETER    , keep );
    pruneProperties( KEY_PARAMETERTYPE, keep );
  /*  
    for( int i = 0; i < 2; i++ )
    {
      parameterColumns[i].pack();
    }
*/
    int i = parameterTable.getSelectionIndex();
    if( parameters.size() == 0 )
    {
      parameterTable.deselectAll();
      editButton.setEnabled( false );
    }
    else
    {
      if( i < 0 )
        parameterTable.setSelection( 0 );
      else if( i >= parameters.size() )
        parameterTable.setSelection( parameters.size() - 1 );
      
      editButton.setEnabled( true );
    }
    
    // Process the inputs    
    keep = new ArrayList<String>();
    
    // Now get types for all inputs, set up tab properties
    for( Element in: inputs )
    {
      Node n;
      for( n = ((Node )in).getFirstChild(); n != null; n = n.getNextSibling() )
        if( n.getNodeName().equals("Type") ) break;
      
      String name = in.getAttribute("name");
      String type = n == null ? null : CalWriter.CalmlToString( n ) ;

      keep.add( name );
      setProperty( KEY_INPUTTYPE + "." + name, type );
    }
    
    pruneProperties( KEY_INPUT    , keep );
    pruneProperties( KEY_INPUTTYPE, keep );

    // Process the outputs    
    keep = new ArrayList<String>();
    
    // Now get types for all inputs, set up tab properties
    for( Element out: outputs )
    {
      keep.add( out.getAttribute("name") );
    }
    
    pruneProperties( KEY_OUTPUT, keep );

    setErrorMessage( null );
    return true;
  }
 
}
