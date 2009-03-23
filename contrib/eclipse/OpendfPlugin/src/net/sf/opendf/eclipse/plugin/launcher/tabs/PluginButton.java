/* 
BEGINCOPYRIGHT X,UC

	Copyright (c) 2009, EPFL
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
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
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

import java.util.ListIterator;

import net.sf.opendf.plugin.PluginManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * PluginButton creates a new Button=>Shell for enabling/disabling of Plugins 
 * @author Samuel Keller EPFL
 */
public class PluginButton {
	
	/**
	 * getButton Creates new button
	 * @param parent Shell composite
	 * @param buttons Containing Composite
	 * @return New button
	 */
	public static Button getButton(Composite parent, Composite buttons){
		final Button plugins = new Button (buttons, SWT.PUSH);
	    final Composite parentf = parent;
	    plugins.setText("Enable plugin");
	    // Enables the button only if a Plugin is available
	    plugins.setEnabled(!PluginManager.getPluginList().isEmpty());
	    
	    // When the Button is clicked, a new windows is launched
	    plugins.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Start the window
				final Shell shell = new Shell (parentf.getShell(), SWT.RESIZE | SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
				shell.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ) );
				shell.setLayout( new GridLayout( 1, false ) );
				shell.setText("Enable/Disable Plugins");
				shell.setSize (300, 300);
				
				// Create the table
				Table table = new Table (shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				ListIterator<String> name = PluginManager.getPluginList().listIterator();
				ListIterator<String> descr = PluginManager.getDescriptions().listIterator();
				while(name.hasNext()){
					String tname = name.next();
					final TableItem titem = new TableItem (table, SWT.NONE);
					titem.setText (tname);
					titem.setChecked(PluginManager.pluginManager.getEnable(tname));
				}
				table.setLayoutData(new GridData( GridData.FILL_BOTH ) );
				table.setLayout( new GridLayout( 1, false ) );
				
				// When the status of an item is changed: modify the PluginManager
				table.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						if(event.detail== SWT.CHECK){
							TableItem sel = ((TableItem)event.item);
							PluginManager.pluginManager.setEnable(sel.getText(),sel.getChecked());
						}
					}
				});
				
				// Add Ok Button
				Composite buttons = new Composite(shell, SWT.NONE);
		        buttons.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_CENTER ) );
		        buttons.setLayout( new GridLayout( 1, false ) );
		        final Button ok = new Button (buttons, SWT.PUSH);
		        ok.setText("  Ok  ");
		        ok.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						shell.close();
					}
		        });
		        
		        // Open the window
				shell.open();
			}
		});
    	return plugins;
	}
}
