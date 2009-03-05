package net.sf.opendf.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class ConfigListTest extends TestCase
{
    final String id = "test.list";
    final String cla = "-tl";
    final String name = "Test list";
    final String desc = "Just a test list";

    protected void setUp () throws Exception
    {
        super.setUp();
    }
    
    public void testConstructor ()
    {
        List defaultValue = new ArrayList();
        defaultValue.add("7");
        ConfigList config = new ConfigList(id, name, cla, desc, true, defaultValue);
        assertNotNull(config);
        assertEquals("Required config must validate false on construction", false, config.validate());
        assertEquals(id, config.getID());
        assertEquals(name, config.getName());
        assertEquals(cla, config.getCLA());
        assertEquals(desc, config.getDescription());
        List readDefaultValue = ((ConfigList)config).getValue();
        assertEquals(readDefaultValue.get(0), defaultValue.get(0));
        try {
            readDefaultValue.remove(0);
            fail("Expect exception on modification of default collection");
        } catch (Exception e) {}
        
    }
    
    public void testGetValue ()
    {
        AbstractConfig config = new ConfigList(id, name, cla, desc, true, Collections.singletonList("7"));
        assertNotNull(config.getValue());
        assertEquals(true, config.getValue() instanceof List);
    }
    
    public void testSetList ()
    {
        List additionalList1 = Collections.singletonList("10");
        List additionalList2 = Collections.singletonList("11");
        ConfigList config = new ConfigList(id, name, cla, desc, true, Collections.singletonList("7"));
        assertEquals(1, ((List)config.getValue()).size());
        // The first user added list overides the default
        config.setValue(additionalList2, true);
        assertEquals(1, ((List)config.getValue()).size());
        assertEquals("11", ((List)config.getValue()).get(0));
        assertEquals(true, config.isUserSpecified());
        // The second call to 'set' overwrites the first
        config.setValue(additionalList1, true);
        assertEquals(1, ((List)config.getValue()).size());
        assertEquals("10", ((List)config.getValue()).get(0));
        assertEquals(true, config.isUserSpecified());
        
        // The second user added list is appended to the first
        config.addValue(additionalList2, true);
        assertEquals(2, ((List)config.getValue()).size());
        assertEquals(true, config.isUserSpecified());
        assertEquals("10", ((List)config.getValue()).get(0));
        assertEquals("11", ((List)config.getValue()).get(1));
    }
    
    public void testSetString ()
    {
        ConfigList config = new ConfigList(id, name, cla, desc, true, Collections.singletonList("7"));
        assertEquals(1, ((List)config.getValue()).size());
        assertEquals("7", ((List)config.getValue()).get(0));
        config.setValue("foo"+File.pathSeparator+"bar", true);
        assertEquals(2, ((List)config.getValue()).size());
        assertEquals("foo", ((List)config.getValue()).get(0));
        assertEquals("bar", ((List)config.getValue()).get(1));
    }
    
    public void testUnset ()
    {
        AbstractConfig config = new ConfigList(id, name, cla, desc, true, Collections.singletonList("7"));
        assertEquals(1, ((ConfigList)config).getValue().size());
        assertEquals(true, ((ConfigList)config).getValue().contains("7"));
        config.setValue(Collections.EMPTY_LIST, true);
        assertEquals(0, ((ConfigList)config).getValue().size());
        config.unset();
        assertEquals(1, ((ConfigList)config).getValue().size());
        assertEquals(true, ((ConfigList)config).getValue().contains("7"));
        
        config.unset();
        config.setValue(Collections.singletonList("4"), false);
        assertEquals(1, ((ConfigList)config).getValue().size());
        config.unset();
        config.setValue(Collections.singletonList("5"), false);
        assertEquals(1, ((ConfigList)config).getValue().size());
    }
        
    public void testOthers ()
    {
        ConfigList config = new ConfigList(id, name, cla, desc, true, Collections.singletonList("7"));

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
    }
    
}
