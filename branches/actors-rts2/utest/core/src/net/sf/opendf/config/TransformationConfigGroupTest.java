package net.sf.opendf.config;

import junit.framework.TestCase;

public class TransformationConfigGroupTest extends TestCase
{

    protected void setUp () throws Exception
    {
        super.setUp();
    }

    public void testRequiredContents ()
    {
        TransformationConfigGroup scg = new TransformationConfigGroup();
        
        // Required from superclass for elaboration
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_FILE));
        assertNotNull(scg.get(ConfigGroup.MESSAGE_SUPPRESS_IDS));
        assertNotNull(scg.get(ConfigGroup.CACHE_DIR));
        // for elaboration
        assertNotNull(scg.get(ConfigGroup.MODEL_PATH));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_TOP));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_NAME));
        assertNotNull(scg.get(ConfigGroup.TOP_MODEL_PARAMS));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_PP));
        assertNotNull(scg.get(ConfigGroup.ELABORATE_INLINE));
        
        // Transformation specific
        assertNotNull(scg.get(ConfigGroup.XSLT_PRESERVE_INTERMEDIATE));
    }
    
    public void testDefaultSettings ()
    {
        TransformationConfigGroup scg = new TransformationConfigGroup();
        
        assertEquals(false, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_INLINE)).getValue().booleanValue());
        assertEquals(false, ((ConfigBoolean)scg.get(ConfigGroup.ELABORATE_PP)).getValue().booleanValue());
    }
    
    public void testGetEmpty ()
    {
        ConfigGroup cg = new TransformationConfigGroup();
        ConfigGroup empty = cg.getEmptyConfigGroup();
        assertNotNull(empty);
        assertEquals(TransformationConfigGroup.class, empty.getClass());
    }
}
