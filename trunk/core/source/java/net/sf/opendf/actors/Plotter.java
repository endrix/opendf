/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
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

package net.sf.opendf.actors;

import net.sf.opendf.hades.des.*;
import net.sf.opendf.hades.des.schedule.*;
import java.util.*;

import ptolemy.plot.Plot;
import ptolemy.plot.PlotFrame;

/**
 *  Component pumping data into a PtPlot window.
 *
 *  This implements a basic hades.DES.DiscreteEventComponent that opens a PtPlot frame and
 *  writes data arriving at its 'Data' connector into the plot. It has a 'Redraw' connector
 *  to explicitly trigger redraws.
 *
 *  It can be parametrized by a map that provides values for certain options of the PtPlot
 *  object. The option names are strings, the allowable values depend on the option.
 <ulist>
 <item> time [Boolean, default true]: If true, the x value is the time stamp of the incoming token.
 otherwise, it is the number (counted separately in each dataset).
 <item> autoredraw [Integer]: If given defines the number of tokens after which the plot
 must be repainted. This feature is independent of the 'Redraw' input.
 <item>connected [Boolean, default true]: If true, subsequent points are connected.
 <item> legend [Boolean, default true]: If true, a legend is produced for each data set.
 <item> impulse [Boolean, default true]: If true, a stem diagram is drawn.
 <item> title [String, default "Time plot" or "Event plot"]: Diagram title.
 <item> xlabel [String]: Label of x axis.
 <item> ylabel [String]: Label of y axis.
 <item> xlog [Boolean, default false]: If true, x axis is scaled logarithmically.
 <item> ylog [Boolean, default false]: If true, y axis is scaled logarithmically.
 <item> marks [String, none|dots|points|various, default none]: style of marks used for data points
 <item> bars [Boolean, default false]: If true, turns bar charts on.
 <item> bars.width [Float]: Width of bars in a bar chart. Can be used alternatively
 to 'bars' option. Bars can be specified by width and offset values.
 <item> bars.offset [Float, default 0.0]: Offset per data set in a bar chart.
 <item> persistence [Integer]: If positive integer, sets persistence (i.e. number of displayed points)
 to that value. If absent or less than or equal to zero, it displays all points.
 </ulist>
 *
 *
 * @version 11/03/99
 * @version 07/07/00 MG changed to Ptplot 3.1; "marks" and "bars" options
 * @author JWJ
 */

public class Plotter extends AbstractDiscreteEventComponent {

  private PlotFrame  frame;
  private List       dataSets;
  private List       sampleCounts;

  private MessageProducer out;

  private List   listX;
  private List   listY;

  private Scheduler scheduler;


  //
  // DEC
  //

  public  void    initializeState(double t, Scheduler s) {

    dataSets = new ArrayList();
    sampleCounts = new ArrayList();

    Plot plot = (Plot) frame.plot;

    plot.clear(true);

    plot.setTitle(plotTitle);
    if (plotXLabel != null) plot.setXLabel(plotXLabel);
    if (plotYLabel != null) plot.setYLabel(plotYLabel);
    plot.setXLog(plotXLog);
    plot.setYLog(plotYLog);

    plot.setImpulses(isImpulse);
    plot.setMarksStyle(plotMarks);
    plot.setPointsPersistence(nPersistence);

    if (givenBSpec)
      plot.setBars(bWidth, bOffset);
    else if (hasBars)
      plot.setBars(hasBars);

    plot.repaint();
    Thread.currentThread().yield();

  }

  public boolean isInitialized() { return scheduler != null; }


  //
  //  add data points
  //

  private void addPoint(int n, double x, double y, boolean conn) {
    //place in try catch as the user may close the plot window
    try {
      ((Plot)frame.plot).addPoint(n, x, y, conn);

      if (nRedraw > 0 && nSamples % nRedraw == 0) {
        frame.plot.repaint();
        Thread.currentThread().yield();
      }
    } catch (Exception e) {
      System.err.println("Error in PtPlot addPoint: " + e);
    }
  }


  private int newDataSet(Object k) {
    int n = dataSets.size();
    dataSets.add(k);
    sampleCounts.add(new Integer(0));
    if (k != null && hasLegend) {
      frame.plot.addLegend(n, k.toString());
      frame.plot.repaint();
      //Thread.currentThread().yield();
    }
    return n;
  }

  private int dataSet(Object k) {
    for (int i = 0; i < dataSets.size(); i++) {
    	Object dsk = dataSets.get(i);
    	if (k == null && dsk == null)
        	return i;
        if (k != null && k.equals(dataSets.get(i)))
        	return i;
    }
    return -1;
  }

  //
  // I/O
  //

  	private int nSamples;

  	class DataInput extends AbstractMessageListener {

    	public void message(Object msg, double time, Object source) {

      		Object lbl = null;
      		boolean doAdd = true;
      		boolean first;
      		int n;
      		double x = 0;
      		double y = 0;

      		if (mode == mdXY) {
      			List l = (List) msg;
      			switch(l.size()) {
      			case 2:
  					List xy;
      				if (l.get(1) instanceof List) {
      					lbl = l.get(0);
      					xy = (List)l.get(1);
      				} else {
      					xy = l;
      				}
					x = ((Number)xy.get(0)).doubleValue();
					y = ((Number)xy.get(1)).doubleValue();      					
      				break;
      			case 3:
					lbl = l.get(0);
					x = ((Number)l.get(1)).doubleValue();
					y = ((Number)l.get(2)).doubleValue();
      				break;
      			default:
      				doAdd = false;
      				break;
      			}
	      		n = dataSet(lbl);
	      		first = (n < 0);

	      		if (first)
					n = newDataSet(lbl);
      		} else {
				if (msg instanceof Number) {
					lbl = source;
					y = ((Number)msg).doubleValue();
	      		} else if (msg instanceof List) {
					List l = (List)msg;
					lbl = l.get(0);
					y = ((Number)l.get(1)).doubleValue();
	      		}
	
	      		n = dataSet(lbl);
	      		first = (n < 0);

	      		if (first)
					n = newDataSet(lbl);
	
	      		if (mode != mdSequence)
					x = time;
	      		else {
					int cnt = ((Integer)sampleCounts.get(n)).intValue();
					sampleCounts.set(n, new Integer(cnt + 1));
					x = cnt;
	      		}
	
	      		nSamples += 1;
      		}
      		if (doAdd) {
      			addPoint(n, x, y, isConnected & !first);
      		}
    	}

  	}

  class RedrawInput extends AbstractMessageListener {

    public void message(Object msg, double time, Object source) {
      frame.plot.repaint();
    }
  }

  //
  // options
  //

  private void setOptions(Map m) {
    if (m.containsKey(csTime))
      mode = ((Boolean)m.get(csTime)).booleanValue() ? mdTime : mdSequence;
    else
      mode = mdTime;
    
    if (m.containsKey(csMode)) {
    	String s = (String)m.get(csMode);
    	if (msTime.equals(s)) {
    		mode = mdTime;
    	} else if (msSequence.equals(s)) {
    		mode = mdSequence;
    	} else if (msXY.equals(s)){
    		mode = mdXY;
    	} else {
    		mode = mdTime;
    	}
    }

    if (m.containsKey(csAutoRedraw))
        nRedraw = ((Number)m.get(csAutoRedraw)).intValue();
      else
        nRedraw = 0;

    if (m.containsKey(csPersistence))
        nPersistence = ((Number)m.get(csPersistence)).intValue();
      else
        nPersistence = 0;

    if (m.containsKey(csConnected))
      isConnected = ((Boolean)m.get(csConnected)).booleanValue();
    else
      isConnected = true;

    if (m.containsKey(csBars))
      hasBars = ((Boolean)m.get(csBars)).booleanValue();
    else
      hasBars = false;

    if (m.containsKey(csBarsWidth)) {
      bWidth = ((Number)m.get(csBarsWidth)).doubleValue();
      givenBSpec = true;
    }
    else
      givenBSpec = false;
    if (givenBSpec && m.containsKey(csBarsOffset))
      bOffset = ((Number)m.get(csBarsOffset)).doubleValue();
    else if (givenBSpec)
      bOffset = 0.0;

    if (m.containsKey(csLegend))
      hasLegend = ((Boolean)m.get(csLegend)).booleanValue();
    else
      hasLegend = true;

    Plot plot = (Plot)frame.plot;
    

    if (m.containsKey(csImpulse))
      isImpulse = ((Boolean)m.get(csImpulse)).booleanValue();
    else
      isImpulse = false;

    if (m.containsKey(csTitle))
      plotTitle = m.get(csTitle).toString();
    else {
    	switch (mode) {
    	case mdTime:
    		plotTitle = "Time Plot";
    		break;
    	case mdSequence:
    		plotTitle = "Sequence/Event Plot";
    		break;
    	case mdXY:
    		plotTitle = "XY Plot";
    		break;
    	default:
    		plotTitle = "Plot";
    		break;
    	}
    }

    if (m.containsKey(csXLabel))
      plotXLabel = m.get(csXLabel).toString();
    else
      plotXLabel = null;

    if (m.containsKey(csYLabel))
      plotYLabel = m.get(csYLabel).toString();
    else
      plotYLabel = null;

    if (m.containsKey(csMarks))
      plotMarks = m.get(csMarks).toString();
    else
      plotMarks = "none";

    if (m.containsKey(csXLog))
      plotXLog = ((Boolean)m.get(csXLog)).booleanValue();
    else
      plotXLog =  false;

    if (m.containsKey(csYLog))
      plotYLog = ((Boolean)m.get(csYLog)).booleanValue();
    else
      plotYLog =  false;
  }

  private int      nRedraw;
  private int      nPersistence;
  private boolean  isConnected;
  private boolean  isImpulse;
  private boolean  hasLegend;
  private boolean  customTitle;
  private String   plotTitle;
  private String   plotXLabel;
  private String   plotYLabel;
  private boolean  plotXLog;
  private boolean  plotYLog;
  private String   plotMarks;
  private boolean  hasBars;
  private boolean  givenBSpec;
  private double   bWidth;
  private double   bOffset;
  private int      mode;
  
  private static final int  mdTime = 1;
  private static final int  mdSequence = 2;
  private static final int  mdXY = 3;
  

  private static final String csTime = "time";
  private static final String csAutoRedraw = "autoredraw";
  private static final String csPersistence = "persistence";
  private static final String csConnected = "connected";
  private static final String csLegend = "legend";
  private static final String csBars = "bars";
  private static final String csBarsWidth = "bars.width";
  private static final String csBarsOffset = "bars.offset";

  private static final String csImpulse = "impulse";
  private static final String csTitle = "title";
  private static final String csXLabel = "xlabel";
  private static final String csYLabel = "ylabel";
  private static final String csXLog = "xlog";
  private static final String csYLog = "ylog";
  private static final String csMarks = "marks";
  private static final String csMode = "mode";
  
  private static final String msTime = "time";
  private static final String msSequence = "sequence";
  private static final String msXY = "xy";
  

  //
  // ctor
  //

  public Plotter() { this(new HashMap()); }

  public Plotter(Map options) {
    inputs.addConnector("Data", new DataInput());
    inputs.addConnector("Redraw", new RedrawInput());
    frame = new PlotFrame("PtPlot", new Plot());
    setOptions(options);
    frame.setVisible(true);
  }
  
  public Plotter(Object args) {
	  this ((Map)args);
  }

  //
  //
  //


  public Object inputData() { return null; }
  public Object inputRedraw() { return null; }
}
