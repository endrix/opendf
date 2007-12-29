
package net.sf.opendf.profiler.schedule.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.*;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


import net.sf.opendf.profiler.schedule.data.ClassMapping;
import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.schedule.data.ResourceConstraint;
import net.sf.opendf.profiler.schedule.gui.GanttDemo2.MilliDateFormat;
import net.sf.opendf.util.logging.Logging;


public class ResourceUseChart extends ApplicationFrame
{

    public ResourceUseChart(InputStream data)
    {
        super("Resource Use");

        IntervalCategoryDataset intervalcategorydataset = readResourceUseData(data);

        JFreeChart chart = ChartFactory.createGanttChart("Resource Use", "Resource", "Time", intervalcategorydataset, true, true, false);
        CategoryPlot categoryplot = (CategoryPlot)chart.getPlot();

        DateAxis da = (DateAxis)categoryplot.getRangeAxis();
        da.setDateFormatOverride(new MilliDateFormat());

        
        CategoryItemRenderer categoryitemrenderer = categoryplot.getRenderer();
        categoryitemrenderer.setSeriesPaint(0, Color.blue);
        ChartPanel chartpanel = new ChartPanel(chart);
        chartpanel.setPreferredSize(new Dimension(500, 270));
        setContentPane(chartpanel);
    }


    private IntervalCategoryDataset readResourceUseData(InputStream data)
    {
    	final SortedMap  resourceSchedules = new TreeMap(new Comparator() {

			public int compare(Object o1, Object o2) {
				if ((o1 instanceof Resource) && (o2 instanceof Resource)) {
					Resource a = (Resource) o1;
					Resource b = (Resource) o2;
					if (a.classID.toString().equals(b.classID.toString())) {
						return a.instanceID - b.instanceID;
					} else {
						return a.classID.toString().compareTo(b.classID.toString());
					}
				} else
					return 0;
			}
    	});
    	
    	DefaultHandler saxHandler = new DefaultHandler() {    		
    		boolean step = false;
    		long start;
    		long duration;
    		String resourceClass = null;
    		int resourceInstance = -1;
    		
			public void startElement(String uri, String localName, String qName, Attributes attributes) {				
				try {
					if ("step".equals(qName)) {			
						Map a = createAttributeMap(attributes);
						if (step) 
							throw new RuntimeException("Illegal format: step within step.");
						step = true;
						start = Long.parseLong((String)a.get("start"));
						duration = Long.parseLong((String)a.get("duration"));
						return;
					}					
					if ("a".equals(qName)) {
						Map a = createAttributeMap(attributes);
						if (step && "resource-class".equals(a.get("key"))) {
							resourceClass = (String)a.get("value");
							return;
						}
						if (step && "resource-instance".equals(a.get("key"))) {
							resourceInstance = Integer.parseInt((String)a.get("value"));
							return;
						}
						return;
					}
					return;
				}
				catch (Exception e) {
                    Logging.dbg().throwing("ResourceUseChart", "startElement", e);
					throw new RuntimeException(e);
				}
			}
			
			public void endElement(String uri, String localName, String qName) {
				try {
					if ("step".equals(qName)) {			
						if (!step) 
							throw new RuntimeException("Illegal format: confused.");

						Interval interval = new Interval(start, start + duration);
						Resource r = new Resource(resourceClass, resourceInstance);
						List sched = (List)resourceSchedules.get(r);
						if (sched == null) {
							sched = new ArrayList();
							resourceSchedules.put(r, sched);
						}
						sched.add(interval);
						
						step = false;
						start = duration = -1;
						resourceClass = null;
						resourceInstance = -1;
						return;
					}					
					return;
				}
				catch (Exception e) {
                    Logging.dbg().throwing("ResourceUseChart", "endElement", e);
					throw new RuntimeException(e);
				}
			}			

			private Map  createAttributeMap(Attributes attributes) {
				Map a = new HashMap();
				for (int i = 0; i < attributes.getLength(); i++) {
					a.put(attributes.getQName(i), attributes.getValue(i));
				}
				return a;
			}			

    	};
    	
		try {
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			p.parse(data, saxHandler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

        TaskSeries tasks = new TaskSeries("");

        for (Iterator i = resourceSchedules.keySet().iterator(); i.hasNext(); ) {
        	Resource r = (Resource)i.next();
        	
        	List iList = (List)resourceSchedules.get(r);
        	Interval [] intervals = (Interval []) iList.toArray(new Interval [iList.size()]);
        	long a = intervals[0].a;
        	long b = intervals[intervals.length - 1].b;
        	
        	Task global = new Task(r.toString(), date(a), date(b));
        	for (int j = 0; j < intervals.length; j++) {
        		Task t = new Task(r.toString() + "::" + j, date(intervals[j].a), date(intervals[j].b));
        		global.addSubtask(t);
        	}
        	tasks.add(global);
        }
        TaskSeriesCollection taskseriescollection = new TaskSeriesCollection();
        taskseriescollection.add(tasks);
        return taskseriescollection;
    }

    private static Date date(long n)
    {
    	return new Date(n);
    }

    public static void main(String args[]) throws Exception 
    {
        ResourceUseChart chart = new ResourceUseChart(new FileInputStream(args[0]));
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }

    static class  MilliDateFormat extends DateFormat {

		public Date parse(String source, ParsePosition pos) {
			throw new RuntimeException("Cannot parse millisecond date.");
		}

		public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
			return toAppendTo.append(date.getTime());
		}
    	
    }
    
    static class Interval {
    	long a;
    	long b;
    	
    	Interval(long a, long b) {
    		this.a = a;
    		this.b = b;
    	}
    }
}
