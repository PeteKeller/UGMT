import java.io.*;
import java.awt.image.*;
import java.awt.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.spi.*;
import javax.swing.*;

public class SVG extends JComponent {
    BufferedImage buf;
    public static void main(String [] args) throws Exception {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        Iterator i = reg.getServiceProviders(ImageReaderSpi.class, false);
        while (i.hasNext()) {
            ImageReaderSpi rspi = (ImageReaderSpi)i.next();
            String[] sufs = rspi.getFileSuffixes();
            for (int j = 0; j < sufs.length; j++)
                System.out.println(sufs[j]);
        }
        // create the transcoder input
        File f = new File(args[0]);
        BufferedImage buff = ImageIO.read(f);

        System.out.println(buff);

        JFrame frame = new JFrame("Client");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new SVG(buff), BorderLayout.SOUTH);
        frame.pack();
        frame.show();
    }

    SVG(BufferedImage buff) { buf = buff; }

    /**
     * Returns current dimension.
     */
    public Dimension getPreferredSize() {
        if (buf == null) return super.getPreferredSize();
        int w = buf.getWidth(null);
        int h = buf.getHeight(null);
        return new Dimension(w, h);
    }
    
    /**
     * Callback for the system GUI manager. Redraw map.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) {
	int w,h;

	// Draw map
        if (buf != null) {
            w = buf.getWidth(null);
            h = buf.getHeight(null);
            g.drawImage
                (buf, 0, 0, w, h, 0, 0, w, h, this);
        }
        revalidate();
    }
}
