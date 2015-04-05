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
package harn.sound;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import rpg.*;
import org.w3c.dom.*;
import javax.swing.text.BadLocationException;

/**
 * This class is the struct that holds the GUI references and the XML-node
 * which belngs to it.
 */
class GuiLine {
    /** Play */
    JCheckBox play;

    /** Pause */
    JCheckBox pause;

    /** Sound combo */
    JSpinner soundlist;

    /** Primary key combo */
    private JComboBox pkeys;

    /** Gain */
    JSlider gain;

    /** Secondary key edit (takes precendence) */
    private Object skeys;

    /** Secondary key combo */
    private JComboBox weathlist;

    /** Looping */
    JCheckBox loop;

    /** The old condition1 potentially still running */
    String oldC1;

    /** The old condition2 sound potentially still running */
    String oldC2;

    /** (Non-)Backgroundcolor */
    private Color bgcol;
    private Color bgcolef;

    /** Reference to DOM node */
    Element myNode;

    /** Father object */
    private LineMixer father;

    /**
     * Constructor. Add a new line to mix. Used by the add button. 100 is the
     * hard-coded limit. Consists of "play", "soundlist", "primary
     * key". "secondary key", "gain", "loop".
     * @param aSound name of sound
     * @param aCnd1 a null condition is unconditional
     * @param aCnd2
     * @param aGain internal volume control
     * @param aLoop loop or not
     * @param node
     */
    public GuiLine(LineMixer aFather, boolean doplay, String aSound, String aCnd1, String aCnd2, int aGain, boolean aLoop, boolean aPause, Element node) {
        father = aFather;
        myNode = node;

        // Play
        play = new JCheckBox("Play",doplay);
        play.setBackground(Color.lightGray);
        play.setToolTipText("Use popup to remove permanently");

        // Pause
        pause = new JCheckBox("Pause",aPause);

        // Sound list
        soundlist = new JSpinner
            (new MySpinModel());
        soundlist.setAlignmentX(Component.RIGHT_ALIGNMENT);

        final ArrayList allSnds = new ArrayList();
        allSnds.add(aSound != null ? aSound : Data.defSound);
        ((MySpinModel)soundlist.getModel()).setList(allSnds);
        JSpinner.DefaultEditor se =
            (JSpinner.DefaultEditor) soundlist.getEditor();
        se.getTextField().setToolTipText
            ("poup select exchanges sound, shift-select adds; plays from bottom to top.");
        se.getTextField().setEditable(false);
        se.getTextField().setColumns
            (Integer.parseInt(father.main.myProps.getProperty("name.width")));
        soundlist.getModel().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    father.top.revalidate();
                    father.top.repaint();
                }
            });

        pkeys = null;
        skeys = new Object();
        weathlist = null;
        oldC1 = "";
        oldC2 = skeys.toString();
        if (aCnd1 != null) {
            // Key list
            int select = -1;
            String[] keys = father.main.myData.getKeys();
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(aCnd1))
                    select = i;
            }
            pkeys = new JComboBox(keys);
            if (select != -1) pkeys.setSelectedIndex(select);

            // name
            skeys = new JTextField("Nameless");
            if (aCnd2 != null) ((JTextField)skeys).setText(aCnd2);
            ((JTextField)skeys).setColumns(15);
        
            // alternate name
            select = -1;
            keys = father.main.myData.getWeatherKeys();
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(aCnd2))
                    select = i;
            }
            weathlist = new JComboBox(keys);
            if (select != -1) weathlist.setSelectedIndex(select);

            // Derived
            oldC1 = (String) pkeys.getSelectedItem();
            oldC2 = ((JTextField)skeys).getText();
        }

        // Gain
        gain = new JSlider(JSlider.HORIZONTAL);
        gain.setBackground(Color.lightGray);
        gain.setValue(aGain*100/gain.getMaximum());

        // Loop
        loop = new JCheckBox("Loop",false);
        loop.setSelected(aLoop);

        bgcol = Color.lightGray;

        JPopupMenu pop = father.main.myData.getSoundFiles(this, father);
        ((JSpinner.DefaultEditor)soundlist.getEditor()).getTextField().addMouseListener(new MyMouseAdapter(pop));

        // Play listener
        play.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    ArrayList list = (ArrayList)
                        ((SpinnerListModel)soundlist.getModel()).getList();
                    Boolean b = new Boolean(play.isSelected());
                    father.ctrl.playSound
                        (oldC1, oldC2, list, null, null, b, null);

                    if (father.main.getState() != null)
                        father.main.getState().setDirty(GuiLine.this);
                }
            });

        // Stop sound
        pop = new JPopupMenu();
        JMenuItem mi = new JMenuItem("Delete");
        mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String snd = (String)soundlist.getValue();
                    String cnd1 = getPSelection();
                    String cnd2 = getSSelection();
                    father.ctrl.stopSound(cnd1, cnd2, allSnds);

                    // Modify GUI
                    if (cnd1.length() == 0) {
                        father.removeFromB(GuiLine.this);
                    }
                    else {
                        father.removeFromT(GuiLine.this);
                    }

                    // Modify State
                    father.main.getState().removeNode(GuiLine.this);
                    father.main.getState().setDirty(null);
                }
            });
        pop.add(mi);
        play.addMouseListener(new MyMouseAdapter(pop));

        // Play listener
        pause.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    ArrayList list = (ArrayList)
                        ((SpinnerListModel)soundlist.getModel()).getList();
                    Boolean b = new Boolean(pause.isSelected());
                    father.ctrl.playSound
                        (oldC1, oldC2, list, null, null, null, b);

                    if (father.main.getState() != null)
                        father.main.getState().setDirty(GuiLine.this);
                }
            });

        // Primary key listener
        if (aCnd1 != null) {
            pkeys.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            ArrayList list = (ArrayList)
                                ((SpinnerListModel)soundlist.getModel()).getList();
                            String cnd1 = getPSelection();
                            float p = gain.getValue()/(float)(gain.getMaximum());
                            // Switch key selection type (field <-> combo)
                            switchSKeySelection(cnd1);
                            String cnd2 = getSSelection();

                            father.ctrl.stopSound(oldC1, oldC2, allSnds);
                            father.ctrl.playSound
                                (cnd1, cnd2, list, new Float(p),
                                 new Boolean(loop.isSelected()),
                                 new Boolean(play.isSelected()),
                                 new Boolean(pause.isSelected()));
                            oldC1 = cnd1;
                            oldC2 = cnd2;

                            if (father.main.getState() != null)
                                father.main.getState().setDirty(GuiLine.this);
                        }
                    }
                });

            ((JTextField)skeys).getDocument().addDocumentListener
                (new MySKeyDocListener(this));

            // Secondary (alternate) key listener
            weathlist.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ArrayList list = (ArrayList)
                            ((SpinnerListModel)soundlist.getModel()).getList();

                        setSSelection((String)weathlist.getSelectedItem());

                        String cnd1 = getPSelection();
                        float p = gain.getValue()/(float)(gain.getMaximum());

                        // Switch key selection type (field <-> combo)
                        switchSKeySelection(cnd1);
                        String cnd2 = getSSelection();

                        father.ctrl.stopSound(oldC1, oldC2, allSnds);
                        father.ctrl.playSound
                            (cnd1, cnd2, list, new Float(p),
                             new Boolean(loop.isSelected()),
                             new Boolean(play.isSelected()),
                             new Boolean(pause.isSelected()));
                        oldC1 = cnd1;
                        oldC2 = cnd2;

                        if (father.main.getState() != null)
                            father.main.getState().setDirty(GuiLine.this);
                    }
                });
        }

        // Gain listener
        gain.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    ArrayList list = (ArrayList)
                        ((SpinnerListModel)soundlist.getModel()).getList();
                    String cnd1 = getPSelection();
                    String cnd2 = getSSelection();
                    float p = gain.getValue()/(float)(gain.getMaximum());
                    father.ctrl.playSound
                        (cnd1, cnd2, list, new Float(p), null, null, null);
                    if (father.main.getState() != null)
                        father.main.getState().setDirty(GuiLine.this);
                }
            });

        // Loop listener
        loop.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    ArrayList list = (ArrayList)
                        ((SpinnerListModel)soundlist.getModel()).getList();
                    String cnd1 = getPSelection();
                    String cnd2 = getSSelection();
                    Boolean b = new Boolean(loop.isSelected());
                    father.ctrl.playSound
                        (cnd1, cnd2, list, null, b, null, null);

                    if (father.main.getState() != null)
                        father.main.getState().setDirty(GuiLine.this);
                }
            });

        // Play for initial conditions
        String cnd1 = getPSelection();
        String cnd2 = getSSelection();
        float p = gain.getValue()/(float)(gain.getMaximum());
        father.ctrl.playSound
            (cnd1, cnd2, allSnds, new Float(p), new Boolean(loop.isSelected()),
             new Boolean(doplay), Boolean.FALSE);
        if (aCnd1 == null) father.addToB(this); else father.addToT(this);
    }

    /**
     * Get primary selection.
     */
    public String getPSelection() {
        return (pkeys != null ? (String)pkeys.getSelectedItem() : oldC1);
    }

    /**
     * Get secondary selection.
     */
    public String getSSelection() {
        if (skeys instanceof JTextField) {
            Component[] comp = father.top.getComponents();
            for (int i = 0; i < comp.length; i++)
                if (comp[i] == skeys)
                    return ((JTextField)skeys).getText();
            return (String)weathlist.getSelectedItem();
        }
        return oldC2;
    }

    /**
     * Get secondary selection.
     */
    public void setSSelection(String txt) {
        if (skeys instanceof JTextField) ((JTextField)skeys).setText(txt);
    }

    /**
     * Remove from GUI.
     * @param container component to remove from
     */
    public void removeFrom(JComponent container) {
        container.remove(soundlist);
        container.remove(play);
        container.remove(gain);
        container.remove(loop);
        container.remove(pause);
        if (pkeys != null) container.remove(pkeys);
        if (skeys instanceof JTextField) container.remove((JTextField)skeys);
        if (weathlist != null) container.remove(weathlist);
    }

    /**
     * Add to GUI.
     * @param container component to add to
     */
    public void addTo(JComponent container, GridBagConstraints constrBox) {
        constrBox.gridx = 0;
        constrBox.weightx = 0;
        container.add(play, constrBox);

        constrBox.gridx = 1;
        container.add(soundlist, constrBox);

        constrBox.gridx = 2;
        if (pkeys != null) {
            container.add(pkeys, constrBox);

            constrBox.gridx = 3;
            if (pkeys.getSelectedItem().equals("Weather")) {
                container.add(weathlist, constrBox);
                ((JTextField)skeys).setText((String) weathlist.getSelectedItem());
            }
            else {
                container.add((JTextField)skeys, constrBox);
            }
        }

        constrBox.gridx = 4;
        constrBox.weightx = 1;
        container.add(gain, constrBox);

        constrBox.gridx = 5;
        constrBox.weightx = 0;
        container.add(loop, constrBox);

        constrBox.gridx = 6;
        container.add(pause, constrBox);

        constrBox.weightx = 0;
        container.revalidate();
    }

    /**
     * Switch selection type.
     * @param cnd key which is used.
     */
    public void switchSKeySelection(String cnd) {
        Component[] comp = father.top.getComponents();
        father.constrT.gridx = 3;
        if (cnd.equals("Weather")) {
            for (int i = 0; i < comp.length; i++)
                if (comp[i] == skeys) {
                    father.top.remove((JTextField)skeys);
                    father.top.add(weathlist, father.constrT, i);
                }
        }
        else {
            for (int i = 0; i < comp.length; i++)
                if (comp[i] == weathlist) {
                    father.top.remove(weathlist);
                    father.top.add((JTextField)skeys, father.constrT, i);
                }
        }
        father.constrT.gridx = 0;
        father.top.revalidate();
        father.top.repaint();
    }

    /**
     * Used for starting menus.
     */
    class MyMouseAdapter extends MouseAdapter {
        /** Popuop to popup */
        JPopupMenu pop;
        /** Constructor */
        MyMouseAdapter(JPopupMenu aPop) { pop = aPop; }
        /** IF method */
        public void mousePressed(MouseEvent e) { popup(e); }
        /** IF method */
        public void mouseReleased(MouseEvent e) { popup(e); }
        /** generic method */
        private void popup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                pop.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Text field listener class for the skey combobox.
     */
    class MySKeyDocListener implements DocumentListener {
        /** Guiline this belongs to */
        private GuiLine gl;
        /** Thread to increase GUI speed */
        Thread t = null;
        /** stall time */
        long doIn = 0;
        /** Constructor */
        MySKeyDocListener(GuiLine aGl) { gl = aGl; }
        /**
         * DocumentListener method
         * @param e event
         */
        public void removeUpdate(DocumentEvent e) { setField(e); }
        /**
         * DocumentListener method
         * @param e event
         */
        public void insertUpdate(DocumentEvent e) { setField(e); }
        /**
         * DocumentListener method
         * @param e event
         */
        public void changedUpdate(DocumentEvent e) { setField(e); }
        /** Set field */
        private void setField(DocumentEvent e) {
            // To increase GUI speed and reduce jitter of audio lines,
            // we only accept input after a second passed.
            doIn = System.currentTimeMillis() + 1000;
            if (t == null) {
                t = new Thread() {
                        public void run() {
                            while (System.currentTimeMillis() < doIn)
                                try {
                                    Thread.sleep(100);
                                }
                                catch (Exception ex) {
                                }
                            propagate();
                            t = null;
                        }
                    };
                t.start();
            }
        }
        /** Propagate */
        private void propagate() {
            String cnd1 = gl.getPSelection();
            String cnd2 = gl.getSSelection();
            ArrayList allSnds = (ArrayList)
                ((SpinnerListModel)gl.soundlist.getModel()).getList();
            float p = gain.getValue()/(float)(gain.getMaximum());
            
            father.ctrl.stopSound(gl.oldC1, gl.oldC2, allSnds);
            father.ctrl.playSound
                (cnd1, cnd2, allSnds, new Float(p), new Boolean(loop.isSelected()),
                 new Boolean(play.isSelected()), new Boolean(pause.isSelected()));
            
            gl.oldC1 = cnd1;
            gl.oldC2 = cnd2;
            
            if (father.main.getState() != null)
                father.main.getState().setDirty(gl);
        }
    }

    /** Class to allow fireStateChange */
    class MySpinModel extends SpinnerListModel {
        public void fireStateChanged() { super.fireStateChanged(); }
    }
}
