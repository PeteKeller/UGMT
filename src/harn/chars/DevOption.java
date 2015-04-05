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

import org.w3c.dom.*;
import java.util.*;

/**
 * Class to hold development options.
 * @author Michael Jung
 */
public class DevOption {
    /** Main reference */
    private Main main;

    /** Subset of choice skills */
    private String[] opts;

    /** List of tests to match before allowing a choice */
    private ArrayList optset;

    /** Next option */
    private DevOption next;

    /** number of option points available */
    private int count;

    /** number of remaining option points */
    private int remain;

    /** function to calculate count (if required) */
    private String func;

    /** argument to functio to calculate count (if required) */
    private String arg;

    /**
     * Constructor.
     * @param prev previous option
     * @param aAmin main reference
     * @param node node with children
     * @param attr attributes already determined
     */
    public DevOption(DevOption prev, Main aMain, Node node, NamedNodeMap attr) {
        main = aMain;
        optset = new ArrayList();

        // Get skill group
        String subset = attr.getNamedItem("subset").getNodeValue();
        opts = subset.split(",");

        // Get count (if available)
        Node cnode = attr.getNamedItem("count");
        if (cnode != null)
            count = Integer.parseInt(cnode.getNodeValue());

        // Get function (if available)
        Node fnode = attr.getNamedItem("function");
        if (fnode != null) {
            func = fnode.getNodeValue();
            arg = attr.getNamedItem("arg").getNodeValue();
        }

        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("set")) {
                String oml = cattr.getNamedItem("oml").getNodeValue();

                String test = null;
                if (cattr.getNamedItem("test") != null)
                    test = cattr.getNamedItem("test").getNodeValue();

                String[] costs = null;
                if (cattr.getNamedItem("cost") != null)
                    costs = cattr.getNamedItem("cost").getNodeValue().split(",");
                optset.add(new OptionSetting(oml, test, costs, this));
            }
        }
        if (prev != null) prev.next = this;
        reset();
    }

    /**
     * Use a skill as given option.
     */
    public void useOption(int cost) { remain -= cost; }

    /**
     * Get remaining option points
     */
    public int getRemainingOpts() { return remain; }

    /**
     * Enable all skills for this option. If none remain or this option is
     * disabled, choose the next option setting. If they return false as well,
     * return the result.
     * @param enable enable/disable
     * @return whether something was actually done
     */
    public boolean enable() {
        // Set GUI label
        if (remain > 0)
            main.bOptions.setText("- CHOICE (" + remain + ") -");
        else
            main.bOptions.setText("- CHOICE -");

        // Distribute the options among GUI skills
        boolean hasopt = main.getData().enable(opts, optset);

        // If nothing left to choose, do attribute adjustments
        if (!hasopt)
            main.getState().getCurrentDev().adjust();

        // If no choice left, choose next.
        if (!hasopt && next != null)
            return next.enable();

        // Set reference, if needed
        if (hasopt) main.getState().setCurrentOpt(this);
        return hasopt;
    }

    /**
     * Reset the development options.
     */
    public void reset() {
        State state = main.getState();
        if (func != null && arg != null) {
            String myArg = state.getAttribute(arg);
            if (myArg == null || myArg.length() == 0) myArg = "0";
            count = Integer.parseInt
                (CharAttribute.evalFunc
                 (func, myArg, null, state.getCal(),
                  state.getAct(), state.getCurrent()));
        }
        remain = count;

        for (int i = 0; i < optset.size(); i++)
            ((OptionSetting)optset.get(i)).reset();
    }
}
