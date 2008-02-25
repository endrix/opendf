package net.sf.opendf.hades.cal;

import junit.framework.TestCase;

import net.sf.opendf.cal.ast.*;
import net.sf.opendf.hades.des.*;
import net.sf.opendf.hades.des.schedule.*;

import java.util.*;


public class CalInterpreterTest extends TestCase
{

    protected void setUp () throws Exception
    {
        super.setUp();
    }

    private Actor buildActor ()
    {
        Actor a = new Actor ( new Import[0], "testActor", "", new Decl[0],
                new PortDecl[0], // inputs 
                new PortDecl[]{  // outputs
                new PortDecl("myOut",null)
        },
                new Action[0], new Action[0], new Decl[0]
                );
        return a;
    }
    
    public void testOutChannelBlock ()
    {
        TestCalInterp interp = new TestCalInterp(buildActor(), Collections.EMPTY_MAP);
        interp.initializeState(0, new NullScheduler());
        assertEquals(1, interp.getOutputConnectors().size());
        CalInterpreter.MosesOutputChannel mp = (CalInterpreter.MosesOutputChannel)interp.getOutputConnectors().getConnector("myOut");
        assertEquals(0, interp.getBlockedOutputs().size());

        Object blockSrc = new Object();
        
        // Send meaningless message
        mp.control(new Object(), new Object());
        interp.flushOutputChannels();
        assertEquals(0, interp.getBlockedOutputs().size());
        
        mp.control(ControlEvent.BLOCK, blockSrc); // BLOCK message
        interp.flushOutputChannels();
        assertEquals(1, interp.getBlockedOutputs().size());
        
        mp.control(ControlEvent.UNBLOCK, new Object()); // WRONG UNBLOCK message
        interp.flushBlockedOutputChannels();
        interp.flushOutputChannels();
        assertEquals(1, interp.getBlockedOutputs().size());
        
        mp.control(ControlEvent.UNBLOCK, blockSrc); // RIGHT UNBLOCK message
        interp.flushBlockedOutputChannels();
        interp.flushOutputChannels();
        assertEquals(0, interp.getBlockedOutputs().size());        
    }
    
    
    private static class TestCalInterp extends CalInterpreter
    {
        public TestCalInterp(Actor a, Map outsideEnv){ super(a, outsideEnv); }
        
        public Set getBlockedOutputs ()
        {
            return Collections.unmodifiableSet(this.blockedOutputChannels);
        }
    }

    // DOES NOTHING USEFULL
    private static class NullScheduler implements Scheduler
    {
        public void addPostfireHandler (PostfireHandler ph){}
        public long currentEventCount () { return 0; }
        public long currentStrongEventCount (){return 0;}
        public double currentTime (){return 0;}
        public int execute (){return 0;}
        public void finalizeSimulation (){}
        public Object getProperty (Object key){return null;}
        public boolean hasEvent (){return false;}
        public void initialize (){}
        public boolean isNextWeak (){return false;}
        public double nextEventTime (){return 0;}
        public void registerSimulationFinalizer (SimulationFinalizer sf){}
        public void removePostfireHandler (PostfireHandler ph){}
        public void schedule (double tm, double precedence, EventProcessor ep){}
        public void schedule (double tm, EventProcessor ep){}
        public Object setProperty (Object key, Object value) { return null; }
        public void unregisterSimulationFinalizer (SimulationFinalizer sf){}
        public void unschedule (EventProcessor ep){}
        
    }
}
