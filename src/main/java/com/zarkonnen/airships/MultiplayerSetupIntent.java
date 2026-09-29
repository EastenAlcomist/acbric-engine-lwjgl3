package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

public class MultiplayerSetupIntent implements CombatSetupIntent, RestrictsShipPlacement {
	public static final int SINGLE_MP_AI_ID = -1;
	
	public AirshipGame g;
	public Server server;
	public int channelID;
	public String channelName = "Combat";
	public boolean leaveToMetaLobby;
	public long seed;
	public ArrayList<ChatMsg> chat = new ArrayList<ChatMsg>();
	public boolean welcomeReceived = false;
	public boolean helloSent = false;
	public boolean readySent = false;
	public HashMap<Integer, PlayerInfo> channelPlayers = new HashMap<Integer, PlayerInfo>();
	public boolean instantCommandRegeneration;
	public boolean allowDirectControl;
	public boolean vsAI;
	public int maxPlayersPerSide;
	public int costLimit;
	public int techTier;
	public boolean oneShipOnly;
	public EnumSet<ShipType> shipTypes;
	public boolean first = true;
	public boolean loadModsFromHandshake;
	public ModReloadProgressDialog mrpd;
	public boolean unableToLoadCorrectMods;
	public ArrayList<ModInfo> modInfos;
	public ArrayList<Expansion> expansions;
	private final HashMap<String, Integer> modChunksSent = new HashMap<String, Integer>();
	public boolean modsLoaded;
	public int landscapingCost = 0;
	public LandFormation originalGround;
	public final boolean fromMetaLobby;
	public String combatID;
	public boolean allPlayersKnown = false;
	public boolean sideAutoPicked = false;
	public int shipNetworkIDCounter = 0;
	public HashMap<IDPair, Integer> moneyTransfers = new HashMap<IDPair, Integer>();
	public boolean placeAIShips;
	public int hostID = 0;
	
	public int getMoneyTransferBalance(int playerID) {
		int balance = 0;
		for (int id : channelPlayers.keySet()) {
			balance += getMoneyFromTo(id, playerID);
		}
		return balance;
	}
	
	public int getMoneyFromTo(int fromID, int toID) {
		if (fromID == toID) { return 0; }
		IDPair idp = new IDPair(fromID, toID);
		if (!moneyTransfers.containsKey(idp)) { return 0; }
		return (idp.idA == fromID ? 1 : -1) * moneyTransfers.get(idp);
	}
	
	public void moveMoneyFromTo(int money, int fromID, int toID) {
		if (fromID == toID) { return; }
		IDPair idp = new IDPair(fromID, toID);
		int newBalance = getMoneyFromTo(fromID, toID) + money;
		if (idp.idA == toID) {
			newBalance = -newBalance;
		}
		moneyTransfers.put(idp, newBalance);
	}
	
	private void clearStaleMoneyTransfers() {
		for (Iterator<IDPair> it = moneyTransfers.keySet().iterator(); it.hasNext();) {
			IDPair idp = it.next();
			PlayerInfo piA = channelPlayers.get(idp.idA);
			PlayerInfo piB = channelPlayers.get(idp.idB);
			if (piA == null || piB == null || piA.isSpectator() || piB.isSpectator() || piA.side != piB.side) {
				it.remove();
			}
		}
	}
	
	public MultiplayerSetupIntent(AirshipGame g, Server server, int maxPlayersPerSide, int costLimit, boolean oneShipOnly, EnumSet<ShipType> shipTypes, int techTier, boolean instantCommandRegeneration, boolean allowDirectControl, boolean vsAI, boolean fromMetaLobby, String combatID) {
		// this.flipped = flipped; MERGEME should there be a flipped value or is that obsolete in the diplo MPSI?
		this.g = g;
		this.server = server;
		this.maxPlayersPerSide = maxPlayersPerSide;
		this.costLimit = costLimit;
		this.oneShipOnly = oneShipOnly;
		this.shipTypes = shipTypes;
		this.techTier = techTier;
		this.instantCommandRegeneration = instantCommandRegeneration;
		this.allowDirectControl = allowDirectControl;
		this.vsAI = vsAI;
		this.fromMetaLobby = fromMetaLobby;
		this.combatID = combatID;
		createAIPlayerIfNeeded();
	}
	
	public static double moneyPitch(int money) {
		money = Math.abs(money);
		double log = Math.log10(money);
		return Math.max(0.5, 1.4 - log * 0.2);
	}
	
	public static double moneyVolume(int money) {
		money = Math.abs(money);
		double log = Math.log10(money);
		return Math.min(2, 0.4 + log * 0.2);
	}
	
	public int getModChunksSent(String id) {
		return modChunksSent.containsKey(id) ? modChunksSent.get(id) : 0;
	}
	
	public void incrementModChunksSent(String id) {
		modChunksSent.put(id, getModChunksSent(id) + 1);
	}
	
	public void resetAllModChunksSent() {
		modChunksSent.clear();
	}
	
	public boolean isInitiating() {
		return server != null || g.playerID() == hostID;
	}
	
	public void doLoadModsFromHandshake(JSONObject msg, UniScreen us) {
		if (modInfos == null) {
			modInfos = new ArrayList<ModInfo>();
			expansions = new ArrayList<Expansion>();
		}
		final JSONArray mods = msg.getJSONObject("info").getJSONArray("mods");
		for (int i = 0; i < mods.length(); i++) {
			JSONObject m = mods.getJSONObject(i);
			modInfos.add(new ModInfo(m.getString("id"), m.getLong("checksum")));
		}
		if (msg.getJSONObject("info").has("expansions")) {
			expansions = Expansion.ofNames(msg.getJSONObject("info").getJSONArray("expansions"));
		}
		if (isInitiating()) {
			if (server != null) {
				doLoadMods(us);
			} else { // If we're from the metalobby, the mods have already been loaded.
				genGround(us);
			}
		} else {
			boolean xLoadNeeded = false;
			for (ModInfo mi : modInfos) {
				if (!Mod.isAvailable(mi.id, mi.checksum) && !Mod.isCached(mi.id, mi.checksum)) {
					//System.out.println("requesting xload of " + mi.id);
					g.sendMessage(msg("requestModCrossload").put("modID", mi.id).put("playerID", g.playerID()));
					xLoadNeeded = true;
				} else {
					//System.out.println(mi.id + " is available");
					mi.isAvailable = true;
				}
			}
			if (xLoadNeeded) {
				UniscreenModLoadUI msmlu = us.findFloat(UniscreenModLoadUI.class);
				us.clearComponents();
				us.floats.add(msmlu);
			}
		}
	}
	
	private boolean pollMessageAndProcess(Input in, final UniScreen us) {
		try {
			if (g.lanClient == null) {
				for (Iterator<PlayerInfo> it = channelPlayers.values().iterator(); it.hasNext();) {
					PlayerInfo pi = it.next();
					if (!pi.isAI() && !g.players.containsKey(pi.id)) {
						chat.add(new ChatMsg(pi, _t("xxx_LEFT_xxx", pi.name), DateTime.now()));
						it.remove();
					}
				}
				if (!g.players.isEmpty() && !g.players.containsKey(hostID) && g.lanClient == null) {
					g.showError(_t("host_left"));
					leave(us, false);
					return false;
				}
			}
			
			JSONObject msg = g.pollMessage();
			if (msg != null) {
				if (msg.getString("type").equals("serverReject")) {
					g.showError(_t("server_rejected_" + msg.getString("reason")));
					leave(us, true);
					return false;
				}
				if (msg.getString("type").equals("assignID")) {
					g.lanID = msg.getInt("playerID");
				} else if (msg.getString("type").equals("welcome")) {
					if (msg.has("info") && msg.getJSONObject("info").has("initiatorID")) {
						leave(us, true);
						g.showError(_t("attempting_to_connect_to_conquest"));
						return false;
					}
					seed = AGame.cleanSeed(msg.getLong("seed"));
					channelID = msg.getInt("channelID");
					if (msg.has("info")) {
						channelName = msg.getJSONObject("info").optString("name", "Combat");
					}
					welcomeReceived = true;
					int version = msg.optInt("version", 0);
					if (version != Server.VERSION) {
						leave(us, true);
						us.g.showError(_t("host_version_incompatible", version, Server.VERSION));
						return false;
					} else {
						if (modInfos == null) {
							modInfos = new ArrayList<ModInfo>();
							expansions = new ArrayList<Expansion>();
						}
						if (loadModsFromHandshake && msg.has("info") && msg.getJSONObject("info").has("mods")) {
							doLoadModsFromHandshake(msg, us);
						} else {
							genGround(us);
						}
					}
				}
				if (msg.getString("type").equals("frame")) {
					JSONArray frameMessages = msg.getJSONArray("messages");
					for (int i = 0; i < frameMessages.length(); i++) {
						JSONObject fm = frameMessages.getJSONObject(i);
						if (fm.getString("type").equals("addNewSetupShip")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							if (pi != null && !pi.isSpectator()) {
								Airship ship = new Airship(new JSONObject(Compression.decompressFromString(fm.getString("ship"))));
								ship.multiplayerControllerID = pi.id;
								ship.originalArms = pi.getArms();
								ship.setX(fm.getDouble("x"));
								ship.setY(fm.getDouble("y"));
								ship.flipped = fm.getBoolean("flipped");
								ship.flipTo = ship.flipped;
								ship.moveTo = new Pt(ship.getX(), ship.getY());
								ship.resetWeaponBarrels();
								if (PlaceShipTool.canPlace(ship, ship.getX(), ship.getY(), ship.flipped, pi.side, us, false, null, pi.side)) {
									pi.fleet.add(ship);
								}
							}
						}
						if (fm.getString("type").equals("moveSetupShip")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							if (pi != null && !pi.isSpectator()) {
								String id = fm.getString("shipID");
								Airship ship = null;
								for (Airship s : pi.fleet) {
									if (id.equals(s.networkID)) {
										ship = s;
									}
								}
								if (ship != null && PlaceShipTool.canPlace(ship, ship.getX(), ship.getY(), ship.flipped, pi.side, us, false, null, pi.side)) {
									ship.setX(fm.getDouble("x"));
									ship.setY(fm.getDouble("y"));
									ship.flipped = fm.getBoolean("flipped");
									ship.flipTo = ship.flipped;
									ship.moveTo = new Pt(ship.getX(), ship.getY());
									ship.resetWeaponBarrels();
								}
							}
						}
						if (fm.getString("type").equals("removeSetupShip")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							if (pi != null) {
								String id = fm.getString("shipID");
								for (Iterator<Airship> it = pi.fleet.iterator(); it.hasNext();) {
									if (id.equals(it.next().networkID)) {
										it.remove();
									}
								}
							}
						}
						if (fm.getString("type").equals("addSetupCaptain")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							if (pi != null) {
								String id = fm.getString("shipID");
								Hero h = new Hero(HeroType.ofName(fm.getString("hero")), null);
								for (Airship s : pi.fleet) {
									if (id.equals(s.networkID)) {
										s.setCaptain(h);
										break;
									}
								}
							}
						}
						if (fm.getString("type").equals("removeSetupCaptain")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							if (pi != null) {
								String id = fm.getString("shipID");
								for (Airship s : pi.fleet) {
									if (id.equals(s.networkID)) {
										s.setCaptain(null);
									}
								}
							}
						}
						if (fm.getString("type").equals("pickCombatSide")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("player"));
							int side = fm.getInt("side");
							if (pi != null && side != pi.side && (side == -1 || numPlayersOnSide(side) < maxPlayersPerSide()) && !(vsAI && side == 1)) {
								pi.side = side;
								pi.fleet.clear();
							}
						}
						if (fm.getString("type").equals("moveMoney")) {
							int fromID = fm.getInt("fromID");
							int toID = fm.getInt("toID");
							int money = fm.getInt("money");
							if (channelPlayers.containsKey(fromID) && channelPlayers.containsKey(toID) && !channelPlayers.get(toID).isReady() && money <= remainingBudget(fromID)) {
								moveMoneyFromTo(money, fromID, toID);
								if (toID == g.playerID() || fromID == g.playerID()) {
									in.play("coin", moneyPitch(money), moneyVolume(money) * us.g.volume, 0, 0);
								}
							}
						}
						if (fm.getString("type").equals("helloCombat")) {
							processHello(us, fm);
							if (!fm.has("version") || fm.getInt("version") != Server.VERSION) {
								g.sendMessage(msg("chat").put("text", "!!! Your version of the game is not compatible with the other player's. Please make sure you have both updated to the newest version of the game.").put("id", g.playerID()));
								leave(us, false);
								us.g.showError(_t("game_version_incompatible"));
								return false;
							}
							if (fm.optInt("costLimit", 0) != 0 && costLimit == 0) {
								vsAI = fm.getBoolean("vsAI");
								if (vsAI) {
									for (PlayerInfo pi : channelPlayers.values()) {
										if (pi.side == 1 && !pi.isAI()) {
											pi.side = -1;
										}
									}
								}
								instantCommandRegeneration = fm.getBoolean("instantCommandRegeneration");
								allowDirectControl = fm.getBoolean("allowDirectControl");
								maxPlayersPerSide = fm.optInt("maxPlayersPerSide", Integer.MAX_VALUE);
								costLimit = fm.getInt("costLimit");
								oneShipOnly = fm.optBoolean("oneShipOnly", false);
								if (fm.has("shipTypes")) {
									JSONArray a = fm.getJSONArray("shipTypes");
									shipTypes = EnumSet.noneOf(ShipType.class);
									for (int j = 0; j < a.length(); j++) {
										shipTypes.add(ShipType.valueOf(a.getString(j)));
									}
								} else if (shipTypes.isEmpty()) {
									shipTypes.addAll(EnumSet.allOf(ShipType.class));
								}
								techTier = fm.getInt("techTier");
							}
						}
						if (fm.getString("type").equals("chat")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("id"));
							if (pi != null) {
								chat.add(new ChatMsg(pi, /* pi.tint + */pi.name + ": " + fm.getString("text"), new DateTime(fm.getLong("t"), DateTimeZone.UTC)));
							}
						}
						if (fm.getString("type").equals("requestModCrossload") && isInitiating() && modInfos != null) {
							//System.out.println("req xload " + fm.getString("modID"));
							for (ModInfo mi : modInfos) {
								if (mi.id.equals(fm.getString("modID"))) {
									//System.out.println("set requested");
									mi.isRequested = true;
									modChunksSent.remove(mi.id);
								}
							}
						}
						if (fm.getString("type").equals("modChunk") && !isInitiating()) {
							String id = fm.getString("id");
							//System.out.println("mod recv " + id);
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
						if (fm.getString("type").equals("ready")) {
							PlayerInfo pi = channelPlayers.get(fm.getInt("id"));
							if (pi != null && !pi.isReady() && canBeReady(pi.id)) {
								chat.add(new ChatMsg(pi, _t("xxx_READY_xxx", pi.name), DateTime.now()));
								pi.setReady(true);
							}					
							return false;
						}
					}

					JSONArray frameMembers = msg.getJSONArray("members");
					HashSet<Integer> channelMemberIDs = new HashSet<Integer>();
					boolean hasHost = false;
					for (int i = 0; i < frameMembers.length(); i++) {
						int id = frameMembers.getInt(i);
						channelMemberIDs.add(id);
						hasHost |= id == hostID;
					}
					ArrayList<Integer> existingMemberIDs = new ArrayList<Integer>(channelPlayers.keySet());
					Collections.sort(existingMemberIDs); // Nevah evah loop through a hashset's keys in MP code!
					for (int id : existingMemberIDs) {
						if (!channelMemberIDs.contains(id)) {
							PlayerInfo pi = channelPlayers.get(id);
							if (pi != null && !pi.isAI()) {
								chat.add(new ChatMsg(pi, _t("xxx_LEFT_xxx", pi.name), DateTime.now()));
								channelPlayers.remove(id);
							}
						}
					}
					if (!hasHost && g.lanClient == null) { // Host has killed session.
						g.showError(_t("host_left"));
						leave(us, false);
						return false;
					}
					allPlayersKnown = channelPlayers.keySet().containsAll(channelMemberIDs);
					clearStaleMoneyTransfers();
				}

				return true;
			}

			return false;
		} catch (Exception e) {
			leave(us, true);
			us.g.reportError(_t("setup_failed") + "\n" + e.getMessage(), e, null, false, false);
			return false;
		}
	}
	
	public boolean allModsReceived() {
		if (isInitiating() || modInfos == null) { return false; }
		for (ModInfo mi : modInfos) {
			if (!mi.isAvailable && (mi.numChunks == 0 || mi.chunks.size() != mi.numChunks)) {
				return false;
			}
		}
		return true;
	}
	
	private void doLoadMods(final UniScreen us) {
		if (modsLoaded) { return; }
		modsLoaded = true;
		final ArrayList<String> newIDs = new ArrayList<String>();
		HashMap<String, Long> checksumsToUse = new HashMap<String, Long>();
		for (ModInfo mi : modInfos) {
			newIDs.add(mi.id);
			checksumsToUse.put(mi.id, mi.checksum);
		}
		Mod.overrideModsToLoad(newIDs, isInitiating() ? null : checksumsToUse, expansions);
		UniscreenModLoadUI msmlu = us.findFloat(UniscreenModLoadUI.class);
		us.clearComponents();
		us.floats.add(msmlu);
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
				us.reloadComponents();
				genGround(us);
			}
		}, new Runnable() {
			@Override
			public void run() {
				leave(us, false);
				us.g.showError(_t("unable_to_load_mp_mods"));
			}
		});
	}
	
	@Override
	public void tick(Input in, int ms, UniScreen us) {
		if (mrpd != null) {
			ModReloadProgressDialog myMrpd = mrpd;
			if (mrpd.tick(in) && myMrpd == mrpd) {
				mrpd = null;
			}
			return;
		}
		
		if (isInitiating() && !loadModsFromHandshake && modInfos == null) {
			modInfos = new ArrayList<ModInfo>();
			for (Mod m : Mod.getEnabledMods()) {
				modInfos.add(new ModInfo(m.id, m.getCachedChecksum()));
			}
			expansions = Expansion.installeds();
		}
		
		 // qqDPS Right now we're only attempting to reconnect if we are already in a state where we have the proper setup info and the mods loaded.
		 // To this end, we do mod loading without checking for connectedness.
		if (welcomeReceived && helloSent && !us.g.isConnected()) {
			int oldID = us.g.playerID();
			if (us.g.completelyReconnectClient()) {
				us.g.s = new ResumeScreen(us.g, us, oldID, true, 1, channelName);
			} else {
				g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
				g.showError(_t("Connection_lost"));
			}
			return;
		}
		
		Client client = g.lanClient == null ? g.client : g.lanClient;
		if (client == null || client.isDisconnected()) {
			if (g.lanClient == null) {
				g.disconnectClient();
			} else {
				g.lanClient = null;
			}
			leave(us, true);
			g.showError(_t("Connection_lost"));
			return;
		}
		
		if (first) {
			ZoomToFitButton.zoomOut(in, us);
			first = false;
		}
		
		if (!us.g.isConnected()) {
			us.g.showError(_t("Connection_lost"));
			leave(us, true);
			return;
		}
		
		if (welcomeReceived && !helloSent) {
			JSONArray sts = new JSONArray();
			for (ShipType t : shipTypes) {
				sts.put(t.name());
			}
			PlayerInfo mpi = myInfo();
			JSONArray fla = new JSONArray();
			if (mpi != null) {
				for (Airship s : mpi.fleet) {
					fla.put(s.toJSON(null));
				}
			}
			if (!g.sendMessageWithSizeCheck(
					msg("helloCombat")
					.put("name", g.playerName())
					.put("id", g.playerID())
					.put("side", mpi == null ? -1 : mpi.side)
					.put("fleet", Compression.compressToString(fla.toString()))
					.put("arms", g.playerArms())
					.put("costLimit", costLimit)
					.put("techTier", techTier)
					.put("oneShipOnly", oneShipOnly)
					.put("maxPlayersPerSide", maxPlayersPerSide)
					.put("shipTypes", sts)
					.put("instantCommandRegeneration", instantCommandRegeneration)
					.put("allowDirectControl", allowDirectControl)
					.put("vsAI", vsAI)
					.put("version", Server.VERSION)))
			{
				if (mpi != null) {
					mpi.fleet.clear();
				}
				g.showError(_t("sent_fleet_too_large"));
			} else {
				if (vsAI && aiInfo() != null) {
					PlayerInfo aiI = aiInfo();
					fla = new JSONArray();
					for (Airship s : aiI.fleet) {
						fla.put(s.toJSON(null));
					}
					if (!g.sendMessageWithSizeCheck(
							msg("helloCombat")
							.put("name", "AI")
							.put("id", SINGLE_MP_AI_ID)
							.put("side", 1)
							.put("fleet", Compression.compressToString(fla.toString()))
							.put("arms", aiI.armsJSON)
							.put("costLimit", costLimit)
							.put("techTier", techTier)
							.put("oneShipOnly", oneShipOnly)
							.put("maxPlayersPerSide", maxPlayersPerSide)
							.put("shipTypes", sts)
							.put("instantCommandRegeneration", instantCommandRegeneration)
							.put("allowDirectControl", allowDirectControl)
							.put("vsAI", vsAI)
							.put("version", Server.VERSION)))
					{
						aiI.fleet.clear();
						g.showError(_t("sent_fleet_too_large"));
					} else {
						helloSent = true;
						if (readySent) {
							sendReady(us); // Resend ready message.
						}
					}
				} else {
					helloSent = true;
					if (readySent) {
						sendReady(us); // Resend ready message.
					}
				}
			}
		}
		
		if (welcomeReceived && isInitiating() && channelPlayers.size() > 1 && g.outQueueSize() < 8 && modInfos != null) {
			for (ModInfo mi : modInfos) {
				if (!mi.isRequested) { continue; }
				if (mi.numChunks == 0) {
					try {
						mi.chunks = Mod.getById(mi.id).getChunks();
					} catch (IOException e) {
						g.showError(_t("unable_to_crossload_mod_x", mi.id));
						leave(us, false);
						return;
					}
					mi.numChunks = mi.chunks.size();
				}
				if (getModChunksSent(mi.id) < mi.numChunks) {
					//System.out.println("mod send " + mi.id);
					g.sendMessage(msg("modChunk").put("id", mi.id).put("checksum", mi.checksum).put("index", getModChunksSent(mi.id)).put("total", mi.numChunks).put("data", mi.chunks.get(getModChunksSent(mi.id))));
					incrementModChunksSent(mi.id);
					break;
				}
			}
		}

		while (pollMessageAndProcess(in, us)) {
			for (PlayerInfo pi : channelPlayers.values()) {
				if (!canBeReady(pi.id)) {
					pi.setReady(false); // Ethelready
					if (pi.id == g.playerID()) {
						readySent = false;
					}
				}
			}
		}
		
		if (allModsReceived()) {
			doLoadMods(us);
		}
		
		if (mrpd != null) {
			return;
		}
		
		if (unableToLoadCorrectMods) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				leave(us, false);
			}
			return;
		}
		
		if (allPlayersKnown && !sideAutoPicked) {
			if (vsAI) {
				g.sendMessage(msg("pickCombatSide").put("player", g.playerID()).put("side", 0));
			} else {
				int side0 = numPlayersOnSide(0);
				int side1 = numPlayersOnSide(1);
				if (side0 < side1 || side0 == 0) {
					g.sendMessage(msg("pickCombatSide").put("player", g.playerID()).put("side", 0));
				} else if (side0 > side1) {
					g.sendMessage(msg("pickCombatSide").put("player", g.playerID()).put("side", 1));
				}
			}
			sideAutoPicked = true;
		}
		
		if (canStart(us)) {
			startGame(us);
		}
	}
	
	public void genGround(UniScreen us) {
		if (us.setupGround == null) {
			GuardedRandom r = new GuardedRandom(seed);
			us.setupBG = Loadable.all(CombatBackgroundFlavor.class).get(r.nextInt(Loadable.all(CombatBackgroundFlavor.class).size()));
			Utils.Pair<LandFormation, List<LandFormation>> p = LandFormation.generate(r, true, us.setupBG.getLandscapeType(r));
			us.setupGround = p.a;
			us.setupFloaters = new ArrayList<LandFormation>(p.b);
			us.setupTime = TimeOfDay.getRandomForTournament(r);
		}
	}
	
	private boolean canStart(UniScreen us) {
		// Need to have two sides, all ready.
		if (numPlayersOnSide(0) == 0 || numPlayersOnSide(1) == 0) { return false; }
		boolean[] sidesReady = { true, true };		
		for (PlayerInfo pi : channelPlayers.values()) {
			if (!pi.isSpectator()) {
				sidesReady[pi.side] &= pi.isReady();
			}
		}
		return sidesReady[0] && sidesReady[1];
	}
	
	private void processHello(UniScreen us, JSONObject fm) {
		if (!channelPlayers.containsKey(fm.getInt("id"))) {
			PlayerInfo pi = new PlayerInfo(fm.getInt("id"), fm.getString("name"));
			pi.armsJSON = fm.getJSONObject("arms");
			pi.side = fm.optInt("side", -1);
			channelPlayers.put(pi.id, pi);
			JSONArray fla = new JSONArray(Compression.decompressFromString(fm.getString("fleet")));
			for (int j = 0; j < fla.length(); j++) {
				pi.fleet.add(new Airship(fla.getJSONObject(j)));
			}
			//in.play("ready", 1.0, 1.0 * g.volume, 0, 0); qqDPS
			chat.add(new ChatMsg(pi, _t("xxx_JOINED_xxx", pi.name), DateTime.now()));
			if (pi.id != g.playerID()) {
				helloSent = false; // Resend hello
				modChunksSent.clear();
			}
		}
	}
	
	public PlayerInfo myInfo() {
		return channelPlayers.get(g.playerID());
	}
	
	public PlayerInfo placingInfo() {
		if (placeAIShips) {
			return aiInfo();
		} else {
			return myInfo();
		}
	}
	
	private void createAIPlayerIfNeeded() {
		if (!vsAI) { return; }
		if (channelPlayers.containsKey(SINGLE_MP_AI_ID)) { return; }
		PlayerInfo aiI = new PlayerInfo(SINGLE_MP_AI_ID, "AI");
		aiI.armsJSON = CoatOfArms.aiArms().toJSON();
		aiI.side = 1;
		channelPlayers.put(SINGLE_MP_AI_ID, aiI);
	}
	
	public PlayerInfo aiInfo() {
		return channelPlayers.get(SINGLE_MP_AI_ID);
	}
	
	public int maxPlayersPerSide() {
		return maxPlayersPerSide;
	}
	
	public int numPlayersOnSide(int side) {
		int i = 0;
		for (PlayerInfo pi : channelPlayers.values()) {
			i += (pi.side == side ? 1 : 0);
		}
		return i;
	}
	
	public boolean canBeReady(int playerID) {
		PlayerInfo pi = channelPlayers.get(playerID);
		if (vsAI && (aiInfo() == null || !aiInfo().isReady())) { return false; }
		return inBudget(playerID) && !pi.fleet.isEmpty() && (!oneShipOnly || pi.fleet.size() == 1);
	}
	
	public boolean inBudget(int playerID) {
		PlayerInfo pi = channelPlayers.get(playerID);
		if (pi == null) { return false; }
		if (pi.isSpectator()) { return true; }
		return pi.cost() <= availableFunds(playerID);
	}
	
	public int availableFunds(int playerID) {
		PlayerInfo pi = channelPlayers.get(playerID);
		if (pi == null) { return 0; }
		if (pi.isSpectator()) { return 0; }
		return costLimit / numPlayersOnSide(pi.side) + getMoneyTransferBalance(playerID);
	}
	
	public int remainingBudget(int playerID) {
		PlayerInfo pi = channelPlayers.get(playerID);
		if (pi == null) { return 0; }
		return availableFunds(playerID) - pi.cost();
	}
	
	public int remainingBudget() {
		PlayerInfo me = myInfo();
		return availableFunds(me.id) - me.cost();
	}
	
	public int placingRemainingBudget() {
		if (placeAIShips) {
			return 100000;
		} else {
			return remainingBudget();
		}
	}
	
	public List<Airship> getMySetupShips() {
		PlayerInfo me = myInfo();
		if (me == null) {
			return Collections.emptyList();
		}
		return me.fleet;
	}
	
	public List<Airship> getSetupShips() {
		return placeAIShips ? aiInfo().fleet : getMySideShips();
	}
	
	public List<Airship> getMySideShips() {
		PlayerInfo me = myInfo();
		if (me == null) {
			//System.out.println("no myinfo " + g.playerID());
			//for (int id : channelPlayers.keySet()) { System.out.println("  " + id); }
			return Collections.emptyList();
		}
		ArrayList<PlayerInfo> ps = new ArrayList<PlayerInfo>(channelPlayers.values());
		Collections.sort(ps);
		ArrayList<Airship> ships = new ArrayList<Airship>();
		for (PlayerInfo pi : ps) {
			if (pi.side == me.side) {
				//System.out.println(pi.id + " on my side (" + me.side + " vs " + pi.side + ") with " + pi.fleet.size() + " ships");
				ships.addAll(pi.fleet);
			}/* else {
				System.out.println(pi.id + " not on my side (" + me.side + " vs " + pi.side + ") with " + pi.fleet.size() + " ships");
			}*/
		}
		return ships;
	}
	
	public List<Airship> getSideShips(int side) {
		ArrayList<PlayerInfo> ps = new ArrayList<PlayerInfo>(channelPlayers.values());
		Collections.sort(ps);
		ArrayList<Airship> ships = new ArrayList<Airship>();
		for (PlayerInfo pi : ps) {
			if (pi.side == side) {
				ships.addAll(pi.fleet);
			}
		}
		return ships;
	}
	
	private void startGame(UniScreen us) {
		g.sendMessage(msg("sealChannel"));
		ArrayList<PlayerInfo> ps = new ArrayList<PlayerInfo>(channelPlayers.values());
		Collections.sort(ps);
		Combat c = new Combat(us.g, g.lanClient != null ? g.lanClient : g.client, server, seed, ps, costLimit, us.setupTime, techTier, instantCommandRegeneration, allowDirectControl, channelName);
		c.combatID = combatID;
		c.chatMessages.addAll(chat);
		c.speedVoters = new HashMap<Integer, CombatSpeed>();
		for (PlayerInfo pi : ps) {
			if (!pi.isAI() && !pi.isSpectator()) {
				c.speedVoters.put(pi.id, CombatSpeed.NORMAL);
			}
		}
		c.recording.header.type = Recording.Header.Type.MULTIPLAYER;
		c.startCountdown = 3 * 1024;
		c.landFormations.add(us.setupGround);
		c.landFormations.addAll(us.setupFloaters);
		c.backgroundFlavor = us.setupBG;
		c.initWheelsLegsTentaclesAndBarrels();
		us.setupGround = null;
		us.setupFloaters = null;
		us.setupBG = null;
		us.combat = c;
		us.mySide = c.getSideForID(g.playerID());
		us.intent = new MultiplayerCombatIntent(myInfo(), fromMetaLobby, c);
		c.fullyRepairAllShips(); // Make sure things are consistent.
	}

	public void leave(UniScreen us, boolean fully) {
		if (leaveToMetaLobby) {
			if (fully) {
				us.g.disconnectClient();
				us.g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
			} else {
				us.g.s = new MetaLobbyScreen(us.g, server);
				g.sendMessage(msg("changeChannel").put("id", 0));
				us.g.helloSent = false;
			}
		} else {
			if (g.lanClient != null) {
				g.lanClient.close();
				g.lanClient = null;
			}
			if (server != null) {
				server.close();
				g.lanServer = null;
			}
			us.g.s = new MainMenu(us.g, MainMenu.Submenu.MULTIPLAYER);
		}
	}
	
	public void sendReady(UniScreen us) {
		PlayerInfo mpi = myInfo();
		if (mpi == null) {
			return;
		}
		readySent = true;
		g.sendMessage(msg("ready").put("id", g.playerID()));
	}

	@Override
	public Rect2D placementLimits(int sideIndex, UniScreen us) {
		switch (sideIndex) {
			case 0:
				return new Rect2D(-us.combatAreaW() / 2 + Combat.OUTER_ZONE_W, AGame.GROUND_LEVEL - Combat.COMBAT_AREA_H, us.combatAreaW() / 2 - Combat.EXCLUSION_ZONE_W / 2 - Combat.OUTER_ZONE_W, Combat.COMBAT_AREA_H + AGame.SGS * 8);
			case 1:
				return new Rect2D(Combat.EXCLUSION_ZONE_W / 2, AGame.GROUND_LEVEL - Combat.COMBAT_AREA_H, us.combatAreaW() / 2 - Combat.EXCLUSION_ZONE_W / 2 - Combat.OUTER_ZONE_W, Combat.COMBAT_AREA_H + AGame.SGS * 8);
		}
		return new Rect2D(-us.combatAreaW() / 2 + Combat.OUTER_ZONE_W, AGame.GROUND_LEVEL - Combat.COMBAT_AREA_H, us.combatAreaW() / 2 - Combat.EXCLUSION_ZONE_W / 2 - Combat.OUTER_ZONE_W, Combat.COMBAT_AREA_H + AGame.SGS * 8);
	}
	
	@Override
	public boolean showOutside() { return false; }
	@Override
	public boolean showDecals() { return true; }
	@Override
	public boolean drawAsBlueprint() { return false; }
	@Override
	public boolean allowMultiSelect() { return false; }

	@Override
	public boolean isShipPlayerControlled(UniScreen us, Airship ship) {
		PlayerInfo pi = placingInfo();
		return pi != null && pi.fleet.contains(ship);
	}
}
