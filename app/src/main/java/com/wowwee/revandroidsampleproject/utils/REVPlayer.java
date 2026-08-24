package com.wowwee.revandroidsampleproject.utils;

import androidx.annotation.Nullable;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;

public class REVPlayer {

	// Singleton
	private static REVPlayer instance = null;

	@Nullable
	private REVRobot playerRev = null;
	private boolean simulatorMode = false;
	private String simulatorName = "Simulator";

	//================================================================================
    // Singleton
    //================================================================================
	
	public static REVPlayer getInstance(){
		if (instance == null) {
			instance = new REVPlayer();
		}
		return instance;
	}
	
	//================================================================================
    // Constructor
    //================================================================================
	
	public REVPlayer() {
		super();
	}
	
	//================================================================================
    // Setter / Getter
    //================================================================================
	
	public void setPlayerRev(@Nullable REVRobot rev) {
		playerRev = rev;
	}
	
	public REVRobot getPlayerRev() {
		return playerRev;
	}

	public void setSimulatorMode(boolean simulatorMode) {
		this.simulatorMode = simulatorMode;
		if (simulatorMode) {
			this.playerRev = null;
		}
	}

	public boolean isSimulatorMode() {
		return simulatorMode;
	}

	public void setSimulatorName(String simulatorName) {
		if (simulatorName != null && !simulatorName.trim().isEmpty()) {
			this.simulatorName = simulatorName;
		}
	}

	public String getSimulatorName() {
		return simulatorName;
	}
}
