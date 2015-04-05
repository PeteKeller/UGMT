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
 * Class that contains the GUI references to a character sheet export, which
 * need to be queried to determine output.
 * @author Michael Jung
 */
class CharSheetsExport extends HttpExport {
    /** List of all characters */
    private Hashtable chars;

    /** Local source for all character files */
    private String source;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    CharSheetsExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "chars", "Player character HTML sheets", panel, gridy);
        chars = new Hashtable();
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

        if (query.indexOf(".html") > -1) {
            // Get code
            query = query.substring(0, query.indexOf(".html"));

            osr.println("Content-type: text/html");
            osr.println();

            // Read file
            if (chars.get(query) != null) {
                File f = ((Char)chars.get(query)).getPage();
                if (source == null) source = setSource(f);

                BufferedReader br = new BufferedReader
                    (new FileReader(f));
                String str;
                while ((str = br.readLine()) != null) {
                    osr.println(str);
                }
                br.close();
            }
            else {
                // Error
                osr.println("<html><head><body>Unknown character query</body></html>");
            }
            return;
        }

        // Everything else must be an image
        String ext = query.substring(query.lastIndexOf('.')+1);
        File img = getImage(query);
        osr.println("Content-type: image/" + ext);
        osr.println();

        FileInputStream fis = new FileInputStream(img);
        byte[] tmp = new byte[1024];
        int len;
        while ((len = fis.read(tmp)) > -1) {
            os.write(tmp);
        }
    }

    /**
     * Get the referenced images for a character sheet. The name is part of
     * the URI.
     * @param name name of image
     */
    private File getImage(String name) {
        // De-URI for Windows
        name = Framework.osName(name);
        return new File(source + main.myFrw.SEP + name);
    }

    /**
     * Set the exported files' sources, so it can be used as starting point
     * for referenced images.
     */
    private String setSource(File f) {
        String ret = f.getAbsolutePath();
        return ret.substring(0, ret.lastIndexOf(f.getName()) - 1);
    }

    /** 
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
        Char c = (Char) obj;
        if (c.isValid())
            chars.put(c.getCode(), c);
        else
            chars.remove(c.getCode());
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {
        main.myFrw.listen(this, Char.TYPE, null);
    }
}
