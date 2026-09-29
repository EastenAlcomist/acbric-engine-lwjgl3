package com.zarkonnen.airships;

import com.zarkonnen.airships.HeroType.Stat;
import com.zarkonnen.airships.HeroType.Stat.Changer;
import com.zarkonnen.catengine.Img;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.Client.msg;
import com.zarkonnen.airships.HeroType.CombatAbility;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;

public class HeroRenderer implements ScrollBar.ScrollElementAdapter<Hero> {
	public final Img techIcon = new Img("ui", 560, 384, 16, 16, false);
	public final Img bonusIcon = new Img("ui", 272, 512, 16, 16, false);
	public final Img leaveIcon = new Img("heroes", 0, 0, 16, 16, false);
	public final Img dieIcon = new Img("heroes", 32, 0, 16, 16, false);
	public final Img winIcon = new Img("heroes", 0, 48, 16, 16, false);
	public final Img crownIcon = new Img("ui", 128, 384, 16, 16, false);
	public final Img evolveIcon = new Img("heroes", 16, 0, 16, 16, false);
	public final Img leaveIconBg = new Img("heroes", 0, 16, 16, 16, false);
	public final Img dieIconBg = new Img("heroes", 32, 16, 16, 16, false);
	public final Img evolveIconBg = new Img("heroes", 16, 16, 16, 16, false);
	public final Img winIconBg = new Img("heroes", 16, 48, 16, 16, false);
	public final Img crownIconBg = new Img("heroes", 64, 0, 16, 16, false);
	public final Img disloyalIcon = new Img("heroes", 48, 80, 16, 16, false);
	public final Img disloyalIconBg = new Img("heroes", 64, 80, 16, 16, false);
	public final Img loseIcon = new Img("heroes", 160, 32, 16, 16, false);
	public final Img loseIconBg = new Img("heroes", 176, 32, 16, 16, false);
	
	public final Clr loseClr = Clr.fromHex("bb421d");
	
	public final Img clearIcon = new Img("ui", 144, 416, 16, 16, false);
	
	public final CampaignWorld cw;
	public final HeroSelectionCallback hsc;
	public final SelectShipCaptainPanel sscp;
	public final StrategicScreen ss;

	public HeroRenderer(CampaignWorld w, StrategicScreen ss) {
		this.cw = w;
		this.hsc = null;
		this.sscp = null;
		this.ss = ss;
	}
	
	public HeroRenderer(HeroSelectionCallback hsc, StrategicScreen ss) {
		this.cw = null;
		this.hsc = hsc;
		this.sscp = null;
		this.ss = ss;
	}
	
	public HeroRenderer(SelectShipCaptainPanel sscp) {
		this.cw = null;
		this.hsc = null;
		this.sscp = sscp;
		this.ss = null;
	}
	
	public int getWidth(MyDraw d) {
		return 
				Math.max(
						portraitSize() + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING + (int) d.textSize("-100 from the Gentle Takeover of a City plus squirrel", AGame.FOUNT).x,
						(int) d.textSize("Mene mene tekel upharsin", AGame.BIG_FOUNT).x
				) + MyDraw.PANEL_INSET * 2;
	}
	
	private int portraitSize() {
		switch (AirshipGame.instance.currentGUIScale) {
			case SMALL: return 100;
			case MEDIUM: return 200;
			case LARGE: return 300;
		}
		return 100;
	}
	
	private String hireAndDismissInfo(Hero hero) {
		if (cw != null) {
			if (hero.hired) {
				String dismissInfo = Hero.getStatChangeAppendix(cw.player, HeroEvent.heroLeft(cw.player, hero.type), cw.map, false);
				if (!dismissInfo.isEmpty()) {
					return _t("if_dismissed_") + dismissInfo;
				}
			} else {
				String hireInfo = Hero.getStatChangeAppendix(cw.player, HeroEvent.heroHired(cw.player, hero.type), cw.map, false);
				if (!hireInfo.isEmpty()) {
					return _t("if_hired_") + hireInfo;
				}
			}
		}
		return "";
	}

	@Override
	public int getHeight(Hero hero, MyDraw d, int availableWidth) {
		availableWidth -= MyDraw.PANEL_INSET * 2;
		int infoH = Math.max(16 + MyDraw.BUTTON_SPACING, AGame.FOUNT.lineHeight) * (1 + (hero.type.canDismiss ? 0 : 1) + hero.type.combatAbilities.size() + hero.type.techs.size() + hero.type.edicts.size() + hero.type.instabuild.size() + (sscp != null || hero.type.bonus == null ? 0 : 1) + (sscp != null || hero.type.departureBonus == null ? 0 : 1) + hero.type.clearableNests.size() + hero.type.moveableNests.size()) +
						MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING + (hasActives(hero) ? MyDraw.UI_SPACING : 0);
		int passives = numPassives(hero);
		infoH += passives * AGame.FOUNT.lineHeight;
		if (passives != 0) { infoH += MyDraw.UI_SPACING; }
		if (sscp == null) {
			infoH += (MyDraw.PROGRESS_BAR_H + MyDraw.BUTTON_SPACING * 2) * hero.type.stats.size();
			for (Stat s : hero.type.stats) {
				infoH += AGame.FOUNT.lineHeight * s.changers.size();
			}
		}
		String hireAndDismissInfo = hireAndDismissInfo(hero);
		if (!hireAndDismissInfo.isEmpty()) {
			infoH += d.textSize(hireAndDismissInfo, AGame.FOUNT, 0, 0, availableWidth - portraitSize() - MyDraw.PANEL_BORDER_W * 2 - MyDraw.UI_SPACING).height;
		}
		return MyDraw.PANEL_INSET * 2 + AGame.BIG_FOUNT.lineHeight + (int) d.textSize(hero.getDesc(), AGame.FOUNT, 0, 0, availableWidth).height + MyDraw.UI_SPACING +
				Math.max(
					portraitSize() + MyDraw.PANEL_BORDER_W * 2,
					infoH
				) + MyDraw.BUTTON_SPACING + MyDraw.BUTTON_H + MyDraw.SCROLL_EL_SPACING;
	}
	
	private boolean hasActives(Hero hero) {
		return !hero.type.edicts.isEmpty() || !hero.type.instabuild.isEmpty() || !hero.type.combatAbilities.isEmpty() || !hero.type.clearableNests.isEmpty() || !hero.type.moveableNests.isEmpty() || !hero.type.techs.isEmpty() || hero.type.bonus != null || hero.type.departureBonus != null;
	}
	
	private int numPassives(Hero hero) {
		int passives = 0;
		if (hero.injuredTime > 0) { passives++; }
		if (hero.nemesisEmpire != null) { passives++; }
		if (hero.homeCity != null) { passives++; }
		if (hero.type.unrest != 0) { passives++; }
		if (hero.type.spyDefence != 0) { passives++; }
		if (hero.type.productionPercent != 0) { passives++; }
		if (hero.type.defenceBudget != 0) { passives++; }
		if (hero.type.incomePercent != 0) { passives++; }
		if (hero.type.research != 0) { passives++; }
		if (hero.type.researchPercent != 0) { passives++; }
		if (hero.type.upgradeCostPercent != 0) { passives++; }
		if (hero.type.upgradeSpeedPercent != 0) { passives++; }
		if (hero.type.airshipSpeedPercent != 0) { passives++; }
		if (hero.type.landshipSpeedPercent != 0) { passives++; }
		if (hero.type.buildingSpeedPercent != 0) { passives++; }
		if (hero.type.fleetSpeedPercent != 0) { passives++; }
		if (hero.type.expeditionStrengthPercent != 0) { passives++; }
		if (hero.type.shipBonus != null) { passives++; }
		if (hero.type.fireRatePercent != 0) { passives++; }
		if (hero.type.accuracyPercent != 0) { passives++; }
		if (hero.type.crewSpeedPercent != 0) { passives++; }
		if (hero.type.flammabilityPercent != 0) { passives++; }
		if (hero.type.explosionRiskPercent != 0) { passives++; }
		if (hero.type.commandCooldownPercent != 0) { passives++; }
		if (hero.type.repairAmountPercent != 0) { passives++; }
		if (hero.type.firefightAmountPercent != 0) { passives++; }
		if (hero.type.propulsionPercent != 0) { passives++; }
		if (hero.type.liftPercent != 0) { passives++; }
		if (hero.type.armourRepairPercent != 0) { passives++; }
		if (hero.type.experiencePercent != 0) { passives++; }
		if (hero.type.lootMoneyPercentage != 0 && sscp == null) { passives++; }
		if (hero.type.scavengeMoneyToSupply != 0 && sscp == null) { passives++; }
		if (hero.type.surpriseAttack) { passives++; }
		if (hero.type.fleetFireRatePercent != 0) { passives++; }
		if (hero.type.fleetAccuracyPercent != 0) { passives++; }
		if (hero.type.fleetCrewSpeedPercent != 0) { passives++; }
		if (hero.type.fleetFlammabilityPercent != 0) { passives++; }
		if (hero.type.fleetExplosionRiskPercent != 0) { passives++; }
		if (hero.type.fleetCommandCooldownPercent != 0) { passives++; }
		if (hero.type.fleetRepairAmountPercent != 0) { passives++; }
		if (hero.type.fleetFirefightAmountPercent != 0) { passives++; }
		return passives;
	}

	@Override
	public void draw(final Hero hero, MyDraw d, int x, int y, int w) {
		final int h = getHeight(hero, d, w) - MyDraw.SCROLL_EL_SPACING;
		d.drawPanel(x, y, w, h, -1);
		x += MyDraw.PANEL_INSET;
		y += MyDraw.PANEL_INSET;
		w -= MyDraw.PANEL_INSET * 2;
		
		WorldMap m = null;
		if (cw != null) {
			m = cw.map;
		}
		if (hsc != null) {
			m = hsc.map();
		}
		
		int bx = x + w;
		if (cw != null) {
			if (!hero.hired) {
				int bw = d.bw(hero.type.hireCost == 0 ? _t("hire") : _t("hire_sx", hero.type.hireCost));
				d.button(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, hero.type.hireCost == 0 ? _t("hire") : _t("hire_sx", hero.type.hireCost), new Runnable() {
					@Override
					public void run() {
						cw.giveCommand(msg("hireHero").put("empire", hero.inEmpire.id).put("hero", hero.id));
					}
				}, hero.inEmpire.getMoney() >= hero.type.hireCost);
				String fx = Hero.getStatChangeAppendix(cw.player, HeroEvent.heroHired(cw.player, hero.type), cw.map, false);
				if (!fx.isEmpty()) {
					d.tooltip(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H, hero.type.hireCost == 0 ? _t("hire") : _t("hire_sx", hero.type.hireCost) + fx);
				}
				bx -= bw + MyDraw.BUTTON_SPACING;
			}

			int bw = d.bw(_t("dismiss"));
			String cannotDismissReason = cw.cannotDismissReason(hero);
			d.button(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, _t("dismiss"), new Runnable() {
				@Override
				public void run() {
					cw.giveCommand(msg("fireHero").put("empire", hero.inEmpire.id).put("hero", hero.id));
				}
			}, cannotDismissReason == null);
			if (cannotDismissReason != null) {
				d.tooltip(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H, cannotDismissReason);
			}
			d.highlight("dismiss-hero-" + hero.id, AirshipGame.instance, bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H);
			String fx = Hero.getStatChangeAppendix(cw.player, HeroEvent.heroLeft(cw.player, hero.type), cw.map, false);
			if (!fx.isEmpty()) {
				d.tooltip(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H, _t("dismiss") + fx);
			}
			bx -= bw + MyDraw.BUTTON_SPACING;
		}
		if (hsc != null) {
			int bw = d.bw(_t("select_hero"));
			String cannotSelectReason = hsc.cannotSelectReason(hero);
			d.button(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, _t("select_hero"), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					hsc.select(hero);
				}
			}, cannotSelectReason == null);
			if (cannotSelectReason != null) {
				d.tooltip(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H, cannotSelectReason);
			}
			d.highlight("select-hero-" + hero.id, AirshipGame.instance, bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, MyDraw.BUTTON_H);
			bx -= bw + MyDraw.BUTTON_SPACING;
		}
		if (sscp != null) {
			int bw = d.bw(_t("select_hero_sx", hero.type.singleCombatCost));
			d.button(bx - bw, y + h - MyDraw.PANEL_INSET * 2 - MyDraw.BUTTON_H, bw, _t("select_hero_sx", hero.type.singleCombatCost), null, new InputRunnable() {
				@Override
				public void run(Input in) {
					sscp.select(hero);
				}
			}, sscp.available(hero));
			bx -= bw + MyDraw.BUTTON_SPACING;
		}
		
		d.text(MyDraw.TITLE_C + hero.getName(), AGame.BIG_FOUNT, x, y);
		y += AGame.BIG_FOUNT.lineHeight;
		d.text(hero.getDesc(), AGame.FOUNT, x, y, w);
		y += d.textSize(hero.getDesc(), AGame.FOUNT, 0, 0, w).height + MyDraw.UI_SPACING;
		d.portraitBlit(hero.type.img, x + MyDraw.PANEL_BORDER_W, y + MyDraw.PANEL_BORDER_W, portraitSize(), portraitSize());
		d.drawPanelBorder(x, y, portraitSize() + MyDraw.PANEL_BORDER_W * 2, portraitSize() + MyDraw.PANEL_BORDER_W * 2);
		x += portraitSize() + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING;
		w -= portraitSize() + MyDraw.PANEL_BORDER_W * 2 + MyDraw.UI_SPACING;
		int skillH = Math.max(16 + MyDraw.BUTTON_SPACING, AGame.FOUNT.lineHeight);
		d.text(_t("Maintenance_cost_x", "$" + hero.type.maintenance), AGame.FOUNT, x, y);
		y += skillH;
		if (!hero.type.canDismiss) {
			d.text(_t("cannotBeDismissed"), AGame.FOUNT, x, y);
			y += skillH;
		}
		if (m == null) {
			d.text(_t("role_" + hero.type.role), AGame.FOUNT, x, y);
		} else if (!hero.hired) {
			String text = _t("role_" + hero.type.role) + ", " + _t("for_hire");
			if (cw != null) {
				text += _t("x_time_until_hero_leaves", cw.describeTime(hero.recruitCooldown));
			}
			d.text(text, AGame.FOUNT, x, y);
		} else {
			String hireInfo = _t("hired") + ", ";
			if (hero.getInCity() != null) {
				d.text(hireInfo + _t("assigned_to_") + hero.getInCity().getDisplayName(), AGame.FOUNT, x, y);
			} else if (hero.getInShip() != null) {
				if (hero.onExpedition()) {
					d.text(hireInfo + _t("on_expedition_with_x", hero.getInShip().getName()), AGame.FOUNT, x, y);
				} else {
					d.text(hireInfo + _t("assigned_to_") + hero.getInShip().getName(), AGame.FOUNT, x, y);
				}
			} else {
				d.text(hireInfo + _t("assigned_to_") + _t("no_assigned"), AGame.FOUNT, x, y);
			}
			if (ss != null && hero.type.role == HeroType.Role.GOVERNOR && hero.getInCity() != null) {
				d.iconButton(x + w - MyDraw.ICON_BUTTON_SZ * 2 - MyDraw.BUTTON_SPACING, y, ss.mapGoto, new Runnable() {
					@Override
					public void run() {
						ss.showHeroes = false;
						ss.shipForHero = null;
						ss.cityForHero = null;
						ss.selectedFleet = null;
						ss.menuCity = hero.getInCity();
						ss.scrollX = -hero.getInCity().x;
						ss.scrollY = -hero.getInCity().y;
					}
				}, true);
			}
			if (ss != null && hero.type.role == HeroType.Role.CAPTAIN && hero.getInShip() != null) {
				d.iconButton(x + w - MyDraw.ICON_BUTTON_SZ * 2 - MyDraw.BUTTON_SPACING, y, ss.mapGoto, new Runnable() {
					@Override
					public void run() {
						if (ss.w.player == null) { return; }
						Fleet f = ss.w.player.fleet(hero.getInShip());
						if (f == null) { return; }
						ss.showHeroes = false;
						ss.shipForHero = null;
						ss.cityForHero = null;
						ss.selectedFleet = f;
						ss.menuCity = null;
						ss.scrollX = -f.intX();
						ss.scrollY = -f.intY();
					}
				}, true);
			}
			if (cw != null) {
				String cannotDismissReason = cw.cannotDismissReason(hero);
				d.iconButton(x + w - MyDraw.ICON_BUTTON_SZ, y, clearIcon, new Runnable() {
					@Override
					public void run() {
						cw.giveCommand(msg("unassignHero").put("empire", hero.inEmpire.id).put("hero", hero.id));
					}
				}, (hero.getInCity() != null || hero.getInShip() != null) && cannotDismissReason == null);
				if (cannotDismissReason != null) {
					d.tooltip(x + w - MyDraw.ICON_BUTTON_SZ, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, cannotDismissReason);
				} else {
					d.tooltip(x + w - MyDraw.ICON_BUTTON_SZ, y, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t("clear_assignement"));
				}
			}
		}
		y += MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING;
		if (hero.injuredTime > 0) {
			d.text(MyDraw.ERROR_C + _t("hero_injury_info", WorldMap.describeTime(hero.injuredTime, BonusSet.empty())), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.homeCity != null) {
			d.text(_t("hero_home_x", hero.homeCity.name), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.nemesisEmpire != null) {
			d.text(_t("hero_nemesis_x", hero.nemesisEmpire.name), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.research != 0) {
			d.text(_t("hero_research", (hero.type.research > 0 ? "+" : "") + hero.type.research), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.unrest != 0) {
			d.text(_t("hero_unrest", (hero.type.unrest > 0 ? "+" : "") + hero.type.unrest), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.spyDefence != 0) {
			d.text(_t("hero_spyDefence", (hero.type.spyDefence > 0 ? "+" : "") + hero.type.spyDefence), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.productionPercent != 0) {
			d.text(_t("hero_production", (hero.type.productionPercent > 0 ? "+" : "") + hero.type.productionPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.defenceBudget != 0) {
			d.text(_t("hero_defenceBudget", (hero.type.defenceBudget > 0 ? "+" : "") + hero.type.defenceBudget), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.incomePercent != 0) {
			d.text(_t("hero_incomePercent", (hero.type.incomePercent > 0 ? "+" : "") + hero.type.incomePercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.researchPercent != 0) {
			d.text(_t("hero_researchPercent", (hero.type.researchPercent > 0 ? "+" : "") + hero.type.researchPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.upgradeCostPercent != 0) {
			d.text(_t("hero_upgradeCostPercent", (hero.type.upgradeCostPercent > 0 ? "+" : "") + hero.type.upgradeCostPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.upgradeSpeedPercent != 0) {
			d.text(_t("hero_upgradeSpeedPercent", (hero.type.upgradeSpeedPercent > 0 ? "+" : "") + hero.type.upgradeSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.airshipSpeedPercent != 0) {
			d.text(_t("hero_airshipSpeedPercent", (hero.type.airshipSpeedPercent > 0 ? "+" : "") + hero.type.airshipSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.landshipSpeedPercent != 0) {
			d.text(_t("hero_landshipSpeedPercent", (hero.type.landshipSpeedPercent > 0 ? "+" : "") + hero.type.landshipSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.buildingSpeedPercent != 0) {
			d.text(_t("hero_buildingSpeedPercent", (hero.type.buildingSpeedPercent > 0 ? "+" : "") + hero.type.buildingSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.expeditionStrengthPercent != 0) {
			d.text(_t("hero_expeditionStrengthPercent", (hero.type.expeditionStrengthPercent > 0 ? "+" : "") + hero.type.expeditionStrengthPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetSpeedPercent != 0) {
			d.text(_t("hero_fleetSpeedPercent", (hero.type.fleetSpeedPercent > 0 ? "+" : "") + hero.type.fleetSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.shipBonus != null) {
			d.text(_t("hero_shipBonus", hero.type.shipBonus.getName()), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fireRatePercent != 0) {
			d.text(_t("hero_fireRatePercent", (hero.type.fireRatePercent > 0 ? "+" : "") + hero.type.fireRatePercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetFireRatePercent != 0) {
			d.text(_t("hero_fleetFireRatePercent", (hero.type.fleetFireRatePercent > 0 ? "+" : "") + hero.type.fleetFireRatePercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.accuracyPercent != 0) {
			d.text(_t("hero_accuracyPercent", (hero.type.accuracyPercent > 0 ? "+" : "") + hero.type.accuracyPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetAccuracyPercent != 0) {
			d.text(_t("hero_fleetAccuracyPercent", (hero.type.fleetAccuracyPercent > 0 ? "+" : "") + hero.type.fleetAccuracyPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.crewSpeedPercent != 0) {
			d.text(_t("hero_crewSpeedPercent", (hero.type.crewSpeedPercent > 0 ? "+" : "") + hero.type.crewSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetCrewSpeedPercent != 0) {
			d.text(_t("hero_crewSpeedPercent", (hero.type.fleetCrewSpeedPercent > 0 ? "+" : "") + hero.type.fleetCrewSpeedPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.flammabilityPercent != 0) {
			d.text(_t("hero_flammabilityPercent", (hero.type.flammabilityPercent > 0 ? "+" : "") + hero.type.flammabilityPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetFlammabilityPercent != 0) {
			d.text(_t("hero_fleetFlammabilityPercent", (hero.type.fleetFlammabilityPercent > 0 ? "+" : "") + hero.type.fleetFlammabilityPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.explosionRiskPercent != 0) {
			d.text(_t("hero_explosionRiskPercent", (hero.type.explosionRiskPercent > 0 ? "+" : "") + hero.type.explosionRiskPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetExplosionRiskPercent != 0) {
			d.text(_t("hero_fleetExplosionRiskPercent", (hero.type.fleetExplosionRiskPercent > 0 ? "+" : "") + hero.type.fleetExplosionRiskPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.commandCooldownPercent != 0) {
			d.text(_t("hero_commandCooldownPercent", (hero.type.commandCooldownPercent > 0 ? "+" : "") + hero.type.commandCooldownPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetCommandCooldownPercent != 0) {
			d.text(_t("hero_fleetCommandCooldownPercent", (hero.type.fleetCommandCooldownPercent > 0 ? "+" : "") + hero.type.fleetCommandCooldownPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.repairAmountPercent != 0) {
			d.text(_t("hero_repairAmountPercent", (hero.type.repairAmountPercent > 0 ? "+" : "") + hero.type.repairAmountPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetRepairAmountPercent != 0) {
			d.text(_t("hero_repairAmountPercent", (hero.type.fleetRepairAmountPercent > 0 ? "+" : "") + hero.type.fleetRepairAmountPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.firefightAmountPercent != 0) {
			d.text(_t("hero_firefightAmountPercent", (hero.type.firefightAmountPercent > 0 ? "+" : "") + hero.type.firefightAmountPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.fleetFirefightAmountPercent != 0) {
			d.text(_t("hero_fleetFirefightAmountPercent", (hero.type.fleetFirefightAmountPercent > 0 ? "+" : "") + hero.type.fleetFirefightAmountPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.propulsionPercent != 0) {
			d.text(_t("hero_propulsionPercent", (hero.type.propulsionPercent > 0 ? "+" : "") + hero.type.propulsionPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.liftPercent != 0) {
			d.text(_t("hero_liftPercent", (hero.type.liftPercent > 0 ? "+" : "") + hero.type.liftPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.armourRepairPercent != 0) {
			d.text(_t("hero_armourRepairPercent", hero.type.armourRepairPercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.experiencePercent != 0) {
			d.text(_t("hero_experiencePercent", (hero.type.experiencePercent > 0 ? "+" : "") + hero.type.experiencePercent), AGame.FOUNT, x, y);
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.lootMoneyPercentage != 0 && sscp == null) {
			d.text(_t("hero_looter"), AGame.FOUNT, x, y);
			d.tooltip(x, y, w, AGame.FOUNT.lineHeight, _t("hero_looter_tooltip", hero.type.lootMoneyPercentage));
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.scavengeMoneyToSupply != 0 && sscp == null) {
			d.text(_t("hero_scavenger"), AGame.FOUNT, x, y);
			d.tooltip(x, y, w, AGame.FOUNT.lineHeight, _t("hero_scavenger_tooltip"));
			y += AGame.FOUNT.lineHeight;
		}
		if (hero.type.surpriseAttack) {
			d.text(_t("hero_surprise_attack"), AGame.FOUNT, x, y);
			d.tooltip(x, y, w, AGame.FOUNT.lineHeight, _t("hero_surprise_attack_tooltip"));
			y += AGame.FOUNT.lineHeight;
		}
		if (numPassives(hero) != 0) { y += MyDraw.UI_SPACING; }
		for (Edict e : hero.type.edicts) {
			d.blit(e.icon, x, y);
			d.text(e.getName(), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			d.tooltip(x, y, w, skillH, e.getDesc(hero, m));
			y += skillH;
		}
		for (CityUpgradeType cut : hero.type.instabuild) {
			d.blit(cut.icon.get(BonusSet.empty()), x, y);
			d.text(_t("can_instabuild_info", cut.getName(), cut.getInstabuildCost()), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			y += skillH;
		}
		for (CombatAbility ab : hero.type.combatAbilities) {
			d.blit(ab.icon, x, y);
			d.text(ab.getName(), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			d.tooltip(x, y, w, skillH, ab.getDesc(hero.type));
			y += skillH;
		}
		for (MonsterNestType nt : hero.type.clearableNests) {
			d.blit(nt.getMapImage(), x, y);
			d.text(_t("can_clear_nest_x", nt.getName()), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			String tt = cw == null ? hero.type.getNestClearDesc(nt, null, null, null) : hero.type.getNestClearDesc(nt, hero, cw.player, cw);
			d.tooltip(x, y, w, skillH, tt);
			y += skillH;
		}
		for (MonsterNestType nt : hero.type.moveableNests) {
			d.blit(nt.getMapImage(), x, y);
			d.text(_t("can_move_nest_x", nt.getName()), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			String tt = cw == null ? hero.type.getNestMoveDesc(nt, null, null, null, null) : hero.type.getNestMoveDesc(nt, hero, cw.player, cw, hero.getInCity());
			d.tooltip(x, y, w, skillH, tt);
			y += skillH;
		}
		for (Tech.Choice tech : hero.type.techs) {
			d.blit(techIcon, x, y);
			d.text(_t("gain_tech_x", tech.getName()), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			StringBuilder desc = new StringBuilder();
			boolean first = true;
			for (Bonus b : tech.bonusList) {
				if (first) {
					first = false;
				} else {
					desc.append("\n");
				}
				desc.append(b.getDesc());
			}
			d.tooltip(x, y, w, skillH, desc.toString());
			y += skillH;
		}
		if (sscp == null && hero.type.bonus != null) {
			d.blit(bonusIcon, x, y);
			d.text(hero.type.bonus.getName(), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			d.tooltip(x, y, w, skillH, hero.type.bonus.getDesc());
			y += skillH;
		}
		if (sscp == null && hero.type.departureBonus != null) {
			d.blit(leaveIcon, x, y);
			d.text(_t("on_departure_bonus", hero.type.departureBonus.getName()), AGame.FOUNT, x + 16 + MyDraw.BUTTON_SPACING, y);
			d.tooltip(x, y, w, skillH, _t("on_departure_bonus_tooltip", hero.getName()) + "\n\n" + hero.type.departureBonus.getDesc());
			y += skillH;
		}
		if (hasActives(hero)) {
			y += MyDraw.UI_SPACING;
		}
		if (sscp == null) {
			for (Stat stat : hero.type.stats) {
				d.progressBar(x, y, w, hero.stats.get(stat.name) / 100.0);
				String label = stat.getName() + ": " + hero.stats.get(stat.name);
				int textW = (int) d.textSize(label, AGame.FOUNT_OUTLINE).x;
				d.heavilyBorderedText(label, AGame.FOUNT, AGame.FOUNT_OUTLINE, Clr.WHITE, Clr.BLACK, x + w / 2 - textW / 2, y + MyDraw.PROGRESS_BAR_H / 2 - AGame.FOUNT.lineHeight / 2, 10000);
				int iconY = y + MyDraw.PROGRESS_BAR_H / 2 - 8;
				int iconX0 = x + MyDraw.PROGRESS_BAR_H / 2 - 8;
				int iconX100 = x + w - MyDraw.PROGRESS_BAR_H / 2 - 8;
				if (stat.name.equals("loyalty")) {
					int boundary = EmpireStat.CORRUPTABLE_LOYALTY.get(cw == null || cw.player == null ? BonusSet.empty() : cw.player.bonuses);
					if (boundary > 0) {
						int iconXDisloyal = x + w * boundary / 100 - MyDraw.PROGRESS_BAR_H / 2 - 8;
						d.blit(disloyalIconBg, MyDraw.ICON_TINT, iconXDisloyal, iconY);
						d.blit(disloyalIcon, iconXDisloyal, iconY);
						d.tooltip(iconXDisloyal, iconY, 16, 16, _t("disloyal_boundary_tt", boundary));
					}
				}
				if (stat.dieOn0) {
					d.blit(dieIconBg, MyDraw.ICON_TINT, iconX0, iconY);
					d.blit(dieIcon, iconX0, iconY);
					d.tooltip(iconX0, iconY, 16, 16, _t("die_on_x_stat", 0, stat.getName()));
				}
				if (stat.leaveOn0) {
					d.blit(leaveIconBg, MyDraw.ICON_TINT, iconX0, iconY);
					d.blit(leaveIcon, iconX0, iconY);
					d.tooltip(iconX0, iconY, 16, 16, _t("leave_on_x_stat", 0, stat.getName()));
				}
				if (stat.loseOn0) {
					d.blit(loseIconBg, MyDraw.ICON_TINT, iconX0, iconY);
					d.blit(loseIcon, loseClr, iconX0, iconY);
					d.tooltip(iconX0, iconY, 16, 16, _t("lose_on_x_stat", 0, stat.getName()));
				}
				if (stat.evolveOn0 != null) {
					d.blit(evolveIconBg, MyDraw.ICON_TINT, iconX0, iconY);
					d.blit(evolveIcon, iconX0, iconY);
					d.tooltip(iconX0, iconY, 16, 16, _t("change_on_x_stat", 0, stat.getName()));
				}
				if (stat.dieOn100) {
					d.blit(dieIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(dieIcon, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("die_on_x_stat", 100, stat.getName()));
				}
				if (stat.leaveOn100) {
					d.blit(leaveIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(leaveIcon, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("leave_on_x_stat", 100, stat.getName()));
				}
				if (stat.evolveOn100 != null) {
					d.blit(evolveIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(evolveIcon, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("change_on_x_stat", 100, stat.getName()));
				}
				if (stat.winOn100 && (m == null || m.toggles.contains(ConquestToggle.HERO_VICTORY))) {
					d.blit(winIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(winIcon, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("win_on_x_stat", 100, stat.getName()));
				}
				if (stat.loseOn100) {
					d.blit(loseIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(loseIcon, loseClr, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("lose_on_x_stat", 100, stat.getName()));
				}
				if (stat.coronationOn100 && (m == null || (m.toggles.contains(ConquestToggle.CORONATION) && m.toggles.contains(ConquestToggle.HERO_VICTORY))) && (hero.inEmpire == null || m == null || m.isCoronationPossible(hero.inEmpire))) {
					d.blit(crownIconBg, MyDraw.ICON_TINT, iconX100, iconY);
					d.blit(crownIcon, iconX100, iconY);
					d.tooltip(iconX100, iconY, 16, 16, _t("coronation_on_x_stat", 100, stat.getName()));
				}
				y += MyDraw.PROGRESS_BAR_H + MyDraw.BUTTON_SPACING;
				for (Changer c : stat.changers) {
					String key = "changer_info";
					if (c.hook != null && c.hook.type == 7) { // 7 here is "every month"
						key = "changer_info_every_month";
					}
					d.text(_t(key, (c.getAmount(cw == null ? null : cw.map) > 0 ? "+ " : "- ") + Math.abs(c.getAmount(m)), c.hook.getDesc(hero)), AGame.FOUNT, x, y);
					y += AGame.FOUNT.lineHeight;
				}
				y += MyDraw.BUTTON_SPACING;
			}
		}
		
		String hireAndDismissInfo = hireAndDismissInfo(hero);
		if (!hireAndDismissInfo.isEmpty()) {
			d.text(hireAndDismissInfo, AGame.FOUNT, x, y, w);
		}
	}
	
	public static interface HeroSelectionCallback {
		public String cannotSelectReason(Hero h);
		public void select(Hero h);
		public WorldMap map();
	}
}
