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
package harn.combat;

import rpg.*;
import harn.repository.*;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.tree.*;
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

    /**
     * Equipment list (top equipment as keys); contains strings (normalised
     * combat equipment) for combatants.
     */
    private Hashtable equipList;

    /** Equipment list (top equipment as keys) */
    private Hashtable equipList2;

    /** dirty flag */
    private boolean dirty;

    /** combatant list (contains Guiline lists) */
    private Hashtable combatArmour;

    /** combatant list (contains string lists) */
    private Hashtable combatWeapon;

    /** combatant list (contains ) */
    private Hashtable wounds;

    /** char list, to notify state change */
    private Hashtable chars;

    /** Name of currently selected combatant */
    private String currentName;

    /** Calendar reference */
    protected TimeFrame cal;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
        main = aMain;
        equipList = new Hashtable();
        equipList2 = new Hashtable();
        combatArmour = new Hashtable();
        combatWeapon = new Hashtable();
        wounds = new Hashtable();

        dirty = false;
        chars = new Hashtable();

        main.tree.setModel
            (new MyTreeModel(new DefaultMutableTreeNode("Combatants")));

        Document dataDoc = Framework.parse(main.myPath + "state.xml", "combat");
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap attr = tree.item(i).getAttributes();
            if (tree.item(i).getNodeName().equals("entry")) {
                String group = attr.getNamedItem("name").getNodeValue();
                String item = attr.getNamedItem("item").getNodeValue();
                String used = attr.getNamedItem("used").getNodeValue();
                String material =
                    attr.getNamedItem("material").getNodeValue();
                String quality =
                    attr.getNamedItem("quality").getNodeValue();
                if (!combatArmour.containsKey(group))
                    combatArmour.put(group, new ArrayList());

                ArrayList al = (ArrayList) combatArmour.get(group);
                al.add
                    (new ArmourLine
                     (main, item, material, quality, used.equals("true")));
            }
            if (tree.item(i).getNodeName().equals("location")) {
                String group = attr.getNamedItem("name").getNodeValue();
                String x = attr.getNamedItem("x").getNodeValue();
                String y = attr.getNamedItem("y").getNodeValue();
                String a = attr.getNamedItem("angle").getNodeValue();
                double[] loc = new double[] {
                    Double.parseDouble(x), Double.parseDouble(y),
                    Double.parseDouble(a)
                };
                if (!combatWeapon.containsKey(group))
                    combatWeapon.put(group, new ArrayList());                
                main.map.addToken(group, loc);
            }
            if (tree.item(i).getNodeName().equals("weapon")) {
                String group = attr.getNamedItem("name").getNodeValue();
                String item = attr.getNamedItem("item").getNodeValue();
                String damage = attr.getNamedItem("damage").getNodeValue();
                String quality = attr.getNamedItem("quality").getNodeValue();
                boolean used = attr.getNamedItem("used") == null ||
                    attr.getNamedItem("used").getNodeValue().equals("true");

                if (!combatWeapon.containsKey(group))
                    combatWeapon.put(group, new ArrayList());
                ArrayList al = (ArrayList) combatWeapon.get(group);
                al.add(new WeaponLine
                       (main, item, damage, quality,
                        main.getData().getWHandmod(item),
                        main.getData().getWMlmod(item),
                        main.getData().getWSkill(item),
                        main.getData().getWWeight(item),
                        used));
            }
            if (tree.item(i).getNodeName().equals("wound")) {
                String group = attr.getNamedItem("name").getNodeValue();
                boolean used = attr.getNamedItem("used") == null ||
                    attr.getNamedItem("used").getNodeValue().equals("true");
                String[] fields = main.getData().getWoundFields();
                String val[] = new String[fields.length];
                for (int j = 0; j < fields.length; j++)
                    if (attr.getNamedItem(fields[j]) != null)
                        val[j] = attr.getNamedItem(fields[j]).getNodeValue();
                if (!wounds.containsKey(group))
                    wounds.put(group, new ArrayList());
                ArrayList al = (ArrayList) wounds.get(group);
                al.add
                    (new WoundLine
                     (main, group, val, main.getData().getWoundFieldTypes(),
                      main.getData().getWoundFieldChoices(),
                      main.getData().getWoundFieldHelps(),
                      used));
            }
        }

        Iterator iter = combatArmour.keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            newCombatant(group);
        }
        iter = combatWeapon.keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            newCombatant(group);
        }

        main.addTreeModelListener();
        setDirty(false);
    }

    /**
     * Get the code (PIN).
     * @param name name of char
     * @return value of code
     */
    public String getCode(String name) {
        if (chars == null) return null;
        return (String) ((Char)chars.get(name)).getCode();
    }

    /**
     * Get the current Char if it is a Char.
     */
    public Char getChar(String name) { return (Char) chars.get(name); }

    /**
     * Export the current combat charts to HTML
     */
    public void export(String achar) {
        Char mychar = (Char)chars.get(achar);
        if (mychar == null) return;
        
        try {
            String intemplate = "template.html";

            // Read in template
            BufferedReader br = new BufferedReader
                (new FileReader(new File(main.myPath + intemplate)));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = br.readLine()) != null) buf.append(str).append("\n");
            String template  = buf.toString();
            String out = template;

            // Replace Name
            out = out.replaceAll("\\$Name", mychar.getName());

            // Replace attributes
            String[] attr = mychar.getAttributeList();
            for (int i = 0; i < attr.length; i++) {
                out = out.replaceAll
                    ("\\$" + attr[i], mychar.getAttribute(attr[i]));
            }

            // Replace skills
            String[] skills = mychar.getSkillList();
            for (int i = 0; i < skills.length; i++) {
                out = out.replaceAll
                    ("\\$Sk-" + skills[i], mychar.getSkill(skills[i]));
            }

            // Replace Armour
            double totalAW = 0;
            ArrayList al = (ArrayList) combatArmour.get(mychar.getName());
            for (int i = 0; i < al.size(); i++) {
                ArmourLine gl = (ArmourLine) al.get(i);
                out = out.replaceAll
                    ("\\$Armour-" + (i+1) + "N", gl.item());
                String q = gl.quality().length() < 3 ? gl.quality() : "*";
                out = out.replaceAll
                    ("\\$Armour-" + (i+1) + "Q", q);
                out = out.replaceAll
                    ("\\$Armour-" + (i+1) + "M", gl.material());
                double w = gl.weight();
                totalAW += w;
                out = out.replaceAll
                    ("\\$Armour-" + (i+1) + "W", convertDouble(w, 2));
            }

            // Coverage and materials
            ArmourAnalysis aa = main.armour;
            int nrows = aa.getRowCount();
            int ncols = aa.getColumnCount();
            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    str = (String)aa.getValueAt(i,j);
                    if (str != null)
                        out = out.replaceAll
                            ("\\$Prot-" + aa.getRowName(i) +
                             aa.getColumnName(j), str);
                }
                str = "";
                Object[] mats = aa.getRowMaterial(i);
                for (int j = 0; mats != null & j < mats.length; j++)
                    str += "+" + mats[j].toString();
                if (str.length() != 0)
                    out = out.replaceAll
                        ("\\$Material-" + aa.getRowName(i), str.substring(1));
            }

            // Weapons
            double totalWW = 0;
            al = (ArrayList) combatWeapon.get(mychar.getName());
            for (int i = 0; i < al.size(); i++) {
                WeaponLine gl = (WeaponLine) al.get(i);
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "N", gl.item());
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "Q", gl.quality());
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "B", gl.damage());
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "H", gl.handmod());
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "M", gl.mlmod());
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "A", gl.aml(mychar));
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "D", gl.dml(mychar));
                double w = gl.weight();
                totalWW += w;
                out = out.replaceAll
                    ("\\$Weapon-" + (i+1) + "W", convertDouble(w, 2));
            }

            // Weights
            out = out.replaceAll("\\$WeaponTotal", convertDouble(totalWW, 2));
            out = out.replaceAll("\\$ArmourTotal", convertDouble(totalAW, 2));

            al = (ArrayList)equipList2.get(achar);
            double total = 0;
            for (int i = 0; (al != null) && (i < al.size()); i++) {
                double w = ((Equipment)al.get(i)).getWeight();
                if (w > 0) total += w;
            }
            out = out.replaceAll("\\$TotalTotal", convertDouble(total, 2));

            // Wounds
            al = (ArrayList)wounds.get(mychar.getName());
            int totalW = 0;
            for (int i = 0; al != null && i < al.size(); i++) {
                WoundLine wl = (WoundLine) al.get(i);
                out = out.replaceAll
                    ("\\$Wound-" + (i+1) + "L", wl.getValue(mychar, "Loc"));
                out = out.replaceAll
                    ("\\$Wound-" + (i+1) + "H", wl.getValue(mychar, "HR"));
                out = out.replaceAll
                    ("\\$Wound-" + (i+1) + "I", wl.getValue(mychar, "Pts"));
                try {
                    totalW += Integer.parseInt(wl.getValue(mychar, "Pts"));
                }
                catch (Throwable e) {
                    // All NullPointers and Divisions by zero ignored
                }
            }

            // Special
            try {
                String endS = mychar.getAttribute("Endurance");
                double end = Double.parseDouble(endS);
                out = out.replaceAll
                    ("\\$EncumbrancePenalty", convertDouble(total/Math.abs(end) + .5, 0));
                out = out.replaceAll("\\$InjuryPenalty", Integer.toString(totalW));
            }
            catch (Throwable e) {
                // All NullPointers and Divisions by zero ignored
            }

            // Clean out
            out = out.replaceAll("\\$[a-zA-Z0-9/,. -]*", "");

            // Output
            String outf = mychar.getCode() + ".html";
            FileWriter fw = new FileWriter(new File(main.myPath + outf));
            fw.write(out);
            fw.close();
        }
        catch(IOException e) {
            // Debugging
            e.printStackTrace();
        }
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

    /**
     * Method to be informed about calendar events and active group.
     * @param e calendar being announced
     */
    public void inform(IExport ex) {
        if (ex instanceof Char) {
            Char c = (Char) ex;
            MyTreeModel tm = (MyTreeModel) main.tree.getModel();
            DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) tm.getRoot();

            chars.put(c.getName(), c);
            if (c.isCombatant()) {
                // Check combatArmour
                if (combatArmour.get(ex.getName()) == null) {
                    tm.insertNodeSorted
                        (new DefaultMutableTreeNode(ex.getName()), root);
                    combatArmour.put(ex.getName(), new ArrayList());
                    main.map.addToken(ex.getName());
                    setDirty(true);
                    if (equipList.get(ex.getName()) != null) {
                        ArrayList al = (ArrayList)equipList.get(ex.getName());
                        equipList.put
                            (ex.getName(), equip2strings(ex.getName(), al));
                        redoCombatant(ex.getName());
                    }
                }
                // Check combatWeapon
                if (combatWeapon.get(ex.getName()) == null) {
                    combatWeapon.put(ex.getName(), new ArrayList());
                    setDirty(true);
                    if (equipList.get(ex.getName()) != null) {
                        ArrayList al = (ArrayList)equipList.get(ex.getName());
                        equipList.put
                            (ex.getName(), equip2strings(ex.getName(), al));
                        redoCombatant(ex.getName());
                    }
                }
            }
        }

        if (ex instanceof Equipment) {
            Equipment e = (Equipment)ex;
            loadEquipment(e);
        }
        if (ex instanceof TimeFrame) {
            cal = (TimeFrame)ex;
        }
    }

    /**
     * Recursively load equipment. This assumes that the announcing plugin
     * adheres to the recommendation to announce only first level
     * equipment. Otherweise this multiples startup time.
     */
    private void loadEquipment(Equipment e) {
        Equipment top = e;
        while (top.getFather() != null) top = top.getFather();

        // Put the missing string arrays into the equipment list
        ArrayList equip = (ArrayList) equipList.get(top.getName());
        ArrayList equip2 = (ArrayList) equipList2.get(top.getName());
        if (equip == null) {
            equip = new ArrayList();
            equipList.put(top.getName(), equip);
        }
        if (equip2 == null) {
            equip2 = new ArrayList();
            equipList2.put(top.getName(), equip2);
        }

        // Recurse
        Equipment[] subs = e.getChildren();
        for (int i = 0; i < subs.length; i++)
            loadEquipment(subs[i]);

        if (top == e) return;

        if (e.isValid()) {
            if (!equip.contains(e)) equip.add(e);
            if (!equip2.contains(e)) equip2.add(e);

            if (combatArmour.get(top.getName()) != null) {
                equipList.put(top.getName(), equip2strings(top.getName(), equip));
                // Check combatArmour
                redoCombatant(top.getName());
            }
        }
        else {
            for (int i = 0; i < equip.size(); i++)
                if (equip.get(i) == e) { equip.remove(i); break; }
            for (int i = 0; i < equip2.size(); i++)
                if (equip2.get(i) == e) { equip2.remove(i); break; }
        }
    }

    /**
     * Turn equipment list into string[] list
     */
    private ArrayList equip2strings(String container, ArrayList equip) {
        ArrayList ret = new ArrayList();
        ArrayList present = (ArrayList) equipList.get(container);

        for (int l = 0; l < present.size(); l++) {
            Object o = present.get(l);
            if (o instanceof String[]) {
                ret.add(o);
                continue;
            }
            Equipment e = (Equipment)o;
            ArrayList test = main.getData().match(e.getName());
            for (int i = 0; i < test.size(); i++) {
                String[] testA = (String[]) test.get(i);
                boolean found = false;
                // Is test[i] present in equip array
                for (int j = 0; !found && j < ret.size(); j++) {
                    String[] equipA = (String[]) ret.get(j);
                    found = equipA.length == testA.length;
                    // String array equality
                    for (int k = 0; found && k < equipA.length; k++)
                        found &= testA[k].equals(equipA[k]);
                }
                if (!found) ret.add(testA);
            }
        }
        return ret;
    }

    /**
     * Redo combatant (because new item was added). Assumes both maps contain
     * the name.
     */
    private void redoCombatant(String name) {
        ArrayList strs = (ArrayList) equipList.get(name); // string arrays
        ArrayList aguils = (ArrayList) combatArmour.get(name); // ArmourLine
        ArrayList wguils = (ArrayList) combatWeapon.get(name); // WeaponLine
        for (int i = 0; i < strs.size(); i++) {
            String[] str = (String[]) strs.get(i);
            boolean found = false;
            if (str.length == 3) {
                for (int j = 0; !found && aguils != null && j < aguils.size(); j++) {
                    ArmourLine guil = (ArmourLine) aguils.get(j);
                    found = guil.item().equals(str[2]) &&
                        guil.material().equals(str[1]) &&
                        guil.quality().equalsIgnoreCase(str[0]);
                }
                if (!found && aguils != null)
                    aguils.add(new ArmourLine(main, str[2], str[1], str[0], false));
            }
            else {
                for (int j = 0; !found && wguils != null && j < wguils.size(); j++) {
                    WeaponLine guil = (WeaponLine) wguils.get(j);
                    found = guil.item().equals(str[0]);
                }
                if (!found && wguils != null)
                    wguils.add(new WeaponLine(main, str[0], str[1], str[2], str[3], str[4], str[5], str[6], true));
            }
        }
 
        // Update GUI
        TreeNode tn = (TreeNode) main.tree.getLastSelectedPathComponent();
        if (tn == null || !tn.isLeaf()) return;

        if (name.equals(tn.toString())) {
            select(tn.toString());
            main.armour.recalc();
        }
    }

    /**
     * Set a new name for old character
     */
    public void setName(MutableTreeNode node, String newName) {
        if (newName.equals(currentName)) return;
        if (chars.get(currentName) != null) {
            // Undo
            node.setUserObject(currentName);
            return;
        }
        // Change lists (Equipment lists are only for characters)
        Object o = combatArmour.get(currentName);
        combatArmour.put(newName, o);
        o = combatWeapon.get(currentName);
        combatWeapon.put(newName, o);

        // Token
        main.map.renameToken(currentName, newName);

        // Finally
        currentName = newName;
    }

    /**
     * Select the character with the given name and show his items.
     */
    public void select(String name) {
        currentName = name;
        // Armour
        main.aitems.clearAll();
        ArrayList ilist = (ArrayList) combatArmour.get(name);
        for (int i = 0; ilist != null && i < ilist.size(); i++)
            main.aitems.addItem((Line)ilist.get(i));
        if (ilist == null || ilist.size() == 0) {
            main.aitems.revalidate();
            main.aitems.repaint();
        }

        // Weapons
        main.witems.clearAll();
        ilist = (ArrayList) combatWeapon.get(name);
        for (int i = 0; ilist != null && i < ilist.size(); i++)
            main.witems.addItem((Line)ilist.get(i));
        if (ilist == null || ilist.size() == 0) {
            main.witems.revalidate();
            main.witems.repaint();
        }

        // Wounds
        main.vitems.clearAll();
        ilist = (ArrayList) wounds.get(name);
        for (int i = 0; ilist != null && i < ilist.size(); i++)
            main.vitems.addItem((Line)ilist.get(i));
        if (ilist == null || ilist.size() == 0) {
            main.vitems.revalidate();
            main.vitems.repaint();
        }
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled
     * @return dirty flag
     */
    public void setDirty(boolean flag) {
	dirty = flag;
	if (main.bSave != null)
	    main.bSave.setEnabled(flag);
    }

    /**
     * Use this method to save the data in this object to file. After saving
     * this object is "clean" once more.
     */
    void save() {
        Document stateDoc = Framework.newDoc();

        Node root = stateDoc.createElement("combat");
        stateDoc.insertBefore(root, stateDoc.getLastChild());
            
        Iterator iter1 = main.map.getTokenIterator();
        while (iter1.hasNext()) {
            String group = (String) iter1.next();

            // Base
            Element elem = stateDoc.createElement("location");
            root.insertBefore(elem, null);
            double[] pos = main.map.getTokenPos(group);
            elem.setAttribute("name", group);
            elem.setAttribute("x", Double.toString(pos[0]));
            elem.setAttribute("y", Double.toString(pos[1]));
            elem.setAttribute("angle", Double.toString(pos[2]));

            // Armour
            ArrayList al = (ArrayList) combatArmour.get(group);
            for (int i = 0; (al != null) && (i < al.size()); i++) {
                ArmourLine sect = (ArmourLine) al.get(i);

                elem = stateDoc.createElement("entry");
                root.insertBefore(elem, null);
                elem.setAttribute("name", group);
                elem.setAttribute("item", sect.item());
                elem.setAttribute("used", Boolean.toString(sect.used()));
                elem.setAttribute("material", sect.material());
                elem.setAttribute("quality", sect.quality());
            }

            // Weapons
            al = (ArrayList) combatWeapon.get(group);
            for (int i = 0; (al != null) && (i < al.size()); i++) {
                WeaponLine sect = (WeaponLine) al.get(i);

                elem = stateDoc.createElement("weapon");
                root.insertBefore(elem, null);
                elem.setAttribute("name", group);
                elem.setAttribute("item", sect.item());
                elem.setAttribute("used", Boolean.toString(sect.used()));
                elem.setAttribute("damage", sect.damage());
                elem.setAttribute("quality", sect.quality());
            }

            // Wounds
            al = (ArrayList) wounds.get(group);
            for (int i = 0; (al != null) && (i < al.size()); i++) {
                WoundLine sect = (WoundLine) al.get(i);

                elem = stateDoc.createElement("wound");
                root.insertBefore(elem, null);
                elem.setAttribute("name", group);
                elem.setAttribute("used", Boolean.toString(sect.used()));
                String[] fields = main.getData().getWoundFields();
                for (int j = 0; j < fields.length; j++)
                    elem.setAttribute
                        (fields[j], sect.getValue(null, fields[j]));
            }
        }

        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Result result = new StreamResult(file);
        Framework.transform(stateDoc, result, null);
            
        setDirty(false);
    }

    /**
     * Add a combatant.
     */
    public void newCombatant(String group) {
        if (group == null) {
            group = "New Combatant";
        }
        if (combatArmour.get(group) == null)
            combatArmour.put(group, new ArrayList());
        if (combatWeapon.get(group) == null)
            combatWeapon.put(group, new ArrayList());

        MyTreeModel model =
            (MyTreeModel) main.tree.getModel();
        DefaultMutableTreeNode root =
            (DefaultMutableTreeNode)main.tree.getModel().getRoot();
        boolean found = false;
        for (int i = 0; !found && i < root.getChildCount(); i++) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) root.getChildAt(i);
            found = node.getUserObject().equals(group);
        }
        if (!found)
            model.insertNodeSorted
                (new DefaultMutableTreeNode(group), root);
        main.map.addToken(group);
        setDirty(true);
    }

    /**
     * Clone a combatant.
     */
    public void cloneCombatant(String group) {
        int i = 1;
        String ngroup = group + " " + 1;
        while (combatArmour.get(ngroup) != null ||
               combatWeapon.get(ngroup) != null ||
               main.map.getTokenPos(ngroup) != null)
            ngroup = group + " " + ++i;

        // Clone Armour
        ArrayList nal = new ArrayList();
        ArrayList al = (ArrayList) combatArmour.get(group);
        for (i = 0; (al != null) && (i < al.size()); i++) {
            ArmourLine sect = (ArmourLine) al.get(i);
            nal.add
                (new ArmourLine
                 (main, sect.item(), sect.material(), sect.quality(),
                  sect.used()));
        }
        combatArmour.put(ngroup, nal);

        // Clone Weapons
        nal = new ArrayList();
        al = (ArrayList) combatWeapon.get(group);
        for (i = 0; (al != null) && (i < al.size()); i++) {
            WeaponLine sect = (WeaponLine) al.get(i);
            nal.add(new WeaponLine
                    (main, sect.item(), sect.damage(), sect.quality(),
                     sect.handmod(), sect.mlmod(), sect.skill,
                     Double.toString(sect.weight()), sect.used()));
        }
        combatWeapon.put(ngroup, nal);

        // Do not clone wounds

        MyTreeModel model =
            (MyTreeModel) main.tree.getModel();
        model.insertNodeSorted
            (new DefaultMutableTreeNode(ngroup),
             (DefaultMutableTreeNode)main.tree.getModel().getRoot());
        main.map.addToken(ngroup);
        setDirty(true);
    }

    /**
     * Remove a combatant.
     * @param group combatant to remove
     */
    public void removeCombatant(MutableTreeNode node) {
        ((DefaultTreeModel)main.tree.getModel()).removeNodeFromParent
            (node);
        Char c = (Char) chars.get(node.toString());
        combatArmour.remove(node.toString());
        main.map.removeToken(node.toString());
        if (c != null) c.makeCombatant(false);
        setDirty(true);
    }

    /**
     * Add a piece of armour.
     */
    public void newArmour(String group, boolean draw) {
        ArrayList al = (ArrayList) combatArmour.get(group);
        if (al == null) {
            al = new ArrayList();
            combatArmour.put(group, al);
        }

        ArmourLine sect = new ArmourLine
            (main, main.myProps.getProperty("default.item"),
             main.myProps.getProperty("default.material"), "0", false);
        al.add(sect);
        setDirty(true);

        if (draw)
            main.aitems.addItem(sect); // Includes revalidate & repaint
    }

    /**
     * Add a piece of weaponry.
     */
    public void newWeapon(String group, boolean draw) {
        ArrayList al = (ArrayList) combatWeapon.get(group);
        if (al == null) {
            al = new ArrayList();
            combatWeapon.put(group, al);
        }

        String name = main.myProps.getProperty("default.weapon");
        WeaponLine sect = new WeaponLine
            (main, name, main.getData().getWDamage(name),
             main.getData().getWQuality(name),
             main.getData().getWHandmod(name),
             main.getData().getWMlmod(name), "0",
             main.getData().getWWeight(name), false);
        al.add(sect);
        setDirty(true);

        if (draw)
            main.witems.addItem(sect); // Includes revalidate & repaint
    }

    /**
     * Add a wound.
     */
    public void newWound(String group, boolean draw) {
        ArrayList al = (ArrayList) wounds.get(group);
        if (al == null) {
            al = new ArrayList();
            wounds.put(group, al);
        }

        WoundLine sect = new WoundLine
            (main, group, new String[0], main.getData().getWoundFieldTypes(),
             main.getData().getWoundFieldChoices(),
             main.getData().getWoundFieldHelps(), false);
        al.add(sect);
        setDirty(true);

        if (draw)
            main.vitems.addItem(sect); // Includes revalidate & repaint
    }

    /**
     * Remove a line. Only issued from the shown character (group).
     */
    public void removeLine(String group, Line sect) {
        Hashtable ht = null;
        ItemList il = null;
        if (sect instanceof ArmourLine) {
            ht = combatArmour;
            il = main.aitems;
        }
        if (sect instanceof WeaponLine) {
            ht = combatWeapon;
            il = main.witems;
        }
        if (sect instanceof WoundLine) {
            ht = wounds;
            il = main.vitems;
        }

        ArrayList al = (ArrayList) ht.get(group);
        for (int i = 0; i < al.size(); i++)
            if (al.get(i).equals(sect)) {
                al.remove(i);
                break;
            }
        setDirty(true);
        il.removeItem(sect); // Includes revalidate & repaint
    }

    /**
     * Upadte all wounds to current date
     */
    public void updateWounds() {
        Iterator iter = wounds.keySet().iterator();
        while (iter.hasNext()) {
            ArrayList al = (ArrayList) wounds.get(iter.next());
            for (int i = 0; i < al.size(); i++)
                while (((WoundLine)al.get(i)).roll())
                    setDirty(true);
        }
    }

    /**
     * Return integer quality. Assumes correct format.
     */
    public static int intQ(String ret) {
        if (ret.startsWith("+"))
            return Integer.parseInt(ret.substring(1));
        else
            return Integer.parseInt(ret);
    }

    /**
     * This class provides an initiative insert to a default tree model
     * @author Michael Jung
     */
    public class MyTreeModel extends DefaultTreeModel {
        /** Default constructor */
        public MyTreeModel(TreeNode root) { super(root); }

        /** insert ordered */
        public void insertNodeSorted(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent) {
            Collator col = Collator.getInstance(Locale.FRANCE);
            for (int i = 0; i < parent.getChildCount(); i++) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) parent.getChildAt(i);

                String a = newChild.getUserObject().toString();
                Char ca = (Char) chars.get(a);
                if (ca != null && ca.getSkill("Initiative").matches("[0-9]+")) {
                    a = "00" + ca.getSkill("Initiative");
                    a = a.substring(a.length() - 3);
                }

                String b = node.getUserObject().toString();
                Char cb = (Char) chars.get(b);
                if (cb != null && cb.getSkill("Initiative").matches("[0-9]+")) {
                    b = "00" + cb.getSkill("Initiative");
                    b = b.substring(b.length() - 3);
                }

                if (col.compare(a,b) > 0) {
                    insertNodeInto(newChild, parent, i);
                    return;
                }
            }
            insertNodeInto(newChild, parent, parent.getChildCount());
        }
    }
}
