package com.zarkonnen.airships;

import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_NEW;
import static com.zarkonnen.airships.Combat.COMBAT_AREA_W_OLD;
import com.zarkonnen.airships.Combat.Side;
import com.zarkonnen.airships.MyDraw.State;
import com.zarkonnen.catengine.Hook;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import com.zarkonnen.catengine.util.SpikeProfiler;
import com.zarkonnen.catengine.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class UniScreen implements Screen, CanMoveAround {
	public static final Img LEFT_ARROW = new Img("ui", 224, 512, 16, 16, false);
	public static final Img RIGHT_ARROW = new Img("ui", 112, 512, 16, 16, false);
	public static final Clr SELECT_RECT = new Clr(255, 255, 255, 90);
	
	public interface VisualLayer {
		public void tick(Input in, int ms, UniScreen us);
		public void draw(MyDraw d, UniScreen us, double cropX, double cropY, double cropW, double cropH);
	}
	public static abstract class Button {
		public abstract boolean visible(UniScreen us);
		public boolean enabled(UniScreen us) { return true; }
		public boolean isToggle() { return false; }
		public boolean selected(UniScreen us) { return false; }
		public abstract String text(UniScreen us);
		public String tooltip(UniScreen us) { return null; }
		public Img icon(UniScreen us) { return null; }
		public abstract void click(Input in, UniScreen us);
		public void tick(Input in, UniScreen us) {}
		public String hotkey(UniScreen us) { return null; }
		public boolean gold(UniScreen us) { return false; }
		public String highlightKey() { return null; }
		public void renderExtra(UniScreen us, MyDraw d, int x, int y, int w, int h) {}
		public String hotkeyID(UniScreen us) {
			return getClass().getSimpleName() + "_" + hotkey(us);
		}
	}
	public static abstract class ShipButton {
		public abstract boolean visible(Airship ship, Side side, UniScreen us);
		public boolean enabled(Airship ship, Side side, UniScreen us) { return true; }
		public boolean acceptsTool(Tool t) { return t == NAVIGATE; }
		public abstract String text(Airship ship, Side side, UniScreen us);
		public abstract void click(Input in, Airship ship, Side side, UniScreen us);
		public void tick(Input in, int ms, UniScreen us) {}
		public String tooltip(Airship ship, Side side, UniScreen us) { return null; }
		public String highlightKey() { return null; }
	}
	public interface InfoPanel {
		public void draw(MyDraw d, Pt cursor, ScreenMode sm, Hooks hs, UniScreen us);
		public void tick(Input in, int ms, UniScreen us);
		public boolean chatEnabled(UniScreen us);
		public boolean doScroll(UniScreen us, int scrollAmt, Pt cursor, ScreenMode sm);
		public boolean arrowKeysInUse(UniScreen us);
	}
	public static abstract class Tool {
		Pt prevDragPt = null;
		int pdtN = 5;
		public abstract void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us);
		public abstract boolean click(Input in, Pt click, ScreenMode sm, UniScreen us);
		public abstract boolean rightClick(Input in, Pt click, ScreenMode sm, UniScreen us);
		public abstract void tick(Input in, int ms, UniScreen us);
		public abstract String getLabel();
		public boolean allowMultiselect(UniScreen us) { return false; }
		public void dragComplete(UniScreen us, Input in, double srX, double srY, double endX, double endY) {}
		public boolean showDragRect(UniScreen us, double startX, double startY, double endX, double endY) { return false; }
		
		public void tick2(Input in, int ms, UniScreen us) {
			if (pdtN++ > 2) {
				prevDragPt = null;
			}
		}
		
		public boolean mouseDown(Input in, Pt click, ScreenMode sm, UniScreen us) {
			if (in.mouseDownButton() != 1) {
				pdtN = 0;
				if (prevDragPt != null) {
					us.scrollX += (click.x - prevDragPt.x) / us.zoom;
					us.scrollY += (click.y - prevDragPt.y) / us.zoom;
				}
				prevDragPt = click;
				us.hasScrolledWithDrag = true;
			}
			return false;
		}
	}
	public static final Tool NAVIGATE = new Tool() {
		@Override
		public void draw(MyDraw d, Pt cursor, ScreenMode sm, UniScreen us) {
			if (us.intent instanceof EditShipIntent) {
				((EditShipIntent) us.intent).mode.draw(d, cursor, sm, us);
			}
		}
		@Override
		public boolean click(Input in, Pt click, ScreenMode sm, UniScreen us) {
			if (allowMultiselect(us) && click.y > MyDraw.TOP_BAR_H && click.x > us.panel(EditInfoPanel.class).myWidth) {
				TechFilterPanel tfp = us.panel(TechFilterPanel.class);
				if (tfp.a.contains(click)) {
					return false;
				}
				((EditShipIntent) us.intent).mode.click(us, in, new Pt(us.screenToWorldX(click.x), us.screenToWorldY(click.y)));
				return true;
			}
			return false;
		}
		@Override
		public boolean rightClick(Input in, Pt click, ScreenMode sm, UniScreen us) {
			if (us.intent instanceof EditShipIntent) {
				EditShipIntent esi = (EditShipIntent) us.intent;
				EditMode em = esi.mode;
				Tool newTool = null;
				switch (em) {
					case ARMOUR:
						newTool = new PlaceArmourTool(us.panel(EditPalettePanel.class).lastArmourType);
						break;
					case DECALS:
						newTool = new PlaceDecalTool(null);
						break;
					case MODULES:
						newTool = new PlaceModuleTool(null, false);
						((PlaceModuleTool) newTool).ms = 10000;
						((PlaceModuleTool) newTool).ticksSinceRightClick = 10000;
						break;
					case PAINT:
						newTool = new PaintArmourTool(PaintType.values().get(0));
						break;
				}
				if (newTool != null && newTool.rightClick(in, click, sm, us)) {
					us.tool = newTool;
					return true;
				}
			}
			return false;
		}
		@Override public void tick(Input in, int ms, UniScreen us) {}
		@Override public String getLabel() { return ""; }
		
		@Override
		public boolean allowMultiselect(UniScreen us) {
			return us.intent instanceof EditShipIntent && ((EditShipIntent) us.intent).mode.allowSelect(us);
		}
		
		@Override
		public void dragComplete(UniScreen us, Input in, double srX, double srY, double endX, double endY) {
			if (allowMultiselect(us)) {
				((EditShipIntent) us.intent).mode.dragComplete(us, in, srX, srY, endX, endY);
			}
		}
		@Override
		public boolean showDragRect(UniScreen us, double startX, double startY, double endX, double endY) {
			return allowMultiselect(us) && ((EditShipIntent) us.intent).mode.showDragRect(us, startX, startY, endX, endY);
		}
	};
	public interface ShipOverlay {
		public boolean drawIfOffScreen();
		public void draw(MyDraw d, Airship ship, UniScreen us);
	}
	public interface ShipChrome {
		public void draw(MyDraw d, Pt cursor, Airship ship, Side side, int x, int y, int w, int h, ScreenMode sm, UniScreen us);
		public void tick(Input in, int ms, UniScreen us);
		public boolean textInputOccurring(UniScreen us);
	}
	public interface Intent {
		public void tick(Input in, int ms, UniScreen us);
		public boolean showOutside();
		public boolean showDecals();
		public boolean drawAsBlueprint();
		public boolean allowMultiSelect();
	}
	public interface ShipLayer {
		public SpritesheetBundle getBaseSSB();
		public SpritesheetBundle getBaseSSB2();
		public boolean doDraw(double scale);
		public void lockShader(SpritesheetBundle ssb, SpritesheetBundle ssb2, MyDraw d, double scale, Image[] light, float lightStrength, Color ambient, float ambientSaturation);
		public void draw(Airship ship, WeatherEffect we, int side, MyDraw d, boolean outside, double scale,
			double x, double y,
			int ms, boolean displayStatusIcons, boolean showDecals, CoatOfArms coa, CoatOfArms enemyCOA, boolean showHPBarsAndDoors,
			Image[] light, float lightStrength, Color ambient, float ambientSaturation, Clr ambientClr,
			SpritesheetBundle ssb, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs);
		public void unlockShader(Image[] light, double scale);
		public boolean drawEvenIfShipOutsideCropRect();
	}
	
	public int combatAreaW() {
		if (city != null) { return city.combatAreaW(); }
		if (combat != null) { return combat.combatAreaW(); }
		if (setupGround != null && setupGround.getX() > -COMBAT_AREA_W_NEW / 2 + 100) {
			return COMBAT_AREA_W_OLD;
		} else {
			return COMBAT_AREA_W_NEW;
		}
	}
	
	// One of these four options needs to be set.
	public WorldMap wm; public City city;
	public Combat combat; public Side mySide;
	
	public CampaignWorld cw;
	
	public LandFormation setupGround;
	public ArrayList<LandFormation> setupFloaters;
	public CombatBackgroundFlavor setupBG;
	public TimeOfDay setupTime;
	
	public Airship standaloneEditShip;
	
	public static Image[] lmCache;
	public Image[] lightingMap;
	
	public AirshipGame g;
	public boolean hideUI = false;
	public Airship followShip;
	
	public boolean attractMode;
	public boolean overrideCamera;
	public int attractTime;
	public int followIndex = 0;
	
	public int topBarScroll = 0;
	public int scrollRange = 0;
	
	// Multiselect
	public boolean mouseIsDown = false;
	public boolean selRectInited = false;
	public double selRectStartX;
	public double selRectStartY;
	public Tool selRectStartTool;
	
	public transient boolean forciblyPulled;
	
	public ArrayList<VisualLayer> lowerVisualLayers = new ArrayList<UniScreen.VisualLayer>();
	public ArrayList<VisualLayer> upperVisualLayers = new ArrayList<UniScreen.VisualLayer>();
	public ArrayList<Button> buttons = new ArrayList<UniScreen.Button>();
	public ArrayList<InfoPanel> infoPanels = new ArrayList<UniScreen.InfoPanel>();
	public ArrayList<InfoPanel> floats = new ArrayList<UniScreen.InfoPanel>();
	public Tool tool = NAVIGATE;
	public Intent intent;
	public ArrayList<ShipOverlay> shipOverlays = new ArrayList<UniScreen.ShipOverlay>();
	public ArrayList<ShipButton> shipButtons = new ArrayList<UniScreen.ShipButton>();
	public ArrayList<ShipChrome> shipChromes = new ArrayList<UniScreen.ShipChrome>();
	public ArrayList<InfoPanel> overlays = new ArrayList<UniScreen.InfoPanel>();
	public ConfirmDialog confirmDialog;
	public GenericProgress genericProgress;
	
	public int scrollX, scrollY, adjScrollX, adjScrollY;
	public int sscrollX, sscrollY;
	public double zoom = 1.0;
	public int time = 0;
	public double vibration = 0;
	
	public Airship selectedShip;
	public LandFormation selectedLF;
	public ArrayList<Airship> selectedShips = new ArrayList<Airship>();
	public TimeOfDay bakedTOD;
	
	public boolean allOutside = false;
	
	public static final int MUSIC_SWITCH_DELAY = 10000;
	
	// Analytics
	public boolean hasScrolledWithArrows;
	public boolean hasScrolledWithDrag;
		
	public boolean hasSetupFleet() {
		return intent instanceof MultiplayerSetupIntent;
	}
	
	public List<Airship> getSetupFleet() {
		if (intent instanceof MultiplayerSetupIntent) {
			MultiplayerSetupIntent mpsi = (MultiplayerSetupIntent) intent;
			return mpsi.getSetupShips();
		}
		return Collections.emptyList();
	}
	
	public TimeOfDay getTimeOfDay() {
		if (combat != null) { return combat.timeOfDay; }
		if (setupTime != null) { return setupTime; }
		if (intent instanceof EditShipIntent) {
			return TimeOfDay.ofName("DAY");
		}
		if (cw != null) {
			Season s = Season.getSeason(cw.map.age);
			ArrayList<TimeOfDay> tods = s.defaultTimeOfDay.get(cw.map.worldBonuses());
			if (!tods.isEmpty()) {
				if (city != null) {
					for (TimeOfDay tod : tods) {
						if (city.ground.landscapeType.timesOfDay.contains(tod)) { return tod; }
					}
				} else {
					return tods.get(0);
				}
			}
		}
		return TimeOfDay.ofName("DAY");
	}
	
	public TimeOfDay prevTOD;
	public double todMix;
	
	public void setTimeOfDay(TimeOfDay tod) {
		if (combat != null) {
			combat.timeOfDay = tod;
			//reloadComponents();
		}
		//prevTOD = null;
	}
	
	public float lightStrength(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return (float) (tod.lightStrength * todMix + prevTOD.lightStrength * (1 - todMix));
		}
		return tod.lightStrength;
	}
	public Color backdropAmbient(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return mix(prevTOD.backdropAmbient, tod.backdropAmbient, (float) todMix);
		}
		return tod.backdropAmbient;
	}
	public float backdropAmbientSaturation(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return (float) (tod.backdropAmbientSaturation * todMix + prevTOD.backdropAmbientSaturation * (1 - todMix));
		}
		return tod.backdropAmbientSaturation;
	}
	public Color ambient(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return mix(prevTOD.ambient, tod.ambient, (float) todMix);
		}
		return tod.ambient;
	}
	public float ambientSaturation(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return (float) (tod.ambientSaturation * todMix + prevTOD.ambientSaturation * (1 - todMix));
		}
		return tod.ambientSaturation;
	}
	public Clr ambientTint(TimeOfDay tod) {
		if (prevTOD != null && tod != prevTOD) {
			return prevTOD.ambientTint.mix(todMix, tod.ambientTint);
		}
		return tod.ambientTint;
	}
	
	public boolean isTimeMoving() {
		if (combat == null || !(intent instanceof CombatIntent)) {
			return true;
		}
		return !combat.combatFinished && combat.speed != CombatSpeed.STOP;
	}
	
	public boolean textInputOccurring() {
		if (g.mpChatOverlayActive) { return true; }
		int ipl = infoPanels.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			if (infoPanels.get(ipi).chatEnabled(this)) { return true; }
		}
		int ol = overlays.size();
		for (int oi = 0; oi < ol; oi++) {
			if (overlays.get(oi).chatEnabled(this)) { return true; }
		}
		int scl = shipChromes.size();
		for (int sci = 0; sci < scl; sci++) {
			if (shipChromes.get(sci).textInputOccurring(this)) { return true; }
		}
		return false;
	}

	public UniScreen(AirshipGame g, Intent intent) {
		this.g = g;
		this.intent = intent;
		reloadComponents();
	}
	
	public final void clearComponents() {
		lowerVisualLayers.clear();
		upperVisualLayers.clear();
		shipOverlays.clear();
		shipChromes.clear();
		shipButtons.clear();
		buttons.clear();
		infoPanels.clear();
		overlays.clear();
		floats.clear();
	}
	
	public final void reloadComponents() {
		clearComponents();
		// Add components here.
		lowerVisualLayers.add(new LightMapLayer());
		lowerVisualLayers.add(new SkyVisualLayer());
		lowerVisualLayers.add(new StarsVisualLayer());
		lowerVisualLayers.add(new CloudsVisualLayer());
		lowerVisualLayers.add(new BackdropVisualLayer());
		lowerVisualLayers.add(new FloatieVisualLayer());
		lowerVisualLayers.add(new BirdsVisualLayer());
		lowerVisualLayers.add(new SelectionVisualLayer());
		lowerVisualLayers.add(new AreaLayer());
		lowerVisualLayers.add(new WeatherVisualLayer());
		lowerVisualLayers.add(new FloaterTargetHeightVisualLayer());
		lowerVisualLayers.add(new LandFormationVisualLayer());
		lowerVisualLayers.add(new LandFormationExternalVisualLayer());
		//lowerVisualLayers.add(new LandFormationPathingVisualLayer());
		lowerVisualLayers.add(new MoveTargetVisualLayer());
		lowerVisualLayers.add(new EditorAttractBGLayer());
		upperVisualLayers.add(new WaterUpperVisualLayer());
		upperVisualLayers.add(new TethersVisualLayer());
		upperVisualLayers.add(new FragmentVisualLayer());
		upperVisualLayers.add(new TroopsVisualLayer());
		upperVisualLayers.add(new UnderConstructionLayer());
		upperVisualLayers.add(new BeamLayer());
		upperVisualLayers.add(new ShotVisualLayer());
		upperVisualLayers.add(new ParticleVisualLayer());
		upperVisualLayers.add(new UpperWeatherVisualLayer());
		upperVisualLayers.add(new LightHaloLayer());
		upperVisualLayers.add(new AIDebugLayer());
		//upperVisualLayers.add(new LegDebugLayer());
		//upperVisualLayers.add(new PathingLayer());
		//upperVisualLayers.add(new LightsDebugLayer());
		//upperVisualLayers.add(new PhysicsDebugLayer());
		shipOverlays.add(new PossiblePlacements());
		shipOverlays.add(new DisconnectionOverlay());
		shipOverlays.add(new ConnectorsOverlay());
		shipOverlays.add(new BlockingModulesOverlay());
		shipOverlays.add(new EditorOverlay());
		shipOverlays.add(new EditSelectionOverlay());
		shipOverlays.add(new ChallengeInfoHighlighter());
		//shipOverlays.add(new OutsidePathingOverlay());
		//shipOverlays.add(new SpringDebugOverlay());
		shipChromes.add(new FireOrdersChrome());
		shipChromes.add(new FireArcs());
		shipChromes.add(new BoardingStatusChrome());
		shipChromes.add(new ClickToSelectShipChrome());
		shipChromes.add(new ShipStatusChrome());
		shipChromes.add(new RenameShipChrome());
		shipChromes.add(new ShipTooltipChrome());
		shipChromes.add(new ManageShipCaptainChrome());
		//shipChromes.add(new CrewJobShipChrome());
		//shipChromes.add(new ModuleDebugChrome());
		//shipChromes.add(new JobBoard());
		//shipChromes.add(new TachoChrome());
		shipButtons.add(new MoveShipButton());
		shipButtons.add(new RenameShipInDefencesButton());
		shipButtons.add(new RemoveShipButton());
		shipButtons.add(new ScrapShipButton());
		shipButtons.add(new RepairShipButton());
		//shipButtons.add(new RefitFromListButton());
		shipButtons.add(new StartRefitFromDefencesButton());
		shipButtons.add(new DefenceReserveButton());
		shipButtons.add(new ReserveButton());
		//shipButtons.add(new PutIntoReserveButton());
		for (Spy.ShipSpyAction ssa : Spy.ShipSpyAction.values()) {
			shipButtons.add(new ShipSpyButton(ssa));
		}
		buttons.add(new HelpButton());
		buttons.add(new SettingsButton());
		buttons.add(new ReadyButton());
		buttons.add(new StartCombatButton());
		buttons.add(new DoRefitFromStrategicButton());
		buttons.add(new DoBuildFromStrategicButton());
		buttons.add(new FleeButton());
		buttons.add(new LeaveToStrategicScreenButton());
		buttons.add(new DamageToolToggle());
		buttons.add(new DestroyToolToggle());
		buttons.add(new RestockResourcesButton());
		buttons.add(new StopPlacingShipsButton());
		for (ShipType st : ShipType.values()) {
			buttons.add(new BuildFromDesignButton(st));
		}
		for (ShipType st : ShipType.values()) {
			buttons.add(new StartDesignFromDefencesButton(st));
		}
		buttons.add(new LeaveMPSetupButton());
		buttons.add(new FlipPlaceShipButton());
		buttons.add(new AddMultiplayerConstructionButton(ShipType.AIRSHIP));
		buttons.add(new AddMultiplayerConstructionButton(ShipType.LANDSHIP));
		buttons.add(new LeaveToMainMenuButton());
		buttons.add(new LeaveShipEditorButton());
		buttons.add(new TryAgainChallengeButton());
		//buttons.add(new StartChallengeCombatButton());
		buttons.add(new DoBuildFromDefencesButton());
		buttons.add(new DoRefitFromDefencesButton());
		buttons.add(new SaveCombatButton());
		buttons.add(new SaveMissionButton());
		for (ShipType st : ShipType.values()) {
			buttons.add(new AddConstructionButton(st));
		}
		buttons.add(new AddMonsterButton());
		buttons.add(new SaveDesignButton());
		buttons.add(new OpenDesignButton());
		buttons.add(new ClearDesignButton());
		buttons.add(new UndoRedoButton(true));
		buttons.add(new UndoRedoButton(false));
		buttons.add(new FlipShipButton());
		buttons.add(new FollowActionButton());
		buttons.add(new ZoomToFitButton());
		buttons.add(new ToggleShowOutsideButton());
		buttons.add(new ToggleAIControlButton());
		buttons.add(new MultiplayerSurrenderButton());
		buttons.add(new ReservePanelToggle());
		buttons.add(new LandscapeEditorToggle());
		buttons.add(new TechFilterToggle());
		buttons.add(new OverlaysToggle());
		buttons.add(new ReplaceButton());
		for (Spy.CitySpyAction csa : Spy.CitySpyAction.values()) {
			buttons.add(new CitySpyButton(csa));
		}
		for (int i = CombatSpeed.values().length - 1; i >= 0; i--) {
			buttons.add(new CombatSpeedButton(CombatSpeed.values()[i]));
		}
		infoPanels.add(new PingInfo());
		infoPanels.add(new ShoutOverlay());
		infoPanels.add(new MoveFloatersButtons());
		infoPanels.add(new WeatherStatusOverlay());
		infoPanels.add(new UnderConstructionChrome());
		infoPanels.add(new DirectControlPanel());
		infoPanels.add(new LobbyChat());
		infoPanels.add(new EditErrorsPanel());
		infoPanels.add(new EditInfoPanel());
		infoPanels.add(new EditPalettePanel());
		infoPanels.add(new OverlayChooserPanel());
		infoPanels.add(new TechFilterPanel());
		infoPanels.add(new ReservePanel());
		infoPanels.add(new LandscapeEditorPanel());
		infoPanels.add(new RaidPanel());
		//infoPanels.add(new ShipProfilePanel());
		infoPanels.add(new ModuleProfilePanel());
		infoPanels.add(new CameraControls());
		infoPanels.add(new CommandButtonsPanel());
		infoPanels.add(new PlaybackControlsPanel());
		infoPanels.add(new SpectateInfoPanel());
		infoPanels.add(new MultiplayerChat());
		infoPanels.add(new SingleCombatSettingsChooser());
		infoPanels.add(new RenameShipPanel());
		infoPanels.add(new MissionEditorPanel());
		infoPanels.add(new MissionInfoPanels());
		infoPanels.add(new PortraitMessagePanel());
		infoPanels.add(new NoticesPanel());
		infoPanels.add(new SpyActionResultDialog());
		infoPanels.add(new SelectShipCaptainPanel());
		infoPanels.add(new VictoryOrDefeatOverlay());
		//infoPanels.add(new LightTestOverlay());
		overlays.add(new CityNameOverlay());
		overlays.add(new CombatSoundEffects());
		floats.add(new SetupExplanationFloat());
		floats.add(new CombatSetupHelpPanel());
		floats.add(new FleetCostPanel());
		floats.add(new MultiplayerCombatInfoPanel());
		floats.add(new FlashFloat());
		floats.add(new ChallengeInfoFloat());
		floats.add(new AutoResolvePanel());
		floats.add(new SelectionGroupsFloat());
		floats.add(new CountdownFloat());
		floats.add(new DrawCountdownFloat());
		floats.add(new MPStatusFloat());
		floats.add(new UniscreenModLoadUI());
		//floats.add(new TroopsDebug());
	}
	
	public <T extends VisualLayer> T visualLayer(Class<T> c) {
		for (int i = 0; i < lowerVisualLayers.size(); i++) {
			VisualLayer vl = lowerVisualLayers.get(i);
			if (c.isInstance(vl)) {
				return (T) vl;
			}
		}
		for (int i = 0; i < upperVisualLayers.size(); i++) {
			VisualLayer vl = upperVisualLayers.get(i);
			if (c.isInstance(vl)) {
				return (T) vl;
			}
		}
		return null;
	}
	
	public <T extends InfoPanel> T panel(Class<T> c) {
		int ipl = infoPanels.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			InfoPanel ip = infoPanels.get(ipi);
			if (c.isInstance(ip)) {
				return (T) ip;
			}
		}
		return null;
	}
	
	public <T extends InfoPanel> T getFloat(Class<T> c) {
		int fls = floats.size();
		for (int ipi = 0; ipi < fls; ipi++) {
			InfoPanel fl = floats.get(ipi);
			if (c.isInstance(fl)) {
				return (T) fl;
			}
		}
		return null;
	}
	
	public <T extends InfoPanel> T overlay(Class<T> c) {
		int ovl = overlays.size();
		for (int ovi = 0; ovi < ovl; ovi++) {
			InfoPanel ov = overlays.get(ovi);
			if (c.isInstance(ov)) {
				return (T) ov;
			}
		}
		return null;
	}
	
	public <T extends ShipOverlay> T shipOverlay(Class<T> c) {
		int ipl = shipOverlays.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			ShipOverlay so = shipOverlays.get(ipi);
			if (c.isInstance(so)) {
				return (T) so;
			}
		}
		return null;
	}
	
	public <T extends InfoPanel> T findFloat(Class<T> c) {
		int ipl = floats.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			InfoPanel so = floats.get(ipi);
			if (c.isInstance(so)) {
				return (T) so;
			}
		}
		return null;
	}
	
	public <T extends Button> T button(Class<T> c) {
		int bl = buttons.size();
		for (int bi = 0; bi < bl; bi++) {
			Button b = buttons.get(bi);
			if (c.isInstance(b)) {
				return (T) b;
			}
		}
		return null;
	}
	
	private transient int moveAroundDx, moveAroundDy;
	
	@Override
	public void moveAround(int dx, int dy) {
		moveAroundDx = dx;
		moveAroundDy = dy;
	}
	
	@Override
	public void input(Input in, State drawState, Pt cursor, Pt click, int ms) {
		if (intent instanceof HasStrategicScreen && ((HasStrategicScreen) intent).getStrategicScreen().w.desyncPubliclyDetected()) {
			StrategicScreen ss = ((HasStrategicScreen) intent).getStrategicScreen();
			g.s = new ResumeScreen(g, ss.w, ss, g.s, g.playerID(), false, 0, ss.w.gameName);
			return;
			/*g.s = ((HasStrategicScreen) intent).getStrategicScreen();
			g.s.input(in, drawState, cursor, click, ms);
			return;*/
		}
		
		/*if (!(shipChromes.get(0) instanceof TachoChrome)) {
			shipChromes.add(0, new TachoChrome());
		}*/
		
		if (g.prevScreen != this || tool != selRectStartTool) {
			selRectInited = false;
			mouseIsDown = false;
			selRectStartTool = null;
		}
		
		TimeOfDay tod = getTimeOfDay();
		if (prevTOD == null) {
			prevTOD = tod;
			todMix = 0;
		}
		if (tod != prevTOD) {
			todMix += ms / 5000.0;
			if (todMix >= 1) {
				prevTOD = tod;
				todMix = 0;
			}
		}
		
		long start = System.currentTimeMillis();
		ScreenMode sm = in.mode();
		
		if (g.testRunner != null) {
			for (int bi = 0; bi < buttons.size(); bi++) {
				Button b = buttons.get(bi);
				if (b instanceof TestAutoClick && b.visible(this) && b.enabled(this)) {
					b.click(in, this);
					break;
				}
			}
		}
		
		if (time == 0 && zoom == 1.0) {
			zoom = sm.width / 800;
		}
		
		Pt mouseDown = in.mouseDown();
		if ((intent.allowMultiSelect() && mySide != null) || tool.allowMultiselect(this)) {
			if (tool == NAVIGATE || tool.allowMultiselect(this)) {
				if (!selRectInited) {
					if (in.mouseDownButton() == 1 && !(intent instanceof EditShipIntent && cursor.x < panel(EditInfoPanel.class).myWidth) && cursor.y > MyDraw.TOP_BAR_H) {
						selRectStartX = screenToWorldX(cursor.x);
						selRectStartY = screenToWorldY(cursor.y);
						mouseIsDown = false;
						selRectInited = true;
						selRectStartTool = tool;
					}
				} else if (in.mouseDownButton() > 1) {
					selRectInited = false;
					selRectStartTool = null;
					mouseIsDown = false;
				} else if (in.clicked() == null && mouseDown != null) {
					if (!mouseIsDown && in.mouseDownButton() == 1) {
						selRectStartX = screenToWorldX(mouseDown.x);
						selRectStartY = screenToWorldY(mouseDown.y);
					}
				} else if (in.clicked() == null && mouseDown == null && mouseIsDown && (StrictMath.abs(cursor.x - worldToScreenX(selRectStartX)) > 3 || StrictMath.abs(cursor.y - worldToScreenY(selRectStartY)) > 3)) {
					double srsX = (StrictMath.min(screenToWorldX(cursor.x), selRectStartX));
					double srsY = (StrictMath.min(screenToWorldY(cursor.y), selRectStartY));
					double sreX = (StrictMath.max(screenToWorldX(cursor.x), selRectStartX));
					double sreY = (StrictMath.max(screenToWorldY(cursor.y), selRectStartY));
					double srW = sreX - srsX;
					double srH = sreY - srsY;
					if (tool.allowMultiselect(this)) {
						tool.dragComplete(this, in, selRectStartX, selRectStartY, screenToWorldX(cursor.x), screenToWorldY(cursor.y));
					} else {
						ArrayList<Airship> ships = new ArrayList<Airship>();
						for (Airship s : mySide.ships) {
							if (ClickToSelectShipChrome.canSelect(s, this) && Rect2D.intersects(s.getX(), s.getY(), s.getBBWidth(), s.getBBHeight(), srsX, srsY, srW, srH)) {
								ships.add(s);
							}
						}
						if (!ships.isEmpty()) {
							if (ships.size() == 1) {
								selectedShips.clear();
								selectedShip = ships.get(0);
								selectedLF = null;
							} else {
								selectedShips.clear();
								selectedShips.addAll(ships);
								selectedShip = null;
								selectedLF = null;
							}
						}
					}
					selRectInited = false;
					selRectStartTool = null;
				}
				mouseIsDown = mouseDown != null;
			} else {
				selRectStartX = screenToWorldX(cursor.x);
				selRectStartY = screenToWorldY(cursor.y);
				mouseIsDown = false;
				selRectInited = false;
			}
		} else {
			selectedShips.clear();
		}
		
		if (combat != null && SimplePref.SCREEN_SHAKE.get() && combat.speed != CombatSpeed.STOP && combat.isTimeMoving()) {
			vibration = 0;
			for (Blast b : combat.blasts) {
				vibration += b.getVibrateContribution(this, sm);
			}
		} else {
			vibration = 0;
		}
		
		genericProgress = null;
		
		time += ms;
		
		intent.tick(in, ms, this);
		
		if (attractMode && (combat.isFinished() || combat.isBoringForAttractMode() || combat.time > 120000 || g.mouseMoved)) {
			MainMenu mm = new MainMenu(g, MainMenu.Submenu.MAIN);
			mm.idleTime = g.mouseMoved ? 0 : 15000;
			g.s = mm;
		}
		
		if (intent instanceof EditorAttractIntent && g.mouseMoved) {
			MainMenu mm = new MainMenu(g, MainMenu.Submenu.MAIN);
			mm.idleTime = g.mouseMoved ? 0 : 15000;
			g.s = mm;
		}
		
		if (attractMode) {
			attractTime += ms;
			if (followShip != null && !combat.sides.get(0).ships.contains(followShip) && !combat.sides.get(1).ships.contains(followShip)) {
				attractTime = 10000;
			}
			if (attractTime >= 8000) {
				attractTime = 0;
				if (AGame.ANIM_R.nextBoolean()) {
					button(FollowActionButton.class).follow = true;
					followShip = null;
				} else {
					Side s = combat.sides.get(0);
					int fi = followIndex;
					if (s.ships.size() <= fi) {
						fi -= s.ships.size();
						s = combat.sides.get(1);
					}
					if (s.ships.size() <= fi) {
						s = combat.sides.get(0);
						followIndex = 0;
						fi = 0;
					}
					if (!s.ships.isEmpty()) {
						button(FollowActionButton.class).follow = false;
						followShip = s.ships.get(followIndex++ % s.ships.size());
						zoom = AGame.ANIM_R.nextInt(2) + 2;
					}
				}
			}
		}
		
		if (!overrideCamera) {
			boolean arrowKeysInUse = false;
			for (InfoPanel ip : infoPanels) {
				arrowKeysInUse = arrowKeysInUse || ip.arrowKeysInUse(this);
			}
			for (InfoPanel ip : floats) {
				arrowKeysInUse = arrowKeysInUse || ip.arrowKeysInUse(this);
			}
			for (InfoPanel ip : overlays) {
				arrowKeysInUse = arrowKeysInUse || ip.arrowKeysInUse(this);
			}
			for (ShipChrome sc : shipChromes) {
				if (sc instanceof RenameShipChrome && ((RenameShipChrome) sc).renaming != null) {
					arrowKeysInUse = true;
				}
			}
			if (!arrowKeysInUse) {
				if (followShip != null) {
					if (in.keyDown("UP")) { sscrollY += (AirshipGame.scrollSpeed / zoom + 1); }
					if (in.keyDown("DOWN")) { sscrollY -= (AirshipGame.scrollSpeed / zoom + 1); }
					if (in.keyDown("LEFT")) { sscrollX += (AirshipGame.scrollSpeed / zoom + 1); }
					if (in.keyDown("RIGHT")) { sscrollX -= (AirshipGame.scrollSpeed / zoom + 1); }
					scrollX = (int) -(followShip.getX() + followShip.getBBWidth() / 2) + sscrollX;
					scrollY = (int) -(followShip.getY() + followShip.getBBHeight() / 2) + sscrollY;
				} else {
					if (moveAroundDy < 0 || in.keyDown("UP")) { scrollY += (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true; }
					if (moveAroundDy > 0 || in.keyDown("DOWN")) { scrollY -= (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true; }
					if (moveAroundDx < 0 || in.keyDown("LEFT")) { scrollX += (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true; }
					if (moveAroundDx > 0 || in.keyDown("RIGHT")) { scrollX -= (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true; }
					if ((in.mouseDown() == null || in.mouseDownButton() < 2) && SimplePref.SIDE_BUMP_TO_SCROLL.get()) {
						if (cursor.y < 2) { scrollY += (AirshipGame.scrollSpeed / zoom + 1); }
						if (cursor.y > in.mode().height - 2) { scrollY -= (AirshipGame.scrollSpeed / zoom + 1); }
						if (cursor.x < 2) { scrollX += (AirshipGame.scrollSpeed / zoom + 1); }
						if (cursor.x > in.mode().width - 2) { scrollX -= (AirshipGame.scrollSpeed / zoom + 1); }
					}
				}
			}
			/*if (in.keyDown("UP")) { ShipLayers.MaskDebug.tby--; }
			if (in.keyDown("DOWN")) { ShipLayers.MaskDebug.tby++; }
			if (in.keyDown("LEFT")) { ShipLayers.MaskDebug.tbx--; }
			if (in.keyDown("RIGHT")) { ShipLayers.MaskDebug.tbx++; }*/

			if (!textInputOccurring()) {
				if (Keys.checkDown(in, "zoom_in", "ADD", false)) {
					zoom = zoom * (1 + AirshipGame.zoomSpeed * 0.01);
				}
				if (Keys.checkDown(in, "zoom_out", "SUBTRACT", false)) {
					zoom = zoom / (1 + AirshipGame.zoomSpeed * 0.01);
				}
				if (!overrideCamera) {
					if (zoom > 16.1) {
						zoom = 16.1;
					}

					if (zoom < 0.29 * sm.width / 1536) { zoom = 0.29 * sm.width / 1536; }
				}
				if (followShip != null) {
					if (Keys.checkDown(in, "up", "W", false)) { sscrollY += (AirshipGame.scrollSpeed / zoom + 1); }
					if (Keys.checkDown(in, "down", "S", false)) { sscrollY -= (AirshipGame.scrollSpeed / zoom + 1); }
					if (Keys.checkDown(in, "left", "A", false)) { sscrollX += (AirshipGame.scrollSpeed / zoom + 1); }
					if (Keys.checkDown(in, "right", "D", false)) { sscrollX -= (AirshipGame.scrollSpeed / zoom + 1); }
					scrollX = (int) -(followShip.getX() + followShip.getBBWidth() / 2) + sscrollX;
					scrollY = (int) -(followShip.getY() + followShip.getBBHeight() / 2) + sscrollY;
				} else {
					if (Keys.checkDown(in, "up", "W", false)) { scrollY += (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true; }
					if (Keys.checkDown(in, "down", "S", false)) { scrollY -= (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true;}
					if (Keys.checkDown(in, "left", "A", false)) { scrollX += (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true;}
					if (Keys.checkDown(in, "right", "D", false)) { scrollX -= (AirshipGame.scrollSpeed / zoom + 1); hasScrolledWithArrows = true;}
				}
			}
			
			// Prevent losing edit ship.
			if (intent instanceof SingleShipIntent && time > 500 && SimplePref.SIDE_BUMP_TO_SCROLL.get()) {
				Airship s = ((SingleShipIntent) intent).getShip(this);
				if (s != null) {
					if (worldToScreenX(s.getX()) > sm.width - 50) {
						scrollX = (int) ((sm.width / 2 - 50) / zoom - s.getX()) / 2 + scrollX / 2;
					}
					if (worldToScreenX(s.getX() + s.getBBWidth()) < 350) {
						scrollX = (int) ((350 - sm.width / 2) / zoom - s.getX() - s.getBBWidth()) / 2 + scrollX / 2;
					}
					if (worldToScreenY(s.getY()) > sm.height - 50) {
						scrollY = (int) ((sm.height / 2 - 50) / zoom - s.getY()) / 2 + scrollY / 2;
					}
					if (worldToScreenY(s.getY() + s.getBBHeight()) < MyDraw.TOP_BAR_H + 50) {
						scrollY = (int) ((MyDraw.TOP_BAR_H + 50 - sm.height / 2) / zoom - s.getY() - s.getBBHeight()) / 2 + scrollY / 2;
					}
				}
			}
		}
		
		if (confirmDialog != null) {
			if (Keys.check(in, "ESCAPE")) {
				confirmDialog.cancel(this);
			} else if (Keys.check(in, "ENTER")) {
				confirmDialog.ok(this);
			}
			return;
		}
		
		if (genericProgress != null) {
			return;
		}
		
		int vll = lowerVisualLayers.size();
		for (int vli = 0; vli < vll; vli++) {
			lowerVisualLayers.get(vli).tick(in, ms, this);
		}
		
		vll = upperVisualLayers.size();
		for (int vli = 0; vli < vll; vli++) {
			upperVisualLayers.get(vli).tick(in, ms, this);
		}
		
		int ipl = infoPanels.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			infoPanels.get(ipi).tick(in, ms, this);
		}
		
		if (in.scrollAmount() != 0 && (cursor == null) || cursor.y > MyDraw.TOP_BAR_H) {
			boolean captured = false;
			for (int ipi = 0; ipi < ipl; ipi++) {
				if (infoPanels.get(ipi).doScroll(this, in.scrollAmount(), cursor, sm)) {
					captured = true;
					break;
				}
			}
			if (!captured && !overrideCamera && in.scrollAmount() != 0) {
				if (cursor != null) {
					final double targetX = screenToWorldX(cursor.x);
					final double targetY = screenToWorldY(cursor.y);
					zoom = zoom * StrictMath.pow((1 + AirshipGame.mouseWheelZoomSpeed * 0.0001), in.scrollAmount());
					if (!overrideCamera) {
						if (zoom > 16.1) {
							zoom = 16.1;
						}

						if (zoom < 0.29 * sm.width / 1536) { zoom = 0.29 * sm.width / 1536; }
					}
					scrollX = (int) (-targetX + (cursor.x - sm.width / 2) / zoom);
					scrollY = (int) (-targetY + (cursor.y - sm.height / 2) / zoom);
				} else {
					zoom = zoom * StrictMath.pow((1 + AirshipGame.mouseWheelZoomSpeed * 0.0001), in.scrollAmount());
					if (!overrideCamera) {
						if (zoom > 16.1) {
							zoom = 16.1;
						}

						if (zoom < 0.29 * sm.width / 1536) { zoom = 0.29 * sm.width / 1536; }
					}
				}
			}
		}
		
		if (in.scrollAmount() != 0 && cursor.y <= MyDraw.TOP_BAR_H) {
			topBarScroll += in.scrollAmount() / 2;
			topBarScroll = StrictMath.min(scrollRange, StrictMath.max(0, topBarScroll));
		}
		
		adjScrollX = scrollX + (int) (sm.width / 2 / zoom) + (int) (vibration * (AGame.ANIM_R.nextDouble() - 0.5));
		adjScrollY = scrollY + (int) (sm.height / 2 / zoom) + (int) (vibration * (AGame.ANIM_R.nextDouble() - 0.5));

		if (!overrideCamera && !(intent instanceof SingleShipIntent)) {
			while (screenToWorldX(0) <= -combatAreaW() / 2) {
				scrollX--;
				adjScrollX = scrollX + (int) (sm.width / 2 / zoom);
			}
			while (screenToWorldX(sm.width) >= combatAreaW() / 2) {
				scrollX++;
				adjScrollX = scrollX + (int) (sm.width / 2 / zoom);
			}
			while (screenToWorldY(0) <= AGame.GROUND_LEVEL - 3500) {
				scrollY--;
				adjScrollY = scrollY + (int) (sm.height / 2 / zoom);
			}
			while (screenToWorldY(sm.height) >= AGame.GROUND_LEVEL + 500) {
				scrollY++;
				adjScrollY = scrollY + (int) (sm.height / 2 / zoom);
			}
		}
		
		
		tool.tick(in, ms, this);
		tool.tick2(in, ms, this);
		
		if (mouseDown != null && mouseDown.y > MyDraw.TOP_BAR_H) {
			tool.mouseDown(in, mouseDown, sm, this);
		}

		if (click != null && click.y > MyDraw.TOP_BAR_H && drawState.canClick()) {
			if (in.clickButton() == 2) {
				if (tool.rightClick(in, click, sm, this)) {
					drawState.hasClicked();
				}
			} else {
				if (tool.click(in, click, sm, this)) {
					drawState.hasClicked();
				}
			}
		}
		
		int sbl = shipButtons.size();
		for (int sbi = 0; sbi < sbl; sbi++) {
			shipButtons.get(sbi).tick(in, ms, this);
		}
		
		int scl = shipChromes.size();
		for (int sci = 0; sci < scl; sci++) {
			shipChromes.get(sci).tick(in, ms, this);
		}
		
		int bl = buttons.size();
		for (int bi = 0; bi < bl; bi++) {
			Button b = buttons.get(bi);
			if (b.visible(this)) {
				b.tick(in, this);
			}
		}
		
		int oll = overlays.size();
		for (int oli = 0; oli < oll; oli++) {
			overlays.get(oli).tick(in, ms, this);
		}
		
		int fll = floats.size();
		for (int fli = 0; fli < fll; fli++) {
			floats.get(fli).tick(in, ms, this);
		}
		
		if (!textInputOccurring()) {
			for (int bi = 0; bi < bl; bi++) {
				Button b = buttons.get(bi);
				if (b.visible(this) && b.hotkey(this) != null && b.enabled(this) && Keys.check(in, b.hotkeyID(this), b.hotkey(this), false)) {
					b.click(in, this);
					break;
				}
			}
		}
		
		if (intent instanceof CombatIntent && ((CombatIntent) intent).perfRep != null) {
			((CombatIntent) intent).perfRep.countTickTime((int) (System.currentTimeMillis() - start));
		}
	}
	
	public double screenToWorldX(double x) { return (x / zoom) - adjScrollX; }
	public double screenToWorldY(double y) { return (y / zoom) - adjScrollY; }
	public double worldToScreenX(double x) { return (x + adjScrollX) * zoom; }
	public double worldToScreenY(double y) { return (y + adjScrollY) * zoom; }
	
	private void drawShipChrome(MyDraw d, Airship ship, Side side, ShipChrome sc, Pt cursor, ScreenMode sm) {
		int x = (int) ((ship.getIntX() + adjScrollX) * zoom);
		int y = (int) ((ship.getIntY() + adjScrollY) * zoom);
		int w = (int) (ship.getBBWidth() * zoom);
		int h = (int) (ship.getBBHeight() * zoom);
		sc.draw(d, cursor, ship, side, x, y, w, h, sm, this);
	}
	
	private void drawShipButton(MyDraw d, final Airship ship, final ShipButton sb, final Side side, int[] offsets) {
		if (hideUI) { return; }
		if (!sb.acceptsTool(tool)) { return; }
		if (!sb.visible(ship, side, this)) { return; }
		int x = (int) ((ship.getIntX() + adjScrollX) * zoom);
		int y = (int) ((ship.getIntY() + adjScrollY) * zoom);
		int w = (int) (ship.getBBWidth() * zoom);
		String text = sb.text(ship, side, this);
		int bw = d.bw(text);//(int) d.textSize(text, AGame.BIG_FOUNT).x + 20;//sb.text(ship, side, this).length() * (AGame.FOUNT.displayWidth) + 10;
		if (offsets[0] > 0 && offsets[0] + bw > w) {
			offsets[0] = 0;
			offsets[1] -= MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING;
		}
		final UniScreen us = this;
		//d.rect(MyDraw.WIN_SHADOW, x + offsets[0] - 1, y - MyDraw.BUTTON_H + offsets[1] - 1 - MyDraw.BUTTON_SPACING, bw + 2, MyDraw.BUTTON_H + 2);
		d.button(x + offsets[0], y - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING + offsets[1], bw, sb.text(ship, side, this), null, new InputRunnable() {
			@Override
			public void run(Input in) {
				sb.click(in, ship, side, us);
			}
		}, sb.enabled(ship, side, us));
		String tt = sb.tooltip(ship, side, this);
		if (tt != null) {
			d.tooltip(x + offsets[0], y - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING + offsets[1], bw, MyDraw.BUTTON_H, tt);
		}
		if (sb.highlightKey() != null) {
			d.highlight(sb.highlightKey(), g, x + offsets[0], y - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING + offsets[1], bw, MyDraw.BUTTON_H);
		}
		offsets[0] += bw + MyDraw.BUTTON_SPACING;
	}
	
	public static boolean isShipInCropRect(Airship ship, double cropX, double cropY, double cropW, double cropH) {
		double sx = ship.getX();
		double sy = ship.getY();
		double sRight = ship.getX() + ship.getBBWidth();
		double sBottom = ship.getY() + ship.getBBHeight();
		final int msz = ship.modules.size();
		for (int mi = 0; mi < msz; mi++) {
			Module m = ship.modules.get(mi);
			double mx = ship.getX() + ship.gridXToWorldX(m.x, m.type.getW()) * AGame.SGS;
			double my = ship.getY() + m.y * AGame.SGS + m.type.getH() * AGame.SGS;
			final int ll = m.legs.size();
			for (int li = 0; li < ll; li++) {
				Leg l = m.legs.get(li);
				if (l.spec.upperLeg == null) { continue; }
				double legW = l.spec.upperLeg.srcHeight;
				
				double hipX =
					ship.flipped
					? mx + (m.type.getW() - l.spec.xOffset) * AGame.SGS
					: mx + l.spec.xOffset * AGame.SGS;
				double hipY = my + l.spec.yOffset * AGame.SGS;
				
				double kneeX = hipX + StrictMath.cos(l.upperRotation) * l.spec.upperLimbLength;
				double kneeY = hipY + StrictMath.sin(l.upperRotation) * l.spec.upperLimbLength;
				sx = StrictMath.min(kneeX - legW / 2, sx);
				sRight = StrictMath.max(kneeX + legW / 2, sRight);
				sy = StrictMath.min(kneeY - legW / 2, sy);
				sBottom = StrictMath.max(kneeY + legW / 2, sBottom);
				
				double footX = kneeX + StrictMath.cos(l.lowerRotation) * l.spec.lowerLimbLength;
				double footY = kneeY + StrictMath.sin(l.lowerRotation) * l.spec.lowerLimbLength;
				sx = StrictMath.min(footX - legW / 2, sx);
				sRight = StrictMath.max(footX + legW / 2, sRight);
				sy = StrictMath.min(footY - legW / 2, sy);
				sBottom = StrictMath.max(footY + l.spec.footHeight + legW / 2, sBottom);
			}
		}
		return Rect2D.intersects(cropX, cropY, cropW, cropH, sx, sy, sRight - sx, sBottom - sy);
	}
	
	private final HashSet<SpritesheetBundle> additionalSSBs = new HashSet<SpritesheetBundle>();
	private final HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs = new HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>>();
	
	public CoatOfArms getBestOverallCOA() {
		if (combat != null) {
			return mySide.arms;
		}
		if (city != null) {
			return wm.owner(city).arms;
		}
		if (hasSetupFleet()) {
			return g.getBestCOA();
		}
		if (standaloneEditShip != null) {
			if (intent instanceof EditShipIntent) {
				return ((EditShipIntent) intent).getArms(this);
			} else if (intent instanceof EditorAttractIntent) {
				return ((EditorAttractIntent) intent).coa;
			}
		}
		return g.getBestCOA();
	}
	
	private void updateFlagImages(MyDraw d) {
		CoatOfArms coa0 = null;
		CoatOfArms coa1 = null;
		if (combat != null) {
			coa0 = combat.sides.get(0).arms;
			coa1 = combat.sides.get(1).arms;
		} else if (city != null) {
			coa0 = wm.owner(city).arms;
			coa1 = coa0;
		} else if (hasSetupFleet()) {
			coa0 = g.getBestCOA();
		} else if (standaloneEditShip != null) {
			if (intent instanceof EditShipIntent) {
				coa0 = ((EditShipIntent) intent).getArms(this);
			} else if (intent instanceof EditorAttractIntent) {
				coa0 = ((EditorAttractIntent) intent).coa;
			} else {
				coa0 = g.getBestCOA();
			}
			coa1 = coa0;
		}
		ShipLayers.Flags.updateFlags(d, coa0, coa1);
	}
	
	private void renderLayer(ShipLayer shipLayer, List<Airship> setupFleet, SpritesheetBundle ssb, MyDraw d, TimeOfDay tod, double cropX, double cropY, double cropW, double cropH, HashSet<SpritesheetBundle> additionalSSBs, SpritesheetBundle ssb2, HashSet<Utils.Pair<SpritesheetBundle, SpritesheetBundle>> additionalSSBPairs) {
		if (!shipLayer.doDraw(zoom)) { return; }
		boolean showHpBarsAndDoors = zoom >= 1.25 && !hideUI;
		shipLayer.lockShader(ssb, ssb2, d, zoom, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod));
		Airship ignore = (intent instanceof SingleShipIntent) ? ((SingleShipIntent) intent).getIgnoredOriginalShip(this) : null;
		if (combat != null) {
			int sl = combat.sides.size();
			for (int si = 0; si < sl; si++) {
				Side side = combat.sides.get(si);
				Side otherSide = combat.otherSide(side);
				int al = side.ships.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = side.ships.get(ai);
					if (ship == ignore) { continue; }
					if (!shipLayer.drawEvenIfShipOutsideCropRect() && !isShipInCropRect(ship, cropX, cropY, cropW, cropH)) { continue; }
					boolean inside =
							side == mySide
							? ship.generatesCommandPoints()
							: !ship.boarders.isEmpty();
					if (ship.boarders.isEmpty() && intent instanceof CombatIntent && !((CombatIntent) intent).isShipPlayerControlled(this, ship)) {
						inside = false;
					}
					if (intent instanceof PlaybackIntent) {
						inside = true;
					}
					if (allOutside) {
						inside = false;
					}
					shipLayer.draw(ship, tod.effect, si, d, !inside, zoom, ship.getIntX(), ship.getIntY(), time, true, intent.showDecals() || allOutside, ship.originalArms == null ? side.arms : ship.originalArms, otherSide.arms, showHpBarsAndDoors, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
				}
			}
		} else if (city != null) {
			CoatOfArms ownerCOA = wm.owner(city).arms;
			Fleet garrison = wm.getGarrison(city);
			for (Airship ship : city.getDefences()) {
				if (ship == ignore) { continue; }
				if (!isShipInCropRect(ship, cropX, cropY, cropW, cropH)) { continue; }
				shipLayer.draw(ship, tod.effect, 1, d, allOutside || intent.showOutside(), zoom, ship.getIntX(), ship.getIntY(), time, true, intent.showDecals() || allOutside, ownerCOA, ownerCOA, showHpBarsAndDoors, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
			}
			if (garrison != null) {
				int al = garrison.actives.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = garrison.actives.get(ai);
					if (ship == ignore) { continue; }
					if (!isShipInCropRect(ship, cropX, cropY, cropW, cropH)) { continue; }
					shipLayer.draw(ship, tod.effect, 1, d, allOutside || intent.showOutside(), zoom, ship.getIntX(), ship.getIntY(), time, true, intent.showDecals() || allOutside, ownerCOA, ownerCOA, showHpBarsAndDoors, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
				}
			}
		} else if (hasSetupFleet()) {
			CoatOfArms coa = g.getBestCOA();
			int al = setupFleet.size();
			for (int ai = 0; ai < al; ai++) {
				Airship ship = setupFleet.get(ai);
				if (ship == ignore) { continue; }
				if (!isShipInCropRect(ship, cropX, cropY, cropW, cropH)) { continue; }
				//shipLayer.draw(ship, tod.effect, 0, d, allOutside || intent.showOutside(), zoom, ship.getIntX(), ship.getIntY(), time, true, intent.showDecals() || allOutside, coa, coa, showHpBarsAndDoors, lightingMap, us.lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
				boolean inside = true;
				if (intent instanceof CombatIntent && !((CombatIntent) intent).isShipPlayerControlled(this, ship)) {
					inside = false;
				}
				shipLayer.draw(ship, tod.effect, 0, d, allOutside || intent.showOutside() || !inside, zoom, ship.getIntX(), ship.getIntY(), time, true, intent.showDecals(), coa, coa, showHpBarsAndDoors, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
			}
		}
		if (standaloneEditShip != null) {
			if (standaloneEditShip == ignore) { System.out.println("ignore standalone edit ship???"); }
			CoatOfArms coa;
			if (intent instanceof EditShipIntent) {
				coa = ((EditShipIntent) intent).getArms(this);
			} else if (intent instanceof EditorAttractIntent) {
				coa = ((EditorAttractIntent) intent).coa;
			} else {
				coa = g.getBestCOA();
			}
			shipLayer.draw(standaloneEditShip, tod.effect, 0, d, intent.showOutside(), zoom, standaloneEditShip.getIntX(), standaloneEditShip.getIntY(), time, true, intent.showDecals(), coa, coa, showHpBarsAndDoors, lightingMap, lightStrength(tod), ambient(tod), ambientSaturation(tod), ambientTint(tod), ssb, additionalSSBs, ssb2, additionalSSBPairs);
		}
		shipLayer.unlockShader(lightingMap, zoom);
	}
	
	@Override
	public void render(MyDraw d, ScreenMode sm, Hooks hs, Pt cursor) {
		//#SpikeProfiler.active = false;
		//#SpikeProfiler.outputDir = new File("spikes").getAbsoluteFile();
		//#SpikeProfiler.start("uniscreen");
		long start = System.currentTimeMillis();
		PerfStats.start();
		updateFlagImages(d);
		if (lightingMap != null && Appearance.useLighting && !Appearance.useSimpleGraphics) {
			if (lightingMap[0].getWidth() != sm.width / AGame.LIGHTMAP_DOWNSCALE ||
				lightingMap[0].getHeight() != sm.height / AGame.LIGHTMAP_DOWNSCALE)
			{
				for (Image lm : lightingMap) {
					try {
						lm.destroy();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				lightingMap = null;
				lmCache = null;
			}
		}
		if (lightingMap == null && Appearance.useLighting && !Appearance.useSimpleGraphics) {
			try {
				if (lmCache == null) {
					lmCache = new Image[4];
					for (int i = 0; i < 4; i++) {
						lmCache[i] = new Image(sm.width / AGame.LIGHTMAP_DOWNSCALE, sm.height / AGame.LIGHTMAP_DOWNSCALE);
					}
				}
				lightingMap = lmCache;
			} catch (SlickException e) {
				throw new RuntimeException(e);
			}
		}
		if (lightingMap != null && (!Appearance.useLighting || Appearance.useSimpleGraphics)) {
			lightingMap = null;
		}
		PerfStats.mark("us lightMaps");
		
		if (hideUI) {
			d.state.setCursor(null, null);
		} else {
			d.state.resetCursor();
		}
		//#SpikeProfiler.start("sky and zoom");
		if (prevTOD != null && prevTOD != getTimeOfDay()) {
			d.rect(prevTOD.skyColors[4].mix(todMix, getTimeOfDay().skyColors[4]), 0, 0, sm.width, sm.height);
		} else {
			d.rect(getTimeOfDay().skyColors[4], 0, 0, sm.width, sm.height);
		}
		d.scale(zoom, zoom);
		d.shift(adjScrollX, adjScrollY);
		//#SpikeProfiler.end("sky and zoom");
		
		double cropX = screenToWorldX(0);
		double cropY = screenToWorldY(0);
		double cropW = screenToWorldX(sm.width) - cropX;
		double cropH = screenToWorldY(sm.height) - cropY;
		
		PerfStats.mark("us bg");
		
		List<Airship> setupFleet = getSetupFleet();
		int vll = lowerVisualLayers.size();
		for (int vli = 0; vli < vll; vli++) {
			//#SpikeProfiler.start(lowerVisualLayers.get(vli).getClass().getSimpleName());
			lowerVisualLayers.get(vli).draw(d, this, cropX, cropY, cropW, cropH);
			PerfStats.mark("us " + lowerVisualLayers.get(vli).getClass().getSimpleName());
			//#SpikeProfiler.end(lowerVisualLayers.get(vli).getClass().getSimpleName());
		}
		
		TimeOfDay tod = getTimeOfDay();
		for (ShipLayer shipLayer : ShipLayers.ALL) {
			//#SpikeProfiler.start(shipLayer.getClass().getSimpleName());
			additionalSSBs.clear();
			additionalSSBPairs.clear();
			if (shipLayer.getBaseSSB2() == null) {
				renderLayer(shipLayer, setupFleet, shipLayer.getBaseSSB(), d, tod, cropX, cropY, cropW, cropH, additionalSSBs, null, null);
				for (SpritesheetBundle ssb : additionalSSBs) {
					renderLayer(shipLayer, setupFleet, ssb, d, tod, cropX, cropY, cropW, cropH, null, null, null);
				}
			} else {
				renderLayer(shipLayer, setupFleet, shipLayer.getBaseSSB(), d, tod, cropX, cropY, cropW, cropH, null, shipLayer.getBaseSSB2(), additionalSSBPairs);
				for (Utils.Pair<SpritesheetBundle, SpritesheetBundle> ssbp : additionalSSBPairs) {
					renderLayer(shipLayer, setupFleet, ssbp.a, d, tod, cropX, cropY, cropW, cropH, null, ssbp.b, null);
				}
			}
			if (RotatingShader.shaderLocked) {
				g.reportError("Rotating shader still locked after " + shipLayer.getClass().getSimpleName(), null, null, false, true);
				RotatingShader.unlockShader();
			}
			PerfStats.mark("us " + shipLayer.getClass().getSimpleName());
			//#SpikeProfiler.end(shipLayer.getClass().getSimpleName());
		}
				
		Airship ignore = (intent instanceof SingleShipIntent) ? ((SingleShipIntent) intent).getIgnoredOriginalShip(this) : null;
		// Ship overlays
		for (ShipOverlay so : shipOverlays) {
			//#SpikeProfiler.start(so.getClass().getSimpleName());
			if (combat != null) {
				int sl = combat.sides.size();
				for (int si = 0; si < sl; si++) {
					Side side = combat.sides.get(si);
					int al = side.ships.size();
					for (int ai = 0; ai < al; ai++) {
						Airship ship = side.ships.get(ai);
						if (ship == ignore) { continue; }
						if (!so.drawIfOffScreen() && !Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
						so.draw(d, ship, this);
					}
				}
			} else if (city != null) {
				Fleet garrison = wm.getGarrison(city);
				for (Airship ship : city.getDefences()) {
					if (ship == ignore) { continue; }
					if (!so.drawIfOffScreen() && !Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					so.draw(d, ship, this);
				}
				if (garrison != null) {
					int al = garrison.actives.size();
					for (int ai = 0; ai < al; ai++) {
						Airship ship = garrison.actives.get(ai);
						if (ship == ignore) { continue; }
						if (!so.drawIfOffScreen() && !Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
						so.draw(d, ship, this);
					}
				}
			} else if (hasSetupFleet()) {
				int al = setupFleet.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = setupFleet.get(ai);
					if (ship == ignore) { continue; }
					if (!so.drawIfOffScreen() && !Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					so.draw(d, ship, this);
				}
			}
			if (standaloneEditShip != null) {
				so.draw(d, standaloneEditShip, this);
			}
			//#SpikeProfiler.end(so.getClass().getSimpleName());
			PerfStats.mark("us " + so.getClass().getSimpleName());
		}
		
		vll = upperVisualLayers.size();
		for (int vli = 0; vli < vll; vli++) {
			//#SpikeProfiler.start(upperVisualLayers.get(vli).getClass().getSimpleName());
			upperVisualLayers.get(vli).draw(d, this, cropX, cropY, cropW, cropH);
			if (RotatingShader.shaderLocked) {
				g.reportError("Rotating shader still locked after " + upperVisualLayers.get(vli).getClass().getSimpleName(), null, null, false, true);
				RotatingShader.unlockShader();
			}
			PerfStats.mark("us " + upperVisualLayers.get(vli).getClass().getSimpleName());
			//#SpikeProfiler.end(upperVisualLayers.get(vli).getClass().getSimpleName());
		}
		
		d.resetTransforms();
		
		int scl = shipChromes.size();
		for (int sci = 0; sci < scl; sci++) {
			//#SpikeProfiler.start(shipChromes.get(sci).getClass().getSimpleName());
			ShipChrome sc = shipChromes.get(sci);
			if (combat != null) {
				int sl = combat.sides.size();
				for (int si = 0; si < sl; si++) {
					Side side = combat.sides.get(si);
					int al = side.ships.size();
					for (int ai = 0; ai < al; ai++) {
						Airship ship = side.ships.get(ai);
						if (ship == ignore) { continue; }
						if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
						drawShipChrome(d, ship, side, sc, cursor, sm);
					}
				}
			} else if (city != null) {
				Fleet garrison = wm.getGarrison(city);
				for (Airship ship : city.getDefences()) {
					if (ship == ignore) { continue; }
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					drawShipChrome(d, ship, null, sc, cursor, sm);
				}
				if (garrison != null) {
					int al = garrison.actives.size();
					for (int ai = 0; ai < al; ai++) {
						Airship ship = garrison.actives.get(ai);
						if (ship == ignore) { continue; }
						if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
						drawShipChrome(d, ship, null, sc, cursor, sm);
					}
				}
			} else if (hasSetupFleet()) {
				int al = setupFleet.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = setupFleet.get(ai);
					if (ship == ignore) { continue; }
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					drawShipChrome(d, ship, null, sc, cursor, sm);
				}
			}
			if (standaloneEditShip != null) {
				drawShipChrome(d, standaloneEditShip, null, sc, cursor, sm);
			}
			PerfStats.mark("us " + sc.getClass().getSimpleName());
			//#SpikeProfiler.end(shipChromes.get(sci).getClass().getSimpleName());
		}
		
		//#SpikeProfiler.start("shipButtons");
		int[] offsets = {0, 0};
		if (combat != null) {
			int sl = combat.sides.size();
			for (int si = 0; si < sl; si++) {
				Side side = combat.sides.get(si);
				int al = side.ships.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = side.ships.get(ai);
					if (ship == ignore) { continue; }
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					offsets[0] = 0; offsets[1] = 0;
					int sbl = shipButtons.size();
					for (int sbi = 0; sbi < sbl; sbi++) {
						drawShipButton(d, ship, shipButtons.get(sbi), side, offsets);
					}
				}
			}
		} else if (city != null) {
			Fleet garrison = wm.getGarrison(city);
			for (Airship ship : city.getDefences()) {
				if (ship == ignore) { continue; }
				if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
				offsets[0] = 0; offsets[1] = 0;
				int sbl = shipButtons.size();
				for (int sbi = 0; sbi < sbl; sbi++) {
					drawShipButton(d, ship, shipButtons.get(sbi), null, offsets);
				}
			}
			if (garrison != null) {
				int al = garrison.actives.size();
				for (int ai = 0; ai < al; ai++) {
					Airship ship = garrison.actives.get(ai);
					if (ship == ignore) { continue; }
					if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
					offsets[0] = 0; offsets[1] = 0;
					int sbl = shipButtons.size();
					for (int sbi = 0; sbi < sbl; sbi++) {
						drawShipButton(d, ship, shipButtons.get(sbi), null, offsets);
					}
				}
			}
		} else if (hasSetupFleet()) {
			int al = setupFleet.size();
			for (int ai = 0; ai < al; ai++) {
				Airship ship = setupFleet.get(ai);
				if (ship == ignore) { continue; }
				if (!Rect2D.intersects(cropX, cropY, cropW, cropH, ship.getX(), ship.getY(), ship.getBBWidth(), ship.getBBHeight())) { continue; }
				offsets[0] = 0; offsets[1] = 0;
				int sbl = shipButtons.size();
				for (int sbi = 0; sbi < sbl; sbi++) {
					drawShipButton(d, ship, shipButtons.get(sbi), null, offsets);
				}
			}
		}
		if (standaloneEditShip != null) {
			offsets[0] = 0; offsets[1] = 0;
			int sbl = shipButtons.size();
			for (int sbi = 0; sbi < sbl; sbi++) {
				drawShipButton(d, standaloneEditShip, shipButtons.get(sbi), null, offsets);
			}
		}
		PerfStats.mark("us shipButtons");
		//#SpikeProfiler.end("shipButtons");
		
		//#SpikeProfiler.start("tool");
		tool.draw(d, cursor, sm, this);
		//#SpikeProfiler.end("tool");
		PerfStats.mark("us tool " + tool.getClass().getSimpleName());
		
		int ipl = infoPanels.size();
		for (int ipi = 0; ipi < ipl; ipi++) {
			//#SpikeProfiler.start(infoPanels.get(ipi).getClass().getSimpleName());
			infoPanels.get(ipi).draw(d, cursor, sm, hs, this);
			//#SpikeProfiler.end(infoPanels.get(ipi).getClass().getSimpleName());
			PerfStats.mark("us " + infoPanels.get(ipi).getClass().getSimpleName());
		}
		
		d.rect(new Clr(255, 0, 0, 1), 1, 1, 1, 1); // qqDPS reset attempt
		
		if (mouseIsDown && (intent.allowMultiSelect() || tool.showDragRect(this, selRectStartX, selRectStartY, screenToWorldX(cursor.x), screenToWorldY(cursor.y))) && selRectInited && (StrictMath.abs(screenToWorldX(cursor.x) - selRectStartX) > 3 || StrictMath.abs(screenToWorldY(cursor.y) - selRectStartY) > 3)) {
			d.state.setCursor("SELECT", null);
			//d.rect(SELECT_RECT, -10, -10, 1, 1); // qqDPS gfx reset
			d.rect(SELECT_RECT, worldToScreenX(selRectStartX), worldToScreenY(selRectStartY), cursor.x - worldToScreenX(selRectStartX), cursor.y - worldToScreenY(selRectStartY));
		}
		
		final UniScreen us = this;
		if (!hideUI) {
			//#SpikeProfiler.start("topBar");
			d.drawTopBar(sm);
			boolean chat = textInputOccurring();
			int bsz = buttons.size();
			
			int totalButtonWidth = 0;
			for (int bi = 0; bi < bsz; bi++) {
				final Button b = buttons.get(bi);
				if (!b.visible(this)) { continue; }
				Img icon = b.icon(this);
				if (icon != null) {
					totalButtonWidth += MyDraw.BUTTON_H;
				} else {
					totalButtonWidth += (int) d.textSize(b.text(this), AGame.BIG_FOUNT).x + (b.isToggle() ? MyDraw.TOGGLE_EXTRA_W : MyDraw.BUTTON_EXTRA_W);
				}
				totalButtonWidth += MyDraw.BUTTON_SPACING;
			}
			totalButtonWidth -= MyDraw.BUTTON_SPACING;
			
			int x = 
					totalButtonWidth > sm.width - MyDraw.BUTTON_SPACING * 2 - MyDraw.BUTTON_H * 2
					? sm.width - MyDraw.SIDE_CLEARANCE - MyDraw.BUTTON_SPACING - MyDraw.BUTTON_H + topBarScroll
					: sm.width - MyDraw.SIDE_CLEARANCE + topBarScroll;
			
			if (totalButtonWidth > sm.width - MyDraw.SIDE_CLEARANCE * 2) {
				d.restrictHooks(MyDraw.SIDE_CLEARANCE + MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING, 0, sm.width - MyDraw.SIDE_CLEARANCE * 2 - MyDraw.BUTTON_H * 2 - MyDraw.BUTTON_SPACING * 2, sm.height);
			}
			
			for (int bi = 0; bi < bsz; bi++) {
				final Button b = buttons.get(bi);
				if (!b.visible(this)) { continue; }
				Img icon = b.icon(this);		
				int bw;
				if (icon != null) {
					bw = MyDraw.ICON_BUTTON_SZ;
					x -= bw;
					if (b.isToggle()) {
						d.iconToggle(x, MyDraw.TOP_BAR_INSET, icon, new InputRunnable() {
							@Override
							public void run(Input in) {
								b.click(in, us);
							}
						}, b.selected(this), b.enabled(this));
					} else {
						d.iconButton(x, MyDraw.TOP_BAR_INSET, icon, new InputRunnable() {
							@Override
							public void run(Input in) {
								b.click(in, us);
							}
						}, b.enabled(this));
					}
					String tt = b.tooltip(this);
					if (tt != null) {
						d.tooltip(x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H, tt);
					}
					b.renderExtra(us, d, x, MyDraw.TOP_BAR_INSET, MyDraw.BUTTON_H, MyDraw.BUTTON_H);
					x -= MyDraw.BUTTON_SPACING;
				} else {
					String text = b.text(this);
					bw = (int) d.textSize(text, AGame.BIG_FOUNT).x + (b.isToggle() ? MyDraw.TOGGLE_EXTRA_W : MyDraw.BUTTON_EXTRA_W);
					x -= bw;
					if (b.isToggle()) {
						d.toggle(x, MyDraw.TOP_BAR_INSET, bw, text, chat ? null : Keys.getText(b.hotkeyID(us), b.hotkey(us), false), new InputRunnable() {
							@Override
							public void run(Input in) {
								b.click(in, us);
							}
						}, b.selected(this), b.enabled(this));
					} else {
						if (b.gold(us)) {
							d.goldbutton(x, MyDraw.TOP_BAR_INSET, bw, text, chat ? null : Keys.getText(b.hotkeyID(us), b.hotkey(us), false), new InputRunnable() {
								@Override
								public void run(Input in) {
									b.click(in, us);
								}
							}, b.enabled(this));
						} else {
							d.button(x, MyDraw.TOP_BAR_INSET, bw, text, chat ? null : Keys.getText(b.hotkeyID(us), b.hotkey(us), false), new InputRunnable() {
								@Override
								public void run(Input in) {
									b.click(in, us);
								}
							}, b.enabled(this));
						}
						if (b.highlightKey() != null) {
							d.highlight(b.highlightKey(), g, x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H);
						}
					}
					String tt = b.tooltip(this);
					if (tt != null) {
						d.tooltip(x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H, tt);
					}
					b.renderExtra(us, d, x, MyDraw.TOP_BAR_INSET, bw, MyDraw.BUTTON_H);
					x -= MyDraw.BUTTON_SPACING;
				}
			}
			
			d.rect(new Clr(255, 0, 0, 1), 1, 1, 1, 1); // qqDPS reset attempt
			
			d.clearHookRestriction();
						
			if (totalButtonWidth > sm.width - MyDraw.SIDE_CLEARANCE * 2) {
				d.hook(sm.width - MyDraw.SIDE_CLEARANCE - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING, MyDraw.SIDE_CLEARANCE, MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING, MyDraw.BUTTON_H + 2, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN, Hook.Type.HOVER) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						// Do nothing.
					}
				});
				d.state.removeIntersectingGlowRects(sm.width - MyDraw.SIDE_CLEARANCE - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING, MyDraw.TOP_BAR_INSET, MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING, MyDraw.BUTTON_H);
				d.drawWoodGrain(
						sm.width - MyDraw.SIDE_CLEARANCE - MyDraw.BUTTON_H - MyDraw.BUTTON_SPACING,
						MyDraw.TOP_BAR_INSET - (MyDraw.BUTTON_START.srcHeight - MyDraw.BUTTON_H) / 2 - 1,
						MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING,
						MyDraw.BUTTON_START.srcHeight + 2);
				
				d.repeatingIconButton(sm.width - MyDraw.SIDE_CLEARANCE - MyDraw.BUTTON_H, MyDraw.TOP_BAR_INSET, RIGHT_ARROW, new Runnable() {
					@Override
					public void run() {
						topBarScroll -= 5;
					}
				}, topBarScroll > 0);
				
				d.hook(0, MyDraw.SIDE_CLEARANCE, MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING, MyDraw.BUTTON_H + 2, new Hook(Hook.Type.MOUSE_1_CLICKED, Hook.Type.MOUSE_1_DOWN, Hook.Type.HOVER) {
					@Override
					public void run(Input in, Pt p, Hook.Type type) {
						// Do nothing.
					}
				});
				d.drawWoodGrain(
						0,
						MyDraw.TOP_BAR_INSET - (MyDraw.BUTTON_START.srcHeight - MyDraw.BUTTON_H) / 2 - 1,
						MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING,
						MyDraw.BUTTON_START.srcHeight + 2);
				
				d.repeatingIconButton(MyDraw.SIDE_CLEARANCE, MyDraw.TOP_BAR_INSET, LEFT_ARROW, new Runnable() {
					@Override
					public void run() {
						topBarScroll += 5;
					}
				}, x < MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING);
				
				scrollRange = StrictMath.max(0, topBarScroll - x + MyDraw.SIDE_CLEARANCE + MyDraw.BUTTON_H + MyDraw.BUTTON_SPACING);
				
				if (x > scrollRange) {
					topBarScroll = scrollRange;
				}
			}
			//#SpikeProfiler.end("topBar");
		}
		PerfStats.mark("us topBar");
				
		int os = overlays.size();
		for (int oi = 0; oi < os; oi++) {
			//#SpikeProfiler.start(overlays.get(oi).getClass().getSimpleName());
			overlays.get(oi).draw(d, cursor, sm, hs, this);
			//#SpikeProfiler.end(overlays.get(oi).getClass().getSimpleName());
			PerfStats.mark("us " + overlays.get(oi).getClass().getSimpleName());
		}
		
		int fll = floats.size();
		for (int fli = 0; fli < fll; fli++) {
			//#SpikeProfiler.start(floats.get(fli).getClass().getSimpleName());
			floats.get(fli).draw(d, cursor, sm, hs, this);
			//#SpikeProfiler.end(floats.get(fli).getClass().getSimpleName());
			PerfStats.mark("us " + floats.get(fli).getClass().getSimpleName());
		}
		
		if (attractMode || intent instanceof EditorAttractIntent) {
			d.state.clearGlowRects();
			hs.list.clear();
			d.getHooks().list.clear();
			d.hook(0, 0, sm.width, sm.height, new Hook(Hook.Type.MOUSE_1_CLICKED) {
				@Override
				public void run(Input in, Pt p, Hook.Type type) {
					g.s = new MainMenu(g, MainMenu.Submenu.MAIN);
				}
			});
		}
		
		if (genericProgress != null) {
			d.state.clearGlowRects();
			hs.list.clear();
			int w = 300 + MyDraw.WINDOW_INSET * 2;
			int h = AGame.FOUNT.height + MyDraw.UI_SPACING + MyDraw.PROGRESS_BAR_H + MyDraw.WINDOW_INSET * 2;
			int x = sm.width / 2 - w / 2;
			int y = sm.height / 2 - h / 2;
			d.drawShadowedPanel(x, y, w, h);
			x += MyDraw.WINDOW_INSET;
			y += MyDraw.WINDOW_INSET;
			d.text(genericProgress.desc, AGame.FOUNT, x, y);
			y += AGame.FOUNT.height + MyDraw.UI_SPACING;
			d.progressBar(x, y, 300, 1.0 * genericProgress.progress / StrictMath.max(1, genericProgress.total));
		}
		
		if (confirmDialog != null) {
			d.state.clearGlowRects();
			hs.list.clear();
			d.getHooks().list.clear();
			d.rect(AirshipGame.ERR_BG_TINT, 0, 0, sm.width, sm.height);
			int dialogW = StrictMath.max(400, sm.width / 2);
			if (confirmDialog instanceof MessageDialog) {
				try {
					d.messageDialog(sm.width / 2 - dialogW / 2, sm.height / 2 - 150, dialogW, confirmDialog.text(), new Runnable() {
						@Override
						public void run() {
							confirmDialog.ok(us);
						}
					});
				} catch (Exception e) {
					g.reportError("{0} problem with " + confirmDialog.text(), e, null, false, true);
				}
			} else if (confirmDialog.noText() != null) {
				d.yesNoCancelDialog(sm.width / 2 - dialogW / 2, sm.height / 2 - 150, dialogW, confirmDialog.text(), confirmDialog.okText(), confirmDialog.noText(), confirmDialog.cancelText(), new Runnable() {
					@Override
					public void run() {
						confirmDialog.ok(us);
					}
				}, new Runnable() {
					@Override
					public void run() {
						confirmDialog.no(us);
					}
				}, new Runnable() {
					@Override
					public void run() {
						confirmDialog.cancel(us);
					}
				});
			} else {
				d.confirmDialog(sm.width / 2 - dialogW / 2, sm.height / 2 - 150, dialogW, confirmDialog.text(), confirmDialog.okText(), confirmDialog.cancelText(), new Runnable() {
					@Override
					public void run() {
						confirmDialog.ok(us);
					}
				}, new Runnable() {
					@Override
					public void run() {
						confirmDialog.cancel(us);
					}
				});
			}
		}
		
		if (intent instanceof CombatIntent && ((CombatIntent) intent).perfRep != null) {
			((CombatIntent) intent).perfRep.countRenderTime((int) (System.currentTimeMillis() - start));
		}
		
		if (hideUI) {
			d.state.cursorAppearance = null;
		}
		
		PerfStats.mark("us dialogs");
		
		//#SpikeProfiler.end("uniscreen");
		//String outN = //#SpikeProfiler.frameDone("uniscreen", 200);
		/*if (outN != null && us.combat != null && us.combat.lastState != null) {
			try {
				FileUtils.write(new File(//#SpikeProfiler.outputDir, outN + ".json"), us.combat.lastState.toString(4));
			} catch (Exception e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().exit(0);
		}*/
	}
	
	public static Color mix(Color a, Color b, float mix) {
		return new Color(a.r * (1 - mix) + b.r * mix, a.g * (1 - mix) + b.g * mix, a.b * (1 - mix) + b.b * mix);
	}
	
	public LandFormation ground() {
		if (combat != null) {
			return combat.landFormations.get(0);
		} else if (city != null) {
			return city.ground;
		} else if (setupGround != null) {
			return setupGround;
		}
		return null;
	}
	
	public LandscapeType landscapeType() {
		if (combat != null) {
			return combat.landFormations.get(0).landscapeType;
		} else if (city != null) {
			return city.ground.landscapeType;
		} else if (setupGround != null) {
			return setupGround.landscapeType;
		}
		return LandscapeType.ofName("GRASSLAND");
	}
	
	@Override
	public ArrayList<String> music() {
		if (intent instanceof CampaignCombatSetupIntent || intent instanceof MultiplayerCampaignCombatSetupIntent) {
			return null;
		}
		if (combat != null && combat.time > 0) {
			return MusicAffinity.pickMusic(combat);
		}
		if (time < MUSIC_SWITCH_DELAY) {
			return null;
		}
		if (intent instanceof EditShipIntent) {
			return AGame.EDITOR_MUSIC;
		}
		if (intent instanceof DefencesIntent || intent instanceof EspionageIntent || intent instanceof ViewCityIntent) {
			return AGame.CITY_MUSIC;
		}
		return AGame.STRATEGIC_MUSIC;
	}
	
	@Override
	public String appearancePostfix() {
		if (intent instanceof MultiplayerCampaignCombatIntent || intent instanceof MultiplayerCampaignCombatSetupIntent || intent instanceof MultiplayerCombatIntent || intent instanceof MultiplayerSetupIntent
				|| (intent instanceof HasStrategicScreen && ((HasStrategicScreen) intent).getStrategicScreen().w.isMultiplayer())
				|| (cw != null && cw.isMultiplayer()))
		{
			return "DAY";
		}
		return getTimeOfDay().appearancePostfix;
	}
	
	@Override
	public boolean alwaysUseAppearancePostfix() { return false; }
	
	private String cleanHotkey(String hk) {
		if ("SLASH".equals(hk)) { return "/"; }
		return hk;
	}
}
