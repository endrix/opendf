package net.sf.opendf.profiler.schedule.gui;

import javax.swing.JComponent;

import net.sf.opendf.profiler.schedule.ScheduleOutput;


/**
 * A View is a component that visualizes one or more schedules by producing a JComponent from the data they receive.
 * A view listens to the output of a schedule and produces a visualization at the end of the scheduling.
 * 
 * Group views are distinguished from views of individual schedules (schedule views) as follows: A schedule view
 * visualizes only one schedule at a time, and will thus erase the previous schedule's visualization when it starts 
 * a new one. Group views are intended to visualize aspects of sets of schedules.
 * 
 * @author jornj
 */

public interface View extends ScheduleOutput {
	
	public JComponent  createViewComponent();
	public boolean     isGroupView();
}
