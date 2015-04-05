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

import java.util.*;
import org.w3c.dom.*;
import rpg.*;
import harn.repository.*;

/**
 * Data object. Represents the data objects from the XML file. Listens to
 * timeframe.
 * @author Michael Jung
 */
public class Data implements IListener {
    /** Root reference */
    private Main main;

    /** Representation of the state.xml file (and changes) */
    private Document dataDoc;

    /** List of known land travel modes */
    private ArrayList list;

    /** List of known water travel modes */
    private Hashtable wlist;

    /** time reference frame */
    private TimeFrame timeframe;

    /** List of all characters */
    private Hashtable allChars;

    /**
     * Constructor. Loads the data of the group plugin. This is
     * not a simple hasmap of hashmaps.
     * @param aMain root reference
     */
    Data(Main aMain) {
	main = aMain;
        allChars = new Hashtable();

        dataDoc = Framework.parse(main.myPath + "data.xml", "data");

        list = new ArrayList();
        wlist = new Hashtable();

        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap attr = tree.item(i).getAttributes();
            if (attr != null) {
                // Get name
                String name = attr.getNamedItem("name").getNodeValue();
                String type = attr.getNamedItem("type").getNodeValue();
                if (type.equals("land"))
                    list.add(name);
                if (type.equals("water")) {
                    String g = attr.getNamedItem("grades").getNodeValue();
                    String d = attr.getNamedItem("deploy").getNodeValue();
                    Hashtable ht = new Hashtable();
                    ht.put("grades", g);
                    ht.put("deploy", d);
                    wlist.put(name,ht);
                }
            }
        }
    }

    /**
     * Get the land mode list.
     * @return mode list
     */
    ArrayList getModes() { return list; }

    /**
     * Get the water mode list.
     * @return mode list
     */
    Hashtable getWaterModes() { return wlist; }

    /**
     * Inform this object about a timeframe.
     * @param e the object to inform about
     */
    public void inform(IExport e) {
        if (e instanceof TimeFrame) {
            timeframe = (TimeFrame) e;
            if (main.getState() != null)
                main.getState().announce(timeframe);
        }
        else { // Char
            Char c = (Char)e;
            if (c.isValid())
                allChars.put(c.getName(), c);
            else
                allChars.remove(c.getName());
        }
    }

    void setSpeed(State.MyCharGroup cg) {
        ArrayList members = cg.getMembers();
        main.display.setSpeed(-1);
        for (int i = 0; i < members.size(); i++) {
            String name = (String) members.get(i);
            try {
                // Ag = 10 implies 5 L/w
                float sp = Float.parseFloat
                    (((Char)allChars.get(name)).getAttribute("Agility"))/2;
                main.display.setSpeed(sp);
            }
            catch (Exception ex) {
                // Ignore
            }
        }
    }

    /**
     * Get a character
     */
    public Char getChar(String name) { return (Char) allChars.get(name); }

    /**
     * Provide the time frame reference to this package
     * @return time frame reference
     */
    TimeFrame getTimeframe() { return timeframe; }
}
