/*
 * UGMT : Unversal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 *
 * Any party obtaining a copy of these files is granted, free of charge, a
 * full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and
 * documentation files (the "Software"), including without limitation the
 * rights to use, copy, modify, merge, publish and/or distribute copies of
 * the Software, and to permit persons who receive copies from any such 
 * party to do so, with the only requirement being that this copyright 
 * notice remain intact.
 */
package harn.sound;

import javax.sound.sampled.*;
import java.util.*;

/**
 * Simple byte queue. This class is not synchronized, there should only be one
 * reader, and one writer. Also contains the format.
 * @author Michael Jung
 */
public class ByteQueue {
    /** lists of byte arrays */
    private ArrayList bytearraylists;

    /** Audioformat of underlying AudioStream */
    private AudioFormat format;

    /** Constructor */
    public ByteQueue(AudioFormat aFormat) {
        bytearraylists = new ArrayList();
        format = aFormat;
    }

    /** Getter */
    public AudioFormat getFormat() { return format; }

    /** Copies bytes into dest and returns number of bytes copied */
    public int read(byte[] dest, int off, int len) {
        int k = bytearraylists.size();
        int idx = 0;
        while (idx < len && bytearraylists.size() > 0) {
            byte[] src = (byte[]) bytearraylists.get(0);
            bytearraylists.remove(0);
            int sz = Math.min(src.length, len - idx);
            System.arraycopy(src, 0, dest, off + idx, sz);
            idx += sz;
            if (src.length - sz > 0) {
                byte[] srcn = new byte[src.length - sz];
                System.arraycopy(src, sz, srcn, 0, src.length - sz);
                bytearraylists.add(0, srcn);                    
            }
        }
        if ( k > bytearraylists.size())
            System.out.println("remove:" + len + "(" + bytearraylists.size() + ")");
        return idx;
    }

    /** Stores bytes from src; must be non-null */
    public void write(byte[] src, int len) {
        byte[] copy = new byte[len];
        System.arraycopy(src, 0, copy, 0, copy.length);
        bytearraylists.add(copy);
        if (bytearraylists.size() > 100) bytearraylists.remove(0);
    }
}
