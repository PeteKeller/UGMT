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
package harn.chars;

import java.util.*;

/**
 * An option setting. Combines test, name of skill and oml value and possibly
 * cost. Also keeps a list of skills that have already used this option at
 * what current price.
 * @author Michael Jung
 */
class OptionSetting {
    /** name of skill, if null applies to all skills */
    String name;

    /** Test, if null always applies */
    String test;

    /** oml to apply in case test (and name) match */
    String oml;

    /** cost array, if empty, no cost */
    private int[] costs;

    /** back reference for option this setting belongs to */
    private DevOption dev;

    /** list of skill and their current index in the cost array */
    private Hashtable skills;
    
    /** Check if used once */
    private boolean usedOnce;

    /**
     * Constructor.
     * @param aName name of skill
     * @param aTest test for the skill
     * @param anOml OML to open the skill
     */
    public OptionSetting(String aName, String aTest, String anOml) {
        test = aTest;
        oml = anOml;
        name = aName;
        usedOnce = false;
    }

    /**
     * Constructor.
     * @param anOml OML to open the skill
     * @param aTest test for the skill
     * @param aCosts costs array for the skill
     * @param aDev back reference to containing option set
     */
    public OptionSetting(String anOml, String aTest, String[] aCosts, DevOption aDev) {
        name = null;
        test = aTest;
        oml = anOml;
        if (aCosts != null) {
            costs = new int[aCosts.length];
            for (int i = 0; i < costs.length; i++)
                costs[i] = Integer.parseInt(aCosts[i]);
        }
        skills = new Hashtable();
        dev = aDev;
    }

    /**
     * Check for sufficient funds for a given skill. 
     * @param skill name of skill
     * @return whether costs can be met by remaining option points.
     */
    public boolean costsMet(String skill) {
        if (costs != null) {
            Integer idx = (Integer) skills.get(skill);

            // Add a "0" if nothing present yet.
            if (idx == null) idx = new Integer(0);
            int i = idx.intValue();
            skills.put(skill, idx);

            return (costs.length > i && costs[i] <= dev.getRemainingOpts());
        }
        // Default - no costs used
        return !usedOnce;
    }

    /**
     * Reduce fonds. Assumes that that sufficiency was tested before.
     * @param skill name of skill
     */
    public void setCosts(String skill) {
        if (costs != null) {
            int i = ((Integer) skills.get(skill)).intValue();
            skills.put(skill, new Integer(i+1));
            dev.useOption(costs[i]);
        }
        else
            dev.useOption(1); // dummy - just to keep enabling going
        usedOnce = true;
    }

    /**
     * Reset this object.
     */
    public void reset() {
        usedOnce = false;
        skills.clear();
    }
}
