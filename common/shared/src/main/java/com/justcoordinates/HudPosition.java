package com.justcoordinates;

public enum HudPosition {
    TOP_LEFT("top_left", Horizontal.LEFT, true),
    TOP_CENTER("top_center", Horizontal.CENTER, true),
    TOP_RIGHT("top_right", Horizontal.RIGHT, true),
    BOTTOM_LEFT("bottom_left", Horizontal.LEFT, false),
    BOTTOM_CENTER("bottom_center", Horizontal.CENTER, false),
    BOTTOM_RIGHT("bottom_right", Horizontal.RIGHT, false);

    public static final HudPosition DEFAULT = TOP_LEFT;

    // Keeps the HUD clear of the hotbar and experience bar when bottom-centered.
    private static final int BOTTOM_CENTER_LIFT = 40;

    private enum Horizontal { LEFT, CENTER, RIGHT }

    private final String serializedName;
    private final Horizontal horizontal;
    private final boolean top;

    HudPosition(String serializedName, Horizontal horizontal, boolean top) {
        this.serializedName = serializedName;
        this.horizontal = horizontal;
        this.top = top;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String getTranslationKey() {
        return "justcoordinates.position." + serializedName;
    }

    public static HudPosition fromSerializedName(String name) {
        for (HudPosition position : values()) {
            if (position.serializedName.equals(name)) {
                return position;
            }
        }
        return null;
    }

    public HudPosition next() {
        HudPosition[] positions = values();
        return positions[(ordinal() + 1) % positions.length];
    }

    public int resolveX(int screenWidth, int hudWidth, int margin) {
        switch (horizontal) {
            case CENTER:
                return (screenWidth - hudWidth) / 2;
            case RIGHT:
                return screenWidth - margin - hudWidth;
            default:
                return margin;
        }
    }

    public int resolveY(int screenHeight, int hudHeight, int margin) {
        if (top) {
            return margin;
        }
        if (this == BOTTOM_CENTER) {
            return screenHeight - BOTTOM_CENTER_LIFT - hudHeight;
        }
        return screenHeight - margin - hudHeight;
    }
}
