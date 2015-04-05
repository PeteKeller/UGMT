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
import org.w3c.dom.*;
import rpg.*;
import java.io.*;
import java.util.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * State object. This represents the calendar state. It therefore
 * listens to "Groups". Exteral events are also listened to.
 * @author Michael Jung
 */
public class State implements IListener {
    /** reference to the act object */
    private CharGroup act;

    /** Root reference */
    private Main main;

    /** current display time */
    private long tDisplay = Long.MIN_VALUE;

    /** List of currently valid active/inactive dates */
    private Set actEncounters;
    private Set inactEncounters;

    /** yesterdays to show */
    int back;

    /** tomorrows to show */
    int front;

    /**
     * Constructor. Loads the state of the weather (predetermined). This is
     * not a simple hasmap of hashmaps.
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
	back = Integer.parseInt(main.myProps.getProperty("yesterdays"));
	front = Integer.parseInt(main.myProps.getProperty("tomorrows"));
        actEncounters = new HashSet();
        inactEncounters = new HashSet();

        Document dataDoc = Framework.parse(main.myPath + "state.xml", "calendar");
        loadDoc(dataDoc.getDocumentElement());
    }

    /**
     * Method to load the state.
     * @param doc (XML) document to load
     */
    private void loadDoc(Node doc) {
        NodeList tree = doc.getChildNodes();
        String[] types = new String[0];
	for (int i = 0; i < tree.getLength(); i++) {
            if (!tree.item(i).getNodeName().equals("types")) continue;
            types =
                tree.item(i).getAttributes().getNamedItem("list").getNodeValue().split(",");
        }
        for (int i = 0; (types != null) && (i < types.length); i++)
            if (types[i].length() != 0) main.myData.addType(types[i], true);
    }

    /**
     * Use this method to save the data in this object to file. After saving,
     * this object is "clean" once more.
     */
    public void save() {
        // Create technical objects
        Document doc = Framework.newDoc();

        // Root
        Node root = doc.createElement("calendar");
        doc.insertBefore(root, doc.getLastChild());

        // List
        String list = "";
        Iterator iter = main.myData.getTypes().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            if (((Boolean)me.getValue()).booleanValue())
                list += (String)me.getKey() + ",";
        }
        if (list.length() > 0) {
            Element eltypes = doc.createElement("types");
            eltypes.setAttribute("list", list.substring(0,list.length() - 1));
            root.insertBefore(eltypes, null);
        }

        // Write the document to the file
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(doc, new StreamResult(file), null);
    }

    /**
     * Put an event in this list according to it's last announcement.
     */
    public void put(CalEvent ev) {
        actEncounters.remove(ev);
        inactEncounters.remove(ev);
        if (ev.isValid() && ev.isActive())
            actEncounters.add(ev);
        if (ev.isValid() && !ev.isActive())
            inactEncounters.add(ev);
    }

    /**
     * Goto the next event.
     * @param idx idx in time array. 0 = begin, 1 = end
     */
    public void gotoNext(int idx) {
        ArrayList eList = getAllEvents();
        if (eList == null) return;
        long t0 = act.getDate() + 2;
        Comparator comp = new MyComparator(t0, front * Data.DAY, idx);
        TreeSet ts = new TreeSet(comp);

        // Add all events
	for (int j = 0; j < front; j++) {
            ArrayList wd = (ArrayList) eList.get(j + back);
            ts.addAll(wd);
        }

        Object[] oa = ts.toArray();
        if (oa == null || oa.length == 0) return;

        float reltime = 0.0f;
        for (int i = 0; reltime == 0.0 && i < oa.length; i++) {
            TimeEvent ev = (TimeEvent) oa[i];
            reltime = ev.isActive(t0, t0 + front * Data.DAY)[idx];
        }
        long date = t0 + (long)(reltime * front * Data.DAY) + (idx == 0 ? 1 : -1);
                                
        act.setLocation(act.getMap(), act.getX(), act.getY(), date);
    }

    /**
     * Get date for active group
     * @return date
     */
    public long getDate() { return (act != null ? act.getDate() : 0); }

    /**
     * Add time t to the current date of the active group.
     * @param t time to add
     */
    public void addDate(long t) {
	long date = act.getDate() + t;
	act.setLocation(act.getMap(), act.getX(), act.getY(), date);
    }

    /**
     * Get the current time of the active group-
     * @return current time
     */
    public long getCurrentTime() { return (act != null ? act.getDate() : 0); }

    /**
     * Method to calc time distance on board
     * @param x coordinate on weather display
     * @param y coordinate on weather display
     * @return time distance
     */
    long calcDist(int x, int y) {
	CalendarDisplay wd = main.getDisplay();
	x = wd.getCalendarX(x);
	y = wd.getCalendarY(y);
	int w = wd.getCalendarWidth();
	int h = wd.getCalendarHeight();

	long ret = -1;
	// Point must be in bounds
	if (x >= 0 && y >= 0 && x <= w && y <= h) {
	    int dy = main.getDisplay().getDate(y) - (int)(getDate() / Data.DAY);
	    int dx = (int)(x - w * (getDate() % Data.DAY) / Data.DAY);

	    // This may return negative
	    ret = (long)(dy * Data.DAY + dx * Data.DAY / w);
	}

	// Show distance
	if (ret > 0)
	    main.bDist.setText("Distance: " + main.myData.time2string(ret));
	else
	    main.bDist.setText("Distance: --:--");

	return ret;
    }

    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
        long redrawTime = -1;

	if (obj instanceof CharGroup) {
            CharGroup cg  = (CharGroup) obj;
            if (cg.isActive()) {
                if (cg != act) act = cg;
                // Get 0:00 of today
                long t0 = act.getDate() - act.getDate() % Data.DAY;

                redrawTime = t0 - tDisplay;
            }

            // Announce active/inactive changes
            Set delta = new HashSet();
            Iterator iter = actEncounters.iterator();
            while (iter.hasNext()) {
                CalEvent e = (CalEvent)iter.next();
                if (!e.isActive()) delta.add(e);
            }
            iter = inactEncounters.iterator();
            while (iter.hasNext()) {
                CalEvent e = (CalEvent)iter.next();
                if (e.isActive()) delta.add(e);
            }
            iter = delta.iterator();
            while (iter.hasNext()) {
                main.myFrw.announce((CalEvent)iter.next());
            }
         }
        if (obj instanceof TimeEvent && !(obj instanceof CalEvent)) {
            TimeEvent ev = (TimeEvent) obj;
            if (!main.myData.containsType(ev.type()))
                main.addType(ev.type());
            int oldsz = main.myData.getEventListSize();
            if (ev.isValid())
                main.myData.addEvent(ev);
            else
                main.myData.removeEvent(ev);
            if (oldsz != main.myData.getEventListSize()) redrawTime = 0;
        }
        if (obj == null) redrawTime = 0;

        // A new day requires a shift of old weather (zero means force update)
        if (act != null && (redrawTime == 0 || Math.abs(redrawTime) >= Data.DAY)) {
            tDisplay += redrawTime;

            // activate
            ArrayList evt = main.myData.getEvent(act.getDate(), act.getDate());
            for (int j = 0; evt != null && j < evt.size(); j++)
                if (evt.get(j) instanceof CalEvent)
                    ((CalEvent)evt.get(j)).setActive(act.getDate(), act.getDate());

            // Redraw
            main.getDisplay().revalidate();
            main.getDisplay().repaint();
        }
        if (redrawTime != 0) main.redo();
    }

    /**
     * Get all events currently active.
     */
    public ArrayList getAllEvents() {
        ArrayList evts = new ArrayList();
        for (int i = 0; i < (front + back); i++) {
            evts.add(main.myData.getEvent
                     (tDisplay + (i - back) * Data.DAY,
                      tDisplay + (i - back + 1) * Data.DAY));
        }
        return evts;
    }

    /** Class to sort events */
    class MyComparator implements Comparator {
        /** Start time, End time */
        long a, b;
        /** Start or end time */
        int idx;
        /** Constructor */
        MyComparator(long aStart, long duration, int anIdx) {
            a = aStart;
            b = a + duration;
            idx = anIdx;
        }
        /** IF method */
        public int compare(Object o1, Object o2) {
            float[] t1 = ((TimeEvent) o1).isActive(a, b);
            float[] t2 = ((TimeEvent) o2).isActive(a, b);
            if (t1 == null) return (int)Float.POSITIVE_INFINITY;
            if (t2 == null) return (int)Float.NEGATIVE_INFINITY;
            int ret = (int)
                (Data.DAY * (t1[idx] - t2[idx]));
            return ret == 0 ? 1 : ret;
        }
        /** IF method */
        public boolean equals(Object obj) { return false; }
    }
}
