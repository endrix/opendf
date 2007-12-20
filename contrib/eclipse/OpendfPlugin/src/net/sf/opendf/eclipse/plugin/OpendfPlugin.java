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

package net.sf.opendf.eclipse.plugin;

import org.eclipse.ui.plugin.*;
import org.eclipse.ui.console.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Color;
import java.util.*;
import org.eclipse.swt.SWT;

public class OpendfPlugin extends AbstractUIPlugin
{

	public static final String ID = "net.sf.opendf.eclipse.OpendfEditorPlugin";
  
  // indices into the imagePaths array
  public static final int IMAGE_action       =  0;
  public static final int IMAGE_actor        =  1;
  public static final int IMAGE_entity       =  2;
  public static final int IMAGE_fsm          =  3;
  public static final int IMAGE_inequality   =  4;
  public static final int IMAGE_network      =  5;
  public static final int IMAGE_priority     =  6;
  public static final int IMAGE_QID          =  7;
  public static final int IMAGE_transition   =  8;
  public static final int IMAGE_variable     =  9;
  public static final int IMAGE_error        = 10;

  public static String getId()
  {
    return ID;
  }
  
  private static final String[] imagePaths =
  {
    "icons/action.gif",
    "icons/actor.gif",
    "icons/entity.gif",
    "icons/fsm.gif",
    "icons/inequality.gif",
    "icons/network.gif",
    "icons/priority.gif",
    "icons/QID.gif",
    "icons/transition.gif",
    "icons/variable.gif",
    "icons/error.gif"
     };

	private List<Image> images;
  
	//The shared instance.
	private static OpendfPlugin plugin;
	
	public OpendfPlugin()
	{
		plugin = this;
	}

	public static void logInfoMessage( String msg )
	{
		plugin.getLog().log( new Status( Status.INFO, ID, Status.OK, msg, null ));	
	}

  public static void logErrorMessage( String msg, Throwable exception )
  {
    plugin.getLog().log( new Status( Status.ERROR, ID, Status.OK, msg, exception ));  
  }
  
	public void start( BundleContext context ) throws Exception
	{
		super.start( context );
    createImages();
 	}

	public void stop( BundleContext context ) throws Exception
	{
    disposeImages();
    plugin = null;
		super.stop( context );
	}

	public static OpendfPlugin getDefault()
	{
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return AbstractUIPlugin.imageDescriptorFromPlugin( ID, path );
	}
  
  private void createImages()
  {
    images = new ArrayList<Image>( imagePaths.length );
    
    for( int i=0; i<imagePaths.length; i++ )
    {
      ImageDescriptor desc = getImageDescriptor( imagePaths[i] );
      Image image = desc == null ? null : desc.createImage();
      
      images.add( image );
    }
  }
  
  private void disposeImages()
  {
    for( int i=0; i<images.size(); i++ )
    {
      Image image = images.get(i);
      if( images != null )
        image.dispose();
    }
  }

  public Image getImage( int i )
  {
     try
    {
      return images.get(i);
    }
    catch( Exception e )
    {
      logErrorMessage("Failed to find image ID " + i, e);
    }
    
    return null;
  }
  
  public MessageConsole findConsole( String name )
  {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    IConsole existing[] = manager.getConsoles();
    
    for( int i=0; i< existing.length; i++ )
    {
      if( name.equals( existing[i].getName() ) )
      {
        return (MessageConsole) existing[i];
      }
    }
    
    MessageConsole console = new MessageConsole( name, null );
    manager.addConsoles( new IConsole[]{ console } );
    
    return console;
    
  }
}
