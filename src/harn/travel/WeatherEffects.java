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

import harn.repository.Weather;
import rpg.*;

/**
 * Weather effects are calculated herein. This object listens to "Weather",
 * stores the active one, and provides methods for other objects of this
 * plugin to get the effects.
 * @author Michael Jung
 */
public class WeatherEffects implements IListener {
    /** Root reference */
    private Main main;

    /** Active weather */
    private Weather weath;

    /**
     * Constructor.
     * @param aMain root reference
     */
    WeatherEffects(Main aMain) { main = aMain; }

    /**
     * This method is called by the framework to inform about a changed or new
     * object.  Note that we remove an invalidated weather object.  Weather
     * will default then.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
	Weather w  = (Weather) obj;
	// The active object
	if (w.isActive()) {
	    weath = w;
	    main.redo(true);
	}
	/// The active object is gone
	if (!w.isActive() && w == weath) {
	    weath = null;
	    main.redo(true);
	}
    }

    /**
     * This method returns the condition travel takes place in. This is
     * matched against the travel tables to see if a modification of travel
     * rate takes place.
     * @param x x-coordinate for condition
     * @param x y-coordinate for condition
     * @return condition
     */
    String getCondition(int x, int y) {
	State st = main.getState();
	if (st != null && weath != null) {
	    String map = st.getMapName();
	    long date = st.getDate();
	    String ret = weath.condition(map, x, y, date);
	    if (ret != null && ret.length() > 0) return ret;
	}
	// Default
	return "dry";
    }
}
