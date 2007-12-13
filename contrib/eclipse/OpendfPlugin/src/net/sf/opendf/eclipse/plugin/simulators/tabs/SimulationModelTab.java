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
import java.util.Iterator;

public class SimulationModelTab extends OpendfConfigurationTab

{
  private static final String TAB_NAME          = "Model";
  
  private static final String LABEL_TOPMODEL      = "Top Level Model:";
  private static final String LABEL_BROWSE        = "Browse...";
  private static final String LABEL_MODELDIALOG   = "Select Top Level Model";
  private static final String LABEL_MODELPATH     = "Model Path:";
  private static final String LABEL_DEFAULT       = "Default";
  private static final String LABEL_SPECIFY       = "Specify";
  private static final String LABEL_PARAMETERS    = "Model Parameters:";
  private static final String LABEL_NAME          = "Name";
  private static final String LABEL_TYPE          = "Type";
  private static final String LABEL_VALUE         = "Value";
  private static final String LABEL_EDIT          = "Edit...";
  private static final String LABEL_PATHSEPARATOR = "path separator is ";
  
  // These are local keys. They can be made unique for the configuration with export()
  private static final String KEY_MODELFILE        = "FILE";
  private static final String KEY_MODELERRORS      = "ERRORS";
  private static final String KEY_MODELDIR         = "DIR";
  private static final String KEY_MODELSEARCHPATH  = "PATH";
  private static final String KEY_USEDEFAULTPATH   = "USEDIR";
  
  public static final String KEY_PARAMETER        = "PNAME";
  public static final String KEY_PARAMETERTYPE    = "PTYPE";
  public static final String KEY_INPUT            = "INAME";
  public static final String KEY_INPUTTYPE        = "ITYPE";
  public static final String KEY_OUTPUT           = "ONAME";
  public static final String KEY_OUTPUTTYPE       = "OTYPE";
 
  private static final int PUSHBUTTON_WIDTH = 100;
  private static final int NAME_INDEX  = 0;
  private static final int TYPE_INDEX  = 1;
  private static final int VALUE_INDEX = 2;

  // Standard file dialog does not remember last filter used!
  private static final String[] filters_nl     = { "*.nl"           , "*.cal"         , "*.xdf"                     , "*.*" };
  private static final String[] filterNames_nl = { "Networks (*.nl)", "Actors (*.cal)", "Structural netlist (*.xdf)", "All files (*.*)" };
  
  private static final String[] filters_cal     = { "*.cal"         , "*.nl"           , "*.xdf"                     , "*.*"  };
  private static final String[] filterNames_cal = { "Actors (*.cal)", "Networks (*.nl)", "Structural netlist (*.xdf)", "All files (*.*)" };
  
  private static final String[] filters_xdf     = { "*.xdf"                     , "*.nl"           , "*.cal"         , "*.*"  };
  private static final String[] filterNames_xdf = { "Structural netlist (*.xdf)", "Networks (*.nl)", "Actors (*.cal)", "All files (*.*)" };
  
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
  private Button editButton;
  
  private OpendfConfigurationTab thisTab;
  
  private Composite tabParent;

  public void createControl( Composite parent )
  {        
    tabParent = parent;
    
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
        new SelectionAdapter()
        {
          public void widgetSelected( SelectionEvent e )
          {
            String file = getProperty( KEY_MODELFILE );
            
            FileDialog dialog = new FileDialog( tabParent.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );
            dialog.setText( LABEL_MODELDIALOG );
            dialog.setFilterPath( getProperty( KEY_MODELDIR ) );
            dialog.setFileName( file ); // null OK
            
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
              
              //loadModel();
              updateLaunchConfigurationDialog();
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
      specifyPath.addModifyListener
      ( new ModifyListener()
        {
          public void modifyText( ModifyEvent e )
          {
            thisTab.setProperty( KEY_MODELSEARCHPATH, specifyPath.getText() );
            updateLaunchConfigurationDialog();
          }
        }
      );
      
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
      TableColumn[] parameterColumns = new TableColumn[ 3 ];
      for( int i = 0; i < 3; i++ )
      {
        parameterColumns[i] = new TableColumn( parameterTable, SWT.LEFT, i );
        parameterColumns[i].setText( columnLabels[i] );
        columnLayout.setColumnData( parameterColumns[i], new ColumnWeightData( i == 2 ? 80 : 10, PUSHBUTTON_WIDTH ) );
      }
      tableHolder.setLayout( columnLayout );
       
      // parameter edit button
      editButton = new Button( parameterGroup, SWT.PUSH );
      GridData buttonData = new GridData( );
      buttonData.widthHint = PUSHBUTTON_WIDTH;
      editButton.setLayoutData( buttonData );
      editButton.setText( LABEL_EDIT );
      editButton.setEnabled( false );
      editButton.addSelectionListener
      ( new SelectionAdapter()
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
            {
              TableItem item = parameterTable.getItem( i );
              item.setText( VALUE_INDEX, value );
              item.setImage( VALUE_INDEX, null );
              // setErrors( KEY_MODELERRORS, checkParameters() ? null : MSG_MISSINGPARAMS );
              updateLaunchConfigurationDialog();
            }            
          }
         }
      );

      // Always reread the model when tab is created
      //loadModel();
   }

  private class radioListener extends SelectionAdapter
  {
    
    public void widgetSelected( SelectionEvent e )
    {
      int i = (Integer) e.widget.getData();
      
      boolean useDefault = i == 0;
      defaultButton.setSelection( useDefault );
      specifyButton.setSelection( ! useDefault );
      specifyPath.setEnabled( ! useDefault );
      
      thisTab.setProperty( KEY_USEDEFAULTPATH, useDefault ? "true" : "false" );
      updateLaunchConfigurationDialog();
    }
  }
  
  /* 
      widgets and keys to be managed by life-cycle methods

     widget type        name               related key(s)
     ===========    =============          ==============
     
        Text         modelName              KEY_MODELFILE
        Text         defaultPath            KEY_MODELDIR
        Button       defaultButton          KEY_USEDEFAULTPATH
        Button       specifyButton          
        Text         specifyPath            KEY_MODELSEARCHPATH
        Table        parameterTable         KEY_PARAMETER, KEY_PARAMETER
                                            KEY_MODELERRORS
                                            KEY_INPUT, KEY_INPUTTYPE
                                            KEY_OUTPUT, KEY_OUTPUTTYPE
        Button       editButton
  */

  // called potentially before controls exist
  public void setDefaults( ILaunchConfigurationWorkingCopy conf )
  {
    // just null out the related keys
    try
    {
      Iterator<String> i = conf.getAttributes().keySet().iterator();
      while( i.hasNext() )
      {
        String key = i.next();
        if( key.startsWith( idPrefix() ) )
          conf.setAttribute( key, (String) null );
      }
    }
    catch( CoreException e ) { }

  }
  
  // this is called the first time
  public void initializeFrom( ILaunchConfiguration conf )
  {
    String name = null;
    String dir  = null;
    String path = null;
    String def  = "true";
    
    try
    {
      name = conf.getAttribute( idPrefix() + KEY_MODELFILE, (String) null );
      dir  = conf.getAttribute( idPrefix() + KEY_MODELFILE, (String) null );
      path = conf.getAttribute( idPrefix() + KEY_MODELFILE, (String) null );
      def  = conf.getAttribute( idPrefix() + KEY_MODELFILE, (String) null );
    }
    catch( CoreException e ) { }
    
    // we have to re-parse the selected model
    loadModel();
    
  //  modelName.setText( "" );
   // defaultPath.setText( "" );
  //  defaultButton.setEnabled( true );
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  private boolean checkParameters( )
  {
    boolean invalid = false;
    
    for( String key: getKeys( KEY_PARAMETER ) )
    {
      String value = getProperty( KEY_PARAMETER, key );
      if( value == null || value.length() == 0 )
      {
        invalid = true;
        break;
      }
    }
     
    return ! invalid;
  }
 
  private void loadModel()
  {
    String file = getProperty( KEY_MODELFILE );
    modelName.setText( file == null ? "" : file );
      
    String dir  = getProperty( KEY_MODELDIR );
    defaultPath.setText( dir== null ? "" : dir );

    if( file == null || dir == null )
    {
      setProperty( KEY_MODELERRORS, MSG_NOMODEL );
      return;
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
      setProperty( KEY_MODELERRORS, MSG_BADMODELTYPE );
      return;
    }

    InputStream is;
    
    try
    {
      is = new FileInputStream( new File( dir, file ) );
    }
    catch( Exception e )
    {
      setProperty( KEY_MODELERRORS, MSG_CANTREAD );
      return;
    }
    
    java.util.List<Element> parameters;
    java.util.List<Element> inputs;
    java.util.List<Element> outputs;
    
    try
    {
      Node document = loader.load( is );
 
      if( xpathEvalElements("//Note[@kind='Report'][@severity='Error']", document ).size() > 0 )
      {
        setProperty( KEY_MODELERRORS, MSG_MODELERRORS );
        return;
      }

      parameters = xpathEvalElements( "(Actor|Network)/Decl[@kind='Parameter']|XDF/Parameter", document );
      inputs     = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Input']"              , document );
      outputs    = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Output']"             , document );
      
    }
    catch( Exception e )
    {
      setProperty( KEY_MODELERRORS, MSG_MODELERRORS );
      return;
    }

    bindKeys( parameters, KEY_PARAMETER, KEY_PARAMETERTYPE );
    bindKeys( inputs    , KEY_INPUT    , KEY_INPUTTYPE     );
    bindKeys( outputs   , KEY_OUTPUT   , KEY_OUTPUTTYPE    );
    
    setProperty( KEY_MODELERRORS, null );
    
  }    
 
  private void bindKeys( java.util.List<Element> elements, String valueKey, String typeKey )
  {
    java.util.List<String> keep = new ArrayList<String>();

    for( Element e: elements )
    {
      Node n;
      for( n = ((Node )e).getFirstChild(); n != null; n = n.getNextSibling() )
        if( n.getNodeName().equals("Type") ) break;
    
      String name = e.getAttribute("name");
      String type = n == null ? null : CalWriter.CalmlToString( n ) ;

      keep.add( name );
    
      setProperty( typeKey, name, type );
      touchProperty( valueKey, name );
    }
    
    // Remove any properties that are no longer applicable
    pruneProperties( valueKey, keep );
    pruneProperties( typeKey , keep );
  }
  
  private void loadParameterTable()
  {
    parameterTable.removeAll();

    java.util.List<String> names = getKeys( KEY_PARAMETER );
    Collections.sort( names, 
                      new Comparator<String>()
                      {
                        public int compare( String a, String b )
                        {
                          return a.compareTo( b );
                        }
                      }
    );
 
    // Process the parameters, set up tab properties
    for( String name: names )
    {
      String value = getProperty( KEY_PARAMETER, name );
      String type  = getProperty( KEY_PARAMETERTYPE, name );
      
      TableItem item = new TableItem( parameterTable, SWT.None );
 
      item.setText( NAME_INDEX, name );
      if( type != null )
        item.setText( TYPE_INDEX, type );

      if( value != null )
      {
        item.setText( VALUE_INDEX, value );
        item.setImage( VALUE_INDEX, null );
      }
      else
      {
        item.setImage( VALUE_INDEX, errorImage );
      }
    }
    
    if( names.size() == 0 )
    {
      parameterTable.deselectAll();
      editButton.setEnabled( false );
    }
    else
    {
      int i = parameterTable.getSelectionIndex();
      if( i < 0 )
        parameterTable.setSelection( 0 );
      else if( i >= names.size() )
        parameterTable.setSelection( names.size() - 1 );
      
      editButton.setEnabled( true );
    }
    System.out.println( "in loadParameterTable()" );
    
    setErrors( KEY_MODELERRORS, checkParameters() ? null : MSG_MISSINGPARAMS );
  }
 
  public boolean isValid( ILaunchConfiguration launchConfig )
  {
    boolean valid = getProperty( KEY_MODELERRORS ) == null && checkParameters();
    System.out.println( "isValid() " + valid );
    
    // tabParent.update();
    return valid;
  }

  private static final String MSG_BADMODELTYPE  = "Invalid top level model type";
  private static final String MSG_NOMODEL       = "No top level model selected";
  private static final String MSG_CANTREAD      = "Can't read top level model";
  private static final String MSG_MODELERRORS   = "Top level model has errors";
  private static final String MSG_MISSINGPARAMS = "Missing model parameter values";
}
