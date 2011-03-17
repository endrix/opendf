/* -*-Java-*- */                                         

/*
 * Copyright (C) 2011  Anders Nilsson <andersn@control.lth.se>
 *                                                              
 * This file is part of Actors model compiler.                      
 */                                                             
package eu.actorsproject.xlim.schedule;

import java.io.File;
import java.util.HashMap;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.schedule.ActionSchedule;
import eu.actorsproject.xlim.schedule.Classifier;
import eu.actorsproject.xlim.schedule.PhasePrinter;
import eu.actorsproject.xlim.schedule.StaticActionSchedule;
import eu.actorsproject.xlim.schedule.XlimPhasePrinterPlugIn;
import eu.actorsproject.xlim.util.InstructionPattern;


public class MergePhasePrinterPlugIn extends XlimPhasePrinterPlugIn {
		
	// We're looking for a pinPeek with an integer constant (index) as input
	// Though XLIM could express variable index, we always peek at a fixed offset into the FIFO 
	private InstructionPattern literalPattern=new InstructionPattern("$literal_Integer");
	private InstructionPattern pinPeekPattern=new InstructionPattern("pinPeek", literalPattern);
	private HashMap<String,String> portMap;
		
	public MergePhasePrinterPlugIn(HashMap<String,String> portMap) {
		super(true);  // true=byt namn på XlimOutputPorts (resultat av operationerna, inte actorportar)
		this.portMap = portMap;
	}
		
	@Override
		protected void printOperation(XlimOperation op) {
		if (pinPeekPattern.matches(op)) {
			// pinPeek mönstret matchar!
			XlimTopLevelPort port=op.getPortAttribute();
				
			// Här vill du kolla att porten motsvarar intern FIFO i den mergade actorn
			// För att illustrera kollar jag istället namnet på porten
			// if (port.getName().equals("Reset")) {
			System.out.println("** portMap="+portMap);
			if (portMap.containsKey(port.getName())) {
				// Så här gör du för att ta reda på index:
				XlimOperation literal=(XlimOperation) pinPeekPattern.getOperand(0, op);
				long index=literal.getIntegerValueAttribute();

				// Vi genererar output via XmlPrinter (landar till slut i strängen, som ModelCompiler får)
				XmlPrinter printer=getPrinter();  
					
				// Här skriver jag ut XLIM rakt av som text
				if (index==0)
					// printer.println("<operation kind=\"custom-peek-at-front\">");
					printer.println("<operation kind=\"noop\">");
				else
					printer.println("<operation kind=\"custom-peek-with-index\" and-the-index-is=\"" + index +"\">");
					
				// Här skrivs XlimOutputPort ut, med nytt (rätt!) namn  <port dir="out" source=... />
				// Någon av de operationer som du genererar ska ha samma ut-port som pinPeek!
					
				printer.increaseIndentation(); // Lyxar till det med indentering
				printer.printElement(op.getOutputPort(0));  
				printer.decreaseIndentation();
					
				printer.println("</operation>");
					
				return; // skriv inte ut den igen!
			}
		}
			
		// Otherwise print as usual
		super.printOperation(op);
	}
}
	
