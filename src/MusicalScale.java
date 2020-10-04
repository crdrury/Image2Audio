/**
 * MusicalScale
 *
 * <p>This class stores definitions for the musical scales that can be used. Scales are saved as arrays of
 * boolean values, representing whether or not each chromatic pitch is allowed in the scale.</p>
 *
 * @author Chris Drury
 */

public class MusicalScale {
    private static boolean[] CHROMATIC_SCALE         = new boolean [] { true,    true,   true,   true,   true,   true,   true,   true,   true,   true,   true,   true    };
    private static boolean[] MAJOR_SCALE             = new boolean [] { true,    false,  true,   false,  true,   true,   false,  true,   false,  true,   false,  true    };
    private static boolean[] PENTATONIC_MINOR_SCALE  = new boolean [] { true,    false,  true,   false,  true,   true,   false,  true,   false,  true,   false,  true    };
    private static boolean[] DIMINISHED_SCALE        = new boolean [] { true,    false,  false,  true,   false,  false,  true,   false,  false,  true,   false,  false   };

    public static boolean[] getScaleByNumber(int scaleNumber) {
        switch (scaleNumber) {
            case 0:
                return CHROMATIC_SCALE;
            case 1:
                return MAJOR_SCALE;
            case 2:
                return PENTATONIC_MINOR_SCALE;
            case 3:
                return DIMINISHED_SCALE;
            default:
                return null;
        }
    }
}
