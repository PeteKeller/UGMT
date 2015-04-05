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
import harn.repository.*;
import java.util.*;
import org.w3c.dom.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** Auto adjust list */
    private Hashtable sklist;

    /**
     * Constructor,
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;
        sklist = new Hashtable();

        Document dataDoc = Framework.parse
            (main.myPath + "data.xml", "skilldata");

        NodeList tree = dataDoc.getDocumentElement().getChildNodes();
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap att1 = tree.item(i).getAttributes();
            if (att1 != null) {
                // Get skill
                String skill = att1.getNamedItem("name").getNodeValue();
                NodeList nl1 = tree.item(i).getChildNodes();
                for (int j = 0; nl1 != null && j < nl1.getLength(); j++) {
                    NamedNodeMap att2 = nl1.item(j).getAttributes();
                    if (att2 != null) {
                        // Get type
                        String type = att2.getNamedItem("name").getNodeValue();
                        NodeList nl2 = nl1.item(j).getChildNodes();
                        for (int k = 0; nl2 != null && k < nl2.getLength(); k++) {
                            NamedNodeMap att3 = nl2.item(k).getAttributes();
                            if (att3 != null) {
                                // Get key
                                String key = att3.getNamedItem("key").getNodeValue();
                                String val = att3.getNamedItem("value").getNodeValue();
                                Hashtable tlist = (Hashtable) sklist.get(skill);
                                if (tlist == null) {
                                    tlist = new Hashtable();
                                    sklist.put(skill, tlist);
                                }

                                Hashtable res = (Hashtable) tlist.get(type);
                                if (res == null) {
                                    res = new Hashtable();
                                    tlist.put(type, res);
                                }

                                res.put(key, val);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Return skill list.
     * @return skill list
     */
    public Hashtable getSkillList() { return sklist; }

}
