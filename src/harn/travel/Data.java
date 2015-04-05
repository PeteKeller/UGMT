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
package harn.travel;

import harn.repository.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data implements IListener {
    /** Root reference */
    private Main main;

    /** data organized in tables of tables... */
    private Hashtable data;

    /** list of all possible terrain/vegetation strings */
    private ArrayList allLongStrings;

    /** External Calendar */
    static TimeFrame cal;

    /** Routes per map */
    private Hashtable routes;

    /** Hidden locations */
    private Set hidden;

    /**
     * Constructor. Turns a simple XML file into a hashmap of hashmaps.  All
     * nodes are read and their names are turned into property nodes. I.e.  <x
     * name="a"><y name="b" value="c"/><y name="d" value="e"></x> will be
     * turned into "a => {b => c, d => e}". Note that x and y are ignored.
     * @param aMain root reference
     * @param frw Framework reference for parsing the data file
     */
    Data(Main aMain) {
	main = aMain;
	allLongStrings = new ArrayList();
        routes = new Hashtable();
        hidden = new HashSet();

	// Parse the data file
	data = new Hashtable();
        Document doc = Framework.parse(main.myPath + "data.xml", "calendar");
        recurseTree(doc.getDocumentElement().getChildNodes(), data);

        // Parse all routes
        File pf = new File(main.myPath);
        String[] list = pf.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml") && name.startsWith("routes");
                }
            });
        for (int i = 0; i < list.length; i++) {
            doc = Framework.parse(main.myPath + list[i], "routes");
            extractRoutes(doc.getDocumentElement().getChildNodes());
        }
    }

    /**
     * Parses a branch on the XML-tree recursively.
     * @param tree current branch to parse
     * @param name current name on the stack
     */
    private void recurseTree(NodeList tree, Hashtable prop) {
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NodeList list = tree.item(i).getChildNodes();
            if (list != null && list.getLength() != 0) {
		Hashtable nprop = new Hashtable();
		prop.put
		    (tree.item(i).getAttributes().getNamedItem("name").
		     getNodeValue(), nprop);
                recurseTree(list, nprop);
            }
            else {
                NamedNodeMap attr = tree.item(i).getAttributes();
                if (attr != null) {
		    prop.put
			(attr.getNamedItem("name").getNodeValue(),
			 attr.getNamedItem("value").getNodeValue());
                }
            }
        }
    }

    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
	TimeFrame tf  = (TimeFrame) obj;
	if (tf != cal) cal = tf;
	main.redo(true);
    }

    /**
     * Return all weather effects.
     */
    public Set getEffects() {
        Set eff = main.getState().getTypes();
        HashSet ret = new HashSet();
        Iterator iter1 = data.keySet().iterator();
        while (iter1.hasNext()) {
            Object o1 = data.get(iter1.next());
            if (o1 instanceof Hashtable) {
                Hashtable ht1 = (Hashtable) o1;
                Iterator iter2 = ht1.keySet().iterator();
                while (iter2.hasNext()) {
                    String idx2 = (String)iter2.next();
                    if (idx2.indexOf(":") > 0) continue;
                    Object o2 = ht1.get(idx2);
                    if (o2 instanceof Hashtable) {
                        Hashtable ht2 = (Hashtable)o2;
                        Iterator iter3 = ht2.keySet().iterator();
                        while (iter3.hasNext()) {
                            String idx3 = (String) iter3.next();
                            ret.add((eff.contains(idx3) ? '+' : '-') + idx3);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns speed for given params. Speed will not always default, if
     * unknown data is delivered here (via the foreign object).
     * @param ter terrain
     * @param veg vegetation
     * @param cond weather condition, defaults to "dry"
     * @param mode travelling mode
     * @param angle travel angle to x-axis in radians
     * @return speed
     */
    public float getWaterSpeed(String ter, String veg, String cond, String mode, double angle, float mapQuot, float actSpeed) {
        int colon = cond.indexOf(':');
        if (colon < 0) return mapQuot * actSpeed;

	Hashtable hTer = (Hashtable) data.get(ter);
	if (hTer == null) return mapQuot * actSpeed;

        int owndir = (int)(angle * 180 / Math.PI);
        String wdirStr = cond.substring(0,colon);
        int wdir = Integer.parseInt(data.get(wdirStr).toString());
        return getWaterSpeed(hTer, wdir, cond.substring(colon + 1), mode, owndir);
    }

    /**
     * Returns speed for given params. Speed will not always default, if
     * unknown data is delivered here (via the foreign object).
     * @param ter terrain
     * @param veg vegetation
     * @param cond weather condition, defaults to "dry"
     * @param mode travelling mode
     * @param angle travel angle to x-axis in radians
     * @return speed
     */
    public float getLandSpeed(String ter, String veg, String cond, String mode, double angle, float mapQuot, float actSpeed) {
	Hashtable hTer = (Hashtable) data.get(ter);
	if (hTer == null) return mapQuot * actSpeed;

        Hashtable hVeg = (Hashtable) hTer.get(veg);
        if (hVeg == null) return mapQuot * actSpeed;

        Hashtable hCond = (Hashtable) hVeg.get(cond);
        if (hCond == null) hCond = (Hashtable) hVeg.get("dry");
        if (hCond == null) return .0f;
        String speed = (String)hCond.get(mode);
        if (speed == null) return .0f;
        float ret = Float.parseFloat(speed);

        return (1 - mapQuot) * ret + mapQuot * actSpeed;
    }

    /**
     * Returns water travel speed for given params. 
     * @param ter terrain table
     * @param wdir wind direction in degrees
     * @param wspeed wind speed
     * @param owndir travel angle to x-axis in degrees
     * @return speed
     */
    private float getWaterSpeed(Hashtable ter, int wdir, String wspeedStr, String mode, int owndir) {
	// Angle to wind
	int ldir = (360 + wdir - owndir) % 360;
	if (ldir > 180) ldir = 360 - ldir;

	// These two delimit our own direction
	String lub = null;
	String glb = null;

	Iterator i = ter.keySet().iterator();
	while (i.hasNext()) {
	    String key = (String)i.next();
	    String[] parts = key.split(":");
	    if (parts.length != 2) return .0f;
	    if (wspeedStr.equals(parts[1])) {
		int a = Integer.parseInt(parts[0]);
		if ((lub == null || Integer.parseInt(lub) > a) && a > ldir)
		    lub = parts[0];
		if ((glb == null || Integer.parseInt(glb) < a) && a < ldir)
		    glb = parts[0];
	    }
	}

	// Take average. d0, d1 are the bounds angle distance from the local
        // angle. a0, a1 are the speeds of the bounds.
	float a0 = .0f, a1 = .0f;
	float d0 = .0f, d1 = .0f;

	String[] modl = mode.split(":");
	if (modl.length != 3) return .0f;

	if (lub != null) {
	    // Traverse tables to speed entry for least upper bound
	    Hashtable ht = (Hashtable)ter.get(lub + ":" + wspeedStr);
	    ht = (Hashtable) ht.get(modl[2]);
	    if (ht != null) {
		String a0Str = (String) ht.get(modl[0] + ":" + modl[1]);
		if (a0Str != null) {
		    a0 = Float.parseFloat(a0Str);
		    d0 = Math.abs(Integer.parseInt(lub) - ldir);
		}
	    }
	}
	if (glb != null) {
	    // Traverse tables to speed entry for greatest lower bound
	    Hashtable ht = (Hashtable)ter.get(glb + ":" + wspeedStr);
	    ht = (Hashtable) ht.get(modl[2]);
	    if (ht != null) {
		String a1Str = (String) ht.get(modl[0] + ":" + modl[1]);
		if (a1Str != null) {
		    a1 = Float.parseFloat(a1Str);
		    d1 = Math.abs(Integer.parseInt(glb) - ldir);
		}
	    }
	}

	float ret = (d0 == 0 ? a0 : (a0 * d1 + a1 * d0)/(d0 + d1));
	if (lub == null) ret = a1;
	if (glb == null) ret = a0;

	return ret;
    }

    /**
     * Get all string combinations of terrain/vegetation in order to determine
     * button widths at the GUI. (The method name is kept generic, other
     * strings may get longer in the future.)
     * @return long string list
     */
    public ArrayList getAllStrings() { return allLongStrings; }

    /**
     * Utility. Pretty prints a date.
     * @param date date to pretty print
     */
    static public String stringDate(long date) {
	if (cal != null) return cal.datetime2string(date);
	return "-";
    }

    /**
     * Utility. Pretty prints a time.
     * @param time time to pretty print
     */
    static public String stringTime(long time) {
	if (cal != null) return cal.time2string(time);
	return "-";
    }

    /**
     * Returns speed factor for given mode. Defaults to 1.25.
     * @param mode travelling mode
     * @return speed factor
     */
    public float getForced(String mode) {
	String factor = (String) data.get(mode);
	if (factor == null) return 1.25f;
	return Float.parseFloat(factor);
    }

    /**
     * Get the routes for a named map.
     * @param map (full) name of map
     */
    public ArrayList getRoutes(String name) {
        ScaledMap map = main.map.getMap(name);
        if (map == null) return new ArrayList();
        String[] names = map.getReachableMaps();
        ArrayList ret = new ArrayList();
        for (int i = 0; i < names.length; i++) {
            ArrayList al = (ArrayList) routes.get(names[i]);
            if (al != null)
                ret.addAll(al);
        }
        return ret;
    }
    
    /**
     * Extract the maps from the document tree.
     * @param tree doc tree to extract from
     */
    private void extractRoutes(NodeList tree) {
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap attr = tree.item(i).getAttributes();
            if (tree.item(i).getNodeName().equals("route")) {
                String map = attr.getNamedItem("map").getNodeValue();
                ArrayList al = (ArrayList) routes.get(map);
                if (al == null) {
                    al = new ArrayList();
                    routes.put(map, al);
                }
                al.add(new Route(attr));
            }
            if (tree.item(i).getNodeName().equals("hide")) {
                hidden.add(attr.getNamedItem("name").getNodeValue());
            }
        }
    }

    /** Is Location hidden */
    public boolean isHidden(String loc) { return hidden.contains(loc); }

    /** Inversion method */
    public Route invert(Route route) {
        return new Route(route);
    }

    public Route getReflexive(String aLoc1, String aLoc2, int[] anX, int[] anY) {
        return new Route(aLoc1, aLoc2, anX, anY);
    }

    /**
     * Class containing routing information.
     */
    class Route {
        /** Endpoints of route */
        private String loc1, loc2;
        /** coordinates including endpoints */
        private int[] x, y;
        /** distance bewteen 1 and 2 */
        private int distance;
        /** Reflexive constructor */
        Route(String aLoc1, String aLoc2, int[] anX, int[] anY) {
            loc1 = aLoc1;
            loc2 = aLoc2;
            distance = 1;
            x = anX;
            y = anY;
        }
        /** Inversion Constructor */
        Route(Route route) {
            loc1 = route.loc2;
            loc2 = route.loc1;
            distance = route.distance;
            x = new int[route.x.length];
            y = new int[route.y.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = route.x[x.length - i - 1];
                y[i] = route.y[y.length - i - 1];
            }
        }
        /** Constructor */
        Route(NamedNodeMap attr) {
            String map = attr.getNamedItem("map").getNodeValue();
            loc1 = map + "//" + attr.getNamedItem("loc1").getNodeValue();
            loc2 = map + "//" + attr.getNamedItem("loc2").getNodeValue();
            String[] p = attr.getNamedItem("coords").getNodeValue().split(",");
            x = new int[p.length/2];
            y = new int[p.length/2];
            if (distance == 0)
                for (int i = 0; i < x.length; i++) {
                    x[i] = Integer.parseInt(p[2*i]);
                    y[i] = Integer.parseInt(p[2*i + 1]);
                    if (i > 0)
                        distance += (int) Math.sqrt
                            ((x[i] + x[i-1])*(x[i] + x[i-1]) +
                             (y[i] + y[i-1])*(y[i] + y[i-1]));
                }
        }
        /** Getter */
        public int x(int i) {
            if (i == -1) return x[x.length -1];
            return x[i];
        }
        /** Getter */
        public int y(int i) {
            if (i == -1) return y[y.length -1];
            return y[i];
        }
        /** Getter */
        public int length() { return (x != null) ? x.length : 0; }
        /** Getter */
        public String loc1() { return loc1; }
        /** Getter */
        public String loc2() { return loc2; }
        /** Getter */
        public int distance() { return distance; }
    }
}
