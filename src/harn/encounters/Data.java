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
package harn.encounters;

import java.io.*;
import org.w3c.dom.*;
import rpg.Framework;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** Minutes of a watch */
    static final int WATCH = 4 * 60;

    /** Minutes of a day */
    static final int DAY = 6 * WATCH;

    /** total (table) data */
    private Document total;

    /**
     * Constructor. Parses the data into a hashmap of hashmaps...
     * Gets all iconic weather items.
     * @param aMain root reference
     */
    public Data(Main aMain, Framework frw) {
	main = aMain;

        File pf = new File(main.myPath);
        String[] datal = pf.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return
                        name.endsWith(".xml") &&
                        name.startsWith("data");
                }
            });
        for (int i = 0; i < datal.length; i++) {
            Document data = Framework.parse(main.myPath + datal[i], "encounters");
            loadFromDoc(data);
        }
    }

    /**
     * Load the availWeath list from the XML doxument (reflecting the current
     * state).
     * @return hashtable containing the weather entries
     */
    private void loadFromDoc(Document data) {
        if (total == null) { total = data; return; }
	NodeList tree = data.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
            Node totroot = total.getDocumentElement();
            totroot.insertBefore(total.importNode(tree.item(i), true), null);
        }
    }

    /**
     * Find a table (node).
     * @param name name of table
     * @return table
     */
    public Node findTable(String name) {
	NodeList tree = total.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
            Node n = tree.item(i);
            if (n.getNodeName().equals("table") &&
                n.getAttributes().getNamedItem("name").getNodeValue().equals(name))
                return n;
        }
        throw new RuntimeException("Can't find table " + name);
    }

    /**
     * Get total table data.
     * @return table data
     */
    public Document getTotal() { return total; }
}
