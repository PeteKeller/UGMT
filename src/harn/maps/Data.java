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
package harn.maps;

import harn.repository.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import java.awt.*;
import java.lang.ref.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data. Since the repository contains
 * abstract classes, we need to provide an inner class that represents the
 * ScaledMaps. All relevant maps are stored here.
 * @author Michael Jung
 */
public class Data {
    /** default return terrain/vegetation combination */
    final static private String[] defTerVeg = new String[] { "flat", "trail" };

    /** Root reference */
    private Main main;

    /** XML atlas documents (fullName -> NodeList) */
    private Hashtable mapNodes;

    /** list of all maps (full name -> map) */
    static private Hashtable mapList;

    /** list of all dir grids (full name -> (grid-coords -> linked map)) */
    static private Hashtable gridLinks;

    /** atlas/dir table of terrain/vegetation combinations */
    private Hashtable color;

    /** soft refs of filenames to images */
    private Hashtable fileRefs;

    /** soft refs of filenames to full images */
    protected Hashtable imgRefs;

    /**
     * Constructor,
     * @param aMain root reference
     * @param frw Framework reference for parsing the data file
     */
    public Data(Main aMain, Framework frw) {
	main = aMain;
        loadData();
    }

    /**
     * Rename a node. The node must be in the Private file.
     * @param path path to new named node 
     * @param oldName old name.
     * @param newName new name.
     */
    public boolean rename(Object[] path, String newName, String oldName) {
        // Get File
        Document doc = loadPrivate();
        if (doc == null) return false;

        // Create new element
        Node node = renameNode(doc, path, oldName, newName);
        if (node == null) return false;

        // Save file
        savePrivate(doc);

        // Reload
        main.resetTree();
        loadData();
        return true;
    }

    /**
     * (Re)load all data.
     */
    public void loadData() {
	mapNodes = new Hashtable();
	mapList = new Hashtable();
        gridLinks = new Hashtable();
	color = new Hashtable();
        fileRefs = new Hashtable();
        imgRefs = new Hashtable();

	// Used later for GUI
	DefaultMutableTreeNode atlNode;

        File dir = new File(main.myPath);
        String[] all = (dir != null ? dir.list() : null);

	try {
	    for (int i = 0; all != null && i < all.length; i++) {
		File pf = new File(main.myPath, all[i]);
		if (pf != null && pf.isDirectory()) {
                    MyAtlas atlas = new MyAtlas(all[i]);

                    // Adjust GUI
                    atlNode = new DefaultMutableTreeNode(all[i]);
                    main.getTreeModel().insertNodeSorted
                        (atlNode, main.maptree);

		    // Get the XML index of this atlas
                    String[] allatl = pf.list(new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".xml");
                            }
                        });

                    // Fill an atlas object with it and announce
                    for (int j = 0; allatl != null && j < allatl.length; j++) {
                        Document doc = Framework.parse
                            (pf.getAbsolutePath() + Framework.SEP + allatl[j], "atlas");
                        createMaps
                            (atlas, atlas.getName() + "/",
                             doc.getDocumentElement(), atlNode);
                    }
                    main.myFrw.announce(atlas);
		}
	    }
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * This method creates the maps in the mapholder class from the document
     * parameter.
     * @param doc document to get maps from
     */
    void createMaps(MyAtlas atlas, String mappath, Node doc, DefaultMutableTreeNode gui) {
	// Get field size
        if (doc.getChildNodes() == null ||
            doc.getChildNodes().getLength() == 0)
            return;
	NamedNodeMap nnm = doc.getChildNodes().item(0).getAttributes();

	// Get all entries
	NodeList tree = doc.getChildNodes();
        Hashtable shapes = new Hashtable();
	for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    NamedNodeMap attr = tree.item(i).getAttributes();
	    if (attr != null) {
		if (tree.item(i).getNodeName().equals("dir")) {
                    // Get dir data
		    String name = attr.getNamedItem("name").getNodeValue();
                    String full = mappath + name;
                    if (attr.getNamedItem("thumbnail") != null) {
                        String ref = attr.getNamedItem("thumbnail").getNodeValue();
                        String gridid = full;
                        if (attr.getNamedItem("grid") != null)
                            gridid = attr.getNamedItem("grid").getNodeValue();
                        String mappat = mappath.substring(0, mappath.length()-1);
                        Hashtable ht = (Hashtable) gridLinks.get(mappat);
                        if (ht == null) ht = new Hashtable();
                        ht.put(gridid, new String[] { main.myPath + atlas.getName() + "/" + ref, full, name });
                        gridLinks.put(mappat, ht);
                    }

                    // Adapt Gui
                    Enumeration children = gui.children();
                    DefaultMutableTreeNode node = null;
                    while (node == null && children.hasMoreElements()) {
                        DefaultMutableTreeNode tmp =
                            (DefaultMutableTreeNode) children.nextElement();
                        if (tmp.getUserObject().equals(name))
                            node = tmp;
                    }
                    if (node == null) {
                        node = new DefaultMutableTreeNode(name) {
                                public boolean isLeaf() { return false; }
                            };
                        ((SortedTreeModel)main.getTreeModel()).insertNodeSorted(node, gui);
                    }

                    // Recurse down tree
                    createMaps
                        (atlas, mappath + name + "/", tree.item(i), node);
                }
		if (tree.item(i).getNodeName().equals("map")) {
		    // Get MyMap data
		    String name = attr.getNamedItem("name").getNodeValue();
                    String full = mappath + name;
		    String img = atlas.getName() + "/" + attr.getNamedItem("img").getNodeValue();
		    String raster = null;
                    if (attr.getNamedItem("raster") != null)
                        raster = atlas.getName() + "/" + attr.getNamedItem("raster").getNodeValue();
		    int scale = 1;
                    if (attr.getNamedItem("scalef") != null) {
                        String scStr = attr.getNamedItem("scalef").getNodeValue();
                        scale = (int)(Double.parseDouble(scStr) * 13200);
                    }
                    else {
                        String scStr = attr.getNamedItem("scale").getNodeValue();
                        scale = Integer.parseInt(scStr);
                    }
		    Node n = attr.getNamedItem("tacticalspeed");
		    float speedQuot =
			(n != null ? Float.parseFloat(n.getNodeValue()) : .0f);
                    if (attr.getNamedItem("thumbnail") != null) {
                        String ref = attr.getNamedItem("thumbnail").getNodeValue();
                        String gridid = full;
                        if (attr.getNamedItem("grid") != null)
                            gridid = attr.getNamedItem("grid").getNodeValue();
                        String mappat = mappath.substring(0, mappath.length()-1);
                        Hashtable ht = (Hashtable) gridLinks.get(mappat);
                        if (ht == null) ht = new Hashtable();
                        ht.put(gridid, new String[] { main.myPath + atlas.getName() + "/" + ref, full, name });
                        gridLinks.put(mappat, ht);
                    }

                    // Get layers
                    ArrayList layers = new ArrayList();
                    layers.add(img);
                    int l = 1;
                    while (l < 4 || attr.getNamedItem("layer" + l) != null) {
                        if (attr.getNamedItem("layer" + l) == null)
                            layers.add(null);
                        else
                            layers.add
                                (atlas.getName() + "/" +
                                 attr.getNamedItem("layer" + l).getNodeValue());
                        l++;
                    }

                    String heightfield = null;
                    if (attr.getNamedItem("heightfield") != null)
                        heightfield = attr.getNamedItem("heightfield").getNodeValue();
                    int height = 0;
                    if (attr.getNamedItem("height") != null)
                        height = Integer.parseInt
                            (attr.getNamedItem("height").getNodeValue());

		    // Create/Load and put in atlas
		    ScaledMap map = new MyMap
			(mappath.substring(mappath.indexOf("/")+1) + name, heightfield,
                         raster, scale, atlas.getName(), speedQuot, layers,
                         tree.item(i).getChildNodes(), height);
		    atlas.addMap(map);

		    // Add to GUI
                    ((SortedTreeModel)main.getTreeModel()).insertNodeSorted
                        (new DefaultMutableTreeNode(name), gui);

		    // Store and announce
		    mapNodes.put(full, tree.item(i).getChildNodes());
		    mapList.put(full, map);
		    main.myFrw.announce(map);
		}
		if (tree.item(i).getNodeName().equals("color")) {
		    String rawName = attr.getNamedItem("name").getNodeValue();
		    Node terA = attr.getNamedItem("terrain"); 
		    String ter = (terA != null ? terA.getNodeValue() : null);
		    Node vegA = attr.getNamedItem("vegetation");
		    String veg = (vegA != null ? vegA.getNodeValue() : null);

		    String name = Integer.toString(Integer.parseInt(rawName,16));

		    String[] comb = new String[] { ter, veg };
                    String mappat = mappath.substring(0, mappath.length()-1);
                    Hashtable ht = (Hashtable)color.get(mappat);
                    if (ht == null) {
                        ht = new Hashtable();
                        color.put(mappat, ht);
                    }
                    ht.put(name, comb);
		}
	    }
	}
    }

    /** Return true if water */
    private boolean isWater(String test) {
        return test != null && test.toLowerCase().equals("water");
    }

    private String[] getTerrVegFromColour(Image img, int x, int y, String mapname) {
	int w = img.getWidth(null);
	int h = img.getHeight(null);
        int fsz = 20; /** @todo */
	int[] data = new int[fsz * fsz];

	// Grab data from the image
	PixelGrabber pg;
	try {
	    int x0 = x - fsz/2;
	    int y0 = y - fsz/2;
	    pg = new PixelGrabber(img,x0,y0,fsz,fsz,data,0,fsz);
	    pg.grabPixels();
	}
	catch (InterruptedException e) {
	    // Debug
	    e.printStackTrace();
	}

	// Center
	int c = fsz/2 * (fsz + 1);

	Hashtable t = (Hashtable) recurseGet(color, mapname);
	String[] ret = new String[3];
	// Check for colors. Iterator from center to outside and then along
	// the border/frame until we find something.
	for (int i = 0; i < 2; i++) {
	    for (int j = 0; j < fsz/2 && ret[i] == null; j++) {
		if (y - j < 0 || y + j > h) continue;
		for (int k = -j; k <= j && ret[i] == null; k++) {
		    if (x + k < 0 || x + k > w) continue;
		    // North
		    if (ret[i] == null) {
			int rgb = 0xffffff & data[c - j * fsz + k];
			String rgbStr = Integer.toString(rgb);
                        String[] rt = (String[]) t.get(rgbStr);
                        if (rt != null) ret[i] = rt[i];
		    }
		    // East
		    if (ret[i] == null) {
			int rgb = 0xffffff & data[c - j + k * fsz];
			String rgbStr = Integer.toString(rgb);
                        String[] rt = (String[]) t.get(rgbStr);
                        if (rt != null) ret[i] = rt[i];
		    }
		    // South
		    if (ret[i] == null) {
			int rgb = 0xffffff & data[c + j * fsz + k];
			String rgbStr = Integer.toString(rgb);
                        String[] rt = (String[]) t.get(rgbStr);
                        if (rt != null) ret[i] = rt[i];
		    }
		    // West
		    if (ret[i] == null) {
			int rgb = 0xffffff & data[c + j + k * fsz];
			String rgbStr = Integer.toString(rgb);
                        String[] rt = (String[]) t.get(rgbStr);
                        if (rt != null) ret[i] = rt[i];
		    }
		}
	    }
	}

	// Take absolute default
	for (int i = 0; i < 2; i++)
	    if (ret[i] == null) ret[i] = defTerVeg[i];

	return ret;
    }

    /** Get Terrain and vegetation from image */
    private String[] getLandTerrVegFromColour(Image img, int x, int y, String mapname) {
	int w = img.getWidth(null);
	int h = img.getHeight(null);
        int fsz = 20; /** @todo */
	int[] data = new int[fsz * fsz];

	// Grab data from the image
	PixelGrabber pg;
	try {
	    int x0 = x - fsz/2;
	    int y0 = y - fsz/2;
	    pg = new PixelGrabber(img,x0,y0,fsz,fsz,data,0,fsz);
	    pg.grabPixels();
	}
	catch (InterruptedException e) {
	    // Debug
	    e.printStackTrace();
	}

	// Center
	int c = fsz/2 * (fsz + 1);

	Hashtable t = (Hashtable) recurseGet(color, mapname);
	String[] ret = null;
	// Check for colors. Iterator from center to outside and then along
	// the border/frame until we find something.
        for (int j = 0; j < fsz/2 && ret == null; j++) {
            if (y - j < 0 || y + j > h) continue;
            for (int k = -j; k <= j && ret == null; k++) {
                if (x + k < 0 || x + k > w) continue;
                // North
                if (ret == null || isWater(ret[0])) {
                    int rgb = 0xffffff & data[c - j * fsz + k];
                    String rgbStr = Integer.toString(rgb);
                    ret = (String[]) t.get(rgbStr);
                }
                // East
                if (ret == null || isWater(ret[0])) {
                    int rgb = 0xffffff & data[c - j + k * fsz];
                    String rgbStr = Integer.toString(rgb);
                    ret = (String[]) t.get(rgbStr);
                }
                // South
                if (ret == null || isWater(ret[0])) {
                    int rgb = 0xffffff & data[c + j * fsz + k];
                    String rgbStr = Integer.toString(rgb);
                    ret = (String[]) t.get(rgbStr);
                }
                // West
                if (ret == null || isWater(ret[0])) {
                    int rgb = 0xffffff & data[c + j + k * fsz];
                    String rgbStr = Integer.toString(rgb);
                    ret = (String[]) t.get(rgbStr);
                }
                if (ret != null && isWater(ret[0])) ret = null;
            }
        }

	// Take absolute default
        if (ret == null) ret = new String[2];
	for (int i = 0; i < 2; i++)
	    if (ret[i] == null) ret[i] = defTerVeg[i];

	return ret;
    }

    /**
     * Get the node list (which contains the border info) for a given map
     * @param full full name of map
     * @return nodelist requested
     */
    NodeList getNodeList(String full) {
	return (NodeList) main.myData.mapNodes.get(full);
    }

    /**
     * Get a map for a given name.
     * @param full name of map
     * @return scaled map
     */
    static ScaledMap getMap(String full) {
        if (full == null) return null;
	return (ScaledMap) mapList.get(full);
    }

    /**
     * Get all maps.
     * @return list of names to all maps
     */
    static Set getMapNames() { return mapList.keySet(); }

    /**
     * Get a directory link list for a given name.
     * @param full name of map
     * @return Shape array list
     */
    Hashtable getGridLinks(String full) { return (Hashtable) gridLinks.get(full); }

    /**
     * Get current map.
     */
    public MyMap getCurrent() {
        if (main.map.getCurrentName() != null)
            return (MyMap) Data.getMap(main.map.getCurrentName());
        return null;
    }

    /**
     * Set the map that is linked in the current map.
     */
    public void setCurrent(int x, int y) {
        MyMap map = getCurrent();
        if (map != null) {
            Iterator iter = map.maps.keySet().iterator();
            while (iter.hasNext()) {
                Shape sh = (Shape) iter.next();
                if (sh.contains(x,y)) {
                    main.map.setIcons((String)map.maps.get(sh), true);
                }
            }
        }
    }

    /**
     * Check for a map that is linked in the current map.
     */
    public boolean checkForSubmap(int x, int y) {
        MyMap map = getCurrent();
        float scale = main.map.getScale();
        AffineTransform trans = AffineTransform.getScaleInstance(scale, scale);
        if (map != null) {
            Iterator iter = map.maps.keySet().iterator();
            while (iter.hasNext()) {
                Shape sh = trans.createTransformedShape((Shape)iter.next());
                if (sh.contains(x,y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Load "Private.xml"
     */
    private Document loadPrivate() {
        File dir = new File(main.myPath, "Private");
        Document doc = null;
        doc = Framework.parse
            (dir.getAbsolutePath() + Framework.SEP + "Private.xml", "atlas");
        if (doc == null) {
            Element el = doc.getDocumentElement();
            el.setAttribute("name", "Private");
            el.setAttribute("vicinity", "20");
        }
        return doc;
    }

    /**
     * Load "Private.xml"
     */
    private void savePrivate(Document doc) {
        File dir = new File(main.myPath, "Private");
        File af = new File(dir.getAbsolutePath(), "Private.xml");

        // Write back
        Framework.transform(doc, new StreamResult(af), null);
    }

    /**
     * Add a Directory.
     * @param path path at which end to add
     */
    public void addDir(Object[] path) {
        // Get File
        Document doc = loadPrivate();
        if (doc == null) return;

        // Create new element
        Node node = createNode(doc, path, "dir", "New Directory");
        if (node == null) return;

        // Save file
        savePrivate(doc);

        // Reload
        main.resetTree();
        loadData();
    }

    /**
     * Create new node.
     * @param path path to new node
     * @param oname old name new node
     * @param nname name of new node
     * @return new node or null
     */
    private Element renameNode(Document doc, Object[] path, String oname, String nname) {
        // Find correct path
        Element root = doc.getDocumentElement();
        boolean found = false;
        NodeList nl = root.getChildNodes();
        Node node = null;

        for (int i = 2; i < path.length; i++) {
            found = false;
            String p = path[i].toString();
            for (int j = 0; (nl != null) && (j < nl.getLength()); j++) {
                node = nl.item(j);
                NamedNodeMap nnm = node.getAttributes();
                if (node.getNodeName().equals("dir") &&
                    nnm.getNamedItem("name").getNodeValue().equals(p)) {
                    root = (Element)node;
                    nl = node.getChildNodes();
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }

        // End of path
        found = false;
        for (int j = 0; (nl != null) && (j < nl.getLength()); j++) {
            node = nl.item(j);
            NamedNodeMap nnm = node.getAttributes();
            if (node.getNodeName().equals("dir") &&
                nnm.getNamedItem("name").getNodeValue().equals(nname)) {
                root = (Element)node;
                nl = node.getChildNodes();
                found = true;
                break;
            }
        }
        if (!found) return null;

        root.setAttribute("name", nname);
        return root;
    }

    /**
     * Create new node.
     * @param path path to new node
     * @param type type of new node
     * @param name name of new node
     * @return new node
     */
    private Element createNode(Document doc, Object[] path, String type, String name) {
        // Find correct path
        Node root = doc.getDocumentElement();
        boolean found = true;
        NodeList nl = root.getChildNodes();

        for (int i = 2; i < path.length; i++) {
            found = false;
            String p = path[i].toString();
            Node node = null;
            for (int j = 0; (nl != null) && (j < nl.getLength()); j++) {
                node = nl.item(j);
                NamedNodeMap nnm = node.getAttributes();
                if (node.getNodeName().equals("dir") &&
                    nnm.getNamedItem("name").getNodeValue().equals(p)) {
                    root = node;
                    nl = node.getChildNodes();
                    if (i == path.length - 1) found = true;
                    break;
                }
            }
            // Intermediate directory not found
            if (root != node) {
                // Add new element
                Element el = doc.createElement("dir");
                el.setAttribute("name", p);
                root.appendChild(el);
                root = el;
                nl = el.getChildNodes();
                if (i == path.length - 1) found = true;
            }
        }

        if (!found) return null;

        // Disallow duplicates (only this file/doc)
        nl = root.getChildNodes();
        for (int j = 0; (nl != null) && (j < nl.getLength()); j++) {
            Node node = nl.item(j);
            NamedNodeMap nnm = node.getAttributes();
            if (nnm != null &&
                nnm.getNamedItem("name").getNodeValue().equals(name))
                return null;
        }

        // Add new element
        Element el = doc.createElement(type);
        el.setAttribute("name", name);
        root.appendChild(el);
        return el;
    }

    /**
     * Add a Directory.
     * @param path path at which end to add
     */
    public void addMap(Object[] path) {
        // Get Options
        String name = "New Map";
        MapParams mp = new MapParams(main, name);
        if (!mp.doIt()) return;
        
        // Get File
        Document doc = loadPrivate();
        if (doc == null) return;

        // Precondition
        String[] params = mp.getParams();
        if (params[0].length() == 0 || params[5].length() == 0)
            return;

        // Create new element
        Element node = createNode(doc, path, "map", name);
        if (node == null) return;

        // Put map
        if (params[0].length() != 0)
            node.setAttribute("img", params[0]);
        if (params[1].length() != 0)
            node.setAttribute("layer1", params[1]);
        if (params[2].length() != 0)
            node.setAttribute("layer2", params[2]);
        if (params[3].length() != 0)
            node.setAttribute("layer3", params[3]);
        if (params[4].length() != 0)
            node.setAttribute("thumbnail", params[4]);
        if (params[5].length() != 0)
            node.setAttribute("scale", params[5]);
        node.setAttribute("tacticalspeed","1");

        // Save file
        savePrivate(doc);

        // Reload
        main.resetTree();
        loadData();
    }

    /** Recursive lookup of mapdir/atlas */
    static private Object recurseGet(Hashtable ht, String name) {
        while (name != null) {
            Object ret = ht.get(name);
            if (ret != null) return ret;
            int i = name.lastIndexOf("/");
            name = (i > 0 ? name.substring(0, i) : null);
        }
        return null;
    }

    /**
     * This class represents a map object, to be notified to the framework. It
     * is an inner clas, as it provided no real functionality.
     */
    class MyMap extends ScaledMap {
	/** Name of map */
	String name;

        /** Name of heightfield */
        String heightfield;

	/** Name of raster image */
	String raster;

	/** Name of atlas this map belongs to */
	String atlas;

        /** Layer names */
        ArrayList layers;

	/** scale */
	int scale;

        /** height */
        int height;

	/** tactical speed quotient */
	float speedQuot;

	/** in order to optimize we keep the last request: coordinates */
	private int lTx, lTy, lVx, lVy;

	/** in order to optimize we keep the last request: values */
	private String lT, lV;

        /** shape -> foreign map names */
        Hashtable maps;

        /** map -> transform */
        private Hashtable transforms;

	/**
	 * Constructor.
	 * @param aName name of map
	 */
	private MyMap(String aName, String aHeightfield, String aRaster, int aScale, String anAtlas, float tactSpeed, ArrayList aLayers, NodeList tree, int aHeight) {
	    name = aName;
	    atlas = anAtlas;
            if (aHeightfield != null) heightfield = atlas + "/" + aHeightfield;
            raster = aRaster;
	    scale = aScale;
            height = aHeight;
	    speedQuot = tactSpeed;
            layers = aLayers;
            String fullName = atlas + "/" + name;

            // Get all map entries
            maps = new Hashtable();
            for (int i = 0; tree != null && i < tree.getLength(); i++) {
                NamedNodeMap ca = tree.item(i).getAttributes();
                String nname = tree.item(i).getNodeName();
                if (nname.equals("area") || nname.equals("link")) {
                    String dest = ca.getNamedItem("href").getNodeValue();
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
                    if (nname.equals("area")) {
                        maps.put(shape, dest);
                    }
                    else {
                        Object o = mapList.get(dest);
                        if (o == null) {
                            o = new Hashtable();
                            mapList.put(dest, o);
                        }
                        if (o instanceof Hashtable)
                            ((Hashtable)o).put(shape, fullName);
                        else
                            ((MyMap)o).maps.put(shape, fullName);
                    }
                }
            }
            if (mapList.containsKey(fullName)) {
                // If this throws an exception, we have a double defined map.
                maps.putAll((Hashtable)mapList.get(fullName));
            }
        }

	/**
	 * Returns the terrain of the map, i.e. a logical property.
	 * @return terrain
	 */
	public String getTerrain(int x, int y) {
	    if (x == lTx && y == lTy && lT != null)
		return lT;
 	    Image icon =
                (raster != null ? getImage(raster) : getImageProper(true));
	    String ret = getTerrVegFromColour(icon, x, y, atlas + "/" + name)[0];
	    lTx = x; lTy = y; lT = ret;
	    return ret;
	}

	/**
	 * Returns the terrain of the map, i.e. a logical property.
	 * @return terrain
	 */
	public String getLandTerrain(int x, int y) {
	    if (x == lTx && y == lTy && !isWater(lT))
		return lT;
 	    Image icon =
                (raster != null ? getImage(raster) : getImageProper(true));
	    String ret = getLandTerrVegFromColour(icon, x, y, atlas + "/" + name)[0];
	    return ret;
        }

	/**
	 * Returns the vegetation of the map, i.e. a logical property.
	 * @return vegetation
	 */
	public String getVegetation(int x, int y) {
	    if (x == lVx && y == lVy && lV != null)
		return lV;
 	    Image icon =
                (raster != null ? getImage(raster) : getImageProper(true));
	    String ret = getTerrVegFromColour(icon, x, y, atlas + "/" + name)[1];
	    lVx = x; lVy = y; lV = ret;
	    return ret;
	}

	/**
	 * Returns the vegetation of the map, i.e. a logical property.
	 * @return vegetation
	 */
	public String getLandVegetation(int x, int y) {
	    if (x == lVx && y == lVy && lV != null &&
                !isWater(lT))
		return lV;
 	    Image icon =
                (raster != null ? getImage(raster) : getImageProper(true));
	    String ret = getLandTerrVegFromColour(icon, x, y, atlas + "/" + name)[1];
	    return ret;
	}

	/**
	 * Returns the scale of the map, i.e. a logical property.
	 * @return scale
	 */
	public int getScale() { return scale; }

	/**
	 * Returns the name of the map.
	 * @return name
	 */
	public String getName() { return atlas + "/" + name; }

	/**
	 * Returns the image of the active map, i.e. a physical property.
         * @param force force image return (when possible)
         * @param raw raw or modified image
	 * @return icon
	 */
	public BufferedImage getBufferedImage(boolean force, boolean raw) {
            if (!force && main.bSuppExp.isSelected()) return null;

            if (raw || !getName().equals(main.map.getCurrentName()))
                return getImageProper(true);

            return main.map.paintMap();
        }

        /**
         * Get an image by file name. Avoid loading duplicates by checking
         * whether the active or current icon already contain the wanted
         * image. May return null.
         */
        private Image getImage(String fname) {
            Image img = null;
            if (fname == null) return null;
            try {
                SoftReference ref = (SoftReference) fileRefs.get(fname);
                if (ref != null) {
                    Object o = ref.get();
                    if (o != null) return (Image) o;
                }
                String osName = Framework.osName(fname);
                if (fname.endsWith(".svg")) {
                    img = readSVG("file:" + main.myPath + osName);
                }
                else {
                    img = ImageIO.read(new File(main.myPath + osName));
                }
                if (img == null) throw new IOException("Can't decode");
                fileRefs.put(fname, new SoftReference(img));
            }
            catch (Exception e) {
                // Debug
                e.printStackTrace();
            }
            return img;
        }

        /**
         * Special SVG reader.
         */
        private Image readSVG(String url) throws Exception {
            return new SVGImage(url);
        }

	/**
	 * Returns the images of the active map, i.e. a physical property.
         * @param full use full image regardless what layering says
         * @param gui update layer switches
	 * @return icon
	 */
	public Image[] getImages() {
            main.myFrw.setBusy(true);
 	    Image[] icon = new Image[layers.size()];

            // Find layered image
            for (int i = 0; i < layers.size(); i++)
                icon[i] = getImage((String)layers.get(i));

            main.myFrw.setBusy(false);
            return icon;
        }

	/**
	 * Returns the image of this map, i.e. a physical property.
         * @param full use full image regardless what layering says
	 * @return icon
	 */
	private BufferedImage getImageProper(boolean full) {
 	    Image[] icon = getImages();

            int w = icon[0].getWidth(null);
            int h = icon[0].getHeight(null);

            int post = 0;
            for (int i = 0; i < icon.length; i++) {
                if (icon[i] == null) continue;
                if (!main.showLayer(i) && !full) continue;
                post += (1<<i);
            }

            // Try soft refs
            SoftReference ref = (SoftReference) imgRefs.get(name + "/" + post);
            if (ref != null) {
                Object o = ref.get();
                if (o != null) return (BufferedImage) o;
            }

            main.myFrw.setBusy(true);

            // Base
            BufferedImage img = new BufferedImage
                (w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = (Graphics2D) img.getGraphics();

            // Layers
            for (int i = 0; i < icon.length; i++) {
                if (icon[i] == null) continue;
                if (!main.showLayer(i) && !full) continue;
                if (icon[i] instanceof SVGImage) {
                    gc.drawImage(((SVGImage)icon[i]).getBitmap(), 0, 0, w, h, null);
                }
                else {
                    gc.drawImage(icon[i], 0, 0, w, h, null);
                }
            }
            gc.dispose();

            imgRefs.put(name + "/" + post, new SoftReference(img));
            main.myFrw.setBusy(false);

            // Final
            return img;
	}

	/**
	 * Get the part (0-1) of tactical speed that is used on this map.
	 * @return tactical speed part of total speed
	 */
	public float getTacticalSpeed() { return speedQuot; }

        /**
         * Get all reachable maps, including self.
         */
        public String[] getReachableMaps() {
            if (transforms == null) {
                transforms = new Hashtable();
                try {
                    getReachableMaps(new AffineTransform(), getName());
                }
                catch (NoninvertibleTransformException e) {
                    // Can't happen
                    e.printStackTrace();
                }
            }
            Set s = (Set) transforms.keySet();
            Object[] oa = s.toArray();
            String[] ret = new String[oa.length];
            for (int i = 0; i < ret.length; i++)
                ret[i] = (String)oa[i];
            return ret;
        }

        /**
         * Get a height field for the map. Height is given as color value: the
         * whiter, the higher.
         * @return heightfield image
         */
        public BufferedImage getHeightField() {
            Image img = getImage(heightfield);
            if (img instanceof SVGImage) return ((SVGImage)img).getBitmap();
            return (BufferedImage) img;
        }
        
        /**
         * Get the maximum height in feet of the heightfield.
         * @return maximum height
         */
        public int getHeight() { return height; }

        /**
         * Get all reachable maps recursively. See also comment on
         * getTransform.
         */
        public void getReachableMaps(AffineTransform trafo, String name) throws NoninvertibleTransformException {
            transforms.put(name, trafo);

            // Check all border regions for overlapping
            NodeList nl = main.myData.getNodeList(name);
            for (int i = 0; nl != null && i < nl.getLength(); i++) {
                // Check for borders
                if (!nl.item(i).getNodeName().equals("border"))
                    continue;
                NamedNodeMap attr = nl.item(i).getAttributes();

                // Full name of foreign map
                String mn = attr.getNamedItem("map").getNodeValue();
                Node parent = nl.item(i).getParentNode();
                while (!parent.getNodeName().equals("atlas"))
                    parent = parent.getParentNode();
                NamedNodeMap pattr = parent.getAttributes();
                String atlas = pattr.getNamedItem("name").getNodeValue();
                String fullName = atlas + "/" + mn;

                if (transforms.keySet().contains(fullName)) continue;

                // Get transform
                String trf = attr.getNamedItem("transform").getNodeValue();
                StringTokenizer tok = new StringTokenizer(trf, ";");
                float a1 = Float.parseFloat(tok.nextToken());
                float b1 = Float.parseFloat(tok.nextToken());
                float c1 = Float.parseFloat(tok.nextToken());
                float a2 = Float.parseFloat(tok.nextToken());
                float b2 = Float.parseFloat(tok.nextToken());
                float c2 = Float.parseFloat(tok.nextToken());
                AffineTransform ntrafo = new AffineTransform
                    (a1, a2, b1, b2, c1, c2);
                ntrafo.concatenate(trafo);
                getReachableMaps(ntrafo, fullName);
            }
        }

        /**
         * Get the transform that maps coords of this map to another map. May
         * yield null. We only consider the same atlas!
         * @return transform
         */
        public AffineTransform getTransform(String map) {
            if (transforms == null) getReachableMaps();
            return (AffineTransform) transforms.get(map);
        }
    }

    /**
     * This class represents an atlas object, to be notified to the
     * framework. It is an inner clas, as it provided no real functionality.
     */
    class MyAtlas extends Atlas {
	/** Name of atlas */
	String name;
	
	/** list of available maps */
	ArrayList maps;

	/**
	 * Constructor.
	 * @param name name of atlas
	 */
	private MyAtlas(String aName) {
	    name = aName;
	    maps = new ArrayList();
	}

	/**
	 * Returns the name of the map.
	 * @return name
	 */
	public String getName() { return name; }

	/**
	 * Add a map to this atlas.
	 * @param map map to add
	 */
	private void addMap(ScaledMap map) { maps.add(map); }

	/**
	 * Get the map of the specified name in this atlas. Default to null.
	 * @param name name of map queried for
	 * @return the queried map
	 */
	public ScaledMap getMap(String name) {
	    if (name == null) return null;
	    for (int i = 0; i < maps.size(); i++)
		if (name.equals(((ScaledMap)maps.get(i)).getName()))
		    return (ScaledMap)maps.get(i);
	    return null;
	}
    }
}
