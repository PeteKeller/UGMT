package org.apache.batik.util;

import org.apache.batik.transcoder.image.*;
import org.apache.batik.transcoder.*;
import java.awt.image.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.*;
import javax.imageio.metadata.*;

public class SVGReader extends ImageReader {
    private BufferedImage bi;
    public SVGReader(SVGReaderSpi spi) { super(spi); }
    public IIOMetadata getImageMetadata(int imageIndex) { return null; }
    public Iterator getImageTypes(int imageIndex) {
        HashSet hs = new HashSet(1);
        hs.add(new ImageTypeSpecifier(bi));
        return hs.iterator();
    }
    public int getNumImages(boolean allowSearch) { return 1; }
    public IIOMetadata getStreamMetadata() { return null; }
    public int getWidth(int imageIndex) { test(); return bi.getWidth(); }
    public int getHeight(int imageIndex) { test(); return bi.getHeight(); }
    public BufferedImage read(int imageIndex, ImageReadParam param) { test(); return bi; }
    private void test() {
        if (bi == null && input != null) {
            MyTranscoder t = new MyTranscoder();
            TranscoderInput is = new TranscoderInput(new MyIInputStream(input));
            try {
                t.transcode(is, null);
                bi = t.bi;
            }
            catch (TranscoderException e) {
                e.printStackTrace();
                // Ignore and hope for the best
            }
        }
    }

    class MyTranscoder extends ImageTranscoder {
        BufferedImage bi;
        public BufferedImage createImage(int width, int height) {
            bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return bi;
        }
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            // nothing to do
        }
    }

    class MyIInputStream extends InputStream {
        private ImageInputStream fii;
        MyIInputStream(Object o) { fii = (ImageInputStream) o; }
	public int available() throws IOException {
            System.out.println(fii.length());
            return (int)(fii.length() > 0 ? fii.length() - fii.getStreamPosition() : 1); }
        public void close() throws IOException { /* already closed */ }
        public void mark(int readlimit) { fii.mark(); }
        public boolean markSupported() { return true; }
	public int read() throws IOException { return fii.read(); }
        public int read(byte[] b) throws IOException { return fii.read(b); }
        public int read(byte[] b, int off, int len) throws IOException { return fii.read(b,off,len); }
        public void reset() throws IOException { fii.reset(); }
        public long skip(long n) throws IOException { return fii.skipBytes(n); }
    }
}
