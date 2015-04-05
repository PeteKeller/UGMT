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

import java.util.*;
import org.w3c.dom.*;
import rpg.DiceRoller;
import rpg.Framework;

/**
 * Class that represents a weather unit. It enhances a compound (as in the
 * data class) with mud and snow level. It also provides lots of convenience
 * methods.
 * @author Michael Jung
 */
class WeatherUnit {
    /** season */
    private String season;

    /** index as in compound */
    private int idx;

    /** compound fields */
    String event;
    String temperature;
    String winddir;
    String windspeed;
    int speedindex;
    String clouds;

    /** condition level fields */
    Hashtable levels;

    /**
     * Constructor. Lets everything default.
     * @param time
     */
    WeatherUnit(long time) {
	String comp = Data.getSeason(time) + "." + DiceRoller.d(20);
	init(comp, new Hashtable(), time);
    }

    /**
     * Constructor. Uses an entry from a data file as initialisation. Used by
     * the state class when loading.
     * @param attrList values in different form
     */
    WeatherUnit(NamedNodeMap attrList) {
	String comp = attrList.getNamedItem("compound").getNodeValue();

	String[] list = Data.getLevelnames();
	Hashtable ht = new Hashtable();

	// Check them all
	for (int i = 0; i < list.length; i++) {
	    Node ns = attrList.getNamedItem(list[i]);
	    int val = (ns != null ? Integer.parseInt(ns.getNodeValue()) : 0);
	    ht.put(list[i], new Integer(val));
	}

	// Time
	Node nt = attrList.getNamedItem("name");
	long time = (nt != null ? Data.string2date(nt.getNodeValue()) : 0);

	init(comp, ht, time);
    }

    /**
     * Constructor. Uses a previous weather unit as base for random
     * generation. No check is made, if the new time is actually the one
     * following the old unit.
     * @param old previous weather
     * @param time time of new unit
     */
    WeatherUnit(WeatherUnit old, long time) {
	int idx = old.idx;
	switch (DiceRoller.d(10)) {
	    case 0: idx = 1 + (idx + 18) % 20; break;
	    case 7: case 8:  idx = 1 + idx % 20; break;
	    case 9: idx = 1 + (idx + 1) % 20; break;
	}
	String comp = Data.getSeason(time) + "." + idx;

	String[] list = Data.getLevelnames();
	Hashtable ht = new Hashtable();

	// Check them all
	for (int i = 0; i < list.length; i++) {
	    int val = calcCond(list[i], old, comp, time);
	    ht.put(list[i], new Integer(val));
	}

	init(comp, ht, time);
    }

    /**
     * Initialize this object.
     * @param comp compound id
     * @param conds levels for all conditions
     */
    private void init(String comp, Hashtable newlevels, long time) {
	season = comp.substring(0, comp.indexOf('.'));
	idx = Integer.parseInt(comp.substring(comp.indexOf('.') + 1));
	event = Data.getEvent(comp);
	temperature = Data.getTemp(comp, time);
	winddir = Data.getWindDir(comp);
	windspeed = Data.getWindSpeed(comp);
	speedindex = DiceRoller.d(6);
	clouds = Data.getClouds(comp);
	levels = newlevels;
    }

    /**
     * Given an old weather unit calculate the new level for condition
     * name. The new compound is taken from this object and assumed to exist.
     * @param name name of condition
     * @param old old weather unit
     * @param time time needed to distinguish day/night
     */
    int calcCond(String name, WeatherUnit old, long time) {
	return calcCond(name, old, season + "." + idx, time);
    }

    /**
     * Given an old weather unit calculate the new level for condition wih
     * name name.
     * @param name name of condition
     * @param old old weather unit
     * @param comp new compound name
     */
    int calcCond(String name, WeatherUnit old, String comp, long time) {
	Data.Condition cond = (Data.Condition) Data.getCondList().get(name);
	Hashtable mt = (Hashtable) cond.events;

	// Check event effect
	String de = (String) mt.get(Data.getEvent(comp));
	// Check temperature effect
	String dt = (String) mt.get(Data.getTemp(comp,time));

	Integer i = (Integer)old.levels.get(name);
	int c = (i != null ? i.intValue() : 0);
	if (de != null) c += Integer.parseInt(de);
	if (dt != null) c += Integer.parseInt(dt);

	if (de == null && dt == null) c -= 1;

	if (cond.max != 0 && c > cond.max) c = cond.max;
	return (c < 0) ? 0 : c;
    }

    /**
     * Utility that turns this unit back into a DOM attribute set
     * @param e element for which attributes are set
     */
    void setAttributes(Element e) {
	e.setAttribute("compound", season + "." + idx);

	Iterator i = levels.keySet().iterator();
	while (i.hasNext()) {
	    String name = (String) i.next();
	    Integer val = (Integer) levels.get(name);
	    if (val != null && val.intValue() != 0)
		e.setAttribute(name, Integer.toString(val.intValue()));
	    else
		e.removeAttribute(name);
	}
    }
}

