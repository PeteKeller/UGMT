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
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import rpg.*;

/**
 * Class that represents the Map.  It handles the GUI and the cursor icon that
 * displays the group's position. It is a proxy for the foreign map object.
 * @author Michael Jung
 */
public class MapHolder extends JComponent implements IListener {
    /** Active map icon */
    private BufferedImage mapIcon;

    /** reference to foreign map object */
    private ScaledMap mapObj;

    /** Active marker */
    private ImageIcon mark;

    /** root reference */
    private Main main;

    /** Collection of all atlas object */
    private ArrayList atlas;

    /** Variable for not redrawing while auto travelling */
    protected boolean autotravel = false;

    /**
     * Constructor. Load mark = group cursor.
     * @param props properties
     * @param main root reference
     */
    MapHolder(Properties props, Main aMain) {
	super();
	main = aMain;
	atlas = new ArrayList();
	mark = new ImageIcon(main.myPath + props.getProperty("mark"));
    }
    
    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
	atlas.add((Atlas) obj);
	if (mapObj == null) {
	    CharGroup cg = main.getState().getActive();
	    if (cg != null) {
		setNewMap(cg.getMap());
		main.redo(false);
	    }
	}
        main.setRoutes(getMapName());
    }

    /**
     * Returns our width.
     * @return width     
     */
    int getMyWidth() {
	if (mapIcon != null) return mapIcon.getWidth();
	return 0;
    }

    /**
     * Returns our height.
     * @return height
     */
    int getMyHeight() {
	if (mapIcon != null) return mapIcon.getHeight();
	return 0;
    }

    /**
     * Callback for the system GUI manager. Redraw map and mark/cursor.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) {
        if (autotravel) return;
	// Draw map first (if present)
	if (mapIcon != null) {
	    int w = mapIcon.getWidth();
	    int h = mapIcon.getHeight();
	    g.drawImage(mapIcon, 0, 0, w, h, this);
	    setPreferredSize(new Dimension(w, h));
	}

	// Draw cursor on top
	int x = main.getState().getX();
	int y = main.getState().getY();
	int w = mark.getIconWidth();
	int h = mark.getIconHeight();
	g.drawImage(mark.getImage(), x - w/2, y - h/2, w, h, this);
	revalidate();
    }

    /**
     * Returns the terrain of the foreign map object, i.e. a logical
     * property. Since this object is a listener to the foreign map, it will
     * handle all internal request to it.
     * @return terrain
     */
    String getTerrain(int x, int y) { return mapObj.getTerrain(x, y); }

    /**
     * Returns the terrain of the foreign map object, i.e. a logical
     * property. Since this object is a listener to the foreign map, it will
     * handle all internal request to it.
     * @return terrain
     */
    String getNWTerrain(int x, int y) { return mapObj.getLandTerrain(x, y); }

    /**
     * Returns the vegetation of the foreign map object, i.e. a logical
     * property. Since this object is a listener to the foreign map, it will
     * handle all internal request to it.
     * @return vegetation
     */
    String getVegetation(int x, int y) { return mapObj.getVegetation(x, y); }

    /**
     * Returns the vegetation of the foreign map object, i.e. a logical
     * property. Since this object is a listener to the foreign map, it will
     * handle all internal request to it.
     * @return vegetation
     */
    String getNWVegetation(int x, int y) { return mapObj.getLandVegetation(x, y); }

    /**
     * Returns the scale of the foreign map object, i.e. a logical
     * property. Since this object is a listener to the foreign map, it will
     * handle all internal request to it.
     * @return scale
     */
    int getScale() { return mapObj.getScale(); }

    /**
     * Get a map form atlas.
     */
    ScaledMap getMap(String name) {
	// Find the map in all atlanti.
	for (int i = 0; i < atlas.size(); i++) {
	    ScaledMap nm = ((Atlas)atlas.get(i)).getMap(name);
	    if (nm != null) return nm;
	}
        return null;
    }

    /**
     * This method sets a new map when the active group has changed the
     * map. During startup we must remain aware that a map may not be found
     * yet.
     * @param aMap new map name
     */
    void setNewMap(String aMap) {
	// Find the map in all atlanti.
	for (int i = 0; i < atlas.size(); i++) {
	    ScaledMap nm = ((Atlas)atlas.get(i)).getMap(aMap);
	    if (nm != null) {
		mapObj = nm;
		break;
	    }
	}

	// Adapt the display and redraw
	if (mapObj != null) {
	    mapIcon = mapObj.getBufferedImage(true, true);
	    int w = mapIcon.getWidth();
	    int h = mapIcon.getHeight();
	    setPreferredSize(new Dimension(w, h));
	}
    }

    /**
     * Get the map id of the map object. Not of the active group, which is
     * handled elsewhere.
     * @return map name
     */
    String getMapName() { return (mapObj != null ? mapObj.getName() : null); }

    /**
     * Get the part (0-1) of tactical speed that is used on this map. A
     * travelling group has inherent speed. Sometimes the group speed is not
     * adequately covered by terrain and vegetation, mostly tactical
     * environs. Not be used for water travel.
     * @return tactical speed part of total speed
     */
    float getSpeed() {
	return (mapObj != null ? mapObj.getTacticalSpeed() : .0f);
    }

    /**
     * A general compound methods, updating fields and map
     */
    void move(int x, int y, long delta) {
        main.getState().addMoved(delta);
        main.getState().addDateSetLocation(x, y, delta);
        repaint();
    }
}
