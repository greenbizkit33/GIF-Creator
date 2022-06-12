package com.nathanhaze.gifcreator.event;

public class ImageSelectedEvent {

    public boolean hasSelections;

    public ImageSelectedEvent(boolean hasSelections) {
        this.hasSelections = hasSelections;
    }
}
