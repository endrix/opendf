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
    public static final int SEMANTIC_CHECKS = 5;
    public static final int ACTOR_INSTANTIATION = 6;
    
    //private static List<NodeListenerIF> nodeListeners = Collections.EMPTY_LIST;
    private static Map<Integer, List<NodeListenerIF>> nodeListeners = new HashMap();


    /**
     * Register a listener for the given type of event.  The type is
     * defined by the static fields of this class.
     *
     * @param type, an int, one of the final ints of this class
     * @param listener, a NodeListenerIF object for handling the event
     */
    public static void registerListener (int type, NodeListenerIF listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot register a null listener");
        System.out.println("Registering " + type + " listener " + listener);
        List<NodeListenerIF> listeners = nodeListeners.get(type);
        if (listeners == null)
        {
            listeners = new ArrayList();
            nodeListeners.put(type,listeners);
        }
        listeners.add(listener);
    }

    /**
     * Returns true if the specified listener was successfully
     * removed.  A return of false may indicate that the specified
     * listener was not registered, or that it was not registered for
     * that type.
     */
    public static boolean removeListener (int type, NodeListenerIF listener)
    {
        List<NodeListenerIF> listeners = nodeListeners.get(type);
        if (listeners == null)
            return false;
        boolean status = listeners.remove(listener);
        if (listeners.size() == 0)
            nodeListeners.remove(type);
        return status;
    }

    //////////////////////////////
    //
    // Methods accessed from the XSLT transforms
    //
    //////////////////////////////
    
    public static void reportProblem (Node report, String message)
    {
        if (!nodeListeners.containsKey(SEMANTIC_CHECKS))
            return;
        
        for (NodeListenerIF listener : nodeListeners.get(SEMANTIC_CHECKS))
        {
            listener.report(report, message);
        }
    }

    /**
     * The specified node will the be the Actor element from the XDF
     * network.
     */
    public static void instantiateActor (Node actor, String message)
    {
        if (!nodeListeners.containsKey(ACTOR_INSTANTIATION))
            return;
        for (NodeListenerIF listener : nodeListeners.get(ACTOR_INSTANTIATION))
        {
            listener.report(actor, message);
        }
    }
    
}

