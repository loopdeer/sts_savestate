package savestate.monsters.beyond;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.WrithingMass;
import com.megacrit.cardcrawl.relics.Omamori;
import savestate.SaveState;
import savestate.fastobjects.AnimationStateFast;
import savestate.monsters.Monster;
import savestate.monsters.MonsterState;

import static savestate.SaveStateMod.shouldGoFast;

public class WrithingMassState extends MonsterState {
    private final boolean firstMove;
    private final boolean usedMegaDebuff;

    public WrithingMassState(AbstractMonster monster) {
        super(monster);

        this.firstMove = ReflectionHacks.getPrivate(monster, WrithingMass.class, "firstMove");
        this.usedMegaDebuff = ReflectionHacks
                .getPrivate(monster, WrithingMass.class, "usedMegaDebuff");

        monsterTypeNumber = Monster.WRITHING_MASS.ordinal();
    }

    public WrithingMassState(String jsonString) {
        super(jsonString);

        // TODO don't parse twice
        JsonObject parsed = new JsonParser().parse(jsonString).getAsJsonObject();

        this.firstMove = parsed.get("first_move").getAsBoolean();
        this.usedMegaDebuff = parsed.get("used_mega_debuff").getAsBoolean();

        monsterTypeNumber = Monster.WRITHING_MASS.ordinal();
    }

    public WrithingMassState(JsonObject monsterJson) {
        super(monsterJson);

        this.firstMove = monsterJson.get("first_move").getAsBoolean();
        this.usedMegaDebuff = monsterJson.get("used_mega_debuff").getAsBoolean();

        monsterTypeNumber = Monster.WRITHING_MASS.ordinal();
    }

    @Override
    public AbstractMonster loadMonster() {
        WrithingMass monster = new WrithingMass();

        populateSharedFields(monster);

        ReflectionHacks.setPrivate(monster, WrithingMass.class, "firstMove", firstMove);
        ReflectionHacks.setPrivate(monster, WrithingMass.class, "usedMegaDebuff", usedMegaDebuff);

        return monster;
    }

    @Override
    public String encode() {
        JsonObject monsterStateJson = new JsonParser().parse(super.encode()).getAsJsonObject();

        monsterStateJson.addProperty("first_move", firstMove);
        monsterStateJson.addProperty("used_mega_debuff", usedMegaDebuff);

        return monsterStateJson.toString();
    }

    @Override
    public JsonObject jsonEncode() {
        JsonObject result = super.jsonEncode();

        result.addProperty("first_move", firstMove);
        result.addProperty("used_mega_debuff", usedMegaDebuff);

        return result;
    }

    @SpirePatch(clz = WrithingMass.class, method = "takeTurn")
    public static class NoObtainEffectsPatch {
        @SpirePrefixPatch
        public static SpireReturn betterObtain(WrithingMass mass) {
            if (shouldGoFast) {
                if (mass.nextMove == 4) {
                    SaveState.CountParasitesPatch.count++;
                    ReflectionHacks.setPrivate(mass, WrithingMass.class, "usedMegaDebuff", true);
                    AbstractDungeon.actionManager
                            .addToBottom(new BetterAddCardToDeckAction(CardLibrary
                                    .getCard("Parasite").makeCopy()));

                    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(mass));

                    return SpireReturn.Return(null);
                }
            }

            return SpireReturn.Continue();
        }

    }

    public static class BetterAddCardToDeckAction extends AbstractGameAction {
        AbstractCard cardToObtain;

        public BetterAddCardToDeckAction(AbstractCard card) {
            this.cardToObtain = card;
            this.duration = 0.01F;
        }

        public void update() {
            if (cardToObtain.color == AbstractCard.CardColor.CURSE && AbstractDungeon.player
                    .hasRelic("Omamori") && AbstractDungeon.player
                    .getRelic("Omamori").counter != 0) {
                ((Omamori) AbstractDungeon.player.getRelic("Omamori")).use();
                this.duration = 0.0F;
                this.isDone = true;
                return;
            }

            AbstractDungeon.player.relics.forEach(relic -> {
                relic.onObtainCard(cardToObtain);
                relic.onMasterDeckChange();
            });
            this.isDone = true;
        }
    }

    @SpirePatch(
            clz = WrithingMass.class,
            paramtypez = {},
            method = SpirePatch.CONSTRUCTOR
    )
    public static class YetNoAnimationsPatch {
        @SpireInsertPatch(loc = 43)
        public static SpireReturn Insert(WrithingMass _instance) {
            if (shouldGoFast) {
                int normalDebuffAmt;
                if (AbstractDungeon.ascensionLevel >= 7) {
                    MonsterState.setHp(_instance, 175, 175);
                } else {
                    MonsterState.setHp(_instance, 160, 160);
                }
                if (AbstractDungeon.ascensionLevel >= 2) {
                    _instance.damage.add(new DamageInfo(_instance, 38));
                    _instance.damage.add(new DamageInfo(_instance, 9));
                    _instance.damage.add(new DamageInfo(_instance, 16));
                    _instance.damage.add(new DamageInfo(_instance, 12));
                    normalDebuffAmt = 2;
                } else {
                    _instance.damage.add(new DamageInfo(_instance, 32));
                    _instance.damage.add(new DamageInfo(_instance, 7));
                    _instance.damage.add(new DamageInfo(_instance, 15));
                    _instance.damage.add(new DamageInfo(_instance, 10));
                    normalDebuffAmt = 2;
                }
                ReflectionHacks
                        .setPrivate(_instance, WrithingMass.class, "normalDebuffAmt", normalDebuffAmt);

                _instance.state = new AnimationStateFast();
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
