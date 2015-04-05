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

import harn.repository.TimeEvent;
import javax.swing.ImageIcon;
import rpg.Framework;

/**
 * Object that represents an event. Such items are announced as they are
 * turned active or inactive.
 * @author Michael Jung
 */
class CalEvent extends TimeEvent {
    /** Main reference */
    private Main main;

    /** name of event */
    private String name;

    /** Long gescription */
    private String type;

    /** Initial date */
    private String initDate;

    /** Duration */
    private int duration;

    /** period */
    private String period;

    /** icon */
    private ImageIcon icon;

    /**
     * Constructor. Any parameter, except name, may be null.
     * @param aMain main reference
     * @param aName name/description of event
     * @param aType type of event
     * @param anIcon icon to be displayed beside event
     */
    CalEvent(Main aMain, String aName, String aType, String date, String time, String aPeriod, ImageIcon anIcon) {
        main = aMain;
        name = aName;
        type = aType;
        icon = anIcon;
        initDate = date;
        period = aPeriod;

        String[] t = time.split(":");
        if (t.length > 0)
            duration = Integer.parseInt(t[0]) * Data.HOUR + Integer.parseInt(t[1]);
        else
            duration = Integer.parseInt(t[0]);
        main.myFrw.announce(this);
        if (main.getState() != null) main.getState().put(this);
    }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public String getName() { return name; }

    /**
     * Several events may be active at a given time. Returns the state of
     * this event.
     * @return true if an active event
     */
    public boolean isActive() {
        return isActive(main.getState().getCurrentTime(), main.getState().getCurrentTime()) != null;
    }

    /**
     * Our events are alway valid.
     */
    public boolean isValid() { return true; }

    /**
     * Set the current time for this event
     * @param from start of interval
     * @param to end of interval
     */
    void setActive(long from, long to) {
        if (isActive(from, to) != null) {
            main.myFrw.announce(this);
            main.getState().put(this);
        }
    }

    /**
     * Check whether this is event is active for the given time. Returns array
     * containing the relative starting and end points [0,1] in the interval
     * [from, to] or null, if no intersection. If from = to, and intersection
     * is non-empty return {0,1}.
     * @param from start of interval
     * @param to end of interval
     * @return start and end.
     */
    public float[] isActive(long from, long to) {
        long init = main.myData.string2datetime(initDate);

        if (to < init) return null;

        int per = getPeriod();

        // from = to
        if (from == to) {
            long n = (per != 0 ? (int)((to - init)/per) : 0);
            if (init + n * per + duration >= to &&
                init + n * per <= from) return new float[]{0,1};
            return null;
        }

        // Ex. n>=0 : (np + [i,i+d]) intersect [f,t] != {}
        long n = (per != 0 ? (int)((to - init)/per) : 0);
        float f0 = ((float)(init + n * per - from))/(to - from);
        float f1 = ((float)(init + n * per + duration - from))/(to - from);
        if (f0 < 0) f0 = 0;
        if (f1 > 1) f1 = 1;
        if (f1 < 0) return null;

        return new float[] { f0, f1 };
    }

    /**
     * Returns the text description of the event.
     * @return textual description of event
     */
    public String type() { return type; }
    
    /**
     * Get numerical period.
     */
    public int getPeriod() {
        if (period == null) return 0;

        // Years
        String t[] = period.split("-");
        int per = Integer.parseInt(t[0]) * (int) Data.YEAR;
        // Months
        /* not supported */
        // Days
        if (t.length > 2) per += Integer.parseInt(t[2]) * (int) Data.DAY;
        return per;
    }

    /**
     * Returns the icon for this event.
     * @return icon for this event
     */
    public ImageIcon getIcon() { return icon; }

    /**
     * This method is called, when the user asks for details of a specific
     * event. Such details may be displayed by the calling plugin.
     * @return displayed string.
     */
    public String details() { return type + ": " + name; }

}
