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
package harn.index;

import harn.repository.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.Position;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.*;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data implements IListener {
    /** Root reference */
    private Main main;

    /** entry -> filename */
    private Hashtable flatdata;

    /** entry -> shapes */
    private Hashtable shapedata;

    /** sound bank */
    private SoundBank sounds;

    /**
     * Constructor. Turns a simple XML file into a hashmap of hashmaps.  All
     * nodes are read and their names are turned into property nodes. I.e.  <x
     * name="a"><y name="b" value="c"/><y name="d" value="e"></x> will be
     * turned into "a => {b => c, d => e}". Note that x and y are ignored.
     * @param aMain root reference
     * @param frw Framework reference for parsing the data file
     */
    Data(Main aMain, Framework frw) {
	main = aMain;
        flatdata = new Hashtable();
        shapedata = new Hashtable();

	// Used later for GUI
	DefaultMutableTreeNode atlNode;

        File dir = new File(main.myPath);
        String[] all = dir.list();

        for (int i = 0; all != null && i < all.length; i++) {
            File f = new File(main.myPath, all[i]);
            if (f != null && f.isDirectory()) {
                // Adjust GUI
                loadCurrent(all[i], null);
            }
        }
        reset();
    }

    /**
     * Load an index.
     * @param file name
     * @param open path to preselect (may be null)
     */
    void loadCurrent(String name, TreePath open) {
        try {
            // Clear old entries
            HashSet remov = new HashSet();
            Iterator iter = shapedata.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                if (!key.startsWith(name)) continue;
                ((MyLocation)shapedata.get(key)).setInvalid();
                remov.add(key);
            }
            iter = remov.iterator();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                shapedata.remove(key);
                flatdata.remove(key);
            }

            // Load data
            File pf = new File(main.myPath + name);
            String[] indexl = pf.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml");
                    }
                });

            for (int i = 0; i < indexl.length; i++) {
                String norm = Character.toUpperCase
                    (indexl[i].charAt(0)) + indexl[i].substring(1, indexl[i].length() - 4);

                Document doc = Framework.parse
                    (main.myPath + name + main.myFrw.SEP + indexl[i], "index");
                loadTree
                    (name, norm, doc.getDocumentElement().getChildNodes());
            }

            if (open == null) return;
            // Reset
            reset();

            // Open path
            Object[] tn = open.getPath();
            TreeNode node = (TreeNode)main.getIndex().getModel().getRoot();
            TreePath np = new TreePath(node);
            for (int i = 1; i < tn.length; i++) {
                for (int j = 0; j < node.getChildCount(); j++) {
                    TreeNode child = node.getChildAt(j);
                    if (child.toString().equals(tn[i].toString())) {
                        np = np.pathByAddingChild(child);
                        node = child;
                        break;
                    }
                }
            }
            main.getIndex().addSelectionPath(np);
            main.getIndex().expandPath(np);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Reset the GUI.
     */
    private void reset() {
        main.setIndexModel(new DefaultTreeModel(getIndexTree()));
        main.setTitleText("");
        main.setViewDocument(new HTMLDocument());
        main.history.clear();
        main.future.clear();
    }

    /**
     * Parses a branch on the XML-tree recursively.
     * @param tree current branch to parse
     */
    private void loadTree(String index, String hiername, NodeList tree) {
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    NamedNodeMap attr = tree.item(i).getAttributes();
	    if (attr != null) {
		String name = attr.getNamedItem("name").getNodeValue();

                // Put into flat view
                flatdata.put
                    (index + "/" + hiername + "/" + name,
                     attr.getNamedItem("value").getNodeValue());

                // Get all shapes
                Hashtable myShapes = new Hashtable();

		NodeList list = tree.item(i).getChildNodes();
		for (int j = 0; list != null && j < list.getLength(); j++) {
		    NamedNodeMap ca = list.item(j).getAttributes();
		    if (ca != null) {
			String map = ca.getNamedItem("href").getNodeValue();

                        // Find list maps->shapes
			if (myShapes.get(map) == null)
			    myShapes.put(map, new ArrayList());

			ArrayList shapes = (ArrayList) myShapes.get(map);
                        String xy = ca.getNamedItem("coords").getNodeValue();
                        String[] pts = xy.split(",");
                        Shape shape = null;

                        String type = ca.getNamedItem("shape").getNodeValue();
                        if (type.equals("poly")) {
                            shape = new Polygon();
                            for (int k = 0; k < pts.length; k += 2) {
                                int x = Integer.parseInt(pts[k]);
                                int y = Integer.parseInt(pts[k+1]);
                                ((Polygon)shape).addPoint(x,y);
                            }
                        }
                        else if (type.equals("circle")) {
                            int x = Integer.parseInt(pts[0]);
                            int y = Integer.parseInt(pts[1]);
                            int r = Integer.parseInt(pts[2]);
                            shape = new Ellipse2D.Float(x-r, y-r, 2*r, 2*r);
                        }
                        else if (type.equals("rect")) {
                            int x0 = Integer.parseInt(pts[0]);
                            int y0 = Integer.parseInt(pts[1]);
                            int x1 = Integer.parseInt(pts[2]);
                            int y1 = Integer.parseInt(pts[3]);
                            shape = new Rectangle(x0, y0, x1 - x0, y1 - y0);
                        }
                        shapes.add(shape);
		    }
		}

                // Add to shapedata
                shapedata.put
                    (index + "/" + hiername + "/" + name,
                     new MyLocation(main, index + "/" + hiername + "/" + name, myShapes));
	    }
	}
    }
    
    /**
     * Get the index tree as gui tree.
     */
    DefaultMutableTreeNode getIndexTree() {
        // Hierarchize flat data
        Hashtable hierdata = new Hashtable();
        Iterator iter = flatdata.keySet().iterator();
        while (iter.hasNext())  { hierAdd(hierdata, (String)iter.next(), 0); }

        // Base
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Files");
        TreeSet ts = new TreeSet(Collator.getInstance(Locale.FRANCE));
        ts.addAll(hierdata.keySet());

        // All indices
        Iterator iiter = ts.iterator();
        while (iiter.hasNext()) {
            String ikey = (String) iiter.next();
            DefaultMutableTreeNode inode = new DefaultMutableTreeNode(ikey);
            root.add(inode);
            Hashtable siht = (Hashtable) hierdata.get(ikey);
            TreeSet sits = new TreeSet(Collator.getInstance(Locale.FRANCE));
            sits.addAll(siht.keySet());

            // All sub-indices
            Iterator siiter = sits.iterator();
            while (siiter.hasNext()) {
                String sikey = (String) siiter.next();
                DefaultMutableTreeNode sinode =
                    new DefaultMutableTreeNode(sikey);
                inode.add(sinode);
                Hashtable eht = (Hashtable) siht.get(sikey);
                TreeSet ets = new TreeSet(Collator.getInstance(Locale.FRANCE));
                ets.addAll(eht.keySet());
                
                // All entries
                Iterator eiter = ets.iterator();
                while (eiter.hasNext()) {
                    String ekey = (String) eiter.next();
                    DefaultMutableTreeNode enode =
                        new DefaultMutableTreeNode(ekey);
                    sinode.add(enode);

                    if (main.getEditor() == null) continue;
                    Hashtable mht =
                        ((MyLocation)shapedata.get(ikey + "/" + sikey + "/" + ekey)).maps;

                    // All shapes
                    Iterator miter = mht.keySet().iterator();
                    while (miter.hasNext()) {
                        String mkey = (String) miter.next();
                        ArrayList al = (ArrayList)mht.get(mkey);
                        for (int i = 0; i < al.size(); i++) {
                            DefaultMutableTreeNode mnode =
                                new DefaultMutableTreeNode(i + " : " + mkey);
                            enode.add(mnode);
                        }
                    }
                }
            }
        }
        return root;
    }
    
    /**
     * Provides the file string for a given index entry
     * @param key name of entry
     * @return file name of entry
     */
    String getFile(String key) {
        String str = (String)flatdata.get(key);
        if (str == null) return null;
        return Framework.osName(str);
    }

    /**
     * Returns string representation of shape.
     * @return string form of shape
     */
    private String getShapeString(Object o) {
        if (o instanceof Polygon) return "Polygon";
        if (o instanceof Rectangle) return "Rectangle";
        return "Circle";
    }

    /**
     * Provides reverse lookup for the file string.
     * @param fname file name of entry
     * @return key of entry
     */
    String getKey(String fname) {
        // Normalize
        String pre = "file:" + main.myPath;
        String val = fname.substring(pre.length());
        
        Iterator iter = flatdata.keySet().iterator();
        while (iter.hasNext()) {
             String key = (String) iter.next();
             if (flatdata.get(key).equals(val))
                 return key;
        }
        return "---";
    }

    /**
     * Provides the location object for a given index entry
     * @param key name of entry
     * @return location object of entry
     */
    MyLocation getLocation(String key) {
	return (MyLocation) shapedata.get(key);
    }

    /**
     * Rename a (sub)index.
     * @param current index in which to rename
     * @param oname old index name
     * @param nname new index name
     * @param tp current tree selection
     */
    void renameIndex(String current, String oname, String nname, TreePath tp) {
        String ofn = current + main.myFrw.SEP + oname.toLowerCase() + ".xml";
        String odn = current + main.myFrw.SEP + oname.toLowerCase();
        File oFile = new File(main.myPath, ofn);
        File oDir = new File(main.myPath, odn);

        String nfn = current + main.myFrw.SEP + nname.toLowerCase() + ".xml";
        String ndn = current + main.myFrw.SEP + nname.toLowerCase();
        File nFile = new File(main.myPath, nfn);
        File nDir = new File(main.myPath, ndn);

        // Transform internal dir references
        try {
            // Get file
            Document doc = Framework.parse(main.myPath + ofn, "index");
            Node root = doc.getDocumentElement();

            // Remove element
            NodeList nl = root.getChildNodes();
            boolean done = false;
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                if (node.getNodeName().equals("entry")) {
                    NamedNodeMap nnm = node.getAttributes();
                    String val = nnm.getNamedItem("value").getNodeValue();
                    val = val.replaceFirst
                        ("/" + oname.toLowerCase() + "/", "/" + nname.toLowerCase() + "/");
                    nnm.getNamedItem("value").setNodeValue(val);
                }
            }

            // Write back
            Framework.transform(doc, new StreamResult(nFile), null);
            oFile.delete();
            oDir.renameTo(nDir);

            // Reload
            loadCurrent(current, tp);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Create a new (sub)index.
     * @param current index in which to create
     * @param name index name
     * @param tp current tree selection
     * @return errno
     */
    int createIndex(String current, String name, TreePath tp) {
        String fn = current + main.myFrw.SEP + name.toLowerCase() + ".xml";
        String dn = current + main.myFrw.SEP + name.toLowerCase();
        File oFile = new File(main.myPath, fn);
        File oDir = new File(main.myPath, dn);

        if (oFile.exists() || oDir.exists()) return -1;

        try {
            // Index file
            FileWriter out = new FileWriter(oFile);
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<index version=\"0.1\">\n");
            out.write("  <entry name=\"Empty\" value=\"");
            out.write(current + "/" + name.toLowerCase() + "/empty.html\"/>\n");
            out.write("</index>\n");
            out.close();

            // Index directory
            oDir.mkdir();

            // First dummy
            oFile = new File(main.myPath, dn + main.myFrw.SEP + "empty.html");
            out = new FileWriter(oFile);
            writeDummy(out);
            loadCurrent(current, tp);
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    /**
     * Import a HTML file into the current index.
     * @param current index in which this takes place
     * @param index subindex set to import into
     * @param name name of index
     * @param iFile file to import
     * @param tp current tree selection
     */
    void importFile(String current, String index, String name, File iFile, TreePath tp) {
        String prefix = index.toLowerCase() + main.myFrw.SEP;
        String ifn = (iFile != null ? iFile.getName() : randName());
        String ofn = current + main.myFrw.SEP + prefix + ifn;
	File oFile = new File(main.myPath, ofn);
        String ext = "";

        // If in = out, we do not copy/replace
        if (!oFile.equals(iFile))
            // Find a free file
            while (oFile.exists()) {
                ofn = current + main.myFrw.SEP + prefix + ext + ifn;
                oFile = new File(main.myPath, ofn);
                ext += "_";
            }

        // Put entry into XML file
        putEntry
            (current + main.myFrw.SEP + index.toLowerCase() + ".xml",
             name, ofn);

        // Reload
        loadCurrent(current, tp);

        // Copy the file
        if (!oFile.equals(iFile))
            try {
                FileWriter out = new FileWriter(oFile);
                
                if (iFile != null) {
                    FileReader in = new FileReader(iFile);
                    int c;
                    while ((c = in.read()) != -1) out.write(c);
                    in.close();
                }
                else {
                    writeDummy(out);
                }
                out.close();
            }
            catch (IOException e) {
                // Debug
                e.printStackTrace();
            }
    }

    /**
     * Remove the current sub-index.
     * @param current index in which to remove
     * @param index subindex to remove
     * @param tp current tree selection
     */
    public void removeIndex(String current, String index, TreePath tp) {
        String fn = current + main.myFrw.SEP + index.toLowerCase() + ".xml";
        String dn = current + main.myFrw.SEP + index.toLowerCase();
        File oFile = new File(main.myPath, fn);
        File oDir = new File(main.myPath, dn);
        rm(oFile);
        rm(oDir);

        // Reload
        loadCurrent(current, tp);
    }

    /**
     * Remove files recursively.
     */
    private void rm(File f) {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (int i = 0; i < fs.length; i++)
                rm(fs[i]);
        }
        f.delete();
    }

    /**
     * Rename the current entry.
     * @param current index in which to rename
     * @param oname old name
     * @param nname new name
     * @param index subindex in which to rename
     * @param tp current tree selection
     */
    public void renameEntry(String current, String oname, String nname, String index, TreePath tp) {
        try {
            // Get file
            String xf =
                main.myPath + current + main.myFrw.SEP +
                index.toLowerCase() + ".xml";
            Document doc = Framework.parse(xf, "index");
            Node root = doc.getDocumentElement();

            // Remove element
            NodeList nl = root.getChildNodes();
            boolean done = false;
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm != null &&
                    nnm.getNamedItem("name").getNodeValue().equals(oname)) {
                    nnm.getNamedItem("name").setNodeValue(nname);
                    done = true;
                }
            }

            // Write back
            Framework.transform(doc, new StreamResult(xf), null);

            // Reload
            loadCurrent(current, tp);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Remove the current entry.
     * @param current index in which to remove
     * @param name name of entry entry to remove
     * @param index subindex in which to remove
     * @param tp current tree selection
     */
    public void removeEntry(String current, String name, String index, TreePath tp) {
        try {
            // Get file
            String xf =
                main.myPath + current + main.myFrw.SEP +
                index.toLowerCase() + ".xml";
            Document doc = Framework.parse(xf, "index");
            Node root = doc.getDocumentElement();

            // Remove element
            NodeList nl = root.getChildNodes();
            boolean done = false;
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm != null &&
                    nnm.getNamedItem("name").getNodeValue().equals(name)) {
                    Node ws = node.getNextSibling();
                    root.removeChild(ws);
                    root.removeChild(node);
                    done = true;
                }
            }

            // Remove referenced file
            File hf = new File(main.myPath + getFile(name));
            hf.delete();

            // Write back
            Framework.transform(doc, new StreamResult(xf), null);

            // Reload
            loadCurrent(current, tp);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Create a location.
     * @param current index in which to create shape
     * @param entry entry name
     * @param index of entry
     * @param tp current tree selection
     */
    public void createShape(String current, String entry, String index, TreePath tp) {
        try {
            // Get file
            String xf =
                main.myPath + current + main.myFrw.SEP +
                index.toLowerCase() + ".xml";
            Document doc = Framework.parse(xf, "index");
            Node root = doc.getDocumentElement();

            boolean done = false;
            NodeList nl = root.getChildNodes();

            // Find correct map
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm != null &&
                    nnm.getNamedItem("name").getNodeValue().equals(entry)) {
                    // Add new element
                    Element el = doc.createElement("area");
                    el.setAttribute("shape", "circle");
                    el.setAttribute("coords", "0,0,0");
                    el.setAttribute("href", "unknown");
                    node.appendChild(el);
                    done = true;
                }
            }
            // Write back
            Framework.transform(doc, new StreamResult(xf), null);

            // Reload
            loadCurrent(current, tp);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Rename a location.
     * @param index index in which to rename
     * @param omap old map reference
     * @param map new map reference
     * @param sh new shape
     * @param idx index of shape in map (not in entry!)
     * @param name of entry
     * @param index for entry
     */
    public void renameShape(String current, String omap, String map, Shape sh, int idx, String name, String index) {
        try {
            // Get file
            String xf =
                main.myPath + current + main.myFrw.SEP +
                index.toLowerCase() + ".xml";
            Document doc = Framework.parse(xf, "index");
            Node root = doc.getDocumentElement();

            NodeList nl = root.getChildNodes();
            boolean done = false;

            // Find correct map
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm != null &&
                    nnm.getNamedItem("name").getNodeValue().equals(name)) {
                    NodeList nl2 = node.getChildNodes();
                    int trueI = 0;
                    // Find correct area
                    for (int j = 0; !done && (j < nl2.getLength()); j++) {
                        Node child = nl2.item(j);
                        if (child.getNodeName().equals("area")) {
                            Element el = (Element) child;
                            if (el.getAttributes().getNamedItem("href").getNodeValue().equals(omap)) {
                                if (idx == trueI) {
                                    if (sh instanceof Rectangle) {
                                        Rectangle rect = (Rectangle) sh;
                                        el.setAttribute("shape", "rect");
                                        el.setAttribute
                                            ("coords", rect.x + "," + rect.y + "," +
                                             (rect.x + rect.width) + "," +
                                             (rect.y + rect.height));
                                    }
                                    if (sh instanceof Ellipse2D.Float) {
                                        Ellipse2D.Float ell = (Ellipse2D.Float) sh;
                                        el.setAttribute("shape", "circle");
                                        el.setAttribute
                                            ("coords", (int)(ell.x + ell.width/2) + ","
                                             + (int)(ell.y + ell.height/2) + ","
                                             + (int)(ell.width/2));
                                    }
                                    if (sh instanceof Polygon) {
                                        Polygon poly = (Polygon) sh;
                                        el.setAttribute("shape", "poly");
                                        String coords = "";
                                        for (i = 0; i < poly.npoints; i++) {
                                            coords += "," + poly.xpoints[i] + "," + poly.ypoints[i];
                                        }
                                        el.setAttribute(coords, coords.substring(1));
                                    }
                                    el.setAttribute("href", map);
                                    done = true;
                                }
                                else
                                    trueI++;
                            }
                        }
                    }
                }
            }

            // Write back
            Framework.transform(doc, new StreamResult(xf), null);

            // Do path self, because cause does not know it
            Object[] tn = main.getIndex().getLeadSelectionPath().getPath();

            // Reload
            loadCurrent(current, null);
            reset();

            // Open path
            TreeNode node = (TreeNode)main.getIndex().getModel().getRoot();
            TreePath np = new TreePath(node);
            for (int i = 1; i < tn.length; i++) {
                for (int j = 0; j < node.getChildCount(); j++) {
                    TreeNode child = node.getChildAt(j);
                    if (child.toString().equals(tn[i].toString())) {
                        np = np.pathByAddingChild(child);
                        node = child;
                        break;
                    }
                }
            }
            main.getIndex().addSelectionPath(np);
            main.getIndex().expandPath(np);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Remove a location.
     * @param current index in which to remove
     * @param i index of shape
     * @param name of entry
     * @param index for shape in entry (not in map!)
     * @param tp current tree selection
     */
    public void removeShape(String current, int idx, String name, String index, TreePath tp) {
        try {
            // Get file
            String xf =
                main.myPath + current + main.myFrw.SEP +
                index.toLowerCase() + ".xml";
            Document doc = Framework.parse(xf, "index");
            Node root = doc.getDocumentElement();

            NodeList nl = root.getChildNodes();
            boolean done = false;

            // Find correct map
            for (int i = 0; !done && (i < nl.getLength()); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                if (nnm != null &&
                    nnm.getNamedItem("name").getNodeValue().equals(name)) {
                    NodeList nl2 = node.getChildNodes();
                    int trueI = 0;
                    // Find correct area
                    for (int j = 0; !done && (j < nl2.getLength()); j++) {
                        Node child = nl2.item(j);
                        if (child.getNodeName().equals("area")) {
                            if (idx == trueI) {
                                // Remove element
                                Node ws = child.getNextSibling();
                                node.removeChild(ws);
                                node.removeChild(child);
                                done = true;
                            }
                            else
                                trueI++;
                        }
                    }
                }
            }

            // Write back
            Framework.transform(doc, new StreamResult(xf), null);

            // Reload
            loadCurrent(current, tp);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Put the entry into a XML file.
     * @param name file to add into
     */
    private void putEntry(String name, String entry, String value) {
        try {
            // Get file
            Document doc = Framework.parse(main.myPath + name, "index");
            Node root = doc.getDocumentElement();

            Element nentry = findNodeWith(doc, entry);
            if (nentry == null) {
                // Add new element
                Element el = doc.createElement("entry");
                el.setAttribute("name", entry);
                el.setAttribute("value", value);
                root.appendChild(el);
            }
            else {
                nentry.setAttribute("value", value);
            }

            // Write back
            Framework.transform
                (doc, new StreamResult(new File(main.myPath + name)), null);
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Adds a hierarchie table.
     * @param hier table to add to
     * @param full string to ad
     * @param lvl continue while lvl <= 2
     */
    private void hierAdd(Hashtable hier, String full, int lvl) {
        if (lvl < 2) {
            int idx = full.indexOf("/");
            String first = full.substring(0, idx);
            String last = full.substring(idx + 1);
            Hashtable ht = (Hashtable) hier.get(first);
            if (ht == null) { ht = new Hashtable(); hier.put(first, ht); }
            hierAdd(ht, last, lvl+1);
        }
        else {
            Hashtable ht = (Hashtable) hier.get(full);
            if (ht == null) { ht = new Hashtable(); hier.put(full, ht); }
            
        }
    }

    /**
     * Inform of a sound bank.
     */
    public void inform(IExport obj) {
        sounds = (SoundBank)obj;
    }

    /**
     * Get the soun bank.
     * @return sound bank
     */
    public SoundBank getSoundBank() { return sounds; }

    /**
     * Get the node in an index doc with the param name.
     */
    static private Element findNodeWith(Document doc, String name) {
        NodeList list = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (!n.hasAttributes()) continue;
            Node nn = n.getAttributes().getNamedItem("name");
            if (name.equals(nn.getNodeValue())) return (Element) n;
        }
        return null;
    }

    /**
     * Get a random name.
     * @return random name
     */
    static private String randName() {
        return Integer.toString(9999 + DiceRoller.d(10000)).substring(1,5);
    }

    /**
     * Write default HTML file.
     * @param out writer to write to
     */
    static private void writeDummy(Writer out) throws IOException {
        out.write("<!doctype html public \"-//W3C//DTD HTML 3.2//EN\">\n");
        out.write("<html lang=\"en\">\n");
        out.write("  <head>\n");
        out.write("  </head>\n");
        out.write("  <body>\n");
        out.write("  </body>\n");
        out.write("</html>\n");
    }
}
