package net.sf.opendf.profiler.schedule.gui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import net.sf.opendf.profiler.schedule.Duration;
import net.sf.opendf.profiler.schedule.ScheduleOutput;
import net.sf.opendf.profiler.schedule.Time;
import net.sf.opendf.profiler.schedule.gui.views.ResourceUseView;

/**
 * 
 * @author jornj
 */
public class TraceAnalyzer {
	
	public static void  setDataSource(DataSource ds) {
		dataSource = ds;

    	//
    	// test
    	//
		if (dataSource != null) {
	    	JInternalFrame frame = new JInternalFrame("test", true);
	    	frame.setClosable(true);
	        frame.setVisible(true);
	    	gui.getDesktop().add(frame);

	    	JScrollPane testsp = new JScrollPane();
	    	frame.setContentPane(testsp);
	    	
	        JComponent chartPanel = (JComponent)((List)new ResourceUseViewBuilder().create(dataSource)).get(0);
	        testsp.setViewportView(chartPanel);
	        Dimension dim = chartPanel.preferredSize();
	        frame.pack();
	        chartPanel.setSize(dim);
		}
	}
	
	
	

	private static DataSource  dataSource; 
	
	class LoadTraceFileButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	class LoadResourceFileButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	class OpenViewsButtonListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	
    public static void main(String args[]) throws Exception {
    	createGUI();
    	gui.getResourceList().setListData(new String [] {"aaa1", "aaa2", "aaa3", "aaa4", "aaa5", "aaa6", "aaa7", "aaa8", "aaa9"});
    	gui.getScheduleViewList().setListData(scheduleViewNames);	
    	
    	gui.setVisible(true);
    }
	
    
    private static ViewBuilder [] scheduleViews = {new ResourceUseView.Builder()};
    private static String      [] scheduleViewNames = {"Resource use"};
    
    private static TraceAnalyzerGui gui;
    
    private static File traceFile;
    
    static void createGUI() {
		gui = new TraceAnalyzerGui();

		gui.getBrowseTraceFileButton().setAction(new BrowseTraceFileAction());
		gui.getStartButton().setAction(new StartAction());
		
		
    	
    }
    
    public static class BrowseTraceFileAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			int ret = fileChooser.showOpenDialog(gui);
			if (ret == JFileChooser.APPROVE_OPTION) {
				traceFile = fileChooser.getSelectedFile();
				gui.getTraceFileNameField().setText(traceFile.getName());
			}
		}
    }
    
    static JFileChooser fileChooser = new JFileChooser();
    
    public static class StartAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			
		}
    }
    
    public static class UpdateGuiAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			
			
		}
    	
    }
    
    public static class  ViewGeneratingScheduleOutput implements ScheduleOutput {

		public void start() {
		}

		public void executeStep(Object stepID, Time start, Duration duration) {
			// TODO Auto-generated method stub
			
		}

		public void beginStep(Object stepID, Time start, Duration duration) {
			// TODO Auto-generated method stub
			
		}

		public void endStep() {
			// TODO Auto-generated method stub
			
		}

		public void attribute(Object key, Object value) {
			// TODO Auto-generated method stub
			
		}

		public void finish(Time tm) {
			// TODO Auto-generated method stub
			
		}
		
		public List  getViews() {
			return null;
		}
		
//		public CompositeScheduleOutput(List views) {
//			
//		}
//		
//		private 
    	
    }
    
}
