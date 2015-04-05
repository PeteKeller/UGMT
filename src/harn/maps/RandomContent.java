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
package harn.maps;

import rpg.*;
import harn.repository.*;
import java.net.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;
import javax.imageio.*;
import java.util.*;
import java.io.*;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 * Random content in the right panel.
 * @author Michael Jung
 */
public class RandomContent {
    /** main reference */
    Main main;

    /** Constructor */
    public RandomContent(Main aMain) {
        main = aMain;
        randPath = Main.myPath + Framework.SEP + "Random" + Framework.SEP;
    }

    static final String GENERATOR =
        "http://localhost/miju/cgi-bin/out.pl?action=";

    static String randPath;

    static final RenderingHints rhints = new RenderingHints
        (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    /**
     * Get a random map, incorporate it into data and select it.
     */
    public void set() {
        try {
            main.myFrw.setBusy(true);
            String id = Long.toString(System.currentTimeMillis()/1000);
            id = id.substring(id.length() - 5);

            // Image
            URL gen = new URL(GENERATOR + "PNG&RAND=" + id);
            InputStream is = new BufferedInputStream(gen.openStream());
            BufferedImage res = ImageIO.read(is);
            File f = new File(randPath + id + ".png");
            ImageIO.write(res, "png", f);

            // Thumb
            AffineTransformOp op = new AffineTransformOp
                (AffineTransform.getScaleInstance(273/1631.0, 273/1631.0),
                 rhints);
            BufferedImage thumb = op.filter(res, null);
            f = new File(randPath + id + "_mini.png");
            ImageIO.write(thumb, "png", f);
            
            // Heightfield
            gen = new URL(GENERATOR + "HGF&RAND=" + id);
            is = new BufferedInputStream(gen.openStream());
            res = ImageIO.read(is);
            f = new File(randPath + id + "_hgf.png");
            ImageIO.write(res, "png", f);

            // XML data file
            Document doc = Framework.parse(randPath + "Random.xml", "atlas");
            
            Element map = doc.createElement("map");
            map.setAttribute("name", id);
            map.setAttribute("img", id + ".png");
            map.setAttribute("heightfield", id + "_hgf.png");
            map.setAttribute("thumbnail", id + "_mini.png");
            map.setAttribute("scale", "9900");
            map.setAttribute("tacticalspeed", "1");
            doc.getDocumentElement().appendChild(map);
            File file = new File(randPath + "Random.xml");
            Framework.transform(doc, new StreamResult(file), null);            

            // Reload
            main.resetTree();
            main.myData.loadData();
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
        finally {
            main.myFrw.setBusy(false);
        }
    }
}