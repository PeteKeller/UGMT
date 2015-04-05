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
import java.net.*;
import javax.swing.*;
import harn.repository.*;
import rpg.*;

/**
 * Class that controls the sound. The actual mixer. The main member is a table
 * that maps general keys to tables, which maps specific keys to sound
 * parameters. A condition "" (!= null) is a non-condition. Another member is
 * the list of current valid conditions. The only guarantee is that any
 * condition that is in the clip list and in the current list shall be
 * running. All other shall not run.
 * @author Michael Jung
 */
public class SoundControl implements IListener {
    /** parent */
    private Main main;

    /** list of all locations */
    private Hashtable allLocs;

    /** list of all locations */
    private Hashtable allEvents;

    /**
     * Table that matches current cond1 -> cond2 -> name -> clip. An entry
     * is only removed, if the sound is taken away from the GUI.  All other
     * sound remain open but may be stopped.
     */
    private Hashtable clipList;
    
    /** Table that contains active cond2 for a given cond1 */
    private Hashtable currCondList;
    
    /** current group */
    private CharGroup actGroup;

    /** current weather object */
    private Weather weather;

    /** Mixing byte stream */
//    public static MixedByteStream export;

    /**
     * Constructor
     * @param mixer parent object; for reference
     */
    SoundControl(Main aMain) {
        main = aMain;
        allLocs = new Hashtable();
        allEvents = new Hashtable();
        clipList = new Hashtable();

        currCondList = new Hashtable();
        currCondList.put("", new HashSet());
        currCondList.put("Location", new HashSet());
        currCondList.put("Weather", new HashSet());
        currCondList.put("Event", new HashSet());

//        export = new MixedByteStream();

        Thread t = new SupervisorThread();
        t.start();
    }

    /**
     * Play a sound with the given gain (0-100). If this sound is already
     * playing only the parameters gain and loop are changed. No other
     * interruption takes place. Take condition into account
     * @param cnd1 type of key (never null)
     * @param cnd2 key name (never null)
     * @param list list of names (never null)
     * @param gain (0-100) percentage
     * @param loop loop this
     */
    void playSound(String cnd1, String cnd2, ArrayList list, Float gain, Boolean loop, Boolean play, Boolean pause) {
        // Build up mapping tables
        synchronized (clipList) {
            Hashtable ht1 = (Hashtable) clipList.get(cnd1);
            if (ht1 == null) {
                ht1 = new Hashtable();
                clipList.put(cnd1, ht1);
            }

            Hashtable ht2 = (Hashtable) ht1.get(cnd2);
            if (ht2 == null) {
                ht2 = new Hashtable();
                ht1.put(cnd2, ht2);
            }

            SoundParams par = (SoundParams) ht2.get(list);
            if (par == null) {
                par = new SoundParams();
                ht2.put(list, par);
            }

            par.files = list;
            if (gain != null) par.gain = gain.floatValue();
            if (loop != null) par.loop = loop.booleanValue();
            if (pause != null) par.pause = pause.booleanValue();
            if (play != null) par.play = play.booleanValue();
            play(cnd1, cnd2, par);
        }
    }

    /**
     * Stop a sound.
     * @param cnd1 type of key
     * @param cnd2 key name
     * @param snd sound name
     */
    void stopSound(String cnd1, String cnd2, ArrayList list) {
        synchronized (clipList) {
            Hashtable ht1 = (Hashtable) clipList.get(cnd1);
            if (ht1 == null) return;

            Hashtable ht2 = (Hashtable) ht1.get(cnd2);
            if (ht2 == null) return;

            SoundParams par = (SoundParams) ht2.get(list);
            if (par == null) return;

            if (par.clip != null) {
                par.clip.stop();
            }
            ht2.remove(list);
            if (ht2.size() == 0) ht1.remove(ht2);
            if (ht1.size() == 0) clipList.remove(ht1);
        }
    }

    /**
     * Play a song if the given name with the specified params
     * @param file file to play
     * @param gain gain for this sound
     * @param cnd1 first condition
     * @param cnd2 second condition
     * @param sp sound parameter object
     */
    private void play(String cnd1, String cnd2, SoundParams sp) {
        sp.conditions = false;
        HashSet hs = (HashSet) currCondList.get(cnd1);
        if (cnd1.length() == 0 || hs != null && hs.contains(cnd2)) {
            sp.conditions = true;
            start(sp);
        }
    }

    /**
     * Yields system sound parameters.
     * @return AudioInputStream, SourceDataLine
     */
    static Object[] init(File f) throws Exception {
        InputStream is = null;
        if (f.getName().endsWith(".m3u")) {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String href = br.readLine();
            is = new BufferedInputStream(new URL(href).openStream());
        }
        else {
            is = new BufferedInputStream(new FileInputStream(f));
        }
        AudioInputStream snd =
            AudioSystem.getAudioInputStream(is);
        AudioFormat format = snd.getFormat();

        // Check for native support
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            // It seems we have to choose something reasonable
            // only. The code is taken from Tritonus examples and
            // works for ALAW, ULAW, MP3 and OGG.
            AudioFormat tmp = new AudioFormat
                (AudioFormat.Encoding.PCM_SIGNED,
                 format.getSampleRate(),
                 16,
                 format.getChannels(),
                 format.getChannels() * 2, // frame size
                 format.getSampleRate(), // frame rate
                 false);
            snd = AudioSystem.getAudioInputStream(tmp, snd);
        }

        info = new DataLine.Info
            (SourceDataLine.class, snd.getFormat());

        SourceDataLine sdl =
            (SourceDataLine) AudioSystem.getLine(info);

//         ByteQueue queue = new ByteQueue(snd.getFormat());
//         export.add(queue);
        return new Object[] { sdl, snd };
    }

    /**
     * Start the sound
     */
    void start(SoundParams sp) {
        if (sp.clip == null) {
            try {
                File f = new File
                    (Main.mySoundPath +
                     Framework.osName((String)sp.files.get(0)));
                Object[] ret = init(f);
                SoundClip clip = new SoundClip
                    ((SourceDataLine)ret[0], (AudioInputStream)ret[1], sp.files);
                sp.clip = clip;
            }
            catch (Exception e) {
                // Debug
                e.printStackTrace();
            }
        }
        // Exceptions do happen
        if (sp.clip != null) sp.clip.setGain(sp.gain);
    }

    /**
     * Be informed about the objects we may accepting triggers from.  We
     * accept only (hard-coded) "Location", "Event", and "Weather". See also
     * the Data.java method getKeys.
     * @param object that has changed
     */
    public void inform(IExport obj) {
        if (obj instanceof Location) {
            Location loc = (Location)obj;
            if (loc.isValid())
                allLocs.put(loc.getName(), obj);
            else
                allLocs.remove(loc.getName());
            checkAllLocs();
        }
        else if (obj instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent) obj;
            if (ev.isValid())
                allEvents.put(ev.getName(), obj);
            else
                allEvents.remove(ev.getName());
            checkAllEvents();
        }
        else if (obj instanceof Weather) {
            Weather w = (Weather) obj;
            if (w.isActive()) {
                weather = w;
                checkAllWeather();
            }
        }
        else { // CharGroup
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive()) actGroup = cg;

            checkAllLocs();
            checkAllEvents();
            checkAllWeather();
        }
    }

    /**
     * Check all weather conditions.
     */
    private void checkAllWeather() {
        // Check weather
        HashSet old = (HashSet)currCondList.get("Weather");
        if (weather == null || actGroup == null) return;

        HashSet young = new HashSet();
        String c1 = weather.condition
            (actGroup.getMap(), actGroup.getX(), actGroup.getY(), actGroup.getDate());
        if (c1 != null) young.add(c1);
        String c2 = weather.event
            (actGroup.getMap(), actGroup.getX(), actGroup.getY(), actGroup.getDate());
        if (c2 != null) young.add(c2);

        reworkClipList("Weather", old, young);
    }

    /** Rework cliplist for old/new condition change */
    private void reworkClipList(String cnd1, Set old, Set young) {
        currCondList.put(cnd1, young);
        synchronized (clipList) {
            Hashtable ht1 = (Hashtable) clipList.get(cnd1);
            if (ht1 == null) return;

            // Stop old
            Iterator iter = old.iterator();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (young.contains(o)) continue;
                Hashtable ht2 = (Hashtable) ht1.get(o);
                if (ht2 == null) continue;

                Iterator iter2 = ht2.keySet().iterator();
                while (iter2.hasNext()) {
                    ArrayList s = (ArrayList)iter2.next();
                    SoundParams sp = (SoundParams) ht2.get(s);
                    sp.conditions = false;
                }
            }

            // Start new
            iter = young.iterator();
            while (iter.hasNext()) {
                Object n = iter.next();
                if (old.contains(n)) continue;
                Hashtable ht2 = (Hashtable) ht1.get(n);
                if (ht2 == null) continue;
                
                Iterator iter2 = ht2.keySet().iterator();
                while (iter2.hasNext()) {
                    ArrayList s = (ArrayList) iter2.next();
                    SoundParams sp = (SoundParams) ht2.get(s);
                    sp.conditions = true;
                    start(sp);
                }
            }
        }
    }

    /**
     * Check all locations, whether the active group is inside one. Also
     * remove locations left.
     */
    private void checkAllLocs() {
        if (actGroup == null) return;

        Set old = (Set)currCondList.get("Location");
        Set young = new HashSet();

        int x = actGroup.getX();
        int y = actGroup.getY();
        Iterator iter = allLocs.keySet().iterator();
        while (iter.hasNext()) {
            String locStr = (String) iter.next();
            Location loc = (Location)allLocs.get(locStr);

            // Only check existing locations
            if (loc == null) continue;
            Shape[] sh = loc.getShapes(actGroup.getMap());
            if (sh == null) continue;

            // Test all shapes
            for (int i = 0; i < sh.length; i++) {
                if (sh[i].contains(x,y)) young.add(locStr);
            }
        }
        reworkClipList("Location", old, young);
    }

    /**
     * Check all events, whether the active group is inside one. Also
     * remove events left.
     */
    private void checkAllEvents() {
        if (actGroup == null) return;

        Set old = (Set)currCondList.get("Event");
        Set young = new HashSet();

        Iterator iter = allEvents.keySet().iterator();
        while (iter.hasNext()) {
            String evStr = (String) iter.next();
            TimeEvent ev = (TimeEvent) allEvents.get(evStr);
            if (ev == null || !ev.isActive()) continue;
            young.add(ev);
        }
        reworkClipList("Event", old, young);
    }

    /**
     * Read a bit.
     */
    public void read(byte[] dest) {
        //export.read(dest);
    }

    /**
     * Class that captures all params of a sound in one class
     */
    class SoundParams {
        /** gain */
        private float gain;
        /** loop */
        private boolean loop;
        /** played clip */
        private SoundClip clip;
        /** filenames for clip */
        private ArrayList files;
        /** playing */
        private boolean play;
        /** pausing */
        private boolean pause;
        /** conditions met */
        private boolean conditions;
    }

    /**
     * Class that watches over ending songs.
     */
    class SupervisorThread extends Thread {
        public void run() {
            while (true) {
                synchronized (clipList) {
                    Iterator i1 = clipList.keySet().iterator();
                    while (i1.hasNext()) {
                        String cnd1 = (String) i1.next();
                        Hashtable ht1 = (Hashtable) clipList.get(cnd1);
                        Iterator i2 = ht1.keySet().iterator();
                        while(i2.hasNext()) {
                            String cnd2 = (String) i2.next();
                            Hashtable ht2 = (Hashtable) ht1.get(cnd2);
                            Iterator i3 = ht2.keySet().iterator();
                            while (i3.hasNext()) {
                                SoundParams p = (SoundParams) ht2.get(i3.next());
                                if (p.clip != null) {
                                    // This will kill the clip
                                    if (!p.play || !p.conditions) {
                                        p.clip.stop();
                                        continue;
                                    }

                                    // Set parameters
                                    p.clip.halt(p.pause);
                                    p.clip.loop(p.loop);

                                    // Run
                                    p.clip.run();
                                }
                            }
                        }
                    }
                }
                try { Thread.sleep(100); } catch (Exception e) { }
            }
        }
    }
}
