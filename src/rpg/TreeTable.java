/*
 * UGMT : Universal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 *
 * Any party obtaining a copy of these files is granted, free of charge, a
 * full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and
 * documentation files (the "Software"), including without limitation the
 * rights to use, copy, modify, merge, publish and/or distribute copies of
 * the Software, and to permit persons who receive copies from any such 
 * party to do so, with the only requirement being that this copyright 
 * notice remain intact.
 */
package rpg;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * This class realizes a DND tree on the left with an accompanying table to the
 * right. Selection will be synchrounous and the table will be made to fit the
 * tree.
 * @author Michael Jung
 */
public class TreeTable extends JPanel {
    /** Left tree */
    private DNDTree tree;

    /** Right table */
    private JTable table;

    /**
     * Constructor. Note that the cell height should be something governed by
     * the Look-and-feel, but - alas - the treetable isn't part of any look
     * and feel. Maybe there is a more elegant way of solving that, but
     * currently the common cell height is given as a parameter wen creating
     * this treetable. This tree also supports a specially rendered button
     * cell in the table.
     * @param treeModel tree model
     * @param tableModel table model
     * @param cellh cell height
     */
    public TreeTable(DNDTree treeModel, TableModel tableModel, int cellh) {
        setLayout(new GridBagLayout());

        // All changes in the tree must be reflected in the table
        tree = treeModel;
        tree.addTreeExpansionListener(new TreeExpansionListener() {
                public void treeCollapsed(TreeExpansionEvent event) { redrawTable(); }
                public void treeExpanded(TreeExpansionEvent event) { redrawTable(); }
            });
        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) { redrawTable(); }
            });
        tree.getModel().addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) { redrawTable(); }
                public void treeNodesInserted(TreeModelEvent e) { redrawTable(); }
                public void treeNodesRemoved(TreeModelEvent e) { redrawTable(); }
                public void treeStructureChanged(TreeModelEvent e) { redrawTable(); }
            });

        // A change in the table must be reflected in the tree
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    int i = table.getSelectedRow();
                    // Do not create endless loops by not setting the identity
                    if (i + 1 != tree.getMinSelectionRow())
                        if (i > 0) {
                            tree.setSelectionInterval(i+1 , i+1);
                        }
                        else {
                            int[] rows = tree.getSelectionRows();
                            if (rows != null && rows.length > 0)
                                tree.removeSelectionRow(rows[0]);
                        }
                }
            });

        // Table properties
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setDefaultRenderer(JButton.class, new ButtonColumn(table));
        table.setDefaultEditor(JButton.class, new ButtonColumn(table));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(cellh);
        table.getTableHeader().setPreferredSize(new Dimension(0, cellh));

        Object rend = table.getDefaultRenderer(Object.class);
        ((DefaultTableCellRenderer)rend).setHorizontalAlignment(SwingConstants.CENTER);

        // Tree properties
        tree.setRowHeight(cellh);

        // Place header of table and table
        JPanel tab = new JPanel(new BorderLayout());
        tab.add(table, BorderLayout.CENTER);
        tab.add(table.getTableHeader(), BorderLayout.NORTH);

        // Place tree and table in common container (this). Let table expand
        // but not tree.
        GridBagConstraints constr = new GridBagConstraints();
        constr.weighty = 1;
        constr.fill = GridBagConstraints.BOTH;
        constr.gridx = 0;
        constr.weightx = 10;
        add(tree, constr);
        constr.gridx = 1;
        constr.weightx = 0;
        add(tab, constr);
    }

    /** Get tree object */
    public DNDTree getTree() { return tree; }

    /** Get table object */
    public JTable getTable() { return table; }

    /**
     * Redraw table. Any change in the tree layout is reflected in the table
     * through this method.
     */
    private void redrawTable() {
        int i = tree.getMinSelectionRow();
        if (i > 0) 
            table.addRowSelectionInterval(i-1, i-1);
        else if (table.getSelectedRow() > -1 &&
                 // The following curious condition is necessary as the table
                 // may be decreased in size before the tree change
                 // notification is passed on
                 table.getSelectedRow() < table.getRowCount())
            table.removeRowSelectionInterval
                (table.getSelectedRow(), table.getSelectedRow());
        table.repaint();
    }

    /**
     * A renderer class for buttons. The class allows for whole columns to be
     * buttons but single instances may go astray. This allows to make
     * directories to not be "rollable".
     */
    private class ButtonColumn extends AbstractCellEditor implements TableCellEditor, TableCellRenderer, ActionListener {
        /** Current row */
        int crow;

        /** Render read-only */
        JComponent read;

        /** Render button */
        JButton butt;

        /**
         * Constructor.
         * @param table table this renederer applies to
         */
        ButtonColumn(JTable table) {
            // Button rendering
            butt = new JButton("Roll");
            butt.addActionListener(this);
            // Non rollable entries
            read = new JPanel();
            read.setBackground(table.getBackground());
        }

        /** IF method */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object tn = tree.getPathForRow(row + 1).getLastPathComponent();
            if (tn instanceof ButtonNode) return butt;
            return read;
        }

        /** IF method */
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            crow = row;
            Object tn = tree.getPathForRow(row + 1).getLastPathComponent();
            if (tn instanceof ButtonNode) return butt;
            return read;
        }

        /** IF method */
        public Object getCellEditorValue() { return this; }

        /** IF method */
        public void actionPerformed(ActionEvent e) {
            // Must be Button
            ButtonNode bn = (ButtonNode) tree.getPathForRow(crow + 1).getLastPathComponent();
            bn.update(crow);
            // If this is missing, strange things happen to the table selection
            fireEditingStopped();
        }
    }

    /**
     * The button node must be implemented by nodes wishing to display a
     * button in the table.
     */
    public interface ButtonNode extends DNDTree.DragNode {
        /**
         * Called when a button is pressed
         * @param row row to update
         */
        public void update(int row);
    }
}
