import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Arrays;

/**
*       ImageToAudioLocal
 *
 *      <p>
 *          The main class in the Image2Audio web app project.
 *          This class contains the program entry point and the main process.
 *      </p>
 *
 *      <p>
 *          The class name currently contains 'Local' because the original design contained
 *          a separate class for operating on a remote image on the web. With the move to a
 *          web app environment, only this version, for operating on a local image resource,
 *          is necessary. Eventually this will probably be renamed to remove 'Local' and reflect
 *          the 'Image2Audio' stylization used in the web app.
 *      </p>
 *
 *       @author Chris Drury
 */

public class ImageToAudioLocal {
    public enum WaveType { SINE, SQUARE, SAWTOOTH, TRIANGLE }       // Types of sound waves that can be used. Includes SINE, SQUARE, SAWTOOTH, and TRIANGLE.
    float scaleToWidth = 20f;                                       // The input image will be down-scaled to this width.
    float scaleToHeight = 20f;                                      // The input image will be down-scaled to this height.

    public ImageToAudioLocal(String url, WaveType waveType, int musicalScaleInt, String outputFilepath) {
        Image image = null;
        try {
            image = ImageIO.read(new URL(url));
        } catch (Exception e) {
            System.out.println("Invalid URL.");
            System.exit(0);
        }

        System.out.println("URL = " + url);
        System.out.println("Output = " + outputFilepath);

        boolean[] musicalScale = MusicalScale.getScaleByNumber(musicalScaleInt);
        if (musicalScale == null) {
            System.out.println("Invalid scale");
            System.exit(0);
        }

        byte[] pixels = ((DataBufferByte) scaleImage(image, scaleToWidth, scaleToHeight).getRaster().getDataBuffer()).getData();   // Pixel data from the image
        int maxArraySize = (int)(Math.pow(2, 24) - 1);
        byte[] byteStream = new byte[maxArraySize];                                     // A big array for storing data
        int arrayIndex = 0;                                                             // Index for storing data in the array
        int r, g, b;                                                                    // Color data of each pixel
        double angle;                                                                   // Used to create the sine wave

        int freq;                                                                       // Calculated frequency for each pixel
        int dur;                                                                        // Calculated duration
        float vol;                                                                      // Calculated volume
        float lastVol = -1;                                                             // The last note's volume for smooth transitions

        // Compressor bypassed until working properly
        Compressor comp = new Compressor(true, 100f, 4f, 3f, 10d, 10d, 20d);

        System.out.println("Generating audio.......");

        for (int i = 0; i < pixels.length - 2; i += 3) {
            // Get color from each pixel
            r = pixels[i];                                                              // Determines frequency
            g = pixels[i + 1];                                                          // Determines duration
            b = pixels[i + 2];                                                          // Determines volume

            // Calculate how many half-steps from middle-C we are
            int halfStepsUp = (int)(((float)r / 255) * ((MusicalConstants.scaleSize * MusicalConstants.octaves - 1)));

            // If this pitch isn't in the scale, drop it to an allowed pitch
            while (!musicalScale[Math.abs(halfStepsUp % MusicalConstants.scaleSize)]) {
                halfStepsUp--;
            }

            // Calculate note values
            freq = (int)(Math.pow(Math.pow(2, MusicalConstants.oneTwelfth), halfStepsUp) * MusicalConstants.rootNoteFreq);
            dur = (int)(MusicalConstants.minDur + ((float)g / 255) * (MusicalConstants.maxDur - MusicalConstants.minDur));
            vol = (MusicalConstants.minVol + ((float)b / 255) * (MusicalConstants.maxVol - MusicalConstants.minVol));

            double waveValue = 0;
            for (int n = 0; n < MusicalConstants.SAMPLE_RATE * dur; n++) {
                // Shouldn't happen with downscaling, but just in case...
                if (arrayIndex >= byteStream.length) {
                    System.out.println("Max array length reached. Cutting audio short.");
                    break;
                }

                // Add this wave to the byte array
                angle = (n / ((MusicalConstants.SAMPLE_RATE) / freq) * 2.0 * Math.PI) % (Math.PI * 2);
                switch (waveType) {
                    case SINE:
                        waveValue = Math.sin(angle);
                        break;
                    case SQUARE:
                        waveValue = 0;
                        for (int s = 1; s <= MusicalConstants.squareSteps * 2; s += 2) {
                            waveValue += Math.sin(angle / s) * (4 / Math.PI / s);
                        }

//                        System.out.println(waveValue);
                        break;
                    case SAWTOOTH:
                        waveValue = 0;
                        for (int s = 1; s <= MusicalConstants.sawSteps; s++) {
                            waveValue += Math.sin(angle * s) * (1.0 / s);
                        }
                        break;
                    case TRIANGLE:
                        if (angle <= Math.PI / 2)
                            waveValue = (angle / (Math.PI / 2));
                        else if (angle <= Math.PI * 3 / 2)
                            waveValue = 1 - ((angle - Math.PI / 2) / Math.PI) * 2;
                        else
                            waveValue = -1 + ((angle - Math.PI * 3 / 2) / (Math.PI / 2));
                        break;
                }

                if (lastVol != -1) {
                    waveValue *= 127.0 * (lastVol + (n / (MusicalConstants.SAMPLE_RATE * dur) * (vol - lastVol)));
                } else {
                    waveValue *= 127.0 * vol;
                }

                // Dynamic range compression attempt
                if (!comp.bypass) {
                    waveValue = comp.compress(waveValue);
                }

                byteStream[arrayIndex++] = (byte)(waveValue);
            }
            lastVol = vol;
        }

        // Copy only the bytes we've filled
        byte[] bytes = Arrays.copyOf(byteStream, arrayIndex);

        // Write the byte array to an audio file
        AudioFormat format = new AudioFormat(44100, 8, 1, true, true);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(bytes), format, bytes.length / format.getFrameSize());
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outputFilepath));
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Success!");
    }

    private BufferedImage scaleImage(Image im, float width, float height) {
        while (im.getWidth(null) == -1) {}
        float imW = im.getWidth(null);
        float imH = im.getHeight(null);
        float wScale = scaleToWidth / imW;
        float hScale = scaleToHeight / imH;
        float scale = Math.min(wScale, hScale);

        // Convert to a BufferedImage
        BufferedImage bIm = new BufferedImage((int)(imW * scale), (int)(imH * scale), BufferedImage.TYPE_3BYTE_BGR);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        bIm.getGraphics().drawImage(im, 0, 0, (int)(imW * scale), (int)(imH * scale), null);

        return bIm;
    }

    public static void main(String[] args) {
        if (args.length == 4) {
            try {
                String url = args[0];
                int waveTypeInt = Integer.parseInt(args[1]);
                int musicalScale = Integer.parseInt(args[2]);
                String outputFilepath = args[3];

                new ImageToAudioLocal(url, WaveType.values()[waveTypeInt], musicalScale, outputFilepath);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid numeric argument.");
            }
        } else
            System.err.println("Wrong number of arguments. Exiting....");
    }
}
