package net.sf.opendf.profiler.schedule.gui;

import java.util.List;

import javax.swing.JComponent;

import org.jfree.chart.JFreeChart;

import net.sf.opendf.profiler.schedule.ScheduleOutput;

/**
 * 
 * @author jornj
 */
public interface ViewBuilder {
	
	public View     create();
	public boolean  isGroupViewBuilder();
}
