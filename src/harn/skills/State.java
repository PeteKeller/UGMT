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
import java.util.*;
import harn.repository.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * State object.  It is updates through the GUI objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** Root reference */
    private Main main;

    /** State change from previous persistent state */
    boolean dirty;

    /** group list (contains skill lists) */
    private Hashtable glist;

    /** Hidden group set */
    private HashSet hiddenGroups;

    /** Show hidden or the other set */
    boolean showHidden;

    /** Global copy clipboard */
    private Section copy;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
        showHidden = false;
        main = aMain;
        main.skilltree =
            new SortedTreeModel(new DefaultMutableTreeNode("Skillgroups 1"));
        main.otherskilltree =
            new SortedTreeModel(new DefaultMutableTreeNode("Skillgroups 2"));
        hiddenGroups = new HashSet();

        Document dataDoc = Framework.parse(main.myPath + "state.xml", "skills");
        glist = new Hashtable();

        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap attr = tree.item(i).getAttributes();
            if (tree.item(i).getNodeName().equals("skill")) {
                String name = attr.getNamedItem("name").getNodeValue();
                String group = attr.getNamedItem("group").getNodeValue();
                String d = attr.getNamedItem("dice").getNodeValue();
                String ml = attr.getNamedItem("ML").getNodeValue();
                String adj1 = attr.getNamedItem("adj1").getNodeValue();
                String adj2 = attr.getNamedItem("adj2").getNodeValue();
                String adj3 = attr.getNamedItem("adj3").getNodeValue();
                boolean hide = attr.getNamedItem("hidden") != null &&
                    attr.getNamedItem("hidden").getNodeValue().equals("true");

                if (!glist.containsKey(group))
                    glist.put(group, new ArrayList());
                
                ArrayList al = (ArrayList) glist.get(group);
                al.add(new Section(false, d, name, ml, adj1, adj2, adj3, hide));
            }
            if (tree.item(i).getNodeName().equals("group")) {
                String name = attr.getNamedItem("name").getNodeValue();
                if (attr.getNamedItem("hidden").getNodeValue().equals("true"))
                    hiddenGroups.add(name);
            }
            if (tree.item(i).getNodeName().equals("gui")) {
                if (attr.getNamedItem("groups").getNodeValue().equals("special"))
                    main.mgr = true;
                if (attr.getNamedItem("skills").getNodeValue().equals("special"))
                    main.msk = true;
            }
        }
    }

    /**
     * Add a group to the tree.
     * @param name name of group to add
     */
    public void addGroup(String name) {
        if (glist.get(name) == null)
            glist.put(name, new ArrayList());
    }

    /**
     * Tells if group is hidden (the other set)
     * @param name group name
     */
    public boolean isHidden(String name) {
        return hiddenGroups.contains(name);
    }

    /**
     * Remove a group from the tree.
     * @param name name of group to remove
     */
    public void removeGroup(String name) {
        glist.remove(name);
        hiddenGroups.remove(name);
    }

    /**
     * Remove a skill section from the given group.
     * @param group group name
     * @param skill skill name
     */
    public void removeSkill(String group, String skill) {
        ArrayList al = (ArrayList) glist.get(group);
        for (int i = 0; al != null && i < al.size(); i++) {
            if (((Section)al.get(i)).name.equals(skill)) {
                al.remove(i);
                break;
            }
        }
    }

    /**
     * Copy a skill section from the given group.
     * @param group group name
     * @param skill skill name
     */
    public void copySkill(String group, String skill) {
        ArrayList al = (ArrayList) glist.get(group);
        for (int i = 0; al != null && i < al.size(); i++) {
            if (((Section)al.get(i)).name.equals(skill)) {
                copy = (Section)al.get(i);
                main.lineMenu.mi4.setEnabled(true);
                main.insertItem.setEnabled(true);
                break;
            }
        }
    }

    /**
     * Add a skill section to the given group.
     * @param group group name
     */
    public Section newSkill(String group) {
        ArrayList al = (ArrayList) glist.get(group);
        if (al == null) {
            al = new ArrayList();
            glist.put(group, al);
        }

        Section sect =
            new Section(false, "1d100", "Skillname", "50", "", "", "", false);
        al.add(sect);
        return sect;
    }

    /**
     * Insert a skill section to the given group.
     * @param group group name
     */
    public Section insertSkill(String group) {
        if (copy == null) return null;
        ArrayList al = (ArrayList) glist.get(group);
        if (al == null) {
            al = new ArrayList();
            glist.put(group, al);
        }
        Section sect = copy.copy();
        al.add(sect);
        return sect;
    }

    /**
     * Returns the skill list for the group. The arraylist contains string
     * arrays. If the group does not exist, a null is returned.
     * @param group group name
     * @return array list of string arrays describing skills
     */
    public ArrayList getSkillList(String group) {
        return (ArrayList) glist.get(group);
    }

    /**
     * Returns the group list.
     * @return the group list
     */
    public Hashtable groupList() { return glist; }

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
     * Toggle the whole display from standard skills to hidden
     */
    public void toggle() { showHidden = !showHidden; }

    /**
     * Create sections for list.
     * @param c character
     * @param al existing skill/attribute list
     * @param names names of skills/attributes to check
     * @param dice either 3d6 or 1d100
     * @param readonly existing skill/attribute list readonly
     */
    private void createSection(Char c, ArrayList al, String[] names, String dice, ArrayList readonly) {
        TreeSet set = new TreeSet();

        for (int j = 0; j < names.length; j++)
            set.add(names[j]);

        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            String item = (String) iter.next();

            String ml =
                dice.equals("1d100") ? c.getSkill(item) : c.getAttribute(item);

            // Ignore non-numeric and empty attributes
            if (ml == null || !ml.matches("[0-9]+")) continue;

            // Add a section if not already there
            Section s = null;
            for (int i = 0; (s == null) && i < al.size(); i++) {
                s = (Section)al.get(i);
                if (!s.name.equals(item)) s = null;
                if (s != null) readonly.set(i, new Boolean(true));
            }
            if (s == null) {
                s = new Section(true, dice, item, "", "", "", "", false);
                al.add(s);
                readonly.add(new Boolean(true));
            }
            s.ml = ml;
            s.readonly = c.isValid();
            s.revalidate();
        }
    }

    /**
     * Listen to characters
     */
    public void inform(IExport obj) {
        Char c = (Char) obj;

        // Add a group for the character if not already there
        ArrayList al = (ArrayList) glist.get(c.getName());
        if (al == null) {
            main.display.addGroup(c.getName(), true);
            al = (ArrayList) glist.get(c.getName());
        }

        ArrayList readonly = new ArrayList();
        for (int i = 0; i < al.size(); i++) readonly.add(new Boolean(false));

        // Find all skills
        String[] skills = c.getSkillList();
        createSection(c, al, skills, "1d100", readonly);

        // Find all attributes
        String[] attrs = c.getAttributeList();
        createSection(c, al, attrs, "3d6", readonly);

        // Editable?
        for (int j = 0; j < readonly.size(); j++)
            if (!((Boolean)readonly.get(j)).booleanValue()) {
                ((Section)al.get(j)).readonly = false;
                ((Section)al.get(j)).revalidate();
            }


        if (main.tree.getSelectionPath() == null) return;
        TreeNode tn =
            (TreeNode) main.tree.getSelectionPath().getLastPathComponent();
        if (tn.isLeaf() && c.getName().equals(tn.toString()))
            main.display.setSkills(c.getName());
    }

    /**
     * Toggle hidden attribute
     */
    void toggleGroup(String group) {
        if (hiddenGroups.contains(group))
            hiddenGroups.remove(group);
        else
            hiddenGroups.add(group);
    }

    /** Recalibrate GuiLines */
    void recalibrateGuiLines(ArrayList skills) {
        SortedSet ts = new TreeSet();
        for (int i = 0; i < skills.size(); i++) {
            ts.add(((Section)skills.get(i)).name + i);
        }
        Iterator iter = ts.iterator();
        int idy = 1;
        while (iter.hasNext()) {
            String str = (String)iter.next();
            for (int i = 0; i < skills.size(); i++) {
                Section sect = (Section)skills.get(i);
                if (str.equals(sect.name + i)) {
                    if (sect.gui != null) sect.gui.setY(idy++);
                    break;
                }
            }
        }
    }

    /**
     * Use this method to save the data in this object to file. After saving
     * this object is "clean" once more.
     */
    void save() {
        try {
            Document stateDoc = Framework.newDoc();

            Node root = stateDoc.createElement("skills");
            stateDoc.insertBefore(root, stateDoc.getLastChild());
            
            Iterator iter1 = glist.keySet().iterator();
            while (iter1.hasNext()) {
                String group = (String) iter1.next();
                ArrayList al = (ArrayList) glist.get(group);
                for (int i = 0; i < al.size(); i++) {
                    Section sect = (Section) al.get(i);
                    if (sect.ml.length() == 0) continue;

                    Element elem = stateDoc.createElement("skill");
                    root.insertBefore(elem, null);
                    elem.setAttribute("group", group);
                    elem.setAttribute("name", sect.name);
                    elem.setAttribute("dice", sect.d);
                    elem.setAttribute("ML", sect.ml);
                    elem.setAttribute("adj1", sect.adj1);
                    elem.setAttribute("adj2", sect.adj2);
                    elem.setAttribute("adj3", sect.adj3);
                    elem.setAttribute("hidden", Boolean.toString(sect.hidden));
                }
            }

            iter1 = hiddenGroups.iterator();
            while (iter1.hasNext()) {
                String group = (String) iter1.next();

                Element elem = stateDoc.createElement("group");
                root.insertBefore(elem, null);
                elem.setAttribute("name", group);
                elem.setAttribute("hidden", "true");
            }

            Element elem = stateDoc.createElement("gui");
            root.insertBefore(elem, null);
            elem.setAttribute
                ("groups",
                 main.bToggleG.getText().equals("Main Groups") ? "main" : "special");
            elem.setAttribute
                ("skills",
                 main.bToggle.getText().equals("Main Skills") ? "main" : "special");

            File file = new File(main.myPath + "state.xml");
            Framework.backup(main.myPath + "state.xml");
            Result result = new StreamResult(file);

            // Write the document to the file
            Framework.transform(stateDoc, result, null);
            
            setDirty(false);
        }
        catch (Exception e) {
            // Debugging
            e.printStackTrace();
        }
    }

    class Section {
        /** dice choice */
        public String d;
        /** name of skill */
        public String name;
        /** ML */
        public String ml;
        /** First free adjustement */
        public String adj1;
        /** Second free adjustement */
        public String adj2;
        /** Third free adjustement */
        public String adj3;
        /** Read only */
        public boolean readonly;
        /** Gui line */
        GuiLine gui;
        /** Hidden */
        private boolean hidden;
        /**
         * Constructor.
         * @param aReadonly readonly flag
         * @param aD dice choice
         * @param aName name of skill
         * @param anMl ML
         * @param anAdj1 First free adjustement
         * @param anAdj2 Second free adjustement
         * @param anAdj3 Third free adjustement
         * @param hidden hide skill or not
         */
        Section(boolean aReadonly, String aD, String aName, String anMl, String anAdj1, String anAdj2, String anAdj3, boolean toHide) {
            d = aD;
            name = aName;
            ml = anMl;
            adj1 = anAdj1;
            adj2 = anAdj2;
            adj3 = anAdj3;
            hidden = toHide;
        }
        /** Clone */
        Section copy() {
            return new Section(false, d, name, ml, adj1, adj2, adj3, hidden);
        }
        /**
         * Set the gui line and force redisplay.
         * @param top panel to add the guiline
         */
        public void addGui(JPanel top) {
            if (showHidden == hidden) {
                if (gui == null) {
                    gui = new GuiLine(this, top, main);
                }
            }
        }
        /**
         * Revalidate if on GUI.
         */
        public void revalidate() { if (gui != null) gui.revalidate(); }
        /**
         * Toggle hidden state
         */
        public void toggle() { hidden = !hidden; }
    }
}
