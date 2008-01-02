/*
 * Xilinx Top Secret.
 *
 * Copyright (c) 2007 Xilinx, Inc.  All rights reserved.
 */
package net.sf.opendf.xslt.util;

import org.w3c.dom.Node;

/**
 * This interface is used by classes needing to receive output from
 * the semantic checks XSLT process.  This interface is triggered via
 * calls to {@link XSLTProcessCallbacks} from
 * callbackProblemSummary.xslt.
 * 
 * @author: imiller (imiller@xilinx.com)
 * <p>Created: Mon Dec 17 08:55:08 2007
 *
 */
public interface NodeListenerIF
{
    /**
     * Called once for each relevent Note in the source XML.
     */
    void report (Node report, String message);
}
