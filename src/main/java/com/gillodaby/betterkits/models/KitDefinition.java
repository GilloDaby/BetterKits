package com.gillodaby.betterkits.models;

import java.util.ArrayList;
import java.util.List;

public class KitDefinition {

    private String name;
    private String displayName;
    private long cooldownSeconds;
    private List<KitItem> items;
    private List<KitItem> armor;
    private boolean allowKitStacking;

    public KitDefinition() {
        this.items = new ArrayList<>();
        this.armor = new ArrayList<>();
        this.allowKitStacking = true;
    }

    public KitDefinition(String name, List<KitItem> items, List<KitItem> armor) {
        this(name, name, 0L, items, armor, true);
    }

    public KitDefinition(String name, String displayName, long cooldownSeconds, List<KitItem> items, List<KitItem> armor) {
        this(name, displayName, cooldownSeconds, items, armor, true);
    }

    public KitDefinition(String name,
                         String displayName,
                         long cooldownSeconds,
                         List<KitItem> items,
                         List<KitItem> armor,
                         boolean allowKitStacking) {
        this.items = new ArrayList<>();
        this.armor = new ArrayList<>();
        this.allowKitStacking = true;
        this.name = name;
        this.displayName = displayName;
        this.cooldownSeconds = cooldownSeconds;
        this.items = items;
        this.armor = armor;
        this.allowKitStacking = allowKitStacking;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<KitItem> getItems() {
        return items;
    }

    public List<KitItem> getArmor() {
        return armor;
    }

    public boolean isAllowKitStacking() {
        return allowKitStacking;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public void setItems(List<KitItem> items) {
        this.items = items;
    }

    public void setArmor(List<KitItem> armor) {
        this.armor = armor;
    }

    public void setAllowKitStacking(boolean allowKitStacking) {
        this.allowKitStacking = allowKitStacking;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KitDefinition that)) {
            return false;
        }
        if (!that.canEqual(this)) {
            return false;
        }
        if (cooldownSeconds != that.cooldownSeconds) {
            return false;
        }
        if (allowKitStacking != that.allowKitStacking) {
            return false;
        }
        if (name == null ? that.name != null : !name.equals(that.name)) {
            return false;
        }
        if (displayName == null ? that.displayName != null : !displayName.equals(that.displayName)) {
            return false;
        }
        if (items == null ? that.items != null : !items.equals(that.items)) {
            return false;
        }
        return armor == null ? that.armor == null : armor.equals(that.armor);
    }

    protected boolean canEqual(Object other) {
        return other instanceof KitDefinition;
    }

    @Override
    public int hashCode() {
        int result = 1;
        long cooldown = cooldownSeconds;
        result = result * 59 + (int) (cooldown ^ (cooldown >>> 32));
        result = result * 59 + (allowKitStacking ? 79 : 97);
        result = result * 59 + (name == null ? 43 : name.hashCode());
        result = result * 59 + (displayName == null ? 43 : displayName.hashCode());
        result = result * 59 + (items == null ? 43 : items.hashCode());
        result = result * 59 + (armor == null ? 43 : armor.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "KitDefinition(name=" + name + ", displayName=" + displayName + ", cooldownSeconds="
            + cooldownSeconds + ", items=" + items + ", armor=" + armor + ", allowKitStacking="
            + allowKitStacking + ")";
    }
}
