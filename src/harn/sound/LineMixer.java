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
 * Class that represents the Mixer GUI. Only weather has a combobox, other
 * conditions are free form.
 * @author Michael Jung
 */
public class LineMixer extends JComponent {
    /** root reference */
    Main main;

    /** Grid constraint for top boxes */
    GridBagConstraints constrT;

    /** Grid constraint for bottom boxes */
    private GridBagConstraints constrB;

    /** top panel reference */
    JPanel top;

    /** bottom panel reference */
    JPanel bottom;

    /** mixer proper */
    protected SoundControl ctrl;

    /**
     * Constructor.
     * @param props properties
     * @param main root reference
     */
    LineMixer(Properties props, Main aMain) {
	super();
	main = aMain;

	setLayout(new GridBagLayout());

        // Container for business buttons
        top = new JPanel(new GridBagLayout());
        bottom = new JPanel(new GridBagLayout());
        bottom.setBackground(Color.lightGray);

	// Constraints for boxes
	constrT = new GridBagConstraints();
	constrT.fill = GridBagConstraints.BOTH;

	constrB = new GridBagConstraints();
	constrB.fill = GridBagConstraints.BOTH;

        // Add a filling dummy in middle
	JPanel dummy = new JPanel();

        // Finish GUI
        GridBagConstraints constr1 = new GridBagConstraints();
        constr1.fill = GridBagConstraints.BOTH;
        constr1.gridx = 0;
        constr1.gridy = 0;
	constr1.weightx = 1;
        add(top, constr1);
        constr1.gridy = 1;
	constr1.weighty = 1;
        add(dummy, constr1);
        constr1.gridy = 2;
	constr1.weighty = 0;
        add(bottom, constr1);

        // Create the proper control
        ctrl = new SoundControl(main);
    }

    /** Add to top */
    public void addToT(GuiLine gl) { gl.addTo(top, constrT); }

    /** Remove from top */
    public void removeFromT(GuiLine gl) { gl.removeFrom(top); top.revalidate(); }

    /** Add to bottom */
    public void addToB(GuiLine gl) { gl.addTo(bottom, constrB); }

    /** Remove from bottom */
    public void removeFromB(GuiLine gl) { gl.removeFrom(bottom); bottom.revalidate(); }

    /**
     * Add a top (conditional) line
     */
    public GuiLine addTopLine(String cnd1, String cnd2) {
        GuiLine gl =
            new GuiLine(this, false, null, cnd1, cnd2, 50, false, false, null);
        return gl;
    }

    /**
     * Add a bottom (unconditional) line
     */
    public GuiLine addBottomLine() {
        GuiLine gl =
            new GuiLine(this, false, null, null, null, 50, false, false, null);
        return gl;
    }

    /**
     * Set LineMixer text.
     * @param add add to box (or remove)
     * @param shift shifted means add not replace
     * @param gl gui line effected
     * @param txt text to add/exchange (ignored when add = false)
     */
    public void setSoundListText(boolean add, boolean shift, GuiLine gl, String txt) {
        String snd = (String)gl.soundlist.getValue();
        String cnd1 = gl.getPSelection();
        String cnd2 = gl.getSSelection();
        float p = gl.gain.getValue()/(float)(gl.gain.getMaximum());
        ArrayList allSnds = (ArrayList)
            ((GuiLine.MySpinModel)gl.soundlist.getModel()).getList();

        if (add) {
            ctrl.stopSound(gl.oldC1, gl.oldC2, allSnds);
            if (shift) {
                allSnds.add(txt);
            }
            else {
                allSnds.set(allSnds.lastIndexOf(snd), txt);
                gl.soundlist.getModel().setValue(txt);
            }
            ctrl.playSound
                (cnd1, cnd2, allSnds, new Float(p), new Boolean(gl.loop.isSelected()),
                 new Boolean(gl.play.isSelected()), new Boolean(gl.pause.isSelected()));
        }
        else {
            if (allSnds.size() > 1) {
                ctrl.stopSound(gl.oldC1, gl.oldC2, allSnds);
                Object o = gl.soundlist.getModel().getPreviousValue();
                if (o == null) o = gl.soundlist.getModel().getNextValue();
                allSnds.remove(gl.soundlist.getModel().getValue());
                gl.soundlist.getModel().setValue(o);
                ctrl.playSound
                    (cnd1, cnd2, allSnds, new Float(p), new Boolean(gl.loop.isSelected()),
                     new Boolean(gl.play.isSelected()), new Boolean(gl.pause.isSelected()));
            }
        }
        ((GuiLine.MySpinModel)gl.soundlist.getModel()).fireStateChanged();

        if (main.getState() != null)
            main.getState().setDirty(gl);
    }
}
