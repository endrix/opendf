package net.sf.opendf.config;

import junit.framework.TestCase;

public class SimulationConfigGroupTest extends TestCase
{

    protected void setUp () throws Exception
    {
        super.setUp();
    }

    public void testRequiredContents ()
    {
        SimulationConfigGroup scg = new SimulationConfigGroup();
        
        // Required from superclass
        assertNotNull(scg.get(ConfigGroup.MODEL_PATH));
        assertNotNull(scg.get(ConfigGroup.CACHE_DIR));
        assertNotNull(scg.get(ConfigGroup.MESSAGE_SUPPRESS_IDS));
        assertNotNull(scg.get(ConfigGroup.LOG_LEVEL_USER));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_TOP));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_NAME));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_PARAMS));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_PP));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_INLINE));
        
        // Simulation specific
        assertNotNull(scg.get(ConfigGroup.ENABLE_ASSERTIONS));
        assertNotNull(scg.get(ConfigGroup.SIM_INPUT_FILE));
        assertNotNull(scg.get(ConfigGroup.SIM_OUTPUT_FILE));
        assertNotNull(scg.get(ConfigGroup.SIM_TIME));
        assertNotNull(scg.get(ConfigGroup.SIM_STEPS));
        assertNotNull(scg.get(ConfigGroup.SIM_MAX_ERRORS));
        assertNotNull(scg.get(ConfigGroup.SIM_INTERPRET_STIMULUS));
        assertNotNull(scg.get(ConfigGroup.SIM_BUFFER_IGNORE));
        assertNotNull(scg.get(ConfigGroup.SIM_BUFFER_RECORD));
        assertNotNull(scg.get(ConfigGroup.SIM_TRACE));
        assertNotNull(scg.get(ConfigGroup.SIM_TYPE_CHECK));
        assertNotNull(scg.get(ConfigGroup.SIM_BUFFER_SIZE_WARNING));
    }
    
    public void testDefaultSettings ()
    {
        SimulationConfigGroup scg = new SimulationConfigGroup();
        
        assertEquals(false, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_INLINE)).getValue().booleanValue());
        assertEquals(false, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_PP)).getValue().booleanValue());
        
        assertEquals(-1, ((ConfigInt)scg.get(ConfigGroup.SIM_TIME)).getValue().intValue());
        assertEquals(-1, ((ConfigInt)scg.get(ConfigGroup.SIM_STEPS)).getValue().intValue());
        assertEquals(100, ((ConfigInt)scg.get(ConfigGroup.SIM_MAX_ERRORS)).getValue().intValue());
    }
    
    public void testGetEmpty ()
    {
        ConfigGroup cg = new SimulationConfigGroup();
        ConfigGroup empty = cg.getEmptyConfigGroup();
        assertNotNull(empty);
        assertEquals(SimulationConfigGroup.class, empty.getClass());
    }
    
}
