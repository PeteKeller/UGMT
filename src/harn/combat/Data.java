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

import java.util.*;
import java.util.regex.*;
import rpg.Framework;
import java.awt.*;
import javax.swing.*;
import org.w3c.dom.*;

/**
 * Data object. Here we collect all our data. This also controls the GUI.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** Data document */
    private Document dataDoc;

    /** Rules document */
    private Document rulesDoc;

    /** Protective values */
    private Node protect;

    /** Norm used */
    private String norm;

    /** List of row names */
    private LinkedList rowNames;

    /** List of possible materials */
    private ArrayList matNames;

    /** List of possible armour items */
    private ArrayList armourNames;

    /** List of possible weapon items */
    private Hashtable weapons;

    /** Pattern for armour equipment matching */
    private Pattern[] armourPattern;

    /** Pattern for weapon equipment matching */
    private Pattern[] weaponPattern;

    /** Wound pattern */
    private ArrayList woundPattern;

    /** Healing rules */
    public ArrayList rules;

    /** Qualities */
    public static String[] qualities =
        new String[] { "-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4" };

    /**
     * Constructor.
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;

        try {
            // Rules
            dataDoc = Framework.parse(main.myPath + "data.xml", "combat");
            rulesDoc = Framework.parse(main.myPath + "rules.xml", "rules");

            // Prepare some things
            columnNames();
            prepRegexp();
            getWeapons();
            loadRules();
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /** Get norm (from ArmourAnalysis.java) */
    public String getNorm() { return norm; }

    /**
     * Get the table model columns.
     */
    public String[] columnNames() {
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    protect = tree.item(i);
            if (protect.getNodeName().equals("protect")) {
                NamedNodeMap cattr = protect.getAttributes();
                return cattr.getNamedItem("types").getNodeValue().split("/");
            }
        }
        return null;
    }

    /**
     * Load the rules.
     */
    private void loadRules() {
        woundPattern = new ArrayList();
        rules = new ArrayList();
        NodeList tree = rulesDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("fields")) {
                NodeList subtree = child.getChildNodes();
                for (int j = 0; j < subtree.getLength(); j++) {
                    child = subtree.item(j);
                    if (child.getNodeName().equals("field")) {
                        NamedNodeMap att = child.getAttributes();
                        String name = att.getNamedItem("name").getNodeValue();
                        String help = null;
                        if (att.getNamedItem("help") != null)
                            help = att.getNamedItem("help").getNodeValue();
                        String[] choices = null;
                        Class clas = JTextField.class;
                        if (att.getNamedItem("choice") != null) {
                            choices = att.getNamedItem
                                ("choice").getNodeValue().split(",");
                            clas = JComboBox.class;
                        }
                        woundPattern.add
                            (new Object[] { name, help, choices, clas });
                    }
                }
            }
            // Proper rules
            if (child.getNodeName().equals("roll")) {
                NamedNodeMap att = child.getAttributes();
                rules.add(att);
            }
        }
    }

    /**
     * Get wound field names.
     */
    public String[] getWoundFields() {
        String[] ret = new String[woundPattern.size()];
        for (int i = 0; i < woundPattern.size(); i++)
            ret[i] = (String)((Object[])woundPattern.get(i))[0];
        return ret;
    }

    /**
     * Get wound field help text.
     */
    public String[] getWoundFieldHelps() {
        String[] ret = new String[woundPattern.size()];
        for (int i = 0; i < woundPattern.size(); i++)
            ret[i] = (String)((Object[])woundPattern.get(i))[1];
        return ret;
    }

    /**
     * Get wound field types.
     */
    public Class[] getWoundFieldTypes() {
        Class[] ret = new Class[woundPattern.size()];
        for (int i = 0; i < woundPattern.size(); i++)
            ret[i] = (Class)((Object[])woundPattern.get(i))[3];
        return ret;
    }

    /**
     * Get wound field choices.
     */
    public String[][] getWoundFieldChoices() {
        String[][] ret = new String[woundPattern.size()][];
        for (int i = 0; i < woundPattern.size(); i++)
            ret[i] = (String[])((Object[])woundPattern.get(i))[2];
        return ret;
    }

    /**
     * Get the table model rows.
     */
    public LinkedList rowNames() {
        if (rowNames != null) return (LinkedList)rowNames.clone();
        rowNames = new LinkedList();
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("areas")) {
                NodeList subtree = child.getChildNodes();
                for (int j = 0; j < subtree.getLength(); j++) {
                    child = subtree.item(j);
                    if (child.getNodeName().equals("group")) {
                        NamedNodeMap cattr = child.getAttributes();
                        String[] tmp = cattr.getNamedItem("names").getNodeValue().split(",");
                        for (int k = 0; k < tmp.length; k++)
                            rowNames.add(tmp[k]);
                        rowNames.add("-");
                    }
                }
                break;
            }
        }
        return rowNames;
    }

    /**
     * Get Materials.
     */
    public ArrayList getMaterials() {
        if (matNames != null) return matNames;
        matNames = new ArrayList();

        // Get names
        NodeList tree = protect.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("material"))
                matNames.add(child.getAttributes().getNamedItem("name").getNodeValue());
        }

        return matNames;
    }

    /**
     * Get Weight factor.
     */
    public double getWeightFactor(String material) {
        // Get names
        NodeList tree = protect.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);

            if (child.getNodeName().equals("material")) {
                NamedNodeMap cattr = child.getAttributes();
                // Base name
                boolean found =
                    cattr.getNamedItem("name").getNodeValue().equals(material);
                // Alternate names
                if (!found && cattr.getNamedItem("alt") != null) {
                    String[] tmp =
                        cattr.getNamedItem("alt").getNodeValue().split("\\b,\\b");
                    for (int k = 0; k < tmp.length; k++)
                        found |= (tmp[k].equals(material));
                }
                if (found)
                    return Double.parseDouble
                        (child.getAttributes().getNamedItem("weight").getNodeValue());
            }
        }
        return 0;
    }

    /**
     * Get Items and potentially a norm.
     */
    public ArrayList getArmours() {
        if (armourNames != null) return armourNames;
        armourNames = new ArrayList();

        // Get values
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("armour"))
                armourNames.add(child.getAttributes().getNamedItem("name").getNodeValue());
            if (child.getNodeName().equals("norm")) {
                norm =
                    child.getAttributes().getNamedItem("value").getNodeValue();
            }
        }

        return armourNames;
    }

    /**
     * Get Items.
     */
    public ArrayList getWeapons() {
        if (weapons != null) return new ArrayList(weapons.keySet());
        weapons = new Hashtable();

        // Get values
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("weapon")) {
                NamedNodeMap nm = child.getAttributes();
                String[] weapon = new String[] { nm.getNamedItem("name").getNodeValue() };
                if (nm.getNamedItem("alt") != null) {
                    weapon =
                        (weapon[0] + "," + nm.getNamedItem("alt").getNodeValue()).split(",");
                }
                for (int j = 0; j < weapon.length; j++) {
                    String item = weapon[j];
                    String damage = nm.getNamedItem("value").getNodeValue();
                    String quality = nm.getNamedItem("quality").getNodeValue();
                    String mlmod = nm.getNamedItem("mlmod").getNodeValue();
                    String skill = nm.getNamedItem("skill").getNodeValue();
                    Node n = nm.getNamedItem("handmod");
                    String handmod = (n != null ? n.getNodeValue() : "-");
                    n = nm.getNamedItem("weight");
                    String weight = (n != null ? n.getNodeValue() : "-");

                    weapons.put(item, new String[] {
                        damage, quality, handmod, mlmod, skill, weight
                    });
                }
            }
        }

        return new ArrayList(weapons.keySet());
    }

    /**
     * Return weapon damage.
     */
    public String getWDamage(String item) { return ((String[])weapons.get(item))[0]; }

    /**
     * Return weapon weight.
     */
    public String getWWeight(String item) { return ((String[])weapons.get(item))[5]; }

    /**
     * Return weapon quality.
     */
    public String getWQuality(String item) { return ((String[])weapons.get(item))[1]; }

    /**
     * Return weapon handmodifier.
     */
    public String getWHandmod(String item) { return ((String[])weapons.get(item))[2]; }

    /**
     * Return weapon ML modifier.
     */
    public String getWMlmod(String item) { return ((String[])weapons.get(item))[3]; }

    /**
     * Return weapon skill.
     */
    public String getWSkill(String item) { return ((String[])weapons.get(item))[4]; }

    /**
     * Get coverage.
     * @param item item that covers
     * @param material material of cover
     * @return hashtable mapping strings to int[]
     */
    public Hashtable getCoverage(String item, String material) {
        HashSet keys = new HashSet();

        // Get all covered areas
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("armour")) {
                NamedNodeMap cattr = child.getAttributes();
                boolean found =
                    cattr.getNamedItem("name").getNodeValue().equals(item);
                // Alternate names
                if (!found && cattr.getNamedItem("alt") != null) {
                    String[] tmp =
                        cattr.getNamedItem("alt").getNodeValue().split("\\b,\\b");
                    for (int k = 0; k < tmp.length; k++)
                        found |= (tmp[k].equals(item));
                }
                if (found) {
                    String[] tmp = cattr.getNamedItem("cover").getNodeValue().split(",");
                    for (int k = 0; k < tmp.length; k++)
                        keys.add(tmp[k]);
                    break;
                }
            }
        }

        // Get coverage values
        int[] val = null;
        tree = protect.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("material")) {
                NamedNodeMap cattr = child.getAttributes();
                // Base name
                boolean found =
                    cattr.getNamedItem("name").getNodeValue().equals(material);
                // Alternate names
                if (!found && cattr.getNamedItem("alt") != null) {
                    String[] tmp =
                        cattr.getNamedItem("alt").getNodeValue().split(",");
                    for (int k = 0; k < tmp.length; k++)
                        found |= (tmp[k].equals(material));
                }
                if (found) {
                    String[] tmp =
                        cattr.getNamedItem("value").getNodeValue().split("/");
                    val = new int[tmp.length];
                    for (int k = 0; k < val.length; k++)
                        val[k] = Integer.parseInt(tmp[k]);
                    break;
                }
            }
        }

        // Create table
        Hashtable ret = new Hashtable();
        Iterator iter = keys.iterator();
        while (val != null && iter.hasNext()) {
            String key = (String) iter.next();
            ret.put(key, val);
            if (key.endsWith("/b"))
                ret.put
                    (key.substring(0,key.indexOf("/")) + "/f",
                     new int[val.length]);
            if (key.endsWith("/f"))
                ret.put
                    (key.substring(0,key.indexOf("/")) + "/b",
                     new int[val.length]);
            if (key.endsWith("/r"))
                ret.put
                    (key.substring(0,key.indexOf("/")) + "/l",
                     new int[val.length]);
            if (key.endsWith("/l"))
                ret.put
                    (key.substring(0,key.indexOf("/")) + "/r",
                     new int[val.length]);
        }
        return ret;
    }

    /**
     * Match the regular armour and weapon expression.
     */
    public ArrayList match(String str) {
        ArrayList ret = new ArrayList();

        String quality = null; // (default) quality
        String material = null; // (default) material
        String rest = str.toLowerCase(); // remaining string to analyse
        String item = null;

        boolean looking = true;
        while (looking) {
            String old = rest;
            for (int i = 0; rest != null && i < armourPattern.length; i++) {
                Matcher m = armourPattern[i].matcher(rest);
                if (!m.matches()) continue;
                if (i == 0) { // material, item, vals
                    material = capit(m.group(1));
                    item = m.group(2);
                    rest = group(m, 4);
                    ret.add(new String[] { m.group(3), material, capit(item) });
                    break;
                }
                else if (i == 1) { // item, vals
                    item = m.group(1);
                    if (material == null) material = capit(getDefMaterial(item));
                    rest = group(m, 3);
                    ret.add(new String[] { m.group(2), material, capit(item) });
                    break;
                }
                else if (i == 2) { // quality, material, item
                    quality = m.group(1);
                    material = capit(m.group(2));
                    item = m.group(3);
                    rest = group(m, 4);
                    ret.add(new String[] { mapQ(quality), material, capit(item) });
                    break;
                }
                else if (i == 3) { // material, item
                    material = capit(m.group(1));
                    item = m.group(2);
                    rest = group(m, 3);
                    if (quality == null) quality = "average";
                    ret.add(new String[] { mapQ(quality), material, capit(item) });
                    break;
                }
                else if (i == 4) { // item
                    item = m.group(1);
                    rest = group(m, 2);
                    if (material == null) material = capit(getDefMaterial(item));
                    if (quality == null) quality = "average";
                    ret.add(new String[] { mapQ(quality), material, capit(item) });
                    break;
                }
            }
            looking = !old.equals(rest) && rest != null;
        }

        looking = true;
        while (looking) {
            String old = rest;
            for (int i = 0; rest != null && i < weaponPattern.length; i++) {
                Matcher m = weaponPattern[i].matcher(rest);
                if (!m.matches()) continue;
                item = capit(m.group(1));
                if (i == 0) { // item, WQ, B E P, skill
                    String dam = m.group(3) + "/" + m.group(4) + "/" + m.group(5);
                    ret.add
                        (new String[] {
                            item, dam, m.group(2),
                            getWHandmod(item), mlmodmod(getWMlmod(item),m.group(6)),
                            getWSkill(item), getWWeight(item) });
                    rest = group(m, 7);
                }
                else if (i == 1) { // item, WQ, BEP, skill
                    String dam = m.group(3) + "/" + m.group(4) + "/" + m.group(5);
                    ret.add
                        (new String[] {
                            item, dam, m.group(2),
                            getWHandmod(item), mlmodmod(getWMlmod(item),m.group(6)),
                            getWSkill(item), getWWeight(item) });
                    rest = group(m, 7);
                }
                if (i == 2) { // item, WQ, B E P
                    String dam = m.group(3) + "/" + m.group(4) + "/" + m.group(5);
                    ret.add
                        (new String[] {
                            item, dam, m.group(2),
                            getWHandmod(item), getWMlmod(item), getWSkill(item), getWWeight(item) });
                    rest = group(m, 6);
                }
                else if (i == 3) { // item, WQ, BEP
                    String dam = m.group(3) + "/" + m.group(4) + "/" + m.group(5);
                    ret.add
                        (new String[] {
                            item, dam, m.group(2),
                            getWHandmod(item), getWMlmod(item), getWSkill(item), getWWeight(item) });
                    rest = group(m, 6);
                }
                else if (i == 4) { // item, WQ
                    ret.add
                        (new String[] {
                            item, getWDamage(item), m.group(2),
                            getWHandmod(item), getWMlmod(item), getWSkill(item), getWWeight(item) });
                    rest = group(m, 3);
                }
                else if (i == 5) { // item
                    rest = group(m, 2);
                    ret.add
                        (new String[] {
                            item, getWDamage(item), getWQuality(item),
                            getWHandmod(item), getWMlmod(item), getWSkill(item), getWWeight(item) });
                }
            }
            looking = !old.equals(rest) && rest != null;
        }

        return ret;
    }

    /**
     * Get the weight for a given item.
     */
    public double getWeight(String item, String material) {
        // Get items
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("armour")) {
                NamedNodeMap cattr = child.getAttributes();
                boolean found = cattr.getNamedItem("name").getNodeValue().toLowerCase().equals
                    (item.toLowerCase());
                if (!found && cattr.getNamedItem("alt") != null) {
                    String[] alt =
                        cattr.getNamedItem("alt").getNodeValue().split("\\b,\\b");
                    for (int j = 0; j < alt.length; j++)
                        found |= alt[j].toLowerCase().equals(item.toLowerCase());
                }
                if (!found) continue;

                // Correct item found
                if (cattr.getNamedItem("weight") == null) return 0;
                double f1 = Double.parseDouble(cattr.getNamedItem("weight").getNodeValue());
                double f2 = getWeightFactor(material);
                return f1*f2;
            }
        }
        return 0;
    }

    /**
     * Get the default material for a given item.
     */
    private String getDefMaterial(String item) {
        // Get items
        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("armour")) {
                NamedNodeMap cattr = child.getAttributes();
                String material = cattr.getNamedItem("material").getNodeValue();
                if (cattr.getNamedItem("name").getNodeValue().toLowerCase().equals(item))
                    return material;
                if (cattr.getNamedItem("alt") != null) {
                    String[] alt =
                        cattr.getNamedItem("alt").getNodeValue().split("\\b,\\b");
                    for (int j = 0; j < alt.length; j++)
                        if (alt[j].toLowerCase().equals(item)) return material;
                }
            }
        }
        return null; // Every item must have a default material
    }

    /**
     * Create the regular expression against which all incoming character
     * equipment is matched.
     */
    private void prepRegexp() {
        // Armour 

        String qualities = "(average|good)";
        String materials = "";
        String items = "(";
        String vals = "\\(?((?:[a-z][0-9]+[/ ]?){3,})";

        armourPattern = new Pattern[5];

        // Get coverage names
        NodeList tree = protect.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("material")) {
                NamedNodeMap nmp = child.getAttributes();
                materials += nmp.getNamedItem("name").getNodeValue() + "|";
                if (nmp.getNamedItem("alt") != null) {
                    String[] alt =
                        nmp.getNamedItem("alt").getNodeValue().split(",");
                    for (int j = 0; j < alt.length; j++)
                        materials += alt[j] + "|";
                }
            }
        }

        materials = materials.substring(0,materials.length() - 1);
        materials = "(" + materials.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)") +
            ")";

        // Get items
        tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("armour")) {
                NamedNodeMap cattr = child.getAttributes();
                items += cattr.getNamedItem("name").getNodeValue() + "|";
                if (cattr.getNamedItem("alt") != null) {
                    String[] alt =
                        cattr.getNamedItem("alt").getNodeValue().split("\\b,\\b");
                    for (int j = 0; j < alt.length; j++)
                        items += alt[j] + "|";
                }
            }
        }
        items = items.substring(0,items.length() - 1) + ")";

        String g[] = new String[armourPattern.length];
        g[0] = ".*?\\b" + materials + "\\b.*?\\b" + items + "\\b.*?" + vals + "\\b(.*)";
        g[1] = ".*?\\b" + items + "\\b.*?" + vals + "\\b(.*)";
        g[2] = ".*?\\b" + qualities + "\\b.*?\\b" + materials + "\\b.*?\\b" +
            items + "\\b(.*)";
        g[3] = ".*?\\b" + materials + "\\b.*?\\b" + items + "\\b(.*)";
        g[4] = ".*?\\b" + items + "\\b(.*)";

        for (int i = 0; i < armourPattern.length; i++)
            armourPattern[i] = Pattern.compile(g[i].toLowerCase());

        // Weapons

        weaponPattern = new Pattern[6];
        items = "(";

        // Get items
        tree = dataDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("weapon")) {
                NamedNodeMap cattr = child.getAttributes();
                items += cattr.getNamedItem("name").getNodeValue() + "|";
                if (cattr.getNamedItem("alt") != null) {
                    String[] alt =
                        cattr.getNamedItem("alt").getNodeValue().split(",");
                    for (int j = 0; j < alt.length; j++)
                        items += alt[j] + "|";
                }
            }
        }
        items = items.substring(0,items.length() - 1) + ")";
        g = new String[6];
        g[0] = ".*?\\b" + items + "\\b.*?WQ\\s+(\\d+).*B\\s*([-\\d]+)\\s*/\\s*E\\s*([-\\d]+)\\s*/P\\s*([-\\d]+).*\\+(\\d+) to skill(.*)";
        g[1] = ".*?\\b" + items + "\\b.*?WQ\\s+(\\d+).*BEP\\s*([-\\d]+)/([-\\d]+)/([-\\d]+).*\\+(\\d+) to skill(.*)";
        g[2] = ".*?\\b" + items + "\\b.*?WQ\\s+(\\d+).*B\\s*([-\\d]+)\\s*/\\s*E\\s*([-\\d]+)\\s*/P\\s*([-\\d]+)(.*)";
        g[3] = ".*?\\b" + items + "\\b.*?WQ\\s+(\\d+).*BEP\\s*([-\\d]+)/([-\\d]+)/([-\\d]+)(.*)";
        g[4] = ".*?\\b" + items + "\\b.*?WQ\\s+(\\d+).*";
        g[5] = ".*?\\b" + items + "\\b(.*)";
        for (int i = 0; i < weaponPattern.length; i++)
            weaponPattern[i] = Pattern.compile(g[i].toLowerCase());

    }

    /**
     * Modify "x/y" by "a".
     * @param xy x/y aml/dml modifier
     * @param a modifier
     */
    public String mlmodmod(String xy, String a) {
        if (xy.equals("-") || a.equals("-")) return "-";
        String aml = xy.substring(0, xy.indexOf("/"));
        String dml = xy.substring(xy.indexOf("/")+1);
        aml = Integer.toString(Integer.parseInt(aml) + Integer.parseInt(a));
        dml = Integer.toString(Integer.parseInt(dml) + Integer.parseInt(a));
        return aml + "/" + dml;
    }

    /**
     * Map quality. We only support "good" and "average".
     */
    private String mapQ(String name) {
        if (name.equals("good")) return "+1";
        return "0";
    }

    /**
     * Captialize.
     */
    private String capit(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Capturing group
     */
    private String group(Matcher m, int i) {
        if (i <= m.groupCount()) return m.group(i);
        return null;
    }
}
