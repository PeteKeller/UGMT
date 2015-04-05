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

import rpg.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class that represents the Group GUI.
 * @author Michael Jung
 */
public class SkillDisplay extends JPanel {
    /** root reference */
    private Main main;

    /** Item contraints */
    GridBagConstraints constrItem;

    /** Actual panel */
    JPanel top;

    /**
     * Constructor. Load mark and prepare layout.
     * @param props properties
     * @param main root reference
     */
    SkillDisplay(Properties props, Main aMain) {
	main = aMain;
	setLayout(new GridBagLayout());

	constrItem = new GridBagConstraints();
	constrItem.weightx = 1;
        constrItem.anchor = GridBagConstraints.NORTH;
	constrItem.fill = GridBagConstraints.HORIZONTAL;
        constrItem.insets = new Insets(2, 2, 2, 2);

        top = new JPanel(new GridBagLayout());
        add(top, constrItem);

        JPanel dummy = new JPanel();
	constrItem.gridy = 1;
	constrItem.weighty = 1;
	constrItem.fill = GridBagConstraints.BOTH;
        add(dummy, constrItem);

        Iterator iter = main.getState().groupList().keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            addGroup(group, false);
        }
    }

    /**
     * Enter a new group
     */
    public void newGroup() {
        addGroup(null, true);
    }

    /**
     * Add a group.  Group may be now or loaded.
     * @param visible show, because this group is new
     */
    public void addGroup(String group, boolean visible) {
        if (group == null) {
            group = "New Group";
        }
        main.getState().addGroup(group);

        SortedTreeModel model = (SortedTreeModel) main.tree.getModel();
        if (!visible)
            model = main.getState().isHidden(group) ? main.otherskilltree : main.skilltree;
        DefaultMutableTreeNode tn = (DefaultMutableTreeNode) model.getRoot();
        model.insertNodeSorted(new DefaultMutableTreeNode(group), tn);
    }

    /**
     * Remove a group.
     * @param group group to remove
     */
    public void removeGroup(MutableTreeNode node) {
        ((DefaultTreeModel)main.tree.getModel()).removeNodeFromParent
            (node);
        main.getState().removeGroup(node.toString());
        top.removeAll();
    }

    /**
     * Add a skill.
     */
    public void newSkill(String group, boolean draw) {
        State.Section sect = main.getState().newSkill(group);
        setDirty(true);

        if (draw) {
            sect.addGui(top);
            revalidate(group);
        }
    }

    /**
     * Insert skill.
     */
    public void insertSkill(String group, boolean draw) {
        State.Section sect = main.getState().insertSkill(group);
        if (sect == null) return;
        setDirty(true);
        if (draw) {
            sect.addGui(top);
            revalidate(group);
        }
    }

    /**
     * Toggle the whole display from standard skills to hidden
     */
    public void toggle() {
        main.getState().toggle();
        if (main.tree.getSelectionPath() == null) return;
        TreeNode tn =
            (TreeNode) main.tree.getSelectionPath().getLastPathComponent();
        if (tn.isLeaf()) setSkills(tn.toString());
    }

    /**
     * Toggle a group from standard groups to hidden
     * @param group group to toggle
     */
    public void toggleGroup(DefaultMutableTreeNode node) {
        SortedTreeModel model = (SortedTreeModel)main.tree.getModel();
        SortedTreeModel othermodel =
            (model == main.skilltree ? main.otherskilltree : main.skilltree);
        model.removeNodeFromParent(node);
        setDirty(true);
        othermodel.insertNodeSorted
            (node, (DefaultMutableTreeNode) othermodel.getRoot());
        main.getState().toggleGroup(node.toString());
    }

    /**
     * Sets the display to the new character.
     * @param group group name
     */
    public void setSkills(String group) {
        top.removeAll();

        // Header
        constrItem.gridy = 0;

        constrItem.gridx = 0;
        top.add(new JLabel("Skill"), constrItem);
        constrItem.gridx = 1;
        top.add(new JLabel("ML"), constrItem);
        constrItem.gridx = 2;
        top.add(new JLabel("Dice"), constrItem);
        constrItem.gridx = 3;
        top.add(new JLabel("Adj-1"), constrItem);
        constrItem.gridx = 4;
        top.add(new JLabel("Adj-2"), constrItem);
        constrItem.gridx = 5;
        top.add(new JLabel("Adj-3"), constrItem);
        constrItem.gridx = 6;
        top.add(new JLabel(" "), constrItem);
        constrItem.gridx = 7;
        top.add(new JLabel("EML/Roll"), constrItem);
        constrItem.gridx = 8;
        top.add(new JLabel("Success"), constrItem);
        constrItem.gridx = 9;
        top.add(new JLabel("Type"), constrItem);
        constrItem.gridx = 10;
        top.add(new JLabel("Result"), constrItem);

        ArrayList skills =
            (ArrayList) main.getState().getSkillList(group);
        if (skills != null)
            main.getState().recalibrateGuiLines(skills);

        for (int i = 0; skills != null && i < skills.size(); i++) {
            ((State.Section) skills.get(i)).gui = null;
            ((State.Section) skills.get(i)).addGui(top);
        }

        revalidate(group);
    }

    /** Revalidate */
    public void revalidate(String group) {
        if (main == null || main.getState() == null) return;
        TreeNode tn = (TreeNode) main.tree.getLastSelectedPathComponent();
        if (tn == null || !tn.isLeaf()) return;

        ArrayList skills =
            (ArrayList) main.getState().getSkillList(tn.toString());
        main.getState().recalibrateGuiLines(skills);

        if (skills != null) {
            for (int i = 0; i < skills.size(); i++) {
                State.Section sect = (State.Section) skills.get(i);
                if (sect.gui != null) sect.gui.display();
            }
        }
        top.revalidate();
        top.repaint();
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled
     * @return dirty flag
     */
    private void setDirty(boolean flag) {
	main.getState().dirty = flag;
	if (main.bSave != null)
	    main.bSave.setEnabled(flag);
    }
}
