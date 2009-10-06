/* -*-Java-*- */                                         

/*
 * Copyright (C) 2009  Anders Nilsson <anders.nilsson@cs.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             

import xdfAST.Start;
import xdfAST.Instance;
import java.util.HashSet;

public class SSRAnalysis extends XdfParser {
	public static void main(String args[]) {
		Start ast = parse(args);

		ast.genSSR(System.out);
		HashSet<Instance> l = ast.genStaticSchedule(new HashSet<Instance>());
		System.out.println("\n\n");

		for (Instance i: l){
			do {
				System.out.print(i.name()+" -> ");
				i = i.next;
			} while (i != null);
			System.out.println();
		}

		System.out.println("\n\n");		
		String s = genScheduleXML(l);
		System.out.println(s);
	}


	static String genScheduleXML(HashSet<Instance> set) {
		StringBuffer sb = new StringBuffer();
		int ind = 0;
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<Schedule>\n");
		ind++;
		for (Instance i: set){
			sb.append(ind(ind));
			sb.append("<StaticSequence>\n");
			ind++;
			do {
				sb.append(ind(ind));
				sb.append("<Instance>");
				sb.append(i.name());
				sb.append("</Instance>\n");
				i = i.next;
			} while (i != null);
			ind--;
			sb.append(ind(ind));
			sb.append("</StaticSequence>\n");
		}		
		ind--;
		sb.append("</Schedule>\n");
		

		return sb.toString();
	}

	static String ind(int level) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<level; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}
}

