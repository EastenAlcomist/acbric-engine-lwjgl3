package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.Img;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.CombatOutcome.CombatOutcomeType;
import com.zarkonnen.airships.PlaceShipTool.Placement;
import com.zarkonnen.airships.Relationship.Offer;
import com.zarkonnen.airships.Relationship.Ultimatum;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.joda.time.DateTime;

public class CampaignWorld implements OutPipe.Writer {
	public static final int NOTICE_LIFESPAN = 60000;	

	public static boolean MAGIC_WIN_ENABLED = false;
	public static final int SPEED_DIV = 8;
	public String gameName = "Conquest Game";
	public final WorldMap map;
	public Empire player;
	public boolean showCompressedFleet;
	public CombatInfo combatInfo;
	public transient Speed speed = Speed.STOP;
	public transient HashMap<Integer, Speed> speedVoters;
	public final AirshipGame g;
	public StrategicHelp help = new StrategicHelp();
	public transient int accumulatedMs;
	public transient int frameAccumulatedMs;
	public boolean waitingForCombatSetup;
	public transient MultiplayerCampaignCombatSetupIntent multiplayerCampaignCombatSetupIntent;
	public transient MultiplayerCampaignCombatIntent multiplayerCampaignCombatIntent;
	public boolean usedCheatCommand;
	public boolean deactivateAICheat;
	public boolean compliantAICheat;

	private JSONArray postCombatCommands = new JSONArray();
	private final LinkedList<JSONObject> frameQueue = new LinkedList<JSONObject>();
	public transient ArrayList<GenericChatMsg> chatMessages = new ArrayList<GenericChatMsg>();
	public transient ArrayList<GenericChatMsg> allyChatMessages = new ArrayList<GenericChatMsg>();
	public transient HashMap<Integer, StrategicPlayerInfo> channelPlayers;
	public transient ArrayList<StrategicPlayerInfo> disconnectedPlayers = new ArrayList<StrategicPlayerInfo>();
	public transient ArrayList<StrategicPlayerInfo> defeatedPlayers = new ArrayList<StrategicPlayerInfo>();
	public transient int defeatedPlayersSeenIndex = 0;
	public transient boolean needResync = false;
	public transient boolean needResyncReceived = false;
	public transient int needResyncAccumulator = 0;
	
	public static final boolean CHECKSUM = true;
	public static final int SYNC_CHECK_EVERY = 16384;
	public static final int MAX_SYNC_CHECK_QUEUE = 2;
	private transient boolean desyncDetected = false;
	public transient boolean needResyncSentOrReceived = false;
	private transient int ticksSinceDesyncDetected = 0;
	private transient int desyncDetectedAt = -1;
	private transient final ArrayList<StoredState> storedStates = new ArrayList<StoredState>();
	private transient final HashMap<Pair<Integer, Integer>, ArrayList<JSONObject>> futureChecksums = new HashMap<Pair<Integer, Integer>, ArrayList<JSONObject>>();
	private transient boolean unevenConsumptionReported;
	private transient boolean nonSequentialFramesReported;
	private transient long mostRecentFrameNumber = -1;
	private transient boolean nonMPCapableSpeedReported;
	
	public ArrayList<String> doneConquestHelpItems = new ArrayList<String>();
	
	transient int expeditionOutcomeIndex;
	
	public String describeTime(int ms) {
		return map.describeTime(ms, player == null ? BonusSet.empty() : player.bonuses);
	}
	
	public void checkAchievements(Relationship.Offer o) {
		if (usedCheatCommand) { return; }
		if (player == null) { return; }
		checkMelos(o);
		checkEatenLast(o);
	}
	
	private void checkMelos(Relationship.Offer o) {
		if (o.newLevel != Relationship.Level.TRUCE) { return; }
		if (!o.getReceivingTribute(player)) { return; }
		if (o.getSubmitting(player)) { return; }
		if (o.moneyTransferAToB > 0 == (player == o.rel.a)) { return; }
		for (Relationship.CityTransfer t : o.cityTransfers) {
			if (player.cities.contains(t.city)) { return; }
		}
		Achievement.achieve(Achievement.MELOS);
	}
	
	private void checkEatenLast(Relationship.Offer o) {
		if (o.newLevel != Relationship.Level.ALLIANCE) { return; }
		if (!o.rel.other(player).bonuses.contains[Bonus.ofName("WORM_EYE_CULT").ordinal()]) { return; }
		Achievement.achieve(Achievement.EATEN_LAST);
	}
	
	public String cannotDismissReason(Hero h) {
		if (h.inEmpire == null) { return null; }
		if (!h.type.canDismiss) {
			return _t("cannotBeDismissed");
		}
		if (h.onExpedition()) {
			return _t("captain_on_expedition");
		}
		if (h.getInShip() != null) {
			Fleet f = h.inEmpire.fleet(h.getInShip());
			if (f == null || f.location == null || !map.isFriendly(h.inEmpire, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
				return _t("captain_in_unavailable_ship");
			}
		}
		if (h.isBusyInCity()) {
			return _t("governor_busy");
		}
		return null;
	}
	
	public transient StrategicScreen ssForDiplomacyCallback;
	
	public transient ArrayList<Relationship.Offer> playerOffers = new ArrayList<Relationship.Offer>();
	
	public boolean has(ConquestToggle t) {
		return map.toggles.contains(t);
	}
	
	private boolean combatStarted() {
		return combatInfo != null && !waitingForCombatSetup && multiplayerCampaignCombatSetupIntent == null;
	}
	
	private boolean inCombatSetup() {
		return combatInfo != null && (waitingForCombatSetup || multiplayerCampaignCombatSetupIntent != null);
	}
	
	public boolean desyncPubliclyDetected() {
		return needResync && needResyncAccumulator > 2000; //needResyncSentOrReceived && ((desyncDetected && map.age > desyncDetectedAt + 1000) || needResyncAccumulator > 2000);
	}
	
	public int frameQueueSize() {
		return frameQueue.size();
	}
	
	private transient boolean badFQSReported = false;
	private transient int badFQSPrintBoundary = 50;
	private transient int badTickIntervalPrintBoundary = 50;
	
	public String getPlayerSummary() {
		if (channelPlayers == null) { return "\nnone"; }
		ArrayList<Integer> ids = new ArrayList<Integer>(channelPlayers.keySet());
		Collections.sort(ids);
		StringBuilder sb = new StringBuilder();
		for (int id : ids) {
			StrategicPlayerInfo spi = channelPlayers.get(id);
			sb.append("\n").append(spi.id).append(": ").append(spi.name);
			if (spi.empire != null) {
				sb.append(" empire: ").append(spi.empire.name).append(" #").append(spi.empire.id);
			}
		}
		return sb.toString();
	}
	
	private SavedStateOutPipe hop = new SavedStateOutPipe();
	
	public void initStoredStateChain() {
		storedStates.add(new StoredState(this, g.playerID()));
	}

	@Override
	public JSONObject write(String writeID) {
		if (writeID.equals("savePreviewFixed")) {
			return SavePreviewInfo.toJSONFixed(this);
		}
		if (writeID.equals("savePreviewVariable")) {
			return SavePreviewInfo.toJSONVariable(this);
		}
		throw new RuntimeException(writeID);
	}

	private static class StoredState {
		public final int age;
		public final int combatTime;
		public final int hash;
		public final SavedStateOutPipe state;
		public HashSet<Integer> confirmedPlayerIDs = new HashSet<Integer>();
		StoredState(final CampaignWorld w, int myID) {
			long start_ru = System.currentTimeMillis();
			PerfStats.syncTimes.clear();
			age = w.map.age;
			w.hop = new SavedStateOutPipe(w.hop);
			state = w.hop;
			final JSONObject mo = w.map.toJSON(w.hop);
			w.hop.register(new OutPipe.Writer() {
				@Override
				public JSONObject write(String writeID) {
					return mo;
				}
			}, "map", age);
			if (w.combatInfo != null && w.combatStarted()) {
				w.hop.register(new OutPipe.Writer() {
					@Override
					public JSONObject write(String writeID) {
						return w.combatInfo.combat.toJSON();
					}
				}, "combat_" + w.combatInfo.combat.conquestID, w.combatInfo.combat.time);
				combatTime = w.combatInfo.combat.time;
			} else {
				combatTime = -1;
			}
			hash = state.compileAndGetHash();
			confirmedPlayerIDs.add(myID);
			PerfStats.syncTime = System.currentTimeMillis() - start_ru;
		}
	}
		
	public transient int playerEmpireIndex;
	
	public Client mpClient;
	public Server mpServer;
	public transient boolean fakeMultiplayerForTesting;
		
	public boolean isMultiplayer() {
		return mpClient != null;
	}
	
	public boolean isHost() {
		return mpServer != null;
	}
	
	public JSONObject shipMsg(String type, Airship ship) {
		return msg(type).put("networkID", ship.networkID);
	}
	
	public void giveCommand(JSONObject cmd) {
		//System.out.println("give " + mpClient);
		if (mpClient != null) {
			g.sendMessage(cmd);
		} else {
			execCommand(cmd);
		}
	}
	
	public boolean giveCommandWithSizeCheck(JSONObject cmd) {
		//System.out.println("give " + mpClient);
		if (mpClient != null) {
			return g.sendMessageWithSizeCheck(cmd);
		} else {
			execCommand(cmd);
			return true;
		}
	}
	
	private void execCommand(JSONObject cmd) {
		//System.out.println("exc " + cmd.toString());
		String type = cmd.getString("type");
		if (!EXECS.containsKey(type)) {
			if (!Combat.EXECS.containsKey(type)) {
				g.reportError("Unknown command: " + type, null, cmd.toString(), false, true);
			}
			return;
		}
		String error = EXECS.get(cmd.getString("type")).run(cmd, map, this);
		JSONObject archived = EXECS.get(cmd.getString("type")).archive(cmd);
		if (archived != null) {
			map.commandLog.add(new CommandLogEntry(cmd.getString("type"), archived, map.age, error));
		}
	}
	
	public static interface CommandExecutor {
		public String run(JSONObject cmd, WorldMap map, CampaignWorld cw);
		public JSONObject archive(JSONObject cmd);
	}

	public static final HashMap<String, CommandExecutor> EXECS = new HashMap<String, CommandExecutor>();
	
	private static Airship getShip(JSONObject cmd, WorldMap m) {
		return m.getShip(cmd.getString("networkID"));
	}
	
	static void checkStrategicChecksum(JSONObject cmd, StoredState state, CampaignWorld w) {
		int hash = cmd.getInt("hash");
		int playerID = cmd.getInt("playerID");
		if (state.hash == hash) {
			//System.out.println("hash match " + state.hash + " = " + hash + " age " + cmd.getInt("age"));
			state.confirmedPlayerIDs.add(playerID);
			while (state.confirmedPlayerIDs.containsAll(w.channelPlayers.keySet()) && w.storedStates.indexOf(state) > 0) {
				w.storedStates.remove(0);
			}
		} else if (!w.desyncDetected) {
			// Desync Detected
			//System.out.println("desync detected");
			w.desyncDetected = true;
			w.desyncDetectedAt = w.map.age;
			int prevStateIndex = w.storedStates.indexOf(state);
			StoredState prevState = null;
			if (prevStateIndex > 0) {
				prevState = w.storedStates.get(prevStateIndex - 1);
			}
			w.storedStates.clear(); // Free up memory.
			String errorMessage = "Desync @ " + cmd.getInt("age") + " ct " + cmd.getInt("combatTime") + " myID " + w.g.playerID() + " " + state.hash + " vs " + playerID + " " + hash;
			errorMessage += "\nmyEmpire: " + (w.player == null ? "none" : w.player.getName());
			errorMessage += "\nframeQueue " + w.frameQueue.size() + " avgTickInterval " + AirshipGame.instance.client.avgTickInterval();
			System.out.println(errorMessage);
			if (w.combatInfo != null) {
				errorMessage += " combatSpeed " + w.combatInfo.combat.speed + " startCombatFrameNumber " + w.combatInfo.combat.startFrameNumber;
				errorMessage += "\nCommands:\n" + w.combatInfo.combat.executedCommands.toString(4) + "\n";
			}
			String e1 = errorMessage + w.getPlayerSummary();
			try {
				System.err.println("");
				System.err.println("@ " + System.currentTimeMillis() + " " + new DateTime());
				System.err.println(e1);
				System.err.println("");
			} catch (Exception e2) {}
			e1 += "\nDesyncState\n" + CompressAndArmour.compressAndArmour(state.state.toJSON().toString());
			w.g.reportDebug("Desync", e1);
			/*try {
				if (prevState != null) {
					String e2 = "PrevState for " + errorMessage + w.getPlayerSummary() + "\nPrevState\n" + CompressAndArmour.compressAndArmour(prevState.state.toJSON().toString());
					w.g.reportDebug("e2);
				}
			} catch (Exception e) {  }*/
			//w.g.reportError(errorMessage, null, null, false, true);
			
		} else {
			System.out.println("mismatch but desync already detected");
		}
	}
	
	private static JSONObject copyExcept(JSONObject o, String... except) {
		JSONObject o2 = new JSONObject();
		lp: for (Object ko : o.keySet()) {
			String k = (String) ko;
			for (int i = 0; i < except.length; i++) {
				if (except[i].equals(k)) { continue lp; }
			}
			o2.put(k, o.get(k));
		}
		return o2;
	}
	
	static {
		EXECS.put("awardMedal", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Airship ship = getShip(cmd, cw.map);
				if (ship == null) { return "Ship not found."; }
				e.awardMedal(ship, e.getMedalDesign(cmd.getInt("medalLevel")));
				return null;
			}

			@Override
			public JSONObject archive(JSONObject cmd) {
				return cmd;
			}
		});
		EXECS.put("setMedalDesign", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Medal m = new Medal(cmd.getJSONObject("medal"));
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				e.setMedalDesign(m.level, m);
				return null;
			}

			@Override
			public JSONObject archive(JSONObject cmd) {
				return cmd;
			}
		});
		EXECS.put("pickIncidentOption", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Incident in = IncidentType.get(cmd.getInt("incident"), map);
				if (in == null) {
					return "Unknown incident " + cmd.getInt("incident");
				}
				if (in.eventPath.size() != cmd.getInt("eventPathDepth")) {
					return "Supplied option for event path depths " + cmd.getInt("eventPathDepth") + " but we are at " + in.eventPath.size();
				}
				if (!in.pickOption(cmd.getInt("option"), map)) {
					return "Unable to pick option " + cmd.getInt("option");
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("makeEdict", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.inEmpire != e) { return "Hero in wrong empire"; }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				if (!e.cities.contains(c)) { return "City not in empire"; }
				if (c.edict != null) { return "City already has edict " + c.edict.name; }
				Edict ed = Edict.ofName(cmd.getString("edict"));
				if (e.getMoney() + ed.money < 0) {
					return "Cannot afford edict.";
				}
				if (c.getEconomicDamage() + ed.pillaging > City.MAX_ECON_DAMAGES) {
					return "Cannot pillage further.";
				}
				c.makeEdict(h, e, ed, map);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("clearNest", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.inEmpire != e) { return "Hero in wrong empire"; }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				if (!e.cities.contains(c)) { return "City not in empire"; }
				MonsterNestType nt = MonsterNestType.ofName(cmd.getString("nestType"));
				if (e.getMoney() + h.type.nestClearMoney < 0) {
					return "Cannot afford nest clear.";
				}
				if (!c.hasNestOfType(nt, map)) {
					return "No nest of this type.";
				}
				if (c.clearNest(h, e, nt, map)) {
					Hero.processHeroEvent(HeroEvent.nestDestroyed(e, nt), map);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("moveNest", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.inEmpire != e) { return "Hero in wrong empire"; }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				if (!e.cities.contains(c)) { return "City not in empire"; }
				MonsterNestType nt = MonsterNestType.ofName(cmd.getString("nestType"));
				if (e.getMoney() + h.type.nestMoveMoney < 0) {
					return "Cannot afford nest move.";
				}
				if (c.nestMovePair(nt, map) == null) {
					return "No suitable source or destination.";
				}
				c.moveNest(h, e, nt, map);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("hireHero", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.inEmpire != e) { return "Hero in wrong empire"; }
				if (!Hero.hire(h, map)) {
					return "Unable to hire";
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("fireHero", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.inEmpire != e) { return "Hero in wrong empire"; }
				if (h.getInShip() != null) {
					Fleet f = e.fleet(h.getInShip());
					if (f == null || f.location == null || !map.isFriendly(e, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
						return "Hero cannot be unassigned because ship fleet not at friendly location.";
					}
				}
				if (!Hero.fire(h, map)) {
					return "Unable to fire";
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("assignHeroToCity", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (!Hero.assign(h, e, c, map)) {
					return "Unable to assign";
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("assignHeroToShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Airship ship = getShip(cmd, cw.map);
				if (ship == null) { return "Ship not found."; }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				// Make sure they're at a friendly port.
				Fleet f = e.fleet(ship);
				if (f == null || f.location == null || !map.isFriendly(e, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
					return "New ship fleet not at friendly location.";
				}
				if (h.getInShip() != null) {
					f = e.fleet(h.getInShip());
					if (f == null || f.location == null || !map.isFriendly(e, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
						return "Old ship fleet not at friendly location.";
					}
				}
				if (!Hero.assign(h, e, ship, map)) {
					return "Unable to assign";
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("unassignHero", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Hero h = Hero.get(cmd.getInt("hero"), map);
				if (h == null) { return "Unknown hero " + cmd.getInt("hero"); }
				if (h.getInShip() != null) {
					Fleet f = e.fleet(h.getInShip());
					if (f == null || f.location == null || !map.isFriendly(e, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
						return "Hero cannot be unassigned because ship fleet not at friendly location.";
					}
				}
				if (!Hero.unassign(h, e)) {
					return "Unable to unassign";
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("requestBesiege", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				e.askAlliesToHelpBesiege(c, cw);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("dismissBesiegeRequest", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				Empire from = map.getEmpire(cmd.getInt("from"));
				if (from == null) { return "Unknown from " + cmd.getInt("from"); }
				City c = map.getCity(cmd.getInt("city"));
				if (c == null) { return "Unknown city " + cmd.getInt("city"); }
				for (int i = 0; i < e.besiegeRequests.size(); i++) {
					BesiegeRequest req = e.besiegeRequests.get(i);
					if (req.from == from && req.city == c) {
						e.besiegeRequests.remove(i);
						i--;
					}
				}
				from.diplomacyNotices.add(new DiplomacyNotice(DiplomacyNotice.Type.BESIEGE_REQUEST_DENIED, e, c));
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("setEmpirePrefix", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Unknown empire " + cmd.getInt("empire"); }
				e.prefix = cmd.optString("prefix", null);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("needResync", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				if (!cw.needResyncReceived) {
					String report = "needResync received";
					report += "\n" + cmd.toString();
					report += "\ncombatInfo " + cw.combatInfo;
					report += "\nMultiplayerCampaignCombatSetupIntent " + cw.multiplayerCampaignCombatSetupIntent;
					if (cw.multiplayerCampaignCombatSetupIntent != null) {
						report += "\n    countdownLeft " + cw.multiplayerCampaignCombatSetupIntent.countdownLeft;
						report += "\n    countdownOverdue " + cw.multiplayerCampaignCombatSetupIntent.countdownOverdue;
						report += "\n    readySent " + cw.multiplayerCampaignCombatSetupIntent.readySent;
						report += "\n    frameQueueSize " + cw.frameQueueSize();
						report += "\n    accumulatedMs " + cw.accumulatedMs;
						report += "\n    spectate " + cw.multiplayerCampaignCombatSetupIntent.spectate;
						report += "\n    readyIDsReceived";
						for (int id : cw.multiplayerCampaignCombatSetupIntent.readyIDsReceived) {
							report += " " + id;
						}
					}
					if (cw.combatInfo != null) {
						report += "\nsides " + Empire.nameList(cw.combatInfo.attackers(cw.map), Lang.currentLocale) + " vs " + Empire.nameList(cw.combatInfo.defenders(cw.map), Lang.currentLocale);
					}
					report += "\nmapAge " + map.age;
					report += "\nscreen " + AirshipGame.instance.s.getClass().getSimpleName();
					if (AirshipGame.instance.s instanceof UniScreen) {
						report += "\nintent " + ((UniScreen) AirshipGame.instance.s).intent.getClass().getSimpleName();
					}
					report += "\nping " + AirshipGame.instance.ping();
					report += "\nframeQueue " + cw.frameQueue.size() + " avgTickInterval " + AirshipGame.instance.client.avgTickInterval();
					report += cw.getPlayerSummary();
					try {
						System.err.println("");
						System.err.println("@ " + System.currentTimeMillis() + " " + new DateTime());
						System.err.println(report);
						System.err.println("");
					} catch (Exception e2) {}
					AirshipGame.instance.reportDebug(report);
				}
				cw.needResyncSentOrReceived = true;
				cw.needResyncReceived = true;
				cw.needResync = true;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("ping", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				return null; // Ignore.
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("goAway", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				return null; // Ignore for now.
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("strategicChecksum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				if (w.desyncDetected) { return null; }
				int age = cmd.getInt("age");
				int combatTime = cmd.getInt("combatTime");
				StoredState state = null;
				for (StoredState ss : w.storedStates) {
					if (ss.age == age && ss.combatTime == combatTime) {
						state = ss;
						break;
					}
				}
				if (state == null) {
					Pair<Integer, Integer> k = new Pair<Integer, Integer>(age, combatTime);
					if (!w.futureChecksums.containsKey(k)) {
						w.futureChecksums.put(k, new ArrayList<JSONObject>());
					}
					w.futureChecksums.get(k).add(cmd);
				} else {
					checkStrategicChecksum(cmd, state, w);
				}			
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("buildShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = new Airship(new JSONObject(Compression.decompressFromString(cmd.getString("ship"))));
				City c = m.getCity(cmd.getInt("city"));
				if (m.owner(c).getMoney() < ship.getCost()) {
					return "Ship too expensive, costs $" + ship.getCost() + " and we only have $" + m.owner(c).getMoney() + ".";
				}
				ship.networkID = m.owner(c).getNextShipID();
				if (ship.type.onGround && !ship.type.mobile && cmd.has("landscapeGY")) {
					c.landscapeVersion++;
					c.ground.doLandscapingForBuilding(ship, ship.getX(), cmd.getInt("landscapeGY"), ship.flipped);
					m.owner(c).setMoney(m.owner(c).getMoney() - cmd.getInt("landscapeCost"));
				}
				c.constructing.add(new MapLocation.ConstructionEntry(ship, ship.getCost(), c.constructionEntryIDCounter++, 0));
				m.owner(c).setMoney(m.owner(c).getMoney() - ship.getCost());
				return null;
			}
			public JSONObject archive(JSONObject cmd) {
				return copyExcept(cmd, "ship");
			}
		});
		EXECS.put("cityUpgrade", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				CityUpgradeType cut = CityUpgradeType.ofName(cmd.getString("upgrade"));	
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.owner(c);
				int cost = cut.getCost(e, c, m);
				if (!cut.isAvailable(e, c, m)) {
					return "Upgrade is not available.";
				}
				if (cost > e.getMoney()) {
					return "Upgrade is too expensive at $" + cost + ", as we only have $" + e.getMoney();
				}
				if (cut.consumesBonus != null) {
					e.bonuses.set(cut.consumesBonus.ordinal(), false);
					e.rewardedBonuses.set(cut.consumesBonus.ordinal(), false);
				}
				c.constructing.add(new MapLocation.ConstructionEntry(cut, cost, c.constructionEntryIDCounter++, 0));
				e.setMoney(e.getMoney() - cost);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("instabuildUpgrade", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				CityUpgradeType cut = CityUpgradeType.ofName(cmd.getString("upgrade"));	
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.owner(c);
				int cost = cut.getInstabuildCost();
				if (!cut.isAvailable(e, c, m)) {
					return "Upgrade is not available.";
				}
				if (cost > e.getMoney()) {
					return "Upgrade is too expensive at $" + cost + ", as we only have $" + e.getMoney();
				}
				if (cut.consumesBonus != null) {
					e.bonuses.set(cut.consumesBonus.ordinal(), false);
					e.rewardedBonuses.set(cut.consumesBonus.ordinal(), false);
				}
				c.upgrades.add(cut);
				e.setMoney(e.getMoney() - cost);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("destroyCityUpgrade", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				CityUpgradeType cut = CityUpgradeType.ofName(cmd.getString("upgrade"));	
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.owner(c);
				if (!c.upgrades.contains(cut)) { return "City does not have upgrade."; }
				if (cut.consumesBonus != null) {
					e.bonuses.set(cut.consumesBonus.ordinal(), true);
					e.rewardedBonuses.set(cut.consumesBonus.ordinal(), true);
				}
				c.upgrades.remove(cut);
				e.setMoney(e.getMoney() + cut.getDestroyRefund());
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("doLandscapingForShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = new Airship(new JSONObject(Compression.decompressFromString(cmd.getString("ship"))));
				City c = m.getCity(cmd.getInt("city"));
				if (ship.type != ShipType.BUILDING) {
					return "Cannot do landscaping for non-building.";
				}
				if (!cmd.has("landscapeGY")) { return "No landscapeGY."; }
				c.landscapeVersion++;
				c.ground.doLandscapingForBuilding(ship, ship.getX(), cmd.getInt("landscapeGY"), ship.flipped);
				m.owner(c).setMoney(m.owner(c).getMoney() - cmd.getInt("landscapeCost"));
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("renameCity", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				String name = cmd.getString("name").trim();
				name = name.substring(0, Math.min(name.length(), 80));
				if (name.isEmpty()) {
					return "New name is empty string.";
				}
				for (Empire e : m.empires) {
					for (City c2 : e.cities) {
						if (c2.name.equals(name)) {
							return "Another city named " + name + " exists.";
						}
					}
				}
				c.name = name;
				return null;
				/*ship.originalArms = m.owner(c).getArms(); // MERGEME Where is this meant to go!?
				c.constructing.add(new MapLocation.ConstructionEntry(ship, ship.getCost(), c.constructionEntryIDCounter++));
				m.owner(c).money -= ship.getCost();*/
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("cancelShipConstruction", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				int id = cmd.getInt("id");
				MapLocation.ConstructionEntry ce = null;
				for (MapLocation.ConstructionEntry e : c.constructing) {
					if (e.id == id) {
						ce = e;
					}
				}
				if (ce == null) { return "Construction entry not found."; }
				
				m.owner(c).setMoney(m.owner(c).getMoney() + c.refund(ce, m.owner(c).bonuses, m));
				if (ce.original != null) {
					if (ce.original.type == ShipType.BUILDING) {
						c.defences.add(ce.original);
					} else {
						m.owner(c).addShipAt(ce.original, c, m);
					}
				}
				if (ce.upgrade != null && ce.upgrade.consumesBonus != null) {
					m.owner(c).bonuses.set(ce.upgrade.consumesBonus.ordinal(), true);
					m.owner(c).rewardedBonuses.set(ce.upgrade.consumesBonus.ordinal(), true);
				}
				c.constructing.remove(ce);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("reorderConstructionQueue", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				City c = map.getCity(cmd.getInt("city"));
				int id = cmd.getInt("id");
				MapLocation.ConstructionEntry ce = null;
				for (MapLocation.ConstructionEntry e : c.constructing) {
					if (e.id == id) {
						ce = e;
					}
				}
				if (ce == null) { return "Construction entry not found."; }
				int oldIndex = c.constructing.indexOf(ce);
				c.constructing.remove(ce);
				switch (cmd.getInt("shift")) {
					case MapLocation.REORDER_TOP:
						c.constructing.add(0, ce);
						break;
					case MapLocation.REORDER_BOTTOM:
						c.constructing.add(ce);
						break;
					case MapLocation.REORDER_UP:
						c.constructing.add(StrictMath.max(0, oldIndex - 1), ce);
						break;
					case MapLocation.REORDER_DOWN:
						c.constructing.add(StrictMath.min(c.constructing.size(), oldIndex + 1), ce);
						break;
					default:
						c.constructing.add(oldIndex, ce);
						break;
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("rebuildShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship newShip = new Airship(new JSONObject(Compression.decompressFromString(cmd.getString("newShip"))));
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.owner(c);
				Airship original = getShip(cmd, m);
				if (original == null) {
					return "Ship not found.";
				}
				int cost = newShip.getRefitCostFrom(original, e.isPlayerControlled());
				newShip.networkID = original.networkID;
				newShip.version = original.version++;
				if (cmd.optBoolean("keepOldPosition", true)) {
					newShip.setX(original.getX());
					newShip.setY(original.getY());
				}
				newShip.setName(original.getName());
				newShip.updateOriginalDesign();
				if (newShip.type == ShipType.BUILDING) {
					ShipArrayList sl = (ShipArrayList) c.shipList(m);
					sl.ships.remove(original);
					Placement p = PlaceShipTool.getPlacement(newShip, newShip.getX(), newShip.getY(), original.flipped, c.ground, sl, true, 1);
					if (!p.succeeded) { return "Cannot place building."; }
					c.ground.doLandscapingForBuilding(newShip, p.x, p.landscapeGY, p.flipped);
					c.landscapeVersion++;
					newShip.setX(p.x);
					newShip.setY(p.y);
					cost += p.landscapeCost;
				}
				e.setMoney(e.getMoney() - cost);
				c.constructing.add(new MapLocation.ConstructionEntry(newShip, original, cost, MapLocation.ConstructionEntry.Type.valueOf(cmd.getString("constructionType")), c.constructionEntryIDCounter++, 0));
				e.remove(original, m);				
				return null;
			}
			public JSONObject archive(JSONObject cmd) {
				return copyExcept(cmd, "newShip");
			}
		});
		EXECS.put("placeDefencesShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) {
					return "Ship not found.";
				}
				if (ship.nonCombat()) { return "Non-combat ship."; }
				City c = m.getCity(cmd.getInt("city"));
				Fleet garrison = m.getGarrison(c);
				if (ship.type.mobile && garrison != null && garrison.reserve.contains(ship)) {
					garrison.reserve.remove(ship);
					garrison.actives.add(ship);
				}
				ship.version++;
				ship.setX(cmd.getDouble("x"));
				ship.setY(cmd.getDouble("y"));
				ship.moveTo = new Pt(ship.getX(), ship.getY());
				ship.setFlipped(cmd.getBoolean("flipped"), null);
				ship.flipTo = cmd.getBoolean("flipped");
				ship.lastPlaced = new Pt(ship.getX(), ship.getY());
				ship.lastPlacedFlipped = ship.flipped;
				ship.resetWeaponBarrels();
				ship.resetTentacles();
				ship.originalArms = m.owner(c).getArms();
				if (ship.type.onGround && !ship.type.mobile && cmd.has("landscapeGY")) {
					c.ground.doLandscapingForBuilding(ship, ship.getX(), cmd.getInt("landscapeGY"), ship.flipped);
					c.landscapeVersion++;
					m.owner(c).setMoney(m.owner(c).getMoney() - cmd.getInt("landscapeCost"));
				}
				ship.version++;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("placeConstructionShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				Airship ship = null;
				for (MapLocation.ConstructionEntry ce : c.constructing) {
					if (ce.ship != null && ce.ship.networkID.equals(cmd.getString("networkID"))) {
						ship = ce.ship;
					}
				}
				if (ship == null) {
					return "Ship not found.";
				}
				if (ship.nonCombat()) { return "Ship is non-combat."; }
				ship.version++;
				ship.setX(cmd.getDouble("x"));
				ship.setY(cmd.getDouble("y"));
				ship.moveTo = new Pt(ship.getX(), ship.getY());
				ship.setFlipped(cmd.getBoolean("flipped"), null);
				ship.flipTo = cmd.getBoolean("flipped");
				ship.lastPlaced = new Pt(ship.getX(), ship.getY());
				ship.lastPlacedFlipped = ship.flipped;
				ship.resetWeaponBarrels();
				ship.resetTentacles();
				ship.originalArms = m.owner(c).getArms();
				if (ship.type.onGround && !ship.type.mobile && cmd.has("landscapeGY")) {
					c.ground.doLandscapingForBuilding(ship, ship.getX(), cmd.getInt("landscapeGY"), ship.flipped);
					c.landscapeVersion++;
					m.owner(c).setMoney(m.owner(c).getMoney() - cmd.getInt("landscapeCost"));
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("reserveDefencesShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) {
					return "Ship not found.";
				}
				Fleet garrison = m.getGarrison(m.getCity(cmd.getInt("city")));
				if (ship.type.mobile && garrison != null && garrison.actives.contains(ship)) {
					garrison.actives.remove(ship);
					garrison.reserve.add(ship);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("giftShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) { return "Ship not found"; }
				Empire from = m.getEmpire(cmd.getInt("from"));
				if (from == null) { return "From empire not found"; }
				Empire to = m.getEmpire(cmd.getInt("to"));
				if (to == null) { return "To empire not found"; }
				Fleet fromFleet = from.fleet(ship);
				if (fromFleet == null) { return "Fleet not found"; }
				if (!to.cities.contains(fromFleet.location)) { return "To-empire does not own fleet location"; }
				m.clearHeroFrom(ship);
				City loc = (City) fromFleet.location;
				Fleet toFleet = m.getGarrison(loc);
				if (toFleet == null) {
					toFleet = new Fleet(loc, m);
					to.getFleets().add(toFleet);
				}
				fromFleet.actives.remove(ship);
				fromFleet.reserve.remove(ship);
				toFleet.reserve.add(ship);
				if (fromFleet.actives.isEmpty() && fromFleet.reserve.isEmpty()) {
					from.getFleets().remove(fromFleet);
					fromFleet.broadcastDestroyed(m);
				}
				to.diplomacyNotices.add(new DiplomacyNotice(from, ship, loc));
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("scrapShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) { return "Ship not found"; }
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				e.scrap(ship, m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("scuttleShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) { return "Ship not found"; }
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				e.remove(ship, m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("renameShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				if (ship == null) { return "Ship not found"; }
				ship.setName(cmd.getString("newName"));
				ship.version++;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("doExpedition", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Fleet f = m.getFleet(cmd.getInt("fleet"));
				if (f == null) { return "Fleet not found"; }
				ArrayList<Airship> ships = new ArrayList<Airship>();
				JSONArray s = cmd.getJSONArray("ships");
				for (int i = 0; i < s.length(); i++) {
					Airship ship = f.getShip(s.getString(i));
					if (ship != null) {
						ships.add(ship);
					}
				}
				if (ships.isEmpty()) { return "No ships selected"; }
				Empire e = (Empire) m.owner(f);
				if (m.eraModifier == null || m.eraModifier.expeditions == null || m.eraModifier.expeditions.get(e.bonuses) == null || m.eraModifier.expeditions.get(e.bonuses).isEmpty()) {
					return "Not in expedition era";
				}
				e.sendExpedition(ships, f, m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("anchorShip", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Airship ship = getShip(cmd, m);
				Fleet f = m.getFleet(cmd.getInt("fleetID"));
				if (f == null) { return "Fleet not found"; }
				if (ship == null) { return "Ship not found"; }
				if (!f.actives.contains(ship) && !f.reserve.contains(ship)) { return "Ship not in fleet"; }
				if (cmd.getBoolean("anchor") && !f.anchoredIDs.contains(ship.networkID)) {
					f.anchoredIDs.add(ship.networkID);
				} else {
					f.anchoredIDs.remove(ship.networkID);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("sendFleet", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Fleet f = m.getFleet(cmd.getInt("id"));
				if (f == null) { return "Fleet not found"; }
				ArrayList<Airship> ships = new ArrayList<Airship>();
				JSONArray s = cmd.getJSONArray("ships");
				for (int i = 0; i < s.length(); i++) {
					Airship ship = f.getShip(s.getString(i));
					if (ship != null) {
						ships.add(ship);
					}
				}
				if (ships.isEmpty()) { return "No ships selected"; }
				if (m.travelTo(f, ships, m.getMapLocation(cmd.getInt("destination")), SupplySplitMode.valueOf(cmd.getString("splitSupply")), cmd.getBoolean("besiege")) != null) {
					return null;
				} else {
					return "travelTo failed";
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("breakOut", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Fleet f = m.getFleet(cmd.getInt("id"));
				if (f == null) { return "Fleet not found"; }
				f.breakOut = true;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("stopSiege", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Fleet f = m.getFleet(cmd.getInt("id"));
				if (f == null) { return "Fleet not found"; }
				f.besiege = false;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("intercept", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Fleet fleet = m.getFleet(cmd.getInt("fleet"));
				if (fleet == null) { return "Fleet not found"; }
				FleetOwner owner = m.owner(fleet);
				ArrayList<Airship> ships = new ArrayList<Airship>();
				JSONArray s = cmd.getJSONArray("ships");
				for (int i = 0; i < s.length(); i++) {
					Airship ship = fleet.getShip(s.getString(i));
					if (ship != null) {
						ships.add(ship);
					}
				}
				if (ships.isEmpty()) { return "No ships selected"; }
				Fleet target = m.getFleet(cmd.getInt("target"));
				if (target == null) { return "Target fleet not found"; }
				FleetOwner targetOwner = m.owner(target);
				if (owner instanceof Empire && targetOwner instanceof Empire && m.getRelationship((Empire) owner, (Empire) targetOwner).level != Relationship.Level.WAR) {
					return "Must be at war to intercept.";
				}
				if (ships.containsAll(fleet.actives) && ships.containsAll(fleet.reserve)) {
					if (fleet.doIntercept(target, m)) {
						fleet.broadcastChangedDirection(m);
					} else {
						return "Intercept failed";
					}
				} else {
					Pt icept = fleet.getFlightIntercept(target, ships, m);
					if (icept != null) {
						SupplySplitMode splitSupply = SupplySplitMode.valueOf(cmd.getString("splitSupply"));
						
						int supplyToGiveNewFleet = 0;
						if (owner instanceof Empire) {
							supplyToGiveNewFleet = splitSupply.calculate((Empire) owner, fleet, ships, null, icept, m);
						}
						
						Fleet newFleet = fleet.split(m, ships, supplyToGiveNewFleet);
						owner.getFleets().add(newFleet);
						if (fleet.location != null && m.defender(fleet.location) == owner) {
							fleet.location.layoutGarrison(fleet);
						}
						newFleet.doIntercept(target, m);
						newFleet.broadcastChangedDirection(m);
					} else {
						return "Intercept for split fleet cannot be computed";
					}
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("startCoronation", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire does not exist"; }
				if (!e.cities.contains(c)) { return "Empire does not hold city"; }
				if (c.isTown) { return "City is town"; }
				if (e.isCoronating()) { return "Already coronating"; }
				if (!e.isCoronationReady(m)) { return "Empire is not ready for coronation"; }
				c.startCoronation();
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("cancelCoronation", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				if (!e.cities.contains(c)) { return "Empire does not hold city"; }
				c.cancelCoronation(null, /* intentionally */ true);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("startFinalRitual", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found."; }
				if (!e.cities.contains(c)) { return "City not owned."; }
				if (!c.isRitualSite) { return "Not a ritual site."; }
				if (e.isDoingFinalRitual()) { return "Already doing ritual"; }
				if (!e.isFinalRitualReady(m)) { return "Not ready."; }
				c.startFinalRitual();
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("cancelFinalRitual", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found."; }
				if (!e.cities.contains(c)) { return "City not owned."; }
				e.cancelFinalRitual(c, /* intentionally */ true, m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("sendSpy", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found."; }
				if (e.spies.size() < EmpireStat.MAX_SPIES.get(e.bonuses)) {
					e.spies.add(new Spy(m.getCity(cmd.getInt("city"))));
					return null;
				} else {
					return "At spy limit.";
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("recallSpy", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found."; }
				Spy spy = e.getSpyFor(m.getCity(cmd.getInt("city")));
				if (spy == null) { return "Spy not found."; }
				e.spies.remove(spy);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("citySpyAction", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Spy.CitySpyAction a = Spy.CitySpyAction.valueOf(cmd.getString("action"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				City c = m.getCity(cmd.getInt("city"));
				m.doCitySpyAction(a, e, c);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("shipSpyAction", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Spy.ShipSpyAction a = Spy.ShipSpyAction.valueOf(cmd.getString("action"));
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				City c = m.getCity(cmd.getInt("city"));
				Airship ship = getShip(cmd, m);
				if (ship == null) { return "Ship not found"; }
				Fleet garrison = m.getGarrison(c);
				if (!c.getDefences().contains(ship) && (garrison == null || !garrison.actives.contains(ship))) { return "Ship not at city"; }
				m.doShipSpyAction2(a, e, c, ship);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("setResearch", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				Tech.Choice c = Tech.ofName(cmd.getString("tech")).getChoice(cmd.getString("choice"));
				Tech.Choice oldC = e.research;
				if (c == oldC) { return null; } // Nothing to do.
				if (c.tech.available(e)) {
					e.research = c;
					e.researchQueue.clear();
				} else {
					ArrayList<Tech.Choice> prereqs = c.getAllPrerequisitesIncludingThis();
					e.research = null;
					e.researchQueue.clear();
					lp: for (Tech.Choice c2 : prereqs) {
						for (Tech.Choice c3 : c2.tech.choices) {
							if (e.techs.contains(c3)) { continue lp; }
						}
						if (e.research == null) {
							e.research = c2;
						} else {
							e.researchQueue.add(c2);
						}
					}
				}
				
				if (oldC != null) {
					e.partialResearchPoints.put(oldC, e.researchPoints);
				}
				e.researchPoints = e.partialResearchPoints.containsKey(e.research) ? e.partialResearchPoints.get(e.research) : 0;
				e.researchPoints += e.unassignedResearchPoints;
				e.unassignedResearchPoints = 0;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("addResearch", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				Tech.Choice c = Tech.ofName(cmd.getString("tech")).getChoice(cmd.getString("choice"));
				Tech.Choice oldC = e.research;
				if (c.tech.available(e)) {
					if (e.research == null) {
						e.research = c;
					} else if (!e.researchQueue.contains(c)) {
						e.researchQueue.add(c);
					}
				} else {
					ArrayList<Tech.Choice> prereqs = c.getAllPrerequisitesIncludingThis();
					lp: for (Tech.Choice c2 : prereqs) {
						for (Tech.Choice c3 : c2.tech.choices) {
							if (e.techs.contains(c3) || e.researchQueue.contains(c3)) { continue lp; }
						}
						if (e.research == null) {
							e.research = c2;
						} else if (!e.researchQueue.contains(c2) && e.research != c2) {
							e.researchQueue.add(c2);
						}
					}
				}
				
				if (oldC != null && oldC != e.research) {
					e.partialResearchPoints.put(oldC, e.researchPoints);
				}
				if (oldC != e.research) {
					e.researchPoints = e.partialResearchPoints.containsKey(e.research) ? e.partialResearchPoints.get(e.research) : 0;
				}
				e.researchPoints += e.unassignedResearchPoints;
				e.unassignedResearchPoints = 0;
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("chooseTakeoverMethod", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				City c = m.getCity(cmd.getInt("city"));
				c.takeoverMethod = TakeoverMethod.ofName(cmd.getString("method"));
				if (w.has(ConquestToggle.REPUTATION)) {
					m.owner(c).changeReputation(c.takeoverMethod.repChange(m.owner(c), c.previousOwner), m);
				}
				c.takeoverNeeded = false;
				c.postTakeoverMethod = null;
				c.postTakeoverCooldown = 0;
				Hero.processHeroEvent(HeroEvent.takeover(m.owner(c), c, c.takeoverMethod), m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("surrenderStrategic", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				if (m.empires.size() < 2) { return "Only " + m.empires.size() + " empires left"; }
				Empire e = m.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				HashMap<City, Empire> newOwners = new HashMap<City, Empire>();
				for (City c : e.cities) {
					City closest = null;
					int closestD2 = 0;
					for (City c2 : m.cities()) {
						if (e.cities.contains(c2)) { continue; }
						int d2 = (c.x - c2.x) * (c.x - c2.x) + (c.y - c2.y) * (c.y - c2.y);
						if (closest == null || d2 < closestD2) {
							closest = c2;
							closestD2 = d2;
						}
					}
					newOwners.put(c, m.owner(closest));
				}
				for (City c : e.cities) {
					Empire newO = newOwners.get(c);
					newO.cities.add(c);
					Hero.processHeroEvent(HeroEvent.cityGained(newO, c), m);
				}
				for (Fleet f : e.getFleets()) {
					f.broadcastDestroyed(m);
				}
				m.empires.remove(e);
				e.cities.clear();
				e.getFleets().clear();
				m.isAdjacents.clear();
				m.clearPathCaches();
				for (Empire e2 : m.empires) {
					Hero.processHeroEvent(HeroEvent.empireDestroyed(e2, e), m);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("magicWinStrategic", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				if (!MAGIC_WIN_ENABLED) { return "Magic win not enabled"; }
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				for (Empire e2 : map.empires) {
					if (e2 != e) {
						for (Fleet f : e2.getFleets()) {
							f.broadcastDestroyed(map);
						}
						e2.getFleets().clear();
						e.cities.addAll(e2.cities);
						e2.cities.clear();
					}
				}
				map.empires.clear();
				map.empires.add(e);
				map.isAdjacents.clear();
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("magicLoseStrategic", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				if (!MAGIC_WIN_ENABLED) { return "Magic lose not enabled"; }
				Empire e = map.getEmpire(cmd.getInt("empire"));
				if (e == null) { return "Empire not found"; }
				for (Fleet f : e.getFleets()) {
					f.broadcastDestroyed(map);
				}
				e.getFleets().clear();
				for (Empire e2 : map.empires) {
					if (e2 != e) {
						e2.cities.addAll(e.cities);
						e.cities.clear();
						break;
					}
				}
				map.empires.remove(e);
				map.isAdjacents.clear();
				for (Empire e2 : map.empires) {
					Hero.processHeroEvent(HeroEvent.empireDestroyed(e2, e), map);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("chat", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				StrategicPlayerInfo pi = w.channelPlayers.get(cmd.getInt("id"));
				if (pi != null) {
					w.chatMessages.add(new StrategicChatMsg(pi, pi.name + ": " + cmd.getString("text"), DateTime.now(), StrategicLobbyScreen.getCOA(pi, w.channelPlayers), StrategicLobbyScreen.getArmsCadence(pi, w.channelPlayers)));
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("chatForAllies", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				StrategicPlayerInfo pi = w.channelPlayers.get(cmd.getInt("id"));
				if (pi != null && w.player != null) {
					JSONArray recipients = cmd.getJSONArray("recipientEmpires");
					for (int i = 0; i < recipients.length(); i++) {
						if (recipients.getInt(i) == w.player.id) {
							w.allyChatMessages.add(new StrategicChatMsg(pi, pi.name + ": " + cmd.getString("text"), DateTime.now(), StrategicLobbyScreen.getCOA(pi, w.channelPlayers), StrategicLobbyScreen.getArmsCadence(pi, w.channelPlayers)));
							break;
						}
					}
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return null; }
		});
		EXECS.put("setCampaignSpeed", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				Speed s = Speed.valueOf(cmd.getString("speed"));
				if (w.speedVoters != null) {
					w.speedVoters.put(cmd.getInt("voterID"), s);
					w.updateSpeedVote();
				} else {
					w.speed = s;
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("makeChallenge", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire other = m.getEmpire(cmd.getInt("other"));
				Empire player = m.getEmpire(cmd.getInt("player"));
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				if (player == null) { return "Player empire not found"; }
				if (other == null) { return "Other empire not found"; }
				m.getRelationship(player, other).makeChallenge(new Relationship.Challenge(cmd.getJSONObject("challenge")), m);
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("dealWithChallenge", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire other = m.getEmpire(cmd.getInt("other"));
				Empire player = m.getEmpire(cmd.getInt("player"));
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				if (player == null) { return "Player empire not found"; }
				if (other == null) { return "Other empire not found"; }
				Relationship rel = m.getRelationship(player, other);
				if (rel.challenge != null) {
					if (cmd.getBoolean("respond")) {
						rel.challengeResponse(m, cmd.optString("detail1", null), cmd.optString("detail2", null));
					} else {
						rel.noChallengeResponse(m, cmd.optString("detail1", null), cmd.optString("detail2", null));
					}
					return null;
				} else {
					return "No challenge found.";
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("forceOffer", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				if (w.combatInfo != null) {
					w.postCombatCommands.put(cmd);
					return null;
				}
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				JSONObject o = cmd.getJSONObject("offer");
				Empire a = m.getEmpire(o.getInt("relA"));
				Empire b = m.getEmpire(o.getInt("relB"));
				Empire player = m.getEmpire(cmd.getInt("player"));
				if (a == null || b == null || player == null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "recipient_does_not_exist");
					}
					return "Missing participants a = " + a + ", b = " + b + ", player = " + player;
				}
				Offer offer = new Offer(o, m);
				String invalidReason = offer.rel.getInvalidReason(offer, player, m, false);
				if (invalidReason != null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				offer.rel.force(offer, player, m, true);
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("acceptOffer", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				if (w.combatInfo != null) {
					w.postCombatCommands.put(cmd);
					return null;
				}
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				JSONObject o = cmd.getJSONObject("offer");
				Empire a = m.getEmpire(o.getInt("relA"));
				Empire b = m.getEmpire(o.getInt("relB"));
				if (a == null || b == null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "recipient_does_not_exist");
					}
					return "Missing participants a = " + a + ", b = " + b;
				}
				Offer offer = new Offer(o, m);
				String invalidReason = offer.rel.getInvalidReason(offer, null, m, false);
				if (invalidReason != null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				if (offer.rel.contains(w.player)) {
					w.checkAchievements(offer);
				}
				offer.rel.agree(offer, m);
				for (Offer o2 : w.playerOffers) {
					if (o2.rel == offer.rel) {
						w.playerOffers.remove(o2);
						break;
					}
				}
				if (a.diplomacyAI != null) {
					a.diplomacyAI.getRecord(b).addAgreed(offer);
				}
				if (b.diplomacyAI != null) {
					b.diplomacyAI.getRecord(a).addAgreed(offer);
				}
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("cancelOffer", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire other = m.getEmpire(cmd.getInt("other"));
				Empire player = m.getEmpire(cmd.getInt("player"));
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				if (other != null && player != null) {
					m.getRelationship(player, other).rejectOffer(player);
					for (Offer o : w.playerOffers) {
						if (o.rel == m.getRelationship(player, other)) {
							w.playerOffers.remove(o);
							break;
						}
					}
					return null;
				} else {
					return "Missing participants player = " + player + ", other = " + other;
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("makeOffer", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire offerer = m.getEmpire(cmd.getInt("player"));
				JSONObject o = cmd.getJSONObject("offer");
				Empire a = m.getEmpire(o.getInt("relA"));
				Empire b = m.getEmpire(o.getInt("relB"));
				if (a != offerer && b != offerer) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "cannot_force_agreements_on_other_empires");
					}
					return "cannot_force_agreements_on_other_empires";
				}
				if (a == null || b == null || offerer == null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "recipient_does_not_exist");
					}
					return "Missing participants a = " + a + ", b = " + b + ", offerer = " + offerer;
				}
				Offer offer = new Offer(o, m);
				String invalidReason = offer.rel.getInvalidReason(offer, null, m, false);
				if (invalidReason != null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				
				Relationship rel = m.getRelationship(a, b);
				
				if (rel.getUltimatum(m) == null) {
					rel.makeOffer(offer, offerer, m);

					for (Offer o2 : w.playerOffers) {
						if (o2.rel == offer.rel) {
							w.playerOffers.remove(o2);
							break;
						}
					}

					if (w.player == rel.other(offerer)) {
						w.playerOffers.add(offer);
					}
					
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
					}
					
					return null;
				} else {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "cannot_make_offers_during_an_ultimatum");
					}
					return "cannot_make_offers_during_an_ultimatum";
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("makeUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap m, CampaignWorld w) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Ultimatum ult;
				try {
					ult = new Ultimatum(cmd.getJSONObject("ultimatum"), m, null);
				} catch (Exception e) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "recipient_does_not_exist");
					}
					return "Ult constructor failure: " + e.getMessage();
				}
				String invalidReason = ult.getInvalidReason(w.map);
				if (invalidReason != null) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				
				if (m.toggles.contains(ConquestToggle.REPUTATION) && ult.forcer.getReputation() < EmpireStat.MIN_ULTIMATUM_REPUTATION.get(ult.forcer.bonuses)) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "need_more_rep_to_make_ultimatum");
					}
					return "need_more_rep_to_make_ultimatum";
				}
				
				if (ult.getRelationship().getUltimatum(w.map) != null && ult.getRelationship().getUltimatum(w.map).forcer != ult.forcer) {
					if (w.ssForDiplomacyCallback != null) {
						w.ssForDiplomacyCallback.diplomacyCallback(callbackID, "cannot_reply_to_ultimatum_with_ultimatum");
					}
					return "cannot_reply_to_ultimatum_with_ultimatum";
				}
				
				ult.getRelationship().makeUltimatum(ult, w.map);
												
				if (w.ssForDiplomacyCallback != null) {
					w.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("cancelUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				if (cw.ssForDiplomacyCallback != null) {
					cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				Empire a = map.getEmpire(cmd.getInt("relA"));
				Empire b = map.getEmpire(cmd.getInt("relB"));
				Relationship rel = map.getRelationship(a, b);
				if (rel.getUltimatum(map) != null) {
					rel.cancelUltimatum(map);
					return null;
				} else {
					return "No ultimatum to cancel";
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("accedeToUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				if (cw.combatInfo != null) {
					cw.postCombatCommands.put(cmd);
					return null;
				}
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire a = map.getEmpire(cmd.getInt("relA"));
				Empire b = map.getEmpire(cmd.getInt("relB"));
				Relationship rel = map.getRelationship(a, b);
				String invalidReason = rel.getUltimatumInvalidReason(map);
				if (invalidReason != null) {
					if (cw.ssForDiplomacyCallback != null) {
						cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				rel.accedeToUltimatum(map);
				if (cw.ssForDiplomacyCallback != null) {
					cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("defyUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire a = map.getEmpire(cmd.getInt("relA"));
				Empire b = map.getEmpire(cmd.getInt("relB"));
				Relationship rel = map.getRelationship(a, b);
				String invalidReason = rel.getUltimatumInvalidReason(map);
				if (invalidReason != null) {
					if (cw.ssForDiplomacyCallback != null) {
						cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				rel.defyUltimatum(map);
				if (cw.ssForDiplomacyCallback != null) {
					cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("enforceUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				if (cw.combatInfo != null) {
					cw.postCombatCommands.put(cmd);
					return null;
				}
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				Empire a = map.getEmpire(cmd.getInt("relA"));
				Empire b = map.getEmpire(cmd.getInt("relB"));
				Relationship rel = map.getRelationship(a, b);
				String invalidReason = rel.getUltimatumInvalidReason(map);
				if (invalidReason != null) {
					if (cw.ssForDiplomacyCallback != null) {
						cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, invalidReason);
					}
					return invalidReason;
				}
				rel.enforceUltimatum(map);
				if (cw.ssForDiplomacyCallback != null) {
					cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				return null;
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
		EXECS.put("dontEnforceUltimatum", new CommandExecutor() {
			@Override
			public String run(JSONObject cmd, WorldMap map, CampaignWorld cw) {
				String callbackID = cmd.optString("messageArrivedCallbackID", "?");
				if (cw.ssForDiplomacyCallback != null) {
					cw.ssForDiplomacyCallback.diplomacyCallback(callbackID, null);
				}
				Empire a = map.getEmpire(cmd.getInt("relA"));
				Empire b = map.getEmpire(cmd.getInt("relB"));
				Relationship rel = map.getRelationship(a, b);
				String invalidReason = rel.getUltimatumInvalidReason(map);
				if (invalidReason == null) {
					rel.dontEnforceUltimatum(map);
					return null;
				} else {
					return invalidReason;
				}
			}
			public JSONObject archive(JSONObject cmd) { return cmd; }
		});
	}
	
	public boolean hasVoteForSpeed(Speed s) {
		for (Map.Entry<Integer, Speed> kv : speedVoters.entrySet()) {
			if (!channelPlayers.containsKey(kv.getKey()) || !map.empires.contains(channelPlayers.get(kv.getKey()).empire)) {
				continue;
			}
			Speed sv = kv.getValue();
			if (s == sv) { return true; }
		}
		return false;
	}
	
	public void updateSpeedVote() {
		speed = Speed.CHEETAH;
		boolean allStop = true, anyStop = false;
		ArrayList<Integer> keys = new ArrayList<Integer>(speedVoters.keySet());
		Collections.sort(keys);
		for (int id : keys) {
			if (!channelPlayers.containsKey(id) || !map.empires.contains(channelPlayers.get(id).empire)) {
				continue;
			}
			Speed sv = speedVoters.get(id);
			if (sv == Speed.STOP) {
				// Can't typically force a stop, so this counts as a vote for normal.
				sv = Speed.NORMAL;
				// But maybe we have anyone can stop enabled.
				anyStop = true;
			} else {
				// Not everyone wants stop.
				allStop = false;
			}
			if (sv.getMsMult() < speed.getMsMult()) {
				speed = sv;
			}
		}
		if (allStop || (anyStop && map.anyoneCanPause)) {
			speed = Speed.STOP;
		}
	}
	
	public boolean someoneWon() {
		return !map.victors().isEmpty();
	}
	
	public String winnerName() {
		return someoneWon() ? Empire.nameList(map.victors(), Lang.currentLocale) : "?";
	}
	
	public boolean victory() {
		return player != null && map.victors().contains(player);
	}
	
	public boolean defeat() {
		return player != null && (player.cities.isEmpty() || (someoneWon() && !map.victors().contains(player)));
	}

	public static enum Speed {
		STOP(true, 0, new Img("ui", 96, 512, 16, 16, false)),
		QUARTER(true, 2, new Img("ui", 240, 512, 16, 16, false)),
		HALF(true, 4, new Img("ui", 288, 512, 16, 16, false)),
		NORMAL(true, 8, new Img("ui", 112, 512, 16, 16, false)),
		FAST(true, 16, new Img("ui", 128, 512, 16, 16, false)),
		CHEETAH(true, 48, new Img("ui", 144, 512, 16, 16, false));

		private final int msMult;
		public final Img icon;
		public final boolean forMultiplayer;

		private Speed(boolean forMultiplayer, int msMult, Img icon) {
			this.forMultiplayer = forMultiplayer;
			this.msMult = msMult;
			this.icon = icon;
		}

		public int getMsMult() {
			if (this == CHEETAH && AGame.isRunTests()) {
				return 72;
			}
			return msMult;
		}
	}


	public CampaignWorld(String worldID, long seed, EnumSet<ConquestToggle> toggles, MapSize size, DifficultyLevel difficulty, AirshipGame g, List<StrategicSetupInfo> playerInfos, int playerEmpireIndex, Locale lang, SeaLevelSetting seaLevelSetting, MonsterSetting monsterity, FrequencySetting heroFrequency, FrequencySetting incidentFrequency, TechSpeedSetting techSpeed, int startingTechTier, boolean strategicRapidCommands, boolean allowDirectControl, boolean anyoneCanPause) {
		this(worldID, seed, toggles, size, difficulty, g, playerInfos, playerEmpireIndex, lang, seaLevelSetting, monsterity, heroFrequency, incidentFrequency, techSpeed, startingTechTier, strategicRapidCommands, allowDirectControl, anyoneCanPause, null, null, new HashMap<Integer, StrategicPlayerInfo>());
	}

	public CampaignWorld(String worldID, long seed, EnumSet<ConquestToggle> toggles, MapSize size, DifficultyLevel difficulty, AirshipGame g, List<StrategicSetupInfo> setupInfos, int playerEmpireIndex, Locale lang, SeaLevelSetting seaLevelSetting, MonsterSetting monsterity, FrequencySetting heroFrequency, FrequencySetting incidentFrequency, TechSpeedSetting techSpeed, int startingTechTier, boolean strategicRapidCommands, boolean allowDirectControl, boolean anyoneCanPause, Client client, Server server, HashMap<Integer, StrategicPlayerInfo> channelPlayers) {
		map = new WorldMap(worldID, seed, toggles, size, difficulty, g, setupInfos, lang, seaLevelSetting, monsterity, heroFrequency, incidentFrequency, techSpeed, startingTechTier, strategicRapidCommands, allowDirectControl, anyoneCanPause);
		this.playerEmpireIndex = playerEmpireIndex;
		this.g = g;
		this.mpClient = client;
		this.mpServer = server;
		this.channelPlayers = channelPlayers;
		if (channelPlayers != null && !channelPlayers.isEmpty()) {
			/*for (StrategicPlayerInfo spi : channelPlayers.values()) {
				System.out.println(spi.id + " " + spi.empireName + " " + spi.name);
			}*/ // qqDPS REVISIT THIS!
			speedVoters = new HashMap<Integer, Speed>();
			for (int id : channelPlayers.keySet()) {
				speedVoters.put(id, Speed.STOP);
			}
		}
	}
	
	public void setupPlayer() {
		player = playerEmpireIndex == -1 ? null : map.empires.get(playerEmpireIndex);
	}
	
	public boolean isCoopEmpire(Empire e) {
		//System.out.println(" e " + e.name);
		if (channelPlayers == null) { /*System.out.println("nope");*/return false; }
		int numOwners = 0;
		for (StrategicPlayerInfo spi : channelPlayers.values()) {
			//System.out.println("spi " + spi.name + ": " + (spi.empire == null ? "null" : spi.empire.name));
			if (spi.empire == e) {
				//System.out.println("+");
				numOwners++;
			}
		}
		return numOwners > 1;
	}

	public CampaignWorld(WorldMap map, Empire player, AirshipGame g) {
		this.map = map;
		this.player = player;
		this.g = g;
	}
	
	public static final int SAVE_VERSION = 10620;
	public static final ArrayList<Integer> READABLE_VERSIONS = new ArrayList<Integer>(Arrays.asList(10000, 10620));

	public CampaignWorld(JSONObject o, AirshipGame g, boolean multiplayer, InPipe ip) throws IOException {
		if (!READABLE_VERSIONS.contains(o.optInt("version", 0))) {
			throw new RuntimeException("Incompatible save version: " + o.optInt("version", 0));
		}
		gameName = o.optString("gameName", "Conquest Game");
		map = new WorldMap(o.getJSONObject("map"), g, ip);
		usedCheatCommand = o.optBoolean("usedCheatCommand", false);
		deactivateAICheat= o.optBoolean("deactivateAICheat", false);
		compliantAICheat = o.optBoolean("compliantAICheat", false);
		if (multiplayer) {
			player = o.getInt("player") == -1 ? null : map.empires.get(o.getInt("player"));
		} else {
			player = map.empires.get(0);
			if (!player.playerControlled) {
				for (Empire e : map.empires) {
					if (e.playerControlled) {
						player = e;
					}
				}
			}
			for (Empire e : map.empires) {
				e.playerControlled = e == player;
			}
		}
		speed = Speed.STOP;
		this.g = g;
		/*if (o.has("combatInfo")) {
			combatInfo = new CombatInfo(o.getJSONObject("combatInfo"), this, g);
			combatInfo.finish(o.getJSONObject("combatInfo"), this);
			waitingForCombatSetup = o.getBoolean("combatSetup");
		}*/
		if (o.has("helpCleared")) {
			JSONArray hc = o.getJSONArray("helpCleared");
			for (int i = 0; i < hc.length(); i++) {
				help.cleared.add(StrategicHelp.Type.valueOf(hc.getString(i)));
			}
		} else {
			help.cleared.addAll(EnumSet.allOf(StrategicHelp.Type.class));
		}
		showCompressedFleet = o.optBoolean("showCompressedFleet", false);
		if (o.has("doneConquestHelpItems")) {
			JSONArray a = o.getJSONArray("doneConquestHelpItems");
			for (int i = 0; i < a.length(); i++) {
				doneConquestHelpItems.add(a.getString(i));
			}
		}
		postCombatCommands = o.has("postCombatCommands") ? o.getJSONArray("postCombatCommands") : new JSONArray();
	}

	public JSONObject toJSON(OutPipe op) {
		JSONObject o = new JSONObject()
				.put("gameName", gameName)
				.put("version", SAVE_VERSION)
				.put("map", map.toJSON(op))
				.put("usedCheatCommand", usedCheatCommand)
				.put("deactivateAICheat", deactivateAICheat)
				.put("compliantAICheat", compliantAICheat)
				.put("player", map.empires.indexOf(player))
				.put("speed", speed.name())
				.put("showCompressedFleet", showCompressedFleet)
				.put("combatSetup", waitingForCombatSetup || multiplayerCampaignCombatSetupIntent != null);
		/*if (combatInfo != null) {
			o.put("combatInfo", combatInfo.toJSON(this));
		}*/
		JSONArray helpCleared = new JSONArray();
		o.put("helpCleared", helpCleared);
		for (StrategicHelp.Type t : help.cleared) {
			helpCleared.put(t.name());
		}
		JSONArray mods = new JSONArray();
		for (Mod m : Mod.getEnabledMods()) {
			mods.put(new JSONObject().put("id", m.id));
		}
		o.put("mods", mods);
		o.put("expansions", Expansion.names(Expansion.enableds()));
		op.register(this, "savePreviewFixed", 1);
		op.registerWithoutVersion(this, "savePreviewVariable");
		JSONArray items = new JSONArray();
		for (ConquestHelpItem i : ConquestHelpItem.values()) {
			if (i.done) {
				items.put(i.name());
			}
		}
		o.put("doneConquestHelpItems", items);
		o.put("postCombatCommands", postCombatCommands);
		return o;
	}

	// If this returns true, immediately return from the input tick.
	public boolean tick(int ms, StrategicScreen ss) {
		/*if (player != null && player.rewardGiven == null) {
			ArrayList<Reward> rs = EraModifier.ofName("AGE_OF_EXPLORATION").expeditions.get(BonusSet.empty());
			ArrayList<Reward> r = new ArrayList<Reward>();
			r.add(rs.get((expeditionOutcomeIndex++) % rs.size()));
			Reward.giveRewardTo(r, player, map, player.getFleets().get(0), new Random().nextInt(10000), "");
		}
		*/
		if (player != null) {
			for (int i = 0; i < playerOffers.size(); i++) {
				Offer o = playerOffers.get(i);
				o.clean();
				if (!map.empires.contains(o.rel.other(player)) || o.isEmpty() || !o.rel.isValid(o, null, map, false)) {
					playerOffers.remove(i);
					i--;
				}
			}
			for (int i = 0; i < player.ultimatumNotices.size(); i++) {
				if ((player.ultimatumNotices.get(i).age += ms) >= NOTICE_LIFESPAN) {
					player.ultimatumNotices.remove(i);
					i--;
				}
			}
			for (int i = 0; i < player.diplomacyNotices.size(); i++) {
				if ((player.diplomacyNotices.get(i).age += ms) >= NOTICE_LIFESPAN) {
					player.diplomacyNotices.remove(i);
					i--;
				}
			}
		}
		
		if (isMultiplayer()) {
			if (!g.isConnected()) {
				ss.emergencySave("disconnected");
				int oldID = g.playerID();
				if (g.completelyReconnectClient()) {
					g.s = new ResumeScreen(g, this, ss, g.s, oldID, true, 1, gameName);
				} else {
					g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
					g.showError(_t("Connection_lost"));
				}
				return true;
			}
			
			if (needResync) {
				needResyncAccumulator += ms;
			}
			JSONObject msg;
			while ((msg = g.pollMessage()) != null) {
				if (msg.getString("type").equals("frame")) {
					frameQueue.add(msg);
				}
			}
			int fqsz = frameQueue.size();
			if (fqsz == 0) {
				return false;
			} else if (fqsz < 3) {
				ms /= 2;
			} else if (fqsz == 3) {
				ms = ms * 3 / 4;
			} else if (fqsz > 90) {
				ms = ms * 8;
			} else if (fqsz > 45) {
				ms = ms * 4;
			} else if (fqsz > 15) {
				ms = ms * 2;
			} else if (fqsz > 7) {
				ms = ms * 3 / 2;
			}
			PerfStats.worldFrameQueueSize = fqsz;
			if (fqsz >= 1000 && !badFQSReported) {
				//g.reportDebug(fqsz + " frame queue");
				badFQSReported = true;
			}
			if (fqsz < 10) {
				badFQSReported = false;
			}
			if (fqsz >= badFQSPrintBoundary) {
				System.err.println("Frame queue " + fqsz + " @ " + System.currentTimeMillis() + " " + new DateTime());
				System.err.println("Recent screens: " + g.recentScreens());
				badFQSPrintBoundary *= 2;
			}
			if (badFQSPrintBoundary > 50 && fqsz < 20) {
				System.err.println("Frame queue back to " + fqsz + " @ " + System.currentTimeMillis() + " " + new DateTime());
				System.err.println("Recent screens: " + g.recentScreens());
				badFQSPrintBoundary = 50;
			}
			
			int avgTick = g.client.avgTickInterval();
			if (avgTick >= badTickIntervalPrintBoundary) {
				System.err.println("Avg tick interval " + avgTick + " Input " + g.avgTickCost() + " Render " + g.avgRenderCost() + " @ " + System.currentTimeMillis() + " " + new DateTime());
				System.err.println("Recent screens: " + g.recentScreens());
				badTickIntervalPrintBoundary *= 2;
			}
			if (badTickIntervalPrintBoundary > 50 && avgTick < 20) {
				System.err.println("Avg tick interval back to " + avgTick + " Input " + g.avgTickCost() + " Render " + g.avgRenderCost() + " @ " + System.currentTimeMillis() + " " + new DateTime());
				System.err.println("Recent screens: " + g.recentScreens());
				badTickIntervalPrintBoundary = 50;
			}
		}
		accumulatedMs += ms;
		long loopStartTime = System.currentTimeMillis();
		int loopNum = 0;
		while (accumulatedMs >= Combat.TICK_LENGTH) {
			if (waitingForCombatSetup || (isMultiplayer() && frameQueue.isEmpty())) {
				return false;
			}
			accumulatedMs -= Combat.TICK_LENGTH;
			long start_ru = System.currentTimeMillis();
			doTick();
			long endTime = System.currentTimeMillis();
			PerfStats.worldTickTime += (endTime - start_ru);
			PerfStats.numworldTicks++;
			if (combatInfo != null && combatInfo.combat.isFinished()) {
				return false;
			}
			if (loopNum++ >= 6 && (endTime - loopStartTime) > 200) {
				return false;
			}
		}
		return false;
	}
	
	public void membershipUpdate(JSONObject frame) {
		if (needResync) { return; } // Players will leave the channel if a resync is happening.
		JSONArray members = frame.getJSONArray("members");
		for (int i = 0; i < members.length(); i++) {
			int id = members.getInt(i);
			boolean isInChannel = false;
			for (int inID : channelPlayers.keySet()) {
				if (id == inID) { isInChannel = true; }
			}
			if (!isInChannel) {
				//System.out.println("goAway " + id);
				g.sendMessage(new JSONObject().put("type", "goAway").put("id", id));
			}
		}
		ArrayList<Integer> playerIDs = new ArrayList<Integer>(channelPlayers.keySet());
		Collections.sort(playerIDs);
		for (int playerID : playerIDs) {
			boolean isPresent = false;
			for (int i = 0; i < members.length(); i++) {
				if (members.getInt(i) == playerID) {
					isPresent = true;
				}
			}
			if (!isPresent) {
				System.out.println("lost " + playerID);
				StrategicPlayerInfo disc = channelPlayers.get(playerID);
				if (disc.empire != null && map.empires.contains(disc.empire)) {
					if (disc.claimedEmpireCoopOwner) {
						for (int pi2 : playerIDs) {
							StrategicPlayerInfo spi2 = channelPlayers.get(pi2);
							if (spi2 != null && spi2 != disc && spi2.empire == disc.empire) {
								spi2.claimedEmpireCoopOwner = true;
								break;
							}
						}
					}
					disconnectedPlayers.add(disc);
					disc.empire.playerNames.remove(disc.name);
					disc.empire.playerControlled = !disc.empire.playerNames.isEmpty();
				}
				channelPlayers.remove(playerID);
			} else {
				StrategicPlayerInfo disc = channelPlayers.get(playerID);
				if (disc.empire != null && disc.empire.cities.isEmpty() && disc.empire.playerControlled && !defeatedPlayers.contains(disc) && disc.empire != player) {
					defeatedPlayers.add(disc);
				}
			}
		}
		updateSpeedVote();
	}
	
	/*public boolean isAboutToChecksum() {
		boolean isTickAge = (combatStarted() ? combatInfo.combat.time : map.age) % SYNC_CHECK_EVERY > SYNC_CHECK_EVERY - 200;
		return CHECKSUM && !inCombatSetup() && isTickAge && (!desyncDetected || map.age < desyncDetectedAt + SYNC_CHECK_EVERY * 2 + 1000);
	}*/
	
	public int checksumTimeCost = 0;
	private long prevSeed = -1;
	
	private void doTick() {
		map.r.guard = false;
		doTick_();
		map.r.guard = true;
		/*if (isMultiplayer()) {
			if (prevSeed != -1 && prevSeed != AGame.getSeed(map.r)) {
				System.out.println("seed changed between ticks");
				g.reportDebug("Seed changed between ticks.");
				GuardedRandom newR = new GuardedRandom(prevSeed);
				map.r = newR;
				if (combatInfo != null) {
					combatInfo.combat.r = newR;
				}
			}
			doTick_();
			prevSeed = AGame.getSeed(map.r);
		} else {
			doTick_();
		}*/
	}
	
	private void doTick_() {
		PerfStats.start();
		boolean consumedFrame = false;
		boolean tickedCombat = false;
		if (isMultiplayer() || fakeMultiplayerForTesting) {
			if (multiplayerCampaignCombatIntent != null) {
				if (multiplayerCampaignCombatIntent.runInitProcess()) {
					PerfStats.mark("combatInitProcess");
					return;
				}
			}
		}
		if (player != null && !player.playerControlled) {
			g.reportError("Player is not player controlled", null, null, false, true);
		}
		if (!isMultiplayer()) {
			for (Empire e : map.empires) {
				if (e != player && e.playerControlled) {
					g.reportError("Non-player is player controlled", null, null, false, true);
				}
			}
		}
		if (isMultiplayer()) {
			if (desyncDetected && !needResyncSentOrReceived) {
				ticksSinceDesyncDetected++;
				if (ticksSinceDesyncDetected > 100) {
					g.sendMessage(msg("needResync").put("subtype", "desyncDetected"));
					needResyncSentOrReceived = true;
					needResync = true;
				}
			}
			boolean timeToChecksumCombat = combatInfo != null && (combatInfo.combat.time <= 16 || combatInfo.combat.time >= combatInfo.combat.mostRecentChecksumTime + SYNC_CHECK_EVERY);
			/*if (combatInfo != null && combatInfo.combat.speed != CombatSpeed.NORMAL) {
				timeToChecksumCombat = false; // qqDPS TESTING PURPOSES
			}*/
			if (combatStarted() && CHECKSUM && storedStates.size() <= MAX_SYNC_CHECK_QUEUE && !inCombatSetup() && timeToChecksumCombat && (!desyncDetected || map.age < desyncDetectedAt + SYNC_CHECK_EVERY * 2 + 1000)) {
				long start = System.currentTimeMillis();
				//System.out.println("checksumming at " + combatInfo.combat.time);
				combatInfo.combat.mostRecentChecksumTime = combatInfo.combat.time;
				StoredState ss = new StoredState(this, g.playerID());
				PerfStats.mark("combatHash");
				checksumTimeCost = (int) (System.currentTimeMillis() - start);
				//System.out.println("sync cost " + checksumTimeCost);
				//System.out.println("sync checking at " + map.age + " : " + ss.hash);
				g.sendMessage(msg("strategicChecksum").put("age", ss.age).put("combatTime", ss.combatTime).put("hash", ss.hash).put("playerID", g.playerID()));	
				//System.out.println("send strat cs " + ss.age + " " + ss.combatTime);
				if (!desyncDetected) {
					//System.out.println("storing state");
					storedStates.add(ss);
					Pair<Integer, Integer> k = new Pair<Integer, Integer>(ss.age, ss.combatTime);
					if (futureChecksums.containsKey(k)) {
						for (JSONObject cmd : futureChecksums.get(k)) {
							checkStrategicChecksum(cmd, ss, this);
						}
						futureChecksums.remove(k);
					}
					PerfStats.mark("future checksums");
				}
			}
			frameAccumulatedMs += Combat.TICK_LENGTH;
			if (frameAccumulatedMs >= Server.SERVER_TICK) {
				frameAccumulatedMs -= Server.SERVER_TICK;
				JSONObject frame = frameQueue.pollFirst();
				consumedFrame = combatInfo != null && multiplayerCampaignCombatSetupIntent == null;
				//System.out.println("frame " + frame.getInt("frameNumber") + " at " + map.age + " mod ST " + (map.age % Server.SERVER_TICK));
				long currentFrameNumber = frame.optLong("###", -1);
				if (currentFrameNumber != -1) {
					if (!nonSequentialFramesReported && mostRecentFrameNumber != -1 && (currentFrameNumber <= mostRecentFrameNumber || currentFrameNumber > mostRecentFrameNumber + 3)) {
						g.reportDebug("Very non-sequential frames: " + mostRecentFrameNumber + ", " + currentFrameNumber + ":\n" + frame.toString(4));
						nonSequentialFramesReported = true;
					}
					mostRecentFrameNumber = currentFrameNumber;
				}
				frame.put("mapAge", map.age);
				frame.put("preSpeed", speed.name());
				if (combatInfo == null && postCombatCommands.length() > 0) {
					for (int i = 0; i < postCombatCommands.length(); i++) {
						execCommand(postCombatCommands.getJSONObject(i));
					}
					postCombatCommands = new JSONArray();
				}
				JSONArray messages = frame.getJSONArray("messages");
				for (int i = 0; i < messages.length(); i++) {
					JSONObject msg = messages.getJSONObject(i);
					//System.out.println("acmd " + msg.toString());
					if (combatInfo != null) {
						if (msg.getString("type").equals("combatReady")) {
							System.out.println("combatReady for " + msg.getInt("combatID") + " with combat " + combatInfo.combat.conquestID);
						}
						if (EXECS.containsKey(msg.getString("type"))) {
							execCommand(msg);
						} else if (multiplayerCampaignCombatSetupIntent != null) {
							multiplayerCampaignCombatSetupIntent.execCommand(msg, currentFrameNumber);
						} else {
							//System.out.println("cexec");
							combatInfo.combat.execCommand(msg, currentFrameNumber);
						}
					} else {
						if (msg.getString("type").equals("combatReady")) {
							int combatID = msg.getInt("combatID");
							if (combatID <= map.combatIDCounter) {
								System.out.println("combatReady for old combat");
							} else {
								g.reportError("bad combatReady for " + player + " with combatID " + combatID + " with counter at " + map.combatIDCounter, null, msg.toString(), false, true);
								g.sendMessage(msg("needResync").put("subtype", "combatReady").put("info", "bad combatReady for " + player + " with combatID " + combatID + " with counter at " + map.combatIDCounter));
								needResyncSentOrReceived = true;
								needResync = true;
							}
						} else {
							execCommand(msg);
						}
					}
				}
				membershipUpdate(frame);
				frame.put("postSpeed", speed.name());
				PerfStats.mark("frame exec " + frame.toString());
			}
		}
				
		if (combatInfo != null) {
			if (!waitingForCombatSetup) {
				if (multiplayerCampaignCombatSetupIntent == null) {
					Side viewingSide = null;
					if (combatInfo.attackers(map).contains(player)) {
						viewingSide = combatInfo.combat.sides.get(0);
					} else if (combatInfo.defenders(map).contains(player)) {
						viewingSide = combatInfo.combat.sides.get(1);
					}
					tickedCombat = true;
					if (!combatInfo.combat.speed.isMPCapable() && !nonMPCapableSpeedReported) {
						g.reportDebug("Non MP capable speed: " + combatInfo.combat.speed);
						nonMPCapableSpeedReported = true;
					}
					for (int n = 0; n < combatInfo.combat.speed.getMult(); n++) {
						combatInfo.combat.doGenericTick(Combat.TICK_LENGTH, viewingSide, combatInfo.combat.timeOfDay.effect.lightningChance);
					}
					multiplayerCampaignCombatIntent.update();
					PerfStats.mark("combat tick");
				} else {
					multiplayerCampaignCombatSetupIntent.update(Combat.TICK_LENGTH);
					PerfStats.mark("combat setup tick");
				}
			}
			
			if (isMultiplayer() && consumedFrame && !tickedCombat && !unevenConsumptionReported) {
				g.reportDebug("ConsumedFrame " + consumedFrame + ", TickedCombat " + tickedCombat);
				unevenConsumptionReported = true;
			}
			return;
		}
		
		int iters, quantum;
		/*if (isMultiplayer()) {
			iters = speed.getMsMult() / 8;
			quantum = Combat.TICK_LENGTH / SPEED_DIV * 8;
		} else {*/
			iters = 1;
			quantum = Combat.TICK_LENGTH / SPEED_DIV * speed.getMsMult();
		//}
		for (int i = 0; i < iters; i++) {
			ArrayList<Empire> empires = new ArrayList<Empire>(map.empires);
			if (quantum > 0) {
				map.smartEmpireIndex = (map.smartEmpireIndex + 1) % empires.size();
			}
			for (Empire e : empires) {
				e.cachedCityIncome = -1;
				if (e.cities.isEmpty()) {
					map.removeEmpire(e);
				} else {
					if (!e.playerControlled && map.empires.contains(e) && g.testRunner == null) {
						for (Empire e2 : empires) {
							e2.cachedCityIncome = -1;
						}
						e.diplomacyAI.tick(e, this, map, quantum, quantum > 0 && empires.indexOf(e) == map.smartEmpireIndex);
						if (map.empires.contains(e)) {
							StrategicAI.tick(this, map, e, empires.indexOf(e) == map.smartEmpireIndex ? quantum : 0);
						}
					}
				}
			}
			PerfStats.mark("AI ticks");
			//System.out.println("q " + quantum + " mp " + isMultiplayer() + " age " + map.age);
			map.tick(quantum, isMultiplayer());
			PerfStats.mark("map tick");
			for (Empire e : new ArrayList<Empire>(map.empires)) {
				map.resolveUncontestedSeaIntercepts(e);
			}
			map.cleanupMissingInterceptFleets("post sea intercept");
			PerfStats.mark("resolve combats");
			if (!combatStarted() && isMultiplayer() && CHECKSUM && !inCombatSetup() && map.age >= map.mostRecentSyncCheck + SYNC_CHECK_EVERY && (!desyncDetected || map.age < desyncDetectedAt + SYNC_CHECK_EVERY * 2 + 1000)) {
				map.mostRecentSyncCheck = map.age;
				long start = System.currentTimeMillis();
				StoredState ss = new StoredState(this, g.playerID());
				PerfStats.mark("non-combat hash");
				checksumTimeCost = (int) (System.currentTimeMillis() - start);
				System.out.println("sync checking at " + map.age + " : " + ss.hash);
				g.sendMessage(msg("strategicChecksum").put("age", ss.age).put("combatTime", ss.combatTime).put("hash", ss.hash).put("playerID", g.playerID()));	
				//System.out.println("send strat cs " + ss.age + " " + ss.combatTime);
				if (!desyncDetected) {
					//System.out.println("storing state");
					storedStates.add(ss);
					Pair<Integer, Integer> k = new Pair<Integer, Integer>(ss.age, ss.combatTime);
					if (futureChecksums.containsKey(k)) {
						for (JSONObject cmd : futureChecksums.get(k)) {
							checkStrategicChecksum(cmd, ss, this);
						}
						futureChecksums.remove(k);
					}
					PerfStats.mark("future checksums");
				}
			}
			
			// Achievements
			if (player != null && !usedCheatCommand) {
				if (player.allianceBreakings >= 3) {
					Achievement.achieve(Achievement.CHRONIC_BACKSTABBING);
				}
				if (player.coronationsSabotaged >= 3) {
					Achievement.achieve(Achievement.CORONATIO_INTERRUPTUS);
				}
				if (player.hasBuiltFleshcracker && player.hasBuiltMechSquid) {
					Achievement.achieve(Achievement.THEY_LAUGHED_AT_ME);
				}
				if (player.bioNestsCleared >= 5) {
					Achievement.achieve(Achievement.EXTERMINATOR);
				}
				if (player.deaths >= 1000) {
					Achievement.achieve(Achievement.DULCE_ET_DECORUM_EST);
				}
				for (City c : player.cities) {
					int numUpgrades = 0;
					for (CityUpgradeType cut : c.upgrades) {
						if (cut.canBuild) {
							numUpgrades++;
						}
					}
					if (numUpgrades >= 5) {
						Achievement.achieve(Achievement.OPULENT_AND_IMPERIAL);
					}
				}
			}
			
			combatInfo = map.getNextCombat(g, this);
			PerfStats.mark("next combat check");

			if (combatInfo != null) {
				waitingForCombatSetup = true;
				return;
			}
		}
	}
	
	public void immediatePostCombat() {
		if (combatInfo.immediatePostCombatDone) {
			System.out.println("Double immediatePostCombat invocation");
			AirshipGame.instance.reportError("Double immediatePostCombat invocation", null, null, false, true);
		}
		
		if (combatInfo.defendingLoc != null) {
			combatInfo.defendingLoc.landscapeVersion++;
		}
		
		ArrayList<FleetOwner> attackers = map.owners(combatInfo.attackingFleets);
		for (FleetOwner attacker : attackers) {
			if (attacker instanceof Empire) {
				((Empire) attacker).deaths += combatInfo.combat.sides.get(0).deaths() / attackers.size();
			}
		}
		
		ArrayList<FleetOwner> defenders = map.owners(combatInfo.defendingFleets);
		for (FleetOwner defender : defenders) {
			if (defender instanceof Empire) {
				((Empire) defender).deaths += combatInfo.combat.sides.get(1).deaths() / attackers.size();
			}
		}
		
		for (Side side : combatInfo.combat.sides) {
			side.lostLocked = side.lost();
		}
		
		boolean attackerWon = combatInfo.combat.won(combatInfo.combat.sides.get(0));
		
		// Deal with fast-resolve side switching.
		for (Combat.Side side : combatInfo.combat.sides) {
			if (!side.surrendered) { continue; }
			ArrayList<Airship> ships = new ArrayList<Airship>(side.ships);
			for (Airship ship : ships) {
				switch (ship.fastResolveBoarding()) {
					case 1: // Switch sides
						side.ships.remove(ship);
						combatInfo.combat.otherSide(side).ships.add(ship);
						break;
					case 2: // Destroyed
						side.ships.remove(ship);
						break;
				}
			}
		}
		
		combatInfo.combat.returnTroopsToOwners();
		combatInfo.combat.partiallyRepairAllShips(combatInfo.defendingLoc instanceof City, combatInfo.defendingLoc instanceof MonsterNest, attackerWon && !combatInfo.combat.isRaid);
		combatInfo.combat.clearShipAIs();
		
		for (Side side : combatInfo.combat.sides) {
			if (side == combatInfo.combat.sides.get(1) && combatInfo.defendingLoc != null && (!attackerWon || combatInfo.combat.isRaid)) {
				side.repairStatus = Combat.RepairStatus.FULL_DEFENDERS;
			} else if (side == combatInfo.combat.sides.get(0) && attackerWon) {
				side.repairStatus = combatInfo.defendingLoc != null ? (combatInfo.combat.isRaid ? Combat.RepairStatus.PARTIAL_RAID : combatInfo.defendingLoc instanceof MonsterNest ? Combat.RepairStatus.PARTIAL_NEST : Combat.RepairStatus.FULL_ATTACKERS) : Combat.RepairStatus.PARTIAL_NO_CITY;
			} else {
				side.repairStatus = combatInfo.defendingLoc != null ? Combat.RepairStatus.PARTIAL_RETREAT : Combat.RepairStatus.PARTIAL_NO_CITY;
			}
		}
		
		combatInfo.combat.pruneUselessShips();
		combatInfo.immediatePostCombatDone = true;	
	}
	
	public CombatOutcome postCombat(boolean uncontested) {
		if (!combatInfo.immediatePostCombatDone) {
			System.out.println("No immediatePostCombat invocation");
			AirshipGame.instance.reportError("No immediatePostCombat invocation", null, null, false, true);
			immediatePostCombat();
		}
		
		boolean attackerWon = combatInfo.combat.won(combatInfo.combat.sides.get(0));
		boolean defenderWon = combatInfo.combat.won(combatInfo.combat.sides.get(1));
		boolean isIntercept = combatInfo.attackingFleets.get(0).interceptTarget != null;
		
		ArrayList<FleetOwner> attackers = map.owners(combatInfo.attackingFleets);
		ArrayList<FleetOwner> defenders = map.owners(combatInfo.defendingFleets);
		
		if (EHeroes.it.enabled) {
			if (attackerWon) {
				for (FleetOwner att : combatInfo.attackers(map)) {
					if (att instanceof Empire) {
						for (FleetOwner def : combatInfo.defenders(map)) {
							Hero.processHeroEvent(HeroEvent.combatVictory((Empire) att, def instanceof Empire ? (Empire) def : null), map);
						}
					}
				}
				for (FleetOwner def : combatInfo.defenders(map)) {
					if (def instanceof Empire) {
						for (FleetOwner att : combatInfo.attackers(map)) {
							Hero.processHeroEvent(HeroEvent.combatDefeat((Empire) def, att instanceof Empire ? (Empire) att : null), map);
						}
					}
				}
			}
			if (defenderWon) {
				for (FleetOwner def : combatInfo.defenders(map)) {
					if (def instanceof Empire) {
						for (FleetOwner att : combatInfo.attackers(map)) {
							Hero.processHeroEvent(HeroEvent.combatVictory((Empire) def, att instanceof Empire ? (Empire) att : null), map);
						}
					}
				}
			}
			
			boolean attackerLostShip = false;
			lp: for (Fleet f : combatInfo.attackingFleets) {
				for (Airship s : f.actives) {
					if (!combatInfo.combat.sides.get(0).ships.contains(s) && !combatInfo.combat.sides.get(0).reserve.contains(s)) {
						attackerLostShip = true;
						break lp;
					}
				}
			}
			boolean defenderLostShip = false;
			lp: for (Fleet f : combatInfo.defendingFleets) {
				for (Airship s : f.actives) {
					if (!combatInfo.combat.sides.get(1).ships.contains(s) && !combatInfo.combat.sides.get(1).reserve.contains(s)) {
						defenderLostShip = true;
						break lp;
					}
				}
			}
			if (!defenderLostShip && combatInfo.defendingLoc != null) {
				lp: for (Airship s : combatInfo.defendingLoc.getDefences()) {
					if (!combatInfo.combat.sides.get(1).ships.contains(s) && !combatInfo.combat.sides.get(1).reserve.contains(s)) {
						defenderLostShip = true;
						break lp;
					}
				}
			}
			if (attackerWon || defenderLostShip) {
				for (Airship s : combatInfo.combat.sides.get(0).ships) {
					if (s.isBonusConstruction) { continue; }
					double xpMult = s.getCaptain() != null ? (1 + s.getCaptain().type.experiencePercent / 100.0) : 1;
					if (attackerWon && combatInfo.defendingLoc instanceof MonsterNest) {
						xpMult *= EmpireStat.CREW_XP_FROM_MONSTER_NESTS.get(s.currentBonuses);
					}
					s.crewExperience = StrictMath.min(EmpireStat.CREW_MAX_XP.get(s.currentBonuses), s.crewExperience + EmpireStat.CREW_XP_PER_COMBAT.get(s.currentBonuses) * xpMult);
				}
			}
			if (defenderWon || attackerLostShip) {
				for (Airship s : combatInfo.combat.sides.get(1).ships) {
					if (s.isBonusConstruction) { continue; }
					double xpMult = s.getCaptain() != null ? (1 + s.getCaptain().type.experiencePercent / 100.0) : 1;
					s.crewExperience = StrictMath.min(EmpireStat.CREW_MAX_XP.get(s.currentBonuses), s.crewExperience + EmpireStat.CREW_XP_PER_COMBAT.get(s.currentBonuses) * xpMult);
				}
			}
			if (defenderWon && attackerLostShip) {
				// Only count attack defeats where the attacker actually got hurt, so they can't be gamed for XP.
				for (FleetOwner att : combatInfo.attackers(map)) {
					if (att instanceof Empire) {
						for (FleetOwner def : combatInfo.defenders(map)) {
							Hero.processHeroEvent(HeroEvent.combatDefeat((Empire) att, def instanceof Empire ? (Empire) def : null), map);
						}
					}
				}
			}
			
			// Injury
			for (Fleet af : combatInfo.attackingFleets) {
				FleetOwner a = map.owner(af);
				if (a instanceof Empire) {
					Empire ae = (Empire) a;
					for (Hero h : Hero.getHeroes(HeroType.Role.CAPTAIN, true, ae, map)) {
						Airship ship = h.getInShip();
						if ((af.actives.contains(ship) || af.reserve.contains(ship)) && !(combatInfo.combat.sides.get(0).ships.contains(ship) || combatInfo.combat.sides.get(0).reserve.contains(ship))) {
							int injuryWeeks = map.r.nextInt(EmpireStat.HERO_INJURY_TIME_MAX_WEEKS.get(ae.bonuses) - EmpireStat.HERO_INJURY_TIME_MIN_WEEKS.get(ae.bonuses)) + EmpireStat.HERO_INJURY_TIME_MIN_WEEKS.get(ae.bonuses);
							h.injuredTime = injuryWeeks * 7 * 400;
							h.setInShip(null);
							if (ae.playerControlled) {
								ae.heroInjuryNotices.add(new Pair<Hero, Integer>(h, h.injuredTime));
							}
						}
					}
				}
			}
			for (Fleet df : combatInfo.defendingFleets) {
				FleetOwner d = map.owner(df);
				if (d instanceof Empire) {
					Empire de = (Empire) d;
					for (Hero h : Hero.getHeroes(HeroType.Role.CAPTAIN, true, de, map)) {
						Airship ship = h.getInShip();
						if ((df.actives.contains(ship) || df.reserve.contains(ship)) && !(combatInfo.combat.sides.get(1).ships.contains(ship) || combatInfo.combat.sides.get(1).reserve.contains(ship))) {
							int injuryWeeks = map.r.nextInt(EmpireStat.HERO_INJURY_TIME_MAX_WEEKS.get(de.bonuses) - EmpireStat.HERO_INJURY_TIME_MIN_WEEKS.get(de.bonuses)) + EmpireStat.HERO_INJURY_TIME_MIN_WEEKS.get(de.bonuses);
							h.injuredTime = injuryWeeks * 7 * 400;
							h.setInShip(null);
							if (de.playerControlled) {
								de.heroInjuryNotices.add(new Pair<Hero, Integer>(h, h.injuredTime));
							}
						}
					}
				}
			}
		}
		
		double x = combatInfo.attackingFleets.get(0).realX(map);
		double y = combatInfo.attackingFleets.get(0).realY(map);
		
		if (!attackerWon) {
			for (FleetOwner attacker : attackers) {
				if (attacker instanceof Empire && !((Empire) attacker).playerControlled && combatInfo.defendingLoc != null) {
					StrategicAI.attackFailed((Empire) attacker, combatInfo.defendingLoc, combatInfo.attackerAIValue, combatInfo.defenderAIValue);
				}
			}
		}
		
		// Put surviving ships into the right fleets.
		ArrayList<Airship> lostShips = new ArrayList<Airship>();
		for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
			ArrayList<Fleet> fleets = sideIndex == 0 ? combatInfo.attackingFleets : combatInfo.defendingFleets;
			if (sideIndex == 1 && isIntercept && !attackerWon && map.water[combatInfo.defendingFleets.get(0).intY()][combatInfo.defendingFleets.get(0).intX()]) {
				// Failed on-water intercept. Which means that any landships the defenders had should survive even though they were not added to the combat.
				for (int fi = 0; fi < fleets.size(); fi++) {
					Fleet fl = fleets.get(fi);
					for (int i = 0; i < fl.actives.size(); i++) {
						if (!fl.actives.get(i).type.onGround) {
							lostShips.add(fl.actives.get(i));
							fl.actives.remove(i);
						}
					}
					for (int i = 0; i < fl.reserve.size(); i++) {
						if (!fl.reserve.get(i).type.onGround) {
							lostShips.add(fl.reserve.get(i));
							fl.reserve.remove(i);
						}
					}
				}
			} else {
				// All other cases.
				for (int fi = 0; fi < fleets.size(); fi++) {
					lostShips.addAll(fleets.get(fi).actives);
					lostShips.addAll(fleets.get(fi).reserve);
					fleets.get(fi).actives.clear();
					fleets.get(fi).reserve.clear();
				}
			}
			
			ArrayList<Airship> ships = combatInfo.combat.sides.get(sideIndex).getAllShips();
			lostShips.removeAll(ships);
			for (int si = 0; si < ships.size(); si++) {
				Airship ship = ships.get(si);
				if (!ship.type.mobile) { continue; }
				boolean inReserve = 
							combatInfo.combat.sides.get(0).reserve.contains(ship) ||
							combatInfo.combat.sides.get(1).reserve.contains(ship);
				// Find its owner.
				boolean foundOwner = false;
				for (int fi = 0; fi < fleets.size(); fi++) {
					Fleet f = fleets.get(fi);
					//System.out.println(ship.getName() + " postCombatOwner " + ship.owner);
					if (map.owner(f) == map.owner(ship.owner)) {
						(inReserve ? f.reserve : f.actives).add(ship);
						foundOwner = true;
					}
				}
				if (!foundOwner) {
					if (combatInfo.defendingLoc != null && map.owner(combatInfo.defendingLoc) == map.owner(ship.owner)) {
						Fleet newFleet = new Fleet(combatInfo.defendingLoc, map);
						newFleet.actives.add(ship);
						map.owner(combatInfo.defendingLoc).getFleets().add(newFleet);
						combatInfo.defendingFleets.add(newFleet);
					} else if (!fleets.isEmpty()) {
						fleets.get(0).reserve.add(ship);
						//throw new RuntimeException("Post-combat ship belonging to " + ship.owner + " who was not party to the combat.");
					}
				}
			}
			// Tidy up.
			for (int fi = 0; fi < fleets.size(); fi++) {
				Fleet f = fleets.get(fi);
				if (f.actives.isEmpty() && f.reserve.isEmpty()) {
					map.owner(f).getFleets().remove(f);
					f.broadcastDestroyed(map);
					fleets.remove(fi);
					fi--;
				}
			}
		}
		if (combatInfo.defendingLoc != null) {
			for (int li = 0; li < lostShips.size(); li++) {
				Airship lost = lostShips.get(li);
				if (lost.owner == null) {
					System.out.println("no owner for " + lost.getName());
					continue;
				}
				if (map.owner(lost.owner) == player) {
					combatInfo.defendingLoc.addMessage(player, City.MessageType.LOST_SHIP, _t("Lost_x", lost.getName()));
				}
			}
		}
		
		// Tidy up defenses, if there are any.
		if (combatInfo.defendingLoc != null) {
			ArrayList<Airship> defences = combatInfo.defendingLoc.getDefences();
			for (int si = 0; si < defences.size(); si++) {
				Airship ship = defences.get(si);
				boolean exists =
						combatInfo.combat.sides.get(0).reserve.contains(ship) ||
						combatInfo.combat.sides.get(1).reserve.contains(ship) ||
						combatInfo.combat.sides.get(0).ships.contains(ship) ||
						combatInfo.combat.sides.get(1).ships.contains(ship);
				if (!exists) {
					// It was destroyed.
					if (map.owner(combatInfo.defendingLoc) == player) {
						combatInfo.defendingLoc.addMessage(player, City.MessageType.LOST_SHIP, _t("Lost_x", ship.getName()));
					}
					combatInfo.defendingLoc.removeDefence(ship);
				}
			}
		}
		
		// What happens to attacking fleets?
		for (int fi = 0; fi < combatInfo.attackingFleets.size(); fi++) {
			Fleet f = combatInfo.attackingFleets.get(fi);
			FleetOwner o = map.owner(f);
			f.breakOut = false;
			if (o instanceof MonsterNest) {
				if (!f.travelTo((MonsterNest) o, map, false)) {
					f.removeLandships();
					if (f.actives.isEmpty() && f.reserve.isEmpty()) {
						o.getFleets().remove(f);
						f.broadcastDestroyed(map);
						combatInfo.attackingFleets.remove(fi);
						fi--;
					} else {
						f.travelTo((MonsterNest) o, map, false);
					}
				}
			} else {
				// Attack by an empire.
				if (!attackerWon || isIntercept) {
					f.stopAndAskForHelp(map);
				}
			}
		}
		
		for (int fi = 0; fi < combatInfo.defendingFleets.size(); fi++) {
			Fleet f = combatInfo.defendingFleets.get(fi);
			FleetOwner o = map.owner(f);
			if (attackerWon) {
				if (isIntercept) {
					o.getFleets().remove(f);
					combatInfo.defendingFleets.remove(fi);
					fi--;
				} else {
					if (!f.canTravelAnywhereElse(map)) {
						f.removeLandships();
						if (!f.canTravelAnywhereElse(map)) {
							if (f.actives.isEmpty() && f.reserve.isEmpty()) {
								o.getFleets().remove(f);
								f.broadcastDestroyed(map);
								combatInfo.attackingFleets.remove(fi);
								fi--;
							}
						} else {
							f.stopAndAskForHelp(map);
						}
					} else {
						f.stopAndAskForHelp(map);
					}
				}
			}
		}
		
		if (combatInfo.defendingLoc != null) {
			combatInfo.defendingLoc.clearDefences();
			for (Airship building : combatInfo.combat.sides.get(1).ships) {
				if (!building.type.mobile) {
					combatInfo.defendingLoc.addDefence(building);
				}
			}
			for (Airship building : combatInfo.combat.sides.get(0).ships) { // Captured buildings get repatriated either case.
				if (!building.type.mobile) {
					combatInfo.defendingLoc.addDefence(building);
				}
			}
			if (attackerWon) {
				if (combatInfo.defendingLoc instanceof City) {
					City city = (City) combatInfo.defendingLoc;
					if (combatInfo.combat.isRaid) {
						MonsterNest attackerNest = (MonsterNest) attackers.get(0);
						if (attackerWon) {
							combatInfo.combat.lootAmount = StrictMath.min(3, City.MAX_ECON_DAMAGES - ((City) combatInfo.defendingLoc).getEconomicDamage());
						}
						attackerNest.money += City.LOOT_PER_ECON_DAMAGE * combatInfo.combat.lootAmount;
						if (!attackerNest.upgrading) {
							attackerNest.upgradeAccumulator += combatInfo.combat.lootAmount * attackerNest.type.upgradeMsPerRaidSuccess;
						}
						city.setEconomicDamage(city.getEconomicDamage() + combatInfo.combat.lootAmount);
						if (combatInfo.combat.lootAmount > 0) {
							city.addMessage(null, MapLocation.MessageType.RAID, _t("raiding_looted_" + combatInfo.combat.lootAmount));
						}
					} else {
						Pair<ArrayList<CityClaim>, Empire> conquerorInfo = map.conqueror(city, combatInfo.attackingFleets);
						Empire conqueror = conquerorInfo.b;
						if (conqueror != null) {
							ArrayList<FleetOwner> fos = map.owners(combatInfo.attackingFleets);
							if (fos.size() > 1) {
								for (FleetOwner o : fos) {
									if (o instanceof Empire) {
										if (o == player) {
											((Empire) o).conquerorInfo = conquerorInfo;
										}
										((Empire) o).previouslyAwardedClaim = false;
									}
								}
							}
							conqueror.previouslyAwardedClaim = true;
							Empire loser = map.owner(city);
							loser.cancelFinalRitual(city, /* intentionally */ false, map);
							map.clearHeroFrom(city);
							loser.cities.remove(city);
							loser.hasLostTerritory = true;
							if (loser.cities.isEmpty()) {
								map.removeEmpire(loser);
							}
							conqueror.cities.add(city);
							map.clearPathCaches();
							for (MapLocation.ConstructionEntry c : city.constructing) {
								conqueror.setMoney(conqueror.getMoney() + c.cost / 10);
							}
							Hero.processHeroEvent(HeroEvent.cityGained(conqueror, city), map);
							city.constructing.clear();
							city.takeoverAmount = 0;
							city.takeoverMethod = null;
							city.cancelCoronation((Empire) loser, /* intentionally */ false);
							city.previousOwner = loser;
							if (city.originalEmpire != conqueror) {
								city.takeoverNeeded = true;
							}
						}
					}
				} else {
					ArrayList<Empire> winningEmpires = new ArrayList<Empire>();
					for (FleetOwner fo : map.owners(combatInfo.attackingFleets)) {
						if (fo instanceof Empire) { winningEmpires.add((Empire) fo); }
					}
					if (!winningEmpires.isEmpty()) {
						MonsterNest nest = (MonsterNest) combatInfo.defendingLoc;
						if (nest.type == map.eraModifier.eraStartSpawnNest && map.eraModifier.eraEndsWhenClearingNest) {
							map.eraModifier.endPrematurely(map, winningEmpires);
						} else {
							// MERGEME Split rewards
							Empire firstWinner = null;
							Fleet firstWinnerFleet = null;
							for (Fleet f : combatInfo.attackingFleets) {
								FleetOwner fo = map.owner(f);
								if (fo instanceof Empire) {
									firstWinner = (Empire) fo;
									firstWinnerFleet = f;
								}
							}
							if (nest.type.isBiological) {
								for (Empire e : winningEmpires) {
									e.bioNestsCleared++;
								}
							}
							if (nest.type.name.equals("cultists") && nest.upgrading && winningEmpires.contains(player) && !usedCheatCommand) {
								Achievement.achieve(Achievement.GO_FOR_THE_HIGH_PRIEST);
							}
							Reward.giveRewardTo(nest.type.rewards, firstWinner, map, firstWinnerFleet, firstWinnerFleet.totalCost(), "");
						}
						nest.addMessage(null, MapLocation.MessageType.CLEAR_NEST, _t("x_defeats_" + nest.type.name, Empire.nameList(winningEmpires, Lang.currentLocale)));
						for (Empire e : winningEmpires) {
							Hero.processHeroEvent(HeroEvent.nestDestroyed(e, nest.type), map);
						}
						nest.clear(map);
					}
				}
			}

			// Tidy up.
			combatInfo.defendingLoc.floaters.clear();
			// LF #0 is the ground.
			if (combatInfo.combat.landFormations.size() > 1) {
				combatInfo.defendingLoc.floaters.addAll(combatInfo.combat.landFormations.subList(1, combatInfo.combat.landFormations.size()));
			}
			
			Fleet garrison = map.getGarrison(combatInfo.defendingLoc);
			if (garrison != null) {
				combatInfo.defendingLoc.layoutGarrison(garrison);
			}
		}
		
		map.cleanupMissingInterceptFleets("post combat");
		
		for (FleetOwner attacker : attackers) {
			if (attacker instanceof MonsterNest) {
				((MonsterNest) attacker).repairAll(map);
			}
		}
				
		for (FleetOwner defender : defenders) {
			if (defender instanceof MonsterNest) {
				((MonsterNest) defender).repairAll(map);
			}
		}
		
		CombatOutcomeType type = (attackers.contains(player) || defenders.contains(player)) ? CombatOutcomeType.DRAW : null;
		
		if (uncontested) {
			if (attackerWon) {
				type = attackers.contains(player) ? CombatOutcomeType.UNCONTESTED_VICTORY : defenders.contains(player) ? CombatOutcomeType.UNCONTESTED_DEFEAT : null;
			} else if (defenderWon) {
				type = attackers.contains(player) ? CombatOutcomeType.UNCONTESTED_DEFEAT : defenders.contains(player) ? CombatOutcomeType.UNCONTESTED_VICTORY : null;
			}
		} else {
			if (attackerWon) {
				type = attackers.contains(player) ? CombatOutcomeType.VICTORY : defenders.contains(player) ? CombatOutcomeType.DEFEAT : null;
			} else if (defenderWon) {
				type = attackers.contains(player) ? CombatOutcomeType.DEFEAT : defenders.contains(player) ? CombatOutcomeType.VICTORY : null;
			}
		}
		
		CombatOutcome co = type == null ? null : new CombatOutcome(
				type,
				(int) x,
				(int) y,
				combatInfo.defendingLoc,
				attackers.contains(player) ? Empire.nameList(defenders, map.lang) : Empire.nameList(attackers, map.lang)
		);
		
		if (co == null && combatInfo.defendingLoc != null) {
			if (attackerWon && !combatInfo.combat.isRaid) {
				combatInfo.defendingLoc.addMessage(null, MapLocation.MessageType.CONQUEST, _t("x_conquers_y", map.owner(combatInfo.defendingLoc).getName(), combatInfo.defendingLoc.getDisplayName()));
			} else if (defenderWon) {
				combatInfo.defendingLoc.addMessage(null, MapLocation.MessageType.REPEL, _t("x_repels_y", combatInfo.defendingLoc.getDisplayName(), Empire.nameList(attackers, map.lang)));
			}
		}
		
		// Looting and scavenging.
		if (!uncontested) {
			if (attackerWon) {
				int defenderRemainingCost = 100;
				for (Fleet f : combatInfo.defendingFleets) {
					if (map.owner(f) == null) { continue; }
					defenderRemainingCost += f.totalCost();
				}
				if (combatInfo.defendingLoc != null) {
					for (Airship s : combatInfo.defendingLoc.getDefences()) {
						defenderRemainingCost += s.getCost();
					}
				}
				int destroyedValue = combatInfo.defenderAIValue - defenderRemainingCost;
				if (destroyedValue > 0) {
					for (Fleet f : combatInfo.attackingFleets) {
						FleetOwner fo = map.owner(f);
						if (fo instanceof Empire) {
							Empire e = (Empire) fo;
							for (Airship s : f.getAllShips()) {
								if (s.getCaptain() != null) {
									e.setMoney(e.getMoney() + destroyedValue * s.getCaptain().type.lootMoneyPercentage / 100);
									f.changeSupply((int) (destroyedValue * s.getCaptain().type.scavengeMoneyToSupply));
								}
							}
						}
					}
				}
			} else if (defenderWon) {
				int attackerRemainingCost = 100;
				for (Fleet f : combatInfo.attackingFleets) {
					if (map.owner(f) == null) { continue; }
					attackerRemainingCost += f.totalCost();
				}
				int destroyedValue = combatInfo.attackerAIValue - attackerRemainingCost;
				if (destroyedValue > 0) {
					for (Fleet f : combatInfo.defendingFleets) {
						FleetOwner fo = map.owner(f);
						if (fo instanceof Empire) {
							Empire e = (Empire) fo;
							for (Airship s : f.getAllShips()) {
								if (s.getCaptain() != null) {
									e.setMoney(e.getMoney() + destroyedValue * s.getCaptain().type.lootMoneyPercentage / 100);
									f.changeSupply((int) (destroyedValue * s.getCaptain().type.scavengeMoneyToSupply));
								}
							}
						}
					}
				}
			}
		}
		
		combatInfo = null;
		map.cleanCaptains();
		
		return co;
	}
}
