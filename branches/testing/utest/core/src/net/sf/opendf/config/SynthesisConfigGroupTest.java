package net.sf.opendf.config;

import junit.framework.TestCase;

public class SynthesisConfigGroupTest extends TestCase
{

    protected void setUp () throws Exception
    {
        super.setUp();
    }

    public void testRequiredContents ()
    {
        SynthesisConfigGroup scg = new SynthesisConfigGroup();
        
        // Required from superclass for elaboration
        assertNotNull(scg.get(ConfigGroup.MODEL_PATH));
        assertNotNull(scg.get(ConfigGroup.CACHE_DIR));
        assertNotNull(scg.get(ConfigGroup.MESSAGE_SUPPRESS_IDS));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_TOP));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_NAME));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_PARAMS));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_PP));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_INLINE));
        
        // Synthesis specific
        assertNotNull(scg.get(ConfigGroup.HDL_OUTPUT_FILE));
        assertNotNull(scg.get(ConfigGroup.GEN_HDL_SIM_MODEL));
        assertNotNull(scg.get(ConfigGroup.ACTOR_OUTPUT_DIR));
    }
    
    public void testDefaultSettings ()
    {
        SynthesisConfigGroup scg = new SynthesisConfigGroup();
        
        assertEquals(true, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_INLINE)).getValue().booleanValue());
        assertEquals(true, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_PP)).getValue().booleanValue());
        
        assertEquals(false, ((ConfigBoolean)scg.get(ConfigGroup.GEN_HDL_SIM_MODEL)).getValue().booleanValue());

        assertEquals("Actors", ((ConfigString)scg.get(ConfigGroup.ACTOR_OUTPUT_DIR)).getValue());
    }
    
    public void testGetEmpty ()
    {
        ConfigGroup cg = new SynthesisConfigGroup();
        ConfigGroup empty = cg.getEmptyConfigGroup();
        assertNotNull(empty);
        assertEquals(SynthesisConfigGroup.class, empty.getClass());
    }
    
}
