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
package harn.chars;

import harn.repository.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * State object.  It is updates through the GUI objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** Root reference */
    private Main main;

    /** State change from previous persistent state */
    private boolean dirty;

    /** current char name */
    private String current;

    /** character attribute/skill list */
    private Hashtable charlist;

    /** Statefiles already merged */
    private String merged;

    /** Time frame = Calendar */
    private TimeFrame cal;

    /** Active group */
    private CharGroup act;

    /** Current development option */
    private DevOption currentOpt;

    /** Current development listener */
    private DevActionListener currentDev;

    /** Equipment root */
    private Hashtable equipRoot;

    /** Spells */
    private HashSet spelllist;

    /** Shows whether editing (or just loading) */
    public static boolean edit;

    /** Depth of nested attribute parts */
    private static int DEPTH = 2;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
        main = aMain;
        charlist = new Hashtable();
        equipRoot = new Hashtable();
        spelllist = new HashSet();
        edit = true;
    }

    /**
     * Load the state file.
     */
    public void importHM() {
        JFileChooser choose = new JFileChooser();
        javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(".hmc");
                }
                public String getDescription() { return "HarnMaker files"; }
            };
        choose.setFileFilter(filter);
        choose.setMultiSelectionEnabled(false);
        int res = choose.showOpenDialog(main.myPanel);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File file = choose.getSelectedFile();
        Document doc = Framework.parse(file.getName(), null);
        DOMResult result = new DOMResult();
        Document xslt =
            Framework.parse(main.myPath + "harnmaker.xslt", null);
        Framework.transform(doc, result, xslt);
        loadDoc(((Document)result.getNode()).getDocumentElement(), main.chartree);
    }

    /**
     * Load the state file.
     */
    public void load() {
        Document stateDoc = Framework.parse(main.myPath + "state.xml", "chars");
        merged = "state.xml";
        loadDoc(stateDoc.getDocumentElement(), main.chartree);

        String[] charl = Framework.getStateFiles(main.myPath, merged);
        for (int i = 1; i < charl.length; i++) {
            stateDoc = Framework.parse(main.myPath + charl[i], "chars");
            loadDoc(stateDoc.getDocumentElement(), main.chartree);
        }
    }

    /**
     * Create a new character
     */
    public void create(Main.TreeNode tn) {
        String prefix = "New Character ";
        int i = 1;
        while (charlist.get(prefix + i) != null) i++;
        charlist.put(prefix + i, new MyCharacter(prefix + i));
        newChar(prefix + i, tn, true);
    }

    /**
     * Remove a character
     * @param name name of character
     */
    public void remove(String name) {
        MyCharacter c = (MyCharacter)charlist.get(name);
        c.valid = false;
        charlist.remove(name);
        main.getFramework().announce(c);
    }

    /**
     * Save a character as Harnmaker file.
     * @param name name of character
     */
    public void exportHM(String name) {
        try {
            JFileChooser choose = new JFileChooser();
            javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".hmc");
                    }
                    public String getDescription() { return "HarnMaker files"; }
                };
            choose.setFileFilter(filter);
            choose.setMultiSelectionEnabled(false);
            int res = choose.showOpenDialog(main.myPanel);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File file = choose.getSelectedFile();
            MyCharacter mychar = (MyCharacter)charlist.get(name);
            String out = export(mychar, "template.hmc");

            // Special attributes for Harnmaker
            out = out.replaceAll("\"Nuz\"", "\"1\"");
            out = out.replaceAll("\"Peo\"", "\"2\"");
            out = out.replaceAll("\"Kel\"", "\"3\"");
            out = out.replaceAll("\"Nol\"", "\"4\"");
            out = out.replaceAll("\"Lar\"", "\"5\"");
            out = out.replaceAll("\"Agr\"", "\"6\"");
            out = out.replaceAll("\"Azu\"", "\"7\"");
            out = out.replaceAll("\"Hal\"", "\"8\"");
            out = out.replaceAll("\"Sav\"", "\"9\"");
            out = out.replaceAll("\"Ilv\"", "\"10\"");
            out = out.replaceAll("\"Nav\"", "\"11\"");
            out = out.replaceAll("\"Mor\"", "\"12\"");
            out = out.replaceAll("\"Eldest\"", "\"1\"");
            out = out.replaceAll("\"2nd Child\"", "\"2\"");
            out = out.replaceAll("\"3rd Child\"", "\"3\"");
            out = out.replaceAll("\"4th Child\"", "\"4\"");
            out = out.replaceAll("\"5th Child\"", "\"5\"");
            out = out.replaceAll("\"6th Child\"", "\"6\"");

            // Special skill section for Harnmaker
            BufferedReader read = new BufferedReader(new StringReader(out));
            out = "";
            Pattern pat = Pattern.compile(".*attributes=\"(.*)\" sunsigns=\"(.*)\" oml=\"(.*)\">(.*)</skill>");
            String tmp;
            while ((tmp = read.readLine()) != null) {
                Matcher m = pat.matcher(tmp);
                if (m.matches()) {
                    String skill = m.group(4);
                    if (skill.equals("Language: Harnic")) skill = "Language/Jarind/H\u00e2rnic";
                    if (skill.equals("Language: Altish")) skill = "Language/Jarind/Altish";
                    if (skill.equals("Language: Emela")) skill = "Language/Jarind/Emela";
                    if (skill.equals("Language: Jarinese")) skill = "Language/Jarind/Jarinese";
                    if (skill.equals("Language: Old Jarinese")) skill = "Language/Jarind/Old Jarinese";
                    if (skill.equals("Language: Orbaalese")) skill = "Language/Jarind/Orbaal";
                    if (skill.equals("Language: Yarili")) skill = "Language/Jarind/Yarili";
                    if (skill.equals("Language: Harbaalese")) skill = "Language/Pharic/Harbaalese";
                    if (skill.equals("Language: Ivinian")) skill = "Language/Pharic/Ivinian";
                    if (skill.equals("Language: Palithanian")) skill = "Language/Pharic/Palithanian";
                    if (skill.equals("Language: Quarph")) skill = "Language/Pharic/Quarph";
                    if (skill.equals("Language: Trierzi")) skill = "Language/Pharic/Trierzi";
                    if (skill.equals("Language: Shorka")) skill = "Language/Pharic/Shorka";
                    if (skill.equals("Language: Azeri")) skill = "Language/Azeri/Azeri";
                    if (skill.equals("Language: Azeryani")) skill = "Language/Azeri/Azeryani";
                    if (skill.equals("Language: High Azeryani")) skill = "Language/Azeri/High Azeryani";
                    if (skill.equals("Language: Low Azeryani")) skill = "Language/Azeri/Low Azeryani";
                    if (skill.equals("Language: Urmech")) skill = "Language/Azeri/Urmech";
                    if (skill.equals("Language: Karejian")) skill = "Language/Azeri/Karejian";
                    if (skill.equals("Language: Byrian")) skill = "Language/Azeri/Byrian";
                    if (skill.equals("Language: Besha")) skill = "Language/Ketar/Besha";
                    if (skill.equals("Language: Karuia")) skill = "Language/Azeri/Karuia";
                    if (skill.equals("Axe") || skill.equals("Blowgun") || skill.equals("Bow") ||
                        skill.equals("Club") || skill.equals("Dagger") || skill.equals("Flail") ||
                        skill.equals("Net") || skill.equals("Polearm") || skill.equals("Shield") ||
                        skill.equals("Sling") || skill.equals("Spear") || skill.equals("Sword") ||
                        skill.equals("Whip"))
                        skill = "Weapon/" + skill;
                    if (skill.startsWith("Musician")) skill = skill.replaceAll(": ", "/");
                    if (skill.startsWith("Script")) skill = skill.replaceAll(": ", "/");
                    if (skill.startsWith("Ritual")) skill = skill.replaceAll(": ", "/");
                    if (skill.startsWith("Invocation")) skill = skill.replaceAll(": ", "/");

                    String val = mychar.getSkill(skill);
                    if (val == null) continue;
                    // Calc SB
                    String att[] = m.group(1).split(" ");
                    for (int i = 0; i < att.length; i++) {
                        if (att[i].equals("AGL")) att[i] = "Agility";
                        else if (att[i].equals("AUR")) att[i] = "Aura";
                        else if (att[i].equals("CML")) att[i] = "Comeliness";
                        else if (att[i].equals("DEX")) att[i] = "Dexterity";
                        else if (att[i].equals("EYE")) att[i] = "Eyesight";
                        else if (att[i].equals("HRG")) att[i] = "Hearing";
                        else if (att[i].equals("INT")) att[i] = "Intelligence";
                        else if (att[i].equals("SML")) att[i] = "Smell";
                        else if (att[i].equals("STA")) att[i] = "Stamina";
                        else if (att[i].equals("STR")) att[i] = "Strength";
                        else if (att[i].equals("VOI")) att[i] = "Voice";
                        else if (att[i].equals("WIL")) att[i] = "Will";
                    }
                    String sun[] = m.group(2).split("/");
                    int sb = CharSkill.calcSB(mychar, att, sun);

                    // OML
                    String term = m.group(3);
                    int oml = CharSkill.evalTerm(0, term, sb, null);
                    int ml = Integer.parseInt(val);

                    tmp = tmp.replaceAll
                        ("attributes=",
                         "op=\"" + (ml - oml)/sb + "\" sd=\"" + (ml - sb*((ml - oml)/sb) - oml) + "\" attributes=");
                    tmp = tmp.replaceAll("([0-9])xSB","SB$1"); 
                    tmp = tmp.replaceAll
                        ("sunsigns=\"(.*)\" oml","sunsigns=\"" + m.group(2).replaceAll("/",";") + "\" oml"); 
                }
                out += tmp + "\n";
            }
            FileWriter fw = new FileWriter(file);
            fw.write(out);
            fw.close();
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Add a Character.
     * @param name name of character
     */
    public void newChar(String name, Main.TreeNode tn, boolean scroll) {
        Main.TreeNode newNode = new Main.TreeNode(name, true);
        ((SortedTreeModel)main.guitree.getModel()).insertNodeSorted
            (newNode, tn);
        if (scroll)
            main.guitree.scrollPathToVisible(new TreePath(newNode.getPath()));
    }

    /**
     * Method to load the state.
     * @param doc (XML) document to load
     */
    private void loadDoc(Node doc, Main.TreeNode gui) {
        NodeList tree = doc.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("char")) {
                if (cattr != null) {
                    MyCharacter c = new MyCharacter(child, cattr);
                    charlist.put(c.cname, c);
                    newChar(c.cname, gui, false);
                }
            }
            if (child.getNodeName().equals("group")) {
                String group = cattr.getNamedItem("name").getNodeValue();
                Enumeration list = gui.children();
                Main.TreeNode newNode = null;
                while (list.hasMoreElements()) {
                    Main.TreeNode tmp = (Main.TreeNode)list.nextElement();
                    String key = (String) tmp.getUserObject();
                    if (key.equals(group)) newNode = tmp;
                }
                if (newNode == null) {
                    newNode = new Main.TreeNode(group, false);
                    ((SortedTreeModel)main.guitree.getModel()).insertNodeSorted
                        (newNode, gui);
                }
                loadDoc(child, newNode);
            }
            if (child.getNodeName().equals("merged") &&
                cattr.getNamedItem("list") != null) {
                merged = cattr.getNamedItem("list").getNodeValue();
            }
        }
    }

    /**
     * Clone a character.
     */
    public void clone(Main.TreeNode tn) {
        String prefix = tn.toString() + " ";
        int i = 1;
        while (charlist.get(prefix + i) != null) i++;
        charlist.put
            (prefix + i,
             ((MyCharacter)charlist.get(tn.toString())).clone(prefix + i));
        newChar(prefix + i, (Main.TreeNode)tn.getParent(), true);
    }

    /**
     * Getter for calendar.
     * @return calendar
     */
    public TimeFrame getCal() { return cal; }

    /**
     * Getter for active group.
     * @return active group
     */
    public CharGroup getAct() { return act; }

    /**
     * Method to be informed about calendar events and active group.
     * @param e calendar being announced
     */
    public void inform(rpg.IExport e) {
        if (e instanceof TimeFrame)
            cal = (TimeFrame) e;
        else if (e instanceof CharGroup) {
            if (((CharGroup)e).isActive())
                act = (CharGroup) e;
        }
        else if (e instanceof Spell) {
            if (((Spell)e).isValid()) {
                spelllist.add(e);
            }
            else {
                spelllist.remove(e);
            }
        }
        else { // Equipment
            if (((Equipment)e).getFather() == null)
                equipRoot.put(e.getName(), e);
        }
    }

    /**
     * Set the current development option.
     * @param opt new development option
     */
    public void setCurrentOpt(DevOption opt) { currentOpt = opt; }

    /**
     * Set the current development option.
     * @return the current development option
     */
    public DevOption getCurrentOpt() { return currentOpt; }

    /**
     * Get current development listener.
     * @return current development action listener
     */
    public DevActionListener getCurrentDev() { return currentDev; }

    /**
     * Set current development listener.
     * @param value new development action listener
     */
    public void setCurrentDev(DevActionListener value) { currentDev = value; }

    /**
     * Propagate the enabling of a new button.
     */
    public void enableNextButton() { main.enableNextButton(); }

    /**
     * Get an attribute value.
     * @param name name of attribute
     * @return value of attribute
     */
    public String getAttribute(String name) {
        if (charlist == null || current == null) return null;
        return (String) ((MyCharacter)charlist.get(current)).getAttribute(name);
    }

    /**
     * Get the current character.
     */
    public MyCharacter getCurrent() {
        if (current == null) return null;
        return (MyCharacter)charlist.get(current);
    }

    /**
     * Get the code (PIN).
     * @param name name of char
     * @return value of code
     */
    public String getCode(String name) {
        if (charlist == null) return null;
        return (String) ((MyCharacter)charlist.get(name)).getCode();
    }

    /**
     * Get a skill value.
     * @param name name of skill
     * @return value of skill
     */
    public String getSkill(String name) {
        if (charlist == null || current == null) return null;
        return (String) ((MyCharacter)charlist.get(current)).getSkill(name);
    }

    /**
     * Get a note value.
     * @param name name of note
     * @return value of note
     */
    public String getNote(String name) {
        if (charlist == null || current == null) return null;
        return (String) ((MyCharacter)charlist.get(current)).getNote(name);
    }

    /**
     * Get a (preliminary) skill value.
     * @param name name of skill
     * @return value of skill
     */
    public String getPrelimSkill(String name) {
        return (String) ((MyCharacter)charlist.get(current)).getPrelimSkill(name);
    }

    /**
     * Get all attributes.
     * @return list of attributes (keys)
     */
    public Set getAttributes(String name) {
        return ((MyCharacter)charlist.get(name)).attrKeySet();
    }

    /**
     * Get all Notes.
     * @return list of notes (keys)
     */
    public Set getNotes(String name) {
        return ((MyCharacter)charlist.get(name)).notesKeySet();
    }

    /**
     * Get heraldry.
     * @return heraldic file name
     */
    public String getHeraldry(String name) {
        return ((MyCharacter)charlist.get(name)).heraldry;
    }

    /**
     * Get all skills.
     * @return list of skills (keys)
     */
    public Set getSkills(String name) {
        return ((MyCharacter)charlist.get(name)).skillKeySet();
    }

    /**
     * Set name. Only the current is selected.
     */
    public void setName(String aName) {
        MyCharacter c = (MyCharacter)charlist.get(current);
        // Void old
        c.valid = false;
        main.getFramework().announce(c);

        // Announce over
        c.valid = true;
        c.cname = aName;
        charlist.remove(current);
        current = aName;
        charlist.put(current, c);
        main.getFramework().announce(c);
    }

    /**
     * Set an attribute value.
     * @param name name of attribute
     * @param value value of attribute
     */
    public void setAttribute(String name, String value) {
        if (current != null && value != null)
            ((MyCharacter)charlist.get(current)).putAttribute(name, value);
    }

    /**
     * Set an note value.
     * @param name name of note
     * @param value value of note
     */
    public void setNote(String name, String value) {
        if (current != null && value != null && value.length() != 0)
            ((MyCharacter)charlist.get(current)).putNote(name, value);
    }

    /**
     * Set the heraldry
     * @param value value of heraldry
     */
    public void setHeraldry(String value) {
        if (current != null && value != null && value.length() != 0)
            ((MyCharacter)charlist.get(current)).heraldry = value;
        if (current != null)
            main.getFramework().announce((MyCharacter)charlist.get(current));
    }

    /**
     * Set a skill value.
     * @param name name of skill
     * @param value value of skill
     */
    public void setSkill(String name, String value) {
        if (current != null && value != null)
            ((MyCharacter)charlist.get(current)).putSkill(name, value, true);
    }

    /**
     * Set a (preliminary) skill value.
     * @param name name of skill
     * @param value value of skill
     */
    public void setPrelimSkill(String name, String value) {
        if (current != null && value != null && value.length() != 0)
            ((MyCharacter)charlist.get(current)).putPrelimSkill(name, value);
    }

    /**
     * Clear all attributes and heraldic.
     */
    public void clearAttributes() {
        ((MyCharacter)charlist.get(current)).cleanAttributes();
    }

    /**
     * Clear all skills.
     */
    public void clearSkills() {
        ((MyCharacter)charlist.get(current)).cleanSkills();
    }

    /**
     * Enable or disable announcement.
     * @param enable set true if announcement should take place
     */
    public void enableAnnounce(boolean enable) {
        if (current == null) return;
        edit = enable;
        ((MyCharacter)charlist.get(current)).enableAnnounce(enable);
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * is accessible by this method.
     * @return dirty flag
     */
    public boolean getDirty() { return dirty; }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled.
     * @return dirty flag
     */
    public void setDirty(boolean flag) {
	dirty = flag;
	if (main.bSave != null)
	    main.bSave.setEnabled(dirty);
    }

    /**
     * Use this method to save the data in this object to file. After saving,
     * this object is "clean" once more.
     */
    public void save(Main.TreeNode node, Node root, Document stateDoc) {
            if (root == null) {
                // Create technical objects
                stateDoc = Framework.newDoc();

                // Root
                root = stateDoc.createElement("chars");
                stateDoc.insertBefore(root, stateDoc.getLastChild());

                Framework.setMergedStates(stateDoc, main.myPath, merged);

                // Save tree
                save(node, root, stateDoc);

                // Write the document to the file
                File file = new File(main.myPath + "state.xml");
                Framework.backup(main.myPath + "state.xml");
                Framework.transform(stateDoc, new StreamResult(file), null);

                // Clean
                setDirty(false);
                return;
            }

            Enumeration list = node.children();
            while (list.hasMoreElements()) {
                Main.TreeNode child = (Main.TreeNode) list.nextElement();

                if (!child.isLeaf()) {
                    Element elem = stateDoc.createElement("group");
                    elem.setAttribute("name", child.toString());
                    root.insertBefore(elem, null);

                    save(child, elem, stateDoc);
                }
                else {
                    String ckey = child.toString();
                    MyCharacter mychar = (MyCharacter) charlist.get(ckey);

                    // Character element
                    Element elem = stateDoc.createElement("char");
                    root.insertBefore(elem, null);
                    elem.setAttribute("name", mychar.cname);
                    elem.setAttribute("code", mychar.code);

                    // Attributes
                    Iterator iter1 = mychar.attrKeySet().iterator();
                    while (iter1.hasNext()) {
                        String akey = (String) iter1.next();
                        if (mychar.getAttribute(akey) == null || mychar.getAttribute(akey).length() == 0)
                            continue;

                        // Attribute element
                        Element attrelem = stateDoc.createElement("attribute");
                        elem.insertBefore(attrelem, null);
                        attrelem.setAttribute("name", akey);
                        attrelem.setAttribute
                            ("value", mychar.getAttribute(akey));
                    }

                    // Skills
                    iter1 = mychar.skillKeySet().iterator();
                    while (iter1.hasNext()) {
                        String akey = (String) iter1.next();
                        if (mychar.getSkill(akey) == null || mychar.getSkill(akey).length() == 0)
                            continue;

                        // Skill element
                        Element skillelem = stateDoc.createElement("skill");
                        elem.insertBefore(skillelem, null);
                        skillelem.setAttribute("name", akey);
                        skillelem.setAttribute("value", mychar.getSkill(akey));
                    }

                    if (mychar.heraldry != null) {
                        // Heraldry element
                        Element heraldelem =
                            stateDoc.createElement("heraldry");
                        elem.insertBefore(heraldelem, null);
                        heraldelem.setAttribute("value", mychar.heraldry);
                    }

                    // Notes
                    iter1 = mychar.notesKeySet().iterator();
                    while (iter1.hasNext()) {
                        String akey = (String) iter1.next();

                        // Skill element
                        Element noteelem = stateDoc.createElement("note");
                        elem.insertBefore(noteelem, null);
                        noteelem.setAttribute("name", akey);
                        noteelem.setAttribute("value", mychar.getNote(akey));
                    }
                }
            }
    }

    /**
     * Turn the character into combatant.
     * @param name name of character
     */
    public void makeCombatant(String name) {
        MyCharacter c = (MyCharacter)charlist.get(name);
        c.makeCombatant(true);
        main.getFramework().announce(c);
    }

    /**
     * Select the character with the given name.
     * @param name name of character
     */
    public void select(String name) {
        if (!name.equals(current)) {
            current = name;
            // Show attributes/skills
            enableAnnounce(false);
            if (main.getData() != null)
                main.getData().load(getAttributes(name), getHeraldry(name));
            enableAnnounce(true);

            // Show export code
            main.bCode.setText
                ("Code: " + ((MyCharacter)charlist.get(name)).code);

            String txt = exportMini();
            main.getRODisplay().setText(txt);
        }
    }


    /**
     * Export the current character to Mini-HTML
     */
    public String exportMini() {
        MyCharacter mychar = (MyCharacter)charlist.get(current);
        
        try {
            return export(mychar, "minitemplate.html");
        }
        catch(IOException e) {
            // Debugging
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Export the current character to HTML.
     */
    public void export(String cname) {
        MyCharacter mychar = (MyCharacter)charlist.get(cname);
        
        try {
            String out = export(mychar, "template.html");

            // Output
            String outf = mychar.code + ".html";
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
     * Export basis.
     */
    private String export(MyCharacter mychar, String intemplate) throws IOException {
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
        Iterator iter = getAttributes(mychar.getName()).iterator();
        while (iter.hasNext()) {
            String attr = (String) iter.next();
            for (int i = 0; i < DEPTH; i++) {
                out = out.replaceAll
                    ("\\$" + attr + "-" + i,
                     mychar.getAttribute(attr + "-" + i));
                out = out.replaceAll
                    ("\\$" + attr + "\\+" + i,
                     mychar.getAttribute(attr + "+" + i));
            }
            out = out.replaceAll
                ("\\$" + attr, mychar.getAttribute(attr));
        }
            
        // Replace skills
        TreeSet keys = new TreeSet(Collator.getInstance(Locale.FRANCE));
        keys.addAll(getSkills(mychar.getName()));
        iter = keys.iterator();
        Hashtable idx = new Hashtable();
        while (iter.hasNext()) {
            String skill = (String) iter.next();
            String val = mychar.getSkill(skill);
            if (val == null || val.length() == 0) continue;

            // Get skill hierarchy
            String group = null;
            String intern = skill;

            // group = prefix, intern = postfix
            if (skill.indexOf("/") > -1) {
                group = skill.substring(0,skill.indexOf("/"));
                intern = skill.substring
                    (skill.lastIndexOf("/")+1,skill.length());
            }

            if (intern.indexOf("#") > -1)
                intern = intern.replaceFirst("#","/");

            // Take section name, if group cannot be found in template
            if (group == null || template.indexOf(group) == -1) {
                group = main.getData().getSection(skill);
            }

            // Find the group to count internally
            Matcher m = Pattern.compile
                ("\\$Sk[a-zA-Z -/]*" + group + "[a-zA-Z -/]*-").matcher(template);
            if (m.find()) {
                group = m.group();
                // Get internal numbering
                Integer i = (Integer) idx.get(group);
                if (i == null) i = new Integer(1);

                // Replace
                out = out.replaceAll
                    ("\\" + group + i.intValue() + "N", intern);
                out = out.replaceAll
                    ("\\" + group + i.intValue() + "V", val);

                idx.put(group, new Integer(i.intValue() + 1));
            }
            else {
                // Find general skills
                m = Pattern.compile("\\$Sk-").matcher(template);

                if (m.find()) {
                    // Get internal numbering
                    Integer i = (Integer) idx.get("Sk");
                    if (i == null) i = new Integer(1);

                    // Replace
                    out = out.replaceAll
                        ("\\$Sk-" + i.intValue() + "N", intern);
                    out = out.replaceAll
                        ("\\$Sk-" + i.intValue() + "V", getSkill(skill));

                    idx.put("Sk", new Integer(i.intValue() + 1));
                }
            }
        }

        // Equipment
        String equip = getEquipment(mychar.getName());
        if (equip != null) {
            out = out.replaceAll("\\$Equipment", equip);
        }

        // Heraldry
        if (mychar.heraldry != null)
            out = out.replaceAll("\\$Heraldry", getHeraldry(mychar.getName()));
            
        // Replace Spells
        int i = 0;
        iter = spelllist.iterator();
        while (iter.hasNext()) {
            Spell spell = (Spell) iter.next();
            if (spell.getOwner() != mychar) continue;

            // Replace
            out = out.replaceAll
                ("\\$Spell-" + i + "N", spell.getName());
            out = out.replaceAll
                ("\\$Spell-" + i + "V", Integer.toString(spell.getML()));
            i++;
            
        }

        // Replace Notes
        iter = getNotes(mychar.getName()).iterator();
        while (iter.hasNext()) {
            String note = (String) iter.next();
            String val = getNote(note);
            if (out.indexOf("$" + note + ".html") > -1) {
                File f = new File(main.myPath + Framework.osName(".." + val + ".html"));
                if (f.exists()) {
                    BufferedReader lbr = new BufferedReader(new FileReader(f));
                    StringBuffer lbuf = new StringBuffer();
                    String lstr;
                    while ((lstr = lbr.readLine()) != null)
                        lbuf.append(lstr).append("\n");
                    out = out.replaceAll
                        ("\\$" + note + ".html", lbuf.toString());
                }
            }
            if (out.indexOf("$" + note + ".png") > -1) {
                File f = new File(main.myPath + Framework.osName(".." + val + ".png"));
                if (f.exists()) {
                    out = out.replaceAll
                        ("\\$" + note + ".png",
                         "<img src=\"" + val.substring(1) + ".png\">");
                }
            }
            if (out.indexOf("$" + note + ".txt") > -1) {
                File f = new File(main.myPath + Framework.osName(".." + val + ".txt"));
                if (f.exists()) {
                    BufferedReader lbr = new BufferedReader(new FileReader(f));
                    StringBuffer lbuf = new StringBuffer();
                    String lstr;
                    while ((lstr = lbr.readLine()) != null)
                        lbuf.append(lstr).append("\n");
                    out = out.replaceAll
                        ("\\$" + note + ".txt", lbuf.toString());
                }
            }
        }
        // Clean out
        return out.replaceAll("\\$[a-zA-Z0-9/,. -]*", "");
    }

    /**
     * Add equipment.
     * @param cname character name
     * @param equip equipment name
     */
    public void addEquipment(String cname, String equip) {
        Equipment root = (Equipment) equipRoot.get(cname);
        if (root == null) return;
        root.addChild(equip);
    }

    /**
     * Get the equipment for named character, if available
     */
    private String getEquipment(String name) {
        Equipment root = (Equipment) equipRoot.get(name);
        if (root == null) return null;

        return getEquipment(root);
    }

    /**
     * Get the sub equipment for equipment.
     * @param base equipment
     * @return stringified subequipment
     */
    private String getEquipment(Equipment base) {
        Equipment[] list = base.getChildren();
        if (list == null) return "";

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < list.length; i++) {
            buf.append(", ").append(list[i].getName());
            Equipment[] children = list[i].getChildren();
            if (children != null && children.length != 0) {
                buf.append(": ").append(getEquipment(list[i]));
            }
        }
        String ret = buf.toString();
        if (ret.length() > 2) return ret.substring(2);
        return "";
    }

    /**
     * Class to hold Character attributes and skills.
     */
    class MyCharacter extends Char {
        /** attribute list */
        Hashtable attrs;
        /** skill list */
        Hashtable skills;
        /** notes list */
        Hashtable notes;
        /** prelim skill list */
        Hashtable prels;
        /** name of char */
        String cname;
        /** Code */
        String code;
        /** Heraldry */
        String heraldry;
        /** Announce changes */
        boolean announce;
        /** not garbage collected? */
        boolean valid;
        /** is combatant */
        boolean isCombat;
        /**
         * Constructor. Used for immediate creation.
         * @param aName character's name
         */
        MyCharacter(String aName) {
            attrs = new Hashtable();
            skills = new Hashtable();
            prels = new Hashtable();
            notes = new Hashtable();
            cname = aName;
            code = Integer.toString(9999 + DiceRoller.d(10000)).substring(1,5);

            announce = true;
            valid = true;
            isCombat = false;
            main.getFramework().announce(this);
        }

        /**
         * Constructor. Used when loading from file.
         * @param node node with children
         * @param attr attributes already determined
         */
        MyCharacter(Node node, NamedNodeMap attr) {
            attrs = new Hashtable();
            skills = new Hashtable();
            prels = new Hashtable();
            notes = new Hashtable();
            cname = attr.getNamedItem("name").getNodeValue();
            if (attr.getNamedItem("code") != null)
                code = attr.getNamedItem("code").getNodeValue();
            else // Harnmaker import
                code =
                    Integer.toString(9999 + DiceRoller.d(10000)).substring(1,5);

            NodeList tree = node.getChildNodes();

            announce = false;
            valid = true;
            for (int i = 0; i < tree.getLength(); i++) {
                Node child = tree.item(i);
                NamedNodeMap cattr = child.getAttributes();
                if (cattr != null) {
                    Hashtable ref = null;
                    if (child.getNodeName().equals("attribute"))
                        attrs.put
                            (cattr.getNamedItem("name").getNodeValue(),
                             cattr.getNamedItem("value").getNodeValue());
                    if (child.getNodeName().equals("skill")) {
                        if (cattr.getNamedItem("value") != null) {
                            putSkill
                                (cattr.getNamedItem("name").getNodeValue(),
                                 cattr.getNamedItem("value").getNodeValue(), false);
                        }
                        else { // Harnmaker import
                            putSkill
                                (cattr.getNamedItem("name").getNodeValue(),
                                 getHMSkill(cattr), false);
                        }
                    }
                    if (child.getNodeName().equals("heraldry"))
                        heraldry = cattr.getNamedItem("value").getNodeValue();
                    if (child.getNodeName().equals("note"))
                        notes.put
                            (cattr.getNamedItem("name").getNodeValue(),
                             cattr.getNamedItem("value").getNodeValue());
                }
            }

            announce = true;
            main.getFramework().announce(this);
        }

        /**
         * Clone this character
         */
        public Object clone(String name) {
            MyCharacter ret = new MyCharacter
                (name);
            ret.heraldry = heraldry;
            Iterator iter = attrs.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                ret.attrs.put(key, attrs.get(key));
            }
            iter = skills.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                ret.skills.put(key, skills.get(key));
            }
            iter = notes.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                ret.notes.put(key, notes.get(key));
            }
            return ret;
        }

        /**
         * Get and calculate HarnMaker values.
         */
        private String getHMSkill(NamedNodeMap cattr) {
            // Get SB
            CharSkill sk = main.getData().getSkill
                (cattr.getNamedItem("name").getNodeValue());
            if (sk == null) return "";
            int sb = sk.calcSB(this);

            // SB Factor
            int sbf = Integer.parseInt
                (cattr.getNamedItem("sb").getNodeValue().substring(2));
            int sbadd = Integer.parseInt
                (cattr.getNamedItem("sbadd").getNodeValue());

            int add = Integer.parseInt
                (cattr.getNamedItem("add").getNodeValue());
            return Integer.toString
                (CharSkill.evalTerm
                 (0, add + "+" + (sbf+sbadd) + "xSB", sb, null));
        }
        /**
         * Enable or disable announcement.
         * @param enable set true if announcement should take place
         */
        public void enableAnnounce(boolean enable) {
            if (enable && enable != announce) {
                announce = enable;
                main.getFramework().announce(this);
            }
            announce = enable;
        }
        /**
         * Getter for attribute values.
         * @param name of attribute
         * @return value of attribute
         */
        public String getAttribute(String name) {
            String truename = name.substring(0, name.length() - 2);
            for (int i = 0; i < DEPTH; i++) {
                int n = name.indexOf("-" + i);
                if (n < 0) continue;
                String[] ret = ((String) attrs.get(truename)).split("[/-]");
                if (ret.length - 1 - i < 0) return null;
                return ret[ret.length - 1 - i];
            }
            for (int i = 0; i < DEPTH; i++) {
                int n = name.indexOf("+" + i);
                if (n < 0) continue;
                String[] ret = ((String) attrs.get(truename)).split("[/-]");
                if (ret.length <= i) return null;
                return ret[i];
            }
            return (String) attrs.get(name);
        }
        /**
         * Setter for attributes.
         * @param name of attribute
         * @param val of attribute
         */
        public void putAttribute(String name, String val) {
            attrs.put(name, val);
            if (announce) main.getFramework().announce(this);
        }
        /**
         * Getter for note values
         * @param name of note
         * @return value of note
         */
        public String getNote(String name) { return (String)notes.get(name); }
        /**
         * Setter for attributes.
         * @param name of attribute
         * @param val of attribute
         */
        public void putNote(String name, String val) {
            notes.put(name, val);
            if (announce) main.getFramework().announce(this);
        }
        /**
         * Getter for skill values
         * @param name of skill
         * @return value of skill
         */
        public String getSkill(String name) { return (String) skills.get(name); }
        /**
         * Setter for skills.
         * @param name of skill
         * @param value of skill
         */
        public void putSkill(String name, String val, boolean lannounce) {
            skills.put(name, val);
            String[] sk_sp = name.split("#");
            if (sk_sp.length > 1 && getSkill(sk_sp[0]) == null) {
                String imply = CharSkill.getImply(name);
                if (imply == null) imply = "0";
                skills.put(sk_sp[0], imply);
            }
            if (lannounce && announce) main.getFramework().announce(this);
        }
        /**
         * Getter for prelim skills
         * @param name of skill
         * @return value of skill
         */
        public String getPrelimSkill(String nam) { return (String)prels.get(nam); }
        /**
         * Setter for prelim skills
         * @param name of skill
         * @param value of skill
         */
        public void putPrelimSkill(String nam, String val) { prels.put(nam, val); }
        /**
         * Clean attributes and heraldic.
         */
        public void cleanAttributes() {
            attrs = new Hashtable();
            heraldry = null;
        }
        /**
         * Clean skills.
         */
        public void cleanSkills() {
            skills = new Hashtable();
            prels = new Hashtable();
        }
        /**
         * Keyset for skills.
         * @return keyset for skills
         */
        public Set skillKeySet() { return skills.keySet(); }
        /**
         * Keyset for attributes.
         * @return keyset for attributes
         */
        public Set attrKeySet() { return attrs.keySet(); }
        /**
         * Keyset for notes.
         * @return keyset for notes
         */
        public Set notesKeySet() { return notes.keySet(); }
        /**
         * Implement as specified in superclass.
         * @return name attribute
         */
        public String getName() { return cname; }
        /**
         * This method provides something of a PIN for a character. This should be
         * provided by external agents when accessing this character.
         * @return the PIN code
         */
        public String getCode() { return code; }
        /**
         * Get the attribute names list.
         * @return attribute names list
         */
        public String[] getAttributeList() {
            String[] ret = new String[attrs.keySet().size()];
            Object[] objs = attrs.keySet().toArray();
            for (int i = 0; i < ret.length; i++)
                ret[i] = (String) objs[i];
            return ret;
        }
        /**
         * Get the skill names list.
         * @return skill names list
         */
        public String[] getSkillList() {
            String[] ret = new String[skills.keySet().size()];
            Object[] objs = skills.keySet().toArray();
            for (int i = 0; i < ret.length; i++)
                ret[i] = (String) objs[i];
            return ret;
        }
        /**
         * Get the heraldry icon
         * @return heraldry icon
         */
        public ImageIcon getHeraldry() {
            if (heraldry != null)
                return new ImageIcon(main.myPath + heraldry);
            else
                return null;
        }
        /**
         * Return the full character HTML page as string.
         * @return HTML page
         */
        public File getPage() {
            return new File(main.myPath + code + ".html");
        }
        /**
         * Several locations may be valid at a given time. Returns the state
         * of this location. A valid location is a location that the plugin
         * knows about.  In abstract terms, an invalid object was released to
         * garbage collection by the implementing plugin.
         * @return true if a valid location
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Set the character active. The meaning of this is dependant on
         * plugin. It may mean that the caharcter is shown on a GUI.
         */
        public void setActive() {
            Enumeration all =
                ((DefaultMutableTreeNode)main.guitree.getModel().getRoot()).depthFirstEnumeration();
            while (all.hasMoreElements()) {
                Object o = all.nextElement();
                if (o instanceof Main.TreeNode &&
                    ((Main.TreeNode)o).isLeaf() &&
                    ((Main.TreeNode)o).getUserObject().equals(cname)) {
                    TreePath tp = new TreePath(((Main.TreeNode)o).getPath());
                    main.guitree.setSelectionPath(tp);
                    main.myFrw.raisePlugin("Chars");
                    return;
                }
            }
        }

        /**
         * Is the character a combatant?
         * @return combatant status
         */
        public boolean isCombatant() { return isCombat; }

        /**
         * Make the character a combatant or remove him from list. Do not
         * announce.
         * @param status combatant status
         */
        public void makeCombatant(boolean status) { isCombat = status; }
    }
}
