package me.mss1r.siegeworks.client.pose;

public final class SiegePoseRenderContext {
    private static final ThreadLocal<Integer> FIRST_PERSON_HAND_DEPTH = ThreadLocal.withInitial(() -> 0);

    private SiegePoseRenderContext() {
    }

    public static void enterFirstPersonHand() {
        FIRST_PERSON_HAND_DEPTH.set(FIRST_PERSON_HAND_DEPTH.get() + 1);
    }

    public static void exitFirstPersonHand() {
        int depth = FIRST_PERSON_HAND_DEPTH.get() - 1;
        if (depth <= 0) {
            FIRST_PERSON_HAND_DEPTH.remove();
        } else {
            FIRST_PERSON_HAND_DEPTH.set(depth);
        }
    }

    public static boolean isRenderingFirstPersonHand() {
        return FIRST_PERSON_HAND_DEPTH.get() > 0;
    }
}
