package net.sf.opendf.profiler.schedule.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JDesktopPane;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;

/**
 * 
 * @author jornj
 */
public class TraceAnalyzerGui extends JFrame {

	private javax.swing.JPanel jContentPane = null;
	private JTabbedPane jTabbedPane = null;
	private JPanel jPanel = null;
	private JPanel jPanel1 = null;
	private JSplitPane jSplitPane = null;
	private JPanel jPanel2 = null;
	private JScrollPane jScrollPane = null;
	private JList resourceConfigurationList = null;
	private JButton addResourceConfigurationButton = null;
	private JButton removeResourceConfigurationButton = null;
	private JPanel jPanel3 = null;
	private JSplitPane jSplitPane1 = null;
	private JPanel jPanel4 = null;
	private JPanel jPanel5 = null;
	private JButton startButton = null;
	private JProgressBar progressBar = null;
	private JCheckBox depDFCheckBox = null;
	private JCheckBox depAllCheckBox = null;
	private JLabel jLabel1 = null;
	private JPanel jPanel7 = null;
	private JLabel jLabel2 = null;
	private JSplitPane jSplitPane2 = null;
	private JPanel jPanel8 = null;
	private JPanel jPanel9 = null;
	private JLabel jLabel = null;
	private JLabel jLabel3 = null;
	private JLabel jLabel4 = null;
	private JSplitPane jSplitPane3 = null;
	private JPanel jPanel6 = null;
	private JScrollPane jScrollPane1 = null;
	private JList scheduleViewList = null;
	private JPanel jPanel10 = null;
	private JScrollPane jScrollPane2 = null;
	private JList groupViewList = null;
	private JPanel jPanel11 = null;
	private JPanel jPanel12 = null;
	private JTextField traceFileNameField = null;
	private JButton browseTraceFileButton = null;
	private JSplitPane jSplitPane4 = null;
	private JPanel jPanel13 = null;
	private JDesktopPane desktop = null;
	private JPanel jPanel14 = null;
	private JPanel jPanel15 = null;
	private JSplitPane jSplitPane5 = null;
	private JPanel jPanel17 = null;
	private JSplitPane jSplitPane6 = null;
	private JPanel jPanel18 = null;
	private JScrollPane jScrollPane5 = null;
	private JList actorInstancesList = null;
	private JPanel jPanel19 = null;
	private JScrollPane jScrollPane6 = null;
	private JList actorClassesList = null;
	private JPanel jPanel16 = null;
	private JScrollPane jScrollPane3 = null;
	private JList resourceList = null;
	/**
	 * This is the default constructor
	 */
	public TraceAnalyzerGui() {
		super();
		initialize();
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(586, 538);
		this.setContentPane(getJContentPane());
		this.setTitle("Trace Analyzer");
	}
	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJContentPane() {
		if(jContentPane == null) {
			jContentPane = new javax.swing.JPanel();
			jContentPane.setLayout(new java.awt.BorderLayout());
			jContentPane.add(getJTabbedPane(), java.awt.BorderLayout.NORTH);
		}
		return jContentPane;
	}
	/**
	 * This method initializes jTabbedPane	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
			jTabbedPane.setName("");
			jTabbedPane.addTab("Setup", null, getJPanel11(), null);
			jTabbedPane.addTab("Views", null, getJPanel(), null);
		}
		return jTabbedPane;
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints1.gridy = 0;
			gridBagConstraints1.weightx = 1.0D;
			gridBagConstraints1.weighty = 1.0D;
			gridBagConstraints1.gridx = 0;
			jPanel = new JPanel();
			jPanel.setLayout(new GridBagLayout());
			jPanel.add(getDesktop(), gridBagConstraints1);
			jPanel.setVisible(true);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jLabel2 = new JLabel();
			jLabel2.setFont(new java.awt.Font("Garamond", java.awt.Font.BOLD, 24));
			jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			jLabel2.setText("Scheduler");
			jLabel2.setForeground(java.awt.Color.blue);
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BoxLayout(getJPanel1(), BoxLayout.Y_AXIS));
			jPanel1.add(jLabel2, null);
			jPanel1.add(getJSplitPane(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes jSplitPane	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane.setBottomComponent(getJPanel3());
			jSplitPane.setTopComponent(getJPanel2());
		}
		return jSplitPane;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.gridy = 2;
			gridBagConstraints5.gridx = 1;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints4.gridy = 2;
			gridBagConstraints4.gridx = 0;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints3.gridwidth = 2;
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.weighty = 1.0;
			gridBagConstraints3.insets = new java.awt.Insets(5,23,5,23);
			jPanel2 = new JPanel();
			jPanel2.setLayout(new GridBagLayout());
			jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Resource configurations", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Arial", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), java.awt.Color.black));
			jPanel2.add(getJScrollPane(), gridBagConstraints3);
			jPanel2.add(getAddResourceConfigurationButton(), gridBagConstraints4);
			jPanel2.add(getRemoveResourceConfigurationButton(), gridBagConstraints5);
		}
		return jPanel2;
	}
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getResourceConfigurationList());
		}
		return jScrollPane;
	}
	/**
	 * This method initializes jList	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getResourceConfigurationList() {
		if (resourceConfigurationList == null) {
			resourceConfigurationList = new JList();
		}
		return resourceConfigurationList;
	}
	/**
	 * This method initializes jButton1	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddResourceConfigurationButton() {
		if (addResourceConfigurationButton == null) {
			addResourceConfigurationButton = new JButton();
			addResourceConfigurationButton.setText("Add...");
		}
		return addResourceConfigurationButton;
	}
	/**
	 * This method initializes jButton2	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getRemoveResourceConfigurationButton() {
		if (removeResourceConfigurationButton == null) {
			removeResourceConfigurationButton = new JButton();
			removeResourceConfigurationButton.setText("Remove");
		}
		return removeResourceConfigurationButton;
	}
	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints9.weighty = 1.0;
			gridBagConstraints9.weightx = 1.0;
			jPanel3 = new JPanel();
			jPanel3.setLayout(new GridBagLayout());
			jPanel3.add(getJSplitPane1(), gridBagConstraints9);
		}
		return jPanel3;
	}
	/**
	 * This method initializes jSplitPane1	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane1() {
		if (jSplitPane1 == null) {
			jSplitPane1 = new JSplitPane();
			jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane1.setBottomComponent(getJPanel5());
			jSplitPane1.setTopComponent(getJPanel4());
		}
		return jSplitPane1;
	}
	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 1;
			gridBagConstraints7.gridy = 0;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.gridy = 0;
			jPanel4 = new JPanel();
			jPanel4.setLayout(new GridBagLayout());
			jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Dependencies", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), null));
			jPanel4.add(getDepDFCheckBox(), gridBagConstraints6);
			jPanel4.add(getDepAllCheckBox(), gridBagConstraints7);
		}
		return jPanel4;
	}
	/**
	 * This method initializes jPanel5	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel5() {
		if (jPanel5 == null) {
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 0;
			gridBagConstraints8.gridy = 0;
			jLabel1 = new JLabel();
			jLabel1.setText("<TBD>");
			jPanel5 = new JPanel();
			jPanel5.setLayout(new GridBagLayout());
			jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Schedulers", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Arial", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), null));
			jPanel5.add(jLabel1, gridBagConstraints8);
		}
		return jPanel5;
	}
	/**
	 * This method initializes jButton5	
	 * 	
	 * @return javax.swing.JButton	
	 */
	public JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton();
			startButton.setText("Start");
		}
		return startButton;
	}
	/**
	 * This method initializes jProgressBar	
	 * 	
	 * @return javax.swing.JProgressBar	
	 */
	public JProgressBar getProgressBar() {
		if (progressBar == null) {
			progressBar = new JProgressBar();
		}
		return progressBar;
	}
	/**
	 * This method initializes depDFCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	public JCheckBox getDepDFCheckBox() {
		if (depDFCheckBox == null) {
			depDFCheckBox = new JCheckBox();
			depDFCheckBox.setText("dataflow");
		}
		return depDFCheckBox;
	}
	/**
	 * This method initializes depAllCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	public JCheckBox getDepAllCheckBox() {
		if (depAllCheckBox == null) {
			depAllCheckBox = new JCheckBox();
			depAllCheckBox.setText("all");
		}
		return depAllCheckBox;
	}
	/**
	 * This method initializes jPanel7	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel7() {
		if (jPanel7 == null) {
			jLabel3 = new JLabel();
			jLabel3.setFont(new java.awt.Font("Garamond", java.awt.Font.BOLD, 24));
			jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			jLabel3.setText("Views");
			jLabel3.setForeground(java.awt.Color.blue);
			jPanel7 = new JPanel();
			jPanel7.setLayout(new BoxLayout(getJPanel7(), BoxLayout.Y_AXIS));
			jPanel7.add(jLabel3, null);
			jPanel7.add(getJSplitPane2(), null);
		}
		return jPanel7;
	}
	/**
	 * This method initializes jSplitPane2	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane2() {
		if (jSplitPane2 == null) {
			jSplitPane2 = new JSplitPane();
			jSplitPane2.setResizeWeight(0.33D);
			jSplitPane2.setLeftComponent(getJPanel8());
			jSplitPane2.setRightComponent(getJPanel9());
		}
		return jSplitPane2;
	}
	/**
	 * This method initializes jPanel8	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel8() {
		if (jPanel8 == null) {
			jLabel = new JLabel();
			jLabel.setFont(new java.awt.Font("Garamond", java.awt.Font.BOLD, 18));
			jLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
			jLabel.setText("view selection");
			jLabel.setForeground(java.awt.Color.blue);
			jPanel8 = new JPanel();
			jPanel8.setLayout(new BoxLayout(getJPanel8(), BoxLayout.Y_AXIS));
			jPanel8.setFont(new java.awt.Font("Dialog", java.awt.Font.ITALIC, 12));
			jPanel8.add(jLabel, null);
			jPanel8.add(getJSplitPane3(), null);
		}
		return jPanel8;
	}
	/**
	 * This method initializes jPanel9	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel9() {
		if (jPanel9 == null) {
			jLabel4 = new JLabel();
			jLabel4.setFont(new java.awt.Font("Garamond", java.awt.Font.BOLD, 18));
			jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
			jLabel4.setText("view filters");
			jLabel4.setForeground(java.awt.Color.blue);
			jPanel9 = new JPanel();
			jPanel9.add(jLabel4, null);
			jPanel9.add(getJSplitPane5(), null);
		}
		return jPanel9;
	}
	/**
	 * This method initializes jSplitPane3	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane3() {
		if (jSplitPane3 == null) {
			jSplitPane3 = new JSplitPane();
			jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane3.setBottomComponent(getJPanel10());
			jSplitPane3.setTopComponent(getJPanel6());
		}
		return jSplitPane3;
	}
	/**
	 * This method initializes jPanel6	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel6() {
		if (jPanel6 == null) {
			jPanel6 = new JPanel();
			jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), "Schedule views", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), java.awt.Color.white));
			jPanel6.add(getJScrollPane1(), null);
		}
		return jPanel6;
	}
	/**
	 * This method initializes jScrollPane1	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getScheduleViewList());
		}
		return jScrollPane1;
	}
	/**
	 * This method initializes scheduleViewList	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getScheduleViewList() {
		if (scheduleViewList == null) {
			scheduleViewList = new JList();
		}
		return scheduleViewList;
	}
	/**
	 * This method initializes jPanel10	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel10() {
		if (jPanel10 == null) {
			TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), "Schedule views", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), java.awt.Color.white);
			titledBorder.setTitleColor(java.awt.Color.black);
			titledBorder.setTitle("Group views");
			jPanel10 = new JPanel();
			jPanel10.setBorder(titledBorder);
			jPanel10.add(getJScrollPane2(), null);
		}
		return jPanel10;
	}
	/**
	 * This method initializes jScrollPane2	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane2() {
		if (jScrollPane2 == null) {
			jScrollPane2 = new JScrollPane();
			jScrollPane2.setViewportView(getGroupViewList());
		}
		return jScrollPane2;
	}
	/**
	 * This method initializes jList1	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getGroupViewList() {
		if (groupViewList == null) {
			groupViewList = new JList();
		}
		return groupViewList;
	}
	/**
	 * This method initializes jPanel11	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel11() {
		if (jPanel11 == null) {
			jPanel11 = new JPanel();
			jPanel11.setLayout(new BoxLayout(getJPanel11(), BoxLayout.Y_AXIS));
			jPanel11.add(getJPanel14(), null);
		}
		return jPanel11;
	}
	/**
	 * This method initializes jPanel12	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel12() {
		if (jPanel12 == null) {
			jPanel12 = new JPanel();
			jPanel12.setLayout(new BoxLayout(getJPanel12(), BoxLayout.X_AXIS));
			jPanel12.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
			jPanel12.add(getBrowseTraceFileButton(), null);
			jPanel12.add(getTraceFileNameField(), null);
		}
		return jPanel12;
	}
	/**
	 * This method initializes jTextField1	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	public JTextField getTraceFileNameField() {
		if (traceFileNameField == null) {
			traceFileNameField = new JTextField();
		}
		return traceFileNameField;
	}
	/**
	 * This method initializes jButton3	
	 * 	
	 * @return javax.swing.JButton	
	 */
	public JButton getBrowseTraceFileButton() {
		if (browseTraceFileButton == null) {
			browseTraceFileButton = new JButton();
			browseTraceFileButton.setText("Browse trace file: ");
			browseTraceFileButton.setActionCommand("");
			browseTraceFileButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		}
		return browseTraceFileButton;
	}
	/**
	 * This method initializes jSplitPane4	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane4() {
		if (jSplitPane4 == null) {
			jSplitPane4 = new JSplitPane();
			jSplitPane4.setResizeWeight(0.5D);
			jSplitPane4.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
			jSplitPane4.setDividerSize(5);
			jSplitPane4.setLeftComponent(getJPanel1());
			jSplitPane4.setRightComponent(getJPanel7());
		}
		return jSplitPane4;
	}
	/**
	 * This method initializes jPanel13	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel13() {
		if (jPanel13 == null) {
			jPanel13 = new JPanel();
			jPanel13.setLayout(new BoxLayout(getJPanel13(), BoxLayout.X_AXIS));
			jPanel13.add(getStartButton(), null);
			jPanel13.add(getProgressBar(), null);
		}
		return jPanel13;
	}
	/**
	 * This method initializes desktop	
	 * 	
	 * @return javax.swing.JDesktopPane	
	 */
	public JDesktopPane getDesktop() {
		if (desktop == null) {
			desktop = new JDesktopPane();
		}
		return desktop;
	}
	/**
	 * This method initializes jPanel14	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel14() {
		if (jPanel14 == null) {
			jPanel14 = new JPanel();
			jPanel14.setLayout(new BoxLayout(getJPanel14(), BoxLayout.Y_AXIS));
			jPanel14.add(getJPanel12(), null);
			jPanel14.add(getJPanel15(), null);
			jPanel14.add(getJPanel13(), null);
		}
		return jPanel14;
	}
	/**
	 * This method initializes jPanel15	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel15() {
		if (jPanel15 == null) {
			jPanel15 = new JPanel();
			jPanel15.setLayout(new BoxLayout(getJPanel15(), BoxLayout.X_AXIS));
			jPanel15.add(getJSplitPane4(), null);
		}
		return jPanel15;
	}
	/**
	 * This method initializes jSplitPane5	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane5() {
		if (jSplitPane5 == null) {
			jSplitPane5 = new JSplitPane();
			jSplitPane5.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane5.setTopComponent(getJPanel16());
			jSplitPane5.setBottomComponent(getJPanel17());
			jSplitPane5.setResizeWeight(0.33D);
		}
		return jSplitPane5;
	}
	/**
	 * This method initializes jPanel17	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel17() {
		if (jPanel17 == null) {
			jPanel17 = new JPanel();
			jPanel17.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
			jPanel17.add(getJSplitPane6(), null);
		}
		return jPanel17;
	}
	/**
	 * This method initializes jSplitPane6	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane6() {
		if (jSplitPane6 == null) {
			jSplitPane6 = new JSplitPane();
			jSplitPane6.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			jSplitPane6.setResizeWeight(0.5D);
			jSplitPane6.setBottomComponent(getJPanel19());
			jSplitPane6.setTopComponent(getJPanel18());
		}
		return jSplitPane6;
	}
	/**
	 * This method initializes jPanel18	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel18() {
		if (jPanel18 == null) {
			jPanel18 = new JPanel();
			jPanel18.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), "Actor instances", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), java.awt.Color.black));
			jPanel18.add(getJScrollPane5(), null);
		}
		return jPanel18;
	}
	/**
	 * This method initializes jScrollPane5	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane5() {
		if (jScrollPane5 == null) {
			jScrollPane5 = new JScrollPane();
			jScrollPane5.setViewportView(getActorInstancesList());
		}
		return jScrollPane5;
	}
	/**
	 * This method initializes jList3	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getActorInstancesList() {
		if (actorInstancesList == null) {
			actorInstancesList = new JList();
		}
		return actorInstancesList;
	}
	/**
	 * This method initializes jPanel19	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel19() {
		if (jPanel19 == null) {
			jPanel19 = new JPanel();
			jPanel19.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED), "Actor classes", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), java.awt.Color.black));
			jPanel19.add(getJScrollPane6(), null);
		}
		return jPanel19;
	}
	/**
	 * This method initializes jScrollPane6	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane6() {
		if (jScrollPane6 == null) {
			jScrollPane6 = new JScrollPane();
			jScrollPane6.setViewportView(getActorClassesList());
		}
		return jScrollPane6;
	}
	/**
	 * This method initializes jList4	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getActorClassesList() {
		if (actorClassesList == null) {
			actorClassesList = new JList();
		}
		return actorClassesList;
	}
	/**
	 * This method initializes jPanel16	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel16() {
		if (jPanel16 == null) {
			jPanel16 = new JPanel();
			jPanel16.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Resources", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", java.awt.Font.BOLD | java.awt.Font.ITALIC, 12), null));
			jPanel16.add(getJScrollPane3(), null);
		}
		return jPanel16;
	}
	/**
	 * This method initializes jScrollPane3	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane3() {
		if (jScrollPane3 == null) {
			jScrollPane3 = new JScrollPane();
			jScrollPane3.setViewportView(getResourceList());
		}
		return jScrollPane3;
	}
	/**
	 * This method initializes jList1	
	 * 	
	 * @return javax.swing.JList	
	 */
	public JList getResourceList() {
		if (resourceList == null) {
			resourceList = new JList();
		}
		return resourceList;
	}
}  //  @jve:decl-index=0:visual-constraint="37,16"
