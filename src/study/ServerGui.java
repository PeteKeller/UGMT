package study;

import javax.imageio.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;

public class ServerGui extends JPanel {
    BufferedImage img0;
    BufferedImage img1;
    BufferedImage img2;
    BufferedImage mask0;
    BufferedImage mask1;
    BufferedImage mask2;
    boolean ready;
    Color col = new Color(0,0,0,64);
    int lx, ly;
    JLabel list;
    int[] pos = new int[2*10];
    int SZ = 50;
    ServerGui() throws IOException {
        ready = false;
        mask0 = new BufferedImage
            (1000, 1000, BufferedImage.TYPE_INT_ARGB);

        JFrame frame = new JFrame("Server");
        frame.getContentPane().setLayout(new BorderLayout());
        JButton send = new JButton("Send");
        send.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ready = true;
                }
            });
        JButton load = new JButton("Load");
        load.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.showOpenDialog((Component)e.getSource());
                    File f0 = fc.getSelectedFile();
                    /*
                    fc.showOpenDialog((Component)e.getSource());
                    File f1 = fc.getSelectedFile();
                    fc.showOpenDialog((Component)e.getSource());
                    File f2 = fc.getSelectedFile();
                    */
                    try {
                        img0 = ImageIO.read(f0);
                        //                        img1 = ImageIO.read(f1);
                        //                        img2 = ImageIO.read(f2);
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        list = new JLabel();
        addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    int[] data = new int[(2*SZ)*(2*SZ)];
                    PixelGrabber pg = new PixelGrabber
                        (mask0, x - SZ, y - SZ, (2*SZ), (2*SZ), data, 0, (2*SZ));
                    try { pg.grabPixels(); } catch (Exception ex) { }
                    for (int i = 0; i < (2*SZ); i++) {
                        for (int j = 0; j < (2*SZ); j++) {
                            data[i*(2*SZ) + j] = mymap(data, i, j);
                        }
                    }
                    mask0.setRGB(x - SZ, y - SZ, (2*SZ), (2*SZ), data, 0, (2*SZ));
                    repaint();
                }
            });

        frame.getContentPane().add(load, BorderLayout.NORTH);
        frame.getContentPane().add(send, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(this), BorderLayout.CENTER);
        frame.getContentPane().add(list, BorderLayout.WEST);
        frame.pack();
        frame.show();
    }

    private int mymap(int[] data, int i, int j) {
        int d = data[(2*SZ)*i + j];
        double s =  1-(Math.sqrt((i-SZ)*(i-SZ) + (j-SZ)*(j-SZ))/(double)SZ);
        if (s < 0) s = 0;
        if (s > 1) s = 1;
        double a = (d >>> 24)/255.0;
        return ((int)((a + s - a*s)*255) << 24) + 0x00ffffff;
    }

    public void add(int id) {
        list.setText(list.getText() + "," + id);
    }

    public void serverWrite(OutputStream os) throws IOException {
        System.out.println("1");
        ImageIO.write(img0, "png", os);
        System.out.println("2");
        ready = false;
    }

    public void set(int x, int y, int id) {
        pos[2*id] = x;
        pos[2*id + 1] = y;
        repaint();
    }

    public boolean ready() { return ready; }

    public void paintComponent(Graphics g) {
        if (img0 != null) {
            g.setColor(Color.WHITE);
            int w = img0.getWidth(null);
            int h = img0.getHeight(null);
            g.fillRect(0, 0, w, h);
            //            g.setClip(500, 700, 400, 300);
            g.drawImage(img0, 0, 0, w, h, this);
            //g.setClip(600, 800, 400, 300);
            //g.drawImage(img1, 0, 0, w, h, this);
            //g.setClip(700, 900, 400, 300);
            //    g.drawImage(img2, 0, 0, w, h, this);
        }
        g.drawImage(mask0, 0, 0, 1000, 1000, this);
        if (img0 == null) return;
        for (int i = 0; i < 10; i++)
            if (pos[2*i] != 0 || pos[2*i + 1] != 0) {
                Shape sh = new Ellipse2D.Float(pos[2*i] - 10, pos[2*i+1] - 10, 20, 20);
                g.setColor(Color.WHITE);
                ((Graphics2D)g).fill(sh);
                g.setColor(Color.BLACK);
                ((Graphics2D)g).draw(sh);
                g.drawString(Integer.toString(i), pos[2*i], pos[2*i+1] - 5);
            }
    }
    public Dimension getPreferredSize() {
        if (img0 == null) return super.getPreferredSize();
        int w = img0.getWidth(null);
        int h = img0.getHeight(null);
        return new Dimension(w, h);
    }
}
