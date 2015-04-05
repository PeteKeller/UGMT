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
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;

/**
 * Class that contains the GUI references to a sketch export, which
 * need to be queried to determine output.
 * @author Michael Jung
 */
class SketchExport extends HttpExport {
    /** Sketch root */
    private Sketch root;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    SketchExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "sketch", "Sketches / Images", panel, gridy);
    }

    /**
     * Called by the exporting Background thread upon a request.
     * @param query queried export
     * @param os output stream for direct access
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        try {
            // Get Subqueries (and observe security a bit)
            if (query.endsWith(".png") ||
                query.endsWith(".jpg") ||
                query.endsWith(".gif")) {
                File f = new File(main.myPath + ".." + Framework.SEP + query);
                osr.println("Content-type: image/png\n\n");
                BufferedImage b = readImage(f);
                ImageIO.write(b, "png", os);
                return;
            }

            // Localize query 
            String[] names = query.split("/");

            // Depth first
            Sketch current = root;
            String base = "sketch/";
            for (int i = 1; (current != null) && (i < names.length); i++) {
                if (names[i].length() == 0) continue;
                Sketch[] subs = current.getSubSketches();
                int j = 0;
                while ((subs != null) && (j < subs.length)) {
                    if (subs[j].getName().equals(names[i])) {
                        base += names[i] + "/";
                        current = subs[j];
                        break;
                    }
                    j++;
                }
                if (subs == null || j == subs.length)
                    throw new Exception(query);
            }

            if (!current.isExported())
                throw new Exception();

            // Display
            if (current != null) {
                Sketch[] subs = current.getSubSketches();
                // Directory
                if (subs != null) {

                    osr.println("Content-type: text/html\n");
                    osr.println("<html>\n<head>\n</head>\n<body>\n<table>");

                    for (int i = 0; (subs != null) && (i < subs.length); i++) {
                        if (subs[i].isExported()) {
                            osr.print("<tr><td><a href=\"/" + base);
                            osr.print(subs[i].getName() + "\">");
                            osr.println(subs[i].getName() + "</a></td></tr>");
                        }
                    }
                    osr.println("</table>\n</body></html>");
                }
                // Image
                else {
                    File f = current.getContent();
                    if (f.getName().endsWith(".txt")) {
                        osr.println
                            ("Content-type: text/plain charset=utf-8\n\n");
                        BufferedReader br = new BufferedReader
                            (new FileReader(f));
                        String in;
                        while ((in = br.readLine()) != null)
                            osr.println(in);
                    }
                    if (f.getName().endsWith(".html")) {
                        osr.println("Content-type: text/html\n");
                        osr.println("<html>\n<head>");
                        BufferedReader br = new BufferedReader
                            (new FileReader(f));
                        String in;
                        while ((in = br.readLine()) != null) {
                            in = in.replaceAll("img src=\"", "img src=\"/");
                            osr.println(in);
                        }
                    }
                    if (f.getName().endsWith(".png")) {
                        osr.println("Content-type: image/png\n\n");
                        BufferedImage b = readImage(f);
                        ImageIO.write(b, "png", os);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            osr.println("HTTP/1.1 403 FORBIDDEN");
            osr.println();
            osr.println("<html><head><body>Forbidden</body></html>");
        }
    }

    /** Read an image */
    private BufferedImage readImage(File png) throws IOException {
        BufferedImage buf = ImageIO.read(png);

        // Reduce white space
        int sx1 = buf.getWidth();
        int sy1 = buf.getHeight();
        int sx2 = 0, sy2 = 0;
        for (int i = 0; i < buf.getHeight(); i++) {
            for (int j = 0; j < buf.getWidth(); j++) {
                int rgb = buf.getRGB(j, i);
                if ((rgb & 0xffffff) != 0xffffff) {
                    sx1 = Math.min(sx1, j);
                    sx2 = Math.max(sx2, j+1);
                    sy1 = Math.min(sy1, i);
                    sy2 = Math.max(sy2, i+1);
                }
            }
        }
        return buf.getSubimage(sx1, sy1, sx2 - sx1, sy2 - sy1);
    }

    /** 
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
        Sketch c = (Sketch) obj;
        root = (c.isValid() ? c : null);        
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {
        main.myFrw.listen(this, Sketch.TYPE, null);
    }
}
