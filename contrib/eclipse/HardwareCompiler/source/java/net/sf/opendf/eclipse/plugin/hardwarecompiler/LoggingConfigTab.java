/* 
BEGINCOPYRIGHT X
  
  Copyright (c) 2008, Xilinx Inc.
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
package net.sf.opendf.eclipse.plugin.hardwarecompiler;

import java.util.*;

import net.sf.opendf.config.*;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.eclipse.plugin.config.ConfigModificationListener;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.ControlRenderingFactory;
import net.sf.opendf.eclipse.plugin.config.TopModelParamParse;
import net.sf.opendf.eclipse.plugin.config.UpdatableControlIF;
import net.sf.opendf.eclipse.plugin.config.TopModelParamParse.ModelParameter;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * This configuration tab implements control widgets for various configurable elements
 * from the {@link net.sf.opend.config} package.  The tab expects to store the launch
 * configuration in its string based attribute mechanism, while the rendered Control
 * elements operate on the values stored in the {@link AbstractConfig} objects from a 
 * {@link ConfigGroup}.  The two are kept in sync via the performApply and initializeFrom
 * methods.  Further, each control has a modification/selection listener which is used to
 * ensure the backing AbstractConfig gets updated.
 * 
 * To ensure that multiple configuration tabs do not override configuration settings, each
 * AbstractConfig should have a control element on only 1 tab.  This allows that tab to safely 
 * use the {@link ConfigGroup#pushConfig} and {@link ConfigGroup#updateConfig} methods 
 * with the contained keys.
 * 
 * @author imiller
 *
 */
public class LoggingConfigTab extends OpendfConfigTab//AbstractLaunchConfigurationTab
{

    public LoggingConfigTab()
    {
        super();
    }
    
    @Override
    public void createControl (Composite parent)
    {
        // Create a scrolling composite in which to put all the controls.  You can organize it however you would like.
        ScrolledComposite tabScroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        tabScroller.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        tabScroller.setLayout( new GridLayout( 1, false ) );
        tabScroller.setExpandHorizontal(true);
        tabScroller.setExpandVertical(true);
        Composite tab = new Composite(tabScroller, SWT.NONE);
        tab.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        tab.setLayout( new GridLayout( 1, false ) );
        
        Composite buttons = new Composite(tab, SWT.NONE);
        buttons.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_CENTER ) );
        buttons.setLayout( new GridLayout( 1, false ) );
        
        setControl( tabScroller );
        tabScroller.setContent(tab);

        final Button defaults = this.getDefaultButton(buttons);
        
        final Group leftColLog = new Group(buttons, SWT.SHADOW_IN);
        leftColLog.setText("Console Logging");
        leftColLog.setLayoutData( new GridData(SWT.FILL, SWT.BEGINNING, true,true) );
        leftColLog.setLayout( new GridLayout( 3, true ) );
        addControl(ConfigGroup.LOG_LEVEL_USER, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.LOG_LEVEL_USER), leftColLog));
        addControl(ConfigGroup.LOG_LEVEL_SIM, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.LOG_LEVEL_SIM), leftColLog));
        addControl(ConfigGroup.LOG_LEVEL_DBG, ControlRenderingFactory.renderConfig(getConfigs().get(ConfigGroup.LOG_LEVEL_DBG), leftColLog));

        tabScroller.setMinSize(tab.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    @Override
    public String getName ()
    {
        return "Logging";
    }

}
