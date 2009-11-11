package net.sf.opendf.config;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

public class ConfigFileTest extends TestCase
{
    final String id = "test.file";
    final String cla = "-tf";
    final String name = "Test File";
    final String desc = "Just a test file";

    protected void setUp () throws Exception
    {
        super.setUp();
    }

    public void testConstructor ()
    {
        ConfigFile config = new ConfigFile(id, name, cla, desc, true, "");
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigFile(id, name, cla, desc, true, "mydef");
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof String);
        assertEquals(true, ((ConfigFile)config).getValueFile() instanceof File);
        assertEquals("mydef", config.getValue());
        assertEquals(new File("mydef"), ((ConfigFile)config).getValueFile());
    }
    
    public void testSetString ()
    {
        ConfigFile config = new ConfigFile(id, name, cla, desc, true, "");
        assertEquals("", config.getValue());
        assertEquals(new File(""), config.getValueFile());
        config.setValue("test", true);
        assertEquals("test", config.getValue());
        assertEquals(new File("test"), config.getValueFile());
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testSetFile ()
    {
        ConfigFile config = new ConfigFile(id, name, cla, desc, true, "");
        assertEquals("", config.getValue());
        assertEquals(new File(""), config.getValueFile());
        config.setValue(new File("Foo"), true);
        assertEquals("Foo", config.getValue());
        assertEquals(new File("Foo"), config.getValueFile());
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigFile(id, name, cla, desc, true, "foo");
        assertEquals("foo", ((ConfigFile)config).getValue());
        config.setValue("bar", true);
        assertEquals("bar", ((ConfigFile)config).getValue());
        config.unset();
        assertEquals("foo", ((ConfigFile)config).getValue());
    }
        
    public void testOthers ()
    {
        ConfigFile config = new ConfigFile(id, name, cla, desc, true, "");
        try {
            config.setValue(true, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}

        //boolean file int map set list string 
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
