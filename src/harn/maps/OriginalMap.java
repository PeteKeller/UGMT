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

import harn.repository.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.color.*;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Class that keeps the original map data.
 * @author Michael Jung
 */
public class OriginalMap {
    /** Main reference */
    private Main main;

    /** Current map icons */
    private Image[] currentImg;

    /** Current masks */
    private BufferedImage[] currentMsk;

    /** Current masks data*/
    private byte[][][] cmskdata;

    /** fog inner drawing width */
    private int fpwidth;

    /** fog pen shadow width */
    private int fpshadow;

    /** Current mask data */
    private float[][] currmask;

    /** The buffered map for speed */
    private BufferedMap bufMap;

    /** Map holder */
    private MapHolder holder;

    /** Constructor */
    OriginalMap(Main aMain, MapHolder pholder) {
        main = aMain;
        fpwidth = (int)Long.parseLong(main.myProps.getProperty("fog.pen.width"));
        fpshadow = (int)Long.parseLong(main.myProps.getProperty("fog.pen.shadow"));
        holder = pholder;
        bufMap = new BufferedMap();
    }

    /** Set bg color for buffer (can't be done on startup) */
    public void setColor(Color bg) { bufMap.setColor(bg); }

    /** Use the fast buffer for display */
    public void useFastBuffer(Shape p) { bufMap.useFastBuffer(p); }

    /** Set base */
    public void set(ScaledMap aMap) {
        if (aMap == null) {
            currentImg = null;
        }
        else {
            currentImg = ((Data.MyMap)aMap).getImages();
            bufMap.setFastBuffer(currentImg, main);
        }
    }

    /** Reset mask */
    public void resetMask() {
        currentMsk = new BufferedImage[currentImg.length];
        cmskdata = new byte[currentImg.length][][];
    }

    /** Get bound rectangle */
    public Shape getBounds() { return new Rectangle(0, 0, getWidth(), getHeight()); }

    /** Not initialized yet */
    public boolean isEmpty() { return currentImg == null; }

    /** Width */
    public int getWidth() { return currentImg[0].getWidth(null); }

    /** Height */
    public int getHeight() { return currentImg[0].getHeight(null); }

    /** Draw into bufmap */
    public void draw(Graphics2D g, float scale) {
        bufMap.beginDraw(g, currentImg[0], scale);
        for (int i = 0; i < currentImg.length; i++) {
            if (currentImg[i] == null || !main.showLayer(i))
                continue;
            bufMap.draw
                (g, currentImg[i],
                 main.bShowFog.isSelected() ? cmskdata[i][0] : null,
                 scale);
        }
        bufMap.endDraw(g, scale);
    }

    public void hideAll(int layer, boolean hide) {
        if (currentMsk != null && currentMsk.length > layer &&
            currentMsk[layer] != null) {
            byte fill = (byte)(hide ? 0x0 : 0xff);
            for (int i = 0; i < cmskdata[layer][0].length; i++)
                cmskdata[layer][0][i] = fill;
        }
    }

    /**
     * Stain the layer as requested.
     * @param layer layer to stain
     * @param shape shape to stain
     * @param y y-coord
     * @param hide hide or un-hide
     */
    public void stain(int layer, Shape shape, boolean hide) {
        if (isEmpty()) return;

        // Anti-scale shape
        float scale = main.map.getScale();
        AffineTransform trans =
            AffineTransform.getScaleInstance(1/scale, 1/scale);
        Rectangle rect;
        if (shape != null) {
            shape = trans.createTransformedShape(shape);
            rect = shape.getBounds();
        }
        else {
            rect = new Rectangle(0, 0, getWidth(), getHeight());
        }

        int x0 = rect.x;
        int y0 = rect.y;
        int w = rect.width;
        int h = rect.height;

        for (int i = 0; i < currentImg.length; i++) {
            if (currentImg[i] == null) continue;
            if (!main.showLayer(i)) continue;

            if (i == layer) {
                if (currentMsk[i] != null) {
                    Color col = new Color(hide ? 0x0 : 0xffffff);
                    Graphics2D mgc = (Graphics2D) currentMsk[i].getGraphics();
                    mgc.setRenderingHint
                        (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    mgc.setColor(col);
                    mgc.fill(shape);
                }
            }
        }
    }
    
    /**
     * Stain the layer as requested.
     * @param layer layer to stain
     * @param x0 point 1
     * @param y0 point 1
     * @param x1 point 2
     * @param y1 point 2
     */
    public void stain(int layer, int x0, int y0, int x1, int y1) {
        if (isEmpty()) return;
        if (!main.bShowFog.isSelected()) return;

        // Anti-scale point
        float scale = main.map.getScale();
        x0 = (int)(x0/scale);
        y0 = (int)(y0/scale);
        x1 = (int)(x1/scale);
        y1 = (int)(y1/scale);

        for (int i = 0; i < currentImg.length; i++) {
            if (currentImg[i] == null) continue;
            if (!main.showLayer(i)) continue;
        }
    }

    /**
     * Stain the layer as requested.
     * @param layer layer to stain
     * @param x x-coord
     * @param y y-coord
     * @param hide hide or un-hide
     * @return modified rectangle
     */
    public Shape stain(int layer, int xc, int yc, boolean hide) {
        if (isEmpty()) return null;
        if (!main.bShowFog.isSelected()) return null;

        // Anti-scale point
        float scale = main.map.getScale();
        xc = (int)(xc/scale);
        yc = (int)(yc/scale);
        float fps = (int)(fpshadow/scale);
        float fpw = (int)(fpwidth/scale);

        // Bounds in original (this)
        int[] coords = getBoundedCoords
            (xc, yc, getWidth(), getHeight(), fps);
        int x0 = coords[0];
        int y0 = coords[1];
        int w = coords[2];
        int h = coords[3];

        for (int i = 0; i < currentImg.length; i++) {
            if (currentImg[i] == null) continue;
            if (!main.showLayer(i)) continue;

            byte[] msk = null;
            if (main.bShowFog.isSelected()) {
                msk = cmskdata[i][0];
                // Modify
                if (i == layer) {
                    modifyMask
                        (xc, yc, w, h, hide, fps, fpw, msk, x0, y0, currentImg[i].getWidth(null));
                }
            }
        }
        return AffineTransform.getScaleInstance(scale,scale).
            createTransformedShape(new Rectangle(x0, y0, w, h));
    }

    /**
     * Redraw.
     * @param layer layer to stain
     * @param hide hide or un-hide
     */
    public void redraw(int layer, boolean hide, Shape shape) {
        if (isEmpty()) return;
        float scale = main.map.getScale();
        stain(layer, shape, hide);
    }

    /**
     * Stain the mask. [0,fpw) = 1, [fpw, fps) = linear, [fps, inf) = 0.
     */
    private void modifyMask(int xc, int yc, int w, int h, boolean show, float fps, float fpw, byte[] mdata, int mx, int my, int mw) {
        if (currmask == null || currmask.length != w || currmask[0].length != h) {
            currmask = new float[w][h];
            for (int yi = 0; yi < h; yi++) {
                for (int xi = 0; xi < w; xi++) {
                    // Center is 1, boundary is 0
                    float a =
                        (fps - (float)Math.sqrt
                         ((xi - w/2)*(xi - w/2) +
                          (yi - h/2)*(yi - h/2)))/
                        (float)(fps - fpw);
                    if (a < 0) a = 0;  
                    if (a > 1) a = 1;
                    currmask[xi][yi] = a;
                }
            }
        }
        for (int yi = 0; yi < h; yi++) {
            for (int xi = 0; xi < w; xi++) {
                // Smooth masking
                float a = currmask[xi][yi];
                float b = getAlpha(mdata[mw*(my + yi) + (xi + mx)]);
                byte c = 0;
                if (!show)
                    c = setAlpha(a + b - a*b);
                else
                    c = setAlpha(b - a*b);
                mdata[mw*(my + yi) + (mx + xi)] = c;
            }
        }
    }

    /**
     * Get alpha value
     */
    private float getAlpha(byte gray) { return ((int)gray&0xff)/255f; }

    /**
     * Set alpha value
     */
    private byte setAlpha(float alpha) { return (byte)(255f*alpha); }

    /**
     * Get the bounded coords. Convert center and unbounded width to rect
     * coords in image frame. It is assumed that (x,y) lie within the
     * rectangle.
     * @param x x-center
     * @param y y-center
     * @param w full width
     * @param h full height
     * @param w0 radius about center
     */
    private int[] getBoundedCoords(int x, int y, int w, int h, float w0) {
        int[] coords = new int[4];
        coords[0] = (int)Math.max(x - w0, 0);
        coords[1] = (int)Math.max(y - w0, 0);
        coords[2] = (int)Math.min(x + w0, w) - coords[0];
        coords[3] = (int)Math.min(y + w0, h) - coords[1];
        return coords;
    }

    /**
     * Save masks.
     */
    public void saveMasks(File f) {
        int w = getWidth();
        int h = getHeight();
        int size = main.getNumLayers();
        BufferedImage mask = new BufferedImage
            (size * w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int k = 0; k < size; k++) {
            if (currentMsk != null && currentMsk.length > k &&
                currentMsk[k] != null) {
                int[] mdata = new int[w*h];
                currentMsk[k].getRGB(0, 0, w, h, mdata, 0, w);
                mask.setRGB(k*w, 0, w, h, mdata, 0, w);
            }
        }

        try {
            ImageIO.write(mask, "png", f);
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }
    
    /**
     * Load masks.
     */
    public void loadMasks(File f, float scale) {
        int w = getWidth();
        int h = getHeight();
        int size = main.getNumLayers();

        try {
            BufferedImage mask = ImageIO.read(f);
            for (int k = 0; k < size; k++) {
                int[] mdata = new int[w*h];
                if (mask.getWidth() < k*w+w) break;
                mask.getRGB(k*w, 0, w, h, mdata, 0, w);
                if (currentMsk != null && currentMsk.length > k &&
                    currentMsk[k] != null) {
                    currentMsk[k].setRGB(0, 0, w, h, mdata, 0, w);
                }
            }
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /** Setup masks. I don't know, why I can't go below two bytes */
    public int setMasks() {
        int post = 0;
        // Prepare image
        for (int i = 0; i < currentMsk.length; i++) {
            if (currentMsk[i] == null) {
                cmskdata[i] = new byte[2][getWidth()*getHeight()];
                
                DataBuffer db = new DataBufferByte(cmskdata[i], cmskdata[i].length);
                int[] bmasks = {0, 1};
                int[] boff = {0, 0};
                WritableRaster wr = Raster.createBandedRaster
                    (db, getWidth(), getHeight(), getWidth(), bmasks, boff, null);
                
                currentMsk[i] = new BufferedImage
                    (new ComponentColorModel
                     (ColorSpace.getInstance(ColorSpace.CS_GRAY),
                      true, true, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE),
                     wr, false, null);

                Graphics mgc = currentMsk[i].getGraphics();
                ((Graphics2D)mgc).setRenderingHint
                    (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                mgc.setColor(new Color(0xffffff));
                mgc.fillRect(0, 0, getWidth(), getHeight());
            }

            if (!main.showLayer(i)) continue;
            post += (1<<i);
        }
        return post;
    }
}
