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
package harn.magic;

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
public class SpellTableModel extends AbstractTableModel {
    /** Column headers */
    final static private String[] colnames = { "ML", "Adj-1", "Adj-2", "Adj-3", "Roll", "Result", "Success" };

    /** Main reference */
    private Main main;

    /** Constructor */
    SpellTableModel(Main aMain) { main = aMain; }

    /** IF method */
    public Object getValueAt(int row, int column) {
        Object tn = main.displaymods.getTree().getPathForRow(row + 1).getLastPathComponent();
        if (tn instanceof Spell) {
            switch (column) {
                case 0: return ((Spell)tn).getML();
                case 1: return ((Spell)tn).adj1;
                case 2: return ((Spell)tn).adj2;
                case 3: return ((Spell)tn).adj3;
                case 4: return null;
                case 5: return ((Spell)tn).result;
                case 6: return ((Spell)tn).success;
            }
        }
        return null;
    }

    /** IF method */
    public void setValueAt(Object aValue, int row, int column) {
        Object tn = main.displaymods.getTree().getPathForRow(row + 1).getLastPathComponent();
        if (tn instanceof Spell) {
            switch (column) {
                case 0: ((Spell)tn).mySpell.ml = (String) aValue; break;
                case 1: ((Spell)tn).adj1 = (String) aValue; break;
                case 2: ((Spell)tn).adj2 = (String) aValue; break;
                case 3: ((Spell)tn).adj3 = (String) aValue; break;
                case 4: return;
                case 5: ((Spell)tn).result = (String) aValue; break;
                case 6: ((Spell)tn).success = (String) aValue; break;
            }
            main.bSave.setEnabled(true);
            ((Spell)tn).calc(row);
            main.displaymods.getTable().repaint();
        }
    }

    /** IF method */
    public String getColumnName(int column) { return colnames[column]; }

    /** IF method */
    public Class getColumnClass(int column) {
        return column == 4 ? JButton.class : Object.class;
    }

    /** IF method */
    public boolean isCellEditable(int row, int column) { return true; }

    /** IF method */
    public int getRowCount() {
        if (main.displaymods == null) return 0;
        return main.displaymods.getTree().getRowCount() - 1;
    }

    /** IF method */
    public int getColumnCount() { return 7; }
}
