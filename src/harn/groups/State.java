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

import rpg.SortedTreeModel;
import harn.repository.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.Framework;

/**
 * State object. This represents the active groups state.
 * @author Michael Jung
 */
public class State {
    /** Root reference */
    private Main main;

    /** Statefiles already merged */
    private String merged;

    /** indicates whether anything is to save */
    private boolean dirty = false;

    /** Representation of the state.xml file (and changes) */
    private Document stateDoc;

    /** List of known char groups */
    private LinkedList list;

    /** Index of currently visible group */
    private int current;

    /**
     * Constructor. Loads the state of the groups. This is not a simple hashmap
     * of hashmaps.
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
        list = new LinkedList();

        stateDoc = Framework.parse(main.myPath + "state.xml", "state");
        Node mergEl =
            stateDoc.getDocumentElement().getElementsByTagName("merged").item(0);
        merged = "state.xml";
        if (mergEl != null)
            merged =
                Framework.getXmlAttrValue(mergEl.getAttributes(), "list", "state.xml");

        String charl[] = Framework.getStateFiles(main.myPath, merged);

        for (int i = 1; i < charl.length; i++) {
            Document tmpDoc = Framework.parse(main.myPath + charl[i], "state");
            Element root = stateDoc.getDocumentElement();
            NodeList list = tmpDoc.getDocumentElement().getChildNodes();
            for (int j = 0; j < list.getLength(); j++) {
                root.insertBefore
                    (stateDoc.importNode(list.item(j), true),
                     root.getLastChild());
            }
            root.removeChild(root.getLastChild());
        }
        list = loadStatesFromDoc();
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * is accessible by this method.
     * @return dirty flag
     */
    boolean getDirty() { return dirty; }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled
     * @return dirty flag
     */
    public void setDirty(boolean flag) {
	dirty = flag;
	if (main.bSave != null)
	    main.bSave.setEnabled(dirty);
    }

    /**
     * Use this method to save the data in this object to file. AFter saving
     * this object is "clean" once more.
     */
    void save() {
        Framework.setMergedStates(stateDoc, main.myPath, merged);
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
        setDirty(false);
    }

    /**
     * Load the groups, their travel mode and the like. Creates a hashmap
     * referencing all groups.
     * @return list containing the groups.
     */
    private LinkedList loadStatesFromDoc() {
	LinkedList ret = new LinkedList();
	main.grouptree = new DefaultMutableTreeNode("Group List");

	// Used later for GUI
	DefaultMutableTreeNode cgNode;

	// We choose the first one to be active by default
	boolean act = true;
	NodeList tree = stateDoc.getDocumentElement().getChildNodes();
	for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    Node node = tree.item(i);
	    NamedNodeMap attr = node.getAttributes();
            if (node.getNodeName().equals("merged") &&
                attr.getNamedItem("list") != null) {
                merged = attr.getNamedItem("list").getNodeValue();
            }
	    if (node.getNodeName().equals("group")) {
		// Get attributes
		String name = attr.getNamedItem("name").getNodeValue();
		String map = attr.getNamedItem("map").getNodeValue();
		String xs = attr.getNamedItem("x").getNodeValue();
		int x = Integer.parseInt(xs);
		String ys = attr.getNamedItem("y").getNodeValue();
		int y = Integer.parseInt(ys);
		String ds = attr.getNamedItem("date").getNodeValue();
		String ss = attr.getNamedItem("speed").getNodeValue();
		float speed = Float.parseFloat(ss);

		// Get travel modes & members
		ArrayList modes = new ArrayList();
		ArrayList members = new ArrayList();
		NodeList tree2 = node.getChildNodes();
		for (int j = 0; tree2 != null && j < tree2.getLength(); j++) {
                    if (tree2.item(j).getNodeName().equals("travelmode")) {
                        NamedNodeMap attr2 = tree2.item(j).getAttributes();
                        modes.add(attr2.getNamedItem("name").getNodeValue());
                    }
                    if (tree2.item(j).getNodeName().equals("member")) {
                        NamedNodeMap attr2 = tree2.item(j).getAttributes();
                        members.add(attr2.getNamedItem("name").getNodeValue());
                    }
                }

		// Adjust GUI
		cgNode = new DefaultMutableTreeNode(name);
                main.grouptree.add(cgNode);

		// Create group
		MyCharGroup cg = new MyCharGroup
		    (name, act, x, y, map, ds, node, modes, speed, members);
		ret.add(cg);
		act = false;

		// Announce
                if (cg.myDate != 0) main.myFrw.announce(cg);
	    }
	}
	return ret;
    }

    /**
     * Only one group may be active at a given time. This method toggles the
     * flag for all involved groups.
     * @param name new active group
     */
    void setActive(String name) {
	for (int i = 0; i < list.size(); i++) {
	    MyCharGroup cg = (MyCharGroup) list.get(i);
	    if (cg.myActive && !cg.myName.equals(name)) {
		cg.myActive = false;
		main.myFrw.announce(cg);
	    }
	    else if (!cg.myActive && cg.myName.equals(name)) {
		cg.myActive = true;
		main.display.revalidate();
                main.tree.repaint();
		main.myFrw.announce(cg);
	    }
	}
    }
    
    /**
     * Only one group may be active at a given time. This method looks up the
     * active flag for a given name.
     * @param name name of group
     * @return whether active or not
     */
    boolean getActive(String name) {
	for (int i = 0; i < list.size(); i++) {
	    MyCharGroup cg = (MyCharGroup) list.get(i);
	    if (cg.myName.equals(name))
		return cg.myActive;
        }
        return false;
    }
    
    /**
     * Only one group may be the currently displayed at a given time. This
     * method gets it
     * @return current group
     */
    MyCharGroup getCurrent() { return (MyCharGroup) list.get(current); }

    /**
     * Get a group for a given name.
     * @param name name of group
     * @return named  group
     */
    MyCharGroup getGroup(String name) {
        for (int i = 0; i < list.size(); i++) {
            MyCharGroup cg = (MyCharGroup) list.get(i);
            if (cg.getName().equals(name))
                return cg;
        }
        return null;
    }

    /**
     * Only one group may be the currently displayed at a given time. This
     * method gets it
     * @param name new active group
     */
    void setCurrent(String name) {
        MyCharGroup oldcg = (MyCharGroup) list.get(current);

        MyCharGroup cg = null;
        for (int i = 0; i < list.size(); i++) {
            cg = (MyCharGroup) list.get(i);
            if (cg.getName().equals(name)) {
                current = i;
                break;
            }
        }

        String[] cgmodes = cg.getTravelModes();
        ArrayList lmodes = main.getData().getModes();

        // Land modes
        for (int i = 0; i < lmodes.size(); i++) {
            boolean ismode = false;
            for (int j = 0; j < cgmodes.length; j++)
                if (cgmodes[j].equals(lmodes.get(i))) {
                    ismode = true;
                    break;
                }
            main.display.setMode((String)lmodes.get(i), ismode);
        }

        // Water modes
        ArrayList wmodes = new ArrayList();
        for (int j = 0; j < cgmodes.length; j++)
            if (cgmodes[j].indexOf(":") >= 0) {
                wmodes.add(cgmodes[j]);
            }

        main.display.setVessels(wmodes, cg != oldcg ? oldcg : null);

        // Members
        main.display.setMembers(cg.getMembers(), oldcg);

        // Speed
        main.getData().setSpeed(cg);
    }

    /**
     * Late announcement because of late timeframe. Check the date.
     * @param tf time frame
     */
    void announce(TimeFrame tf) {
	for (int i = 0; i < list.size(); i++) {
	    MyCharGroup cg = (MyCharGroup)list.get(i);
	    cg.myDate = tf.string2datetime(cg.myDateStr);
	    main.myFrw.announce(cg);
	}
    }

    /**
     * Remove a new group.
     */
    void removeGroup(String name) {
        MyCharGroup cg = null;
        int ridx = 0;
        for (ridx = 0; ridx < list.size(); ridx++)
            if (name.equals(((MyCharGroup)list.get(ridx)).getName())) {
                cg = (MyCharGroup) list.get(ridx);
                break;
            }

        // Finalize surrogat
        cg.destroy();

        // Make a default current
        int c = (ridx == 0 ? 1 : 0);
        if (cg.myActive)
            setActive(((MyCharGroup)list.get(c)).getName());

        list.remove(ridx);
        if (current > 0) current -= 1;

        // Update GUI
        ((DefaultTreeModel)main.tree.getModel()).removeNodeFromParent
            ((MutableTreeNode)main.grouptree.getChildAt(ridx));
        main.tree.addSelectionInterval(c,c);
    }

    /**
     * Create a new group, announce, etc. But don't make it active.
     */
    void newGroup() {
        ArrayList modes = new ArrayList();
        modes.add("foot");

        ArrayList members = new ArrayList();
        float speed = 0.0f;
        String name = "New Group";
        String date = "1-Nuz-720 0:00";
        String map = "-";
        int x = 0;
        int y = 0;

        // Adjust DOM structures
        Node parent =  stateDoc.getDocumentElement();

        Element node = stateDoc.createElement("group");
        parent.insertBefore(node, parent.getLastChild());

        node.setAttribute("name", name);
        node.setAttribute("map", map);
        node.setAttribute("x", Integer.toString(x));
        node.setAttribute("y", Integer.toString(y));
        node.setAttribute("date", date);
        node.setAttribute("speed", Float.toString(y));

        // Internal object
        MyCharGroup cg = new MyCharGroup
            (name, false, x, y, map, date, node, modes, speed, members);
        list.addFirst(cg);

        // Adjust GUI
	DefaultMutableTreeNode cgNode = new DefaultMutableTreeNode(name);
        ((SortedTreeModel)main.tree.getModel()).insertNodeSorted
            (cgNode, main.grouptree);

        // Announce & make current
        main.myFrw.announce(cg);
        main.tree.addSelectionInterval(1,1);
    }
    
    /**
     * Change the name of a group
     * @param idx index in list
     * @param name new name
     */
    void setName(int idx, String newName) {
        ((MyCharGroup)list.get(idx)).setName(newName);
    }

    /**
     * Class that holds a (character) group instance. Note that only one date
     * is valid - the string or long version. Until the long version isn't
     * set, the object will not be announced.
     */
    class MyCharGroup extends CharGroup {
	/** name of group */
	private String myName;

	/** whether group is active */
	private boolean myActive;

	/** x coordinate */
	private int myX;

	/** y coordinate */
	private int myY;

	/** map name for map wherein coordinates lie */
	private String myMap;

	/** current date of group (or 0) */
	private long myDate;

	/** current string date of group if timeframe not yet available */
	private String myDateStr;

	/** XML doc node reference */
	private Node myNode;

	/** travel modes */
	private ArrayList tmodes;

	/** inherent speed */
	private float speed;

        /** members */
        private ArrayList myMembers;

	/**
	 * Constructor.
	 * @param name name of group
	 * @param active whether group is active
	 * @param map the map identifier for the location
	 * @param x map coordinate
	 * @param y map coordinate
	 * @param date new date of group
	 * @param attr attributes in XML document
	 */
	MyCharGroup(String name, boolean active, int x, int y, String map, String date, Node node, ArrayList modes, float sp, ArrayList members) {
	    myName = name;
	    myActive = active;
	    myNode = node;
	    tmodes = modes;
	    speed = sp;
	    myMap = map;
	    myX = x;
	    myY = y;
            myMembers = members;
            TimeFrame tf = main.getData().getTimeframe();
	    if (tf != null) {
		myDate = tf.string2datetime(date);
	    }
	    else {
		myDateStr = date;
	    }
	}

        /**
         * Destructor substitute
         */
        void destroy() {
            myNode.getParentNode().removeChild(myNode);
        }

	/**
	 * Implement as specified in superclass.
	 * @return name attribute
	 */
	public String getName() { return myName; }

	/**
	 * Return Member list.
	 * @return members
	 */
	public ArrayList getMembers() { return myMembers; }

	/**
	 * Set Member list
	 * @param members new member list
	 */
	void setMembers(ArrayList members) {
            Object[] old = myMembers.toArray();

            for (int i = 0; i < old.length; i++)
                removeMember((String) old[i], false);

            for (int j = 0; j < members.size(); j++)
                addMember((String)members.get(j), false);
        }

	/**
	 * Add member to list. IF method.
	 * @param name name of member to add
	 */
	public void addMember(String name) {
            addMember(name, true);
        }

	/**
	 * Add member to list
	 * @param name name of member to add
         * @param makedirty whether to announce and dirty this change
	 */
	public void addMember(String name, boolean makedirty) {
            myMembers.add(name);

            // Put into DOM
            Document doc = myNode.getOwnerDocument();

            // Put item
            Element nn = doc.createElement("member");
            nn.setAttribute("name", name);
            myNode.insertBefore(nn, myNode.getLastChild());
            if (makedirty) {
                setDirty(true);
                main.myFrw.announce(this);
            }

            // Recalc speed
            if (getCurrent() == this) {
                main.getData().setSpeed(this);
                main.display.addMember(name);
            }
        }

	/**
	 * Remove a member from list. IF method.
	 * @param name name of member to remove
	 */
	public void removeMember(String name) {
            removeMember(name, true);
        }

	/**
	 * Remove a member from list
	 * @param name name of member to remove
         * @param makedirty whether to announce and dirty this change
	 */
	public void removeMember(String name, boolean makedirty) {
            for (int i = 0; i < myMembers.size(); i++) {
                if (myMembers.get(i).equals(name)) {
                    myMembers.remove(i);
                    if (makedirty) {
                        setDirty(true);
                        main.myFrw.announce(this);
                    }
                    break;
                }
            }
            // Remove from DOM
            NodeList children = myNode.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                NamedNodeMap attr = child.getAttributes();
                if (attr != null) {
                    String locname = attr.getNamedItem("name").getNodeValue();
                    if (locname.equals(name)) {
                        // Remove item
                        myNode.removeChild(child);
                        break;
                    }
                }
            }
            // Recalc speed
            if (getCurrent() == this)
                main.getData().setSpeed(this);
        }

	/**
	 * Set name.
	 * @return name attribute
	 */
	private void setName(String aName) {
            myName = aName;
            NamedNodeMap attr = myNode.getAttributes();
            attr.getNamedItem("name").setNodeValue(aName);
            setDirty(true);
        }

	/**
	 * Only one group may be active at a given time. It is used by plugins
	 * which can handle only one group at a time or need to designate a
	 * special group.
	 * @return true if the one active group
	 */
	public boolean isActive() { return myActive; }
    
	/**
	 * Called by plugins that move a group. The map attribute may not be
	 * meaningful to a provider, but a location only makes sense in that
	 * reference frame. The time may also changed. All changes are
	 * combined to avoid redundant announcements.<br> If only partial
	 * location information is set, use the getters below for the
	 * unchanged parts.
	 * @param map the map identifier for the location
	 * @param x map coordinate
	 * @param y map coordinate
	 * @param date new date of group
	 */
	public void setLocation(String map, int x, int y, long date) {
	    // We don't really tolerate junk
	    if (map == null) return;

	    if (!map.equals(myMap) || myX != x || myY != y || myDate != date) {
		TimeFrame tf = main.getData().getTimeframe();
		NamedNodeMap attr = myNode.getAttributes();

		// Update this
		myMap = map;
		myX = x;
		myY = y;
		myDate = date;

		// Update XML
		attr.getNamedItem("map").setNodeValue(map);
		attr.getNamedItem("x").setNodeValue(Integer.toString(x));
		attr.getNamedItem("y").setNodeValue(Integer.toString(y));
		attr.getNamedItem("date").setNodeValue
		    (tf != null ? tf.datetime2string(date) : "0-0-0");

		// Announce and tell GUI
		main.myFrw.announce(this);
		setDirty(true);
	    }
	}

	/**
	 * Return current map. If the group could technically be located on
	 * several maps, choose one but be consistent.
	 * @return map identifier
	 */
	public String getMap() { return myMap; }

	/**
	 * Return the current x coordinate on the map the group is located on.
	 * @return x coordinate
	 */
	public int getX() { return myX; }

	/**
	 * Return the current y coordinate on the map the group is located on.
	 * @return y coordinate
	 */
	public int getY() { return myY; }

	/**
	 * Gets the date of the current group belonging to the location given.
	 * @return current date of group
	 */
	public long getDate() { return myDate; }

	/**
	 * Gets the travel modes the group currently uses. A travel plugin must
	 * know these.
	 * @return mode list
	 */
	public String[] getTravelModes() {
	    Object[] oa = tmodes.toArray();
	    String[] sa = new String[oa.length];
	    for (int i = 0; i < sa.length; i++) {
		sa[i] = (String) oa[i];
            }
	    return sa;
	}

        /**
         * Replace the previous water travel modes with the new ones. This
         * will be notified. We replace water modes by creating a new HashSet
         * altogether.
         * @param newlist new vessel travel list
         */
        void replaceWaterTravelModes(ArrayList vessels) {
            Object[] old = tmodes.toArray();

            // Remove old
            for (int i = 0; i < old.length; i++) {
                String mode = (String) old[i];
                if (mode.indexOf(":") >= 0)
                    removeTravelMode(mode, false);
            }

            // Add new
            for (int j = 0; j < vessels.size(); j++)
                addTravelMode((String)vessels.get(j), false);

            main.myFrw.announce(this);
        }

	/**
	 * Get the inherent speed of the group in km/h (leagues/watch).
	 */
	public float getSpeed() { return speed; }

	/**
	 * Adds a land travel mode. This avoids duplicates.
	 * @param new mode
         * @param makedirty whether to announce and dirty this change
	 */
	void addLandTravelMode(String mode, boolean makedirty) {
            if (tmodes.indexOf(mode) == -1)
                addTravelMode(mode, makedirty);
        }

	/**
	 * Adds a travel mode.
	 * @param new mode
         * @param makedirty whether to announce and dirty this change
	 */
	void addTravelMode(String mode, boolean makedirty) {
	    tmodes.add(mode);

            // Put into DOM
            Document doc = myNode.getOwnerDocument();

            // Put item
            Element nn = doc.createElement("travelmode");
            nn.setAttribute("name", mode);
            myNode.insertBefore(nn, myNode.getLastChild());

            if (makedirty) {
                setDirty(true);
                main.myFrw.announce(this);
            }
	}

	/**
	 * Removes a travel mode.
	 * @param new mode
         * @param makedirty whether to announce and dirty this change
	 */
	void removeTravelMode(String mode, boolean makedirty) {
	    int idx = tmodes.indexOf(mode);
            if (idx == -1) return;

            tmodes.remove(idx);

            // Remove from DOM
            NodeList children = myNode.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                NamedNodeMap attr = child.getAttributes();
                if (attr != null) {
                    String name = attr.getNamedItem("name").getNodeValue();
                    if (mode.equals(name)) {
                        // Remove item
                        myNode.removeChild(child);
                        break;
                    }
                }
            }

            if (makedirty) {
                setDirty(true);
                main.myFrw.announce(this);
            }
        }
    }
}
