package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.Lang.currentLocale;
import com.zarkonnen.catengine.Draw;
import com.zarkonnen.catengine.ExceptionHandler;
import com.zarkonnen.catengine.Fount;
import com.zarkonnen.catengine.Frame;
import com.zarkonnen.catengine.Game;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.Loop;
import com.zarkonnen.catengine.MusicCallback;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Rect;
import com.zarkonnen.catengine.util.ScreenMode;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

public class AirshipGame implements Game, ExceptionHandler, Lwjgl3Engine.ReportHandler {
	public Screen s = /*ew TentacleTest();*new AnimationViewer(this);*/new ResChooserScreen(this);
	
	private final Img close = new Img("ui", 144, 416, 16, 16, false);
	
	public boolean showFrameCosts = false;
	public final Hooks hs = new Hooks();
	private final MyDraw.State drawState = new MyDraw.State();
	private boolean first = true;
	public boolean exiting = false;
	public int delayExitForFlush = 0;
	public volatile int delayExitForNetwork = 0;
	public Pt cursor = new Pt(0, 0);
	public static final AirshipsPrefs PREFS = new AirshipsPrefs();
	public static final Clr ERR_BG_TINT = new Clr(0, 0, 0, 63);
	public String error = null;
	public int backgroundPlayTimeLeft = 0;
	public String helpTopic = null;
	public String helpText = null;
	public Runnable hideHelpRunnable = null;
	public ScrollBar helpTextScroll = new ScrollBar(); public IntRect helpTextScrollR = new IntRect();
	public ScrollBar helpTopicsScroll = new ScrollBar(); public IntRect helpTopicsScrollR = new IntRect();
	public Integration integration = null;
	public boolean checkedMPServer = false;
	public String chosenMultiplayerServer = null;
	public LinkedList<Integer> frameCosts = new LinkedList<Integer>();
	public static ScreenMode scaleFrom;
	private static Image scaleBuffer;
	private long tickEnd = 0;
	public LinkedList<Pair<String, Long>> recentScreens = new LinkedList<Pair<String, Long>>();
	
	public HashMap<String, Pt> nextHelpHighlights = new HashMap<String, Pt>();
	public HashMap<String, Pt> currentHelpHighlights = new HashMap<String, Pt>();
	public HashMap<String, IntRect> highlights = new HashMap<String, IntRect>();
	
	public Pt highlitStart(String key) { return currentHelpHighlights.get(key); }
	public void highlight(String key, Pt p) { nextHelpHighlights.put(key, p); }
	
	static void setScaleFrom(ScreenMode sm) {
		scaleFrom = sm;
	}
	
	public TestRunner testRunner;
	
	int time;
	
	public long dataChecksum = 0;
	public long expectedDataChecksum = 0;
	
	public boolean hasCorrectChecksum() {
		for (Expansion ex : Expansion.installeds()) {
			if (!ex.hasCorrectChecksum()) { return false; }
		}
		return dataChecksum == expectedDataChecksum;
	}
	
	public long msPlayed = 0;
	
	public String currentMusic = null;
	public int fading = 0;
	public ArrayList<String> currentMusicOptions = new ArrayList<String>();
	
	public int lastMs = 0;
	public volatile boolean hasRendered = false;
	
	private final LinkedList<Integer> tickCosts = new LinkedList<Integer>();
	private final LinkedList<Integer> renderCosts = new LinkedList<Integer>();
	
	public Screen postInputScreen;
	public Screen prevScreen;
	
	public int avgTickCost() {
		int sum = 0;
		if (tickCosts.isEmpty()) { return 0; }
		for (int c : tickCosts) {
			sum += c;
		}
		return sum / tickCosts.size();
	}
	
	public int avgRenderCost() {
		int sum = 0;
		if (renderCosts.isEmpty()) { return 0; }
		for (int c : renderCosts) {
			sum += c;
		}
		return sum / renderCosts.size();
	}
	
	public Pt lastCursorPt;
	public boolean mouseMoved = false;
	
	byte[] emergencyReserve = new byte[100000];
		
	// Settings
	public boolean phoneHomeAsked = true;
	public boolean phoneHomeWithErrors = true;
	public double volume = 0.5;
	public double musicVolume = 0.5;
	private boolean loggedZeroMusicVolume = false;
	private boolean loggedNoMusicOptions = false;
	public static int scrollSpeed = 12;
	public static int zoomSpeed = 3; // 1.0zoomSpeed
	public static int mouseWheelZoomSpeed = 3; // 1.000mouseWheelZoomSpeed
	//public static boolean langReported = false;
	public boolean prevUserFirstTimev5 = false;
	public static boolean multiplayerChatOverlay = false;
	public static MetaLobbyOverlay metaLobbyOverlay = null;
	public boolean mpChatOverlayActive = false;
	public int msSinceIntegration = 0;
	public int tooltipDelay = 16;
	public static boolean steamDeckSmallGUI = false;
	
	// Analytics
	public boolean firstLaunch = false;
	
	public String wmSize = "SMALLISH";
	public String difficulty = "EASY";
	public String seaLevelSettingName = "MIXED";
	public String monsterityName = "DEFAULT";
	public String techSpeedName = "NORMAL";
	public String heroFrequencyName = "DEFAULT";
	public String incidentFrequencyName = "DEFAULT";
	public String heroName = "?";
	public int startingTechTier = 0;
	public EnumSet<ConquestToggle> conquestToggles = EnumSet.allOf(ConquestToggle.class);
	public boolean strategicHelp;
	public boolean strategicHelpHeroesAndVillains;
	public boolean strategicRapidCommands;
	public boolean strategicAllowDirectControl;
		
	public boolean swallowExceptions = false;
	public boolean disableAllModsOnStartup = false;
	
	public GUIScale guiScaleOverride = null;
	public GUIScale currentGUIScale = GUIScale.SMALL;
	
	public String overlayServer;
	
	public Client client;
	public Client lanClient;
	public Server lanServer;
	public final ArrayList<ChatMsg> chat = new ArrayList<ChatMsg>();
	public final HashMap<Integer, PlayerInfo> players = new HashMap<Integer, PlayerInfo>();
	public final ArrayList<ChannelInfo> channels = new ArrayList<ChannelInfo>();
	public String nonceFromServer;
	public boolean helloSent = false;
	public boolean idReceived = false;
	public boolean channelsLoaded = false;
	public int myID = 0;
	public String myName;
	public JSONObject myArmsJSON;
	public int lanID = 0;
	public String lanName;
	public JSONObject lanArmsJSON;
	private final LinkedList<JSONObject> preProcessedMessages = new LinkedList<JSONObject>();
	
	public static boolean openURL(String url, ScreenMode sm) {
		try {
			Desktop.getDesktop().browse(new URI(url));
			if (sm == null || sm.fullscreen) {
				Analytics.flushSync(new Runnable() {
					@Override
					public void run() {
						AirshipGame.instance.startExit();
					}
				});
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public void startExit() {
		exiting = true;
		if (client != null) { disconnectClient(); }
		s = new ExitScreen(this);
	}
	
	public void disconnectClient() {
		if (client != null) {
			client.close();
			client = null;
			players.clear();
			channels.clear();
			myID = 0;
			idReceived = false;
			helloSent = false;
			chat.clear();
		}
	}
	
	public boolean isConnected() {
		return lanClient != null ? lanClient.isConnectedRaw() : client != null && client.isConnectedRaw();
	}
	
	public boolean isDisconnected() {
		return lanClient != null ? lanClient.isDisconnected() : client != null && client.isDisconnected();
	}
	
	public boolean completelyReconnectClient() {
		if (lanClient != null) {
			lanClient = null;
			return false;
		}
		if (client == null) { return false; }
		if (players.containsKey(myID)) {
			chat.add(new ChatMsg(players.get(myID), _t("xxx_RECONNECTED_xxx", players.get(myID).name), DateTime.now()));
		}
		players.clear();
		channels.clear();
		helloSent = false;
		idReceived = false;
		channelsLoaded = false;
		myID = 0;
		client = new Client(myName, client.serverIP, this);
		System.out.println("crc done");
		return true;
	}
	
	public int playerID() {
		if (lanClient != null) {
			return lanID;
		} else {
			return myID;
		}
	}
	
	public String playerName() {
		if (lanClient != null) {
			return lanName;
		} else {
			setMyNameAndArms();
			return myName;
		}
	}
	
	public JSONObject playerArms() {
		if (lanClient != null) {
			return lanArmsJSON;
		} else {
			setMyNameAndArms();
			return myArmsJSON;
		}
	}
	
	public JSONObject pollMessage() {
		if (lanClient != null) {
			return lanClient.pollMessageRaw();
		} else {
			return preProcessedMessages.pollFirst();
		}
	}
	
	public void sendMessage(JSONObject msg) {
		if (lanClient != null) {
			lanClient.sendMessageRaw(msg);
		} else if (client != null) {
			client.sendMessageRaw(msg);
		} else {
			reportError(_t("Unable_to_send_no_active_network_connection"), null, null, false);
		}
	}
	
	public boolean sendMessageWithSizeCheck(JSONObject msg) {
		if (lanClient != null) {
			return lanClient.sendMessageRawWithSizeCheck(msg);
		} else if (client != null) {
			return client.sendMessageRawWithSizeCheck(msg);
		} else {
			reportError(_t("Unable_to_send_no_active_network_connection"), null, null, false);
			return true;
		}
	}
	
	private boolean processClient() {
		if (client == null) { return false; }
		JSONObject msg = client.pollMessageRaw();
		
		if (!helloSent && idReceived) {
			setMyNameAndArms();
			client.sendMessageRaw(msg("hello").put("info", new JSONObject().put("name", myName).put("id", myID).put("arms", myArmsJSON)));
			helloSent = true;
		}
		
		if (msg != null) {
			if (msg.getString("type").equals("serverReject")) {
				showError(_t("server_rejected_" + msg.getString("reason")));
				disconnectClient();
				s = new MainMenu(this, MainMenu.Submenu.MAIN);
				return false;
			}
			if (msg.get("type").equals("assignID")) {
				myID = msg.getInt("playerID");
				System.out.println("myID is " + myID);
				idReceived = true;
			}
			if (msg.get("type").equals("addPlayer")) {
				processAddPlayer(msg);
			}
			if (msg.get("type").equals("removePlayer")) {
				processRemovePlayer(msg);
			}
			if (msg.get("type").equals("channelList")) {
				processChannelList(msg);
			}
			if (msg.getString("type").equals("frame")) {
				processChatFrame(msg);
			}
			preProcessedMessages.add(msg);
			//System.out.println("preprocs " + preProcessedMessages.size());
			return true;
		}
		
		return false;
	}
	
	private void processChannelList(JSONObject msg) {
		nonceFromServer = msg.optString("nonce", "?");
		JSONArray l = msg.getJSONArray("channels");
		channels.clear();
		for (int i = 0; i < l.length(); i++) {
			JSONObject o = l.getJSONObject(i);
			int id = o.getInt("id");
			if (id != 0 && o.getInt("players") > 0) {
				channels.add(new ChannelInfo(id, o.getJSONObject("info"), o.getInt("players"), o.optBoolean("sealed", false), o.optString("salt", null)));
			}
		}
	}
	
	private void processAddPlayer(JSONObject msg) {
		JSONObject info = msg.getJSONObject("info");
		if (!players.containsKey(info.getInt("id"))) {
			PlayerInfo pi = new PlayerInfo(info.getInt("id"), info.getString("name"));
			pi.armsJSON = info.optJSONObject("arms");
			if (pi.armsJSON == null) {
				pi.armsJSON = new CoatOfArms().toJSON();
			}
			players.put(info.getInt("id"), pi);
			chat.add(new ChatMsg(pi, _t("xxx_JOINED_xxx", pi.name), DateTime.now()));
		}
	}
	
	private void processRemovePlayer(JSONObject msg) {
		int id = msg.getInt("id");
		if (players.containsKey(id)) {
			chat.add(new ChatMsg(players.get(id), _t("xxx_LEFT_xxx", players.get(id).name), DateTime.now()));
		}
		players.remove(id);
	}
	
	private void processChatFrame(JSONObject msg) {
		JSONArray frameMessages = msg.getJSONArray("messages");
		for (int i = 0; i < frameMessages.length(); i++) {
			JSONObject fm = frameMessages.getJSONObject(i);
			if (fm.getString("type").equals("createGame")) {
				PlayerInfo pi = players.get(fm.getInt("id"));
				if (pi != null) {
					chat.add(new ChatMsg(pi, _t("xxx_NEW_GAME_xxx", pi.name, fm.getString("name")), DateTime.now()));
				}
			}
			if (fm.getString("type").equals("globalChat")) {
				PlayerInfo pi = players.get(fm.getInt("id"));
				if (pi != null) {
					chat.add(new ChatMsg(pi, /* pi.tint + */pi.name + ": " + fm.getString("text"), new DateTime(fm.getLong("t"), DateTimeZone.UTC)));
				} else if (fm.has("historyInfo") && fm.getJSONObject("historyInfo").has("name") && fm.getJSONObject("historyInfo").has("arms")) {
					JSONObject info = fm.getJSONObject("historyInfo");
					pi = new PlayerInfo(info.getInt("id"), info.getString("name"));
					pi.armsJSON = info.getJSONObject("arms");
					chat.add(new ChatMsg(pi, /* pi.tint + */pi.name + ": " + fm.getString("text"), new DateTime(fm.getLong("t"), DateTimeZone.UTC)));
				}
			}
		}
	}
	
	static AirshipGame instance;
	static void report(String text) {
		instance.reportError(text, null, null, false, true);
	}
	
	public void initIntegration() {
		if (integration == null) {
			integration = new Integration(
				new WebIntegrationBackend(
						"https://airships.zarkonnen.com/api/",
						"https://airships.zarkonnen.com/static/media/",
						"https://airships.zarkonnen.com",
						"https://airships.zarkonnen.com/multiplayer_calendar"
				));
		}
	}
	
	public final void loadSettings() {
		try {
			firstLaunch = PREFS.getDouble("volume", 666) > 111;
			Analytics.pseudonym = PREFS.get("pseudonym", "?");
			prevUserFirstTimev5 = PREFS.getDouble("volume", -100) > -1 && !PREFS.getBoolean("usedv5", false);
			phoneHomeAsked = true;//PREFS.getBoolean("phoneHomeAsked", false);
			phoneHomeWithErrors = PREFS.getBoolean("phoneHomeWithErrors", true);
			volume = PREFS.getDouble("volume", 1.0);
			tooltipDelay = PREFS.getInt("tooltipDelay", 10);
			steamDeckSmallGUI = PREFS.getBoolean("steamDeckSmallGUI", false);
			strategicHelp = PREFS.getBoolean("strategicHelp", true);
			strategicHelpHeroesAndVillains = PREFS.getBoolean("strategicHelpHeroesAndVillains", true);
			strategicRapidCommands = PREFS.getBoolean("strategicRapidCommands", false);
			strategicAllowDirectControl = PREFS.getBoolean("strategicAllowDirectControl", true);
			musicVolume = PREFS.getDouble("musicVolume", 0.5);
			if (AGame.randomDataDir()) {
				musicVolume = 0;
				volume = 0;
			}
			scrollSpeed = PREFS.getInt("scrollSpeed", 12);
			zoomSpeed = PREFS.getInt("zoomSpeed", 3);
			mouseWheelZoomSpeed = PREFS.getInt("mouseWheelZoomSpeed", 3);
			String lang = PREFS.get("language", "-----");
			if (!lang.equals("-----")) {
				Lang.setCurrentLocale(Locale.forLanguageTag(lang));
			}
			Appearance.useSimpleGraphics = PREFS.getBoolean("useSimpleGraphics", false);
			Appearance.useLighting = PREFS.getBoolean("useLighting", true);
			for (SimplePref sp : SimplePref.values()) {
				sp.load();
			}
			wmSize = PREFS.get("wmSize", "SMALLISH");
			difficulty = PREFS.get("difficulty", "EASY");
			
			seaLevelSettingName = PREFS.get("seaLevelSettingName", "MIXED");
			monsterityName = PREFS.get("monsterityName", "DEFAULT");
			heroFrequencyName = PREFS.get("heroFrequencyName", "DEFAULT");
			incidentFrequencyName = PREFS.get("incidentFrequencyName", "DEFAULT");
			techSpeedName = PREFS.get("techSpeedName", "NORMAL");
			startingTechTier = PREFS.getInt("startingTechTier", 0);
			conquestToggles.clear();
			for (ConquestToggle ct : ConquestToggle.values()) {
				if (PREFS.getBoolean("ct_" + ct.name(), true)) {
					conquestToggles.add(ct);
				}
			}
			chosenMultiplayerServer = PREFS.get("chosenMultiplayerServer", "");
			if (chosenMultiplayerServer.isEmpty()) {
				chosenMultiplayerServer = null;
			}
			
			setMultiplayerChatOverlay(PREFS.getBoolean("multiplayerChatOverlay", false));
			String gui = PREFS.get("guiScale", "AUTO");
			if (gui.equals("AUTO")) {
				guiScaleOverride = null;
			} else {
				guiScaleOverride = GUIScale.valueOf(gui);
			}
			
			MainMenu.spidersReleased = PREFS.getBoolean("spidersReleased", false);
			MainMenu.gachakuDismissed = PREFS.getBoolean("gachakuDismissed", false);
			heroName = PREFS.get("heroName", "?");
			//langReported = PREFS.getBoolean("langReported", false);
		} catch (Exception e) {
			e.printStackTrace();
			reportError("Unable to load settings.", e, null, false);
		}

		if (Analytics.pseudonym.equals("?")) {
			Analytics.pseudonym = AGame.ANIM_R.nextLong() + "" + System.currentTimeMillis();
			try {
				PREFS.put("pseudonym", Analytics.pseudonym);
				PREFS.save();
			} catch (Exception e) {}
		}
	}
	
	public final void saveSettings() {
		try {
			PREFS.putBoolean("phoneHomeAsked", phoneHomeAsked);
			PREFS.putBoolean("phoneHomeWithErrors", phoneHomeWithErrors);
			PREFS.putDouble("volume", volume);
			PREFS.putInt("tooltipDelay", tooltipDelay);
			PREFS.putBoolean("strategicHelp", strategicHelp);
			PREFS.putBoolean("strategicHelpHeroesAndVillains", strategicHelpHeroesAndVillains);
			PREFS.putBoolean("strategicRapidCommands", strategicRapidCommands);
			PREFS.putBoolean("strategicAllowDirectControl", strategicAllowDirectControl);
			PREFS.putBoolean("steamDeckSmallGUI", steamDeckSmallGUI);
			PREFS.putDouble("musicVolume", musicVolume);
			PREFS.putInt("scrollSpeed", scrollSpeed);
			PREFS.putInt("zoomSpeed", zoomSpeed);
			PREFS.putInt("mouseWheelZoomSpeed", mouseWheelZoomSpeed);
			PREFS.putBoolean("usedv5", true);
			PREFS.put("language", Lang.currentLocale.toLanguageTag());
			PREFS.putBoolean("useSimpleGraphics", Appearance.useSimpleGraphics);
			PREFS.putBoolean("useLighting", Appearance.useLighting);
			PREFS.put("wmSize", wmSize);
			PREFS.put("difficulty", difficulty);
			PREFS.put("chosenMultiplayerServer", chosenMultiplayerServer == null ? "" : chosenMultiplayerServer);
			//PREFS.putBoolean("langReported", langReported);
			PREFS.put("seaLevelSettingName", seaLevelSettingName);
			PREFS.put("monsterityName", monsterityName);
			PREFS.put("heroFrequencyName", heroFrequencyName);
			PREFS.put("incidentFrequencyName", incidentFrequencyName);
			PREFS.put("techSpeedName", techSpeedName);
			PREFS.putInt("startingTechTier", startingTechTier);
			for (SimplePref sp : SimplePref.values()) {
				sp.save();
			}
			for (ConquestToggle ct : ConquestToggle.values()) {
				PREFS.putBoolean("ct_" + ct.name(), conquestToggles.contains(ct));
			}
			PREFS.putBoolean("multiplayerChatOverlay", multiplayerChatOverlay);
			PREFS.put("guiScale", guiScaleOverride == null ? "AUTO" : guiScaleOverride.name());
			PREFS.putBoolean("spidersReleased", MainMenu.spidersReleased);
			PREFS.putBoolean("gachakuDismissed", MainMenu.gachakuDismissed);
			PREFS.put("heroName", heroName);
			PREFS.save();
		} catch (Exception e) {
			reportError("Unable to save settings.", e, null, false);
		}
	}
	
	public static void setMultiplayerChatOverlay(boolean mco) {
		multiplayerChatOverlay = mco;
	}
	
	// End settings
	
	public AirshipGame() {
		loadSettings();
		if (AGame.isRunTests()) {
			testRunner = new TestRunner();
			volume = 0;
			musicVolume = 0;
		}
		instance = this;
		System.err.println("Init AirshipGame");
		if (Main.ON_STEAM_DECK) {
			s = new LoadingScreen(this);
		}
	}
	
	public boolean isIntegrated() {
		return integration != null;
	}
	
	public CoatOfArms getBestCOA() {
		if (integration != null && integration.getRegisteredCOAIfAvailable() != null) {
			return integration.getRegisteredCOAIfAvailable();
		}
		return CoatEditor.getMyStrategicArms();
	}
	
	public ArrayList<String> helpTopics = new ArrayList<String>();
	public HashMap<String, String> helps = new HashMap<String, String>();
	public HashMap<String, Img> helpIcons = new HashMap<String, Img>();
	public Locale helpLocale;
	public TextField helpSearchF = new TextField();
	
	private List<String> getHelpTopics() {
		if (helpSearchF.getText().trim().isEmpty()) {
			return helpTopics;
		} else {
			ArrayList<String> topics = new ArrayList<String>();
			for (String t : helpTopics) {
				String txt = helps.get(t);
				if (txt.toLowerCase(Lang.currentLocale).contains(helpSearchF.getText().trim().toLowerCase(Lang.currentLocale))) {
					topics.add(t);
				}
			}
			return topics;
		}
	}
	
	public void showHelp(String topic, Runnable hideHelpRunnable) {
		showHelpTopic(topic);
		this.hideHelpRunnable = hideHelpRunnable;
	}
	
	private void showHelpTopic(String topic) {
		if (Lang.currentLocale != helpLocale) {
			if (new File(new File(AGame.getStaticGameDirectory(), "data"), "help_" + Lang.currentLocale.toLanguageTag()).exists()) {
				helpLocale = Lang.currentLocale;
			} else {
				helpLocale = Locale.ENGLISH;
			}
		}
		if (helpTopics.isEmpty()) {
			ArrayList<File> loadBases = new ArrayList<File>();
			loadBases.add(new File(AGame.getStaticGameDirectory(), "data"));
			for (Expansion e : Expansion.enableds()) {
				loadBases.add(e.getDataDir());
			}
			for (File base : loadBases) {
				try {
					List<String> topics = FileUtils.readLines(new File(new File(base, "help_" + helpLocale.toLanguageTag()), "topics.txt"), "UTF-8");
					helpTopics.addAll(topics);
					for (String l : FileUtils.readLines(new File(new File(base, "help_" + helpLocale.toLanguageTag()), "icons.txt"), "UTF-8")) {
						String[] bits = l.split(", ");
						if (bits.length == 6) {
							helpIcons.put(bits[0], new Img(bits[1], Integer.parseInt(bits[2]), Integer.parseInt(bits[3]), Integer.parseInt(bits[4]), Integer.parseInt(bits[5]), false));
						}
						if (bits.length == 5) {
							helpIcons.put(bits[0], new Img("ui", Integer.parseInt(bits[1]), Integer.parseInt(bits[2]), Integer.parseInt(bits[3]), Integer.parseInt(bits[4]), false));
						}
					}
					for (String t : topics) {
						helps.put(t, FileUtils.readFileToString(new File(new File(base, "help_" + helpLocale.toLanguageTag()), AGame.makeFileSafe(t).replace(" ", "_") + ".txt"), "UTF-8"));
					}
				} catch (IOException e) {}
			}
		}
		helpText = helps.containsKey(topic) ? helps.get(topic) : "Unable to load help!";
		helpTopic = topic;
		helpTextScroll.offset = 0;
	}
	
	public void reportPerformance(PerfReport pr, Recording rec) {
		if (pr == null) { return; }
		//System.out.println(pr.toString());
		//System.err.println(pr.toString());
		if (phoneHomeWithErrors && pr.hasBadPerformance()) {
			String rep = pr.toString();
			if (rep != null) {
				try {
					rep += "\n\nRecording:\n" + rec.header.toJSON().toString() + "\n" + Compression.compressToString(rec.toJSON().toString());
				} catch (Exception e) {}
			}
			integration.sendFeedback(rep);
		}
	}

	public void showError(String error) {
		this.error = error;
	}
	
	public void reportError(String message, Exception e, String detail, boolean fatal) {
		reportError(message, e, detail, fatal, false);
	}
	
	public String recentScreens() {
		StringBuilder sb = new StringBuilder();
		long t = System.currentTimeMillis();
		for (Pair<String, Long> s : recentScreens) {
			sb.append(s.a).append(": ").append(t - s.b).append(",  ");
			t = s.b;
		}
		return sb.toString();
	}
	
	public void reportDebug(String message) {
		reportDebug("", message);
	}
	
	public void reportDebug(String headline, String message) {
		message += "\n\ngamedir " + AGame.getGameDirectory().getAbsolutePath() + "\n";
		message += "static " + AGame.getStaticGameDirectory().getAbsolutePath() + "\n";
		message += "playerID " + playerID() + "\n";
		message += "datetime " + new Date().toString();
		message += "\nrecent screens: " + recentScreens();
		message += "\nsimplegraphics " + Appearance.useSimpleGraphics + " lighting " + Appearance.useLighting;
		message += "\nmemory free " + Runtime.getRuntime().freeMemory() + ", total " + Runtime.getRuntime().totalMemory() + ", max " + Runtime.getRuntime().maxMemory();
		message += "\nv " + System.getProperty("java.version", "?") + ", os arch " + System.getProperty("os.arch", "?") + ", java arch " + System.getProperty("sun.arch.data.model", "?");
		try {
			integration.sendFeedback(headline, message);
		} catch (Exception e2) {
			// Ignore
		} catch (OutOfMemoryError oome) {}
	}
	
	public HashSet<String> seenErrorMessages = new HashSet<String>();
		
	public void reportError(String message, Exception e, String detail, boolean fatal, boolean silent) {
		String headline = e == null ? "" : e.toString();
		String origMsg = message;
		try {
			if (message == null) {
				if (e != null) {
					message = e.toString();
				} else {
					message = "";
				}
			} else if (e != null) {
				message = e.toString() + " " + message;
			}
		} catch (Exception e2) {
			message = "";
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			if (e != null) {
				e.printStackTrace(pw);
				pw.flush();
				message += "\n\nStack Trace:\n" + sw.toString();
			} else {
				new Throwable().printStackTrace(pw);
				pw.flush();
				message += "\n\nStack:\n" + sw.toString();
			}
		} catch (Exception e2) {
			// Nothing
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		
		// Once stack is added, check that we don't report multiple times.
		if (seenErrorMessages.contains(message)) {
			return;
		}
		seenErrorMessages.add(message);
		
		try {
			message += "\n\ngamedir " + AGame.getGameDirectory().getAbsolutePath() + "\n";
			message += "static " + AGame.getStaticGameDirectory().getAbsolutePath() + "\n";
			message += "playerID " + playerID() + "\n";
			message += "datetime " + new Date().toString() + "\n";
			message += "recent screens: " + recentScreens() + "\n";
			if (client != null) {
				message += "client inQueue " + client.inQueueSize() + " outQueue " + client.outQueueSize() + "\n";
			}
			if (lanClient != null) {
				message += "lanClient inQueue " + lanClient.inQueueSize() + " outQueue " + lanClient.outQueueSize() + "\n";
			}
			message += "v " + System.getProperty("java.version", "?") + ", os arch " + System.getProperty("os.arch", "?") + " , java arch " + System.getProperty("sun.arch.data.model", "?");
		} catch (Exception e2) {
			// Nothing
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		
		try {
			message += "\nsimplegraphics " + Appearance.useSimpleGraphics + " lighting " + Appearance.useLighting;
			message += "\nmemory free " + Runtime.getRuntime().freeMemory() + ", total " + Runtime.getRuntime().totalMemory() + ", max " + Runtime.getRuntime().maxMemory();

			if (detail != null) {
				message += "\n\nDetail:\n" + CompressAndArmour.compressAndArmour(detail);
			}
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		
		try {
			if (s instanceof ResumeScreen) {
				message += "\n\n" +  ((ResumeScreen) s).dumpInfo();
			}
			if (s instanceof UniScreen) {
				UniScreen us = (UniScreen) s;
				if (us.intent instanceof HasStrategicScreen) {
					StrategicScreen ss = ((HasStrategicScreen) us.intent).getStrategicScreen();
					message += "\nmultiplayer? " + ss.w.isMultiplayer() + " host? " + ss.w.isHost();
				}
				message += "\n\nIntent: " + us.intent.getClass().getSimpleName();
				if (us.combat != null) {
					message += "\n\nCombat:\n" + CompressAndArmour.compressAndArmour(us.combat.toJSON().toString());
				}
				if (us.standaloneEditShip != null) {
					message += "\n\nShip:\n" + CompressAndArmour.compressAndArmour(us.standaloneEditShip.toJSON(null).toString());
				}
				if (us.intent instanceof HasStrategicScreen) {
					StrategicScreen ss = ((HasStrategicScreen) us.intent).getStrategicScreen();
					message += "\nNetwork: world frame queue size is " + ss.w.frameQueueSize();
					message += "\n\nWorld:\n" + CompressAndArmour.compressAndArmour(SavedStateOutPipe.toJSON(ss.w).toString());
				}
			}
			/*if (s instanceof StrategicScreen) {
				StrategicScreen ss = (StrategicScreen) s;
				message += "\nmultiplayer? " + ss.w.isMultiplayer() + " host? " + ss.w.isHost();
				message += "\nNetwork: world frame queue size is " + ss.w.frameQueueSize();
				message += "\n\nWorld:\n" + CompressAndArmour.compressAndArmour(SavedStateOutPipe.toJSON(ss.w).toString());
			}*/
		} catch (Exception e2) {
			// Nothing
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		
		try {
			System.err.println(message);
			System.err.flush();
		} catch (Exception e2) {
			// Nothing
		} catch (OutOfMemoryError oome) {
			emergencyReserve = null;
			Runtime.getRuntime().gc();
		}
		Throwable err = null;
		
		if (testRunner != null) {
			Runtime.getRuntime().exit(1);
		}
		
		if (phoneHomeWithErrors && isIntegrated() && !integration.backend.getMultiplayerServers().isEmpty()) {
			try {
				integration.sendFeedback(headline, message);
				if (!silent) {
					if (origMsg != null) {
						showError(origMsg + " " + _t("error_log_sending"));
					} else {
						showError(_t("unknown_error_log_sending"));
					}
					if (fatal) {
						s = new MainMenu(this, MainMenu.Submenu.MAIN);
					}
				}
				return;
			} catch (Exception e2) {
				err = e2;
			} catch (OutOfMemoryError oome) {
				emergencyReserve = null;
				Runtime.getRuntime().gc();
				integration.sendFeedback(origMsg);
				err = oome;
			}
		}
		if (phoneHomeWithErrors && isIntegrated()) {
			try {
				integration.sendFeedback("Unable to send feedback due to errors (" + err + "), sorry.");
			} catch (Throwable t) { /* ignore */ }
		}
		String logPath = AGame.getLogFile().getAbsolutePath().replace("\\", "/");
		if (!silent) {
			if (origMsg != null) {
				showError(origMsg + " " + _t("please_send_error_log", logPath, "david.stark@zarkonnen.com"));
			} else {
				showError(_t("unknown_error_please_send_error_log", logPath, "david.stark@zarkonnen.com"));
			}
			if (fatal) {
				s = new MainMenu(this, MainMenu.Submenu.MAIN);
			}
		}
	}
	
	public void manageMusic(Input in) {
		if (musicVolume == 0) {
			// 诊断：音量 0 时音乐被静默跳过（音效正常但无音乐时的最常见原因）
			if (!loggedZeroMusicVolume) {
				System.err.println("Music skipped: musicVolume == 0 (check Settings music volume slider)");
				loggedZeroMusicVolume = true;
			}
			in.stopMusic();
			return;
		}
		ArrayList<String> options = s.music();
		if (options == null) {
			options = currentMusicOptions;
		} else {
			currentMusicOptions = options;
		}
		if (fading > 0) {
			fading -= in.msDelta();
			if (fading <= 0) {
				currentMusic = null;
			}
		} else {
			if (currentMusic != null && options.isEmpty()) {
				fading = 2100;
				in.fadeOutMusic(2000);
				return;
			}
			if (currentMusic != null && !options.contains(currentMusic)) {
				fading = 2100;
				in.fadeOutMusic(2000);
				return;
			}
			if (currentMusic == null && !options.isEmpty()) {
				currentMusic = options.get(AGame.ANIM_R.nextInt(options.size()));
				fading = 0;
				in.playMusic(currentMusic, musicVolume, null, new MusicCallback() {
					@Override
					public void run(String music, double volume) {
						if (fading <= 0) {
							currentMusic = null;
						}
					}
				});
			} else if (currentMusic == null && options.isEmpty()) {
				// 诊断：当前屏幕未提供音乐列表
				if (!loggedNoMusicOptions) {
					System.err.println("Music skipped: no music options for screen " + s.getClass().getSimpleName());
					loggedNoMusicOptions = true;
				}
			}
		}
	}
	
	public static final int LOW_MEMORY_TRIGGER_MB = 50;
	public static final int MS_AT_LOW_MEMORY_THRESHOLD = 3000;
	private int msAtLowMemory = 0;
	private boolean doCheckMemory;
	
	public void doLowMemoryCheck() {
		Runtime.getRuntime().gc();
		msAtLowMemory = 0;
		doCheckMemory = true;
	}

	public void memoryWarningTick(int ms) {
		if (doCheckMemory) {
			int mbLeft = (int) (Runtime.getRuntime().freeMemory() / 1000000l);
			int mbClaimed = (int) (Runtime.getRuntime().totalMemory() / 1000000l);
			int mbTotalAvailable = (int) (Runtime.getRuntime().maxMemory() / 1000000l);
			if (mbLeft <= LOW_MEMORY_TRIGGER_MB && mbClaimed + LOW_MEMORY_TRIGGER_MB >= mbTotalAvailable) {
				if (msAtLowMemory < 1000 && msAtLowMemory + ms >= 1000) {
					Runtime.getRuntime().gc();
				}
				msAtLowMemory += ms;
			} else {
				System.err.println("Memory check: " + mbLeft);
				msAtLowMemory = 0;
				doCheckMemory = false;
			}
		}
	}
	
	public boolean lowMemory() {
		return msAtLowMemory >= MS_AT_LOW_MEMORY_THRESHOLD;
	}
	
	public void setMyNameAndArms() {
		if (myName == null) {
			myName = SteamBackend.isEnabled()
				? SteamBackend.getSteamNickDisplayName()
				: integration.username != null && integration.username.length() > 0
				? integration.username
				: System.getProperty("user.name", "Airships Player " + AGame.ANIM_R.nextInt(10000));
			myArmsJSON = getBestCOA().toJSON();
		}
	}
	
	public String getBestUserIdentifier() {
		if (SteamBackend.isEnabled()) {
			return SteamBackend.getSteamNickDisplayName();
		}
		if (integration.username != null && AGame.makeFileSafe(integration.username).length() > 3) {
			return AGame.makeFileSafe(integration.username);
		}
		return AGame.makeFileSafe(AGame.getStaticGameDirectory().getAbsolutePath().replace("/", "-").replace("\\", "-"));
	}
	
	public boolean isShowingChatOverlay() {
		return multiplayerChatOverlay && canShowMPChatOverlay() && metaLobbyOverlay != null && mpChatOverlayActive;
	}
	
	//boolean paused = false;
		
	@Override
	public void input(Input in) {
		// 每帧重置按键查询去重缓存：否则全屏（scaleFrom==null、input 单例）下
		// Keys.check 的 queriedKeys 跨帧不清空，ESCAPE 等键只在首次按下生效。
		Keys.resetQueriedKeys();
		/*if (in.keyPressed("X")) {
			paused = !paused;
			System.out.println("Paused: " + paused);
		}
		if (paused) {
			return;
		}*/
		if (AGame.isResumeDebug()) {
			int i = 1;
			for (ResumeScreen.InfoPhase ip : ResumeScreen.InfoPhase.values()) {
				if (in.keyPressed(i + "")) {
					if (ResumeScreen.DEBUG_STALL_PHASES.contains(ip)) {
						ResumeScreen.DEBUG_STALL_PHASES.remove(ip);
					} else {
						ResumeScreen.DEBUG_STALL_PHASES.add(ip);
					}
				}
				i++;
			}
		}
		final Input originalIn = in;
		if (scaleFrom != null) {
			in = new ScaledInput(originalIn);
		}
		long tickStart = System.currentTimeMillis();
		/*if (tickEnd != 0 && (tickStart - tickEnd) > 1000) {
			reportDebug("exogenous pause of " + (tickStart - tickEnd) + " in " + s.getClass().getSimpleName());
		}*/
		time += in.msDelta();
		if (!hasRendered && AGame.TICK_RENDER_LOCK) {
			return;
		}
		hasRendered = false;
		try {
			if (testRunner != null) {
				testRunner.tick(this, in);
			}
			if (first) {
				if (Main.ON_STEAM_DECK) {
					in.setMode(new ScreenMode(1280, 800, false));
				} else {
					in.setMode(new ScreenMode(800, 600, false));
				}
				Mod.resetLoadBases(AirshipGame.getMyInput(in));
				first = false;
			}

			if (integration != null) {
				msSinceIntegration += in.msDelta();
			}

			memoryWarningTick(in.msDelta());
			while (processClient()) {}
			
			String screenName = s instanceof UniScreen ? ((UniScreen) s).intent.getClass().getSimpleName() : s.getClass().getSimpleName();
			if (recentScreens.isEmpty() || !recentScreens.get(0).a.equals(screenName)) {
				recentScreens.add(0, new Pair<String, Long>(screenName, System.currentTimeMillis()));
			}
			while (recentScreens.size() > 5) {
				recentScreens.remove(recentScreens.size() - 1);
			}

			CombatSoundEffects.alwaysTick(this, in);
			Monkey.tick(in, in.msDelta());			
			GUIScale newGUIScale = guiScaleOverride == null ? GUIScale.best(in.mode()) : guiScaleOverride;
			if (Main.ON_STEAM_DECK) {
				newGUIScale = steamDeckSmallGUI ? GUIScale.SMALL : GUIScale.MEDIUM;
			}
			if (newGUIScale != currentGUIScale) {
				newGUIScale.activate();
			}
			currentGUIScale = newGUIScale;

			// Maybe the one we stored was garbage, let's check.
			if (s instanceof MainMenu && chosenMultiplayerServer != null && !checkedMPServer && !integration.backend.getMultiplayerServers().isEmpty()) {
				boolean serverFound = false;
				for (Pair<String, String> server : integration.backend.getMultiplayerServers()) {
					if (server.b.equals(chosenMultiplayerServer)) {
						serverFound = true;
					}
				}
				if (!serverFound) {
					chosenMultiplayerServer = null;
				}
				checkedMPServer = true;
			}

			if (s instanceof MainMenu && overlayServer != null && !overlayServer.equals(chosenMultiplayerServer)) {
				disconnectClient();
				metaLobbyOverlay = null;
			}
			if (
					AGame.CHAT_OVERLAY_ENABLED &&
					!exiting &&
					checkedMPServer &&
					canShowMPChatOverlay() &&
					metaLobbyOverlay == null &&
					multiplayerChatOverlay &&
					integration != null &&
					!integration.backend.getMultiplayerServers().isEmpty() &&
					chosenMultiplayerServer != null &&
					(client == null || !client.isDisconnected()) && // Not our job to resuscitate the client.
					(msSinceIntegration >= 10000 || integration.getRegisteredCOAIfAvailable() != null) &&
					(!SteamBackend.isEnabled() || SteamBackend.hasRealNickname())
			)
			{
				if (client == null) {
					overlayServer = chosenMultiplayerServer;
					players.clear();
					channels.clear();
					helloSent = false;
					idReceived = false;
					channelsLoaded = false;
					myID = 0;
					setMyNameAndArms();
					client = new Client(playerName(), chosenMultiplayerServer, this);
					client.tick();
				}
				metaLobbyOverlay = new MetaLobbyOverlay(this);
			}

			if (!multiplayerChatOverlay && metaLobbyOverlay != null) {
				metaLobbyOverlay = null;
			}

			if (!canShowMPChatOverlay()) {
				mpChatOverlayActive = false;
			}

			if (client != null && client.isMessageTooLarge()) {
				showError(_t("generic_message_too_large"));
				client.clearMessageTooLarge();
			}

			if (lanClient != null && lanClient.isMessageTooLarge()) {
				showError(_t("generic_message_too_large"));
				lanClient.clearMessageTooLarge();
			}

			Pt cs = in.cursor();
			mouseMoved = cs != null && !cs.equals(lastCursorPt);
			lastCursorPt = cs;
			manageMusic(in);
			backgroundPlayTimeLeft -= in.msDelta();
			if (in.isCursorVisible() != (SimplePref.SYSTEM_CURSOR.get() || LaunchSettings.forceSystemCursor || Main.ON_STEAM_DECK)) {
				in.setCursorVisible(SimplePref.SYSTEM_CURSOR.get() || LaunchSettings.forceSystemCursor || Main.ON_STEAM_DECK);
			}
			drawState.tick(in.msDelta(), cursor = in.cursor());
			if (delayExitForFlush > 0) {
				delayExitForFlush -= in.msDelta();
			}
			if (delayExitForNetwork > 0) {
				delayExitForNetwork -= in.msDelta();
			}
			if (error == null) {
				hs.hit(in);
				long start = showFrameCosts ? System.nanoTime() : 0;
				Pt clk = in.clicked();
				if (clk != null && in.clickButton() > 3) {
					clk = null;
				}
				boolean subWithBlankInput = false;
				if (helpText != null) {
					helpTextScroll.tick(in, helpTextScrollR.x, helpTextScrollR.y, helpTextScrollR.w, helpTextScrollR.h);
					helpTopicsScroll.tick(in, helpTopicsScrollR.x, helpTopicsScrollR.y, helpTopicsScrollR.w, helpTopicsScrollR.h);
					List<String> topics = getHelpTopics();
					if (in.keyPressed("DOWN") && !topics.isEmpty()) {
						showHelpTopic(topics.get((topics.indexOf(helpTopic) + 1) % topics.size()));
					}
					if (in.keyPressed("UP") && !topics.isEmpty()) {
						showHelpTopic(topics.get((topics.indexOf(helpTopic) - 1 + topics.size()) % topics.size()));
					}
					if (Keys.check(in, "ESCAPE") || Keys.check(in, "ENTER")) {
						helpText = null;
						if (hideHelpRunnable != null) {
							hideHelpRunnable.run();
						}
						hideHelpRunnable = null;
					}
					helpSearchF.useUpDown = false;
					helpSearchF.input(in, cursor, clk, in.msDelta());
					subWithBlankInput = true;
				} else if (multiplayerChatOverlay && canShowMPChatOverlay() && metaLobbyOverlay != null) {
					metaLobbyOverlay.input(in, drawState, cursor, clk, in.msDelta());
					subWithBlankInput = mpChatOverlayActive;
				}
				if (subWithBlankInput) {
					in = new BlankInput(in);
				}
				s.input(in, drawState, cursor, clk, in.msDelta());
				prevScreen = postInputScreen;
				postInputScreen = s;
				if (showFrameCosts) {
					frameCosts.add((int) ((System.nanoTime() - start) / 100000));
					if (frameCosts.size() > 300) {
						frameCosts.pollFirst();
					}
				}
			} else {
				if (in.clicked() != null) {
					error = null;
					drawState.hasClicked();
				}
				in = new BlankInput(in);
				s.input(in, drawState, cursor, null, in.msDelta());
			}
			hs.list.clear();
			if (integration != null) { integration.tick(); }
			lastMs = in.msDelta();
			SteamBackend.tick();
			Achievement.tick(in.msDelta());
			Analytics.tick(in.msDelta());

			msPlayed += in.msDelta();
			if (msPlayed > 60 * 5 * 1000) {
				msPlayed -= 60 * 5 * 1000;
				try {
					PREFS.putInt("M_PLAYED", PREFS.getInt("M_PLAYED", 0) + 5);
					PREFS.save();
				} catch (Exception e) {
					// Ignore lovingly
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.flush();
			Exception e = t instanceof Exception ? (Exception) t : new RuntimeException(t);
			reportError(null, e, null, true);
		}
		tickEnd = System.currentTimeMillis();
		int tickCost = (int) (tickEnd - tickStart);
		PerfStats.inputTime += tickCost;
		tickCosts.add(tickCost);
		if (tickCosts.size() > 1000) {
			tickCosts.remove(0);
		}
		PerfStats.check();
		PerfStats.clear();
		/*if (tickCost > 10000 && !(s instanceof LoadingScreen)) {
			reportDebug("tick cost " + tickCost + " in " + s.getClass().getName());
		}*/
	}
	
	public boolean showReviewQuestion() {
		return
				PREFS.getInt("M_PLAYED", 0) >= 60 * 6 &&
				PREFS.getInt("M_PLAYED", 0) < 60 * 30 &&
				SteamBackend.isEnabled() &&
				new Date().getDate() % 5 == 0;
	}
	
	private static final int TT_Y_OFFSET = 18;
	
	private boolean canShowMPChatOverlay() {
		if (s instanceof MetaLobbyScreen) {
			return false;
		}
		if (s instanceof ResumeScreen) {
			return false;
		}
		if (s instanceof UniScreen) {
			UniScreen us = (UniScreen) s;
			if (us.intent instanceof MultiplayerSetupIntent) {
				return false;
			}
		}
		if (s instanceof LoadingScreen) {
			return false;
		}
		if (s instanceof ResChooserScreen) {
			return false;
		}
		return true;
	}
	
	@Override
	public void render(Frame f) {
		long renderStart = System.currentTimeMillis();
		currentHelpHighlights.clear();
		currentHelpHighlights.putAll(nextHelpHighlights);
		nextHelpHighlights.clear();
		hasRendered = true;
		try {
			if (first) { return; }
						
			if (Loadable.map.containsKey(SpritesheetBundle.class)) {
				if (s.alwaysUseAppearancePostfix() || Appearance.useSimpleGraphics || !Appearance.useLighting || Appearance.shaderLoadFailed) {
					Appearance.switchSpritesheet(s.appearancePostfix());
				} else {
					Appearance.switchSpritesheet("");
				}
			}
			
			((Graphics) f.nativeRenderer()).clearClip();
			final Frame originalF = f;
			if (scaleFrom != null) {
				f = new ScaledFrame(originalF);
				if (scaleBuffer == null || scaleBuffer.getWidth() != scaleFrom.width || scaleBuffer.getHeight() != scaleFrom.height) {
					if (scaleBuffer != null) { scaleBuffer.destroy(); }
					scaleBuffer = new Image(scaleFrom.width, scaleFrom.height);
					scaleBuffer.setFilter(Image.FILTER_LINEAR);
				}
			}
			
			drawState.resetCursor();
			s.render(new MyDraw(f, hs, drawState, integration), f.mode(), hs, cursor);
			MyDraw d = new MyDraw(f, hs, drawState, integration);
			
			ScreenMode sm = f.mode();
			if (multiplayerChatOverlay && canShowMPChatOverlay() && metaLobbyOverlay != null) {
				if (mpChatOverlayActive) {
					drawState.glowRects.clear();
					hs.list.clear();
					d.rect(ERR_BG_TINT, 0, 0, sm.width, sm.height);
				}
				metaLobbyOverlay.render(d, sm, hs, cursor);
			}
			
			if (helpText != null) {
				drawState.glowRects.clear();
				hs.list.clear();
				int maxTopicW = 0;
				for (String t : helpTopics) {
					maxTopicW = Math.max(maxTopicW, (int) d.textSize(t, AGame.FOUNT).x);
				}
				maxTopicW += MyDraw.UI_SPACING;
				int topicsW = maxTopicW + ScrollBar.SCROLL_BAR_W + MyDraw.PANEL_INSET * 2;
				int textW = sm.width / 3;
				int w = topicsW + textW + MyDraw.UI_SPACING + MyDraw.WINDOW_INSET * 2;
				int x = sm.width / 2 - w / 2;
				int h = sm.height / 2;
				int y = sm.height / 2 - h / 2;
				d.drawShadowedWindow(x, y, w, h, -1);
				d.iconButton(x + w - MyDraw.ICON_BUTTON_SZ, y, close, new Runnable() {
					@Override
					public void run() {
						helpText = null;
						if (hideHelpRunnable != null) {
							hideHelpRunnable.run();
						}
						hideHelpRunnable = null;
					}
				}, true);
				x += MyDraw.WINDOW_INSET;
				y += MyDraw.WINDOW_INSET;
				w -= MyDraw.WINDOW_INSET * 2;
				h -= MyDraw.WINDOW_INSET * 2;
				int scrollH = h - Math.max(AGame.BIGGER_FOUNT.lineHeight, MyDraw.BUTTON_H);
				d.text(_t("manual"), AGame.BIGGER_FOUNT, x + w / 2 - (int) d.textSize(_t("manual"), AGame.BIGGER_FOUNT).x / 2, y);
				y += Math.max(AGame.BIGGER_FOUNT.lineHeight, MyDraw.BUTTON_H);
				helpSearchF.help = _t("Search");
				helpSearchF.render(x, y, topicsW, d);
				helpTopicsScrollR.update(x, y + MyDraw.textFieldH() + MyDraw.BUTTON_SPACING, topicsW, scrollH - MyDraw.textFieldH() - MyDraw.BUTTON_SPACING);
				helpTopicsScroll.draw(d, x, y + MyDraw.textFieldH() + MyDraw.BUTTON_SPACING, topicsW, scrollH- MyDraw.textFieldH() - MyDraw.BUTTON_SPACING, getHelpTopics(), helpTopicsAdapter);
				helpTextScrollR.update(x + topicsW + MyDraw.UI_SPACING, y, textW, scrollH);
				helpTextScroll.draw(d, x + topicsW + MyDraw.UI_SPACING, y, textW, scrollH, Collections.singletonList(helpText), helpTextAdapter);
			}
			
			updateUIGlowRects(lastMs, drawState);
			
			/*if (MyDraw.UI_GLOW_COLOR.a > 0) {
				for (UIGlowRect r : drawState.glowRects.values()) {
					if (r.hoverMs > 0) {
						Clr c = new Clr(MyDraw.UI_GLOW_COLOR.r, MyDraw.UI_GLOW_COLOR.g, MyDraw.UI_GLOW_COLOR.b, StrictMath.min(255, MyDraw.UI_GLOW_COLOR.a * r.hoverMs / 255));
						d.rect(c, r.x, r.y, r.w, r.h);
					}
				}
			}*/

			// 渲染前刷新光标位置（f.cursor() 为实时读取），消除自定义光标的显示延迟
			cursor = f.cursor();

			if (!Main.ON_STEAM_DECK && cursor != null && drawState.cursorText != null && drawState.cursorAppearance != null) {
				d.text(drawState.cursorText, AGame.FOUNT, cursor.x + drawState.cursorAppearance.dx(currentGUIScale), cursor.y + drawState.cursorAppearance.dy(currentGUIScale) + 18);
			}
			if (drawState.ticksSinceMouseMoved > tooltipDelay && !drawState.tooltipText.isEmpty() && cursor != null && drawState.cursorAppearance != null) {
				int ttw = AGame.FOUNT.displayWidth * 20 + 150;
				Rect sz = d.textSize(drawState.tooltipText, AGame.FOUNT, 0, 0, ttw);
				int ttH = (int) sz.height;
				int ttW = (int) sz.width;
				int ttX = (int) cursor.x + drawState.cursorAppearance.dx(currentGUIScale) + 16;
				if (ttX + ttW + MyDraw.PANEL_INSET * 2 + 16 >= sm.width) {
					ttX = sm.width - (ttW + MyDraw.PANEL_INSET * 2 + 16);
				}
				if (drawState.maxTooltipX != -1) {
					ttX = StrictMath.min(ttX, drawState.maxTooltipX - ttW);
				}
				int ttY = (int) cursor.y + drawState.cursorAppearance.dy(currentGUIScale) + TT_Y_OFFSET;
				if (ttY + ttH + MyDraw.PANEL_INSET * 2 + 16 >= sm.height) {
					ttY = sm.height - (ttH + MyDraw.PANEL_INSET * 2 + 16);
				}
				d.drawShadowedPanel(ttX, ttY, ttW + MyDraw.PANEL_INSET * 2, ttH + MyDraw.PANEL_INSET * 2);
				d.text(drawState.tooltipText, AGame.FOUNT, ttX + MyDraw.PANEL_INSET, ttY + MyDraw.PANEL_INSET, ttw);
			}

			if (!SteamBackend.isEnabled() && Achievement.getRecentAchievement() != null) {
				Achievement rec = Achievement.getRecentAchievement();
				String txt = "Achievement unlocked:\n" + rec.displayName + "\n" + rec.desc;
				Pt sz = d.textSize(txt, AGame.FOUNT);
				int w = 15 + 64 + (int) sz.x;
				int h = 10 + 64;
				d.drawPanel(sm.width - w, sm.height - h, w, h, 0);
				d.blit(rec.logo, sm.width - w + 5, sm.height - h + 5);
				d.text(txt, AGame.FOUNT, sm.width - w + 5 + 64 + 5, sm.height - h + 5);
			}
			
			if (error != null) {
				d.rect(ERR_BG_TINT, 0, 0, sm.width, sm.height);
				int w = StrictMath.max(400, sm.width / 2);
				int h = (int) d.textSize(error, AGame.FOUNT, 0, 0, w - MyDraw.WINDOW_INSET * 2).height + MyDraw.WINDOW_INSET * 2;
				int x = sm.width / 2 - w / 2;
				int y = sm.height / 2 - h / 2;
				try {
					d.drawShadowedWindow(x, y, w, h, -1);
					x += MyDraw.WINDOW_INSET;
					y += MyDraw.WINDOW_INSET;
					w -= MyDraw.WINDOW_INSET * 2;
					h -= MyDraw.WINDOW_INSET * 2;
				} catch (Exception e) {}
				d.text(error, AGame.FOUNT, x, y, w);
			}

			if (showFrameCosts) {
				drawFrameCosts(d, f.mode());
			}
			
			if (!(s instanceof LoadingScreen) || (s instanceof UniScreen && ((UniScreen) s).intent instanceof PlaybackIntent) && ((PlaybackIntent) ((UniScreen) s).intent).takeGifUntil != -1) {
				if (drawState.cursorAppearance != null && !(SimplePref.SYSTEM_CURSOR.get() || LaunchSettings.forceSystemCursor)) {
					drawState.cursorAppearance.draw(d, cursor.x, cursor.y, currentGUIScale);
				}
			}
			
			if (AGame.debugCursor()) {
				d.rect(Clr.BLACK, 0, 0, 100, AGame.FOUNT.height * 5);
				d.text("x1 " + f.cursor().x + "\ny1 " + f.cursor().y + "\nx2 " + f.cursor().x + "\ny2 " + f.cursor().y,
						AGame.FOUNT, 10, 10);
			}
			
			if (SimplePref.SHOW_CLOCK.get() && !mpChatOverlayActive) {
				String wt = AGame.wallTime();
				int wtx = sm.width - (int) d.textSize(wt, AGame.FOUNT).x - MyDraw.SIDE_CLEARANCE;
				int wty = s instanceof MainMenu ? MyDraw.TOP_BAR_INSET : sm.height - MyDraw.SIDE_CLEARANCE - AGame.FOUNT.lineHeight;
				if (s instanceof StrategicScreen) {
					if (multiplayerChatOverlay) {
						wtx = MyDraw.SIDE_CLEARANCE + MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
					} else {
						wtx = MyDraw.SIDE_CLEARANCE;
					}
				} else if (s instanceof MetaLobbyScreen) {
					wtx = MyDraw.SIDE_CLEARANCE + MyDraw.UI_SPACING;
					wty -= MyDraw.UI_SPACING;
				} else if (multiplayerChatOverlay && !(s instanceof MainMenu)) {
					wtx -= MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
				}
				
				d.text(wt, AGame.FOUNT, wtx, wty);
			}
			
			d.renderHighlights(this);
			
			if (lowMemory()) {
				int mbLeft = (int) (Runtime.getRuntime().freeMemory() / 1000000l);
				String lms = _t("low_memory_warning");
				lms += " (" + mbLeft + "/" + ((Runtime.getRuntime().totalMemory() / 1000000l)) + "/"  + (Runtime.getRuntime().maxMemory() / 1000000l) + ")";
				Rect r = d.textSize(lms, AGame.FOUNT, 0, 0, sm.width - MyDraw.SIDE_CLEARANCE * 2);
				int w = (int) r.width;
				int h = (int) r.height;
				d.drawPanel(sm.width / 2 - w / 2 - MyDraw.PANEL_INSET, sm.height - h - MyDraw.PANEL_INSET * 2, w + MyDraw.PANEL_INSET * 2, h + MyDraw.PANEL_INSET * 4, -1);
				d.text(MyDraw.ERROR_C + lms, AGame.FOUNT, sm.width / 2 - w / 2, sm.height - h - MyDraw.PANEL_INSET);
			}
			
			Monkey.render(d);
			
			if (AGame.isResumeDebug()) {
				int i = 1;
				for (ResumeScreen.InfoPhase ip : ResumeScreen.InfoPhase.values()) {
					d.text("[bg=000000]" + (ResumeScreen.DEBUG_STALL_PHASES.contains(ip) ? "[ff3333]" : "[999999]") + i + ": " + _t("resync_" + ip.name()), AGame.FOUNT, 20, i * 20);
					i++; 
				}
			}
						
			if (scaleFrom != null) {
				ScreenMode scaleTo = originalF.mode();
				Graphics gfx = ((Graphics) originalF.nativeRenderer());
				gfx.copyArea(scaleBuffer, 0, 0);
				int srcAspectRatio = scaleFrom.width * 1000 / scaleFrom.height;
				int trgAspectRatio = scaleTo.width * 1000 / scaleTo.height;
				originalF.rect(Clr.BLACK, 0, 0, scaleTo.width, scaleTo.height, 0);
				GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
				if (srcAspectRatio > trgAspectRatio) {
					// Letterboxing
					int targetH = scaleFrom.height * scaleTo.width / scaleFrom.width;
					gfx.drawImage(scaleBuffer, 0, (scaleTo.height - targetH) / 2, scaleTo.width, (scaleTo.height - targetH) / 2 + targetH, 0, 0, scaleBuffer.getWidth(), scaleBuffer.getHeight());
				} else if (srcAspectRatio < trgAspectRatio) {
					// Pillarboxing
					int targetW = scaleFrom.width * scaleTo.height / scaleFrom.height;
					gfx.drawImage(scaleBuffer, (scaleTo.width - targetW) / 2, 0, (scaleTo.width - targetW) / 2 + targetW, scaleTo.height, 0, 0, scaleBuffer.getWidth(), scaleBuffer.getHeight());
				} else {
					//Pure scaling
					gfx.drawImage(scaleBuffer, 0, 0, scaleTo.width, scaleTo.height, 0, 0, scaleBuffer.getWidth(), scaleBuffer.getHeight());
				}
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.flush();
			Exception e = t instanceof Exception ? (Exception) t : new RuntimeException(t);
			reportError(null, e, null, true);
		}
		long cost = (System.currentTimeMillis() - renderStart);
		PerfStats.renderTime += cost;
		PerfStats.numRenders++;
		renderCosts.add((int) cost);
		if (renderCosts.size() > 1000) {
			renderCosts.remove(0);
		}
	}
	
	public void drawFrameCosts(Draw d, ScreenMode sm) {
		int x = sm.width - 305;
		d.rect(Clr.WHITE, x, 200, 300, 1); // 16 ms line
		for (int i : frameCosts) {
			d.rect(Clr.WHITE, x, 360 - i, 1, i);
			x++;
		}
	}

	@Override
	public void handle(Exception e, boolean fatal) {
		if (!swallowExceptions) {
			reportError(null, e, null, fatal);
		}
	}
	
	public void updateUIGlowRects(int ms, MyDraw.State s) {
		for (Iterator<UIGlowRect> it = s.glowRects.values().iterator(); it.hasNext();) {
			UIGlowRect r = it.next();
			if (!r.confirmed) {
				it.remove();
			} else {
				r.confirmed = false;
				if (Rect.contains(r.x, r.y, r.w, r.h, cursor)) {
					r.hoverMs = StrictMath.min(UIGlowRect.MAX_GLOW, r.hoverMs + ms * 2);
				} else {
					r.hoverMs = StrictMath.max(0, r.hoverMs - ms);
				}
			}
		}
	}

	public int outQueueSize() {
		if (lanClient != null) {
			return lanClient.outQueueSize();
		}
		if (client != null) {
			return client.outQueueSize();
		}
		return 0;
	}
	
	public int inQueueSize() {
		if (lanClient != null) {
			return lanClient.inQueueSize();
		}
		if (client != null) {
			return client.inQueueSize();
		}
		return 0;
	}
	
	public void tickClients() {
		if (lanClient != null) {
			lanClient.tick();
		}
		if (client != null) {
			client.tick();
		}
		while (processClient()) {}
	}
	
	public boolean isReconnecting() {
		if (lanClient != null) {
			return lanClient.isReconnecting();
		}
		if (client != null) {
			return client.isReconnecting();
		}
		return false;
	}
	
	public long ping() {
		if (lanClient != null) {
			return lanClient.smoothedRecentPing();
		}
		if (client != null) {
			return client.smoothedRecentPing();
		}
		return 0;
	}
	
	public int unacknowledgedBytes() {
		if (lanClient != null) {
			return lanClient.unacknowledgedBytes();
		}
		if (client != null) {
			return client.unacknowledgedBytes();
		}
		return 0;
	}

	@Override
	public void report(String s, Throwable t) {
		reportError(s, new RuntimeException(t), null, false, true);
	}
	
	public static Lwjgl3Engine.MyInput getMyInput(Input in) {
		if (in instanceof BlankInput) {
			in = ((BlankInput) in).originalIn;
		}
		if (in instanceof ScaledInput) {
			in = ((ScaledInput) in).originalIn;
		}
		return (Lwjgl3Engine.MyInput) in;
	}
	
	public static String getTypedText(Input in) {
		if (in instanceof BlankInput) {
			return "";
		}
		if (in instanceof ScaledInput) {
			in = ((ScaledInput) in).originalIn;
		}
		String text = ((Lwjgl3Engine.MyInput) in).typedText();
		return text == null ? "" : text;
	}
	
	public static Pt ptFromRealToScaled(Pt p, ScreenMode scaleTo) {
		if (p == null) { return null; }
		if (scaleFrom == null || scaleTo == null) { return p; }
		int srcAspectRatio = scaleFrom.width * 1000 / scaleFrom.height;
		int trgAspectRatio = scaleTo.width * 1000 / scaleTo.height;
		if (srcAspectRatio > trgAspectRatio) {
			// Letterboxing
			int targetH = scaleFrom.height * scaleTo.width / scaleFrom.width;
			return new Pt(p.x * scaleFrom.width / scaleTo.width, (p.y - (scaleTo.height - targetH) / 2) * scaleFrom.width / scaleTo.width);
		} else if (srcAspectRatio < trgAspectRatio) {
			// Pillarboxing
			int targetW = scaleFrom.width * scaleTo.height / scaleFrom.height;
			return new Pt((p.x - (scaleTo.width - targetW) / 2) * scaleFrom.height / scaleTo.height, p.y * scaleFrom.height / scaleTo.height);
		} else {
			//Pure scaling
			return new Pt(p.x * scaleFrom.width / scaleTo.width, p.y * scaleFrom.height / scaleTo.height);
		}
	}

	public static class ScaledInput implements Input {
		public final Input originalIn;
		public final ScreenMode scaleTo;

		public ScaledInput(Input originalIn) {
			this.originalIn = originalIn;
			scaleTo = originalIn.mode();
		}

		@Override
		public boolean keyDown(String string) {
			return originalIn.keyDown(string);
		}

		@Override
		public boolean keyPressed(String string) {
			return originalIn.keyPressed(string);
		}

		@Override
		public String lastKeyPressed() {
			return originalIn.lastKeyPressed();
		}

		@Override
		public char lastInput() {
			return originalIn.lastInput();
		}

		@Override
		public Pt cursor() {
			return ptFromRealToScaled(originalIn.cursor(), scaleTo);
		}

		@Override
		public Pt mouseDown() {
			return ptFromRealToScaled(originalIn.mouseDown(), scaleTo);
		}

		@Override
		public int mouseDownButton() {
			return originalIn.mouseDownButton();
		}

		@Override
		public Pt clicked() {
			return ptFromRealToScaled(originalIn.clicked(), scaleTo);
		}

		@Override
		public int clickButton() {
			return originalIn.clickButton();
		}

		@Override
		public int scrollAmount() {
			return originalIn.scrollAmount();
		}

		@Override
		public int msDelta() {
			return originalIn.msDelta();
		}

		@Override
		public ScreenMode mode() {
			return scaleFrom == null ? originalIn.mode() : scaleFrom;
		}

		@Override
		public Input setMode(ScreenMode sm) {
			originalIn.setMode(sm);
			return this;
		}

		@Override
		public ArrayList<ScreenMode> modes() {
			return originalIn.modes();
		}

		@Override
		public boolean isCursorVisible() {
			return originalIn.isCursorVisible();
		}

		@Override
		public Input setCursorVisible(boolean bln) {
			originalIn.setCursorVisible(bln);
			return originalIn;
		}

		@Override
		public void preload(List<Img> list) {
			originalIn.preload(list);
		}

		@Override
		public void preloadSounds(List<String> list) {
			originalIn.preloadSounds(list);
		}

		@Override
		public void play(String string, double d, double d1, double d2, double d3) {
			originalIn.play(string, d, d1, d2, d3);
		}

		@Override
		public Loop loop(String string, double d, double d1, double d2, double d3) {
			return originalIn.loop(string, d, d1, d2, d3);
		}

		@Override
		public void preloadMusic(String string) {
			originalIn.preloadMusic(string);
		}

		@Override
		public void playMusic(String string, double d, MusicCallback mc, MusicCallback mc1) {
			originalIn.playMusic(string, d, mc, mc1);
		}

		@Override
		public void stopMusic() {
			originalIn.stopMusic();
		}

		@Override
		public void fadeOutMusic(int i) {
			originalIn.fadeOutMusic(i);
		}

		@Override
		public void quit() {
			originalIn.quit();
		}
	}
	
	private final ScrollBar.ScrollElementAdapter<String> helpTopicsAdapter = new ScrollBar.ScrollElementAdapter<String>() {
		@Override
		public int getHeight(String t, MyDraw d, int availableWidth) {
			return AGame.FOUNT.lineHeight + MyDraw.SCROLL_EL_SPACING;
		}
		@Override
		public void draw(final String t, MyDraw d, int x, int y, int width) {
			if (t.equals(helpTopic)) {
				d.text(MyDraw.SELECTED_C + t, AGame.FOUNT, x, y);
			} else {
				d.text(t, AGame.FOUNT, x, y);
			}
			d.hook(x, y, width, AGame.FOUNT.lineHeight + MyDraw.SCROLL_EL_SPACING, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input input, Pt pt, Hook.Type type) {
					showHelpTopic(t);
				}
			});
		}
	};
	
	private final ScrollBar.ScrollElementAdapter<String> helpTextAdapter = new ScrollBar.ScrollElementAdapter<String>() {
		@Override
		public int getHeight(String t, MyDraw d, int availableWidth) {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			while (t.indexOf("[", i) != -1) {
				int start = t.indexOf("[", i);
				sb.append(t.substring(i, start));
				int end = t.indexOf("]", start);
				String linkText = t.substring(start + 1, end).split("->")[0];
				sb.append(linkText);
				i = end + 1;
			}
			sb.append(t.substring(i));
			return AGame.BIG_FOUNT.lineHeight + (int) d.textSize(sb.toString(), AGame.FOUNT, 0, 0, availableWidth).height;
		}
		@Override
		public void draw(final String t, MyDraw d, int x, int y, int width) {
			d.text(helpTopic.trim(), AGame.BIG_FOUNT, x, y);
			if (helpIcons.containsKey(helpTopic)) {
				d.blit(helpIcons.get(helpTopic), x + width - helpIcons.get(helpTopic).srcWidth, y);
			}
			y += AGame.BIG_FOUNT.lineHeight;
			// Implementing hypertext, whee.
			StringBuilder sb = new StringBuilder();
			ArrayList<TextLink> links = new ArrayList<TextLink>();
			int i = 0;
			while (t.indexOf("[", i) != -1) {
				int start = t.indexOf("[", i);
				sb.append(t.substring(i, start));
				int end = t.indexOf("]", start);
				String linkText = t.substring(start + 1, end).split("->")[0];
				String linkValue = t.substring(start + 1, end).split("->")[1];
				int linkStart = sb.length() - 1;
				sb.append(MyDraw.SELECTED_C).append(linkText).append("[]");
				int linkEnd = sb.length();
				links.add(new TextLink(linkStart, linkEnd, linkValue));
				i = end + 1;
			}
			sb.append(t.substring(i));
			String t2 = sb.toString();
			d.text(t2, AGame.FOUNT, x, y, width);
			for (final TextLink tl : links) {
				Rect startLoc = charLocation(d, t2, tl.start, AGame.FOUNT, x, y, width);
				Rect endLoc = charLocation(d, t2, tl.end, AGame.FOUNT, x, y, width);
				if (startLoc.y == endLoc.y) {
					d.hook(startLoc.x, startLoc.y, endLoc.x + endLoc.width - startLoc.x, AGame.FOUNT.lineHeight, new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input input, Pt pt, Hook.Type type) {
							showHelpTopic(tl.link);
						}
					});
				} else {
					d.hook(startLoc.x, startLoc.y, width - startLoc.x, AGame.FOUNT.lineHeight, new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input input, Pt pt, Hook.Type type) {
							showHelpTopic(tl.link);
						}
					});
					d.hook(x, endLoc.y, endLoc.x + endLoc.width - x, AGame.FOUNT.lineHeight, new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input input, Pt pt, Hook.Type type) {
							showHelpTopic(tl.link);
						}
					});
				}
			}
		}
		
		public Rect charLocation(MyDraw d, String text, int charLoc, Fount fount, int x, int y, int maxWidth) {
			if (charLoc == -1) { return new Rect(0, 0, 0, fount.lineHeight); }
			Pt p = d.doText(false, charLoc, text, fount, x, y, maxWidth, 10000, 0, true);
			int charWidth = charLoc > text.length() ? 0 : fount.getWidth(text.charAt(charLoc));
			return new Rect(x + p.x - charWidth, y + p.y - fount.lineHeight, charWidth, fount.lineHeight);
		}
	};
	
	private static class TextLink {
		final int start, end;
		final String link;

		public TextLink(int start, int end, String link) {
			this.start = start;
			this.end = end;
			this.link = link;
		}
	}

	public static class ScaledFrame implements Frame {
		public final Frame originalF;
		public final ScreenMode scaleTo;

		public ScaledFrame(Frame originalF) {
			this.originalF = originalF;
			scaleTo = originalF.mode();
		}

		@Override
		public ScreenMode mode() {
			return scaleFrom == null ? originalF.mode() : scaleFrom;
		}

		@Override
		public int fps() {
			return originalF.fps();
		}

		@Override
		public Object nativeRenderer() {
			return originalF.nativeRenderer();
		}

		@Override
		public Pt cursor() {
			return ptFromRealToScaled(originalF.cursor(), scaleTo);
		}

		@Override
		public void rect(Clr clr, double d, double d1, double d2, double d3, double d4) {
			originalF.rect(clr, d, d1, d2, d3, d4);
		}

		@Override
		public void blit(Img img, Clr clr, double d, double d1, double d2, double d3, double d4, double d5) {
			originalF.blit(img, clr, d, d1, d2, d3, d4, d5);
		}

		@Override
		public double getWidth(Img img) {
			return originalF.getWidth(img);
		}

		@Override
		public double getHeight(Img img) {
			return originalF.getHeight(img);
		}

		@Override
		public void shift(double d, double d1) {
			originalF.shift(d, d1);
		}

		@Override
		public void scale(double d, double d1) {
			originalF.scale(d, d1);
		}

		@Override
		public void rotate(double d) {
			originalF.rotate(d);
		}

		@Override
		public void resetTransforms() {
			originalF.resetTransforms();
		}
	}
}
