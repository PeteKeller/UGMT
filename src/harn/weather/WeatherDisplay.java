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

import java.awt.*;
import java.util.Properties;
import javax.swing.*;

/**
 * Class that represents the Weather.  It handles the GUI and the cursor icon
 * that displays the group's time position.
 * @author Michael Jung
 */
public class WeatherDisplay extends JComponent {
    /** Date offset in pixels */
    private static int doff = 64;

    /** root reference */
    private Main main;

    /** Currently displayed list */
    WeatherUnit[] wList;

    /** yesterdays to display */
    private int back;

    /** tomorrows to display */
    private int front;

    /** Export sizes (both equal zero means unused) */
    private int exp_min = 0;
    private int exp_max = 0;

    /**
     * Constructor. Load mark and prepare layout.
     * @param props properties
     * @param main root reference
     */
    WeatherDisplay(Properties props, Main aMain) {
	super();
	main = aMain;

	// Throw exception if non-integers
	int w = Integer.parseInt(props.getProperty("compound.width"));
	int h = Integer.parseInt(props.getProperty("compound.height"));

	back = Integer.parseInt(props.getProperty("yesterdays"));
	front = Integer.parseInt(props.getProperty("tomorrows"));

	// Layout & load cursor
	setPreferredSize(new Dimension(6*w + doff + 1, (front + back)*h + 1));
    }
    
    /**
     * Returns our width for the weather only.
     * @return width
     */
    int getWeatherWidth() { return getPreferredSize().width - doff; }

    /**
     * Returns our height for the weather only.
     * @return height
     */
    int getWeatherHeight() { return getPreferredSize().height; }

    /**
     * Returns X for weather only.
     * @param x on the canvas
     * @return x on the weather canvas
     */
    int getWeatherX(int x) { return x - doff; }

    /**
     * Returns Y for weather only.
     * @param y on the canvas
     * @return y on the weather canvas
     */
    int getWeatherY(int y) { return y; }

    /**
     * Get export dimension.
     * @return expoort dimension
     */
    public int[] getExportSize() {
        if (exp_min + exp_max == 0)
            return new int[] { 0, 0, 0, 0 };
	int hm = getWeatherHeight()/(front + back);
        int wt = (int)(main.getState().getDate()/main.WATCH) % 6;
        int y1 = (back*6 + wt - exp_min)/6*hm;
        int y2 = (back*6 + wt + 6 + exp_max)/6*hm;
        return new int[] { 0, y1, getPreferredSize().width, (y2 - y1) + 1 };
    }

    /**
     * Set export bounds.
     * @param min back watches
     * @param max forward watches
     */
    void setBounds(int min, int max) { exp_min = min; exp_max = max; }

    /**
     * Reset export bounds.
     */
    void resetBounds() { exp_min = exp_max = 0; }

    /**
     * Callback for the system GUI manager. Redraws weather display and
     * mark/cursor.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) {
	// Get Font data
	FontMetrics fm = getFontMetrics(getFont());
	int hf = fm.getHeight();
        g.setColor(Color.black);

	int wm = getWeatherWidth()/6;
	int hm = getWeatherHeight()/(front + back);

	Data data = main.myData;
	State state = main.getState();

        // Time offset from today
        int wt = (int)(state.getDate()/main.WATCH) % 6;

	for (int j = 0; j < (front + back); j++) {
            long t = state.getDate() + (j - back) * main.DAY;
            if (exp_min + exp_max != 0 &&
                ((j - back)*6 - wt + 5 < -exp_min ||
                 (j - back)*6 - wt > exp_max)) continue;

	    // Get the time of dawn and dusk and scale it to display
	    int da = (int)(6*wm * (data.getDawn(t) % main.DAY) / main.DAY);
	    int du = (int)(6*wm * (data.getDusk(t) % main.DAY) / main.DAY);

	    // Color background
	    Color old = g.getColor();
	    g.setColor(Color.lightGray);
	    g.fillRect(doff, j*hm, 6*wm + 1, hm);
	    g.setColor(Color.white);
	    g.fillRect(doff + da, j*hm, (du - da), hm);
	    g.setColor(old);

	    // Show date (pathological t < 0 allowed here)
	    String dat = Data.stringDate(t);

            if (dat != null) {
                int idx = dat.lastIndexOf('-');
                if (idx < 0) idx = dat.lastIndexOf('.');
                dat = dat.substring(0, idx);
            }
            else {
                dat = Long.toString(t/Main.DAY);
            }

	    int wf = fm.stringWidth(dat);
	    g.drawString(dat, doff - wf - 7, j*hm + 3*hf/2);
	    g.drawString(dat, doff - wf - 8, j*hm + 3*hf/2);
        }

	for (int j = 0; j < (front + back); j++) {
	    // Display info in all four corners
	    for (int i = 0; i < 6; i++) {
		// NE corner
		int top = j*hm;
		int left = doff + i*wm;

		// Get weather item strings
		if (wList == null) continue;
                if (exp_min + exp_max == 0 ||
                    i + (j - back)*6 - wt >= -exp_min &&
                    i + (j - back)*6 - wt <= exp_max) {
		    String[] wd = weatherDetails(wList[i + j*6]);
		    g.drawRoundRect(left, top, wm, hm, 8, 8);

		    int ioff = 0; // icon offset
		    for (int k = 0; k < 4; k++) {
			int wf = fm.stringWidth(wd[k]);
			ioff = drawItem
			    (g, wd[k], k, left, top, wm, hm, hf, wf, ioff);
		    }
		}
	    }
	}

	// Draw cursor on top
        Color old = g.getColor();
	int x = (int)((state.getDate() % main.DAY) * 6*wm / main.DAY) + doff;
        g.setColor(Color.darkGray);
        g.fillRect(x - wm/40, back*hm, wm/20, hm);
        g.setColor(old);
    }

    /**
     * Give weather details as printable strings. The returned array contains
     * temperature, winddir (windspeed), cloudcover, event
     * @param w the weather unit
     */
    private String[] weatherDetails(WeatherUnit w) {
	String[] ret = new String[4];
	ret[0] = w.clouds;
	ret[1] = w.event;
	ret[2] = w.temperature;
	ret[3] = w.winddir + "(" + w.windspeed + ")";
	return ret;
    }

    /**
     * Draws a weather item. This may result in text or in an icon, if one is
     * found.
     * @param g graphics object to draw from
     * @param name name of the weather item
     * @param idx corner index (0, 1, 2, 3)
     * @param x left side
     * @param y top side
     * @param w width
     * @param h height
     * @param hf height of text in font
     * @param wf width of text in font
     * @param ioff icon offset
     */
    private int drawItem(Graphics g, String name, int idx, int x, int y, int w, int h, int hf, int wf, int ioff) {
	ImageIcon ic = main.myData.getIcon(name);
	if (ic != null) {
	    int iw = ic.getIconWidth();
	    int ih = ic.getIconHeight();
	    g.drawImage
		(ic.getImage(), x + 2 + ioff, y + (h - ih)/2, iw, ih, this);
	    return ioff + iw + 2;
	}
	else {
	    switch (idx) {
		case 0: g.drawString(name, x + 4, y + 1*hf); break;
		case 1: g.drawString(name, x + 4, y + 2*hf); break;
		case 2: g.drawString(name, x + w - wf - 4, y + 1*hf); break;
		case 3: g.drawString(name, x + w - wf - 4, y + 2*hf); break;
	    }
	    return ioff;
	}
    }
}
