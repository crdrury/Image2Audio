public final class MusicalConstants {
    public static final float SAMPLE_RATE = 7200f;                                                      // Output sample rate for the audio file

    public static final int scaleSize = 12;                                                             // How many notes in a chromatic scale
    public static final float octaves = 3;                                                              // Octave range of this song
    public static final double rootNoteFreq = 65.4;                                                     // Frequency of Middle-C
    public static final double oneTwelfth = 1d / 12;                                                    // Used for calculating pitch
    public static final int minDur = 1, maxDur = 4;                                                     // Duration range
    public static final float minVol = 0.4f, maxVol = 0.8f;                                             // Volume range

    public static final float squareSteps = 3;
    public static final float sawSteps = 9;
}