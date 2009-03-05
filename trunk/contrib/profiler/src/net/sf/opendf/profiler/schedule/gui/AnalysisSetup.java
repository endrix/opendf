package net.sf.opendf.profiler.schedule.gui;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author jornj
 */
public class AnalysisSetup {
	
	public File  traceFile;

	//
	//  scheduling setup
	//
	
	public List  resourceConfigurationFiles;
	public List  dependencyMasks;
	public List  schedulers;
	
	// 
	//  view setup
	//
	
	public List  scheduleViews;
	public List  groupViews;
	
	// 
	//  view filters
	//
	
	public List  resources;
	public List  actorInstances;
	public List  actorClasses;
}
