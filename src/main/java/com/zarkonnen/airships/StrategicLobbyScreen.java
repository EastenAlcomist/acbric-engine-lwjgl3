package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.HeroRenderer.HeroSelectionCallback;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.ScrollBar.ScrollElementAdapter;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

public class StrategicLobbyScreen implements Screen, HeroSelectionCallback {
	private Img editIcon = new Img("ui", 416, 416, 16, 16, false);
	
	public final AirshipGame g;
	public final Client client;
	public final Server server;
	
	public String worldID;
	public long seed;
	public boolean helloSent;
	public boolean welcomeReceived;
	public String gameName = "Conquest Game";
	public boolean readySent;
	private boolean unableToLoadCorrectMods;
	private boolean modsLoaded;
	public Locale lang;
	
	public TextField seedF;
	
	private String yourIP;
	
	public final TextField empireNameField = new TextField(false, AGame.FILE_SAFE_ALPHABET, AGame.MAX_NAME_LENGTH);
	public JSONObject armsJSON;
	public Hero hero;
	private DifficultyLevel difficulty = DifficultyLevel.ofName("NORMAL");
	private MapSize size = MapSize.ofName("SMALL");
	private SeaLevelSetting seaLevelSetting = SeaLevelSetting.ofName("MIXED");
	private MonsterSetting monsterity = MonsterSetting.ofName("DEFAULT");
	private FrequencySetting heroFrequency = FrequencySetting.ofName("DEFAULT");
	private FrequencySetting incidentFrequency = FrequencySetting.ofName("DEFAULT");
	private TechSpeedSetting techSpeed = TechSpeedSetting.ofName("NORMAL");
	private EnumSet<ConquestToggle> toggles = EnumSet.allOf(ConquestToggle.class);
	private int startingTechTier = 0;
	private boolean strategicRapidCommands;
	private boolean allowDirectControl = true;
	private boolean anyoneCanPause = false;
	public boolean allowEmpireCoop = true;
	
	private ModReloadProgressDialog mrpd;
	
	public boolean showSettingsTab = false;
	private boolean pleaseConfirmLeave = false;
	
	private final TextField chatField = new TextField(AGame.MAX_CHAT_LENGTH);
	public final ScrollBar chatScrollBar = new ScrollBar();
	public final ScrollBar playerScrollBar = new ScrollBar();
	
	private final ChatAdapter chatAdapter = new ChatAdapter();
	private final StrategicPlayerAdapter playerAdapter = new StrategicPlayerAdapter(this);
	
	public final ScrollBar empiresScrollBar = new ScrollBar();
	private final IntRect empireSBRect = new IntRect();
	private final EmpiresAdapter empiresAdapter = new EmpiresAdapter(this);
	private final EmpireSlotsAdapter empireSlotsAdapter = new EmpireSlotsAdapter(this);
	
	public ArrayList<StrategicChatMsg> chat = new ArrayList<StrategicChatMsg>();
	public HashMap<Integer, StrategicPlayerInfo> channelPlayers = new HashMap<Integer, StrategicPlayerInfo>();
	
	public boolean isResumeFromLoaded;
	public JSONObject loadGameData;
	public CampaignWorld loadedGame;
	public ArrayList<String> gameDataChunks = new ArrayList<String>();
	public int numGameDataChunks = -1;
		
	public int initiatorID;
	public StrategicSetupHoster hoster;
	
	public ArrayList<ModInfo> modInfos;
	public ArrayList<Expansion> expansions;
	
	public boolean lanIDSet;
	
	public static final int GAME_DATA_CHUNK_MAX_SIZE = 2048;
	
	private final ScrollBar heroesScrollBar = new ScrollBar();
	private final IntRect heroesSBR = new IntRect();
	private ArrayList<Hero> availableHeroes;
	private boolean showHeroSelector;
	private HeroRenderer heroRenderer;
	
	private boolean isInitiating() {
		if (g.lanClient != null) {
			return server != null;
		} else {
			return g.playerID() == initiatorID;
		}
	}

	public StrategicLobbyScreen(AirshipGame g, Client client, Server server, JSONObject loadGameData) throws IOException {
		this.g = g;
		this.client = client;
		this.server = server;
		this.loadGameData = loadGameData;
		armsJSON = CoatOfArms.getRandom(AGame.ANIM_R, HeraldicStyle.ofName("player")).toJSON();
		ArrayList<CityName> names = Loadable.all(CityName.class);
		empireNameField.setText(names.get(AGame.ANIM_R.nextInt(names.size())).name);
		empireNameField.focus = false;
		seed = AGame.cleanSeed(AGame.ANIM_R.nextLong()); // Will get overwritten by whoever is actually hosting.
		seedF = new TextField(false, "0123456789", 6);
		seedF.setText("" + seed);
		seedF.focus = false;
		try {
			yourIP = _t("Your_IP_address") + ": " + HostOrJoinGameScreen.getMyIP() + " port " + Server.PORT + " & " + Server.RECONNECT_PORT;
		} catch (Exception e) {
			// Ignore.
		}
		if (loadGameData != null) {
			isResumeFromLoaded = true;
			loadedGame = new CampaignWorld(loadGameData.getJSONObject("world"), g, /* multiplayer */ true, new JSONObjectInPipe(loadGameData));
			String gameDataString = loadGameData.toString();
			gameDataString = Compression.compressToString(gameDataString);
			int n = 0;
			while (n < gameDataString.length()) {
				gameDataChunks.add(gameDataString.substring(n, StrictMath.min(n + GAME_DATA_CHUNK_MAX_SIZE, gameDataString.length())));
				n += GAME_DATA_CHUNK_MAX_SIZE;
			}
			numGameDataChunks = gameDataChunks.size();
		}
		if (server != null) {
			hoster = new StrategicSetupHoster(g, loadedGame);
		}
	}
	
	public void claimEmpire(int empireIndex, JSONObject armsJSON, String name) {
		g.sendMessage(msg("claimEmpire")
				.put("id", g.playerID())
				.put("name", g.playerName())
				.put("arms", armsJSON)
				.put("empireName", name)
				.put("empireIndex", empireIndex));
	}
	
	public void joinEmpire(int empireIndex) {
		g.sendMessage(msg("joinEmpire")
				.put("id", g.playerID())
				.put("name", g.playerName())
				.put("empireIndex", empireIndex));
	}
	
	public void claimLoadedEmpire(Empire e) {
		g.sendMessage(msg("claimEmpire")
				.put("id", g.playerID())
				.put("name", g.playerName())
				.put("arms", e.arms.toJSON())
				.put("empireName", e.name)
				.put("empireIndex", loadedGame.map.empires.indexOf(e)));
	}
	
	public void joinLoadedEmpire(Empire e) {
		g.sendMessage(msg("joinEmpire")
				.put("id", g.playerID())
				.put("name", g.playerName())
				.put("arms", e.arms.toJSON())
				.put("empireName", e.name)
				.put("empireIndex", loadedGame.map.empires.indexOf(e)));
	}
	
	public void yieldEmpire() {
		g.sendMessage(msg("claimEmpire")
				.put("id", g.playerID())
				.put("name", g.playerName())
				.put("empireIndex", -1));
	}
	
	private StrategicPlayerInfo myInfo() {
		for (StrategicPlayerInfo spi : channelPlayers.values()) {
			if (spi.id == g.playerID()) {
				return spi;
			}
		}
		return null;
	}
	
	private boolean isSpectatingOrJoiningCoopEmpire() {
		StrategicPlayerInfo myInfo = myInfo();
		return myInfo != null && (myInfo.claimedEmpireIndex == -1 || !myInfo.claimedEmpireCoopOwner);
	}
	
	private StrategicPlayerInfo joiningCoopEmpire() {
		StrategicPlayerInfo myInfo = myInfo();
		if (myInfo == null || myInfo.claimedEmpireIndex == -1 || myInfo.claimedEmpireCoopOwner) {
			return null;
		}
		for (StrategicPlayerInfo spi : channelPlayers.values()) {
			if (spi.claimedEmpireIndex == myInfo.claimedEmpireIndex && spi.claimedEmpireCoopOwner) {
				return spi;
			}
		}
		return null;
	}
	
	private int claimedEmpireIndex() {
		StrategicPlayerInfo pi = channelPlayers.get(g.playerID());
		return pi == null ? -1 : pi.claimedEmpireIndex;
	}
	
	private boolean loadModsFromHandshake() {
		return server == null;// && loadGameData == null;
	}
	
	private boolean leaveToMetaLobby() {
		return g.lanClient == null;
	}
	
	public static CoatOfArms getCOA(StrategicPlayerInfo spi, HashMap<Integer, StrategicPlayerInfo> channelPlayers) {
		if (spi.claimedEmpireIndex == -1) {
			return CoatOfArms.spectatorArms();
		}
		if (spi.claimedEmpireCoopOwner) {
			return spi.getArms();
		}
		ArrayList<Integer> idKeys = new ArrayList<Integer>(channelPlayers.keySet());
		Collections.sort(idKeys);
		for (int idKey : idKeys) {
			StrategicPlayerInfo spi2 = channelPlayers.get(idKey);
			if (spi2.claimedEmpireCoopOwner && spi2.claimedEmpireIndex == spi.claimedEmpireIndex) {
				return spi2.getArms();
			}
		}
		return spi.getArms();
	}
	
	public static int getArmsCadence(StrategicPlayerInfo spi, HashMap<Integer, StrategicPlayerInfo> channelPlayers) {
		if (spi.claimedEmpireIndex == -1) {
			return 0;
		}
		ArrayList<Integer> idKeys = new ArrayList<Integer>(channelPlayers.keySet());
		Collections.sort(idKeys);
		if (spi.claimedEmpireCoopOwner) {
			// Are we sharing?
			for (int idKey : idKeys) {
				StrategicPlayerInfo spi2 = channelPlayers.get(idKey);
				if (spi2 != spi && spi2.claimedEmpireIndex == spi.claimedEmpireIndex) {
					return 1;
				}
			}
			return 0;
		} else {
			int cadence = 2; // We're at least at cadence 2 but if there are others before us maybe it's higher.
			for (int idKey : idKeys) {
				StrategicPlayerInfo spi2 = channelPlayers.get(idKey);
				if (spi2 != spi && spi2.claimedEmpireIndex == spi.claimedEmpireIndex && !spi2.claimedEmpireCoopOwner) {
					cadence++;
				}
				if (spi2 == spi) {
					return cadence;
				}
			}
			return cadence;
		}
	}
	
	private void sendHello() {
		g.sendMessage(
				msg("helloStrategic")
				.put("name", g.playerName())
				.put("id", g.playerID())
				.put("arms", armsJSON)
				.put("empireName", empireNameField.getText())
				.put("empireIndex", claimedEmpireIndex())
				.put("version", Server.VERSION));
		helloSent = true;
	}
	
	public boolean processWelcome(JSONObject msg) {
		//channelID = msg.getInt("channelID");
		welcomeReceived = true;
		if (g.lanClient == null || lanIDSet) {
			sendHello();
		}
		if (isInitiating() && !isResumeFromLoaded) {
			sendSizeAndDifficultyUpdate();
		}
		int version = msg.optInt("version", 0);
		if (version != Server.VERSION && g.lanClient != null) {
			leave(false);
			g.showError(_t("host_version_incompatible", version, Server.VERSION));
			return false;
		} else {
			JSONObject info = msg.getJSONObject("info");
			if (!info.has("initiatorID")) {
				g.showError(_t("attempting_to_connect_to_combat"));
				leave(true);
				return false;
			}
			gameName = info.optString("name", "Conquest Game");
			//seed = AGame.cleanSeed(msg.getLong("seed")); // Now provided by hoster instead of server.
			worldID = info.optString("worldID", "0");
			lang = Locale.forLanguageTag(info.getString("lang"));
			initiatorID = info.getInt("initiatorID");
			if (g.lanClient == null && initiatorID == g.playerID() && hoster == null) {
				hoster = new StrategicSetupHoster(g, loadedGame);
				sendSizeAndDifficultyUpdate();
			}
			isResumeFromLoaded = info.getBoolean("isResumeFromLoaded");
			modInfos = new ArrayList<ModInfo>();
			if (loadModsFromHandshake() && info.has("mods")) {
				expansions = Expansion.ofNames(info.getJSONArray("expansions"));
				final JSONArray mods = info.getJSONArray("mods");
				for (int i = 0; i < mods.length(); i++) {
					JSONObject m = mods.getJSONObject(i);
					modInfos.add(new ModInfo(m.getString("id"), m.getLong("checksum")));
				}
				if (isInitiating() && loadGameData == null) { // If loadGameData is set as the server, we already did mod loading.
					doLoadMods();
				}
				if (!isInitiating()) {
					for (ModInfo mi : modInfos) {
						if (!Mod.isAvailable(mi.id, mi.checksum) && !Mod.isCached(mi.id, mi.checksum)) {
							//System.out.println("requesting xload of " + mi.id);
							g.sendMessage(msg("requestModCrossload").put("modID", mi.id).put("playerID", g.playerID()));
						} else {
							//System.out.println(mi.id + " is available");
							mi.isAvailable = true;
						}
					}
				}
			} else if (server != null) {
				for (Mod m : Mod.getEnabledMods()) {
					modInfos.add(new ModInfo(m.id, m.getCachedChecksum()));
				}
				expansions = Expansion.installeds();
			} else {
				expansions = new ArrayList<Expansion>();
			}
		}
		return true;
	}
	
	private boolean allModsReceived() {
		if (isInitiating() || modInfos == null) { return false; }
		for (ModInfo mi : modInfos) {
			if (!mi.isAvailable && (mi.numChunks == 0 || mi.chunks.size() != mi.numChunks)) {
				return false;
			}
		}
		return true;
	}
	
	private void doLoadMods() {
		if (modsLoaded) { return; }
		modsLoaded = true;
		final ArrayList<String> newIDs = new ArrayList<String>();
		HashMap<String, Long> checksumsToUse = new HashMap<String, Long>();
		for (ModInfo mi : modInfos) {
			newIDs.add(mi.id);
			checksumsToUse.put(mi.id, mi.checksum);
		}
		Mod.overrideModsToLoad(newIDs, isInitiating() ? null : checksumsToUse, expansions);
		mrpd = new ModReloadProgressDialog(true, new Runnable() {
			@Override
			public void run() {
				for (String id : newIDs) {
					Mod m = Mod.getById(id);
					if (m == null || !m.isCurrentlyEnabled()) {
						unableToLoadCorrectMods = true;
						return;
					}
				}
				size = MapSize.ofName(size.name);
				difficulty = DifficultyLevel.ofName(difficulty.name);
				seaLevelSetting = SeaLevelSetting.ofName(seaLevelSetting.name);
				monsterity = MonsterSetting.ofName(monsterity.name);
				heroFrequency = FrequencySetting.ofName(heroFrequency.name);
				incidentFrequency = FrequencySetting.ofName(incidentFrequency.name);
				techSpeed = TechSpeedSetting.ofName(techSpeed.name);
			}
		}, new Runnable() {
			@Override
			public void run() {
				leave(false);
				g.showError(_t("unable_to_load_mp_mods"));
			}
		});
	}
	
	private boolean pollMessageAndProcess() {
		try {
			JSONObject msg = g.pollMessage();
			if (msg != null) {
				if (msg.getString("type").equals("serverReject")) {
					g.showError(_t("server_rejected_" + msg.getString("reason")));
					leave(true);
					return false;
				}
				if (msg.getString("type").equals("assignID")) {
					g.lanID = msg.getInt("playerID");
					lanIDSet = true;
				} else if (msg.getString("type").equals("welcome")) {
					if (!processWelcome(msg)) {
						return false;
					}
				}
				if (hoster != null) {
					hoster.processMessage(msg);
				}
				if (msg.getString("type").equals("frame")) {
					JSONArray frameMembers = msg.getJSONArray("members");
					if (initiatorID != 0 && g.lanClient == null) {
						boolean foundInitiator = false;
						for (int i = 0; i < frameMembers.length(); i++) {
							if (frameMembers.getInt(i) == initiatorID) {
								foundInitiator = true;
							}
						}
						if (!foundInitiator) {
							g.showError(_t("host_left"));
							leave(false);
							return false;
						}
					}
					
					JSONArray frameMessages = msg.getJSONArray("messages");
					for (int i = 0; i < frameMessages.length(); i++) {
						JSONObject fm = frameMessages.getJSONObject(i);
						if (fm.getString("type").equals("helloStrategic")) {
							if (!fm.has("version") || fm.getInt("version") != Server.VERSION) {
								g.sendMessage(msg("chat").put("text", "!!! Your version of the game is not compatible with the other player's. Please make sure you have both updated to the newest version of the game.").put("id", g.playerID()));
								leave(false);
								g.showError(_t("game_version_incompatible"));
								return false;
							}
						}
						if (fm.getString("type").equals("chat")) {
							StrategicPlayerInfo pi = channelPlayers.get(fm.getInt("id"));
							if (pi != null) {
								chat.add(new StrategicChatMsg(pi, pi.name + ": " + fm.getString("text"), new DateTime(fm.getLong("t"), DateTimeZone.UTC), getCOA(pi, channelPlayers), getArmsCadence(pi, channelPlayers)));
							}
						}
						if (fm.getString("type").equals("gameDataChunk") && !isInitiating()) {
							numGameDataChunks = fm.getInt("total");
							int index = fm.getInt("index");
							if (index == gameDataChunks.size()) {
								gameDataChunks.add(fm.getString("data"));
							}
						}
						if (fm.getString("type").equals("requestModCrossload") && isInitiating() && modInfos != null) {
							//System.out.println("req xload " + fm.getString("modID"));
							for (ModInfo mi : modInfos) {
								if (mi.id.equals(fm.getString("modID"))) {
									//System.out.println("set requested");
									mi.isRequested = true;
									hoster.resetModChunksSent(mi.id);
								}
							}
						}
						if (fm.getString("type").equals("modChunk") && !isInitiating()) {
							String id = fm.getString("id");
							long checksum = fm.getLong("checksum");
							ModInfo mi = null;
							for (ModInfo mi2 : modInfos) {
								if (mi2.id.equals(id) && mi2.checksum == checksum) {
									mi = mi2;
									break;
								}
							}
							if (mi == null) {
								throw new RuntimeException(_t("unexpected_mod_data", id));
							}
							
							if (!mi.isAvailable) {
								if (mi.chunks == null) {
									mi.chunks = new ArrayList<String>();
									mi.numChunks = fm.getInt("total");
								}

								int index = fm.getInt("index");
								if (index == mi.chunks.size()) {
									mi.chunks.add(fm.getString("data"));
								}
								if (mi.chunks.size() == mi.numChunks) {
									Mod.cacheModF(mi.id, mi.checksum, mi.chunks);
									mi.isAvailable = true;
								}
							}
						}
						if (fm.getString("type").equals("hosterUpdate")) {
							JSONArray players = fm.getJSONArray("players");
							TreeMap<Integer, StrategicPlayerInfo> oldChannelPlayers = new TreeMap<>(channelPlayers);
							channelPlayers.clear();
							for (int j = 0; j < players.length(); j++) {
								JSONObject p = players.getJSONObject(j);
								StrategicPlayerInfo spi = new StrategicPlayerInfo(p.getInt("id"), p.getString("name"), p.optString("empireName", null), p.getJSONObject("arms"), p.optJSONObject("hero"));
								spi.ready = p.getBoolean("ready");
								spi.claimedEmpireIndex = p.getInt("claimedEmpireIndex");
								spi.claimedEmpireCoopOwner = p.getBoolean("claimedEmpireCoopOwner");
								channelPlayers.put(p.getInt("id"), spi);
								if (spi.id == g.playerID()) {
									readySent = spi.ready;
								}
								if (!oldChannelPlayers.containsKey(spi.id)) {
									chat.add(new StrategicChatMsg(spi, _t("xxx_JOINED_xxx", spi.name)/* + " : " + spi.id*/, DateTime.now(), getCOA(spi, channelPlayers), getArmsCadence(spi, channelPlayers)));
								}
							}
							for (StrategicPlayerInfo spi : oldChannelPlayers.values()) {
								if (!channelPlayers.containsKey(spi.id)) {
									chat.add(new StrategicChatMsg(spi, _t("xxx_LEFT_xxx", spi.name)/* + " : " + spi.id*/, DateTime.now(), getCOA(spi, channelPlayers), getArmsCadence(spi, channelPlayers)));
								}
							}
							difficulty = DifficultyLevel.ofName(fm.getString("difficulty"));
							size = MapSize.ofName(fm.getString("size"));
							seaLevelSetting = SeaLevelSetting.ofName(fm.getString("seaLevelSetting"));
							monsterity = MonsterSetting.ofName(fm.getString("monsterity"));
							heroFrequency = FrequencySetting.ofName(fm.getString("heroFrequency"));
							incidentFrequency = FrequencySetting.ofName(fm.getString("incidentFrequency"));
							techSpeed = TechSpeedSetting.ofName(fm.getString("techSpeed"));
							startingTechTier = fm.getInt("startingTechTier");
							strategicRapidCommands = fm.getBoolean("strategicRapidCommands");
							allowDirectControl = fm.getBoolean("allowDirectControl");
							anyoneCanPause = fm.getBoolean("anyoneCanPause");
							allowEmpireCoop = fm.getBoolean("allowEmpireCoop");
							seed = AGame.cleanSeed(fm.getLong("seed"));
							seedF.setText("" + seed);
							//System.out.println("hosterUpdate < " + seed);
							toggles.clear();
							JSONArray ts = fm.getJSONArray("toggles");
							for (int j = 0; j < ts.length(); j++) {
								toggles.add(ConquestToggle.valueOf(ts.getString(j)));
							}
						}
					}
				}

				return !canStart();
			}

			return false;
		} catch (Exception e) {
			leave(true);
			g.reportError(_t("setup_failed") + "\n" + e.getMessage(), e, null, false, false);
			return false;
		}
	}
	
	private void leave(boolean fully) {
		if (leaveToMetaLobby()) {
			if (fully) {
				g.disconnectClient();
				g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
			} else {
				g.s = new MetaLobbyScreen(g, server);
				g.sendMessage(msg("changeChannel").put("id", 0));
				g.helloSent = false;
			}
		} else {
			g.lanClient.close();
			g.lanClient = null;
			if (server != null) {
				server.close();
				g.lanServer = null;
			}
			g.s = new MainMenu(g, MainMenu.Submenu.MULTIPLAYER);
		}
	}
	
	private boolean atLeastOneClaimedEmpire() {
		for (StrategicPlayerInfo pi : channelPlayers.values()) {
			if (pi.claimedEmpireIndex != -1 && pi.claimedEmpireIndex < size.empires) {
				return true;
			}
		}
		return false;
	}
	
	private boolean canSendReady() {
		if (isResumeFromLoaded && loadedGame == null) {
			return false;
		}
		if (!isResumeFromLoaded && !isSpectatingOrJoiningCoopEmpire() && empireNameField.getText().isEmpty()) { return false; }
		if (!isResumeFromLoaded && !isSpectatingOrJoiningCoopEmpire() && EHeroes.it.enabled && heroFrequency.frequencyMultiplier != 0 && hero == null) { return false; }
		return atLeastOneClaimedEmpire() && channelPlayers.size() > 1;
	}
	
	private boolean canStart() {
		if (mrpd != null || unableToLoadCorrectMods) {
			return false;
		}
		for (StrategicPlayerInfo spi : channelPlayers.values()) {
			if (!spi.ready) { return false; }
		}
		return canSendReady();
	}
	
	private JSONArray toggleArray() {
		JSONArray a = new JSONArray();
		for (ConquestToggle ct : toggles) {
			a.put(ct.name());
		}
		return a;
	}
	
	private void sendReady() {
		readySent = true;
		// Also doubles as a hello.
		if (isResumeFromLoaded) {
			g.sendMessage(msg("strategicResumeReady")
					.put("id", g.playerID()).put("name", g.playerName()));
		} else {
			JSONObject msg = msg("strategicReady")
					.put("id", g.playerID())
					.put("name", g.playerName())
					.put("empireName", empireNameField.getText())
					.put("arms", armsJSON);
			if (isInitiating()) {
				msg
						.put("difficulty", difficulty.name)
						.put("size", size.name)
						.put("seaLevelSetting", seaLevelSetting.name)
						.put("monsterity", monsterity.name)
						.put("heroFrequency", heroFrequency.name)
						.put("incidentFrequency", incidentFrequency.name)
						.put("techSpeed", techSpeed.name)
						.put("startingTechTier", startingTechTier)
						.put("strategicRapidCommands", strategicRapidCommands)
						.put("allowDirectControl", allowDirectControl)
						.put("anyoneCanPause", anyoneCanPause)
						.put("allowEmpireCoop", allowEmpireCoop)
						.put("seed", seed)
						.put("toggles", toggleArray());
				//System.out.println("strategicReady " + seed);
			}
			g.sendMessage(msg);
		}
	}
	
	private void sendSizeAndDifficultyUpdate() {
		g.sendMessage(msg("strategicSizeAndDifficulty")
				.put("difficulty", difficulty.name)
				.put("size", size.name)
				.put("seaLevelSetting", seaLevelSetting.name)
				.put("monsterity", monsterity.name)
				.put("heroFrequency", heroFrequency.name)
				.put("incidentFrequency", incidentFrequency.name)
				.put("techSpeed", techSpeed.name)
				.put("startingTechTier", startingTechTier)
				.put("strategicRapidCommands", strategicRapidCommands)
				.put("allowDirectControl", allowDirectControl)
				.put("anyoneCanPause", anyoneCanPause)
				.put("allowEmpireCoop", allowEmpireCoop)
				.put("seed", seed)
				.put("toggles", toggleArray()));
		//System.out.println("strategicSizeAndDifficulty " + seed);
	}
	
	public final void sendEmpireNameAndArmsUpdate() {
		JSONObject m = msg("strategicEmpireAndArms")
				.put("id", g.playerID())
				.put("empireName", empireNameField.getText())
				.put("arms", armsJSON);
		if (hero != null) {
			m.put("hero", hero.toJSON());
		}
		g.sendMessage(m);
	}
	
	private void startGame() {
		g.sendMessage(msg("sealChannel"));
		if (isResumeFromLoaded) {
			loadedGame.mpClient = client;
			loadedGame.mpServer = server;
			for (Empire e : loadedGame.map.empires) {
				e.playerControlled = false;
				e.playerNames.clear();
			}
			ArrayList<Integer> idKeys = new ArrayList<Integer>(channelPlayers.keySet());
			Collections.sort(idKeys);
			for (int idKey : idKeys) {
				StrategicPlayerInfo spi = channelPlayers.get(idKey);
				if (spi.claimedEmpireIndex == -1) {
					spi.empire = null;
					spi.empireName = _t("Spectator");
					spi.armsJSON = CoatOfArms.spectatorArms().toJSON();
				} else {
					spi.empire = loadedGame.map.empires.get(spi.claimedEmpireIndex);
					spi.empireName = spi.empire.name;
					spi.empire.playerControlled = true;
					spi.empire.playerNames.add(spi.name);
				}
				if (spi.id == g.playerID()) {
					loadedGame.player = spi.empire;
				}
			}
			loadedGame.channelPlayers = channelPlayers;
			loadedGame.speedVoters = new HashMap<Integer, CampaignWorld.Speed>();
			for (int id : loadedGame.channelPlayers.keySet()) {
				loadedGame.speedVoters.put(id, CampaignWorld.Speed.STOP);
			}
			loadedGame.chatMessages.addAll(chat);
			StrategicScreen ss = new StrategicScreen(g, loadedGame);
			g.s = ss;
			ss.autosaveIODir = StrategicScreen.autosave(null, loadedGame);
			loadedGame.initStoredStateChain();
		} else {
			// Clean up bubbles in the claimed empire indexes.
			int loopUntil = size.empires;
			for (int i = 0; i < loopUntil; i++) {
				boolean isBubble = true;
				for (StrategicPlayerInfo spi : channelPlayers.values()) {
					if (spi.claimedEmpireIndex == i) {
						isBubble = false;
						break;
					}
				}
				if (isBubble) {
					// move everything down one
					for (StrategicPlayerInfo spi : channelPlayers.values()) {
						if (spi.claimedEmpireIndex > i) {
							spi.claimedEmpireIndex--;
						}
					}
					i--;
					loopUntil--;
				}
			}
			int myPlayerIndex = -1;
			ArrayList<StrategicSetupInfo> setupInfos = new ArrayList<StrategicSetupInfo>();
			ArrayList<Integer> idKeys = new ArrayList<Integer>(channelPlayers.keySet());
			Collections.sort(idKeys);
			HashMap<Integer, StrategicSetupInfo> idToSSI = new HashMap<Integer, StrategicSetupInfo>();
			for (int idKey : idKeys) {
				StrategicPlayerInfo spi = channelPlayers.get(idKey);
				if (spi.claimedEmpireIndex != -1) {
					if (!idToSSI.containsKey(spi.claimedEmpireIndex) && spi.claimedEmpireCoopOwner) {
						StrategicSetupInfo ssi = new StrategicSetupInfo(spi.empireName, spi.getArms(), spi.getHero(), spi.name, spi);
						setupInfos.add(ssi);
						idToSSI.put(spi.claimedEmpireIndex, ssi);
					}
				} else {
					spi.empireName = _t("Spectator");
					spi.armsJSON = CoatOfArms.spectatorArms().toJSON();
				}
			}
			// Fill up names
			for (int idKey : idKeys) {
				StrategicPlayerInfo spi = channelPlayers.get(idKey);
				if (idToSSI.containsKey(spi.claimedEmpireIndex) && !idToSSI.get(spi.claimedEmpireIndex).playerNames.contains(spi.name)) {
					idToSSI.get(spi.claimedEmpireIndex).playerNames.add(spi.name);
					idToSSI.get(spi.claimedEmpireIndex).strategicPlayerInfos.add(spi);
					spi.armsJSON = idToSSI.get(spi.claimedEmpireIndex).getArms().toJSON();
				}
			}
			StrategicPlayerInfo mySPI = channelPlayers.get(g.playerID());
			if (mySPI != null) {
				myPlayerIndex = setupInfos.indexOf(idToSSI.get(mySPI.claimedEmpireIndex));
			}
			CampaignWorld cw = new CampaignWorld(worldID, seed, ConquestToggle.allowed(toggles), size, difficulty, g, setupInfos, myPlayerIndex, lang, seaLevelSetting, monsterity, heroFrequency, incidentFrequency, techSpeed, startingTechTier, strategicRapidCommands, allowDirectControl, anyoneCanPause, client, server, channelPlayers);
			cw.gameName = gameName;
			cw.chatMessages.addAll(chat);
			g.s = new WorldGenScreen(cw, g);
		}
	}
	
	private void sendChat() {
		g.sendMessage(msg("chat").put("id", g.playerID()).put("text", chatField.getText()));
		chatField.setText("");
	}
	
	public boolean update(Input in, int ms) {
		if (!helloSent && (g.lanClient == null || lanIDSet)) { sendHello(); }
		
		if (welcomeReceived && isInitiating() && hoster != null && channelPlayers.size() > 1 && g.outQueueSize() < 8) {
			if (hoster.gameDataChunksSent < numGameDataChunks) {
				g.sendMessage(msg("gameDataChunk").put("index", hoster.gameDataChunksSent).put("total", numGameDataChunks).put("data", gameDataChunks.get(hoster.gameDataChunksSent)));
				hoster.gameDataChunksSent++;
			} else {
				for (ModInfo mi : modInfos) {
					if (!mi.isRequested) { continue; }
					if (mi.numChunks == 0) {
						try {
							mi.chunks = Mod.getById(mi.id).getChunks();
						} catch (IOException e) {
							g.showError(_t("unable_to_crossload_mod_x", mi.id));
							leave(true);
							return true;
						}
						mi.numChunks = mi.chunks.size();
					}
					if (hoster.getModChunksSent(mi.id) < mi.numChunks) {
						g.sendMessage(msg("modChunk").put("id", mi.id).put("checksum", mi.checksum).put("index", hoster.getModChunksSent(mi.id)).put("total", mi.numChunks).put("data", mi.chunks.get(hoster.getModChunksSent(mi.id))));
						hoster.incrementModChunksSent(mi.id);
						break;
					}
				}
			}
		}

		while (pollMessageAndProcess()) {}
		
		if (allModsReceived()) {
			doLoadMods();
		}
		
		if (canStart()) {
			startGame();
			return true;
		}
		return false;
	}
	
	@Override
	public void input(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		/*if (hoster != null) {
			System.out.println("Hoster");
			for (Map.Entry<Integer, StrategicPlayerInfo> e : hoster.channelPlayers.entrySet()) {
				System.out.println(e.getKey() + ": " + e.getValue().id + " " + e.getValue().name);
			}
		}*/
		
		if (!g.isConnected()) {
			int oldID = g.playerID();
			if (g.completelyReconnectClient()) {
				g.s = new ResumeScreen(g, this, oldID, 1, gameName);
			} else {
				g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
				g.showError(_t("Connection_lost"));
			}
			return;
		}
		
		if (update(in, ms)) { return; }
		
		if (unableToLoadCorrectMods) {
			if (Keys.check(in, "ESCAPE") || Keys.check(in, "ENTER")) {
				leave(false);
			}
			return;
		}
		
		if (pleaseConfirmLeave) {
			if (Keys.check(in, "ESCAPE")) {
				pleaseConfirmLeave = false;
			} else if (Keys.check(in, "ENTER")) {
				leave(false);
			}
			return;
		}
		
		chatScrollBar.stickToBottom = true;
		
		ScreenMode sm = in.mode();
		
		int leftSize = (int) (sm.width * 0.4);
		
		int x = leftSize + MyDraw.SIDE_CLEARANCE + MyDraw.UI_SPACING;
		int y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		int w = sm.width - x - MyDraw.SIDE_CLEARANCE;
		int h = sm.height - MyDraw.TOP_BAR_H - MyDraw.SIDE_CLEARANCE - MyDraw.UI_SPACING - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING;	
		
		boolean showPlayers = sm.width > 1000;
		
		if (isResumeFromLoaded) {
			if (loadedGame != null) {
				empiresScrollBar.tick(in, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_H + MyDraw.UI_SPACING, leftSize - MyDraw.SIDE_CLEARANCE, sm.height - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING - MyDraw.SIDE_CLEARANCE);
			}
		} else {
			empiresScrollBar.tick(in, empireSBRect.x, empireSBRect.y, empireSBRect.w, empireSBRect.h);
		}
		
		if (settingsSBR.x > 0) {
			settingsSB.tick(in, settingsSBR.x, settingsSBR.y, settingsSBR.w, settingsSBR.h);
		}
		
		if (heroFrequency.frequencyMultiplier == 0) {
			showHeroSelector = false;
		}
		
		if (EHeroes.it.enabled && showHeroSelector && !readySent && availableHeroes != null) {
			heroesScrollBar.tick(in, heroesSBR.x, heroesSBR.y, heroesSBR.w, heroesSBR.h);
		} else {
			chatScrollBar.tick(in, x, y, w - (showPlayers ? 200 + MyDraw.UI_SPACING : 0), h);
			if (showPlayers) {
				playerScrollBar.tick(in, x + w - 200, y, 200, h);
			}
		}
		
		if (mrpd != null) {
			ModReloadProgressDialog myMrpd = mrpd;
			if (mrpd.tick(in) && myMrpd == mrpd) {
				mrpd = null;
			}
			return;
		}
		
		if (unableToLoadCorrectMods) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				leave(false);
			}
			return;
		}
		
		if (availableHeroes == null && EHeroes.it.enabled && !isResumeFromLoaded && lang != null) {
			availableHeroes = Hero.getStarters(lang, AGame.ANIM_R);
			heroRenderer = new HeroRenderer(this, null);
		}
		
		if (isResumeFromLoaded && !isInitiating() && gameDataChunks.size() == numGameDataChunks && loadedGame == null && modsLoaded) {
			StringBuilder sb = new StringBuilder(numGameDataChunks * GAME_DATA_CHUNK_MAX_SIZE);
			for (String chunk : gameDataChunks) {
				sb.append(chunk);
			}
			String decompressed = Compression.decompressFromString(sb.toString());
			sb = null; // To free memory.
			loadGameData = new JSONObject(decompressed);
			try {
				loadedGame = new CampaignWorld(loadGameData.getJSONObject("world"), g, /* multiplayer */ true, new JSONObjectInPipe(loadGameData));
			} catch (IOException e) {
				g.reportError("Unable_to_load_game", e, null, true);
				return;
			}
		}
		
		if (in.keyPressed("TAB")) {
			empireNameField.focus = chatField.focus;
			chatField.focus = !chatField.focus;
		}
		
		if (readySent) {
			empireNameField.focus = false;
			chatField.focus = true;
		}
		
		if (Keys.check(in, "ESCAPE")) {
			if (isInitiating()) {
				pleaseConfirmLeave = true;
			} else {
				leave(false);
			}
			return;
		}
		
		if (seedF.focus) {
			if (readySent) {
				seedF.setText("" + seed);
				seedF.focus = false;
			} else {
				seedF.input(in, cursor, click, ms);
				if (in.keyDown("ENTER")) {
					updateSeed();
					seedF.focus = false;
				}
			}
		} else if (chatField.focus) {
			chatField.input(in, cursor, click, ms);
			if (in.keyDown("ENTER") && !chatField.getText().isEmpty()) {
				sendChat();
			}
		} else if (!readySent) {
			String prevName = empireNameField.getText();
			empireNameField.input(in, cursor, click, ms);
			if (!empireNameField.getText().isEmpty() && !empireNameField.getText().equals(prevName)) {
				sendEmpireNameAndArmsUpdate();
			}
		}
		
		if (canStart()) {
			startGame();
		}
	}
	
	private ScrollBar settingsSB = new ScrollBar();
	private IntRect settingsSBR = new IntRect();
	
	private void updateSeed() {
		try {
			seed = AGame.cleanSeed(Long.parseLong(seedF.getText()));
			sendSizeAndDifficultyUpdate();
		} catch (Exception e) {}
		seedF.setText("" + seed);
	}
	
	private ScrollElementAdapter<Object> settingsAdapter = new ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int extraFrequencies = 0;
			if (EHeroes.it.enabled) {
				extraFrequencies++;
			}
			if (!Loadable.all(IncidentType.class).isEmpty()) {
				extraFrequencies++;
			}
			return Math.max(
				(5 + ConquestToggle.values().length + Loadable.all(MapSize.class).size() + Loadable.all(MapSize.class).size() + Loadable.all(TechSpeedSetting.class).size()) * (MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING) + AGame.BIG_FOUNT.lineHeight * 3 + MyDraw.UI_SPACING * 3,
				(Loadable.all(DifficultyLevel.class).size() + 1 + Loadable.all(MonsterSetting.class).size() + Loadable.all(FrequencySetting.class).size() * extraFrequencies + Tech.getMaxTier() - Tech.getMinTier()) * (MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING) + AGame.BIG_FOUNT.lineHeight * (3 + extraFrequencies) + MyDraw.UI_SPACING * (2 + extraFrequencies)
			);
		}

		@Override
		public void draw(Object t, MyDraw d, int x, int y, int w) {
			w -= MyDraw.UI_SPACING;
			int colW = (w - MyDraw.UI_SPACING) / 2;
			int col2X = x + colW + MyDraw.UI_SPACING;
			int y1 = y;
					
			d.text(_t("Settings"), AGame.BIG_FOUNT, x, y1);
			y1 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			
			d.text(_t("Seed"), AGame.FOUNT, x, y1 + MyDraw.PANEL_INSET);
			int seedW = (int) d.textSize(_t("Seed"), AGame.FOUNT).x;
			int seedFW = (int) d.textSize("999999X", AGame.FOUNT).x + MyDraw.PANEL_INSET * 2;
			seedF.render(x + seedW + MyDraw.UI_SPACING, y1, seedFW, d);
			d.iconToggle(x + seedW + MyDraw.UI_SPACING * 2 + seedFW, y1, editIcon, new Runnable() {
				@Override
				public void run() {
					if (seedF.focus) {
						updateSeed();
					}
					seedF.focus = !seedF.focus;
				}
			}, seedF.focus, true);
			d.tooltip(x + seedW + MyDraw.UI_SPACING * 2 + seedFW, y1, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Edit_Seed"));
			
			y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			d.toggle(x, y1, colW, _t("Empire_Coop"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					allowEmpireCoop = !allowEmpireCoop;
					sendSizeAndDifficultyUpdate();
				}
			}, allowEmpireCoop, !readySent);
			d.tooltip(x, y1, colW, MyDraw.BUTTON_H, _t("Empire_Coop_tooltip"));
			y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			
			d.toggle(x, y1, colW, _t("Rapid_Commands"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					strategicRapidCommands = !strategicRapidCommands;
					sendSizeAndDifficultyUpdate();
				}
			}, strategicRapidCommands, !readySent);
			d.tooltip(x, y1, colW, MyDraw.BUTTON_H, _t("Rapid_Commands_tooltip"));
			y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			
			d.toggle(x, y1, colW, _t("Allow_Direct_Ship_Control"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					allowDirectControl = !allowDirectControl;
					sendSizeAndDifficultyUpdate();
				}
			}, allowDirectControl, !readySent);
			d.tooltip(x, y1, colW, MyDraw.BUTTON_H, _t("Allow_Direct_Ship_Control_tooltip"));
			y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			
			d.toggle(x, y1, colW, _t("Anyone_Can_Pause"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					anyoneCanPause = !anyoneCanPause;
					sendSizeAndDifficultyUpdate();
				}
			}, anyoneCanPause, !readySent);
			d.tooltip(x, y1, colW, MyDraw.BUTTON_H, _t("Anyone_Can_Pause_tooltip"));
			y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

			for (final ConquestToggle ct : ConquestToggle.values()) {
				d.toggle(x, y1, colW, ct.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						if (toggles.contains(ct)) {
							toggles.remove(ct);
						} else {
							toggles.add(ct);
						}
						sendSizeAndDifficultyUpdate();
					}
				}, toggles.contains(ct) && ct.available(toggles), !readySent && ct.available(toggles));
				d.tooltip(x, y1, colW, MyDraw.BUTTON_H, ct.getTooltip());
				y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}

			y1 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;

			d.text(_t("setup_mapsize"), AGame.BIG_FOUNT, x, y1);
			y1 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (final MapSize sz : Loadable.all(MapSize.class)) {
				d.toggle(x, y1, colW, sz.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						size = sz;
						sendSizeAndDifficultyUpdate();
					}
				}, size.name.equals(sz.name), !readySent);
				y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y1 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;

			d.text(_t("setup_sealevel"), AGame.BIG_FOUNT, x, y1);
			y1 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (final SeaLevelSetting sls : Loadable.all(SeaLevelSetting.class)) {
				d.toggle(x, y1, colW, sls.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						seaLevelSetting = sls;
						sendSizeAndDifficultyUpdate();
					}
				}, seaLevelSetting.name.equals(sls.name), !readySent);
				y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y1 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;

			d.text(_t("setup_techspeed"), AGame.BIG_FOUNT, x, y1);
			y1 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (final TechSpeedSetting tss : Loadable.all(TechSpeedSetting.class)) {
				d.toggle(x, y1, colW, tss.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						techSpeed = tss;
						sendSizeAndDifficultyUpdate();
					}
				}, techSpeed.name.equals(tss.name), !readySent);
				y1 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y1 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;

			if (!Mod.getEnabledMods().isEmpty()) {
				d.text(_t("Mods"), AGame.BIG_FOUNT, x, y1);
				y1 += AGame.BIG_FOUNT.lineHeight;
				for (Mod m : Mod.getEnabledMods()) {
					d.text(m.getName(), AGame.FOUNT, x, y1);
					y1 += AGame.FOUNT.lineHeight;
				}
			}

			int y2 = y;
			d.text(_t("setup_difficulty"), AGame.BIG_FOUNT, col2X, y2);
			y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (final DifficultyLevel dl : Loadable.all(DifficultyLevel.class)) {
				d.toggle(col2X, y2, colW, dl.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						difficulty = dl;
						sendSizeAndDifficultyUpdate();
					}
				}, difficulty.name.equals(dl.name), !readySent);
				d.tooltip(col2X, y2, colW, MyDraw.BUTTON_H, dl.getDesc());
				y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y2 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			
			d.text(_t("setup_monsterity"), AGame.BIG_FOUNT, col2X, y2);
			y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (final MonsterSetting ms : Loadable.all(MonsterSetting.class)) {
				d.toggle(col2X, y2, colW, ms.getName(), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						monsterity = ms;
						sendSizeAndDifficultyUpdate();
					}
				}, monsterity.name.equals(ms.name), !readySent);
				y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y2 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			
			if (EHeroes.it.enabled) {
				d.text(_t("setup_heroFrequency"), AGame.BIG_FOUNT, col2X, y2);
				y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
				for (final FrequencySetting fs : Loadable.all(FrequencySetting.class)) {
					d.toggle(col2X, y2, colW, fs.getName(), null, new InputRunnable() {
						@Override
						public void run(Input in) {
							heroFrequency = fs;
							sendSizeAndDifficultyUpdate();
						}
					}, heroFrequency.name.equals(fs.name), !readySent);
					y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
				}
				y2 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			}
			
			if (!Loadable.all(IncidentType.class).isEmpty()) {
				d.text(_t("setup_incidentFrequency"), AGame.BIG_FOUNT, col2X, y2);
				y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
				for (final FrequencySetting fs : Loadable.all(FrequencySetting.class)) {
					d.toggle(col2X, y2, colW, fs.getName(), null, new InputRunnable() {
						@Override
						public void run(Input in) {
							incidentFrequency = fs;
							sendSizeAndDifficultyUpdate();
						}
					}, incidentFrequency.name.equals(fs.name), !readySent);
					y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
				}
				y2 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			}

			d.text(_t("setup_starttier"), AGame.BIG_FOUNT, col2X, y2);
			y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
			for (int tier = Tech.getMinTier() - 1; tier <= Tech.getMaxTier(); tier++) {
				final int tierf = tier;
				d.toggle(col2X, y2, colW, _t("tier_x", tier + 1), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						startingTechTier = tierf;
						sendSizeAndDifficultyUpdate();
					}
				}, startingTechTier == tier, !readySent);
				y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			y2 += MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			y = StrictMath.max(y1, y2);
		}
	};

	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {		
		d.rect(AGame.SKY, 0, 0, sm.width, sm.height);
		d.drawBG(MyDraw.SCREEN_BG, sm);
		
		d.drawTopBar(sm);
		
		if (server != null) {
			d.text(yourIP, AGame.FOUNT, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET + 2);
		} else {
			d.text(g.isReconnecting() ? _t("Reconnecting_") : (g.ping() + " ping"), AGame.FOUNT, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET + 2);
		}

		int bw = d.bw(_t("Leave"));
		int x2 = sm.width - bw - MyDraw.SIDE_CLEARANCE;
		d.button(x2, MyDraw.TOP_BAR_INSET, bw, _t("Leave"), null, new InputRunnable() {
			@Override
			public void run(Input in) {
				if (isInitiating()) {
					pleaseConfirmLeave = true;
				} else {
					leave(false);
				}
			}
		}, true);//!(isInitiating() && readySent));
		
		bw = d.bw(_t("Ready"));
		x2 -= bw + MyDraw.BUTTON_SPACING;
		d.button(x2, MyDraw.TOP_BAR_INSET, bw, _t("Ready"), null, new InputRunnable() {
			@Override
			public void run(Input in) {
				sendReady();
			}
		}, !readySent && canSendReady());
		if (!isResumeFromLoaded && !isSpectatingOrJoiningCoopEmpire() && EHeroes.it.enabled && heroFrequency.frequencyMultiplier != 0 && hero == null) {
			d.tooltip(x2, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H, _t("select_hero_first"));
		}
		
		int leftSize = (int) (sm.width * 0.4);
		int x = MyDraw.SIDE_CLEARANCE;
		int y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		int w = leftSize - MyDraw.SIDE_CLEARANCE;
		
		settingsSBR.x = 0;
		
		if (isResumeFromLoaded && loadedGame == null && gameDataChunks.size() != numGameDataChunks) {
			d.text(_t("Receiving_game_data_"), AGame.FOUNT, x, y);
			y += AGame.FOUNT.height + MyDraw.UI_SPACING;
			if (numGameDataChunks != -1) {
				d.progressBar(x, y, w, 1.0 * gameDataChunks.size() / numGameDataChunks);
			}
		} else if (!isInitiating() && !allModsReceived()) {
			d.text(_t("Crossloading_mods_"), AGame.FOUNT, x, y);
			y += AGame.FOUNT.height + MyDraw.UI_SPACING;
			if (modInfos != null) {
				for (ModInfo mi : modInfos) {
					if (mi.isAvailable) { continue; }
					d.text(mi.id, AGame.FOUNT, x, y);
					if (mi.numChunks > 0) {
						d.progressBar(x + w / 2, y, w / 2, 1.0 * mi.chunks.size() / mi.numChunks);
					}
					y += Math.max(AGame.FOUNT.lineHeight, MyDraw.PROGRESS_BAR_H) + MyDraw.BUTTON_SPACING;
				}
			}
		} else if (isResumeFromLoaded) {
			if (loadedGame != null) {
				int h = sm.height - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING - MyDraw.SIDE_CLEARANCE;
				ArrayList<Empire> empires = new ArrayList<Empire>(loadedGame.map.empires);
				empires.add(0, null);
				empiresScrollBar.draw(d, x, y, w, h, empires, empiresAdapter);
			} else {
				d.text(_t("Loading_game_data_"), AGame.FOUNT, x, y);
			}
		} else {
			int colW = (w - MyDraw.UI_SPACING) / 2;

			if (isInitiating()) {
				int col2X = x + colW + MyDraw.UI_SPACING;
				d.toggle(x, y, colW, _t("Setup"), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						showSettingsTab = false;
					}
				}, !showSettingsTab, true);
				
				d.toggle(col2X, y, colW, _t("Settings"), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						showSettingsTab = true;
					}
				}, showSettingsTab, true);
				
				y += MyDraw.BUTTON_H + MyDraw.UI_SPACING;
				
				if (showSettingsTab) {
					if (settingsAdapter.getHeight(null, d, w) > sm.height - y - MyDraw.SIDE_CLEARANCE) {
						ArrayList<Object> dummy = new ArrayList<Object>();
						dummy.add(null);
						settingsSB.drawNaked(d, x, y, w, sm.height - y - MyDraw.SIDE_CLEARANCE, dummy, settingsAdapter);
						settingsSBR.x = x; settingsSBR.y = y; settingsSBR.w = w; settingsSBR.h = sm.height - y - MyDraw.SIDE_CLEARANCE;
					} else {
						settingsAdapter.draw(null, d, x, y, w);
					}
				}
			} else {
				String infoT = 
						_t("Seed") + ": " + seed + "\n" +
						_t("Map_size_") + size.getName() + "\n" +
						_t("Sea_level_") + seaLevelSetting.getName() + "\n" +
						_t("Tech_speed_") + techSpeed.getName() + "\n" +
						_t("Difficulty_") + difficulty.getName() + "\n" +
						_t("Monsterity_") + monsterity.getName() + "\n";
				if (EHeroes.it.enabled) {
					infoT += _t("Hero_Frequency_") + heroFrequency.getName() + "\n";
				}
				if (!Loadable.all(IncidentType.class).isEmpty()) {
					infoT += _t("Incident_Frequency_") + incidentFrequency.getName() + "\n";
				}
				infoT += _t("Starting_tech_tier_") + _t("tier_x", (startingTechTier + 1));
				d.text(infoT, AGame.BIG_FOUNT, x, y);
				StringBuilder sb = new StringBuilder();
				int n = 0;
				if (allowEmpireCoop) {
					sb.append(_t("Empire_Coop")).append("\n");
					n++;
				}
				if (strategicRapidCommands) {
					sb.append(_t("Rapid_Commands")).append("\n");
					n++;
				}
				if (allowDirectControl) {
					sb.append(_t("Allow_Direct_Ship_Control")).append("\n");
					n++;
				}
				if (anyoneCanPause) {
					sb.append(_t("Anyone_Can_Pause")).append("\n");
					n++;
				}
				for (ConquestToggle ct : ConquestToggle.allowed(toggles)) {
					sb.append(ct.getName()).append("\n");
					n++;
				}
				d.text(sb.toString(), AGame.BIG_FOUNT, x + colW + MyDraw.UI_SPACING, y);
				y += AGame.BIG_FOUNT.lineHeight * Math.max(6, n) + MyDraw.UI_SPACING;
			}
			
			if (!(isInitiating() && showSettingsTab)) {
				if (!isSpectatingOrJoiningCoopEmpire()) {
					empireNameField.render(x, y, w, d);
					d.hook(x, y, w, MyDraw.textFieldH(), new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input in, Pt p, Hook.Type type) {
							empireNameField.focus = true;
							chatField.focus = false;
						}
					});
					y += MyDraw.textFieldH() + MyDraw.UI_SPACING;

					int col2X = x + colW + MyDraw.UI_SPACING;

					d.drawPanel(x, y, colW, colW, -1);
					new CoatOfArms(armsJSON).draw(d, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, colW - MyDraw.PANEL_INSET * 2);
					int y2 = y;
					final StrategicLobbyScreen sls = this;
					d.button(col2X, y2, colW, _t("Edit_Arms"), null, new Runnable() {
						@Override
						public void run() {
							g.s = new CoatEditor(g, sls);
						}
					}, !readySent);
					y2 += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
					d.button(col2X, y2, colW, _t("Random_Arms"), null, new Runnable() {
						@Override
						public void run() {
							armsJSON = CoatOfArms.getRandom(AGame.ANIM_R, HeraldicStyle.ofName("player")).toJSON();
							sendEmpireNameAndArmsUpdate();
						}
					}, !readySent);
					y2 += MyDraw.BUTTON_H + MyDraw.UI_SPACING;
					d.text(new CoatOfArms(armsJSON).getBonusOrTechDesc(), AGame.FOUNT, col2X, y2, colW, colW - MyDraw.BUTTON_H * 2 - MyDraw.UI_SPACING * 2, 0, true);
					y2 += d.textSize(new CoatOfArms(armsJSON).getBonusOrTechDesc(), AGame.FOUNT, col2X, y2, colW).height + MyDraw.BUTTON_SPACING;
					
					if (EHeroes.it.enabled && heroFrequency.frequencyMultiplier != 0) {
						int portraitH = Math.min(colW / 2, y + colW - y2);
						int heroX = col2X;
						int heroW = colW;
						if (portraitH > 40) {
							d.rect(Clr.DARK_GREY, col2X + MyDraw.PANEL_BORDER_W, y2 + MyDraw.PANEL_BORDER_W, portraitH - MyDraw.PANEL_BORDER_W * 2, portraitH - MyDraw.PANEL_BORDER_W * 2);
							if (hero != null) {
								d.portraitBlit(hero.type.img, col2X + MyDraw.PANEL_BORDER_W, y2 + MyDraw.PANEL_BORDER_W, portraitH - MyDraw.PANEL_BORDER_W * 2, portraitH - MyDraw.PANEL_BORDER_W * 2);
								d.tooltip(col2X, y2, portraitH, portraitH, hero.getDetails(true));
							}
							d.drawPanelBorder(col2X, y2, portraitH, portraitH);
							heroX += portraitH + MyDraw.BUTTON_SPACING;
							heroW -= portraitH + MyDraw.BUTTON_SPACING;
						}
						d.text(hero == null ? "?" : MyDraw.TITLE_C + hero.getName(), AGame.FOUNT, heroX, y2);
						y2 += AGame.FOUNT.lineHeight;
						/*if (portraitH >= AGame.FOUNT.lineHeight * 2 + MyDraw.BUTTON_SPACING + MyDraw.BUTTON_H) {
							d.text(hero == null ? "" : hero.getDesc(), AGame.FOUNT, heroX, y2); // qqDPS multiline?
							y2 += AGame.FOUNT.lineHeight;
						}*/
						y2 += MyDraw.BUTTON_SPACING;
						d.toggle(heroX, y2, heroW, _t("select_hero"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								showHeroSelector = !showHeroSelector;
							}
						}, showHeroSelector && !readySent, !readySent && availableHeroes != null);
					}
					
					y += colW + MyDraw.UI_SPACING;
				} else if (joiningCoopEmpire() != null) {
					StrategicPlayerInfo jce = joiningCoopEmpire();
					d.drawPanel(x, y, 32 + MyDraw.PANEL_INSET * 2, 32 + MyDraw.PANEL_INSET * 2, -1);
					new CoatOfArms(jce.armsJSON).draw(d, x + MyDraw.PANEL_INSET, y + MyDraw.PANEL_INSET, 32);
					d.tooltip(x, y, 32 + MyDraw.PANEL_INSET * 2, 32 + MyDraw.PANEL_INSET * 2, new CoatOfArms(jce.armsJSON).getBonusOrTechDesc());
					if (EHeroes.it.enabled && heroFrequency.frequencyMultiplier != 0) {
						int xx = x + 32 + MyDraw.PANEL_INSET * 2 + MyDraw.UI_SPACING;
						if (jce.heroJSON != null) {
							d.portraitBlit(new Hero(jce.heroJSON, null).type.img, xx + MyDraw.PANEL_BORDER_W, y + MyDraw.PANEL_BORDER_W, 32 + MyDraw.PANEL_INSET * 2 - MyDraw.PANEL_BORDER_W * 2, 32 + MyDraw.PANEL_INSET * 2 - MyDraw.PANEL_BORDER_W * 2);
							d.tooltip(xx, y, 32 + MyDraw.PANEL_INSET * 2, 32 + MyDraw.PANEL_INSET * 2, new Hero(jce.heroJSON, null).getDetails(true));
						}
						d.drawPanelBorder(xx, y, 32 + MyDraw.PANEL_INSET * 2, 32 + MyDraw.PANEL_INSET * 2);
					}
					y += 32 + MyDraw.PANEL_INSET * 2 + MyDraw.UI_SPACING;
				}

				int h = sm.height - MyDraw.SIDE_CLEARANCE - y;

				ArrayList<Integer> empireSlots = new ArrayList<Integer>();
				for (int i = -1; i < size.empires; i++) {
					empireSlots.add(i);
				}
				empiresScrollBar.draw(d, x, y, w, h, empireSlots, empireSlotsAdapter);
				empireSBRect.update(x, y, w, h);
			}
		}
		
		// chat window or hero selector
		x = leftSize + MyDraw.SIDE_CLEARANCE + MyDraw.UI_SPACING;
		y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		w = sm.width - x - MyDraw.SIDE_CLEARANCE;
		
		if (EHeroes.it.enabled && showHeroSelector && !readySent && availableHeroes != null) {
			int h = sm.height - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING - MyDraw.SIDE_CLEARANCE;
			heroesSBR.x = x;
			heroesSBR.y = y;
			heroesSBR.w = w;
			heroesSBR.h = h;
			heroesScrollBar.draw(d, x, y, w, h, availableHeroes, heroRenderer);
		} else {
			bw = d.bw(_t("Send"));
			int chatFieldH = chatField.multilineHeight(w - bw - MyDraw.BUTTON_SPACING, d);
			int h = sm.height - MyDraw.TOP_BAR_H - MyDraw.SIDE_CLEARANCE - MyDraw.UI_SPACING - Math.max(chatFieldH, MyDraw.BUTTON_H) - MyDraw.BUTTON_SPACING;	

			boolean showPlayers = sm.width > 1000;

			chatScrollBar.draw(d, x, y, w - (showPlayers ? 200 + MyDraw.UI_SPACING : 0), h, chat, chatAdapter);

			if (showPlayers) {
				playerScrollBar.draw(d, x + w - 200, y, 200, h, new ArrayList<StrategicPlayerInfo>(channelPlayers.values()), playerAdapter);
			}

			int mlo = AirshipGame.multiplayerChatOverlay ? MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING : 0; 
			chatField.renderMultilineIfNeeded(x, y + h + MyDraw.BUTTON_SPACING, w - bw - MyDraw.BUTTON_SPACING - mlo, d, sm, hs, cursor);
			d.hook(x, y + h + MyDraw.BUTTON_SPACING, w - bw - MyDraw.BUTTON_SPACING - mlo, chatFieldH, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					empireNameField.focus = false;
					chatField.focus = true;
				}
			});

			d.button(sm.width - bw - MyDraw.SIDE_CLEARANCE - mlo, y + h + MyDraw.BUTTON_SPACING, bw, _t("Send"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					sendChat();
				}
			}, g.isConnected());
		}
		
		if (mrpd != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			mrpd.render(d, sm, hs, cursor);
		}
		
		if (pleaseConfirmLeave) {
			d.getHooks().list.clear();
			d.confirmDialog(sm.width / 2 - 200, sm.height / 2 - 200, 400, _t("host_leave_confirm"), new Runnable() {
				@Override
				public void run() {
					leave(false);
				}
			}, new Runnable() {
				@Override
				public void run() {
					pleaseConfirmLeave = false;
				}
			});
		}
		
		if (unableToLoadCorrectMods) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 200, 400, _t("unable_to_load_mp_mods"), new Runnable() {
				@Override
				public void run() {
					leave(false);
				}
			});
		}
		
		/*if (client.isDisconnected()) {
			d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 200, 400, _t("Connection_lost"), new Runnable() {
				@Override
				public void run() {
					leave(true);
				}
			});
		}*/
	}

	@Override
	public ArrayList<String> music() {
		return null;
	}

	@Override
	public String appearancePostfix() {
		return "DAY";
	}

	@Override
	public boolean alwaysUseAppearancePostfix() {
		return true;
	}

	@Override
	public void select(Hero h) {
		hero = h;
		showHeroSelector = false;
		sendEmpireNameAndArmsUpdate();
	}

	@Override
	public String cannotSelectReason(Hero h) {
		return null;
	}
	
	@Override
	public WorldMap map() { return null; }
}
