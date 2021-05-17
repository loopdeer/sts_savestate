package savestate.monsters.exordium;

import basemod.ReflectionHacks;
import savestate.fastobjects.AnimationStateFast;
import savestate.monsters.Monster;
import savestate.monsters.MonsterState;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.LouseNormal;

import static savestate.SaveStateMod.shouldGoFast;

public class LouseNormalState extends MonsterState {
    private final boolean isOpen;
    private final int biteDamage;

    public LouseNormalState(AbstractMonster monster) {
        super(monster);

        isOpen = ReflectionHacks
                .getPrivate(monster, LouseNormal.class, "isOpen");
        biteDamage = ReflectionHacks
                .getPrivate(monster, LouseNormal.class, "biteDamage");

        monsterTypeNumber = Monster.FUZZY_LOUSE_NORMAL.ordinal();
    }

    public LouseNormalState(String jsonString) {
        super(jsonString);

        // TODO don't parse twice
        JsonObject parsed = new JsonParser().parse(jsonString).getAsJsonObject();

        this.isOpen = parsed.get("is_open").getAsBoolean();
        this.biteDamage = parsed.get("bite_damage").getAsInt();

        monsterTypeNumber = Monster.FUZZY_LOUSE_NORMAL.ordinal();
    }

    @Override
    public AbstractMonster loadMonster() {
        LouseNormal monster = new LouseNormal(offsetX, offsetY);
        populateSharedFields(monster);

        ReflectionHacks
                .setPrivate(monster, LouseNormal.class, "isOpen", isOpen);
        ReflectionHacks
                .setPrivate(monster, LouseNormal.class, "biteDamage", biteDamage);

        return monster;
    }

    @Override
    public String encode() {
        JsonObject monsterStateJson = new JsonParser().parse(super.encode()).getAsJsonObject();

        monsterStateJson.addProperty("is_open", isOpen);
        monsterStateJson.addProperty("bite_damage", biteDamage);

        return monsterStateJson.toString();
    }

    @SpirePatch(
            clz = LouseNormal.class,
            paramtypez = {float.class, float.class},
            method = SpirePatch.CONSTRUCTOR
    )
    public static class NoAnimationsPatch {
        @SpireInsertPatch(loc = 40)
        public static SpireReturn LouseNormal(LouseNormal _instance, float x, float y) {
            // ascension is hopefully handled in state setting
            if (shouldGoFast) {
                if (AbstractDungeon.ascensionLevel >= 7) {
                    MonsterState.setHp(_instance, 11, 16);
                } else {
                    MonsterState.setHp(_instance, 10, 15);
                }

                int tmpBiteDamage;
                if (AbstractDungeon.ascensionLevel >= 2) {
                    tmpBiteDamage = AbstractDungeon.monsterHpRng.random(6, 8);
                } else {
                    tmpBiteDamage = AbstractDungeon.monsterHpRng.random(5, 7);
                }

                ReflectionHacks
                        .setPrivate(_instance, LouseNormal.class, "biteDamage", tmpBiteDamage);

                _instance.damage.add(new DamageInfo(_instance, tmpBiteDamage));
                _instance.state = new AnimationStateFast();
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
