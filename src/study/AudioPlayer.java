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

public class AudioPlayer
{
    /**	Flag for debugging messages.
     *	If true, some messages are dumped to the console
     *	during operation.	
     */
    private static boolean DEBUG = true;

    private static int	DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) throws Exception {
        String	strMixerName = null;
        int	nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        int	nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

        String	strFilenameOrUrl = args[0];

        AudioInputStream audioInputStream = null;
        File file = new File(strFilenameOrUrl);
        audioInputStream = AudioSystem.getAudioInputStream(file);
	
        AudioFormat audioFormat = audioInputStream.getFormat();
        if (DEBUG) out("AudioPlayer.main(): primary format: " + audioFormat);
        DataLine.Info info = new DataLine.Info
            (SourceDataLine.class, audioFormat, nInternalBufferSize);
        boolean	bIsSupportedDirectly = AudioSystem.isLineSupported(info);
        if (!bIsSupportedDirectly) {
            AudioFormat	sourceFormat = audioFormat;
            AudioFormat	targetFormat = new AudioFormat
                (AudioFormat.Encoding.PCM_SIGNED,
                 sourceFormat.getSampleRate(),
                 16,
                 sourceFormat.getChannels(),
                 sourceFormat.getChannels() * 2,
                 sourceFormat.getSampleRate(),
                 false);
            if (DEBUG) {
                out("AudioPlayer.main(): source format: " + sourceFormat);
                out("AudioPlayer.main(): target format: " + targetFormat);
            }
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                audioFormat = audioInputStream.getFormat();
                if (DEBUG) out("AudioPlayer.main(): converted AIS: " + audioInputStream);
                if (DEBUG) out("AudioPlayer.main(): converted format: " + audioFormat);
            }

        SourceDataLine	line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
        if (line == null)
            {
                out("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
                System.exit(1);
            }
        if (DEBUG) out("AudioPlayer.main(): line: " + line);
        if (DEBUG) out("AudioPlayer.main(): line format: " + line.getFormat());
        if (DEBUG) out("AudioPlayer.main(): line buffer size: " + line.getBufferSize());


        /*
         *	Still not enough. The line now can receive data,
         *	but will not pass them on to the audio output device
         *	(which means to your sound card). This has to be
         *	activated.
         */
        line.start();

        /*
         *	Ok, finally the line is prepared. Now comes the real
         *	job: we have to write data to the line. We do this
         *	in a loop. First, we read data from the
         *	AudioInputStream to a buffer. Then, we write from
         *	this buffer to the Line. This is done until the end
         *	of the file is reached, which is detected by a
         *	return value of -1 from the read method of the
         *	AudioInputStream.
         */
        int	nBytesRead = 0;
        byte[]	abData = new byte[nExternalBufferSize];
        if (DEBUG) out("AudioPlayer.main(): starting main loop");
        while (nBytesRead != -1)
            {
                try
                    {
                        nBytesRead = audioInputStream.read(abData, 0, abData.length);
                    }
                catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                if (DEBUG) out("AudioPlayer.main(): read from AudioInputStream (bytes): " + nBytesRead);
                if (nBytesRead >= 0)
                    {
                        int	nBytesWritten = line.write(abData, 0, nBytesRead);
                        if (DEBUG) out("AudioPlayer.main(): written to SourceDataLine (bytes): " + nBytesWritten);
                    }
            }

        if (DEBUG) out("AudioPlayer.main(): finished main loop");

        /*
         *	Wait until all data is played.
         *	This is only necessary because of the bug noted below.
         *	(If we do not wait, we would interrupt the playback by
         *	prematurely closing the line and exiting the VM.)
         *
         *	Thanks to Margie Fitch for bringing me on the right
         *	path to this solution.
         */
        if (DEBUG) out("AudioPlayer.main(): before drain");
        line.drain();

        /*
         *	All data are played. We can close the shop.
         */
        if (DEBUG) out("AudioPlayer.main(): before close");
        line.close();

        // 		/*
        // 		 *	There is a bug in the Sun jdk1.3/1.4.
        // 		 *	It prevents correct termination of the VM.
        // 		 *	So we have to exit ourselves.
        //		 *	This bug has been fixed for the Sun JDK1.5.0.
        // 		 */
        // 		if (DEBUG)
        // 		{
        // 			out("AudioPlayer.main(): before exit");
        // 		}
        // 		System.exit(0);
    }


    // TODO: maybe can used by others. AudioLoop?
    // In this case, move to AudioCommon.
    private static SourceDataLine getSourceDataLine(String strMixerName,
                                                    AudioFormat audioFormat,
                                                    int nBufferSize)
    {
        /*
         *	Asking for a line is a rather tricky thing.
         *	We have to construct an Info object that specifies
         *	the desired properties for the line.
         *	First, we have to say which kind of line we want. The
         *	possibilities are: SourceDataLine (for playback), Clip
         *	(for repeated playback)	and TargetDataLine (for
         *	 recording).
         *	Here, we want to do normal playback, so we ask for
         *	a SourceDataLine.
         *	Then, we have to pass an AudioFormat object, so that
         *	the Line knows which format the data passed to it
         *	will have.
         *	Furthermore, we can give Java Sound a hint about how
         *	big the internal buffer for the line should be. This
         *	isn't used here, signaling that we
         *	don't care about the exact size. Java Sound will use
         *	some default value for the buffer size.
         */
        SourceDataLine	line = null;
        DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
                                                 audioFormat, nBufferSize);
        try
            {
                if (strMixerName != null)
                    {
                        Mixer.Info	mixerInfo = AudioCommon.getMixerInfo(strMixerName);
                        if (mixerInfo == null)
                            {
                                out("AudioPlayer: mixer not found: " + strMixerName);
                                System.exit(1);
                            }
                        Mixer	mixer = AudioSystem.getMixer(mixerInfo);
                        line = (SourceDataLine) mixer.getLine(info);
                    }
                else
                    {
                        line = (SourceDataLine) AudioSystem.getLine(info);
                    }

                /*
                 *	The line is there, but it is not yet ready to
                 *	receive audio data. We have to open the line.
                 */
                line.open(audioFormat, nBufferSize);
            }
        catch (LineUnavailableException e)
            {
                if (DEBUG) e.printStackTrace();
            }
        catch (Exception e)
            {
                if (DEBUG) e.printStackTrace();
            }
        return line;
    }



    private static void printUsageAndExit()
    {
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
        out("\t\t<soundfileOrUrl>");
        System.exit(1);
    }



    private static void out(String strMessage)
    {
        System.out.println(strMessage);
    }
}



/*** AudioPlayer.java ***/

