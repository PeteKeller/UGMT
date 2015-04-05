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

import rpg.Framework;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.io.File;

/**
 * GUI object to fetch all relevant entries for a personal map. Used during
 * editing.
 * @author Michael Jung
 */
public class MapParams extends JDialog {
    /** Textfields containing parameters */
    private JTextField tf1, tf2, tf3, tf4, tf5, tf6;

    /** accept the input */
    private boolean accept;

    /** Constructor */
    public MapParams(final Main aMain, String name) {
        super
            (JOptionPane.getFrameForComponent(aMain.myPanel), "Add map", true);
        accept = false;

        Container cp = getContentPane();
        cp.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.BOTH;
        constr.insets = new Insets(2, 2, 2, 2);
        constr.weightx = 0;
        constr.weighty = 1;

        JButton b1 = new JButton("Image");
        b1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(aMain.myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    String osName = f.getAbsolutePath();
                    if (!osName.startsWith(Main.myPath))
                        return;
                    osName = osName.replaceFirst(Main.myPath,"");
                    tf1.setText
                        (osName.replace(Framework.SEP, '/'));
                }
            });
        b1.setToolTipText
            ("<html>The base image. Mandatory. PNG format preferred.<br>" +
             "Click to browse. Must be inside a local directory.</html>");
        constr.gridy = 0;
        cp.add(b1, constr);
        JButton b2 = new JButton("Layer 1");
        b2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(aMain.myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    String osName = f.getAbsolutePath();
                    if (!osName.startsWith(Main.myPath))
                        return;
                    osName = osName.replaceFirst(Main.myPath,"");
                    tf2.setText
                        (osName.replace(Framework.SEP, '/'));
                }
            });
        b2.setToolTipText
            ("<html>A first layer. Optional. PNG format preferred.<br>" +
             "Click to browse. Must be inside a local directory.</html>");
        constr.gridy = 1;
        cp.add(b2, constr);
        JButton b3 = new JButton("Layer 2");
        b3.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(aMain.myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    String osName = f.getAbsolutePath();
                    if (!osName.startsWith(Main.myPath))
                        return;
                    osName = osName.replaceFirst(Main.myPath,"");
                    tf3.setText
                        (osName.replace(Framework.SEP, '/'));
                }
            });
        b3.setToolTipText
            ("<html>A second layer. Optional. PNG format preferred.<br>" +
             "Click to browse. Must be inside a local directory.</html>");
        constr.gridy = 2;
        cp.add(b3, constr);
        JButton b4 = new JButton("Layer 3");
        b4.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(aMain.myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    String osName = f.getAbsolutePath();
                    if (!osName.startsWith(Main.myPath))
                        return;
                    osName = osName.replaceFirst(Main.myPath,"");
                    tf4.setText
                        (osName.replace(Framework.SEP, '/'));
                }
            });
        b4.setToolTipText
            ("<html>A third layer. Optional. PNG format preferred.<br>" +
             "Click to browse. Must be inside a local directory.</html>");
        constr.gridy = 3;
        cp.add(b4, constr);
        JButton b5 = new JButton("Icon");
        b5.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(aMain.myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    String osName = f.getAbsolutePath();
                    if (!osName.startsWith(Main.myPath))
                        return;
                    osName = osName.replaceFirst(Main.myPath,"");
                    tf5.setText
                        (osName.replace(Framework.SEP, '/'));
                }
            });
        b5.setToolTipText
            ("<html>An optional miniature representation. " +
             "PNG format preferred.</br> Click to browse. " +
             "Must be inside a local directory.</html>");
        constr.gridy = 4;
        cp.add(b5, constr);
        JButton b6 = new JButton("Scale");
        b6.setEnabled(false);
        b6.setToolTipText("Scale (Pixel/League). Mandatory. Integer only.");
        constr.gridy = 5;
        cp.add(b6, constr);

        constr.weightx = 1;
        constr.gridx = 1;
        tf1 = new JTextField(); // Image
        constr.gridy = 0;
        cp.add(tf1, constr);
        tf2 = new JTextField(); // Layer 1
        constr.gridy = 1;
        cp.add(tf2, constr);
        tf3 = new JTextField(); // Layer 2
        constr.gridy = 2;
        cp.add(tf3, constr);
        tf4 = new JTextField(); // Layer 3
        constr.gridy = 3;
        cp.add(tf4, constr);
        tf5 = new JTextField(); // Icon
        constr.gridy = 4;
        cp.add(tf5, constr);
        tf6 = new JTextField(); // Scale
        tf6.setDocument(new IntDoc());
        constr.gridy = 5;
        cp.add(tf6, constr);

        JPanel p = new JPanel(new GridBagLayout());
        constr.gridx = 0;
        constr.gridy = 6;
        constr.gridwidth = 2;
        cp.add(p, constr);

        GridBagConstraints constr2 = new GridBagConstraints();
        constr2.insets = new Insets(5, 5, 5, 5);

        JPanel dummy = new JPanel();
        constr2.weightx = 1;
        p.add(dummy, constr2);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    accept = true;
                    setVisible(false);
                }
            });
        constr2.weightx = 0;
        p.add(ok, constr2);

        dummy = new JPanel();
        constr2.weightx = 1;
        p.add(dummy, constr2);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
        constr2.weightx = 0;
        p.add(cancel, constr2);

        dummy = new JPanel();
        constr2.weightx = 1;
        p.add(dummy, constr2);

        pack();
    }

    /**
     * Get the options.
     */
    public boolean doIt() {
        setVisible(true);
        return accept;
    }

    /**
     * Get current parameters.
     */
    public String[] getParams() {
        String[] ret = new String[6];
        ret[0] = tf1.getText();
        ret[1] = tf2.getText();
        ret[2] = tf3.getText();
        ret[3] = tf4.getText();
        ret[4] = tf5.getText();
        ret[5] = tf6.getText();
        return ret;
    }
    /**
     * Validating integer text field.
     */
    class IntDoc extends PlainDocument {
        public void insertString(int off, String str, AttributeSet a) {
            try {
                check(off, str, off);
                super.insertString(off, str, a);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
        public void remove(int off, int len) {
            try {
                check(off, "", off + len);
                super.remove(off, len);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
 	public void replace(int off, int len, String str, AttributeSet a) {
            try {
                check(off, str, off + len);
                super.replace(off, len, str, a);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
        private void check(int off1, String str, int off2) throws Exception {
            String test =
                getText(0, off1) + str + getText(off2, getLength() - off2);
            int i = Integer.parseInt(test);
            if (i < 0) throw new Exception("Negative scale");
        }
    }
}
