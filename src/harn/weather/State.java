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
package harn.weather;

import harn.repository.*;
import java.io.*;
import java.util.Hashtable;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;
import java.awt.image.BufferedImage;
import java.awt.*;

/**
 * State object. This represents the active weather state. It therefore
 * listens to "Groups" and updates the weather accordingly.
 * @author Michael Jung
 */
public class State extends Weather implements IListener {
    /** reference to the act object */
    private CharGroup act;

    /** Root reference */
    private Main main;

    /** current display time */
    private long tDisplay = Long.MIN_VALUE;

    /** indicates whether anything is to save */
    private boolean dirty = false;

    /** find out whether this object changed in "inform" */
    private boolean changed;

    /** Weather list already determined */
    private Hashtable availWeath;

    /** Representation of the state.xml file (and changes) */
    private Document stateDoc;

    /** yesterdays to show */
    private int back;

    /** tomorrows to show */
    private int front;

    /** map of all maps */
    private Hashtable maps;

    /**
     * Constructor. Loads the state of the weather (predetermined). This is
     * not a simple hasmap of hashmaps.
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
	back = Integer.parseInt(main.myProps.getProperty("yesterdays"));
	front = Integer.parseInt(main.myProps.getProperty("tomorrows"));
	maps = new Hashtable();
        availWeath = new Hashtable();

        main.myFrw.listen(new MapListener(), ScaledMap.TYPE, null);
        main.myFrw.announce(this);
        stateDoc = Framework.parse(main.myPath + "state.xml", "state");
        availWeath = loadFromDoc();
    }

    /**
     * Get date for active group
     * @return date
     */
    long getDate() { return (act != null ? act.getDate() : 0); }

    /**
     * Add time t to the current date of the active group.
     * @param t time to add
     */
    void addDate(long t) {
	if (act != null) {
	    long date = act.getDate() + t;
	    act.setLocation(act.getMap(), act.getX(), act.getY(), date);
	}
    }

    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
	CharGroup cg  = (CharGroup) obj;
	if (cg.isActive()) {
	    if (act == null || !cg.getName().equals(act.getName())) act = cg;
	    // Get 0:00 of today
	    long t0 = act.getDate() - act.getDate()%main.DAY;

	    // Assume weather doesn't change
	    changed = false;

	    if (Data.existsTimeframe()) {
		// If the new day requires a shift of old weather
		if (Math.abs(tDisplay - t0) >= main.DAY) {
		    WeatherDisplay w = main.getDisplay();
		    w.wList = new WeatherUnit[(front + back)*6];
		    for (int i = 0; i < (front + back); i++) {
			for (int j  = 0; j < 6; j++) {
                            w.wList[6*i + j] = null;
                            if (t0 + ( (i - back) * 6 + j)*main.WATCH > 0)
                                w.wList[6*i + j] = getWeather
                                    (t0 + ( (i - back) * 6 + j)*main.WATCH);
			}
		    }
		    tDisplay = t0;
		}
	    }
	    if (changed) main.myFrw.announce(this);
	    main.redo(true);
	}
    }

    /**
     * Access to the private reference object
     * @return active group
     */
    CharGroup getActive() { return act; }

    /**
     * Returns weather unit.
     * @return current weather unit
     */
    WeatherUnit getWeather() {
	return getWeather(getDate(), 3); }

    /**
     * Returns weather unit.
     * @param time time for weather unit
     * @return weather unit valid for time
     */
    private WeatherUnit getWeather(long time) {
	return getWeather(time, 3);
    }

    /**
     * Returns weather unit for a given time. Also the depth with which to
     * search in the past for a weather unit already existing to build upon is
     * given. If none is found, a random weather unit is created.
     * @param time time for weather unit
     * @param stack depth to search backwards for existing weather 
     * @return weather key string
     */
    private WeatherUnit getWeather(long time, int stack) {
        if (time < 0) return null;
	String sidx = Data.stringDateTime(Main.WATCH*(time/Main.WATCH));
        if (sidx == null) return null; // No calendar present
	WeatherUnit weath = (WeatherUnit) availWeath.get(sidx);
	// Weather unit isn't there yet.
	if (weath == null) {
	    if (stack > 0) {
		WeatherUnit old = getWeather(time - main.WATCH, stack - 1);
                if (old != null)
                    weath = new WeatherUnit(old, time);
                else
                    weath = new WeatherUnit(time);
	    }
	    else {
		weath = new WeatherUnit(time);
	    }
	    changed = true;
	    setDirty(true);
	    availWeath.put(sidx, weath);

	    // Get parent in DOM document
	    Node parent = stateDoc.getDocumentElement();

	    // Put into DOM document
	    Element nn = stateDoc.createElement("date");
	    nn.setAttribute("name", Data.stringDateTime(time));
	    weath.setAttributes(nn);
	    parent.appendChild(nn);
	}
	return weath;
    }

    /**
     * Weather IF method. We provide a singleton, so this is constant.
     * @return singleton name
     */
    public String getName() { return "TheWeather"; }

    /**
     * Weather IF method. We provide a singleton, so this is constant.
     * @return true
     */
    public boolean isActive() { return true; }

    /**
     * Weather IF method. We need to know whether we are in water or not at
     * this location.
     * @param map map name (unused)
     * @param x coordinate on map (unused)
     * @param y coordinate on map (unused)
     * @param date time for weather
     * @return weather condition
     */
    public String condition(String map, int x, int y, long date) {
	WeatherUnit weath = getWeather(date);
        if (weath == null) return "dry"; // No calendar present
	// Water is handled different, only wind counts
	ScaledMap local = (ScaledMap)maps.get(map);
	if (local != null && local.getTerrain(x,y).equals("water")) {
	    return windCondition(local.getVegetation(x,y), weath);
	}
	
	Data.HashList list = Data.getCondList();
	for (int i = 0; i < list.keySet().size(); i++) {
	    String n = list.getKey(i);
	    int thresh = ((Data.Condition)list.get(n)).threshhold;
	    String lvl = ((Data.Condition)list.get(n)).lvlname.toString();
            if ((weath.levels.get(lvl) != null) &&
                ((Integer)weath.levels.get(lvl)).intValue() >= thresh)
		return n;
	}

	// Default
	return "dry";
    }

    /**
     * Weather IF method.
     * @param map map name (unused)
     * @param x coordinate on map (unused)
     * @param y coordinate on map (unused)
     * @param date time for weather
     * @return weather condition
     */
    public String event(String map, int x, int y, long date) {
	WeatherUnit wu = getWeather(date);
        if (wu != null) return wu.event;
        return null;
    }

    /**
     * Wind condition helper. Used with water only.
     * @param type water type
     * @param weath general weather
     * @return wind condition
     */
    public String windCondition(String type, WeatherUnit weath) {
	int w = weath.speedindex;

	// low wind
	if (type.equals("inland") && w <= 3 ||
	    type.equals("coastal") && w <= 2 ||
	    type.equals("open") && w <= 1 ||
	    weath.event.equals("fog"))
	    return weath.winddir + ":" + weath.windspeed.substring(0,1);
	// high wind
	if (type.equals("inland") && w >= 3 ||
	    type.equals("coastal") && w >= 2 ||
	    type.equals("open") && w >= 1)
	    return weath.winddir + ":" + weath.windspeed.substring(2,3);

	// average wind
	int le = Integer.parseInt(weath.windspeed.substring(0,1));
	int he = Integer.parseInt(weath.windspeed.substring(2,3));
	return weath.winddir + ":" + ((le + he)/2);
    }

    /**
     * Add to / Subtract from condition level.
     * @param date time for which to change the condition level.
     * @param lvl number to add (may be negative)
     */
    void setCond(String name, long date, int lvl) {
	WeatherUnit weath1 = getWeather(date);
	int old = ((Integer)weath1.levels.get(name)).intValue();
	int nyu = old + lvl;
	if (nyu < 0) nyu = 0;
	if (old != nyu) {
	    weath1.levels.put(name,new Integer(nyu));
	    // Cascade down
	    setCond(name, date);
	    main.myFrw.announce(this);
	    main.redo(false);
	}
    }

    /**
     * Calculate condition level
     * @param date time for which to calculate condition level
     */
    private void setCond(String name, long date) {
	// Get data
	WeatherUnit weath1 = getWeather(date);
	WeatherUnit weath2 = getWeather(date + main.WATCH);
	if (weath2 == null) return;

	// Calculate 
	int val = weath2.calcCond(name, weath1, date);
	if (val == ((Integer)weath2.levels.get(name)).intValue()) return;
	weath2.levels.put(name, new Integer(val));

	// Cascade down
	setCond(name, date + main.WATCH);
    }

    /**
     * Method to calc time distance on board
     * @param x coordinate on weather display
     * @param y coordinate on weather display
     * @return time distance
     */
    long calcDist(int x, int y) {
	WeatherDisplay wd = main.getDisplay();
	x = wd.getWeatherX(x);
	y = wd.getWeatherY(y);
	int w = wd.getWeatherWidth();
	int h = wd.getWeatherHeight();

	long ret = -1;
	// Point must be in bounds
	if (x >= 0 && y >= 0 && x <= w && y <= h) {
	    int dy = (front + back) * y / h - back;
	    int dx = (int)(x - w * (getDate() % main.DAY) / main.DAY);

	    // This may return negative
	    ret = (long)(dy * main.DAY + dx * main.DAY / w);
	}

	// Show distance
	if (ret > 0)
	    main.bDist.setText("Distance: " + Data.stringTime(ret));
	else
	    main.bDist.setText("Distance: --:--");

	return ret;
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * is accessible by this method.
     * @return firty flag
     */
    boolean getDirty() { return dirty; }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled
     * @return firty flag
     */
    private void setDirty(boolean flag) {
	dirty = flag;
	main.bSave.setEnabled(dirty);
    }

    /**
     * Use this method to save the data in this object to file. AFter saving
     * this object is "clean" once more.
     */
    void save() {
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
        setDirty(false);
    }
    
    /**
     * This method drops all or invisible weather entries, if all=false those
     * that are older than "back" days or further than "front".
     * @param all all or just invisible?
     */
    void dump(boolean all) {
        // For XML update
        Node tree = stateDoc.getDocumentElement();
        NodeList children = tree.getChildNodes();

        long first = 0;
        long end = 0;
        if (act != null) {
            // first & last date to keep
	    first = act.getDate() - back * main.DAY;
	    end = act.getDate() + front * main.DAY;
        }

        for (int i = 0; i < children.getLength(); i++) {
            NamedNodeMap attr = children.item(i).getAttributes();
            if (attr != null) {
                String name = attr.getNamedItem("name").getNodeValue();
                if (first > Data.string2date(name) ||
                    end < Data.string2date(name) || all) {
                    tree.removeChild(children.item(i));
                    availWeath.remove(name);
                }
            }
        }

        // Redraw
        tDisplay = Integer.MIN_VALUE;
        inform(act);
    }

    /**
     * Load the availWeath list from the XML doxument (reflecting the current
     * state).
     * @return hashtable containing the weather entries
     */
    private Hashtable loadFromDoc() {
	Hashtable tab = new Hashtable();

	NodeList tree = stateDoc.getDocumentElement().getChildNodes();
	for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    NamedNodeMap attr = tree.item(i).getAttributes();
	    if (attr != null) {
		String name = attr.getNamedItem("name").getNodeValue();
		tab.put(name, new WeatherUnit(attr));
	    }
	}
	return tab;
    }

    /**
     * Provide a weather export in image form. The provider decides size and
     * contents of image.
     * @return image
     */
    public BufferedImage getExport() {
        int ebackw = Integer.parseInt
            (main.myProps.getProperty("export.back"));
        int efrontw = main.forecast;
        main.display.setBounds(ebackw, efrontw);

        Dimension dim = main.display.getPreferredSize(); 
        BufferedImage img = new BufferedImage
            (dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        Graphics gc = img.getGraphics();
        main.display.paintComponent(gc);
        int d[] = main.display.getExportSize(); 
        gc.dispose();

        main.display.resetBounds();
        return img.getSubimage(d[0], d[1], d[2], d[3]);
    }

    /**
     * This class listens to all maps and registers them locally for later
     * reference.
     */
    private class MapListener implements IListener {
	public void inform(IExport obj) {
	    maps.put(((ScaledMap)obj).getName(), obj);
	}
    }
}
