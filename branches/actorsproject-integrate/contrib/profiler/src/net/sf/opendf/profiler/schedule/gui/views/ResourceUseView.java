package net.sf.opendf.profiler.schedule.gui.views;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;

import net.sf.opendf.profiler.schedule.Duration;
import net.sf.opendf.profiler.schedule.Time;
import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.schedule.gui.View;
import net.sf.opendf.profiler.schedule.gui.ViewBuilder;

public class ResourceUseView implements View {

	public JComponent createViewComponent() {
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
        		if (intervals[j].a != intervals[j].enabledSince) {
        			double delayCoeff = ((double)intervals[j].b - intervals[j].a) / ((double)intervals[j].b - intervals[j].enabledSince); 
        			t.setPercentComplete(delayCoeff);
        		}
        		global.addSubtask(t);
        	}
        	tasks.add(global);
        }
        TaskSeriesCollection taskseriescollection = new TaskSeriesCollection();
        taskseriescollection.add(tasks);

        
        JFreeChart chart = ChartFactory.createGanttChart("Resource Use", "Resource", "Time", taskseriescollection, true, true, false);
        CategoryPlot categoryplot = (CategoryPlot)chart.getPlot();

        DateAxis da = (DateAxis)categoryplot.getRangeAxis();
        da.setDateFormatOverride(new MilliDateFormat());

        
        CategoryItemRenderer categoryitemrenderer = categoryplot.getRenderer();
        categoryitemrenderer.setSeriesPaint(0, Color.blue);
        
        return new ChartPanel(chart);
	}

	public boolean isGroupView() {
		return false;
	}
	
	

	public void start() {
		
    	resourceSchedules = new TreeMap(new Comparator() {

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
    	
    	step = false;

	}

	public void executeStep(Object stepID, Time start, Duration duration) {
		beginStep(stepID, start, duration);
		endStep();
	}

	public void beginStep(Object stepID, Time start, Duration duration) {
		if (step) 
			throw new RuntimeException("Step already acive.");
		
		this.step = true;
		this.start = start.value();
		this.duration = duration.value();
		this.resourceClass = null;
		this.resourceInstance = -1;
		this.enabledSince = -1;
	}

	public void endStep() {
		if (step && (resourceClass != null) && (resourceInstance >= 0) && (enabledSince >= 0)) {
			Interval interval = new Interval(start, start + duration, enabledSince);
			Resource r = new Resource(resourceClass, resourceInstance);
			List sched = (List)resourceSchedules.get(r);
			if (sched == null) {
				sched = new ArrayList();
				resourceSchedules.put(r, sched);
			}
			sched.add(interval);
		}
		step = false;
		start = duration = -1;
		resourceClass = null;
		resourceInstance = -1;
	}

	public void attribute(Object key, Object value) {
		if (step && "resource-class".equals(key)) {
			resourceClass = value.toString();
			return;
		}
		if (step && "resource-instance".equals(key)) {
			resourceInstance = Integer.parseInt((String)value);
			return;
		}
		if (step && "enabled-since".equals(key)) {
			enabledSince = Long.parseLong((String)value);
			return;
		}
	}

	public void finish(Time tm) {
	}

	
    private static Date date(long n)
    {
    	return new Date(n);
    }



	private SortedMap resourceSchedules;
	private boolean  step;
	private long     start;
	private long     duration;
	private String resourceClass;
	private int    resourceInstance;
	private long   enabledSince;
	
	
	
	public static class Builder implements ViewBuilder {

		public View create() {
			return new ResourceUseView();
		}

		public boolean isGroupViewBuilder() {
			return false;
		}		
	}
	
    static class Interval {
    	long a;
    	long b;
    	long enabledSince;
    	
    	Interval(long a, long b, long enabledSince) {
    		this.a = a;
    		this.b = b;
    		this.enabledSince = enabledSince;
    	}

    	Interval(long a, long b) {
    		this(a, b, a);
    	}
    }

}
