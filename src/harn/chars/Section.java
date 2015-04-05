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

import java.util.*;
import org.w3c.dom.*;
import javax.swing.*;
import java.awt.*;

/**
 * Class to hold attribute/skill sections.
 * @author Michael Jung
 */
public class Section {
    /** main reference */
    private Main main;

    /** Name of section */
    private String name;

    /** attribute/skill list */
    private ArrayList charitems;

    /**
     * Constructor.
     * @param aMain main reference
     * @param ew entry width
     * @param node node with children
     * @param attr attributes already determined
     */
    public Section(Main aMain, int ew, Node node, NamedNodeMap attr) {
        main = aMain;
        charitems = new ArrayList();

        // Get name
        name = attr.getNamedItem("name").getNodeValue();

        // Create panel with name
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(name));
        panel.setBackground(Color.lightGray);

        // Layout for this section
        GridBagConstraints constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.gridx = 0;
        main.getDisplay().add(panel, constr);

        JPanel dummy = new JPanel();
        dummy.setBackground(Color.lightGray);
        constr.weightx = 10e-6;
        constr.gridx = 99;
        panel.add(dummy, constr);

        // Parse subtree
        int idx = 0;
        boolean skillhead = false;
        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            State state = main.getState();

            if (child.getNodeName().equals("attribute"))
                charitems.add
                    (new CharAttribute(main, ew, idx++, child, cattr, panel));

            if (child.getNodeName().equals("skill")) {
                if (!skillhead) {
                    new CharSkill(panel, ew); // Header
                    idx++;
                    skillhead = true;
                }
                charitems.add
                    (new CharSkill(main.hideb, state, ew, idx++, child, cattr, panel));
            }

            if (child.getNodeName().equals("heraldry")) {
                charitems.add
                    (new CharHeraldry(main, child, cattr, panel));
            }

            if (child.getNodeName().equals("note")) {
                charitems.add
                    (new CharNote(main, ew, idx++, cattr, panel, this));
            }
        }
    }

    /**
     * Get name of section.
     * @return name of section
     */
    public String getName() { return name; }

    /**
     * Rolls all attributes of a section and resets all skills.
     */
    public void roll() {
        for (int i = 0; i < charitems.size(); i++) {
            setDirty();
            if (charitems.get(i) instanceof CharAttribute)
                ((CharAttribute)charitems.get(i)).roll(null);
            else if (charitems.get(i) instanceof CharSkill)
                ((CharSkill)charitems.get(i)).redo();
            else if (charitems.get(i) instanceof CharHeraldry)
                ((CharHeraldry)charitems.get(i)).setIcon((String)null, true);
        }
    }

    /**
     * Set Dirty.
     */
    protected void setDirty() { main.getState().setDirty(true); }

    /**
     * Method to clear all attributes and heraldry.
     */
    public void clearAttrs() {
        if (charitems.size() > 0) setDirty();

        for (int i = 0; i < charitems.size(); i++) {
            if (charitems.get(i) instanceof CharAttribute)
                ((CharAttribute)charitems.get(i)).clear();
            if (charitems.get(i) instanceof CharHeraldry)
                ((CharHeraldry)charitems.get(i)).clear();
        }
    }

    /**
     * Method to find a skill within this section.
     * @param skill skill to search
     * @return true if found
     */
    public CharSkill find(String skill) {
        for (int i = 0; i < charitems.size(); i++)
            if (charitems.get(i) instanceof CharSkill) {
                // Return closer match subskill
                CharSkill sk = (CharSkill) charitems.get(i);
                CharSkill ssk = sk.findSubSkill(skill);

                if (ssk != null) return ssk;

                // Return general skill (good enough for order criteria and
                // for attr/sun modifiers)
                String[] sk_sp = skill.split("#");
                ssk = sk.findSubSkill(sk_sp[0]);
                if (ssk != null) return ssk;
            }
        return null;
    }

    /**
     * Method to clear all skills
     */
    public void clearSkills() {
        for (int i = 0; i < charitems.size(); i++) {
            setDirty();
            if (charitems.get(i) instanceof CharSkill)
                ((CharSkill)charitems.get(i)).clear();
        }
    }

    /**
     * Method to copy prelim. skills to MLs
     */
    public void finish() {
        for (int i = 0; i < charitems.size(); i++) {
            setDirty();
            if (charitems.get(i) instanceof CharSkill)
                ((CharSkill)charitems.get(i)).fixPrelim();
        }
    }

    /**
     * Set the PML of a skill.
     * @param name skill name
     * @param vale skill value
     */
    public void setPML(String name, String value) {
        for (int i = 0; i < charitems.size(); i++) {
            if (charitems.get(i) instanceof CharSkill) {
                CharSkill sk = (CharSkill) charitems.get(i);
                sk.setPML(name, value);
            }
        }
    }

    /**
     * Set an attribute.
     * @param name attribute name
     * @param vale attribute value
     */
    public void setAttribute(String name, String value) {
        for (int i = 0; i < charitems.size(); i++) {
            if (charitems.get(i) instanceof CharAttribute) {
                CharAttribute attr = (CharAttribute) charitems.get(i);
                if (attr.getName().equals(name))
                    attr.set(value);
            }
        }
    }

    /**
     * Load a section. The passed parameters are supersets of
     * skills/attributes in this section. The icon is just set according per
     * "default".
     * @param attlist set of attributes to set
     * @param heraldry heraldry to set
     */
    public void load(Set attlist, String heraldry) {
        for (int i = 0; i < charitems.size(); i++) {
            if (charitems.get(i) instanceof CharAttribute) {
                CharAttribute att = (CharAttribute) charitems.get(i);
                if (attlist.contains(att.getName()))
                    att.set(main.getState().getAttribute(att.getName()));
                else
                    att.set("");
            }
            else if (charitems.get(i) instanceof CharSkill) {
                CharSkill sk = (CharSkill) charitems.get(i);
                sk.set();
            }
            else if (charitems.get(i) instanceof CharHeraldry) {
                CharHeraldry her = (CharHeraldry) charitems.get(i);
                her.setIcon(heraldry, false);
            }
            else { // Notes
                CharNote note = (CharNote) charitems.get(i);
                note.set();
            }
        }
    }
        
    /**
     * Method to enable buttons (and change background color) for option
     * handling. 
     * @param skills skill list (null if all)
     * @param ops list of OptionSettings
     * @return whether something was actually done
     */
    public boolean enable(String[] skills, ArrayList ops) {
        boolean ret = false;
        for (int i = 0; i < charitems.size(); i++) {
            if (!(charitems.get(i) instanceof CharSkill)) continue;

            CharSkill sk = (CharSkill) charitems.get(i);
            if (skills == null) {
                ret |= sk.enable(skills, ops);
                continue;
            }

            boolean all = false;
            for (int j = 0; !all && (j < skills.length); j++)
                all = (sk.getName().indexOf(skills[j]) > -1);

            ret |= sk.enable((all ? null : skills), ops);
        }
        return ret;
    }
}
