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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

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
  private static final String TAB_NAME            = "Model";
  
  private static final String LABEL_TOPMODEL      = "Top Level Model:";
  private static final String LABEL_MODELDIALOG   = "Select Top Level Model";
  private static final String LABEL_MODELPATH     = "Model Path:";
  private static final String LABEL_DEFAULT       = "Default";
  private static final String LABEL_SPECIFY       = "Specify";
  private static final String LABEL_PARAMETERS    = "Model Parameters:";
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

  // Standard file dialog does not remember last filter used!
  private static final String[] filters     = { "*.nl"           , "*.cal"         , "*.xdf"                     , "*.*" };
  private static final String[] filterNames = { "Networks (*.nl)", "Actors (*.cal)", "Structural netlist (*.xdf)", "All files (*.*)" };
 
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
  private ValueTable parameterTable;

  
  private OpendfConfigurationTab thisTab;

  public void createControl( Composite parent )
  {        
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
      new BrowseButton( this, modelGroup, LABEL_MODELDIALOG, KEY_MODELFILE,
          KEY_MODELDIR, filters, filterNames );
      
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
    parameterTable = new ValueTable( this, tab, LABEL_PARAMETERS, 
          KEY_PARAMETER, KEY_PARAMETERTYPE, KEY_MODELERRORS );
 
   }

  private class radioListener extends SelectionAdapter
  {
    
    public void widgetSelected( SelectionEvent e )
    {
      if( (Integer) e.widget.getData() == 0 )
      {
         boolean useDefault = defaultButton.getSelection();
         thisTab.setProperty( KEY_USEDEFAULTPATH, useDefault ? "true" : "false" );
         updateLaunchConfigurationDialog();
      }
    }
  }
  
  public void setDefaults( ILaunchConfigurationWorkingCopy conf )
  {
    super.setDefaults( conf );
    
    // over-ride the default behavior (null)
    setProperty( KEY_USEDEFAULTPATH, "true" );
    conf.setAttribute( export( KEY_USEDEFAULTPATH ), "true" );
  }
  
  // this is called the first time
  public void initializeFrom( ILaunchConfiguration conf )
  {
    // import all relevant attributes
    super.initializeFrom( conf );

    // Must parse the model
    loadModel();
    updateWidgets();
  }

  // this is called the first time
  public void performApply( ILaunchConfigurationWorkingCopy conf )
  {
    String oldModel = null;    
    String oldDir   = null;    
    String newModel = getProperty( KEY_MODELFILE );    
    String newDir   = getProperty( KEY_MODELDIR  );

    try
    {
       oldModel = conf.getAttribute( export( KEY_MODELFILE ), (String) null );    
       oldDir   = conf.getAttribute( export( KEY_MODELDIR  ), (String) null );    
    }
    catch( CoreException e )
    { 
      OpendfPlugin.logErrorMessage( "Exception in performApply() for tab " + TAB_NAME, e );
    }
    
    if( oldModel == null || oldDir == null || !oldModel.equals( newModel ) ||
        !oldDir.equals( newDir ) )
    {
      // re-parse required
      loadModel();
      
      clearConfiguration( conf );
    }
    else
    {
      // just update the parameter table
      parameterTable.update();
    }
   
    updateWidgets();
    
    // export all the attributes
    super.performApply( conf );
  }

  // Update local widgets but be careful with widgets that have Listeners to
  // prevent unnecessary cascading or recursive calls to performApply()
  public void updateWidgets()
  {
    String snew;
    String sold;
    
    snew = getProperty( KEY_MODELFILE );
    modelName.setText( snew == null ? "" : snew );
    
    snew = getProperty( KEY_MODELDIR );
    defaultPath.setText( snew == null ? "" : snew );
    
    // The model path selection is listened to
    snew = getProperty( KEY_USEDEFAULTPATH );
    boolean bnew = ( snew == null || snew.equals( "true" ) );
    boolean bold = defaultButton.getSelection();
    if( bnew != bold )
    { 
      defaultButton.setSelection( bnew );
      specifyButton.setSelection( ! bnew );
    }
    specifyPath.setEnabled( ! bnew );

    // This has a modify Listener
    snew = getProperty( KEY_MODELSEARCHPATH );
    sold = specifyPath.getText();
    if( snew == null )
    { 
      if( sold != null && sold.length() > 0 ) specifyPath.setText( "" );
    }
    else if( ! snew.equals( sold ) ) specifyPath.setText( snew );

    setErrorMessage( getProperty( KEY_MODELERRORS ) );
  }

  private void loadModel()
  {
    parameterTable.clear();

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

    createKeys( parameters, KEY_PARAMETER, KEY_PARAMETERTYPE );
    createKeys( inputs    , KEY_INPUT    , KEY_INPUTTYPE     );
    createKeys( outputs   , KEY_OUTPUT   , KEY_OUTPUTTYPE    );
    
    setProperty( KEY_MODELERRORS, null );
    
    parameterTable.load();
  }    
 
  private void createKeys( java.util.List<Element> elements, String valueKey, String typeKey )
  {
    java.util.List<String> keep = new java.util.ArrayList<String>();

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
 
  public boolean isValid( ILaunchConfiguration launchConfig )
  {
    return getProperty( KEY_MODELERRORS ) == null;
  }

  private static final String MSG_BADMODELTYPE  = "Invalid top level model type";
  private static final String MSG_NOMODEL       = "No top level model selected";
  private static final String MSG_CANTREAD      = "Can't read top level model";
  private static final String MSG_MODELERRORS   = "Top level model has errors";
}
