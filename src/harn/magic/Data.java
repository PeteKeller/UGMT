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
package harn.magic;

import rpg.SortedTreeModel;
import rpg.*;
import java.io.*;
import javax.swing.tree.DefaultMutableTreeNode;
import org.w3c.dom.*;
import java.util.*;

/**
 * Data object. Here we collect all our data. This also controls the GUI tree.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** All spells/invocations */
    protected Hashtable spells;

    /** All development / with skillindex */
    protected Hashtable si;

    /** All development / with attributes */
    protected Hashtable attr;

    /** All development conditions */
    protected Hashtable cond;

    /** Full names of all spells */
    protected HashSet full;

    /**
     * Constructor.
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;
        spells = new Hashtable();
        attr = new Hashtable();
        si = new Hashtable();
        cond = new Hashtable();
        full = new HashSet();

        try {
            // Rules
            Document dataDoc;

            File pf = new File(main.myPath);
            String[] datl = pf.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml") &&
                            name.startsWith("data");
                    }
                });
            for (int i = 0; i < datl.length; i++) {
                dataDoc = Framework.parse(main.myPath + datl[i], "magic");
                DataTreeNode root = null;
                if (main.tree != null)
                    root = (DataTreeNode) main.tree.getModel().getRoot();
                loadDoc(root, dataDoc.getDocumentElement(), "List", "", "oml:5", null);
            }

        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Method to load the spells.
     * @param doc document to load
     */
    private DefaultMutableTreeNode loadDoc(DataTreeNode gui, Node doc, String name, String fullname, String defOML, String link) {
        if (gui == null) gui = new DataTreeNode(name, link);

        if (main.tree == null)
            main.tree = new DragTree(new SortedTreeModel(gui), main);

        NodeList tree = doc.getChildNodes();
        if (tree == null || tree.getLength() == 0) {
            full.add(fullname.substring(1));
            spells.put(name, defOML);
        }

	for (int i = 0; tree != null && i < tree.getLength(); i++) {
            String myOML = defOML;
	    Node child = tree.item(i);
            String myLink = link;
	    NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("spell") ||
                child.getNodeName().equals("group")) {
                String cname = cattr.getNamedItem("name").getNodeValue();
                if (cattr.getNamedItem("oml") != null)
                    myOML = "oml:" + cattr.getNamedItem("oml").getNodeValue();
                if (cattr.getNamedItem("skill") != null)
                    myOML = "skill:" + cattr.getNamedItem("skill").getNodeValue();
                if (cattr.getNamedItem("attribute") != null)
                    myOML = "attr:" + cattr.getNamedItem("attribute").getNodeValue();
                if (cattr.getNamedItem("link") != null)
                    myLink = cattr.getNamedItem("link").getNodeValue();
                DataTreeNode gnode = getChild(gui, cname);
                DataTreeNode tn =
                    (DataTreeNode) loadDoc(gnode, child, cname, fullname + "/" + cname, myOML, myLink);
                if (gnode == null)
                ((SortedTreeModel)main.tree.getModel()).insertNodeSorted(tn, gui);
            }
            if (child.getNodeName().equals("development")) {
                String group = cattr.getNamedItem("group").getNodeValue();
                if (cattr.getNamedItem("SI") != null)
                    si.put
                        (group, cattr.getNamedItem("SI").getNodeValue());
                else
                    attr.put
                        (group, cattr.getNamedItem("attr").getNodeValue());

                loadDev(group, child);
            }
        }
        return gui;
    }

    /**
     * Get the gui child node. If none exists, return null.
     * @param gnode GUI node, never null
     * @param name name to search for
     */
    private DataTreeNode getChild(DataTreeNode gnode, String name) {
        DataTreeNode ret = null;
        Enumeration num = gnode.children();
        while (num.hasMoreElements()) {
            DataTreeNode tn = (DataTreeNode) num.nextElement();
            if (tn.getUserObject().equals(name))
                return tn;
        }
        return ret;
    }

    /**
     * Load option sets
     */
    private void loadDev(String group, Node node) {
        NodeList tree = node.getChildNodes();
        if (tree == null || tree.getLength() == 0) return;

        Hashtable ht = new Hashtable();
	for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    Node child = tree.item(i);
	    NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("set")) {
                String test = cattr.getNamedItem("test").getNodeValue();
                String cost = cattr.getNamedItem("cost").getNodeValue();
                ht.put(test, cost);
            }
        }
        cond.put(group, ht);
    }

    /**
     * Class to hold data and state.
     */
    class DataTreeNode extends DefaultMutableTreeNode {
        String _link;
        DataTreeNode(String name, String link) { super(name); _link = link; }
        public boolean getEnabled() {
            return main.develop.costs.keySet().contains(getUserObject());
        }
        public String getLink() { return _link; }
    }
}
