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

import javax.sound.sampled.*;
import harn.repository.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import rpg.*;

/**
 * Class that contains the GUI references to an sound plugin export, which
 * need to be queried to determine output. Also does the exporting itself.
 * @author Michael Jung
 */
class SoundExport extends HttpExport {
    /** Sound queue */
    private harn.sound.Main plugin;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    SoundExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "sound", "The currently playing sound", panel, gridy);
    }

    /**
     * Called by the exporting thread upon a request for index export.
     * @param query queried export
     * @param os output stream for direct access (not used)
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        if (plugin == null) {
            Object o = main.myFrw.getPlugins("sound");
            // If o = null, the next cast will probably horribly fail, which
            // is why this is not a one-liner
            if (o != null) plugin = (harn.sound.Main) o;
        }
        osr.println("Content-type: audio/mpeg");
        osr.println();
        osr.flush();
        AudioInputStream stream = plugin.getExportStream();
        while (plugin != null && true && isSelected()) {
            byte[] out = new byte[10*1024];
            int read = stream.read(out);
            if (read < 0) read = out.length;
            try { Thread.sleep(100); } catch(InterruptedException e) {}
            os.write(out, 0, read);
            os.flush();
        }
    }

    /**
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {}

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {}
}
