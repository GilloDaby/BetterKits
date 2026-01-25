package com.gillodaby.betterkits.models;

public class KitItem {

    private String id;
    private int quantity;

    public KitItem() {
    }

    public KitItem(String id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KitItem that)) {
            return false;
        }
        if (quantity != that.quantity) {
            return false;
        }
        if (id == null) {
            return that.id == null;
        }
        return id.equals(that.id);
    }

    protected boolean canEqual(Object other) {
        return other instanceof KitItem;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + quantity;
        result = result * 59 + (id == null ? 43 : id.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "KitItem(id=" + id + ", quantity=" + quantity + ")";
    }
}
