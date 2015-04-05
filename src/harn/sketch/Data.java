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
package harn.sketch;

import org.w3c.dom.*;
import java.util.*;
import javax.swing.tree.*;
import java.io.*;
import rpg.*;
import harn.repository.Sketch;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main;

    /** State change from previous persistent state */
    private boolean dirty;

    /**
     * Constructor.
     * @param aMain root reference
     */
    public Data(Main aMain) {
        main = aMain;
        File dir = new File(main.myPath);
	main.sketchtree = new TreeNode(main, "sketch", dir);
        buildTree(dir, main.sketchtree);
        load();
    }

    /**
     * Dir-Tree builder
     */
    private void buildTree(File dir, TreeNode node) {
        Object[] all = dir.list();
        Hashtable added = new Hashtable();

        TreeSet sort = new TreeSet();
        for (int i = 0; all != null && i < all.length; i++) sort.add(all[i]);

        Iterator iter = sort.iterator();
        while (iter.hasNext()){
            String curr = (String) iter.next();
            File f = new File(dir.getAbsolutePath() + Framework.SEP + curr);
            if (f.isDirectory()) {
                TreeNode child =
                    new TreeNode(main, curr, f);
                node.add(child);
                buildTree(f, child);
            }
            else {
                if (curr.endsWith(".html") ||
                    curr.endsWith(".txt") ||
                    curr.endsWith(".png")) {
                    String name = curr.replaceFirst("\\.....?$","");
                    TreeNode tn = (TreeNode) added.get(name);
                    if (tn != null) {
                        if (curr.endsWith(".txt")) tn.setTxt(f);
                        if (curr.endsWith(".png")) tn.setPng(f);
                        if (curr.endsWith(".html")) tn.setHtml(f);
                    }
                    else {
                        tn = new TreeNode(main, name, f);
                        node.add(tn);
                        added.put(name, tn);
                    }
                }
            }
        }
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
    public void save() {
        // Clean
        main.myDrawing.save();
        setDirty(false);
    }

    /**
     * Use this method to save a synopsis. Has no effect on "clean".
     */
    public void saveSynopsis(TreeNode node) {
        try {
            LinkedList al = new LinkedList();
            Framework.backup(main.myPath + "synopsis.hml");
            File f = new File(main.myPath + "synopsis.html");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String s = null;
                while ((s = br.readLine()) != null)
                    al.add(s+"\n");
            }
            FileWriter fw = new FileWriter(f);

            Enumeration e = node.depthFirstEnumeration();
            while (e.hasMoreElements()) {
                TreeNode tn = (TreeNode)e.nextElement();
                String fn = main.fullName(tn);
                if (tn.isLeaf()) {
                    if (tn.sketch.isExported()) {
                        for (int i = 3; fn != null && i < al.size() - 2; i++) {
                            if (((String)al.get(i)).indexOf(fn) > -1)
                                fn = null;
                        }
                        if (fn != null)
                            al.add
                                (al.size() - 2,
                                 "  <p><a href=\"" + fn + "\">" + fn + "</a>\n");
                    }
                    else {
                        for (int i = 3; i < al.size() - 2; i++) {
                            if (((String)al.get(i)).indexOf(fn) > -1) {
                                al.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < al.size(); i++)
                fw.write(al.get(i).toString());
            fw.close();
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Use this method to save the export tree. Has no effect on "clean".
     */
    public void saveExports(TreeNode node, Element el, Document doc) {
        Enumeration list = node.children();
        while (list.hasMoreElements()) {
            Element newChild = doc.createElement("sketch");
            saveExports((TreeNode) list.nextElement(), newChild, doc);
            el.appendChild(newChild);
        }
        el.setAttribute("name", node.sketch.name);
        el.setAttribute("export", node.sketch.isExported() ? "true" : "false");
    }

    /**
     * Create a new note.
     */
    public void createNote(TreeNode tn) {
        File txt = new File
            (tn.getDir().getAbsolutePath() + Framework.SEP + "New-Note.txt");
        File png = new File
            (tn.getDir().getAbsolutePath() + Framework.SEP + "New-Note.png");
        File html = new File
            (tn.getDir().getAbsolutePath() + Framework.SEP + "New-Note.html");

        if (txt.exists() || png.exists() || html.exists()) return;
        try {
            txt.createNewFile();
            png.createNewFile();
            TreeNode newNode = new TreeNode(main, "New-Note", txt);
            newNode.setPng(png);
            ((SortedTreeModel)main.guitree.getModel()).insertNodeSorted(newNode, tn);
            main.guitree.scrollPathToVisible(new TreePath(newNode.getPath()));
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Create a new directory.
     */
    public void createDirectory(TreeNode tn) {
        File f = new File
            (tn.getDir().getAbsolutePath() + Framework.SEP + "New-Group");

        if (f.exists()) return;
        f.mkdir();
        TreeNode newNode = new TreeNode(main, "New-Group", f);
        ((SortedTreeModel)main.guitree.getModel()).insertNodeSorted
            (newNode, tn);
        main.guitree.scrollPathToVisible(new TreePath(newNode.getPath()));
    }

    /**
     * Remove a node.
     */
    public void remove(TreeNode tn) {
        if (tn.delete())
            ((DefaultTreeModel)main.guitree.getModel()).removeNodeFromParent(tn);
    }

    /**
     * Load the state file.
     */
    private void load() {
        Document dataDoc = Framework.parse(main.myPath + "state.xml", "sketch");
        loadDoc(dataDoc.getDocumentElement(), main.sketchtree);
    }

    /**
     * Method to load the state.
     * @param doc (XML) element to load
     * @param gui gui node that corresponds to "doc"
     */
    private void loadDoc(Node doc, TreeNode gui) {
        NodeList tree = doc.getChildNodes();
        NamedNodeMap cattr = doc.getAttributes();
        if (cattr.getNamedItem("export").getNodeValue().equals("true"))
            gui.sketch.export = true;

	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("sketch")) {
                cattr = child.getAttributes();
                String sketch = cattr.getNamedItem("name").getNodeValue();
                Enumeration list = gui.children();
                while (list.hasMoreElements()) {
                    TreeNode tmp = (TreeNode)list.nextElement();
                    String key = (String) tmp.getUserObject();
                    if (key.equals(sketch))
                        loadDoc(child, tmp);
                }
            }
        }
    }
}
