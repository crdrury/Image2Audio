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
import java.util.Scanner;

public class ImageToAudioLocal {
    // Audio format info
    public static final float SAMPLE_RATE = 8000f;
    public static final int BIT_DEPTH = 8;

    // Wave type values
    public enum WaveType { SINE, SQUARE, SAWTOOTH, TRIANGLE }
    WaveType waveType;

    // Downscale the image to avoid 5-hour audio files
    float scaleToWidth = 20f;
    float scaleToHeight = 20f;

    boolean[][] allowPitch;
    int musicalScale;
    String audioFileName = "ImageSong.wav";

    public ImageToAudioLocal(String[] args) {
        // Define the musical scales
        allowPitch = new boolean[4][0];
        allowPitch[0] = new boolean[] {
                true, true, true, true, true, true,
                true, true, true, true, true, true };
        allowPitch[1] = new boolean[] {
                true, false, true, false, true, true,
                false, true, false, true, false, true };
        allowPitch[2] = new boolean[] {
                true, false, false, true, false, true,
                false, true, false, false, true, false };
        allowPitch[3] = new boolean[] {
                true, false, false, true, false, false,
                true, false, false, true, false, false};

        Image image = null;
        if (args.length == 4) {
            try {
                image = ImageIO.read(new URL(args[0]));
            } catch (Exception e) {
                System.out.println("Invalid URL.");
                System.exit(0);
            }
            int t = Integer.parseInt(args[1]);
            if (t < waveType.values().length)
                waveType = WaveType.values()[t];
            else {
                System.out.println("Invalid wave type");
                System.exit(0);
            }
            musicalScale = Integer.parseInt(args[2]);
            audioFileName = args[3];

            System.out.println("URL = " + args[0]);
            System.out.println("Output = " + args[3]);
        } else {
            // Request the name of the image to use
            Scanner scanner = new Scanner(System.in);
            System.out.println("Image filepath: ");
            String filepath = scanner.next();
            image = Toolkit.getDefaultToolkit().createImage(getClass().getResource(filepath));
            System.out.println("Wave type: ");
            System.out.println("0=sine, 1=square, 2=sawtooth, 3=triangle");
            int t = scanner.nextInt();
            if (t < waveType.values().length)
                waveType = WaveType.values()[t];
            else {
                System.out.println("Invalid wave type");
                System.exit(0);
            }
            System.out.println("Choose a scale: ");
            System.out.println("0=chromatic, 1=major, 2=pentatonic minor, 3=diminished");
            musicalScale = scanner.nextInt();
        }
        if (musicalScale > allowPitch.length) {
            System.out.println("Invalid scale");
            System.exit(0);
        }


        // Load the image and scale it down

        while (image.getWidth(null) == -1) {}
        float imW = image.getWidth(null);
        float imH = image.getHeight(null);
        float wScale = scaleToWidth / imW;
        float hScale = scaleToHeight / imH;
        float scale = Math.min(wScale, hScale);

        // Convert to a BufferedImage
        BufferedImage bIm = new BufferedImage((int)(imW * scale), (int)(imH * scale), BufferedImage.TYPE_3BYTE_BGR);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        bIm.getGraphics().drawImage(image, 0, 0, (int)(imW * scale), (int)(imH * scale), null);

        byte[] pixels = ((DataBufferByte) bIm.getRaster().getDataBuffer()).getData();   // Pixel data from the image
        int maxArraySize = (int)(Math.pow(2, 24) - 1);
        byte[] byteStream = new byte[maxArraySize];                                     // A big array for storing data
        int arrayIndex = 0;                                                             // Index for storing data in the array
        int r, g, b;                                                                    // Color data of each pixel
        double angle;                                                                   // Used to create the sine wave

        int scaleSize = 12;                                                             // How many notes in a chromatic scale
        float octaves = 3;                                                              // Octave range of this song
        double rootNoteFreq = 65.4;                                                     // Frequency of Middle-C
        double oneTwelfth = 1d / 12;                                                    // Used for calculating pitch
        int minDur = 1, maxDur = 4;                                                     // Duration range
        float minVol = 0.4f, maxVol = 1f;                                               // Volume range

        int freq;                                                                       // Calculated frequency for each pixel
        int dur;                                                                        // Calculated duration
        float vol;                                                                      // Calculated volume
        float lastVol = -1;                                                             // The last note's volume for smooth transitions

        // WIP dynamic range compression variables
        boolean bypass = true;
        float threshold = 100f;
        float ratio = 4.0f;
        float makeUpGain = 3f;
        double attack = 10f;
        double hold = 10f;
        double release = 20f;
        float compTimer = -1;

        float squareSteps = 3;
        float sawSteps = 9;

        System.out.println("Generating audio.......");

        for (int i = 0; i < pixels.length - 2; i += 3) {
            // Get color from each pixel
            r = pixels[i];                                                              // Determines frequency
            g = pixels[i + 1];                                                          // Determines duration
            b = pixels[i + 2];                                                          // Determines volume

            // Calculate how many half-steps from middle-C we are
            int halfStepsUp = (int)(((float)r / 255) * ((scaleSize * octaves - 1)));

            // If this pitch isn't in the scale, drop it to an allowed pitch
            while (!allowPitch[musicalScale][Math.abs(halfStepsUp%scaleSize)]) {
                halfStepsUp--;
            }

            // Calculate note values
            freq = (int)(Math.pow(Math.pow(2, oneTwelfth), halfStepsUp) * rootNoteFreq);
            dur = (int)(minDur + ((float)g / 255) * (maxDur - minDur));
            vol = (minVol + ((float)b / 255) * (maxVol - minVol));

            for (int n = 0; n < SAMPLE_RATE * dur; n++) {
                // Shouldn't happen with downscaling, but just in case...
                if (arrayIndex >= byteStream.length) {
                    System.out.println("Max array length reached");
                    break;
                }

                // Add this wave to the byte array
                angle = (n / ((SAMPLE_RATE) / freq) * 2.0 * Math.PI) % (Math.PI * 2);
                double waveValue = 0;
                switch (waveType) {
                    case SINE:
                        waveValue = Math.sin(angle);
                        break;
                    case SQUARE:
                        waveValue = 0;
                        for (int s = 1; s <= squareSteps * 2; s += 2) {
                            waveValue += Math.sin(angle / s) * (4 / Math.PI / s);
                        }

//                        System.out.println(waveValue);
                        break;
                    case SAWTOOTH:
                        waveValue = 0;
                        for (int s = 1; s <= sawSteps; s++) {
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
                    waveValue *= 127.0 * (lastVol + (n / (SAMPLE_RATE * dur) * (vol - lastVol)));
                } else {
                    waveValue *= 127.0 * vol;
                }

                // Dynamic range compression attempt
                if (!bypass) {
                    if (Math.abs(waveValue) > threshold) {
                        compTimer = 0f;
                    }
                    double dif = waveValue - threshold;
                    if (compTimer < attack) {
                        double attackTime = compTimer - attack - hold;
                        double percent = attackTime / attack;
                        waveValue = threshold + dif / ratio / percent;
                    } else if (compTimer < attack + hold) {
                        waveValue = threshold + dif / ratio;
                    } else if (compTimer < attack + hold + release) {
                        double releaseTime = compTimer - attack - hold;
                        double percent = releaseTime / release;
                        waveValue = threshold + dif / ratio * percent;
                    }
                    compTimer += 1 / SAMPLE_RATE;
                    waveValue += makeUpGain;
                }

                byteStream[arrayIndex++] = (byte)(waveValue);
            }
            lastVol = vol;
        }

        // Copy only the bytes we've filled
        byte[] bytes = Arrays.copyOf(byteStream, arrayIndex);

        // Write the byte array to an audio file
        AudioFormat format = new AudioFormat(44100, 8, 1, true, true);
        AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(bytes), format, bytes.length / format.getFrameSize());
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(audioFileName));
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Success!");
    }

    public static void main(String[] args) {
        new ImageToAudioLocal(args);
    }
}
