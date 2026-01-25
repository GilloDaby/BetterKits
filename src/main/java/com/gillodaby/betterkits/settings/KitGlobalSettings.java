package com.gillodaby.betterkits.settings;

public class KitGlobalSettings {

    private long menuCommandCooldownSeconds;
    private boolean showRandomKitButton;
    private double backgroundDefaultOpacity;

    public KitGlobalSettings() {
        this.menuCommandCooldownSeconds = 0L;
        this.showRandomKitButton = true;
        this.backgroundDefaultOpacity = 0.25d;
    }

    public long getMenuCommandCooldownSeconds() {
        return Math.max(0L, menuCommandCooldownSeconds);
    }

    public void setMenuCommandCooldownSeconds(long menuCommandCooldownSeconds) {
        this.menuCommandCooldownSeconds = Math.max(0L, menuCommandCooldownSeconds);
    }

    public void setShowRandomKitButton(boolean showRandomKitButton) {
        this.showRandomKitButton = showRandomKitButton;
    }

    public boolean isShowRandomKitButton() {
        return showRandomKitButton;
    }

    public void setBackgroundDefaultOpacity(double backgroundDefaultOpacity) {
        this.backgroundDefaultOpacity = backgroundDefaultOpacity;
    }

    public double getBackgroundDefaultOpacity() {
        return backgroundDefaultOpacity;
    }
}
