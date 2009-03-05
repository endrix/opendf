package net.sf.opendf.config;

import java.io.File;
import java.util.*;

import junit.framework.TestCase;

public class ConfigSetTest extends TestCase
{
    final String id = "test.set";
    final String cla = "-tset";
    final String name = "Test set";
    final String desc = "Just a test set";

    protected void setUp () throws Exception
    {
        super.setUp();
    }
    
    public void testConstructor ()
    {
        Set defaultValue = Collections.singleton("7");
        ConfigSet config = new ConfigSet(id, name, cla, desc, true, defaultValue);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
        Set readDefaultValue = ((ConfigSet)config).getValue();
        assertEquals(readDefaultValue.iterator().next(), defaultValue.iterator().next());
        try {
            readDefaultValue.remove(0);
            fail("Expect exception on modification of default collection");
        } catch (Exception e) {}
        
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigSet(id, name, cla, desc, true, Collections.singleton("7"));
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof Set);
        assertEquals("7", ((ConfigSet)config).getValue().iterator().next());
    }
    
    public void testSetSet ()
    {
        Set additionalSet1 = Collections.singleton("10");
        Set additionalSet2 = Collections.singleton("11");
        ConfigSet config = new ConfigSet(id, name, cla, desc, true, Collections.singleton("7"));
        assertEquals(1, ((Set)config.getValue()).size());
        // The first user added list overides the default
        config.setValue(additionalSet2, true);
        assertEquals(1, ((Set)config.getValue()).size());
        assertEquals(true, config.isUserSpecified());
        // The call to 'set' overwrites the first
        config.setValue(additionalSet1, true);
        assertEquals(1, ((Set)config.getValue()).size());
        assertEquals("10", ((Set)config.getValue()).iterator().next());
        assertEquals(true, config.isUserSpecified());
        
        // The second user added list is appended to the first
        config.addValue(additionalSet2, true);
        Set values = new HashSet(((ConfigSet)config).getValue());
        assertEquals(true, values.remove("10"));
        assertEquals(true, values.remove("11"));
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigSet(id, name, cla, desc, true, Collections.singleton("7"));
        assertEquals(1, ((ConfigSet)config).getValue().size());
        assertEquals(true, ((ConfigSet)config).getValue().contains("7"));
        config.setValue(Collections.EMPTY_SET, true);
        assertEquals(0, ((ConfigSet)config).getValue().size());
        config.unset();
        assertEquals(1, ((ConfigSet)config).getValue().size());
        assertEquals(true, ((ConfigSet)config).getValue().contains("7"));
        
        config.unset();
        config.setValue(Collections.singleton("4"), false);
        assertEquals(1, ((ConfigSet)config).getValue().size());
        config.unset();
        config.setValue(Collections.singleton("5"), false);
        assertEquals(1, ((ConfigSet)config).getValue().size());
    }
    
    public void testOthers ()
    {
        ConfigSet config = new ConfigSet(id, name, cla, desc, true, Collections.singleton("7"));

        //boolean file int map set list string 
        try {
            config.setValue(true, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(new File("foo"), true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(1, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(Collections.EMPTY_MAP, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(Collections.EMPTY_LIST, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue("foo", true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
    }
    
}
