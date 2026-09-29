package com.zarkonnen.airships;

import static com.zarkonnen.airships.CampaignWorld.EXECS;
import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.HeroType.CombatAbility;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.MultiplayerSetupIntent.SINGLE_MP_AI_ID;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class  Combat implements JSONAble, ShipList {
	public static final int OUTER_ZONE_W = 400;
	public static final int COMBAT_AREA_W_OLD = 6400;
	public static final int COMBAT_AREA_W_NEW = 9600;
	public static final int COMBAT_AREA_H = 1600;
	public static final int EXCLUSION_ZONE_W = 700;
	public static final int RESERVE_ZONE_W = 100;
	public static final int CRUSH_DEPTH = AGame.GROUND_LEVEL + 500;
		
	public static final int MS_PER_RAID_SUCCESS = 50000;
	
	public static final int NO_INTERESTING_COMBAT_EVENT_FOR_DRAW_MS = 60000;
	public static final int FINISHED_COUNTDOWN_MS = 5000;
	
	public static final int MAX_FRAGMENTS = 160;
	public static final int INITIAL_FRAG_LIFE_CUTOFF = 200000;
	public static final int HASH_EVERY = 1024;
	
	public static final int MIN_TIME_UNTIL_TOD_CHANGE = 90000;
	public static final double TOD_CHANGE_CHANCE_PER_MS = 1.0 / 1000 / 60 / 3;
	
	public static final int TICK_LENGTH = 16;
	public static final boolean DEBUG_SYNC = true;
	public static final int HISTORY_KEEP = 8000;
	public static final int INITIAL_QUEUE_SIZE = 5;
	public CombatBackgroundFlavor backgroundFlavor = CombatBackgroundFlavor.ofName("sea");
	public ArrayList<Side> sides = new ArrayList<Side>();
	public LinkedList<Shot> shots = new LinkedList<Shot>();
	public LinkedList<Particle> particles = new LinkedList<Particle>();
	public LinkedList<Fragment> fragments = new LinkedList<Fragment>();
	public ArrayList<LandFormation> landFormations = new ArrayList<LandFormation>();
	public GuardedRandom r = new GuardedRandom(); // This needs to be in sync.
	public boolean desyncConfirmed = false;
	public Client mpClient;
	public Server mpServer;
	public CampaignWorld campaignWorld;
	public long randomSeed;
	public HashMap<PlayerInfo, Side> playerToSide = new HashMap<PlayerInfo, Side>();
	public HashMap<Integer, PlayerInfo> idToPlayer = new HashMap<Integer, PlayerInfo>();
	private LinkedList<JSONObject> frameQueue = new LinkedList<JSONObject>();
	private JSONObject baseState;
	private ArrayList<JSONObject> networkFrameHistory = new ArrayList<JSONObject>();
	private HashMap<Integer, Integer> hashHistory = new HashMap<Integer, Integer>(1000);
	private int ticksSinceLastFrame = 0;
	private boolean runSimAnyway = false;
	private int netTick = 0;
	public int simTick = 0;
	private int damageStatsAge = 0;
	public transient ArrayList<GenericChatMsg> chatMessages = new ArrayList<GenericChatMsg>();
	private boolean initialQueueFilled = false;
	private CombatOutcome currentCombatOutcome = null;
	private int finishedCountdown = 8000;
	public boolean combatFinished = false;
	private final AirshipGame g;
	public transient int time;
	public transient int mostRecentChecksumTime;
	public transient int pointsLimit;
	public transient Pt lightningPt;
	public transient int lightningAmt;
	public static final int LIGHTNING_P = 10000;
	public static final int LIGHTING_AMT = 90;
	public int msSinceInterestingCombatEvent = -10000;
	public transient boolean resetMoveTos = true;
	public transient ArrayList<String> musicChoice = null;
	public boolean hasInaccuracyMult = true;
	public boolean instantCommandRegeneration = false;
	public boolean allowDirectControl = true;
	public int conquestID;
	public String combatID = "combat";
	public String combatName = "Combat";
	public boolean usedCheatCommand = false;
	public LandscapeType setupLandscapeType; // Used only in combat setup, do not use during combat.
	public ArrayList<CrashZone> crashZones = new ArrayList<CrashZone>();
	public static final int HIGH_STORM_TIMEOUT = 120000;
	public TimeOfDay changeToTOD;
	public int changeToTODTimeout;
	public int timeOfDayAge;
	public boolean todHasChanged;
	public boolean canChangeTimeOfDay;
	
	public transient HashSet<FleetOwnerRef> surrenderedOwners = new HashSet<FleetOwnerRef>();
	
	public boolean isRaid = false;
	public int lootAmount = 0;
	public int maxLootAmount = 0;
	
	// Used for debug reporting.
	public transient JSONArray executedCommands = new JSONArray();
	public transient long startFrameNumber = -1;
	
	public int startCountdown = 0;
	
	public transient ArrayList<Blast> blasts = new ArrayList<Blast>();
	
	public TimeOfDay timeOfDay;
	
	public transient boolean slowMotion = false;
	
	public transient CombatSpeed speed = CombatSpeed.NORMAL;
	public transient HashMap<Integer, CombatSpeed> speedVoters;
	public transient boolean hasHadFractionalSpeed = false;

	public Physics physics;
	public transient BodyPathing bodyPathing = new BodyPathing();
	public transient ArrayList<ExceptionalCombatEvent> exceptionalCombatEvents = new ArrayList<ExceptionalCombatEvent>();

	public ArrayList<CombatSound> sounds = new ArrayList<CombatSound>();
	public ArrayList<Trail> trails = new ArrayList<Trail>();
	
	public Recording recording = new Recording(); // Record all fights by default.
	public long frameMID;
	public JSONArray commandsExecutedInThisFrame = new JSONArray();
	public Recording playback = null;
	public int playbackIndex = 0;
	
	public transient HashSet plinkedArmoursAndModules = new HashSet();
	
	public int combatAreaW() {
		if (!landFormations.isEmpty() && landFormations.get(0).getX() > -COMBAT_AREA_W_NEW / 2 + 100) {
			return COMBAT_AREA_W_OLD;
		} else {
			return COMBAT_AREA_W_NEW;
		}
	}
		
	public void play(String soundName, double x, double y, double volume) {
		sounds.add(new CombatSound(new SoundEffect(soundName, volume), x, y, 0, 0, 1, false, null));
	}
	
	public void play(SoundEffect effect, double x, double y, double xSpeed, double ySpeed, boolean onViewingSide) {
		if (effect == null) { return; }
		sounds.add(new CombatSound(effect, x, y, xSpeed, ySpeed, 1.0, onViewingSide, null));
	}
	
	public void play(SoundEffect effect, double x, double y, double xSpeed, double ySpeed, double volume, boolean onViewingSide) {
		if (effect == null) { return; }
		sounds.add(new CombatSound(effect, x, y, xSpeed, ySpeed, volume, onViewingSide, null));
	}
	
	public void loop(String key, SoundEffect effect, double x, double y, double xSpeed, double ySpeed, double volume, boolean onViewingSide) {
		if (effect == null) { return; }
		sounds.add(new CombatSound(effect, x, y, xSpeed, ySpeed, volume, onViewingSide, key));
	}
	
	public void saveRecording() throws IOException {
		if (recording != null && recording.initialCombat != null) {
			recording.header.numTicks = recording.tickCommands.size();
			File dir = new File(AGame.getGameDirectory(), "recordings");
			dir.mkdirs();
			File f = new File(dir, System.currentTimeMillis() + ".json");
			FileUtils.write(f, recording.header.toJSON().toString() + "\n" + Compression.compressToString(recording.toJSON().toString()));
			MainMenu.newRecordings++;
			recording = null;
		}
	}
	
	private static final MessageDigest MD5;
	static {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		MD5 = md;
	}
	
	public boolean waitingForNetworkMessages() {
		return isSingleMultiplayer() && initialQueueFilled && frameQueue.isEmpty();
	}
	
	public boolean connectionLost() {
		return mpClient != null && mpClient.isDisconnected();
	}

	private Pair<Long, JSONArray> getTickCommands(int serverTickMult) {
		if (mpClient != null) {
			JSONObject msg = g.pollMessage();
			if (msg != null) {
				if (msg.getString("type").equals("frame")) {
					frameQueue.add(msg);
					networkFrameHistory.add(msg);
				}
			}
			if (frameQueue.size() >= INITIAL_QUEUE_SIZE) {
				initialQueueFilled = true;
			}
			if (initialQueueFilled && isTimeMoving()) {
				if (netTick == 0) {
					baseState = toJSON();
				}
				if (DEBUG_SYNC && time != 0 && time % HASH_EVERY == 0 && !desyncConfirmed) {
					int hash = cheapHash();
					hashHistory.remove(time - HISTORY_KEEP);
					hashHistory.put(time, hash);
					g.sendMessage(msg("checksum").put("time", time).put("hash", hash));
				}
				ticksSinceLastFrame++;
				netTick++;
				if (ticksSinceLastFrame == AGame.SERVER_TICK * serverTickMult / TICK_LENGTH) {
					startCountdown = StrictMath.max(0, startCountdown - AGame.SERVER_TICK);
					ticksSinceLastFrame = 0;
					JSONObject fr = frameQueue.pollFirst();
					//System.out.println("F " + fr.getInt("frameNumber") + " l " + fr.getJSONArray("messages").length() + " @ " + netTick + " " + toJSON().toString().hashCode());// + " " + (tick % 100 == 0 ? toJSON().toString() : ""));
					runSimAnyway = true;
					if (isSingleMultiplayer()) {
						if (fr.has("members")) {
							processSingleMultiplayerMembership(fr.getJSONArray("members"));
						}
					}
					return new Pair<Long, JSONArray>(fr.optLong("###", -1), fr.getJSONArray("messages"));
				}
			}
		} else if (playback != null) {
			if (playbackIndex < playback.tickCommands.size()) {
				//System.out.println(playback.tickCommands.get(playbackIndex).toString());
				return playback.tickCommands.get(playbackIndex++);
			}
		}
		return new Pair<Long, JSONArray>(-1l, new JSONArray());
	}
	
	public boolean isTimeMoving() {
		return mpClient == null || (initialQueueFilled && !frameQueue.isEmpty());
	}

	private long runTickCommands(int serverTickMult) {
		Pair<Long, JSONArray> e = getTickCommands(serverTickMult);
		JSONArray cs = e.b;
		for (int i = 0; i < cs.length(); i++) {
			execCommand(cs.getJSONObject(i), e.a);
		}
		return e.a;
	}

	public void giveCommand(JSONObject cmd) {
		if (campaignWorld != null && campaignWorld.fakeMultiplayerForTesting) {
			if (CampaignWorld.EXECS.containsKey(cmd.getString("type"))) {
				campaignWorld.giveCommand(cmd);
			} else if (campaignWorld.multiplayerCampaignCombatSetupIntent != null) {
				campaignWorld.multiplayerCampaignCombatSetupIntent.execCommand(cmd, -1);
			} else {
				execCommand(cmd, -1);
			}
		} else if (mpClient != null || (campaignWorld != null && campaignWorld.isMultiplayer())) {
			g.sendMessage(cmd);
		} else {
			execCommand(cmd, -1);
		}
	}
	
	public transient ArrayList<Notice> notices = new ArrayList<Notice>();

	public void remapMultiplayerControllerIDs(TreeMap<Integer, Integer> oldToNewID) {
		for (Side side : sides) {
			for (Airship ship : side.getAllShips()) {
				ship.multiplayerControllerID_tmp = ship.multiplayerControllerID;
				for (Crewman cm : ship.crew) {
					cm.multiplayerControllerID_tmp = cm.multiplayerControllerID;
				}
				for (Crewman b : ship.boarders) {
					b.multiplayerControllerID_tmp = b.multiplayerControllerID;
				}
			}
			for (Crewman t : side.troops) {
				t.multiplayerControllerID_tmp = t.multiplayerControllerID;
			}
		}
		
		for (Side side : sides) {
			for (Airship ship : side.getAllShips()) {
				ship.multiplayerControllerID = oldToNewID.containsKey(ship.multiplayerControllerID_tmp) ? oldToNewID.get(ship.multiplayerControllerID_tmp) : 0;
				for (Crewman cm : ship.crew) {
					cm.multiplayerControllerID = oldToNewID.containsKey(cm.multiplayerControllerID_tmp) ? oldToNewID.get(cm.multiplayerControllerID_tmp) : 0;
				}
				for (Crewman b : ship.boarders) {
					b.multiplayerControllerID = oldToNewID.containsKey(b.multiplayerControllerID_tmp) ? oldToNewID.get(b.multiplayerControllerID_tmp) : 0;
				}
			}
			for (Crewman t : side.troops) {
				t.multiplayerControllerID = oldToNewID.containsKey(t.multiplayerControllerID_tmp) ? oldToNewID.get(t.multiplayerControllerID_tmp) : 0;
			}
		}
	}

	public void applyAutoResolution(CombatInfo ci) {
		combatFinished = true;
		sides.get(ci.autoAttackerWon ? 1 : 0).lostLocked = true;
		sides.get(0).ships.retainAll(ci.autoAttackerRemainingShips);
		sides.get(0).reserve.retainAll(ci.autoAttackerRemainingShips);
		sides.get(1).ships.retainAll(ci.autoDefenderRemainingShips);
		sides.get(1).reserve.retainAll(ci.autoDefenderRemainingShips);
		for (Empire e : ci.autoDeathsByEmpire.keySet()) {
			e.deaths += ci.autoDeathsByEmpire.get(e);
		}
		for (Side side : sides) {
			for (Airship s : side.ships) {
				s.halveResources();
			}
			for (Airship s : side.reserve) {
				s.halveResources();
			}
		}
		if (isRaid) {
			lootAmount = ci.autoLoot;
		}
	}
	
	/*public void revertMultiplayerControllerIDs() {
		System.out.println("revert");
		for (Side side : sides) {
			for (Airship ship : side.getAllShips()) {
				ship.multiplayerControllerID = ship.multiplayerControllerID_tmp;
				for (Crewman cm : ship.crew) {
					cm.multiplayerControllerID = cm.multiplayerControllerID_tmp;
				}
				for (Crewman b : ship.boarders) {
					b.multiplayerControllerID = b.multiplayerControllerID_tmp;
				}
			}
			for (Crewman t : side.troops) {
				t.multiplayerControllerID = t.multiplayerControllerID_tmp;
			}
		}
	}*/
	
	public static class Notice {
		public final String text;
		public int age = 0;

		public Notice(String text) {
			this.text = text;
		}
	}
	
	private void processSingleMultiplayerMembership(JSONArray frameMembers) {
		HashSet<Integer> channelMemberIDs = new HashSet<Integer>();
		for (int i = 0; i < frameMembers.length(); i++) {
			channelMemberIDs.add(frameMembers.getInt(i));
		}
		// The AI can remain even with no players attached.
		if (idToPlayer.containsKey(SINGLE_MP_AI_ID)) {
			channelMemberIDs.add(SINGLE_MP_AI_ID);
		}
		ArrayList<Integer> remainingPlayerIDs = new ArrayList<Integer>(idToPlayer.keySet());
		Collections.sort(remainingPlayerIDs); // Nevah evah loop through a hashset's keys in MP code!
		ArrayList<Integer> departedPlayerIDs = new ArrayList<Integer>(remainingPlayerIDs);
		departedPlayerIDs.removeAll(channelMemberIDs);
		remainingPlayerIDs.removeAll(departedPlayerIDs);
		for (int si = 0; si < 2; si++) {
			Side side = sides.get(si);
			int firstRemainingPlayerID = 0;
			boolean found = false;
			for (int ii = 0; ii < remainingPlayerIDs.size(); ii++) {
				PlayerInfo pi = idToPlayer.get(remainingPlayerIDs.get(ii));
				Side pSide = playerToSide.get(pi);
				if (side == pSide) {
					firstRemainingPlayerID = pi.id;
					found = true;
				}
			}
			if (!found && !side.surrendered) {
				notices.add(new Notice(_t("side_combat_empty_" + si)));
				side.surrendered = true;
			}
			if (found) {
				for (int dpii = 0; dpii < departedPlayerIDs.size(); dpii++) {
					int departedPlayerID = departedPlayerIDs.get(dpii);
					PlayerInfo departedPlayer = idToPlayer.get(departedPlayerID);
					if (side != playerToSide.get(departedPlayer)) { continue; }
					for (int shi = 0; shi < side.ships.size(); shi++) {
						Airship ship = side.ships.get(shi);
						ship.replaceSingleMultiplayerControllerID(departedPlayer.id, firstRemainingPlayerID);
					}
					for (int shi = 0; shi < side.reserve.size(); shi++) {
						Airship ship = side.reserve.get(shi);
						ship.replaceSingleMultiplayerControllerID(departedPlayer.id, firstRemainingPlayerID);
					}
					for (int ti = 0; ti < side.troops.size(); ti++) {
						Crewman t = side.troops.get(ti);
						if (t.multiplayerControllerID == departedPlayer.id) {
							t.multiplayerControllerID = firstRemainingPlayerID;
						}
					}
					Side otherSide = otherSide(side);
					for (int shi = 0; shi < otherSide.ships.size(); shi++) {
						Airship ship = otherSide.ships.get(shi);
						ship.replaceSingleMultiplayerControllerID(departedPlayer.id, firstRemainingPlayerID);
					}
					for (int shi = 0; shi < otherSide.reserve.size(); shi++) {
						Airship ship = otherSide.reserve.get(shi);
						ship.replaceSingleMultiplayerControllerID(departedPlayer.id, firstRemainingPlayerID);
					}
				}
			}
		}
		for (int dpii = 0; dpii < departedPlayerIDs.size(); dpii++) {
			int departedPlayerID = departedPlayerIDs.get(dpii);
			PlayerInfo departedPlayer = idToPlayer.get(departedPlayerID);
			notices.add(new Notice(_t("x_mp_combat_disconnect", departedPlayer.name())));
			idToPlayer.remove(departedPlayerID);
			playerToSide.remove(departedPlayer);
			speedVoters.remove(departedPlayerID);
		}
	}

	public void execCommand(JSONObject cmd, long frameNumber) {
		if (frameNumber < 0) {
			cmd.remove("t"); // Local command not guaranteed to have same time.
		}
		if (!EXECS.containsKey(cmd.getString("type"))) {
			g.reportError("Unknown command: " + cmd.getString("type"), null, cmd.toString(), false, true);
			return;
			//throw new RuntimeException("Unknown command: " + cmd.getString("type"));
		}
		if (recording != null) {
			commandsExecutedInThisFrame.put(cmd);
		}
		EXECS.get(cmd.getString("type")).run(cmd, this);
		executedCommands.put(new JSONObject().put("command", cmd).put("frameNumber", frameNumber).put("time", time).put("simTick", simTick));
		// Check if all sides in MP have surrendered.
		if (!idToPlayer.isEmpty()) {
			sides.get(0).surrendered = true;
			sides.get(1).surrendered = true;
			for (Map.Entry<PlayerInfo, Side> e : playerToSide.entrySet()) {
				if (e.getValue() != null) {
					e.getValue().surrendered &= e.getKey().surrender;
				}
			}
		}
		checkSurrenderedOwners();
	}
	
	public void checkSurrenderedOwners() {
		if (!surrenderedOwners.isEmpty()) {
			for (Side side : sides) {
				side.surrendered = true;
				for (int i = 0; i < side.ships.size(); i++) {
					if (!surrenderedOwners.contains(side.ships.get(i).owner)) {
						side.surrendered = false;
					}
				}
			}
		}
	}
	
	public GridBody getGridBody(JSONObject msg, String prefix) {
		if (msg.has(prefix + "Ship")) {
			return getShip(msg.getJSONObject(prefix + "Ship"));
		}
		if (msg.has(prefix + "LF")) {
			int index = msg.getInt(prefix + "LF");
			if (index >= 0 && index < landFormations.size()) {
				return landFormations.get(index);
			}
		}
		return null;
	}
	
	public Airship getShip(String netID) {
		for (Side s : sides) {
			for (Airship ship : s.ships) {
				if (ship.networkID.equals(netID)) {
					return ship;
				}
			}
		}
		return null;
	}

	public Airship getShip(JSONObject id) {
		if (id.has("netID")) {
			for (Side s : sides) {
				for (Airship ship : s.ships) {
					if (ship.networkID.equals(id.getString("netID"))) {
						return ship;
					}
				}
			}
		}
		if (id.has("side")) {
			System.err.println("old-style ship ID used!");
			int sideIndex = id.getInt("side");
			int shipIndex = id.getInt("ship");
			if (sideIndex < sides.size() && shipIndex < sides.get(sideIndex).ships.size()) {
				return sides.get(sideIndex).ships.get(shipIndex);
			}
		}
		return null;
	}

	public JSONObject getShipID(Airship ship) {
		/*for (int sideID = 0; sideID < sides.size(); sideID++) {
			if (sides.get(sideID).ships.contains(ship)) {
				return new JSONObject().put("side", sideID).put("ship", sides.get(sideID).ships.indexOf(ship));
			}
		}
		throw new RuntimeException("Unknown ship: " + ship.name);*/
		/*if (ship.networkID.equals("[no network ID]")) {
			g.reportError("Ship has no network ID!", null, this.toString(), false, true);
		}*/
		return new JSONObject().put("netID", ship.networkID);
	}

	public boolean isFinished() {
		return combatFinished;
	}
	
	public boolean isBoringForAttractMode() {
		return msSinceInterestingCombatEvent > 16000;
	}

	public void pruneUselessShips() {
		for (Side s : sides) {
			for (Iterator<Airship> it = s.ships.iterator(); it.hasNext();) {
				if (it.next().uselessPostRepair()) { it.remove(); }
			}
			for (Iterator<Airship> it = s.reserve.iterator(); it.hasNext();) {
				if (it.next().uselessPostRepair()) { it.remove(); }
			}
		}
	}

	public void returnTroopsToOwners() {
		for (Side side : sides) {
			boolean won = won(side);
			for (int i = 0; i < side.troops.size(); i++) {
				side.troops.get(i).instantlyReturnHome(this, side, won);
			}
		}
	}
	
	public void partiallyRepairAllShips(boolean defendersAreInCity, boolean defendersAreInNest, boolean attackerWon) {
		for (Side side : sides) {
			if (
				(defendersAreInCity && ((side == sides.get(1) && !attackerWon) || (side == sides.get(0) && attackerWon))) ||
				(defendersAreInNest && side == sides.get(1) && !attackerWon)
			){
				for (Airship s : side.ships) {
					s.storeStats();
					s.repair(/* resetXP */ false);
				}
				for (Airship s : side.reserve) {
					s.storeStats();
					s.repair(/* resetXP */ false);
				}
			} else {
				for (Airship s : side.ships) {
					s.storeStats();
					s.repairPartially();
				}
				for (Airship s : side.reserve) {
					s.storeStats();
					s.repairPartially();
				}
			}
		}
	}
	
	public void clearShipAIs() {
		for (Side s : sides) {
			for (Airship ship : s.ships) {
				ship.setAI(null);
			}
			for (Airship ship : s.reserve) {
				ship.setAI(null);
			}
		}
	}

	public Side otherSide(Side side) {
		for (Side s : sides) {
			if (s != side) { return s; }
		}
		return null;
	}

	@Override
	public int totalSize() {
		int sz = 0;
		int ssz = sides.size();
		for (int si = 0; si < ssz; si++) {
			sz += sides.get(si).ships.size();
		}
		return sz;
	}
	
	@Override
	public int activesSize() {
		return totalSize();
	}

	@Override
	public Airship get(int index) {
		int sideI = 0;
		while (index >= sides.get(sideI).ships.size()) {
			index -= sides.get(sideI).ships.size();
			sideI++;
		}
		return sides.get(sideI).ships.get(index);
	}

	// type: 0 pen 1 blast 2 direct
	public void doSplashDmg(int type, double x, double y, int blastDmg, int blastSplashRadius, Shot optionalSourceShot, Airship inside, Side immuneCrewSide, Side immuneShipSide) {
		int sisz = sides.size();
		for (int sii = 0; sii < sisz; sii++) {
			Side side = sides.get(sii);
			if (side != immuneShipSide) {
				int ssz = side.ships.size();
				for (int si = 0; si < ssz; si++) {
					Airship ship = side.ships.get(si);
					ship.splashHit(type, this, x, y, blastDmg, blastSplashRadius, side == lastViewingSide, optionalSourceShot, ship == inside);
				}
			}
			if (side != immuneCrewSide) {
				int tsz = side.troops.size();
				for (int ti = 0; ti < tsz; ti++) {
					Crewman t = side.troops.get(ti);
					double tx = t.getX() + t.getBBWidth() / 2;
					double ty = t.getY() + t.getBBHeight() / 2;
					double dSq = (x - tx) * (x - tx) + (y - ty) * (y - ty);
					if (dSq < blastSplashRadius * blastSplashRadius) {
						double d = StrictMath.sqrt(dSq);
						int actualDamage = (int) (blastDmg * (blastSplashRadius - d) / blastSplashRadius);
						if (actualDamage == 0) { continue; }
						t.hurt(optionalSourceShot, actualDamage, this, side == lastViewingSide);
					}
				}
			}
		}
	}

	public double getInaccuracyMultiplier() {
		if (!hasInaccuracyMult) { return 1; }
		// The idea here is that ships get more accurate over time to make combats conclude.
		if (time < 3 * 1000) {
			return 2; // Make the first few shots inaccurate to prevent boring insta-wins.
		}
		if (time < 6 * 1000) {
			return 1.5;
		}
		if (time < 9 * 1000) {
			return 1.2;
		}
		if (time > 4 * 60 * 1000) {
			return 0.3; // After four minutes of fighting, we want to bring this to an end.
		}
		if (time > 2 * 60 * 1000) {
			return 0.7; // We want to start seeing some results.
		}
		return 1;
	}
	
	private static interface CommandExecutor {
		public void run(JSONObject cmd, Combat c);
	}

	public static final HashMap<String, CommandExecutor> EXECS = new HashMap<String, CommandExecutor>();

	static {
		EXECS.put("ping", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				// Ignore
			}
		});
		EXECS.put("globalChat", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				// Ignore
			}
		});
		EXECS.put("createGame", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				// Ignore
			}
		});
		EXECS.put("surrender", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				//System.out.println("got surrender");
				if (!c.idToPlayer.isEmpty() && cmd.has("playerID")) {
					PlayerInfo pi = c.idToPlayer.get(cmd.getInt("playerID"));
					if (pi != null) {
						pi.surrender = true;
						PlayerInfo myPI = c.idToPlayer.get(c.g.playerID());
						if (myPI != null && myPI.side == pi.side && pi != myPI) {
							c.notices.add(new Notice(_t("x_wants_to_surrender", pi.name())));
						}
					}
				} else if (cmd.has("fleetOwnerShipID")) {
					Airship fleetOwnerShip = c.getShip(cmd.getJSONObject("fleetOwnerShipID"));
					if (fleetOwnerShip != null && fleetOwnerShip.owner != null) {
						if (!c.surrenderedOwners.contains(fleetOwnerShip.owner)) {
							//System.out.println("Surrendering as " + fleetOwnerShip.owner.getName());
							if (c.campaignWorld != null) {
								c.notices.add(new Notice(_t("x_wants_to_surrender", c.campaignWorld.map.owner(fleetOwnerShip.owner).getName())));
							}
							c.surrenderedOwners.add(fleetOwnerShip.owner);
						}
					}
				} else {
					c.sides.get(cmd.getInt("side")).surrendered = true;
				}
			}
		});
		EXECS.put("checksum", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				int hash = cmd.getInt("hash");
				int time = cmd.getInt("time");
				if (!c.desyncConfirmed && c.hashHistory.containsKey(time) && !c.hashHistory.get(time).equals(hash)) {
					String info = "New checksum error at #" + time + "\n" + c.hashHistory.get(time) + " vs " + hash;
					if (c.recording != null) {
						c.recording.header.placeName = info;
						info =
								c.recording.header.toJSON().toString() + "\n" +
								Compression.compressToString(c.recording.toJSON().toString());
					}
					c.desyncConfirmed = true;
					c.g.reportError(_t("network_diverge_warning"), null, info, false, true);
				}
			}
		});
		EXECS.put("focusOnShooting", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.focusOnShooting = true;
				ship.focusOnRepair = false;
				ship.focusOnFirefighting = false;
				ship.focusOnMoving = false;
				ship.commandGiven();
			}
		});
		EXECS.put("focusOnFirefighting", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.focusOnShooting = false;
				ship.focusOnRepair = false;
				ship.focusOnFirefighting = true;
				ship.focusOnMoving = false;
				ship.commandGiven();
			}
		});
		EXECS.put("focusOnRepair", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.focusOnShooting = false;
				ship.focusOnRepair = true;
				ship.focusOnFirefighting = false;
				ship.focusOnMoving = false;
				ship.commandGiven();
			}
		});
		EXECS.put("focusOnMoving", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.focusOnShooting = false;
				ship.focusOnRepair = false;
				ship.focusOnFirefighting = false;
				ship.focusOnMoving = true;
				ship.commandGiven();
			}
		});
		EXECS.put("fireMode", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.fireMode = FireMode.valueOf(cmd.getString("value"));
				ship.fireOrderSpoken = false;
				ship.commandGiven();
			}
		});
		EXECS.put("doAbility", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				Airship target = null;
				if (cmd.has("target")) {
					target = c.getShip(cmd.getJSONObject("target"));
				}
				c.doAbility(ship, CombatAbility.valueOf(cmd.getString("name")), target);
			}
		});
		EXECS.put("sinkhole", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				if (ship.usedAbilities.contains(HeroType.CombatAbility.SINKHOLE)) { return; }
				if (ship.getCaptain() == null || !ship.getCaptain().type.combatAbilities.contains(HeroType.CombatAbility.SINKHOLE)) { return; }
				ship.usedAbilities.add(HeroType.CombatAbility.SINKHOLE);
				c.sinkhole(cmd.getDouble("x"));
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("cast " + HeroType.CombatAbility.SINKHOLE.name() + " " + ship.getCaptain().type.name, c.sideOf(ship), cmd.getDouble("x"), 0, null, null));
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("received " + HeroType.CombatAbility.SINKHOLE.name(), c.otherSide(c.sideOf(ship)), cmd.getDouble("x"), 0, null, null));
			}
		});
		EXECS.put("crashZone", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				if (ship.usedAbilities.contains(HeroType.CombatAbility.CRASH_ZONE)) { return; }
				if (ship.getCaptain() == null || !ship.getCaptain().type.combatAbilities.contains(HeroType.CombatAbility.CRASH_ZONE)) { return; }
				ship.usedAbilities.add(HeroType.CombatAbility.CRASH_ZONE);
				c.addCrashZone(cmd.getDouble("x"));
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("cast " + HeroType.CombatAbility.CRASH_ZONE.name() + " " + ship.getCaptain().type.name, c.sideOf(ship), cmd.getDouble("x"), 0, null, null));
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("received " + HeroType.CombatAbility.CRASH_ZONE.name(), c.otherSide(c.sideOf(ship)), cmd.getDouble("x"), 0, null, null));
				c.particles.add(new Particle(ParticleType.ofName("magic2"), cmd.getDouble("x"), AGame.GROUND_LEVEL - 400));
				c.play("magic2", cmd.getDouble("x"), AGame.GROUND_LEVEL - 400, 3);
			}
		});
		EXECS.put("aircraftMode", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.aircraftMode = AircraftBehaviourMode.valueOf(cmd.getString("value"));
				ship.aircraftOrderSpoken = false;
				ship.commandGiven();
			}
		});
		EXECS.put("releaseOneUse", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				String what = cmd.getString("what");
				if (what.equals("lift")) {
					ship.releaseOneUseLift = true;
				}
				if (what.equals("propulsion")) {
					ship.releaseOneUsePropulsion = true;
				}
				if (what.equals("weapons")) {
					ship.releaseOneUseWeapons = true;
				}
				ship.commandGiven();
			}
		});
		EXECS.put("ground", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.groundShip();
				ship.commandGiven();
			}
		});
		EXECS.put("abandon", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.abandonShip(c);
				ship.commandGiven();
			}
		});
		EXECS.put("moveTo", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.moveTo = new Pt(cmd.getDouble("x"), cmd.getDouble("y"));
				ship.flipTo = cmd.getBoolean("flipTo");
				ship.ramming = false;
				ship.grounding = false;
				ship.sitting = false;
				ship.moveMode = Airship.MoveMode.valueOf(cmd.optString("moveMode", Airship.MoveMode.DEFAULT.name()));
				if (ship.burstOfSpeedTime > 0) {
					ship.commandPoints = ship.commandPoints - ship.commandPoints / Airship.BURST_OF_SPEED_MOVE_CMD_DIV;
					ship.wasReadyForCommand = false;
				} else {
					ship.commandGiven();
				}
			}
		});
		EXECS.put("ram", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.moveTo = new Pt(cmd.getDouble("x"), cmd.getDouble("y"));
				ship.flipTo = cmd.getBoolean("flipTo");
				ship.ramming = true;
				ship.grounding = false;
				ship.ramOrderSpoken = false;
				ship.sitting = false;
				ship.moveMode = Airship.MoveMode.valueOf(cmd.optString("moveMode", Airship.MoveMode.DEFAULT.name()));
				ship.commandGiven();
			}
		});
		EXECS.put("fireAt", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				if (c.getShip(cmd.getJSONObject("target")) == null) { return; }
				ship.fireAt = c.getShip(cmd.getJSONObject("target"));
				ship.commandGiven();
			}
		});
		EXECS.put("board", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				Airship target = c.getShip(cmd.getJSONObject("target"));
				if (target == null || !target.canBeBoarded()) { return; }
				ship.board = target;
				ship.commandGiven();
			}
		});
		EXECS.put("tetherAt", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				if (c.getShip(cmd.getJSONObject("target")) == null) { return; }
				ship.tetherAt = c.getShip(cmd.getJSONObject("target"));
				ship.commandGiven();
			}
		});
		EXECS.put("cutOwnTethers", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.tetherAt = null;
				for (Module m : ship.modules) {
					m.tether = null;
				}
				ship.commandGiven();
			}
		});
		EXECS.put("chat", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				PlayerInfo pi = c.idToPlayer.get(cmd.getInt("id"));
				if (pi != null) {
					c.chatMessages.add(new ChatMsg(pi, cmd.getString("text"), DateTime.now()));
				}
			}
		});
		EXECS.put("moveTroops", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Side side = c.sides.get(cmd.getInt("side"));
				GridBody source = c.getGridBody(cmd, "source");
				GridBody target = c.getGridBody(cmd, "target");
				if (source == null || target == null) { return; }
				boolean boardersOnly = target instanceof Airship && !side.ships.contains(target);
				boolean shouted = false;
				for (Crewman cm : side.troops) {
					if (boardersOnly && !cm.type.canBoard) { continue; }
					if (cm.attachedTo == source) {
						cm.ultimateBoardTarget = (GridBody) target;
						cm.proximateBoardTarget = null;
						cm.hookLaunched = false;
						cm.walkToGR = null;
						cm.walkToTargetGR = null;
						if (!shouted) {
							cm.doShout(cm.pickShout("troopMove"));
							shouted = true;
						}
					}
				}
			}
		});
		EXECS.put("setCombatSpeed", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				CombatSpeed cs = CombatSpeed.valueOf(cmd.getString("speed"));
				if (cs.isMPCapable()) {
					if (c.speedVoters != null) {
						c.speedVoters.put(cmd.getInt("voterID"), cs);
						c.speed = CombatSpeed.VERY_FAST;
						for (CombatSpeed csv : c.speedVoters.values()) {
							if (csv.getMult() < c.speed.getMult()) {
								c.speed = csv;
							}
						}
					} else {
						c.speed = cs;
					}
				}
			}
		});
		EXECS.put("placeShip", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Combat.Side side = c.sides.get(cmd.getInt("side"));
				Airship ship = side.getShip(cmd.getString("ship"));
				if (ship != null) {
					if (ship.nonCombat()) { return; }
					if (side.reserve.contains(ship)) {
						side.reserve.remove(ship);
						side.ships.add(ship);
						ship.switchedReserve = true;
					}
					double oldX = ship.getX();
					double oldY = ship.getY();
					ship.setFlipped(cmd.getBoolean("flipped"), c); // Also flips crew as needed.
					ship.setX(cmd.getDouble("x"));
					ship.setY(cmd.getDouble("y"));
					ship.moveTo = new Pt(ship.getX(), ship.getY());
					ship.flipTo = cmd.getBoolean("flipped");
					ship.lastPlaced = new Pt(ship.getX(), ship.getY());
					ship.lastPlacedFlipped = ship.flipped;
					ship.version++;
					if (cmd.optBoolean("flanking", false)) {
						for (Side s : c.sides) {
							for (Crewman cm : s.troops) {
								if (cm.attachedTo == ship) {
									cm.setX(cm.getX() - oldX + ship.getX());
									cm.setY(cm.getY() - oldY + ship.getY());
								}
							}
						}
						ship.usedAbilities.add(CombatAbility.FLANK);
						c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("cast " + HeroType.CombatAbility.FLANK.name() + " " + ship.getCaptain().type.name, c.sideOf(ship), cmd.getDouble("x"), cmd.getDouble("y"), null, null));
						c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("received " + HeroType.CombatAbility.FLANK.name(), c.otherSide(c.sideOf(ship)), cmd.getDouble("x"), cmd.getDouble("y"), null, null));
						c.play("spin_up", ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2, 3);
						c.particles.add(new Particle(ParticleType.ofName("flank"), ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2));
					}
					ship.resetWeaponBarrels();
					ship.resetTentacles();
				}
			}
		});
		EXECS.put("reserveShip", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Combat.Side side = c.sides.get(cmd.getInt("side"));
				Airship ship = side.getShip(cmd.getString("ship"));
				if (ship != null) {
					if (side.ships.contains(ship) && !ship.isBeingBoarded()) {
						side.ships.remove(ship);
						side.reserve.add(ship);
						ship.switchedReserve = cmd.getBoolean("isReserveSwitch");
					}
				}
			}
		});
		EXECS.put("setDirectControl", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				if (!c.allowDirectControl) { return; }
				int controllerID = cmd.optInt("controllerID", -1);
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				for (Airship s : c.sideOf(ship).ships) {
					if (s.multiplayerControllerID == ship.multiplayerControllerID && s.getDirectControlID() == controllerID) {
						ship.setDirectControl(-1);
					}
				}
				ship.setDirectControl(controllerID);
				ship.commandGiven();
			}
		});
		EXECS.put("speedOrder", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.speedOrder = ShipSpeed.valueOf(cmd.getString("speed"));
			}
		});
		EXECS.put("altitudeOrder", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.altitudeOrder = cmd.getInt("altitude");
			}
		});
		EXECS.put("flipOrder", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				ship.flipTo = cmd.getBoolean("flipTo");
			}
		});
		EXECS.put("aiControl", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				Side side = c.sideOf(ship);
				if (side == null) { return; }
				ship.aiControl = cmd.getBoolean("enabled");
				if (cmd.getBoolean("enabled")) {
					ship.setAI(new TacticalAI(ship, c, side, c.otherSide(side)));
					ship.getAI().doesSurrender = false;
				} else {
					ship.setAI(null);
				}
			}
		});
		EXECS.put("fireOrder", new CommandExecutor() {
			@Override
			public void run(JSONObject cmd, Combat c) {
				Airship ship = c.getShip(cmd.getJSONObject("id"));
				if (ship == null) { return; }
				if (cmd.has("crewTarget")) {
					Side enemy = c.otherSide(c.sideOf(ship));
					if (enemy == null) { return; }
					int ctIndex = cmd.getInt("crewTarget");
					if (ctIndex < enemy.troops.size()) {
						Crewman target = enemy.troops.get(ctIndex);
						JSONArray ms = cmd.getJSONArray("modules");
						for (int i = 0; i < ms.length(); i++) {
							int mIndex = ms.getInt(i);
							if (mIndex >= ship.modules.size()) { continue; }
							Module m = ship.modules.get(mIndex);
							m.targetTroopOverride = target;
						}
					}
					return;
				}
				Module.DirectControlFireMode mode = Module.DirectControlFireMode.valueOf(cmd.getString("mode"));
				if (cmd.has("target")) {
					Airship target = c.getShip(cmd.getJSONObject("target"));
					if (target == null) { return; }
					int gx = cmd.getInt("gx");
					int gy = cmd.getInt("gy");
					Tile tt = target.tileAt(gx, gy);
					if (tt == null) { return; }
					JSONArray ms = cmd.getJSONArray("modules");
					for (int i = 0; i < ms.length(); i++) {
						int mIndex = ms.getInt(i);
						if (mIndex >= ship.modules.size()) { continue; }
						Module m = ship.modules.get(mIndex);
						m.fireMode = mode;
						m.prevTarget = tt;
						m.prevTargetShip = target;
					}
				} else {
					JSONArray ms = cmd.getJSONArray("modules");
					for (int i = 0; i < ms.length(); i++) {
						int mIndex = ms.getInt(i);
						if (mIndex >= ship.modules.size()) { continue; }
						Module m = ship.modules.get(mIndex);
						m.fireMode = mode;
					}
				}
			}
		});
	}
	
	private static final class CombatOutcome {
		boolean[] sideNotLost;
		boolean done;

		public CombatOutcome(boolean[] sideNotLost, boolean done) {
			this.sideNotLost = sideNotLost;
			this.done = done;
		}
		
		public CombatOutcome(JSONObject o) {
			done = o.getBoolean("done");
			JSONArray a = o.getJSONArray("sideNotLost");
			sideNotLost = new boolean[a.length()];
			for (int i = 0; i < a.length(); i++) {
				sideNotLost[i] = a.getBoolean(i);
			}
		}
		
		public JSONObject toJSON() {
			JSONObject o = new JSONObject().put("done", done);
			JSONArray a = new JSONArray();
			for (boolean b : sideNotLost) {
				a.put(b);
			}
			o.put("sideNotLost", a);
			return o;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof CombatOutcome)) { return false; }
			CombatOutcome co2 = (CombatOutcome) o;
			if (done != co2.done || sideNotLost.length != co2.sideNotLost.length) {
				return false;
			}
			for (int i = 0; i < sideNotLost.length; i++) {
				if (sideNotLost[i] != co2.sideNotLost[i]) { return false; }
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			int c = done ? 1 : 0;
			int n = 2;
			for (boolean snl : sideNotLost) {
				if (snl) {
					c += n;
				}
				n *= 2;
			}
			return c;
		}
	}
	
	private CombatOutcome outcome() {
		boolean[] nonLosers = new boolean[sides.size()];
		int numNonLosers = 0;
		for (int i = 0; i < nonLosers.length; i++) {
			if (!lost(sides.get(i))) {
				nonLosers[i] = true;
				numNonLosers++;
			}
		}
		return new CombatOutcome(nonLosers, numNonLosers <= 1 || msSinceInterestingCombatEvent >= NO_INTERESTING_COMBAT_EVENT_FOR_DRAW_MS);
	}
	
	public Side sideOf(Airship ship) {
		int ssz = sides.size();
		for (int si = 0; si < ssz; si++) {
			Side side = sides.get(si);
			if (side.ships.contains(ship) || side.reserve.contains(ship)) { return side; }
		}
		return null;
	}
	
	public Side sideOf(Crewman cm) {
		if (cm.ship != null) {
			return sideOf(cm.ship);
		}
		if (cm.boardingShip != null) {
			return otherSide(sideOf(cm.boardingShip));
		}
		int ssz = sides.size();
		for (int si = 0; si < ssz; si++) {
			Side side = sides.get(si);
			if (side.troops.contains(cm)) { return side; }
		}
		return null;
	}

	public boolean won(Side s) {
		if (s.lost()) { return false; }
		for (Side side : sides) {
			if (side != s && !side.lost()) { return false; }
		}
		return true;
	}

	public boolean lost(Side s) {
		return s.lost();
	}

	public Side getSideForID(int id) {
		return playerToSide.isEmpty() ? sides.get(id) : playerToSide.get(idToPlayer.get(id));
	}
	
	public void setRandomSeed(long seed) {
		this.randomSeed = seed;
		this.r = new GuardedRandom(seed);
	}
	
	public Combat(AirshipGame g, Client client, Server server, long seed, ArrayList<PlayerInfo> pis, int pointsLimit, TimeOfDay timeOfDay, int techTier, boolean instantCommandRegeneration, boolean allowDirectControl, String combatName) {
		this.mpClient = client;
		this.mpServer = server;
		this.randomSeed = seed;
		this.combatName = combatName;
		this.r = new GuardedRandom(seed);
		this.pointsLimit = pointsLimit;
		this.g = g;
		this.timeOfDay = timeOfDay;
		this.instantCommandRegeneration = instantCommandRegeneration;
		this.allowDirectControl = allowDirectControl;
		idToPlayer = new HashMap<Integer, PlayerInfo>();
		StringBuilder[] sName = { new StringBuilder(), new StringBuilder() };
		ArrayList<CoatOfArms>[] sArms = new ArrayList[2];
		sArms[0] = new ArrayList<CoatOfArms>();
		sArms[1] = new ArrayList<CoatOfArms>();
		ArrayList<Airship>[] sFleet = new ArrayList[2];
		sFleet[0] = new ArrayList<Airship>();
		sFleet[1] = new ArrayList<Airship>();
		for (PlayerInfo pi : pis) {
			if (pi.side == 0 || pi.side == 1) {
				if (sName[pi.side].length() != 0) {
					sName[pi.side].append(" + ");
				}
				sName[pi.side].append(pi.name);
				sArms[pi.side].add(pi.getArms());
				for (Airship s : pi.fleet) {
					s.setOwner(pi.id, null, pi.getArms());
				}
				sFleet[pi.side].addAll(pi.fleet);
			}
		}
		for (int i = 0; i < 2; i++) {
			sides.add(new Side(sName[i].toString(), false, Tech.getBonusesForTier(techTier)));
			sides.get(i).ships = sFleet[i];
			sides.get(i).arms = Empire.mergedArms(sArms[i], r);
		}
		for (PlayerInfo pi : pis) {
			idToPlayer.put(pi.id, pi);
			playerToSide.put(pi, pi.side == -1 ? null : sides.get(pi.side));
			if (!pi.isSpectator() && pi.isAI()) {
				for (Airship s : pi.fleet) {
					s.setAI(new TacticalAI(s, this, sides.get(pi.side), otherSide(sides.get(pi.side))));
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			frameQueue.add(new JSONObject().put("messages", new JSONArray()).put("time", -1));
		}
	}

	public boolean isSingleMultiplayer() {
		return mpClient != null;
	}
	
	public boolean canAbandonShip(Airship ship) {
		if (!ship.type.onGround && (ship.lastGrounded == null || ship.msSinceOnGround > 100)) { return false; }
		Side s = sideOf(ship);
		if (s == null) { return false; }
		for (Airship as : s.ships) {
			if (as != ship && as.inCombat(this)) {
				return true;
			}
		}
		return false;
	}	

	public Combat(AirshipGame g, TimeOfDay timeOfDay) {
		Side you = new Side("You_player", true, Tech.getStandardBonuses());
		you.arms = CoatEditor.getMyStrategicArms();
		sides.add(you);
		sides.add(new Side("Enemy_player", true, Tech.getStandardBonuses()));
		this.g = g;
		this.timeOfDay = timeOfDay;
		this.randomSeed = AGame.ANIM_R.nextLong();
		this.r = new GuardedRandom(randomSeed);
	}
	
	public Combat(AirshipGame g, TimeOfDay timeOfDay, GuardedRandom r, CampaignWorld cw) {
		Side you = new Side("You_player", true, Tech.getStandardBonuses());
		you.arms = CoatEditor.getMyStrategicArms();
		sides.add(you);
		sides.add(new Side("Enemy_player", true, Tech.getStandardBonuses()));
		this.g = g;
		this.timeOfDay = timeOfDay;
		this.randomSeed = AGame.getSeed(r);
		this.r = r;
		this.campaignWorld = cw;
		instantCommandRegeneration = cw.map.strategicRapidCommands;
		allowDirectControl = cw.map.allowDirectControl;
	}

	private void finish() {
		if (mpClient != null && mpClient == g.lanClient) {
			g.lanClient.close();
			g.lanClient = null;
		}
		if (mpServer != null) {
			mpServer.close();
			g.lanServer = null;
		}
		try {
			saveRecording();
		} catch (IOException e) {
			e.printStackTrace();
			g.showError(_t("Unable_to_save_combat_recording."));
		}
	}
	
	static boolean reportedDupe = false;
	
	private void checkSidesForDupes() {
		for (Side s : sides) {
			int ssz = s.ships.size();
			int rsz = s.reserve.size();
			for (int si = 0; si < ssz; si++) {
				Airship ship = s.ships.get(si);
				int count = 0;
				for (int si2 = 0; si2 < ssz; si2++) {
					if (s.ships.get(si2) == ship) {
						count++;
					}
				}
				for (int si2 = 0; si2 < rsz; si2++) {
					if (s.reserve.get(si2) == ship) {
						count++;
					}
				}
				if (count > 1) {
					if (!reportedDupe) {
						AirshipGame.instance.reportError("Duplicate in combat ships", null, null, false, true);
						reportedDupe = true;
					}
					while (s.reserve.contains(ship)) {
						s.reserve.remove(ship);
					}
					while (s.ships.contains(ship)) {
						s.ships.remove(ship);
					}
					s.ships.add(ship);
					ssz = s.ships.size();
					rsz = s.reserve.size();
					si = 0;
				}
			}
			
			ssz = s.ships.size();
			rsz = s.reserve.size();
			for (int si = 0; si < rsz; si++) {
				Airship ship = s.reserve.get(si);
				int count = 0;
				for (int si2 = 0; si2 < ssz; si2++) {
					if (s.ships.get(si2) == ship) {
						count++;
					}
				}
				for (int si2 = 0; si2 < rsz; si2++) {
					if (s.reserve.get(si2) == ship) {
						count++;
					}
				}
				if (count > 1) {
					if (!reportedDupe) {
						AirshipGame.instance.reportError("Duplicate in combat reserve", null, null, false, true);
						reportedDupe = true;
					}
					while (s.reserve.contains(ship)) {
						s.reserve.remove(ship);
					}
					while (s.ships.contains(ship)) {
						s.ships.remove(ship);
					}
					s.reserve.add(ship);
					ssz = s.ships.size();
					rsz = s.reserve.size();
					si = 0;
				}
			}
		}
	}
	
	public int cheapHash() {
		int h = 7;
		for (Side s : sides) {
			int ssz = s.ships.size();
			for (int si = 0; si < ssz; si++) {
				h = h * 47 + s.ships.get(si).cheapHash();
			}
			int tsz = s.troops.size();
			for (int ti = 0; ti < tsz; ti++) {
				h = h * 43 + s.troops.get(ti).cheapHash();
			}
		}
		for (Shot s : shots) {
			h = h * 37 + Double.valueOf(s.sX).hashCode();
			h = h * 37 + Double.valueOf(s.sY).hashCode();
			h = h * 37 + Double.valueOf(s.tX).hashCode();
			h = h * 37 + Double.valueOf(s.tY).hashCode();
			h = h * 37 + s.time;
		}
		for (LandFormation lf : landFormations) {
			h = h * 31 + Double.valueOf(lf.getX()).hashCode();
			h = h * 31 + Double.valueOf(lf.getY()).hashCode();
			h = h * 31 + Double.valueOf(lf.getxSpeed()).hashCode();
			h = h * 31 + Double.valueOf(lf.getySpeed()).hashCode();
			h = h * 31 + Double.valueOf(lf.getBBWidth()).hashCode();
			h = h * 31 + Double.valueOf(lf.getBBHeight()).hashCode();
		}
		return h;
	}
	
	private int localMsAccum = 0;
	
	private Side lastViewingSide;
	
	public void partiallyRepairAllShips(boolean defendersAreInCity) {
		for (Side side : sides) {
			if (side == sides.get(1) && defendersAreInCity) {
				for (Airship s : side.ships) {
					s.repair(/* resetXP */ false);
				}
				for (Airship s : side.reserve) {
					s.repair(/* resetXP */ false);
				}
			} else {
				for (Airship s : side.ships) {
					s.repairPartially();
				}
				for (Airship s : side.reserve) {
					s.repairPartially();
				}
			}
		}
	}
	
	public void fullyRepairAllShips() {
		for (Side side : sides) {
			for (Airship s : side.ships) {
				s.repair(/* resetXP */ false);
			}
			for (Airship s : side.reserve) {
				s.repair(/* resetXP */ false);
			}
		}
	}
		
	public void tick(int ms, Side viewingSide, double lightningChance, int serverTickMult) {
		if (slowMotion || speed.div != 1) {
			hasHadFractionalSpeed = true;
		}
		
		lastViewingSide = viewingSide;
		if (slowMotion && !isSingleMultiplayer()) {
			ms /= 4;
		}
		
		if (isSingleMultiplayer()) {
			int fqsz = frameQueue.size();
			//System.out.println("fqsz " + fqsz);
			if (fqsz < 3) {
				ms /= 2;
			} else if (fqsz == 3) {
				ms = ms * 3 / 4;
			} else if (fqsz > 15) {
				ms = ms * 2;
			} else if (fqsz > 7) {
				ms = ms * 3 / 2;
			}
			/*if (ms != TICK_LENGTH) {
				System.out.println("timewarp to " + ms);
			}*/
			
			localMsAccum += ms;
			
			if (localMsAccum >= TICK_LENGTH) {
				localMsAccum -= TICK_LENGTH;
				doTick(TICK_LENGTH, viewingSide, lightningChance, serverTickMult);
			}
			if (localMsAccum >= TICK_LENGTH) {
				localMsAccum -= TICK_LENGTH;
				doTick(TICK_LENGTH, viewingSide, lightningChance, serverTickMult);
			}
		} else {
			doTick(ms, viewingSide, lightningChance, serverTickMult);
		}
	}
			
	private void doTick(int ms, Side viewingSide, double lightningChance, int serverTickMult) {
		r.guard = false;
		checkSidesForDupes();
		
		if (combatFinished) { return; }
				
		if (connectionLost()) {
			finish();
			return;
		}

		frameMID = runTickCommands(serverTickMult);
		
		//System.out.println("fqueuesize " + frameQueue.size());

		if ((!isTimeMoving() && !runSimAnyway) || startCountdown > 0) {
			return;
		}
		
		doGenericTick(ms, viewingSide, lightningChance);
		r.guard = true;
	}
	
	public void doGenericTick(int ms, Side viewingSide, double lightningChance) {		
		if (recording != null && recording.initialCombat == null) {
			recording.initialCombat = toJSON();
			JSONArray mods = new JSONArray();
			for (Mod m : Mod.getEnabledMods()) {
				mods.put(new JSONObject().put("id", m.id).put("name", m.getName()));
			}
			recording.initialCombat.put("mods", mods);
			recording.initialCombat.put("expansions", Expansion.names(Expansion.enableds()));
			recording.header.extractSideInfo(this);
		}
		
		for (Iterator<Blast> it = blasts.iterator(); it.hasNext();) {
			if (it.next().tick(ms)) {
				it.remove();
			}
		}
		
		if (LaunchSettings.recordEntireCombatState && playback != null && playbackIndex >= 1 && playback.tickCombats.size() >= playbackIndex) {
			String state = toJSON().toString(4);
			String orig = Compression.decompressFromString(playback.tickCombats.get(playbackIndex - 1));
			if (!state.equals(orig)) {
				try {
					FileUtils.write(new File("C:\\Users\\Zarkonnen\\Desktop\\replay.json"), state);
					FileUtils.write(new File("C:\\Users\\Zarkonnen\\Desktop\\orig.json"), orig);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Runtime.getRuntime().exit(0);
			}
		}
		
		// Surprise attack mechanic.
		if (simTick == 0) {
			boolean hasSurpriseAttack = false;
			for (Airship s : sides.get(0).ships) {
				if (s.surpriseAttackFromMedals || (s.getCaptain() != null && s.getCaptain().type.surpriseAttack)) {
					hasSurpriseAttack = true;
					break;
				}
			}
			if (!hasSurpriseAttack) {
				for (Airship s : sides.get(0).reserve) {
					if (s.surpriseAttackFromMedals || (s.getCaptain() != null && s.getCaptain().type.surpriseAttack)) {
						hasSurpriseAttack = true;
						break;
					}
				}
			}
			if (hasSurpriseAttack) {
				for (Airship ship : sides.get(1).ships) {
					ship.commandPoints = 0;
				}
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("surpriseAttacker", sides.get(0), 0, 0, null, null));
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("surpriseAttacked", sides.get(1), 0, 0, null, null));
			}
		}
		
		// Crash zones
		for (int i = 0; i < crashZones.size(); i++) {
			CrashZone cz = crashZones.get(i);
			cz.timeout -= ms;
			double x = cz.cx + AGame.ANIM_R.nextInt(CRASH_ZONE_W) - CRASH_ZONE_W / 2;
			double y = AGame.GROUND_LEVEL + 500 - AGame.ANIM_R.nextInt(3000);
			if (landFormations.get(0).yBoundaryAt(x) > y) {
				particles.add(new Particle(ParticleType.ofName(cz.halfStrength ? "crashZoneMagicHalfStrength": "crashZoneMagic"), x, y));
			}
			if (cz.timeout <= 0) {
				crashZones.remove(i);
				i--;
			}
		}
		
		for (Side s : sides) {
			s.aiTick();
		}
						
		if (recording != null) {
			if (speed.div != 1) {
				recording.expectDivergence = true;
			}
			recording.tickCommands.add(new Pair<Long, JSONArray>(frameMID, commandsExecutedInThisFrame));
			commandsExecutedInThisFrame = new JSONArray();
			if (LaunchSettings.recordEntireCombatState) {
				recording.tickCombats.add(Compression.compressToString(toJSON().toString(4)));
			}
		}
		
		/*if (playback != null && time > 60000 && time < 180000) {
			String state = toJSON().toString(4);
			int fullHash = state.hashCode();
			File dir = new File("/home/zar/Desktop/states/" + time);
			File f = new File(dir, fullHash + ".json");
			if (dir.exists()) {
				if (!f.exists()) {
					try {
						FileUtils.write(f, state);
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("different at " + time);
					Runtime.getRuntime().exit(0);
				} else {
					//System.out.println("exists " + time + " " + fullHash);
				}
			} else {
				try {
					System.out.println("write " + time + " " + fullHash);
					dir.mkdirs();
					FileUtils.write(f, state);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}*/
		
		CombatOutcome co = outcome();
		if (co.done) {
			if (!co.equals(currentCombatOutcome)) {
				finishedCountdown = FINISHED_COUNTDOWN_MS;
			}
			currentCombatOutcome = co;
			finishedCountdown -= ms;
			if (finishedCountdown <= 0) {
				combatFinished = true;
				finish();
				return;
			}
		} else {
			finishedCountdown = FINISHED_COUNTDOWN_MS;
		}

		runSimAnyway = false;
		
		double wind = timeOfDay.effect.wind;

		if (recording != null && /*time % (TICK_LENGTH * 100) == 0 && */!hasHadFractionalSpeed) {
			recording.tickHashes.put(time, cheapHash());
		}
		
		simTick++;
		time += ms;
		msSinceInterestingCombatEvent += ms;
		damageStatsAge += ms;
		if (damageStatsAge >= 8000) {
			damageStatsAge -= 8000;
			for (Side side : sides) {
				for (Airship ship : side.ships) {
					ship.damageTaken[1] = ship.damageTaken[0];
					ship.damageTaken[0] = 0;
					ship.damageMissed[1] = ship.damageMissed[0];
					ship.damageMissed[0] = 0;
					ship.damageTakenFromAbove[1] = ship.damageTakenFromAbove[0];
					ship.damageTakenFromAbove[0] = 0;
					for (int i = 0; i < ship.modules.size(); i++) {
						Module m = ship.modules.get(i);
						m.damageDealt[1] = m.damageDealt[0];
						m.damageDealt[0] = 0;
						m.damageNotDealt[1] = m.damageNotDealt[0];
						m.damageNotDealt[0] = 0;
					}
				}
			}
		} 
		//System.out.println(netTick + " .. " + simTick);

		//#SpikeProfiler.active = false;
		//#SpikeProfiler.outputDir = new File("spikes").getAbsoluteFile();
		//#SpikeProfiler.start("combat");
		
		if (physics == null) {
			//#SpikeProfiler.start("init physics");
			physics = new Physics();
			physics.gravity = AGame.G;
			for (LandFormation lf : landFormations) {
				physics.bodies.add(lf);
			}
			//#SpikeProfiler.end("init physics");
		}
		//#SpikeProfiler.start("update bodies");
		for (Side s : sides) {
			for (Airship a : s.ships) {
				if (!a.removeMe(this) && !physics.bodies.contains(a)) {
					physics.bodies.add(a);
					for (Module m : a.modules) {
						for (Leg l : m.legs) {
							physics.bodies.add(l.foot);
						}
						for (Wheel w : m.wheels) {
							physics.bodies.add(w.body);
						}
					}
					if (resetMoveTos) {
						a.moveTo = new Pt(a.getX(), a.getY());
					}
				}
				int msz = a.modules.size();
				for (int mi = 0; mi < msz; mi++) {
					Module m = a.modules.get(mi);
					if (m.type.createsExceptionalCombatEventAfterMs() != 0 && m.type.createsExceptionalCombatEventAfterMs() > time - ms && m.type.createsExceptionalCombatEventAfterMs() <= time) {
						exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyHas " + m.type.name, otherSide(s),
								a.getX() + a.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS + m.type.getW() * AGame.SGS / 2,
								a.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS / 2,
								null, a));
					}
					/*int lsz = m.legs.size();
					for (int li = 0; li < lsz; li++) {
						Leg l = m.legs.get(li);
						if (m.hp <= 0) {
							physics.bodies.remove(l.foot);
						} else if (!physics.bodies.contains(l.foot)) {
							physics.bodies.add(l.foot);
						}
					}*/
				}
			}
			for (Airship a : s.reserve) {
				physics.bodies.remove(a);
				for (Module m : a.modules) {
					for (Leg l : m.legs) {
						physics.bodies.remove(l.foot);
					}
					for (Wheel w : m.wheels) {
						physics.bodies.remove(w.body);
					}
				}
			}
		}
		for (Side s : sides) {
			if (s.originalComposition.isEmpty()) {
				s.originalComposition.addAll(s.ships);
				s.originalComposition.addAll(s.reserve);
				s.initStats();
				for (Airship a : s.ships) {
					for (Module m : a.modules) {
						if (m.type.getReload(a.currentBonuses) > 0) {
							// Have weapons be randomly slightly loaded to prevent everything shooting at once.
							m.shootAccumulator = m.type.getReload(a.currentBonuses) / 2 + r.nextInt(m.type.getReload(a.currentBonuses)) / 2;
						}
					}
				}
			}
		}
		//#SpikeProfiler.endStart("update bodies", "events");
		
		if (time >= 6000 && time - ms < 6000) {
			if (sides.get(0).getCost() > 4 * sides.get(1).getCost()) {
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("mySideMuchStronger", sides.get(0), 0, AGame.GROUND_LEVEL - 300, null, null));
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("mySideMuchWeaker", sides.get(1), 0, AGame.GROUND_LEVEL - 300, null, null));
			} else if (sides.get(1).getCost() > 4 * sides.get(0).getCost()) {
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("mySideMuchStronger", sides.get(1), 0, AGame.GROUND_LEVEL - 300, null, null));
				exceptionalCombatEvents.add(new ExceptionalCombatEvent("mySideMuchWeaker", sides.get(0), 0, AGame.GROUND_LEVEL - 300, null, null));
			}
		}
		
		if (isRaid) {
			if (lost(sides.get(1))) {
				lootAmount = maxLootAmount;
			} else {
				lootAmount = 0;
				int amt = time / MS_PER_RAID_SUCCESS;
				int vic = 1;
				while (amt >= vic && lootAmount < maxLootAmount) {
					lootAmount++;
					amt -= vic;
					vic++;
				}
			}
		}

		//#SpikeProfiler.endStart("events", "physics");
		physics.tick(ms, this);
		//#SpikeProfiler.endStart("physics", "troopPhysics");
		TroopPhysics.tick(ms, this, viewingSide);
		//#SpikeProfiler.endStart("troopPhysics", "precalcAIValues");
		for (Side s : sides) {
			for (Airship ship : s.ships) {
				ship.precalcAIValues(this);
			}
		}
		//#SpikeProfiler.end("precalcAIValues");
		
		for (Side s : sides) {
			//#SpikeProfiler.start("side " + s.name);
			boolean won = won(s);
			boolean lost = lost(s);
			s.checkForCombatEvents(otherSide(s), this, won, lost, physics);
			
			s.tick(ms, this, won, physics, s == viewingSide);
			//#SpikeProfiler.end("side " + s.name);
		}
		
		//#SpikeProfiler.start("particles");
		
		if (particles.size() > LaunchSettings.maxParticles) {
			particles.subList(0, particles.size() - LaunchSettings.maxParticles).clear();
		}
		
		for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
			if (it.next().tick(ms, wind, this)) { it.remove(); }
		}
		
		//#SpikeProfiler.endStart("particles", "fragments");
		
		for (Iterator<Fragment> it = fragments.iterator(); it.hasNext();) {
			if (it.next().tick(ms, this)) { it.remove(); }
		}
		
		int fragCutoff = INITIAL_FRAG_LIFE_CUTOFF;
		while (fragments.size() > MAX_FRAGMENTS) {
			int i = 0;
			for (Iterator<Fragment> it = fragments.iterator(); it.hasNext();) {
				if (it.next().age > fragCutoff && i++ % 4 == 0) {
					it.remove();
				}
			}
			fragCutoff /= 2;
		}
		
		//#SpikeProfiler.endStart("fragments", "landFormations");
		
		ArrayList<LandFormation> newLandFormations = new ArrayList<LandFormation>();
		for (Iterator<LandFormation> it = landFormations.iterator(); it.hasNext();) {
			LandFormation lf = it.next();
			if (lf.tick(ms, this)) {
				it.remove();
			} else {
				ArrayList<LandFormation> nlfs = lf.splitIntoChunksIfNeeded();
				if (nlfs != null) {
					newLandFormations.addAll(nlfs);
				}
			}
		}
		for (LandFormation lf : newLandFormations) {
			landFormations.add(lf);
			physics.bodies.add(lf);
		}
		
		//#SpikeProfiler.endStart("landFormations", "shots");
		
		for (Iterator<Shot> it = shots.iterator(); it.hasNext();) {
			Shot s = it.next();
			if (s.tick(ms, this, sideOf(s.target) != viewingSide)) { it.remove(); }
		}
		
		//#SpikeProfiler.endStart("shots", "trails");
		
		for (Iterator<Trail> it = trails.iterator(); it.hasNext();) {
			if (it.next().tick(ms)) { it.remove(); }
		}
		
		//#SpikeProfiler.endStart("trails", "blockParticleEmitters");
				
		for (LandFormation lf : landFormations) {
			for (int gy = 0; gy < lf.grid.length; gy++) {
				for (int gx = 0; gx < lf.grid[gy].length; gx++) {
					if (lf.edge[gy][gx]) {
						LandBlockType lbt = lf.grid[gy][gx];
						Particle.Emitter em = lbt.particleEmitterByVariant.get(timeOfDay.effect.landscapeVisualVariant);
						if (em != null) {
							if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
								double px = lf.getX() + gx * AGame.SGS + AGame.SGS / 2;
								double py = lf.getY() + gy * AGame.SGS + AGame.SGS / 2;
								for (int i = 0; i < em.numParticles; i++) {
									particles.add(new Particle(em.t, px, py));
								}
								if (em.soundEffect != null) {
									play(em.soundEffect, px, py, lf.getxSpeed(), lf.getySpeed(), false);
								}
							}
						}
					}
				}
			}
		}
		
		//#SpikeProfiler.endStart("blockParticleEmitters", "moduleParticleEmitters");
		if (!SimplePref.REDUCED_VISUAL_NOISE.get()) {
			for (Side side : sides) { for (Airship ship : side.ships) {
				if (ship.glimmerTime > 0 || ship.blindnessTime > 0) {
					ParticleType gl = ParticleType.ofName("blinding_glimmer");
					for (int ci = 0; ci < ship.crew.size(); ci++) {
						Crewman c = ship.crew.get(ci);
						if (c.currentTile != null && c.currentTile.module.type.isWeapon()) {
							if (AGame.ANIM_R.nextDouble() < 0.01 * ms) {
								particles.add(new Particle(
										gl,
										ship.getX() + ship.gridXToWorldX(c.currentTile.x, 1) * AGame.SGS + AGame.SGS / 2,
										ship.getY() + c.currentTile.y * AGame.SGS + AGame.SGS / 2));
							}
						}
					}
				}
				if (ship.crosswindsTime > 0) {
					ParticleType cw = ParticleType.ofName("crosswinds");
					ParticleType cw2 = ParticleType.ofName("crosswinds_2");
					for (int ti = 0; ti < ship.tiles.size(); ti++) {
						if (AGame.ANIM_R.nextDouble() < 0.002 * ms) {
							Tile t = ship.tiles.get(ti);
							particles.add(new Particle(
										AGame.ANIM_R.nextBoolean() ? cw : cw2,
										ship.getX() + ship.gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2,
										ship.getY() + t.y * AGame.SGS + AGame.SGS / 2));
						}
					}
				}
				if (ship.gustOfWindTime > 0) {
					ParticleType cw = ParticleType.ofName("crosswinds");
					ParticleType cw2 = ParticleType.ofName("crosswinds_2");
					for (int ti = 0; ti < ship.tiles.size(); ti++) {
						if (AGame.ANIM_R.nextDouble() < 0.004 * ms) {
							Tile t = ship.tiles.get(ti);
							Particle p = new Particle(
										AGame.ANIM_R.nextBoolean() ? cw : cw2,
										ship.getX() + ship.gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2,
										ship.getY() + t.y * AGame.SGS + AGame.SGS / 2);
							p.dx = ship.gustOfWindDX * 5;
							p.dy = ship.gustOfWindDY * 5;
							particles.add(p);
						}
					}
				}
				if (ship.suddenStormTime > 0) {
					ParticleType cw = ParticleType.ofName("crosswinds");
					ParticleType cw2 = ParticleType.ofName("crosswinds_2");
					for (int ti = 0; ti < ship.tiles.size(); ti++) {
						if (AGame.ANIM_R.nextDouble() < 0.004 * ms) {
							Tile t = ship.tiles.get(ti);
							Particle p = new Particle(
										AGame.ANIM_R.nextBoolean() ? cw : cw2,
										ship.getX() + ship.gridXToWorldX(t.x, 1) * AGame.SGS + AGame.SGS / 2,
										ship.getY() + t.y * AGame.SGS + AGame.SGS / 2);
							p.dx = ship.suddenStormDX * 5;
							p.dy = 0;
							particles.add(p);
						}
					}
				}
				
				for (int di = 0; di < ship.decals.size(); di++) {
					Decal d = ship.decals.get(di);
					for (int ei = 0; ei < d.type.emitters.size(); ei++) {
						ModuleType.ModuleParticleEmitter em = d.type.emitters.get(ei);
						if (!em.inside || !ship.showingOutside) {
							int x = ship.flipped ? ship.getWidth() - d.x : d.x;

							if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
								double px = x * AGame.SGS + ship.getIntX() + (ship.flipped ? -1 : 1) * em.x * AGame.SGS;
								double py = d.y * AGame.SGS + ship.getIntY() + em.y * AGame.SGS;
								for (int i = 0; i < em.numParticles; i++) {
									particles.add(new Particle(em.t, px, py));
								}
								if (em.soundEffect != null) {
									play(em.soundEffect, px, py, ship.getxSpeed(), ship.getySpeed(), side == viewingSide);
								}
							}
						}
					}
				}

				for (Module m : ship.getModules()) {
					ArrayList<ModuleType.ModuleParticleEmitter> ems2 = m.getDamagedOrDestroyedEmitters();
					if (ems2 != null) {
						int ems2s = ems2.size();
						for (int emi = 0; emi < ems2s; emi++) {
							ModuleType.ModuleParticleEmitter em = ems2.get(emi);
							if (!em.inside || !ship.showingOutside) {
								int x = ship.flipped ? ship.getWidth() - m.x : m.x;

								if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
									double px = x * AGame.SGS + ship.getIntX() + (ship.flipped ? -1 : 1) * em.x * AGame.SGS;
									double py = m.y * AGame.SGS + ship.getIntY() + em.y * AGame.SGS;
									for (int i = 0; i < em.numParticles; i++) {
										particles.add(new Particle(em.t, px, py));
									}
									if (em.soundEffect != null) {
											play(em.soundEffect, px, py, ship.getxSpeed(), ship.getySpeed(), side == viewingSide);
									}
								}
							}
						}
					}
					if (m.running()) {
						ArrayList<ModuleType.ModuleParticleEmitter> ems = m.getEmitters();
						int emss = ems.size();
						for (int emi = 0; emi < emss; emi++) {
							ModuleType.ModuleParticleEmitter em = ems.get(emi);
							if (!em.inside || !ship.showingOutside) {
								int x = ship.flipped ? ship.getWidth() - m.x : m.x;

								if (AGame.ANIM_R.nextDouble() < em.emitProbability * ms) {
									double px = x * AGame.SGS + ship.getIntX() + (ship.flipped ? -1 : 1) * em.x * AGame.SGS;
									double py = m.y * AGame.SGS + ship.getIntY() + em.y * AGame.SGS;
									for (int i = 0; i < em.numParticles; i++) {
										particles.add(new Particle(em.t, px, py));
									}
									if (em.soundEffect != null) {
										play(em.soundEffect, px, py, ship.getxSpeed(), ship.getySpeed(), side == viewingSide);
									}
								}
							}
						}
						if (m.type.getReload(ship.currentBonuses) > 0 && m.fired && !SimplePref.REDUCED_FLASHING.get() && m.type.muzzleFlash(ship.currentBonuses)) { // Bang!
							int totalDmg = m.type.getBlastDmg(ship.currentBonuses) + m.type.getPenDmg(ship.currentBonuses) + m.type.getDirectDmg(ship.currentBonuses);
							double scale = totalDmg / 20.0;
							Pt mz = m.currentMuzzle(totalDmg > 20 ? 1.5 : 1.1);
							int parts = StrictMath.min(3, totalDmg / 30 + (AGame.ANIM_R.nextInt(20) == 0 ? 1 : 0));
							for (int i = 0; i < parts; i++) {
								particles.add(new Particle(ParticleType.ofName("smoke"), mz.x, mz.y));
							}
							ParticleType pt = totalDmg > 20 ? ParticleType.ofName("muzzle") : ParticleType.ofName("muzzle_small");
							particles.add(new Particle(pt, mz.x, mz.y));
							if (pt == ParticleType.ofName("muzzle")) {
								double phase = AGame.ANIM_R.nextDouble() * StrictMath.PI * 2;
								for (int i = 0; i < 6; i++) {
									particles.add(new Particle(ParticleType.ofName("muzzle_chunk"), mz.x, mz.y,
											StrictMath.cos(m.weaponAngle + StrictMath.PI / 2) * StrictMath.sin(StrictMath.PI * 2 * i / 6 + phase) + AGame.ANIM_R.nextDouble() * 0.1 - 0.05,
											StrictMath.sin(m.weaponAngle + StrictMath.PI / 2) * StrictMath.sin(StrictMath.PI * 2 * i / 6 + phase) + AGame.ANIM_R.nextDouble() * 0.1 - 0.05,
											scale));
									int n = 3 + 1;
									particles.add(new Particle(ParticleType.ofName("muzzle_chunk"), mz.x, mz.y,
											StrictMath.cos(m.weaponAngle) * n, StrictMath.sin(m.weaponAngle) * n,
											scale * 3 / n));
								}
							}
						}
					}
				}}
			}
		}
		
		//#SpikeProfiler.endStart("moduleParticleEmitters", "capturing");
		
		for (Side side : sides) {
			ArrayList<Airship> ships = new ArrayList<Airship>(side.ships);
			for (Airship ship : ships) {
				if (ship.shouldSwitchSides() && ship.switchSides()) {
					exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyShipCaptured", otherSide(side), ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2, null, ship));
					exceptionalCombatEvents.add(new ExceptionalCombatEvent("myShipCaptured", side, ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2, null, ship));
					msSinceInterestingCombatEvent = 0;
					side.ships.remove(ship);
					otherSide(side).ships.add(ship);
					if (otherSide(side).isAllUsingAI()) {
						ship.setAI(new TacticalAI(ship, this, otherSide(side), side));
						ship.getAI().doesSurrender = false;
					} else {
						ship.setAI(null);
					}
				}
			}
		}
		
		//#SpikeProfiler.end("capturing");
		timeOfDayAge += ms;
		if (changeToTODTimeout > 0 && changeToTOD != null) {
			changeToTODTimeout -= ms;
			if (changeToTODTimeout <= 0) {
				timeOfDay = changeToTOD;
				changeToTOD = null;
			}
		} else if (!todHasChanged && timeOfDayAge > MIN_TIME_UNTIL_TOD_CHANGE && !timeOfDay.canTurnInto.isEmpty() && r.nextDouble() < TOD_CHANGE_CHANCE_PER_MS * ms) {
			timeOfDay = TimeOfDay.ofName(timeOfDay.canTurnInto.get(r.nextInt(timeOfDay.canTurnInto.size())));
			todHasChanged = true;
			exceptionalCombatEvents.add(new ExceptionalCombatEvent("newTimeOfDay " + timeOfDay.name, sides.get(0), 0, AGame.GROUND_LEVEL - 400, null, null));
			exceptionalCombatEvents.add(new ExceptionalCombatEvent("newTimeOfDay " + timeOfDay.name, sides.get(1), 0, AGame.GROUND_LEVEL - 400, null, null));
		}
		if (lightningChance > 0 && timeOfDayAge > 8000) {
			//#SpikeProfiler.start("lightning");
			lightningAmt -= ms;
			if (lightningAmt <= 0 && r.nextDouble() < lightningChance * ms) {
				ArrayList<Airship> victims = new ArrayList<Airship>();
				for (Side s : sides) { victims.addAll(s.ships); }
				for (int i = 0; i < victims.size(); i++) {
					if (victims.get(i).getY() > timeOfDay.effect.maxLightningY) {
						victims.remove(i);
						i--;
					}
				}
				int xPos = r.nextInt(combatAreaW()) - combatAreaW() / 2;
				Airship victim = null;
				double bestDist = 0;
				for (Airship ship : victims) {
					double sx = ship.getX() + ship.getBBWidth() / 2;
					double sy = ship.getY() + ship.getBBHeight() / 2;
					double ly = AGame.GROUND_LEVEL - 2500;
					double d = (sx - xPos) * (sx - xPos) + (sy - ly) * (sy - ly);
					if (victim == null || d < bestDist) {
						victim = ship;
						bestDist = d;
					}
				}
				if (victim != null) {
					int vx = r.nextInt(victim.getWidth());
					int vy = 0;
					while (victim.tileAt(vx, vy) == null) {
						vy++;
					}
					lightningPt = new Pt(victim.getX() + vx * AGame.SGS + AGame.SGS / 2, victim.getY() + vy * AGame.SGS + AGame.SGS / 2);
					lightningAmt = LIGHTING_AMT;
					Tile t = victim.tileAt(vx, vy);
					t.armour.hp = StrictMath.max(0, t.armour.hp - 20);
					t.module.doDamage(30);
					if (!SimplePref.REDUCED_FLASHING.get()) {
						for (double ly = lightningPt.y; ly > AGame.GROUND_LEVEL - 2500; ly -= 20 + AGame.ANIM_R.nextInt(20)) {
							particles.add(new Particle(ParticleType.ofName("lightning_flash"), lightningPt.x, ly));
						}
					}
					t.module.fire += Module.INITIAL_FIRE * 2;
					for (int dy = -1; dy < 2; dy++) { for (int dx = -1; dx < 2; dx++) {
						if (dx == 0 && dy == 0) { continue; }
						Tile t2 = victim.tileAt(vx + dx, vy + dy);
						if (t2 != null) {
							t2.armour.hp = StrictMath.max(0, t2.armour.hp - 20);
							t2.module.doDamage(30);
							t.module.fire += Module.INITIAL_FIRE;
						}
					}}
					play(MiscCombatSound.LIGHTNING, lightningPt.x, lightningPt.y, 0, 0, false);
				}
			}
			//#SpikeProfiler.end("lightning");
		}
		//#SpikeProfiler.end("combat");
		//String outN = //#SpikeProfiler.frameDone("combat", 45);
		/*if (outN != null && time > 3000) {
			try {
				FileUtils.write(new File(//#SpikeProfiler.outputDir, outN + ".json"), lastState.toString(4));
			} catch (Exception e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().exit(0);
		}*/
	}
	

	public Combat(AirshipGame g, JSONObject o) {
		if (o.has("backgroundFlavor")) {
			backgroundFlavor = CombatBackgroundFlavor.ofName(o.getString("backgroundFlavor").toLowerCase(Locale.ENGLISH));
		}
		if (o.has("currentCombatOutcome")) {
			currentCombatOutcome = new CombatOutcome(o.getJSONObject("currentCombatOutcome"));
		}
		combatName = o.optString("combatName", "Combat");
		hasInaccuracyMult = o.optBoolean("hasInaccuracyMult", true);
		finishedCountdown = o.optInt("finishedCountdown", 0);
		combatFinished = o.optBoolean("combatFinished", false);
		isRaid = o.optBoolean("isRaid", false);
		conquestID = o.optInt("conquestID", 0);
		lootAmount = o.optInt("lootAmount", 0);
		maxLootAmount = o.optInt("maxLootAmount", 0);
		randomSeed = o.optLong("randomSeed", AGame.ANIM_R.nextLong());
		instantCommandRegeneration = o.optBoolean("instantCommandRegeneration", false);
		allowDirectControl = o.optBoolean("allowDirectControl", true);
		combatID = o.optString("combatID", "combat");
		usedCheatCommand = o.optBoolean("usedCheatCommand", usedCheatCommand);
		changeToTODTimeout = o.optInt("changeToTODTimeout", 0);
		timeOfDayAge = o.optInt("timeOfDayAge", timeOfDayAge);
		canChangeTimeOfDay = o.optBoolean("canChangeTimeOfDay", false);
		todHasChanged = o.optBoolean("todHasChanged", false);
		if (o.has("changeToTOD")) {
			changeToTOD = TimeOfDay.ofName(o.getString("changeToTOD"));
		}
		if (o.has("setupLandscapeType")) {
			setupLandscapeType = LandscapeType.ofName(o.getString("setupLandscapeType"));
		}
		r = new GuardedRandom(randomSeed);
		JSONArray a = o.getJSONArray("sides");
		for (int i = 0; i < a.length(); i++) {
			sides.add(new Side(a.getJSONObject(i)));
		}
		a = o.getJSONArray("shots");
		for (int i = 0; i < a.length(); i++) {
			shots.add(new Shot(a.getJSONObject(i), this));
		}
		timeOfDay = TimeOfDay.ofName(o.optString("timeOfDay", "DAY"));
		HashMap<Integer, LandBlockType>[] mappingRef = new HashMap[1];
		mappingRef[0] = LandBlockType.getMapping(o);
		a = o.getJSONArray("landFormations");
		
		// Looping backwards so we get floaters first and can apply heuristic for LBT mapping.
		for (int i = a.length() - 1; i >= 0; i--) {
			landFormations.add(0, new LandFormation(a.getJSONObject(i), mappingRef));
		}
		
		physics = new Physics();
		physics.gravity = AGame.G;
		for (LandFormation lf : landFormations) {
			physics.bodies.add(lf);
		}
		for (Side s : sides) {
			for (Airship ship : s.ships) {
				if (!ship.removeMe(this)) {
					physics.bodies.add(ship);
					for (Module m : ship.modules) {
						for (Leg l : m.legs) {
							physics.bodies.add(l.foot);
						}
						for (Wheel w : m.wheels) {
							physics.bodies.add(w.body);
						}
					}
				}
				/*for (Module m : ship.modules) {
					for (Leg l : m.legs) {
						if (m.hp <= 0) {
							physics.bodies.remove(l.foot);
						} else if (!physics.bodies.contains(l.foot)) {
							physics.bodies.add(l.foot);
						}
					}
				}*/
			}
		}
		
		a = o.getJSONArray("sides");
		for (int i = 0; i < a.length(); i++) {
			sides.get(i).finish(a.getJSONObject(i), this);
		}
		
		if (o.has("crashZones")) {
			a = o.getJSONArray("crashZones");
			for (int i = 0; i < a.length(); i++) {
				crashZones.add(new CrashZone(a.getJSONObject(i).getDouble("cx"), a.getJSONObject(i).getInt("timeout"), a.getJSONObject(i).optBoolean("halfStrength", false)));
			}
		}
		this.g = g;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject o = new JSONObject();
		o.put("netVersion", Server.VERSION);
		o.put("combatName", combatName);
		o.put("time", time);
		o.put("timeOfDay", timeOfDay.name);
		o.put("finishedCountdown", finishedCountdown);
		o.put("combatFinished", combatFinished);
		o.put("isRaid", isRaid);
		o.put("conquestID", conquestID);
		o.put("lootAmount", lootAmount);
		o.put("maxLootAmount", maxLootAmount);
		o.put("randomSeed", randomSeed);
		o.put("hasInaccuracyMult", hasInaccuracyMult);
		o.put("instantCommandRegeneration", instantCommandRegeneration);
		o.put("allowDirectControl", allowDirectControl);
		o.put("combatID", combatID);
		o.put("usedCheatCommand", usedCheatCommand);
		o.put("changeToTODTimeout", changeToTODTimeout);
		o.put("timeOfDayAge", timeOfDayAge);
		o.put("todHasChanged", todHasChanged);
		o.put("canChangeTimeOfDay", canChangeTimeOfDay);
		if (setupLandscapeType != null) {
			o.put("setupLandscapeType", setupLandscapeType.name);
		}
		if (changeToTOD != null) {
			o.put("changeToTOD", changeToTOD.name);
		}
		if (currentCombatOutcome != null) {
			o.put("currentCombatOutcome", currentCombatOutcome.toJSON());
		}
		JSONArray a = new JSONArray();
		o.put("sides", a);
		for (Side s : sides) { a.put(s.toJSON(this)); }
		a = new JSONArray();
		o.put("shots", a);
		for (Shot s : shots) { a.put(s.toJSON(this)); }
		a = new JSONArray();
		o.put("crashZones", a);
		for (CrashZone cz : crashZones) {
			a.put(new JSONObject().put("cx", cz.cx).put("timeout", cz.timeout).put("halfStrength", cz.halfStrength));
		}
		a = new JSONArray();
		o.put("landFormations", a);
		for (LandFormation lf : landFormations) {
			a.put(lf.toJSON());
		}
		LandBlockType.writeMapping(o);
		return o;
	}
	
	public boolean canPlace(Airship ship, List<Airship> others, int x, int y, int spacing, Side side) {
		int w = ship.getWidth() * AGame.SGS;
		int h = ship.getHeight() * AGame.SGS;
		if (x + w > combatAreaW() / 2 || x < -combatAreaW() / 2) {
			return false;
		}
		if (sides.indexOf(side) == 0) {
			if (x + w > -EXCLUSION_ZONE_W / 2) { return false; }
		} else {
			if (x < EXCLUSION_ZONE_W / 2) { return false; }
		}
		int ceiling = AGame.GROUND_LEVEL - ship.serviceCeiling();
		if (!ship.type.onGround && y < ceiling) { return false; }
		for (Airship s2 : others) {
			int x2 = s2.getIntX();
			int y2 = s2.getIntY();
			int w2 = s2.getWidth() * AGame.SGS;
			int h2 = s2.getHeight() * AGame.SGS;
			if (x2 + w2 + spacing > x && x + w + spacing > x2 && y2 + h2 + spacing > y && y + h + spacing > y2) {
				return false;
			}
		}
		double ox = ship.getX();
		double oy = ship.getY();
		ship.setX(x);
		ship.setY(y);
		for (LandFormation lf : landFormations) {
			if (ship.overlapsWith(lf, /* ignoreSoftThings */ false)) {
				ship.setX(ox);
				ship.setY(oy);
				return false;
			}
		}
		ship.setX(ox);
		ship.setY(oy);
		return true;
	}
	
	public void initWheelsLegsTentaclesAndBarrels() {
		for (Side side : sides) {
			for (Airship s : side.ships) {
				s.initWheelsLegsAndTentacles(null, landFormations, this);
				s.resetWeaponBarrels();
			}
		}
	}
	
	public static enum RepairStatus {
		PARTIAL_RETREAT, PARTIAL_NO_CITY, PARTIAL_RAID, PARTIAL_NEST, FULL_ATTACKERS, FULL_DEFENDERS
	}
	
	public static class Side {
		public boolean lost() {
			if (surrendered || lostLocked) { return true; }
			int ssz = ships.size();
			for (int si = 0; si < ssz; si++) {
				Airship ship = ships.get(si);
				if (ship.outOfCombatMs < 2000) { return false; }
			}
			return true;
		}

		private String name;
		private boolean nameIsTranslationKey;
		public transient RepairStatus repairStatus;
		public transient boolean lostLocked;
		public CoatOfArms arms = CoatOfArms.getRandom(AGame.ANIM_R, HeraldicStyle.ofName("city"));
		public ArrayList<Airship> ships = new ArrayList<Airship>();
		public ArrayList<Airship> reserve = new ArrayList<Airship>();
		public ArrayList<Crewman> troops = new ArrayList<Crewman>();
		public transient ArrayList<Airship> originalComposition = new ArrayList<Airship>();
		public BonusSet bonuses = Tech.getStandardBonuses();
		public boolean surrendered = false;
		public boolean fledInSetup = false;
		public boolean usingAI = false;
		public AIQuality aiQuality = AIQuality.NORMAL;
		public transient boolean biggestShipDeclared = false;
		public transient HashSet<CrewType> outsideCrewWeaponFiredVsShip = new HashSet<CrewType>();
		public transient HashSet<CrewType> outsideCrewWeaponFiredVsCrew = new HashSet<CrewType>();
		public transient HashSet<CrewType> outsideCrewCrashing = new HashSet<CrewType>();
		public int smartCMIndex = 0;
		public HashMap<String, Integer> combatStats = new HashMap<String, Integer>();
		public HashMap<ModuleType, HashMap<String, Integer>> moduleCombatStats = new HashMap<ModuleType, HashMap<String, Integer>>();
		public int aircraftDownedByAircraft;
		public transient boolean shipGotCrushed;
		
		public ArrayList<Airship> getPossibleFastResolveSwitchers() {
			ArrayList<Airship> l = new ArrayList<Airship>();
			for (int i = 0; i < ships.size(); i++) {
				Airship s = ships.get(i);
				if (s.type.mobile && s.fastResolveBoardingResult() > 0.35) {
					l.add(s);
				}
			}
			return l;
		}
		
		public boolean isAllUsingAI() {
			if (usingAI) { return true; }
			for (int i = 0; i < ships.size(); i++) {
				if (ships.get(i).getAI() == null) {
					return false;
				}
			}
			return true;
		}
		
		public ArrayList<Airship> getAllShips() {
			ArrayList<Airship> allShips = new ArrayList<Airship>();
			allShips.addAll(ships);
			allShips.addAll(reserve);
			return allShips;
		}

		public Side(String name, boolean nameIsTranslationKey, BonusSet bonuses) {
			this.name = name;
			this.nameIsTranslationKey = nameIsTranslationKey;
			this.bonuses = bonuses;
		}
		
		public void aiTick() {
			for (Airship s : new ArrayList<Airship>(ships)) {
				if (s.getAI() != null) {
					s.getAI().tick();
				}
			}
		}
		
		public void tick(int ms, Combat c, boolean won, Physics ph, boolean onViewingSide) {
			boolean lost = lost();
			double fleetCommandBonus = 0;
			double fleetFireRateMult= 1; double fleetAccuracyMult= 1; double fleetCrewSpeedMult= 1; double fleetFlammabilityMult= 1; double fleetExplosionRiskMult= 1; double fleetCommandCooldownMult= 1; double fleetRepairAmountMult= 1; double fleetFirefightAmountMult= 1;
			ArrayList<Module> fleetCommandBonusModules = new ArrayList<Module>();
			for (Airship s : ships) {
				Hero cap = s.getCaptain();
				if (cap != null) {
					fleetFireRateMult = StrictMath.max(fleetFireRateMult, (100.0 + cap.type.fleetFireRatePercent) / 100.0);
					fleetAccuracyMult = StrictMath.max(fleetAccuracyMult, (100.0 + cap.type.fleetAccuracyPercent) / 100.0);
					fleetCrewSpeedMult = StrictMath.max(fleetCrewSpeedMult, (100.0 + cap.type.fleetCrewSpeedPercent) / 100.0);
					fleetFlammabilityMult = StrictMath.min(fleetFlammabilityMult, (100.0 + cap.type.fleetFlammabilityPercent) / 100.0);
					fleetExplosionRiskMult = StrictMath.min(fleetExplosionRiskMult, (100.0 + cap.type.fleetExplosionRiskPercent) / 100.0);
					fleetCommandCooldownMult = StrictMath.min(fleetCommandCooldownMult, (100.0 + cap.type.fleetCommandCooldownPercent) / 100.0);
					fleetRepairAmountMult = StrictMath.max(fleetRepairAmountMult, (100.0 + cap.type.fleetRepairAmountPercent) / 100.0);
					fleetFirefightAmountMult = StrictMath.max(fleetFirefightAmountMult, (100.0 + cap.type.fleetFirefightAmountPercent) / 100.0);
				}
				modules: for (int mi = 0; mi < s.modules.size(); mi++) {
					Module m = s.modules.get(mi);
					if (m.type.shipwideModifiers.get("fleetCommandBonus").value.get(s.currentBonuses) != 0) {
						double bonusAmt = (m.type.shipwideModifiers.get("fleetCommandBonus").value.get(s.currentBonuses) * m.staffProportion());
						
						for (int fcbmi = 0; fcbmi < fleetCommandBonusModules.size(); fcbmi++) {
							Module fcbm = fleetCommandBonusModules.get(fcbmi);
							if (m.type.shipwideModifiers.get("fleetCommandBonus").doesNotStackWith.contains(fcbm.type)) {
								int bomusAmt2 = (int) (fcbm.type.shipwideModifiers.get("fleetCommandBonus").value.get(fcbm.ship.currentBonuses) * fcbm.staffProportion());
								if (bonusAmt > bomusAmt2) {
									fleetCommandBonus += bonusAmt - bomusAmt2;
									fleetCommandBonusModules.set(fcbmi, m);
								}
								continue modules;
							}
						}
						
						fleetCommandBonusModules.add(m);
						fleetCommandBonus += bonusAmt;
					}
				}
			}
			for (Airship s : new ArrayList<Airship>(ships)) {
				//#SpikeProfiler.start("ship " + s.getName());
				if (s.tick(ms, c, won, lost, onViewingSide, fleetCommandBonus,
						fleetFireRateMult,
						fleetAccuracyMult,
						fleetCrewSpeedMult,
						fleetFlammabilityMult,
						fleetExplosionRiskMult,
						fleetCommandCooldownMult,
						fleetRepairAmountMult,
						fleetFirefightAmountMult))
				{
					ships.remove(s);
					ph.bodies.remove(s);
				}
				//#SpikeProfiler.end("ship " + s.getName());
			}
			//#SpikeProfiler.start("troops");
			smartCMIndex++;
			if (smartCMIndex > 10000) { smartCMIndex = 0; }
			int tsz = troops.size();
			for (int i = 0; i < tsz; i++) {
				troops.get(i).isSmart = smartCMIndex % tsz == i;
			}
			for (Crewman cm : new ArrayList<Crewman>(troops)) {
				if (cm.outsideTick(ms, c, this, onViewingSide)) { troops.remove(cm); }
			}
			//#SpikeProfiler.end("troops");
			if (!biggestShipDeclared) {
				ArrayList<Airship> s2 = new ArrayList<Airship>();
				for (Airship s : ships) {
					if (s.type.mobile) {
						s2.add(s);
					}
				}
				Collections.sort(s2, new Comparator<Airship>() {
					@Override
					public int compare(Airship t, Airship t1) {
						return t1.getCost() - t.getCost();
					}
				});
				if (s2.size() > 2 && s2.get(0).getCost() > 2 * s2.get(1).getCost()) {
					s2.get(0).biggestInFleet = true;
				}
				biggestShipDeclared = true;
			}
		}
		
		public void clearTargeting() {
			for (Airship s : ships) {
				s.clearTargeting();
			}
		}
		
		public void fullyRepairAllShipsNoForceResetCrew() {
			for (Airship s : ships) {
				s.repair(/* resetXP */ false, /* forceResetCrew */ false);
			}
			for (Airship s : reserve) {
				s.repair(/* resetXP */ false, /* forceResetCrew */ false);
			}
		}
		
		public Side(JSONObject o) {
			name = o.getString("name");
			nameIsTranslationKey = o.optBoolean("nameIsTranslationKey", false);
			surrendered = o.getBoolean("surrendered");
			fledInSetup = o.optBoolean("fledInSetup", false);
			arms = new CoatOfArms(o.getJSONObject("arms"));
			smartCMIndex = o.optInt("smartCMIndex", 0);
			JSONArray a = o.getJSONArray("ships");
			for (int i = 0; i < a.length(); i++) {
				ships.add(new Airship(a.getJSONObject(i)));
			}
			a = o.optJSONArray("reserve");
			if (a != null) {
				for (int i = 0; i < a.length(); i++) {
					reserve.add(new Airship(a.getJSONObject(i)));
				}
			}
			usingAI = o.optBoolean("usingAI", false);
			aiQuality = o.has("aiQuality") ? AIQuality.valueOf(o.getString("aiQuality")) : AIQuality.NORMAL;
			a = o.optJSONArray("troops");
			if (a != null) {
				for (int i = 0; i < a.length(); i++) {
					troops.add(new Crewman(a.getJSONObject(i), null, null));
				}
			}
			if (o.has("bonuses")) {
				bonuses = new BonusSet();
				a = o.getJSONArray("bonuses");
				for (int i = 0; i < a.length(); i++) {
					bonuses.add(Bonus.ofNameOrNone(a.getString(i)));
				}
			}
			if (o.has("combatStats")) {
				a = o.getJSONArray("combatStats");
				for (int i = 0; i < a.length(); i += 2) {
					combatStats.put(a.getString(i), a.getInt(i + 1));
				}
			}
			if (o.has("moduleCombatStats")) {
				a = o.getJSONArray("moduleCombatStats");
				for (int i = 0; i < a.length(); i += 2) {
					ModuleType mt = ModuleType.ofName(a.getString(i));
					JSONArray kv = a.getJSONArray(i + 1);
					HashMap<String, Integer> mtStats = new HashMap<String, Integer>();
					for (int j = 0; j < kv.length(); j += 2) {
						mtStats.put(kv.getString(j), kv.getInt(j + 1));
					}
					moduleCombatStats.put(mt, mtStats);
				}
			}
			aircraftDownedByAircraft = o.optInt("aircraftDownedByAircraft", 0);
		}
		
		public void finish(JSONObject o, Combat c) {
			JSONArray a = o.getJSONArray("ships");
			for (int i = 0; i < a.length(); i++) {
				ships.get(i).finishWithCombat(a.getJSONObject(i), c);
			}
			a = o.optJSONArray("reserve");
			if (a != null) {
				for (int i = 0; i < a.length(); i++) {
					reserve.get(i).finishWithCombat(a.getJSONObject(i), c);
				}
			}
			a = o.optJSONArray("troops");
			if (a != null) {
				for (int i = 0; i < a.length(); i++) {
					troops.get(i).finishLoadingWithCombat(a.getJSONObject(i), c);
				}
			}
		}
		
		public JSONObject toJSON(Combat c) {
			JSONObject o = new JSONObject()
					.put("name", name)
					.put("nameIsTranslationKey", nameIsTranslationKey)
					.put("surrendered", surrendered)
					.put("fledInSetup", fledInSetup)
					.put("usingAI", usingAI)
					.put("aiQuality", aiQuality.name())
					.put("hasLost", false)
					.put("smartCMIndex", smartCMIndex)
					.put("arms", arms.toJSON())
					.put("aircraftDownedByAircraft", aircraftDownedByAircraft);
			JSONArray a = new JSONArray();
			o.put("ships", a);
			for (Airship s : ships) { a.put(s.toJSON(c)); }
			a = new JSONArray();
			o.put("reserve", a);
			for (Airship s : reserve) { a.put(s.toJSON(c)); }
			a = new JSONArray();
			o.put("troops", a);
			for (Crewman cm : troops) { a.put(cm.toJSON(c)); }
			a = new JSONArray();
			o.put("bonuses", a);
			for (Bonus b : bonuses.list()) { a.put(b.name()); }
			a = new JSONArray();
			ArrayList<String> keys = new ArrayList<String>(combatStats.keySet());
			Collections.sort(keys);
			for (String k : keys) {
				a.put(k);
				a.put(combatStats.get(k));
			}
			o.put("combatStats", a);
			a = new JSONArray();
			ArrayList<ModuleType> mtKeys = new ArrayList<ModuleType>(moduleCombatStats.keySet());
			Collections.sort(mtKeys);
			for (ModuleType mt : mtKeys) {
				a.put(mt.name);
				JSONArray kv = new JSONArray();
				keys = new ArrayList<String>(moduleCombatStats.get(mt).keySet());
				Collections.sort(keys);
				for (String k : keys) {
					kv.put(k);
					kv.put(moduleCombatStats.get(mt).get(k));
				}
				a.put(kv);
			}
			o.put("moduleCombatStats", a);
			return o;
		}
		
		public int deaths() {
			int crewKilledTotal =
					getStat("crewKilledByModuleDestruction") +
					getStat("crewKilledByShot") +
					getStat("crewBurned") +
					getStat("crewFallen") +
					getStat("crewStomped") +
					getStat("crewDrivenOver") +
					getStat("crewDrowned");
			for (String k : getStatsStartingWith("crewKilledByCrew")) {
				crewKilledTotal += getStat(k);
			}
			for (String k : getStatsStartingWith("eatenBy")) {
				crewKilledTotal += getStat(k);
			}
			return crewKilledTotal;
		}
		
		public Airship getShip(String networkID) {
			for (Airship s : ships) {
				if (s.networkID.equals(networkID)) { return s; }
			}
			for (Airship s : reserve) {
				if (s.networkID.equals(networkID)) { return s; }
			}
			return null;
		}

		public int getCost() {
			int c = 0;
			int al = ships.size();
			for (int ai = 0; ai < al; ai++) {
				c += ships.get(ai).getCost();
			}
			return c;
		}
		
		public int getCostWithCaptains() {
			int c = 0;
			int al = ships.size();
			for (int ai = 0; ai < al; ai++) {
				c += ships.get(ai).getCost();
				if (ships.get(ai).getCaptain() != null) {
					c += ships.get(ai).getCaptain().type.singleCombatCost;
				}
			}
			return c;
		}
		
		public int getCachedDanger() {
			int d = 0;
			int al = ships.size();
			for (int ai = 0; ai < al; ai++) {
				d += ships.get(ai).dangerCache;
			}
			return d;
		}
		
		public void layoutShips(Combat c, boolean flipped) {
			ships.addAll(reserve);
			reserve.clear();
			for (Iterator<Airship> it = ships.iterator(); it.hasNext();) {
				Airship s = it.next();
				if (s.nonCombat()) {
					reserve.add(s);
					it.remove();
				} else {
					if (s.type.mobile) {
						s.setX(-10000);
						s.setY(-10000);
					}
					s.version++;
				}
			}
			// Try using the pre-existing placements
			ArrayList<Airship> prePlaced = new ArrayList<Airship>();
			LandFormation gnd = c.landFormations.get(0);
			lp: for (Airship s : ships) {
				if (!s.type.mobile) { continue; }
				if (s.lastPlaced == null) {
					continue;
				}
				
				int x = (int) s.lastPlaced.x;
				int y = (int) s.lastPlaced.y;
				if (flipped && x < EXCLUSION_ZONE_W / 2) { continue; }
				if (!flipped && x > -EXCLUSION_ZONE_W / 2) { continue; }
				if (s.type.onGround) {
					y = gnd.getVerticalPosition(s, x, s.lastPlacedFlipped, /* ignoreSoftThings*/ false) - (int) s.groundOffset();
					// Don't place landships into shallow water.
					if (gnd.landscapeType.hasWater && y + s.getBBHeight() > AGame.GROUND_LEVEL + AGame.SGS / 4) {
						continue;
					} 
					for (Airship s2 : ships) {
						if (s == s2) { continue; }
						if (s2.type.onGround && x + s.getBBWidth() >= s2.getX() && s2.getX() + s2.getBBWidth() >= x) {
							continue lp;
						}
					}
				}
				if (c.canPlace(s, ships, x, y, 1, this)) {
					s.setX(x);
					s.setY(y);
					s.setFlipped(s.lastPlacedFlipped, c);
					s.initWheelsLegsAndTentacles(null, c.landFormations, c);
					prePlaced.add(s);
				}
			}
			
			lp: for (Iterator<Airship> it = ships.iterator(); it.hasNext();) {
				Airship s = it.next();
				if (prePlaced.contains(s)) { continue; }
				s.version++;
				if (s.type.mobile) {
					s.setFlipped(flipped, c);
					s.flipTo = flipped;
					if (s.type.onGround) {
						xAttempts: for (int x = (flipped ? EXCLUSION_ZONE_W / 2 : -EXCLUSION_ZONE_W / 2 - s.getWidth() * AGame.SGS); (flipped ? x < c.combatAreaW() / 2 - OUTER_ZONE_W : x > -c.combatAreaW() / 2 + OUTER_ZONE_W); x += (flipped ? 80 : -80)) {
							if (s.type.onGround) {
								int y = gnd.getVerticalPosition(s, (int) x, flipped, /* ignoreSoftThings*/ false) - (int) s.groundOffset();
								// Don't place landships into shallow water.
								if (gnd.landscapeType.hasWater && y + s.getBBHeight() > AGame.GROUND_LEVEL + AGame.SGS / 4) {
									continue;
								} 
								for (Airship s2 : ships) {
									if (s == s2) { continue; }
									if (s2.type.onGround && x + s.getBBWidth() >= s2.getX() && s2.getX() + s2.getBBWidth() >= x) {
										continue xAttempts; // No landship stacking!
									}
								}
								if (c.canPlace(s, ships, x, y, 20, this)) {
									if (s.lastPlaced == null) {
										s.lastPlaced = new Pt(x, y);
										s.lastPlacedFlipped = s.flipped;
									}
									s.setX(x);
									s.setY(y);
									s.initWheelsLegsAndTentacles(null, c.landFormations, c);
									continue lp;
								}
							}
						}
					} else {
						for (int y = AGame.GROUND_LEVEL - COMBAT_AREA_H; y < AGame.GROUND_LEVEL; y += 40) {
							for (int x = (flipped ? EXCLUSION_ZONE_W / 2 : -EXCLUSION_ZONE_W / 2 - s.getWidth() * AGame.SGS); (flipped ? x < c.combatAreaW() / 2 - OUTER_ZONE_W : x > -c.combatAreaW() / 2 + OUTER_ZONE_W); x += (flipped ? 80 : -80)) {
								if (c.canPlace(s, ships, x, y, 20, this)) {
									if (s.lastPlaced == null) {
										s.lastPlaced = new Pt(x, y);
										s.lastPlacedFlipped = s.flipped;
									}
									s.setX(x);
									s.setY(y);
									s.initWheelsLegsAndTentacles(null, c.landFormations, c);
									continue lp;
								}
							}
						}
					}
					reserve.add(s);
					it.remove();
				}
			}
		}

		private void checkForCombatEvents(Side otherSide, Combat c, boolean won, boolean lost, Physics physics) {
			if (won) {
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("victory", this, 0, AGame.GROUND_LEVEL - 300, null, null));
			}
			if (lost) {
				c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("defeat", this, 0, AGame.GROUND_LEVEL - 300, null, null));
			}
			if (c.timeOfDay.effect.fogClr != null & c.time > 2000) {
				int nMyShipsAboveFog = 0;
				int nMyShips = 0;
				for (int i = 0; i < ships.size(); i++) {
					Airship s = ships.get(i);
					if (s.type == ShipType.AIRSHIP) {
						nMyShips++;
						if (s.getY() + s.getBBHeight() * 0.75 < AGame.GROUND_LEVEL - c.timeOfDay.effect.fogLevel) {
							nMyShipsAboveFog++;
						}
					}
				}
				int nEnemiesBelowFog = 0;
				for (int i = 0; i < otherSide.ships.size(); i++) {
					Airship s = otherSide.ships.get(i);
					if (s.getY() > AGame.GROUND_LEVEL - c.timeOfDay.effect.fogLevel) {
						nEnemiesBelowFog++;
					}
				}
				if (nMyShipsAboveFog > nMyShips / 2 && nEnemiesBelowFog > otherSide.ships.size() / 2) {
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyBelowFog", this, 0, AGame.GROUND_LEVEL - c.timeOfDay.effect.fogLevel / 2, null, null));
				}
			}
			if (c.timeOfDay.effect.shootToLeftJitterMult > 1 && c.time > 3000) {
				int nEnemiesLookingIntoSun = 0;
				for (int i = 0; i < otherSide.ships.size(); i++) {
					Airship s = otherSide.ships.get(i);
					if (s.type != ShipType.BUILDING && s.flipped) {
						nEnemiesLookingIntoSun++;
					}
				}
				if (nEnemiesLookingIntoSun > otherSide.ships.size() / 2) {
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyLookingIntoSun", this, 0, AGame.GROUND_LEVEL - 400, null, null));
				}
			}
			if (c.timeOfDay.effect.shootToRightJitterMult > 1 && c.time > 3000) {
				int nEnemiesLookingIntoSun = 0;
				for (int i = 0; i < otherSide.ships.size(); i++) {
					Airship s = otherSide.ships.get(i);
					if (s.type != ShipType.BUILDING && !s.flipped) {
						nEnemiesLookingIntoSun++;
					}
				}
				if (nEnemiesLookingIntoSun > otherSide.ships.size() / 2) {
					c.exceptionalCombatEvents.add(new ExceptionalCombatEvent("enemyLookingIntoSun", this, 0, AGame.GROUND_LEVEL - 400, null, null));
				}
			}
		}

		public String getName() {
			return nameIsTranslationKey ? _t(name) : name;
		}

		public void setName(String name, boolean nameIsTranslationKey) {
			this.name = name;
			this.nameIsTranslationKey = nameIsTranslationKey;
		}
		
		private void initStats() {
			for (Airship ship : originalComposition) {
				ship.setStat("crew", ship.crew.size());
				ship.setStat("ammo", ship.getTotalResource(Resource.AMMO));
				ship.setStat("coal", ship.getTotalResource(Resource.COAL));
				ship.setStat("water", ship.getTotalResource(Resource.WATER));
				ship.setStat("repair", ship.getTotalResource(Resource.REPAIR));
				changeStat("crew", ship.crew.size());
				changeStat("ammo", ship.getTotalResource(Resource.AMMO));
				changeStat("coal", ship.getTotalResource(Resource.COAL));
				changeStat("water", ship.getTotalResource(Resource.WATER));
				changeStat("repair", ship.getTotalResource(Resource.REPAIR));
			}
		}

		public ArrayList<String> getStatsStartingWith(String s) {
			ArrayList<String> l = new ArrayList<String>();
			for (String k : combatStats.keySet()) {
				if (k.startsWith(s)) {
					l.add(k);
				}
			}
			Collections.sort(l);
			return l;
		}

		public void setStat(String key, int value) {
			combatStats.put(key, value);
		}

		public int getStat(String key) {
			return combatStats.containsKey(key) ? combatStats.get(key) : 0;
		}

		public void changeStat(String key, int value) {
			combatStats.put(key, getStat(key) + value);
		}

		public void incStat(String key) {
			changeStat(key, 1);
		}

		public void setModuleStat(ModuleType mt, String key, int value) {
			mt = mt.getSymmetryGroupHead();
			if (!moduleCombatStats.containsKey(mt)) {
				moduleCombatStats.put(mt, new HashMap<String, Integer>());
			}
			moduleCombatStats.get(mt).put(key, value);
		}
		
		public ArrayList<ModuleType> getStatModules() {
			ArrayList<ModuleType> mts = new ArrayList<ModuleType>(moduleCombatStats.keySet());
			Collections.sort(mts);
			return mts;
		}

		public int getModuleStat(ModuleType mt, String key) {
			return moduleCombatStats.containsKey(mt) && moduleCombatStats.get(mt).containsKey(key) ? moduleCombatStats.get(mt).get(key) : 0;
		}

		public void changeModuleStat(ModuleType mt, String key, int value) {
			setModuleStat(mt, key, getModuleStat(mt, key) + value);
		}

		public void incModuleStat(ModuleType mt, String key) {
			changeModuleStat(mt, key, 1);
		}
	}
	
	public void incStat(Airship ship, String k) {
		Side s = sideOf(ship);
		if (s != null) {
			s.incStat(k);
		}
	}
	
	public void incModuleStat(Airship ship, ModuleType mt, String k) {
		Side s = sideOf(ship);
		if (s != null) {
			s.incModuleStat(mt, k);
		}
	}
	
	public void incStat(Crewman cm, String k) {
		Side s = sideOf(cm);
		if (s != null) {
			s.incStat(k);
		}
	}
	
	public void changeStat(Airship ship, String k, int value) {
		Side s = sideOf(ship);
		if (s != null) {
			s.changeStat(k, value);
		}
	}
	
	public void changeModuleStat(Airship ship, ModuleType mt, String k, int value) {
		Side s = sideOf(ship);
		if (s != null) {
			s.changeModuleStat(mt, k, value);
		}
	}
	
	public Combat clone(AirshipGame g) {
		return new Combat(g, toJSON());
	}
	
	// Abilities
	public void doAbility(Airship ship, CombatAbility ab, Airship target) {
		double shipX = ship.getX() + ship.getBBWidth() / 2;
		double shipY = ship.getY() + ship.getBBHeight() / 2;
		double targetX = target == null ? 0 : (target.getX() + target.getBBWidth() / 2);
		double targetY = target == null ? 0 : (target.getY() + target.getBBHeight() / 2);
		if (ship.usedAbilities.contains(ab)) { return; }
		if (ship.getCaptain() == null || !ship.getCaptain().type.combatAbilities.contains(ab)) { return; }
		boolean used = true;
		
		double evtX = target == null ? shipX : targetX;
		double evtY = target == null ? shipY : targetY;
		Airship withinShip = target == null ? ship : null;
		
		Side mySide = sideOf(ship);
		Side enemySide = otherSide(mySide);
		if (enemySide == null) { return; }
		
		exceptionalCombatEvents.add(new ExceptionalCombatEvent("cast " + ab.name() + " " + ship.getCaptain().type.name, mySide, evtX, evtY, null, withinShip));
		exceptionalCombatEvents.add(new ExceptionalCombatEvent("received " + ab.name(), enemySide, evtX, evtY, null, null));
		
		switch (ab) {
			case SMOKESCREEN:
				ship.smokescreenTime = Airship.SMOKESCREEN_TIME;
				play("smokescreen", shipX, shipY, 3);
				break;
			case IMPROVISE_MUNITIONS:
				ship.improviseResource(Resource.AMMO, this);
				play("scavenge-ammo", shipX, shipY, 3);
				break;
			case SCAVENGE_FUEL:
				ship.improviseResource(Resource.COAL, this);
				play("scavenge-coal", shipX, shipY, 3);
				break;
			case SCAVENGE_MATERIALS:
				ship.improviseResource(Resource.REPAIR, this);
				play("scavenge-tools", shipX, shipY, 3);
				break;
			case EXTINGUISH:
				ship.extinguish(this);
				play("quench2", shipX, shipY, 3);
				break;
			case NECROMANTIC_INCANTATION:
				particles.add(new Particle(ParticleType.ofName("magic1"), ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2));
				ship.resurrect(this);
				play("resurrect", shipX, shipY, 3);
				break;
			case ENGINEERING_MIRACLE:
				used = ship.engineeringMiracle(this);
				if (used) { play("miracle-repair", shipX, shipY, 3); }
				break;
			case LAST_STAND:
				ship.holdOnTime = Airship.HOLD_ON_TIME;
				for (Module m : ship.modules) {
					m.startHoldingOn();
				}
				for (Crewman m : ship.crew) {
					m.startHoldingOn();
				}
				play("crowdshout", shipX, shipY, 3);
				break;
			case BURST_OF_SPEED:
				ship.burstOfSpeedTime = Airship.BURST_OF_SPEED_TIME;
				play("spin_up", shipX, shipY, 3);
				break;
			case SUPERCHARGE_SUSPENDIUM:
				ship.superchargeSuspendiumTime = Airship.SUPERCHARGE_SUSPENDIUM_TIME;
				play("supercharge-suspendium", shipX, shipY, 3);
				break;
			case DOUBLE_TIME:
				ship.doubleTimeTime = Airship.DOUBLE_TIME_TIME;
				play("double-time", shipX, shipY, 3);
				break;
			case FEAR:
				particles.add(new Particle(ParticleType.ofName("magic4"), ship.getX() + ship.getBBWidth() / 2, ship.getY() + ship.getBBHeight() / 2));
				ship.fearTime = Airship.FEAR_TIME;
				play("fear", shipX, shipY, 3);
				break;
			case BLINDING_GLIMMER:
				particles.add(new Particle(ParticleType.ofName("magic4"), target.getX() + target.getBBWidth() / 2, target.getY() + target.getBBHeight() / 2));
				target.glimmerTime = Airship.GLIMMER_TIME;
				play("magic2", shipX, shipY, 3);
				play("glimmer", targetX, targetY, 3);
				break;
			case HYSTERICAL_BLINDNESS:
				for (Airship s : enemySide.ships) {
					particles.add(new Particle(ParticleType.ofName("magic4"), s.getX() + s.getBBWidth() / 2, s.getY() + s.getBBHeight() / 2));
					s.blindnessTime = Airship.GLIMMER_TIME;
				}
				play("magic2", shipX, shipY, 3);
				play("glimmer", 0, 0, 5);
				break;
			case CRIPPLING_SHOT:
				target.cripplingShotTarget = true;
				target.disarmingShotTarget = false;
				Side cs = sideOf(target);
				if (cs != null) { otherSide(cs).clearTargeting(); }
				play("target", shipX, shipY, 3);
				break;
			case DISARMING_SHOT:
				target.disarmingShotTarget = true;
				target.cripplingShotTarget = false;
				Side ds = sideOf(target);
				if (ds != null) { otherSide(ds).clearTargeting(); }
				play("target", shipX, shipY, 3);
				break;
			case TAUNT:
				ship.tauntTime = Airship.TAUNT_TIME;
				Side ts = sideOf(ship);
				if (ts != null) { ts.clearTargeting(); }
				play("taunt", shipX, shipY, 2);
				break;
			case CROSSWINDS:
				particles.add(new Particle(ParticleType.ofName("magic2"), target.getX() + target.getBBWidth() / 2, target.getY() + target.getBBHeight() / 2));
				target.crosswindsTime = Airship.CROSSWINDS_TIME;
				play("magic2", shipX, shipY, 3);
				play("gust-of-wind", targetX, targetY, 3);
				break;
			case PARALYSIS:
				particles.add(new Particle(ParticleType.ofName("magic3"), target.getX() + target.getBBWidth() / 2, target.getY() + target.getBBHeight() / 2));
				target.paralysisTime = Airship.PARALYSIS_TIME;
				play("magic2", shipX, shipY, 3);
				play("stun", targetX, targetY, 3);
				break;
			case MOMENT_OF_DOUBT:
				for (Airship s : enemySide.ships) {
					particles.add(new Particle(ParticleType.ofName("magic3"), s.getX() + s.getBBWidth() / 2, s.getY() + s.getBBHeight() / 2));
					s.momentOfDoubtTime = Airship.MOMENT_OF_DOUBT_TIME;
				}
				play("magic2", shipX, shipY, 3);
				play("stun", 0, 0, 5);
				break;
			case TURNABOUT:
				turnabout(target);
				play("magic2", shipX, shipY, 3);
				play("gust-of-wind", targetX, targetY, 3);
				break;
			case GUST_OF_WIND:
				particles.add(new Particle(ParticleType.ofName("magic2"), target.getX() + target.getBBWidth() / 2, target.getY() + target.getBBHeight() / 2));
				target.gustOfWindTime = Airship.GUST_OF_WIND_TIME;
				double dist = StrictMath.sqrt((shipX - targetX) * (shipX - targetX) + (shipY - targetY) * (shipY - targetY));
				target.gustOfWindDX = (targetX - shipX) / dist;
				target.gustOfWindDY = (targetY - shipY) / dist;
				play("magic2", shipX, shipY, 3);
				play("gust-of-wind", targetX, targetY, 3);
				break;
			case SUDDEN_STORM:
				for (Airship s : enemySide.ships) {
					particles.add(new Particle(ParticleType.ofName("magic2"), s.getX() + s.getBBWidth() / 2, s.getY() + s.getBBHeight() / 2));
					s.suddenStormTime = Airship.SUDDEN_STORM_TIME;
					s.suddenStormDX = sides.indexOf(enemySide) == 0 ? -1 : 1;
				}
				play("magic2", shipX, shipY, 5);
				play("gust-of-wind", 0, 0, 5);
				break;
			case AERIAL_ACE:
				spawnFlyers(ship, ship.getCaptain().type.aerialAceCrewType, 1);
				break;
			case AIR_SUPPORT:
				spawnFlyers(ship, ship.getCaptain().type.airSupportCrewType, ship.getCaptain().type.airSupportNumCrew);
				break;
			case PERSONAL_GUARD:
				ship.addCaptainsGuard(this);
				play("drumroll", shipX, shipY, 3);
				break;
			case EMERGENCY_ORDERS:
				for (Airship s : mySide.ships) {
					s.commandPoints = s.commandPointsRequired();
				}
				play("whoosh", shipX, shipY, 1);
				break;
			case EARTHQUAKE:
				earthquake();
				break;
			case HIGH_STORM:
				changeToTODTimeout = HIGH_STORM_TIMEOUT;
				changeToTOD = timeOfDay;
				timeOfDayAge = 0;
				if ("snow".equals(timeOfDay.effect.landscapeVisualVariant)) {
					timeOfDay = TimeOfDay.ofName("MASSIVE_SNOWSTORM");
				} else {
					timeOfDay = TimeOfDay.ofName("MASSIVE_STORM");
				}
				break;
		}
		if (used) {
			ship.usedAbilities.add(ab);
		}
	}
	
	private void earthquake() {
		play("earthquake", 0, AGame.GROUND_LEVEL, 9);
		//play("magic2", 0, AGame.GROUND_LEVEL, 2);
		for (Side side : sides) {
			for (Airship ship : side.ships) {
				if (ship.type == ShipType.BUILDING) {
					ship.doEarthquake(30 + ship.getHeightToBaseRatio() * 50, this);
				} else if (ship.type == ShipType.LANDSHIP) {
					ship.doEarthquake(80, this);
				}
			}
		}
		LandFormation ground = landFormations.get(0);
		ParticleType pem = ParticleType.ofName("earthquakeMagic");
		ParticleType pe = ParticleType.ofName("earthquake");
		ground.shakeAmount = 12;
		for (int gx = 0; gx < ground.grid[0].length; gx++) {
			double y = ground.yBoundaryAt(ground.getX() + gx * AGame.SGS + AGame.SGS / 2);
			if (AGame.ANIM_R.nextInt(5) == 1) {
				particles.add(new Particle(pem, ground.getX() + gx * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS, y));
			}
			if (AGame.ANIM_R.nextInt(3) == 1) {
				particles.add(new Particle(pe, ground.getX() + gx * AGame.SGS + AGame.ANIM_R.nextDouble() * AGame.SGS, y));
			}
		}
	}
	
	private void spawnFlyers(Airship ship, CrewType ct, int quantity) {
		for (int i = 0; i < quantity; i++) {
			int x = sides.indexOf(sideOf(ship)) == 0 ? -combatAreaW() / 2 - 200 - i * 50 : combatAreaW() / 2 + 200 + i * 50;
			int y = -i * 50;
			Crewman ace = new Crewman(null, null, ct);
			ace.setX(x);
			ace.setY(y);
			ace.leaveOnEmptyAmmo = true;
			sideOf(ship).troops.add(ace);
			if (i == 0 && ct.launchSnd != null) {
				play(ct.launchSnd, x, y, 0, 0, 10, true);
			}
		}
	}
	
	private void turnabout(Airship target) {
		particles.add(new Particle(ParticleType.ofName("magic2"), target.getX() + target.getBBWidth() / 2, target.getY() + target.getBBHeight() / 2));
		target.setFlipped(!target.flipped, this);
		target.flipTo = target.flipped;
		target.moveTo = new Pt(target.getX(), target.getY());
		target.commandPoints = 0;
		ParticleType ltrWind = ParticleType.ofName(target.flipped ? "rtl_wind" : "ltr_wind");
		ParticleType rtlWind = ParticleType.ofName(target.flipped ? "ltr_wind" : "rtl_wind");
		ParticleType ltrWind2 = ParticleType.ofName(target.flipped ? "rtl_wind_2" : "ltr_wind_2");
		ParticleType rtlWind2 = ParticleType.ofName(target.flipped ? "ltr_wind_2" : "rtl_wind_2");
		for (int gy = 0; gy < target.getHeight(); gy++) {
			for (int gx = 0; gx < target.getWidth(); gx++) {
				if (target.tileAt(gx, gy) != null) {
					for (int i = 0; i < 4; i++) {
						particles.add(new Particle(ltrWind, target.getX() + target.gridXToWorldX(gx, 1) * AGame.SGS + AGame.SGS / 2, target.getY() + gy * AGame.SGS + AGame.SGS / 2));
					}
					for (int i = 0; i < 4; i++) {
						particles.add(new Particle(ltrWind2, target.getX() + target.gridXToWorldX(gx, 1) * AGame.SGS + AGame.SGS / 2, target.getY() + gy * AGame.SGS + AGame.SGS / 2));
					}
					break;
				}
			}
			for (int gx = target.getWidth() - 1; gx >= 0; gx--) {
				if (target.tileAt(gx, gy) != null) {
					for (int i = 0; i < 4; i++) {
						particles.add(new Particle(rtlWind, target.getX() + target.gridXToWorldX(gx, 1) * AGame.SGS + AGame.SGS / 2, target.getY() + gy * AGame.SGS + AGame.SGS / 2));
					}
					for (int i = 0; i < 4; i++) {
						particles.add(new Particle(rtlWind2, target.getX() + target.gridXToWorldX(gx, 1) * AGame.SGS + AGame.SGS / 2, target.getY() + gy * AGame.SGS + AGame.SGS / 2));
					}
					break;
				}
			}
		}
	}
	
	private void sinkhole(double cx) {
		LandFormation ground = landFormations.get(0);
		particles.add(new Particle(ParticleType.ofName("magic1"), cx, ground.getY()));
		play("landslide", cx, ground.getY() + ground.getBBHeight() / 2, 3);
		for (int gx = 0; gx < ground.grid[0].length; gx++) {
			double tileCenterX = ground.getX() + gx * AGame.SGS + AGame.SGS / 2;
			if (tileCenterX >= cx - SinkholeTool.SINKHOLE_RANGE && tileCenterX <= cx + SinkholeTool.SINKHOLE_RANGE) {
				int dist = (int) StrictMath.abs(tileCenterX - cx);
				int undepth = 3 + dist / 24;
				for (int gy = 0; gy < ground.grid.length - undepth; gy++) {
					LandBlockType lbt = ground.grid[gy][gx];
					ground.hp[gy][gx] = 0;
					ground.destroy[gy][gx] = true;
					ParticleType pt = lbt.destroyparticle;
					if (pt != null) {
						for (int i = 0; i < 7; i++) {
							particles.add(new Particle(pt,
								ground.getX() + gx * AGame.SGS + AGame.SGS / 2,
								ground.getY() + gy * AGame.SGS + AGame.SGS / 2));
						}
					}
				}
			}
		}
	}
	
	public static final int CRASH_ZONE_W = 640;
	public static final int CRASH_ZONE_TIMEOUT = 60000;
	public static final double CRASH_ZONE_LIFT_MULT = 0.1;
	public static final double CRASH_ZONE_HALF_STRENGTH_LIFT_MULT = 0.35;

	public static class CrashZone {
		public final double cx;
		public final boolean halfStrength;
		public int timeout;
		public CrashZone(double cx, int timeout, boolean halfStrength) {
			this.cx = cx;
			this.timeout = timeout;
			this.halfStrength = halfStrength;
		}
	}
	
	/* 0: no, 1: half-strength, 2: full strength */
	public int inCrashZone(double x) {
		int max = 0;
		for (int i = 0; i < crashZones.size(); i++) {
			CrashZone cz = crashZones.get(i);
			if (x >= cz.cx - CRASH_ZONE_W / 2 && x <= cz.cx + CRASH_ZONE_W / 2) {
				if (cz.halfStrength) {
					max = 1;
				} else {
					return 2;
				}
			}
		}
		return max;
	}
	
	private void addCrashZone(double cx) {
		boolean halfStrength = false;
		LandFormation gnd = landFormations.get(0);
		if (gnd.landscapeType.deepWater) {
			halfStrength = true;
		} else if (gnd.landscapeType.hasWater) {
			int numDeepWaters = 0;
			for (double xx = cx - CRASH_ZONE_W / 2; xx <= cx + CRASH_ZONE_W / 2; xx += AGame.SGS) {
				if (gnd.yBoundaryAt(xx) >= AGame.GROUND_LEVEL + AGame.SGS * 2) {
					numDeepWaters++;
					if (numDeepWaters >= 3) {
						halfStrength = true;
						break;
					}
				}
			}
		}
		crashZones.add(new CrashZone(cx, CRASH_ZONE_TIMEOUT, halfStrength));
	}
}
