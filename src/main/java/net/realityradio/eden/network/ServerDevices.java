package net.realityradio.eden.network;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.realityradio.eden.Eden;
import net.realityradio.eden.api.*;
import net.realityradio.eden.core.*;
import net.realityradio.eden.device.*;
import net.realityradio.eden.storage.EdenSavedData;

public final class ServerDevices {
    private record Session(UUID device, BlockPos block, ResourceLocation dimension, long opened, long lastAction) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private ServerDevices() {}
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }
    public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
    public static void openItem(ServerPlayer player, ItemStack stack, String kind) {
        var id = stack.get(Eden.DEVICE_ID.get());
        if (id == null) { id = UUID.randomUUID(); stack.set(Eden.DEVICE_ID.get(), id); }
        open(player, id, kind, null);
    }
    public static void openBlock(ServerPlayer player, DeviceBlockEntity device) {
        device.setChanged();
        open(player, device.deviceId, "desktop", device.getBlockPos());
    }
    private static void open(ServerPlayer player, UUID id, String kind, BlockPos block) {
        var data = EdenSavedData.get(player.server);
        if (!data.network.devices.containsKey(id)) {
            data.network.devices.put(id, new DeviceRecord(id, kind)); data.setDirty();
        }
        long now = player.server.overworld().getGameTime();
        SESSIONS.put(player.getUUID(), new Session(id, block, player.serverLevel().dimension().location(), now, now - 4));
        snapshot(player, id, true, "");
    }
    private static boolean held(ServerPlayer player, UUID id) {
        return id.equals(player.getMainHandItem().get(Eden.DEVICE_ID.get())) && player.getMainHandItem().getItem() instanceof DeviceItem
            || id.equals(player.getOffhandItem().get(Eden.DEVICE_ID.get())) && player.getOffhandItem().getItem() instanceof DeviceItem;
    }
    private static boolean accessible(ServerPlayer player, Session session) {
        if (session.block == null) return held(player, session.device);
        return session.dimension.equals(player.serverLevel().dimension().location())
            && session.block.distToCenterSqr(player.position()) <= 64
            && player.serverLevel().hasChunkAt(session.block)
            && player.serverLevel().getBlockEntity(session.block) instanceof DeviceBlockEntity device
            && device.deviceId.equals(session.device);
    }
    public static void handle(DeviceAction action, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var session = SESSIONS.get(player.getUUID());
        long tick = player.server.overworld().getGameTime();
        if (session == null || !session.device.equals(action.device()) || tick - session.opened > 12000 || !accessible(player, session)) {
            player.displayClientMessage(Component.literal("Device access expired. Open the device again."), true);
            return;
        }
        if (tick - session.lastAction < 4) return;
        SESSIONS.put(player.getUUID(), new Session(session.device, session.block, session.dimension, tick, tick));
        var data = EdenSavedData.get(player.server);
        var network = data.network;
        String result = "";
        try {
            var device = network.device(session.device);
            switch (action.action()) {
                case "refresh" -> {}
                case "insert" -> { insert(player, network, device); data.setDirty(); result = "SIM inserted"; }
                case "eject" -> {
                    var id = network.eject(device.id);
                    var stack = new ItemStack(Eden.SIM.get()); stack.set(Eden.SIM_ID.get(), id);
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                    data.setDirty(); result = "SIM ejected";
                }
                case "message" -> {
                    var message = network.send(device.id, action.a(), action.b(), System.currentTimeMillis());
                    data.setDirty(); result = "Message sent";
                    for (var recipient : player.server.getPlayerList().getPlayers()) {
                        var other = SESSIONS.get(recipient.getUUID());
                        if (other != null && !recipient.equals(player) && accessible(recipient, other)) {
                            try {
                                if (network.account(other.device).number.equals(message.receiver()))
                                    snapshot(recipient, other.device, false, "New message from " + message.sender());
                            } catch (IllegalArgumentException ignored) { /* Device without a SIM */ }
                        }
                    }
                }
                case "contact" -> { network.contact(device.id, action.a(), action.b()); data.setDirty(); result = "Contact saved"; }
                case "delete_contact" -> { network.account(device.id).contacts.remove(action.a()); data.setDirty(); result = "Contact deleted"; }
                case "notes" -> {
                    if (action.b().length() > 2048) throw new IllegalArgumentException("Notes exceed 2048 characters");
                    device.notes = action.b(); data.setDirty(); result = "Notes saved";
                }
                case "theme" -> {
                    if (!Set.of("magenta", "blue", "green").contains(action.a())) throw new IllegalArgumentException("Unknown theme");
                    device.theme = action.a(); data.setDirty(); result = "Theme updated";
                }
                case "publish_app" -> {
                    network.publishApp(new StudioApp(action.a(), player.getUUID(), action.c(), action.b(), ""));
                    data.setDirty(); result = "App published. It is available on all devices.";
                }
                case "link_service" -> {
                    var app = network.apps.get(action.a());
                    if (app == null || !app.author().equals(player.getUUID())) throw new IllegalArgumentException("You can only edit your own apps");
                    if (!action.c().isBlank()) {
                        if (!action.c().matches("studio:[a-z0-9_]{1,32}") && EdenPlatform.service(ResourceLocation.parse(action.c())).isEmpty())
                            throw new IllegalArgumentException("Service is not registered");
                    }
                    network.publishApp(new StudioApp(app.id(), app.author(), app.title(), app.text(), action.c()));
                    data.setDirty(); result = "Service link saved";
                }
                case "publish_service" -> {
                    network.publishBulletin(action.a(), player.getUUID(), action.b()); data.setDirty(); result = "Bulletin service published";
                }
                case "service" -> {
                    if (action.a().startsWith("studio:")) {
                        var bulletin = network.bulletins.get(action.a().substring(7));
                        if (bulletin == null) throw new IllegalArgumentException("Service not found");
                        result = bulletin.text();
                    } else {
                        var service = EdenPlatform.service(ResourceLocation.parse(action.a()))
                            .orElseThrow(() -> new IllegalArgumentException("Service not found"));
                        var args = JsonParser.parseString(action.b().isBlank() ? "{}" : action.b()).getAsJsonObject();
                        result = service.execute(new ServiceContext(player, device, network), args);
                        data.setDirty();
                        if (result == null) result = "Service completed";
                    }
                }
                default -> throw new IllegalArgumentException("Unknown device action");
            }
        } catch (IllegalArgumentException | IllegalStateException | com.google.gson.JsonParseException | net.minecraft.ResourceLocationException exception) {
            result = "Error: " + exception.getMessage();
        }
        snapshot(player, session.device, false, result, action.action());
    }
    private static void insert(ServerPlayer player, WorldNetwork network, DeviceRecord device) {
        if (device.sim != null) throw new IllegalArgumentException("Eject the installed SIM first");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!stack.is(Eden.SIM.get())) continue;
            var id = stack.get(Eden.SIM_ID.get());
            if (id == null) { id = network.issueSim("Alyon Wireless").id; stack.set(Eden.SIM_ID.get(), id); }
            network.insert(device.id, id);
            stack.shrink(1); player.getInventory().setChanged(); return;
        }
        throw new IllegalArgumentException("Put a SIM card in your inventory first");
    }
    private static void snapshot(ServerPlayer player, UUID id, boolean open, String status) {
        snapshot(player, id, open, status, "");
    }
    private static void snapshot(ServerPlayer player, UUID id, boolean open, String status, String response) {
        var network = EdenSavedData.get(player.server).network;
        var device = network.device(id);
        var root = new JsonObject();
        root.addProperty("device", id.toString()); root.addProperty("kind", device.kind);
        root.addProperty("theme", device.theme); root.addProperty("notes", device.notes);
        root.addProperty("status", status.substring(0, Math.min(status.length(), 2048)));
        root.addProperty("response", response);
        root.addProperty("number", "No SIM");
        root.addProperty("carrier", "Offline");
        var contacts = new JsonArray(); var messages = new JsonArray();
        if (device.sim != null) {
            var account = network.account(id);
            root.addProperty("number", account.number); root.addProperty("carrier", account.carrier);
            for (var entry : account.contacts.entrySet()) {
                var contact = new JsonObject(); contact.addProperty("number", entry.getKey()); contact.addProperty("name", entry.getValue()); contacts.add(contact);
            }
            var history = account.messages;
            for (int i = Math.max(0, history.size() - 50); i < history.size(); i++) {
                var m = history.get(i); var row = new JsonObject();
                row.addProperty("from", m.sender()); row.addProperty("to", m.receiver()); row.addProperty("body", m.body()); row.addProperty("time", m.timestamp()); messages.add(row);
            }
        }
        root.add("contacts", contacts); root.add("messages", messages);
        var level = player.serverLevel();
        root.addProperty("weather", level.isThundering() ? "Thunderstorm" : level.isRaining() ? "Rain" : "Clear");
        root.addProperty("dimension", level.dimension().location().toString()); root.addProperty("day", level.getDayTime() / 24000);
        var apps = new JsonArray();
        for (var app : EdenPlatform.apps()) {
            var entry = new JsonObject(); entry.addProperty("id", app.id().toString()); entry.addProperty("title", app.title());
            entry.addProperty("text", app.description()); entry.addProperty("service", app.service() == null ? "" : app.service().toString()); apps.add(entry);
        }
        for (var app : network.apps.values()) {
            var entry = new JsonObject(); entry.addProperty("id", "studio:" + app.id()); entry.addProperty("title", app.title());
            entry.addProperty("text", app.text()); entry.addProperty("service", app.service());
            entry.addProperty("mine", app.author().equals(player.getUUID())); apps.add(entry);
        }
        root.add("apps", apps);
        PacketDistributor.sendToPlayer(player, new DeviceSnapshot(root.toString(), open));
    }
}
