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
import net.realityradio.eden.infrastructure.*;

public final class ServerDevices {
    private record Session(UUID device, BlockPos block, ResourceLocation dimension, long opened, long lastAction) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private ServerDevices() {}
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); PhoneCalls.disconnect(event.getEntity().getUUID()); }
    public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); PhoneCalls.clear(); Connectivity.clear(); NodeNetworking.clear(); }
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
    public static boolean terminalAvailable(ServerPlayer p,UUID id,DeviceRecord d){var s=SESSIONS.get(p.getUUID());return s!=null&&s.device.equals(id)&&s.block!=null&&accessible(p,s)&&Connectivity.link(p,d,s.block).online();}
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
            if(!Set.of("refresh","insert_battery","eject_battery","insert","eject","hangup").contains(action.action())&&!BatteryPower.powered(player.server,device))throw new IllegalArgumentException("Battery empty or missing; charge the phone or install a charged battery");
            if(Set.of("message","call","answer","publish_app","publish_service","link_service","service","install_app").contains(action.action())&&!Connectivity.link(player,device,session.block).online())throw new IllegalArgumentException("No powered network connection");
            switch (action.action()) {
                case "refresh" -> {}
                case "insert_battery" -> {
                    if(device.kind.equals("desktop"))throw new IllegalArgumentException("Desktop does not use a removable battery");
                    if(device.battery!=null)throw new IllegalArgumentException("Remove the installed battery first");
                    int slot=Integer.parseInt(action.a());if(slot<0||slot>=player.getInventory().getContainerSize())throw new IllegalArgumentException("Invalid inventory slot");
                    var stack=player.getInventory().getItem(slot);if(!(stack.getItem()instanceof BatteryItem cell))throw new IllegalArgumentException("Put a battery in your inventory first");
                    var batteryId=stack.get(Eden.BATTERY_ID.get());if(batteryId==null)batteryId=UUID.randomUUID();device.battery=new BatteryPack(batteryId,cell.type,cell.energy(stack).getEnergyStored(),tick);
                    stack.shrink(1);player.getInventory().setChanged();data.setDirty();result="Battery installed";
                }
                case "eject_battery" -> {
                    if(device.battery==null)throw new IllegalArgumentException("No battery installed");BatteryPower.powered(player.server,device);var b=device.battery;var stack=new ItemStack(Eden.BATTERIES.get(b.type).get());stack.set(Eden.BATTERY_ID.get(),b.id);stack.set(Eden.BATTERY_ENERGY.get(),b.charge);device.battery=null;PhoneCalls.disconnect(player.getUUID());if(!player.getInventory().add(stack))player.drop(stack,false);data.setDirty();result="Battery removed";
                }
                case "wifi" -> {device.wifiSsid=action.a().trim();device.wifiKey=NetworkPolicy.wifiKey(device.wifiSsid,action.b());data.setDirty();result="Wi-Fi settings saved";}
                case "carrier" -> {if(action.a().isBlank()||action.a().length()>48)throw new IllegalArgumentException("Carrier must be 1–48 characters");network.account(device.id).carrier=action.a().trim();data.setDirty();result="SIM carrier updated";}
                case "call" -> result=PhoneCalls.dial(player,device,action.a());
                case "answer" -> result=PhoneCalls.answer(player,device);
                case "hangup" -> {PhoneCalls.disconnect(player.getUUID());result="Call ended";}
                case "wallpaper" -> {if(!Set.of("b1","deauville","stmichel","oscuridad","iluminacion","geometry","acsgui").contains(action.a()))throw new IllegalArgumentException("Unknown wallpaper");device.wallpaper=action.a();data.setDirty();result="Wallpaper updated";}
                case "save_note" -> {if(device.notebook.size()>=32&&!device.notebook.containsKey(action.a()))throw new IllegalArgumentException("Maximum 32 notes");device.notebook.put(action.a(),new PhoneNote(action.a(),action.c(),action.b(),System.currentTimeMillis()));data.setDirty();result="Note saved";}
                case "delete_note" -> {device.notebook.remove(action.a());data.setDirty();result="Note deleted";}
                case "install_app","uninstall_app" -> {
                    String id=action.a();if(Set.of("eden:phone","eden:messages","eden:contacts","eden:notes","eden:settings","eden:appstore").contains(id)&&action.action().equals("uninstall_app"))throw new IllegalArgumentException("System apps cannot be removed");
                    boolean exists=id.startsWith("studio:")?network.apps.containsKey(id.substring(7)):EdenPlatform.apps().stream().anyMatch(a->a.id().toString().equals(id));if(!exists)throw new IllegalArgumentException("Unknown app");
                    if(action.action().equals("install_app")){if(device.installedApps.size()>=64)throw new IllegalArgumentException("Maximum 64 apps");device.installedApps.add(id);}else device.installedApps.remove(id);data.setDirty();result="App updated";
                }
                case "package_put","package_get","package_delete","package_export","package_import" -> {
                    String[] target=action.a().split("/",2);if(target.length!=2||!target[1].matches("[a-zA-Z0-9_.-]{1,40}"))throw new IllegalArgumentException("Choose a rack and package name");var pos=BlockPos.of(Long.parseLong(target[0]));
                    var rack=Connectivity.racks(player,device,session.block).stream().filter(n->n.getBlockPos().equals(pos)).findFirst().orElseThrow(()->new IllegalArgumentException("Rack not reachable on this LAN"));
                    if(action.action().equals("package_get"))result=PackageVault.open(rack.packages.get(target[1]),action.c());else{
                        if(!player.hasPermissions(2)&&!player.getUUID().equals(rack.owner))throw new IllegalArgumentException("Only the rack owner can change packages");
                        if(action.action().equals("package_export")){if(!rack.exportDisk(target[1]))throw new IllegalArgumentException("Put a blank Data Cartridge in the rack");}
                        else if(action.action().equals("package_import")){if(!rack.importDisk())throw new IllegalArgumentException("Put an encrypted Data Cartridge in the rack");}
                        else if(action.action().equals("package_delete"))rack.packages.remove(target[1]);else{if(rack.packages.size()>=32&&!rack.packages.containsKey(target[1]))throw new IllegalArgumentException("Maximum 32 packages");rack.packages.put(target[1],PackageVault.seal(action.b(),action.c()));}
                        rack.setChanged();result="Package updated";
                    }
                }
                case "insert" -> { insert(player, network, device); data.setDirty(); result = "SIM inserted"; }
                case "eject" -> {
                    PhoneCalls.disconnect(player.getUUID());
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
    public static void snapshot(ServerPlayer player, UUID id, boolean open, String status) {
        snapshot(player, id, open, status, "");
    }
    private static void snapshot(ServerPlayer player, UUID id, boolean open, String status, String response) {
        var network = EdenSavedData.get(player.server).network;
        var device = network.device(id);
        var root = new JsonObject();
        root.addProperty("device", id.toString()); root.addProperty("kind", device.kind);
        root.addProperty("theme", device.theme); root.addProperty("notes", device.notes);
        root.addProperty("wallpaper",device.wallpaper);root.addProperty("wifi",device.wifiSsid);
        boolean powered=BatteryPower.powered(player.server,device);root.addProperty("powered",powered);
        var battery=new JsonObject();battery.addProperty("type",device.battery==null?"none":device.battery.type);battery.addProperty("charge",device.battery==null?0:device.battery.charge);battery.addProperty("capacity",device.battery==null?0:BatteryPack.capacity(device.battery.type));root.add("battery",battery);
        var batteries=new JsonArray();for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var stack=player.getInventory().getItem(slot);if(stack.getItem()instanceof BatteryItem cell){var e=new JsonObject();e.addProperty("slot",slot);e.addProperty("type",cell.type);e.addProperty("charge",cell.energy(stack).getEnergyStored());e.addProperty("capacity",BatteryPack.capacity(cell.type));batteries.add(e);}}root.add("batteries",batteries);
        var session=SESSIONS.get(player.getUUID());var terminal=session!=null&&session.device.equals(id)?session.block:null;var link=Connectivity.link(player,device,terminal);root.addProperty("online",powered&&link.online());root.addProperty("connection",link.mode());root.addProperty("networkName",link.network());
        var racks=new JsonArray();for(var rack:Connectivity.racks(player,device,terminal).stream().limit(16).toList()){var e=new JsonObject();e.addProperty("id",Long.toString(rack.getBlockPos().asLong()));e.addProperty("name",rack.ssid);e.addProperty("mine",player.hasPermissions(2)||player.getUUID().equals(rack.owner));e.add("files",new Gson().toJsonTree(rack.packages.keySet()));racks.add(e);}root.add("racks",racks);
        root.add("notebook",new Gson().toJsonTree(device.notebook.values()));root.add("installed",new Gson().toJsonTree(device.installedApps));root.add("call",PhoneCalls.snapshot(player.getUUID(),id));root.addProperty("voiceAvailable",PhoneCalls.voiceAvailable());
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
        root.addProperty("worldTime",level.getDayTime());
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
