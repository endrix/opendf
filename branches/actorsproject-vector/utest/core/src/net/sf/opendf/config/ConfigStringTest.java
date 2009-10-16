package net.sf.opendf.config;


import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

public class ConfigStringTest extends TestCase
{
    final String id = "test.string";
    final String cla = "-ts";
    final String name = "Test String";
    final String desc = "Just a test string";


    public void setUp () throws Exception
    {
        super.setUp();
    }

    public void testConstructor ()
    {
        ConfigString config = new ConfigString(id, name, cla, desc, true, "");
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigString(id, name, cla, desc, true, "mydef");
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof String);
        assertEquals("mydef", config.getValue());
    }
    
    public void testSetString ()
    {
        ConfigString config = new ConfigString(id, name, cla, desc, true, "");
        assertEquals("", config.getValue());
        config.setValue("test", true);
        assertEquals("test", config.getValue());
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigString(id, name, cla, desc, true, "foo");
        assertEquals("foo", ((ConfigString)config).getValue());
        config.setValue("bar", true);
        assertEquals("bar", ((ConfigString)config).getValue());
        config.unset();
        assertEquals("foo", ((ConfigString)config).getValue());
    }
    
    public void testOthers ()
    {
        ConfigString config = new ConfigString(id, name, cla, desc, true, "");

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
            config.setValue(Collections.EMPTY_SET, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(Collections.EMPTY_LIST, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
    }
    
    
}
