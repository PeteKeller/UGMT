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
import java.awt.color.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 * Class that contains the intermediate map that is used to speed up things.
 * The scaling ios different for bitmaps and vectors. bitmap layers are first
 * composed and then scaled, while vector layers are first scaled and then
 * composed. The fastBuffer is used when drawing (mostly) bitmap
 * images. Scaling of the layer stack is easy, if vectors are not involved,
 * which is when the fastBuffer is used.
 * @author Michael Jung
 */
public class BufferedMap {
    /** Image buffer, used as intermediate canvas */
    private BufferedImage ibuffer;

    /** Temporary mask draw buffer */
    private BufferedImage mbuffer;

    /** Temp mask draw buffer */
    private byte[][] mbufdata;

    /** Dummy */
    private static byte[] manyBytes;

    /** Usage of the fastBuffer */
    private Shape useShape;

    /** Fast buffer */
    private BufferedImage fastBuffer;

    /** Background color */
    private Color bgCol;

    /** Constructor */
    public void setColor(Color pBG) { bgCol = pBG; }

    /** Use the fast buffer for display */
    public void useFastBuffer(Shape p) {
        useShape = p;
    }

    public void setFastBuffer(Image[] img, Main main) {
        main.myFrw.setBusy(true);
        fastBuffer = new BufferedImage
            (img[0].getWidth(null), img[0].getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D fbg = fastBuffer.createGraphics();
        fbg.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        fbg.setColor(new Color(0x00, true));
        fbg.fillRect(0, 0, img[0].getWidth(null), img[0].getHeight(null));
        for (int i = 0; i < img.length; i++) {
            if (img[i] == null || !main.showLayer(i))
                continue;
            if (img[i] instanceof SVGImage) break;
            fbg.drawImage(img[i], null, null);
        }
        main.myFrw.setBusy(false);
    }



    /**
     * Commit the drawing; uses fastBuffer, if it is being used at all.
     */
    public void beginDraw(Graphics2D g, Image img, float scale) {
        if (useShape != null) {
            Graphics2D fbg = fastBuffer.createGraphics();
            fbg.setRenderingHint
                (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Rectangle oRect = null;
            AffineTransform g2o = AffineTransform.getScaleInstance(1/scale, 1/scale);
            if (g.getClip() != null) {
                Rectangle r = g.getClip().getBounds().createUnion(useShape.getBounds()).getBounds();
                oRect = g2o.createTransformedShape(r).getBounds();
            }
            else {
                oRect = new Rectangle(0, 0, fastBuffer.getWidth(), fastBuffer.getHeight());
            }
            fbg.setColor(bgCol);
            fbg.fill(oRect);
        }
    }

    /**
     * Commit the drawing; uses fastBuffer, if it is being used at all.
     */
    public void endDraw(Graphics2D g, float scale) {
        if (useShape == null) {
            AffineTransform o2g =
                AffineTransform.getScaleInstance(scale, scale);
            g.drawImage(fastBuffer, o2g, null);
        }
    }

    /** Draw this map */
    public void draw(Graphics2D g, Image img, byte[] mdata, float scale) {
        if (img instanceof SVGImage) {
            draw(g, (SVGImage)img, mdata, scale);
            return;
        }

        // We shall use the fastBuffer
        if (useShape == null) return;

        g.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Rectangle oRect = null;
        AffineTransform g2o = AffineTransform.getScaleInstance(1/scale, 1/scale);
        AffineTransform o2g = AffineTransform.getScaleInstance(scale, scale);

        if (g.getClip() != null) {            
            Rectangle r = g.getClip().getBounds().createUnion(useShape.getBounds()).getBounds();
            oRect = g2o.createTransformedShape(r).getBounds();
        }
        else {
            oRect = new Rectangle(0, 0, img.getWidth(null), img.getHeight(null));
        }

        AffineTransform ominus = AffineTransform.getTranslateInstance
            (-oRect.x, -oRect.y);
        AffineTransform oplus = AffineTransform.getTranslateInstance
            (oRect.x, oRect.y);

        Rectangle oRect0 = new Rectangle(0, 0, oRect.width, oRect.height);
        if (ibuffer == null)
            ibuffer = new BufferedImage
                (oRect0.width, oRect0.height, BufferedImage.TYPE_INT_ARGB);
        if (ibuffer.getWidth() < oRect0.width ||
            ibuffer.getHeight() < oRect0.height)
            ibuffer = new BufferedImage
                (Math.max(ibuffer.getWidth(), oRect0.width),
                 Math.max(ibuffer.getHeight(), oRect0.height),
                 BufferedImage.TYPE_INT_ARGB);


        Graphics2D gb = ibuffer.createGraphics();
        gb.setClip(oRect0);
        gb.setComposite(AlphaComposite.Src);
        gb.setColor(new Color(0x00, true));
        gb.fill(oRect0);
        gb.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gb.drawImage
            (img, ominus, null);
        
        if (mdata != null) {
            gb.setComposite(AlphaComposite.DstIn);
            mbuffer = createMbuffer(oRect0.width, oRect0.height);
            int mw = img.getWidth(null);
            for (int j = 0; j < oRect0.height; j++) {
                int moff = Math.max(0,(oRect.y + j)*mw + oRect.x);
                int mboff = j*mbuffer.getWidth();
                int len = Math.min(oRect.width, mdata.length - moff);
                len = Math.min(len, mbufdata[3].length - mboff);
                if (len > 0)
                    System.arraycopy(mdata, moff, mbufdata[3], mboff, len);
            }
            gb.drawImage(mbuffer, null, 0, 0);
        }

        o2g.concatenate(oplus);
        g.drawImage
            (ibuffer, o2g, null);
        Graphics2D fbg = fastBuffer.createGraphics();
        fbg.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        fbg.setClip(oRect);
        fbg.drawImage
            (ibuffer, null, oRect.x, oRect.y);
    }

    /** Draw this map */
    public void draw(Graphics2D g, SVGImage img, byte[] mdata, float scale) {
        g.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Rectangle gRect = null;
        Rectangle oRect = null;
        AffineTransform g2o = AffineTransform.getScaleInstance(1/scale, 1/scale);
        AffineTransform o2g = AffineTransform.getScaleInstance(scale, scale);

        // Take care of previous fastBuffer usage
        if (useShape == null) {
            g.drawImage(fastBuffer, o2g, null);
            useShape = new Rectangle(0,0,0,0);
        }

        // Clipping
        if (g.getClip() != null) {
            gRect = g.getClip().getBounds().createUnion(useShape.getBounds()).getBounds();
            oRect = g2o.createTransformedShape(gRect).getBounds();
        }
        else {
            oRect = new Rectangle(0, 0, img.getWidth(null), img.getHeight(null));
            gRect = o2g.createTransformedShape(oRect).getBounds();
        }
        //Don't remember what this was for
        //gRect.grow(1,1);
        //oRect.grow(1,1);

        AffineTransform gminus = AffineTransform.getTranslateInstance
            (-gRect.x, -gRect.y);
        AffineTransform gplus = AffineTransform.getTranslateInstance
            (gRect.x, gRect.y);

        Rectangle gRect0 = new Rectangle(0, 0, gRect.width, gRect.height);
        if (ibuffer == null)
            ibuffer = new BufferedImage
                (gRect0.width, gRect0.height, BufferedImage.TYPE_INT_ARGB);
        if (ibuffer.getWidth() < gRect0.width ||
            ibuffer.getHeight() < gRect0.height)
            ibuffer = new BufferedImage
                (Math.max(ibuffer.getWidth(),gRect0.width),
                 Math.max(ibuffer.getHeight(),gRect0.height),
                 BufferedImage.TYPE_INT_ARGB);

        Graphics2D gb = ibuffer.createGraphics();
        gb.setClip(gRect0);
        gb.setColor(new Color(0x00, true));
        gb.setComposite(AlphaComposite.Src);
        gb.fill(gRect0);
        gb.setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        gminus.concatenate(o2g);
        img.draw(gb, gminus);

        if (mdata != null) {
            gb.setComposite(AlphaComposite.DstIn);
            mbuffer = createMbuffer(oRect.width, oRect.height);
            int mw = img.getWidth(null);
            for (int j = 0; j < oRect.height; j++) {
                int len = Math.min(oRect.width,
                                   mdata.length - ((oRect.y + j)*mw + oRect.x));
                len = Math.min(len,
                               mbufdata[3].length - (j*mbuffer.getWidth()));
                if (len > 0) {
                    System.arraycopy
                        (mdata, (oRect.y + j)*mw + oRect.x,
                         mbufdata[3], j*mbuffer.getWidth(), len);
                }
            }

            // Work around Java bug #4723021
            BufferedImage tmp = new BufferedImage
                (oRect.width, oRect.height, BufferedImage.TYPE_INT_ARGB);
            tmp.createGraphics().drawImage(mbuffer, null, null);

            gb.drawImage(tmp, o2g, null);
        }

        g.drawImage(ibuffer, gplus, null);
    }

    /** Create a temporary masking buffer */
    private BufferedImage createMbuffer(int w, int h) {
        if (manyBytes == null || manyBytes.length < w*h)
            manyBytes = new byte[w*h];
        else
            for (int i = 0; i < w*h; i++) manyBytes[i] = 0;

        if (mbuffer == null ||
            mbuffer.getWidth() != w || mbuffer.getHeight() != h) {
            mbufdata = new byte[4][];
            mbufdata[0] = mbufdata[1] = mbufdata[2] = mbufdata[3] = manyBytes;

            DataBuffer db = new DataBufferByte(mbufdata, mbufdata.length);
            int[] bmasks = { 0, 1, 2, 3 };
            int[] boff = {0, 0, 0, 0};
            WritableRaster wr = Raster.createBandedRaster
                (db, w, h, w, bmasks, boff, null);
            mbuffer = new BufferedImage
                (new ComponentColorModel
                 (ColorSpace.getInstance(ColorSpace.CS_sRGB),
                  true, true, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE),
                 wr, false, null);
        }
        return mbuffer;
    }
}
