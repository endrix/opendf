package net.sf.opendf.plugin.causation;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class StatsResultTreeTableRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1461105850246060216L;

	public StatsResultTreeTableRenderer() {
    }

    @Override
	public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        
        if(node != null && node.getUserObject() != null && (node.getUserObject() instanceof StatsResultTreeTableRowData))
        {
        	StatsResultTreeTableRowData item = (StatsResultTreeTableRowData)(node.getUserObject());
        	setText(item.getActionName());
        }
        else
        {
        	setIcon(null);
        }
        return this;

    }
}
