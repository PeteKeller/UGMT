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
package harn.equipment;

import java.text.Collator;
import java.util.*;
import java.util.regex.*;
import javax.swing.tree.*;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data. This also controls the GUI.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** Weight scales */
    protected static Hashtable weights;

    /**
     * Constructor.
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;
        weights = new Hashtable();
        Document dataDoc = Framework.parse(main.myPath + "data.xml", "items");
        loadDoc(dataDoc);
    }

    /**
     * Method to load the rules and implement the GUI.
     * @param doc document to load
     */
    private void loadDoc(Document doc) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("List");
        main.dataModel = new SortedTreeModel(root);

        NodeList tree = doc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
	    NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("item")) {
                String name = cattr.getNamedItem("name").getNodeValue();
                new Item(true, name, main, child, main.dataModel, root);
            }
            if (child.getNodeName().equals("weight")) {
                String name = cattr.getNamedItem("name").getNodeValue();
                if (cattr.getNamedItem("scale") != null) {
                    String scale = cattr.getNamedItem("scale").getNodeValue();
                    weights.put(name,scale);
                }
            }
        }
    }

    /**
     * Calculate the weight from a string such as "12.345lbs".
     */
    public static double calcWeight(String str) {
        Matcher m = Pattern.compile("(\\d*\\.?\\d*)\\s*(\\w*)").matcher(str);
        if (!m.matches()) return -1;
        double val = Double.parseDouble(m.group(1));
        String scaleS = m.group(2);
        if (weights.get(scaleS) == null) return val;
        double scale = Double.parseDouble((String)weights.get(scaleS));
        return val * scale;
    }
}
