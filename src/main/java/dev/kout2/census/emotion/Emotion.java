package dev.kout2.census.emotion;

/**
 * Eight core emotions — a pragmatic reduction of the OCC model's 22 to the
 * ones that read clearly in gameplay. Each carries its basis vector in
 * PAD space (Pleasure, Arousal, Dominance), used to nudge a mob's longer-term
 * {@link PADMood} when the emotion fires.
 */
public enum Emotion {
    JOY(0.8f, 0.3f, 0.2f),
    DISTRESS(-0.8f, 0.2f, -0.3f),
    HOPE(0.4f, 0.3f, 0.1f),
    FEAR(-0.6f, 0.8f, -0.7f),
    PRIDE(0.6f, 0.3f, 0.6f),
    SHAME(-0.6f, 0.1f, -0.6f),
    ANGER(-0.5f, 0.8f, 0.4f),
    GRATITUDE(0.6f, 0.2f, -0.1f);

    public static final int COUNT = values().length;

    private final float pleasure;
    private final float arousal;
    private final float dominance;

    Emotion(float pleasure, float arousal, float dominance) {
        this.pleasure = pleasure;
        this.arousal = arousal;
        this.dominance = dominance;
    }

    public float pleasure() {
        return pleasure;
    }

    public float arousal() {
        return arousal;
    }

    public float dominance() {
        return dominance;
    }

    public String lowerName() {
        return name().toLowerCase();
    }
}
