package net.sf.opendf.eclipse.plugin.config;

import org.eclipse.swt.widgets.Control;

public interface UpdatableControlIF
{

    /**
     * Used to force the backing Control instance to update its value
     */
    public void updateValue ();
    
    /**
     * @return the Control element
     */
    public Control getControl ();
    
    public void addModifyListener (ConfigModificationListener listener);
}
