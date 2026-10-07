package net.realityradio.eden.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.realityradio.eden.Eden;
import net.realityradio.eden.network.EdenNetworking;

@Mod(value = Eden.MODID, dist = Dist.CLIENT)
public final class EdenClient {
    public EdenClient() {
        EdenNetworking.nodeReceiver = snapshot -> {
            var minecraft = Minecraft.getInstance();
            var data = JsonParser.parseString(snapshot.json()).getAsJsonObject();
            if (minecraft.screen instanceof NetworkConfigScreen screen) screen.update(data);
            else minecraft.setScreen(new NetworkConfigScreen(data));
        };
        EdenNetworking.clientReceiver = snapshot -> {
            var minecraft = Minecraft.getInstance();
            var data = JsonParser.parseString(snapshot.json()).getAsJsonObject();
            if (snapshot.open()) minecraft.setScreen(new DeviceScreen(data));
            else if (minecraft.screen instanceof DeviceScreen screen) screen.update(data);
        };
    }
}
