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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

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

public class SimulationModelTab extends OpendfConfigurationTab

{
  public static final String TAB_NAME          = "Main";
  
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
  public static final String LABEL_PATHSEPARATOR = "path separator is";
  
  public static final String KEY_MODELFILE        = "FILE";
  public static final String KEY_MODELDIR         = "DIR";
  public static final String KEY_MODELSEARCHPATH  = "PATH";
  public static final String KEY_USEDEFAULTPATH   = "USEDIR";
  public static final String KEY_PARAMETER        = "PARAM";
  public static final String KEY_PARAMETERTYPE    = "PARAM.TYPE";
  public static final String KEY_INPUT            = "INPUT";
  public static final String KEY_INPUTTYPE        = "INPUT.TYPE";
  public static final String KEY_OUTPUT           = "OUTPUT";
  public static final String KEY_OUTPUTTYPE       = "OUTPUT.TYPE";

  Composite fParent;
  
  public SimulationModelTab()
  {
    super( TAB_NAME );
  }
  
  private Text modelName;
  private Text defaultPath;
  private Button defaultButton;
  private Button specifyButton;
  private Text specifyPath;
  private Table parameterTable;
  private TableColumn[] parameterColumns;
  private Button editButton;
  
  public void createControl( Composite parent )
  {
    GridLayout gl;
    GridData gd;
    
    fParent = parent;
    
    Composite tab = new Composite(parent, SWT.NONE);
    setControl( tab );
    gl = new GridLayout();
    gl.numColumns = 1;
    tab.setLayout( gl );

    // Top Level Model Selection
    Group modelGroup = new Group( tab, SWT.NONE );
    modelGroup.setText( LABEL_TOPMODEL );
    gd = new GridData( GridData.FILL_HORIZONTAL );
    modelGroup.setLayoutData( gd );
    gl = new GridLayout();
    gl.numColumns = 2;
    modelGroup.setLayout( gl );
    
      // Model name display
      modelName = new Text( modelGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY );
      gd = new GridData( GridData.FILL_HORIZONTAL );
      modelName.setLayoutData( gd );
      
      // Browse button
      Button browseButton = new Button( modelGroup, SWT.PUSH );
      SelectionListener browser = new browseListener( null );
      browseButton.setText( LABEL_BROWSE );
      browseButton.addSelectionListener( browser );
      
    // Model Path Selection
    Group pathGroup = new Group( tab, SWT.NONE );
    pathGroup.setText( LABEL_MODELPATH );
    gd = new GridData( GridData.FILL_HORIZONTAL );
    pathGroup.setLayoutData( gd );
    gl = new GridLayout();
    gl.numColumns = 2;
    pathGroup.setLayout( gl );

      // choose default model path
      defaultButton = new Button( pathGroup, SWT.RADIO );
      defaultButton.setText( LABEL_DEFAULT );
      defaultButton.setData( new Integer( 0 ) );
      defaultButton.setSelection( true );
      defaultButton.addSelectionListener( new radioListener() );
      
      // display default model path
      defaultPath = new Text( pathGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY );
      gd = new GridData( GridData.FILL_HORIZONTAL );
      defaultPath.setLayoutData( gd );

      // choose custom model path
      specifyButton = new Button( pathGroup, SWT.RADIO );
      specifyButton.setText( LABEL_SPECIFY );
      specifyButton.setData( new Integer( 1 ) );
      specifyButton.setSelection( false );
      specifyButton.addSelectionListener( new radioListener() );
      
      // specify custom model path
      specifyPath = new Text( pathGroup, SWT.LEFT | SWT.SINGLE | SWT.BORDER );
      specifyPath.setEnabled( false );
      gd = new GridData( GridData.FILL_HORIZONTAL );
      specifyPath.setLayoutData( gd );

    // Model Parameters
    Group parameterGroup = new Group( tab, SWT.NONE );
    parameterGroup.setText( LABEL_PARAMETERS );
    gd = new GridData( GridData.FILL_BOTH );
    parameterGroup.setLayoutData( gd );
    gl = new GridLayout();
    gl.numColumns = 2;
    parameterGroup.setLayout( gl );
    
      // parameter display table
      parameterTable = new Table( parameterGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION );
      gd = new GridData( GridData.FILL_BOTH );
      parameterTable.setLayoutData( gd );
      parameterTable.setLinesVisible( true );
      parameterTable.setHeaderVisible( true );
      String[] columnLabels = { LABEL_NAME, LABEL_TYPE, LABEL_VALUE };
      parameterColumns = new TableColumn[ columnLabels.length ];
      for( int i = 0; i < columnLabels.length; i++ )
      {
        parameterColumns[i] = new TableColumn( parameterTable, SWT.LEFT, i );
        parameterColumns[i].setText( columnLabels[i] );
        parameterColumns[i].pack();
      }
      
      // parameter edit button
      editButton = new Button( parameterGroup, SWT.PUSH );
      editButton.setText( LABEL_EDIT );
      editButton.setEnabled( false );
      SelectionListener edit = new editListener( null );
      editButton.addSelectionListener( edit );

      setErrorMessage("Ho ho ho");
   }

  private class browseListener implements SelectionListener
  {
    private FileDialog dialog;
    
    public browseListener( String dir )
    {
      String[] filters     = { "*.nl"           , "*.cal"         , "*.xdf"  };
      String[] filterNames = { "Networks (*.nl)", "Actors (*.cal)", "Structural netlist (*.xdf)" };
      dialog = new FileDialog( fParent.getShell(), SWT.OPEN | SWT.APPLICATION_MODAL );
      dialog.setText( LABEL_MODELDIALOG );
      dialog.setFilterExtensions( filters );
      dialog.setFilterNames( filterNames );
      dialog.setFilterPath( dir );
    }
    
    public void widgetDefaultSelected( SelectionEvent e )
    {
     
    }

    public void widgetSelected( SelectionEvent e )
    {
      String file = dialog.open() ;
      
      if( file != null )
      {
        String dir  = dialog.getFilterPath();
        int dl = dir.length() + 1;
        if( file.length() > dl )
        {
          // Strip off the directory part
          file = file.substring( dl );
        }
        else
        {
          // Counldn't find a directory prefix ??
          dir = "";
        }
        
        modelName.setText( file );
        setProperty( KEY_MODELFILE, file );
        
        defaultPath.setText( dir );
        setProperty( KEY_MODELDIR , dir );
      }
      
      parseModel();
    }
  }

  
  private class radioListener implements SelectionListener
  {
    private FileDialog dialog;
    
    public void widgetDefaultSelected( SelectionEvent e )
    {
     
    }

    public void widgetSelected( SelectionEvent e )
    {
      int i = (Integer) e.widget.getData();
      
      if( i == 0 )
      {
        // use the default model path
        defaultButton.setSelection( true );
        specifyButton.setSelection( false );
        specifyPath.setEnabled( false );
      }
      else
      {
        defaultButton.setSelection( false );
        specifyButton.setSelection( true );
        specifyPath.setEnabled( true );        
      }
    }
  }
  
  private String lastModelFile;
 
  private class elementComparator implements Comparator<Element>
  {
    public int compare( Element a, Element b )
    {
      return a.getAttribute( "name" ).compareTo( b.getAttribute( "name" ) );
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
    Collections.sort( parameters, new elementComparator() );
    
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
      
      item.setText( 0, name );
      if( type != null )
        item.setText( 1, type );
    }
    
    // Remove any properties that are no longer applicable
    pruneProperties( KEY_PARAMETER    , keep );
    pruneProperties( KEY_PARAMETERTYPE, keep );
    
    for( int i = 0; i < parameterColumns.length; i++ )
    {
      parameterColumns[i].pack();
    }

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
