package com.zarkonnen.airships;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LaunchSettings {
	public static boolean forceSystemCursor;
	public static boolean useCustomWindow;
	public static boolean customWindowFullscreen;
	public static boolean customWindowFullscreenWindow;
	public static boolean customWindowBorderless;
	public static int customWindowX, customWindowY, customWindowW = 800, customWindowH = 600;
	public static boolean customWindowUseSimpleGraphics;
	public static String customMultiplayerServerAddress;
	public static boolean forceCustomMultiplayerServer;
	public static String customDataDirectoryLocation;
	public static int maxGIFSizeMB = 15;
	public static String customGIFSaveDirectoryLocation;
	public static int maxNetworkSendBytes = 50000;
	public static int maxPingUnacknowledgedBytes = 5000;
	public static int tooMuchNetworkDelayStrikes = 3;
	public static int measureNetworkDelayEveryMilliseconds = 3000;
	public static int minimumLagReconnectInterval = 7000;
	public static int tooMuchNetworkDelayMilliseconds = 3000;
	public static int tooMuchNetworkDelayMillisecondsOnReconnect = 6000;
	public static int reconnectAttemptIntervalMilliseconds = 5000;
	public static int maxReconnectAttempts = 10;
	public static int crashReconnectBackoffMilliseconds = 1000;
	public static int connectTimeout = 15000;
	public static int reconnectTimeout = 5000;
	public static int maxConnectAttempts = 3;
	public static int connectAttemptIntervalMilliseconds = 15000;
	public static boolean allowMacSteamIntegration;
	public static boolean recordEntireCombatState = false;
	public static int maxResumeAttempts = 3;
	public static int maxParticles = 4000;
	public static int maxSoundsPerFrame = 8;
	public static int maxRecordings = 100;
	public static int targetFPS = 60;
	
	static {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(AGame.getStaticGameDirectory(), "launch_settings.json")), "UTF-8"));
			JSONObject cfg = new JSONObject(new JSONTokener(br));
			forceSystemCursor = cfg.optBoolean("forceSystemCursor", false);
			useCustomWindow = cfg.optBoolean("useCustomWindow", false);
			customWindowFullscreenWindow = cfg.optBoolean("customWindowFullscreenWindow", false);
			customWindowFullscreen = cfg.optBoolean("customWindowFullscreen", false) && !customWindowFullscreenWindow;
			customWindowBorderless = cfg.optBoolean("customWindowBorderless", false);
			customWindowX = cfg.optInt("customWindowX", 0);
			customWindowY = cfg.optInt("customWindowY", 0);
			customWindowW = cfg.optInt("customWindowW", 800);
			customWindowH = cfg.optInt("customWindowH", 600);
			customWindowUseSimpleGraphics = cfg.optBoolean("customWindowUseSimpleGraphics", false);
			customMultiplayerServerAddress = cfg.optString("customMultiplayerServerAddress", null);
			forceCustomMultiplayerServer = cfg.optBoolean("forceCustomMultiplayerServer", false);
			customDataDirectoryLocation = cfg.optString("customDataDirectoryLocation", null);
			maxGIFSizeMB = StrictMath.max(2, cfg.optInt("maxGIFSizeMB", 15));
			customGIFSaveDirectoryLocation = cfg.optString("customGIFSaveDirectoryLocation", null);
			maxNetworkSendBytes = cfg.optInt("maxNetworkSendBytes", 50000);
			maxPingUnacknowledgedBytes = cfg.optInt("maxPingUnacknowledgedBytes", 5000);
			tooMuchNetworkDelayStrikes = cfg.optInt("tooMuchNetworkDelayStrikes", 3);
			measureNetworkDelayEveryMilliseconds = cfg.optInt("measureNetworkDelayEveryMilliseconds", 3000);
			tooMuchNetworkDelayMilliseconds = Math.max(500, cfg.optInt("tooMuchNetworkDelayMilliseconds", 3000));
			tooMuchNetworkDelayMillisecondsOnReconnect = Math.max(500, cfg.optInt("tooMuchNetworkDelayMillisecondsOnReconnect", 3000));
			allowMacSteamIntegration = cfg.optBoolean("allowMacSteamIntegration", false);
			reconnectAttemptIntervalMilliseconds = cfg.optInt("reconnectAttemptIntervalMilliseconds", 5000);
			maxReconnectAttempts = cfg.optInt("maxReconnectAttempts", 5);
			minimumLagReconnectInterval = cfg.optInt("minimumLagReconnectInterval", 7000);
			crashReconnectBackoffMilliseconds = cfg.optInt("crashReconnectBackoffMilliseconds", 1000);
			recordEntireCombatState = cfg.optBoolean("recordEntireCombatState", false);
			connectTimeout = cfg.optInt("connectTimeout", 15000);
			reconnectTimeout = cfg.optInt("reconnectTimeout", 5000);
			maxResumeAttempts = cfg.optInt("maxResumeAttempts", 3);
			maxConnectAttempts = cfg.optInt("maxConnectAttempts", 3);
			connectAttemptIntervalMilliseconds = cfg.optInt("connectAttemptIntervalMilliseconds", 3);
			maxParticles = cfg.optInt("maxParticles", 4000);
			maxRecordings = cfg.optInt("maxRecordings", 100);
			maxSoundsPerFrame = cfg.optInt("maxSoundsPerFrame", 8);
			targetFPS = Math.max(1, cfg.optInt("targetFPS", 60));
			
			// qqDPS
			if (AGame.randomDataDir()) {
				customDataDirectoryLocation = "/home/zar/Desktop/a" + AGame.ANIM_R.nextInt();
				forceSystemCursor = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { br.close(); } catch (Exception e) {}
		}
	}
}
