package net.sf.caltrop.cli.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.caltrop.cli.ModelClassLocator;

public class DirectoryModelClassLocator implements ModelClassLocator {

	public InputStream getAsStream(String name) {
		String fname = name + dirpath;
		try {
			InputStream is = new FileInputStream(fname);
			return is;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public DirectoryModelClassLocator(String dirpath) {
		this.dirpath = dirpath + File.separator;
	}

	private String dirpath;
}
