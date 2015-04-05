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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Class that contains the GUI references to a weather export, which
 * need to be queried to determine output.
 * @author Michael Jung
 */
class WeatherExport extends HttpExport {
    /** List of all characters */
    private Weather weath;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    WeatherExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "weather", "Weather condition and effects", panel, gridy);
    }

    /**
     * Called by the exporting Background thread upon a request.
     * @param query queried export
     * @param os output stream for direct access
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        BufferedImage bi = weath.getExport();

        osr.println("Content-type: image/png");
        osr.println();
        ImageIO.write(bi, "png", os);
    }

    /** 
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
        weath = (Weather) obj;
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {
        main.myFrw.listen(this, Weather.TYPE, null);
    }
}
