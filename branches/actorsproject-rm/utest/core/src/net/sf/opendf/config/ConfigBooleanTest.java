package net.sf.opendf.config;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

public class ConfigBooleanTest extends TestCase
{
    final String id = "test.boolean";
    final String cla = "-tb";
    final String name = "Test boolean";
    final String desc = "Just a test boolean";

    protected void setUp () throws Exception
    {
        super.setUp();
    }
    
    public void testConstructor ()
    {
        ConfigBoolean config = new ConfigBoolean(id, name, cla, desc, true, true);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigBoolean(id, name, cla, desc, true, true);
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof Boolean);
        assertEquals(true, ((Boolean)config.getValue()).booleanValue());
    }
    
    public void testSetBoolean ()
    {
        ConfigBoolean config = new ConfigBoolean(id, name, cla, desc, true, true);
        assertEquals(true, ((Boolean)config.getValue()).booleanValue());
        config.setValue(false, true);
        assertEquals(false, ((Boolean)config.getValue()).booleanValue());
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigBoolean(id, name, cla, desc, true, true);
        assertEquals(true, ((ConfigBoolean)config).getValue().booleanValue());
        config.setValue(false, true);
        assertEquals(false, ((ConfigBoolean)config).getValue().booleanValue());
        config.unset();
        assertEquals(true, ((ConfigBoolean)config).getValue().booleanValue());
    }
        
    public void testOthers ()
    {
        ConfigBoolean config = new ConfigBoolean(id, name, cla, desc, true, true);

        //boolean file int map set list string 
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
        try {
            config.setValue("foo", true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
    }
    
}
