package com.zarkonnen.airships;

import static com.zarkonnen.airships.Client.msg;
import static com.zarkonnen.airships.DiplomacyWindow.ICON_SIZE;
import static com.zarkonnen.airships.EmpireStat.CANCELLED_ULTIMATUM_COST;
import static com.zarkonnen.airships.Lang._t;
import com.zarkonnen.airships.Relationship.Offer;
import com.zarkonnen.airships.Relationship.Ultimatum;
import com.zarkonnen.catengine.Hooks;
import com.zarkonnen.catengine.Img;
import com.zarkonnen.catengine.Input;
import com.zarkonnen.catengine.util.Clr;
import com.zarkonnen.catengine.util.Pt;
import com.zarkonnen.catengine.util.ScreenMode;
import java.util.Collections;

public class OfferEditor {
	public final DiplomacyWindow diplomacy;
	public final Img add = new Img("ui", 272, 512, 16, 16, false);
	public final Img remove = new Img("ui", 144, 416, 16, 16, false);
	
	public Offer offer;
	public Offer threat;
	public boolean demandCity;
	
	private final OfferEditor editor;
	
	private ScrollBar offerOptionsSB = new ScrollBar();
	private ScrollBar offerSB = new ScrollBar();
	private ScrollBar relationshipSB = new ScrollBar();
	private ScrollBar threatOptionsSB = new ScrollBar();
	private ScrollBar threatSB = new ScrollBar();
	private IntRect offerOptionsSBRect = new IntRect();
	private IntRect offerSBRect = new IntRect();
	private IntRect relationshipSBRect = new IntRect();
	private IntRect threatOptionsSBRect = new IntRect();
	private IntRect threatSBRect = new IntRect();
	
	DiplomacyWindow.OfferEffectsSummarizer cascadeSummarizer;
	DiplomacyWindow.OfferEffectsSummarizer threatCascadeSummarizer;

	public OfferEditor(DiplomacyWindow diplomacy, Offer offer) {
		this.diplomacy = diplomacy;
		this.offer = offer;
		cascadeSummarizer = diplomacy.cascadeSummarizer(null);
		editor = this;
	}
	
	public OfferEditor(DiplomacyWindow diplomacy, Offer offer, Offer threat) {
		this.diplomacy = diplomacy;
		this.offer = offer;
		this.threat = threat;
		cascadeSummarizer = diplomacy.cascadeSummarizer(null);
		threatCascadeSummarizer = diplomacy.cascadeSummarizer(diplomacy.ss.w.player);
		editor = this;
	}
	
	public boolean isUltimatum() {
		return threat != null;
	}
	
	public boolean isCeasefireOffer() {
		return !isUltimatum() && offer.rel.level == Relationship.Level.WAR;
	}

	private void drawOption(boolean doAdd, MyDraw d, Img icon, boolean crossedOut, String text, final Offer o2, int x2, int y2, int colW, int aiEvaluation, String aiEvaluationTooltip) {
		drawOption(doAdd, d, icon, crossedOut, text, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip, null, null);
	}
	
	private void drawOption(boolean doAdd, MyDraw d, Img icon, boolean crossedOut, String text, final Offer o2, int x2, int y2, int colW, int aiEvaluation, String aiEvaluationTooltip, final City c, final StrategicScreen ss) {
		int availableW = colW - MyDraw.PANEL_INSET * 2 - ICON_SIZE - MyDraw.UI_SPACING - MyDraw.ICON_BUTTON_SZ;
		if (aiEvaluationTooltip != null) {
			availableW -= (int) d.textSize("-999", AGame.FOUNT).x + MyDraw.UI_SPACING;
		}
		int panelH = Math.max(Math.max(ICON_SIZE, MyDraw.ICON_BUTTON_SZ), (int) d.textSize(text, AGame.BIG_FOUNT, 0, 0, availableW).height)
				+ MyDraw.PANEL_INSET * 2;
		d.drawPanel(x2, y2, colW, panelH, -1);
		d.blit(icon, x2 + MyDraw.PANEL_INSET, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2);
		if (crossedOut) {
			d.blit(diplomacy.crossedOut, x2 + MyDraw.PANEL_INSET + 1, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2 + 1);
		}
		d.text(text, AGame.BIG_FOUNT, x2 + MyDraw.PANEL_INSET + ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET, availableW);
		//d.rect(Clr.RED, x2 + MyDraw.PANEL_INSET + DiplomacyWindow.ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET, availableW, 2);
		WorldMap m = diplomacy.ss.w.map;
		String failReason = o2.rel.getInvalidReason(o2, null, m, isUltimatum());
		d.iconButton(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y2 + MyDraw.PANEL_INSET, doAdd ? add : remove, new Runnable() {
			@Override
			public void run() {
				offer = o2;
			}
		}, failReason == null);
		if (c != null) {
			d.iconButton(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ * 2 - MyDraw.BUTTON_SPACING, y2 + MyDraw.PANEL_INSET, ss.mapGoto, new Runnable() {
				@Override
				public void run() {
					ss.scrollX = -c.x;
					ss.scrollY = -c.y;
					if (ss.showDiplomacy) {
						ss.showCityForDiplomacy = true;
					}
				}
			}, true);
		}
		if (aiEvaluationTooltip != null && (failReason == null || !doAdd)) {
			String evalText = aiEvaluation > 0 ? "+" + aiEvaluation : "" + aiEvaluation;
			int evalTextW = (int) d.textSize(evalText, AGame.FOUNT).x + MyDraw.UI_SPACING;
			d.text(evalText, AGame.FOUNT, x2 + colW - MyDraw.PANEL_BORDER_W - MyDraw.ICON_BUTTON_SZ - evalTextW - (c != null ? MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING : 0), y2 + MyDraw.PANEL_INSET);
			d.tooltip(x2 + colW - MyDraw.PANEL_BORDER_W - MyDraw.ICON_BUTTON_SZ - evalTextW - (c != null ? MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING : 0), y2 + MyDraw.PANEL_INSET, evalTextW, AGame.FOUNT.lineHeight, aiEvaluationTooltip);
		}
		if (failReason != null) {
			d.tooltip(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ - (c != null ? MyDraw.ICON_BUTTON_SZ + MyDraw.BUTTON_SPACING : 0), y2 + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t(failReason));
		}
	}
	
	private int optionH(MyDraw d, String text, int colW, boolean aiEvaluation) {
		int availableW = colW - MyDraw.PANEL_INSET * 2 - ICON_SIZE - MyDraw.UI_SPACING - MyDraw.ICON_BUTTON_SZ;
		if (aiEvaluation) {
			availableW -= (int) d.textSize("-999", AGame.FOUNT).x + MyDraw.UI_SPACING;
		}
		return Math.max(Math.max(ICON_SIZE, MyDraw.ICON_BUTTON_SZ), (int) d.textSize(text, AGame.BIG_FOUNT, 0, 0, availableW).height)
				+ MyDraw.PANEL_INSET * 2;
	}
	
	private void drawOption(boolean doAdd, MyDraw d, Img icon, boolean crossedOut, String text, int x2, int y2, int colW, Runnable run, boolean enabled) {
		d.drawPanel(x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, -1);
		d.blit(icon, x2 + MyDraw.PANEL_INSET, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2);
		if (crossedOut) {
			d.blit(diplomacy.crossedOut, x2 + MyDraw.PANEL_INSET + 1, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2 + 1);
		}
		d.text(text, AGame.BIG_FOUNT, x2 + MyDraw.PANEL_INSET + ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET);
		d.iconButton(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y2 + MyDraw.PANEL_INSET, doAdd ? add : remove, run, enabled);
	}
	
	private void drawThreatOption(boolean doAdd, MyDraw d, Img icon, boolean crossedOut, String text, final Offer o2, int x2, int y2, int colW, int aiEvaluation, String aiEvaluationTooltip) {
		d.drawPanel(x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, -1);
		d.blit(icon, x2 + MyDraw.PANEL_INSET, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2);
		if (crossedOut) {
			d.blit(diplomacy.crossedOut, x2 + MyDraw.PANEL_INSET + 1, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2 + 1);
		}
		d.text(text, AGame.BIG_FOUNT, x2 + MyDraw.PANEL_INSET + ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET);
		WorldMap m = diplomacy.ss.w.map;
		String failReason = o2.rel.getInvalidReason(o2, diplomacy.ss.w.player, m, false);
		if (threat.newLevel == Relationship.Level.WAR && o2.newLevel != null) {
			failReason = "must_stop_threatening_war_first";
		}
		d.iconButton(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y2 + MyDraw.PANEL_INSET, doAdd ? add : remove, new Runnable() {
			@Override
			public void run() {
				threat = o2;
			}
		}, failReason == null);
		if (aiEvaluationTooltip != null && failReason == null) {
			String evalText = aiEvaluation > 0 ? "+" + aiEvaluation : "" + aiEvaluation;
			int evalTextW = (int) d.textSize(evalText, AGame.FOUNT).x + MyDraw.UI_SPACING;
			d.text(evalText, AGame.FOUNT, x2 + colW - MyDraw.PANEL_BORDER_W - MyDraw.ICON_BUTTON_SZ - evalTextW, y2 + MyDraw.PANEL_INSET);
			d.tooltip(x2 + colW - MyDraw.PANEL_BORDER_W - MyDraw.ICON_BUTTON_SZ - evalTextW, y2 + MyDraw.PANEL_INSET, evalTextW, AGame.FOUNT.lineHeight, aiEvaluationTooltip);
		}
		if (failReason != null) {
			d.tooltip(x2 + colW - MyDraw.PANEL_INSET - MyDraw.ICON_BUTTON_SZ, y2 + MyDraw.PANEL_INSET, MyDraw.ICON_BUTTON_SZ, MyDraw.ICON_BUTTON_SZ, _t(failReason));
		}
	}
	
	private void drawItem(MyDraw d, Img icon, String text, int x2, int y2, int colW) {
		d.drawPanel(x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, -1);
		d.blit(icon, x2 + MyDraw.PANEL_INSET, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2);
		d.text(text, AGame.BIG_FOUNT, x2 + MyDraw.PANEL_INSET + ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET);
	}
	
	private void drawItem(MyDraw d, Img icon, String text, int x2, int y2, int colW, int aiEvaluation, String aiEvaluationTooltip) {
		d.drawPanel(x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, -1);
		d.blit(icon, x2 + MyDraw.PANEL_INSET, y2 + MyDraw.PANEL_INSET + MyDraw.BUTTON_H / 2 - ICON_SIZE / 2);
		d.text(text, AGame.BIG_FOUNT, x2 + MyDraw.PANEL_INSET + ICON_SIZE + MyDraw.UI_SPACING, y2 + MyDraw.PANEL_INSET);
		String evalText = aiEvaluation > 0 ? "+" + aiEvaluation : "" + aiEvaluation;
		int evalTextW = (int) d.textSize(evalText, AGame.FOUNT).x + MyDraw.UI_SPACING;
		d.text(evalText, AGame.FOUNT, x2 + colW - MyDraw.PANEL_BORDER_W - evalTextW, y2 + MyDraw.PANEL_INSET);
		d.tooltip(x2 + colW - MyDraw.PANEL_BORDER_W - evalTextW, y2 + MyDraw.PANEL_INSET, evalTextW, AGame.FOUNT.lineHeight, aiEvaluationTooltip);
	}
	
	public int armsSize() {
		switch (diplomacy.ss.g.currentGUIScale) {
			case SMALL: return 32;
			case MEDIUM: return 64;
			case LARGE: return 96;
			default: return 64;
		}
	}
	
	private final ScrollBar.ScrollElementAdapter<Object> offerOptionsAdapter = new ScrollBar.ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int relsH = 0;
			Empire me = diplomacy.ss.w.player;
			Empire them = offer.rel.other(me);
			Relationship.Level[] lvls = Relationship.Level.values();
			if (!isCeasefireOffer()) {
				for (int i = 0; i < lvls.length; i++) {
					Relationship.Level newLevel = lvls[i];
					if (newLevel == Relationship.Level.WAR) { continue; }
					if (offer.newLevel != null && offer.newLevel == newLevel) { continue; }
					if (offer.rel.level == newLevel) { continue; }
					String name;
					if (newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && offer.rel.level.ordinal() > newLevel.ordinal()) {
						name = _t("downgrade_" + offer.rel.level + "_to_" + newLevel);
					} else {
						name = _t("relationship_" + newLevel);
					}
					relsH += optionH(d, name, availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
				}
			}
			if (!offer.rel.getSendingTribute(me) && !offer.getSendingTribute(me) && !isUltimatum()) {
				relsH += optionH(d, _t("Offer_Tribute"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.getReceivingTribute(me) && !offer.getReceivingTribute(me)) {
				relsH += optionH(d, _t("Demand_Tribute"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.getSendingTribute(me) && offer.newTribute != Relationship.Direction.NEITHER) {
				relsH += optionH(d, _t("Stop_Paying_Tribute"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.getReceivingTribute(me) && offer.newTribute != Relationship.Direction.NEITHER && !isUltimatum()) {
				relsH += optionH(d, _t("Stop_Receiving_Tribute"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.tradeTreaty && offer.newTradeTreaty == null) {
				relsH += optionH(d, _t("Trade_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.tradeTreaty && offer.newTradeTreaty == null) {
				relsH += optionH(d, _t("cancel_Trade_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.researchTreaty && offer.newResearchTreaty == null) {
				relsH += optionH(d, _t("Research_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.researchTreaty && offer.newResearchTreaty == null) {
				relsH += optionH(d, _t("cancel_Research_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.moneyTransferAToB == 0) {
				int sendAmt = Relationship.giveMoneyAmount(me, them, diplomacy.ss.w.map);
				relsH += optionH(d, _t("Send_x_Money_short", sendAmt), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
				int getAmt = Relationship.giveMoneyAmount(them, me, diplomacy.ss.w.map);
				relsH += optionH(d, _t("Receive_x_Money_short", getAmt), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (diplomacy.ss.w.has(ConquestToggle.REPUTATION) && offer.submissionAToB == null) {
				relsH += optionH(d, _t("Display_of_Submission_short"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
				relsH += optionH(d, _t("Receive_Submission_short"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.annexationAToB == null && offer.rel.canAnnex(them) && diplomacy.ss.w.isMultiplayer()) {
				relsH += optionH(d, _t("Become_Annexed"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.annexationAToB == null && offer.rel.canAnnex(me)) {
				relsH += optionH(d, _t("Do_Annex"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			int meCityTransfers = 0;
			for (int i = 0; i < offer.cityTransfers.size(); i++) {
				if (offer.cityTransfers.get(i).aToB == (me == offer.rel.a)) {
					meCityTransfers++;
				}
			}
			int themCityTransfers = offer.cityTransfers.size() - meCityTransfers;
			if (meCityTransfers < me.cities.size() - 1) {
				relsH += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (themCityTransfers < them.cities.size() - 1) {
				relsH += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			return relsH == 0 ? 0 : relsH - MyDraw.BUTTON_SPACING;
		}

		@Override
		public void draw(Object t, MyDraw d, int x2, int y2, int colW) {
			Empire me = diplomacy.ss.w.player;
			Empire other = offer.rel.other(diplomacy.ss.w.player);
			Relationship.Level[] lvls = Relationship.Level.values();
			if (!isCeasefireOffer()) {
				for (int i = 0; i < lvls.length; i++) {
					Relationship.Level newLevel = lvls[i];
					if (newLevel == Relationship.Level.WAR) { continue; }
					if (offer.newLevel != null && offer.newLevel == newLevel) { continue; }
					if (offer.rel.level == newLevel) { continue; }
					Offer o2 = offer.clone();
					o2.newLevel = newLevel;
					String aiEvaluationTooltip = null;
					int aiEvaluation = 0;
					if (!other.playerControlled && newLevel != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.newLevelQuality(other, me, newLevel, offer.rel, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
					String name;
					if (newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && offer.rel.level.ordinal() > newLevel.ordinal()) {
						name = _t("downgrade_" + offer.rel.level + "_to_" + newLevel);
					} else {
						name = _t("relationship_" + newLevel);
					}
					int optionH = optionH(d, name, colW, !other.playerControlled);
					DiplomacyWindow.levelTooltip(newLevel, me, other, d, x2, y2, colW, optionH, diplomacy.ss);
					drawOption(true, d, newLevel.icon, false, name, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
					if (newLevel == Relationship.Level.NON_AGGRESSION_PACT) {
						d.highlight("addNonAggressionPact", diplomacy.ss.g, x2, y2, colW, optionH);
					}
					y2 += optionH + MyDraw.BUTTON_SPACING;
				}
			}
			if (!offer.rel.getSendingTribute(me) && !offer.getSendingTribute(me) && !isUltimatum()) {
				Offer o2 = offer.clone();
				o2.setSendingTribute(me, true);
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.payTributeTooltip(other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.payingTribute, false, _t("Offer_Tribute"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Offer_Tribute"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.getReceivingTribute(me) && !offer.getReceivingTribute(me)) {
				Offer o2 = offer.clone();
				o2.setSendingTribute(other, true);
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.giveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.receivingTribute, false, _t("Demand_Tribute"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				d.highlight("addDemandTribute", diplomacy.ss.g, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += optionH(d, _t("Demand_Tribute"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.getSendingTribute(me) && offer.newTribute != Relationship.Direction.NEITHER) {
				Offer o2 = offer.clone();
				o2.newTribute = Relationship.Direction.NEITHER;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.payTributeTooltip(other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.payingTribute, true, _t("Stop_Paying_Tribute"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Stop_Paying_Tribute"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.getReceivingTribute(me) && offer.newTribute != Relationship.Direction.NEITHER && !isUltimatum()) {
				Offer o2 = offer.clone();
				o2.newTribute = Relationship.Direction.NEITHER;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.giveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.receivingTribute, true, _t("Stop_Receiving_Tribute"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Stop_Receiving_Tribute"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.tradeTreaty && offer.newTradeTreaty == null) {
				Offer o2 = offer.clone();
				o2.newTradeTreaty = true;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.tradeTreatyTooltip(me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.tradeTreaty, false, _t("Trade_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				d.highlight("addTrade", diplomacy.ss.g, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += optionH(d, _t("Trade_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.tradeTreaty && offer.newTradeTreaty == null) {
				Offer o2 = offer.clone();
				o2.newTradeTreaty = false;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.tradeTreatyTooltip(me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.tradeTreaty, true, _t("cancel_Trade_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("cancel_Trade_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (!offer.rel.researchTreaty && offer.newResearchTreaty == null) {
				Offer o2 = offer.clone();
				o2.newResearchTreaty = true;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.researchTreatyTooltip(me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.researchTreaty, false, _t("Research_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Research_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.researchTreaty && offer.newResearchTreaty == null) {
				Offer o2 = offer.clone();
				o2.newResearchTreaty = false;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.researchTreatyTooltip(me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.researchTreaty, true, _t("cancel_Research_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("cancel_Research_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.moneyTransferAToB == 0) {
				Offer o2 = offer.clone();
				int amt = Relationship.giveMoneyAmount(me, other, diplomacy.ss.w.map);
				o2.moneyTransferAToB = (me == offer.rel.a ? 1 : -1) * amt;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.receiveMoneyQuality(amt, other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.moneyTransferTooltip(d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.sendMoney, false, _t("Send_x_Money_short", amt), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Send_x_Money_short", amt), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.moneyTransferAToB == 0) {
				Offer o2 = offer.clone();
				int amt = Relationship.giveMoneyAmount(other, me, diplomacy.ss.w.map);
				o2.moneyTransferAToB = (me == offer.rel.a ? -1 : 1) * amt;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.giveMoneyQuality(amt, other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.moneyTransferTooltip(d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.receiveMoney, false, _t("Receive_x_Money_short", amt), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Receive_x_Money_short", amt), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (diplomacy.ss.w.has(ConquestToggle.REPUTATION) && offer.submissionAToB == null) {
				Offer o2 = offer.clone();
				o2.submissionAToB = me == offer.rel.a;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.receiveSubmissionQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.submissionTooltip(true, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.sendSubmission, false, _t("Display_of_Submission_short"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Display_of_Submission_short"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (diplomacy.ss.w.has(ConquestToggle.REPUTATION) && offer.submissionAToB == null) {
				Offer o2 = offer.clone();
				o2.submissionAToB = me != offer.rel.a;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.giveSubmissionQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.submissionTooltip(false, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.receiveSubmission, false, _t("Receive_Submission_short"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Receive_Submission_short"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			
			if (offer.annexationAToB == null && offer.rel.canAnnex(other) && diplomacy.ss.w.isMultiplayer()) {
				Offer o2 = offer.clone();
				o2.annexationAToB = me == offer.rel.a;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.doAnnexQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.annexationTooltip(true, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.becomeAnnexed, false, _t("Become_Annexed"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Become_Annexed"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.annexationAToB == null && offer.rel.canAnnex(me)) {
				Offer o2 = offer.clone();
				o2.annexationAToB = me != offer.rel.a;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.becomeAnnexedQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.annexationTooltip(false, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.doAnnex, false, _t("Do_Annex"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t("Do_Annex"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			
			int meCityTransfers = 0;
			for (int i = 0; i < offer.cityTransfers.size(); i++) {
				if (offer.cityTransfers.get(i).aToB == (me == offer.rel.a)) {
					meCityTransfers++;
				}
			}
			int themCityTransfers = offer.cityTransfers.size() - meCityTransfers;
			if (meCityTransfers < me.cities.size() - 1) {
				DiplomacyWindow.cityTransferTooltip(true, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.sendCity, false, _t("offer_city_"), x2, y2, colW, new Runnable() {
					@Override
					public void run() {
						demandCity = false;
						diplomacy.ss.selectCityForOfferEditor = editor;
					}
				}, offer.annexationAToB == null);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (themCityTransfers < other.cities.size() - 1) {
				DiplomacyWindow.cityTransferTooltip(true, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(true, d, diplomacy.sendCity, false, _t("demand_city_"), x2, y2, colW, new Runnable() {
					@Override
					public void run() {
						demandCity = true;
						diplomacy.ss.selectCityForOfferEditor = editor;
					}
				}, offer.annexationAToB == null);
				d.highlight("demandCity", diplomacy.ss.g, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
		}
	};
	
	private final ScrollBar.ScrollElementAdapter<Object> offerAdapter = new ScrollBar.ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int relsH = 0;
			Empire me = diplomacy.ss.w.player;
			Empire them = offer.rel.other(me);
			if (offer.newLevel != null) {
				String name;
				if (offer.newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && offer.rel.level.ordinal() > offer.newLevel.ordinal()) {
					name = _t("downgrade_" + offer.rel.level + "_to_" + offer.newLevel);
				} else {
					name = _t("relationship_" + offer.newLevel);
				}
				relsH += optionH(d, name, availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			int n = 0;
			if (offer.newTribute != null) {
				String text;
				if (offer.newTribute == Relationship.Direction.NEITHER) {
					text = _t(offer.rel.getSendingTribute(me) ? "Stop_Paying_Tribute" : "Stop_Receiving_Tribute");
				} else if (offer.getSendingTribute(me)) {
					text = _t("Offer_Tribute");
				} else {
					text = _t("Demand_Tribute");
				}
				relsH += optionH(d, text, availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.newTradeTreaty != null) {
				relsH += optionH(d, _t(offer.newTradeTreaty ? "Trade_Treaty" : "cancel_Trade_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.newResearchTreaty != null) {
				relsH += optionH(d, _t(offer.newResearchTreaty ? "Research_Treaty" : "cancel_Research_Treaty"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.moneyTransferAToB != 0) {
				boolean meSendsMoney = offer.moneyTransferAToB > 0 == (me == offer.rel.a);
				relsH += optionH(d, _t(meSendsMoney ? "Send_x_Money" : "Receive_x_Money", StrictMath.abs(offer.moneyTransferAToB)), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.submissionAToB != null) {
				boolean meSubmits = offer.submissionAToB == (me == offer.rel.a);
				relsH += optionH(d, _t(meSubmits ? "Display_of_Submission" : "Receive_Submission"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.annexationAToB != null) {
				boolean meIsAnnexed = offer.annexationAToB == (me == offer.rel.a);
				relsH += optionH(d, _t(meIsAnnexed ? "Become_Annexed" : "Do_Annex"), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			for (Relationship.CityTransfer ct : offer.cityTransfers) {
				boolean meSends = ct.aToB == (me == offer.rel.a);
				relsH += optionH(d, _t(meSends ? "Give_city_x" : "Receive_city_x", ct.city.name), availableWidth, !them.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			n += offer.cityTransfers.size();
			if (showTerritoryLossMalus()) {
				n++;
			}
			return n == 0 && relsH == 0 ? 0 : relsH + (n * (MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING + MyDraw.UI_SPACING + cascadeSummarizer.getHeight(offer, d, availableWidth));
		}

		@Override
		public void draw(Object t, MyDraw d, int x2, int y2, int colW) {
			Empire me = diplomacy.ss.w.player;
			Empire other = offer.rel.other(diplomacy.ss.w.player);
			if (offer.newLevel != null) {
				Offer o2 = offer.clone();
				o2.newLevel = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled && offer.newLevel != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.newLevelQuality(other, me, offer.newLevel, offer.rel, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				String name;
				if (offer.newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && offer.rel.level.ordinal() > offer.newLevel.ordinal()) {
					name = _t("downgrade_" + offer.rel.level + "_to_" + offer.newLevel);
				} else {
					name = _t("relationship_" + offer.newLevel);
				}
				int optionH = optionH(d, name, colW, !other.playerControlled);
				DiplomacyWindow.levelTooltip(offer.newLevel, me, other, d, x2, y2, colW, optionH, diplomacy.ss);
				drawOption(false, d, offer.newLevel.icon, false, name, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH + MyDraw.BUTTON_SPACING;
			}
			if (offer.newTribute != null) {
				Offer o2 = offer.clone();
				o2.newTribute = null;
				Img icon;
				String text;
				boolean crossed;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (offer.newTribute == Relationship.Direction.NEITHER) {
					icon = offer.rel.getSendingTribute(me) ? diplomacy.payingTribute : diplomacy.receivingTribute;
					crossed = true;
					text = _t(offer.rel.getSendingTribute(me) ? "Stop_Paying_Tribute" : "Stop_Receiving_Tribute");
					if (offer.rel.getSendingTribute(me)) {
						if (!other.playerControlled) {
							StringBuilder sb = new StringBuilder();
							sb.append(_t("ai_diplo_quality")).append("\n");
							aiEvaluation = -other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
							sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
							sb.append("\n\n").append(_t("ai_eval_help"));
							aiEvaluationTooltip = sb.toString();
						}
						DiplomacyWindow.payTributeTooltip(offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
					} else {
						if (!other.playerControlled) {
							StringBuilder sb = new StringBuilder();
							sb.append(_t("ai_diplo_quality")).append("\n");
							aiEvaluation = -other.diplomacyAI.giveTributeQuality(other, me, diplomacy.ss.w.map, sb);
							sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
							sb.append("\n\n").append(_t("ai_eval_help"));
							aiEvaluationTooltip = sb.toString();
						}
						DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
					}
				} else if (offer.getSendingTribute(me)) {
					icon = diplomacy.payingTribute;
					crossed = false;
					text = _t("Offer_Tribute");
					if (!other.playerControlled) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
					DiplomacyWindow.payTributeTooltip(offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				} else {
					icon = diplomacy.receivingTribute;
					crossed = false;
					text = _t("Demand_Tribute");
					if (!other.playerControlled) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.giveTributeQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
					DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				}
				drawOption(false, d, icon, crossed, text, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, text, colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.newTradeTreaty != null) {
				Offer o2 = offer.clone();
				o2.newTradeTreaty = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					if (offer.newTradeTreaty) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = -other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.tradeTreatyTooltip(me, offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, diplomacy.tradeTreaty, !offer.newTradeTreaty, _t(offer.newTradeTreaty ? "Trade_Treaty" : "cancel_Trade_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t(offer.newTradeTreaty ? "Trade_Treaty" : "cancel_Trade_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.newResearchTreaty != null) {
				Offer o2 = offer.clone();
				o2.newResearchTreaty = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					if (offer.newResearchTreaty) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = -other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.researchTreatyTooltip(me, offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, diplomacy.researchTreaty, !offer.newResearchTreaty, _t(offer.newResearchTreaty ? "Research_Treaty" : "cancel_Research_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t(offer.newResearchTreaty ? "Research_Treaty" : "cancel_Research_Treaty"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.moneyTransferAToB != 0) {
				Offer o2 = offer.clone();
				o2.moneyTransferAToB = 0;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				boolean meSendsMoney = offer.moneyTransferAToB > 0 == (me == offer.rel.a);
				if (!other.playerControlled) {
					if (meSendsMoney) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.receiveMoneyQuality(StrictMath.abs(offer.moneyTransferAToB), other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.giveMoneyQuality(StrictMath.abs(offer.moneyTransferAToB), other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.moneyTransferTooltip(d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, meSendsMoney ? diplomacy.sendMoney : diplomacy.receiveMoney, false, _t(meSendsMoney ? "Send_x_Money" : "Receive_x_Money", StrictMath.abs(offer.moneyTransferAToB)), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t(meSendsMoney ? "Send_x_Money" : "Receive_x_Money", StrictMath.abs(offer.moneyTransferAToB)), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.submissionAToB != null) {
				Offer o2 = offer.clone();
				o2.submissionAToB = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				boolean meSubmits = offer.submissionAToB == (me == offer.rel.a);
				if (!other.playerControlled) {
					if (meSubmits) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.receiveSubmissionQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.giveSubmissionQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.submissionTooltip(meSubmits, meSubmits ? me : other, meSubmits ? other : me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, meSubmits ? diplomacy.sendSubmission : diplomacy.receiveSubmission, false, _t(meSubmits ? "Display_of_Submission" : "Receive_Submission"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t(meSubmits ? "Display_of_Submission" : "Receive_Submission"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (offer.annexationAToB != null) {
				Offer o2 = offer.clone();
				o2.annexationAToB = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				boolean meIsAnnexed = offer.annexationAToB == (me == offer.rel.a);
				if (!other.playerControlled) {
					if (meIsAnnexed) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.doAnnexQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.becomeAnnexedQuality(other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.annexationTooltip(meIsAnnexed, meIsAnnexed ? me : other, meIsAnnexed ? other : me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, meIsAnnexed ? diplomacy.becomeAnnexed : diplomacy.doAnnex, false, _t(meIsAnnexed ? "Become_Annexed" : "Do_Annex"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += optionH(d, _t(meIsAnnexed ? "Become_Annexed" : "Do_Annex"), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			for (Relationship.CityTransfer ct : offer.cityTransfers) {
				Offer o2 = offer.clone();
				o2.cityTransfers.remove(ct);
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				boolean meSends = ct.aToB == (me == offer.rel.a);
				if (!other.playerControlled) {
					if (meSends) {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.receiveCityQuality(ct.city, other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(_t("ai_diplo_quality")).append("\n");
						aiEvaluation = other.diplomacyAI.giveCityQuality(ct.city, other, me, diplomacy.ss.w.map, sb);
						sb.append("\n\n").append(_t("ai_eval_help"));
						aiEvaluationTooltip = sb.toString();
					}
				}
				DiplomacyWindow.cityTransferTooltip(meSends, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawOption(false, d, meSends ? diplomacy.sendCity : diplomacy.receiveCity, false, _t(meSends ? "Give_city_x" : "Receive_city_x", ct.city.name), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip, ct.city, diplomacy.ss);
				y2 += optionH(d, _t(meSends ? "Give_city_x" : "Receive_city_x", ct.city.name), colW, !other.playerControlled) + MyDraw.BUTTON_SPACING;
			}
			if (showTerritoryLossMalus()) {
				int q = isUltimatum() ? other.diplomacyAI.personality.territoryUltimatumMalus : other.diplomacyAI.personality.territoryLossMalus;
				int baseQ = isUltimatum() ? new DiplomacyPersonality().territoryUltimatumMalus : new DiplomacyPersonality().territoryLossMalus;
				StringBuilder sb = new StringBuilder();
				DiplomacyAI.exCmp(q, "demanding_territory", sb, baseQ, other.diplomacyAI.personality);
				drawItem(d, diplomacy.receiveCity, _t("demanding_territory"), x2, y2, colW, q, sb.toString());
			}
			y2 += MyDraw.UI_SPACING;
			cascadeSummarizer.draw(offer, d, x2, y2, colW);
		}
	};
	
	private final ScrollBar.ScrollElementAdapter<Object> relationshipAdapter = new ScrollBar.ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			Empire me = diplomacy.ss.w.player;
			int n = 1;
			if (offer.rel.getSendingTribute(me)) {
				n++; 
			} else if (offer.rel.getReceivingTribute(me)) {
				n++;
			}
			if (offer.rel.tradeTreaty) { n++; }
			if (offer.rel.researchTreaty) { n++; }
			return n == 0 ? 0 : n * (MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING;
		}

		@Override
		public void draw(Object t, MyDraw d, int x2, int y2, int colW) {
			Empire me = diplomacy.ss.w.player;
			Empire other = offer.rel.other(diplomacy.ss.w.player);
			drawItem(d, offer.rel.level.icon, _t("relationship_" + offer.rel.level.name()), x2, y2, colW);
			DiplomacyWindow.levelTooltip(offer.rel.level, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, diplomacy.ss);
			y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			if (offer.rel.getSendingTribute(me)) {
				drawItem(d, diplomacy.payingTribute, _t("Paying_Tribute"), x2, y2, colW);
				DiplomacyWindow.payTributeTooltip(offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			} else if (offer.rel.getReceivingTribute(me)) {
				drawItem(d, diplomacy.receivingTribute, _t("Receiving_Tribute"), x2, y2, colW);
				DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.tradeTreaty) {
				drawItem(d, diplomacy.tradeTreaty, _t("Trade_Treaty"), x2, y2, colW);
				DiplomacyWindow.tradeTreatyTooltip(me, offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (offer.rel.researchTreaty) {
				drawItem(d, diplomacy.researchTreaty, _t("Research_Treaty"), x2, y2, colW);
				DiplomacyWindow.researchTreatyTooltip(me, offer.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
		}
	};
	
	private final ScrollBar.ScrollElementAdapter<Object> threatOptionsAdapter = new ScrollBar.ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int n = 0;
			Relationship.Level[] lvls = Relationship.Level.values();
			Empire me = diplomacy.ss.w.player;
			for (int i = 0; i < lvls.length; i++) {
				Relationship.Level newLevel = lvls[i];
				if (newLevel == Relationship.Level.TRUCE) { continue; }
				if (threat.newLevel != null && threat.newLevel == newLevel) { continue; }
				if (threat.rel.level.ordinal() <= newLevel.ordinal()) { continue; }
				n++;
			}
			if (threat.rel.getSendingTribute(me) && threat.newTribute != Relationship.Direction.NEITHER) { n++; }
			if (threat.rel.tradeTreaty && threat.newTradeTreaty == null) { n++; }
			if (threat.rel.researchTreaty && threat.newResearchTreaty == null) { n++; }
			return n == 0 ? 0 : n * (MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING;
		}

		@Override
		public void draw(Object t, MyDraw d, int x2, int y2, int colW) {
			Relationship.Level[] lvls = Relationship.Level.values();
			Empire me = diplomacy.ss.w.player;
			Empire other = offer.rel.other(diplomacy.ss.w.player);
			for (int i = 0; i < lvls.length; i++) {
				Relationship.Level newLevel = lvls[i];
				if (newLevel == Relationship.Level.TRUCE) { continue; }
				if (threat.newLevel != null && threat.newLevel == newLevel) { continue; }
				if (threat.rel.level.ordinal() <= newLevel.ordinal()) { continue; }
				Offer o2 = threat.clone();
				o2.newLevel = newLevel;
				if (newLevel == Relationship.Level.WAR) {
					o2.setToWar();
				}
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled && newLevel != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.newLevelQuality(other, me, newLevel, offer.rel, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.levelTooltip(newLevel, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, diplomacy.ss);
				String name;
				if (newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && threat.rel.level.ordinal() > newLevel.ordinal()) {
					name = _t("downgrade_" + threat.rel.level + "_to_" + newLevel);
				} else {
					name = _t("relationship_" + newLevel);
				}
				drawThreatOption(true, d, newLevel.icon, false, name, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				d.highlight("addThreat-" + newLevel.name(), diplomacy.ss.g, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.rel.getSendingTribute(me) && threat.newTribute != Relationship.Direction.NEITHER) {
				Offer o2 = threat.clone();
				o2.newTribute = Relationship.Direction.NEITHER;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.payTributeTooltip(threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawThreatOption(true, d, diplomacy.payingTribute, true, _t("Stop_Paying_Tribute"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.rel.tradeTreaty && threat.newTradeTreaty == null) {
				Offer o2 = threat.clone();
				o2.newTradeTreaty = false;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.tradeTreatyTooltip(me, threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawThreatOption(true, d, diplomacy.tradeTreaty, true, _t("cancel_Trade_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.rel.researchTreaty && threat.newResearchTreaty == null) {
				Offer o2 = threat.clone();
				o2.newResearchTreaty = false;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.researchTreatyTooltip(me, threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawThreatOption(true, d, diplomacy.researchTreaty, true, _t("cancel_Research_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
		}
	};
	
	private final ScrollBar.ScrollElementAdapter<Object> threatAdapter = new ScrollBar.ScrollElementAdapter<Object>() {
		@Override
		public int getHeight(Object t, MyDraw d, int availableWidth) {
			int n = 0;
			Empire me = diplomacy.ss.w.player;
			if (threat.newLevel != null && threat.newLevel != threat.rel.level) { n++; }
			if (threat.newTribute != null && threat.newTribute != threat.rel.tribute) { n++; }
			if (threat.newTradeTreaty != null && threat.newTradeTreaty != threat.rel.tradeTreaty) { n++; }
			if (threat.newResearchTreaty != null && threat.newResearchTreaty != threat.rel.researchTreaty) { n++; }
			return n == 0 ? 0 : (n * (MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING) - MyDraw.BUTTON_SPACING + MyDraw.UI_SPACING + threatCascadeSummarizer.getHeight(offer, d, availableWidth));
		}

		@Override
		public void draw(Object t, MyDraw d, int x2, int y2, int colW) {
			Empire me = diplomacy.ss.w.player;
			Empire other = offer.rel.other(diplomacy.ss.w.player);
			if (threat.newLevel != null && threat.newLevel != threat.rel.level) {
				Offer o2 = threat.clone();
				o2.newLevel = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled && threat.newLevel != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = other.diplomacyAI.newLevelQuality(other, me, threat.newLevel, offer.rel, diplomacy.ss.w.map, sb);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.levelTooltip(threat.newLevel, me, other, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2, diplomacy.ss);
				String name;
				if (threat.newLevel.ordinal() >= Relationship.Level.PEACE.ordinal() && threat.rel.level.ordinal() > threat.newLevel.ordinal()) {
					name = _t("downgrade_" + threat.rel.level + "_to_" + threat.newLevel);
				} else {
					name = _t("relationship_" + threat.newLevel);
				}
				drawThreatOption(false, d, threat.newLevel.icon, false, name, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.newTribute != null && threat.newTribute != threat.rel.tribute) {
				Offer o2 = threat.clone();
				o2.newTribute = null;
				Img icon;
				String text;
				boolean crossed;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.receiveTributeQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				if (threat.newTribute == Relationship.Direction.NEITHER) {
					icon = threat.rel.getSendingTribute(me) ? diplomacy.payingTribute : diplomacy.receivingTribute;
					crossed = true;
					text = _t(threat.rel.getSendingTribute(me) ? "Stop_Paying_Tribute" : "Stop_Receiving_Tribute");
					if (threat.rel.getSendingTribute(me)) {
						DiplomacyWindow.payTributeTooltip(threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
					} else {
						DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
					}
				} else if (threat.getSendingTribute(me)) {
					icon = diplomacy.payingTribute;
					crossed = false;
					text = _t("Offer_Tribute");
					DiplomacyWindow.payTributeTooltip(threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				} else {
					icon = diplomacy.receivingTribute;
					crossed = false;
					text = _t("Demand_Tribute");
					DiplomacyWindow.receiveTributeTooltip(me, d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				}
				drawThreatOption(false, d, icon, crossed, text, o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.newTradeTreaty != null && threat.newTradeTreaty != threat.rel.tradeTreaty) {
				Offer o2 = threat.clone();
				o2.newTradeTreaty = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.tradeTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.tradeTreatyTooltip(me, threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawThreatOption(false, d, diplomacy.tradeTreaty, !threat.newTradeTreaty, _t(threat.newTradeTreaty ? "Trade_Treaty" : "cancel_Trade_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			if (threat.newResearchTreaty != null && threat.newResearchTreaty != threat.rel.researchTreaty) {
				Offer o2 = threat.clone();
				o2.newResearchTreaty = null;
				String aiEvaluationTooltip = null;
				int aiEvaluation = 0;
				if (!other.playerControlled) {
					StringBuilder sb = new StringBuilder();
					sb.append(_t("ai_diplo_quality")).append("\n");
					aiEvaluation = -other.diplomacyAI.researchTreatyQuality(other, me, diplomacy.ss.w.map, sb);
					sb.append("\n").append(_t("cancellation_")).append(aiEvaluation);
					sb.append("\n\n").append(_t("ai_eval_help"));
					aiEvaluationTooltip = sb.toString();
				}
				DiplomacyWindow.researchTreatyTooltip(me, threat.rel.other(me), d, x2, y2, colW, MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2);
				drawThreatOption(false, d, diplomacy.researchTreaty, !threat.newResearchTreaty, _t(threat.newResearchTreaty ? "Research_Treaty" : "cancel_Research_Treaty"), o2, x2, y2, colW, aiEvaluation, aiEvaluationTooltip);
				y2 += MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING;
			}
			y2 += MyDraw.UI_SPACING;
			threatCascadeSummarizer.draw(threat, d, x2, y2, colW);
		}
	};
	
	private boolean showTerritoryLossMalus() {
		Empire player = diplomacy.ss.w.player;
		Empire other = offer.rel.other(diplomacy.ss.w.player);
		int playerTerritoryLosses = 0;
		int otherTerritoryLosses = 0;
		for (Relationship.CityTransfer ct : offer.cityTransfers) {
			if (ct.aToB == (player == offer.rel.a)) {
				playerTerritoryLosses++;
			} else {
				otherTerritoryLosses++;
			}
		}
		return playerTerritoryLosses < otherTerritoryLosses && !other.playerControlled;
	}
	
	public void render(final MyDraw d, ScreenMode sm, Pt cursor) {
		Empire me = diplomacy.ss.w.player;
		Empire other = offer.rel.other(diplomacy.ss.w.player);
		int maxItemW = 0;
		Relationship.Level[] lvls = Relationship.Level.values();
		for (int i = 0; i < lvls.length; i++) {
			maxItemW = Math.max(maxItemW, (int) d.textSize(_t("relationship_" + lvls[i].name()), AGame.FOUNT).x);
			// Makes it too wide, oops
			/*if (i >= Relationship.Level.NON_AGGRESSION_PACT.ordinal()) {
				for (int j = Relationship.Level.PEACE.ordinal(); j < i; j++) {
					maxItemW = Math.max(maxItemW, (int) d.textSize(_t("downgrade_" + lvls[i].name() + "_to_" + lvls[j].name()), AGame.FOUNT).x);
					System.out.println(_t("downgrade_" + lvls[i].name() + "_to_" + lvls[j].name()));
				}
			}*/
		}
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Trade_Treaty"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Research_Treaty"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("cancel_Trade_Treaty"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("cancel_Research_Treaty"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Demand_Tribute"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Offer_Tribute"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("cancel_tribute"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Display_of_Submission"), AGame.BIG_FOUNT).x);
		maxItemW = Math.max(maxItemW, (int) d.textSize(_t("Receive_Submission"), AGame.BIG_FOUNT).x);
		maxItemW += (int) d.textSize("+99", AGame.FOUNT).x + MyDraw.UI_SPACING; // for AI evals
		int colW = ICON_SIZE + MyDraw.UI_SPACING + maxItemW + MyDraw.UI_SPACING + MyDraw.ICON_BUTTON_SZ + MyDraw.PANEL_INSET * 2;
		int colAndScrollW = colW + ScrollBar.SCROLL_BAR_W;
		
		int colH = (MyDraw.BUTTON_H + MyDraw.PANEL_INSET * 2 + MyDraw.BUTTON_SPACING) * (isUltimatum() ? 10 : 15);
		int h;
		
		int maxH = sm.height - MyDraw.SIDE_CLEARANCE * 2 - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING;
		int maxY = sm.height - MyDraw.SIDE_CLEARANCE;
		if (!diplomacy.ss.w.isMultiplayer() && (diplomacy.ss.g.strategicHelp || diplomacy.ss.g.strategicHelpHeroesAndVillains) && ConquestHelpSystem.hasAny(diplomacy.ss, diplomacy.ss)) {
			int helpH = ConquestHelpSystem.getHeight(d, diplomacy.ss, diplomacy.ss, sm.width / 3);
			maxH = sm.height - MyDraw.SIDE_CLEARANCE * 2 - helpH - MyDraw.UI_SPACING - MyDraw.TOP_BAR_H - MyDraw.UI_SPACING;
			maxY = sm.height - MyDraw.SIDE_CLEARANCE - helpH - MyDraw.UI_SPACING;
		}
		
		String repInfo = "";
		int repInfoH = 0;
		if (isUltimatum()) {
			if (diplomacy.ss.w.has(ConquestToggle.REPUTATION)) {
				repInfo = _t("rep_info", threat.rel.cascadedForceRepCost(threat, me, diplomacy.ss.w.map), other.getName(), EmpireStat.FAILED_FALSE_ULTIMATUM_COST.explain(me.bonuses));
				repInfoH = (int) d.textSize(repInfo, AGame.FOUNT, 0, 0, colAndScrollW).height;
			}
			colH = Math.max(colH, repInfoH);
			h = Math.max(armsSize() + MyDraw.PANEL_BORDER_W, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + colH + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			h += MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + colH;
			if (h > maxH) {
				h = maxH;
				colH = (h
						- (Math.max(armsSize() + MyDraw.PANEL_BORDER_W, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2)
						- (MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING)
						)
						/ 2;
			}
		} else {
			h = Math.max(armsSize() + MyDraw.PANEL_BORDER_W, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + colH + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2;
			if (h > maxH) {
				h = maxH;
				colH = (h
						- (Math.max(armsSize() + MyDraw.PANEL_BORDER_W, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING + AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + MyDraw.UI_SPACING + MyDraw.BUTTON_H + MyDraw.WINDOW_INSET * 2)
						);
			}
		}
				
		String title;
		if (isUltimatum()) {
			title = _t("editing_ultimatum", other.getName());
		} else {
			title = _t(isCeasefireOffer() ? "negotiating_truce_with_x" : "negotiating_with_x", other.getName());
		}
		
		int w = Math.max((int) d.textSize(title, AGame.BIGGER_FOUNT).x + armsSize() + MyDraw.PANEL_BORDER_W + MyDraw.UI_SPACING, colAndScrollW * 3 + MyDraw.UI_SPACING * 2) + MyDraw.WINDOW_INSET * 2;
		if (w > sm.width - MyDraw.SIDE_CLEARANCE * 2) {
			w = sm.width - MyDraw.SIDE_CLEARANCE * 2;
			colAndScrollW = (sm.width - MyDraw.SIDE_CLEARANCE * 2 - MyDraw.UI_SPACING * 2 - MyDraw.WINDOW_INSET * 2) / 3;
			colW = colAndScrollW - ScrollBar.SCROLL_BAR_W;
		}
		int x = sm.width / 2 - w / 2;
		int y = Math.min(maxY - h, sm.height / 2 - h / 2);
		d.drawShadowedWindow(x, y, w, h, 23);
		x += MyDraw.WINDOW_INSET;
		y += MyDraw.WINDOW_INSET;
		w -= MyDraw.WINDOW_INSET * 2;
		h -= MyDraw.WINDOW_INSET * 2;
		d.drawPanel(x, y, armsSize() + MyDraw.PANEL_BORDER_W * 2, armsSize() + MyDraw.PANEL_BORDER_W * 2, 22);
		other.arms.draw(d, x + MyDraw.PANEL_BORDER_W, y + MyDraw.PANEL_BORDER_W, armsSize());
		d.text(title, AGame.BIGGER_FOUNT, x + armsSize() + MyDraw.PANEL_BORDER_W + MyDraw.UI_SPACING, y);
		if (other.playerControlled) {
			d.text(_t("player_controlled_empire") + ": " + other.getPlayerNames(), AGame.FOUNT, x + w - (int) d.textSize(_t("player_controlled_empire") + ": " + other.getPlayerNames(), AGame.FOUNT).x, y);
		} else {
			int tw = (int) d.textSize(_t("personality_" + other.diplomacyAI.personality.name), AGame.FOUNT).x;
			d.text(_t("personality_" + other.diplomacyAI.personality.name), AGame.FOUNT, x + w - tw, y);
			if (Lang.flavour()) {
				d.tooltip(x + w - tw, y, tw, AGame.FOUNT.lineHeight, _t("personality_desc_" + other.diplomacyAI.personality.name));
			}
		}
		y += Math.max(armsSize() + MyDraw.PANEL_BORDER_W, AGame.BIGGER_FOUNT.lineHeight) + MyDraw.UI_SPACING;
		int x2 = x;
		int y2 = y;
		d.text(_t(isUltimatum() ? "demand_options" : "negotiation_options"), AGame.BIG_FOUNT, x2, y2);
		y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
		
		offerOptionsSBRect.x = x2; offerOptionsSBRect.y = y2; offerOptionsSBRect.w = colAndScrollW; offerOptionsSBRect.h = colH;
		offerOptionsSB.draw(d, x2, y2, colAndScrollW, colH, Collections.singletonList(new Object()), offerOptionsAdapter);
		
		x2 = x + colAndScrollW + MyDraw.UI_SPACING;
		y2 = y;
		String offerText = _t(
				isUltimatum() ? "demands"
				: canAccept() ? "their_negotiation_offer": "negotiation_offer");
		d.text(offerText, AGame.BIG_FOUNT, x2, y2);
		y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
		
		offerSBRect.x = x2; offerSBRect.y = y2; offerSBRect.w = colAndScrollW; offerSBRect.h = colH;
		offerSB.draw(d, x2, y2, colAndScrollW, colH, Collections.singletonList(new Object()), offerAdapter);
		
		x2 = x + colAndScrollW * 2 + MyDraw.UI_SPACING * 2;
		y2 = y;
		d.text(_t("current_relationship"), AGame.BIG_FOUNT, x2, y2);
		y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
		
		relationshipSBRect.x = x2; relationshipSBRect.y = y2; relationshipSBRect.w = colAndScrollW; relationshipSBRect.h = colH;
		relationshipSB.draw(d, x2, y2, colAndScrollW, colH, Collections.singletonList(new Object()), relationshipAdapter);
		
		y += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + colH;
		
		if (isUltimatum()) {
			y += MyDraw.UI_SPACING;
			x2 = x;
			y2 = y;
			d.text(_t("threat_options"), AGame.BIG_FOUNT, x2, y2);
			y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
			
			threatOptionsSBRect.x = x2; threatOptionsSBRect.y = y2; threatOptionsSBRect.w = colAndScrollW; threatOptionsSBRect.h = colH;
			threatOptionsSB.draw(d, x2, y2, colAndScrollW, colH, Collections.singletonList(new Object()), threatOptionsAdapter);
			
			x2 = x + colAndScrollW + MyDraw.UI_SPACING;
			y2 = y;
			d.text(_t("threats"), AGame.BIG_FOUNT, x2, y2);
			y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
			
			threatSBRect.x = x2; threatSBRect.y = y2; threatSBRect.w = colAndScrollW; threatSBRect.h = colH;
			threatSB.draw(d, x2, y2, colAndScrollW, colH, Collections.singletonList(new Object()), threatAdapter);
			
			if (diplomacy.ss.w.has(ConquestToggle.REPUTATION)) {
				x2 = x + colAndScrollW * 2 + MyDraw.UI_SPACING * 2;
				y2 = y;
				d.text(_t("Reputation_Cost_") + " " + threat.rel.cascadedForceRepCost(threat, me, diplomacy.ss.w.map), AGame.BIG_FOUNT, x2, y2);
				y2 += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING;
				d.text(repInfo, AGame.FOUNT, x2, y2, colAndScrollW);
			}
			
			y += AGame.BIG_FOUNT.lineHeight + MyDraw.UI_SPACING + colH;
		}
		
		y2 = y + MyDraw.UI_SPACING;
		x2 = x + w;
		String acceptText = _t(isUltimatum() ? "Make_Ultimatum" : canAccept() ? "Accept_Offer" : "Make_Offer"); 
		int bw = d.bw(acceptText);
		d.button(x2 - bw, y2, bw, acceptText, null, new Runnable() {
			@Override
			public void run() {
				confirm();
			}
		}, canConfirm());
		d.highlight("makeOffer", diplomacy.ss.g, x2 - bw, y2, bw, MyDraw.BUTTON_H);
		String invalidReason = cannotConfirmReason();
		if (invalidReason != null) {
			d.tooltip(x2 - bw, y2, bw, MyDraw.BUTTON_H, invalidReason);
		} else if (isUltimatum()) {
			String tt = "";
			String abandonHeroInfo = me.getRepChangeHeroAppendix(-CANCELLED_ULTIMATUM_COST.get(me.bonuses), diplomacy.ss.w.map, false);
			if (!abandonHeroInfo.isEmpty()) {
				tt = _t("if_abandon_ult_header") + abandonHeroInfo;
			}
			String forceHeroInfo = me.getRepChangeHeroAppendix(-threat.rel.cascadedForceRepCost(threat, me, diplomacy.ss.w.map), diplomacy.ss.w.map, false);
			if (!forceHeroInfo.isEmpty()) {
				if (!tt.isEmpty()) { tt += "\n"; }
				tt += _t("if_enforce_ult_header") + forceHeroInfo;
			}
			String backDownHeroInfo = me.getRepChangeHeroAppendix(-EmpireStat.FAILED_FALSE_ULTIMATUM_COST.get(me.bonuses), diplomacy.ss.w.map, false);
			if (!backDownHeroInfo.isEmpty()) {
				if (!tt.isEmpty()) { tt += "\n"; }
				tt += _t("if_back_down_ult_header") + backDownHeroInfo;
			}
			if (!tt.isEmpty()) {
				d.tooltip(x2 - bw, y2, bw, MyDraw.BUTTON_H, tt);
			}
		}
		x2 -= bw + MyDraw.BUTTON_SPACING;
		bw = d.bw(_t("Cancel"));
		d.button(x2 - bw, y2, bw, _t("Cancel"), null, new Runnable() {
			@Override
			public void run() {
				diplomacy.offerEditor = null;
				diplomacy.removeNoticeOnPopupOK = null;
			}
		});
		x2 -= bw;
		
		String qualityText = "";
		String qualityTooltip = "";
		if (isUltimatum() && !other.playerControlled) {
			StringBuilder sb = new StringBuilder();
			sb.append(_t("ai_ult_response_explanation")).append("\n\n");
			sb.append(_t("demands"));
			int demandQuality = other.diplomacyAI.evaluateOffer(offer, other, me, diplomacy.ss.w.map, false, sb, /* considerWarQuality */ true, /* ultimatumDemand */ true);
			sb.append("\n").append(_t("quality_total")).append(demandQuality);
			sb.append("\n\n").append(_t("threats"));
			int threatQuality = other.diplomacyAI.evaluateOffer(threat, other, me, diplomacy.ss.w.map, false, sb);
			sb.append("\n").append(_t("quality_total")).append(threatQuality);
			sb.append("\n\n").append(_t("quality_difference_")).append(demandQuality - threatQuality);
			Ultimatum ult = new Ultimatum(diplomacy.ss.w.map, offer.rel, me, offer, threat, 100);
			int defacto = 0;
			if (ult.isDeFactoWarDeclaration()) {
				sb.append("\n").append(_t("de_facto_war_declaration_")).append("-999");
				defacto = -999;
			}
			if (other.diplomacyAI.personality.agreeToUltimatumBaseline != 0) {
				sb.append("\n").append(_t("agreeToUltimatumBaseline_")).append(other.diplomacyAI.personality.agreeToUltimatumBaseline > 0 ? "+" : "").append(other.diplomacyAI.personality.agreeToUltimatumBaseline);
				
			}
			if (defacto != 0 || other.diplomacyAI.personality.agreeToUltimatumBaseline != 0) {
				sb.append("\n").append(_t("quality_total")).append(demandQuality - threatQuality + other.diplomacyAI.personality.agreeToUltimatumBaseline + defacto);
			}
			int acceptLikelihoodPercent = (int) ((demandQuality - threatQuality + other.diplomacyAI.personality.agreeToUltimatumBaseline + defacto) * other.diplomacyAI.personality.ultimatumQualityDifferenceToLikelihoodPercent);
			sb.append("\n").append(_t("ai_ult_acceptance_probability_multiplier", other.diplomacyAI.personality.ultimatumQualityDifferenceToLikelihoodPercent));
			if (other.diplomacyAI.accedeToUltimatumCooldown > 0) {
				acceptLikelihoodPercent = Math.max(0, acceptLikelihoodPercent);
				sb.append("\n").append(_t("ai_ult_acceptance_probability_percent", acceptLikelihoodPercent));
				sb.append("\n").append(_t("ai_ult_recent_ultimatum_penalty", other.diplomacyAI.personality.recentAccededToUltimatumPenalty));
				acceptLikelihoodPercent -= other.diplomacyAI.personality.recentAccededToUltimatumPenalty;
			}
			acceptLikelihoodPercent = Math.max(0, Math.min(100, acceptLikelihoodPercent));
			sb.append("\n\n").append(_t("ai_ult_acceptance_probability_percent", acceptLikelihoodPercent));
			qualityTooltip = sb.toString();
			if (acceptLikelihoodPercent == 0) {
				qualityText = MyDraw.ERROR_C + acceptLikelihoodPercent + "%";
			} else {
				qualityText = acceptLikelihoodPercent + "%";
			}
			qualityText = _t("ai_ult_acceptance_chance_", other.getName()) + qualityText;
		} else if (!canAccept() && !other.playerControlled) {
			StringBuilder sb = new StringBuilder();
			int quality = other.diplomacyAI.evaluateOffer(offer, other, me, diplomacy.ss.w.map, false, sb);
			if (quality > 0) {
				qualityText = "+" + quality;
			} else {
				qualityText = MyDraw.ERROR_C + quality;
			}
			qualityText = other.getName() + ": " + qualityText;
			qualityTooltip = _t("ai_diplo_quality") + "\n" + sb.toString() + "\n" + _t("quality_total") + quality + "\n\n" + _t("ai_eval_help");
		}
		int qWidth = (int) d.textSize(qualityText, AGame.BIG_FOUNT).x;
		d.text(qualityText, AGame.BIG_FOUNT, x2 - qWidth - MyDraw.UI_SPACING, y2);
		d.tooltip(x2 - qWidth - MyDraw.UI_SPACING, y2, qWidth, AGame.BIG_FOUNT.lineHeight, qualityTooltip);
	}
	
	private boolean canConfirm() {
		if (offer.isEmpty()) { return false; }
		if (threat != null) {
			if (threat.isEmpty()) { return false; }
		}
		return cannotConfirmReason() == null;
	}
	
	private String cannotConfirmReason() {
		CampaignWorld w = diplomacy.ss.w;
		WorldMap m = w.map;
		Empire me = w.player;
		Empire other = offer.rel.other(diplomacy.ss.w.player);
		String invalidReason = offer.rel.getInvalidReason(offer, null, m, isUltimatum());
		if (threat != null && w.has(ConquestToggle.REPUTATION) && me.getReputation() < EmpireStat.MIN_ULTIMATUM_REPUTATION.get(me.bonuses)) {
			invalidReason = _t("min_ult_message", EmpireStat.MIN_ULTIMATUM_REPUTATION.get(me.bonuses));
		} else if (invalidReason == null && threat != null && !threat.rel.isValid(threat, me, m, false)) {
			invalidReason = _t("Ultimatum_Threat_") + " " + _t(threat.rel.getInvalidReason(threat, me, m, false));
		} else if (invalidReason != null) {
			invalidReason = _t(invalidReason);
		} else if (threat != null && Relationship.demandAndThreatOverlap(offer, threat)) {
			invalidReason = _t("ultimatum_demand_and_threat_overlap");
		}
		if (invalidReason == null && !isUltimatum() && !other.playerControlled && !w.compliantAICheat) {
			StringBuilder sb = new StringBuilder();
			int total = other.diplomacyAI.evaluateOffer(offer, other, me, diplomacy.ss.w.map, false, sb);
			if (total <= 0) {
				invalidReason = _t("x_will_not_accept_this_offer", other.getName()) + "\n\n" + sb + "\n" + _t("quality_total") + total;
			}
		}
		return invalidReason;
	}
	
	private boolean canAccept() {
		return offer.rel.isOffered(diplomacy.ss.w.player, diplomacy.ss.w.map) && offer.equals(offer.rel.getOffer(diplomacy.ss.w.map));
	}
	
	private void confirm() {
		diplomacy.waitForMessageArrival = diplomacy.ss.w.player.id + "_" + DiplomacyWindow.diplomacyMessageCounter++;
		if (isUltimatum()) {
			Ultimatum ult = new Ultimatum(diplomacy.ss.w.map, offer.rel, diplomacy.ss.w.player, offer, threat, Relationship.ULTIMATUM_TIME);
			diplomacy.ss.w.giveCommand(msg("makeUltimatum").put("ultimatum", ult.toJSON()).put("messageArrivedCallbackID", diplomacy.waitForMessageArrival));
		} else {
			if (canAccept()) {
				diplomacy.ss.w.checkAchievements(offer);
				diplomacy.ss.w.giveCommand(msg("acceptOffer").put("offer", offer.rel.getOffer(diplomacy.ss.w.map).toJSON()).put("messageArrivedCallbackID", diplomacy.waitForMessageArrival).put("player", diplomacy.ss.w.player.id));
			} else {
				if (!diplomacy.ss.w.isMultiplayer()) {
					diplomacy.waitForMessageArrival = null;
					diplomacy.ss.w.checkAchievements(offer);
					offer.rel.other(diplomacy.ss.w.player).diplomacyAI.quickProcessOffer(offer.rel.other(diplomacy.ss.w.player), offer, diplomacy.ss.w);
				} else {
					diplomacy.ss.w.giveCommand(msg("makeOffer").put("offer", offer.toJSON()).put("messageArrivedCallbackID", diplomacy.waitForMessageArrival).put("player", diplomacy.ss.w.player.id));
				}
			}
		}
		diplomacy.offerEditor = null;
		if (diplomacy.removeNoticeOnPopupOK != null) {
			diplomacy.ss.w.playerOffers.remove(diplomacy.removeNoticeOnPopupOK);
			diplomacy.removeNoticeOnPopupOK = null;
		}
	}
	
	public void input(Input in, MyDraw.State drawState, Pt cursor, Pt click, int ms) {
		WorldMap m = diplomacy.ss.w.map;
		offer.clean();
		if (!offer.rel.isValid(offer, null, m, isUltimatum())) {
			boolean wasTruce = offer.newLevel == Relationship.Level.TRUCE;
			offer = new Offer(offer.rel);
			if (wasTruce) {
				offer.newLevel = Relationship.Level.TRUCE;
			}
		}
		if (Keys.check(in, "ESCAPE")) {
			diplomacy.offerEditor = null;
			return;
		}
		if (canConfirm() && Keys.check(in, "ENTER")) {
			confirm();
			return;
		}
		
		offerOptionsSB.tick(in, offerOptionsSBRect.x, offerOptionsSBRect.y, offerOptionsSBRect.w, offerOptionsSBRect.h);
		offerSB.tick(in, offerSBRect.x, offerSBRect.y, offerSBRect.w, offerSBRect.h);
		relationshipSB.tick(in, relationshipSBRect.x, relationshipSBRect.y, relationshipSBRect.w, relationshipSBRect.h);
		if (isUltimatum()) {
			threatOptionsSB.tick(in, threatOptionsSBRect.x, threatOptionsSBRect.y, threatOptionsSBRect.w, threatOptionsSBRect.h);
			threatSB.tick(in, threatSBRect.x, threatSBRect.y, threatSBRect.w, threatSBRect.h);
		}
	}

	public void cede(City c) {
		offer.cityTransfers.add(new Relationship.CityTransfer(c, offer.rel.a == diplomacy.ss.w.player));
		diplomacy.ss.selectCityForOfferEditor = null;
	}
	
	public void demand(City c) {
		offer.cityTransfers.add(new Relationship.CityTransfer(c, offer.rel.a == offer.rel.other(diplomacy.ss.w.player)));
		diplomacy.ss.selectCityForOfferEditor = null;
	}
	
	public void cancelCitySelection() {
		diplomacy.ss.selectCityForOfferEditor = null;
	}
}
