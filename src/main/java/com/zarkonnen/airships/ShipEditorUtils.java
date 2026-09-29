package com.zarkonnen.airships;

import com.zarkonnen.airships.Combat.Side;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.catengine.util.Utils.Pair;
import static com.zarkonnen.catengine.util.Utils.*;

import java.util.ArrayList;

public strictfp class ShipEditorUtils {
	public static final int MAX_COST = 20000;
	public static final int MAX_CREW = 120;
	
	public static ArrayList<Pair<String, String>> getStats(Airship ship, int budget, Airship original) {
		ArrayList<Pair<String, String>> l = new ArrayList<Pair<String, String>>();
		
		if (ship.nonCombat()) {
			l.add(p("Cost_", "$" + ship.getCost()));
			l.add(p("Maintenance_", "$" + ship.maintenanceCost()));
			l.add(p("Speed_", "" + (int) StrictMath.ceil(ship.getReportedMainMapSpeed() * 3600 / AGame.PX_TO_M) + _t("kmperhour")));
			int sr = ship.getSupplyRequired();
			if (sr > 0) {
				l.add(p("Supply_", sr + ""));
			}
			if (ship.getSupplyCapacity() > 0) {
				l.add(p("Supply_Capacity_", "" + ship.getSupplyCapacity()));
			}
			l.add(p("non_combatant", ""));
			return l;
		}
		
		int cost = original == null ? ship.getCost() : ship.getRefitCostFrom(original, true);
		String costName = original == null ? "Cost_" : "Rebuild_cost_";
		String maxCostInfo = "";
		if (ship.getCost() > MAX_COST * 0.8) {
			maxCostInfo = " [ffce6d](" + _t("max") + " " + MAX_COST + ")[]";
		}
		if (budget > 0) {
			l.add(p(costName, (cost > budget ? MyDraw.ERROR_C : "") + "$" + cost + " / " + budget + maxCostInfo));
		} else {
			l.add(p(costName, "$" + cost + "[]" + maxCostInfo));
		}
		
		int mc = ship.maintenanceCost();
		if (mc >= 0) {
			l.add(p("Maintenance_", "$" + mc));
		} else {
			l.add(p("Income_", "$" + (0 - mc)));
		}
		
		if (ship.type.mobile) {
			if (ship.type.onGround) {
				l.add(p("Weight_", (ship.maxCarryWeight() < ship.getWeight() ? MyDraw.ERROR_C : "") + (ship.getWeight() + "/" + ship.maxCarryWeight()) + "[]"));
			} else {
				l.add(p("Service_ceiling_", (ship.realServiceCeiling() <= 0 ? MyDraw.ERROR_C : "") + ship.realServiceCeiling() / AGame.PX_TO_M + "m[]"));
			}
			l.add(p("Speed_", "" + (int) StrictMath.ceil(ship.getMainMapSpeed(BonusSet.empty()) * 3600 / AGame.PX_TO_M) + _t("kmperhour")));
		}
		
		int n = ship.getQuartered(CrewType.onlyWorkers);
		String maxCrewMsg = "";
		if (n > MAX_CREW * 0.8) {
			maxCrewMsg = " [ffce6d](" + _t("max") + " " + MAX_CREW + ")[]";
		}
		int m = ship.getRequiredCrew();
		l.add(p("Crew_", (n < m ? MyDraw.ERROR_C : "") + n + maxCrewMsg + "[]"));
		if (n < m) {
			l.add(p("Min_crew_", "" + m));
		} else {
			l.add(p("Recommended_crew_", "" + ship.getRecommendedCrew()));
		}
		if (ship.currentWorkingCrew() > ship.designedWorkingCrew()) {
			l.add(p("More_quarters_needed", ""));
		}
		
		l.add(p("Coal_", "" + ship.getCoalCapacity()));
		l.add(p("Ammo_", "" + ship.getAmmoCapacity()));
		l.add(p("Water_", "" + ship.getWaterCapacity()));
		l.add(p("Repair_supplies_", "" + ship.getRepairCapacity()));

		if (ship.type.mobile) {
			l.add(p("Supply_Required_", (ship.getSupplyRequired() > ship.getSupplyProvided() ? MyDraw.ERROR_C : "") + ship.getSupplyRequired() + "[]"));
			l.add(p("Supply_Provided_", "" + ship.getSupplyProvided()));
			l.add(p("Supply_Capacity_", "" + ship.getSupplyCapacity()));
		} else {
			l.add(p("Supply_", (ship.getSupplyRequired() > ship.getSupplyProvided() ? MyDraw.ERROR_C : "") + ship.getSupplyRequired() + "/" + ship.getSupplyProvided() + "[]"));
		}
		
		if (!ship.generatesCommandPoints()) {
			l.add(p("Cannot_issue_commands", ""));
		} else {
			l.add(p("Can_issue_a_command_every_", "" + ship.commandPointsRequired() / ship.commandPointsGenerated() / 1000 + " " + _t("seconds")));
		}
		if (ship.totalFleetCommandBonus() != 0) {
			l.add(p("Fleet_Command_Bonus_", (int) (ship.totalFleetCommandBonus() * 100) + "%"));
		}
		
		return l;
	}
	
	public static boolean isModuleAFixer(Airship ship, ModuleType type, BonusSet bonuses) {
		if (ship.modules.isEmpty()) { return false; }
		
		if (type.getCommand(ship.currentBonuses) > 0 && !ship.generatesCommandPoints()) {
			return true;
		}
		
		if (type == ModuleType.ofName("CORRIDOR") && !ship.isPathingFullyConnected()) {
			return true;
		}
		
		if (!ship.type.onGround) {
			// insufficient lift
			if (type.getLift(bonuses) > 0 && ship.realServiceCeiling() <= 0) {
				return true;
			}
		}
		if (ship.type.mobile) {
			// no engines
			if (type.getPropulsion(ship.currentBonuses) > 0 && ship.getSpeed() == 0) {
				return true;
			}
		}
		if (type.getCoal(ship.currentBonuses) > 0 && ship.getCoalCapacity() == 0 && ship.requiresCoal()) {
			return true;
		}
		
		if (type.getAmmo(ship.currentBonuses) > 0 && ship.requiresAmmo() && ship.getAmmoCapacity() == 0) {
			return true;
		}

		if (type.getQuarters(ship.currentBonuses) > 0 && ship.getQuartered(CrewType.onlyWorkers) < ship.getRequiredCrew() && CrewType.onlyWorkers.contains(type.getQuartersType(ship.currentBonuses))) {
			return true;
		}
		
		if (type.getSupplyProvided(ship.currentBonuses) > 0 && ship.getSupplyProvided() < ship.getSupplyRequired()) {
			return true;
		}
		
		return false;
	}

	public static boolean checkForOverlaps(Airship ship, UniScreen us) {
		Airship ignore = (us.intent instanceof SingleShipIntent) ? ((SingleShipIntent) us.intent).getIgnoredOriginalShip(us) : null;
		if (us.city != null) {
			for (Airship s : us.city.getDefences()) {
				if (s == ignore) { continue; }
				if (s != ship && s.overlapsWith(ship)) {//Rect2D.intersects(s.x, s.y, s.getBBWidth(), s.getBBHeight(), ship.x, ship.y, ship.getBBWidth(), ship.getBBHeight())) {
					return true;
				}
			}
			Fleet gar = us.wm.getGarrison(us.city);
			if (gar != null) {
				for (Airship s : gar.actives) {
					if (s == ignore) { continue; }
					if (s != ship && s.overlapsWith(ship)) {
						return true;
					}
				}
			}
			if (ship.overlapsWith(us.city.ground, /* ignoreSoftThings */ false)) {
				return true;
			}
		}
		if (us.combat != null) {
			for (Side side : us.combat.sides) {
				for (Airship s : side.ships) {
					if (s == ignore) { continue; }
					if (s != ship && s.overlapsWith(ship)) {
						return true;
					}
				}
			}
			if (ship.overlapsWith(us.combat.landFormations.get(0), /* ignoreSoftThings */ false)) {
				return true;
			}
		}
		if (us.getSetupFleet() != null) {
			for (Airship s : us.getSetupFleet()) {
				if (s == ignore) { continue; }
				if (s != ship && s.overlapsWith(ship)) {
					return true;
				}
			}
			if (us.setupGround != null && ship.overlapsWith(us.setupGround, /* ignoreSoftThings */ false)) {
				return true;
			}
		}
		return false;
	}
	
	public static ArrayList<String> shipWarnings(Airship ship) {
		ArrayList<String> ws = new ArrayList<String>();
		if (!ship.generatesCommandPoints() && !ship.modules.isEmpty()) {
			ws.add(_t("Cannot_give_commands_to_" + ship.type.name()));
		}
		if (!ship.type.onGround && ship.realServiceCeiling() < AGame.SGS * 7 && ship.realServiceCeiling() > 0) {
			ws.add(_t("Ship_service_ceiling_low"));
		}
		if (ship.type.mobile) {
			if (ship.type.onGround) {
				if (ship.getWeight() > ship.optimalCarryWeight()) {
					ws.add(_t("The_ship_is_heavy"));
				}
			}
		}
		if (ship.getStructuralStressHPMultiplier() < 1.0) {
			if (ship.getStructuralStressHPMultiplier() < 0.75) {
				if (ship.getStructuralStressHPMultiplier() < 0.4) {
					ws.add(_t("Ship_structural_integrity_super_low"));
				} else {
					ws.add(_t("Ship_structural_integrity_very_low"));
				}
			} else {
				ws.add(_t("Ship_structural_integrity_low"));
			}
		}
		return ws;
	}
	
	public static ArrayList<String> shipErrors(Airship ship, boolean checkBudget, boolean checkBonuses, int budget, BonusSet bonuses, Airship original, UniScreen checkForOverlaps) {
		ArrayList<String> es = new ArrayList<String>();
		// empty
		// insufficient crew
		if (ship.modules.isEmpty()) {
			es.add(_t("edit_get_started"));
			return es;
		}
		
		BonusSet originalCurrentBonuses = ship.currentBonuses.clone();
		BonusSet originalBaseBonuses = ship.getBaseBonuses();
		if (bonuses != null) {
			ship.setBaseBonuses(bonuses);
		}
		
		if (checkForOverlaps != null && checkForOverlaps(ship, checkForOverlaps)) {
			es.add(_t("The_" + ship.type.name() + "_cannot_be_placed_here"));
		}
		
		if (!ship.isFullyConnectedInEditor()) {
			es.add(_t("disconnected_" + ship.type.name()));
		} else if (!ship.isPathingFullyConnected()) {
			es.add(_t("inaccessible_" + ship.type.name()));
		}

		if (!ship.type.onGround) {
			// insufficient lift
			if (ship.realServiceCeiling() <= 0) {
				es.add(_t("The_ship_is_too_heavy_to_fly"));
			}
		}
		
		if (ship.type.mobile) {
			// no engines
			if (ship.getSpeed() == 0) {
				es.add(_t("The_ship_has_no_propulsion"));
			}
			if (ship.type.onGround) {
				if (ship.getWeight() > ship.maxCarryWeight()) {
					es.add(_t("The_ship_is_too_heavy"));
				}
			}
		}
		if (ship.getCoalCapacity() == 0 && ship.requiresCoal()) {
			es.add(_t("The_ship_has_no_coal"));
		}
		if (!ship.type.mobile) {
			int baseWidth = 0;
			for (int tx = 0; tx < ship.getWidth(); tx++) {
				if (ship.tileAt(tx, ship.getHeight() - 1) != null) {
					baseWidth++;
				}
			}
			double dmg = 0.016 * 0.016 * (ship.getCollisionMass() + 5000) * 0.8 / baseWidth - 1;
			if (dmg >= 0.3) {
				es.add(_t("collapse_warning"));
			}
		}

		// no ammo
		if (ship.requiresAmmo() && ship.getAmmoCapacity() == 0) {
			es.add(_t("The_" + ship.type.name() + "_has_no_ammunition"));
		}

		if (ship.getRequiredCrew() > 0 && ship.getQuartered(CrewType.onlyWorkers) == 0) {
			es.add(_t("The_" + ship.type.name() + "_has_no_crew"));
		} else if (ship.getQuartered(CrewType.onlyWorkers) < ship.getRequiredCrew()) {
			es.add(_t("The_" + ship.type.name() + "_has_insufficient_crew"));
		}
		
		if (ship.getSupplyProvided() < ship.getSupplyRequired()) {
			es.add(_t("The_" + ship.type.name() + "_needs_more_supply_hatches"));
		}
		
		if (!ship.getObstructedModules().isEmpty()) {
			es.add(_t("Modules_are_obstructing_modules"));
		}
		
		for (int i = 0; i < ship.modules.size(); i++) {
			if (!ship.modules.get(i).type.availableFor(ship.type)) {
				es.add(_t("x_module_not_available_for_" + ship.type.name(), ship.modules.get(i).type.getName()));
				break;
			}
		}
		
		for (int i = 0; i < ship.tiles.size(); i++) {
			if (!ship.tiles.get(i).armour.type.availableFor(ship.type)) {
				es.add(_t("x_module_not_available_for_" + ship.type.name(), ship.tiles.get(i).armour.type.getName()));
				break;
			}
		}

		if (checkBudget) {
			// too expensive
			if (original == null) {
				if (ship.getCost() > budget) {
					es.add(_t("Insufficient_funds_to_build_this_" + ship.type.name()));
				}
			} else {
				if (ship.getRefitCostFrom(original, true) > budget) {
					es.add(_t("Insufficient_funds_to_refit_this_" + ship.type.name()));
				}
			}
		}
		if (checkBonuses) {
			// module/armour X not available
			for (ModuleType mt : Loadable.all(ModuleType.class)) {
				if (mt.getRequired() != null && !bonuses.contains[mt.getRequired().ordinal()]) {
					for (Module m : ship.modules) {
						if (m.type == mt) {
							es.add(_t("x_is_not_available", mt.getName()));
							break;
						}
					}
				}
			}
			for (ArmourType at : Loadable.all(ArmourType.class)) {
				if (at.required != null && !bonuses.contains[at.required.ordinal()]) {
					for (Tile t : ship.tiles) {
						if (t.armour.type == at) {
							es.add(_t("x_is_not_available", at.getName()));
							break;
						}
					}
				}
			}
		}
		
		ship.setBaseBonuses(originalBaseBonuses);
		ship.currentBonuses.clear();
		ship.currentBonuses.addAll(originalCurrentBonuses);
		
		return es;
	}

	public static ConstructionBackend shipsList(ShipType st, ShipHelperWidget shw) {
		return new ConstructionBackend(st, shw);
	}
}
