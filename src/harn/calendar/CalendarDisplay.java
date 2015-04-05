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
import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Class that represents the Calendar.  It handles the GUI.
 * @author Michael Jung
 */
public class CalendarDisplay extends JComponent {

    /** Date offset in pixels */
    private static int doff = 64;

    /** root reference */
    private Main main;

    /** yesterdays to display */
    private int back;

    /** tomorrows to display */
    private int front;

    /** Currenty displayed rectangles */
    private Hashtable rect2events;

    /** Compound width */
    private int compoundw;

    /** Compound height */
    private int compoundh;

    /**
     * Constructor. Load mark and prepare layout.
     * @param props properties
     * @param main root reference
     */
    public CalendarDisplay(Properties props, Main aMain) {
	super();
	main = aMain;

	back = Integer.parseInt(props.getProperty("yesterdays"));
	front = Integer.parseInt(props.getProperty("tomorrows"));

	// Throw exception if non-integers
	compoundw = Integer.parseInt(props.getProperty("compound.width"));
	compoundh = Integer.parseInt(props.getProperty("compound.height"));
    }

    /**
     * Get GUI size of this.
     */
    public Dimension getPreferredSize() {
        ArrayList eList = main.getState().getAllEvents();
        int line = 0;
        int height = 0;
	for (int j = 0; j < (front + back); j++) {
            ArrayList wd = (ArrayList) eList.get(j);
            height = (wd != null ? wd.size() : 0);
            if (j == back && height == 0) height = 1;
            line += height;
        }
        return new Dimension(6*compoundw + doff + 1, line*compoundh + 1);
    }
    
    /**
     * Callback for the system GUI manager. Redraws calendar display and
     * mark/cursor.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) {
	// Get Font data
	FontMetrics fm = getFontMetrics(getFont());
	int hf = fm.getHeight();

	Data data = main.myData;
	State state = main.getState();
        rect2events = new Hashtable();
        ArrayList eList = state.getAllEvents();
        
        // Background
        Color old = null;
        long t0 = state.getDate() - state.getDate() % Data.DAY;
        int line = 0;
        int height = 0;
	for (int j = 0; j < (front + back); j++) {
            ArrayList wd = (ArrayList) eList.get(j);

            // Color and height
	    old = g.getColor();
            g.setColor(Color.lightGray);
            height = (wd != null ? wd.size() : 0);
            if (j == back && height == 0) height = 1;

	    // Dawn and dusk
	    long t = state.getDate() + (j - back) * Data.DAY;
	    int da =
                (int)(6*compoundw * (data.getDawn(t) % Data.DAY) / Data.DAY);
	    int du =
                (int)(6*compoundw * (data.getDusk(t) % Data.DAY) / Data.DAY);
	    g.drawRect(doff + da, line*compoundh, (du - da), height*compoundh);

            // Highlight today
            if (j == back) {
                g.fillRect
                    (0, line*compoundh, 6*compoundw + doff, height*compoundh);
            }
            else {
                g.drawRect
                    (0, line*compoundh, 6*compoundw + doff, height*compoundh);
            }
            g.setColor(old);
            // Continue
            line += height;
        }
        g.setColor(Color.lightGray);
        g.drawRect(doff, 0, 1, line*compoundh);
        g.setColor(old);

        // Events
        line = 0;
        height = 0;
	for (int j = 0; j < (front + back); j++) {
            TreeSet ts = new TreeSet
                (new MyComparator(t0 + (j - back) * Data.DAY, Data.DAY));
            ArrayList wd = (ArrayList) eList.get(j);
            ts.addAll(wd);
            wd = new ArrayList(ts);

            old = g.getColor();
            height = (wd != null ? wd.size() : 0);
            if (j == back && height == 0) height = 1;

            // Get calendar item strings                
            for (int k = 0; wd != null && k < wd.size(); k++) {
                TimeEvent wdk = (TimeEvent) wd.get(k);
                float time[] = wdk.isActive
                    (t0 + (j - back)*Data.DAY, t0 + (j + 1 - back)*Data.DAY);
                int top = (line + k)*compoundh;
                // This shouldn't happen, but it does!?
                if (time == null || time[0] == time[1]) continue;
                int left = doff + (int)(6 * compoundw * time[0]);
                int width = (int)(6 * compoundw * (time[1] - time[0]));
                Rectangle rect = new Rectangle(left, top, width, compoundh);
                rect2events.put(rect, wdk);

                g.drawRoundRect(left, top, width, compoundh, 8, 8);
                String sa0 = wdk.type();
                String sa1 = wdk.getName();
                String sa = sa0 + sa1;
                int wf = fm.stringWidth(sa);

                // Draw image instead of s1
                ImageIcon icon = wdk.getIcon();
                if (icon != null) {
                    drawIcon(g, icon, left, top, compoundh);
                    left += icon.getIconWidth() + 2;
                }
                drawItem(g, sa0, 0, left, top, width, compoundh, hf, wf);
                drawItem(g, sa1, 1, left, top, width, compoundh, hf, wf);
            }
            // Draw dates
            if ((wd != null && wd.size() != 0) || (j == back)) {
                // Time offset from today
                long t = state.getDate() + (j - back) * Data.DAY;

                // Show date (pathological t < 0 allowed!)
                String dat = main.myData.date2string(t);
                dat = dat.substring(0, dat.lastIndexOf('-'));
                int wf = fm.stringWidth(dat);

                g.drawString(dat, doff - wf - 7, line*compoundh + 3*hf/2);
                g.drawString(dat, doff - wf - 8, line*compoundh + 3*hf/2);
            }
            // Draw cursor on top
            if (j == back) {
                old = g.getColor();
                int x = doff + (int)
                    ((state.getDate() % Data.DAY) * 6*compoundw / Data.DAY);
                g.setColor(Color.darkGray);
                g.fillRect
                    (x - compoundw/40, line * compoundh, compoundw/20,
                     height*compoundh);
                g.setColor(old);
            }
            // Continue
            line += height;
	}
    }

    /**
     * Get day (noot date) of a given line on the display. We need to
     * recalculate everything, since the relevant data is not kept.
     */
    public int getDate(int y) {
	State state = main.getState();
        ArrayList eList = state.getAllEvents();
        
        // Background
        long t0 = state.getDate() - state.getDate() % Data.DAY;
        int line = 0;

	for (int j = 0; j < (front + back); j++) {
            TreeSet ts = new TreeSet
                (new MyComparator(t0 + (j - back) * Data.DAY, Data.DAY));
            ArrayList wd = (ArrayList) eList.get(j);
            ts.addAll(wd);
            wd = new ArrayList(ts);

            int height = (wd != null ? wd.size() : 0);
            if (j == back && height == 0) height = 1;

            // Get calendar item strings                
            for (int k = 0; wd != null && k < wd.size(); k++) {
                TimeEvent wdk = (TimeEvent) wd.get(k);
                float time[] = wdk.isActive
                    (t0 + (j - back)*Data.DAY, t0 + (j + 1 - back)*Data.DAY);

                int top = (line + k)*compoundh;

                // This shouldn't happen, but it does!?
                if (time == null || time[0] == time[1]) continue;

                if (y > top && y < top + compoundh)
                    return (int)((t0 + (j - back) * Data.DAY) / Data.DAY);
            }
            // Continue
            line += height;
	}
        return 0;
    }

    /**
     * Draws an icon at the specified position
     * @param g graphics object to draw from
     * @param ic icon to draw
     * @param x left side
     * @param y top side
     * @param v height to fill
     */
    private void drawIcon(Graphics g, ImageIcon ic, int x, int y, int v) {
	int w = ic.getIconWidth();
	int h = ic.getIconHeight();
	g.drawImage(ic.getImage(), x + 4, y + (v - h)/2, w, h, this);
    }

    /**
     * Returns our width for the weather only.
     * @return width
     */
    int getCalendarWidth() { return getPreferredSize().width - doff; }

    /**
     * Returns our height for the weather only.
     * @return height
     */
    int getCalendarHeight() { return getPreferredSize().height; }

    /**
     * Returns X for weather only.
     * @param x on the canvas
     * @return x on the weather canvas
     */
    int getCalendarX(int x) { return x - doff; }

    /**
     * Returns Y for weather only.
     * @param y on the canvas
     * @return y on the weather canvas
     */
    int getCalendarY(int y) { return y; }

    /**
     * Draws a calendar item. This may result in text or in an icon, if one is
     * found.
     * @param g graphics object to draw from
     * @param name name of the calendar item
     * @param idx corner index (0, 1, 2, 3)
     * @param x left side
     * @param y top side
     * @param w width
     * @param hf height of text in font
     * @param wf width of text in font
     */
    private void drawItem(Graphics g, String name, int idx, int x, int y, int w, int h, int hf, int wf) {
        if (name != null) {
            Shape sh = g.getClip();
            g.clipRect(x + 2, y, w - 4, h);
            switch (idx) {
                case 0: g.drawString(name, x + 4, y + 1*hf); break;
                case 1: g.drawString(name, x + 4, y + 2*hf); break;
            }
            g.setClip(sh);
        }
    }

    /**
     * Find a rectangle for an event at a given location.
     * @param x x-coordinate
     * @param y y-coordinate
     * @return rectangle for an event at location
     */
    public Rectangle findEventRect(int x, int y) {
        int wm = getWidth();
        int hm = getHeight();
        // Out of bounds
        if (x < 0 || y < 0 || x > wm || y > hm) return null;
        Point p = new Point(x,y);

        Iterator iter = rect2events.keySet().iterator();
        while (iter.hasNext()) {
            Rectangle rect = (Rectangle) iter.next();
            if (rect.contains(p)) return rect;
        }
        return null;
    }

    /**
     * Find an event at a given location.
     * @param x x-coordinate
     * @param y y-coordinate
     * @return component at coords (or null)
     */
    public TimeEvent findEvent(int x, int y) {
        Rectangle rect = findEventRect(x, y);
        if (rect == null) return null;
        return (TimeEvent)rect2events.get(rect);
    }

    /**
     * Popup the details of the event
     * @param rect rectangle that references the evnt
     */
    public void popUp(Rectangle rect) {
        TimeEvent ev = (TimeEvent) rect2events.get(rect);
        ev.details();
    }

    /** Class to sort events */
    class MyComparator implements Comparator {
        /** Start time */
        long a, b;
        /** Constructor */
        MyComparator(long aStart, long duration) { a = aStart; b = a + duration; }
        /** IF method */
        public int compare(Object o1, Object o2) {
            if (o1 == null || o2 == null) return 1;
            float[] o1a = ((TimeEvent) o1).isActive(a, b);
            float[] o2a = ((TimeEvent) o2).isActive(a, b);
            if (o1a == null || o2a == null) return 1;
            int ret = (int) (Data.DAY * (o1a[0] - o2a[0]));
            return ret == 0 ? 1 : ret;
        }
        /** IF method */
        public boolean equals(Object obj) { return false; }
    }
}
