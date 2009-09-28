package net.sf.opendf.config;

import junit.framework.TestCase;
import java.util.*;

public class ConfigStringPickOneTest extends TestCase
{
    final String id = "test.string";
    final String cla = "-ts";
    final String name = "Test String";
    final String desc = "Just a test string";
    
    protected void setUp () throws Exception
    {
        super.setUp();
    }

    public void testConstructor ()
    {
        List<String> allowed = new ArrayList();
        allowed.add("one");
        allowed.add("two");
        ConfigStringPickOne config = new ConfigStringPickOne(id, name, cla, desc, true, "one", allowed);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
        
        try
        {
            config = new ConfigStringPickOne(id, name, cla, desc, true, "notInList", allowed);
            fail("Expected exception not received");
        } catch (IllegalArgumentException iae)
        {}
    }
    
    public void testValidate ()
    {
        List<String> allowed = new ArrayList();
        allowed.add("one");
        allowed.add("two");
        ConfigStringPickOne config = new ConfigStringPickOne(id, name, cla, desc, false, "one", allowed);
        assertEquals(true, config.validate());
        config.setValue("no", true);
        assertEquals(false, config.validate());
        
        config.setValue("one", true);
        assertEquals(true, config.validate());
        
        config.setValue("two", true);
        assertEquals(true, config.validate());
    }
    
    public void testAllowables ()
    {
        List<String> allowed = new ArrayList();
        allowed.add("one");
        allowed.add("two");
        ConfigStringPickOne config = new ConfigStringPickOne(id, name, cla, desc, false, "one", allowed);
        
        List<String> retrieve = config.getAllowable();
        assertEquals(allowed, retrieve);
    }    
}
