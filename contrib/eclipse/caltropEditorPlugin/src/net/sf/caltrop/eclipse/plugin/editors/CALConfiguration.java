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

package net.sf.caltrop.eclipse.plugin.editors;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.ui.editors.text.*;
import net.sf.caltrop.eclipse.plugin.editors.scanners.*;
import org.eclipse.jface.preference.*;

public class CALConfiguration extends TextSourceViewerConfiguration
{
	private CALColorManager colorManager;
	private CALScanner scanner;

	public CALConfiguration( IPreferenceStore store, CALColorManager cm )
	{
		super( store );
		colorManager = cm;
     	scanner = new CALScanner( colorManager );
	}
	
	// No partitioner is attached, so the entire document will be one
	// partition of the default type
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
	{
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE  };			
	}

	// Extend the default damager so that we compute damage properly.
	// The default does not compurte sufficient damage to cover extensive
	// multi-line comment changes.
	private class MajorDamage extends DefaultDamagerRepairer
	{
		MajorDamage( ITokenScanner scanner )
	   {
		   super( scanner );
	   }
	   
	   public IRegion getDamageRegion( ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) 
	   { 
		   // For now, damage the whole document
		   return new Region( partition.getOffset(), partition.getLength() );
	   }
	}
	
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
	{
		PresentationReconciler reconciler = new PresentationReconciler();

		MajorDamage dr = new MajorDamage( scanner );
		reconciler.setDamager ( dr, IDocument.DEFAULT_CONTENT_TYPE );
		reconciler.setRepairer( dr, IDocument.DEFAULT_CONTENT_TYPE );
		
		return reconciler;
	}
}
