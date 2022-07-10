package com.nathanhaze.gifcreator.event

public data class ProgressUpdateEvent(val message: String, val currentMilli: Int, val creatingFrame: Boolean, val addingFrames: Boolean)
