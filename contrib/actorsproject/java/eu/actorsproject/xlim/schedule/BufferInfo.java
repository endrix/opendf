/* -*-Java-*- */                                         

/*
 * Copyright (C) 2011  Anders Nilsson <andersn@control.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

package eu.actorsproject.xlim.schedule;

public class BufferInfo {
	public int ix;
	public int size;

	public BufferInfo(Integer ix, int size) {
		this.ix = ix;
		this.size = size;
	}
}