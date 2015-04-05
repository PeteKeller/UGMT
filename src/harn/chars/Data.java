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
import java.io.*;
import java.awt.*;
import javax.swing.*;
import org.w3c.dom.*;
import rpg.Framework;

/**
 * Data object. Here we collect all our data. This also controls the GUI.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** list of attribute/skill sections */
    private ArrayList sections;

    /** list of action listeners */
    private ArrayList actlist;

    /** Mini-display template */
    private StringBuffer minitemplate;

    /**
     * Constructor.
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;
        sections = new ArrayList();
        actlist = new ArrayList();

        try {
            // Rules
            Document dataDoc = Framework.parse(main.myPath + "rules.xml", "rules");
            loadDoc(dataDoc);

            // Mini-Display
            minitemplate = new StringBuffer();
            BufferedReader br = new BufferedReader
                (new FileReader(main.myPath + "minitemplate.html"));
            String str;
            while ((str = br.readLine()) != null)
                minitemplate.append(str);
            br.close();

        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Get the Mini-display template
     */
    public String getMinitemplate() { return minitemplate.toString(); }

    /**
     * Method to load the rules and implement the GUI.
     * @param doc document to load
     */
    private void loadDoc(Document doc) {
        // Get the default width for the GUI
        JLabel lab = new JLabel("Some default width");
        int w = lab.getPreferredSize().width;

        NodeList tree = doc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
	    NamedNodeMap catt = child.getAttributes();

            // Section
            if (child.getNodeName().equals("section")) {
                sections.add(new Section(main, w, child, catt));
            }
            // Development (button)
            if (child.getNodeName().equals("development")) {
                boolean a = catt.getNamedItem("active") != null;
                AbstractButton b;
                DevActionListener al = new DevActionListener(main, child, a);
                if (a) {
                    b = new JButton(catt.getNamedItem("name").getNodeValue());
                }
                else {
                    actlist.add(al);
                    b = new JToggleButton
                        (catt.getNamedItem("name").getNodeValue());
                }
                b.addActionListener(al);
                main.addButton(b, a);
            }
        }
    }

    /**
     * Method to obtain a skill.
     * @param skill skill name to search for
     * @return skill
     */
    public CharSkill getSkill(String name) {
        for (int i = 0; i < sections.size(); i++) {
            Section sect = (Section) sections.get(i);
            CharSkill sk = sect.find(name);
            if (sk != null) return sk;
        }
        return null;
    }

    /**
     * Method to obtain the section a skill belongs to
     * @param skill skill to search
     * @return section name
     */
    public String getSection(String skill) {
        for (int i = 0; i < sections.size(); i++) {
            Section sect = (Section) sections.get(i);
            if (sect.find(skill) != null)
                return sect.getName();
        }
        return null;
    }

    /**
     * Method to load a character into display
     * @param attlist attribute key set
     * @param skilllist skill key set
     */
    public void load(Set attlist, String heraldry) {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).load(attlist, heraldry);
    }

    /**
     * Method to clear all attributes and heraldic,
     */
    public void clearAttrs() {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).clearAttrs();
    }

    /**
     * Method to clear all skills.
     */
    public void clearSkills() {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).clearSkills();
    }

    /**
     * Method to copy prelim. skills to ML.
     */
    public void finish() {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).finish();
    }

    /**
     * Reset the development (option) sections.
     */
    public void resetDevelopment() {
        for (int i = 0; i < actlist.size(); i++)
            ((DevActionListener)actlist.get(i)).resetDevelopment();
    }

    /**
     * Method to enable buttons (and change background color) for option
     * handling.
     * @param skills skill list
     * @param ops list of OptionSettings
     * @return whether something was actually done
     */
    public boolean enable(String[] skills, ArrayList opts) {
        boolean ret = false;
        for (int i = 0; i < sections.size(); i++) {
            Section sect = (Section)sections.get(i);
            boolean all = false;
            for (int j = 0; !all && (j < skills.length); j++)
                all = (sect.getName().indexOf(skills[j]) > -1);
            
            ret |= sect.enable((all ? null : skills), opts);
        }
        return ret;
    }

    /**
     * Method to set the OML of a skill.
     * @param name name of skill
     * @param value value of skill
     */
    public void setPML(String name, String value) {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).setPML(name, value);
    }

    /**
     * Method to set an attribute
     * @param name name of attribute
     * @param value value of attribute
     */
    public void setAttribute(String name, String value) {
        for (int i = 0; i < sections.size(); i++)
            ((Section)sections.get(i)).setAttribute(name, value);
    }

    /**
     * Roll a character, i.e. all attributes
     */
    public void roll() {
        for (int i = 0; i < sections.size(); i++) {
            ((Section)sections.get(i)).roll();
        }
    }
}
