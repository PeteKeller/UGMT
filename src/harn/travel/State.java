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
import java.text.*;
import org.w3c.dom.*;
import javax.swing.*;
import rpg.*;
import java.io.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * State object. This represents the state of the active group and its travel
 * corollaries, such as speed.  It therefore listens to "Groups" and forwards
 * any changes to that object. It updates all simple GUI objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** reference to the act object */
    private CharGroup act;

    /** Last location of act group */
    private int actX, actY;
    private long actT;
    private String actM;

    /** Root reference */
    private Main main;

    /** Current Speed */
    private float speed;

    /** Moved without rest */
    private long moved;

    /** active travel modes */
    private HashSet modes;

    /** Set of weather stalling features */
    private Set suppress;

    /** Current encounters */
    private Set encounters;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
	modes = new HashSet();
        suppress = new HashSet();
        encounters = new HashSet();
        Document dataDoc = Framework.parse(main.myPath + "state.xml", "travel");
        loadDoc(dataDoc.getDocumentElement());
    }

    /**
     * Method to load the state.
     * @param doc (XML) document to load
     */
    private void loadDoc(Node doc) {
        NodeList tree = doc.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
            if (!tree.item(i).getNodeName().equals("suppress")) continue;
            String[] types =
                tree.item(i).getAttributes().getNamedItem("list").getNodeValue().split(",");
            suppress.addAll(Arrays.asList(types));
        }
    }

    /** Get all weather stallers */
    public Set getTypes() { return suppress; }

    /** Add a weather staller. */
    public void addType(String name, boolean val) {
        if (val) suppress.add(name);
    }

    /** Add a weather staller. */
    public void toggleType(String name) {
        if (suppress.contains(name))
            suppress.remove(name);
        else
            suppress.add(name);
    }

    public boolean activeEncounter() {
        if (!getTypes().contains("Encounters")) return false;
        return !encounters.isEmpty();
    }

    /**
     * Get X coordinate for active group
     * @return x coordinate
     */
    int getX() { return (act != null ? act.getX() : 0) ; }

    /**
     * Get Y coordinate for active group
     * @return y coordinate
     */
    int getY() { return (act != null ? act.getY() : 0); }

    /**
     * Get date for active group
     * @return date
     */
    long getDate() { return (act != null ? act.getDate() : 0); }

    /**
     * Get map for active group
     * @return date
     */
    String getMap() { return (act != null ? act.getMap() : null); }

    /**
     * Add time t to the current date of the active group.
     * @param t time to add
     */
    void addDate(long t) {
	addDateSetLocation(act.getX(), act.getY(), t);
    }

    /**
     * Add time t to the current date of the act group and change location.
     * @param x new x coordinate for active group
     * @param y new y coordinate for active group
     * @param t time to add
     */
    void addDateSetLocation(int x, int y, long t) {
	if (act != null) {
	    long date = act.getDate() + t;
	    act.setLocation(act.getMap(), x, y, date);
	}
    }

    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
        if (obj instanceof CharGroup) {
            CharGroup cg  = (CharGroup) obj;
            if (cg.isActive()) {
                if (cg != act) {
                    clearMoved();
                    act = cg;
                    main.contTravel.setEnabled(false);
                }
                if (actX == act.getX() &&
                    actY == act.getY() && 
                    actM.equals(act.getMap()) && 
                    actT != act.getDate())
                    clearMoved();

                actX = act.getX();
                actY = act.getY();
                actT = act.getDate();
                actM = act.getMap();

                MapHolder mh = main.getMap();
                if (mh != null &&
                    !getMapName().equals(mh.getMapName())) {
                    mh.setNewMap(getMapName());
                    main.setRoutes(getMapName());
                }
                modes = new HashSet();
                String[] ms = getModes();
                for (int i = 0; i < ms.length; i++) {
                    modes.add(ms[i]);
                }
                main.redo(true);
            }
        }
        if (obj instanceof TimeEvent) {
            TimeEvent e = (TimeEvent) obj;
            if (e.isActive() && e.isValid() && e.type().equals("Encounter"))
                encounters.add(e);
            else
                encounters.remove(e);
        }
    }

    /**
     * Get the map id (not the map object! The map object is held elsewhere.)
     * as present in the active group.
     * @return map name
     */
    String getMapName() { return (act != null ? act.getMap() : ""); }

    /**
     * Modify the moved (time) field. Adjust GUI.
     * @param delta time to add
     */
    void addMoved(long delta) {
	moved += delta;
	main.bMove.setText("Move: " + Data.stringTime(moved));
    }

    /**
     * Clear the moved (time) field. Adjust GUI.
     */
    void clearMoved() {
	addMoved(-moved);
    }
    
    /**
     * Return the active group for direct access.
     */
    CharGroup getActive() { return act; }

    /**
     * Calculate the (time) distance from x0,y0 to current coordinates on the
     * active map. It also updates the relevant buttons.
     * @param x0 requested destination x coordinate
     * @param y0 requested destination y coordinate
     * @param free movement type (free, noemal, forced)
     * @return the distance in minutes
     */
    int calcDist(int x0, int y0, String free) {
	// Get all relevant data
	String ter = main.getMap().getTerrain(x0, y0);
	String terNW = main.getMap().getNWTerrain(x0, y0);
	String veg = main.getMap().getVegetation(x0, y0);
	String vegNW = main.getMap().getNWVegetation(x0, y0);
	String cond = main.getWeather().getCondition(x0, y0);
	float mapQuot = main.getMap().getSpeed();
	float actSpeed = getActive().getSpeed();

	// Update the button
	main.bTerr.setText(ter + " & " + veg);

	// Distance in leagues
	int dx = x0 - getX();
	int dy = y0 - getY();
	double ret = Math.sqrt(dx*dx + dy*dy)/main.getMap().getScale();

	// Get speed
	float sp;
	if (main.bSpeed.getText().equals("Auto speed")) {
	    sp = calcSpeed
		(ter, terNW, veg, vegNW, cond, free, Math.atan2(-dy, dx), mapQuot, actSpeed);
	}
	else {
            try {
                sp = NumberFormat.getInstance().parse
                    (main.eSpeed.getText()).floatValue();
            }
            catch (ParseException e) {
                // Cannot happen
                throw new NumberFormatException(main.eSpeed.getText());
            }
	}

	// Calc distance in minutes
	if (sp > 0) ret = (ret / sp * 4 * 60); else ret = -1;

	// Update buttons
	if (ret < 0)
	    main.bDist.setText("Distance: --:--");
	else
	    main.bDist.setText("Distance: " + Data.stringTime((int)ret));

	if (sp != Float.POSITIVE_INFINITY)
	    main.eSpeed.setValue(new Float(sp));

	return (int) ret;
    }

    /**
     * Calculates the speed given terrain/vegetation and travel modes. The
     * common speed is the minimum of all used modes. The angle is interesting
     * for sea travel.
     * @param ter terrain
     * @param veg vegetation
     * @param cond weather condition
     * @param free travel type (free, normal, forced)
     * @param angle angle to x-axis
     * @return speed
     */
    private float calcSpeed(String ter, String terNW, String veg, String vegNW, String cond, String free, double angle, float mapQuot, float actSpeed) {
	float wsp = Float.POSITIVE_INFINITY;
	float lsp = Float.POSITIVE_INFINITY;
	Iterator iter = modes.iterator();

	// If there is no mode chosen, don't move
	if (!iter.hasNext()) wsp = lsp = 0;

	while (iter.hasNext()) {
	    String mode = (String)iter.next();

	    // Get the speed modifier, INFINITY = free move
	    float spm = Float.POSITIVE_INFINITY;
	    if (free.equals("Forced")) {	    
		spm = main.myData.getForced(mode);
	    }
	    if (free.equals("Normal")) {
		spm = 1;
	    }

	    // Get Speed if necessary
	    if (spm != Float.POSITIVE_INFINITY) {
		float asp;
                // Determine water and land separately
                int colon = mode.indexOf(':');
                if (colon > 0) {
                    asp = main.myData.getWaterSpeed
                        (ter, veg, cond, mode, angle, mapQuot, actSpeed);
                    wsp = Math.min(asp * spm, wsp);
                }
                else {
                    asp = main.myData.getLandSpeed
                        (terNW, vegNW, cond, mode, angle, mapQuot, actSpeed);
                    lsp = Math.min(asp * spm, lsp);
                }
	    }
	}
        if (wsp == Float.POSITIVE_INFINITY) return lsp;
        if (lsp == Float.POSITIVE_INFINITY) return wsp;
	return Math.max(wsp, lsp);
    }

    /**
     * Use this method to save the data in this object to file. After saving,
     * this object is "clean" once more.
     */
    public void save() {
        // Create technical objects
        Document doc = Framework.newDoc();

        // Root
        Node root = doc.createElement("travel");
        doc.insertBefore(root, doc.getLastChild());

        // List
        String list = "";
        Iterator iter = suppress.iterator();
        while (iter.hasNext())
            list += iter.next() + ",";

        if (list.length() > 0) {
            Element eltypes = doc.createElement("suppress");
            eltypes.setAttribute("list", list.substring(0,list.length() - 1));
            root.insertBefore(eltypes, null);
        }

        // Write the document to the file
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(doc, new StreamResult(file), null);
    }

    /**
     * Obtains the mode list for the active group.
     * @return active group travel mode list
     */
    String[] getModes() {
	if (act != null) return act.getTravelModes();
	return null;
    }
}
