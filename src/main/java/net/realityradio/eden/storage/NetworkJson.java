package net.realityradio.eden.storage;

import com.google.gson.*;
import net.realityradio.eden.core.*;

/** Versioned world format. A future schema fails visibly instead of silently losing data. */
public final class NetworkJson {
    private static final Gson GSON = new GsonBuilder().create();
    private NetworkJson() {}
    public static String encode(WorldNetwork network) {
        var root = new JsonObject();
        root.addProperty("schema", WorldNetwork.SCHEMA);
        root.add("network", GSON.toJsonTree(network));
        return GSON.toJson(root);
    }
    public static WorldNetwork decode(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("schema") || root.get("schema").getAsInt() != WorldNetwork.SCHEMA)
            throw new IllegalStateException("Unsupported E.D.E.N. world schema; restore the compatible mod version");
        if (!root.has("network") || !root.get("network").isJsonObject())
            throw new IllegalStateException("Missing E.D.E.N. world network");
        var state = root.getAsJsonObject("network");
        for (String field : new String[]{"sims", "devices", "apps", "bulletins", "nextNumber"})
            if (!state.has(field) || state.get(field).isJsonNull())
                throw new IllegalStateException("Incomplete E.D.E.N. world data: " + field);
        var result = GSON.fromJson(state, WorldNetwork.class);
        if (result == null || result.sims == null || result.devices == null || result.apps == null || result.bulletins == null)
            throw new IllegalStateException("Incomplete E.D.E.N. world data");
        result.restoreNextNumber(result.nextNumber());
        var numbers = new java.util.HashSet<String>();
        for (var entry : result.sims.entrySet()) {
            var sim = entry.getValue();
            if (sim == null || !entry.getKey().equals(sim.id) || sim.number == null || !sim.number.matches("[0-9]{1,20}")
                    || !numbers.add(sim.number) || sim.carrier == null || sim.contacts == null || sim.messages == null)
                throw new IllegalStateException("Invalid saved SIM account");
            if (sim.installedDevice != null) {
                var device = result.devices.get(sim.installedDevice);
                if (device == null || !sim.id.equals(device.sim))
                    throw new IllegalStateException("Invalid saved SIM binding");
            }
        }
        for (var entry : result.devices.entrySet()) {
            var device = entry.getValue();
            if (device == null || !entry.getKey().equals(device.id) || device.kind == null || device.notes == null || device.theme == null)
                throw new IllegalStateException("Invalid saved device");
            if (device.sim != null) {
                var sim = result.sims.get(device.sim);
                if (sim == null || !device.id.equals(sim.installedDevice))
                    throw new IllegalStateException("Invalid saved device binding");
            }
        }
        return result;
    }
}
