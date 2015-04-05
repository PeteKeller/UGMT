package org.apache.batik.util;

import java.awt.image.*;
import java.awt.*;
import javax.imageio.*;
import javax.imageio.stream.*;

public class SVGReaderSpi extends javax.imageio.spi.ImageReaderSpi {
    public SVGReaderSpi() {
        suffixes = new String[] { "svg" };
        names = suffixes;
        MIMETypes = new String[] { "image/svg", "image/svg+xml" };
    }
    static final Class[] it = { ImageInputStream.class };
    public boolean canDecodeInput(Object source) {
        if (source instanceof FileImageInputStream) {
            FileImageInputStream fii = (FileImageInputStream) source;
            try {
                fii.readLine();
                String str = fii.readLine();
                if (str.matches(".*-//W3C//DTD *SVG.*")) return true;
                fii.reset();
            }
            catch(Exception e) {
                return false;
            }
        }
        return false;
    }
    public ImageReader createReaderInstance(Object extension) {
        return new SVGReader(this);
    }
    public String getDescription(java.util.Locale loc) {
        return "Reads SVG images";
    }
    public Class[] getInputTypes() { return it; }
    public String getPluginClassName() { return "org.apache.batik.util.SVGReaderSpi"; }
}
