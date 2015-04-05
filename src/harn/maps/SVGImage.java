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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import org.apache.batik.bridge.*;
import org.apache.batik.dom.svg.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.util.*;
import org.w3c.dom.svg.*;

/**
 * Class that covers bitmap images for SVGs. Inherited methods should not
 * be called from this image, but from the delegate. For some reason the
 * direct method does not work.
 */
class SVGImage extends BufferedImage {
    /** underlying SVG */
    private SVGDocument doc;

    /** Renderer SVG to bitmap */
    private GraphicsNode gnode;

    /** Original width and height */
    private int w, h;

    /** Constructor */
    SVGImage(String surl) throws IOException {
        super(1, 1, BufferedImage.TYPE_INT_ARGB);
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory pfact = new SAXSVGDocumentFactory(parser);
        doc = pfact.createSVGDocument(surl);
        w = (int)Double.parseDouble(doc.getRootElement().getAttribute("width"));
        h = (int)Double.parseDouble(doc.getRootElement().getAttribute("height"));

        GVTBuilder gtb = new GVTBuilder();
        gnode = gtb.build(new BridgeContext(new UserAgentAdapter()), doc);
    }
    public int getHeight(ImageObserver observer) { return h; }
    public int getHeight() { return h; }
    public int getWidth(ImageObserver observer) { return w; }
    public int getWidth() { return w; }

    public BufferedImage getBitmap() {
        BufferedImage draw = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = (Graphics2D)draw.getGraphics();
        gc.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        draw((Graphics2D)draw.getGraphics(), null);
        return draw;
    }

    public void draw(Graphics2D gc, AffineTransform trafo) {
        gnode.setTransform(trafo);
        gnode.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gnode.setRenderingHint
            (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gnode.paint(gc);
    }
}
