package net.sf.opendf.config;

import java.io.File;
import java.util.*;

import junit.framework.TestCase;

public class ConfigMapTest extends TestCase
{
    final String id = "test.map";
    final String cla = "-tmap";
    final String name = "Test map";
    final String desc = "Just a test map";

    protected void setUp () throws Exception
    {
        super.setUp();
    }
    
    public void testConstructor ()
    {
        Map defaultValue = Collections.singletonMap("7", "7val");
        ConfigMap config = new ConfigMap(id, name, cla, desc, true, defaultValue);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
        Map readDefaultValue = ((ConfigMap)config).getValue();
        assertEquals(1, readDefaultValue.size());
        assertEquals("7", readDefaultValue.keySet().iterator().next());
        assertEquals("7val", readDefaultValue.get("7"));
        
        try {
            readDefaultValue.remove("7");
            fail("Expect exception on modification of default collection");
        } catch (Exception e) {}
        
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigMap(id, name, cla, desc, true, Collections.singletonMap("7", "7v"));
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof Map);
        assertEquals("7", ((ConfigMap)config).getValue().keySet().iterator().next());
        assertEquals("7v", ((ConfigMap)config).getValue().get("7"));
    }
    
    public void testSetMap ()
    {
        Map additionalMap1 = Collections.singletonMap("10", "10v");
        Map additionalMap2 = Collections.singletonMap("11", "11v");
        ConfigMap config = new ConfigMap(id, name, cla, desc, true, Collections.singletonMap("7", "7v"));
        assertEquals(1, ((Map)config.getValue()).size());
        // The first user added list overides the default
        config.setValue(additionalMap2, true);
        assertEquals(1, ((Map)config.getValue()).size());
        assertEquals("11v", ((Map)config.getValue()).get("11"));
        assertEquals(true, config.isUserSpecified());
        // The second call to 'set' overwrites the first
        config.setValue(additionalMap1, true);
        assertEquals(1, ((Map)config.getValue()).size());
        assertEquals("10v", ((Map)config.getValue()).get("10"));
        assertEquals(true, config.isUserSpecified());
        
        
        // The second user added list is appended to the first
        config.addValue(additionalMap2, true);
        Map values = new HashMap(((ConfigMap)config).getValue());
        assertEquals("10v", values.remove("10"));
        assertEquals("11v", values.remove("11"));
        assertEquals(true, config.isUserSpecified());
    }

    public void testUnset ()
    {
        AbstractConfig config = new ConfigMap(id, name, cla, desc, true, Collections.singletonMap("7","7v"));
        assertEquals(1, ((ConfigMap)config).getValue().size());
        assertEquals("7v", ((ConfigMap)config).getValue().get("7"));
        config.setValue(Collections.EMPTY_MAP, true);
        assertEquals(0, ((ConfigMap)config).getValue().size());
        config.unset();
        assertEquals(1, ((ConfigMap)config).getValue().size());
        assertEquals("7v", ((ConfigMap)config).getValue().get("7"));
        
        config.unset();
        config.setValue(Collections.singletonMap("4","4v"), false);
        assertEquals(1, ((ConfigMap)config).getValue().size());
        config.unset();
        config.setValue(Collections.singletonMap("5","5v"), false);
        assertEquals(1, ((ConfigMap)config).getValue().size());
    }
    
    public void testOthers ()
    {
        ConfigMap config = new ConfigMap(id, name, cla, desc, true, Collections.singletonMap("7", "7v"));

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
