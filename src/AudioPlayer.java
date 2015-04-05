/*
 *	AudioPlayer.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999, 2000 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 |&lt;---            this code is formatted to fit into 80 columns             --->|
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/*
 * If the compilation fails because this class is not available, get gnu.getopt
 * from the URL given in the comment below.
 */

/**
 * &lt;titleabbrev>AudioPlayer&lt;/titleabbrev> &lt;title>Playing an audio file
 * (advanced)&lt;/title>
 * 
 * &lt;formalpara>&lt;title>Purpose&lt;/title> &lt;para> Plays a single audio
 * file. Capable of playing some compressed audio formats (A-law, &amp;mu;-law,
 * maybe ogg vorbis, mp3, GSM06.10). Allows control over buffering and which
 * mixer to use. &lt;/para>&lt;/formalpara>
 * 
 * &lt;formalpara>&lt;title>Usage&lt;/title> &lt;para> &lt;cmdsynopsis>
 * &lt;command>java AudioPlayer&lt;/command> &lt;arg
 * choice="plain">&lt;option>-l&lt;/option>&lt;/arg> &lt;/cmdsynopsis>
 * &lt;cmdsynopsis> &lt;command>java AudioPlayer&lt;/command>
 * &lt;arg>&lt;option>-M
 * &lt;replaceable>mixername&lt;/replaceable>&lt;/option>&lt;/arg>
 * &lt;arg>&lt;option>-e
 * &lt;replaceable>buffersize&lt;/replaceable>&lt;/option>&lt;/arg>
 * &lt;arg>&lt;option>-i
 * &lt;replaceable>buffersize&lt;/replaceable>&lt;/option>&lt;/arg> &lt;arg
 * choice="plain">&lt;replaceable>audiofile&lt;/replaceable>&lt;/arg>
 * &lt;/cmdsynopsis> &lt;/para>&lt;/formalpara>
 * 
 * &lt;formalpara>&lt;title>Parameters&lt;/title> &lt;variablelist>
 * &lt;varlistentry> &lt;term>&lt;option>-h&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>print usage message&lt;/para>&lt;/listitem>
 * &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-l&lt;/option>&lt;/term> &lt;listitem>&lt;para>lists the
 * available mixers&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-M
 * &lt;replaceable>mixername&lt;/replaceable>&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>selects a mixer to play on&lt;/para>&lt;/listitem>
 * &lt;/varlistentry> &lt;varlistentry> &lt;term>&lt;option>-e
 * &lt;replaceable>buffersize&lt;/replaceable>&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>the buffer size to use in the application
 * ("extern")&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-i
 * &lt;replaceable>buffersize&lt;/replaceable>&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>the buffer size to use in Java Sound
 * ("intern")&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-E
 * &lt;replaceable>endianess&lt;/replaceable>&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>the endianess ("big" or "little") to use in
 * conversions. The default is little. Specifying this option forces a
 * conversion, even if the audio format is supported by SourceDataLines
 * directly.&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-S &lt;replaceable>sample
 * size&lt;/replaceable>&lt;/option>&lt;/term> &lt;listitem>&lt;para>the sample
 * size in bits to use in conversions. The default is 16. Specifying this option
 * forces a conversion, even if the audio format is supported by SourceDataLines
 * directly.&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-D&lt;/option>&lt;/term> &lt;listitem>&lt;para>enable
 * debugging output&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-f&lt;/option>&lt;/term> &lt;listitem>&lt;para>interpret
 * filename arguments as filenames. This is the default. This option is
 * exclusive to &lt;option>-u&lt;/option>.&lt;/para>&lt;/listitem>
 * &lt;/varlistentry> &lt;varlistentry>
 * &lt;term>&lt;option>-u&lt;/option>&lt;/term> &lt;listitem>&lt;para>interpret
 * filename arguments as URLs. The default is to interpret them as filenames.
 * This option is exclusive to
 * &lt;option>-f&lt;/option>.&lt;/para>&lt;/listitem> &lt;/varlistentry>
 * &lt;varlistentry>
 * &lt;term>&lt;option>&lt;replaceable>audiofile&lt;/replaceable>&lt;/option>&lt;/term>
 * &lt;listitem>&lt;para>the file name of the audio file to
 * play&lt;/para>&lt;/listitem> &lt;/varlistentry> &lt;/variablelist>
 * &lt;/formalpara>
 * 
 * &lt;formalpara>&lt;title>Bugs, limitations&lt;/title> &lt;para> Compressed
 * formats can be handled depending on the capabilities of the Java Sound
 * implementation. A-law and &amp;mu;-law can be handled in any known Java Sound
 * implementation. Ogg vorbis, mp3 and GSM 06.10 can be handled by Tritonus. If
 * you want to play these formats with the Sun jdk1.3/1.4, you have to install
 * the respective plug-ins from &lt;ulink url
 * ="http://www.tritonus.org/plugins.html">Tritonus Plug-ins&lt;/ulink>.
 * &lt;/para> &lt;/formalpara>
 * 
 * &lt;formalpara>&lt;title>Source code&lt;/title> &lt;para> &lt;ulink
 * url="AudioPlayer.java.html">AudioPlayer.java&lt;/ulink>, &lt;ulink
 * url="AudioCommon.java.html">AudioCommon.java&lt;/ulink>, &lt;olink
 * targetdocent="getopt">gnu.getopt.Getopt&lt;/olink> &lt;/para>
 * &lt;/formalpara>
 * 
 */
public class AudioPlayer {
	/**
	 * Flag for debugging messages. If true, some messages are dumped to the
	 * console during operation.
	 */
	private static boolean DEBUG = false;

	private static int DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

	public static void main(String[] args) throws Exception {
		/**
		 * Determines if command line arguments are intereted as URL. If true,
		 * filename arguments on the command line are interpreted as URL. If
		 * false, they are interpreted as filenames. This flag is set by the
		 * command line option "-u". It is reset by the command line option
		 * "-f".
		 */
		boolean bInterpretFilenameAsUrl = false;

		/**
		 * Flag for forcing a conversion. If set to true, a conversion of the
		 * AudioInputStream (AudioSystem.getAudioInputStream(...,
		 * AudioInputStream)) is done even if the format of the original
		 * AudioInputStream would be supported for SourceDataLines directly.
		 * This flag is set by the command line options "-E" and "-S".
		 */
		boolean bForceConversion = false;

		/**
		 * Endianess value to use in conversion. If a conversion of the
		 * AudioInputStream is done, this values is used as endianess in the
		 * target AudioFormat. The default value can be altered by the command
		 * line option "-B".
		 */
		boolean bBigEndian = false;

		/**
		 * Sample size value to use in conversion. If a conversion of the
		 * AudioInputStream is done, this values is used as sample size in the
		 * target AudioFormat. The default value can be altered by the command
		 * line option "-S".
		 */
		int nSampleSizeInBits = 16;

		String strMixerName = null;

		int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;

		int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

		/*
		 * Parsing of command-line options takes place...
		 */
		while (args.length > 0) {
			char c = args[0].charAt(0);

			switch (c) {
			case 'h':
				printUsageAndExit();

			case 'l':
				AudioCommon.listMixersAndExit(true);

			case 'u':
				bInterpretFilenameAsUrl = true;
				break;

			case 'f':
				bInterpretFilenameAsUrl = false;
				break;

			case 'M':
				strMixerName = args[1];
				if (DEBUG)
					out("AudioPlayer.main(): mixer name: " + strMixerName);
				break;

			case 'e':
				nExternalBufferSize = Integer.parseInt(args[1]);
				break;

			case 'i':
				nInternalBufferSize = Integer.parseInt(args[1]);
				break;

			case 'E':
				String strEndianess = args[1];
				strEndianess = strEndianess.toLowerCase();
				if (strEndianess.equals("big")) {
					bBigEndian = true;
				} else if (strEndianess.equals("little")) {
					bBigEndian = false;
				} else {
					printUsageAndExit();
				}
				bForceConversion = true;
				break;

			case 'S':
				nSampleSizeInBits = Integer.parseInt(args[1]);
				bForceConversion = true;
				break;

			case 'D':
				DEBUG = true;
				break;

			case '?':
				printUsageAndExit();

			default:
				out("getopt() returned " + c);
				break;
			}
			String[] nargs = new String[args.length - 1];
			for (int i = 0; i < nargs.length; i++)
				nargs[i] = args[i + 1];
			args = nargs;
		}

		/*
		 * We make shure that there is only one more argument, which we take as
		 * the filename of the soundfile we want to play.
		 */
		String strFilenameOrUrl = null;
		for (int i = 0; i < args.length; i++) {
			if (strFilenameOrUrl == null) {
				strFilenameOrUrl = args[i];
			} else {
				printUsageAndExit();
			}
		}
		if (strFilenameOrUrl == null) {
			printUsageAndExit();
		}

		AudioInputStream audioInputStream = null;
		if (bInterpretFilenameAsUrl) {
			URL url = new URL(strFilenameOrUrl);
			audioInputStream = AudioSystem.getAudioInputStream(url);
		} else {
			// Are we requested to use standard input?
			if (strFilenameOrUrl.equals("-")) {
				InputStream inputStream = new BufferedInputStream(System.in);
				audioInputStream = AudioSystem.getAudioInputStream(inputStream);
			} else {
				File file = new File(strFilenameOrUrl);
				audioInputStream = AudioSystem.getAudioInputStream(file);
			}
		}

		if (DEBUG)
			out("AudioPlayer.main(): primary AIS: " + audioInputStream);

		/*
		 * From the AudioInputStream, i.e. from the sound file, we fetch
		 * information about the format of the audio data. These information
		 * include the sampling frequency, the number of channels and the size
		 * of the samples. These information are needed to ask Java Sound for a
		 * suitable output line for this audio stream.
		 */
		AudioFormat audioFormat = audioInputStream.getFormat();
		if (DEBUG)
			out("AudioPlayer.main(): primary format: " + audioFormat);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
				audioFormat, nInternalBufferSize);
		boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
		if (!bIsSupportedDirectly || bForceConversion) {
			AudioFormat sourceFormat = audioFormat;
			AudioFormat targetFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED, sourceFormat
							.getSampleRate(), nSampleSizeInBits, sourceFormat
							.getChannels(), sourceFormat.getChannels()
							* (nSampleSizeInBits / 8), sourceFormat
							.getSampleRate(), bBigEndian);
			if (DEBUG) {
				out("AudioPlayer.main(): source format: " + sourceFormat);
				out("AudioPlayer.main(): target format: " + targetFormat);
			}
			audioInputStream = AudioSystem.getAudioInputStream(targetFormat,
					audioInputStream);
			audioFormat = audioInputStream.getFormat();
			if (DEBUG)
				out("AudioPlayer.main(): converted AIS: " + audioInputStream);
			if (DEBUG)
				out("AudioPlayer.main(): converted format: " + audioFormat);
		}

		SourceDataLine line = getSourceDataLine(strMixerName, audioFormat,
				nInternalBufferSize);
		if (line == null) {
			out("AudioPlayer: cannot get SourceDataLine for format "
					+ audioFormat);
			System.exit(1);
		}
		if (DEBUG)
			out("AudioPlayer.main(): line: " + line);
		if (DEBUG)
			out("AudioPlayer.main(): line format: " + line.getFormat());
		if (DEBUG)
			out("AudioPlayer.main(): line buffer size: " + line.getBufferSize());

		/*
		 * Still not enough. The line now can receive data, but will not pass
		 * them on to the audio output device (which means to your sound card).
		 * This has to be activated.
		 */
		line.start();

		/*
		 * Ok, finally the line is prepared. Now comes the real job: we have to
		 * write data to the line. We do this in a loop. First, we read data
		 * from the AudioInputStream to a buffer. Then, we write from this
		 * buffer to the Line. This is done until the end of the file is
		 * reached, which is detected by a return value of -1 from the read
		 * method of the AudioInputStream.
		 */
		int nBytesRead = 0;
		byte[] abData = new byte[nExternalBufferSize];
		if (DEBUG)
			out("AudioPlayer.main(): starting main loop");
		while (nBytesRead != -1) {
			try {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (DEBUG)
				out("AudioPlayer.main(): read from AudioInputStream (bytes): "
						+ nBytesRead);
			if (nBytesRead >= 0) {
				int nBytesWritten = line.write(abData, 0, nBytesRead);
				if (DEBUG)
					out("AudioPlayer.main(): written to SourceDataLine (bytes): "
							+ nBytesWritten);
			}
		}

		if (DEBUG)
			out("AudioPlayer.main(): finished main loop");

		/*
		 * Wait until all data is played. This is only necessary because of the
		 * bug noted below. (If we do not wait, we would interrupt the playback
		 * by prematurely closing the line and exiting the VM.)
		 * 
		 * Thanks to Margie Fitch for bringing me on the right path to this
		 * solution.
		 */
		if (DEBUG)
			out("AudioPlayer.main(): before drain");
		line.drain();

		/*
		 * All data are played. We can close the shop.
		 */
		if (DEBUG)
			out("AudioPlayer.main(): before close");
		line.close();

		// /*
		// * There is a bug in the Sun jdk1.3/1.4.
		// * It prevents correct termination of the VM.
		// * So we have to exit ourselves.
		// * This bug has been fixed for the Sun JDK1.5.0.
		// */
		// if (DEBUG)
		// {
		// out("AudioPlayer.main(): before exit");
		// }
		// System.exit(0);
	}

	// TODO: maybe can used by others. AudioLoop?
	// In this case, move to AudioCommon.
	private static SourceDataLine getSourceDataLine(String strMixerName,
			AudioFormat audioFormat, int nBufferSize) {
		/*
		 * Asking for a line is a rather tricky thing. We have to construct an
		 * Info object that specifies the desired properties for the line.
		 * First, we have to say which kind of line we want. The possibilities
		 * are: SourceDataLine (for playback), Clip (for repeated playback) and
		 * TargetDataLine (for recording). Here, we want to do normal playback,
		 * so we ask for a SourceDataLine. Then, we have to pass an AudioFormat
		 * object, so that the Line knows which format the data passed to it
		 * will have. Furthermore, we can give Java Sound a hint about how big
		 * the internal buffer for the line should be. This isn't used here,
		 * signaling that we don't care about the exact size. Java Sound will
		 * use some default value for the buffer size.
		 */
		SourceDataLine line = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
				audioFormat, nBufferSize);
		try {
			if (strMixerName != null) {
				Mixer.Info mixerInfo = AudioCommon.getMixerInfo(strMixerName);
				if (mixerInfo == null) {
					out("AudioPlayer: mixer not found: " + strMixerName);
					System.exit(1);
				}
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				line = (SourceDataLine) mixer.getLine(info);
			} else {
				line = (SourceDataLine) AudioSystem.getLine(info);
			}

			/*
			 * The line is there, but it is not yet ready to receive audio data.
			 * We have to open the line.
			 */
			line.open(audioFormat, nBufferSize);
		} catch (LineUnavailableException e) {
			if (DEBUG)
				e.printStackTrace();
		} catch (Exception e) {
			if (DEBUG)
				e.printStackTrace();
		}
		return line;
	}

	private static void printUsageAndExit() {
		out("AudioPlayer: usage:");
		out("\tjava AudioPlayer -h");
		out("\tjava AudioPlayer -l");
		out("\tjava AudioPlayer");
		out("\t\t[-M <mixername>]");
		out("\t\t[-e <externalBuffersize>]");
		out("\t\t[-i <internalBuffersize>]");
		out("\t\t[-S <SampleSizeInBits>]");
		out("\t\t[-B (big | little)]");
		out("\t\t[-D]");
		out("\t\t[-u | -f]");
		out("\t\t<soundfileOrUrl>t;");
		System.exit(1);
	}

	private static void out(String strMessage) {
		System.out.println(strMessage);
	}
}

/** * AudioPlayer.java ** */

