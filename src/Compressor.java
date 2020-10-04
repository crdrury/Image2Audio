public class Compressor {
    boolean bypass;
    float threshold;
    float ratio;
    float makeUpGain;
    double attack;
    double hold;
    double release;

    public float compTimer = -1;

    public Compressor(boolean bypass, float threshold, float ratio, float makeUpGain, double attack, double hold, double release) {
        this.bypass = bypass;
        this.threshold = threshold;
        this.ratio = ratio;
        this.makeUpGain = makeUpGain;
        this.attack = attack;
        this.hold = hold;
        this.release = release;
    }

    public double compress(double waveValue) {
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
        compTimer += 1 / MusicalConstants.SAMPLE_RATE;
        waveValue += makeUpGain;

        return waveValue;
    }
}