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
package harn.export;

import harn.repository.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import rpg.*;

/**
 * Class that contains the GUI references to an index plugin export, which
 * need to be queried to determine output. Also does the exporting itself.
 * @author Michael Jung
 */
class IndexExport extends HttpExport {
    /** Currently selected index page */
    private Location myLoc;

    /** Current group coordinates */
    private CharGroup act;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    IndexExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "index", "The selected index information page", panel, gridy);
    }

    /**
     * Called by the exporting thread upon a request for index export.
     * @param query queried export
     * @param os output stream for direct access (not used)
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        osr.println("Content-type: text/html");
        osr.println();
        if (myLoc != null) osr.println(myLoc.getPage());
    }

    /**
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
        if (obj instanceof CharGroup) {
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive()) {
                act = cg;
            }
        }

        if (obj instanceof Location) {
            Location loc = (Location) obj;
            if (loc.getSelected())
                myLoc = loc;
        }
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {
        main.myFrw.listen(this, CharGroup.TYPE, null);
        main.myFrw.listen(this, Location.TYPE, null);
    }
}
