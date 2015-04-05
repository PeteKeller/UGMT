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

import harn.repository.TimeFrame;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data implements IListener {
    /** Root reference */
    private Main main;

    /** properties, access to package */
    static Hashtable data;

    /** icon weather item */
    private Hashtable icons;

    /** Calendar reference */
    static private TimeFrame cal;

    /** List of all possible button conditions */
    static private Hashtable condButtonList;

    /** List of all conditions as class object below */
    static private HashList condList;

    /** prepared GUI panel for conditions */
    private JPanel myPanel;

    /**
     * Constructor. Parses the data into a hashmap of hashmaps...
     * Gets all iconic weather items.
     * @param aMain root reference
     */
    Data(Main aMain) {
	main = aMain;
	icons = new Hashtable();
	myPanel = new JPanel();

	data = new Hashtable();
	condList = new HashList();
	condButtonList = new Hashtable();

	// Parse the file
        Document doc = Framework.parse(main.myPath + "data.xml", "weather");
        parseTree(doc.getDocumentElement().getChildNodes());
    }

    /**
     * Parses the XML-tree.
     * @param tree current branch to parse
     * @param name current name on the stack
     */
    private void parseTree(NodeList tree) {
	// Gui contrsaints
	GridBagConstraints constr;
	myPanel.setLayout(new GridBagLayout());
	constr = new GridBagConstraints();
	constr.gridx = 0;
	constr.weightx = 1;
	constr.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < tree.getLength(); i++) {
	    NamedNodeMap attr = tree.item(i).getAttributes();
	    if (attr == null) continue;

	    // Get the node as new root for properties
            Node item = tree.item(i);
	    Node nnode = item.getAttributes().getNamedItem("name");
	    String name = nnode.getNodeValue();
	    Hashtable prop = new Hashtable();
	    data.put(name, prop);

	    NodeList list = item.getChildNodes();

	    if (item.getNodeName().equals("compound")) {
		// Get the children of a compound
		for (int j = 0; j < list.getLength(); j++) {
		    attr = list.item(j).getAttributes();
		    if (attr == null) continue;

		    Node nn = attr.getNamedItem("name");
		    Node nv = attr.getNamedItem("value");
		    prop.put(nn.getNodeValue(), nv.getNodeValue());

		    // compound item as icon
		    String val = main.myProps.getProperty(nv.getNodeValue());
		    if (val != null) icons.put(nv.getNodeValue(), val);
		}
	    }
	    else if (item.getNodeName().equals("condition")) {
		// Get the threshold for the condition
		Node nt = item.getAttributes().getNamedItem("value");
		String thresh = (nt != null ? nt.getNodeValue() : "1");

		// A previous ref may have created this
		Condition c = (Condition) condList.get(name);
		if (c == null) c = new Condition();
		condList.put(name, c);
		c.threshhold = Integer.parseInt(thresh);
		
		boolean foundRef = false;
		boolean foundEvent = false;
		// Get the children of the condition
		for (int j = 0; j < list.getLength(); j++) {
		    attr = list.item(j).getAttributes();
		    if (attr == null) continue;
		    
		    if (list.item(j).getNodeName().equals("effect")) {
			// We have an event
			Node nn = attr.getNamedItem("name");
			Node nv = attr.getNamedItem("value");
			c.events.put(nn.getNodeValue(), nv.getNodeValue());
			c.lvlname.setLength(0);
			c.lvlname.append(name);
			foundEvent = true;
		    }
		    else {
			// We have a reference
			String ref = attr.getNamedItem("name").getNodeValue();
			Condition cref = (Condition) condList.get(ref);
			if (cref == null) {
			    cref = new Condition();
			    condList.put(ref, cref);
			}
			// Get events from there
			c.events = cref.events;
			c.lvlname = cref.lvlname;
			foundRef = true;
		    }
		}
		// Use default event
		if (!foundEvent && !foundRef) {
		    c.lvlname.setLength(0);
		    c.lvlname.append(name);
		    c.events.put(name,"1");
		    c.max = 1;
		}
 
		if (foundEvent) {
		    // Create a button
		    JButton b = new JButton(name + " (0)");
		    b.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				String s = e.getActionCommand();
				s = s.substring(0, s.indexOf('(')-1);
				State state = main.getState();
				if ((e.getModifiers() &
				     ActionEvent.SHIFT_MASK) == 0)
				    state.setCond(s, state.getDate(), 1);
				else
				    state.setCond(s, state.getDate(), -1);
			    }
			});
                    b.setToolTipText
                        ("to increase click, to decrease shift-click");
		    condButtonList.put(name, b);
		    myPanel.add(b, constr);
		}
	    }
	}
    }

    /**
     * Get the prepared GUI panel for all conditions.
     * @return GUI panel
     */
    JPanel getPanel() { return myPanel; }

    /**
     * Redraw the buttons.
     */
    void setButtons() {
	Iterator i = condButtonList.keySet().iterator();
	while (i.hasNext()) {
	    String name = (String) i.next();
	    JButton b = (JButton) condButtonList.get(name);
	    WeatherUnit wu = main.getState().getWeather();
            if (wu == null) continue; // No calendar present
            Integer val = (Integer) wu.levels.get(name);
	    
	    b.setText(name + " (" + (val != null ? val.intValue() : 0) + ")");
	}
    }

    /**
     * Access to the private reference object
     * @return active calendar
     */
    static boolean existsTimeframe() { return (cal != null); }

    /**
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
	if (cal == null || cal.getName() != obj.getName()) {
	    cal = (TimeFrame) obj;
	    if (main.getState().getActive() != null)
		main.getState().inform(main.getState().getActive());
	    main.redo(true);
	}
    }

    /**
     * Returns Moon phase (+percentage if waxing, -percentage if waning)
     * @return moon phase
     */
    int getMoon(long time) {
	if (cal != null) return cal.getMoon(time);
	return 0;
    }

    /**
     * Returns dawn of the day in which time lies.
     * @param time time in a day
     * @return dawn of the day
     */
    static long getDawn(long time) {
	if (cal != null) return cal.getDawn(time);
	return 0;
    }

    /**
     * Returns dusk of the day in which time lies.
     * @param time time in a day
     * @return dawn of the day
     */
    static long getDusk(long time) {
	if (cal != null) return cal.getDusk(time);
	return 0;
    }

    /**
     * Returns true if daytime
     * @param time time in a day
     * @return whether time is day time
     */
    static boolean getDayTime(long time) {
	return (time > getDawn(time) && time < getDusk(time));
    }

    /**
     * Returns temperature for given params. Note that some compounds have two
     * temperatures, one during the day and one furing the night. Therefore
     * the time parameter is needed.
     * @param comp compound to question
     * @param time time of day
     * @return temperature string
     */
    static String getTemp(String compound, long time) {
	// Get the compound temperature (or default)
	Hashtable hComp = (Hashtable) data.get(compound);
	if (hComp == null) hComp = (Hashtable) data.get("Spring.1");
	String ret = (String) hComp.get("temperature");

	// Check for multiple temperatures
	int i = ret.indexOf('(');
	if (i == -1) return ret;

	// day temperature
	if (getDayTime(time)) return ret.substring(0,ret.indexOf('('));
	// night temperature
	return ret.substring(ret.indexOf('(')+1,ret.indexOf(')'));
    }

    /**
     * Returns wind direction for given params.
     * @param comp compound to question
     * @return wind direction string
     */
    static String getWindDir(String compound) {
	Hashtable hComp = (Hashtable) data.get(compound);
	if (hComp == null) hComp = (Hashtable) data.get("Spring.1");
	return (String) hComp.get("winddirection");
    }

    /**
     * Returns cloudcover for given params
     * @param comp compound to question
     * @return cloud cover string
     */
    static String getClouds(String compound) {
	Hashtable hComp = (Hashtable) data.get(compound);
	if (hComp == null) hComp = (Hashtable) data.get("Spring.1");
	return (String) hComp.get("cloudcover");
    }

    /**
     * Returns wind speed for given params
     * @param comp compound to question
     * @return wind speed string
     */
    static String getWindSpeed(String compound) {
	Hashtable hComp = (Hashtable) data.get(compound);
	if (hComp == null) hComp = (Hashtable) data.get("Spring.1");
	return (String) hComp.get("windspeed");
    }

    /**
     * Returns event for given params
     * @param comp compound to question
     * @return event string (may be empty)
     */
    static String getEvent(String compound) {
	Hashtable hComp = (Hashtable) data.get(compound);
	if (hComp == null) hComp = (Hashtable) data.get("Spring.1");
        String ret = (String) hComp.get("events");
	if (ret == null) ret = "";
	return ret;
    }

    /**
     * Get Season.
     * @param time time to determine season for
     * @return season string
     */
    static String getSeason(long date) {
	if (cal != null) return cal.getSeason(date);
	return "Spring";
    }

    /**
     * Get an icon for a specific weather item
     * @return icon
     */
    ImageIcon getIcon(String key) {
	String name = (String) icons.get(key);
	if (name != null) {
            String osName= Framework.osName(name);
            return new ImageIcon(main.myPath + name);
        }
	return null;
    }
    /**
     * Calculate the sunsign for this month.
     */
    static String getSunsign(long date) {
	if (cal != null) return cal.getSunsign(date);
	return null;
    }

    /**
     * Utility. Pretty prints a date.
     * @param date date to pretty print
     */
    static String stringDate(long date) {
	if (cal != null) return cal.date2string(date);
	return null;
    }

    /**
     * Utility. Pretty prints date and time.
     * @param date date to pretty print
     */
    static String stringDateTime(long date) {
	if (cal != null) return cal.datetime2string(date);
	return null;
    }

    /**
     * Utility. Pretty prints a date.
     * @param date date to pretty print
     */
    static public String stringTime(long date) {
	if (cal != null) return cal.time2string(date);
	return null;
    }

    /**
     * Utility that converts a string date format into a long date.
     * @param str string version of date
     * @return long date
     */
    static long string2date(String str) {
	if (cal != null) return cal.string2datetime(str);
	return 0;
    }

    /**
     * Return the list of possible conditions
     * @return list of conditions
     */
    static String[] getLevelnames() {
	HashSet al = new HashSet();

	// Get all level names
	Iterator i = condList.keySet().iterator();
	while (i.hasNext()) {
	    String key = (String) i.next();
	    Condition c = ((Condition)condList.get(key));
	    al.add(c.lvlname.toString());
	}

	// Turn into string array
	Object[] obj = al.toArray();
	String[] ret = new String[obj.length];
	for (int j = 0; j < obj.length; j++) {
	    ret[j] = (String) obj[j];
	}
	return ret;
    }

    /**
     * Return the raw condition list
     * @return list of conditions
     */
    static HashList getCondList() { return condList; }

    /**
     * Class to combine a threshhold, level-name and events.
     */
    class Condition {
	/** threshhold */
	int threshhold;
	/** event list */
	Hashtable events;
	/** level name */
	StringBuffer lvlname;
	/** max (0 = infinity) */
	int max;
	/** Constructor */
	Condition() {
	    events = new Hashtable();
	    lvlname = new StringBuffer();
	}
    }

    /**
     * Class to combine an array list with a hashmap
     */
    class HashList extends Hashtable {
	/** contains the order */
	ArrayList keys = new ArrayList();

	/**
	 * Override to allow preservation of order.
	 * @param key key
	 * @param val value
	 */
	public void put(String key, Object val) {
	    keys.add(key);
	    super.put(key, val);
	}

	/**
	 * Get entry from ordered list of keys.
	 * @param key key
	 * @param val value
	 */
	public String getKey(int i) { return (String) keys.get(i); }
    }
}
