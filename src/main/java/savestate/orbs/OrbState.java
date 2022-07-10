package savestate.orbs;

import basemod.ReflectionHacks;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import savestate.StateFactories;

import java.util.function.Function;

public abstract class OrbState {
    public static boolean IGNORE_MISSING_ORBS = false;

    public final int evokeAmount;
    public final int passiveAmount;

    public final int baseEvokeAmount;
    public final int basePassiveAmount;

    public OrbState(AbstractOrb orb) {
        this.evokeAmount = orb.evokeAmount;
        this.passiveAmount = orb.passiveAmount;

        this.baseEvokeAmount = ReflectionHacks
                .getPrivate(orb, AbstractOrb.class, "baseEvokeAmount");
        this.basePassiveAmount = ReflectionHacks
                .getPrivate(orb, AbstractOrb.class, "basePassiveAmount");
    }

    public OrbState(String jsonString) {
        JsonObject parsed = new JsonParser().parse(jsonString).getAsJsonObject();

        this.evokeAmount = parsed.get("evoke_amount").getAsInt();
        this.passiveAmount = parsed.get("passive_amount").getAsInt();

        this.baseEvokeAmount = parsed.get("base_evoke_amount").getAsInt();
        this.basePassiveAmount = parsed.get("base_passive_amount").getAsInt();
    }

    public String encode() {
        JsonObject result = new JsonObject();

        result.addProperty("evoke_amount", evokeAmount);
        result.addProperty("passive_amount", passiveAmount);

        result.addProperty("base_evoke_amount", evokeAmount);
        result.addProperty("base_passive_amount", passiveAmount);

        result.addProperty("lookup_key", loadOrb().getClass().getSimpleName());

        return result.toString();

    }

    public abstract AbstractOrb loadOrb();

    public static OrbState forOrb(AbstractOrb orb) {
        return StateFactories.orbByClassMap.get(orb.getClass()).factory.apply(orb);
    }

    public static OrbState forJsonString(String jsonString) {
        JsonObject parsed = new JsonParser().parse(jsonString).getAsJsonObject();

        String lookupKey = parsed.get("lookup_key").getAsString();

        if (!StateFactories.orbClassByName.containsKey(lookupKey) || !StateFactories.orbByClassMap
                .containsKey(StateFactories.orbClassByName.get(lookupKey))) {
            if (IGNORE_MISSING_ORBS) {
                return null;
            } else {
                throw new IllegalArgumentException("Missing state factory for orb " + lookupKey);
            }
        }

        return StateFactories.orbByClassMap
                .get(StateFactories.orbClassByName.get(lookupKey)).jsonFactory.apply(jsonString);
    }

    public static class OrbFactories {
        public Function<AbstractOrb, OrbState> factory;
        public Function<String, OrbState> jsonFactory;

        public OrbFactories(Function<AbstractOrb, OrbState> factory, Function<String, OrbState> jsonFactory) {
            this.factory = factory;
            this.jsonFactory = jsonFactory;
        }
    }
}
