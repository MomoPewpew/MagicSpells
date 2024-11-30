package com.nisovin.magicspells.events;

public interface IDippGenBypass {
    default boolean isBypassDippGen() {
        return getBypassDippGen();
    }

    default void setBypassDippGen(boolean bypass) {
        setBypassDippGenField(bypass);
    }

    // Abstract methods for the implementing class to handle its own field storage
    boolean getBypassDippGen();
    void setBypassDippGenField(boolean bypass);
}
