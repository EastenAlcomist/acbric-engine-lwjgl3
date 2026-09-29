package com.zarkonnen.airships;

import com.zarkonnen.catengine.lwjgl3.GLCompat;
import com.zarkonnen.airships.CampaignWorld.Speed;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.MyDraw.State;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hook.Type;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.Rect;
import com.zarkonnen.catengine.util.ScreenMode;
import com.zarkonnen.catengine.util.Utils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import static com.zarkonnen.airships.Client.msg;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.MultiplayerChat.COLLAPSE;
import static com.zarkonnen.airships.MultiplayerChat.EXPAND;
import com.zarkonnen.airships.Relationship.Ultimatum;
import com.zarkonnen.airships.ShapeUtils.TrianglesArea;
import com.zarkonnen.catengine.Fount;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Random;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glBegin;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor3f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glColor4f;
import static com.zarkonnen.catengine.lwjgl3.GLCompat.glEnd;
import static com.zarkonnen.airships.MultiplayerChat.COLLAPSE;
import static com.zarkonnen.airships.MultiplayerChat.EXPAND;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.util.Iterator;
import java.util.LinkedList;
import org.newdawn.slick.opengl.TextureImpl;

public strictfp class StrategicScreen implements Screen, FileScreen.BackgroundTask, CanMoveAround, HeroRenderer.HeroSelectionCallback {
	public static final Clr ROT = Clr.fromHex("74595233");
	public static final Clr PARCHMENT = Clr.fromHex("e7e0a9");
	public static final Color PARCHMENT_COLOR = new Color(PARCHMENT.r, PARCHMENT.g, PARCHMENT.b);
	public static final Clr PARCHMENT_OVER = Clr.fromHex("e7e0a966");
	public static final Clr INK_TINT = Clr.fromHex("745952").mix(0.5, PARCHMENT);
	public static final Clr INK_PARCHMENT_25 = Clr.fromHex("7d673e").mix(0.25, PARCHMENT);
	public static final Color INK_TINT_COLOR = new Color(INK_TINT.r, INK_TINT.g, INK_TINT.b);
	public static final Clr INK = Clr.fromHex("7d673e");
	public static final Color INK_C = new Color(INK.r, INK.g, INK.b);
	public static final Clr DARK_INK = Clr.fromHex("7d673e");
	public static final Clr DARKER_INK = Clr.fromHex("7d673e");
	public static final Color DARKER_INK_C = new Color(DARKER_INK.r, DARKER_INK.g, DARKER_INK.b);
	public static final Clr STRONG_RED_INK = Clr.fromHex("7d673e").mix(0.5, Clr.RED);
	public static final Clr PLAGUE = Clr.fromHex("87a060");
	public static final String PLAGUE_TINT = "[87a060]";
	public static final Clr RED_INK = Clr.fromHex("7d673e").mix(0.7, Clr.RED);
	public static final Clr DARK_RED_INK = Clr.fromHex("7d673e").mix(0.25, Clr.RED); public static final String DARK_RED_INK_TINT = DARK_RED_INK.toString();
	public static final Clr DARKISH_RED_INK = Clr.fromHex("7d673e").mix(0.5, Clr.RED);
	public static final Color DARKISH_RED_INK_C = new Color(DARKISH_RED_INK.r, DARKISH_RED_INK.g, DARKISH_RED_INK.b);
	public static final Clr DARK_GREEN_INK = Clr.fromHex("7d673e").mix(0.15, Clr.GREEN); public static final String DARK_GREEN_INK_TINT = DARK_GREEN_INK.toString();
	public static final int MAP_TILE_SIZE = StrictMath.max(16, 256 / WorldMap.SCALE_FACTOR);
	public static final Clr BACKDROP_DARKENING = new Clr(216, 216, 216);
	public static final Clr OCEAN = PARCHMENT.mix(0.2, new Clr(30, 70, 190));
	public static final Clr GOLD = new Clr(255, 255, 145);
	private final Img indicator = new Img("ui", 208, 512, 16, 16, false);
	
	public static final Clr MILD_UNREST = Clr.fromHex("6a6968");
	public static final Color MILD_UNREST_COLOR = new Color(MILD_UNREST.r, MILD_UNREST.g, MILD_UNREST.b);
	public static final Clr SOME_UNREST = Clr.fromHex("726534");
	public static final Color SOME_UNREST_COLOR = new Color(SOME_UNREST.r, SOME_UNREST.g, SOME_UNREST.b);
	public static final Clr MORE_UNREST = Clr.fromHex("bb421d");
	public static final Color MORE_UNREST_COLOR = new Color(MORE_UNREST.r, MORE_UNREST.g, MORE_UNREST.b);
	private final Img unrestIcon = new Img("ui", 112, 368, 16, 16, false);
	private final Img plagueIcon = new Img("ui", 144, 368, 16, 16, false);
	private final Img econDamageIcon = new Img("ui", 32, 416, 16, 16, false);
	private final Img disconnectedIcon = new Img("ui", 560 + 16, 592, 16, 16, false);
	private final Img ritualSiteIcon = new Img("ui", 528, 432, 16, 16, false);
	private final Img noEraIcon = new Img("ui", 192, 368, 16, 16, false);
	
	private final Img editAirship = new Img("ui", 16, 496, 16, 16, false);
	private final Img editLandship = new Img("ui", 32, 496, 16, 16, false);
	private final Img editBuilding = new Img("ui", 96, 496, 16, 16, false);
	
	/*private final Img[] cities = new Img[10];
	private final Img[] towns = new Img[6];
	private final Img[] cities_bg = new Img[10];
	private final Img[] towns_bg = new Img[6];*/
	// qqDPS These should be images!
	private final Img airfleet = new Img("ui", 10 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img airfleetOutline = new Img("ui", 12 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img landfleet = new Img("ui", 14 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img landfleetOutline = new Img("ui", 16 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img mixedfleet = new Img("ui", 18 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img mixedfleetOutline = new Img("ui", 20 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img convoy = new Img("ui", 22 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img convoyOutline = new Img("ui", 24 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img escortedConvoy = new Img("ui", 26 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img escortedConvoyOutline = new Img("ui", 28 * 16, 30 * 16, 2 * 16, 2 * 16, false);
	private final Img victoryUnderlay = new Img("ui", 0 * 16, 32 * 16, 2 * 16, 2 * 16, false);
	private final Img defeatOverlay = new Img("ui", 2 * 16, 32 * 16, 2 * 16, 2 * 16, false);
	private final Img fight = new Img("ui", 4 * 16, 32 * 16, 16, 16, false);
	private final Img rearm = new Img("ui", 176, 416, 16, 16, false);
	private final Img newUpgrade = new Img("ui", 496, 384, 16, 16, false);
	private final Img plague = new Img("ui", 144, 368, 16, 16, false);
	private final Img strike = new Img("ui", 112, 368, 16, 16, false);
	private final Img econRecovery = new Img("ui", 16, 384, 16, 16, false);
	private final Img revolt = new Img("ui", 240, 384, 16, 16, false);
	private final Img shipLoss = new Img("ui", 5 * 16, 32 * 16, 16, 16, false);
	public final Img mapGoto = new Img("ui", 0, 416, 16, 16, false);
	private final Img invader = new Img("ui", 16, 496, 16, 16, false);
	private final Img expedition = new Img("ui", 640, 384, 16, 16, false);
	private final Img letter = new Img("ui", 352, 512, 16, 16, false);
	private final Img tick = new Img("ui", 352 + 16, 512, 16, 16, false);
	private final Img cross = new Img("ui", 144, 416, 16, 16, false);
	private final Img ultimatum = new Img("ui", 192, 368, 16, 16, false);
	private final Img fight_big = new Img("ui", 272, 288, 64, 64, false);
	private final Img eye = new Img("ui", 208, 384, 16, 16, false);
	private final Img researchIcon = new Img("ui", 560, 384, 16, 16, false);
	private final Img help = new Img("ui", 512, 448, 16, 16, false);
	public final Img details = new Img("ui", 384, 512, 16, 16, false);
	public final Img heroVictoryIcon = new Img("heroes", 0, 48, 16, 16, false);
	final Img edit = new Img("ui", 26 * 16, 26 * 16, 16, 16, false);
	final Img refit = new Img("ui", 224, 384, 16, 16, false);
	final Img repair = new Img("ui", 48, 416, 16, 16, false);
	final Img instabuild = new Img("ui", 144, 512, 16, 16, false);
	final Img scrap = new Img("ui", 432, 416, 16, 16, false);
	final Img scuttle = new Img("ui", 144, 416, 16, 16, false);
	final Img gift = new Img("ui", 528, 448, 16, 16, false);
	final Img compact = new Img("ui", 240, 400, 16, 16, false);
	final Img select = new Img("ui", 112, 400, 16, 16, false);
	final Img anchor = new Img("ui", 480, 384, 16, 16, false);
	final Img up = new Img("ui", 208, 512, 16, 16, false);
	final Img down = new Img("ui", 192, 512, 16, 16, false);
	//final Img unAnchor = new Img("ui", 480, 388, 16, 12, false);
	private final Img emptyNest = new Img("ui", 416, 464, 16, 16, false);
	private final Img emptyNestBG = new Img("ui", 432, 464, 16, 16, false);
	private final Img largeEmptyNest = new Img("ui", 464, 256, 32, 32, false);
	private final Img largeEmptyNestBG = new Img("ui", 496, 256, 32, 32, false);
	private final Img coronationIcon = new Img("ui", 128, 384, 16, 16, false);
	public static final double TOO_CLOSE = 1;
	public static final int AUTOSAVE_INTERVAL_MS = 60 * 1000;
	public static final int MIN_ZOOM_LEVEL = 4;
	public static final int MAX_ZOOM_LEVEL = 32;
	final Img canAttackButNotReturn = new Img("ui", 288, 592, 16, 16, false);
	final Img canAttackAndReturn = new Img("ui", 288 + 16, 592, 16, 16, false);
	final Img cannotAttack = new Img("ui", 288 + 32, 592, 16, 16, false);
	final Img canMove = new Img("ui", 288 + 48, 592, 16, 16, false);
	final Img cannotMove = new Img("ui", 288 + 64, 592, 16, 16, false);
	final Img limpHome = new Img("ui", 288 + 64 + 16, 592, 16, 16, false);
	final Img canBesiege = new Img("ui", 384, 592, 16, 16, false);
	final Img askAlliesToBesiege = new Img("ui", 496, 592, 16, 16, false);
	public static final int DOCKED_FLEET_DX = -32;
	public static final int FLEET_IMG_SZ = 32;
	public static ArrayList<Img> GENTLE_WAVES = new ArrayList<Img>();
	public static ArrayList<Img> STRONG_WAVES = new ArrayList<Img>();
	public static ArrayList<Img> SMALL_CITIES = new ArrayList<Img>();
	public static ArrayList<Img> SMALL_CITIES_BGS = new ArrayList<Img>();
	public static ArrayList<Img> SMALL_TOWNS = new ArrayList<Img>();
	public static ArrayList<Img> SMALL_TOWNS_BGS = new ArrayList<Img>();
	public static ArrayList<Img> BIG_CITIES = new ArrayList<Img>();
	public static ArrayList<Img> BIG_CITIES_BGS = new ArrayList<Img>();
	public static ArrayList<Img> BIG_TOWNS = new ArrayList<Img>();
	public static ArrayList<Img> BIG_TOWNS_BGS = new ArrayList<Img>();
	
	public int techScreenScrollX = MyDraw.SIDE_CLEARANCE, techScreenScrollY = MyDraw.UI_SPACING + MyDraw.TOP_BAR_H;
	
	public boolean debug = AGame.isDebug();

	// The world
	public AirshipGame g;
	public final CampaignWorld w;
	public CombatOutcome combatOutcome;
	public boolean combatConfirmed = false;
	public UniScreen combatUniScreen = null;
	public UniScreen returnTo = null;
	
	// Time
	public int time = 0;
	public int inputTick = 0;
	private CampaignWorld.Speed lastNonStopSpeed = CampaignWorld.Speed.NORMAL;
	private long lastAutosaved = System.currentTimeMillis();
	
	// Space
	public double scrollX = 0, scrollY = 0;
	public double adjScrollX = 0, adjScrollY = 0;
	public double zoom = 8;
	private boolean dragging = false;
	private double dragStartX, dragStartY;
	private double dragStartScrollX, dragStartScrollY;
	public Pt currentCursor = null;
	
	// Screens and dialogs
	boolean menu;
	public boolean showEmpireDetails = false;
	public EmpireDetailsMode empireDetailsMode = EmpireDetailsMode.BUDGET;
	public ScrollBar empireBonusesSB = new ScrollBar();
	public IntRect empireBonusesSBR = new IntRect();
	public ScrollBar settingsSB = new ScrollBar();
	public IntRect settingsSBR = new IntRect();
	public MonsterNest selectedNest = null;
	private MonsterNest upgradeStartedNest = null;
	private MonsterNest upgradeNearingCompletionNest = null;
	private MonsterNest upgradeCompleteNest = null;
	public ShipNameDialog shipNameDialog = null;
	Airship askForScrap = null;
	Airship askForScuttle = null;
	ArrayList<String> disconnectedPlayers = new ArrayList<String>();
	private int fleetListWidth = 0;
	boolean checkLeave;
	City renameCity;
	TextField renameCityField = new TextField(false, AGame.FILE_SAFE_ALPHABET, AGame.MAX_NAME_LENGTH);
	int savedIndicator = 0;
	CampaignStatsDisplay statsDisplay = new CampaignStatsDisplay();
	boolean showStats = false;
	MapLocation travelToDialogDestination;
	boolean travelToDialogBesiege;
	Fleet travelToDialogTarget;	
	public StrategicMapMode mapMode = StrategicMapMode.NORMAL;
	public Empire mapModePOV = null;
	double prevZoom = 0;
	int msSinceZoomChanged = 0;
	public MedalEditor medalEditor = null;
	public ScrollBar medalsSB = new ScrollBar();
	public IntRect medalsSBR = new IntRect();
	public Airship awardMedalTo;
	public ArrayList<Integer> awardMedalToLevels;
	public ShipsLedger shipsLedger = new ShipsLedger(this);
	public CityLedger cityLedger = new CityLedger(this);
	
	// Debug
	boolean debugShowEraMenu;
	boolean showCheatsMenu = false;
	boolean showSwitchEmpireMenu = false;
	boolean showSwitchDifficultyMenu = false;
	boolean showStatsDuringGame = false;
	boolean showStartIncidentMenu = false;
	boolean switchIncidentRoles = false;
	boolean showGetHeroMenu = false;
	
	public transient boolean waitForOtherPlayers;
	public transient int timeWaitedForOtherPlayers;
	public static final int EXTRA_WAIT_TIME = 30000;
	
	public Img ad = AprilAds.getShortImg();
	
	// Diplomacy
	public boolean showDiplomacy;
	public final DiplomacyWindow diplomacy = new DiplomacyWindow(this);
	private transient String diplomacyFailureNotice;
	private transient int diplomacyFailureNoticeTimeout;
	public static final int DIPLOMACY_NOTICE_TIMEOUT = 10000;
	private transient boolean addDiplomacyOffersToList = true;
	private final ScrollBar conquerorSB = new ScrollBar();
	private final IntRect conquerorSBR = new IntRect();
	public OfferEditor selectCityForOfferEditor = null;
	public boolean showCityForDiplomacy;
	public LinkedList<NotificationSound> notificationSounds = new LinkedList<NotificationSound>();
	public int timeUntilNextNotificationSound = 0;
	
	// Heroes
	public boolean showHeroes;
	public Hero heroToShow = null;
	public Airship shipForHero = null;
	public City cityForHero = null;
	public ScrollBar heroesSB = new ScrollBar();
	public final Img leaveIcon = new Img("heroes", 0, 0, 16, 16, false);
	public final Img dieIcon = new Img("heroes", 32, 0, 16, 16, false);
	public final Img evolveIcon = new Img("heroes", 16, 0, 16, 16, false);
	public final Img leaveIconBg = new Img("heroes", 0, 16, 16, 16, false);
	public final Img dieIconBg = new Img("heroes", 32, 16, 16, 16, false);
	public final Img evolveIconBg = new Img("heroes", 16, 16, 16, 16, false);
	public final Img heroIcon = new Img("heroes", 48, 0, 16, 16, false);
	public final Img addHeroIcon = new Img("heroes", 48, 16, 16, 16, false);
	public final Img removeHeroIcon = new Img("heroes", 48, 32, 16, 16, false);
	public final Img disloyalIcon = new Img("heroes", 48, 80, 16, 16, false);
	public final Img disloyalIconBg = new Img("heroes", 64, 80, 16, 16, false);
	private transient IncidentType.Event eventAnnounced;
	private transient String incidentNoticeAnnounced;
	
	private enum EmpireDetailsMode {
		BUDGET("Budget", "B"),
		SHIPS("Ships", "S"),
		CITIES("Cities", "C"),
		MEDALS("Medals", "M") {
			@Override
			public boolean available(CampaignWorld w) { return EHeroes.it.enabled && w.map.heroFrequency.frequencyMultiplier != 0; }
		},
		BONUSES("Bonuses", "O"),
		SETTINGS("Settings", "T"),;
		public final String name;
		public final String shortcut;

		private EmpireDetailsMode(String name, String shortcut) {
			this.name = name;
			this.shortcut = shortcut;
		}
		
		public boolean available(CampaignWorld w) { return true; }
	}
	public transient boolean editSettings;

	// Hover
	private City hoverCity;
	private City nextHoverCity;
	private Fleet hoverFleet;
	private Fleet nextHoverFleet;
		
	// Select
	public City menuCity;
	private final ScrollBar cityScrollBar = new ScrollBar();
	private final ScrollBar takeoverScrollBar = new ScrollBar();
	private final CityDetail cityDetail = new CityDetail(this);
	private int cityDetailWidth = 0;
	Fleet selectedFleet;
	private ArrayList<Airship> _fleetSelection = new ArrayList<Airship>();
	private final ScrollBar fleetScrollBar = new ScrollBar();
	private MapLocation highlitTravelConnection;
	private boolean highlitTravelConnectionBesiege;
	private MapLocation nextMoveToHover, moveToHover;
	private int highlitTravelConnectionTime = 10;
	
	// Misc
	public StrategicHelp.HelpMsg helpMsg = null;
	public ArrayList<Backdrop> backdrops = new ArrayList<Backdrop>();
	//private transient EnumMap<ShipType, UniScreen> mpEditors = new EnumMap<ShipType, UniScreen>(ShipType.class);
	public transient boolean showTooSlowMessage = false, tooSlowMessageShown = false;
	public transient boolean mpVictoryOrDefeatReported = false;
	public transient Pair<String, Img> endOfGameMessage;
	//public transient boolean victoryTimeReported = false;
	
	// Chat
	private final ScrollBar chatScrollBar = new ScrollBar();
	public final TextField chatField = new TextField(AGame.MAX_CHAT_LENGTH);
	public boolean chatCollapsed = false;
	public boolean chatForAllies = false;
	private final ChatAdapter<GenericChatMsg> chatAdapter = new ChatAdapter<GenericChatMsg>();
	private final Img allyChatToggle = new Img("ui", 496, 592, 16, 16, false);
	
	// File load stuff
	Pair<JSONObject, InPipe> gameToLoad;
	IODirectory autosaveIODir;
	IODirectory saveIODir;
	ModReloadProgressDialog mrpd;
	
	private static final BackdropType[] STAINS = {
		new BackdropType(new Img("stains", 10, 11, 279, 167, false), false),
		new BackdropType(new Img("stains", 324, 0, 205, 202, false), false),
		new BackdropType(new Img("stains", 0, 200, 318, 321, false), false),
		new BackdropType(new Img("stains", 337, 220, 292, 290, false), false),
		new BackdropType(new Img("stains", 60, 577, 96, 99, false), false),
		new BackdropType(new Img("stains", 255, 594, 102, 88, false), false),
		new BackdropType(new Img("stains", 465, 528, 224, 215, false), false),
		new BackdropType(new Img("stains", 22, 697, 204, 209, false), false),
		new BackdropType(new Img("stains", 797, 192, 124, 114, false), false)
	};
	
	private static final BackdropType[] BACKDROPS = {
		new BackdropType(new Img("p_dove.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_wyrm.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_report.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_spy.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_listing.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_spider.jpg", 0, 0, 843, 1024, false), true, true),
		new BackdropType(new Img("p_dragon.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_torpedo.jpg", 0, 0, 843, 566, false), true),
		new BackdropType(new Img("p_guardian.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_cultists.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_kraken.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("p_jelly.jpg", 0, 0, 843, 1024, false), true),
		new BackdropType(new Img("notes.jpg", 114, 4, 292, 73, false), true),
		new BackdropType(new Img("notes.jpg", 396, 4, 153, 73, false), true),
		new BackdropType(new Img("notes.jpg", 530, 2, 152, 70, false), true),
		new BackdropType(new Img("notes.jpg", 86, 79, 215, 59, false), true),
		new BackdropType(new Img("notes.jpg", 425, 79, 121, 61, false), true),
		new BackdropType(new Img("notes.jpg", 540, 82, 116, 54, false), true),
		new BackdropType(new Img("notes.jpg", 329, 149, 116, 54, false), true),
		new BackdropType(new Img("notes.jpg", 438, 150, 116, 54, false), true),
		new BackdropType(new Img("notes.jpg", 118, 151, 225, 54, false), true),
		new BackdropType(new Img("notes.jpg", 80, 223, 225, 54, false), true),
		new BackdropType(new Img("notes.jpg", 280, 221, 225, 54, false), true),
		new BackdropType(new Img("notes.jpg", 250, 291, 265, 57, false), true),
		new BackdropType(new Img("notes.jpg", 127, 352, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 429, 350, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 124, 412, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 100, 663, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 112, 728, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 101, 796, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 382, 805, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 384, 724, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 394, 667, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 115, 469, 288, 96, false), true),
		new BackdropType(new Img("notes.jpg", 371, 488, 288, 57, false), true),
		new BackdropType(new Img("notes.jpg", 507, 571, 155, 41, false), true),
		new BackdropType(new Img("notes.jpg", 104, 286, 155, 41, false), true),
		new BackdropType(new Img("notes.jpg", 561, 408, 155, 83, false), true),
		new BackdropType(new Img("notes.jpg", 411, 404, 155, 83, false), true),
		new BackdropType(new Img("notes.jpg", 123, 575, 177, 83, false), true),
		new BackdropType(new Img("notes.jpg", 339, 575, 177, 83, false), true)
	};
	
	private static strictfp class BackdropType {
		public final Img img;
		public final boolean shadow, spider;

		public BackdropType(Img img, boolean shadow) {
			this.img = img;
			this.shadow = shadow;
			this.spider = false;
		}
		
		public BackdropType(Img img, boolean shadow, boolean spider) {
			this.img = img;
			this.shadow = shadow;
			this.spider = spider;
		}
	}
	
	private static strictfp class Backdrop {
		BackdropType type;
		int x, y;
		double rot;

		public Backdrop(BackdropType type, int x, int y, double rot) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.rot = rot;
		}
		
	}

	public StrategicScreen(AirshipGame g, CampaignWorld w) {
		this.g = g;
		this.w = w;
		
		if (w != null && w.map.autosaveIODir != null) {
			autosaveIODir = w.map.autosaveIODir;
		}

		chatField.focus = false;
		chatScrollBar.stickToBottom = true;
		chatScrollBar.offset = 100000;
		
		/*for (int i = 0; i < 10; i++) {
			cities[i] = new Img("ui", i * 16, 480, 16, 16, false);
			cities_bg[i] = new Img("ui", i * 16, 448, 16, 16, false);
		}
		for (int i = 0; i < 6; i++) {
			towns[i] = new Img("ui", 160 + i * 16, 448, 16, 16, false);
			towns_bg[i] = new Img("ui", 256 + i * 16, 448, 16, 16, false);
		}*/
		
		if (menuCity == null && w != null && w.player != null) {
			menuCity = w.player.cities.get(0);
		}
		updateBackgrounds();
		ConquestHelpItem.resetAll();
		if (w != null) {
			ConquestHelpItem.setDone(w.doneConquestHelpItems);
		}
	}
	
	public void diplomacyCallback(String messageID, String invalidReason) {
		if (!messageID.equals(diplomacy.waitForMessageArrival)) {
			return;
		}
		diplomacy.waitForMessageArrival = null;
		if (invalidReason != null) {
			if (showDiplomacy) {
				diplomacy.actionFailed = invalidReason;
			} else {
				diplomacyFailureNotice = _t("diplomaticActionFailed") + ": " + _t(invalidReason);
				diplomacyFailureNoticeTimeout = 0;
			}
		}
	}
	
	void surrender() {
		if (w.player == null) { return; }
		w.giveCommand(msg("surrenderStrategic").put("empire", w.player.id));
		spectate();
	}
	
	void spectate() {
		w.player = null;
		selectedFleet = null;
		selectedNest = null;
		menuCity = null;
	}
	
	void checkedLeave() {
		if (w.isHost()) {
			checkLeave = true;
		} else {
			leave();
		}
	}
	
	void leave() {
		if (g.s != this) { return; }
		g.doLowMemoryCheck();
		if (w != null && w.isMultiplayer()) {
			if (g.lanClient != null) {
				g.lanClient.close();
				g.lanClient = null;
				g.s = new MainMenu(g, MainMenu.Submenu.MULTIPLAYER);
			} else {
				g.s = new MetaLobbyScreen(g, null);
				g.sendMessage(msg("changeChannel").put("id", 0));
				g.helloSent = false;
			}
			if (w.mpServer != null) {
				w.mpServer.close();
				w.mpServer = null;
				g.lanServer = null;
			}
		} else {
			g.s = new MainMenu(g, (w != null && w.isMultiplayer()) ? MainMenu.Submenu.MULTIPLAYER : MainMenu.Submenu.CONQUEST);
		}
	}
	
	void updateBackgrounds() {
		if (w == null) { return; }
		backdrops.clear();
		int nBackdrops = w.map.empires.size() / 2 + 5;
		ArrayList<BackdropType> stains = new ArrayList<BackdropType>(Arrays.asList(STAINS));
		Collections.shuffle(stains, AGame.ANIM_R.getRandom());
		ArrayList<BackdropType> backs = new ArrayList<BackdropType>(Arrays.asList(BACKDROPS));
		Collections.shuffle(backs, AGame.ANIM_R.getRandom());
		ArrayList<BackdropType> cands = new ArrayList<BackdropType>();
		cands.addAll(stains.subList(0, StrictMath.min(nBackdrops / 2, stains.size())));
		cands.addAll(backs);
		for (int i = 0; i < nBackdrops; i++) {
			BackdropType c = cands.get(i % cands.size());
			switch (AGame.ANIM_R.nextInt(4)) {
				case 0:
					backdrops.add(new Backdrop(c, AGame.ANIM_R.nextInt(w.map.water[0].length + c.img.srcWidth / WorldMap.SCALE_FACTOR / 2) - c.img.srcWidth / WorldMap.SCALE_FACTOR / 2, -AGame.ANIM_R.nextInt(c.img.srcHeight / WorldMap.SCALE_FACTOR / 4 + 1) - c.img.srcHeight / WorldMap.SCALE_FACTOR / 4, -0.2 + 0.4 * AGame.ANIM_R.nextDouble()));
					break;
				case 1:
					backdrops.add(new Backdrop(c, AGame.ANIM_R.nextInt(w.map.water[0].length + c.img.srcWidth / WorldMap.SCALE_FACTOR / 2) - c.img.srcWidth / WorldMap.SCALE_FACTOR / 2, w.map.water.length - AGame.ANIM_R.nextInt(c.img.srcHeight / WorldMap.SCALE_FACTOR / 4 + 1), -0.2 + 0.4 * AGame.ANIM_R.nextDouble()));
					break;
				case 2:
					backdrops.add(new Backdrop(c, -AGame.ANIM_R.nextInt(c.img.srcWidth / WorldMap.SCALE_FACTOR / 4 + 1) - c.img.srcWidth / WorldMap.SCALE_FACTOR / 4, AGame.ANIM_R.nextInt(w.map.water.length + c.img.srcHeight / WorldMap.SCALE_FACTOR / 2) - c.img.srcHeight / WorldMap.SCALE_FACTOR / 2, -0.2 + 0.4 * AGame.ANIM_R.nextDouble()));
					break;
				case 3:
					backdrops.add(new Backdrop(c, w.map.water[0].length - AGame.ANIM_R.nextInt(c.img.srcWidth / WorldMap.SCALE_FACTOR / 4 + 1), AGame.ANIM_R.nextInt(w.map.water.length + c.img.srcHeight / WorldMap.SCALE_FACTOR / 2) - c.img.srcHeight / WorldMap.SCALE_FACTOR / 2, -0.2 + 0.4 * AGame.ANIM_R.nextDouble()));
					break;
			}
		}
	}
	
	public double screenToWorldX(double x) { return (x / zoom) - adjScrollX; }
	public double screenToWorldY(double y) { return (y / zoom) - adjScrollY; }
	public double worldToScreenX(double x) { return (x + adjScrollX) * zoom; }
	public double worldToScreenY(double y) { return (y + adjScrollY) * zoom; }

	public void postCombat() {
		combatOutcome = w.postCombat(/* uncontested */ false);
	}
	
	public ArrayList<Airship> getFleetSelection() {
		if (selectedFleet == null) {
			_fleetSelection.clear();
		} else {
			_fleetSelection.retainAll(selectedFleet.getAllShips());
		}
		return _fleetSelection;
	}

	public void setFleetSelection(ArrayList<Airship> fleetSelection) {
		this._fleetSelection = fleetSelection;
	}
	
	private boolean canRenameCity() {
		if (renameCity == null || renameCityField.getText().isEmpty()) { return false; }
		for (Empire e : w.map.empires) {
			for (City c : e.cities) {
				if (c != renameCity && c.name.equals(renameCityField.getText())) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean modalUp() {
		if (w.player == null) { return false; }
		for (City c : w.player.cities) {
			if (c.takeoverNeeded) { return true; }
		}
		if (w.player.conquerorInfo != null) { return true; }
		for (Fleet fl : w.player.getFleets()) {
			if (fl.fleeDestinationNeeded) {
				if (selectedFleet == null) {
					selectedFleet = fl;
					setFleetSelection(new ArrayList<Airship>(fl.actives));
					getFleetSelection().addAll(fl.reserve);
				}
				return true;
			}
		}
		return false;
	}
	
	public void doGift(Airship ship, Empire to) {
		if (w.player == null) { return; }
		w.giveCommand(w.shipMsg("giftShip", ship).put("from", w.player.id).put("to", to.id));
	}
	
	private void doScrap() {
		if (w.player == null) { return; }
		w.giveCommand(w.shipMsg("scrapShip", askForScrap).put("empire", w.player.id));
		askForScrap = null;
		if (selectedFleet != null && selectedFleet.actives.isEmpty() && selectedFleet.reserve.isEmpty()) {
			selectedFleet = null;
		}
	}
	
	private void doScuttle() {
		if (w.player == null) { return; }
		w.giveCommand(w.shipMsg("scuttleShip", askForScuttle).put("empire", w.player.id));
		askForScuttle = null;
		if (selectedFleet != null && selectedFleet.actives.isEmpty() && selectedFleet.reserve.isEmpty()) {
			selectedFleet = null;
		}
	}
	
	private DiplomacyNotice declarationOfWar() {
		if (w.player == null) { return null; }
		for (DiplomacyNotice notice : w.player.diplomacyNotices) {
			if (notice.type == DiplomacyNotice.Type.WAR_DECLARED || notice.type == DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR || notice.type == DiplomacyNotice.Type.ALLY_DECLARED_WAR || notice.type == DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR) {
				return notice;
			}
		}
		return null;
	}
	
	private boolean hasDeclarationsOfWar() {
		return declarationOfWar() != null;
	}
	
	private void acknowledgeDeclarationOfWar() {
		if (w.player == null) { return; }
		for (DiplomacyNotice notice : w.player.diplomacyNotices) {
			if (notice.type == DiplomacyNotice.Type.WAR_DECLARED || notice.type == DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR || notice.type == DiplomacyNotice.Type.ALLY_DECLARED_WAR || notice.type == DiplomacyNotice.Type.ENEMY_ALLIANCE_CAUSED_WAR) {
				w.player.diplomacyNotices.remove(notice);
				return;
			}
		}
	}
	
	private boolean showAutosaveNotice() {
		long t = System.currentTimeMillis();
		return t >= lastAutosaved + AUTOSAVE_INTERVAL_MS - 1000 && !w.defeat() && !w.victory() && w.combatInfo == null;
	}
	
	public static IODirectory autosave(IODirectory autosaveIODir, CampaignWorld w) {
		// Don't autosave if we're in a fight, because the versioning will be wrong.
		if (w.combatInfo != null) {
			System.out.println("skipping autosave");
			return autosaveIODir;
		}
		try {
			System.err.println("autosave");
			if (autosaveIODir == null || !autosaveIODir.saveID.equals(w.map.worldID)) {
				String autosaveName = "Autosave.json";
				if (AGame.isDebug()) {
					if (w.player != null) {
						autosaveName = "Autosave-" + w.player.name + ".json";
					} else {
						autosaveName = "Autosave-Spectator-" + AirshipGame.instance.playerID() + ".json";
					}
				}
				File f = new File(new File(AGame.getGameDirectory(), "saves"), autosaveName); //qqDPS
				f.mkdirs();
				autosaveIODir = new IODirectory(f, w.map.worldID);
				System.err.println("new autosave");
			}
			/*if (w.player != null && !w.player.getFleets().isEmpty()) { // Intentionally break saves.
				w.player.getFleets().get(0).actives.get(0).setName("ham-" + System.currentTimeMillis());
			}*/
			long t = System.currentTimeMillis();
			final JSONObject o = w.toJSON(autosaveIODir);
			autosaveIODir.registerWithoutVersion(new OutPipe.Writer() {
				@Override
				public JSONObject write(String writeID) {
					return o;
				}
			}, "world");
			autosaveIODir.write();
			System.err.println("autosave took " + (System.currentTimeMillis() - t) + " ms");
		} catch (OutOfMemoryError oome) {
			AirshipGame.instance.reportError("AutosaveOOME", new RuntimeException(oome), null, false, true);
		} catch (Exception e) {
			AirshipGame.instance.reportError(_t("Unable_to_save_game"), e, null, false);
		}
		return autosaveIODir;
	}
	
	private void autosaveIfNeeded() {
		long t = System.currentTimeMillis();
		if (t >= lastAutosaved + AUTOSAVE_INTERVAL_MS && !w.defeat() && !w.victory()) {
			lastAutosaved = t;
			autosaveIODir = autosave(autosaveIODir, w);
		}
	}
	
	public void emergencySave(String reason) {
		autosaveIODir = autosave(autosaveIODir, w);
	}
	
	@Override
	public boolean backgroundTick(int ms) {
		if (w == null) { return false; }
		if (w.isMultiplayer()) {
			if (w.tick(ms, this)) { return true; }
			if (w.desyncPubliclyDetected()) {
				g.s = new ResumeScreen(g, w, this, g.s, g.playerID(), false, 0, w.gameName);
				return true;
			}
			if (baseTick(ms)) {
				return true;
			}
			if (w.combatInfo != null && combatUniScreen != null) {
				int mySideIndex = w.combatInfo.playerCombatSideIndex(w);
				if (mySideIndex != -1 && w.multiplayerCampaignCombatIntent != null) {
					if (g.s instanceof UniScreen && ((UniScreen) g.s).intent instanceof StrategicEditShipIntent) {
						returnTo = (UniScreen) g.s;
					}
					g.s = combatUniScreen;
					combatUniScreen.forciblyPulled = true;
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean showNotice() {
		if (w == null) { return false; }
		return w.isMultiplayer() || (w.player != null && !w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains) && ConquestHelpSystem.hasAny(this, g.s));
	}
	
	private boolean showActionableNotice() {
		if (w == null) { return false; }
		return ((w.combatInfo != null && combatUniScreen != null) || (combatOutcome != null && (combatOutcome.type == CombatOutcome.CombatOutcomeType.UNCONTESTED_VICTORY || combatOutcome.type == CombatOutcome.CombatOutcomeType.UNCONTESTED_DEFEAT)));
	}
	
	private String noticeInfoString() {
		if (w == null) { return ""; }
		if (w.combatInfo != null && combatUniScreen != null) {
			boolean spectate = w.combatInfo.playerCombatSideIndex(w) == -1;
			if (spectate) {
				if (w.combatInfo.defendingLoc != null) {
					return _t("Combat_between_x_and_y_at_z", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale), Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale), w.combatInfo.defendingLoc.getDisplayName());
				} else if (w.combatInfo.nearLoc != null) {
					return _t("Combat_between_x_and_y_near_z", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale), Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale), w.combatInfo.nearLoc.getDisplayName());
				} else {
					return _t("Combat_between_x_and_y", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale), Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale));
				}
			} else {
				if (w.combatInfo.attackers(w.map).contains(w.player)) {
					if (w.combatInfo.defendingLoc != null) {
						return _t("You_are_attacking_x_at_y", Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale), w.combatInfo.defendingLoc.getDisplayName());
					} else if (w.combatInfo.nearLoc != null) {
						return _t("You_are_fighting_x_near_y", Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale), w.combatInfo.nearLoc.getDisplayName());
					} else {
						return _t("You_are_intercepting_x", Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale));
					}
				} else {
					if (w.combatInfo.defendingLoc != null) {
						return _t("You_are_being_attacked_by_x_at_y", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale), w.combatInfo.defendingLoc.getDisplayName());
					} else if (w.combatInfo.nearLoc != null) {
						return _t("You_are_fighting_x_near_y", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale), w.combatInfo.nearLoc.getDisplayName());
					} else {
						return _t("You_are_being_intercepted_by_x", Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale));
					}
				}
			}
		}
		if (combatOutcome != null) {
			if (combatOutcome.loc == null) {
				return _t("outcome_vs_y", combatOutcome.type.getName(), combatOutcome.opponentName);
			} else {
				return _t("outcome_vs_y_at_z", combatOutcome.type.getName(), combatOutcome.opponentName, combatOutcome.loc.getDisplayName());
			}
		}
		return "???";
	}
	
	private String noticeButtonText() {
		if (w == null) { return ""; }
		if (w.combatInfo != null && combatUniScreen != null) {
			boolean spectate = w.combatInfo.playerCombatSideIndex(w) == -1;
			if (spectate) {
				return _t("Spectate");
			} else {
				if (w.multiplayerCampaignCombatSetupIntent != null) {
					return _t("Setup") + " (" + w.multiplayerCampaignCombatSetupIntent.countdownLeft / 1000 + ")";
				} else {
					return _t("Combat");
				}
			}
		} else {
			return _t("do_view");
		}
	}
	
	private void doNoticeButton() {
		if (w == null) { return; }
		menu = false;
		if (w.combatInfo != null && combatUniScreen != null) {
			g.s = combatUniScreen;
		} else if (combatOutcome != null) {
			g.s = this;
			scrollX = -combatOutcome.x;
			scrollY = -combatOutcome.y;
		} else {
			g.s = this; // ???
		}
	}
	
	@Override
	public void renderNotice(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor, int x, int y, int width, int height) {
		if (w.player != null && !w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains) && ConquestHelpSystem.hasAny(this, g.s)) {
			ConquestHelpSystem.render(d, x, y, this, g.s, width);
			return;
		}
		if (showActionableNotice()) {
			final StrategicScreen ss = this;
			d.blit(fight_big, x, y);
			x += fight_big.srcWidth + MyDraw.UI_SPACING;
			width -= fight_big.srcWidth + MyDraw.UI_SPACING;
			String info = noticeInfoString();
			d.text(info, AGame.FOUNT, x, y, width);
			y += d.textSize(info, AGame.FOUNT, x, y, width).height + MyDraw.UI_SPACING;
			d.button(x, y, width, noticeButtonText(), new Runnable() {
				@Override
				public void run() {
					if (g.s instanceof UniScreen && ((UniScreen) g.s).intent instanceof StrategicEditShipIntent) {
						if (w != null && w.combatInfo != null && combatUniScreen != null) {
							returnTo = (UniScreen) g.s;
						} else {
							UniScreen us = (UniScreen) g.s;
							StrategicEditShipIntent sesi = (StrategicEditShipIntent) us.intent;
							if (!sesi.justSaved) {
								us.confirmDialog = new StrategicEditShipIntent.LeaveDialog(new Runnable() {
									@Override
									public void run() {
										ss.doNoticeButton();
									}
								});
								return;
							}
						}
					}
					ss.doNoticeButton();
				}
			});
		} else {
			Graphics gfx = (Graphics) d.frame().nativeRenderer();
			gfx.setClip(x, y, width, height);
			d.blit(w.speed.icon, x, y);
			if (w.player != null) {
				y += 16 + MyDraw.BUTTON_SPACING;
				ArrayList<FleetOwner> fos = w.map.fleetOwners();
				int fosz = fos.size();
				final StrategicScreen ss = this;
				for (int foi = 0; foi < fosz; foi++) {
					FleetOwner fo = fos.get(foi);
					if (fo == w.player) { continue; }
					int fsz = fo.getFleets().size();
					for (int fi = 0; fi < fsz; fi++) {
						final Fleet f = fo.getFleets().get(fi);
						if (w.player.cities.contains(f.destination)) {
							String text = fo.getName() + " > " + f.destination.getDisplayName() + "\n" + w.describeTime(f.travelTimeLeft(fo.bonuses(), w.map));
							d.blit(invader, x, y);
							d.text(text, AGame.FOUNT, x + 16 + MyDraw.UI_SPACING, y);
							d.iconButton(x + width - MyDraw.ICON_BUTTON_SZ, y, mapGoto, new Runnable() {
								@Override
								public void run() {
									if (g.s instanceof UniScreen && ((UniScreen) g.s).intent instanceof StrategicEditShipIntent) {
										UniScreen us = (UniScreen) g.s;
										StrategicEditShipIntent sesi = (StrategicEditShipIntent) us.intent;
										if (!sesi.justSaved) {
											us.confirmDialog = new StrategicEditShipIntent.LeaveDialog(new Runnable() {
												@Override
												public void run() {
													g.s = ss;
													scrollX = -f.realX(w.map);
													scrollY = -f.realY(w.map);
												}
											});
											return;
										}
									}
									g.s = ss;
									menu = false;
									scrollX = -f.realX(w.map);
									scrollY = -f.realY(w.map);
								}
							}, true);
							y += Math.max(Math.max(16, MyDraw.ICON_BUTTON_SZ), AGame.FOUNT.lineHeight * 2) + MyDraw.BUTTON_SPACING;
						}
					}
				}
			}
			gfx.clearClip();
		}
	}
	
	@Override
	public int noticeHeight(MyDraw d, int width) {
		if (w.player != null && !w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains) && ConquestHelpSystem.hasAny(this, g.s)) {
			return ConquestHelpSystem.getHeight(d, this, g.s, width);
		}
		if (showActionableNotice()) {
			return StrictMath.max(
					fight_big.srcHeight,
					(int) d.textSize(noticeInfoString(), AGame.FOUNT, 0, 0, width - fight_big.srcWidth - MyDraw.UI_SPACING).height + MyDraw.UI_SPACING + MyDraw.BUTTON_H);
		} else {
			int h = 16;
			if (w.player == null) { return h; }
			ArrayList<FleetOwner> fos = w.map.fleetOwners();
			int fosz = fos.size();
			for (int foi = 0; foi < fosz; foi++) {
				FleetOwner fo = fos.get(foi);
				if (fo == w.player) { continue; }
				int fsz = fo.getFleets().size();
				for (int fi = 0; fi < fsz; fi++) {
					final Fleet f = fo.getFleets().get(fi);
					if (w.player.cities.contains(f.destination)) {
						h += Math.max(Math.max(16, MyDraw.ICON_BUTTON_SZ), AGame.FOUNT.lineHeight * 2) + MyDraw.BUTTON_SPACING;
					}
				}
			}
			return h;
		}
	}
	
	private boolean baseTick(int ms) {		
		if (w.combatInfo != null && w.waitingForCombatSetup && w.combatInfo.isUncontested()) {
			w.immediatePostCombat();
			combatOutcome = w.postCombat(/* uncontested */ true);
			w.combatInfo = null;
			w.waitingForCombatSetup = false;
			combatConfirmed = false;
		}
		if (w.combatInfo != null && (combatConfirmed || w.isMultiplayer() || w.fakeMultiplayerForTesting) && w.waitingForCombatSetup) {
			combatConfirmed = false;
			UniScreen.Intent intent;
			int mySideIndex = w.combatInfo.playerCombatSideIndex(w);
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) {
				w.multiplayerCampaignCombatSetupIntent = new MultiplayerCampaignCombatSetupIntent(this, w.combatInfo, mySideIndex == -1);
				intent = w.multiplayerCampaignCombatSetupIntent;
			} else {
				intent = new CampaignCombatSetupIntent(this, w.combatInfo);
			}
			w.waitingForCombatSetup = false;
			
			combatUniScreen = new UniScreen(g, intent);
			combatUniScreen.combat = w.combatInfo.combat;
			combatUniScreen.cw = w;
			combatUniScreen.mySide = mySideIndex == -1 ? null : w.combatInfo.combat.sides.get(mySideIndex);
			if (!(w.isMultiplayer() || w.fakeMultiplayerForTesting)) {
				g.s = combatUniScreen;
				return true;
			}
		}

		if (combatUniScreen == null) {
			time += ms;
		}
		
		if (w.combatInfo == null) {
			autosaveIfNeeded(); // Don't autosave in the middle of combats, it's not safe.
		}
		
		return false;
	}
	
	private transient int moveAroundDx, moveAroundDy;
	
	@Override
	public void moveAround(int dx, int dy) {
		moveAroundDx = dx;
		moveAroundDy = dy;
	}

	@Override
	public void input(Input in, State drawState, Pt cursor, Pt click, int ms) {
		/*if (in.keyPressed("C")) { // qqDPS DEBUG
			w.player.name = System.currentTimeMillis() % 10000 + "";
			w.player.cities.get(0).name = w.player.name;
		}*/
		if (savedIndicator > 0) { savedIndicator -= ms; }
		if (mrpd != null) {
			ModReloadProgressDialog myMrpd = mrpd;
			if (mrpd.tick(in) && myMrpd == mrpd) {
				mrpd = null;
			}
			return;
		}
		
		if (zoom != prevZoom) {
			msSinceZoomChanged = 0;
		}
		msSinceZoomChanged += ms;
		prevZoom = zoom;
		
		// qqDPS
		/*if (w.player.conquerorInfo == null) {
			ArrayList<Fleet> fleets = new ArrayList<Fleet>();
			for (Empire e : w.map.empires) {
				if (e != w.player && !e.getFleets().isEmpty()) {
					fleets.add(e.getFleets().get(0));
				}
				if (fleets.size() == 4) {
					e.previouslyAwardedClaim = true;
					break;
				}
			}
			w.player.conquerorInfo = w.map.conqueror(w.player.cities.get(0), fleets);
		}*/
		
		if (w == null || w.map == null) {
			leave();
			g.showError(_t("world_gen_load_failed"));
			return;
		}
		
		if (w.player != null && !w.player.playerControlled) {
			g.reportError("World Player " + w.player.name + " " + w.player.id + " not player controlled", null, null, false, true);
		}
		
		if (w.player == null) {
			showDiplomacy = false;
		}
		
		if (addDiplomacyOffersToList && w.player != null) {
			for (Relationship rel : w.map.getRelationships(w.player)) {
				Relationship.Offer o = rel.getOffer(w.map);
				if (o != null && !rel.isOfferingNegotiation(w.player)) {
					w.playerOffers.add(o);
				}
			}
			addDiplomacyOffersToList = false;
		}
		
		if (w.player != null) {
			// Clear out old offer notices.
			for (int i = 0; i < w.playerOffers.size(); i++) {
				Relationship.Offer o = w.playerOffers.get(i);
				if (o.rel.getOffer(w.map) == null) {
					w.playerOffers.remove(i);
					i--;
				}
			}
			for (int i = 0; i < w.player.diplomacyNotices.size(); i++) {
				if (!w.player.diplomacyNotices.get(i).isValid(w.map)) {
					w.player.diplomacyNotices.remove(i);
					i--;
				} 
			}
			// Age out hero stat changes.
			boolean many = w.player.heroStatEvents.size() > 3;
			for (Iterator<Hero.StatChange> sci = w.player.heroStatChanges.iterator(); sci.hasNext();) {
				Hero.StatChange sc = sci.next();
				sc.age += ms;
				if (sc.age >= 10000 || (many && sc.age > 1000)) {
					sci.remove();
				}
			}
		}
		
		/*
		for (Hero h : w.map.heroes) {
			if (!h.hired && !h.type.isStarter) {// && h.type.role != HeroType.Role.CAPTAIN) {
				h.inEmpire = w.player;
			}
		}
		*/
		
		w.ssForDiplomacyCallback = this;
		
		if (w.combatInfo == null) {
			combatUniScreen = null;
			if (returnTo != null && ((StrategicEditShipIntent) returnTo.intent).recreate(this)) {
				returnTo = null;
				return;
			}
		} else if (returnTo != null && ((StrategicEditShipIntent) returnTo.intent).recreate(this)) {
			returnTo = null;
			return;
		}
		returnTo = null;
		
		if (w.player != null && (w.victory() || w.defeat())) {
			statsDisplay.input(w.map.stats, in);
			if (w.isMultiplayer() && w.tick(ms, this)) { return; }
			if (Keys.check(in, "ESCAPE")) {
				if (w.someoneWon() || g.lanServer == null) {
					leave();
				} else {
					spectate();
				}
			}
			return;
		}
		
		if (w.desyncPubliclyDetected()) {
			g.s = new ResumeScreen(g, w, this, g.s, g.playerID(), false, 0, w.gameName);
			return;
		}
		
		if (waitForOtherPlayers) {
			timeWaitedForOtherPlayers += ms;
			g.tickClients();
			for (int iter = 0; iter < 100; iter++) {
				JSONObject frame = g.pollMessage();
				if (frame != null && frame.getString("type").equals("frame")) {
					w.membershipUpdate(frame);
					JSONArray messages = frame.getJSONArray("messages");
					for (int i = 0; i < messages.length(); i++) {
						JSONObject msg = messages.getJSONObject(i);
						System.out.println(msg.getString("type"));
						if (msg.getString("type").equals("chat")) {
							CampaignWorld.EXECS.get("chat").run(msg, w.map, w);
						} else if (msg.getString("type").equals("worldgenComplete")) {
							StrategicPlayerInfo pi = w.channelPlayers.get(msg.getInt("playerID"));
							if (pi != null) {
								System.out.println("Received worldgenComplete for " + pi.name + " " + pi.id);
								pi.worldgenComplete = true;
								timeWaitedForOtherPlayers = 0;
							}
						}
					}
				}
				boolean allReady = true;
				for (StrategicPlayerInfo spi : w.channelPlayers.values()) {
					allReady &= spi.worldgenComplete;
				}
				if (allReady) {
					waitForOtherPlayers = false;
					return;
				} else {
					int timeLeft = Server.clientRetainTime + EXTRA_WAIT_TIME - timeWaitedForOtherPlayers;
					if (timeLeft <= 0) {
						g.showError(_t("cannot_rejoin_server_timed_out"));
						g.disconnectClient();
						g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
						return;
					}
				}
			}
			return;
		}
		
		moveToHover = nextMoveToHover;
		nextMoveToHover = null;
		
		if (!w.disconnectedPlayers.isEmpty()) {
			StringBuilder reason = new StringBuilder("disconnected");
			for (StrategicPlayerInfo spi : w.disconnectedPlayers) {
				disconnectedPlayers.add(spi.name);
				reason.append(AGame.makeFileSafe(spi.name));
			}
			emergencySave(reason.toString());
			w.disconnectedPlayers.clear();
		}
		
		if (diplomacyFailureNotice != null) {
			diplomacyFailureNoticeTimeout += ms;
			if (diplomacyFailureNoticeTimeout >= DIPLOMACY_NOTICE_TIMEOUT) {
				diplomacyFailureNotice = null;
			}
		}
		
		if (selectedFleet != null && selectedFleet.actives.isEmpty() && selectedFleet.reserve.isEmpty()) {
			selectedFleet = null;
		}
		
		if (w.map.resistance != null) {
			// Deref and gc.
			w.map.resistance = null;
			w.map.approxCityDist = null;
			w.map.locPairs = null;
			Runtime.getRuntime().gc();
		}
		
		if (time == 0 && w.player != null) {
			scrollX = -w.player.cities.get(0).x;
			scrollY = -w.player.cities.get(0).y;
		}
		
		if (baseTick(ms)) { return; }
		
		// If we're already in strategic and there's a fight, do just yank. In SP, the user has to do the confirm before
		// the combatUniScreen is created, so this is OK.
		if (w.combatInfo != null && combatUniScreen != null) {
			int mySideIndex = w.combatInfo.playerCombatSideIndex(w);
			if (mySideIndex != -1) {
				g.s = combatUniScreen;
				return;
			}
		}
		
		if (inputTick++ < 3) {
			notificationSounds.clear();
		}
		timeUntilNextNotificationSound -= ms;
		if (timeUntilNextNotificationSound <= 0 && !notificationSounds.isEmpty()) {
			notificationSounds.pollFirst().play(in);
			timeUntilNextNotificationSound = 400;
		}
		
		ScreenMode sm = in.mode();
		
		Pt mouseDown = in.mouseDown();
		if (mouseDown != null && in.mouseDownButton() > 1) {
			if (dragging) {
				scrollX = (int) (dragStartScrollX + (mouseDown.x - dragStartX) / zoom);
				scrollY = (int) (dragStartScrollY + (mouseDown.y - dragStartY) / zoom);
			} else {
				dragStartX = mouseDown.x;
				dragStartY = mouseDown.y;
				dragStartScrollX = scrollX;
				dragStartScrollY = scrollY;
				dragging = true;
			}
		} else {
			dragging = false;
		}
		
		if (Keys.checkDown(in, "zoom_in", "ADD", false)) {
			zoom = zoom * (1 + AirshipGame.zoomSpeed * 0.01);
		}
		if (Keys.checkDown(in, "zoom_out", "SUBTRACT", false)) {
			zoom = zoom / (1 + AirshipGame.zoomSpeed * 0.01);
		}
		
		boolean inConquerorSB = false;
		if (w.player != null && w.player.conquerorInfo != null) {
			conquerorSB.tick(in, conquerorSBR.x, conquerorSBR.y, conquerorSBR.w, conquerorSBR.h);
			inConquerorSB = MyDraw.in(conquerorSBR.x, conquerorSBR.y, conquerorSBR.w, conquerorSBR.h, in.cursor());
		}
		if (w.isMultiplayer()) {
			int y = MyDraw.TOP_BAR_H;
			int x = MyDraw.SIDE_CLEARANCE;
			int width = StrictMath.max(200, sm.width / 4);
			int height = StrictMath.max(150, sm.height / 4);
			if (!chatCollapsed) {
				chatScrollBar.tick(in, x, y, width, height);
			}
			if (shipNameDialog == null && (!showDiplomacy || selectCityForOfferEditor != null || showCityForDiplomacy) && !showHeroes && !inConquerorSB && !showEmpireDetails &&
				(cursor.x < x || cursor.x > x + width || cursor.y < y || cursor.y > y + height) &&
				(selectedFleet == null || cursor.x < sm.width - fleetListWidth) &&
				(menuCity == null || cursor.x < sm.width - cityDetailWidth)
			)
			{
				adjScrollX = scrollX + (sm.width / 2 / zoom);
				adjScrollY = scrollY + (sm.height / 2 / zoom);
				final double targetX = screenToWorldX(cursor.x);
				final double targetY = screenToWorldY(cursor.y);
				zoom = zoom * StrictMath.pow((1 + AirshipGame.mouseWheelZoomSpeed * 0.0001), in.scrollAmount());
				zoom = StrictMath.max(MIN_ZOOM_LEVEL, StrictMath.min(MAX_ZOOM_LEVEL, zoom));
				scrollX = -targetX + (cursor.x - sm.width / 2) / zoom;
				scrollY = -targetY + (cursor.y - sm.height / 2) / zoom;
			}
		} else if (shipNameDialog == null && (!showDiplomacy || selectCityForOfferEditor != null || showCityForDiplomacy) && !showHeroes && !inConquerorSB && !showEmpireDetails && (selectedFleet == null || cursor.x < sm.width - fleetListWidth) && (menuCity == null || cursor.x < sm.width - cityDetailWidth) && in.scrollAmount() != 0) {
			adjScrollX = scrollX + (sm.width / 2 / zoom);
			adjScrollY = scrollY + (sm.height / 2 / zoom);
			final double targetX = screenToWorldX(cursor.x);
			final double targetY = screenToWorldY(cursor.y);
			zoom = zoom * StrictMath.pow((1 + AirshipGame.mouseWheelZoomSpeed * 0.0001), in.scrollAmount());
			zoom = StrictMath.max(MIN_ZOOM_LEVEL, StrictMath.min(MAX_ZOOM_LEVEL, zoom));
			scrollX = -targetX + (cursor.x - sm.width / 2) / zoom;
			scrollY = -targetY + (cursor.y - sm.height / 2) / zoom;
		}
		
		zoom = StrictMath.max(MIN_ZOOM_LEVEL, StrictMath.min(MAX_ZOOM_LEVEL, zoom));
				
		if (SimplePref.SIDE_BUMP_TO_SCROLL.get()) {
			if (cursor.y < 2) { scrollY += AirshipGame.scrollSpeed / zoom; }
			if (cursor.y > in.mode().height - 2) { scrollY -= AirshipGame.scrollSpeed / zoom; }
			if (cursor.x < 2) { scrollX += AirshipGame.scrollSpeed / zoom; }
			if (cursor.x > in.mode().width - 2) { scrollX -= AirshipGame.scrollSpeed / zoom; }
		}
		if (!chatField.focus && renameCity == null && shipNameDialog == null && medalEditor == null && !showEmpireDetails) {
			if (moveAroundDy < 0 || in.keyDown("UP") || (Keys.checkDown(in, "up", "W", false))) { scrollY += AirshipGame.scrollSpeed / zoom; }
			if (moveAroundDy > 0 || in.keyDown("DOWN") || (Keys.checkDown(in, "down", "S", false))) { scrollY -= AirshipGame.scrollSpeed / zoom; }
			if (moveAroundDx < 0 || in.keyDown("LEFT") || (Keys.checkDown(in, "left", "A", false))) { scrollX += AirshipGame.scrollSpeed / zoom; }
			if (moveAroundDx > 0 || in.keyDown("RIGHT") || (Keys.checkDown(in, "right", "D", false))) { scrollX -= AirshipGame.scrollSpeed / zoom; }
		}
		moveAroundDx = 0;
		moveAroundDy = 0;
		
		scrollX = Math.max(-w.map.cityOwnership.length, Math.min(0, scrollX));
		scrollY = Math.max(-w.map.cityOwnership.length, Math.min(0, scrollY));
		
		adjScrollX = scrollX + (sm.width / 2 / zoom);
		adjScrollY = scrollY + (sm.height / 2 / zoom);
		
		if (w.combatInfo != null && !combatConfirmed && !w.isMultiplayer()) {
			if (g.testRunner != null || Keys.check(in, "ENTER") || in.clickButton() == 1) {
				combatConfirmed = true;
			}
			return;
		}
		
		if (selectCityForOfferEditor != null) {
			if (Keys.check(in, "ESCAPE")) {
				selectCityForOfferEditor.cancelCitySelection();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (showCityForDiplomacy) {
			if (Keys.check(in, "ESCAPE")) {
				showCityForDiplomacy = false;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (checkLeave) {
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player == null) { shipNameDialog = null; }
		if (shipNameDialog != null) {
			shipNameDialog.input(in, drawState, cursor, click, ms);
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (highlitTravelConnectionTime++ > 2) {
			highlitTravelConnection = null;
		}
		
		if (showEmpireDetails && empireDetailsMode == EmpireDetailsMode.BONUSES) {
			empireBonusesSB.tick(in, empireBonusesSBR.x, empireBonusesSBR.y, empireBonusesSBR.w, empireBonusesSBR.h);
		}
		if (showEmpireDetails && empireDetailsMode == EmpireDetailsMode.SETTINGS) {
			settingsSB.tick(in, settingsSBR.x, settingsSBR.y, settingsSBR.w, settingsSBR.h);
		}
		if (showEmpireDetails && empireDetailsMode == EmpireDetailsMode.MEDALS) {
			medalsSB.tick(in, medalsSBR.x, medalsSBR.y, medalsSBR.w, medalsSBR.h);
		}
		if (showEmpireDetails && empireDetailsMode == EmpireDetailsMode.SHIPS) {
			shipsLedger.tick(in);
		}
		if (showEmpireDetails && empireDetailsMode == EmpireDetailsMode.CITIES) {
			cityLedger.tick(in);
		}
		
		if (selectedFleet != null && !showHeroes && !showEmpireDetails) {
			fleetListWidth =
			StrictMath.max(sm.width / 6,
				StrictMath.max(
					200,
					MyDraw.BUTTON_H * 3 + MyDraw.BUTTON_SPACING * 2 + MyDraw.WINDOW_INSET + MyDraw.BUTTON_SPACING + ScrollBar.SCROLL_BAR_W + MyDraw.PANEL_INSET + MyDraw.UI_SPACING
				)
			);
			int x = sm.width - fleetListWidth;
			fleetScrollBar.tick(in, x, MyDraw.TOP_BAR_H + MyDraw.WINDOW_INSET, fleetListWidth, sm.height - MyDraw.TOP_BAR_H - MyDraw.WINDOW_INSET * 2);
		}
		
		if (menuCity != null && !showHeroes && !showEmpireDetails) {
			if (w.player != null && w.player.cities.contains(menuCity) && menuCity.takeoverNeeded) {
				takeoverScrollBar.tick(in, sm.width - cityDetailWidth, MyDraw.TOP_BAR_H + MyDraw.WINDOW_INSET, cityDetailWidth, sm.height - MyDraw.TOP_BAR_H - MyDraw.WINDOW_INSET * 2);
			} else {
				cityScrollBar.tick(in, sm.width - cityDetailWidth, MyDraw.TOP_BAR_H + MyDraw.WINDOW_INSET, cityDetailWidth, sm.height - MyDraw.TOP_BAR_H - MyDraw.WINDOW_INSET * 2);
			}
		}
		
		/* Why would this ever make sense? It makes the game queue back up!
		if (!disconnectedPlayers.isEmpty()) {
			return;
		}
		*/
		
		if (w.victory() || w.defeat()) {
			if (w.isMultiplayer()) { w.tick(ms, this); }
			return;
		}
		
		if (renameCity != null) {
			renameCityField.input(in, cursor, click, ms);
			if (canRenameCity() && Keys.check(in, "ENTER")) {
				//renameCity.name = renameCityField.getText();
				w.giveCommand(msg("renameCity").put("city", renameCity.id).put("name", renameCityField.getText()));
				renameCity = null;
			} else if (Keys.check(in, "ESCAPE")) {
				renameCity = null;
			}
			if (w.isMultiplayer()) { w.tick(ms, this); }
			return;
		}
		
		if (showDiplomacy) {
			diplomacy.input(in, drawState, cursor, click, ms);
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (showHeroes) {
			heroesSB.tick(in, 0, 0, sm.width, sm.height);
			if (Keys.check(in, "ESCAPE") || Keys.check(in, "ENTER") || Keys.check(in, "strategic_heroes", "H", false)) {
				showHeroes = false;
				heroToShow = null;
				cityForHero = null;
				shipForHero = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (combatOutcome != null) {
			if (w.player != null && w.player.rewardGiven != null) {
				combatOutcome = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (hasDeclarationsOfWar()) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				acknowledgeDeclarationOfWar();
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (askForScrap != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				doScrap();
				drawState.hasClicked();
			}
			if (Keys.check(in, "ESCAPE") && drawState.canClick()) {
				askForScrap = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (askForScuttle != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				doScuttle();
				drawState.hasClicked();
			}
			if (Keys.check(in, "ESCAPE") && drawState.canClick()) {
				askForScuttle = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (medalEditor != null) {
			medalEditor.tick(in, ms);
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (awardMedalTo != null) {
			medalsSB.tick(in, medalsSBR.x, medalsSBR.y, medalsSBR.w, medalsSBR.h);
			if (Keys.check(in, "ESCAPE")) {
				awardMedalTo = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && !w.player.coronationMessageSeen && w.has(ConquestToggle.CORONATION) && w.player.isCoronationReady(w.map) && !w.player.isCoronating()) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationMessageSeen = true;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && !w.player.finalRitualMessageSeen && w.has(ConquestToggle.CORONATION) && w.player.isFinalRitualReady(w.map) && !w.player.isDoingFinalRitual()) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.finalRitualMessageSeen = true;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.coronationFailedDueToLostCityCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationFailedDueToLostCityCity = null;
				w.player.coronationFailedDueToLostCityEmpire = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.finalRitualFailedDueToLostCityCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.finalRitualFailedDueToLostCityCity = null;
				w.player.finalRitualFailedDueToLostCityEmpire = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.coronationFailedDueToNoLongerReadyCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationFailedDueToNoLongerReadyCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.coronationIntentionallyCancelledCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationIntentionallyCancelledCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.finalRitualIntentionallyCancelledCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.finalRitualIntentionallyCancelledCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.coronationWarningCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationWarningCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.finalRitualWarningCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.finalRitualWarningCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.coronationStartedCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.coronationStartedCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.player.finalRitualStartedCity != null) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.finalRitualStartedCity = null;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null && w.map.areRitualSitesVisible() && !w.player.ritualSitesVisibleMessageSeen) {
			if (Keys.check(in, "ENTER") && drawState.canClick()) {
				w.player.ritualSitesVisibleMessageSeen = true;
				drawState.hasClicked();
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (!menu && showEmpireDetails) {
			if (Keys.check(in, "ESCAPE") || Keys.check(in, "ENTER") || Keys.check(in, "strategic_empire_details", "E", false)) {
				showEmpireDetails = false;
			} else {
				for (EmpireDetailsMode edm : EmpireDetailsMode.values()) {
					if (!edm.available(w)) { continue; }
					if (in.keyPressed(edm.shortcut)) {
						empireDetailsMode = edm;
					}
				}
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (w.player != null) {
			if (selectedFleet == null || !selectedFleet.fleeDestinationNeeded) {
				for (Fleet fl : w.player.getFleets()) {
					if (fl.fleeDestinationNeeded) {
						selectedFleet = fl;
						setFleetSelection(new ArrayList<Airship>(fl.actives));
						getFleetSelection().addAll(fl.reserve);
						menuCity = null;
					}
				}
			}

			// If we've selected a fleet that no longer exists.
			if (selectedFleet != null && w.map.owner(selectedFleet) == null) {
				// Try to find a fleet the ships are in now.
				Fleet oldF = selectedFleet;
				selectedFleet = null;
				for (Fleet fl : w.player.getFleets()) {
					if (fl.containsAny(oldF)) {
						selectedFleet = fl;
						ArrayList<Airship> both = new ArrayList<Airship>(fl.actives);
						both.addAll(fl.reserve);
						getFleetSelection().retainAll(both);
						break;
					}
				}
			}
		} else {
			if (w.map.owner(selectedFleet) == null) {
				selectedFleet = null;
			}
		}

		if (menu && drawState.canClick()) {
			if (Keys.check(in, "strategic_menu_quit", "Q", false)) {
				checkedLeave();
				drawState.hasClicked();
				return;
			}
			if (!w.isMultiplayer() && Keys.check(in, "strategic_menu_open", "O", false)) {
				drawState.hasClicked();
				open(false);
				return;
			}
			if (w.combatInfo == null && Keys.check(in, "strategic_menu_save", "V", false)) {
				drawState.hasClicked();
				save();
				return;
			}
			if (Keys.check(in, "strategic_menu_back_to_game", "B", false) || Keys.check(in, "ESCAPE")) {
				menu = false;
				drawState.hasClicked();
			}
		}
		
		if ((w.isMultiplayer() || w.fakeMultiplayerForTesting) && chatField.focus && !menu) {
			chatField.input(in, cursor, click, ms);
			if (Keys.check(in, "ENTER")) {
				sendChat();
			} else if (Keys.check(in, "ESCAPE")) {
				chatField.focus = false;
			}
			w.tick(ms, this);
			return;
		}
		
		if (!menu && w.player != null && (travelToDialogDestination != null || travelToDialogTarget != null)) {
			if (Keys.check(in, "ENTER")) {
				if (travelToDialogDestination != null) {
					travelTo(travelToDialogDestination, SupplySplitMode.getDefault(w.player, selectedFleet, getFleetSelection(), travelToDialogDestination, null, w.map), travelToDialogBesiege);
				} else {
					Pt icept = selectedFleet.getFlightIntercept(travelToDialogTarget, getFleetSelection(), w.map);
					if (icept != null) {
						doIntercept(travelToDialogTarget, SupplySplitMode.getDefault(w.player, selectedFleet, getFleetSelection(), null, icept, w.map));
					}
				}
				travelToDialogDestination = null;
				travelToDialogTarget = null;
			}
			if (Keys.check(in, "ESCAPE")) {
				travelToDialogDestination = null;
				travelToDialogTarget = null;
			}
			if (w.isMultiplayer()) { w.tick(ms, this); }
			return;
		}
		
		if (!menu && w.player != null && w.player.researchedTech != null) {
			if (Keys.check(in, "ENTER")) {
				w.player.researchedTech = null;
				if (w.player.research == null && !w.player.isAllResearchDone(w.map)) {
					g.s = new TechScreen(this);
				}
			} else if (Keys.check(in, "ESCAPE")) {
				w.player.researchedTech = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (!menu && w.map.newEra) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				w.map.newEra = false;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		}
		
		if (selectedNest != null && selectedNest.type == null) { selectedNest = null; }
		
		if (selectedNest != null || (w.player != null && w.player.rewardGiven != null)) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				if (w.player != null && w.player.rewardGiven != null) {
					w.player.rewardGiven = null;
				} else {
					selectedNest = null;
				}
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		} else if (upgradeStartedNest != null) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				upgradeStartedNest.showUpgradeStartDialog = false;
				upgradeStartedNest = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		} else if (upgradeNearingCompletionNest != null) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				upgradeNearingCompletionNest.showUpgradeNearingCompletionDialog = false;
				upgradeNearingCompletionNest = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		} else if (upgradeCompleteNest != null) {
			if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
				upgradeCompleteNest.showUpgradeCompleteDialog = false;
				upgradeCompleteNest = null;
			}
			if (w.isMultiplayer() || w.fakeMultiplayerForTesting) { w.tick(ms, this); }
			return;
		} else if (upgradeStartedNest != null && upgradeStartedNest.type == null) {
			upgradeStartedNest = null;
		} else if (upgradeNearingCompletionNest != null && upgradeNearingCompletionNest.type == null) {
			upgradeNearingCompletionNest = null;
		} else if (upgradeCompleteNest != null && upgradeCompleteNest.type == null) {
			upgradeCompleteNest = null;
		}
		
		// Look for monster nests to tell the player about.
		if (w.player != null) {
			if (upgradeStartedNest != null && !upgradeStartedNest.showUpgradeStartDialog()) {
				upgradeStartedNest = null;
			}
			if (upgradeNearingCompletionNest != null && !upgradeNearingCompletionNest.showUpgradeNearingCompletionDialog()) {
				upgradeNearingCompletionNest = null;
			}
			if (upgradeCompleteNest != null && !upgradeCompleteNest.showUpgradeCompleteDialog()) {
				upgradeCompleteNest = null;
			}
			lp: for (City c : w.player.cities) {
				for (MonsterNest n : c.nestsInTerritory(w.map)) {
					if (n.showUpgradeStartDialog()) {
						upgradeStartedNest = n;
						break lp;
					}
					if (n.showUpgradeNearingCompletionDialog()) {
						upgradeNearingCompletionNest = n;
						break lp;
					}
					if (n.showUpgradeCompleteDialog()) {
						upgradeCompleteNest = n;
						break lp;
					}
				}
			}
		}
		
		if (!menu && Keys.check(in, "strategic_menu", "M", false) && drawState.canClick()) {
			menu = true;
			drawState.hasClicked();
		}
		
		if (selectedFleet != null && Keys.check(in, "ESCAPE")) {
			selectedFleet = null;
		} else if (menuCity != null && Keys.check(in, "ESCAPE")) {
			menuCity = null;
		} else if (!menu && !showEmpireDetails && selectedFleet == null && menuCity == null && Keys.check(in, "ESCAPE") && drawState.canClick()) {
			menu = true;
			drawState.hasClicked();
		}
		
		if (w.player != null) {
			if (!menu && !w.player.messages.isEmpty()) {
				if (Keys.check(in, "ENTER") || Keys.check(in, "ESCAPE")) {
					w.player.messages.remove(w.player.messages.size() - 1);
				}
				if (!w.isMultiplayer() && !w.fakeMultiplayerForTesting) {
					return;
				}
			}
			
			
		}
		
		if (!menu && (w.player != null || !w.isMultiplayer()) && Keys.check(in, "strategic_pause", "SPACE", false) && drawState.canClick()) {
			CampaignWorld.Speed oldSpeed;
			CampaignWorld.Speed newSpeed;
			if (w.isMultiplayer()) {
				oldSpeed = w.speedVoters.containsKey(g.playerID()) ? w.speedVoters.get(g.playerID()) : w.speed;
			} else {
				oldSpeed = w.speed;
			}
			if (oldSpeed == CampaignWorld.Speed.STOP) {
				newSpeed = lastNonStopSpeed;
			} else {
				newSpeed = CampaignWorld.Speed.STOP;
				lastNonStopSpeed = oldSpeed;
			}
			w.giveCommand(msg("setCampaignSpeed").put("voterID", g.playerID()).put("speed", newSpeed.name()));
			drawState.hasClicked();
		}
		
		if (!menu && w.player != null && w.map.techSpeed.speedMultiplier != 0 && Keys.check(in, "strategic_research", "R", false)) {
			g.s = new TechScreen(this);
		}
		
		if (!menu && !showEmpireDetails) {
			if (Keys.check(in, "strategic_empire_details", "E", false)) {
				showEmpireDetails = true;
			}
		}
		
		if (!menu && !showDiplomacy && w.player != null && w.has(ConquestToggle.DIPLOMACY) && Keys.check(in, "strategic_diplomacy", "I", false)) {
			diplomacy.showPopupOnly = false;
			diplomacy.focusEmpire = null;
			showDiplomacy = true;
		}
		
		if (!menu && showDiplomacy && !diplomacy.showPopupOnly && (Keys.check(in, "ESCAPE") || Keys.check(in, "ENTER")  || Keys.check(in, "strategic_diplomacy", "I", false))) {
			showDiplomacy = false;
		}
		
		if (!menu && w.player != null && !showHeroes && EHeroes.it.enabled && w.map.heroFrequency.frequencyMultiplier != 0 && Keys.check(in, "strategic_heroes", "H", false)) {
			showHeroes = true;
			//Hero.printList(w.map);
			heroToShow = null;
		}

		if (!menu && menuCity != null) {
			if (w.player == null) {
				if (Keys.check(in, "strategic_city_view", "V", false) && drawState.canClick()) {
					UniScreen us = new UniScreen(g, new ViewCityIntent(this));
					us.cw = w;
					us.wm = w.map;
					us.city = menuCity;
					g.s = us;
				}
			} else {
				if (w.player.cities.contains(menuCity)) {
					if (Keys.check(in, "strategic_city_create_ship", "C", false) && drawState.canClick() && menuCity.canBuild(ShipType.AIRSHIP)) {
						drawState.hasClicked();
						createShip(ShipType.AIRSHIP);
					}
					if (Keys.check(in, "strategic_city_build_ship", "B", false) && drawState.canClick() && menuCity.canBuild(ShipType.AIRSHIP)) {
						drawState.hasClicked();
						buildSavedShip(ShipType.AIRSHIP);
					}
					if (Keys.check(in, "strategic_city_create_landship", "N", false) && drawState.canClick() && menuCity.canBuild(ShipType.LANDSHIP)) {
						drawState.hasClicked();
						createShip(ShipType.LANDSHIP);
					}
					if (Keys.check(in, "strategic_city_build_landship", "L", false) && drawState.canClick() && menuCity.canBuild(ShipType.LANDSHIP)) {
						drawState.hasClicked();
						buildSavedShip(ShipType.LANDSHIP);
					}
					if (Keys.check(in, "strategic_city_defences", "F", false) && drawState.canClick() && menuCity.canBuild(ShipType.BUILDING)) {
						drawState.hasClicked();
						defenses();
					}
				} else {
					Spy spy = w.player.getSpyFor(menuCity);
					if (Keys.check(in, "strategic_city_spy", "P", false) && drawState.canClick()) {
						if (spy == null) {
							if (w.player.spies.size() < EmpireStat.MAX_SPIES.get(w.player.bonuses)) {
								w.giveCommand(msg("sendSpy").put("empire", w.player.id).put("city", menuCity.id));
							}
						} else {
							w.giveCommand(msg("recallSpy").put("empire", w.player.id).put("city", menuCity.id));
						}
					}
					if (Keys.check(in, "strategic_city_view", "V", false) && drawState.canClick() && w.player.spyCanViewCity(menuCity, w.map)) {
						UniScreen us = new UniScreen(g, new EspionageIntent(this, menuCity));
						us.cw = w;
						us.wm = w.map;
						us.city = menuCity;
						g.s = us;
					}
				}
			}
		}
		
		if (w.isMultiplayer() && !chatField.focus && !menu && Keys.check(in, "ENTER")) {
			chatField.focus = true;
		}
		
		if (!w.isMultiplayer() && selectedFleet != null) {
			selectedFleet.fixOffroad(w.map);
		}
		
		if ((!menu && !modalUp() && !showEmpireDetails && !g.isShowingChatOverlay() && g.helpText == null) || w.isMultiplayer()) {
			w.tick(ms, this);
		}
		
		hoverCity = nextHoverCity;
		nextHoverCity = null;
		hoverFleet = nextHoverFleet;
		nextHoverFleet = null;
		if (hoverFleet != null && w.map.owner(hoverFleet) == null) {
			hoverFleet = null;
		}
	}
	
	public static Image MAP_BG;
	
	public boolean loadMapBG() {
		if (MAP_BG == null) {
			MAP_BG = SpriteUtils.loadImage("map_overlay.png");
			if (MAP_BG == null) {
				try { Thread.sleep(100); } catch (Exception e) {}
				MAP_BG = SpriteUtils.loadImage("map_overlay.png");
			}
			if (MAP_BG == null) {
				g.reportError("Unable to load map background.", null, SpriteUtils.loadImageReport("map_overlay.png"), true, false);
				return false;
			}
		}
		return true;
	}
	
	public static String getShipTooltip(Airship ship) {
		StringBuilder statsB = new StringBuilder();
		if (ship.isBonusConstruction) {
			String desc = Lang._tWithUntranslatedFallback("tooltip_" + ship.getName().replace(" ", "_"), "");
			if (!desc.isEmpty()) {
				statsB.append(desc).append("\n\n");
			}
		}
		if (EHeroes.it.enabled && !ship.isBonusConstruction) {
			statsB.append(CrewExperienceLevel.getLevel(ship.crewExperience).getName()).append("\n");
		}
		for (Utils.Pair<String, String> s : ShipEditorUtils.getStats(ship, 0, null)) {
			statsB.append(_t(s.a)).append(s.b).append("\n");
		}
		for (Medal medal : ship.medals) {
			statsB.append("\n").append(medal.name).append("\n").append(medal.effect.getDesc()).append("\n");
		}
		String stats = statsB.toString();
		return stats.substring(0, stats.length() - 1);
	}

	
	private final FleetElementAdapter fea = new FleetElementAdapter(this);
	
	private int fleetListWidth(ScreenMode sm) {
		return StrictMath.max(sm.width / 6,
			StrictMath.max(
				200,
				MyDraw.ICON_BUTTON_SZ * 5 + MyDraw.BUTTON_SPACING * 4 + MyDraw.WINDOW_INSET + MyDraw.BUTTON_SPACING + ScrollBar.SCROLL_BAR_W + MyDraw.PANEL_INSET + MyDraw.UI_SPACING
			)
		);
	}
	
	private void renderFleetList(final MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		if (w.player == null) {
			selectedFleet = null;
			return;
		}
		currentCursor = cursor;
		int width = fleetListWidth(sm);
		int y = MyDraw.TOP_BAR_H;
		int x = sm.width - width;
		d.drawRightSideWindow(sm, x, y);
		d.state.removeIntersectingGlowRects(x, y, width, sm.height);
		int height = sm.height - y - MyDraw.BUTTON_SPACING - MyDraw.UI_SPACING;
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.UI_SPACING;
		width -= MyDraw.WINDOW_INSET + MyDraw.BUTTON_SPACING;
		if (checked(sm, x, y, width, height)) {
			d.hook(x, y, width, height, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
				@Override
				public void run(Input in, Pt p, Type type) {
					// Ignore click to close.
				}
			});
		}
		
		if (w.map.toggles.contains(ConquestToggle.SUPPLY)) {
			int maxSupply = Fleet.maxSupply(_fleetSelection);
			int needed = 0;
			if (hoverFleet != null && selectedFleet.canIntercept(hoverFleet, getFleetSelection(), w.map)) {
				needed = w.player.moveSupplyCost(selectedFleet, getFleetSelection(), null, selectedFleet.getFlightIntercept(hoverFleet, getFleetSelection(), w.map), w.map);
			} else if (highlitTravelConnection != null) {
				needed = w.player.moveSupplyCost(selectedFleet, _fleetSelection, highlitTravelConnection, null, w.map);
			}
			if (needed == 0) {
				String t = _t("Supplies") + "\n" + Math.min(maxSupply, selectedFleet.supply()) + "/" + maxSupply;
				d.text(t, AGame.FOUNT, x, y);
				int tw = (int) d.textSize(t, AGame.FOUNT).x;
				d.progressBar(x + tw + MyDraw.UI_SPACING, y, width - tw - MyDraw.UI_SPACING, Math.min(1, selectedFleet.supply() * 1.0 / maxSupply));
				if (selectedFleet.supply() < maxSupply && selectedFleet.resupplyAmount(w.player.bonuses) > 0) {
					int timeUntilFullySupplied = ((maxSupply - selectedFleet.supply()) * Empire.MS_PER_INCOME) / selectedFleet.resupplyAmount(w.player.bonuses);
					d.tooltip(x + tw + MyDraw.UI_SPACING, y, width - tw - MyDraw.UI_SPACING, MyDraw.PROGRESS_BAR_H, w.describeTime(timeUntilFullySupplied));
				}
			} else {
				String t = _t("Supplies") + "\n" + needed + "/" + Math.min(maxSupply, selectedFleet.supply());
				d.text(t, AGame.FOUNT, x, y);
				int tw = (int) d.textSize(t, AGame.FOUNT).x;
				d.resourceNeededBar(x + tw + MyDraw.UI_SPACING, y, width - tw - MyDraw.UI_SPACING, Math.min(1, selectedFleet.supply() * 1.0 / maxSupply), needed * 1.0 / maxSupply);
			}
			y += MyDraw.PROGRESS_BAR_H + MyDraw.UI_SPACING;
			height -= MyDraw.PROGRESS_BAR_H + MyDraw.UI_SPACING;
		}
		y += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
		height -= MyDraw.BUTTON_H + MyDraw.UI_SPACING;
		if (ad != null) {
			height -= ad.srcHeight * (width - 4) / ad.srcWidth + MyDraw.UI_SPACING + MyDraw.SIDE_CLEARANCE;
		}
		FleetOwner owner = w.map.owner(selectedFleet);
		if (owner == null) {
			selectedFleet = null;
		} else {
			final ArrayList<Airship> allShips = selectedFleet.getAllShips();
			fleetScrollBar.drawNaked(d, x, y, width, height, FleetListEntry.generate(allShips, selectedFleet, false), fea);
			if (ad != null) {
				d.blit(ad, x + 2, y + height + MyDraw.UI_SPACING + 2, width - 4, ad.srcHeight * (width - 4) / ad.srcWidth);
				d.drawPanelBorder(x, y + height + MyDraw.UI_SPACING, width, ad.srcHeight * (width - 4) / ad.srcWidth + 4);
			}
			y -= MyDraw.BUTTON_H + MyDraw.UI_SPACING;
			if (owner == w.player) {
				final boolean selectAll = allShips.size() != getFleetSelection().size();
				d.iconButton(x, y, select, new Runnable() {
					@Override
					public void run() {
						if (selectedFleet == null) { return; }
						getFleetSelection().clear();
						if (selectAll) {
							getFleetSelection().addAll(allShips);
						}
					}
				}, true);
				d.tooltip(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, selectAll ? _t("Select_All") : _t("Deselect_All"));
				x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;

				int repairCost = 0;
				boolean canRepair = selectedFleet.location != null && selectedFleet.needsRepairs() && w.player.cities.contains(selectedFleet.location) && ((City) selectedFleet.location).canBuild(ShipType.AIRSHIP);
				if (canRepair) {
					for (Airship ship : allShips) {
						if (ship.getOriginalDesign() != null && ship.getOriginalDesign().modules.size() != ship.modules.size()) {
							repairCost += ship.getOriginalDesign().getRefitCostFrom(ship, true);
						}
					}
				}
				d.iconButton(x, y, repair, new Runnable() {
					@Override
					public void run() {
						if (selectedFleet == null || !(selectedFleet.location instanceof City)) { return; }
						for (Airship ship : allShips) {
							if (ship.getOriginalDesign() != null && ship.getOriginalDesign().modules.size() != ship.modules.size()) {
								repairShip((City) selectedFleet.location, ship);
							}
						}
					}
				}, canRepair && repairCost <= w.player.getMoney());
				if (canRepair) {
					d.tooltip(x, y, MyDraw.BUTTON_H, MyDraw.BUTTON_H, _t("Repair_Sx", repairCost));
				}

				x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
			d.iconToggle(x, y, compact, new Runnable() {
				@Override
				public void run() {
					w.showCompressedFleet = !w.showCompressedFleet;
				}
			}, w.showCompressedFleet, true);
			d.tooltip(x, y, MyDraw.BUTTON_H, MyDraw.BUTTON_H, _t("Show_Compact_List"));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			
			if (w.player == owner && w.map.eraModifier != null && w.map.eraModifier.expeditions != null && w.map.eraModifier.expeditions.get(w.player.bonuses) != null && !w.map.eraModifier.expeditions.get(w.player.bonuses).isEmpty()) {
				int cost = w.player.expeditionCost(getFleetSelection());
				d.iconButton(x, y, expedition, new Runnable() {
					@Override
					public void run() {
						if (selectedFleet == null || !w.player.cities.contains(selectedFleet.location)) { return; }
						JSONArray ships = new JSONArray();
						for (Airship s : getFleetSelection()) {
							ships.put(s.networkID);
						}
						w.giveCommand(msg("doExpedition").put("fleet", selectedFleet.id).put("ships", ships));
					}
				}, w.player.expedition == null && w.player.cities.contains(selectedFleet.location) && !getFleetSelection().isEmpty() && cost <= w.player.getMoney());
				d.tooltip(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("expedition_button_tooltip", getFleetSelection().size(), w.describeTime(EmpireStat.EXPEDITION_MS.get(w.player.bonuses)) + expeditionOutcomePrediction(), cost));
			}
		}
	}
	
	private String expeditionOutcomePrediction() {
		StringBuilder sb = new StringBuilder();
		int expeditionSize = w.player.expeditionStrength(getFleetSelection(), w.map, sb);
		if (expeditionSize == 0 || w.map.eraModifier == null || w.player == null) { return ""; }
		if (expeditionSize < w.map.eraModifier.expeditionLowRewardsBoundary.get(w.player.bonuses)) {
			return "\n\n" + _t("expedition_very_low_rewards_expected");
		}
		if (expeditionSize < w.map.eraModifier.expeditionMediumRewardsBoundary.get(w.player.bonuses)) {
			return "\n\n" + _t("expedition_low_rewards_expected");
		}
		if (expeditionSize < w.map.eraModifier.expeditionHighRewardsBoundary.get(w.player.bonuses)) {
			return "\n\n" + _t("expedition_medium_rewards_expected");
		}
		if (expeditionSize < w.map.eraModifier.expeditionVeryHighRewardsBoundary.get(w.player.bonuses)) {
			return "\n\n" + _t("expedition_high_rewards_expected");
		}
		return sb + "\n\n" + _t("expedition_very_high_rewards_expected");
	}
	
	void refitShip(final City c, final Airship original) {
		if (w.player == null) { return; }
		UniScreen us = new UniScreen(g, new RefitFromStrategicIntent(this, original, c));
		us.cw = w;
		us.standaloneEditShip = original.clone();
		g.s = us;
	}
	
	public void repairShip(City c, Airship ship) {
		if (w.player == null) { return; }
		if (!w.giveCommandWithSizeCheck(
				w.shipMsg("rebuildShip", ship)
				.put("newShip", Compression.compressToString(ship.getOriginalDesign().toJSON(null).toString()))
				.put("city", c.id)
				.put("constructionType", MapLocation.ConstructionEntry.Type.REPAIR.name())))
		{
			g.showError(_t("sent_construction_too_large"));
		}
	}
	
	private void intercept(Fleet target, SupplySplitMode splitSupply) {
		if (w.player == null || selectedFleet == null || selectedFleet.didIntercept) { return; }
		FleetOwner owner = w.map.owner(target);
		if (owner == w.player || owner instanceof MonsterNest) {
			doIntercept(target, splitSupply);
		} else {
			Relationship rel = w.map.getRelationship((Empire) owner, w.player);
			if (rel.level != Relationship.Level.WAR) {
				showDiplomacy = true;
				diplomacy.focusEmpire = null;
				diplomacy.showPopupOnly = true;
				diplomacy.invasionFleet = selectedFleet;
				diplomacy.interceptTarget = target;
				diplomacy.declareWarCheck((Empire) owner);
			} else {
				doIntercept(target, splitSupply);
			}
		}
	}
	
	public void doIntercept(Fleet target, SupplySplitMode splitSupply) {
		if (w.player == null) { return; }
		if (selectedFleet == null) { return; }
		if (!w.player.getFleets().contains(selectedFleet)) { return; }
		if (getFleetSelection().isEmpty()) { return; }
		if (splitSupply == null) {
			if (!getFleetSelection().containsAll(selectedFleet.getAllShips())) {
				if (selectedFleet.supply() == selectedFleet.maxSupply()) {
					splitSupply = SupplySplitMode.MAX;
				} else {
					// If not all ships are going and supply isn't full, throw up a dialog
					travelToDialogTarget = target;
					return;
				}
			} else {
				splitSupply = SupplySplitMode.MAX;
			}
		}
		JSONArray ships = new JSONArray();
		for (Airship s : getFleetSelection()) {
			ships.put(s.networkID);
		}
		w.giveCommand(msg("intercept").put("fleet", selectedFleet.id).put("ships", ships).put("target", target.id).put("splitSupply", splitSupply.name()));
	}
	
	private boolean canTravelTo(MapLocation destination) {
		if (w.player == null) { return false; }
		if (selectedFleet == null || getFleetSelection().isEmpty()) {
			return false;
		}
		if (!w.player.getFleets().contains(selectedFleet)) { return false; }
		
		boolean selectionCanFly = true;
		for (Airship ship : getFleetSelection()) {
			if (ship.type.onGround) { selectionCanFly = false; break; }
		}
		
		if (selectionCanFly) { return true; }
		
		return selectedFleet.canTravelTo(destination, w.map, w.player);
	}
	
	private void askForBesiege(City c) {
		w.giveCommand(msg("requestBesiege").put("empire", w.player.id).put("city", c.id));
	}
	
	private void dismissRequest(BesiegeRequest req) {
		w.giveCommand(msg("dismissBesiegeRequest").put("empire", w.player.id).put("from", req.from.id).put("city", req.city.id));
	}
	
	private void doBesiege(Fleet f, City c) {
		JSONObject msg = msg("sendFleet");
		msg.put("destination", c.id);
		msg.put("id", f.id);
		msg.put("besiege", true);
		JSONArray a = new JSONArray();
		for (Airship s : f.getAllShips()) {
			a.put(s.networkID);
		}
		msg.put("ships", a);
		msg.put("splitSupply", SupplySplitMode.MAX.name());
		w.giveCommand(msg);
	}

	private void travelTo(MapLocation destination, SupplySplitMode splitSupply, boolean besiege) {
		if (w.player == null) { return; }
		FleetOwner owner = w.map.owner(destination);
		if (owner == w.player || owner instanceof MonsterNest) {
			doTravelTo(destination, splitSupply, besiege);
		} else {
			Relationship rel = w.map.getRelationship((Empire) owner, w.player);
			if (rel.level == Relationship.Level.TRUCE || rel.level == Relationship.Level.PEACE) {
				showDiplomacy = true;
				diplomacy.focusEmpire = null;
				diplomacy.showPopupOnly = true;
				diplomacy.invasionFleet = selectedFleet;
				diplomacy.invasionTarget = (City) destination;
				diplomacy.invasionBesiege = besiege;
				diplomacy.declareWarCheck((Empire) owner);
				// todo: actually have the OK on that dialog send the fleet
				// and also to enforce war declarations in those circumstances at command processing level
			} else {
				doTravelTo(destination, splitSupply, besiege);
			}
		}
	}
	
	public void doTravelTo(MapLocation destination, SupplySplitMode splitSupply, boolean besiege) {
		if (w.player == null) { return; }
		if (selectedFleet == null) { return; }
		if (getFleetSelection().isEmpty()) { return; }
		if (!w.map.toggles.contains(ConquestToggle.SUPPLY)) {
			splitSupply = SupplySplitMode.MAX;
		}
		if (splitSupply == null) {
			if (!getFleetSelection().containsAll(selectedFleet.getAllShips())) {
				if (selectedFleet.supply() == selectedFleet.maxSupply()) {
					splitSupply = SupplySplitMode.MAX;
				} else {
					// If not all ships are going and supply isn't full, throw up a dialog
					travelToDialogDestination = destination;
					travelToDialogBesiege = besiege;
					return;
				}
			} else {
				splitSupply = SupplySplitMode.MAX;
			}
		}
		JSONObject msg = msg("sendFleet");
		msg.put("destination", destination.id);
		msg.put("id", selectedFleet.id);
		msg.put("besiege", besiege);
		JSONArray a = new JSONArray();
		for (Airship s : getFleetSelection()) {
			a.put(s.networkID);
		}
		msg.put("ships", a);
		msg.put("splitSupply", splitSupply.name());
		w.giveCommand(msg);
		selectedFleet = null;
	}
	
	private void renderSupplySplitDialog(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		// as much as possible
		// as little as possible
		// enough to get there and back
		// enough to get there and back to home territory
		// a fair split of the remainder
		if (w.player == null || selectedFleet == null || getFleetSelection().isEmpty()) {
			travelToDialogDestination = null;
			travelToDialogTarget = null;
			return;
		}
		//Fleet f2 = selectedFleet.split(w.map, getFleetSelection(), selectedFleet.supply(), /* experimentally */ true);
		final int supplyNeeded;
		
		Pt icept = null;
		if (travelToDialogDestination != null) {
			supplyNeeded = w.player.moveSupplyCost(selectedFleet, getFleetSelection(), travelToDialogDestination, null, w.map);
		} else {
			icept = selectedFleet.getFlightIntercept(travelToDialogTarget, getFleetSelection(), w.map);
			if (icept == null) {
				travelToDialogTarget = null;
				return;
			}
			supplyNeeded = w.player.moveSupplyCost(selectedFleet, getFleetSelection(), null, icept, w.map);
		}
		
		final int extraSupplyPossible = StrictMath.min(selectedFleet.supply() - supplyNeeded, Fleet.maxSupply(getFleetSelection()) - supplyNeeded);
		if (extraSupplyPossible < 0) {
			travelToDialogTarget = null;
			travelToDialogDestination = null;
			return;
		}
		
		String text;
		if (travelToDialogDestination != null) {
			text = _t("supplySplitTravelTo", getFleetSelection().size(), travelToDialogDestination.getDisplayName(), extraSupplyPossible);
		} else {
			text = _t("supplySplitIntercept", getFleetSelection().size(), extraSupplyPossible);
		}
		if (travelToDialogDestination == null || (!w.player.cities.contains(travelToDialogDestination) && !(travelToDialogDestination instanceof MonsterNest && ((MonsterNest) travelToDialogDestination).type == null))) {
			int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(selectedFleet, getFleetSelection(), travelToDialogDestination, icept, w.map);
			int additional = moveAndReturnSupplyCost - supplyNeeded;
			if (moveAndReturnSupplyCost <= selectedFleet.supply()) {
				if (moveAndReturnSupplyCost <= Fleet.maxSupply(getFleetSelection())) {
					text += "\n\n" + _t("supplyCanCarryAdditionalSupplies", additional);
				} else {
					text += "\n\n" + _t("supplyCannotCarryAdditionalSupplies", additional);
				}
			} else {
				text += "\n\n" + _t("supplyInsufficientSupplies", additional);
			}
		}
		
		text += "\n\n" + _t("howManySupplies");
		
		SupplySplitMode defSSM = SupplySplitMode.getDefault(w.player, selectedFleet, getFleetSelection(), travelToDialogDestination, icept, w.map);
		int width = sm.width / 4;
		int visibleSSMs = 0;
		for (SupplySplitMode ssm : SupplySplitMode.values()) {
			if (ssm.isVisible(w.player, selectedFleet, getFleetSelection(), travelToDialogDestination, icept, w.map)) {
				width = Math.max(d.bw(_t("supplySplitMode_" + ssm.name()), ssm == defSSM ? "ENTER" : null), width);
				visibleSSMs++;
			}
		}
		
		width = Math.max(d.bw("Cancel", "ESCAPE"), width);
		int titleW = (int) d.textSize(_t("Supplies_Allocation"), AGame.BIGGER_FOUNT).x;
		width = Math.max(titleW, width);
		Rect textSz = d.textSize(text, AGame.FOUNT, 0, 0, width);
		width += MyDraw.WINDOW_INSET * 2;
		int h = MyDraw.WINDOW_INSET * 2 + AGame.BIGGER_FOUNT.lineHeight + (int) textSz.height + MyDraw.UI_SPACING + visibleSSMs * (MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING) + MyDraw.BUTTON_H;
		int x = sm.width / 2 - width / 2;
		int y = sm.height / 2 - h / 2;
		d.drawShadowedWindow(x, y, width, h, 7);
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		width -= MyDraw.WINDOW_INSET * 2;
		d.text(_t("Supplies_Allocation"), AGame.BIGGER_FOUNT, sm.width / 2 - titleW / 2, y);
		y += AGame.BIGGER_FOUNT.lineHeight;
		d.text(text, AGame.FOUNT, x, y, width);
		y += textSz.height + MyDraw.UI_SPACING;
		for (final SupplySplitMode ssm : SupplySplitMode.values()) {
			if (!ssm.isVisible(w.player, selectedFleet, getFleetSelection(), travelToDialogDestination, icept, w.map)) { continue; }
			d.button(x, y, width, _t("supplySplitMode_" + ssm.name()), ssm == defSSM ? "ENTER" : null, new Runnable() {
				@Override
				public void run() {
					if (travelToDialogDestination != null) {
						travelTo(travelToDialogDestination, ssm, travelToDialogBesiege);
					} else {
						doIntercept(travelToDialogTarget, ssm);
					}
					travelToDialogDestination = null;
					travelToDialogTarget = null;
				}
			}, ssm.isAvailable(w.player, selectedFleet, getFleetSelection(), travelToDialogDestination, icept, w.map));
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		}
		d.button(x, y, width, _t("Cancel"), "ESCAPE", new Runnable() {
			@Override
			public void run() {
				travelToDialogDestination = null;
				travelToDialogTarget = null;
			}
		});
	}
		
	private void teleportTo(MapLocation destination) {
		if (w.player == null) { return; }
		if (selectedFleet == null) { return; }
		if (getFleetSelection().isEmpty()) { return; }
		selectedFleet.fleeDestinationNeeded = false;
		selectedFleet.location = destination;
		selectedFleet.destination = null;
		selectedFleet.road = null;
		selectedFleet.isLimpHome = false;
		selectedFleet.broadcastArrived(w.map);
		selectedFleet = null;
		w.usedCheatCommand = true;
	}

	private void renderMenu(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		int width = 100;
		width = StrictMath.max(width, d.bw(_t("Save")));
		width = StrictMath.max(width, d.bw(_t("Open")));
		width = StrictMath.max(width, d.bw(_t("Quit")));
		width = StrictMath.max(width, d.bw(_t("Back_to_game")));
		width = StrictMath.max(width, d.bw(_t("Settings")));
		width = StrictMath.max(width, d.bw(_t("Key_Configuration")));
		width += MyDraw.WINDOW_INSET * 2;
		int numButtons = 6;
		if (saveIODir != null) {
			numButtons++;
		}
		int height = numButtons * (MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING + MyDraw.WINDOW_INSET * 2;
		int x = sm.width / 2 - width / 2;
		int y = sm.height / 2 - height / 2;
		d.drawShadowedWindow(x, y, width, height, 1);
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		width -= MyDraw.WINDOW_INSET * 2;
		final StrategicScreen ss = this;
		d.button(x, y, width, _t("Settings"), null, new Runnable() {
			@Override
			public void run() {
				g.s = new SettingsScreen(g);
				((SettingsScreen) g.s).returnTo = ss;
			}
		});
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		d.button(x, y, width, _t("Key_Configuration"), null, new Runnable() {
			@Override
			public void run() {
				g.s = new KeyConfigScreen(g, ss);
			}
		});
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		d.button(x, y, width, _t("Save"), Keys.getText("strategic_menu_save", "V", false), new Runnable() {
			@Override
			public void run() {
				if (Monkey.active) {
					monkeySave();
				} else {
					save();
				}
			}
		}, w.combatInfo == null);
		if (w.combatInfo != null) {
			d.tooltip(x, y, width, MyDraw.BUTTON_H, _t("Cannot_save_during_combat"));
		}
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		if (saveIODir != null && !Monkey.active) {
			d.button(x, y, width, _t("Save_As"), null, new Runnable() {
				@Override
				public void run() {
					saveAs();
				}
			}, w.combatInfo == null);
			if (w.combatInfo != null) {
				d.tooltip(x, y, width, MyDraw.BUTTON_H, _t("Cannot_save_during_combat"));
			}
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		}
		if (w.isMultiplayer()) {
			d.button(x, y, width, _t("Surrender"), null, new Runnable() {
				@Override
				public void run() {
					surrender();
				}
			}, w.player != null);
		} else {
			d.button(x, y, width, _t("Open"), Keys.getText("strategic_menu_open", "O", false), new Runnable() {
				@Override
				public void run() {
					if (Monkey.active) {
						monkeyOpen();
					} else {
						open(false);
					}
				}
			});
		}
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		d.button(x, y, width, _t("Quit"), Keys.getText("strategic_menu_quit", "Q", false), new Runnable() {
			@Override
			public void run() {
				checkedLeave();
			}
		});
		y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		d.button(x, y, width, _t("Back_to_game"), Keys.getText("strategic_menu_back_to_game", "B", false), new Runnable() {
			@Override
			public void run() {
				menu = false;
			}
		});
		if (CampaignWorld.MAGIC_WIN_ENABLED) {
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			d.button(x, y, width, "Win", null, new Runnable() {
				@Override
				public void run() {
					if (w.player == null) { return; }
					w.giveCommand(msg("magicWinStrategic").put("empire", w.player.id));
				}
			}, w.player != null);
		}
		if (CampaignWorld.MAGIC_WIN_ENABLED) {
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			d.button(x, y, width, "Lose", null, new Runnable() {
				@Override
				public void run() {
					if (w.player == null) { return; }
					w.giveCommand(msg("magicLoseStrategic").put("empire", w.player.id));
				}
			}, w.player != null);
		}
	}
	
	private void renderConquerorInfo(MyDraw d, ScreenMode sm) {
		int armsSize = 32;
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL: armsSize = 32; break;
			case MEDIUM: armsSize = 64; break;
			case LARGE: armsSize = 64; break;
		}
		City c = w.player.conquerorInfo.a.get(0).city;
		String title = _t("city_awarded", c.getDisplayName(), w.player.conquerorInfo.b.getName());
		int titleWidth = armsSize + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING + (int) d.textSize(title, AGame.BIGGER_FOUNT).x;
		int baseHeight = Math.max(armsSize + MyDraw.PANEL_BORDER_W * 2, AGame.BIGGER_FOUNT.lineHeight);
		baseHeight += MyDraw.UI_SPACING * 2 + MyDraw.BUTTON_H;
		int entryWidth = (int) d.textSize(_t("claim_fleet_strength"), AGame.FOUNT).x;
		entryWidth = Math.max(entryWidth, (int) d.textSize(_t("city_distance_strength"), AGame.FOUNT).x);
		entryWidth = Math.max(entryWidth, (int) d.textSize(_t("original_owner_strength"), AGame.FOUNT).x);
		entryWidth = Math.max(entryWidth, (int) d.textSize(_t("previously_awarded_claim_strength"), AGame.FOUNT).x);
		entryWidth = Math.max(entryWidth, (int) d.textSize(_t("claim_total_and_best"), AGame.FOUNT).x);
		entryWidth = Math.max(entryWidth, (int) d.textSize(_t("claim_total"), AGame.FOUNT).x);
		entryWidth += MyDraw.UI_SPACING + AGame.FOUNT.displayWidth * 2; // Spacing and number
		for (CityClaim cc : w.player.conquerorInfo.a) {
			entryWidth = Math.max(entryWidth, (int) d.textSize(cc.claimant.getName(), AGame.BIG_FOUNT).x);
		}
		entryWidth += 32 + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING;
		int width = Math.max(titleWidth, entryWidth + ScrollBar.SCROLL_BAR_W) + MyDraw.WINDOW_INSET * 2;
		int height = Math.max(baseHeight + 200, sm.height / 2) + MyDraw.WINDOW_INSET * 2;
		int x = (int) (worldToScreenX(c.x)) - width - 16 - MyDraw.UI_SPACING;
		int y = (int) (worldToScreenY(c.y)) - height / 2;
		d.drawShadowedWindow(x, y, width, height, 4);
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		width -= MyDraw.WINDOW_INSET * 2;
		height -= MyDraw.WINDOW_INSET * 2;
		w.player.conquerorInfo.b.arms.draw(d, x + MyDraw.PANEL_BORDER_W, y + MyDraw.PANEL_BORDER_W, armsSize);
		d.drawPanelBorder(x, y, armsSize + MyDraw.PANEL_BORDER_W * 2, armsSize + MyDraw.PANEL_BORDER_W * 2);
		d.text(title, AGame.BIGGER_FOUNT, x + armsSize + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING, y);
		y += Math.max(armsSize + MyDraw.PANEL_BORDER_W * 2, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING;
		int sbHeight = height - baseHeight;
		conquerorSBR.x = x;
		conquerorSBR.y = y;
		conquerorSBR.w = width;
		conquerorSBR.h = sbHeight;
		conquerorSB.draw(d, x, y, width, sbHeight, w.player.conquerorInfo.a, new CityClaimAdapter(w.player.conquerorInfo, this));
		y += sbHeight + MyDraw.UI_SPACING;
		d.button(x, y, width, _t("OK"), new Runnable() {
			@Override
			public void run() {
				w.player.conquerorInfo = null;
			}
		});
	}
	
	private void renderLocationMoveMenu(final MapLocation c, MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		if (w.player == null || !w.player.getFleets().contains(selectedFleet)) { return; }
		if (c instanceof MonsterNest && ((MonsterNest) c).type == null) { return; } // Stop exploit.
		int x = (int) (worldToScreenX(c.x)) - MyDraw.ICON_BUTTON_SZ / 2;//(c instanceof City ? 16 : 9);
		int y = (int) (worldToScreenY(c.y)) + 13;//(c instanceof City ? 25 : 13);
		
		if (selectedFleet.location == c && c instanceof City && selectedFleet.besiege && w.map.owner((City) c) != w.player && w.map.getRelationship(w.map.owner((City) c), w.player).level == Relationship.Level.WAR) {
			final Fleet f = selectedFleet;
			String text = _t("Attack");
			int width = d.bw(text);
			if (checked(sm, x, y, width, MyDraw.BUTTON_H)) {
				d.button(x, y, width, text, new Runnable() {
					@Override
					public void run() {
						w.giveCommand(msg("stopSiege").put("id", f.id));
					}
				});
				Empire victim = c instanceof City ? (Empire) w.map.owner(c) : null;
				String heroText = Hero.getStatChangeAppendix(w.player, HeroEvent.combatVictory(w.player, victim), w.map, false) +
					Hero.getStatChangeAppendix(w.player, HeroEvent.combatDefeat(w.player, victim), w.map, false);
				if (!heroText.isEmpty()) {
					d.tooltip(x, y, width, MyDraw.BUTTON_H, heroText);
				}
			}
			ArrayList<Empire> besiegeHelpers = w.player.canHelpBesiegeAllies((City) c, w);
			if (!besiegeHelpers.isEmpty()) {
				d.iconButton(x, y + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING, askAlliesToBesiege, new Runnable() {
					@Override
					public void run() {
						askForBesiege((City) c);
					}
				}, true);
				d.tooltip(x, y + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("besiege_request", Empire.nameList(besiegeHelpers, Lang.currentLocale), c.getDisplayName()));
			}
			return;
		}
		if (selectedFleet.location == c && c instanceof City && w.map.isUnderSiege((City) c) && (w.map.owner((City) c) == w.player || w.map.getRelationship(w.map.owner((City) c), w.player).level == Relationship.Level.ALLIANCE)) {
			final Fleet f = selectedFleet;
			String text = _t("Break_Out");
			int width = d.bw(text);
			if (checked(sm, x, y, width, MyDraw.BUTTON_H)) {
				d.button(x, y, width, text, new Runnable() {
					@Override
					public void run() {
						w.giveCommand(msg("breakOut").put("id", f.id));
					}
				});
				String tooltip = _t("break_out_explanation", c.getDisplayName());
				String heroText = "";
				for (Empire besieger : w.map.besiegingEmpires((City) c)) {
					heroText += Hero.getStatChangeAppendix(w.player, HeroEvent.combatVictory(w.player, besieger), w.map, false) +
					Hero.getStatChangeAppendix(w.player, HeroEvent.combatDefeat(w.player, besieger), w.map, false);
				}
			
				if (!heroText.isEmpty()) {
					tooltip += "\n" + heroText;
				}
				d.tooltip(x, y, width, MyDraw.BUTTON_H, tooltip);
			}
			return;
		}
		
		if (!checked(sm, x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ)) {
			return;
		}
		if (selectedFleet.location != c && !_fleetSelection.isEmpty() && canTravelTo(c) && selectedFleet.destination != c) {
			FleetOwner def = w.map.defender(c);
			boolean invade = def != w.map.owner(selectedFleet);
			if (def instanceof MonsterNest && ((MonsterNest) def).type == null) {
				invade = false;
			}
			if (def instanceof Empire && w.map.getRelationship(w.player, (Empire) def).level.ordinal() >= Relationship.Level.NON_AGGRESSION_PACT.ordinal()) {
				invade = false;
			}
			boolean besiege = invade && !(c instanceof MonsterNest);
			ArrayList<Empire> besiegeHelpers = besiege ? w.player.canHelpBesiegeAllies((City) c, w) : new ArrayList<Empire>();
			d.hook(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.HOVER) {
				@Override
				public void run(Input in, Pt p, Type type) {
					if (selectedFleet == null) { return; }
					highlitTravelConnection = c;
					highlitTravelConnectionBesiege = false;
					highlitTravelConnectionTime = 0;
				}
			});
			if (besiege) {
				d.hook(x, y + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.HOVER) {
					@Override
					public void run(Input in, Pt p, Type type) {
						if (selectedFleet == null) { return; }
						highlitTravelConnection = c;
						highlitTravelConnectionBesiege = true;
						highlitTravelConnectionTime = 0;
					}
				});
			}
			if (w.map.toggles.contains(ConquestToggle.SUPPLY)) {
				int moveSupplyCost = w.player.moveSupplyCost(selectedFleet, _fleetSelection, c, null, w.map);
				if (moveSupplyCost > StrictMath.min(Fleet.maxSupply(_fleetSelection), selectedFleet.supply())) {
					d.colouredIconButton(x, y, invade ? cannotAttack : cannotMove, new Runnable() {
						@Override
						public void run() {
							// Do nuffink! Nuffink!
						}
					}, false);
				} else {
					Img icon;
					if (invade) {
						int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(selectedFleet, _fleetSelection, c, null, w.map);
						if (moveAndReturnSupplyCost > StrictMath.min(selectedFleet.supply(), Fleet.maxSupply(getFleetSelection()))) {
							icon = canAttackButNotReturn;
						} else {
							icon = canAttackAndReturn;
						}
					} else {
						icon = canMove;
						if (w.player.isLimpHome(selectedFleet, getFleetSelection(), c, w.map)) {
							icon = limpHome;
						}
					}

					d.colouredIconButton(x, y, icon, new Runnable() {
						@Override
						public void run() {
							travelTo(c, null, false);
						}
					}, true);
					d.highlight("travelTo-" + c.id, g, x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ);
					
					if (besiege) {
						d.iconButton(x, y + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, canBesiege, new Runnable() {
							@Override
							public void run() {
								travelTo(c, null, true);
							}
						}, true);
						
						if (!besiegeHelpers.isEmpty()) {
							d.iconButton(x, y + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, askAlliesToBesiege, new Runnable() {
								@Override
								public void run() {
									askForBesiege((City) c);
								}
							}, true);
							d.tooltip(x, y + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("besiege_request", Empire.nameList(besiegeHelpers, Lang.currentLocale), c.getDisplayName()));
						}
					}
				}
			} else {
				d.colouredIconButton(x, y, canMove, new Runnable() {
					@Override
					public void run() {
						travelTo(c, null, false);
					}
				}, true);
				
				if (besiege) {
					d.iconButton(x, y + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, canBesiege, new Runnable() {
						@Override
						public void run() {
							travelTo(c, null, true);
						}
					}, true);
					
					if (!besiegeHelpers.isEmpty()) {
						d.iconButton(x, y + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, askAlliesToBesiege, new Runnable() {
							@Override
							public void run() {
								askForBesiege((City) c);
							}
						}, true);
						d.tooltip(x, y + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("besiege_request", Empire.nameList(besiegeHelpers, Lang.currentLocale), c.getDisplayName()));
					}
				}
			}
			
			if (SimplePref.CHEATS.get() && !w.isMultiplayer()) {
				d.button(x, y + MyDraw.BUTTON_H * 2+ MyDraw.BUTTON_SPACING * 2, d.bw(_t("Teleport_Cheat")), _t("Teleport_Cheat"), new Runnable() {
					@Override
					public void run() {
						teleportTo(c);
						w.usedCheatCommand = true;
					}
				});
				d.tooltip(x, y + MyDraw.BUTTON_H * 2 + MyDraw.BUTTON_SPACING * 2, d.bw(_t("Teleport_Cheat")), MyDraw.BUTTON_H, _t("Teleport_Cheat_tooltip"));
			}
		}
	}
	
	private void renderLocationMoveInfo(final MapLocation c, boolean forBesiege, MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		int x = (int) (worldToScreenX(c.x)) - MyDraw.ICON_BUTTON_SZ / 2;//(c instanceof City ? 16 : 9);
		int y = (int) (worldToScreenY(c.y)) + 13;//(c instanceof City ? 25 : 13);
		if (forBesiege) {
			y += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
		}
		if (!checked(sm, x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ)) {
			return;
		}
		if (selectedFleet.location != c && !_fleetSelection.isEmpty() && canTravelTo(c) && selectedFleet.destination != c) {
			String extraText = "";
			String travelTime = "";
			FleetOwner def = w.map.defender(c);
			boolean invade = def != w.map.owner(selectedFleet);
			if (def instanceof MonsterNest && ((MonsterNest) def).type == null) {
				invade = false;
			}
			if (def instanceof Empire && w.map.getRelationship(w.player, (Empire) def).level.ordinal() >= Relationship.Level.NON_AGGRESSION_PACT.ordinal()) {
				invade = false;
			}
			String text = "?";
			if (w.map.toggles.contains(ConquestToggle.SUPPLY)) {
				boolean limp = !invade && w.player.isLimpHome(selectedFleet, _fleetSelection, c, w.map);
				int moveSupplyCost = w.player.moveSupplyCost(selectedFleet, _fleetSelection, c, null, w.map);
				if (moveSupplyCost > selectedFleet.supply()) {
					extraText = _t("Insufficient_supply_to_move");
				} else if (moveSupplyCost > Fleet.maxSupply(_fleetSelection)) {
					extraText = _t("Insufficient_supply_storage_to_move") + " (" +  Fleet.maxSupply(_fleetSelection) + " / " + moveSupplyCost + ")";
				} else {
					int t = selectedFleet.timeTo(c, w.map, _fleetSelection, limp);
					if (t > -1) {
						travelTime = w.describeTime(t) + "\n";
					}
					if (invade) {
						int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(selectedFleet, _fleetSelection, c, null, w.map);
						if (moveAndReturnSupplyCost > selectedFleet.supply()) {
							extraText = _t("Sufficient_supply_to_attack") + "\n" + _t("Insufficient_supply_to_return") + " (" + moveAndReturnSupplyCost + " / " + selectedFleet.supply() + ")";
						} else if (moveAndReturnSupplyCost > Fleet.maxSupply(_fleetSelection)) {
							extraText = _t("Sufficient_supply_to_attack") + "\n" + _t("Insufficient_supply_storage_to_return") + " (" + moveAndReturnSupplyCost + " / " + Fleet.maxSupply(_fleetSelection) + ")";
						} else {
							extraText = _t("Sufficient_supply_to_attack") + "\n" + _t("Sufficient_supply_to_return");
						}
					} else {
						extraText = _t("Sufficient_supply_to_move");
						if (limp) {
							extraText = _t("Limp_back_to_the_nearest_safe_port");
						}
					}
				}

				text = travelTime + extraText + "\n" + _t("x_supply_per_week", (c.getResupplySpeed(w.player, w.map, null) / 2)) + "\n\n" + w.player.moveSupplyCostExplanation(selectedFleet, _fleetSelection, c, null, w.map);
			} else {
				int t = selectedFleet.timeTo(c, w.map, false);
				if (t > -1) {
					text = w.describeTime(t);
				}
			}
			if (forBesiege) {
				text = _t("besiege_explanation", c.getDisplayName()) + "\n\n" + text;
			}
			if (c instanceof City && !((City) c).isConnectedToCapital(w.player, w.map)) {
				text += "\n\n" + _t("city_will_be_disconnected", c.getDisplayName(), w.player.getCapital().getDisplayName(), EmpireStat.DISCONNECTED_FROM_CAPITAL_UNREST.explain(w.player.bonuses));
			}
			String statChangeAppendix = "";
			if (invade) {
				if (c instanceof City) {
					statChangeAppendix +=
						Hero.getStatChangeAppendix(w.player, HeroEvent.combatVictory(w.player, w.map.owner((City) c)), w.map, false) +
						Hero.getStatChangeAppendix(w.player, HeroEvent.combatDefeat(w.player, w.map.owner((City) c)), w.map, false);
					statChangeAppendix += Hero.getStatChangeAppendix(w.player, HeroEvent.cityGained(w.player, (City) c), w.map, false);
				}
				if (c instanceof MonsterNest && ((MonsterNest) c).type != null) {
					statChangeAppendix +=
						Hero.getStatChangeAppendix(w.player, HeroEvent.combatVictory(w.player, null), w.map, false) +
						Hero.getStatChangeAppendix(w.player, HeroEvent.combatDefeat(w.player, null), w.map, false);
					statChangeAppendix += Hero.getStatChangeAppendix(w.player, HeroEvent.nestDestroyed(w.player, ((MonsterNest) c).type), w.map, false);
				}
			}
			if (!statChangeAppendix.isEmpty()) {
				text += "\n" + statChangeAppendix;
			}
			int ttw = AGame.FOUNT.displayWidth * 20 + 150;
			Rect ts = d.textSize(text, AGame.FOUNT, 0, 0, ttw);
			d.drawPanel(x, y + MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING, (int) ts.width + MyDraw.PANEL_INSET * 2, (int) ts.height + MyDraw.PANEL_INSET * 2, 19);
			d.text(text, AGame.FOUNT, x + MyDraw.PANEL_INSET, y + MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING + MyDraw.PANEL_INSET, ttw);
		}
	}
	
	private boolean fullCityMenuVisible() {
		return menuCity != null && w.player != null && (w.player.cities.contains(menuCity) || w.player.getSpyFor(menuCity) != null);
	}
	
	private void renderCityMenu(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		final City c = menuCity;
		if (c == null) { return; }
		final StrategicScreen ss = this;
		int x = (int) (worldToScreenX(c.x)) - 16;
		int y = (int) (worldToScreenY(c.y)) + 25;
		int width = 120;
		width = StrictMath.max(width, d.bw(_t("Send_Spy_Sx", "7", "7"), Keys.getText("strategic_city_spy", "P", false)));
		width = StrictMath.max(width, d.bw(_t("View_City"), "V"));
		width = StrictMath.max(width, d.bw(_t("Diplomacy")));
		
		// qqDPS
		/*d.button(x, y - 100, 100, "Diplo Overview", new Runnable() {
			@Override
			public void run() {
				System.out.println(DiplomacyAI.getDecisionsOverview(w.map.empires, w.map, AGame.ANIM_R));
			}
		});
		d.button(x, y - 60, 100, "Diplo Debug", new Runnable() {
			@Override
			public void run() {
				//System.out.println(DiplomacyAI.getSummary(w.map.owner(c), w.map));
				System.out.println(w.map.getRelationship(w.map.owner(c), w.player).a.name + " to " + w.map.getRelationship(w.map.owner(c), w.player).b.name + ": " + w.map.owner(c).diplomacyAI.records.get(w.player).tributeAgreeCooldowns.get(Relationship.Direction.A_TO_B));
				System.out.println(w.map.getRelationship(w.map.owner(c), w.player).b.name + " to " + w.map.getRelationship(w.map.owner(c), w.player).a.name + ": " + w.map.owner(c).diplomacyAI.records.get(w.player).tributeAgreeCooldowns.get(Relationship.Direction.B_TO_A));
				System.out.println("neither: " + w.map.owner(c).diplomacyAI.records.get(w.player).tributeAgreeCooldowns.get(Relationship.Direction.NEITHER));

			}
		});*/
		
		if (w.player == null) {
			d.button(x, y, width, _t("View_City"), "V", new Runnable() {
				@Override
				public void run() {
					UniScreen us = new UniScreen(g, new ViewCityIntent(ss));
					us.cw = w;
					us.wm = w.map;
					us.city = menuCity;
					g.s = us;
				}
			});
			return;
		}
		
		if (w.player.cities.contains(c) && c.takeoverNeeded) {
			cityDetailWidth = cityDetail.getWidth(d, c) + ScrollBar.SCROLL_BAR_W + MyDraw.WINDOW_INSET;
			d.drawRightSideWindow(sm, sm.width - cityDetailWidth, MyDraw.TOP_BAR_H);
			d.hook(sm.width - cityDetailWidth, MyDraw.TOP_BAR_H, cityDetailWidth, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
				@Override
				public void run(Input input, Pt pt, Type type) {
					// Ignore
				}
			});
			d.state.removeIntersectingGlowRects(sm.width - cityDetailWidth, MyDraw.TOP_BAR_H, cityDetailWidth, sm.height);
			takeoverScrollBar.drawNaked(d, sm.width - cityDetailWidth + MyDraw.WINDOW_INSET - MyDraw.BUTTON_SPACING, MyDraw.TOP_BAR_H + MyDraw.BUTTON_SPACING, cityDetailWidth - MyDraw.WINDOW_INSET, sm.height - MyDraw.TOP_BAR_H - MyDraw.BUTTON_SPACING - MyDraw.BUTTON_SPACING, TakeoverMethod.getAvailable(w.player.bonuses), new TakeoverOptionsAdapter(c, w));
			return;
		}
		
		if (!w.player.cities.contains(c) && w.player.getSpyFor(c) == null && !w.player.spyCanViewCity(c, w.map)) {
			if (w.has(ConquestToggle.DIPLOMACY)) {
				d.button(x, y, width, _t("Diplomacy"), null, new Runnable() {
					@Override
					public void run() {
						if (w.player == null) { return; }
						diplomacy.showPopupOnly = false;
						diplomacy.focusEmpire = w.map.owner(c);
						showDiplomacy = true;
					}
				});
				y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			d.button(x, y, width, _t("Send_Spy_Sx", w.player.spies.size() + 1, EmpireStat.MAX_SPIES.get(w.player.bonuses)), Keys.getText("strategic_city_spy", "P", false), new Runnable() {
				@Override
				public void run() {
					if (w.player == null) { return; }
					w.giveCommand(msg("sendSpy").put("empire", w.player.id).put("city", c.id));
				}
			}, w.player.spies.size() < EmpireStat.MAX_SPIES.get(w.player.bonuses));
			d.highlight("sendSpy", g, x, y, width, MyDraw.BUTTON_H);
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			return;
		}
		
		cityDetailWidth = cityDetail.getWidth(d, c) + ScrollBar.SCROLL_BAR_W + MyDraw.WINDOW_INSET;
		d.drawRightSideWindow(sm, sm.width - cityDetailWidth, MyDraw.TOP_BAR_H);
		d.hook(sm.width - cityDetailWidth, MyDraw.TOP_BAR_H, cityDetailWidth, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
			@Override
			public void run(Input input, Pt pt, Type type) {
				// Ignore
			}
		});
		d.state.removeIntersectingGlowRects(sm.width - cityDetailWidth, MyDraw.TOP_BAR_H, cityDetailWidth, sm.height);
		cityScrollBar.drawNaked(d, sm.width - cityDetailWidth + MyDraw.WINDOW_INSET - MyDraw.BUTTON_SPACING, MyDraw.TOP_BAR_H + MyDraw.BUTTON_SPACING, cityDetailWidth - MyDraw.WINDOW_INSET, sm.height - MyDraw.TOP_BAR_H - MyDraw.BUTTON_SPACING - MyDraw.BUTTON_SPACING, Collections.singletonList(menuCity), cityDetail);
	}
	
	public void sendChat() {
		if (!chatField.getText().isEmpty()) {
			if (chatForAllies) {
				JSONArray recipients = new JSONArray();
				recipients.put(w.player.id);
				for (Empire e : w.player.getHumanAllies(w.map)) {
					recipients.put(e.id);
				}
				g.sendMessage(msg("chatForAllies").put("text", chatField.getText()).put("id", g.playerID()).put("recipientEmpires", recipients));
			} else {
				g.sendMessage(msg("chat").put("text", chatField.getText()).put("id", g.playerID()));
			}
			chatField.setText("");
		}
		chatField.focus = false;
	}

	public void startUpgrade(CityUpgradeType cut) {
		if (w.player == null) { return; }
		w.giveCommand(msg("cityUpgrade").put("city", menuCity.id).put("upgrade", cut.name));
	}
	
	public void instabuildUpgrade(CityUpgradeType cut) {
		if (w.player == null) { return; }
		w.giveCommand(msg("instabuildUpgrade").put("city", menuCity.id).put("upgrade", cut.name));
	}
	
	public void destroyUpgrade(CityUpgradeType cut) {
		if (w.player == null) { return; }
		w.giveCommand(msg("destroyCityUpgrade").put("city", menuCity.id).put("upgrade", cut.name));
	}
	
	void buildSavedShip(ShipType st) {
		if (w.player == null) { return; }
		ShipHelperWidget shw = ShipHelperWidget.get(st, w.player.arms, w.player.bonuses(), true, null, w.player.getMoney(), menuCity.production(w.player, w.map), w.isMultiplayer());
		ConstructionBackend cbe = ShipEditorUtils.shipsList(st, shw);
		FileScreen fs = new FileScreen(
				cbe,
				new BuildShipMission(g, this, st, cbe),
				shw,
				new PriceInlineInfo(shw, null),
				st.name());
		fs.enabledFilter = new AffordableFilter(w.player.getMoney(), shw, null);
		fs.backgroundTask = this;
		g.s = fs;
	}

	public void buildWithoutNaming(final Airship a) {
		if (w.player == null) { return; }
		if (menuCity == null) { return; }
		a.setBaseBonuses(w.player.bonuses());
		a.repair(/* resetXP */ true);
		a.updateOriginalDesign();
		if (!w.giveCommandWithSizeCheck(msg("buildShip").put("ship", Compression.compressToString(a.toJSON(null).toString())).put("city", menuCity.id))) {
			g.showError(_t("sent_construction_too_large"));
		}
	}

	public void build(final Airship a) {
		if (w.player == null) { return; }
		if (menuCity == null) { return; }
		a.setBaseBonuses(w.player.bonuses());
		a.repair(/* resetXP */ true);
		a.updateOriginalDesign();
		shipNameDialog = new ShipNameDialog(w, a, w.player, menuCity.production(w.player, w.map), new ShipNameDialog.ShipNameCallback() {
			@Override
			public void run(ArrayList<String> names) {
				String orig = a.getName();
				for (int i = 0; i < names.size(); i++) {
					a.setName(names.get(i));
					if (!w.giveCommandWithSizeCheck(msg("buildShip").put("ship", Compression.compressToString(a.toJSON(null).toString())).put("city", menuCity.id))) {
						g.showError(_t("sent_construction_too_large"));
					}
				}
				shipNameDialog = null;
				a.setName(orig);
			}
			
			@Override
			public boolean canHaveMultiple() {
				return true;
			}

			@Override
			public boolean multipleAllowed(int n) {
				return n > 0 && a.getCost() * n <= w.player.getMoney();
			}
		},
		new Runnable() {
			@Override public void run() {
				shipNameDialog = null;
			}
		});
	}
	
	public void rebuild(City c, Airship original, Airship newShip, boolean keepOldPosition) {
		if (w.player == null) { return; }
		newShip.setBaseBonuses(w.player.bonuses());
		if (!w.giveCommandWithSizeCheck(
				w.shipMsg("rebuildShip", original)
				.put("keepOldPosition", keepOldPosition)
				.put("newShip", Compression.compressToString(newShip.toJSON(null).toString()))
				.put("city", c.id).put("constructionType", MapLocation.ConstructionEntry.Type.REFIT.name())))
		{
			g.showError(_t("sent_construction_too_large"));
		}
	}

	void createShip(ShipType st) {
		if (w.player == null) { return; }
		UniScreen us = new UniScreen(g, new DesignFromStrategicIntent(this));
		us.cw = w;
		us.standaloneEditShip = new Airship(st);
		us.standaloneEditShip.setName(ConstructionName.getName(w.player, w.map.lang, st, AGame.ANIM_R));
		us.standaloneEditShip.setBaseBonuses(w.player.bonuses());
		g.s = us;
	}
	
	void cancelShip(MapLocation.ConstructionEntry ce) {
		if (w.player == null) { return; }
		if (!menuCity.constructing.contains(ce)) { return; }	
		w.giveCommand(msg("cancelShipConstruction").put("city", menuCity.id).put("id", ce.id));
	}
	
	void reorderConstruction(MapLocation.ConstructionEntry ce, int shift) {
		if (w.player == null) { return; }
		if (!menuCity.constructing.contains(ce)) { return; }	
		w.giveCommand(msg("reorderConstructionQueue").put("city", menuCity.id).put("id", ce.id).put("shift", shift));
	}
	
	/*private void doEdit(ShipType st) {
		if (!mpEditors.containsKey(st)) {
			UniScreen us = new UniScreen(g, new MultiplayerEditShipIntent(this));
			us.standaloneEditShip = new Airship(st, w.map.lang, AGame.ANIM_R);
			us.standaloneEditShip.constructionBonuses = w.player.bonuses();
			us.standaloneEditShip.currentBonuses = w.player.bonuses();
			us.cw = w;
			mpEditors.put(st, us);
		}
		g.s = mpEditors.get(st);
	}*/
	
	void defenses() {
		UniScreen us = new UniScreen(g, new DefencesIntent(this));
		us.city = menuCity;
		us.wm = w.map;
		us.cw = w;
		g.s = us;
	}
	
	private boolean hasSpy(City c) {
		if (w.player == null) { return false; }
		for (Spy s : w.player.spies) {
			if (s.location == c) {
				return s.infiltrationTimeout <= 0;
			}
		}
		return false;
	}
	
	public static final Clr MAP_SHADOW = new Clr(0, 0, 0, 30);
	
	private void drawRoadLine(ScreenMode sm, Graphics g, Road road) {
		for (int i = 0; i < road.intPath.size() - 1; i++) {
			int[] a = road.intPath.get(i);
			int[] b = road.intPath.get(i + 1);
			double ax = worldToScreenX(w.map.roadXs[a[1]][a[0]]);
			double ay = worldToScreenY(w.map.roadYs[a[1]][a[0]]);
			double bx = worldToScreenX(w.map.roadXs[b[1]][b[0]]);
			double by = worldToScreenY(w.map.roadYs[b[1]][b[0]]);
			if (
				(ax < 0 && bx < 0) ||
				(ay < 0 && by < 0) ||
				(ax > sm.width && bx > sm.width) ||
				(ay > sm.height && by > sm.height)
			) {
				continue;
			}
			
			g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
		}
	}
	
	private void drawRoadLineRemainingForFleet(ScreenMode sm, Graphics g, Fleet f) {
		if (f.road == null) { return; }
		int start = StrictMath.min(f.road.intPath.size() - 1 , (int) StrictMath.ceil(f.progress));
		
		int[] a = null;
		int[] b = f.road.intPath.get(start);
		double ax = worldToScreenX(f.realX(w.map));
		double ay = worldToScreenY(f.realY(w.map));
		double bx = worldToScreenX(w.map.roadXs[b[1]][b[0]]);
		double by = worldToScreenY(w.map.roadYs[b[1]][b[0]]);
		g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
		
		for (int i = start; i < f.road.intPath.size() - 1; i++) {
			a = f.road.intPath.get(i);
			b = f.road.intPath.get(i + 1);
			ax = worldToScreenX(w.map.roadXs[a[1]][a[0]]);
			ay = worldToScreenY(w.map.roadYs[a[1]][a[0]]);
			bx = worldToScreenX(w.map.roadXs[b[1]][b[0]]);
			by = worldToScreenY(w.map.roadYs[b[1]][b[0]]);
			if (
				(ax < 0 && bx < 0) ||
				(ay < 0 && by < 0) ||
				(ax > sm.width && bx > sm.width) ||
				(ay > sm.height && by > sm.height)
			) {
				continue;
			}
			
			g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
		}
	}
	
	private void drawRiver(ScreenMode sm, Graphics g, River river, int extraLineWidth) {
		for (int i = 0; i < river.path.size() - 1; i++) {
			River.Node a = river.path.get(i);
			River.Node b = river.path.get(i + 1);
			g.setLineWidth((zoom > 9 ? (2 + a.w / 2) : (1 + a.w / 2)) + extraLineWidth);
			double ax = worldToScreenX(a.x);
			double ay = worldToScreenY(a.y);
			double bx = worldToScreenX(b.x);
			double by = worldToScreenY(b.y);
			double dx = bx - ax;
			double dy = by - ay;
			double l = Math.sqrt(dx * dx + dy * dy);
			if (l > 0) {
				dx /= l;
				dy /= l;
				ax -= dx * extraLineWidth;
				ay -= dy * extraLineWidth;
				if (i == river.path.size() - 2) {
					bx -= dx * extraLineWidth;
					by -= dy * extraLineWidth;
				}
			}
			//g.setColor(i % 2 == 0 ? Color.red : Color.black);
			if (
				(ax < 0 && bx < 0) ||
				(ay < 0 && by < 0) ||
				(ax > sm.width && bx > sm.width) ||
				(ay > sm.height && by > sm.height)
			) {
				continue;
			}
			
			g.drawLine((float) ax, (float) ay, (float) bx, (float) by);
		}
	}
	
	private void highlightRoadLine(ScreenMode sm, Graphics g, Road road) {
		g.setColor(DARKER_INK_C);
		g.setLineWidth(4);
		drawRoadLine(sm, g, road);
		g.setLineWidth(1);
	}
	
	private void highlightInvasionLine(ScreenMode sm, Graphics g, Fleet f) {
		g.setColor(DARKISH_RED_INK_C);
		g.setLineWidth(4);
		drawRoadLineRemainingForFleet(sm, g, f);
		g.setLineWidth(1);
	}
	
	private void renderInterceptInfo(MyDraw d, ScreenMode sm, final Fleet fl) {
		if (!w.map.toggles.contains(ConquestToggle.SUPPLY)) { return; }
		int x = (int) worldToScreenX(fl.realX(w.map));
		int y = (int) worldToScreenY(fl.realY(w.map));
		if (selectedFleet.canIntercept(fl, getFleetSelection(), w.map)) {
			String extraText = "";
			int supplyCost = w.player.moveSupplyCost(selectedFleet, getFleetSelection(), null, selectedFleet.getFlightIntercept(fl, getFleetSelection(), w.map), w.map);
			if (supplyCost <= StrictMath.min(Fleet.maxSupply(_fleetSelection), selectedFleet.supply())) {
				int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(selectedFleet, getFleetSelection(), null, selectedFleet.getFlightIntercept(fl, getFleetSelection(), w.map), w.map);
				if (moveAndReturnSupplyCost > StrictMath.min(selectedFleet.supply(), Fleet.maxSupply(_fleetSelection))) {
					extraText = _t("Sufficient_supply_to_intercept_but_not_return", moveAndReturnSupplyCost, selectedFleet.supply());
				} else {
					extraText = _t("Sufficient_supply_to_intercept_and_return");
				}
			} else {
				extraText = _t("Insufficient_supply_to_intercept");
			}

			String text = extraText + "\n\n" + w.player.moveSupplyCostExplanation(selectedFleet, _fleetSelection, null, selectedFleet.getFlightIntercept(fl, getFleetSelection(), w.map), w.map);
			Empire victim = w.map.owner(fl) instanceof Empire ? ((Empire) w.map.owner(fl)) : null;
			String heroText = Hero.getStatChangeAppendix(w.player, HeroEvent.combatVictory(w.player, victim), w.map, false) + Hero.getStatChangeAppendix(w.player, HeroEvent.combatDefeat(w.player, victim), w.map, false);
			
			if (!heroText.isEmpty()) {
				text += "\n" + heroText;
			}
			
			if (selectedFleet.didIntercept) {
				text = _t("cannot_change_icept");
			}
			Pt ts = d.textSize(text, AGame.FOUNT);
			d.drawPanel(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING * 2 + MyDraw.ICON_BUTTON_SZ, (int) ts.x + MyDraw.PANEL_INSET * 2, (int) ts.y + MyDraw.PANEL_INSET * 2, 19);
			d.text(text, AGame.FOUNT, x - MyDraw.ICON_BUTTON_SZ / 2 + MyDraw.PANEL_INSET, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING * 2 + MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET);
		}
	}
	
	private void renderLocationFleets(final MyDraw d, ScreenMode sm, final MapLocation loc, Pt cursor) {
		Fleet playerFleet = null;
		Fleet singleViewableOtherFleet = null;
		boolean canViewOtherFleets = w.player == null || loc.canSeeFleetsHere(w.map, w.player);
		boolean airships = false;
		boolean landships = false;
		ArrayList<Fleet> fleets = new ArrayList<Fleet>();
		ArrayList<Empire> owners = new ArrayList<Empire>();
		for (int ei = 0; ei < w.map.empires.size(); ei++) {
			Empire e = w.map.empires.get(ei);
			for (int fi = 0; fi < e.getFleets().size(); fi++) {
				Fleet f = e.getFleets().get(fi);
				if (f.location == loc) {
					if (e == w.player) {
						playerFleet = f;
					} else if (canViewOtherFleets|| w.player.canInspectFleets(e, w.map)) {
						singleViewableOtherFleet = f;
					}
					fleets.add(f);
					airships = airships || !f.groundOnly();
					landships = landships || !f.canFly();
					if (!owners.contains(e)) {
						owners.add(e);
					}
				}
			}
		}
		
		if (fleets.isEmpty()) { return; }
		if (fleets.size() > 1) { singleViewableOtherFleet = null; }
		
		final Fleet defaultSelectFleet = playerFleet != null ? playerFleet : singleViewableOtherFleet;
		
		int x = (int) worldToScreenX(fleets.get(0).realX(w.map)) + DOCKED_FLEET_DX;
		int y = (int) worldToScreenY(fleets.get(0).realY(w.map));
		
		if (defaultSelectFleet != null && checked(sm, x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ)) {
			d.hook(x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ, new Hook("fleet", Hook.Type.HOVER, Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
				@Override
				public void run(Input in, Pt p, Type type) {
					if (type == Hook.Type.HOVER) {
						nextHoverFleet = defaultSelectFleet;
					} else if (d.state.canClick()) {
						menuCity = null;
						selectedFleet = defaultSelectFleet;
						setFleetSelection(defaultSelectFleet.getAllNonAnchoredShips());
						fleetScrollBar.offset = 0;
						d.state.hasClicked();
					}
				}
			});
		}
		
		Img fleetApp = airfleet;
		Img fleetAppOutline = airfleetOutline;
		if (landships) {
			if (airships) {
				fleetApp = mixedfleet;
				fleetAppOutline = mixedfleetOutline;
			} else {
				fleetApp = landfleet;
				fleetAppOutline = landfleetOutline;
			}
		}
		
		boolean highlit = defaultSelectFleet != null && (defaultSelectFleet == selectedFleet || defaultSelectFleet == hoverFleet);
		
		Clr ink = INK;
		if (highlit && w.player != null) {
			ink = w.player.getMainTincture().tint.mix(0.7, INK);
		}
		
		if (owners.size() > 1) {
			d.blit(fleetApp, highlit ? ink : PARCHMENT, x - FLEET_IMG_SZ / 2 - 2, y - FLEET_IMG_SZ / 2 - 2);
			d.blit(fleetAppOutline, highlit ? PARCHMENT : ink, x - FLEET_IMG_SZ / 2 - 2, y - FLEET_IMG_SZ / 2 - 2);
			d.blit(fleetApp, highlit ? ink : PARCHMENT, x - FLEET_IMG_SZ / 2 + 2, y - FLEET_IMG_SZ / 2 + 2);
			d.blit(fleetAppOutline, highlit ? PARCHMENT : ink, x - FLEET_IMG_SZ / 2 + 2, y - FLEET_IMG_SZ / 2 + 2);
		} else {
			d.blit(fleetApp, highlit ? owners.get(0).getMainTincture().tint.mix(0.7, INK) : PARCHMENT, x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2);
			d.blit(fleetAppOutline, highlit ? PARCHMENT : owners.get(0).getMainTincture().tint.mix(0.7, INK), x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2);
			
			if (owners.get(0) == w.player && fleets.get(0).needsRepairs()) {
				d.borderedBlit(repair, PARCHMENT, owners.get(0).getMainTincture().tint.mix(0.7, INK),
						x - FLEET_IMG_SZ / 2 - repair.srcWidth - MyDraw.SCROLL_EL_SPACING - 1, y - repair.srcHeight / 2);
				d.tooltip(x - FLEET_IMG_SZ / 2 - repair.srcWidth - MyDraw.SCROLL_EL_SPACING - 1, y - repair.srcHeight / 2, repair.srcWidth, repair.srcHeight, _t("Repairs_Possible"));
			}
			if (owners.get(0) == w.player && w.has(ConquestToggle.SUPPLY)) {
				d.rect(INK, x - FLEET_IMG_SZ / 2, y + FLEET_IMG_SZ / 2 + 2, FLEET_IMG_SZ, 6);
				d.rect(PARCHMENT, x - FLEET_IMG_SZ / 2 + 1, y + FLEET_IMG_SZ / 2 + 2 + 1, FLEET_IMG_SZ - 2, 4);
				d.rect(INK, x - FLEET_IMG_SZ / 2 + 2, y + FLEET_IMG_SZ / 2 + 2 + 2, (FLEET_IMG_SZ - 4) * fleets.get(0).supply() / fleets.get(0).maxSupply(), 2);
			}
		}
		
		d.highlight("selectFleetAt-" + loc.id, g, x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ);
		
		for (int foi = 0; foi < owners.size(); foi++) {
			int xOffset =
					foi % 2 == 0 // Left or center
						? foi == owners.size() - 1
							? -10  // Center
							: -21 // Left
						: 1; // Right
			int yOffset = -(foi / 2) * 22;
			boolean canView = canViewOtherFleets || w.player.canInspectFleets(owners.get(foi), w.map);
			boolean hover = canView && Rect.contains(x + xOffset, y - FLEET_IMG_SZ / 2 - 16 - 2 - MyDraw.BUTTON_SPACING + yOffset, 20, 20, cursor);
			d.rect(hover ? INK : PARCHMENT, x + xOffset, y - FLEET_IMG_SZ / 2 - 16 - 2 - MyDraw.BUTTON_SPACING + yOffset, 20, 20);
			d.rect(hover ? PARCHMENT : INK, x + xOffset + 1, y - FLEET_IMG_SZ / 2 - 16 - 1 - MyDraw.BUTTON_SPACING + yOffset, 18, 18);
			owners.get(foi).arms.draw(d, x + xOffset + 2, y - FLEET_IMG_SZ / 2 - 16 - MyDraw.BUTTON_SPACING + yOffset, 16);
			final FleetOwner fo = owners.get(foi);
			if (canView) {
				d.hook(x + xOffset, y - FLEET_IMG_SZ / 2 - 16 - 2 - MyDraw.BUTTON_SPACING + yOffset, 20, 20, new Hook("fleet", Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						ArrayList<Fleet> fs = fo.getFleets();
						for (int fi = 0; fi < fs.size(); fi++) {
							Fleet f = fs.get(fi);
							if (f.location == loc) {
								menuCity = null;
								selectedFleet = f;
								setFleetSelection(f.getAllNonAnchoredShips());
								fleetScrollBar.offset = 0;
								d.state.hasClicked();
								break;
							}
						}
					}
				});
			}
		}
	}
	
	private void renderFleet(MyDraw d, ScreenMode sm, final Fleet fl, Empire eOwner, MonsterNest mOwner) {
		int x = (int) worldToScreenX(fl.realX(w.map));
		final int y = (int) worldToScreenY(fl.realY(w.map));
		
		if (eOwner != null) {
			Img fleetApp = airfleet;
			Img fleetAppOutline = airfleetOutline;
			if (!fl.canFly()) {
				if (w.map.water[fl.intY()][fl.intX()]) {
					if (fl.groundOnly()) {
						fleetApp = convoy;
						fleetAppOutline = convoyOutline;
					} else {
						fleetApp = escortedConvoy;
						fleetAppOutline = escortedConvoyOutline;
					}
				} else {
					if (fl.groundOnly()) {
						fleetApp = landfleet;
						fleetAppOutline = landfleetOutline;
					} else {
						fleetApp = mixedfleet;
						fleetAppOutline = mixedfleetOutline;
					}
				}
			}
			d.blit(fleetApp, fl == selectedFleet || fl == hoverFleet ? eOwner.getMainTincture().tint.mix(0.7, INK) : PARCHMENT, x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2);
			d.blit(fleetAppOutline, fl == selectedFleet || fl == hoverFleet ? PARCHMENT : eOwner.getMainTincture().tint.mix(0.7, INK), x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2);
			d.rect(PARCHMENT, x - 10, y - FLEET_IMG_SZ / 2 - 16 - 2 - MyDraw.BUTTON_SPACING, 20, 20);
			d.rect(INK, x - 9, y - FLEET_IMG_SZ / 2 - 16 - 1 - MyDraw.BUTTON_SPACING, 18, 18);
			eOwner.arms.draw(d, x - 8, y - FLEET_IMG_SZ / 2 - 16 - MyDraw.BUTTON_SPACING, 16);
			d.tooltip(x - 9, y - FLEET_IMG_SZ / 2 - 16 - 1 - MyDraw.BUTTON_SPACING, 18, 18, eOwner.getTooltip(w.player, w.map));
			if (eOwner == w.player && fl.needsRepairs()) {
				d.borderedBlit(repair, PARCHMENT, eOwner.getMainTincture().tint.mix(0.7, INK),
						x - FLEET_IMG_SZ / 2 - repair.srcWidth - MyDraw.SCROLL_EL_SPACING - 1, y - repair.srcHeight / 2);
				d.tooltip(x - FLEET_IMG_SZ / 2 - repair.srcWidth - MyDraw.SCROLL_EL_SPACING - 1, y - repair.srcHeight / 2, repair.srcWidth, repair.srcHeight, _t("Repairs_Possible"));
			}
			if (eOwner == w.player && w.has(ConquestToggle.SUPPLY)) {
				d.rect(INK, x - FLEET_IMG_SZ / 2, y + FLEET_IMG_SZ / 2 + 2, FLEET_IMG_SZ, 6);
				d.rect(PARCHMENT, x - FLEET_IMG_SZ / 2 + 1, y + FLEET_IMG_SZ / 2 + 2 + 1, FLEET_IMG_SZ - 2, 4);
				d.rect(INK, x - FLEET_IMG_SZ / 2 + 2, y + FLEET_IMG_SZ / 2 + 2 + 2, (FLEET_IMG_SZ - 4) * fl.supply() / fl.maxSupply(), 2);
				if (fl.isLimpHome) {
					checkedHeavilyBorderedText(d, sm, _t("Limping_Back"), AGame.FOUNT, AGame.FOUNT_OUTLINE, INK, PARCHMENT, x - FLEET_IMG_SZ / 2, y + FLEET_IMG_SZ / 2 + 10, 10000);
					d.tooltip(x - FLEET_IMG_SZ / 2 - 1, y + FLEET_IMG_SZ / 2 + 10 - 1, d.textSize(_t("Limping_Back"), AGame.FOUNT).x + 2, (int) AGame.FOUNT.lineHeight + 2, _t("Limping_Back_Tooltip"));
				}
			}
		} else if (mOwner != null) {
			if (mOwner.type.getMapFleetImage() == null) {
				d.rect(PARCHMENT, x - 10, y - 10, 20, 20);
				d.rect(INK, x - 9, y - 9, 18, 18);
				mOwner.getArms().draw(d, x - 8, y - 8, 16);
			} else {
				for (int dy = -1; dy <= 1; dy++) { for (int dx = -1; dx <= 1; dx++) {
					d.blit(mOwner.type.getMapFleetBackground(), fl == hoverFleet ? DARKER_INK : PARCHMENT, x + dx - 8, y + dy - 8);
				}}
				d.blit(mOwner.type.getMapFleetImage(), fl == hoverFleet ? PARCHMENT : DARKER_INK, x - 8, y - 8);
			}
		}
	}

	private void renderInTransitFleet(MyDraw d, ScreenMode sm, final Fleet fl, Empire eOwner, MonsterNest mOwner) {
		int x = (int) worldToScreenX(fl.realX(w.map));
		final int y = (int) worldToScreenY(fl.realY(w.map));
		
		if (!fl.inTransit()) {
			return;
		} else {
			d.tooltip(x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ,
					w.describeTime(fl.travelTimeLeft(eOwner == null ? mOwner.bonuses() : eOwner.bonuses, w.map)),
					new Runnable() {
						@Override
						public void run() {
							nextHoverFleet = fl;
						}
					}
			);
		}
		
		Graphics g = (Graphics) d.frame().nativeRenderer();
		
		if (fl.inTransit() && (fl == selectedFleet || fl == hoverFleet)) {
			if (fl.interceptPoint != null) {
				d.dottedLine(DARKER_INK, 2, 8, worldToScreenX(fl.interceptPoint.x), worldToScreenY(fl.interceptPoint.y), x, y);
				if (fl.interceptTarget.interceptPoint != null) {
					d.dottedLine(DARKER_INK, 2, 8, worldToScreenX(fl.interceptTarget.interceptPoint.x), worldToScreenY(fl.interceptTarget.interceptPoint.y), x, y);
				} else if (fl.interceptTarget.destination != null) {
					d.dottedLine(DARKER_INK, 2, 8, worldToScreenX(fl.interceptTarget.destination.x), worldToScreenY(fl.interceptTarget.destination.y), x, y);
				}
			} else if (fl.road != null) {
				highlightRoadLine(sm, g, fl.road);
			} else {
				d.dottedLine(DARKER_INK, 2, 8, worldToScreenX(fl.destination.x), worldToScreenY(fl.destination.y), x, y);
			}
		}
		
		// Highlight if fleet is invading player.
		if (fl.inTransit() && w.player != null && w.player.cities.contains(fl.destination) && ((eOwner != null && w.map.getRelationship(w.player, eOwner).level == Relationship.Level.WAR) || mOwner != null)) {
			if (fl.road != null) {
				highlightInvasionLine(sm, g, fl);
			} else {
				d.dottedLine(DARKISH_RED_INK, 2, 4, worldToScreenX(fl.destination.x), worldToScreenY(fl.destination.y), x, y);
			}
		}
		
		renderFleet(d, sm, fl, eOwner, mOwner);
		
		if (selectedFleet != null && eOwner != w.player && w.player != null && w.player.getFleets().contains(selectedFleet)) {
			if (selectedFleet.interceptTarget != fl && selectedFleet.canIntercept(fl, getFleetSelection(), w.map)) {
				if (selectedFleet.didIntercept) {
					d.colouredIconButton(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING, cannotAttack, new Runnable() {
						@Override
						public void run() {
							// Do nuffink! Nuffink!
						}
					}, false);
				} else {
					if (w.map.toggles.contains(ConquestToggle.SUPPLY)) {
						int supplyCost = w.player.moveSupplyCost(selectedFleet, getFleetSelection(), null, selectedFleet.getFlightIntercept(fl, getFleetSelection(), w.map), w.map);
						if (supplyCost <= StrictMath.min(Fleet.maxSupply(_fleetSelection), selectedFleet.supply())) {
							int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(selectedFleet, getFleetSelection(), null, selectedFleet.getFlightIntercept(fl, getFleetSelection(), w.map), w.map);
							Img icon = null;
							if (moveAndReturnSupplyCost > StrictMath.min(selectedFleet.supply(), Fleet.maxSupply(_fleetSelection))) {
								icon = canAttackButNotReturn;
							} else {
								icon = canAttackAndReturn;
							}
							d.colouredIconButton(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING, icon, new Runnable() {
								@Override
								public void run() {
									intercept(fl, null);
								}
							}, true);
						} else {
							d.colouredIconButton(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING, cannotAttack, new Runnable() {
								@Override
								public void run() {
									// Do nuffink! Nuffink!
								}
							}, false);
						}
					} else {
						d.colouredIconButton(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING, canMove, new Runnable() {
							@Override
							public void run() {
								intercept(fl, null);
							}
						}, true);
					}
				}
				
				d.hook(x - MyDraw.ICON_BUTTON_SZ / 2, y + FLEET_IMG_SZ / 2 + MyDraw.UI_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, new Hook(Hook.Type.HOVER) {
					@Override
					public void run(Input in, Pt p, Type type) {
						nextHoverFleet = fl;
					}
				});
			}
		}
	}
	
	private static boolean checked(ScreenMode sm, int x, int y, int w, int h) {
		return x + w > 0 && y + h > 0 && x <= sm.width && y <= sm.height;
	}
	
	private static void checkedBlit(MyDraw d, ScreenMode sm, Img img, Clr c, double x, double y) {
		if (img != null && x < sm.width && y < sm.height && x + img.srcWidth > 0 && y + img.srcHeight > 0) {
			d.blit(img, c, x, y);
		}
	}
	
	private static void checkedBlit(MyDraw d, ScreenMode sm, Img img, Clr c, double x, double y, int w, int h) {
		if (img != null && x < sm.width && y < sm.height && x + w > 0 && y + h > 0) {
			d.blit(img, c, x, y, w, h);
		}
	}
	
	public static void checkedBorderedBlit(MyDraw d, ScreenMode sm, Img img, Clr c, Clr border, double x, double y) {
		if (img != null && x < sm.width && y < sm.height && x + img.srcWidth > 0 && y + img.srcHeight > 0) {
			d.borderedBlit(img, c, border, x, y);
		}
	}
	
	public static void checkedHeavilyBorderedText(MyDraw d, ScreenMode sm, String text, Fount f, Fount bg, Clr c, Clr border, int x, int y, int maxW) {
		if (x >= sm.width || y >= sm.height || x + maxW < 0) {
			return;
		}
		Pt sz = d.textSize(text, bg, maxW, 10000, 0, true);
		if (x + sz.x < 0 || y + sz.y < 0) {
			return;
		}
		d.heavilyBorderedText(text, f, bg, c, border, x, y, maxW);
	}
	
	public String spyNetworkLevelInfo(Spy spy, City c) {
		String info = _t("spy_network_level_info", spy.networkLevel, EmpireStat.MAX_SPY_NETWORK_LEVEL.get(w.player.bonuses), _t(spy.getNetworkLevelName()));
		if (spy.networkLevel >= EmpireStat.SPY_CAN_SEE_LOCAL_INSIDES.get(w.player.bonuses)) {
			info += "\n\n" + MyDraw.SELECTED_C;
		} else {
			info += "\n\n[]";
		}
		info += EmpireStat.SPY_CAN_SEE_LOCAL_INSIDES.get(w.player.bonuses) + ": " + _t("SPY_CAN_SEE_LOCAL_INSIDES");
		if (!c.isTown) {
			if (spy.networkLevel >= EmpireStat.SPY_CAN_INSPECT_FLEETS.get(w.player.bonuses)) {
				info += "\n" + MyDraw.SELECTED_C;
			} else {
				info += "\n[]";
			}
			info += EmpireStat.SPY_CAN_INSPECT_FLEETS.get(w.player.bonuses) + ": " + _t("SPY_CAN_INSPECT_FLEETS");
			
			if (spy.networkLevel >= EmpireStat.SPY_CAN_VIEW_ALL_CITIES.get(w.player.bonuses)) {
				info += "\n" + MyDraw.SELECTED_C;
			} else {
				info += "\n[]";
			}
			info += EmpireStat.SPY_CAN_VIEW_ALL_CITIES.get(w.player.bonuses) + ": " + _t("SPY_CAN_VIEW_ALL_CITIES");
			
			if (spy.networkLevel >= EmpireStat.SPY_CAN_SEE_ALL_INSIDES.get(w.player.bonuses)) {
				info += "\n" + MyDraw.SELECTED_C;
			} else {
				info += "\n[]";
			}
			info += EmpireStat.SPY_CAN_SEE_ALL_INSIDES.get(w.player.bonuses) + ": " + _t("SPY_CAN_SEE_ALL_INSIDES");
		}
		info += "[]";
		return info;
	}

	@Override
	public void render(final MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		adjScrollX = scrollX + (sm.width / 2 / zoom);
		adjScrollY = scrollY + (sm.height / 2 / zoom);
		
		if (w != null && w.player != null && (w.victory() || w.defeat())) {
			boolean playerWon = w.victory();
			if (!mpVictoryOrDefeatReported && w.isMultiplayer()) {
				//g.reportDebug("Multiplayer " + (playerWon ? "victory" : "defeat") + " empireName " + w.player.name + " playerName " + w.player.playerName + " mapAge " + w.map.age);
			}
			/*if (!victoryTimeReported) {
				victoryTimeReported = true;
				if (g.isIntegrated()) {
					g.integration.sendFeedbackMessage("victory age " + w.map.age + " mapSize " + w.map.size.name + " difficulty " + w.map.difficulty.name + " cheats " + w.usedCheatCommand);
				}
			}*/
			if (endOfGameMessage == null) {
				if (playerWon) {
					endOfGameMessage = w.map.getVictoryMessage(w.player);
				} else {
					endOfGameMessage = w.map.getDefeatMessage(w.player);
				}
			}
			if (playerWon && !w.usedCheatCommand) {
				if (w.map.victors().size() > 1) {
					Achievement.achieve(Achievement.CONQUER_TOGETHER);
				} else if (w.player.isCrowned(w.map)) {
					Achievement.achieve(Achievement.RESTITUTOR_ORBIS);
				} else if (w.player.hasCompletedFinalRitual(w.map)) {
					Achievement.achieve(Achievement.AGE_OF_THE_WORM);
				} else if (w.has(ConquestToggle.REPUTATION) && w.player.getReputationLevel() == Empire.ReputationLevel.HATED) {
					Achievement.achieve(Achievement.ARE_WE_THE_BADDIES);
				}
				if ((w.map.difficulty.name.equals("VERY_HARD") || w.map.difficulty.name.equals("IMPERIAL")) && !w.player.hasLostTerritory) {
					Achievement.achieve(Achievement.INVICTUS);
				}
				if (!w.player.hasBuiltAirship) {
					Achievement.achieve(Achievement.LANDSHIPS);
				}
			}
			mpVictoryOrDefeatReported = true;
			Img img = endOfGameMessage.b;
			String label = _t(playerWon ? "VICTORY" : "DEFEAT");
			d.rect(MyDraw.DESK, 0, 0, sm.width, sm.height);
			int availableW = sm.width - MyDraw.SIDE_CLEARANCE * 2;
			int availableH = sm.height - MyDraw.SIDE_CLEARANCE * 2 - AGame.HUGE_FOUNT.lineHeight - MyDraw.UI_SPACING * 2 - MyDraw.BUTTON_H;
			double availableAspectRatio = availableW * 1.0 / availableH;
			double imageAspectRatio = img.srcWidth * 1.0 / img.srcHeight;
			int imgW, imgH;
			if (availableAspectRatio > imageAspectRatio) {
				imgH = availableH;
				imgW = availableH * img.srcWidth / img.srcHeight;
			} else {
				imgW = availableW;
				imgH = availableW * img.srcHeight / img.srcWidth;
			}
			int x = 0;
			int y = MyDraw.SIDE_CLEARANCE;
			x += (sm.width - imgW) / 2;
			//d.drawWindow(x, y, 128 + MyDraw.WINDOW_INSET * 2, 128 + MyDraw.WINDOW_INSET * 2);
			//w.player.arms.draw(d, x + MyDraw.WINDOW_INSET, y + MyDraw.WINDOW_INSET, 128);
			d.text(MyDraw.TITLE_C + label, AGame.HUGE_FOUNT, sm.width / 2 - (int) d.textSize(label, AGame.HUGE_FOUNT).x / 2, y);
			//y += 128 + MyDraw.WINDOW_INSET * 2 + MyDraw.UI_SPACING;
			y += AGame.HUGE_FOUNT.lineHeight + MyDraw.UI_SPACING;
			if (showStats) {
				statsDisplay.render(w.map, w.map.stats, w.player == null ? BonusSet.empty() : w.player.bonuses, d, x, y, imgW, imgH);
			} else {
				d.blit(img, x, y, imgW, imgH);
				d.drawPanelBorder(x - 1, y - 1, imgW + 2, imgH + 2);
				if (Lang.flavour()) {
					Rect msgSize = d.textSize(endOfGameMessage.a, AGame.FOUNT, 0, 0, imgW + 2 - MyDraw.PANEL_INSET * 2);
					int msgH = (int) msgSize.height;
					d.drawPanel(x - 1, y + availableH - msgH - MyDraw.PANEL_INSET * 2, imgW + 2, msgH + MyDraw.PANEL_INSET * 2, -1);
					d.text(endOfGameMessage.a, AGame.FOUNT, x - 1 + MyDraw.PANEL_INSET, y + availableH - msgH - MyDraw.PANEL_INSET, imgW + 2 - MyDraw.PANEL_INSET * 2);
				}
				//d.drawPanel(x - 1, 
			}
			y += imgH + MyDraw.UI_SPACING;
			int x2 = x + imgW;
			int bw = d.bw(_t("Leave"));
			x2 -= bw;
			d.button(x2, y, bw, _t("Leave"), null, new Runnable() {
				@Override
				public void run() {
					leave();
				}
			}, w.someoneWon() || g.lanServer == null);
			if (!w.someoneWon() && g.lanServer != null) {
				d.tooltip(x2, y, bw, MyDraw.BUTTON_H, _t("you_are_the_loser_host_notice"));
			}
			if (!w.someoneWon()) {
				bw = d.bw(_t("Spectate"));
				x2 -= bw + MyDraw.BUTTON_SPACING;
				d.button(x2, y, bw, _t("Spectate"), new Runnable() {
					@Override
					public void run() {
						spectate();
					}
				});
			}
			bw = d.tw(_t("campaign_stats"));
			d.toggle(x, y, bw, _t("campaign_stats"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					showStats = !showStats;
				}
			}, showStats, true);
			return;
		}
		
		if (!loadMapBG()) { return; }
		
		if (upgradeStartedNest != null && upgradeStartedNest.type == null) {
			upgradeStartedNest = null;
		}
		
		if (upgradeNearingCompletionNest != null && upgradeNearingCompletionNest.type == null) {
			upgradeNearingCompletionNest = null;
		}
		
		if (upgradeCompleteNest != null && upgradeCompleteNest.type == null) {
			upgradeCompleteNest = null;
		}
		
		d.rect(MyDraw.DESK, 0, 0, sm.width, sm.height);
		
		if (mrpd != null) {
			mrpd.render(d, sm, hs, cursor);
			return;
		}
		
		if (w == null || w.map == null) {
			leave();
			g.showError(_t("world_gen_load_failed"));
			return;
		}
		
		if (time == 0) {
			zoom = 10 * sm.width / 1000;
			if (w.player != null) {
				scrollX = -w.player.cities.get(0).x;
				scrollY = -w.player.cities.get(0).y;
			}
			adjScrollX = scrollX + (sm.width / 2 / zoom);
			adjScrollY = scrollY + (sm.height / 2 / zoom);
		}
		
		if (selectedFleet != null && (w.map.owner(selectedFleet) == null || (selectedFleet.actives.isEmpty() && selectedFleet.reserve.isEmpty()))) {
			selectedFleet = null;
		}
		if (hoverFleet != null && w.map.owner(hoverFleet) == null) {
			hoverFleet = null;
		}

		d.hook(0, 0, sm.width, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED) {
			@Override
			public void run(Input in, Pt p, Type type) {
				menuCity = null;
				selectedFleet = null;
			}
		});

		PerfStats.start();
		// Map rendering
		d.shift(sm.width / 2, sm.height / 2);
		d.scale(zoom, zoom);
		d.shift(scrollX, scrollY);
		
		double worldScreenLeft = screenToWorldX(0), worldScreenRight = screenToWorldX(sm.width), worldScreenTop = screenToWorldY(0), worldScreenBottom = screenToWorldY(sm.height);

		for (Backdrop bd : backdrops) {
			if (SimplePref.ARACHNOPHOBIA_MODE.get() && bd.type.spider) { continue; }
			if (Rect2D.intersects(bd.x - bd.type.img.srcWidth / WorldMap.SCALE_FACTOR / 2, bd.y - bd.type.img.srcHeight / WorldMap.SCALE_FACTOR / 2, bd.type.img.srcWidth / WorldMap.SCALE_FACTOR, bd.type.img.srcHeight / WorldMap.SCALE_FACTOR, worldScreenLeft, worldScreenTop, worldScreenRight - worldScreenLeft, worldScreenBottom - worldScreenTop)) {
				if (bd.type.shadow) {
					d.blit(bd.type.img, BACKDROP_DARKENING, bd.x, bd.y, bd.type.img.srcWidth / WorldMap.SCALE_FACTOR / 2, bd.type.img.srcHeight / WorldMap.SCALE_FACTOR / 2, bd.rot);
				} else {
					d.blit(bd.type.img, Clr.WHITE, bd.x, bd.y, bd.type.img.srcWidth / WorldMap.SCALE_FACTOR / 2, bd.type.img.srcHeight / WorldMap.SCALE_FACTOR / 2, bd.rot);
				}
			}
		}
		
		d.rect(OCEAN, 0, 0, w.map.water[0].length + 1, w.map.water.length + 1);
		
		TextureImpl.bindNone();
		glBegin(GLCompat.GL_TRIANGLES);
		glColor4f(PARCHMENT.r / 255f, PARCHMENT.g / 255f, PARCHMENT.b / 255f, 1f);
		for (ShapeUtils.Area area : w.map.landBoundaries) {
			TrianglesArea p = area.getPolygon();
			if (
					p.polygon.getMaxX() >= worldScreenLeft &&
					p.polygon.getMinX() <= worldScreenRight &&
					p.polygon.getMaxY() >= worldScreenTop &&
					p.polygon.getMinY() <= worldScreenBottom
			) {
				p.draw();
			}
		}
		if (w.player == null || (mapModePOV != null && !w.map.empires.contains(mapModePOV))) {
			mapMode = StrategicMapMode.NORMAL;
			mapModePOV = null;
		}
		for (ShapeUtils.Area area : w.map.cityOwnershipAreas) {
			City city = w.map.getCity(area.identifier);
			Empire e = w.map.owner(city);
			if (city.territoryPolygon == null) {
				city.territoryPolygon = new ShapeUtils.TrianglesArea(area);
			}
			if (
					city.territoryPolygon.polygon.getMaxX() >= worldScreenLeft &&
					city.territoryPolygon.polygon.getMinX() <= worldScreenRight &&
					city.territoryPolygon.polygon.getMaxY() >= worldScreenTop &&
					city.territoryPolygon.polygon.getMinY() <= worldScreenBottom
			) {
				Color c = mapMode.getColor(city, w, mapModePOV == null ? w.player : mapModePOV);
				glColor4f(c.r, c.g, c.b, 0.33f);
				city.territoryPolygon.draw();
			}
		}
		
		glEnd();
		glColor3f(1.0f, 1.0f, 1.0f);
		TextureImpl.bindNone();
		
		d.resetTransforms();
		
		// Water debug render
		/*for (int y = 0; y < w.map.water.length; y++) {
			for (int x = 0; x < w.map.water[0].length; x++) {
				if (w.map.water[y][x]) {
					d.rect(Clr.BLUE, (int) worldToScreenX(x), (int) worldToScreenY(y), 8, 8);
				}
			}
		}*/
		
		// Eikon overlay attempt
		if (msSinceZoomChanged > 500) {
			boolean alternator = false;
			for (double iconY = 0; iconY < w.map.water.length - 8 / zoom; iconY += 60 / zoom) {
				alternator = !alternator;
				for (double iconX = alternator ? 30 / zoom : 0; iconX < w.map.water[0].length - 8 / zoom; iconX += 60 / zoom) {
					int centerX = Math.min(w.map.cityOwnership[0].length - 1, (int) (iconX + 8 / zoom));
					int centerY = Math.min(w.map.cityOwnership.length - 1, (int) (iconY + 8 / zoom));
					int left = Math.max(0, (int) (iconX - 4 / zoom));
					int top = Math.max(0, (int) (iconY - 4 / zoom));
					int right = Math.min(w.map.cityOwnership[0].length - 1, (int) (iconX + 20 / zoom));
					int bottom = Math.min(w.map.cityOwnership.length - 1, (int) (iconY + 20 / zoom));
					int ownCenter = w.map.cityOwnership[centerY][centerX];
					if (ownCenter >= 0 &&
						ownCenter == w.map.cityOwnership[top][left] &&
						ownCenter == w.map.cityOwnership[bottom][right] &&
						ownCenter == w.map.cityOwnership[bottom][left] &&
						ownCenter == w.map.cityOwnership[top][right])
					{
						Img icon = mapMode.getIcon(w.map.getCity(ownCenter), w, mapModePOV == null ? w.player : mapModePOV);
						if (icon != null) {
							d.blit(icon, DARK_INK, (int) worldToScreenX(iconX), (int) worldToScreenY(iconY));
						}
					}
				}
			}
		}
		
		d.shift(sm.width / 2, sm.height / 2);
		d.scale(zoom, zoom);
		d.shift(scrollX, scrollY);
		Graphics graphics = (Graphics) d.frame().nativeRenderer();
		
		graphics.setLineWidth(zoom > 9 ? 3 : 2);
		
		for (ShapeUtils.Area area : w.map.cityOwnershipAreas) {
			City city = w.map.getCity(area.identifier);
			Empire e = w.map.owner(city);
			if (
					city.territoryPolygon.polygon.getMaxX() >= worldScreenLeft &&
					city.territoryPolygon.polygon.getMinX() <= worldScreenRight &&
					city.territoryPolygon.polygon.getMaxY() >= worldScreenTop &&
					city.territoryPolygon.polygon.getMinY() <= worldScreenBottom
			) {
				graphics.setColor(e.mapColor2);
				graphics.draw(city.territoryPolygon.polygon);
			}
		}
		
		int mapBgSize = 1024 / MAX_ZOOM_LEVEL;
		for (int y = 0; y < w.map.water.length; y += mapBgSize) {
			for (int x = 0; x < w.map.water[0].length; x+= mapBgSize) {
				if (checked(sm, (int) worldToScreenX(x) - 1, (int) worldToScreenY(y) - 1, (int) (mapBgSize * zoom) + 1, (int) (mapBgSize * zoom) + 1)) {
					MAP_BG.draw(x + 0.5f, y + 0.5f, mapBgSize, mapBgSize);
				}
			}
		}
		
		d.resetTransforms();
		
		PerfStats.mark("ss territory");
		/*mapBgSize = 1024 / MAX_ZOOM_LEVEL;
		for (int y = 0; y < w.map.water.length; y += mapBgSize) {
			for (int x = 0; x < w.map.water[0].length; x+= mapBgSize) {
				d.rect(Clr.RED, (int) worldToScreenX(x) - 1, (int) worldToScreenY(y) - 1, (int) (mapBgSize * zoom) + 1, (int) (mapBgSize * zoom) + 1);
			}
		}*/
		
		graphics.setLineWidth(zoom > 9 ? 3 : 2);
		graphics.setColor(INK_C);
		for (ShapeUtils.Area area : w.map.landBoundaries) {
			for (int j = 0; j < area.points.size(); j++) {
				ShapeUtils.P a = area.points.get(j);
				if (a.x < TOO_CLOSE || a.x > w.map.water[0].length - TOO_CLOSE || a.y < TOO_CLOSE || a.y > w.map.water.length - TOO_CLOSE) {
					continue;
				}
				ShapeUtils.P b = area.points.get((j + 1) % area.points.size());
				if (b.x < TOO_CLOSE || b.x > w.map.water[0].length - TOO_CLOSE || b.y < TOO_CLOSE || b.y > w.map.water.length - TOO_CLOSE) {
					continue;
				}
				double ax = worldToScreenX(a.x);
				double ay = worldToScreenY(a.y);
				double bx = worldToScreenX(b.x);
				double by = worldToScreenY(b.y);
				if (
					(ax < 0 && bx < 0) ||
					(ay < 0 && by < 0) ||
					(ax > sm.width && bx > sm.width) ||
					(ay > sm.height && by > sm.height)
				) {
					continue;
				}

				graphics.drawLine((float) ax, (float) ay, (float) bx, (float) by);
			}
		}
		
		PerfStats.mark("ss landboundaries");
		
		// Rivers
		if (zoom > 15) {
			graphics.setColor(INK_C);
			for (River r : w.map.rivers) {
				drawRiver(sm, graphics, r, 2);
			}
		}
		
		graphics.setColor(new Color(120, 120, 150));
		for (River r : w.map.rivers) {
			drawRiver(sm, graphics, r, 0);
		}
		
		// Drawing waves
		int index = 0;
		for (int y = 2; y < w.map.water.length - 2; y += 5) {
			lp: for (int x = (y * 3 / 2) % 17; x < w.map.water[0].length; x += 17) {
				ArrayList<Img> waves = w.map.height[y][x] > 0.2 ? GENTLE_WAVES : STRONG_WAVES;
				Img img = waves.get((index++) % waves.size());
				int waveWidth = img.srcWidth / 32 + 3;
				for (int yy = y - 1; yy < y + 2; yy++) { // No bounds check needed due to the restricted loop range above.
					for (int xx = x; xx < x + waveWidth; xx++) {
						if (xx >= w.map.water[yy].length || !w.map.water[yy][xx]) {
							continue lp;
						}
					}
				}
				d.blit(img, INK_TINT, worldToScreenX(x + 1), worldToScreenY(y) - img.srcHeight / 2 * zoom / 32, img.srcWidth * zoom / 32, img.srcHeight * zoom / 32);
			}
		}
		
		PerfStats.mark("ss waves");
			
		graphics.setColor(INK_C);
		// Roads
		graphics.setLineWidth(zoom > 11 ? 2 : 1);
		for (int yy = 0; yy < w.map.roads.length - 1; yy++) {
			for (int xx = 0; xx < w.map.roads[0].length - 1; xx++) {
				//d.rect(w.map.roads[yy][xx] ? Clr.BLACK : Clr.RED, worldToScreenX(xx), worldToScreenY(yy), 5, 5);
				if (w.map.roads[yy][xx]) {
					
					double ax = worldToScreenX(w.map.roadXs[yy][xx]);
					double ay = worldToScreenY(w.map.roadYs[yy][xx]);
					if (w.map.roads[yy + 1][xx]) {
						double bx = worldToScreenX(w.map.roadXs[yy + 1][xx]);
						double by = worldToScreenY(w.map.roadYs[yy + 1][xx]);
						if (
							(ax < 0 && bx < 0) ||
							(ay < 0 && by < 0) ||
							(ax > sm.width && bx > sm.width) ||
							(ay > sm.height && by > sm.height)
						) {
							continue;
						}

						graphics.drawLine((float) ax, (float) ay, (float) bx, (float) by);
					}
					if (w.map.roads[yy][xx + 1]) {
						double bx = worldToScreenX(w.map.roadXs[yy][xx + 1]);
						double by = worldToScreenY(w.map.roadYs[yy][xx + 1]);
						if (
							(ax < 0 && bx < 0) ||
							(ay < 0 && by < 0) ||
							(ax > sm.width && bx > sm.width) ||
							(ay > sm.height && by > sm.height)
						) {
							continue;
						}

						graphics.drawLine((float) ax, (float) ay, (float) bx, (float) by);
					}
					if (w.map.roads[yy + 1][xx + 1]) {
						double bx = worldToScreenX(w.map.roadXs[yy + 1][xx + 1]);
						double by = worldToScreenY(w.map.roadYs[yy + 1][xx + 1]);
						if (
							(ax < 0 && bx < 0) ||
							(ay < 0 && by < 0) ||
							(ax > sm.width && bx > sm.width) ||
							(ay > sm.height && by > sm.height)
						) {
							continue;
						}

						graphics.drawLine((float) ax, (float) ay, (float) bx, (float) by);
					}
					if (yy > 0 && w.map.roads[yy - 1][xx + 1]) {
						double bx = worldToScreenX(w.map.roadXs[yy - 1][xx + 1]);
						double by = worldToScreenY(w.map.roadYs[yy - 1][xx + 1]);
						if (
							(ax < 0 && bx < 0) ||
							(ay < 0 && by < 0) ||
							(ax > sm.width && bx > sm.width) ||
							(ay > sm.height && by > sm.height)
						) {
							continue;
						}

						graphics.drawLine((float) ax, (float) ay, (float) bx, (float) by);
					}
				}
			}
		}
		
		graphics.setLineWidth(1);
		
		PerfStats.mark("ss roads");
				
		// Terrain features
		int tfImgI = 0;
		for (WorldMap.TerrainFeature f : w.map.features) {
			double fx = f.x + 0.5;
			double fy = f.y + 0.5;
			double scaleDiv = 32;
			if (f.type.backgrounds != null) {
				Img img = f.type.backgrounds.get(tfImgI % f.type.backgrounds.size());
				boolean needsFilter = img.machineImgCache == null;
				int w = (int) (img.srcWidth * zoom / scaleDiv);
				int h = (int) (img.srcHeight * zoom / scaleDiv);
				checkedBlit(d, sm, img, f.type.backgroundColor, worldToScreenX(fx) - w / 2, worldToScreenY(fy) - h / 2, w, h);
				if (needsFilter && img.machineImgCache != null) {
					((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
				}
			}
			Img img = f.type.foregrounds.get(tfImgI % f.type.foregrounds.size());
			boolean needsFilter = img.machineImgCache == null;
			int w = (int) (img.srcWidth * zoom / scaleDiv);
			int h = (int) (img.srcHeight * zoom / scaleDiv);
			checkedBlit(d, sm, img, INK_PARCHMENT_25, worldToScreenX(fx) - w / 2, worldToScreenY(fy) - h / 2, w, h);
			if (needsFilter && img.machineImgCache != null) {
				((Image) img.machineImgCache).setFilter(Image.FILTER_NEAREST);
			}
			tfImgI++;
		}
		
		/* // Super-slow debug view for seeing that beaches etc are placed right.
		for (int yy = 0; yy < w.map.water.length; yy++) { for (int xx = 0; xx < w.map.water[0].length; xx++) {
			CombatBackgroundFlavor cbf = map().getBackground(xx, yy);
			Clr c = null;
			if (cbf.name.equals("ocean")) {
				c = Clr.BLUE;
			}
			if (cbf.name.equals("plains")) {
				c = Clr.GREEN;
			}
			if (cbf.name.contains("coast")) {
				if (cbf.name.contains("off_coast")) {
					c = Clr.CYAN;
				} else {
					c = Clr.YELLOW;
				}
			}
			if (c != null) {
				d.rect(c, worldToScreenX(xx), worldToScreenY(yy), 8, 8);
			}
		}}
		*/
		
		// Markers
		for (int mmi = 0; mmi < w.map.markers.size(); mmi++) {
			MapMarker mm = w.map.markers.get(mmi);
			Fount fount = mm.importance > 15 ? AGame.MAP : AGame.MAP_SMALL;
			Fount fountO = mm.importance > 15 ? AGame.MAP_OUTLINE : AGame.MAP_SMALL_OUTLINE;
			if (mm.textW == 0) {
				Pt sz = d.textSize(mm.text, fountO);
				mm.textW = (int) sz.x;
				mm.textH = (int) sz.y;
			}
			if (mm.textW > mm.widthAvailable * zoom) {
				mm.textDrawn = false;
				continue;
			}
			mm.textX = (int) worldToScreenX(mm.x) - mm.textW / 2;
			mm.textY = (int) worldToScreenY(mm.y) - fount.lineHeight / 2;
			mm.textDrawn = true;
			for (int mmj = 0; mmj < mmi; mmj++) {
				MapMarker mm2 = w.map.markers.get(mmj);
				if (mm2.textDrawn && mm2.textX + mm2.textW >= mm.textX && mm.textX + mm.textW >= mm2.textX && mm2.textY + mm2.textH >= mm.textY && mm.textY + mm.textH >= mm2.textY) {
					mm.textDrawn = false;
					break;
				}
			}
			if (mm.textDrawn) {
				checkedHeavilyBorderedText(d, sm, mm.text, fount, fountO, INK_PARCHMENT_25, PARCHMENT, mm.textX, mm.textY, 1000);
			}
		}
		
		// Specials
		for (int ei = 0; ei < w.map.empires.size(); ei++) {
			Empire e = w.map.empires.get(ei);
			for (int ci = 0; ci < e.cities.size(); ci++) {
				City c = e.cities.get(ci);
				for (int ui = 0; ui < c.upgrades.size(); ui++) {
					CityUpgradeType cut = c.upgrades.get(ui);
					if (!cut.special) { continue; }
					int cx = (int) (worldToScreenX(c.specialX)) - 16;
					int cy = (int) (worldToScreenY(c.specialY)) - 16;
					if (checked(sm, cx, cy, 32, 32)) {
						d.blit(cut.icon32.get(e.bonuses), cx, cy);
						String tt = cut.getName() + "\n\n" + cut.getDesc(w.player == null ? BonusSet.empty() : w.player.bonuses, w.map, !c.takeoverNeeded && c.takeoverMethod == null);
						d.tooltip(cx, cy, 32, 32, tt);
					}
				}
			}
		}
		
		PerfStats.mark("ss tfeatures");
		
		// Map Locations
		/*for (final MonsterNest n : w.map.nests) {
			int nx = (int) (worldToScreenX(n.x));
			int ny = (int) (worldToScreenY(n.y));
			String info = n.getDisplayName();
			if (n.type != null && w.player != null) {
				String bs = n.type.bonusSuffix(w.player);
				info = _t(n.type.name + "_displayName" + bs);
			}
			boolean inCity = false;
			if (w.player == null) {
				inCity = true;
			} else {
				for (City c : w.player.cities) {
					if (c.nestsInTerritory(w.map).contains(n)) {
						inCity = true;
						break;
					}
				}
			}
		}*/
			
		// Map location icons
		
		// Nests
		for (final MonsterNest n : w.map.nests) {
			int nx = (int) (worldToScreenX(n.x));
			int ny = (int) (worldToScreenY(n.y));
			Clr inner = n.type == null ? DARK_INK : DARK_RED_INK;
			Clr outer = PARCHMENT;
			
			int sz = g.currentGUIScale == GUIScale.SMALL? 8 : 16;
			d.highlight("nest-" + n.id, g, nx - sz, ny - sz, sz * 2, sz * 2);

			Rect r = d.textSize(n.getDisplayName(), AGame.MAP_SMALL_OUTLINE, nx + sz + 7, ny - AGame.MAP_SMALL_OUTLINE.height  * 2 / 5, 1000);
			n.textX = (int) r.x;
			n.textY = (int) r.y;
			n.textW = (int) r.width;
			n.textH = (int) r.height;
			
			if (n.type != null && cursor != null && ((n.textDrawn && r.contains(cursor)) || Rect.contains(nx - sz, ny - sz, sz * 2, sz * 2, cursor))) {
				Clr tmp = inner;
				inner = outer;
				outer = tmp;
			}
			
			if (n.type != null && n.type.getMapImage() == null) {
				if (g.currentGUIScale == GUIScale.SMALL) {
					d.rect(PARCHMENT, nx - 10, ny - 10, 20, 20);
					d.rect(INK, nx - 9, ny - 9, 18, 18);
					n.getArms().draw(d, nx - 8, ny - 8, 16);
				} else {
					d.rect(PARCHMENT, nx - 18, ny - 18, 36, 36);
					d.rect(INK, nx - 17, ny - 17, 34, 34);
					n.getArms().draw(d, nx - 16, ny - 16, 32);
				}
			} else {
				if (n.type == null) {
					if (g.currentGUIScale == GUIScale.SMALL) {
						for (int dy = -1; dy <= 1; dy++) { for (int dx = -1; dx <= 1; dx++) {
							checkedBlit(d, sm, emptyNestBG, outer, nx - 8 + dx, ny - 8 + dy);
						}}
						checkedBlit(d, sm, emptyNest, inner, nx - 8, ny - 8);
					} else {
						checkedBlit(d, sm, largeEmptyNestBG, outer, nx - 16, ny - 16);
						checkedBlit(d, sm, largeEmptyNest, inner, nx - 16, ny - 16);
					}
				} else {
					if (g.currentGUIScale == GUIScale.SMALL || n.type.getLargeMapImage() == null) {
						for (int dy = -1; dy <= 1; dy++) { for (int dx = -1; dx <= 1; dx++) {
							checkedBlit(d, sm, n.type.getMapBackground(), outer, nx - 8 + dx, ny - 8 + dy);
						}}
						checkedBlit(d, sm, n.type.getMapImage(), inner, nx - 8, ny - 8);
					} else {
						checkedBlit(d, sm, n.type.getLargeMapBackground(), outer, nx - 16, ny - 16);
						checkedBlit(d, sm, n.type.getLargeMapImage(), inner, nx - 16, ny - 16);
					}
				}
			}
			if (n.type != null) {
				if (checked(sm, nx - sz, ny - sz, sz * 2, sz *2)) {
					d.hook(nx - sz, ny - sz, sz * 2, sz * 2, new Hook("selectNest", Hook.Type.MOUSE_1_CLICKED, Hook.Type.NO_TEST) {
						@Override
						public void run(Input in, Pt p, Type type) {
							selectedNest = n;
						}
					});
				}
				if (checked(sm, nx + 15, ny - 6, (int) r.width, (int) r.height)) {
					d.hook(nx + 15, ny - 6, (int) r.width, (int) r.height, new Hook("selectNest", Hook.Type.MOUSE_1_CLICKED, Hook.Type.NO_TEST) {
						@Override
						public void run(Input in, Pt p, Type type) {
							selectedNest = n;
						}
					});
				}
			}
		}
		
		PerfStats.mark("ss nests");

		// Towns and cities, icons
		for (Empire em : w.map.empires) {
			for (final City c : em.cities) {
				int cx = (int) (worldToScreenX(c.x));
				int cy = (int) (worldToScreenY(c.y));
				Img img;
				int sz;
				if (g.currentGUIScale == GUIScale.SMALL) {
					img = c.isTown ? SMALL_TOWNS_BGS.get(c.appearance % SMALL_TOWNS_BGS.size()) : SMALL_CITIES_BGS.get(c.appearance % SMALL_CITIES_BGS.size());
					sz = 9;
				} else {
					img = c.isTown ? BIG_TOWNS_BGS.get(c.appearance % BIG_TOWNS_BGS.size()) : BIG_CITIES_BGS.get(c.appearance % BIG_CITIES_BGS.size());
					sz = 17;
				}
				for (int dy = -1; dy <= 1; dy++) { for (int dx = -1; dx <= 1; dx++) {
					if (!c.isTown) {
						checkedBlit(d, sm, CoatOfArms.SHIELD_OUTLINE, PARCHMENT, cx - CoatOfArms.SHIELD_W / 2 + dx, cy - 8 - sz - CoatOfArms.SHIELD_H + dy);
					}
				}}
				boolean doesHover = c == hoverCity;
				checkedBlit(d, sm, img, doesHover ? INK : PARCHMENT, cx - sz, cy - sz);
				if (g.currentGUIScale == GUIScale.SMALL) {
					img = c.isTown ? SMALL_TOWNS.get(c.appearance % SMALL_TOWNS.size()) : SMALL_CITIES.get(c.appearance % SMALL_CITIES.size());
				} else {
					img = c.isTown ? BIG_TOWNS.get(c.appearance % BIG_TOWNS.size()) : BIG_CITIES.get(c.appearance % BIG_CITIES.size());
				}
				checkedBlit(d, sm, img, doesHover ? PARCHMENT : INK, cx - sz, cy - sz);
				d.highlight("city-" + c.name, g, cx - sz, cy - sz, img.srcWidth, img.srcHeight);
			}
		}
		
		// Map Location Text
		
		// Towns and cities
		// prioritise, cities first, then towns. names first, then details
		// look at rectangles
		for (int dt = 0; dt < 2; dt++) {
			boolean doTowns = dt == 1;
			for (int ei = 0; ei < w.map.empires.size(); ei++) {
				Empire em = w.map.empires.get(ei);
				for (int ci = 0; ci < em.cities.size(); ci++) {
					final City c = em.cities.get(ci);
					if (doTowns != c.isTown) { continue; }
					int cx = (int) (worldToScreenX(c.x));
					int cy = (int) (worldToScreenY(c.y));
					
					//d.rect(Clr.RED, cx - 1, cy - 1, 2, 2);

					int sz = g.currentGUIScale == GUIScale.SMALL ? 9 : 17;
					c.textX = cx + 11 + sz;
					c.textY = cy - (c.isTown ? AGame.MAP_SMALL_OUTLINE : AGame.MAP_OUTLINE).lineHeight * 2 / 5;
					if (c.textW == 0) {
						Pt tsz = d.textSize(c.name, c.isTown ? AGame.MAP_SMALL_OUTLINE : AGame.MAP_OUTLINE);
						c.textW = (int) tsz.x;
						c.textH = (int) tsz.y;
					}
					
					c.textDrawn = false;
					if (!c.isTown || zoom > 9) {
						c.textDrawn = true;
						lp: for (int ej = 0; ej <= ei; ej++) {
							Empire em2 = w.map.empires.get(ej);
							for (int cj = 0; cj < (ei == ej ? ci : em2.cities.size()); cj++) {
								City c2 = em2.cities.get(cj);
								if (doTowns || !c2.isTown) {
									if (c2.textDrawn && c2.textX + c2.textW >= c.textX && c.textX + c.textW >= c2.textX && c2.textY + c2.textH >= c.textY && c.textY + c.textH >= c2.textY) {
										c.textDrawn = false;
										break lp;
									}
								}
							}
						}
					}
					
					if (c.textDrawn) {
						Fount f;
						Fount bgF;
						if (AGame.isMapSafe(c.name)) {
							f = c.isTown ? AGame.MAP_SMALL : AGame.MAP;
							bgF = c.isTown ? AGame.MAP_SMALL_OUTLINE : AGame.MAP_OUTLINE;
						} else {
							f = c.isTown ? AGame.MAP_FALLBACK_SMALL : AGame.MAP_FALLBACK;
							bgF = c.isTown ? AGame.MAP_FALLBACK_SMALL_OUTLINE : AGame.MAP_FALLBACK_OUTLINE;
						}
						checkedHeavilyBorderedText(d, sm, c.name, f, bgF, DARK_INK, PARCHMENT, c.textX, c.textY, 1000);
					}

					if (checked(sm, cx - CoatOfArms.SHIELD_W / 2, cy - 8 - sz - CoatOfArms.SHIELD_H, CoatOfArms.SHIELD_W, 24 + sz + CoatOfArms.SHIELD_H)) {
						// qqDPS Update this hook.
						Hook selectHook = new Hook(Hook.Type.HOVER, Hook.Type.MOUSE_1_CLICKED) {
							@Override
							public void run(Input in, Pt p, Type type) {
								nextHoverCity = c;
								if (type == Hook.Type.MOUSE_1_CLICKED) {
									d.state.hasClicked();
									selectedFleet = null;
									menuCity = c;
								}
							}
						};
						if (c.isTown) {
							if (zoom > 9) {
								d.hook(cx - sz - 1, cy - 8 - sz - 9, sz * 2 + 2, sz * 2 + 2 + 16 + 9, selectHook);
							} else {
								d.hook(cx - sz - 1, cy - 1, sz * 2 + 2, sz * 2 + 2, selectHook);
							}
						} else {
							d.hook(cx - CoatOfArms.SHIELD_W / 2, cy - 8 - sz - CoatOfArms.SHIELD_H, CoatOfArms.SHIELD_W, 24 + sz + CoatOfArms.SHIELD_H, selectHook);
						}
						d.hook(cx - sz, cy - sz, sz * 2, sz * 2, new Hook(Hook.Type.MOUSE_2_CLICKED) {
							@Override
							public void run(Input in, Pt p, Type type) {
								d.state.hasClicked();
								if (selectedFleet != null && !_fleetSelection.isEmpty() && selectedFleet.location != c && canTravelTo(c)) {
									travelTo(c, null, false);
								}
							}
						});
					}
				}
			}
		}
		
		// Nests
		for (int ni = 0; ni < w.map.nests.size(); ni++) {
			MonsterNest n = w.map.nests.get(ni);
			n.textDrawn = false;
			if (zoom > 9) {
				n.textDrawn = true;
				lp: for (int ej = 0; ej < w.map.empires.size(); ej++) {
					Empire em2 = w.map.empires.get(ej);
					for (int cj = 0; cj < em2.cities.size(); cj++) {
						City c2 = em2.cities.get(cj);
						if (c2.textDrawn && c2.textX + c2.textW >= n.textX && n.textX + n.textW >= c2.textX && c2.textY + c2.textH >= n.textY && n.textY + n.textH >= c2.textY) {
							n.textDrawn = false;
							break lp;
						}
					}
				}
				
				if (n.textDrawn) {
					for (int nj = 0; nj < ni; nj++) {
						MonsterNest n2 = w.map.nests.get(nj);
						if (n2.textDrawn && n2.textX + n2.textW >= n.textX && n.textX + n.textW >= n2.textX && n2.textY + n2.textH >= n.textY && n.textY + n.textH >= n2.textY) {
							n.textDrawn = false;
							break;
						}
					}
				}
			}
			
			if (n.textDrawn) {
				Fount f;
				Fount bgF;
				if (AGame.isMapSafe(n.getDisplayName())) {
					f = AGame.MAP_SMALL;
					bgF = AGame.MAP_SMALL_OUTLINE;
				} else {
					f = AGame.MAP_FALLBACK_SMALL;
					bgF = AGame.MAP_FALLBACK_SMALL_OUTLINE;
				}
				checkedHeavilyBorderedText(d, sm, n.getDisplayName(), f, bgF, DARK_RED_INK, PARCHMENT, n.textX, n.textY, 1000);
				if (n.type != null && n.upgrading) {
					d.rect(DARKER_INK, n.textX, n.textY + f.lineHeight + 1 + MyDraw.BUTTON_SPACING, 50, 6);
					d.rect(PARCHMENT, n.textX + 1, n.textY + f.lineHeight + 1 + MyDraw.BUTTON_SPACING + 1, 48, 4);
					d.rect(DARKER_INK, n.textX + 2, n.textY + f.lineHeight + 1 + MyDraw.BUTTON_SPACING + 2, 46 * n.upgradeAccumulator / n.type.msForUpgrade, 2);
					d.tooltip(n.textX, n.textY + f.lineHeight + 1 + MyDraw.BUTTON_SPACING, 50, 6, w.describeTime(n.type.msForUpgrade - n.upgradeAccumulator));
				}
			}
		}
		
		// Details
		// Towns and cities
		for (int dt = 0; dt < 2; dt++) {
			boolean doTowns = dt == 1;
			for (int ei = 0; ei < w.map.empires.size(); ei++) {
				final Empire em = w.map.empires.get(ei);
				for (int ci = 0; ci < em.cities.size(); ci++) {
					final City c = em.cities.get(ci);
					if (doTowns != c.isTown) { continue; }
					int cx = (int) (worldToScreenX(c.x));
					int cy = (int) (worldToScreenY(c.y));
					
					if (mapMode.canSwitchPOV && em != (mapModePOV == null ? w.player : mapModePOV)) {
						d.iconButton(cx - MyDraw.ICON_BUTTON_SZ / 2, cy - MyDraw.ICON_BUTTON_SZ / 2, eye, new Runnable() {
							@Override
							public void run() {
								mapModePOV = em;
							}
						}, true);
						d.tooltip(cx - MyDraw.ICON_BUTTON_SZ / 2, cy - MyDraw.ICON_BUTTON_SZ / 2, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("switch_pov"));
					}
					
					c.infoX = cx + 20;
					Fount f;
					if (AGame.isMapSafe(c.name)) {
						f = c.isTown ? AGame.MAP_SMALL : AGame.MAP;
					} else {
						f = c.isTown ? AGame.MAP_FALLBACK_SMALL : AGame.MAP_FALLBACK;
					}
					c.infoY = cy + f.lineHeight / 2;
					c.infoW = 0;
					c.infoH = 0;
					
					int x = cx + 20;
					int y = cy + ((c.isTown && zoom <= 9) ? -8 : (f.lineHeight / 2));
					int maxW = 10000;//w.map.water[0].length - c.x - 30;
					
					int iconX = x;
					
					if ((w.player == null || w.player.cities.contains(c) || hasSpy(c) || debug) && (!c.isTown || zoom > 9)) {
						Hero guvnor = Hero.get(c, w.map);
						if (guvnor != null) {
							checkedBlit(d, sm, evolveIconBg, PARCHMENT, x, y);
							checkedBlit(d, sm, heroIcon, DARKER_INK, x, y);
							int hx = x;
							int hw = 16;
							String deets = guvnor.getDetails(true);
							if (guvnor.getLoyalty() < EmpireStat.CORRUPTABLE_LOYALTY.get(em.bonuses)) {
								checkedBlit(d, sm, disloyalIconBg, PARCHMENT, x + 16 + MyDraw.BUTTON_SPACING, y);
								checkedBlit(d, sm, disloyalIcon, DARKER_INK, x + 16 + MyDraw.BUTTON_SPACING, y);
								deets += "\n\n" + _t("low_loyalty_notice");
								hx += 16 + MyDraw.BUTTON_SPACING;
								hw += 16 + MyDraw.BUTTON_SPACING;
							}
							checkedHeavilyBorderedText(d, sm, guvnor.getName(), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, hx + 16 + MyDraw.BUTTON_SPACING, y, maxW);
							d.tooltip(x, y, hw + MyDraw.BUTTON_SPACING + (int) d.textSize(guvnor.getName(), AGame.FOUNT).x + 2, Math.max(AGame.FOUNT.lineHeight, 16), deets);
							y += Math.max(16, AGame.FOUNT.lineHeight) + MyDraw.BUTTON_SPACING;
						}
						if (c.edict != null) {
							checkedBlit(d, sm, c.edict.iconBackground, PARCHMENT, x, y);
							checkedBlit(d, sm, c.edict.icon, DARKER_INK, x, y);
							checkedHeavilyBorderedText(d, sm, c.edict.getName(), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x + 16 + MyDraw.BUTTON_SPACING, y, maxW);
							d.tooltip(x, y, 16 + MyDraw.BUTTON_SPACING + (int) d.textSize(c.edict.getName(), AGame.FOUNT).x + 2, Math.max(AGame.FOUNT.lineHeight, 16), _t("edict_time_left", w.describeTime(c.edictTimeLeft)) + "\n" + c.edict.getDesc(null, w.map));
							y += Math.max(16, AGame.FOUNT.lineHeight) + MyDraw.BUTTON_SPACING;
						}
					}
				
					if (c.plagueLevel != null && (c.plagueLevel.visible.get(em.bonuses) || debug)) {
						if (checked(sm, iconX, y + AGame.FOUNT.lineHeight + 2, 16, 16)) {
							d.borderedBlit(plagueIcon, c.plagueLevel.color, PARCHMENT, iconX, y);
							d.tooltip(iconX, y, 16, 16, c.plagueLevel.tooltip());
						}
						iconX += 16 + MyDraw.BUTTON_SPACING;
					}
					
					// qqDPS temp info
					/*y += 10;
					checkedHeavilyBorderedText(d, sm, "SpyQuality: " + StrategicAI.spyQuality(w.player, em, c, w.map, new ArrayList<MapLocation>()), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
					y += 30;*/
					
					if (c.isRitualSite && w.map.areRitualSitesVisible()) {
						if (checked(sm, iconX, y, 16, 16)) {
							d.borderedBlit(ritualSiteIcon, DARK_INK, PARCHMENT, iconX, y);
							d.tooltip(iconX, y, 16, 16, _t("ritual_site_tooltip"));
						}
						iconX += 16 + MyDraw.BUTTON_SPACING;
					}
					
					if ((w.player == null || w.player.cities.contains(c) || hasSpy(c) || debug) && (!c.isTown || zoom > 9)) {
						StringBuilder sb = new StringBuilder();
						int un = c.unrest(w.map, sb);
						if (un > 10) {
							Clr tint = MILD_UNREST;
							if (un > 60) {
								tint = MORE_UNREST;
							} else if (un > 30) {
								tint = SOME_UNREST;
							}
							if (checked(sm, iconX, y, 16, 16)) {
								d.borderedBlit(unrestIcon, tint, PARCHMENT, iconX, y);
								d.tooltip(iconX, y, 16, 16, _t("Unrest_") + un + "\n\n" + sb.toString());
							}
							iconX += 16 + MyDraw.BUTTON_SPACING;
						}
						
						if (!c.isConnectedToCapital(em, w.map)) {
							if (checked(sm, iconX, y, 16, 16)) {
								d.borderedBlit(disconnectedIcon, DARK_INK, PARCHMENT, iconX, y);
								d.tooltip(iconX, y, 16, 16, _t("Giving_Away_Disconnected_From_Capital") + "\n" + _t("Unrest_") + "+" + EmpireStat.DISCONNECTED_FROM_CAPITAL_UNREST.get(em.bonuses));
							}
							iconX += 16 + MyDraw.BUTTON_SPACING;
						}

						if (c.getEconomicDamage() > 0) {
							if (checked(sm, iconX, y, 16, 16)) {
								d.borderedBlit(econDamageIcon, DARK_INK, PARCHMENT, iconX, y);
								d.tooltip(iconX, y, 16, 16, _t("econ_damage_" + c.getEconomicDamage()));
							}
							iconX += 16 + MyDraw.BUTTON_SPACING;
						}

						for (int i = 0; i < c.upgrades.size(); i++) {
							CityUpgradeType cut = c.upgrades.get(i);
							if (cut.icon != null && !cut.special) {
								if (checked(sm, iconX, y, 16, 16)) {
									d.borderedBlit(cut.icon.get(em.bonuses), DARK_INK, PARCHMENT, iconX, y);
									d.tooltip(iconX, y, 16, 16, cut.getName() + "\n\n" + cut.getDesc(em.bonuses, w.map, !c.takeoverNeeded && c.takeoverMethod == null));
								}
								iconX += 16 + MyDraw.BUTTON_SPACING;
							}
						}

						if (iconX > x) {
							y += 16 + MyDraw.BUTTON_SPACING;
						}
						
						sb = new StringBuilder();
						String incomeInfo = "$" + c.adjustedIncome(w.map, sb);
						checkedHeavilyBorderedText(d, sm, incomeInfo, AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
						d.tooltip(x - 1, y, (int) d.textSize(incomeInfo, AGame.FOUNT).x + 2, AGame.FOUNT.lineHeight + 2, sb.toString());
						y += AGame.FOUNT.lineHeight + 2;
						sb = new StringBuilder();

						if (w.player != null && hasSpy(c)) {
							Spy spy = w.player.getSpyFor(c);
							checkedHeavilyBorderedText(d, sm, _t("Spy_Active") + " (" + spy.networkLevel + " / " + EmpireStat.MAX_SPY_NETWORK_LEVEL.get(w.player.bonuses) + ")", AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
							int ttw = (int) d.textSize(_t("Spy_Active") + " (" + spy.networkLevel + " / " + EmpireStat.MAX_SPY_NETWORK_LEVEL.get(w.player.bonuses) + ")", AGame.FOUNT).x;
							String tt = spyNetworkLevelInfo(spy, c);
							y += AGame.FOUNT.height;
							if (spy.actionCooldown > 0) {
								d.rect(RED_INK, x, y, 100, 6);
								d.rect(PARCHMENT, x + 1, y + 1, 98, 4);
								d.rect(RED_INK, x + 2, y + 2, 96 * (spy.actionCooldownStartValue - spy.actionCooldown) / spy.actionCooldownStartValue, 2);
								tt += "\n" + _t("spy_action_cooldown", w.describeTime(spy.actionCooldown));
								ttw = Math.max(ttw, 100);
								d.tooltip(x, y - AGame.FOUNT.lineHeight, ttw, AGame.FOUNT.lineHeight + 8, tt);
								y += 8;
							} else {
								d.tooltip(x, y - AGame.FOUNT.lineHeight, ttw, AGame.FOUNT.lineHeight, tt);
							}
						}
						if (c.takeoverMethod != null) {
							d.rect(RED_INK, x, y, 100, 6);
							d.rect(PARCHMENT, x + 1, y + 1, 98, 4);
							d.rect(RED_INK, x + 2, y + 2, 96 * c.takeoverAmount / c.takeoverMethod.timeTaken.get(em.bonuses), 2);
							y += 8;
							checkedHeavilyBorderedText(d, sm, c.takeoverMethod.getName(), AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
							//checkedHeavilyBorderedText(d, sm, w.describeTime(c.takeoverMethod.takeoverTime - c.takeoverAmount), AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
							//y += AGame.FOUNT.height;
						} else if (!c.constructing.isEmpty()) {
							d.rect(DARKER_INK, x, y, 100, 6);
							d.rect(PARCHMENT, x + 1, y + 1, 98, 4);
							d.rect(DARKER_INK, x + 2, y + 2, 96 * c.constructing.get(0).progress / c.baseConstructionTimeCost(c.constructing.get(0), em.bonuses, w.map), 2);
							y += 8;
							checkedHeavilyBorderedText(d, sm, c.constructing.get(0).desc(), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
						}
						if (c.strike > 0) {
							d.tooltip(x, y, 100, 8 + AGame.FOUNT.height * 2, _t("strike_tooltip"));
							d.rect(RED_INK, x, y, 100, 6);
							d.rect(PARCHMENT, x + 1, y + 1, 98, 4);
							d.rect(RED_INK, x + 2, y + 2, 96 * (City.STRIKE_LENGTH - c.strike) / City.STRIKE_LENGTH, 2);
							y += 8;
							checkedHeavilyBorderedText(d, sm, _t("strike"), AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
							//checkedHeavilyBorderedText(d, sm, w.describeTime(c.strike), AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
							//y += AGame.FOUNT.height;
						}
						
						c.infoH = y - c.infoY;
						c.infoW = Math.max(100, iconX - x + 16);
						
						//d.rect(new Clr(255, 0, 0, 100), c.infoX, c.infoY, c.infoW, c.infoH);
						if (debug) {
							checkedHeavilyBorderedText(d, sm, "SpyActions: " + em.spyActionsDone, AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
							checkedHeavilyBorderedText(d, sm, "$" + em.getMoney() + " d" + em.incomeBalance(w.map), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
							checkedHeavilyBorderedText(d, sm, em.constructionStrategy.name, AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
							y += AGame.FOUNT.height;
							if (c.getConstructionTarget() != null) {
								checkedHeavilyBorderedText(d, sm, "Target: " + c.getConstructionTarget().getName() + " $" + c.getConstructionTarget().getCost(), AGame.FOUNT, AGame.FOUNT_OUTLINE, DARKER_INK, PARCHMENT, x, y, maxW);
							}
						}
					} else {
						if (iconX > x) {
							y += 16 + MyDraw.BUTTON_SPACING;
						}
						
						Spy spy = w.player == null ? null : w.player.getSpyFor(c);
						if (spy != null) {
							if (spy.infiltrationTimeout > 0) {
								d.rect(RED_INK, x, y, 100, 6);
								d.rect(PARCHMENT, x + 1, y + 1, 98, 4);
								d.rect(RED_INK, x + 2, y + 2, 96 * (Spy.INFILTRATION_TIME - spy.infiltrationTimeout) / Spy.INFILTRATION_TIME, 2);
								y += 8;
								checkedHeavilyBorderedText(d, sm, _t("Infiltrating_City") + " (" + w.describeTime(spy.infiltrationTimeout) + ")", AGame.FOUNT, AGame.FOUNT_OUTLINE, RED_INK, PARCHMENT, x, y, maxW);
								y += AGame.FOUNT.height;
							}
						}
						Empire owner = w.map.owner(c);
						for (int i = 0; i < c.upgrades.size(); i++) {
							CityUpgradeType cut = c.upgrades.get(i);
							if (!cut.special && cut.icon != null && w.map.eraModifier != null && w.map.eraModifier.eraStartSpawnUpgrade == cut) {
								if (checked(sm, iconX, y, 16, 16)) {
									d.borderedBlit(cut.icon.get(owner.bonuses), DARK_INK, PARCHMENT, iconX, y);
									d.tooltip(iconX, y, 16, 16, cut.getName() + "\n\n" + cut.getDesc(owner.bonuses, w.map, !c.takeoverNeeded && c.takeoverMethod == null));
								}
								iconX += 16 + MyDraw.BUTTON_SPACING;
							}
						}
					}
				}
			}
		}
		
		// Nests
		for (int ni = 0; ni < w.map.nests.size(); ni++) {
			MonsterNest n = w.map.nests.get(ni);
			int nx = (int) (worldToScreenX(n.x));
			int ny = (int) (worldToScreenY(n.y));
			String info = null;
			boolean inCity = false;
			Empire cityOwner = null;
			if (w.player == null) {
				inCity = true;
			} else {
				for (City c : w.player.cities) {
					if (c.nestsInTerritory(w.map).contains(n)) {
						inCity = true;
						break;
					}
				}
			}
			for (Empire e : w.map.empires) {
				for (City c : e.cities) {
					if (c.nestsInTerritory(w.map).contains(n)) {
						cityOwner = e;
					}
				}
			}
			BonusSet nestBS = cityOwner == null ? BonusSet.empty() : cityOwner.bonuses;
			if (inCity && n.type != null && n.type.incomeModifierPercentage.get(nestBS) != 0) {
				info = _t("x_city_income", (n.type.incomeModifierPercentage.get(nestBS) > 0 ? "+" : "") + n.type.incomeModifierPercentage.get(nestBS) + "%");
			} else if (inCity && n.type != null && n.type.incomeModifier.get(nestBS) != 0) {
				info = _t("x_city_income", (n.type.incomeModifier.get(nestBS) > 0 ? "+" : "") + n.type.incomeModifier.get(nestBS));
			}

			if (info != null) {
				int sz = g.currentGUIScale == GUIScale.SMALL? 8 : 16;
				Fount f;
				if (AGame.isMapSafe(n.getDisplayName())) {
					f = AGame.MAP_SMALL;
				} else {
					f = AGame.MAP_FALLBACK_SMALL;
				}
				Rect r = d.textSize(info, AGame.FOUNT_OUTLINE, nx + 7 + sz, ny + f.lineHeight / 2, 1000);
				n.infoX = (int) r.x;
				n.infoY = (int) r.y;
				n.infoW = (int) r.width;
				n.infoH = (int) r.height;
			}
			
			n.infoDrawn = false;
			if (zoom > 14 && info != null) {				
				n.infoDrawn = true;
				lp: for (int ej = 0; ej < w.map.empires.size(); ej++) {
					Empire em2 = w.map.empires.get(ej);
					for (int cj = 0; cj < em2.cities.size(); cj++) {
						City c2 = em2.cities.get(cj);
						if (c2.textDrawn && c2.textX + c2.textW >= n.infoX && n.infoX + n.infoW >= c2.textX && c2.textY + c2.textH >= n.infoY && n.infoY + n.infoH >= c2.textY) {
							n.infoDrawn = false;
							break lp;
						}
						if (c2.infoDrawn && c2.infoX + c2.infoW >= n.infoX && n.infoX + n.infoW >= c2.infoX && c2.infoY + c2.infoH >= n.infoY && n.infoY + n.infoH >= c2.infoY) {
							n.infoDrawn = false;
							break lp;
						}
					}
				}

				if (n.infoDrawn) {
					for (int nj = 0; nj < w.map.nests.size(); nj++) {
						MonsterNest n2 = w.map.nests.get(nj);
						if (n2 == n) { continue; }
						if (n2.textDrawn && n2.textX + n2.textW >= n.infoX && n.infoX + n.infoW >= n2.textX && n2.textY + n2.textH >= n.infoY && n.infoY + n.infoH >= n2.textY) {
							n.infoDrawn = false;
							break;
						}
						if (nj < ni && n2.infoDrawn && n2.infoX + n2.infoW >= n.infoX && n.infoX + n.infoW >= n2.infoX && n2.infoY + n2.infoH >= n.infoY && n.infoY + n.infoH >= n2.infoY) {
							n.infoDrawn = false;
							break;
						}
					}
				}
			}
			
			if (n.infoDrawn) {
				checkedHeavilyBorderedText(d, sm, info, AGame.FOUNT, AGame.FOUNT_OUTLINE, DARK_RED_INK, PARCHMENT, n.infoX, n.infoY, 1000);
			}
		}
		
		// Towns and cities, arms
		for (Empire em : w.map.empires) {
			for (final City c : em.cities) {
				int cx = (int) (worldToScreenX(c.x));
				int cy = (int) (worldToScreenY(c.y));
				int sz = g.currentGUIScale == GUIScale.SMALL ? 9 : 17;
				if (!c.isTown) {
					em.arms.layout.drawShield(em.arms, d, cx - CoatOfArms.SHIELD_W / 2, cy - 8 - sz - CoatOfArms.SHIELD_H, 1, DARK_INK);
				} else if (zoom > 9) {
					d.rect(PARCHMENT, cx - 9, cy - 8 - sz - 9, 18, 18);
					em.arms.draw(d, cx - 8, cy - 8 - sz - 8, 16);
				}
			}
		}

		// Fleets
		if (highlitTravelConnection != null && selectedFleet != null && selectedFleet.location != null) {
			if (Fleet.canFly(getFleetSelection())) {
				d.dottedLine(DARKER_INK, 2, 8, worldToScreenX(highlitTravelConnection.x), worldToScreenY(highlitTravelConnection.y), worldToScreenX(selectedFleet.location.x), worldToScreenY(selectedFleet.location.y));
			} else {
				Road it = selectedFleet.getRoadPath(highlitTravelConnection, w.map);
				if (it != null) {
					highlightRoadLine(sm, graphics, it);
				}
			}
		}
		
		for (MonsterNest mn : w.map.nests) {
			renderLocationFleets(d, sm, mn, cursor);
		}
		
		for (Empire em : w.map.empires) {
			for (City c : em.cities) {
				renderLocationFleets(d, sm, c, cursor);
			}
		}
		
		for (MonsterNest mn : w.map.nests) {
			for (final Fleet fl : mn.getFleets()) {
				renderInTransitFleet(d, sm, fl, null, mn);
			}
		}
		
		// Render in-transit fleets.
		for (int passes = 0; passes < 2; passes++) {
			for (Empire e : w.map.empires) {
				if ((e == w.player) != (passes == 1)) { continue; }
				for (final Fleet fl : e.getFleets()) {
					if (fl.fleeDestinationNeeded && fl.location == null) {
						renderFleet(d, sm, fl, e, null);
					} else {
						renderInTransitFleet(d, sm, fl, e, null);
					}
					
					int x = (int) worldToScreenX(fl.realX(w.map));
					int y = (int) worldToScreenY(fl.realY(w.map));

					if (e == w.player) {
						if (fl.fleeDestinationNeeded && e == w.player) {
							int tw = x - FLEET_IMG_SZ / 2 - MyDraw.UI_SPACING - (int) (d.textSize(_t("Select_destination_to_fly_to").toUpperCase(), AGame.FOUNT).x);
							checkedHeavilyBorderedText(d, sm, _t("Select_destination_to_fly_to").toUpperCase(), AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.RED, PARCHMENT, tw, y - FLEET_IMG_SZ / 2, 10000);
							if (!fl.canTravelAnywhereElse(w.map) && !fl.getAllShipsOfType(ShipType.LANDSHIP).isEmpty()) {
								tw = x - FLEET_IMG_SZ / 2 - MyDraw.UI_SPACING - (int) (d.textSize(_t("roadless_landships"), AGame.FOUNT).x);
								checkedHeavilyBorderedText(d, sm, _t("roadless_landships"), AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.RED, PARCHMENT, tw, y - FLEET_IMG_SZ / 2 + AGame.FOUNT.lineHeight, 10000);
							}
						}
					}
					if (fl.inTransit() && (e == w.player || w.player == null || w.player.canInspectFleets(e, w.map)) && checked(sm, x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ)) {
						d.hook(x - FLEET_IMG_SZ / 2, y - FLEET_IMG_SZ / 2, FLEET_IMG_SZ, FLEET_IMG_SZ, new Hook("fleet", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
							@Override
							public void run(Input in, Pt p, Type type) {
								if (d.state.canClick()) {
									menuCity = null;
									selectedFleet = fl;
									setFleetSelection(fl.getAllNonAnchoredShips());
									fleetScrollBar.offset = 0;
									d.state.hasClicked();
								}
							}
						});
					}
				}
			}
		}
		
		if (selectCityForOfferEditor != null) {
			d.getHooks().list.clear();
			hs.list.clear();
			d.state.glowRects.clear();
			
			Empire other = selectCityForOfferEditor.offer.rel.other(w.player);
			
			// Add buttons to select towns and cities
			if (selectCityForOfferEditor.demandCity) {
				for (final City c : other.cities) {
					if (selectCityForOfferEditor.offer.hasCityTransfer(c)) { continue; }
					String text = _t("do_demand");
					String tt = null;
					if (!other.playerControlled) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						int amt = other.diplomacyAI.giveCityQuality(c, other, w.player, w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						tt = sb.toString();
						text += (amt > 0 ? " (+" : " (") + amt + ")";
					}
					int x = (int) (worldToScreenX(c.x)) - d.bw(text) / 2;
					int y = (int) (worldToScreenY(c.y)) + 10;
					d.button(x, y, d.bw(text), text, new Runnable() {
						@Override
						public void run() {
							selectCityForOfferEditor.demand(c);
						}
					});
					d.highlight("demandCity-" + c.name, g, x, y, d.bw(text), MyDraw.BUTTON_H);
					if (tt != null) {
						d.tooltip(x, y, d.bw(text), MyDraw.BUTTON_H, tt);
					}
				}
			} else {
				for (final City c : w.player.cities) {
					if (selectCityForOfferEditor.offer.hasCityTransfer(c)) { continue; }
					String text = _t("do_cede");
					String tt = null;
					if (!other.playerControlled) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						int amt = other.diplomacyAI.receiveCityQuality(c, other, w.player, w.map, sb);
						tt = sb.toString();
						text += (amt > 0 ? " (+" : " (") + amt + ")";
					}
					int x = (int) (worldToScreenX(c.x)) - d.bw(text) / 2;
					int y = (int) (worldToScreenY(c.y)) + 10;
					d.button(x, y, d.bw(text), text, new Runnable() {
						@Override
						public void run() {
							selectCityForOfferEditor.cede(c);
						}
					});
					if (tt != null) {
						d.tooltip(x, y, d.bw(text), MyDraw.BUTTON_H, tt);
					}
				}
			}
			d.drawTopBar(sm);
			d.text(_t("Select_city_to_transfer"), AGame.BIG_FOUNT, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET);
			int bw = d.bw(_t("Cancel"));
			int x = sm.width - MyDraw.SIDE_CLEARANCE - bw;
			d.button(x, MyDraw.TOP_BAR_INSET, bw, _t("Cancel"), null, new Runnable() {
				@Override
				public void run() {
					selectCityForOfferEditor.cancelCitySelection();
				}
			});
			
			if (!w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains)) {
				ConquestHelpSystem.render(d, MyDraw.SIDE_CLEARANCE, sm.height - MyDraw.SIDE_CLEARANCE - ConquestHelpSystem.getHeight(d, this, this, sm.width / 3),  this, this, sm.width / 3);
			}
			
			return;
		}
		
		if (showCityForDiplomacy) {
			int bw = d.bw(_t("Leave"));
			int x = sm.width - MyDraw.SIDE_CLEARANCE - bw;
			d.button(x, MyDraw.TOP_BAR_INSET, bw, _t("Leave"), null, new Runnable() {
				@Override
				public void run() {
					showCityForDiplomacy = false;
				}
			});
			return;
		}
		
		PerfStats.mark("ss fleets");
		// Coronation indication MERGEME: does this fit with all the other UI?
		for (Empire em : w.map.empires) {
			for (final City c : em.cities) {
				int cx = (int) (worldToScreenX(c.x));
				int cy = (int) (worldToScreenY(c.y));
								
				if (c.coronation) {
					int progressW = Math.max(100, (int) d.textSize(_t("coronation_preparations"), AGame.FOUNT).x);
					int w = progressW + MyDraw.PANEL_INSET * 2;
					int h = AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING + MyDraw.PROGRESS_BAR_H + MyDraw.PANEL_INSET * 2;
					int corY = cy - 8 - CoatOfArms.SHIELD_H - MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING - h;
					d.drawShadowedPanel(cx - w / 2, corY, w, h);
					corY += MyDraw.PANEL_INSET;
					d.text(_t("coronation_preparations"), AGame.FOUNT, cx - progressW / 2, corY);
					corY += AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
					d.progressBar(cx - progressW / 2, corY, progressW, Math.max(0, c.coronationProgress) * 1.0 / EmpireStat.CORONATION_TIME.get(em.bonuses()));
				}
				
				if (c.finalRitual) {
					int progressW = Math.max(100, (int) d.textSize(_t("final_ritual_progress"), AGame.FOUNT).x);
					int w = progressW + MyDraw.PANEL_INSET * 2;
					int h = AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING + MyDraw.PROGRESS_BAR_H + MyDraw.PANEL_INSET * 2;
					int ritY = cy - 8 - 16 - MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING - h;
					d.drawShadowedPanel(cx - w / 2, ritY, w, h);
					ritY += MyDraw.PANEL_INSET;
					d.text(_t("final_ritual_progress"), AGame.FOUNT, cx - progressW / 2, ritY);
					ritY += AGame.FOUNT.lineHeight + MyDraw.BUTTON_SPACING;
					d.progressBar(cx - progressW / 2, ritY, progressW, Math.max(0, c.finalRitualProgress) * 1.0 / EmpireStat.FINAL_RITUAL_TIME.get(em.bonuses()));
				}
			}
		}
		
		// Messages
		for (MonsterNest c : w.map.nests) {
			int cx = (int) (worldToScreenX(c.x)) - 8;
			int cy = (int) (worldToScreenY(c.y)) - Math.max(20, AGame.FOUNT_OUTLINE.lineHeight) - MyDraw.SCROLL_EL_SPACING;
			for (MapLocation.Message msg : c.messages) {
				if (w.player != null && msg.owner != w.player && msg.owner != null) { continue; }
				Img icon = null;
				switch (msg.type) {
					case RAID:
					case CLEAR_NEST:
					case REPEL:
					case CONQUEST: icon = fight; break;
					case LOST_SHIP: icon = shipLoss; break;
					case REVOLT: icon = fight; break;
					case REARM: icon = rearm; break;
					case MONSTER_OCCUPATION: icon = c.type == null ? null : c.type.getMapImage(); break;
				}
				if (icon != null) {
					checkedBorderedBlit(d, sm, icon, msg.type.clr, PARCHMENT, cx, cy);
				}
				if (zoom > 9) {
					checkedHeavilyBorderedText(d, sm, msg.getText(w.player), AGame.FOUNT, AGame.FOUNT_OUTLINE, msg.type.clr, PARCHMENT, cx + 20, cy, 1000);
				}
				cy -= Math.max(20, AGame.FOUNT_OUTLINE.lineHeight) + MyDraw.SCROLL_EL_SPACING;
			}
		}
		
		for (Empire em : w.map.empires) {
			for (City c : em.cities) {
				int cx = (int) (worldToScreenX(c.x)) - 8;
				int cy = (int) (worldToScreenY(c.y)) - Math.max(20, AGame.FOUNT_OUTLINE.lineHeight) - MyDraw.SCROLL_EL_SPACING;
				for (City.Message msg : c.messages) {
					if (w.player != null && msg.owner != w.player && msg.owner != null) { continue; }
					Img icon = null;
					switch (msg.type) {
						case ECON_RECOVERY: icon = econRecovery; break;
						case RAID:
						case CLEAR_NEST:
						case REPEL:
						case CONQUEST: icon = fight; break;
						case LOST_SHIP: icon = shipLoss; break;
						case REVOLT: icon = revolt; break;
						case REARM: icon = rearm; break;
						case NEW_UPGRADE: icon = newUpgrade; break;
						case PLAGUE: icon = plague; break;
						case STRIKE: icon = strike; break;
					}
					if (icon != null) {
						checkedBorderedBlit(d, sm, icon, msg.type.clr, PARCHMENT, cx, cy);
					}
					if (zoom > 9) {
						checkedHeavilyBorderedText(d, sm, msg.getText(w.player), AGame.FOUNT, AGame.FOUNT_OUTLINE, msg.type.clr, PARCHMENT, cx + 20, cy, 1000);
					}
					cy -= Math.max(20 + MyDraw.SCROLL_EL_SPACING, AGame.FOUNT_OUTLINE.lineHeight);
				}
			}
		}
		
		// Menus
		if (w.player != null) {
			for (MonsterNest n : w.map.nests) {
				if (selectedFleet != null && !_fleetSelection.isEmpty()) {
					renderLocationMoveMenu(n, d, sm, hs, cursor);
				}
			}

			for (Empire em : w.map.empires) {
				for (City c : em.cities) {
					if (selectedFleet != null && !_fleetSelection.isEmpty()) {
						renderLocationMoveMenu(c, d, sm, hs, cursor);
					}
				}
			}
			
			if (selectedFleet != null && hoverFleet != null && selectedFleet.interceptTarget != hoverFleet && w.map.owner(hoverFleet) != w.player) {
				renderInterceptInfo(d, sm, hoverFleet);
			}
			
			if (w.player.conquerorInfo != null) {
				renderConquerorInfo(d, sm);
			}
		}
		
		PerfStats.mark("ss menus");
		
		int sidePanelOffset = 0;
		int mapInfoOffset = -MyDraw.SIDE_CLEARANCE;
		if (selectedFleet != null) {
			sidePanelOffset = -fleetListWidth(sm);
			mapInfoOffset = sidePanelOffset - MyDraw.UI_SPACING;
		} else if (fullCityMenuVisible()) {
			sidePanelOffset -= cityDetailWidth;
			mapInfoOffset = sidePanelOffset - MyDraw.UI_SPACING;
		}
		
		if (showStatsDuringGame) {
			d.getHooks().list.clear();
			hs.list.clear();
			d.state.glowRects.clear();
			statsDisplay.render(w.map, w.map.stats, w.player == null ? BonusSet.empty() : w.player.bonuses, d, MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_H + MyDraw.UI_SPACING, sm.width - MyDraw.SIDE_CLEARANCE * 2, sm.height - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING - MyDraw.SIDE_CLEARANCE);
		}
		
		PerfStats.mark("ss cityMessages");
		
		if (w.player != null) {
			int mapModeX = sm.width - (MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING) * StrategicMapMode.values().length + MyDraw.BUTTON_SPACING + mapInfoOffset - MyDraw.PANEL_INSET;
			d.drawPanel(mapModeX - MyDraw.PANEL_INSET, sm.height - MyDraw.SIDE_CLEARANCE - MyDraw.ICON_BUTTON_SZ - MyDraw.PANEL_INSET, (MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING) * StrategicMapMode.values().length + MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 2, 17);
			for (final StrategicMapMode smm : StrategicMapMode.values()) {
				if (!smm.available(w)) { continue; }
				d.iconToggle(mapModeX, sm.height - MyDraw.SIDE_CLEARANCE - MyDraw.ICON_BUTTON_SZ, smm.icon, new Runnable() {
					@Override
					public void run() {
						mapMode = smm;
						if (!smm.canSwitchPOV) {
							mapModePOV = null;
						}
					}
				}, mapMode == smm, true);
				d.tooltip(mapModeX, sm.height - MyDraw.SIDE_CLEARANCE - MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, smm.getName());
				mapModeX += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			}
			if (mapMode != StrategicMapMode.NORMAL) {
				ArrayList<StrategicMapModeInfoLine> info = mapMode.getInfo(w);
				int mapInfoWidth = (int) d.textSize(mapMode.getTitle(), AGame.FOUNT).x;
				if (mapModePOV != null && mapModePOV != w.player) {
					mapInfoWidth = Math.max(mapInfoWidth, (int) d.textSize(_t("POV_x", mapModePOV.getName()), AGame.FOUNT).x);
				}
				for (StrategicMapModeInfoLine line : info) {
					mapInfoWidth = Math.max(mapInfoWidth, (int) d.textSize(line.string, AGame.FOUNT).x);
				}
				mapInfoWidth += 22 + MyDraw.BUTTON_SPACING;
				int infoLineHeight = Math.max(22 + MyDraw.BUTTON_SPACING, AGame.FOUNT.lineHeight);
				int mapInfoHeight = info.size() * infoLineHeight + MyDraw.UI_SPACING + AGame.FOUNT.lineHeight + (mapModePOV != null && mapModePOV != w.player ? AGame.FOUNT.lineHeight : 0);
				int mapInfoX = sm.width + mapInfoOffset - mapInfoWidth - MyDraw.PANEL_INSET;
				int mapInfoY = sm.height - MyDraw.SIDE_CLEARANCE - MyDraw.ICON_BUTTON_SZ - MyDraw.UI_SPACING - mapInfoHeight - MyDraw.PANEL_INSET;
				d.drawPanel(mapInfoX - MyDraw.PANEL_INSET, mapInfoY - MyDraw.PANEL_INSET, mapInfoWidth + MyDraw.PANEL_INSET * 2, mapInfoHeight + MyDraw.PANEL_INSET * 2, 22);
				d.text(mapMode.getTitle(), AGame.FOUNT, mapInfoX, mapInfoY);
				mapInfoY += AGame.FOUNT.lineHeight;
				if (mapModePOV != null && mapModePOV != w.player) {
					d.text(_t("POV_x", mapModePOV.getName()), AGame.FOUNT, mapInfoX, mapInfoY);
					mapInfoY += AGame.FOUNT.lineHeight;
				}
				mapInfoY += MyDraw.UI_SPACING;
				for (StrategicMapModeInfoLine line : info) {
					d.rect(INK, mapInfoX, mapInfoY, 22, 22);
					d.rect(line.clr.mix(0.65, PARCHMENT), mapInfoX + 1, mapInfoY + 1, 20, 20);
					if (line.icon != null) {
						d.blit(line.icon, DARK_INK, mapInfoX + 11 - line.icon.srcWidth / 2, mapInfoY + 11 - line.icon.srcHeight / 2);
					}
					int textYOffset = g.currentGUIScale == GUIScale.SMALL ? 0 :  - AGame.FOUNT.lineHeight / 4;
					d.text(line.string, AGame.FOUNT, mapInfoX + 22 + MyDraw.BUTTON_SPACING, mapInfoY + textYOffset, 10000);
					mapInfoY += infoLineHeight;
				}
			}
		}

		PerfStats.mark("ss cityMessages");
				
		String date = w.map.date(w.player == null ? BonusSet.empty() : w.player.bonuses);
		ArrayList<Moon> moons = Moon.visibleMoons(w.player == null ? BonusSet.empty() : w.player.bonuses);
		String eraName = w.map.eraModifier.name.equals("NO_BONUS") ? _t("normal_era") : _t("bonus_" + w.map.eraModifier.name);
		String eraDesc;
		if (Lang.flavour()) {
			eraDesc = w.map.eraModifier.name.equals("NO_BONUS") ? "": ("\n" + _t(w.map.eraModifier.desc.get(w.player == null ? BonusSet.empty() : w.player.bonuses), w.map.newEraDetail[0], w.map.newEraDetail[1], w.map.newEraDetail[2]) + "\n\n" + _t(w.map.eraModifier.effectsDesc.get(w.player == null ? BonusSet.empty() : w.player.bonuses)));
		} else {
			eraDesc = w.map.eraModifier.name.equals("NO_BONUS") ? "": (_t(w.map.eraModifier.effectsDesc.get(w.player == null ? BonusSet.empty() : w.player.bonuses)));
		}
		Img eraIcon = w.map.eraModifier.name.equals("NO_BONUS") ? noEraIcon : w.map.eraModifier.eraIcon;
		Season season = Season.getSeason(w.map.age);
		int iconsW = (16 + MyDraw.BUTTON_SPACING) * moons.size() + season.icon.srcWidth + MyDraw.UI_SPACING + 16 + MyDraw.BUTTON_SPACING;
		int dateW = (int) d.textSize(date, AGame.FOUNT).x;
		int datePanelX = sm.width - dateW - iconsW - MyDraw.PANEL_INSET * 2 + sidePanelOffset;
		d.drawPanel(datePanelX, MyDraw.TOP_BAR_H - MyDraw.PANEL_INSET, dateW + MyDraw.PANEL_INSET * 3 + iconsW, AGame.FOUNT.lineHeight + MyDraw.PANEL_INSET * 3, 23);
		d.text(date, AGame.FOUNT, sm.width - dateW - iconsW - MyDraw.PANEL_INSET + sidePanelOffset, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET);
		d.tooltip(sm.width - dateW - iconsW - MyDraw.PANEL_INSET + sidePanelOffset, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET, dateW, AGame.FOUNT.lineHeight, _t(EmpireStat.CALENDAR_NAME.get(w.player == null ? BonusSet.empty() : w.player.bonuses)));
		int x3 = sm.width - MyDraw.PANEL_INSET - iconsW + MyDraw.UI_SPACING + sidePanelOffset;
		d.blit(season.icon, x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - season.icon.srcHeight / 2);
		d.tooltip(x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - season.icon.srcHeight / 2, season.icon.srcWidth, season.icon.srcHeight, _t("season_" + season.name));
		x3 += season.icon.srcWidth + MyDraw.BUTTON_SPACING;
		for (Moon m : moons) {
			MoonPhase p = MoonPhase.calc(w.map.age, m);
			d.blit(p.img, x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - p.img.srcHeight / 2);
			d.tooltip(x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - p.img.srcHeight / 2, p.img.srcWidth, p.img.srcHeight, _t("moon_" + m.name + "_desc") + "\n" + _t("phase_" + p.name()));
			x3 += p.img.srcWidth + MyDraw.BUTTON_SPACING;
		}

		d.blit(eraIcon, x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - eraIcon.srcHeight / 2);
		StringBuilder info = new StringBuilder();
		info.append(eraName);
		info.append(eraDesc);
		info.append("\n\n").append(_t("x_base_research", EmpireStat.RESEARCH.get(w.map.worldBonuses())));
		info.append("\n").append(_t("x_spies_available", EmpireStat.MAX_SPIES.get(w.map.worldBonuses())));
		info.append("\n").append(_t("x_unrest_per_city", w.map.unrestPerCity(w.map.worldBonuses())));
		info.append("\n").append(_t("x_unrest_per_town", w.map.unrestPerTown(w.map.worldBonuses())));
		if (w.map.era != null && !w.map.era.isFinalEra()) {
			int msUntilNextEra = StrategicEra.msUntilNextEra(w.map);
			info.append("\n\n").append(_t("x_era_time_left", w.describeTime(msUntilNextEra)));
		}
		d.tooltip(x3, MyDraw.TOP_BAR_H + MyDraw.PANEL_INSET + AGame.FOUNT.lineHeight / 2 - eraIcon.srcHeight / 2, eraIcon.srcWidth, eraIcon.srcHeight, info.toString());
		
		int y3 = MyDraw.TOP_BAR_H + AGame.FOUNT.lineHeight + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
		// Hero victory conditions
		ArrayList<Hero> winnyHeroes = new ArrayList<Hero>();
		if (w.map.toggles.contains(ConquestToggle.HERO_VICTORY)) {
			for (int hi = 0; hi < w.map.heroes.size(); hi++) {
				Hero h = w.map.heroes.get(hi);
				for (int si = 0; si < h.type.stats.size(); si++) {
					if (h.inEmpire != null && h.hired && h.type.stats.get(si).winOn100) {
						winnyHeroes.add(h);
						break;
					}
				}
			}
		}
		if (!winnyHeroes.isEmpty()) {
			int portraitSz = 32 + MyDraw.PANEL_BORDER_W * 2;
			int namesW = 0;
			for (Hero h : winnyHeroes) {
				namesW = Math.max(namesW, (int) d.textSize(h.getName(), AGame.FOUNT).x);
			}
			int barW = Math.min(sm.width / 4, Math.max(namesW, sm.width / 8));
			int panelW = barW + 32 + 32 + MyDraw.UI_SPACING + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 3;
			int heroH = Math.max(portraitSz, AGame.FOUNT.lineHeight + MyDraw.PROGRESS_BAR_H);
			int panelH = Math.max(16, AGame.FOUNT.lineHeight) + (heroH + MyDraw.BUTTON_SPACING) * winnyHeroes.size() + MyDraw.PANEL_INSET * 2;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 12);
			d.tooltip(x3, y3, panelW, panelH, _t("hero_victory_tooltip"));
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			panelW -= MyDraw.PANEL_INSET * 3;
			d.blit(heroVictoryIcon, x3, y3);
			d.text(_t("hero_victory_title"), AGame.FOUNT, x3 + 16 + MyDraw.UI_SPACING, y3);
			y3 += Math.max(16, AGame.FOUNT.lineHeight);
			for (final Hero h : winnyHeroes) {
				y3 += MyDraw.BUTTON_SPACING;
				h.inEmpire.arms.draw(d, x3 + MyDraw.PANEL_BORDER_W, y3 + MyDraw.PANEL_BORDER_W, 32);
				d.tooltip(x3, y3, portraitSz, portraitSz, h.inEmpire.getTooltip(w.player, w.map));
				if (w.player != null && h.inEmpire != w.player && w.has(ConquestToggle.DIPLOMACY)) {
					d.hook(x3, y3, portraitSz, portraitSz, new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input input, Pt pt, Type type) {
							diplomacy.showPopupOnly = false;
							diplomacy.focusEmpire = h.inEmpire;
							showDiplomacy = true;
						}
					});
				}
				d.drawPanelBorder(x3, y3, portraitSz, portraitSz);
				d.portraitBlit(h.type.img, x3 + portraitSz + MyDraw.BUTTON_SPACING + MyDraw.PANEL_BORDER_W, y3 + MyDraw.PANEL_BORDER_W, 32, 32);
				d.drawPanelBorder(x3 + portraitSz + MyDraw.BUTTON_SPACING, y3, portraitSz, portraitSz);
				d.tooltip(x3 + portraitSz + MyDraw.BUTTON_SPACING, y3, portraitSz, portraitSz, h.getDetails(true));
				if (h.inEmpire == w.player) {
					d.hook(x3 + portraitSz + MyDraw.BUTTON_SPACING, y3, portraitSz, portraitSz, new Hook(Hook.Type.MOUSE_1_CLICKED) {
						@Override
						public void run(Input input, Pt pt, Type type) {
							showHero(h);
						}
					});
				}
				d.text((h.inEmpire == w.player ? MyDraw.SELECTED_C : "") + h.getName(), AGame.FOUNT, x3 + portraitSz * 2 + MyDraw.BUTTON_SPACING * 2, y3);
				for (int si = 0; si < h.type.stats.size(); si++) {
					HeroType.Stat stat = h.type.stats.get(si);
					if (stat.winOn100) {
						d.progressBar(x3 + portraitSz * 2 + MyDraw.BUTTON_SPACING * 2, y3 + AGame.FOUNT.lineHeight, panelW - portraitSz * 2 - MyDraw.BUTTON_SPACING * 2, h.stats.get(stat.name) / 100.0);
						break;
					}
				}
				y3 += Math.max(portraitSz, AGame.FOUNT.lineHeight + MyDraw.PROGRESS_BAR_H);
			}
			y3 += MyDraw.PANEL_INSET + MyDraw.BUTTON_SPACING;
		}
		// Expedition progress
		if (w.player != null && w.player.expedition != null) {
			int barW = sm.width / 8;
			int panelW = barW + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 3;
			int panelH = Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.PANEL_INSET * 2;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 18);
			d.tooltip(x3, y3, panelW, panelH, _t("expedition_progress_tooltip", w.describeTime(EmpireStat.EXPEDITION_MS.get(w.player.bonuses) - w.player.expeditionTime)));
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			d.blit(expedition, x3, y3 + MyDraw.PROGRESS_BAR_H / 2 - 8);
			x3 += 16 + MyDraw.UI_SPACING;
			d.progressBar(x3, y3, barW, w.player.expeditionTime * 1.0 / EmpireStat.EXPEDITION_MS.get(w.player.bonuses));
			y3 += Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.PANEL_INSET + MyDraw.BUTTON_SPACING;
		}
		// Era end conditions
		if (w.player != null && w.map.eraModifier != null && w.map.eraModifier.eraEndsWhenControllingUpgrades && w.map.eraModifier.eraStartSpawnNumUpgrades > 1) {
			int numUpgrades = 0;
			ArrayList<City> cs = new ArrayList<City>();
			int maxCityNameW = 0;
			for (City c : w.map.cities()) {
				if (c.upgrades.contains(w.map.eraModifier.eraStartSpawnUpgrade)) {
					cs.add(c);
					maxCityNameW = Math.max(maxCityNameW, (int) d.textSize(c.getDisplayName(), AGame.FOUNT).x);
					if (w.player.cities.contains(c)) {
						numUpgrades++;
					}
				}
			}
			int entryH = Math.max(MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, AGame.FOUNT.lineHeight);
			int barW = Math.min(sm.width / 4, Math.max(maxCityNameW + MyDraw.UI_SPACING + MyDraw.ICON_BUTTON_SZ, sm.width / 8));
			int panelW = barW + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 3;
			int panelH = Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.PANEL_INSET * 2 + cs.size() * entryH + MyDraw.UI_SPACING - MyDraw.BUTTON_SPACING;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 11);
			d.highlight("eraGoal", g, x3, y3, panelW, panelH);
			d.tooltip(x3, y3, panelW, panelH, _t("x_y_z_upgrades_to_end_the_age", numUpgrades, w.map.eraModifier.eraStartSpawnNumUpgrades, _t("cityUpgrade_plural_" + w.map.eraModifier.eraStartSpawnUpgrade.name), _t("bonus_" + w.map.eraModifier.name)));
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			d.blit(w.map.eraModifier.eraIcon, x3, y3 + MyDraw.PROGRESS_BAR_H / 2 - 8);
			x3 += 16 + MyDraw.UI_SPACING;
			d.progressBar(x3, y3, barW, numUpgrades * 1.0 / w.map.eraModifier.eraStartSpawnNumUpgrades);
			y3 += Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.UI_SPACING;
			for (final City c : cs) {
				d.text((w.player.cities.contains(c) ? MyDraw.SELECTED_C : "") + c.getDisplayName(), AGame.FOUNT, x3, y3);
				d.iconButton(x3 + barW - MyDraw.ICON_BUTTON_SZ, y3, mapGoto, new Runnable() {
					@Override
					public void run() {
						scrollX = -c.x;
						scrollY = -c.y;
					}
				}, true);
				y3 += entryH;
			}
			y3 += MyDraw.PANEL_INSET;
		}
		if (w.player != null && w.map.eraModifier != null && w.map.eraModifier.endEraMeter != null) {
			int barW = sm.width / 8;
			int panelW = barW + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 3;
			int panelH = Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.PANEL_INSET * 2;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 12);
			d.tooltip(x3, y3, panelW, panelH, _t("x_y_z_upgrades_to_end_the_age", w.map.endEraMeter, 100, _t(w.map.eraModifier.endEraMeter), _t("bonus_" + w.map.eraModifier.name)));
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			d.blit(w.map.eraModifier.endEraMeterIcon, x3, y3 + MyDraw.PROGRESS_BAR_H / 2 - 8);
			x3 += 16 + MyDraw.UI_SPACING;
			d.progressBar(x3, y3, barW, w.map.endEraMeter / 100.0);
			y3 += Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.UI_SPACING;
			y3 += MyDraw.PANEL_INSET;
		}
		if (w.player != null && w.map.eraModifier != null && w.map.eraModifier.eraEndsWhenUpgradesBuilt != null) {
			int totalNumUpgrades = 0;
			Empire leadingEmpire = null;
			int leadingEmpireNumUpgrades = 0;
			for (Empire e : w.map.empires) {
				int empireNumUpgrades = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(w.map.eraModifier.eraEndsWhenUpgradesBuilt)) {
						totalNumUpgrades++;
						empireNumUpgrades++;
					}
				}
				if (empireNumUpgrades > leadingEmpireNumUpgrades) {
					leadingEmpire = e;
					leadingEmpireNumUpgrades = empireNumUpgrades;
				}
			}
			StringBuilder lbsb = new StringBuilder();
			for (Empire e : w.map.empires) {
				int empireNumUpgrades = 0;
				for (City c : e.cities) {
					if (c.upgrades.contains(w.map.eraModifier.eraEndsWhenUpgradesBuilt)) {
						empireNumUpgrades++;
					}
				}
				if (empireNumUpgrades > 0) {
					lbsb.append("\n");
					lbsb.append(e == leadingEmpire ? MyDraw.SELECTED_C : "[]");
					lbsb.append(e.getName()).append(": ").append(empireNumUpgrades);
				}
			}
			if (lbsb.length() == 0) {
				lbsb.append("\n").append(_t("Nothing_Built"));
			}
			String leaderboard = _t("cityUpgrade_plural_" + w.map.eraModifier.eraEndsWhenUpgradesBuilt.name) + ":" + lbsb.toString();
			Pt lbSize = d.textSize(leaderboard, AGame.FOUNT);
			int barW = Math.min(sm.width / 4, Math.max((int) lbSize.x, sm.width / 8));
			int panelW = barW + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 3;
			int panelH = Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.PANEL_INSET * 2 + MyDraw.UI_SPACING + (int) lbSize.y;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 13);
			d.highlight("eraGoal", g, x3, y3, panelW, panelH);
			int requiredUpgrades = (int) Math.ceil(w.map.cities().size() * w.map.eraModifier.eraEndsWhenProportionHasUpgrade);
			String tt = _t("x_y_z_upgrades_to_end_the_age", totalNumUpgrades, requiredUpgrades, _t("cityUpgrade_plural_" + w.map.eraModifier.eraEndsWhenUpgradesBuilt.name), _t("bonus_" + w.map.eraModifier.name)) +
					"\n\n" + _t("build_the_most", _t("cityUpgrade_plural_" + w.map.eraModifier.eraEndsWhenUpgradesBuilt.name), _t("bonus_" + w.map.eraModifier.name));
			if (leadingEmpire == w.player) {
				tt += "\n\n" + _t("you_are_in_the_lead");
			}
			d.tooltip(x3, y3, panelW, panelH, tt);
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			d.blit(w.map.eraModifier.eraIcon, x3, y3 + MyDraw.PROGRESS_BAR_H / 2 - 8);
			x3 += 16 + MyDraw.UI_SPACING;
			d.progressBar(x3, y3, barW, totalNumUpgrades * 1.0 / requiredUpgrades);
			y3 += Math.max(16, MyDraw.PROGRESS_BAR_H) + MyDraw.UI_SPACING;
			d.text(leaderboard, AGame.FOUNT, x3, y3);
			y3 += MyDraw.PANEL_INSET + (int) lbSize.y;
		}
		if (w.player != null && w.map.eraModifier != null && w.map.eraModifier.eraEndsWhenClearingNest) {
			int nestNameW = (int) d.textSize(_t(w.map.eraModifier.eraStartSpawnNest.name + "_displayName"), AGame.FOUNT).x;
			int panelW = nestNameW + 16 + MyDraw.UI_SPACING * 2 + MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 3;
			int panelH = Math.max(Math.max(16, MyDraw.ICON_BUTTON_SZ), AGame.FOUNT.lineHeight) + MyDraw.PANEL_INSET * 2;
			x3 = sm.width - panelW + MyDraw.PANEL_INSET + sidePanelOffset;
			d.drawPanel(x3, y3, panelW, panelH, 14);
			d.highlight("eraGoal", g, x3, y3, panelW, panelH);
			d.tooltip(x3, y3, panelW, panelH, _t("destroy_nest_x_to_end_age", _t(w.map.eraModifier.eraStartSpawnNest.name + "_displayName"), _t("bonus_" + w.map.eraModifier.name)));
			x3 += MyDraw.PANEL_INSET;
			y3 += MyDraw.PANEL_INSET;
			d.blit(w.map.eraModifier.eraIcon, x3, y3);
			x3 += 16 + MyDraw.UI_SPACING;
			d.text(_t(w.map.eraModifier.eraStartSpawnNest.name + "_displayName"), AGame.FOUNT, x3, y3);
			x3 += nestNameW + MyDraw.UI_SPACING;
			d.iconButton(x3, y3, mapGoto, new Runnable() {
				@Override
				public void run() {
					if (w.map.eraModifier == null) { return; }
					for (MonsterNest mn : w.map.nests) {
						if (mn.type == w.map.eraModifier.eraStartSpawnNest) {
							scrollX = -mn.x;
							scrollY = -mn.y;
						}
					}
				}
			}, true);
			y3 += panelH - MyDraw.PANEL_INSET;
		}
					
		PerfStats.mark("ss date");
		
		d.drawTopBar(sm);
		
		CoatOfArms coa = w.player == null ? CoatOfArms.spectatorArms() : w.player.arms;
		int topBarYStart = g.currentGUIScale == GUIScale.LARGE ? 11 : 3;
		int borderSize = g.currentGUIScale == GUIScale.LARGE ? 4 : 2;
		int coaSize = 16;
		while (coaSize + 16 <= MyDraw.TOP_BAR_H - topBarYStart - borderSize * 2) {
			coaSize += 16;
		}
		int coaYOffset = (MyDraw.TOP_BAR_H - topBarYStart - borderSize * 2 - coaSize) / 2;
		if (coa != null) {
			coa.draw(d, MyDraw.SIDE_CLEARANCE + borderSize, topBarYStart + borderSize + coaYOffset, coaSize);
		}
		d.drawPanelBorder(MyDraw.SIDE_CLEARANCE, topBarYStart + coaYOffset, coaSize + borderSize * 2, coaSize + borderSize * 2);
		
		/*
		if (coa == null) {
			d.rect(Clr.DARK_GREY, 9, 9, 96, 96);
		} else {
			coa.draw(d, 9, 9, 96);
		}*/
		
		int x = MyDraw.SIDE_CLEARANCE + MyDraw.TOP_BAR_H - 3 + MyDraw.UI_SPACING;
		
		if (w.player != null) {
			StringBuilder moneyInfo = new StringBuilder();
			moneyInfo.append(w.player.getMoney() == 0 ? MyDraw.ERROR_C : "").append("$").append(w.player.getMoney()).append("[]");
			int inc = w.player.incomeBalance(w.map);
			if (inc > 0) {
				moneyInfo.append(" +").append(inc);
			} else if (inc < 0) {
				moneyInfo.append(" ").append(MyDraw.ERROR_C).append(inc);
			} else {
				moneyInfo.append(" 0");
			}
			
			Pt ts = d.textSize(moneyInfo.toString(), AGame.BIG_FOUNT);
			if (w.player.getMoney() == 0) {
				d.tooltip(x, MyDraw.TOP_BAR_INSET, ts.x, ts.y, _t("bonus_BROKE_desc"));
			}
			d.text(moneyInfo.toString(), AGame.BIG_FOUNT, x, MyDraw.TOP_BAR_INSET);
			x += d.textSize("$99999 +999", AGame.FOUNT).x + MyDraw.UI_SPACING;
			
			if (w.has(ConquestToggle.REPUTATION)) {
				Empire.ReputationLevel lvl = w.player.getReputationLevel();
				int rep = w.player.getReputation();
				StringBuilder repInfo = new StringBuilder();
				repInfo.append("[] ").append(_t("Rep_")).append(rep).append(" ").append(_t("_" + lvl.name() + "_rep_"));
				d.text(repInfo.toString(), AGame.BIG_FOUNT, x, MyDraw.TOP_BAR_INSET);
				ts = d.textSize(repInfo.toString(), AGame.BIG_FOUNT);
				d.tooltip(x, MyDraw.TOP_BAR_INSET, ts.x, ts.y, w.player.getRepInfo(w));
				d.highlight("reputation", g, x, MyDraw.TOP_BAR_INSET, (int) ts.x, (int) ts.y);
				x += ts.x + MyDraw.BUTTON_SPACING;
			}

			if (w.has(ConquestToggle.CORONATION)) {
				if (w.map.isCoronationPossible(w.player)) {
					if (w.player.isCoronationReady(w.map)) {
						d.blit(coronationIcon, MyDraw.SELECTED, x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8);
						Hero h = w.player.getCoronationEnablingHero(w.map);
						if (h != null) {
							d.tooltip(x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8, 16, 16, _t("hero_coronation_info_message_ready", h.getName(), w.map.requiredCitiesForCoronation()));
						} else {
							d.tooltip(x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8, 16, 16, _t("coronation_info_message_ready", EmpireStat.CORONATION_REPUTATION.explain(w.player.bonuses()), w.map.requiredCitiesForCoronation()));
						}
					} else {
						d.blit(coronationIcon, INK, x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8);
						d.tooltip(x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8, 16, 16, _t("coronation_info_message", EmpireStat.CORONATION_REPUTATION.explain(w.player.bonuses()), w.map.requiredCitiesForCoronation(), w.player.numFullCities()));
					}

					x += 16 + MyDraw.UI_SPACING;
				}
				
				if (w.map.isFinalRitualPossible(w.player)) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ritualVictoryInfo")).append("\n");
					if (EmpireStat.CAN_SEE_RITUAL_SITES.get(w.player.bonuses)) {
						ArrayList<City> controlled = new ArrayList<City>();
						ArrayList<City> missing = new ArrayList<City>();
						for (City c : w.map.cities()) {
							if (c.isRitualSite) {
								if (w.player.cities.contains(c)) {
									controlled.add(c);
								} else {
									missing.add(c);
								}
							}
						}
						if (!controlled.isEmpty()) {
							sb.append("\n").append(_t("ritualVictoryInfoControlledSites"));
							for (City c : controlled) {
								sb.append("\n  ").append(c.getDisplayName());
							}
						}
						if (!missing.isEmpty()) {
							sb.append("\n").append(_t("ritualVictoryInfoUncontrolledSites"));
							for (City c : missing) {
								sb.append("\n  ").append(c.getDisplayName());
							}
						}
			
					} else {
						sb.append("\n").append(_t("ritualVictoryInfoSitesDiscoveredNo"));
					}
					sb.append("\n").append(_t(EmpireStat.CAN_DO_FINAL_RITUAL.get(w.player.bonuses) ? "ritualVictoryInfoTechResearchedYes" : "ritualVictoryInfoTechResearchedNo"));

					if (w.player.isDoingFinalRitual()) {
						City ritualC = null;
						for (City c : w.player.cities) {
							if (c.finalRitual) {
								ritualC = c;
								break;
							}
						}
						sb.append("\n\n").append(_t("ritualVictoryInfoProgress", w.describeTime(EmpireStat.FINAL_RITUAL_TIME.get(w.player.bonuses()) - ritualC.finalRitualProgress)));
					} else {
						if (w.player.isFinalRitualReady(w.map)) {
							sb.append("\n\n").append(_t("ritualVictoryInfoReady"));
						} else {
							sb.append("\n\n").append(_t("ritualVictoryInfoUnready"));
						}
					}
					
					d.blit(ritualSiteIcon, w.player.isFinalRitualReady(w.map) ? MyDraw.SELECTED : INK, x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8);
					d.tooltip(x, MyDraw.TOP_BAR_INSET + AGame.BIG_FOUNT.lineHeight / 2 - 8, 16, 16, sb.toString());
					x += 16 + MyDraw.UI_SPACING;
				}
			}
			d.button(x, MyDraw.TOP_BAR_INSET, d.bw(_t("Empire_Details")), _t("Empire_Details"), Keys.getText("strategic_empire_details", "E", false), new Runnable() {
				@Override
				public void run() {
					showEmpireDetails = true;
				}
			});
			x += d.bw(_t("Empire_Details")) + MyDraw.BUTTON_SPACING;
			// qqDPS DEBUG
			/*d.button(x, MyDraw.TOP_BAR_INSET, d.bw("Inc"), "Inc", null, new Runnable() {
				@Override
				public void run() {
					for (IncidentType t : Loadable.all(IncidentType.class)) {
						for (Incident inc : t.getPossibleIncidents(w.player, w.map, -1)) {
							System.out.println(inc);
						}
					}
				}
			});
			x += d.bw("Inc") + MyDraw.BUTTON_SPACING;*/
			if (w.has(ConquestToggle.DIPLOMACY)) {
				int bw = d.bw(_t("Diplomacy"));
				d.button(x, MyDraw.TOP_BAR_INSET, bw, _t("Diplomacy"), Keys.getText("strategic_diplomacy", "I", false), new Runnable() {
					@Override
					public void run() {
						diplomacy.showPopupOnly = false;
						diplomacy.focusEmpire = null;
						showDiplomacy = true;
					}
				});
				d.highlight("diplomacy", g, x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H);
				x += bw + MyDraw.BUTTON_SPACING;
			}
			if (EHeroes.it.enabled && w.map.heroFrequency.frequencyMultiplier != 0) {
				int bw = d.bw(_t("Heroes"));
				d.button(x, MyDraw.TOP_BAR_INSET, bw, _t("Heroes"), Keys.getText("strategic_heroes", "H", false), new Runnable() {
					@Override
					public void run() {
						showHeroes = true;
						heroToShow = null;
						cityForHero = null;
						shipForHero = null;
					}
				});
				d.highlight("showHeroes", g, x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H);
				x += bw + MyDraw.BUTTON_SPACING;
			}
			final StrategicScreen self = this;
			int maxTechW = sm.width - MyDraw.ICON_BUTTON_SZ - MyDraw.SIDE_CLEARANCE - d.tw(_t("Menu")) - 3 * MyDraw.UI_SPACING - 5 * (MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING) - x;
			if (w.map.techSpeed.speedMultiplier != 0) {
				if (w.player.research == null) {
					if (w.player.isAllResearchDone(w.map)) {
						d.button(x, MyDraw.TOP_BAR_INSET, Math.min(maxTechW, d.bw(_t("Research"))), _t("Research"), "R", new InputRunnable() {
							@Override
							public void run(Input in) {
								g.s = new TechScreen(self);
							}
						}, true);
					} else {
						d.goldbutton(x, MyDraw.TOP_BAR_INSET, Math.min(maxTechW, d.bw(_t("Research"))), _t("Research"), "R", new InputRunnable() {
							@Override
							public void run(Input in) {
								g.s = new TechScreen(self);
							}
						}, true);
					}
					d.highlight("researchButton", g, x, MyDraw.TOP_BAR_INSET, d.bw(_t("Research")), MyDraw.BUTTON_H);
					x += d.bw(_t("Research")) + MyDraw.BUTTON_SPACING;
				} else {
					int textW = (int) d.textSize(_t("tech_" + w.player.research.name), AGame.FOUNT).x;
					d.text(_t("tech_" + w.player.research.name), AGame.FOUNT, x, MyDraw.TOP_BAR_INSET);
					int barW = Math.min(maxTechW - MyDraw.BUTTON_SPACING - MyDraw.ICON_BUTTON_SZ, textW);
					d.progressBar(x, MyDraw.TOP_BAR_INSET + AGame.FOUNT.lineHeight, barW, MyDraw.PROGRESS_BAR_H - AGame.FOUNT.lineHeight, w.player.researchPoints * 1.0 / w.player.research.cost(w.player, w.map));
					d.iconButton(x + barW + MyDraw.BUTTON_SPACING, MyDraw.TOP_BAR_INSET, researchIcon, new Runnable() {
						@Override
						public void run() {
							g.s = new TechScreen(self);
						}
					}, true);
					String timeCost = "?";
					StringBuilder sb = new StringBuilder();
					int researchOutput = w.player.researchOutput(w.map, sb, true);
					if (researchOutput > 0) {
						timeCost = sb.toString() + "\n" + w.describeTime((w.player.research.cost(w.player, w.map) - w.player.getResearchPoints(w.player.research)) / researchOutput);
					}
					d.tooltip(x, MyDraw.TOP_BAR_INSET, barW, MyDraw.PROGRESS_BAR_H, _t("Research") + ": " + _t("tech_" + w.player.research.name) + "\n" + timeCost);
					x += barW + MyDraw.BUTTON_SPACING * 2 + MyDraw.ICON_BUTTON_SZ;
				}
			}
			
			if (SimplePref.CHEATS.get() && !w.isMultiplayer()) {
				int cmw = d.bw(_t("Cheat"));
				cmw = Math.max(cmw, d.bw(_t("money_cheat")));
				cmw = Math.max(cmw, d.bw(_t("tech_cheat")));
				cmw = Math.max(cmw, d.tw(_t("do_not_attack_cheat")));
				cmw = Math.max(cmw, d.bw(_t("switch_empire_cheat")));
				cmw = Math.max(cmw, d.bw(_t("constant_raids_cheat")));
				d.toggle(x, MyDraw.TOP_BAR_INSET, cmw, _t("Cheat"), null, new InputRunnable() {
					@Override
					public void run(Input in) {
						showCheatsMenu = !showCheatsMenu;
						showSwitchEmpireMenu = false;
						showSwitchDifficultyMenu = false;
						showStartIncidentMenu = false;
						showGetHeroMenu = false;
					}
				}, showCheatsMenu, true);
				if (showCheatsMenu) {
					int my = MyDraw.TOP_BAR_INSET + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
					if (showGetHeroMenu) {
						int xx = 20;
						int yy = 80;
						for (final Hero h : w.map.heroes) {
							if (h.inEmpire == w.player) { continue; }
							d.portraitBlit(h.type.img, xx, yy, 50, 50);
							d.button(xx + 60, yy, 200, h.getName(), new Runnable() {
								@Override
								public void run() {
									h.clearStats();
									h.setInShip(null);
									h.setInCity(null);
									h.inEmpire = w.player;
									h.hired = true;
									showCheatsMenu = false;
									w.usedCheatCommand = true;
								}
							});
							yy += 60;
							if (yy + 50 > sm.height) {
								xx += 270;
								yy = 80;
							}
						}
					} else if (showStartIncidentMenu) {
						int xx = 20;
						int yy = 80;
						d.heavilyBorderedText("Start Incident\nSuitable for testing only!", AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.WHITE, Clr.BLACK, xx, yy, 10000);
						yy += AGame.FOUNT_OUTLINE.lineHeight * 2 + MyDraw.UI_SPACING;
						d.button(xx, yy, 250, "Cancel", new Runnable() {
							@Override
							public void run() {
								showStartIncidentMenu = false;
							}
						});
						yy += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						d.toggle(xx, yy, 250, "Other Side", null, new InputRunnable() {
							@Override
							public void run(Input in) {
								switchIncidentRoles = !switchIncidentRoles;
							}
						}, switchIncidentRoles, true);
						yy += MyDraw.BUTTON_H + MyDraw.UI_SPACING;
						for (final IncidentType it : Loadable.all(IncidentType.class)) {
							d.button(xx, yy, 250, it.name, new Runnable() {
								@Override
								public void run() {
									Empire other = null;
									for (Empire e : w.map.empires) {
										if (e != w.player) {
											other = e;
											break;
										}
									}
									Empire a = switchIncidentRoles ? other : w.player;
									Empire b = switchIncidentRoles ? w.player : other;
									Incident in = new Incident(w.map.incidentIDCounter++, it, a, b, a.cities.get(0), b.cities.get(0), w.map.nests.get(0), w.map.nests.get(1));
									in.applyCurrentEvent(w.map);
									w.map.incidents.add(in);
									w.usedCheatCommand = true;
									showStartIncidentMenu = false;
									showCheatsMenu = false;
								}
							});
							yy += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
							if (yy + MyDraw.BUTTON_H > sm.height - 80) {
								xx += 270;
								yy = 80;
							}
						}
					} else if (showSwitchEmpireMenu) {
						int entryH = Math.max(34, MyDraw.BUTTON_H) + MyDraw.BUTTON_SPACING;
						int menuW = cmw;
						for (Empire e : w.map.empires) {
							menuW = Math.max(menuW, 34 + MyDraw.BUTTON_SPACING + d.bw(e.getName()));
						}
						d.drawPanel(x - MyDraw.PANEL_INSET, my, menuW + MyDraw.PANEL_INSET * 2,  MyDraw.PANEL_INSET * 2 + entryH * (w.map.empires.size() - 1) + MyDraw.BUTTON_H, -1);
						my += MyDraw.PANEL_INSET;
						d.button(x, my, menuW, _t("Cancel"), new Runnable() {
							@Override
							public void run() {
								showSwitchEmpireMenu = false;
							}
						});
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						for (final Empire e : w.map.empires) {
							if (e == w.player) { continue; }
							d.rect(MyDraw.SELECTED, x, my, 34, 34);
							e.arms.draw(d, x + 1, my + 1, 32);
							d.button(x + 34 + MyDraw.BUTTON_SPACING, my, menuW - 34 - MyDraw.BUTTON_SPACING, e.getName(), new Runnable() {
								@Override
								public void run() {
									w.usedCheatCommand = true;
									w.player.playerControlled = false;
									e.easySpyCheat = w.player.easySpyCheat;
									w.player.easySpyCheat = false;
									w.player = e;
									w.player.playerControlled = true;
									w.playerOffers.clear();
									selectedFleet = null;
									menuCity = null;
									showSwitchEmpireMenu = false;
									showCheatsMenu = false;
								}
							});
							my += entryH;
						}
					} else if (showSwitchDifficultyMenu) {
						int entryH = MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						int menuW = cmw;
						ArrayList<DifficultyLevel> dls = Loadable.all(DifficultyLevel.class);
						for (DifficultyLevel dl : dls) {
							menuW = Math.max(menuW, d.tw(dl.getName()));
						}
						d.drawPanel(x - MyDraw.PANEL_INSET, my, menuW + MyDraw.PANEL_INSET * 2,  MyDraw.PANEL_INSET * 2 + entryH * dls.size() + MyDraw.BUTTON_H, -1);
						my += MyDraw.PANEL_INSET;
						d.button(x, my, menuW, _t("Cancel"), new Runnable() {
							@Override
							public void run() {
								showSwitchDifficultyMenu = false;
							}
						});
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						for (final DifficultyLevel dl : dls) {
							d.toggle(x, my, menuW, dl.getName(), null, new InputRunnable() {
								@Override
								public void run(Input in) {
									w.map.difficulty = dl;
									w.usedCheatCommand = true;
									showSwitchDifficultyMenu = false;
									showCheatsMenu = false;
								}
							}, dl == w.map.difficulty, true);
							d.tooltip(x, my, menuW, MyDraw.BUTTON_H, dl.getDesc());
							my += entryH;
						}
					} else {
						int numItems = 11;
						if (!w.map.eraModifier.name.equals("NO_BONUS")) { numItems++; }
						if (EHeroes.it.enabled) {
							numItems++;
						}
						if (SimplePref.EDITOR_TOOLS.get() && w.map.incidentFrequency.frequencyMultiplier != 0) {
							numItems++;
						}
						d.drawPanel(x - MyDraw.PANEL_INSET, my, cmw + MyDraw.PANEL_INSET * 2,  MyDraw.PANEL_INSET * 2 + (MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING) * numItems - MyDraw.BUTTON_SPACING, -1);
						my += MyDraw.PANEL_INSET;
						d.button(x, my, cmw, _t("money_cheat"), new Runnable() {
							@Override
							public void run() {
								w.player.setMoney(1000000);
								w.usedCheatCommand = true;
							}
						});
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("money_cheat_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

						d.button(x, my, cmw, _t("tech_cheat"), new Runnable() {
							@Override
							public void run() {
								for (Tech t : Loadable.all(Tech.class)) {
									boolean alreadyHas = false;
									for (Tech.Choice c : t.choices) {
										if (w.player.techs.contains(c)) { alreadyHas = true; }
									}
									if (!alreadyHas) {
										w.player.techs.add(t.choices.get(0));
										w.player.bonuses.addAll(t.choices.get(0).bonuses);
									}
								}
								w.player.research = null;
								w.player.researchQueue.clear();
								w.usedCheatCommand = true;
							}
						});
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("tech_cheat_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

						d.toggle(x, my, cmw, _t("do_not_attack_cheat"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								w.player.doNotAttackCheat = !w.player.doNotAttackCheat;
								w.usedCheatCommand = true;
							}
						}, w.player.doNotAttackCheat, true);
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("do_not_attack_cheat_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.toggle(x, my, cmw, _t("cheat_deactivate_AI"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								w.deactivateAICheat = !w.deactivateAICheat;
								w.usedCheatCommand = true;
							}
						}, w.deactivateAICheat, true);
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("cheat_deactivate_AI_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.toggle(x, my, cmw, _t("cheat_compliant_AI"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								w.compliantAICheat = !w.compliantAICheat;
								w.usedCheatCommand = true;
							}
						}, w.compliantAICheat, true);
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("cheat_compliant_AI_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.toggle(x, my, cmw, _t("constant_raids_cheat"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								w.map.constantRaidsCheat = !w.map.constantRaidsCheat;
								w.usedCheatCommand = true;
							}
						}, w.map.constantRaidsCheat, true);
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("constant_raids_cheat_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.toggle(x, my, cmw, _t("cheat_easy_spy"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								w.player.easySpyCheat = !w.player.easySpyCheat;
								w.usedCheatCommand = true;
							}
						}, w.player.easySpyCheat, true);
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("cheat_easy_spy_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;

						d.button(x, my, cmw, _t("switch_empire_cheat"), new Runnable() {
							@Override
							public void run() {
								showSwitchEmpireMenu = true;
							}
						});
						d.tooltip(x, my, cmw, MyDraw.BUTTON_H, _t("switch_empire_cheat_tooltip"));
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.button(x, my, cmw, _t("switch_difficulty_cheat"), new Runnable() {
							@Override
							public void run() {
								showSwitchDifficultyMenu = true;
							}
						});
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						d.button(x, my, cmw, _t("start_era_cheat"), new Runnable() {
							@Override
							public void run() {
								debugShowEraMenu = true;
							}
						});
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						if (!w.map.eraModifier.name.equals("NO_BONUS")) {
							d.button(x, my, cmw, _t("end_era_cheat"), new Runnable() {
								@Override
								public void run() {
									ArrayList<Empire> winners = new ArrayList<Empire>();
									winners.add(w.player);
									w.map.eraModifier.endPrematurely(w.map, winners);
								}
							});
							my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						}
						d.toggle(x, my, cmw, _t("campaign_stats"), null, new InputRunnable() {
							@Override
							public void run(Input in) {
								showStatsDuringGame = !showStatsDuringGame;
							}
						}, showStatsDuringGame, true);
						my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						
						if (EHeroes.it.enabled && w.map.heroFrequency.frequencyMultiplier != 0) {
							d.button(x, my, cmw, "Get Hero", new Runnable() {
								@Override
								public void run() {
									showGetHeroMenu = true;
								}
							}, w.player != null);
							my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						}
						
						if (SimplePref.EDITOR_TOOLS.get() && w.map.incidentFrequency.frequencyMultiplier != 0) {
							d.button(x, my, cmw, "Start Incident", new Runnable() {
								@Override
								public void run() {
									showStartIncidentMenu = true;
								}
							}, w.player != null && IncidentType.get(w.player, w.map) == null);
							my += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						}
					}
				}				
				x += cmw + MyDraw.BUTTON_SPACING;
			}
		}
		
		/*if (w.player != null && w.isMultiplayer()) {
			d.iconButton(x, MyDraw.TOP_BAR_INSET, editAirship, new Runnable() {
				@Override
				public void run() {
					doEdit(ShipType.AIRSHIP);
				}
			}, true);
			d.tooltip(x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Airship_Editor"));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			d.iconButton(x, MyDraw.TOP_BAR_INSET, editLandship, new Runnable() {
				@Override
				public void run() {
					doEdit(ShipType.LANDSHIP);
				}
			}, true);
			d.tooltip(x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Landship_Editor"));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
			d.iconButton(x, MyDraw.TOP_BAR_INSET, editBuilding, new Runnable() {
				@Override
				public void run() {
					doEdit(ShipType.BUILDING);
				}
			}, true);
			d.tooltip(x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Building_Editor"));
			x += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
		}*/
		
		PerfStats.mark("ss topBar");
		
		if (w.combatInfo != null && combatUniScreen != null) {
			d.goldbutton(x, MyDraw.TOP_BAR_INSET, d.bw(_t("Combat")), _t("Combat"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					g.s = combatUniScreen;
				}
			}, true);
			x += d.bw(_t("Combat")) + MyDraw.BUTTON_SPACING;
			d.iconButton(x, MyDraw.TOP_BAR_INSET, mapGoto, new Runnable() {
				@Override
				public void run() {
					scrollX = -w.combatInfo.attackingFleets.get(0).realX(w.map);
					scrollY = -w.combatInfo.attackingFleets.get(0).realY(w.map);
				}
			}, true);
			d.tooltip(x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Show_on_map"));
		}
		
		if (selectedFleet != null) {
			renderFleetList(d, sm, hs, cursor);
		}
		
		PerfStats.mark("ss fleetList");

		for (Empire em : w.map.empires) {
			for (City c : em.cities) {
				if (w.player != null && w.player.cities.contains(c) && c.takeoverNeeded && !(w.player.conquerorInfo != null && w.player.conquerorInfo.a.get(0).city == c)) {
					menuCity = c;
				}
			}
		}
		if (menuCity != null) {
			renderCityMenu(d, sm, hs, cursor);
		}
		
		PerfStats.mark("ss cityMenu");
		
		// Pseudo-tooltips
		if (highlitTravelConnection != null && selectedFleet != null) {
			renderLocationMoveInfo(highlitTravelConnection, highlitTravelConnectionBesiege, d, sm, hs, cursor);
		}
		
		int noticesMaxY = sm.height - MyDraw.SIDE_CLEARANCE;
		
		if (w.player != null && (g.strategicHelp || g.strategicHelpHeroesAndVillains) && !showDiplomacy && !w.isMultiplayer()) {
			noticesMaxY = ConquestHelpSystem.render(d, MyDraw.SIDE_CLEARANCE, noticesMaxY - ConquestHelpSystem.getHeight(d, this, this, sm.width / 3),  this, this, sm.width / 3);
		}
		
		int baseNoticesY = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
		
		if (w.isMultiplayer()) {
			int y = MyDraw.TOP_BAR_H;
			x = MyDraw.SIDE_CLEARANCE;
			if (chatCollapsed && !chatField.focus) {
				d.iconButton(x, y, EXPAND, new Runnable() {
					@Override
					public void run() {
						chatCollapsed = false;
					}
				}, true);
				d.tooltip(x, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Show_chat"));
			} else {
				int width = StrictMath.max(200, sm.width / 4);
				int height = StrictMath.max(150, sm.height / 4);
				baseNoticesY = y + height + MyDraw.BUTTON_SPACING + MyDraw.BUTTON_H + MyDraw.UI_SPACING;
				chatAdapter.allyChat = chatForAllies;
				chatScrollBar.draw(d, x, y, width, height, chatForAllies ? w.allyChatMessages : w.chatMessages, chatAdapter);
				y += height + MyDraw.BUTTON_SPACING;
				if (chatField.focus) {
					int bw = d.bw(_t("Send"));
					chatField.renderMultilineIfNeeded(x, y, width - bw - MyDraw.BUTTON_SPACING, d, sm, hs, cursor);
					d.button(x + width - bw, y, bw, _t("Send"), null, new Runnable() {
						@Override
						public void run() {
							sendChat();
						}
					}, !chatField.getText().isEmpty());
				} else {
					boolean hasAllies = w.player != null && (!w.player.getHumanAllies(w.map).isEmpty() || w.isCoopEmpire(w.player));
					d.button(x, y, width - MyDraw.BUTTON_SPACING - MyDraw.ICON_BUTTON_SZ - (hasAllies ? MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ : 0), chatForAllies ? _t("ally_chat") : _t("Chat"), new Runnable() {
						@Override
						public void run() {
							chatField.focus = true;
						}
					});
					d.iconButton(x + width - MyDraw.ICON_BUTTON_SZ, y, COLLAPSE, new Runnable() {
						@Override
						public void run() {
							chatCollapsed = true;
						}
					}, true);
					d.tooltip(x + width - MyDraw.ICON_BUTTON_SZ, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Hide_chat"));
					if (hasAllies) {
						d.iconToggle(x + width - MyDraw.ICON_BUTTON_SZ * 2 - MyDraw.BUTTON_SPACING, y, allyChatToggle, new Runnable() {
							@Override
							public void run() {
								chatForAllies = !chatForAllies;
							}
						}, chatForAllies, true);
						d.tooltip(x + width - MyDraw.ICON_BUTTON_SZ * 2 - MyDraw.BUTTON_SPACING, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("ally_chat_toggle"));
					}
				}
			}
		}
		
		PerfStats.mark("ss chat");
		int noticesY = baseNoticesY;
		
		if (w.player != null) {
			int noticeColX = MyDraw.SIDE_CLEARANCE;
			int maxNoticeW = 0;
			int noticeWLimit = sm.width / 3;
			
			final Incident inc = IncidentType.get(w.player, w.map);
			final IncidentType.Event evt = inc == null ? null : inc.getEvent(w.player);
			if (evt != eventAnnounced) {
				eventAnnounced = null;
			}
			if (inc != null && evt != null) {
				if (evt != eventAnnounced) {
					notificationSounds.add(new NotificationSound("spark", 0.5));
					eventAnnounced = evt;
				}
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2;
				int textX = noticeColX + MyDraw.PANEL_INSET;
				int extraW = 0;
				int textMinH = 0;
				if (inc.b != null && !evt.hideOtherEmpire) {
					extraW += 16 + MyDraw.UI_SPACING;
					textX += 16 + MyDraw.UI_SPACING;
					textMinH = Math.max(16, MyDraw.ICON_BUTTON_SZ);
					if (evt.mapPin != null) {
						extraW += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
						textMinH += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
					}
				} else if (evt.mapPin != null) {
					extraW += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
					textMinH = MyDraw.ICON_BUTTON_SZ;
				}
				
				textWLimit -= extraW;
				String text = MyDraw.TITLE_C + evt.getTitle(inc, w.player) + "[]\n\n" + evt.getText(inc, w.player, w);
				
				ArrayList<IncidentType.EventOptionEntry> options = evt.getOptions(inc, w.player, w.map);
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int textH = (int) sz.height;
				int nw = (int) sz.width + extraW;
				int nh = Math.max(textH, textMinH) + MyDraw.UI_SPACING + MyDraw.PROGRESS_BAR_H + options.size() * (MyDraw.BUTTON_SPACING + MyDraw.BUTTON_H) + MyDraw.PANEL_INSET * 2;
				for (IncidentType.EventOptionEntry o : options) {
					nw = Math.max(nw, Math.min(textWLimit, d.bw(o.text)));
				}
				nw += MyDraw.PANEL_INSET * 2;
				maxNoticeW = nw;
				d.drawPanel(noticeColX, noticesY, nw, nh, inc.type.pattern);
				nw -= MyDraw.PANEL_INSET * 2;
				int nx = noticeColX + MyDraw.PANEL_INSET;
				int ny = noticesY + MyDraw.PANEL_INSET;
				d.text(text, AGame.FOUNT, textX, ny, textWLimit);
				int sideButtonY = ny;
				if (inc.b != null && !evt.hideOtherEmpire) {
					final Empire other = inc.a == w.player ? inc.b : inc.a;
					other.getArms().draw(d, nx, ny, 16);
					d.tooltip(nx, ny, 16, 16, other.getTooltip(w.player, w.map));
					d.iconButton(noticeColX + nw + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, sideButtonY, diplomacy.details, new Runnable() {
						@Override
						public void run() {
							diplomacy.showPopupOnly = false;
							diplomacy.focusEmpire = other;
							showDiplomacy = true;
						}
					}, true);
					sideButtonY += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (evt.mapPin != null) {
					d.iconButton(noticeColX + nw + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, sideButtonY, mapGoto, new Runnable() {
						@Override
						public void run() {
							if (evt.mapPin.equals("aCity") && inc.aCity != null) {
								scrollX = -inc.aCity.x;
								scrollY = -inc.aCity.y;
							}
							if (evt.mapPin.equals("bCity") && inc.bCity != null) {
								scrollX = -inc.bCity.x;
								scrollY = -inc.bCity.y;
							}
							if (evt.mapPin.equals("aNest") && inc.aNest != null) {
								scrollX = -inc.aNest.x;
								scrollY = -inc.aNest.y;
							}
							if (evt.mapPin.equals("bNest") && inc.bNest != null) {
								scrollX = -inc.bNest.x;
								scrollY = -inc.bNest.y;
							}
						}
					}, true);
				}
				ny += Math.max(textH, textMinH) + MyDraw.UI_SPACING;
				d.progressBar(nx, ny, nw, 1.0 * (Incident.TIMEOUT - inc.timeout) / Incident.TIMEOUT);
				d.tooltip(nx, ny, nw, MyDraw.PROGRESS_BAR_H, _t("incident_timeout_notice"));
				ny += MyDraw.PROGRESS_BAR_H + MyDraw.BUTTON_SPACING;
				for (final IncidentType.EventOptionEntry o : options) {
					d.button(nx, ny, nw, o.text, new Runnable() {
						@Override
						public void run() {
							w.giveCommand(msg("pickIncidentOption").put("incident", inc.id).put("eventPathDepth", inc.eventPath.size()).put("option", o.index));
						}
					}, o.enabled);
					if (SimplePref.EDITOR_TOOLS.get()) {
						StringBuilder tt = new StringBuilder();
						tt.append("Empire A: ").append(inc.a.name);
						if (inc.b != null) {
							tt.append("\nEmpire B: ").append(inc.b.name);
						}
						if (inc.aCity != null) {
							tt.append("\nCity A: ").append(inc.aCity.name);
						}
						if (inc.bCity != null) {
							tt.append("\nCity B: ").append(inc.bCity.name);
						}
						if (inc.aNest != null) {
							tt.append("\nNest A: ").append(inc.aNest.getDisplayName());
						}
						if (inc.bNest != null) {
							tt.append("\nNest B: ").append(inc.bNest.getDisplayName());
						}
						if (o.debugText != null) {
							tt.append("\nDisabled reason:\n").append(o.debugText);
						} else {
							tt.append("\nEnabled");
						}
						d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, tt.toString());
					}
					ny += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
				}
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			if (w.player.incidentNotice != null) {
				if (w.player.incidentNotice != incidentNoticeAnnounced) {
					notificationSounds.add(new NotificationSound("spark", 0.5));
					incidentNoticeAnnounced = w.player.incidentNotice;
				}
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2;
				int textX = noticeColX + MyDraw.PANEL_INSET;
				int extraW = 0;
				int textMinH = 0;
				if (w.player.incidentNoticeEmpire != null) {
					extraW += 16 + MyDraw.UI_SPACING;
					textX += 16 + MyDraw.UI_SPACING;
					textMinH = Math.max(16, MyDraw.ICON_BUTTON_SZ);
					if (w.player.incidentNoticePin != null) {
						extraW += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
						textMinH += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
					}
				} else if (w.player.incidentNoticePin != null) {
					extraW += MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
					textMinH = MyDraw.ICON_BUTTON_SZ;
				}
				
				textWLimit -= extraW;
				String text = w.player.incidentNotice;
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int textH = (int) sz.height;
				int nw = (int) sz.width + extraW;
				int nh = Math.max(textH, textMinH) + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2;
				nw += MyDraw.PANEL_INSET * 2;
				maxNoticeW = nw;
				d.drawPanel(noticeColX, noticesY, nw, nh, 2);
				nw -= MyDraw.PANEL_INSET * 2;
				int nx = noticeColX + MyDraw.PANEL_INSET;
				int ny = noticesY + MyDraw.PANEL_INSET;
				d.text(text, AGame.FOUNT, textX, ny, textWLimit);
				
				int sideButtonY = ny;
				if (w.player.incidentNoticeEmpire != null) {
					w.player.incidentNoticeEmpire.getArms().draw(d, nx, ny, 16);
					d.tooltip(nx, ny, 16, 16, w.player.incidentNoticeEmpire.getTooltip(w.player, w.map));
					d.iconButton(noticeColX + nw + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, sideButtonY, diplomacy.details, new Runnable() {
						@Override
						public void run() {
							diplomacy.showPopupOnly = false;
							diplomacy.focusEmpire = w.player.incidentNoticeEmpire;
							showDiplomacy = true;
						}
					}, true);
					sideButtonY += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				}
				if (w.player.incidentNoticePin != null) {
					d.iconButton(noticeColX + nw + MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, sideButtonY, mapGoto, new Runnable() {
						@Override
						public void run() {
							if (w.player.incidentNoticePin != null) {
								scrollX = -w.player.incidentNoticePin.x;
								scrollY = -w.player.incidentNoticePin.y;
							}
						}
					}, true);
				}
				
				ny += Math.max(textH, textMinH) + MyDraw.UI_SPACING;
				
				d.button(nx, ny, nw, _t("OK"), new Runnable() {
					@Override
					public void run() {
						w.player.incidentNotice = null;
					}
				});
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			ArrayList<Relationship> myRels = w.map.getRelationships(w.player);
			for (int ri = 0; ri < myRels.size(); ri++) {
				final Relationship rel = myRels.get(ri);
				Ultimatum ult = rel.getUltimatum(w.map);
				if (ult == null) { continue; }
				
				if (!ult.announced && ult.forcer != w.player) {
					notificationSounds.add(new NotificationSound("ultimatum", 0.5));
					ult.announced = true;
				}
				if (ult.defied && !ult.defianceAnnounced && ult.forcer == w.player) {
					notificationSounds.add(new NotificationSound("ultrefused", 0.4));
					ult.defianceAnnounced = true;
				}
				
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - 16 - MyDraw.UI_SPACING;
				int nw = 0;
				int nh = MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING + MyDraw.PROGRESS_BAR_H;
				String text;
				String text2;
				String b1Text = "";
				String b2Text = "";
				String b1Tooltip = null;
				String b2Tooltip = null;
				if (ult.defied) {
					if (ult.forcer == w.player) {
						text = _t("ultimatum_defied_forcer", rel.other(w.player).getName());
						text2 = _t("ultimatum_decision_notice");
						nh += MyDraw.BUTTON_SPACING * 2 + MyDraw.BUTTON_H * 2; // Enforce or don't
						
						b1Text = _t("Enforce_Ultimatum");
						b2Text = _t("Dont_Enforce_Ultimatum");
						if (w.has(ConquestToggle.REPUTATION)) {
							if (rel.cascadedForceRepCost(ult.getOrElse(), w.player, w.map) > 0) {
								b1Text += " (-" + rel.cascadedForceRepCost(ult.getOrElse(), w.player, w.map) + " " + _t("rep") + ")";
								String heroCost = w.player.getRepChangeHeroAppendix(rel.cascadedForceRepCost(ult.getOrElse(), w.player, w.map), w.map, false);
								if (!heroCost.isEmpty()) {
									b1Tooltip = "-" + rel.cascadedForceRepCost(ult.getOrElse(), w.player, w.map) + " " + _t("rep") + heroCost;
								}
							}
							if (EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(w.player.bonuses) > 0) {
								b2Text += " (-" + EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(w.player.bonuses) + " " + _t("rep") + ")";
								b2Tooltip = _t("Reputation_Cost_") + EmpireStat.FAILED_FALSE_ULTIMATUM_COST.explain(w.player.bonuses) +
										w.player.getRepChangeHeroAppendix(-EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(w.player.bonuses), w.map, false);
							}
						}
					} else {
						text = _t("ultimatum_defied_victim", rel.other(w.player).getName());
						text2 = _t("ultimatum_enemy_enforcement_notice");
						// No buttons, just have to wait.
					}
				} else {
					if (ult.forcer == w.player) {
						text = _t("ultimatum_forcer", rel.other(w.player).getName());
						text2 = _t("ultimatum_enemy_decision_notice");
						nh += MyDraw.BUTTON_SPACING + MyDraw.BUTTON_H; // Waiting for answer, can cancel
						
						b1Text = _t("Abandon_Ultimatum");
						if (w.has(ConquestToggle.REPUTATION)) {
							b1Text += " (-" + EmpireStat.CANCELLED_ULTIMATUM_COST.get(w.player.bonuses) + " " + _t("rep") + ")";
							b1Tooltip = _t("Reputation_Cost_") + EmpireStat.CANCELLED_ULTIMATUM_COST.explain(w.player.bonuses) +
									w.player.getRepChangeHeroAppendix(-EmpireStat.CANCELLED_ULTIMATUM_COST.get(w.player.bonuses), w.map, false);
						}
					} else {
						text = _t("ultimatum_victim", rel.other(w.player).getName());
						text2 = _t("ultimatum_decision_notice");
						nh += MyDraw.BUTTON_SPACING * 2 + MyDraw.BUTTON_H * 2; // Accede or defy
						b1Text = _t("Accede_to_Ultimatum");
						b2Text = _t("Defy_Ultimatum");
					}
				}
				nw = Math.max(nw, d.bw(b1Text));
				nw = Math.max(nw, d.bw(b2Text));
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				nw = Math.max(nw, (int) sz.width);
				nh += sz.height;
				sz = d.textSize(text2, AGame.FOUNT, 0, 0, textWLimit);
				nw = Math.max(nw, (int) sz.width);
				nh += sz.height;
				nh += AGame.FOUNT.lineHeight + OfferRenderer.getHeight(d, ult.getRelationship(), ult.getDemand(), w.player, w.map, textWLimit);
				nw = Math.max(nw, OfferRenderer.getWidth(d, ult.getRelationship(), ult.getDemand(), w.player, w.map, textWLimit, false) + MyDraw.UI_SPACING);
				nh += AGame.FOUNT.lineHeight + OfferRenderer.getHeight(d, ult.getRelationship(), ult.getOrElse(), w.player, w.map, textWLimit);
				nw = Math.max(nw, OfferRenderer.getWidth(d, ult.getRelationship(), ult.getOrElse(), w.player, w.map, textWLimit, false) + MyDraw.UI_SPACING);
				nw += 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				nh = Math.max(nh, 16 + 16 + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2);
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.drawPanel(noticeColX, noticesY, nw, nh, 3);
				nw -= MyDraw.PANEL_INSET * 2;
				d.blit(ultimatum, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				rel.other(w.player).getArms().draw(d, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16, 16, rel.other(w.player).getTooltip(w.player, w.map));
				
				int ny = noticesY + MyDraw.PANEL_INSET;
				int nx = noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING;
				nw -= 16 + MyDraw.UI_SPACING;
				d.text(text, AGame.FOUNT, nx, ny, textWLimit);
				ny += d.textSize(text, AGame.FOUNT, 0, 0, textWLimit).height;
				d.text(text2, AGame.FOUNT, nx, ny, textWLimit);
				ny += d.textSize(text2, AGame.FOUNT, 0, 0, textWLimit).height;
				d.text(_t("Ultimatum_Demands_"), AGame.FOUNT, nx, ny);
				ny += AGame.FOUNT.lineHeight;
				OfferRenderer.render(d, nx + MyDraw.UI_SPACING, ny, ult.getRelationship(), ult.getDemand(), w.player, textWLimit, this, false);
				ny += OfferRenderer.getHeight(d, ult.getRelationship(), ult.getDemand(), w.player, w.map, textWLimit);
				d.text(_t("Ultimatum_Threat_"), AGame.FOUNT, nx, ny);
				ny += AGame.FOUNT.lineHeight;
				OfferRenderer.render(d, nx + MyDraw.UI_SPACING, ny, ult.getRelationship(), ult.getOrElse(), w.player, textWLimit, this, false);
				ny += OfferRenderer.getHeight(d, ult.getRelationship(), ult.getOrElse(), w.player, w.map, textWLimit);
				ny += MyDraw.BUTTON_SPACING;
				d.progressBar(nx, ny, nw - MyDraw.ICON_BUTTON_SZ - MyDraw.BUTTON_SPACING, (Relationship.ULTIMATUM_TIME - ult.timeLeft) * 1.0 / Relationship.ULTIMATUM_TIME);
				d.iconButton(nx + nw - MyDraw.ICON_BUTTON_SZ, ny, mapGoto, new Runnable() {
					@Override
					public void run() {
						City cap = rel.other(w.player).getCapital();
						if (cap != null) {
							scrollX = -cap.x;
							scrollY = -cap.y;
						}
					}
				}, true);
				d.tooltip(nx + nw - MyDraw.ICON_BUTTON_SZ, ny, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("do_view"));
				
				ny += MyDraw.PROGRESS_BAR_H + MyDraw.BUTTON_SPACING;
				if (ult.defied) {
					if (ult.forcer == w.player) {
						// Enforce or don't
						d.button(nx, ny, nw, b1Text, new Runnable() {
							@Override
							public void run() {
								diplomacy.enforceUltimatum(rel, w.map);
							}
						}, diplomacy.canDoActions());
						if (b1Tooltip != null) {
							d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, b1Tooltip);
						}
						ny += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						d.button(nx, ny, nw, b2Text, new Runnable() {
							@Override
							public void run() {
								diplomacy.dontEnforceUltimatum(rel, w.map);
							}
						}, diplomacy.canDoActions());
						if (b2Tooltip != null) {
							d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, b2Tooltip);
						}
					}
				} else {
					if (ult.forcer == w.player) {
						// Waiting for answer, can cancel
						d.button(nx, ny, nw, b1Text, new Runnable() {
							@Override
							public void run() {
								diplomacy.cancelUltimatum(rel, w.map);
							}
						}, diplomacy.canDoActions());
						if (b1Tooltip != null) {
							d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, b1Tooltip);
						}
					} else {
						// Accede or defy
						d.button(nx, ny, nw, b1Text, new Runnable() {
							@Override
							public void run() {
								diplomacy.accedeToUltimatum(rel, w.map);
							}
						}, diplomacy.canDoActions());
						if (b1Tooltip != null) {
							d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, b1Tooltip);
						}
						ny += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
						d.button(nx, ny, nw, b2Text, new Runnable() {
							@Override
							public void run() {
								diplomacy.defyUltimatum(rel, w.map);
							}
						}, diplomacy.canDoActions());
						if (b2Tooltip != null) {
							d.tooltip(nx, ny, nw, MyDraw.BUTTON_H, b2Tooltip);
						}
					}
				}
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int ri = 0; ri < myRels.size(); ri++) {
				final Relationship rel = myRels.get(ri);
				final Relationship.Challenge ch = rel.challenge;
				if (ch == null) { continue; }
				if (!ch.announced && ch.challenger(rel) != w.player) {
					if (ch.type == Relationship.Challenge.Type.DELEGATION) {
						notificationSounds.add(new NotificationSound("delegation", 0.2));
					}
					if (ch.type == Relationship.Challenge.Type.INSULT) {
						notificationSounds.add(new NotificationSound("insult", 0.2));
					}
					ch.announced = true;
				}
				final Empire other = rel.other(w.player);
				int nw = 0;
				int nh = 0;
				int textWLimit = noticeWLimit - 16 + MyDraw.UI_SPACING - MyDraw.PANEL_INSET * 2;
				if (ch.challenger(rel) != w.player) {
					textWLimit -= MyDraw.UI_SPACING + MyDraw.ICON_BUTTON_SZ;
				}
				String text;
				String text2;
				if (ch.challenger(rel) == w.player) {
					text = _t("challenge_sent_" + ch.type.name(), rel.other(w.player).getName());
					text2 = MyDraw.SELECTED_C + ch.getDetail(rel);
				} else {
					text = _t("challenge_received_" + ch.type.name(), rel.other(w.player).getName());
					text2 = MyDraw.SELECTED_C + ch.getDetail(rel);
				}
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				nw = Math.max(nw, (int) sz.width);
				int text1Height = (int) sz.height;
				nh += text1Height;
				sz = d.textSize(text2, AGame.FOUNT, 0, 0, textWLimit);
				nw = Math.max(nw, (int) sz.width);
				int text2Height = (int) sz.height;
				nh += text2Height;
				nw += 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				nh = Math.max(nh, 16 + 16 + MyDraw.BUTTON_SPACING);
				if (ch.challenger(rel) != w.player) {
					nw += MyDraw.UI_SPACING + MyDraw.ICON_BUTTON_SZ;
					nh = Math.max(nh, MyDraw.ICON_BUTTON_SZ + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING);
				}
				nh += MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING + MyDraw.PROGRESS_BAR_H;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.drawPanel(noticeColX, noticesY, nw, nh, 10);
				nw -= MyDraw.PANEL_INSET * 2;
				d.blit(ch.challenger(rel) == w.player ? ch.type.senderIcon : ch.type.receiverIcon, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				rel.other(w.player).getArms().draw(d, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16, 16, rel.other(w.player).getTooltip(w.player, w.map));
				int ny = noticesY + MyDraw.PANEL_INSET;
				if (ch.challenger(rel) != w.player) {
					int bx = noticeColX + MyDraw.PANEL_INSET + nw - MyDraw.ICON_BUTTON_SZ;
					d.iconButton(bx, noticesY + MyDraw.PANEL_INSET, ch.type.responseIcon, new Runnable() {
						@Override
						public void run() {
							JSONObject m = msg("dealWithChallenge").put("player", w.player.id).put("other", other.id).put("respond", true);
							if (ch.type == Relationship.Challenge.Type.INSULT) {
								m.put("detail1", EmpireStat.INSULT_1_PREFIX.get(w.player.bonuses) + AGame.ANIM_R.nextInt(EmpireStat.INSULT_1_NUM.get(w.player.bonuses)));
								m.put("detail2", EmpireStat.INSULT_2_PREFIX.get(w.player.bonuses) + AGame.ANIM_R.nextInt(EmpireStat.INSULT_2_NUM.get(w.player.bonuses)));
							}
							if (ch.type == Relationship.Challenge.Type.DELEGATION) {
								m.put("detail1", EmpireStat.DELEGATION_ACCEPTED_PREFIX.get(w.player.bonuses) + AGame.ANIM_R.nextInt(EmpireStat.DELEGATION_ACCEPTED_NUM.get(w.player.bonuses)));
							}
							w.giveCommand(m);
						}
					}, true);
					if (ch.type == Relationship.Challenge.Type.INSULT) {
						d.tooltip(bx, noticesY + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ,
								_t("insult_back_tooltip", EmpireStat.ANSWERED_INSULT_GRIEVANCES.get(other.bonuses), EmpireStat.ANSWERED_INSULT_REP_LOSS.get(w.player.bonuses)));
					}
					if (ch.type == Relationship.Challenge.Type.DELEGATION) {
						if (rel.getGrievances(w.player) == 0 && rel.getGrievances(other) == 0) {
							d.tooltip(bx, noticesY + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("no_grievances_accept_delegation_tooltip",
									EmpireStat.NO_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(other.bonuses),
									EmpireStat.NO_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(other.bonuses)) +
									w.player.getRepChangeHeroAppendix(EmpireStat.NO_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(other.bonuses), w.map, false));
						} else if (rel.getGrievances(other) > 0) {
							d.tooltip(bx, noticesY + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("sender_grievances_accept_delegation_tooltip",
									EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(other.bonuses),
									Math.min(rel.getGrievances(other), EmpireStat.SENDER_GRIEVANCES_DELEGATION_SENDER_GRIEVANCES_LOSS.get(other.bonuses))));
						} else { // other has grievances
							d.tooltip(bx, noticesY + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("receiver_grievances_accept_delegation_tooltip",
									EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_SENDER_REP_GAIN.get(other.bonuses),
									EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(other.bonuses),
									Math.min(rel.getGrievances(w.player), EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_GRIEVANCES_LOSS.get(other.bonuses))) +
									w.player.getRepChangeHeroAppendix(EmpireStat.RECEIVER_GRIEVANCES_DELEGATION_RECEIVER_REP_GAIN.get(other.bonuses), w.map, false));
						}
					}
					
					d.iconButton(bx, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, ch.type.noResponseIcon, new Runnable() {
						@Override
						public void run() {
							JSONObject m = msg("dealWithChallenge").put("player", w.player.id).put("other", other.id).put("respond", false);
							if (ch.type == Relationship.Challenge.Type.DELEGATION) {
								m.put("detail1", EmpireStat.DELEGATION_REJECTED_PREFIX.get(w.player.bonuses) + AGame.ANIM_R.nextInt(EmpireStat.DELEGATION_REJECTED_NUM.get(w.player.bonuses)));
							}
							w.giveCommand(m);
						}
					}, true);
					if (ch.type == Relationship.Challenge.Type.INSULT) {
						d.tooltip(bx, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ,
								_t("ignore_insult_tooltip", EmpireStat.UNANSWERED_INSULT_GRIEVANCES.get(w.player.bonuses), EmpireStat.UNANSWERED_INSULT_REP_LOSS.get(other.bonuses)) +
								w.player.getRepChangeHeroAppendix(-EmpireStat.UNANSWERED_INSULT_REP_LOSS.get(other.bonuses), w.map, false));
					}
					if (ch.type == Relationship.Challenge.Type.DELEGATION) {
						d.tooltip(bx, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ,
								_t("reject_delegation_tooltip", EmpireStat.DELEGATION_SNUB_GRIEVANCES.get(w.player.bonuses), EmpireStat.DELEGATION_SNUB_REP_LOSS.get(other.bonuses)));
					}
				}
				int nx = noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING;
				nw -= 16 + MyDraw.UI_SPACING;
				d.text(text, AGame.FOUNT, nx, ny, textWLimit);
				ny += text1Height;
				d.text(text2, AGame.FOUNT, nx, ny, textWLimit);
				ny += text2Height + MyDraw.BUTTON_SPACING;
				int pw = nw;
				if (ch.challenger(rel) != w.player) {
					pw -= MyDraw.ICON_BUTTON_SZ + MyDraw.UI_SPACING;
				}
				d.progressBar(nx, ny, pw, (Relationship.CHALLENGE_TIME - ch.timeLeft) * 1.0 / Relationship.CHALLENGE_TIME);
				ny += MyDraw.PROGRESS_BAR_H + MyDraw.BUTTON_SPACING;
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			ArrayList<FleetOwner> fos = w.map.fleetOwners();
			int fosz = fos.size();
			lp: for (int foi = 0; foi < fosz; foi++) {
				FleetOwner fo = fos.get(foi);
				if (fo == w.player) { continue; }
				if (fo instanceof Empire && w.map.getRelationship(w.player, (Empire) fo).level != Relationship.Level.WAR) {
					continue;
				}
				int textWLimit = noticeWLimit - 16 - MyDraw.UI_SPACING * 2 - MyDraw.ICON_BUTTON_SZ - MyDraw.PANEL_INSET * 2;
				int fsz = fo.getFleets().size();
				for (int fi = 0; fi < fsz; fi++) {
					final Fleet f = fo.getFleets().get(fi);
					if (w.player.cities.contains(f.destination)) {
						String text = _t("incoming_enemy_fleet") + "\n" + fo.getName() + " > " + f.destination.getDisplayName() + "\n" + w.describeTime(f.travelTimeLeft(fo.bonuses(), w.map));
						Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
						int nw = (int) sz.width + 16 + MyDraw.UI_SPACING * 2 + MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 2;
						int nh = (int) sz.height + MyDraw.PANEL_INSET * 2;
						if (noticesY + nh > noticesMaxY) {
							noticesY = baseNoticesY;
							noticeColX += maxNoticeW + MyDraw.UI_SPACING;
							maxNoticeW = 0;
						}
						maxNoticeW = Math.max(nw, maxNoticeW);
						d.drawPanel(noticeColX, noticesY, nw, nh, 21);
						d.blit(invader, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
						d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
						d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET, mapGoto, new Runnable() {
							@Override
							public void run() {
								scrollX = -f.realX(w.map);
								scrollY = -f.realY(w.map);
							}
						}, true);
						noticesY += nh + MyDraw.UI_SPACING;
					}
				}
			}
			
			for (int oi = 0; oi < w.playerOffers.size(); oi++) {
				final Relationship.Offer o = w.playerOffers.get(oi);
				final Empire other = o.rel.other(w.player);
				if (!w.map.empires.contains(other)) { continue; }
				
				if (!o.announced) {
					notificationSounds.add(new NotificationSound("offer", 0.4));
					o.announced = true;
				}
				
				int textWLimit = noticeWLimit - 16 - MyDraw.UI_SPACING * 2 - MyDraw.ICON_BUTTON_SZ - MyDraw.PANEL_INSET * 2;
				String text = _t("Diplomatic_offer_from_x", other.getName());
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = Math.max((int) sz.width, OfferRenderer.getWidth(d, o.rel, o, w.player, w.map, textWLimit, false));
				int nh = (int) sz.height;
				nh += OfferRenderer.getHeight(d, o.rel, o, w.player, w.map, textWLimit);
				nw += 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2 + MyDraw.UI_SPACING + MyDraw.ICON_BUTTON_SZ;
				nh = Math.max(MyDraw.ICON_BUTTON_SZ * 4 + MyDraw.BUTTON_SPACING * 3, Math.max(nh, 16 + 16 + MyDraw.BUTTON_SPACING)) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 23);
				
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.playerOffers.remove(o);
					}
				});
				d.blit(letter, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				other.getArms().draw(d, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16, 16, other.getTooltip(w.player, w.map));
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				OfferRenderer.render(d, noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET + (int) sz.height, o.rel, o, w.player, textWLimit, this, false);
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET, tick, new Runnable() {
					@Override
					public void run() {
						showDiplomacy = true;
						diplomacy.focusEmpire = null;
						diplomacy.showPopupOnly = true;
						diplomacy.removeNoticeOnPopupOK = o;
						diplomacy.checkAcceptProposal(other);
					}
				}, diplomacy.canDoActions());
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Accept_Offer"));
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, cross, new Runnable() {
					@Override
					public void run() {
						w.playerOffers.remove(o);
						diplomacy.cancelProposal(other);
					}
				}, diplomacy.canDoActions());
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Reject_Offer"));
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, edit, new Runnable() {
					@Override
					public void run() {
						showDiplomacy = true;
						diplomacy.focusEmpire = null;
						diplomacy.showPopupOnly = true;
						diplomacy.removeNoticeOnPopupOK = o;
						diplomacy.editProposal(other);
					}
				}, diplomacy.canDoActions());
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Counteroffer"));
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ * 3 + MyDraw.BUTTON_SPACING * 3, mapGoto, new Runnable() {
					@Override
					public void run() {
						City cap = other.getCapital();
						if (cap != null) {
							scrollX = -cap.x;
							scrollY = -cap.y;
						}
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + MyDraw.ICON_BUTTON_SZ * 3 + MyDraw.BUTTON_SPACING * 3, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("do_view"));
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int ni = 0; ni < w.player.diplomacyNotices.size(); ni++) {
				final DiplomacyNotice notice = w.player.diplomacyNotices.get(ni);
				
				if (!notice.announced) {
					if (notice.type.sound != null) {
						notificationSounds.add(notice.type.sound);
					}
					notice.announced = true;
				}
				
				if (notice.type == DiplomacyNotice.Type.WAR_DECLARED || notice.type == DiplomacyNotice.Type.ALLIANCE_CAUSED_WAR || notice.type == DiplomacyNotice.Type.ALLY_DECLARED_WAR) { continue; }
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - 16 - MyDraw.UI_SPACING;
				int nw = 0;
				int nh = 0;
				if (notice.type.showOffer) {
					nh = OfferRenderer.getHeight(d, notice.basis, notice.offer, w.player, w.map, textWLimit);
					nw = OfferRenderer.getWidth(d, notice.basis, notice.offer, w.player, w.map, textWLimit, /* past tense */ true);
				}
				textWLimit = Math.max(textWLimit, nw);
				String text = notice.getMessage(w.player);
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				nw = Math.max(nw, (int) sz.width);
				nh += (int) sz.height + MyDraw.PANEL_INSET * 2;
				if (notice.forcedInfo != null) {
					nh += AGame.FOUNT.lineHeight;
				}
				nw += 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				nh = Math.max(nh, 16 + 16 + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2);
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.diplomacyNotices.remove(notice);
					}
				});
				if (notice.forcedInfo != null) {
					d.tooltip(noticeColX, noticesY, nw, nh, notice.forcedInfo + "\n\n" + _t("right_click_to_dismiss"));
				} else {
					d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				}
				d.drawPanel(noticeColX, noticesY, nw, nh, 24);
				d.blit(notice.type.icon, noticeColX + MyDraw.PANEL_INSET + 8 - notice.type.icon.srcWidth / 2, noticesY + MyDraw.PANEL_INSET + 8 - notice.type.icon.srcHeight / 2);
				if (notice.type.crossedOut) {
					d.blit(cross, noticeColX + MyDraw.PANEL_INSET + 8 - cross.srcWidth / 2, noticesY + MyDraw.PANEL_INSET + 8 - cross.srcHeight / 2);
				}
				notice.other.getArms().draw(d, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16, 16, notice.other.getTooltip(w.player, w.map));
				
				int ny = noticesY + MyDraw.PANEL_INSET;
				int nx = noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING;
				d.text(text, AGame.FOUNT, nx, ny, textWLimit);
				ny += (int) sz.height;
				if (notice.type.showOffer) {
					ny = OfferRenderer.render(d, nx, ny, notice.basis, notice.offer, w.player, textWLimit, this, /* past tense */ true);
				}
				if (notice.forcedInfo != null) {
					d.text((notice.forcedScore > 0 ? "+" : "") + notice.forcedScore, AGame.FOUNT, nx, ny);
					ny += AGame.FOUNT.lineHeight;
				}
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (final BesiegeRequest req : w.player.besiegeRequests) {
				if (!req.announced) {
					notificationSounds.add(new NotificationSound("besiegerequest", 0.7));
					req.announced = true;
				}
				
				int textWLimit = noticeWLimit - 16 - MyDraw.UI_SPACING * 2 - MyDraw.ICON_BUTTON_SZ - MyDraw.PANEL_INSET * 2;
				String text = _t("besiege_request_msg", req.from.getName(), req.city.getDisplayName());
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + 16 + MyDraw.UI_SPACING * 2 + MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(MyDraw.ICON_BUTTON_SZ * 3 + MyDraw.BUTTON_SPACING * 2, (int) sz.height) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.drawPanel(noticeColX, noticesY, nw, nh, 5);
				int ny = noticesY + MyDraw.PANEL_INSET;
				int nx = noticeColX + MyDraw.PANEL_INSET;
				d.blit(canBesiege, nx, ny);
				req.from.getArms().draw(d, nx, ny + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(nx, ny + 16 + MyDraw.BUTTON_SPACING, 16, 16, req.from.getTooltip(w.player, w.map));
				nx += 16 + MyDraw.UI_SPACING;
				d.text(text, AGame.FOUNT, nx, ny, textWLimit);
				nx = noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ;
				final Fleet f = w.player.canHelpAllyBesiegeFleet(req.city, w);
				if (f == null) {
					d.colouredIconButton(nx, ny, cannotAttack, new Runnable() {
						@Override
						public void run() {
							// Nothing
						}
					}, false);
					d.tooltip(nx, ny, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("no_besiege_fleet_available"));
				} else {
					Img icon = canMove;
					if (w.has(ConquestToggle.SUPPLY)) {
						int moveAndReturnSupplyCost = w.player.moveAndReturnSupplyCost(f, f.getAllShips(), req.city, null, w.map);
						icon = moveAndReturnSupplyCost > f.supply() ? canAttackButNotReturn : canAttackAndReturn;
					}
					d.colouredIconButton(nx, ny, icon, new Runnable() {
						@Override
						public void run() {
							doBesiege(f, req.city);
						}
					}, true);
					d.tooltip(nx, ny, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("send_requested_fleet", f.location.getDisplayName(), Airship.nameList(f.getAllShips(), Lang.currentLocale)));
				}
				d.iconButton(nx, ny + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, cross, new Runnable() {
					@Override
					public void run() {
						dismissRequest(req);
					}
				}, true);
				d.tooltip(nx, ny + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("dismiss_request"));
				d.iconButton(nx, ny + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, mapGoto, new Runnable() {
					@Override
					public void run() {
						scrollX = -req.city.x;
						scrollY = -req.city.y;
					}
				}, true);
				d.tooltip(nx, ny + MyDraw.ICON_BUTTON_SZ * 2 + MyDraw.BUTTON_SPACING * 2, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("Show_on_map"));
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			if (diplomacyFailureNotice != null) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - 16 - MyDraw.UI_SPACING;
				Rect sz = d.textSize(diplomacyFailureNotice, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = StrictMath.max(MyDraw.ICON_BUTTON_SZ, StrictMath.max((int) sz.height, 16)) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.drawPanel(noticeColX, noticesY, nw, nh, 12);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						diplomacyFailureNotice = null;
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.blit(letter, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				d.blit(cross, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				d.text(diplomacyFailureNotice, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int ni = 0; ni < w.player.ultimatumNotices.size(); ni++) {
				final UltimatumNotice notice = w.player.ultimatumNotices.get(ni);
				
				if (!notice.announced) {
					notificationSounds.add(notice.type.sound);
					notice.announced = true;
				}
				
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - 16 - MyDraw.UI_SPACING;
				String text = notice.getMessage(w.player);
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width;
				int nh = (int) sz.height + MyDraw.PANEL_INSET * 2;
				if (notice.type.showDemands) {
					nh += AGame.FOUNT.lineHeight + OfferRenderer.getHeight(d, null, notice.ult.getOriginalDemand(), w.player, w.map, textWLimit);
					nw = Math.max(nw, OfferRenderer.getWidth(d, null, notice.ult.getOriginalDemand(), w.player, w.map, textWLimit, notice.type.pastTense));
				}
				if (notice.type.showThreat) {
					nh += AGame.FOUNT.lineHeight + OfferRenderer.getHeight(d, null, notice.ult.getOriginalOrElse(), w.player, w.map, textWLimit);
					nw = Math.max(nw, OfferRenderer.getWidth(d, null, notice.ult.getOriginalOrElse(), w.player, w.map, textWLimit, notice.type.pastTense));
				}
				nw += 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				nh = Math.max(nh, 16 + 16 + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET * 2);
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.ultimatumNotices.remove(notice);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 7);
				d.blit(letter, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				notice.ult.getRelationship().other(w.player).getArms().draw(d, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16);
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET + 16 + MyDraw.BUTTON_SPACING, 16, 16, notice.ult.getRelationship().other(w.player).getTooltip(w.player, w.map));
				
				int ny = noticesY + MyDraw.PANEL_INSET;
				int nx = noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING;
				d.text(text, AGame.FOUNT, nx, ny, textWLimit);
				ny += (int) sz.height;
				if (notice.type.showDemands) {
					d.text(_t("Ultimatum_Demands_"), AGame.FOUNT, nx, ny);
					ny += AGame.FOUNT.lineHeight;
					OfferRenderer.render(d, nx, ny, null, notice.ult.getOriginalDemand(), w.player, textWLimit, this, notice.type.pastTense);
					ny += OfferRenderer.getHeight(d, null, notice.ult.getOriginalDemand(), w.player, w.map, textWLimit);
				}
				if (notice.type.showThreat) {
					d.text(_t("Ultimatum_Threat_"), AGame.FOUNT, nx, ny);
					ny += AGame.FOUNT.lineHeight;
					OfferRenderer.render(d, nx, ny, null, notice.ult.getOriginalOrElse(), w.player, textWLimit, this, notice.type.pastTense);
				}
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int i = 0; i < w.player.newRecruits.size(); i++) {
				if (w.player.newRecruits.get(i).a.hired || w.player.newRecruits.get(i).a.inEmpire != w.player) {
					w.player.newRecruits.remove(i);
					i--;
				}
			}
			for (final Pair<Hero, HeroEvent.Hook> recruit : w.player.newRecruits) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - noticePortraitSize() - MyDraw.UI_SPACING;
				String text = Lang._tWithFallback(
						"hero_recruit",
						recruit.a.type.name + "_recruit_" + recruit.b.tkey,
						recruit.b.getDesc(recruit.a), recruit.a.getName());
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + noticePortraitSize() + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(noticePortraitSize(), (int) sz.height + MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED, Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						if (type == Hook.Type.MOUSE_1_CLICKED) {
							showHero(recruit.a);
						}
						w.player.newRecruits.remove(recruit);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.portraitBlit(recruit.a.type.img, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize());
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize(), recruit.a.getDetails(true));
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + noticePortraitSize() + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, details, new InputRunnable() {
					@Override
					public void run(Input in) {
						showHero(recruit.a);
						w.player.newRecruits.remove(recruit);
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("hero_info"));
				
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int i = 0; i < w.player.heroInjuryNotices.size(); i++) {
				if (w.player.heroInjuryNotices.get(i).a.inEmpire != w.player) {
					w.player.heroInjuryNotices.remove(i);
					i--;
				}
			}
			for (final Pair<Hero, Integer> injury : w.player.heroInjuryNotices) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - noticePortraitSize() - MyDraw.UI_SPACING;
				String text = MyDraw.ERROR_C + Lang._t("hero_injury_notice", injury.a.getName(), w.describeTime(injury.b));
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + noticePortraitSize() + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(noticePortraitSize(), (int) sz.height + MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED, Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						if (type == Hook.Type.MOUSE_1_CLICKED) {
							showHero(injury.a);
						}
						w.player.heroInjuryNotices.remove(injury);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.portraitBlit(injury.a.type.img, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize());
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize(), injury.a.getDetails(true));
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + noticePortraitSize() + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, details, new InputRunnable() {
					@Override
					public void run(Input in) {
						showHero(injury.a);
						w.player.heroInjuryNotices.remove(injury);
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("hero_info"));
				
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (int i = 0; i < w.player.heroRecoveryNotices.size(); i++) {
				if (w.player.heroRecoveryNotices.get(i).inEmpire != w.player || w.player.heroRecoveryNotices.get(i).getInCity() != null || w.player.heroRecoveryNotices.get(i).getInShip() != null) {
					w.player.heroRecoveryNotices.remove(i);
					i--;
				}
			}
			for (final Hero h : w.player.heroRecoveryNotices) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - noticePortraitSize() - MyDraw.UI_SPACING;
				String text = Lang._t("hero_recovery_notice", h.getName());
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + noticePortraitSize() + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(noticePortraitSize(), (int) sz.height + MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED, Hook.Type.MOUSE_1_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						if (type == Hook.Type.MOUSE_1_CLICKED) {
							showHero(h);
						}
						w.player.heroRecoveryNotices.remove(h);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.portraitBlit(h.type.img, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize());
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize(), h.getDetails(true));
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + noticePortraitSize() + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, details, new InputRunnable() {
					@Override
					public void run(Input in) {
						showHero(h);
						w.player.heroRecoveryNotices.remove(h);
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("hero_info"));
				
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (final HeroStatNotice hsn : w.player.heroStatEvents) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - noticePortraitSize() - MyDraw.UI_SPACING;
				String text = hsn.getText();
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + noticePortraitSize() + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(noticePortraitSize(), (int) sz.height + MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.heroStatEvents.remove(hsn);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.blit(hsn.getImg(), noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize());
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize(), hsn.getHero().getDetails(true));
				Img icon = null;
				Img iconBg = null;
				if (hsn.died) {
					icon = dieIcon;
					iconBg = dieIconBg;
				} else if (hsn.left) {
					icon = leaveIcon;
					iconBg = leaveIconBg;
				} else {
					icon = evolveIcon;
					iconBg = evolveIconBg;
				}
				d.blit(iconBg, MyDraw.ICON_TINT, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W + MyDraw.BUTTON_SPACING, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W + MyDraw.BUTTON_SPACING);
				d.blit(icon, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W + MyDraw.BUTTON_SPACING, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W + MyDraw.BUTTON_SPACING);
				
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + noticePortraitSize() + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, details, new InputRunnable() {
					@Override
					public void run(Input in) {
						showHero(hsn.getHero());
						w.player.heroStatEvents.remove(hsn);
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("hero_info"));
				
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (final Empire.HeroComment hc : w.player.heroComments) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - noticePortraitSize() - MyDraw.UI_SPACING;
				String text = MyDraw.SELECTED_C + hc.comment + (hc.statChange == null ? "" : "\n\n[]" + hc.statChange.getShortText(w.map));
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + noticePortraitSize() + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = Math.max(noticePortraitSize(), (int) sz.height/* + MyDraw.BUTTON_SPACING + MyDraw.ICON_BUTTON_SZ*/) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.heroComments.remove(hc);
					}
				});
				String tt = _t("right_click_to_dismiss");
				if (hc.statChange != null) {
					tt = hc.statChange.getChangeTextOnly(w.map) + "\n\n" + tt;
				}
				d.tooltip(noticeColX, noticesY, nw, nh, tt);
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.portraitBlit(hc.hero.type.img, noticeColX + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_INSET + MyDraw.PANEL_BORDER_W, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, noticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize());
				d.tooltip(noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET, noticePortraitSize(), noticePortraitSize(), hc.hero.getDetails(true));
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + noticePortraitSize() + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				/*d.iconButton(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, details, new InputRunnable() {
					@Override
					public void run(Input in) {
						showHero(hc.hero);
						w.player.heroComments.remove(hc);
					}
				}, true);
				d.tooltip(noticeColX + nw - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, noticesY + MyDraw.PANEL_INSET + (int) sz.height + MyDraw.BUTTON_SPACING, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("hero_info"));
				*/
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (final Hero.StatChange hsc : w.player.heroStatChanges) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - smallNoticePortraitSize() - MyDraw.BUTTON_SPACING;
				String text = hsc.getShortText(w.map);
				
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + smallNoticePortraitSize() + MyDraw.BUTTON_SPACING + MyDraw.PANEL_INSET;
				int nh = Math.max(smallNoticePortraitSize(), (int) sz.height + MyDraw.PANEL_INSET * 2);
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.BUTTON_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.heroStatChanges.remove(hsc);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, hsc.getChangeTextOnly(w.map) + "\n\n" + _t("right_click_to_dismiss"));
				d.drawPanel(noticeColX, noticesY, nw, nh, 0);
				
				d.portraitBlit(hsc.h.type.img, noticeColX + MyDraw.PANEL_BORDER_W, noticesY + MyDraw.PANEL_BORDER_W, smallNoticePortraitSize() - MyDraw.PANEL_BORDER_W * 2, smallNoticePortraitSize() - MyDraw.PANEL_BORDER_W * 2);
				d.drawPanelBorder(noticeColX, noticesY, smallNoticePortraitSize(), smallNoticePortraitSize());
				d.tooltip(noticeColX, noticesY, smallNoticePortraitSize(), smallNoticePortraitSize(), hsc.h.getDetails(true));
				d.text(text, AGame.FOUNT, noticeColX + smallNoticePortraitSize() + MyDraw.BUTTON_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				noticesY += nh + MyDraw.UI_SPACING;
			}
			
			for (final SpyActionResult sar : w.player.spyActionResults) {
				int textWLimit = noticeWLimit - MyDraw.PANEL_INSET * 2 - 16 - MyDraw.UI_SPACING;
				String text = sar.actor == w.player ? sar.actorText : sar.victimText;
				Rect sz = d.textSize(text, AGame.FOUNT, 0, 0, textWLimit);
				int nw = (int) sz.width + 16 + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
				int nh = StrictMath.max(MyDraw.ICON_BUTTON_SZ, StrictMath.max((int) sz.height, 16)) + MyDraw.PANEL_INSET * 2;
				if (noticesY + nh > noticesMaxY) {
					noticesY = baseNoticesY;
					noticeColX += maxNoticeW + MyDraw.UI_SPACING;
					maxNoticeW = 0;
				}
				maxNoticeW = Math.max(nw, maxNoticeW);
				d.drawPanel(noticeColX, noticesY, nw, nh, 12);
				d.hook(noticeColX, noticesY, nw, nh, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_2_CLICKED) {
					@Override
					public void run(Input input, Pt pt, Type type) {
						w.player.spyActionResults.remove(sar);
					}
				});
				d.tooltip(noticeColX, noticesY, nw, nh, _t("right_click_to_dismiss"));
				d.blit(eye, noticeColX + MyDraw.PANEL_INSET, noticesY + MyDraw.PANEL_INSET);
				d.text(text, AGame.FOUNT, noticeColX + MyDraw.PANEL_INSET + 16 + MyDraw.UI_SPACING, noticesY + MyDraw.PANEL_INSET, textWLimit);
				noticesY += nh + MyDraw.UI_SPACING;
			}
		}
		
		if (savedIndicator > 0) {
			d.heavilyBorderedText(_t("Game_Saved"), AGame.BIG_FOUNT, AGame.BIG_FOUNT_OUTLINE, Clr.WHITE, Clr.BLACK, sm.width / 2 - (int) d.textSize(_t("Game_Saved"), AGame.BIG_FOUNT).x / 2, sm.height / 3, 10000);
		}
		
		x = sm.width - MyDraw.ICON_BUTTON_SZ - MyDraw.SIDE_CLEARANCE;
		d.iconButton(x, MyDraw.TOP_BAR_INSET, help, new Runnable() {
			@Override
			public void run() {
				g.showHelp("Conquest", null);
			}
		}, true);
		d.tooltip(x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("manual"));
		int bw = d.tw(_t("Menu"));
		x -= bw + MyDraw.UI_SPACING;
		d.toggle(x, MyDraw.TOP_BAR_INSET, bw, _t("Menu"), "M", new InputRunnable() {
			@Override
			public void run(Input in) {
				menu = true;
			}
		}, menu, !menu);

		x -= MyDraw.UI_SPACING;
		if (w.player != null || !w.isMultiplayer()) {
			for (int i = CampaignWorld.Speed.values().length - 2; i >= 0; i--) { // qqDPS Forbid fastest speed for perf reasons.
				final CampaignWorld.Speed sp = CampaignWorld.Speed.values()[i];
				if (w.isMultiplayer() && !sp.forMultiplayer) {
					continue;
				}
				x -= MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
				boolean selected = w.speed == sp;
				if (w.isMultiplayer()) {
					selected = w.speedVoters.containsKey(g.playerID()) && w.speedVoters.get(g.playerID()) == sp;
				}
				d.iconToggle(x, MyDraw.TOP_BAR_INSET, sp.icon, null, new InputRunnable() {
					@Override
					public void run(Input in) {
						if (sp != CampaignWorld.Speed.STOP) {
							if (w.isMultiplayer()) {
								lastNonStopSpeed = w.speedVoters.containsKey(g.playerID()) ? w.speedVoters.get(g.playerID()) : w.speed;
							} else {
								lastNonStopSpeed = w.speed;
							}
						}
						w.giveCommand(msg("setCampaignSpeed").put("voterID", g.playerID()).put("speed", sp.name()));
					}
				}, selected, !menu, w.map.age == 0 && sp == CampaignWorld.Speed.NORMAL && w.player != null && (w.player.research != null || w.player.isAllResearchDone(w.map)));
				if (sp == CampaignWorld.Speed.NORMAL) {
					d.highlight("conquestSpeed", g, x, MyDraw.TOP_BAR_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ);
				}
				if (w.isMultiplayer()) {
					if (w.speed == sp) {
						d.blit(indicator, MyDraw.TITLE, x + MyDraw.ICON_BUTTON_SZ / 2 - indicator.srcWidth / 2, MyDraw.TOP_BAR_H);
					} else if (w.hasVoteForSpeed(sp)) {
						d.blit(indicator, MyDraw.WIN_SHADOW, x + MyDraw.ICON_BUTTON_SZ / 2 - indicator.srcWidth / 2, MyDraw.TOP_BAR_H);
					}
				}
			}
		}
		
		int speedX = x;
		
		if (w.player != null) {
			if (!w.player.messages.isEmpty()) {
				d.state.clearGlowRects();
				d.getHooks().list.clear();
				Empire.Message msg = w.player.messages.get(w.player.messages.size() - 1);
				String text = msg.text;
				d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, text, new Runnable() {
					@Override
					public void run() {
						if (w.player == null) { return; }
						w.player.messages.remove(w.player.messages.size() - 1);
					}
				});
			}
		}
		
		if (w.combatInfo != null && !combatConfirmed && !(w.isMultiplayer() || w.fakeMultiplayerForTesting)) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			hs.list.clear();
			String text = Empire.nameList(w.combatInfo.attackers(w.map), Lang.currentLocale) + " vs " + Empire.nameList(w.combatInfo.defenders(w.map), Lang.currentLocale);
			Pt sz = d.textSize(text, AGame.BIG_FOUNT);
			int width = (int) sz.x + MyDraw.PANEL_INSET * 2;
			int height = (int) sz.y + MyDraw.PANEL_INSET * 2;
			int yy = sm.height / 3 - height / 2;
			int xx = sm.width / 2 - width / 2;
			checkedBlit(d, sm, fight_big, Clr.WHITE, sm.width / 2 - 32, yy - MyDraw.UI_SPACING - 64);
			Graphics g = (Graphics) d.frame().nativeRenderer();
			g.setColor(Color.black);
			g.setLineWidth(6);
			g.drawLine(xx + width / 2, yy + height / 2, (int) worldToScreenX(w.combatInfo.attackingFleets.get(0).realX(w.map)), (int) worldToScreenY(w.combatInfo.attackingFleets.get(0).realY(w.map)));
			g.setColor(new Color(245, 228, 116));
			g.setLineWidth(2);
			g.drawLine(xx + width / 2, yy + height / 2, (int) worldToScreenX(w.combatInfo.attackingFleets.get(0).realX(w.map)), (int) worldToScreenY(w.combatInfo.attackingFleets.get(0).realY(w.map)));
			g.setLineWidth(1);
			d.drawPanel(xx, yy, width, height, 2);
			xx += MyDraw.PANEL_INSET;
			yy += MyDraw.PANEL_INSET;
			d.text(text, AGame.BIG_FOUNT, xx, yy);
			d.hook(0, 0, sm.width, sm.height, new Hook("confirmCombat", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
				@Override
				public void run(Input input, Pt pt, Type type) {
					combatConfirmed = true;
				}
			});
			return;
		} else if (w.combatInfo != null) {
			int ix = (int) worldToScreenX(w.combatInfo.attackingFleets.get(0).realX(w.map));
			int iy = (int) worldToScreenY(w.combatInfo.attackingFleets.get(0).realY(w.map));
			checkedBlit(d, sm, fight_big, Clr.WHITE, ix - 32, iy - MyDraw.UI_SPACING - 64);
		}
		PerfStats.mark("ss notices");
		if (showEmpireDetails && w.player != null) {
			int contentW = 0;
			int contentH = 0;
			int lineItemsW = 0;
			int moneyW = (int) d.textSize("10000", AGame.FOUNT).x;
			int buttonsW = -MyDraw.BUTTON_SPACING;
			
			for (EmpireDetailsMode edm : EmpireDetailsMode.values()) {
				if (!edm.available(w)) { continue; }
				buttonsW += d.tw(_t(edm.name), edm.shortcut) + MyDraw.BUTTON_SPACING;
			}

			lineItemsW = StrictMath.max(lineItemsW, (int) d.textSize(_t("Cities"), AGame.FOUNT).x);
			lineItemsW = StrictMath.max(lineItemsW, (int) d.textSize(_t("Defences_Maintenance"), AGame.FOUNT).x);
			lineItemsW = StrictMath.max(lineItemsW, (int) d.textSize(_t("Fleet_Maintenance"), AGame.FOUNT).x);
			lineItemsW = StrictMath.max(lineItemsW, (int) d.textSize(_t("City_Upgrades"), AGame.FOUNT).x);
			if (w.player.isCoronating()) {
				lineItemsW = StrictMath.max(lineItemsW, (int) d.textSize(_t("coronation_preparations"), AGame.FOUNT).x);
			}
			contentW = StrictMath.max(contentW, lineItemsW + MyDraw.UI_SPACING + moneyW + MyDraw.UI_SPACING);
			contentW = StrictMath.max(contentW, buttonsW);
			int els = 4;
			if (EmpireStat.EMPIRE_BASE_INCOME.get(w.player.bonuses) != 0) { els++; }
			if (w.player.isCoronating()) { els++; }
			if (w.player.isDoingFinalRitual()) { els++; }
			if (w.player.tradeIncome(w.map) > 0) { els++; }
			if (w.player.tributeIncome(w.map) > 0) { els++; }
			if (w.player.tributeCost(w.map) > 0) { els++; }
			if (w.player.heroesCost(w.map) > 0) { els++; }
			contentH = StrictMath.max(contentH, (AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING) * els + AGame.FOUNT.height);
			
			Pt ts = d.textSize(_t("The_Empire_of_x", w.player.name), AGame.BIGGER_FOUNT);

			contentW = StrictMath.max(contentW, (int) ts.x);
			
			ArrayList<Bonus> blist = w.player.bonuses().list();
			ArrayList<String> bstrings = new ArrayList<String>();
			for (Tech.Choice t : w.player.techs) {
				blist.removeAll(t.bonusList);
			}
			for (Bonus b : blist) {
				if (b.name.equals("NO_BONUS")) { continue; }
				if (b.getName().equals(b.getDesc())) {
					bstrings.add(MyDraw.SELECTED_C + b.getName() + "\n");
				} else {
					bstrings.add(MyDraw.SELECTED_C + b.getName() + "[]\n" + b.getDesc() + "\n");
				}
			}
			contentW = Math.max(contentW, sm.width / 3 + ScrollBar.SCROLL_BAR_W + MyDraw.UI_SPACING);
			contentH = Math.max(contentH, sm.height / 3);
			
			if (empireDetailsMode == EmpireDetailsMode.SHIPS || empireDetailsMode == EmpireDetailsMode.CITIES) {
				contentW = sm.width - MyDraw.WINDOW_INSET * 2 - MyDraw.SIDE_CLEARANCE * 2;
				contentH = sm.height - MyDraw.SIDE_CLEARANCE * 2 - (140 + AGame.BIGGER_FOUNT.height + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.UI_SPACING + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2);
			}
			
			int width = contentW + MyDraw.WINDOW_INSET * 2;
			int height = 140 + AGame.BIGGER_FOUNT.height + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.UI_SPACING + contentH + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			x = sm.width / 2 - width / 2;
			int y = sm.height / 2 - height / 2;
			if (empireDetailsMode == EmpireDetailsMode.SHIPS || empireDetailsMode == EmpireDetailsMode.CITIES) {
				contentH -= MyDraw.TOP_BAR_H;
				height -= MyDraw.TOP_BAR_H;
				y += MyDraw.TOP_BAR_H;
			}
			d.drawShadowedWindow(x, y, width, height, 13 + empireDetailsMode.ordinal());
			d.hook(x, y, width, height, new Hook(Hook.Type.HOVER, Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN) {
				@Override
				public void run(Input input, Pt pt, Type type) {
					// make window opaque to clicks
				}
			});
			x += MyDraw.WINDOW_INSET;
			y += MyDraw.WINDOW_INSET;
			width -= MyDraw.WINDOW_INSET * 2;
			
			d.drawPanel(x + (width / 2 - 66), y, 132, 132, -1);
			y += 2;
			ArrayList<CoatOfArms> coas = new ArrayList<CoatOfArms>();
			for (City c : w.player.cities) {
				if (c.originalEmpire == w.player) {
					if (!coas.contains(w.player.arms)) {
						coas.add(0, w.player.arms);
					}
				} else {
					if (!coas.contains(c.originalArms)) {
						coas.add(c.originalArms);
					}
				}
			}
			if (coas.size() < 2) {
				w.player.arms.draw(d, x + (width / 2 - 64), y, 128);
			} else {
				Marshalling.marshal(coas, d, x + (width / 2 - 64), y, 128, MyDraw.DARK_BG);
			}
			y += 140;
			d.text(_t("The_Empire_of_x", w.player.name), AGame.BIGGER_FOUNT, x + (int) (width / 2 - ts.x / 2), y);
			y += AGame.BIGGER_FOUNT.height + MyDraw.UI_SPACING;

			int x2 = x;
			for (final EmpireDetailsMode edm : EmpireDetailsMode.values()) {
				if (!edm.available(w)) { continue; }
				int tw = d.tw(_t(edm.name), edm.shortcut);
				d.toggle(x2, y, tw, _t(edm.name), edm.shortcut, new InputRunnable() {
					@Override
					public void run(Input in) {
						empireDetailsMode = edm;
					}
				}, empireDetailsMode == edm, true);
				x2 += tw + MyDraw.BUTTON_SPACING;
			}
			
			y += MyDraw.BUTTON_H + MyDraw.UI_SPACING;
			
			switch (empireDetailsMode) {
				case BUDGET:
					int prevY = y;
					if (EmpireStat.EMPIRE_BASE_INCOME.get(w.player.bonuses) != 0) {
						d.text(_t("Base_Income"), AGame.FOUNT, x, y);
						d.text("" + EmpireStat.EMPIRE_BASE_INCOME.get(w.player.bonuses), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + EmpireStat.EMPIRE_BASE_INCOME.get(w.player.bonuses), AGame.FOUNT).x, y);
						d.tooltip(x, y, contentW, AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING, EmpireStat.EMPIRE_BASE_INCOME.explain(w.player.bonuses));
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					d.text(_t("Cities"), AGame.FOUNT, x, y);
					d.text("" + w.player.cityIncome(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + w.player.cityIncome(w.map), AGame.FOUNT).x, y);
					y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					
					if (w.player.tradeIncome(w.map) > 0) {
						d.text(_t("Trade_Income"), AGame.FOUNT, x, y);
						d.text("" + w.player.tradeIncome(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + w.player.tradeIncome(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					if (w.player.tributeIncome(w.map) > 0) {
						d.text(_t("Tribute_Received"), AGame.FOUNT, x, y);
						d.text("" + w.player.tributeIncome(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + w.player.tributeIncome(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					if (w.player.tributeCost(w.map) > 0) {
						d.text(_t("Tribute_Paid"), AGame.FOUNT, x, y);
						d.text("" + -w.player.tributeCost(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.tributeCost(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					if (w.player.heroesCost(w.map) > 0) {
						d.text(_t("Heroes"), AGame.FOUNT, x, y);
						d.text("" + -w.player.heroesCost(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.heroesCost(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}

					d.text(_t("Defences_Maintenance"), AGame.FOUNT, x, y);
					d.text("" + -w.player.totalDefencesCost(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.totalDefencesCost(w.map), AGame.FOUNT).x, y);
					y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;

					d.text(_t("Fleet_Maintenance"), AGame.FOUNT, x, y);
					d.text("" + -w.player.totalFleetCosts(), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.totalFleetCosts(), AGame.FOUNT).x, y);
					y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					
					d.text(_t("City_Upgrades"), AGame.FOUNT, x, y);
					d.text("" + -w.player.totalUpgradeMaintenanceCosts(), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.totalUpgradeMaintenanceCosts(), AGame.FOUNT).x, y);	
					y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;

					if (w.player.isCoronating()) {
						d.text(_t("coronation_preparations"), AGame.FOUNT, x, y);
						d.text("" + -w.player.coronationCost(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.coronationCost(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					if (w.player.isDoingFinalRitual()) {
						d.text(_t("Final_Ritual"), AGame.FOUNT, x, y);
						d.text("" + -w.player.finalRitualCost(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + -w.player.finalRitualCost(w.map), AGame.FOUNT).x, y);
						y += AGame.FOUNT.height + MyDraw.SCROLL_EL_SPACING;
					}
					
					d.text(_t("TOTAL"), AGame.FOUNT, x, y);
					d.text("" + w.player.incomeBalance(w.map), AGame.FOUNT, x + lineItemsW + MyDraw.UI_SPACING + moneyW - (int) d.textSize("" + w.player.incomeBalance(w.map), AGame.FOUNT).x, y);
					y += AGame.FOUNT.height;
					y = prevY + contentH;
					break;
				case SHIPS:
					shipsLedger.draw(d, x, y, contentW, contentH);
					y += contentH;
					break;
				case CITIES:
					cityLedger.draw(d, x, y, contentW, contentH);
					y += contentH;
					break;
				case MEDALS:
					medalsSBR.x = x;
					medalsSBR.y = y;
					medalsSBR.w = contentW;
					medalsSBR.h = contentH;
					ArrayList<Integer> levels = new ArrayList<Integer>();
					for (int lvl = 1; lvl <= CrewExperienceLevel.getMaxMedalLevel(w.player.bonuses); lvl++) {
						levels.add(lvl);
					}
					medalsSB.drawNaked(d, x, y, contentW, contentH, levels, new EmpireMedalLevelRenderer(this, null));
					y += contentH;
					break;
				case BONUSES:
					empireBonusesSBR.x = x;
					empireBonusesSBR.y = y;
					empireBonusesSBR.w = contentW;
					empireBonusesSBR.h = contentH;
					empireBonusesSB.drawNaked(d, x, y, contentW, contentH, bstrings, new TextAdapter());
					//d.text(bonusString, AGame.FOUNT, x, y, contentW);
					y += contentH;
					break;
				case SETTINGS:
					settingsSBR.x = x;
					settingsSBR.y = y;
					settingsSBR.w = contentW;
					settingsSBR.h = contentH;
					ViewSettingsAdapter vsa = new ViewSettingsAdapter(this);
					if (vsa.getHeight("x", d, contentW) <= contentH) {
						vsa.draw("x", d, x, y, contentW);
					} else {
						settingsSB.drawNaked(d, x, y, contentW, contentH, Collections.singletonList("x"), vsa);
					}
					y += contentH;
					break;
			}
			
			y += MyDraw.UI_SPACING;
			
			d.button(sm.width / 2 - d.bw(_t("OK")) / 2, y, d.bw(_t("OK")), _t("OK"), new Runnable() {
				@Override
				public void run() {
					showEmpireDetails = false;
				}
			});
		}
		
		PerfStats.mark("ss empireDetails");
		
		if (selectedNest != null && selectedNest.type == null) { selectedNest = null; }
		
		if (selectedNest != null || (w.player != null && (w.player.rewardGiven != null || upgradeStartedNest != null || upgradeNearingCompletionNest != null || upgradeCompleteNest != null))) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			hs.list.clear();
			MonsterNest nest = null;
			if (selectedNest != null) { nest = selectedNest; }
			if (upgradeStartedNest != null) { nest = upgradeStartedNest; }
			if (upgradeNearingCompletionNest != null) { nest = upgradeNearingCompletionNest; }
			if (upgradeCompleteNest != null) { nest = upgradeCompleteNest; }
			if (nest != null) {
				Graphics g = (Graphics) d.frame().nativeRenderer();
				g.setColor(Color.black);
				g.setLineWidth(6);
				g.drawLine(sm.width / 2, sm.height / 2, (int) worldToScreenX(nest.x), (int) worldToScreenY(nest.y));
				g.setColor(new Color(245, 228, 116));
				g.setLineWidth(2);
				g.drawLine(sm.width / 2, sm.height / 2, (int) worldToScreenX(nest.x), (int) worldToScreenY(nest.y));
				g.setLineWidth(1);
			}
			int width = 400;
			String infoTitle = "?";
			String infoText = "?";
			Img img = null;
			if (w.player != null && w.player.rewardGiven != null) {
				if (!w.usedCheatCommand && w.player.rewardWinnerEmpires != null && w.player.rewardWinnerEmpires.contains(w.player) &&
						(w.player.rewardGiven.name.equals("decadenceVictory") || w.player.rewardGiven.name.equals("indRevEnd") || w.player.rewardGiven.name.equals("indRevEndRev"))
				) {
					Achievement.achieve(Achievement.OUTRAGE_OVERFLOW_EXCEPTION);
				}
				String rewardWinnerEmpireName = w.player.rewardWinnerEmpires == null ? w.player.name : Empire.nameList(w.player.rewardWinnerEmpires, Lang.currentLocale);
				Reward r = w.player.rewardGiven;
				infoTitle = _t(r.title);
				infoText = "";
				int money = r.getMoney(w.player);
				int research = r.getResearch(w.player);
				int rep = w.has(ConquestToggle.REPUTATION) ? r.rep : 0;
				int supplies = r.supplies;
				if (money != 0) {
					infoText += "\n+ $" + money;
				}
				if (supplies != 0) {
					infoText += "\n+ " + _t("x_Supplies", supplies);
				}
				if (research != 0) {
					infoText += "\n+ " + research + " " + _t("Research");
				}
				if (w.has(ConquestToggle.REPUTATION)) {
					if (rep > 0) {
						infoText += "\n+ " + rep + " " + _t("Reputation");
					}
					if (rep < 0) {
						infoText += "\n- " + -rep + " " + _t("Reputation");
					}
				}
				if (r.bonus != null) {
					infoText += "\n+ " + r.bonus.getName();
					for (CityUpgradeType cut : Loadable.all(CityUpgradeType.class)) {
						if (cut.requires.contains[r.bonus.ordinal()]) {
							infoText += "\n  " + _t(cut.forTown ? "enables_town_upgrade_x" : "enables_city_upgrade_x", cut.getName());
						}
					}
				}
				if (r.tech != null) {
					infoText += "\n+ " + _t("Technology_") + _t("tech_" + r.tech.name);
				}
				if (r.specialConstruction != null) {
					if (r.numSpecialConstructions == 1) {
						infoText += "\n" + _t("single_" + r.specialConstruction.getName().replace(" ", "_"));
					} else {
						infoText += "\n" + r.numSpecialConstructions + " " + _t("plural_" + r.specialConstruction.getName().replace(" ", "_"));
					}
				}
				if (r.destroyAllUpgrades != null) {
					infoText += "\n" + _t("all_x_upgrades_destroyed", _t("cityUpgrade_plural_" + r.destroyAllUpgrades.name));
				}
				if (r.getStartEraModifier() != null) {
					infoText += "\n" + _t("bonus_" + r.getStartEraModifier().name);
				}
				if (r.getEndsEraModifier() != null) {
					infoText += "\n" + _t("ends_era_modifier_x", _t("bonus_" + r.getEndsEraModifier().name));
				}
				String flavorKey = r.nestType == null ? r.name : r.name + r.nestType.bonusSuffix(w.player);
				if (Lang.hasLocalString(flavorKey)) {
					if (!infoText.isEmpty()) {
						infoText = _t(flavorKey, rewardWinnerEmpireName, _t(w.player.rewardDetails[0]), _t(w.player.rewardDetails[1]), _t(w.player.rewardDetails[2])) + "\n" + MyDraw.SELECTED_C + infoText;
					} else {
						infoText = _t(flavorKey, rewardWinnerEmpireName, _t(w.player.rewardDetails[0]), _t(w.player.rewardDetails[1]), _t(w.player.rewardDetails[2]));
					}
				} else {
					if (infoText.startsWith("\n")) {
						infoText = infoText.substring(1);
					}
					infoText = MyDraw.SELECTED_C + infoText;
				}
				infoText += "[]" + w.player.rewardExtraInfo;
				img = r.getImg();
			} else if (selectedNest != null) {
				infoTitle = _t(selectedNest.type.name + "_displayName" + selectedNest.type.bonusSuffix(w.player));
				infoText = _t(selectedNest.type.name + "_description" + selectedNest.type.bonusSuffix(w.player));
				img = selectedNest.type.getImg();
			} else if (upgradeStartedNest != null) {
				infoTitle = _t(upgradeStartedNest.type.name + "_displayName" + upgradeStartedNest.type.bonusSuffix(w.player));
				infoText = Lang._tWithFallback(upgradeStartedNest.type.name + "_upgradeStartedDescription" + upgradeStartedNest.type.bonusSuffix(w.player), upgradeStartedNest.type.name + "_upgradeCompleteDescription");
				img = upgradeStartedNest.type.getImg();
			} else if (upgradeNearingCompletionNest != null) {
				infoTitle = _t(upgradeNearingCompletionNest.type.name + "_displayName" + upgradeNearingCompletionNest.type.bonusSuffix(w.player));
				infoText = _t(upgradeNearingCompletionNest.type.name + "_upgradeNearingCompletionDescription" + upgradeNearingCompletionNest.type.bonusSuffix(w.player));
				img = upgradeNearingCompletionNest.type.getImg();
			} else if (upgradeCompleteNest != null) {
				infoTitle = _t(upgradeCompleteNest.type.name + "_displayName" + upgradeCompleteNest.type.bonusSuffix(w.player));
				infoText = _t(upgradeCompleteNest.type.name + "_upgradeCompleteDescription" + upgradeCompleteNest.type.bonusSuffix(w.player));
				img = upgradeCompleteNest.type.getImg();
			}
			int titleH = (int) d.textSize(infoTitle, AGame.BIGGER_FOUNT, 0, 0, width).height;
			int textH = (int) d.textSize(infoText, AGame.FOUNT, 0, 0, width).height;
			int height = titleH + MyDraw.BUTTON_SPACING + 300 + MyDraw.UI_SPACING + textH + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			width += MyDraw.WINDOW_INSET * 2;
			x = sm.width / 2 - width / 2;
			int y = sm.height / 2 - height / 2;
			d.drawWindow(x, y, width, height, 4);
			width -= MyDraw.WINDOW_INSET * 2;
			height -= MyDraw.WINDOW_INSET * 2;
			x += MyDraw.WINDOW_INSET;
			y += MyDraw.WINDOW_INSET;
			if (titleH > AGame.BIGGER_FOUNT.lineHeight * 1.1) {
				d.text(MyDraw.TITLE_C + infoTitle, AGame.BIGGER_FOUNT, x, y, width);
			} else {
				d.text(MyDraw.TITLE_C + infoTitle, AGame.BIGGER_FOUNT, sm.width / 2 - (int) (d.textSize(infoTitle, AGame.BIGGER_FOUNT).x) / 2, y, width);
			}
			y += titleH + MyDraw.BUTTON_SPACING;
			d.blit(img, x, y);
			d.drawPanelBorder(x, y, 400, 300);
			y += 300 + MyDraw.UI_SPACING;
			d.text(infoText, AGame.FOUNT, x, y, width);
			y += textH + MyDraw.UI_SPACING;
			d.button(sm.width / 2 - d.bw(_t("OK")) / 2, y, d.bw(_t("OK")), _t("OK"), new Runnable() {
				@Override
				public void run() {
					if (w.player != null && w.player.rewardGiven != null) {
						w.player.rewardGiven = null;
					} else if (selectedNest != null) {
						selectedNest = null;
					} else if (upgradeStartedNest != null) {
						upgradeStartedNest.showUpgradeStartDialog = false;
						upgradeStartedNest = null;
					} else if (upgradeNearingCompletionNest != null) {
						upgradeNearingCompletionNest.showUpgradeNearingCompletionDialog = false;
						upgradeNearingCompletionNest = null;
					} else if (upgradeCompleteNest != null) {
						upgradeCompleteNest.showUpgradeCompleteDialog = false;
						upgradeCompleteNest = null;
					}
				}
			});
		}
		
		if (w.player != null && (travelToDialogDestination != null || travelToDialogTarget != null)) {
			d.getHooks().list.clear();
			renderSupplySplitDialog(d, sm, hs, cursor);
		}
		
		PerfStats.mark("ss nestInfo");
		
		if (w.player != null && w.player.researchedTech != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = (int) d.textSize(_t("Research_complete_x", _t("tech_" + w.player.researchedTech.name)), AGame.BIG_FOUNT).x;
			int bsz = w.player.researchedTech.bonusList.size();
			for (int bi = 0; bi < bsz; bi++) {
				Bonus b = w.player.researchedTech.bonusList.get(bi);
				width = StrictMath.max(width, (int) d.textSize(_t("bonus_" + b.name + "_desc"), AGame.FOUNT).x);
			}
			if (w.player.research != null) {
				width = StrictMath.max(width, (int) d.textSize(_t("Researching_x", _t("tech_" + w.player.research.name)), AGame.FOUNT).x);
			}
			width += TechScreen.TECH_ICON_SIZE + MyDraw.UI_SPACING + MyDraw.WINDOW_INSET * 2;
			int infoHeight = StrictMath.max(TechScreen.TECH_ICON_SIZE, AGame.BIG_FOUNT.lineHeight + w.player.researchedTech.bonusList.size() * AGame.FOUNT.lineHeight);
			int height = infoHeight + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			if (w.player.research != null) {
				height += MyDraw.UI_SPACING + AGame.FOUNT.lineHeight;
			}
			final boolean choiceNeeded = w.player.research == null && !w.player.isAllResearchDone(w.map);
			if (choiceNeeded) {
				height += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			x = sm.width / 2 - width / 2;
			int y = sm.height / 2 - height / 2;
			d.drawWindow(x, y, width, height, 21);
			x += MyDraw.WINDOW_INSET;
			y += MyDraw.WINDOW_INSET;
			width -= MyDraw.WINDOW_INSET * 2;
			d.blit(w.player.researchedTech.getImg(), x, y);
			int x2 = x + TechScreen.TECH_ICON_SIZE + MyDraw.UI_SPACING;
			int y2 = y;
			d.text(_t("Research_complete_x", _t("tech_" + w.player.researchedTech.name)), AGame.BIG_FOUNT, x2, y2);
			y2 += AGame.BIG_FOUNT.lineHeight;
			for (int bi = 0; bi < bsz; bi++) {
				Bonus b = w.player.researchedTech.bonusList.get(bi);
				d.text(_t("bonus_" + b.name + "_desc"), AGame.FOUNT, x2, y2);
				y2 += AGame.FOUNT.lineHeight;
			}
			y += infoHeight + MyDraw.UI_SPACING;
			if (w.player.research != null) {
				d.text(_t("Researching_x", _t("tech_" + w.player.research.name)), AGame.FOUNT, x2, y);
				y += MyDraw.UI_SPACING + AGame.FOUNT.lineHeight;
			}
			
			final StrategicScreen self = this;
			if (choiceNeeded) {
				d.button(x, y, width, _t("Select_Research"), new Runnable() {
					@Override
					public void run() {
						if (w.player == null) { return; }
						w.player.researchedTech = null;
						g.s = new TechScreen(self);
					}
				});
				y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
			d.button(x, y, width, _t("OK"), new Runnable() {
				@Override
				public void run() {
					if (w.player == null) { return; }
					w.player.researchedTech = null;
				}
			});
		}
		
		if (w.map.newEra) {
			d.getHooks().list.clear();
			d.state.removeIntersectingGlowRects(0, 0, sm.width, sm.height);
			info = new StringBuilder();
			int maxTextWidth = Math.min(sm.width - MyDraw.SIDE_CLEARANCE * 2 - MyDraw.WINDOW_INSET * 2 - 404 - MyDraw.UI_SPACING, 404);
			if (!w.map.eraModifier.name.equals("NO_BONUS") && !w.map.newEraIsContinuedEra) {
				if (Lang.flavour()) {
					info.append(_t(w.map.eraModifier.desc.get(w.player == null ? BonusSet.empty() : w.player.bonuses), w.map.newEraDetail[0], w.map.newEraDetail[1], w.map.newEraDetail[2])).append("\n\n");
				}
				info.append(MyDraw.SELECTED_C).append(_t(w.map.eraModifier.effectsDesc.get(w.player == null ? BonusSet.empty() : w.player.bonuses))).append("\n");
			}
			info.append("\n").append(_t("x_base_research", EmpireStat.RESEARCH.get(w.map.worldBonuses())));
			info.append("\n").append(_t("x_spies_available", EmpireStat.MAX_SPIES.get(w.map.worldBonuses())));
			info.append("\n").append(_t("x_unrest_per_city", w.map.unrestPerCity(w.map.worldBonuses())));
			info.append("\n").append(_t("x_unrest_per_town", w.map.unrestPerTown(w.map.worldBonuses())));
			
			String infoS = info.toString();
			Rect infoSz = d.textSize(infoS, AGame.FOUNT, 0, 0, maxTextWidth);
			String title = w.map.eraModifier.name.equals("NO_BONUS") ? _t("a_new_age_dawns") : _t("bonus_" + w.map.eraModifier.name);
			if (w.map.newEraIsContinuedEra && !w.map.eraModifier.name.equals("NO_BONUS")) {
				title = _t("continued_era_") + title;
			}
			int width = (int) infoSz.width;
			int height = AGame.HUGE_FOUNT.lineHeight + MyDraw.UI_SPACING + Math.max(w.map.eraModifier.eraImg != null ? 300 + 4 : 0, (int) infoSz.height) + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			if (w.map.eraModifier.eraImg != null) {
				width += MyDraw.UI_SPACING + 4 + 400;
			}
			width = Math.max(Math.max(400, width), (int) d.textSize(title, AGame.HUGE_FOUNT).x) + MyDraw.WINDOW_INSET * 2;
			x = sm.width / 2 - width / 2;
			int y = sm.height / 2 - height / 2;
			d.drawWindow(x, y, width, height, w.map.eraModifier.pattern);
			x += MyDraw.WINDOW_INSET;
			y += MyDraw.WINDOW_INSET;
			d.text(MyDraw.TITLE_C + title, AGame.HUGE_FOUNT, sm.width / 2 - (int) d.textSize(title, AGame.HUGE_FOUNT).x / 2, y);
			y += AGame.HUGE_FOUNT.lineHeight + MyDraw.UI_SPACING;
			if (w.map.eraModifier.eraImg != null) {
				d.blit(w.map.eraModifier.eraImg, x + 2, y + 2, 400, 300);
				d.drawPanelBorder(x, y, 400 + 4, 300 + 4);
				x += MyDraw.PANEL_INSET * 2 + 400 + 4;
			}
			d.text(infoS, AGame.FOUNT, x, y, maxTextWidth);
			y += Math.max(infoSz.height, w.map.eraModifier.eraImg != null ? 300 + 4 : 0) + MyDraw.UI_SPACING;
			bw = d.bw(_t("OK"));
			d.button(sm.width / 2 - bw / 2, y, bw, _t("OK"), new Runnable() {
				@Override
				public void run() {
					w.map.newEra = false;
				}
			});
		}
		PerfStats.mark("ss researchInfo, era");

		if (menu) {
			renderMenu(d, sm, hs, cursor);
		}
		
		PerfStats.mark("ss menu");
		
		if (w.player != null && helpMsg != null) {
			int y = MyDraw.TOP_BAR_H + MyDraw.UI_SPACING;
			x = MyDraw.WINDOW_INSET;
			int width = StrictMath.min(500, sm.width - 300);
			Rect tsz = d.textSize(helpMsg.text, AGame.FOUNT, x, y, width - MyDraw.WINDOW_INSET * 2);
			Pt pointer = null;
			if (helpMsg.city != null) {
				pointer = new Pt(worldToScreenX(helpMsg.city.x), worldToScreenY(helpMsg.city.y));
			}
			if (helpMsg.fleet != null) {
				pointer = new Pt(worldToScreenX(helpMsg.fleet.realX(w.map)), worldToScreenY(helpMsg.fleet.realY(w.map)));
			}
			if (helpMsg.speedControls) {
				pointer = new Pt(speedX, MyDraw.TOP_BAR_H / 2);
			}
			if (pointer != null) {
				int y2 = y + (int) tsz.height + MyDraw.WINDOW_INSET * 2;
				Graphics g = (Graphics) d.frame().nativeRenderer();
				g.setColor(Color.black);
				g.setLineWidth(6);
				g.drawLine(x + width - 1, y2 - 1, (int) pointer.x, (int) pointer.y);
				g.setColor(new Color(245, 228, 116));
				g.setLineWidth(2);
				g.drawLine(x + width - 1, y2 - 1, (int) pointer.x, (int) pointer.y);
				g.setLineWidth(1);
			}
			d.drawShadowedWindow(x, y, width, (int) tsz.height + MyDraw.WINDOW_INSET * 2, -1);
			d.text(helpMsg.text, AGame.FOUNT, x + MyDraw.WINDOW_INSET, y + MyDraw.WINDOW_INSET, width - MyDraw.WINDOW_INSET * 2);
			y += (int) tsz.height + MyDraw.WINDOW_INSET * 2 + MyDraw.UI_SPACING;
			final CampaignWorld cw = this.w;
			d.button(x, y, d.bw(_t("OK")), _t("OK"), new Runnable() {
				@Override
				public void run() {
					cw.help.clear(helpMsg.type);
				}
			});
		}
		
		if (showDiplomacy && w.player == null) {
			showDiplomacy = false;
		}
		
		if (w.someoneWon()) { // MERGEME Are we ever gonna get to here? Isn't this pre-empted by a fancier dialog further up? -- who cares?
			d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, _t("x_has_won", w.winnerName()), new Runnable() {
				@Override
				public void run() {
					leave();
				}
			});
		} else if (showDiplomacy) {
			d.getHooks().list.clear();
			hs.list.clear();
			d.state.clearGlowRects();
			diplomacy.render(d, sm, hs, cursor);
			if (!w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains)) {
				//int hh = ConquestHelpSystem.getHeight(d, this, this, sm.width / 3);
				//d.rect(Clr.RED, 5, sm.height - MyDraw.SIDE_CLEARANCE - hh, 3, hh);
				ConquestHelpSystem.render(d, MyDraw.SIDE_CLEARANCE, sm.height - MyDraw.SIDE_CLEARANCE - ConquestHelpSystem.getHeight(d, this, this, sm.width / 3),  this, this, sm.width / 3);
			}
		} else if (showHeroes) {
			d.getHooks().list.clear();
			hs.list.clear();
			d.state.clearGlowRects();
			HeroRenderer hr = (cityForHero == null && shipForHero == null) ? new HeroRenderer(w, this) : new HeroRenderer(this, this);
			int hw = hr.getWidth(d) + ScrollBar.SCROLL_BAR_W + MyDraw.PANEL_INSET * 2 + MyDraw.WINDOW_INSET * 2;
			int hh = sm.height * 3 / 4;
			int hx = sm.width / 2 - hw / 2;
			int hy = sm.height / 2 - hh / 2;
			if (!w.isMultiplayer() && (g.strategicHelp || g.strategicHelpHeroesAndVillains) && ConquestHelpSystem.hasAny(this, this)) {
				int helpH = ConquestHelpSystem.getHeight(d, this, this, sm.width / 3);
				hy = MyDraw.SIDE_CLEARANCE;
				hh = sm.height - hy - MyDraw.SIDE_CLEARANCE - helpH - MyDraw.UI_SPACING;
			}
			d.drawWindow(hx, hy, hw, hh, 23);
			hx += MyDraw.WINDOW_INSET;
			hy += MyDraw.WINDOW_INSET;
			hw -= MyDraw.WINDOW_INSET * 2;
			hh -= MyDraw.WINDOW_INSET * 2;
			String okb = _t("OK");
			ArrayList<Hero> heroes = null;
			if (cityForHero != null && !w.player.cities.contains(cityForHero)) {
				cityForHero = null;
			}
			if (shipForHero != null && !w.player.has(shipForHero)) {
				shipForHero = null;
			}
			if (heroToShow != null && heroToShow.inEmpire != w.player) {
				heroToShow = null;
			}
			if (cityForHero != null) {
				heroes = Hero.getHeroes(HeroType.Role.GOVERNOR, true, w.player, w.map);
				d.text(MyDraw.TITLE_C + _t("select_governor_for_x", cityForHero.getDisplayName()), AGame.BIG_FOUNT, hx, hy);
				hy += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
				hh -= AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
				okb = _t("Cancel");
			} else if (shipForHero != null) {
				heroes = Hero.getHeroes(HeroType.Role.CAPTAIN, true, w.player, w.map);
				d.text(MyDraw.TITLE_C + _t("select_captain_for_x", shipForHero.getName()), AGame.BIG_FOUNT, hx, hy);
				hy += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
				hh -= AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
				okb = _t("Cancel");
			} else if (heroToShow != null) {
				heroes = new ArrayList<Hero>();
				heroes.add(heroToShow);
			} else {
				heroes = Hero.getHeroes(w.player, w.map);
				d.text(MyDraw.TITLE_C + _t("Heroes"), AGame.BIGGER_FOUNT, hx + hw / 2 - (int) d.textSize(_t("Heroes"), AGame.BIGGER_FOUNT).x / 2, hy);
				hy += AGame.BIGGER_FOUNT.lineHeight + MyDraw.UI_SPACING;
				hh -= AGame.BIGGER_FOUNT.lineHeight + MyDraw.UI_SPACING;
			}
			d.button(hx + hw / 2 - d.bw(okb) / 2, hy + hh - MyDraw.BUTTON_H, d.bw(okb), okb, new Runnable() {
				@Override
				public void run() {
					showHeroes = false;
					heroToShow = null;
					cityForHero = null;
					shipForHero = null;
				}
			});
			hh -= MyDraw.BUTTON_H + MyDraw.UI_SPACING;
			heroesSB.draw(d, hx, hy, hw, hh, heroes, hr);
		} else if (combatOutcome != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			hs.list.clear();
			// 3 lines: what, where, who
			String what = combatOutcome.type.getName();
			String where = combatOutcome.loc == null ? "" : combatOutcome.loc.getDisplayName();
			String who = combatOutcome.opponentName;
			int width = (int) StrictMath.max(StrictMath.max(d.textSize(what, AGame.HUGE_FOUNT).x, d.textSize(where, AGame.FOUNT).x), d.textSize(who, AGame.FOUNT).x) + MyDraw.PANEL_INSET * 2 + MyDraw.WINDOW_INSET * 2;
			int xx = sm.width / 2 - width / 2;
			int height = AGame.HUGE_FOUNT.lineHeight + MyDraw.UI_SPACING + AGame.FOUNT.lineHeight * 2 + MyDraw.PANEL_INSET * 2 + MyDraw.WINDOW_INSET;
			int yy = sm.height / 3 - height/ 2;
			checkedBlit(d, sm, fight_big, Clr.WHITE, sm.width / 2 - 32, yy - MyDraw.UI_SPACING - 64);
			Graphics g = (Graphics) d.frame().nativeRenderer();
			g.setColor(Color.black);
			g.setLineWidth(6);
			g.drawLine(sm.width / 2, yy + height / 2, (int) worldToScreenX(combatOutcome.x), (int) worldToScreenY(combatOutcome.y));
			g.setColor(new Color(245, 228, 116));
			g.setLineWidth(2);
			g.drawLine(sm.width / 2, yy + height / 2, (int) worldToScreenX(combatOutcome.x), (int) worldToScreenY(combatOutcome.y));
			g.setLineWidth(1);
			d.drawPanel(xx, yy, width, height, 23);
			xx += MyDraw.PANEL_INSET;
			yy += MyDraw.PANEL_INSET;
			d.text(what, AGame.HUGE_FOUNT, sm.width / 2 - (int) d.textSize(what, AGame.HUGE_FOUNT).x / 2, yy);
			yy += AGame.HUGE_FOUNT.lineHeight + MyDraw.UI_SPACING;
			d.text(where, AGame.FOUNT, sm.width / 2 - (int) d.textSize(where, AGame.FOUNT).x / 2, yy);
			yy += AGame.FOUNT.lineHeight;
			d.text(who, AGame.FOUNT, sm.width / 2 - (int) d.textSize(who, AGame.FOUNT).x / 2, yy);
			yy += AGame.FOUNT.lineHeight;
			d.hook(0, 0, sm.width, sm.height, new Hook("confirmCombatOutcome", Hook.Type.MOUSE_1_CLICKED, Hook.Type.TEST) {
				@Override
				public void run(Input in, Pt p, Type type) {
					combatOutcome = null;
				}
			});
		} else if (hasDeclarationsOfWar()) {
			DiplomacyNotice dn = declarationOfWar();
			String text = dn.getMessage(w.player);
			String cascade = null;
			if (dn.cascadedFromDeclarationOfWarOn != null && w.map.getRelationship(w.player, dn.cascadedFromDeclarationOfWarOn).level == Relationship.Level.ALLIANCE) {
				cascade = _t("war_alliance_cascade", dn.other.getName(), dn.cascadedFromDeclarationOfWarOn.getName());
			}
			if (dn.cascadedFromDeclarationOfWarOn != null && w.map.getRelationship(w.player, dn.cascadedFromDeclarationOfWarOn).level == Relationship.Level.DEFENSIVE_PACT) {
				cascade = _t("war_pact_cascade", dn.other.getName(), dn.cascadedFromDeclarationOfWarOn.getName());
			}
			int width = (int) d.textSize(text, AGame.BIGGER_FOUNT).x;
			if (cascade != null) {
				width = Math.max(width, (int) d.textSize(cascade, AGame.FOUNT).x);
			}
			width += MyDraw.WINDOW_INSET * 2;
			int height = 64 + MyDraw.PANEL_BORDER_W  * 2 + MyDraw.UI_SPACING * 2 + AGame.BIGGER_FOUNT.lineHeight + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			if (cascade != null) {
				height += MyDraw.UI_SPACING + AGame.FOUNT.lineHeight;
			}
			int xx = sm.width / 2 - width / 2;
			int yy = sm.height / 3 - height / 3;
			d.drawWindow(xx, yy, width, height, 2);
			xx += MyDraw.WINDOW_INSET;
			yy += MyDraw.WINDOW_INSET;
			width -= MyDraw.WINDOW_INSET * 2;
			if (dn.basis != null && dn.basis.other(dn.other) != w.player) {
				dn.basis.other(dn.other).arms.draw(d, xx + width / 2 - 32, yy + MyDraw.PANEL_BORDER_W, 64);
				d.tooltip(xx + width / 2 - 32, yy + MyDraw.PANEL_BORDER_W, 64, 64, dn.basis.other(dn.other).getTooltip(w.player, w.map));
			} else {
				dn.other.arms.draw(d, xx + width / 2 - 32, yy + MyDraw.PANEL_BORDER_W, 64);
				d.tooltip(xx + width / 2 - 32, yy + MyDraw.PANEL_BORDER_W, 64, 64, dn.other.getTooltip(w.player, w.map));
			}
			d.drawPanelBorder(xx + width / 2 - 32 - MyDraw.PANEL_BORDER_W, yy, 64 + MyDraw.PANEL_BORDER_W * 2, 64 + MyDraw.PANEL_BORDER_W * 2);
			d.blit(fight_big, xx + width / 2 - 32, yy + MyDraw.PANEL_BORDER_W);
			yy += 64 + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING;
			d.text(text, AGame.BIGGER_FOUNT, xx + width / 2 - (int) d.textSize(text, AGame.BIGGER_FOUNT).x / 2, yy);
			yy += AGame.BIGGER_FOUNT.lineHeight + MyDraw.UI_SPACING;
			if (cascade != null) {
				d.text(cascade, AGame.FOUNT, xx + width / 2 - (int) d.textSize(cascade, AGame.FOUNT).x / 2, yy);
				yy += AGame.FOUNT.lineHeight + MyDraw.UI_SPACING;
			}
			d.button(xx + width / 2 - d.bw(_t("OK")) / 2, yy, d.bw(_t("OK")), _t("OK"), new Runnable() {
				@Override
				public void run() {
					acknowledgeDeclarationOfWar();
				}
			});
			if (dn.forcedInfo != null) {
				String scoreT = (dn.forcedScore > 0 ? "+" : "") + dn.forcedScore;
				Pt sz = d.textSize(scoreT, AGame.BIG_FOUNT);
				d.text(scoreT, AGame.BIG_FOUNT, xx + width - (int) sz.x, yy + MyDraw.BUTTON_H - (int) sz.y);
				d.tooltip(xx + width - (int) sz.x, yy + MyDraw.BUTTON_H - (int) sz.y, (int) sz.x, (int) sz.y, dn.forcedInfo);
			}
		} else if (askForScrap != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.confirmDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("Scrap_x_for_y_gold", askForScrap.getName(), (askForScrap.getCost() / 8)) + (askForScrap.medals.isEmpty() ? "" : "\n\n" + _t("medal_loss_warning")), new Runnable() {
				@Override
				public void run() {
					doScrap();
				}
			}, new Runnable() {
				@Override
				public void run() {
					askForScrap = null;
				}
			});
		} else if (askForScuttle != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.confirmDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("Discard_x", askForScuttle.getName()) + (askForScuttle.medals.isEmpty() ? "" : "\n\n" + _t("medal_loss_warning")), new Runnable() {
				@Override
				public void run() {
					doScuttle();
				}
			}, new Runnable() {
				@Override
				public void run() {
					askForScuttle = null;
				}
			});
		} else if (w.player != null && medalEditor != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			d.drawWindow(sm.width / 2 - medalEditor.getWidth(d) / 2 - MyDraw.WINDOW_INSET, sm.height / 2 - medalEditor.getHeight(d) / 2 - MyDraw.WINDOW_INSET, medalEditor.getWidth(d) + MyDraw.WINDOW_INSET * 2, medalEditor.getHeight(d) + MyDraw.WINDOW_INSET * 2, 12);
			medalEditor.draw(d, sm.width / 2 - medalEditor.getWidth(d) / 2, sm.height / 2 - medalEditor.getHeight(d) / 2);
		} else if (w.player != null && awardMedalTo != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			EmpireMedalLevelRenderer emlr = new EmpireMedalLevelRenderer(this, awardMedalTo);
			int wh = MyDraw.WINDOW_INSET * 2 + Math.max(AGame.BIGGER_FOUNT.lineHeight, MyDraw.BUTTON_H) + MyDraw.UI_SPACING + MyDraw.PANEL_INSET * 2;
			int ww = (int) d.textSize(_t("award_medal_to_x", awardMedalTo.getName()), AGame.BIGGER_FOUNT).x + MyDraw.UI_SPACING + d.bw(_t("Cancel"));
			for (Integer lvl : awardMedalToLevels) {
				wh += emlr.getHeight(lvl, d, 10000);
				ww = Math.max(ww, emlr.getWidth(lvl, d) + ScrollBar.SCROLL_BAR_W);
			}
			wh = Math.min(sm.height - MyDraw.SIDE_CLEARANCE * 2, wh);
			ww += MyDraw.WINDOW_INSET * 2;
			int wx = sm.width / 2 - ww / 2;
			int wy = sm.height / 2 - wh / 2;
			d.drawWindow(wx, wy, ww, wh, 11);
			wx += MyDraw.WINDOW_INSET;
			wy += MyDraw.WINDOW_INSET;
			ww -= MyDraw.WINDOW_INSET * 2;
			
			d.text(MyDraw.TITLE_C + _t("award_medal_to_x", awardMedalTo.getName()), AGame.BIGGER_FOUNT, wx, wy);
			d.button(wx + ww - d.bw(_t("Cancel")), wy, d.bw(_t("Cancel")), _t("Cancel"), new Runnable() {
				@Override
				public void run() {
					awardMedalTo = null;
				}
			});
			wy += Math.max(AGame.BIGGER_FOUNT.lineHeight, MyDraw.BUTTON_H) + MyDraw.UI_SPACING;
			wh -= MyDraw.WINDOW_INSET * 2 + Math.max(AGame.BIGGER_FOUNT.lineHeight, MyDraw.BUTTON_H) + MyDraw.UI_SPACING;
			medalsSB.drawNaked(d, wx, wy, ww, wh, awardMedalToLevels, emlr);
		} else if (w.player != null && !w.player.coronationMessageSeen && w.has(ConquestToggle.CORONATION) && w.player.isCoronationReady(w.map) && !w.player.isCoronating()) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			String text;
			Hero h = w.player.getCoronationEnablingHero(w.map);
			if (h != null) {
				text = _t("hero_coronation_ready_message", h.getName(), w.map.requiredCitiesForCoronation());
			} else {
				text = _t("coronation_ready_message", EmpireStat.CORONATION_REPUTATION.get(w.player.bonuses), w.map.requiredCitiesForCoronation());
			}
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, text, new Runnable() {
				@Override
				public void run() {
					w.player.coronationMessageSeen = true;
				}
			});
		} else if (w.player != null && !w.player.finalRitualMessageSeen && w.has(ConquestToggle.CORONATION) && w.player.isFinalRitualReady(w.map) && !w.player.isDoingFinalRitual()) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("final_ritual_ready_message"), new Runnable() {
				@Override
				public void run() {
					w.player.finalRitualMessageSeen = true;
				}
			});
		} else if (w.player != null && w.player.coronationFailedDueToLostCityCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			String msg = w.player == w.player.coronationFailedDueToLostCityEmpire
					? _t("my_coronationFailedDueToLostCityCity", w.player.coronationFailedDueToLostCityCity.getDisplayName())
					: _t("coronationFailedDueToLostCityCity", w.player.coronationFailedDueToLostCityCity.getDisplayName(), w.player.coronationFailedDueToLostCityEmpire.getName());
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, msg, new Runnable() {
				@Override
				public void run() {
					w.player.coronationFailedDueToLostCityCity = null;
				}
			});
		} else if (w.player != null && w.player.finalRitualFailedDueToLostCityCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			String msg = w.player == w.player.finalRitualFailedDueToLostCityEmpire
					? _t("my_finalRitualFailedDueToLostCityCity", w.player.finalRitualFailedDueToLostCityCity.getDisplayName())
					: _t("finalRitualFailedDueToLostCityCity", w.player.finalRitualFailedDueToLostCityCity.getDisplayName(), w.player.finalRitualFailedDueToLostCityEmpire.getName());
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, msg, new Runnable() {
				@Override
				public void run() {
					w.player.finalRitualFailedDueToLostCityCity = null;
				}
			});
		} else if (w.player != null && w.player.coronationFailedDueToNoLongerReadyCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			String msg = w.player.cities.contains(w.player.coronationFailedDueToNoLongerReadyCity)
					?  _t("my_coronationFailedDueToNoLongerReadyCity", w.player.coronationFailedDueToNoLongerReadyCity.getDisplayName())
					:  _t("coronationFailedDueToNoLongerReadyCity", w.player.coronationFailedDueToNoLongerReadyCity.getDisplayName(), w.map.owner(w.player.coronationFailedDueToNoLongerReadyCity).getName());
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, msg, new Runnable() {
				@Override
				public void run() {
					w.player.coronationFailedDueToNoLongerReadyCity = null;
				}
			});
		} else if (w.player != null && w.player.coronationIntentionallyCancelledCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("coronationIntentionallyCancelledCity", w.player.coronationIntentionallyCancelledCity.getDisplayName(), w.map.owner(w.player.coronationIntentionallyCancelledCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.coronationIntentionallyCancelledCity = null;
				}
			});
		} else if (w.player != null && w.player.finalRitualIntentionallyCancelledCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("finalRitualIntentionallyCancelledCity", w.player.finalRitualIntentionallyCancelledCity.getDisplayName(), w.map.owner(w.player.finalRitualIntentionallyCancelledCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.finalRitualIntentionallyCancelledCity = null;
				}
			});
		} else if (w.player != null && w.player.coronationWarningCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("coronationWarningCity", w.player.coronationWarningCity.getDisplayName(), w.map.owner(w.player.coronationWarningCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.coronationWarningCity = null;
				}
			});
		} else if (w.player != null && w.player.finalRitualWarningCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("finalRitualWarningCity", w.player.finalRitualWarningCity.getDisplayName(), w.map.owner(w.player.finalRitualWarningCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.finalRitualWarningCity = null;
				}
			});
		} else if (w.player != null && w.player.coronationStartedCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("coronationStartedCity", w.player.coronationStartedCity.getDisplayName(), w.map.owner(w.player.coronationStartedCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.coronationStartedCity = null;
				}
			});
		} else if (w.player != null && w.player.finalRitualStartedCity != null) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t("finalRitualStartedCity", w.player.finalRitualStartedCity.getDisplayName(), w.map.owner(w.player.finalRitualStartedCity).getName()), new Runnable() {
				@Override
				public void run() {
					w.player.finalRitualStartedCity = null;
				}
			});
		} else if (w.player != null && w.map.areRitualSitesVisible() && !w.player.ritualSitesVisibleMessageSeen) {
			d.state.clearGlowRects();
			d.getHooks().list.clear();
			int width = 300;
			ArrayList<City> l = new ArrayList<City>();
			for (City c : w.map.cities()) {
				if (c.isRitualSite) { l.add(c); }
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < l.size(); i++) {
				if (i == l.size() - 1 && l.size() > 1) {
					if (l.size() > 2) {
						sb.append(",");
					}
					sb.append(" ").append(_t("list_and"));
				} else if (i > 0) {
					sb.append(", ");
				}
				sb.append(l.get(i).getDisplayName());
			}
			String msgKey = EmpireStat.CAN_SEE_RITUAL_SITES.get(w.player.bonuses) ? "can_see_ritual_sites_message_cultist" : "can_see_ritual_sites_message_other";
			d.messageDialog(sm.width / 2 - width / 2, sm.height / 2 - 150, width, _t(msgKey, sb.toString()), new Runnable() {
				@Override
				public void run() {
					w.player.ritualSitesVisibleMessageSeen = true;
				}
			});
		}
		
		if (renameCity != null) {
			hs.list.clear();
			int width = Math.max(d.bw(_t("Rename")) + d.bw(_t("Cancel")) + MyDraw.BUTTON_SPACING, sm.width / 4) + MyDraw.PANEL_INSET * 2;
			int height = MyDraw.textFieldH() + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2;
			int xx = sm.width / 2 - width / 2;
			int yy = sm.height / 2 - height / 2;
			d.drawPanel(xx, yy, width, height, 11);
			xx += MyDraw.PANEL_INSET;
			yy += MyDraw.PANEL_INSET;
			width -= MyDraw.PANEL_INSET * 2;
			renameCityField.render(xx, yy, width, d);
			yy += MyDraw.textFieldH() + MyDraw.UI_SPACING;
			xx += width - d.bw(_t("Rename"));
			d.button(xx, yy, d.bw(_t("Rename")), _t("Rename"), null, new Runnable() {
				@Override
				public void run() {
					w.giveCommand(msg("renameCity").put("city", renameCity.id).put("name", renameCityField.getText()));
					renameCity = null;
				}
			}, canRenameCity());
			xx -= d.bw(_t("Cancel")) + MyDraw.BUTTON_SPACING;
			d.button(xx, yy, d.bw(_t("Cancel")), _t("Cancel"), new Runnable() {
				@Override
				public void run() {
					renameCity = null;
				}
			});
		}
		
		if (w.player == null) { shipNameDialog = null; }
		if (shipNameDialog != null) {
			hs.list.clear();
			d.state.glowRects.clear();
			shipNameDialog.render(d, sm, hs, cursor);
		}
		
		if (w.defeatedPlayersSeenIndex < w.defeatedPlayers.size() && !w.someoneWon()) {
			hs.list.clear();
			d.state.glowRects.clear();
			StringBuilder msg = new StringBuilder(_t("Lost_player_connections"));
			for (String dp : disconnectedPlayers) {
				msg.append("\n").append(dp);
			}
			d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, _t("x_has_been_defeated", w.defeatedPlayers.get(w.defeatedPlayersSeenIndex).name()), new Runnable() {
				@Override
				public void run() {
					w.defeatedPlayersSeenIndex++;
				}
			});
		}
		
		if (!disconnectedPlayers.isEmpty() && !w.someoneWon()) {
			hs.list.clear();
			d.state.glowRects.clear();
			StringBuilder msg = new StringBuilder(_t("Lost_player_connections"));
			for (String dp : disconnectedPlayers) {
				msg.append("\n").append(dp);
			}
			d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, msg.toString(), new Runnable() {
				@Override
				public void run() {
					disconnectedPlayers.clear();
				}
			});
		}
		
		if (checkLeave) {
			hs.list.clear();
			d.state.glowRects.clear();
			d.confirmDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, _t("mp_host_leave_warning"), new Runnable() {
				@Override
				public void run() {
					leave();
				}
			}, new Runnable() {
				@Override
				public void run() {
					checkLeave = false;
				}
			});
		}
		
		/*if (w.desyncPubliclyDetected()) {
			d.heavilyBorderedText(_t("desync_notice"), AGame.BIG_FOUNT, AGame.BIG_FOUNT_OUTLINE, Clr.BLACK, Clr.WHITE, MyDraw.SIDE_CLEARANCE, sm.height / 2, 100000);
		} else */
		if (showAutosaveNotice()) {
			d.heavilyBorderedText(_t("autosave_notice"), AGame.BIG_FOUNT, AGame.BIG_FOUNT_OUTLINE, Clr.BLACK, Clr.WHITE, MyDraw.SIDE_CLEARANCE, sm.height / 2, 100000);
		}/* else if (w.isMultiplayer() && w.isAboutToChecksum()) {
			d.heavilyBorderedText(_t("sync_check_notice"), AGame.BIG_FOUNT, AGame.BIG_FOUNT_OUTLINE, Clr.BLACK, Clr.WHITE, MyDraw.SIDE_CLEARANCE, sm.height / 2, 100000);
		}*/
		
		if (AGame.isDebug()) {
			d.borderedText("Input " + g.avgTickCost() + " Render " + g.avgRenderCost() + " TickInterval avg " + (g.client != null ? g.client.avgTickInterval() : 0) + " max " + (g.client != null ? g.client.maxRecentTickInterval(): 0) + " sincePing " + (g.client != null ? g.client.longestTimeBetweenTicksSincePingSent : 0) + " checksumT " + w.checksumTimeCost/* + " FPS " + g.targetFrameRate */, AGame.FOUNT, Clr.WHITE, Clr.BLACK, MyDraw.SIDE_CLEARANCE, sm.height - MyDraw.SIDE_CLEARANCE - AGame.FOUNT.height * 3);
		}
		
		if (debugShowEraMenu) {
			int y = 100;
			d.button(100, y, 200, _t("Cancel"), new Runnable() {
				@Override
				public void run() {
					debugShowEraMenu = false;
				}
			});
			y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			for (final EraModifier e : Loadable.all(EraModifier.class)) {
				d.button(100, y, 200, _t("bonus_" + e.name), new Runnable() {
					@Override
					public void run() {
						w.map.cheatEraModifierOverride = e;
						w.map.skipNextEraModifierPick = false;
						debugShowEraMenu = false;
					}
				});
				y += MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
			}
		}
		
		if (w.isMultiplayer()) {
			d.borderedText(g.isReconnecting() ? _t("Reconnecting_") : ("ping " + g.ping()) + (AGame.isDebug() ? (" network queue size = " + w.frameQueueSize() + ", world processing queue size = " + g.inQueueSize() + ", outgoing networking queue size = " + g.outQueueSize()) : ""), AGame.FOUNT, Clr.WHITE, Clr.BLACK, MyDraw.SIDE_CLEARANCE, sm.height - MyDraw.SIDE_CLEARANCE - AGame.FOUNT.height);
		
			if (g.inQueueSize() > 200 && w.map.age > 20000 && !tooSlowMessageShown) {
				showTooSlowMessage = true;
				tooSlowMessageShown = true;
				g.reportError("Too slow. Ping is " + g.ping() + " queue size is " + g.inQueueSize() + " map size is " + w.map.height.length + ".", null, null, false, true);
			}
			
			if (showTooSlowMessage) {
				hs.list.clear();
				d.state.glowRects.clear();
				d.messageDialog(sm.width / 2 - 200, sm.height / 2 - 150, 400, _t("mp_too_slow_warning"), new Runnable() {
					@Override
					public void run() {
						showTooSlowMessage = false;
					}
				});
			}
		}
		
		if (waitForOtherPlayers) {
			hs.list.clear();
			d.state.glowRects.clear();
			int tw = (int) d.textSize(_t("Waiting_for_other_players") + " (99)", AGame.FOUNT).x;
			String desc = _t("Waiting_for_other_players");
			int timeLeft = Server.clientRetainTime + EXTRA_WAIT_TIME - timeWaitedForOtherPlayers;
			if (timeLeft <= 60000) {
				desc += " (" + Math.abs(timeLeft / 1000) + ")";
			}
			desc += "\n";
			for (StrategicPlayerInfo spi : w.channelPlayers.values()) {
				desc += "\n";
				desc += spi.worldgenComplete ? MyDraw.SELECTED_C : "[]";
				desc += spi.name.replace("[", "").replace("]", "").replace("{", "").replace("}", "");
			}
			Pt sz = d.textSize(desc, AGame.FOUNT);
			tw = Math.max((int) sz.x, tw);
			d.drawPanel(sm.width / 2 - tw / 2 - MyDraw.PANEL_INSET, sm.height / 3 - MyDraw.PANEL_INSET, tw + MyDraw.PANEL_INSET * 2, (int) sz.y + MyDraw.PANEL_INSET * 2, 0);
			d.text(desc, AGame.FOUNT, sm.width / 2 - tw / 2, sm.height / 3);
		}
		
		PerfStats.mark("ss dialogs");
	}

	public static FileScreen.Backend savesList() {
		File f = new File(AGame.getGameDirectory(), "saves");
		f.mkdirs();
		return new JSONFileAndIODirBackend(f);
	}
	
	public void monkeySave() {
		System.out.println("Monkey save");
		if (saveIODir == null) {
			try {
				saveIODir = new IODirectory(new File(new File(AGame.getGameDirectory(), "saves"), "monkey" + (w.player == null ? ("-spectator-" + g.playerID()) : w.player.name) + ".json"), w.map.worldID);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		save();
	}
	
	public void monkeyOpen() {
		if (saveIODir == null) {
			System.out.println("Not saved");
			return;
		}
		System.out.println("Monkey open");
		OpenGameMission ogm = new OpenGameMission(g, this, false, null);
		ogm.openFile(saveIODir.dirF);
	}
	
	public void save() {
		if (saveIODir == null) {
			saveAs();
		} else {
			System.err.println("save-over");
			try {
				final JSONObject o = w.toJSON(saveIODir);
				saveIODir.registerWithoutVersion(new OutPipe.Writer() {
					@Override
					public JSONObject write(String writeID) {
						return o;
					}
				}, "world");
				saveIODir.write();
				savedIndicator = 3000;
			} catch (Exception e) {
				g.reportError(_t("Unable_to_save_game"), e, null, false);
			}
			menu = false;
		}
	}

	public void saveAs() {
		if (w.defeat() || w.victory()) { return; }
		FileScreen.Backend b = savesList();
		FileScreen fs = new FileScreen(
				b,
				new SaveGameMission(g, this, b),
				null,
				new DateInfo(b),
				"saves", 1, true);
		fs.backgroundTask = this;
		g.s = fs;
	}

	public void open(boolean returnToMainMenu) {
		FileScreen.Backend b = savesList();
		FileScreen fs = new FileScreen(
				b,
				new OpenGameMission(g, this, returnToMainMenu, b),
				new SaveHelperWidget(b),
				new DateInfo(b),
				"saves",
				1, true);
		fs.backgroundTask = this;
		g.s = fs;
	}
	
	private int noticePortraitSize() {
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL: return 60;
			case MEDIUM: return 120;
			case LARGE: return 180;
		}
		return 60;
	}
	
	private int smallNoticePortraitSize() {
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL: return 40;
			case MEDIUM: return 70;
			case LARGE: return 100;
		}
		return 70;
	}
	
	public void showHero(Hero h) {
		heroToShow = h;
		showHeroes = true;
		shipForHero = null;
		cityForHero = null;
	}
	
	public void selectHeroForCity(City c) {
		heroToShow = null;
		showHeroes = true;
		shipForHero = null;
		cityForHero = c;
	}
	
	public void selectHeroForShip(Airship s) {
		heroToShow = null;
		showHeroes = true;
		shipForHero = s;
		cityForHero = null;
	}
	
	@Override
	public String cannotSelectReason(Hero h) {
		if (h.injuredTime > 0) {
			return _t("hero_injured");
		}
		if (shipForHero != null && h.inEmpire != null) {
			if (h.onExpedition()) {
				return _t("captain_on_expedition");
			}
			if (h.getInShip() != null) {
				Fleet f = h.inEmpire.fleet(h.getInShip());
				if (f == null || f.location == null || !w.map.isFriendly(h.inEmpire, f.location, false || !(f.location instanceof City) || ((City) f.location).takeoverMethod != null)) {
					return _t("captain_in_unavailable_ship");
				}
			}
		}
		if (cityForHero != null && h.inEmpire != null) {
			if (h.isBusyInCity()) {
				return _t("governor_busy");
			}
		}
		return null;
	}
	
	@Override
	public void select(Hero h) {
		if (cityForHero != null) {
			w.giveCommand(msg("assignHeroToCity").put("empire", w.player.id).put("hero", h.id).put("city", cityForHero.id));
		} else if (shipForHero != null) {
			w.giveCommand(msg("assignHeroToShip").put("empire", w.player.id).put("hero", h.id).put("networkID", shipForHero.networkID));
		}
		
		showHeroes = false;
		cityForHero = null;
		shipForHero = null;
	}
	
	@Override
	public WorldMap map() { return w.map; }

	@Override
	public ArrayList<String> music() {
		return AGame.STRATEGIC_MUSIC;
	}
	
	@Override
	public String appearancePostfix() {
		return "DAY";
	}
	
	@Override
	public boolean alwaysUseAppearancePostfix() {
		return true;
	}
}
