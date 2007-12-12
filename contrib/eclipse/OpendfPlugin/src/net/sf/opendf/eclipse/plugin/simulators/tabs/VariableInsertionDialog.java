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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class VariableInsertionDialog extends org.eclipse.swt.widgets.Dialog
{
  private java.util.List<String> variables;
  private String exclude;
  private Shell parent;
  private Shell shell;
    
  public VariableInsertionDialog( Shell parent, java.util.List<String> variables, String exclude )
  {
    super( parent );
    
    this.parent = parent;
    this.variables = variables;
    this.exclude = exclude;
  }
 
  private String returnValue;
  private List list;
    
  public Object open()
  {
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
    shell.setText( LABEL_INSERTVARIABLE );
    shell.setLayout( new GridLayout( 1, false ) );
      
    list = new List( shell, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER );
    GridData listData = new GridData( GridData.FILL_BOTH );
    listData.widthHint = 300;
    list.setLayoutData( listData );
    
    for( String name : variables )
      if( ! name.equals( exclude ) ) list.add( name );
    
    list.setSelection( 0 );
    list.pack();
 
    Composite buttonHolder = new Composite( shell, SWT.NONE );
    buttonHolder.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    buttonHolder.setLayout( new GridLayout( 2, false ) );

    Button insert = new Button( buttonHolder, SWT.PUSH );
    GridData insertData = new GridData( GridData.HORIZONTAL_ALIGN_BEGINNING );
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
    GridData cancelData = new GridData( GridData.HORIZONTAL_ALIGN_END );
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
  
  private static final int PUSHBUTTON_WIDTH = 100;  
  private static final String LABEL_INSERTVARIABLE= "Insert Variable Reference";
  private static final String LABEL_INSERT        = "Insert";
  private static final String LABEL_CANCEL        = "Cancel";
}
