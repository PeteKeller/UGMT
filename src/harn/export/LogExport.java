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

import rpg.*;
import harn.repository.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * Class that contains the GUI references to a log sheet export, which need to
 * be queried to determine output.
 * @author Michael Jung
 */
class LogExport extends HttpExport {
    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    LogExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "log", "Show log HTML sheets", panel, gridy);
    }

    /**
     * Called by the exporting Background thread upon a request.
     * @param query queried export
     * @param os output stream for direct access
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        // Localize query 
        query = query.substring(query.indexOf('/')+1);

        osr.println("Content-type: text/html");
        osr.println();

        // Read file
        try {
            File f = new File
                (main.myPath + ".." + Framework.SEP + name() +
                 Framework.SEP + query);

            BufferedReader br = new BufferedReader
                (new FileReader(f));
            String str;
            while ((str = br.readLine()) != null) {
                osr.println(str);
            }
            br.close();
        }
        catch (Exception e) {
            // Error
            osr.println("<html><head><body>Unknown log query</body></html>");
        }
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {}

    /** 
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {}
}
