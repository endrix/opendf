/*
 * Xilinx Top Secret.
 *
 * Copyright (c) 2007 Xilinx, Inc.  All rights reserved.
 */
package net.sf.opendf.xslt.util;

import java.util.*;
import org.w3c.dom.Node;

/**
 * This class is the central point for general purpose (non CAL or NL
 * specific) callbacks from XSLT processing into the Java domain.
 * 
 * @author: imiller (imiller@xilinx.com)
 * <p>Created: Mon Dec 17 07:58:24 2007
 * 
 */
public class XSLTProcessCallbacks
{

    private static List<ProblemListenerIF> problemListeners = Collections.EMPTY_LIST;

    public static void registerProblemListener (ProblemListenerIF listener)
    {
        if (problemListeners.size() == 0)
            problemListeners = new ArrayList();
        problemListeners.add(listener);
    }

    public static boolean removeProblemListener (ProblemListenerIF listener)
    {
        boolean status = problemListeners.remove(listener);
        if (problemListeners.size() == 0)
            problemListeners = Collections.EMPTY_LIST;
        return status;
    }

    public static void reportProblem (Node report, String message)
    {
        for (ProblemListenerIF listener : problemListeners)
        {
            listener.report(report, message);
        }
    }
    
}

