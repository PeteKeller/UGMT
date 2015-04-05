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
import java.io.*;
import java.net.*;
import harn.repository.*;
import rpg.*;
import org.tritonus.share.sampled.TConversionTool;

/**
 * Mixing several sound byte streams into another. Original idea inspired by
 * MixingAudioInputStream.java from Matthias Pfisterer at
 * http://www.jsresources.org.
 * @author Michael Jung
 */
public class MixedByteStream extends AudioInputStream {
    /** List of ByteQueues */
    private ArrayList mixbase;

    /** Export Audio format */
    private static AudioFileFormat.Type MP3 =
        org.tritonus.share.sampled.AudioFileTypes.getType("MP3", "mp3");

    /** Export audio encoding */
    private static AudioFormat.Encoding MPEG1L3 =
        org.tritonus.share.sampled.Encodings.getEncoding("MPEG1L3");

    private static AudioFormat MIXFORMAT =
        new AudioFormat(44100, 16, 2, true, true);
    /** Constructor */
    public MixedByteStream() {
        super
            (new ByteArrayInputStream(new byte[0]),
             MIXFORMAT, AudioSystem.NOT_SPECIFIED);
        mixbase = new ArrayList();
    }

    /** Add byte queue */
    public synchronized void add(ByteQueue nQueue) {
        mixbase.add(nQueue);
    }

    /** Remove byte queue */
    public synchronized void remove(ByteQueue nQueue) {
        mixbase.remove(nQueue);
    }

    /** Add byte queue */
    public synchronized void add(AudioInputStream nQueue) {
        mixbase.add(nQueue);
    }

    /** Remove byte queue */
    public synchronized void remove(AudioInputStream nQueue) {
        mixbase.remove(nQueue);
    }

    /**
     * The maximum of the frame length of the input stream is calculated and
     * returned.  If at least one of the input streams has length
     * <code>AudioInputStream.NOT_SPECIFIED</code>, this value is returned.
     */
    public synchronized long getFrameLength() {
        long len = 0;
        for (int i = 0; i < mixbase.size(); i++) {
            AudioInputStream stream = (AudioInputStream) mixbase.get(i);
            long l = stream.getFrameLength();
            if (l == AudioSystem.NOT_SPECIFIED)
                return AudioSystem.NOT_SPECIFIED;

            len = Math.max(len, l);
        }
        return len;
    }

    /** IF method */
    public synchronized int read() throws IOException {
        int sample = 0;
        for (int i = 0; i < mixbase.size(); i++) {
            AudioInputStream stream = (AudioInputStream) mixbase.get(i);
            int byt = stream.read();
            if (byt == -1) continue; else sample += byt;
        }
        return (byte) (sample & 0xFF);
    }

    /** IF method */
    public synchronized int read(byte[] data, int offset, int len) throws IOException {
        int ocls = getFormat().getChannels(); // = 2
        int osz = getFormat().getSampleSizeInBits()/8; // = 2 (byte)
        int oclsz = ocls*osz;
        boolean obe = getFormat().isBigEndian(); // true
        float osr = getFormat().getSampleRate();
        int osam = (int)(len/oclsz);
        int olen = osam*oclsz;

        byte[][] buf = new byte[mixbase.size()][];
        int[] bytesRead = new int[mixbase.size()];
        ByteQueue[] stream = new ByteQueue[mixbase.size()];
        int[] isz = new int[mixbase.size()];
        int[] icls = new int[mixbase.size()];
        boolean[] ibe = new boolean[mixbase.size()];
        float[] isr = new float[mixbase.size()];
        int ilen[] = new int[mixbase.size()];
        int[] iclsz = new int[mixbase.size()];

        for (int j = 0; j < mixbase.size(); j++) {
            stream[j] = (ByteQueue) mixbase.get(j);
            isz[j] = stream[j].getFormat().getSampleSizeInBits()/8;
            icls[j] = stream[j].getFormat().getChannels();
            iclsz[j] = isz[j]*icls[j];
            ibe[j] = stream[j].getFormat().isBigEndian();
            isr[j] = stream[j].getFormat().getSampleRate();
            int iidcs = (int)(osam*isr[j]/osr + .5);
            ilen[j] = (iidcs + 1)*iclsz[j];
            buf[j] = new byte[ilen[j]];

            // For reasons that I do notunderstand reading the buffer at once
            // yields a different reasult than reading it sample by sample
            stream[j].read(buf[j], 0, ilen[j]);
            int ii = 0;
            for (int i = 0; i < osam; i++) {
                int nii = (int)(i*isr[j]/osr + .5)*iclsz[j];
                //stream[j].read(buf[j], ii, nii - ii);
                /*
int k = 0;
for (int n = ii; n < nii-ii; n++) if (buf[j][n] != 0) k++;
System.out.println(">" + (nii-ii) + ":" + k);
                */
                ii = nii;
            }
        }

        int[] mixedSamples = new int[ocls]; // Big-Endian
        for (int oidx = 0; oidx < len; oidx += oclsz) {
            for (int i = 0; i < mixedSamples.length; i++) mixedSamples[i] = 0;

            // Mixing a Sample
            for (int j = 0; j < mixbase.size(); j++) {
                int iidx = (int)(oidx/(oclsz)*isr[j]/osr + .5)*iclsz[j];
                if (bytesRead[j] == -1) continue;

                for (int channel = 0; channel < ocls; channel++) {
                    int tmp;
                    switch (isz[j]) {
                    case 1: tmp = buf[j][iidx + channel * (icls[j]-1)];
                        break;
                    case 2: tmp = TConversionTool.bytesToInt16
                                (buf[j], iidx + channel * 2 * (icls[j]-1), ibe[j]);
                        break;
                    case 3: tmp = TConversionTool.bytesToInt24
                                (buf[j], iidx + channel * 3 * (icls[j]-1), ibe[j]);
                        break;
                    default: tmp = TConversionTool.bytesToInt32
                                (buf[j], iidx + channel * 4 * (icls[j]-1), ibe[j]);
                        break;
                    }
                    mixedSamples[channel] += tmp;
                }
            }

            // Writing a sample
            for (int channel = 0; channel < ocls; channel++) {
                int dataOffset = offset + oidx + channel * osz;
                // Big-Endian
                data[dataOffset] = (byte)(mixedSamples[channel] >> 8);
                data[dataOffset + 1] = (byte)(mixedSamples[channel] & 0xff);
            }
        }
        return olen;
    }

    static private AudioInputStream convert(AudioInputStream ais) {
        AudioFormat format = ais.getFormat();
        if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
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
            return AudioSystem.getAudioInputStream(tmp, ais);
        }
        return ais;
    }

    /**
     * Calls skip() on all input streams. There is no way to assure that the
     * number of bytes really skipped is the same for all input streams. Due
     * to that, this method always returns the passed value. In other words:
     * the return value is useless (better ideas appreciated).
     */
    public synchronized long skip(long len) throws IOException {
        Iterator iter = mixbase.iterator();
        while (iter.hasNext())
            ((AudioInputStream)iter.next()).skip(len);
        return len;
    }

    /**
     * The minimum of available() of all input stream is calculated and
     * returned.
     */
    public synchronized int available() throws IOException {
        int avail = 0;
        Iterator iter = mixbase.iterator();
        while (iter.hasNext())
            avail = Math.min
                (avail, ((AudioInputStream) iter.next()).available());
        return avail;
    }

    /** We can ignore this method. */
    public void close() throws IOException {}

    /**
     * Calls mark() on all input streams.
     */
    public synchronized void mark(int nReadLimit) {
        Iterator iter = mixbase.iterator();
        while (iter.hasNext())
            ((AudioInputStream) iter.next()).mark(nReadLimit);
    }

    /**
     * Calls reset() on all input streams.
     */
    public synchronized void reset() throws IOException {
        Iterator iter = mixbase.iterator();
        while (iter.hasNext())
            ((AudioInputStream) iter.next()).reset();
    }

    /**
     * Returns true if all input stream return true for markSupported().
     */
    public synchronized boolean markSupported() {
        Iterator iter = mixbase.iterator();
        while (iter.hasNext())
            if (!((AudioInputStream)iter.next()).markSupported())
                return false;
        return true;
    }


    public static void main(String[] args)
    {
        final MixedByteStream x = new MixedByteStream();
        Thread t = new Thread() {
                public void run() {
                    try {
                        try {
                            AudioInputStream is = convert(AudioSystem.getAudioInputStream(new File("sound/sounds/animal/hooves.mp3")));
                            x.add(is);
                            is = AudioSystem.getAudioInputStream(MPEG1L3, x);
                            AudioSystem.write(is, MP3, new File("test.mp3"));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        t.start();
        try {
            Thread.sleep(1000);
            x.add(AudioSystem.getAudioInputStream
                  (new File("sound/sounds/animal/cow1.wav")));
            Thread.sleep(2000);
            x.add(convert(AudioSystem.getAudioInputStream
                  (new File("sound/sounds/bark.au"))));
            Thread.sleep(2000);
            t.stop();
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }
}

/*
    public int readx(byte[] data, int offset, int len) throws IOException {
        int sample = 0;
        int channels = getFormat().getChannels();
        int sampleSize = getFormat().getSampleSizeInBits()/8;
        boolean bBigEndian = getFormat().isBigEndian();

        int[] mixedSamples = new int[channels];
        byte[] buf = new byte[sampleSize*channels];
        for (sample = 0; sample < len; sample += sampleSize*channels) {
            for (int i = 0; i < channels; i++) mixedSamples[i] = 0;

            for (int j = 0; j < mixbase.size(); j++) {
                AudioInputStream stream = (AudioInputStream) mixbase.get(j);
                int sz = stream.getFormat().getSampleSizeInBits()/8;
                int nBytesRead = stream.read(buf, 0, sampleSize*channels);
                if (nBytesRead == -1)
                    continue;

                for (int nChannel = 0; nChannel < channels; nChannel++) {
                    int nBufferOffset = nChannel * sampleSize;
                    mixedSamples[nChannel] += TConversionTool.bytesToInt16(buf, nBufferOffset, bBigEndian);
                } // loop over channels
            } // loop over streams

            for (int nChannel = 0; nChannel < channels; nChannel++) {
                int nBufferOffset = offset + sample + nChannel * sampleSize;
                TConversionTool.intToBytes16(mixedSamples[nChannel], data, nBufferOffset, bBigEndian);
            } // (final) loop over channels
        } // loop over frames
        return len;
    }
 */
