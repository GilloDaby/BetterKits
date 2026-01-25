package com.gillodaby.betterkits.settings;

import java.util.ArrayList;
import java.util.List;

public class KitSettings {

    private long cooldownSeconds;
    private boolean allowKitStacking;
    private boolean overlapOtherKits;
    private boolean invDeletion;
    private List<String> allowedWorlds;

    public KitSettings() {
        this.cooldownSeconds = 0L;
        this.allowKitStacking = true;
        this.overlapOtherKits = true;
        this.invDeletion = false;
        this.allowedWorlds = new ArrayList<>();
    }

    public KitSettings(long cooldownSeconds,
                       boolean allowKitStacking,
                       boolean overlapOtherKits,
                       boolean invDeletion,
                       List<String> allowedWorlds) {
        this.cooldownSeconds = 0L;
        this.allowKitStacking = true;
        this.overlapOtherKits = true;
        this.invDeletion = false;
        this.allowedWorlds = new ArrayList<>();
        this.cooldownSeconds = cooldownSeconds;
        this.allowKitStacking = allowKitStacking;
        this.overlapOtherKits = overlapOtherKits;
        this.invDeletion = invDeletion;
        this.allowedWorlds = allowedWorlds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public void setAllowKitStacking(boolean allowKitStacking) {
        this.allowKitStacking = allowKitStacking;
    }

    public void setOverlapOtherKits(boolean overlapOtherKits) {
        this.overlapOtherKits = overlapOtherKits;
    }

    public void setInvDeletion(boolean invDeletion) {
        this.invDeletion = invDeletion;
    }

    public void setAllowedWorlds(List<String> allowedWorlds) {
        this.allowedWorlds = allowedWorlds;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isAllowKitStacking() {
        return allowKitStacking;
    }

    public boolean isOverlapOtherKits() {
        return overlapOtherKits;
    }

    public boolean isInvDeletion() {
        return invDeletion;
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }
}
