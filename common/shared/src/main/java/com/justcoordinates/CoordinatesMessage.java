package com.justcoordinates;

public final class CoordinatesMessage {
    private CoordinatesMessage() {
    }

    /**
     * Builds the chat line for sharing coordinates. Deliberately not translated: the message is
     * sent to other players, whose language may differ from the sender's.
     */
    public static String format(int x, int y, int z) {
        return "X: " + x + ", Y: " + y + ", Z: " + z;
    }
}
