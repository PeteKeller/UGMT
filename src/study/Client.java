package study;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
import java.awt.geom.*;
import java.net.*;
import java.awt.event.*;
import java.awt.*;

public class Client extends JPanel {
    static BufferedImage img = null;
    int lx, ly;
    boolean moved;
    static public void main(String[] argv) { 
        try {
            Client gui = new Client();

            InetAddress addr = InetAddress.getByName(argv[0]);
            int port = Integer.parseInt(argv[1]);
            SocketAddress sockaddr = new InetSocketAddress(addr, port);
            Socket sock = new Socket();
            int timeout = 2000;
            sock.connect(sockaddr, timeout);

            PrintWriter osr = new PrintWriter(sock.getOutputStream());
                
            InputStream is = sock.getInputStream();

            while (true) {
                if (gui.moved) {
                    osr.println(gui.lx + "," + gui.ly);
                    osr.flush();
                    gui.moved = false;
                }
                if (is.available() > 0) {
                    System.out.println("1");
                    BufferedImage tmp = ImageIO.read(is);
                    System.out.println("2");
                    if (tmp != null) {
                        img = tmp;
                        gui.revalidate();
                        gui.repaint();
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Client() {
        JFrame frame = new JFrame("Client");
        addMouseListener(new MouseAdapter() { 
                public void mousePressed(MouseEvent e) {
                    lx = e.getX();
                    ly = e.getY();
                    revalidate();
                    repaint();
                    moved = true;
                }
            });
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.SOUTH);
        frame.pack();
        frame.show();
    }

    public void paintComponent(Graphics g) {
        if (img != null) {
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            g.drawImage(img, 0, 0, w, h, this);

            if (lx != 0 && ly != 0) {
                g.setColor(Color.BLACK);
                ((Graphics2D)g).fill(new Ellipse2D.Float(lx - 10, ly - 10, 20, 20));
            }
        }
    }
    public Dimension getPreferredSize() {
        if (img != null) {
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            return new Dimension(w, h);
        }
        return super.getPreferredSize();
    }
}
