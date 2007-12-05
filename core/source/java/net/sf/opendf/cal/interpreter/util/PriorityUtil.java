/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.caltrop.cal.interpreter.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.ast.Action;
import net.sf.caltrop.cal.ast.Actor;
import net.sf.caltrop.cal.ast.QID;


/**
 * This class contains a set of static methods that deal with priority specifications
 * in actors.
 * 
 * Orders are specified either by passing the actor, or by passing an "order map." The keys
 * of an order map are <tt>Action</tt>s, the values are sets of <tt>Action</tt>s, viz. those
 * actions that precede the key in the order. 
 * 
 * The method {#computeActionPriorities(Actor) computeActionPriorities} is used to compute
 * the order map of an actor.
 * 
 * @author jornj
 */
public class PriorityUtil {


	/**
	 * Produces an array of the actions in the specified actor which is
	 * sorted according to the priority order of the actor. It uses Java's 
	 * stable sorting facility, hence actions that are not prioritized 
	 * relative to each other should remain in document order. 
	 * 
	 * @param a
	 * @return
	 */
	
	public static Action []  prioritySortActions(Actor a) {
	
		Map order = PriorityUtil.computeActionPriorities(a);
		
		Action [] actions = a.getActions();
		Action [] sortedActions = new Action [actions.length];
		Comparator comp = new PriorityUtil.PriorityComparator(order);
		for (int i = 0; i < actions.length; i ++) {
			int k = i;
			int j = i - 1;
			while (j >= 0) {
				if (comp.compare(actions[i], sortedActions[j]) < 0) {
					k = j;
				}
				j -= 1;
			}
			insertAction(sortedActions, i, actions[i], k);
		}

		return sortedActions;
	}
	
	private static void insertAction(Action [] actions, int len, Action a, int pos) {
		assert pos <= len;

		int i = len;
		while (pos < i) {
			actions[i] = actions[i - 1];
			i -= 1;
		}
		actions[pos] = a;
	}

	/**
	 * A comparator that relates actions according to a specified order map.
	 * 
	 * @author jornj
	 */

	static class PriorityComparator implements Comparator {
		
			public int compare(Object a, Object b) {
		
				Set sa = (Set)order.get(a);
				Set sb = (Set)order.get(b);
				if (sa.contains(b)) {
					if (sb.contains(a)) { // ERROR!
						return 0;
					} else {
						return 1;
					}
				} else {
					if (sb.contains(a)) { 
						return -1;
					} else {
						return 0;
					}
				}
			}
			
			PriorityComparator(Map order) {
				this.order = order;
			}
			
			Map order;
		}

	/**
	 * Return a map (the "order map") that contains for every action in the specified actor 
	 * a set which in turn contains all actions that have a higher 
	 * priority than the respective action (according to the priority
	 * settings in the actor).
	 * 
	 * If such a set contains the action itself, the priorities lead to
	 * a circular ordering, which is an error.
	 * 
	 * Note that the sets contain <it>all</it> actions of higher priority. 
	 * As a consequence, the size of the set itself is an indicator of the priority
	 * of an action---if A has a lower priority than B, the set associated with
	 * A is strictly larger than that of B (of course, the inverse need not be true).
	 * 
	 * Hence topologcal sorting can be done based on the size of the associated set, or
	 * (somewhat more precisely) on proper set inclusion.
	 */
	
	public static Map computeActionPriorities(Actor a) {
		
		Action [] actions = a.getActions();
		List [] priorities = a.getPriorities();
	
		Map order = new HashMap();
		for (int i = 0; i < actions.length; i++)
			order.put(actions[i], new HashSet());
		
		for (int i = 0; i < actions.length; i++) {
			for (int j = 0; j < actions.length; j++) {
				if (isOrdered(actions[i], actions[j], priorities)) {
					Set s = (Set)order.get(actions[j]);
					s.add(actions[i]);
				}
			}
		}
		
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Iterator i = order.keySet().iterator(); i.hasNext(); ) {
				Action ca = (Action)i.next();
				Set caSet = (Set)order.get(ca);
				int caSize = caSet.size();
				for (Iterator j = new HashSet(caSet).iterator(); j.hasNext(); ) {
					Action x = (Action)j.next();
					caSet.addAll((Set)order.get(x));
				}
				if (caSize != caSet.size())
					changed = true;
			}
		}
		
		return order;
	}
	
	/**
	 * Determines whether the first action has precedence over the second, 
	 * according to the specified priority lists.
	 * 
	 * @param a
	 * @param b
	 * @param p
	 * @return
	 */
	public static boolean  isOrdered(Action a, Action b, List [] p) {
		
		QID tagA = a.getTag();
		QID tagB = b.getTag();
		if (tagA == null || tagB == null)
			return false;
		for (int i = 0; i < p.length; i++) {
			List pi = p[i];
			int posA = findPrefixInList(tagA, pi, 0);
			if (posA >= 0) {
				if (findPrefixInList(tagB, pi, posA + 1) >= 0)
					return true;
			}
		}	
		return false;
	}

	/**
	 * Determine whether the specified actor contains priority clauses.
	 */

	public static boolean  hasPriorityOrder(Actor a) {
		List [] p = a.getPriorities();
		return (p != null) && (p.length > 0);
	}

	/**
	 * Determine whether the specified priority order is valid, i.e. non-cyclic.
	 */
	
	public static boolean isValidPriorityOrder(Map order) {
		for (Iterator i = order.keySet().iterator(); i.hasNext(); ) {
			Object a = i.next();
			Set aSet = (Set)order.get(a);
			if (aSet.contains(a))
				return false;
		}
		return true;
	}

	/**
	 * Determine whether the priority order of the specified actor is valid,
	 * i.e. non-cyclic.
	 */
	
	public static boolean isValidPriorityOrder(Actor a) {
		return isValidPriorityOrder(PriorityUtil.computeActionPriorities(a));
	}
	
	
	/**
	 * DEBUG---write a representation of the order map to System.out.
	 */

	private static void  printOrder(String s, Map order) {
		
		System.out.println(s + " " + order.keySet());
		for (Iterator i = order.keySet().iterator(); i.hasNext(); ) {
			Action a = (Action)i.next();
			System.out.print(" " + a.getTag() + " ---");
			Set d = (Set)order.get(a);
			for (Iterator j = d.iterator(); j.hasNext(); ) {
				Action b = (Action)j.next();
				System.out.print(" " + b.getTag());
			}
			System.out.println(".");
		}
	}

	/**
	 * Determines the index of the first entry in the list which is a prefix of
	 * the specified QID, and is not smaller than the specified number. Returns
	 * a negative number if such an entry does not exist.
	 * 
	 * @param q A QID.
	 * @param p A list of QIDs.
	 * @param n The start index for the search.
	 * @return The index of the first prefix of q in p not smaller than n, -1 if none.
	 */
	
	private static int  findPrefixInList(QID q, List p, int n) {
		for (int i = n; i < p.size(); i++) {
			QID a = (QID)p.get(i);
			if (a.isPrefixOf(q)) {
				return i;
			}
		}
		return -1;
	}

}
