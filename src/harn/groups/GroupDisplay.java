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
package harn.groups;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import harn.repository.Char;

/**
 * Class that represents the Group GUI.
 * @author Michael Jung
 */
public class GroupDisplay extends JPanel {
    /** root reference */
    private Main main;

    /** movement check box references */
    private Hashtable cblist;

    /** member list */
    private JPanel members;

    /** vessel list */
    private JPanel vessels;

    /** movement modes list */
    private JPanel movmodes;

    /** speed selection */
    private JButton move;

    /** Constraints for new boxes (eg. for ships) */
    private GridBagConstraints constrBox;

    /** Constraints for new item (eg. for SG) */
    private GridBagConstraints constrItem;

    /** Constraints for new subitem (eg. for SG) */
    private GridBagConstraints constrSubItem;

    /** Deletion popup menu */
    private JPopupMenu pDelete;

    /** Popup coordinates */
    private Point pScreen;

    /**
     * Constructor. Load mark and prepare layout.
     * @param props properties
     * @param main root reference
     */
    GroupDisplay(Properties props, Main aMain) {
	main = aMain;
	setLayout(new GridBagLayout());

	ArrayList lmodes = main.getData().getModes();
	cblist = new Hashtable();

        // Deletion popup
        pDelete = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Delete");
        pDelete.add(mi);
        mi.addActionListener(new ActionListener() {
                boolean enable = false;
		public void actionPerformed(ActionEvent e) {
                    JTextField tf = (JTextField) SwingUtilities.getDeepestComponentAt
                        (main.display, pScreen.x, pScreen.y);
                    main.getState().getCurrent().removeMember
                        (tf.getText(), true);
                    tf.getParent().remove(tf);
                    main.getState().setDirty(true);
                    members.revalidate();
                }
            });
        mi = new JMenuItem("Show");
        pDelete.add(mi);
        mi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    JTextField tf = (JTextField) SwingUtilities.getDeepestComponentAt
                        (main.display, pScreen.x, pScreen.y);
                    Char c = main.getData().getChar(tf.getText());
                    if (c != null) c.setActive();
                }
            });

	// Display container for boxes
        constrBox = new GridBagConstraints();
        constrBox.anchor = GridBagConstraints.NORTHWEST;
        constrBox.weightx = constrBox.weighty = 1;
	constrBox.fill = GridBagConstraints.HORIZONTAL;

	// Constraints for items
	constrItem = new GridBagConstraints();
	constrItem.gridx = 0;
	constrItem.weightx = constrItem.weighty = 1;
	constrItem.anchor = GridBagConstraints.NORTHWEST;
	constrItem.fill = GridBagConstraints.HORIZONTAL;

	// Constraints for subitems
	constrSubItem = new GridBagConstraints();
	constrSubItem.weightx = 1;
	constrSubItem.fill = GridBagConstraints.BOTH;

        // Members
	members = new JPanel(new GridBagLayout());
	add(members, constrBox);
        JButton info = new JButton("  Members  ");
        info.setEnabled(false);
        members.add(info, constrItem);

	// Land travel modes
        movmodes = new JPanel(new GridBagLayout());
	add(movmodes, constrBox);
        info = new JButton("Travel Modes");
        info.setEnabled(false);
        movmodes.add(info, constrItem);

        move = new JButton("0.0");
        move.setEnabled(false);
        movmodes.add(move, constrItem);

	for (int i = 0; i < lmodes.size(); i++) {
	    JCheckBox cb = new JCheckBox((String)lmodes.get(i));
	    cb.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
			String mod = ((JCheckBox)e.getItem()).getText();
			if (e.getStateChange() == ItemEvent.SELECTED)
			    main.getState().getCurrent().addLandTravelMode
                                (mod, true);
			else
			    main.getState().getCurrent().removeTravelMode
                                (mod, true);
		    }
		});
	    cblist.put(lmodes.get(i), cb);
	    movmodes.add(cb, constrItem);
	}

        // Ships
	vessels = new JPanel(new GridBagLayout());
	add(vessels, constrBox);
        info = new JButton("  Vessels  ");
        info.setEnabled(false);
        vessels.add(info, constrItem);
        JButton add = new JButton("Add");
        add.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    addVessel(null);
                }
            });
        vessels.add(add, constrItem);

	revalidate();
    }

    /**
     * Sets a land travel mode.
     * @param name name of mode to set
     * @param activate whether to check or uncheck
     */
    void setMode(String name, boolean activate) {
        Component[] comps = movmodes.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comps[i];
                if (cb.getText().equals(name)) {
                    cb.setSelected(activate);
                    break;
                }
            }
        }
    }

    /**
     * Sets the speed on GUI.
     * @param float speed
     */
    void setSpeed(float speed) {
        if (speed < 0) {
            move.setText("-");
            return;
        }

        String sp = move.getText();
        // Default
        float csp = Float.POSITIVE_INFINITY;
        if (!sp.equals("-"))
            csp = Float.parseFloat(sp);
        if (csp > speed || csp < 0)
            move.setText(Float.toString(speed));
    }

    /**
     * Add a vessel to the GUI. If the parameter is null, then the vessel is
     * added to the current group as well. Otherwise it is assumed that the
     * name passed was obtained from the current group.
     */
    private void addVessel(String mode) {
        JPanel p = new JPanel(new GridBagLayout());
        JCheckBox cb = new JCheckBox((Icon)null,true);
        cb.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    JCheckBox cb = (JCheckBox)e.getItem();
                    JPanel panel = (JPanel) cb.getParent();
                    JComboBox tbox = (JComboBox) panel.getComponent(1);
                    JComboBox gbox = (JComboBox) panel.getComponent(2);
                    JComboBox dbox = (JComboBox) panel.getComponent(3);

                    // Modify travel modes directly
                    StringBuffer sb = new StringBuffer
                        ((String) tbox.getSelectedItem());
                    sb.append(":").append((String) gbox.getSelectedItem());
                    sb.append(":").append((String) dbox.getSelectedItem());
                    main.getState().getCurrent().removeTravelMode
                        (sb.toString(), true);

                    // Modify GUI
                    vessels.remove(panel);
                    vessels.revalidate();
                }
            });
        p.add(cb);

        Hashtable water = main.getData().getWaterModes();
        Object[] sails = water.keySet().toArray();
        JComboBox sailList = new JComboBox(sails);
        if (mode != null) sailList.setSelectedItem(mode.split(":")[0]);
        sailList.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    Hashtable water = main.getData().getWaterModes();
                    JComboBox src = (JComboBox)e.getSource();
                    Object sail = src.getSelectedItem();
                    JPanel parent = (JPanel) src.getParent();

                    parent.remove(2);
                    parent.remove(2); // 3 has moved up

                    String gstr = (String)((Hashtable)water.get(sail)).get
                        ("grades");
                    String dstr = (String)((Hashtable)water.get(sail)).get
                        ("deploy");

                    addDependentVesselItems(null, parent, gstr, dstr);
                }
            });
        p.add(sailList, constrSubItem);

        Object watergrades = ((Hashtable)water.get(sails[0])).get("grades");
        Object waterdeploy = ((Hashtable)water.get(sails[0])).get("deploy");

        // 1. Prepare GUI, 2. Cascade creation & announce, 3. update GUI
        vessels.add(p, constrItem);

        addDependentVesselItems
            (mode, p, (String)watergrades, (String)waterdeploy);

        vessels.revalidate();
    }

    /**
     * Add a member to the GUI. If the parameter is null, then the member is
     * added to the current group as well. Otherwise it is assumed that the
     * name passed was obtained from the current group.
     * @param name name of member
     */
    protected void addMember(String name) {
        JTextField tf = new JTextField(name);
        tf.setEnabled(false);
        tf.getDocument().addDocumentListener(new DocumentListener() {
                // Any change will make the member list dirty
                public void removeUpdate(DocumentEvent e) { setField(); }
                public void insertUpdate(DocumentEvent e) { setField(); }
                public void changedUpdate(DocumentEvent e) { setField(); }
                private void setField() { main.getState().setDirty(true); }
            });
        tf.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pScreen = SwingUtilities.convertPoint
                            (e.getComponent(), e.getX(), e.getY(), main.display); 
                        pDelete.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        members.add(tf, constrItem);
        members.revalidate();
    }

    /**
     * Set the member list. Used when initializing the GUI member list. Takes
     * care to update the previous group.
     * @param newlist new member list
     * @param oldgroup old group to be updated
     */
    void setMembers(ArrayList newlist, State.MyCharGroup oldgroup) {
        Component[] comps = members.getComponents();
        ArrayList oldMembers = new ArrayList();

        // The dirty flag will change during the following
        boolean save = main.getState().getDirty();

        int arIdx = 0;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JTextField) {
                JTextField tf = (JTextField) comps[i];

                // Save old entry
                oldMembers.add(tf.getText());
                if (arIdx < newlist.size()) {
                    tf.setText((String)newlist.get(arIdx));
                    arIdx += 1;
                }
                else {
                    members.remove(comps[i]);
                }
            }
        }
        while (arIdx < newlist.size()) {
            addMember((String)newlist.get(arIdx));
            arIdx += 1;
        }
        // Only modify if old != new and old isn't void
        if (oldgroup != null && oldgroup.getMembers() != newlist)
            oldgroup.setMembers(oldMembers);
        main.getState().setDirty(save);
        members.revalidate();
    }

    /**
     * Set the vessel list. Used when initializing the GUI member list. Takes
     * care to update the previous group.
     * @param newlist new member list
     * @param oldgroup old group to be updated
     */
    void setVessels(ArrayList list, State.MyCharGroup oldgroup) {
        Component[] comps = vessels.getComponents();
        ArrayList oldVessels = new ArrayList();

        // The dirty flag will change during the following
        boolean save = main.getState().getDirty();

        int arIdx = 0;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JPanel) {
                JPanel vPanel = (JPanel) comps[i];

                // Save old entry
                JComboBox tbox = (JComboBox) vPanel.getComponent(1);
                JComboBox gbox = (JComboBox) vPanel.getComponent(2);
                JComboBox dbox = (JComboBox) vPanel.getComponent(3);

                StringBuffer sb = new StringBuffer
                    ((String) tbox.getSelectedItem());
                sb.append(":").append((String) gbox.getSelectedItem());
                sb.append(":").append((String) dbox.getSelectedItem());
                oldVessels.add(sb.toString());

                if (arIdx < list.size()) {
                    String sel[] = ((String)list.get(arIdx)).split(":");
                    tbox.setSelectedItem(sel[0]);
                    gbox.setSelectedItem(sel[1]);
                    dbox.setSelectedItem(sel[2]);
                    arIdx += 1;
                }
                else {
                    vessels.remove(comps[i]);
                }
            }
        }
        while (arIdx < list.size()) {
            addVessel((String)list.get(arIdx));
            arIdx += 1;
        }
        if (oldgroup != null)
            oldgroup.replaceWaterTravelModes(oldVessels);

        main.getState().setDirty(save);
        vessels.revalidate();
    }

    /**
     * Utility to add/change the items of a vessel selection box list. Given
     * are the stringified dependent items. The return value is need for
     * initialisation of new vessels.
     * @param mode the predetermined mode or null
     * @param panel Panel to add to
     * @param gstr grades stringified
     * @param dstr deployment maximum as string
     * @return the initial values
     */
    private void addDependentVesselItems(String mode, JPanel panel, String gstr, String dstr) {
        String[] grades = ((String)gstr).split(":");

        JComboBox gradeList = new JComboBox(grades);
        if (mode != null) gradeList.setSelectedItem(mode.split(":")[1]);
        gradeList.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    announce();
                }
            });
        panel.add(gradeList, constrSubItem);

        String[] deploy = new String[Integer.parseInt((String)dstr)];
        for (int i = 0; i < deploy.length; i++)
            deploy[i] = Integer.toString(i+1);
        JComboBox deployList = new JComboBox(deploy);
        if (mode != null) deployList.setSelectedItem(mode.split(":")[2]);
        deployList.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    announce();
                }
            });
        panel.add(deployList, constrSubItem);
        announce();
    }

    /**
     * Any change in the list boxes must be propagated.
     */
    private void announce() {
        ArrayList tmodes = new ArrayList();

        Component[] comps = vessels.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JPanel) {
                JPanel vPanel = (JPanel) comps[i];

                // Save old entry
                JComboBox tbox = (JComboBox) vPanel.getComponent(1);
                JComboBox gbox = (JComboBox) vPanel.getComponent(2);
                JComboBox dbox = (JComboBox) vPanel.getComponent(3);

                StringBuffer sb = new StringBuffer
                    ((String) tbox.getSelectedItem());
                sb.append(":").append((String) gbox.getSelectedItem());
                sb.append(":").append((String) dbox.getSelectedItem());
                tmodes.add(sb.toString());
            }
        }
        main.getState().getCurrent().replaceWaterTravelModes(tmodes);

        // Do remaining GUI stuff
        main.getState().setDirty(true);
        vessels.revalidate();
    }
}
