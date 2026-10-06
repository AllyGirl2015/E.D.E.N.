package net.realityradio.eden.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.realityradio.eden.core.Calculator;
import net.realityradio.eden.network.DeviceAction;

/** Shared responsive native screen. Common/server code never imports this class. */
public final class DeviceScreen extends Screen {
    private JsonObject data;
    private final UUID device;
    private String app = "home";
    private String localStatus = "";
    private String serviceOutput = "";
    private int left, top, panelWidth, panelHeight, offset;
    private int bodyTop;
    private final Map<String, EditBox> fields = new LinkedHashMap<>();
    private final Map<String, String> drafts = new HashMap<>();
    private final List<String> lines = new ArrayList<>();
    public DeviceScreen(JsonObject data) {
        super(Component.literal("E.D.E.N. OS")); this.data = data; this.device = UUID.fromString(value("device"));
    }
    private String value(String key) { return data.has(key) ? data.get(key).getAsString() : ""; }
    public void update(JsonObject next) {
        if (!device.toString().equals(next.get("device").getAsString())) return;
        preserveDrafts(); data = next; localStatus = value("status");
        if (value("response").equals("service")) serviceOutput = localStatus;
        rebuildWidgets();
    }
    private void preserveDrafts() { fields.forEach((key, field) -> drafts.put(key, field.getValue())); }
    private String text(String key) { var field = fields.get(key); return field == null ? drafts.getOrDefault(key, "") : field.getValue(); }
    @Override protected void init() {
        fields.clear(); lines.clear();
        panelWidth = Math.min(460, width - 16); panelHeight = Math.min(440, height - 12);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int y = top + 48;
        bodyTop = y;
        button("Home", left + 10, top + panelHeight - 28, 58, () -> navigate("home"));
        button("Refresh", left + 74, top + panelHeight - 28, 68, () -> send("refresh", "", "", ""));
        button("Close", left + panelWidth - 68, top + panelHeight - 28, 58, this::onClose);
        switch (app) {
            case "home" -> home(y);
            case "eden:messages" -> {
                field("to", "Recipient number", y, 20); field("message", "Message", y + 24, 512);
                button("Send", left + 10, y + 49, 90, () -> send("message", text("to"), text("message"), ""));
                bodyTop = y + 76;
                var messages = data.getAsJsonArray("messages");
                for (int i = messages.size() - 1; i >= 0; i--) {
                    var row = messages.get(i).getAsJsonObject();
                    lines.add(row.get("from").getAsString() + " > " + row.get("to").getAsString() + ": " + row.get("body").getAsString());
                }
                if (lines.isEmpty()) lines.add("No messages. Share your number to connect.");
            }
            case "eden:contacts" -> {
                field("contactNumber", "Number", y, 20); field("contactName", "Name", y + 24, 48);
                button("Save", left + 10, y + 49, 72, () -> send("contact", text("contactNumber"), text("contactName"), ""));
                button("Delete", left + 88, y + 49, 72, () -> send("delete_contact", text("contactNumber"), "", ""));
                bodyTop = y + 76;
                for (var entry : data.getAsJsonArray("contacts")) {
                    var contact = entry.getAsJsonObject(); lines.add(contact.get("name").getAsString() + " — " + contact.get("number").getAsString());
                }
                if (lines.isEmpty()) lines.add("Your contacts travel with your SIM.");
            }
            case "eden:notes" -> {
                drafts.putIfAbsent("notes", value("notes")); field("notes", "Note (maximum 2048 characters)", y, 2048);
                button("Save note", left + 10, y + 25, 100, () -> send("notes", "", text("notes"), ""));
                bodyTop = y + 53; lines.add(value("notes").isBlank() ? "No saved note yet." : value("notes"));
            }
            case "eden:weather" -> {
                lines.add("Current conditions: " + value("weather")); lines.add("Dimension: " + value("dimension")); lines.add("World day: " + value("day"));
                lines.add("Refresh for current weather. Forecasting is not available.");
            }
            case "eden:calculator" -> {
                field("expression", "Expression: + - * / and parentheses", y, 128);
                button("Calculate", left + 10, y + 25, 100, () -> {
                    try { localStatus = "= " + Calculator.evaluate(text("expression")); }
                    catch (IllegalArgumentException exception) { localStatus = exception.getMessage(); }
                });
                bodyTop = y + 54; lines.add("Examples: (12 + 8) / 4, 2.5 * 9");
            }
            case "eden:settings" -> {
                button("Insert SIM", left + 10, y, 104, () -> send("insert", "", "", ""));
                button("Eject SIM", left + 120, y, 104, () -> send("eject", "", "", ""));
                y += 26;
                for (String theme : List.of("magenta", "blue", "green")) {
                    final String selected = theme;
                    button(theme, left + 10 + List.of("magenta", "blue", "green").indexOf(theme) * 80, y, 74, () -> send("theme", selected, "", ""));
                }
                bodyTop = y + 32;
                lines.add("Device: " + value("kind")); lines.add("Number: " + value("number")); lines.add("Provider: " + value("carrier"));
                lines.add("SIM insertion takes the first SIM from your inventory.");
                lines.add("Service currently covers the whole world. Tower-based coverage is planned.");
            }
            case "eden:studio" -> studio(y);
            default -> {
                var descriptor = descriptor(app);
                if (descriptor == null) { lines.add("App no longer exists. Return Home."); break; }
                lines.add(descriptor.get("text").getAsString());
                String service = descriptor.get("service").getAsString();
                if (!service.isBlank()) {
                    field("arguments", "Service arguments (JSON)", y, 2048);
                    button("Run service", left + 10, y + 25, 110, () -> send("service", service, text("arguments"), ""));
                    bodyTop = y + 54;
                    if (!serviceOutput.isBlank()) { lines.add("Service response:"); lines.add(serviceOutput); }
                }
                if (app.startsWith("studio:") && descriptor.has("mine") && descriptor.get("mine").getAsBoolean())
                    lines.add("Edit this app in App Studio using ID " + app.substring(7) + ".");
            }
        }
    }
    private void home(int y) {
        var apps = data.getAsJsonArray("apps");
        int rows = Math.max(1, (panelHeight - 124) / 25);
        int count = rows * 2;
        int start = Math.min(offset, Math.max(0, apps.size() - 1));
        for (int i = start; i < Math.min(apps.size(), start + count); i++) {
            var descriptor = apps.get(i).getAsJsonObject();
            String id = descriptor.get("id").getAsString();
            int slot = i - start;
            button(descriptor.get("title").getAsString(), left + 10 + (slot % 2) * ((panelWidth - 20) / 2), y + slot / 2 * 25,
                    (panelWidth - 26) / 2, () -> {
                        var custom = ClientApps.create(ResourceLocation.parse(id), data, this);
                        if (custom != null) minecraft.setScreen(custom); else navigate(id);
                    });
        }
        if (apps.size() > count) {
            button("<", left + 148, top + panelHeight - 28, 28, () -> { preserveDrafts(); offset = Math.max(0, offset - count); rebuildWidgets(); });
            button(">", left + 180, top + panelHeight - 28, 28, () -> { preserveDrafts(); offset = offset + count < apps.size() ? offset + count : 0; rebuildWidgets(); });
        }
    }
    private void studio(int y) {
        field("appId", "App/service ID", y, 32);
        field("appTitle", "App title", y + 22, 32);
        field("appText", "App text / bulletin text", y + 44, 1024);
        field("serviceId", "Service link (e.g. studio:news)", y + 66, 64);
        int bw = (panelWidth - 32) / 3;
        button("Publish app", left + 10, y + 90, bw, () -> send("publish_app", text("appId"), text("appText"), text("appTitle")));
        button("Link service", left + 16 + bw, y + 90, bw, () -> send("link_service", text("appId"), "", text("serviceId")));
        button("Publish service", left + 22 + bw * 2, y + 90, bw, () -> send("publish_service", text("appId"), text("appText"), ""));
        bodyTop = y + 114;
        lines.add("1. Give your app an ID, title and text. Publish app.");
        lines.add("2. For a shared bulletin, publish service with an ID and text.");
        lines.add("3. Set app ID + studio:service_id, then Link service.");
        lines.add("All devices share this catalog. Only the author can change their app/service.");
    }
    private JsonObject descriptor(String id) {
        for (var entry : data.getAsJsonArray("apps")) if (entry.getAsJsonObject().get("id").getAsString().equals(id)) return entry.getAsJsonObject();
        return null;
    }
    private void navigate(String id) { preserveDrafts(); app = id; offset = 0; localStatus = ""; serviceOutput = ""; rebuildWidgets(); }
    private void send(String action, String a, String b, String c) {
        preserveDrafts(); PacketDistributor.sendToServer(new DeviceAction(device, action, a, b, c));
    }
    private void field(String key, String hint, int y, int limit) {
        var box = new EditBox(font, left + 10, y, panelWidth - 20, 20, Component.literal(hint));
        box.setMaxLength(limit); box.setHint(Component.literal(hint)); box.setValue(drafts.getOrDefault(key, ""));
        fields.put(key, box); addRenderableWidget(box);
    }
    private void button(String text, int x, int y, int w, Runnable handler) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> handler.run()).bounds(x, y, w, 20).build());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!app.equals("home")) { offset = Math.max(0, Math.min(offset - (int) Math.signum(sy) * 3, Math.max(0, wrappedLines().size() - 1))); return true; }
        return super.mouseScrolled(x, y, sx, sy);
    }
    private List<net.minecraft.util.FormattedCharSequence> wrappedLines() {
        var result = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (var line : lines) { result.addAll(font.split(Component.literal(line), panelWidth - 28)); result.add(net.minecraft.util.FormattedCharSequence.EMPTY); }
        return result;
    }
    @Override public void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        renderBackground(graphics, mx, my, partialTick);
        int accent = switch (value("theme")) { case "blue" -> 0xFF287AB8; case "green" -> 0xFF247744; default -> 0xFFB32C83; };
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF121624);
        graphics.fill(left, top, left + panelWidth, top + 24, accent);
        var title = descriptor(app);
        graphics.drawString(font, app.equals("home") ? "E.D.E.N. OS" : title == null ? app : title.get("title").getAsString(), left + 10, top + 8, 0xFFFFFFFF, false);
        graphics.drawString(font, value("kind") + " | " + value("number") + " | " + value("carrier"), left + 10, top + 33, 0xFFBBBBCC, false);
        int bottom = top + panelHeight - 64;
        graphics.enableScissor(left + 8, bodyTop, left + panelWidth - 8, Math.max(bodyTop, bottom));
        var text = wrappedLines(); int y = bodyTop;
        for (int i = offset; i < text.size() && y + 10 <= bottom; i++, y += 11)
            graphics.drawString(font, text.get(i), left + 12, y, 0xFFEEEEF5, false);
        graphics.disableScissor();
        graphics.drawString(font, font.plainSubstrByWidth(localStatus, panelWidth - 24), left + 10, top + panelHeight - 48, 0xFFFFDDEE, false);
        super.render(graphics, mx, my, partialTick);
    }
}
