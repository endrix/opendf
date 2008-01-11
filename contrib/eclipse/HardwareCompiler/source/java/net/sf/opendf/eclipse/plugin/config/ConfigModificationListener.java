package net.sf.opendf.eclipse.plugin.config;

public interface ConfigModificationListener
{
    public static final int TEXT_MODIFICATION = 1;
    public static final int BUTTON_SELECTION = 2;

    /**
     * This method will be called by configuration {@link Control} widgets 
     * when their status is changed.
     * 
     * @param type one of the values listed in this interface
     */
    public void registerModification (int type);
    
}
