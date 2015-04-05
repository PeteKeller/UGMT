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
package harn.calendar;

import harn.repository.*;
import java.io.*;
import java.util.*;
import javax.swing.ImageIcon;
import org.w3c.dom.*;
import rpg.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data extends TimeFrame implements IListener {
    /** Root reference */
    private Main main;

    /** months' properties */
    private Hashtable months;

    /** sunsigns' properties */
    private Hashtable sunsigns;

    /** events */
    private HashSet events;

    /** event types */
    private Hashtable types;

    /** length of year */
    public static long YEAR = 0;

    /** length of hour */
    final public static int HOUR = 60;

    /** Length of watch */
    final public static int WATCH = 4 * HOUR;

    /** length of day */
    final public static int DAY = 6 * WATCH;

    /** Daytime delta */
    private int dtDelta;

    /** moon period */
    private long moon;

    /** sunsign offset */
    private long ssOff;

    /** sunsign maximum */
    private int ssMax;

    /** sound bank */
    private SoundBank sounds;

    /**
     * Constructor. Parses the data into a hashmap of hashmaps...
     * Gets all iconic weather items.
     * @param aMain root reference
     */
    public Data(Main aMain, Framework frw) {
	main = aMain;
	months = new Hashtable();
	sunsigns = new Hashtable();
	events = new HashSet();
        types = new Hashtable();

	try {
            File pf = new File(main.myPath);
            String[] datal = pf.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return
                            name.endsWith(".xml") &&
                            name.startsWith("data");
                    }
                });
            Document data = Framework.parse(main.myPath + "data.xml", "calendar");
            loadFromDoc(data);
            for (int i = 0; i < datal.length; i++) {
                if (!datal[i].equals("data.xml")) {
                    data = Framework.parse(main.myPath + datal[i], "calendar");
                    loadFromDoc(data);
                }
            }
	    YEAR *= DAY;
	    frw.announce(this);
	}
	catch (Exception e) {
	    // Debug
	    e.printStackTrace();
	}
    }

    /**
     * Get Month (1 - max) as index
     * @param time time to determine month for
     * @return month index
     */
    public int getMonth(long date) {
	long t = date % YEAR;

	// Start iteration
	long t0 = 0;
	for (int i = 1; months.get(Integer.toString(i)) != null; i++) {
	    Hashtable ht = (Hashtable) months.get(Integer.toString(i));
	    int m0 = DAY * Integer.parseInt((String) ht.get("days"));
	    if (m0 + t0 > t) return i;
	    t0 += m0;
	}
	// default - always wrong
	return 0;
    }

    /**
     * Get length of day.
     * @return length of day in minutes
     */
    public long getLengthOfDay() { return DAY; }

    /**
     * Get length of hour.
     * @return length of hour in minutes
     */
    public long getLengthOfHour() { return HOUR; }

    /**
     * Get Day in month
     * @param time time to determine day in month for
     * @return day
     */
    public int getDay(long date) {
	int t = (int)(date % YEAR);

	// Start iteration
	int t0 = 0;
	for (int i = 1; months.get(Integer.toString(i)) != null; i++) {
	    Hashtable ht = (Hashtable) months.get(Integer.toString(i));
	    int m0 = DAY * Integer.parseInt((String) ht.get("days"));
	    if (m0 + t0 > t) return (t - t0) / DAY + 1;
	    t0 += m0;
	}
	// default - always wrong
	return 0;
    }

    /**
     * Get Month (1 - max) as string
     * @param time time to determine month for
     * @return month name
     */
    public String getMonthName(long date) {
	long t = date % YEAR;

	// Start iteration
	long t0 = 0;
	for (int i = 1; months.get(Integer.toString(i)) != null; i++) {
	    Hashtable ht = (Hashtable) months.get(Integer.toString(i));
	    int m0 = DAY * Integer.parseInt((String) ht.get("days"));
	    String name = (String)ht.get("name");
	    if (m0 + t0 > t) return name;
	    t0 += m0;
	}
	// default - always wrong
	return null;
    }

    /**
     * Get Season
     * @param time time to determine season for
     * @return season string
     */
    public String getSeason(long date) {
	int m = getMonth(date);
	Hashtable t = (Hashtable) months.get(Integer.toString(m));
	return (String) t.get("season");
    }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public String getName() { return "TheCalendar"; }

    /**
     * Pretty prints a (small) time.
     * @param date date to print
     */
    public String time2string(long date) {
	// h:mm
	return date / HOUR + ":" +
	    (date % HOUR) / 10 + "" + (date % HOUR) % 10;
    }

    /**
     * Pretty prints a (big) date.
     * @param date date to print
     */
    public String date2string(long date) {
	int d = getDay(date);
	String m = getMonthName(date);
	long y = date / YEAR;
	return d + "-" + m.substring(0,3) + "-" + y;
    }

    /**
     * Pretty prints date and time. Prototypical is a "d.m.y h:mm" format.
     * @param date date/time to print
     */
    public String datetime2string(long date) {
	return date2string(date) + " " + time2string(date % DAY);
    }

    /**
     * Utility that converts a string date format into a long date. This
     * utility must be able to parse all dates it prints with the three
     * methods above.
     * @param str string version of date
     * @return long date
     */
    public long string2datetime(String str) {
	if (str.indexOf(' ') > 0)
	    return string2date(str.substring(0,str.indexOf(' ')))
		+ string2time(str.substring(str.indexOf(' ')+1));
	else if (str.indexOf('-') > 0)
	    return string2date(str);
	else 
	    return string2time(str);
    }
	    
    /**
     * Utility that converts a string date format into a long date. Parses
     * only date.
     * @param str string version of date
     * @return long date
     */
    long string2date(String str) {
	long ret = DAY * (Integer.parseInt(str.split("-")[0]) - 1);

	// Add up all months
	str = str.substring(str.indexOf('-')+1);
	String m = str.substring(0,str.indexOf('-'));
	for (int i = 1; months.get(Integer.toString(i)) != null; i++) {
	    Hashtable t = (Hashtable) months.get(Integer.toString(i));
	    String name = (String) t.get("name");
	    if (name.substring(0,3).equals(str.substring(0,3)))
		break;
	    ret += DAY * Long.parseLong((String) t.get("days"));
	}

	str = str.substring(str.indexOf('-')+1);
	ret += YEAR * Integer.parseInt(str);
	return ret;
    }

    /**
     * Utility that converts a string date format into a long date. Parses
     * only time.
     * @param str string version of date
     * @return long date
     */
    long string2time(String str) {
	int h = Integer.parseInt(str.substring(0,str.indexOf(':')));
	str = str.substring(str.indexOf(':')+1);
	int m = Integer.parseInt(str);
	return m + HOUR * h; 
    }

    /**
     * Returns dawn of the day in which time lies. Uses a simple algorithm to
     * take into account seasons.
     * @param time time in a day
     * @return dawn of the day
     */
    public long getDawn(long time) {
	// If dtdelta = 2:00, we have 0 = YEAR/2 = 6:00; YEAR/4 = 4:00;
	// 3YEAR/4 = 8:00

	long day = (long)(time / DAY) * DAY + 6 * HOUR;
	// Offset between -1 and 1
	double offset = - Math.sin(2*Math.PI * (time % YEAR) / YEAR);
	return day + (long)(dtDelta * offset);
    }

    /**
     * Returns dusk of the day in which time lies. Uses a simple algorithm to
     * take into account seasons.
     * @param time time in a day
     * @return dawn of the day
     */
    public long getDusk(long time) {
	// if dtdelta = 2:00, we have 0 = YEAR/2 = 18:00; YEAR/4 = 20:00;
	// 3YEAR/4 = 16:00

	long day = (long)(time / DAY) * DAY + 18 * HOUR;
	// Offset between -1 and 1
	double offset = Math.sin(2*Math.PI * (time % YEAR) / YEAR);
	return day + (long)(dtDelta * offset);
    }

    /**
     * Returns true if daytime
     * @param time time in a day
     * @return whether time is day time
     */
    public boolean getDayTime(long time) {
	return (time > getDawn(time) && time < getDusk(time));
    }

    /**
     * Load the availWeath list from the XML doxument (reflecting the current
     * state).
     * @return hashtable containing the weather entries
     */
    private void loadFromDoc(Document data) {
	boolean getBaseCal = (YEAR == 0) ;
	NodeList tree = data.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    NamedNodeMap attr = tree.item(i).getAttributes();
            String nodename = tree.item(i).getNodeName();

            // This is for month entries
            if (nodename.equals("month")) {
                // Get all attributes
                String name = attr.getNamedItem("name").getNodeValue();
                String days = attr.getNamedItem("days").getNodeValue();
                String idx = attr.getNamedItem("index").getNodeValue();
                String season = attr.getNamedItem("season").getNodeValue();

                // Store in hashtable
                Hashtable t = new Hashtable();
                t.put("name", name);
                t.put("days", days);
                t.put("season", season);

                // Store hashtable
                months.put(idx, t);
                if (getBaseCal) YEAR += Long.parseLong(days);
            }

            // This is for events
            if (nodename.equals("event")) {
                // Get all attributes
                String evname = attr.getNamedItem("name").getNodeValue();
                String type = attr.getNamedItem("type").getNodeValue();
                String date = attr.getNamedItem("date").getNodeValue();
                String time = attr.getNamedItem("time").getNodeValue();

                // Get period (if available)
                String period = null;
                Node pern = attr.getNamedItem("period");
                if (pern != null)
                    period = pern.getNodeValue();

                // Get icon (if available)
                ImageIcon icon = null;
                Node in = attr.getNamedItem("img");
                if (in != null) {
                    String iname = in.getNodeValue();
                    String osName = Framework.osName(iname);
                    icon = new ImageIcon(main.myPath + osName);
                }

                // Add event
                TimeEvent ev = new CalEvent(main, evname, type, date, time, period, icon);
                events.add(ev);
                addType(type, false);
            }

            // This is for day/night time
            if (nodename.equals("daytime")) {
		Node delta = attr.getNamedItem("delta");
                dtDelta = Integer.parseInt(delta.getNodeValue());
            }

            // This is for the moon phase
            if (nodename.equals("moon")) {
		Node period = attr.getNamedItem("period");
                moon = DAY * Integer.parseInt(period.getNodeValue());
            }

            // This is for the sunsigns
            if (nodename.equals("sunsigns")) {
		Node ss = attr.getNamedItem("start");
                ssOff = DAY * Integer.parseInt(ss.getNodeValue());
                NodeList tree2 = tree.item(i).getChildNodes();
                for (int j = 0; j < tree2.getLength(); j++) {
                    // Get all attributes
                    NamedNodeMap attr2 = tree2.item(j).getAttributes();
                    if (attr2 != null) {
                        Node nn = attr2.getNamedItem("name");
                        String name = nn.getNodeValue();
                        Node ln = attr2.getNamedItem("length");
                        String length = ln.getNodeValue();
                        Node in = attr2.getNamedItem("index");
                        String idx = in.getNodeValue();

                        // Store in hashtable
                        Hashtable t = new Hashtable();
                        t.put("name", name);
                        t.put("length", length);

                        // Store hash table
                        sunsigns.put(idx, t);
                        ssMax = Math.max(ssMax,Integer.parseInt(idx));
                    }
                }
            }
        }
    }

    /**
     * Get the moon phase as percentage value. Positive is waxing, negative is
     * waning, 100 is a full moon, 0 a new moon.
     * @param time time for which to determine moon phase
     * @return moon phase
     */
    public int getMoon(long time) {
	long t = ( (time + DAY) % moon) / DAY;
	if (t * DAY <= moon / 2)
	    return (int)(200 * t * DAY / moon);
	else
	    return (int)(200 * (moon - t * DAY) / moon);
    }

    public String getSunsign(long date) {
	long t = (date - ssOff + YEAR + DAY) % YEAR;

	// Start iteration
	int t0 = 0;
	for (int i = 1; i <= ssMax; i++) {
	    Hashtable ht = (Hashtable) sunsigns.get(ssMod(i));
	    int m0 = DAY * Integer.parseInt((String) ht.get("length"));
	    if (t < m0 + t0 && t >= t0) {
		// We found the main sunsign
		String prim = (String)ht.get("name");
		String sec = "";

		// Get the next cusp
		if (t >= m0 + t0 - 2 * DAY) {
		    ht = (Hashtable) sunsigns.get(ssMod(i+1));
		    if (ht == null)
			ht = (Hashtable) sunsigns.get("1");
		    sec = "/" + (String) ht.get("name");
		}

		// Get the previous cusp
		if (t < t0 + 2 * DAY) {
		    ht = (Hashtable) sunsigns.get(ssMod(i-1));
		    if (ht == null)
			ht = (Hashtable) sunsigns.get(ssMod(ssMax));
		    sec = "/" + (String) ht.get("name");
		}
		return prim + sec;
	    }
	    t0 += m0;
	}
	// default - always wrong
	return null;
    }

    /**
     * Really small helper for off-beat modulus of ssMax
     * @param i integer for modulus
     * @return modulus
     */
    private String ssMod(int i) {
	return Integer.toString((i + ssMax - 1) % ssMax + 1);
    }

    /**
     * Get the calender events for the time given
     * @param from time for events
     * @param to time for events
     * @return event list
     */
    ArrayList getEvent(long from, long to) {
        Iterator iter = events.iterator();
        ArrayList al = new ArrayList();
        while (iter.hasNext()) {
            TimeEvent ev = (TimeEvent)iter.next();
            float[] time = ev.isActive(from, to);
            if (time == null || time[0] == time[1]) continue;
            if (suppressType(ev.type())) continue;
            int i = 0;
            for (i = 0; i < al.size(); i++) {
                TimeEvent ev2 = (TimeEvent)al.get(i);
                if (ev2.type().equals(ev.type()))
                    if (ev instanceof CalEvent && ev2 instanceof CalEvent) 
                        if (((CalEvent)ev2).getPeriod() > ((CalEvent)ev).getPeriod()) {
                            al.set(i, ev2);
                            break;
                        }
            }
            if (i == al.size()) al.add(ev);
        }
	return al;
    }

    /**
     * Inform of a sound bank.
     */
    public void inform(IExport obj) {
        sounds = (SoundBank)obj;
    }

    /**
     * Get the sound bank.
     * @return sound bank
     */
    public SoundBank getSoundBank() { return sounds; }

    /**
     * Get event list size.
     * @return event list size
     */
    public int getEventListSize() { return events.size(); }

    /**
     * Add an event.
     * @param event event to add
     */
    public void addEvent(TimeEvent ev) { events.add(ev); }

    /**
     * Remove an event.
     * @param event event to remove
     */
    public void removeEvent(TimeEvent ev) { events.remove(ev); }

    /**
     * Add an event type.
     * @param type event type to add
     */
    public void addType(String ev, boolean val) {
        types.put(ev, new Boolean(val));
    }

    /**
     * Show an event type?
     * @return whether to show type or not
     */
    public boolean suppressType(String type) { return ((Boolean)types.get(type)).booleanValue(); }

    /**
     * Is event type contained?
     * @return whether containstype or not
     */
    public boolean containsType(String type) { return types.get(type) != null; }

    /**
     * Get the event types.
     * @return event types
     */
    public Set getTypes() { return types.entrySet(); }
}
