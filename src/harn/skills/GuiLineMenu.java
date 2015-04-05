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
package harn.skills;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;

public class GuiLineMenu extends JPopupMenu {
    /** Menu items */
    JMenuItem mi1, mi2, mi3, mi4;

    /** Main reference */
    private Main main;
    
    /** GuiLine we are on */
    private GuiLine line;

    /** Constructor */
    public GuiLineMenu(Main aMain) {
        super();
        main = aMain;

        mi1 = new JMenuItem("Other Set");
        add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    getLine().sect.toggle();
                    getLine().undisplay();
		}
	    });

        mi2 = new JMenuItem("Delete");
        add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    if (main.tree.getSelectionPath() == null) return;
                    TreeNode tn = (TreeNode)
                        main.tree.getSelectionPath().getLastPathComponent();
                    if (tn.isLeaf()) {
                        main.getState().removeSkill
                            (tn.toString(), getLine().name.getText());
                        getLine().undisplay();
                    }
		}
	    });

        mi3 = new JMenuItem("Copy");
        add(mi3);
        mi3.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    if (main.tree.getSelectionPath() == null) return;
                    TreeNode tn = (TreeNode)
                        main.tree.getSelectionPath().getLastPathComponent();
                    if (tn.isLeaf()) {
                        main.getState().copySkill
                            (tn.toString(), getLine().name.getText());
                    }
		}
	    });

        mi4 = new JMenuItem("Insert");
        add(mi4);
        mi4.setEnabled(false);
        mi4.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (main.tree.getSelectionPath() == null) return;
                    TreeNode tn = (TreeNode)
                        main.tree.getSelectionPath().getLastPathComponent();
                    if (tn.isLeaf()) {
                        main.display.insertSkill(tn.toString(), true);
                    }
                }
            });
    }

    /** Find the line we are on */
    private GuiLine getLine() { return line; }

    /** Set the line we are on */
    public void setLine(GuiLine l) { line = l; }
}
