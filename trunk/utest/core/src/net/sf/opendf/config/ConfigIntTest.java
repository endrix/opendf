package net.sf.opendf.config;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;

public class ConfigIntTest extends TestCase
{
    final String id = "test.int";
    final String cla = "-ti";
    final String name = "Test int";
    final String desc = "Just a test int";

    protected void setUp () throws Exception
    {
        super.setUp();
    }
    
    public void testConstructor ()
    {
        ConfigInt config = new ConfigInt(id, name, cla, desc, true, 7);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigInt(id, name, cla, desc, true, 7);
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof Integer);
        assertEquals(7, ((Integer)config.getValue()).intValue());
        assertEquals("7", ((ConfigInt)config).getValueString());
    }
    
    public void testSetInt ()
    {
        ConfigInt config = new ConfigInt(id, name, cla, desc, true, 7);
        assertEquals(7, ((Integer)config.getValue()).intValue());
        config.setValue(11, true);
        assertEquals(11, ((Integer)config.getValue()).intValue());
        assertEquals(true, config.isUserSpecified());
    }
    
    public void testSetString ()
    {
        ConfigInt config = new ConfigInt(id, name, cla, desc, true, 7);
        assertEquals(7, ((Integer)config.getValue()).intValue());
        config.setValue("11", true);
        assertEquals(11, ((Integer)config.getValue()).intValue());
        assertEquals("11", config.getValueString());
        assertEquals(true, config.isUserSpecified());
        
        config.setValue("0x11", true);
        assertEquals(17, ((Integer)config.getValue()).intValue());
        assertEquals("0x11", config.getValueString());
        
        config.setValue("0XA", true);
        assertEquals(10, ((Integer)config.getValue()).intValue());
        assertEquals("0xa", config.getValueString());
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigInt(id, name, cla, desc, true, 1);
        assertEquals(1, ((ConfigInt)config).getValue().intValue());
        config.setValue(9, true);
        assertEquals(9, ((ConfigInt)config).getValue().intValue());
        config.unset();
        assertEquals(1, ((ConfigInt)config).getValue().intValue());
    }
    
    public void testOthers ()
    {
        ConfigInt config = new ConfigInt(id, name, cla, desc, true, 7);

        //boolean file int map set list string 
        try {
            config.setValue(false, true);
            fail("Expected exception not received (UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {}
        try {
            config.setValue(new File("foo"), true);
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
