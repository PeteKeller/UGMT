/**
 * UGMT : Universal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 * miju@phantasia.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package harn.equipment;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import rpg.*;

/**
 * Class to provide GUI access to modifiers and the like.
 * @author Michael Jung
 */
public class EquipTableModel extends AbstractTableModel {
    /** Column headers */
    final static private String[] colnames = { "Weight" };

    /** Main reference */
    private Main main;

    /** table reference */
    TreeTable ttref;

    /** Constructor */
    EquipTableModel(Main aMain) { main = aMain; }

    /** IF method */
    public Object getValueAt(int row, int column) {
        Object tn = ttref.getTree().getPathForRow(row + 1).getLastPathComponent();
        if (tn instanceof Item) {
            return ((Item)tn).getSelfWeight();
        }
        return null;
    }

    /**
     * Convert a double to (truncated) string. One decimal place.
     * @param d double to truncate
     * @return truncated double
     */
    private String convertDouble(double d, int off) {
        if (d == 0) return "-";
        String ret = Double.toString(d);
        int idx = ret.indexOf('.');
        if (idx < 0) return ret;
        return ret.substring(0,Math.min(idx + off,ret.length()));
    }

    /** IF method */
    public void setValueAt(Object aValue, int row, int column) {
        Object tn = ttref.getTree().getPathForRow(row + 1).getLastPathComponent();
        if (tn instanceof Item) {
            ((Item)tn).setWeight((String)aValue);
            main.bSave.setEnabled(true);
            ttref.getTable().repaint();
        }
    }

    /** IF method */
    public String getColumnName(int column) { return colnames[column]; }

    /** IF method */
    public Class getColumnClass(int column) { return Object.class; }

    /** IF method */
    public boolean isCellEditable(int row, int column) { return true; }

    /** IF method */
    public int getRowCount() {
        if (ttref == null) return 0;
        return ttref.getTree().getRowCount() - 1;
    }

    /** IF method */
    public int getColumnCount() { return 1; }
}
