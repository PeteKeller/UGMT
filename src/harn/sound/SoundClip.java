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

import javax.sound.sampled.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import harn.repository.*;
import rpg.*;

/**
 * Class that behaves as a clip (for our purposes, but can handle long files.
 */
class SoundClip {
    /** Input */
    private AudioInputStream ais;

    /** Output */
    private SourceDataLine sdl;

    /** File names to open */
    private ArrayList orig;

    /** run this */
    private Thread runner;

    /** volume/gain */
    private float dB;

    /** dying */
    private boolean end = false;

    /** stalling */
    private boolean halt = false;

    /** looping */
    private boolean loop = false;

    /** Ran once */
    private boolean once = false;

    /** index in file list */
    private int idx;

    /** Constructor */
    public SoundClip(SourceDataLine aSdl, AudioInputStream aAis, ArrayList anOrig) {
        ais = aAis; sdl = aSdl; orig = anOrig;
    }

    /** Pause */
    public void halt(boolean val) { halt = val; }

    /** Loop */
    public void loop(boolean val) { loop = val; }

    /** Set volume */
    public void setGain(float gain) {
        try {
            FloatControl gCtrl =
                (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
            dB = 20.0f * (float) (Math.log(gain + .0001)/Math.log(10));
            gCtrl.setValue(dB);
        }
        catch (Exception e) {
            // We ignore this one. There may be encodings which do not allow
            // gain/volume setting.
        }
    }

    /** Stop clip */
    public void stop() {
        end = true;
        while (runner != null) {
            try { Thread.sleep(50); } catch (Exception e) {}
        }
        sdl.stop();
    }

    /** start clip */
    public void run() {
        synchronized (orig) {
            if (runner != null) return;
            if (!loop && once) return;
            runner = new Thread() {
                    public void run() {
                        try {
                            do {
                                once = true;
                                sdl.open(ais.getFormat());
                                sdl.start();
                                int len = sdl.getBufferSize();
                                ((FloatControl)sdl.getControl(FloatControl.Type.MASTER_GAIN)).setValue(dB);
                                byte[] buf = new byte[len];
                                while (len != -1 && !end) {
                                    if (!halt) {
                                        len = ais.read(buf);
                                        if (len != -1) {
                                            sdl.write(buf, 0, len);
//                                            export.write(buf, len);
                                        }
                                    }
                                    else {
                                        try { Thread.sleep(100); }
                                        catch (Exception e) {}
                                    }
                                }
                                // This seems to be buggy in java. The
                                // sleep is a work-around.
                                sdl.drain();
                                try { Thread.sleep(100); } catch (Exception e) {}
                                sdl.close();
                                // Reset
                                File f = new File
                                    (Main.mySoundPath +
                                     Framework.osName((String)orig.get((idx++)%orig.size())));
                                end = (f == null);
                                if (!end) {
                                    Object[] ret = SoundControl.init(f);
                                    sdl = (SourceDataLine)ret[0];
                                    ais = (AudioInputStream)ret[1];
                                }
                            }
                            while (loop && !end);
                        }
                        catch (Exception e) {
                            // Debug
                            e.printStackTrace();
                        }
                        synchronized (orig) {
                            runner = null;
                        }
                    }
                };
            end = false;
            runner.start();
        }
    }
}
