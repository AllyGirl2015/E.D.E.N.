/* SPhone GuiBase/GuiHome portrait shell, original artwork and app workflows,
 * adapted to current Minecraft GUI and server-owned data. Apache-2.0 NOTICE
 * is retained in third_party/sphone; no legacy ACS/JDBC binaries are bundled. */
package net.realityradio.eden.client;

import com.google.gson.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import net.realityradio.eden.core.Calculator;
import net.realityradio.eden.network.DeviceAction;

public final class DeviceScreen extends Screen {
  private static final int W = 220,
      H = 450,
      WHITE = 0xFFF3F4F6,
      MUTED = 0xFF9EA8B8,
      BLUE = 0xFF599AF8;
  private static final List<String> DOCK =
      List.of("eden:phone", "eden:notes", "eden:contacts", "eden:messages");
  private JsonObject data;
  private final UUID device;
  private String app = "home", selected = "", toast = "", calc = "", serviceOutput = "";
  private long toastUntil;
  private int page, chatScroll, ticks;
  private float scale, left, top;
  private final Map<String, EditBox> fields = new LinkedHashMap<>();
  private final Map<String, String> drafts = new HashMap<>();
  private final ArrayDeque<DeviceAction> actions = new ArrayDeque<>();
  private MultiLineEditBox multiline;
  private String multilineKey;
  private final List<Draw> art = new ArrayList<>();

  @FunctionalInterface
  private interface Draw {
    void render(GuiGraphics g);
  }

  public DeviceScreen(JsonObject d) {
    super(Component.literal("E.D.E.N. Phone"));
    data = d;
    device = UUID.fromString(value("device"));
    if (!powered()) app = "eden:battery";
    else if (!callState().equals("idle")) app = "eden:phone";
  }

  private String value(String key) {
    return data.has(key) ? data.get(key).getAsString() : "";
  }

  private JsonArray array(String key) {
    return data.has(key) ? data.getAsJsonArray(key) : new JsonArray();
  }

  private boolean powered() {
    return !data.has("powered") || data.get("powered").getAsBoolean();
  }

  private boolean sim() {
    return !value("number").equals("No SIM");
  }

  private String callState() {
    return data.has("call") ? data.getAsJsonObject("call").get("state").getAsString() : "idle";
  }

  private String callNumber() {
    return data.has("call") && data.getAsJsonObject("call").has("number")
        ? data.getAsJsonObject("call").get("number").getAsString()
        : "";
  }

  private void preserve() {
    fields.forEach((k, b) -> drafts.put(k, b.getValue()));
    if (multiline != null) drafts.put(multilineKey, multiline.getValue());
  }

  private String text(String key) {
    return key.equals(multilineKey) && multiline != null
        ? multiline.getValue()
        : fields.containsKey(key) ? fields.get(key).getValue() : drafts.getOrDefault(key, "");
  }

  public void update(JsonObject next) {
    if (!device.toString().equals(next.get("device").getAsString())) return;
    preserve();
    String oldCall = callState();
    data = next;
    String status = value("status"), response = value("response");
    if (!status.isBlank())
      notice(
          response.equals("package_get") && !status.startsWith("Error:")
              ? "Package downloaded"
              : status.length() > 100 ? "Response received" : status);
    if (response.equals("service")) serviceOutput = status;
    if (response.equals("package_get") && !status.startsWith("Error:"))
      drafts.put("packageBody", status);
    if (response.equals("message") && status.equals("Message sent")) {
      drafts.put("message", "");
      chatScroll = 0;
    }
    if (response.equals("save_note") && status.equals("Note saved") && app.equals("note_edit")) {
      app = "eden:notes";
      page = 0;
    }
    if (!oldCall.equals(callState()) && !callState().equals("idle")) {
      app = "eden:phone";
      if (callState().equals("incoming") && minecraft.player != null)
        minecraft.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), .7F, 1.3F);
    }
    if (!powered()) app = "eden:battery";
    String focus =
        fields.entrySet().stream()
            .filter(e -> e.getValue().isFocused())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("");
    boolean multiFocus = multiline != null && multiline.isFocused();
    rebuildWidgets();
    if (fields.containsKey(focus)) setFocused(fields.get(focus));
    else if (multiFocus && multiline != null) setFocused(multiline);
  }

  private void navigate(String next) {
    preserve();
    app = powered() || next.equals("eden:battery") ? next : "eden:battery";
    page = 0;
    chatScroll = 0;
    rebuildWidgets();
  }

  private void openApp(String id) {
    if (!powered()) {
      navigate("eden:battery");
      return;
    }
    var custom = ClientApps.create(ResourceLocation.parse(id), data, this);
    if (custom != null) minecraft.setScreen(custom);
    else navigate(id);
  }

  private void send(String action, String a, String b, String c) {
    preserve();
    if (actions.size() < 16) actions.add(new DeviceAction(device, action, a, b, c));
  }

  public void tick() {
    super.tick();
    ticks++;
    if (minecraft.getConnection() == null) return;
    if (ticks % 6 == 0 && !actions.isEmpty()) PacketDistributor.sendToServer(actions.remove());
    else if (ticks % 100 == 0 && actions.isEmpty()) send("refresh", "", "", "");
    if (callState().equals("incoming") && ticks % 60 == 0 && minecraft.player != null)
      minecraft.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), .6F, 1.3F);
  }

  protected void init() {
    fields.clear();
    multiline = null;
    multilineKey = null;
    art.clear();
    scale = Math.min(1.15F, (height - 12F) / H);
    left = Math.max(6, width - W * scale - 12);
    top = (height - H * scale) / 2;
    hit(
        "Home",
        65,
        431,
        90,
        11,
        () -> {
          if (app.equals("home") || !powered()) onClose();
          else navigate("home");
        },
        null,
        0);
    if (!powered()) {
      battery();
      return;
    }
    if (app.equals("home")) {
      home();
      return;
    }
    if (Set.of(
                "eden:messages",
                "conversation",
                "new_message",
                "eden:contacts",
                "contact_detail",
                "contact_edit",
                "eden:phone")
            .contains(app)
        && !sim()) {
      title("No SIM", () -> navigate("home"));
      empty(
          "Connect your phone",
          "Keep a SIM in your inventory and insert it to get your number.",
          "settings/custom");
      hit("Insert SIM", 30, 315, 160, 28, () -> send("insert", "", "", ""), null, BLUE);
      return;
    }
    switch (app) {
      case "eden:messages" -> conversations();
      case "new_message" -> newMessage();
      case "conversation" -> conversation();
      case "eden:contacts" -> contacts();
      case "contact_detail" -> contactDetail();
      case "contact_edit" -> contactEditor();
      case "eden:notes" -> notes();
      case "note_edit" -> noteEditor();
      case "eden:calculator" -> calculator();
      case "eden:weather" -> weather();
      case "eden:settings" -> settings();
      case "wallpapers" -> wallpapers();
      case "eden:appstore" -> appStore();
      case "eden:studio" -> studio();
      case "eden:phone" -> phone();
      case "eden:camera" -> camera();
      case "eden:gallery" -> gallery();
      case "eden:plusplus" -> plusGame();
      case "eden:network" -> network();
      case "eden:packages" -> packages();
      case "eden:battery" -> battery();
      default -> customApp();
    }
  }

  private void title(String name, Runnable back) {
    hit("Back", 15, 40, 28, 24, back, "remove", 0);
    art.add(
        g -> {
          label(g, font.plainSubstrByWidth(name, 126), 48, 46, WHITE, 1.1F);
          g.fill(16, 70, 204, 71, 0xFF313745);
        });
  }

  private String name(String n) {
    for (var e : array("contacts")) {
      var c = e.getAsJsonObject();
      if (c.get("number").getAsString().equals(n)) return c.get("name").getAsString();
    }
    return n;
  }

  private void home() {
    var apps = new ArrayList<JsonObject>();
    Set<String> installed = new HashSet<>();
    for (var e : array("installed")) installed.add(e.getAsString());
    for (var e : array("apps")) {
      var d = e.getAsJsonObject();
      String id = d.get("id").getAsString();
      if (installed.contains(id) && !DOCK.contains(id)) apps.add(d);
    }
    int count = 16,
        start = Math.min(page * count, Math.max(0, ((apps.size() - 1) / count) * count));
    for (int i = start; i < Math.min(start + count, apps.size()); i++) {
      var d = apps.get(i);
      int n = i - start;
      appIcon(
          d.get("id").getAsString(),
          d.get("title").getAsString(),
          20 + n % 4 * 47,
          62 + n / 4 * 64);
    }
    art.add(
        g -> {
          rounded(g, 18, 369, 184, 48, 16, 0x66000000);
          centered(
              g,
              value("connection") + " · " + (sim() ? value("carrier") : "No SIM"),
              335,
              WHITE,
              .85F);
        });
    for (int i = 0; i < DOCK.size(); i++) {
      String id = DOCK.get(i);
      hit(id.substring(5), 27 + i * 45, 376, 32, 32, () -> openApp(id), appIconPath(id), 0);
    }
    if (apps.size() > count) {
      hit(
          "<",
          67,
          309,
          30,
          18,
          () -> {
            page = Math.max(0, page - 1);
            rebuildWidgets();
          },
          null,
          0x66000000);
      hit(
          ">",
          123,
          309,
          30,
          18,
          () -> {
            page = (page + 1) * count < apps.size() ? page + 1 : 0;
            rebuildWidgets();
          },
          null,
          0x66000000);
    }
  }

  private void appIcon(String id, String name, int x, int y) {
    hit(name, x, y, 32, 32, () -> openApp(id), appIconPath(id), 0);
    art.add(
        g -> {
          String s = font.plainSubstrByWidth(name, 62);
          label(g, s, x + 16 - font.width(s) * .75F / 2, y + 38, WHITE, .75F);
        });
  }

  private String appIconPath(String id) {
    return switch (id) {
      case "eden:messages" -> "message";
      case "eden:contacts" -> "contacts";
      case "eden:notes" -> "notes";
      case "eden:weather" -> "weather_icon";
      case "eden:calculator" -> "calculator";
      case "eden:settings" -> "settings";
      case "eden:phone" -> "call";
      case "eden:camera" -> "photo";
      case "eden:gallery" -> "gallery";
      case "eden:appstore" -> "appstore";
      case "eden:plusplus" -> "plusplusgame";
      default -> "devapp";
    };
  }

  private List<JsonObject> threads() {
    var m = new LinkedHashMap<String, JsonObject>();
    for (var e : array("messages")) {
      var d = e.getAsJsonObject();
      String peer =
          d.get("from").getAsString().equals(value("number"))
              ? d.get("to").getAsString()
              : d.get("from").getAsString();
      m.put(peer, d);
    }
    return m.entrySet().stream()
        .sorted(
            (a, b) ->
                Long.compare(
                    b.getValue().get("time").getAsLong(), a.getValue().get("time").getAsLong()))
        .map(
            e -> {
              var d = e.getValue().deepCopy();
              d.addProperty("peer", e.getKey());
              return d;
            })
        .toList();
  }

  private void row(String title, String subtitle, int y, Runnable click) {
    art.add(
        g -> {
          rounded(g, 16, y, 188, 43, 6, 0xFF2F3540);
          label(g, clip(title, 170), 24, y + 7, WHITE, 1);
          label(g, clip(subtitle, 170), 24, y + 24, MUTED, .85F);
        });
    hit(title, 16, y, 188, 43, click, null, 0, false);
  }

  private void conversations() {
    title("Messages", () -> navigate("home"));
    hit("New message", 174, 42, 22, 22, () -> navigate("new_message"), "add", 0);
    var list = threads();
    if (list.isEmpty())
      empty("Your conversations", "Start a message using a contact or phone number.", "message");
    for (int i = page * 6; i < Math.min(page * 6 + 6, list.size()); i++) {
      var m = list.get(i);
      String peer = m.get("peer").getAsString();
      row(
          name(peer),
          m.get("body").getAsString(),
          85 + i % 6 * 48,
          () -> {
            selected = peer;
            navigate("conversation");
          });
    }
    pagination(list.size(), 6);
  }

  private void newMessage() {
    title("New message", () -> navigate("eden:messages"));
    field("recipient", "Phone number", 20, 101, 180, 20);
    hit(
        "Open conversation",
        20,
        136,
        180,
        27,
        () -> {
          String n = text("recipient").trim();
          if (n.matches("[0-9]{1,20}")) {
            selected = n;
            navigate("conversation");
          } else notice("Enter a phone number");
        },
        null,
        BLUE);
    hit("Contacts", 20, 231, 180, 26, () -> navigate("eden:contacts"), null, 0xFF343C4B);
  }

  private void conversation() {
    title(clip(name(selected), 105), () -> navigate("eden:messages"));
    hit(
        "Share location",
        174,
        42,
        22,
        22,
        () -> {
          if (minecraft.player != null) {
            var p = minecraft.player.blockPosition();
            send(
                "message",
                selected,
                "Location: "
                    + p.getX()
                    + ", "
                    + p.getY()
                    + ", "
                    + p.getZ()
                    + " · "
                    + value("dimension"),
                "");
          }
        },
        "map",
        0);
    art.add(this::chat);
    field("message", "Message", 18, 383, 153, 512);
    hit("Send", 176, 382, 25, 23, this::sendMessage, "send", 0);
    setFocused(fields.get("message"));
  }

  private void sendMessage() {
    if (!text("message").isBlank()) send("message", selected, text("message"), "");
  }

  private void chat(GuiGraphics g) {
    record Bubble(String text, boolean me, long time, int height) {}
    var bubbles = new ArrayList<Bubble>();
    int total = 0;
    for (var e : array("messages")) {
      var m = e.getAsJsonObject();
      String from = m.get("from").getAsString(), to = m.get("to").getAsString();
      if (!from.equals(selected) && !to.equals(selected)) continue;
      String t = m.get("body").getAsString();
      int h = font.split(Component.literal(t), 145).size() * 12 + 22;
      bubbles.add(new Bubble(t, from.equals(value("number")), m.get("time").getAsLong(), h));
      total += h + 8;
    }
    chatScroll = Math.min(chatScroll, Math.max(0, total - 280));
    int y = 370 - total + chatScroll;
    clipArea(g, 14, 79, 206, 375);
    for (var b : bubbles) {
      int x = b.me ? 48 : 16;
      rounded(g, x, y, 156, b.height, 8, b.me ? 0xFF087DF3 : 0xFF343A45);
      int ty = y + 7;
      for (var line : font.split(Component.literal(b.text), 145)) {
        g.drawString(font, line, x + 6, ty, WHITE, false);
        ty += 12;
      }
      label(
          g,
          new SimpleDateFormat("HH:mm").format(new Date(b.time)),
          x + 112,
          y + b.height - 12,
          MUTED,
          .7F);
      y += b.height + 8;
    }
    g.disableScissor();
  }

  private void contacts() {
    title("Contacts", () -> navigate("home"));
    hit(
        "Add contact",
        174,
        42,
        22,
        22,
        () -> {
          selected = "";
          drafts.put("contactNumber", "");
          drafts.put("contactName", "");
          navigate("contact_edit");
        },
        "add",
        0);
    var list = array("contacts");
    if (list.isEmpty()) empty("Your contacts", "Add a name and phone number.", "contacts");
    for (int i = page * 6; i < Math.min(page * 6 + 6, list.size()); i++) {
      var c = list.get(i).getAsJsonObject();
      String n = c.get("number").getAsString();
      row(
          c.get("name").getAsString(),
          n,
          85 + i % 6 * 48,
          () -> {
            selected = n;
            navigate("contact_detail");
          });
    }
    pagination(list.size(), 6);
  }

  private void contactDetail() {
    title("Contact", () -> navigate("eden:contacts"));
    art.add(
        g -> {
          icon(g, "contacts", 78, 103, 64);
          centered(g, clip(name(selected), 172), 188, WHITE, 1.2F);
          centered(g, selected, 220, MUTED, 1);
        });
    hit("Message", 30, 265, 160, 29, () -> navigate("conversation"), null, BLUE);
    hit(
        "Call",
        30,
        307,
        160,
        29,
        () -> {
          drafts.put("dialNumber", selected);
          send("call", selected, "", "");
          navigate("eden:phone");
        },
        null,
        0xFF249947);
    hit(
        "Edit",
        30,
        349,
        160,
        29,
        () -> {
          drafts.put("contactNumber", selected);
          drafts.put("contactName", name(selected));
          navigate("contact_edit");
        },
        null,
        0xFF343C4B);
  }

  private void contactEditor() {
    title("Edit contact", () -> navigate("eden:contacts"));
    field("contactName", "Name", 20, 112, 180, 48);
    field("contactNumber", "Phone number", 20, 156, 180, 20);
    hit(
        "Save contact",
        20,
        215,
        180,
        28,
        () -> {
          send("contact", text("contactNumber"), text("contactName"), "");
        },
        null,
        BLUE);
    if (!selected.isEmpty())
      hit(
          "Delete contact",
          20,
          261,
          180,
          28,
          () -> send("delete_contact", selected, "", ""),
          null,
          0xFFBB3742);
    art.add(
        g ->
            paragraph(
                g,
                "Changing the number creates a new contact; remove the old entry separately.",
                24,
                320,
                172,
                MUTED));
  }

  private void notes() {
    title("Notes", () -> navigate("home"));
    hit(
        "New note",
        174,
        42,
        22,
        22,
        () -> {
          selected = UUID.randomUUID().toString();
          drafts.put("noteTitle", "");
          drafts.put("noteText", "");
          navigate("note_edit");
        },
        "add",
        0);
    var list = array("notebook");
    if (list.isEmpty()) empty("Your notes", "Write and save notes on this device.", "notes");
    for (int i = page * 6; i < Math.min(page * 6 + 6, list.size()); i++) {
      var n = list.get(i).getAsJsonObject();
      row(
          n.get("title").getAsString(),
          n.get("text").getAsString(),
          85 + i % 6 * 48,
          () -> {
            selected = n.get("id").getAsString();
            drafts.put("noteTitle", n.get("title").getAsString());
            drafts.put("noteText", n.get("text").getAsString());
            navigate("note_edit");
          });
    }
    pagination(list.size(), 6);
  }

  private void noteEditor() {
    title("Edit note", () -> navigate("eden:notes"));
    if (selected.isBlank()) selected = UUID.randomUUID().toString();
    field("noteTitle", "Title", 20, 102, 180, 48);
    multiline("noteText", "Write your note…", 20, 136, 180, 240, 2048);
    hit(
        "Save",
        20,
        393,
        83,
        25,
        () -> send("save_note", selected, text("noteText"), text("noteTitle")),
        null,
        0xFFAD8826);
    hit(
        "Delete",
        116,
        393,
        84,
        25,
        () -> {
          send("delete_note", selected, "", "");
          navigate("eden:notes");
        },
        null,
        0xFF943A43);
  }

  private void calculator() {
    title("Calculator", () -> navigate("home"));
    art.add(g -> label(g, clip(calc.isEmpty() ? "0" : calc, 176), 22, 100, WHITE, 1.4F));
    String[] keys = {
      "C", "(", "%", "/", "7", "8", "9", "*", "4", "5", "6", "-", "1", "2", "3", "+", "del", "0",
      ".", "="
    };
    for (int i = 0; i < keys.length; i++) {
      String k = keys[i];
      hit(
          k,
          20 + i % 4 * 47,
          151 + i / 4 * 47,
          39,
          37,
          () -> {
            switch (k) {
              case "C" -> calc = "";
              case "del" -> calc = calc.isEmpty() ? "" : calc.substring(0, calc.length() - 1);
              case "=" -> {
                try {
                  calc = String.valueOf(Calculator.evaluate(calc));
                } catch (IllegalArgumentException e) {
                  notice(e.getMessage());
                }
              }
              case "%" -> calc += "/100";
              case "(" ->
                  calc +=
                      calc.chars().filter(c -> c == '(').count()
                              > calc.chars().filter(c -> c == ')').count()
                          ? ")"
                          : "(";
              default -> {
                if (calc.length() < 120) calc += k;
              }
            }
          },
          null,
          i % 4 == 3 ? 0xFFB27D24 : 0xFF343C4B);
    }
  }

  private void weather() {
    title("Weather", () -> navigate("home"));
    art.add(
        g -> {
          String c = value("weather");
          icon(
              g,
              "weather/"
                  + (c.equals("Thunderstorm") ? "thunder" : c.equals("Rain") ? "rain" : "sun"),
              60,
              101,
              100);
          centered(g, c, 219, WHITE, 1.6F);
          centered(g, "World day " + value("day"), 255, MUTED, 1);
          paragraph(g, "Current conditions in " + value("dimension"), 25, 289, 170, MUTED);
          paragraph(g, "Weather updates while the phone is open.", 25, 338, 170, MUTED);
        });
  }

  private void settings() {
    title("Settings", () -> navigate("home"));
    art.add(
        g -> {
          label(g, "MY PHONE", 22, 88, MUTED, .85F);
          label(g, value("number"), 22, 107, WHITE, 1.3F);
          label(g, value("carrier"), 22, 131, MUTED, .9F);
        });
    hit("Wallpaper", 20, 164, 180, 33, () -> navigate("wallpapers"), null, 0xFF343C4B);
    hit(
        sim() ? "Eject SIM" : "Insert SIM",
        20,
        209,
        180,
        33,
        () -> send(sim() ? "eject" : "insert", "", "", ""),
        null,
        0xFF343C4B);
    hit("Manage apps", 20, 254, 180, 33, () -> navigate("eden:appstore"), null, 0xFF343C4B);
    hit("Network & SIM", 20, 294, 180, 30, () -> navigate("eden:network"), null, 0xFF343C4B);
    if (!value("kind").equals("desktop"))
      hit("Battery", 20, 334, 180, 29, () -> navigate("eden:battery"), null, 0xFF343C4B);
    art.add(
        g ->
            paragraph(
                g,
                "E.D.E.N. · SPhone adaptation\n" + value("kind") + " · NeoForge 1.21.1",
                24,
                378,
                172,
                MUTED));
  }

  private void battery() {
    title(powered() ? "Battery" : "Power off", powered() ? () -> navigate("home") : this::onClose);
    var b = data.has("battery") ? data.getAsJsonObject("battery") : new JsonObject();
    String type = b.has("type") ? b.get("type").getAsString() : "none";
    int charge = b.has("charge") ? b.get("charge").getAsInt() : 0,
        capacity = b.has("capacity") ? b.get("capacity").getAsInt() : 0;
    art.add(
        g -> {
          rounded(g, 76, 92, 65, 31, 4, 0xFF7D879A);
          g.fill(141, 101, 145, 113, 0xFF7D879A);
          int fill = capacity == 0 ? 0 : (int) (57L * charge / capacity);
          g.fill(80, 96, 80 + fill, 119, charge > 0 ? 0xFF35B864 : 0xFFBE4349);
          centered(
              g,
              type.equals("none")
                  ? "No battery installed"
                  : type.equals("creative")
                      ? "Creative · Infinite"
                      : type + " · " + 100L * charge / Math.max(1, capacity) + "%",
              141,
              WHITE,
              1);
          if (!type.equals("none") && !type.equals("creative"))
            centered(
                g,
                String.format(Locale.ROOT, "%.1f Minecraft days remaining", charge / 24000.0),
                163,
                MUTED,
                .9F);
          paragraph(
              g,
              "Put the whole phone on a powered Charging Station, or remove the battery and charge"
                  + " it separately. Shift-right-click the station to open its slots.",
              24,
              192,
              172,
              MUTED);
        });
    if (!type.equals("none"))
      hit(
          "Remove battery",
          20,
          285,
          180,
          29,
          () -> send("eject_battery", "", "", ""),
          null,
          0xFF343C4B);
    else {
      var list = array("batteries");
      if (list.isEmpty())
        art.add(
            g ->
                paragraph(
                    g, "Put a battery in your inventory to install it.", 24, 290, 172, MUTED));
      for (int i = page * 3; i < Math.min(page * 3 + 3, list.size()); i++) {
        var cell = list.get(i).getAsJsonObject();
        String label =
            cell.get("type").getAsString()
                + " · "
                + 100L
                    * cell.get("charge").getAsLong()
                    / Math.max(1, cell.get("capacity").getAsLong())
                + "%";
        hit(
            "Install " + label,
            20,
            279 + i % 3 * 35,
            180,
            28,
            () -> send("insert_battery", cell.get("slot").getAsString(), "", ""),
            null,
            BLUE);
      }
      pagination(list.size(), 3);
    }
  }

  private void wallpapers() {
    title("Wallpaper", () -> navigate("eden:settings"));
    var list =
        List.of("b1", "deauville", "stmichel", "oscuridad", "iluminacion", "geometry", "acsgui");
    for (int i = 0; i < list.size(); i++) {
      String id = list.get(i);
      int x = 20 + i % 3 * 62, y = 91 + i / 3 * 98;
      art.add(g -> texture(g, "textures/ui/background/" + id + ".png", x, y, 52, 87));
      hit("Use " + id, x, y, 52, 87, () -> send("wallpaper", id, "", ""), null, 0, false);
    }
  }

  private void appStore() {
    title("App Store", () -> navigate("home"));
    var list = array("apps");
    Set<String> installed = new HashSet<>();
    for (var i : array("installed")) installed.add(i.getAsString());
    for (int i = page * 5; i < Math.min(page * 5 + 5, list.size()); i++) {
      var d = list.get(i).getAsJsonObject();
      String id = d.get("id").getAsString();
      int y = 87 + i % 5 * 59;
      boolean has = installed.contains(id);
      art.add(
          g -> {
            icon(g, appIconPath(id), 20, y, 32);
            label(g, clip(d.get("title").getAsString(), 132), 61, y + 3, WHITE, 1);
          });
      hit(
          has ? "Remove" : "Get",
          61,
          y + 23,
          73,
          21,
          () -> send(has ? "uninstall_app" : "install_app", id, "", ""),
          null,
          has ? 0xFF343C4B : 0xFF1957D3);
      if (has) hit("Open", 140, y + 23, 60, 21, () -> openApp(id), null, 0xFF343C4B);
    }
    pagination(list.size(), 5);
  }

  private void phone() {
    title("Phone", () -> navigate("home"));
    String state = callState();
    if (!state.equals("idle")) {
      art.add(
          g -> {
            icon(g, "contacts", 78, 105, 64);
            centered(g, clip(name(callNumber()), 172), 194, WHITE, 1.3F);
            centered(
                g,
                state.equals("incoming")
                    ? "Incoming call"
                    : state.equals("ringing") ? "Calling…" : "Connected",
                226,
                MUTED,
                1);
            if (state.equals("active")) {
              long seconds =
                  Math.max(
                      0,
                      (System.currentTimeMillis()
                              - data.getAsJsonObject("call").get("since").getAsLong())
                          / 1000);
              centered(g, String.format("%02d:%02d", seconds / 60, seconds % 60), 253, WHITE, 1.1F);
            }
          });
      if (state.equals("incoming"))
        hit("Answer", 30, 300, 160, 29, () -> send("answer", "", "", ""), null, 0xFF249947);
      hit(
          state.equals("incoming") ? "Decline" : "Hang up",
          30,
          344,
          160,
          29,
          () -> send("hangup", "", "", ""),
          null,
          0xFFBB3742);
      art.add(
          g ->
              paragraph(
                  g, "Use your Simple Voice Chat microphone key to speak.", 25, 384, 170, MUTED));
      return;
    }
    field("dialNumber", "Phone number", 27, 99, 166, 20);
    String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "del"};
    for (int i = 0; i < keys.length; i++) {
      String k = keys[i];
      hit(
          k,
          30 + i % 3 * 57,
          144 + i / 3 * 42,
          46,
          34,
          () -> {
            String n = text("dialNumber");
            fields
                .get("dialNumber")
                .setValue(
                    k.equals("Clear")
                        ? ""
                        : k.equals("del")
                            ? n.isEmpty() ? "" : n.substring(0, n.length() - 1)
                            : n + k);
          },
          null,
          0xFF343C4B);
    }
    hit("Call", 30, 332, 160, 29, () -> send("call", text("dialNumber"), "", ""), null, 0xFF249947);
    art.add(
        g ->
            paragraph(
                g,
                data.has("voiceAvailable") && data.get("voiceAvailable").getAsBoolean()
                    ? "Simple Voice Chat ready"
                    : "Calls require Simple Voice Chat on both players and the server.",
                26,
                380,
                168,
                MUTED));
  }

  private void camera() {
    title("Camera", () -> navigate("home"));
    empty(
        "Capture your world",
        "The phone closes for a clear view. Press F2 to take a photo; reopen Gallery to view it.",
        "photo");
    hit("Open viewfinder", 30, 315, 160, 28, this::onClose, null, BLUE);
  }

  private void gallery() {
    title("Gallery", () -> navigate("home"));
    var photos = PhoneGallery.photos(minecraft);
    int start = page * 6;
    if (photos.isEmpty())
      empty(
          "Your photos",
          "Press F2 in the world to capture a photo. Screenshots stay on this computer.",
          "gallery");
    for (int i = start; i < Math.min(start + 6, photos.size()); i++) {
      var file = photos.get(i);
      int x = 20 + (i - start) % 2 * 95, y = 91 + (i - start) / 2 * 86;
      var image = PhoneGallery.texture(minecraft, file);
      if (image != null) art.add(g -> g.blit(image, x, y, 0, 0, 85, 65, 85, 65));
      hit(
          file.getFileName().toString(),
          x,
          y,
          85,
          65,
          () -> minecraft.setScreen(new PhotoScreen(this, file)),
          null,
          0,
          false);
      art.add(g -> label(g, clip(file.getFileName().toString(), 88), x, y + 68, MUTED, .75F));
    }
    pagination(photos.size(), 6);
  }

  private int gameTarget = 8, gameScore;

  private void plusGame() {
    title("++ Game", () -> navigate("home"));
    art.add(
        g -> {
          centered(g, "Make " + gameTarget, 116, WHITE, 1.6F);
          centered(g, "Score " + gameScore, 151, MUTED, 1);
        });
    for (int i = 1; i <= 9; i++) {
      int k = i;
      hit(
          String.valueOf(i),
          30 + (i - 1) % 3 * 57,
          202 + (i - 1) / 3 * 44,
          46,
          35,
          () -> {
            gameTarget -= k;
            if (gameTarget == 0) {
              gameScore++;
              gameTarget = 10 + new Random().nextInt(40);
              notice("Nice! Next number");
            } else if (gameTarget < 0) {
              gameScore = 0;
              gameTarget = 8;
              notice("Too far. Try again");
            }
          },
          null,
          0xFF973F90);
    }
  }

  private void studio() {
    title("App Studio", () -> navigate("home"));
    field("appId", "App/service ID", 20, 87, 180, 32);
    field("appTitle", "App title", 20, 117, 180, 48);
    multiline("appText", "App or bulletin text", 20, 147, 180, 120, 1024);
    field("serviceId", "studio:service_id", 20, 280, 180, 64);
    hit(
        "Publish app",
        20,
        314,
        180,
        24,
        () -> send("publish_app", text("appId"), text("appText"), text("appTitle")),
        null,
        BLUE);
    hit(
        "Link service",
        20,
        344,
        85,
        24,
        () -> send("link_service", text("appId"), "", text("serviceId")),
        null,
        0xFF343C4B);
    hit(
        "Publish service",
        111,
        344,
        89,
        24,
        () -> send("publish_service", text("appId"), text("appText"), ""),
        null,
        0xFF343C4B);
    art.add(
        g -> paragraph(g, "Install your published app from the App Store.", 24, 386, 172, MUTED));
  }

  private void network() {
    title("Network", () -> navigate("home"));
    art.add(
        g -> {
          label(g, value("connection"), 22, 88, WHITE, 1.3F);
          paragraph(g, value("networkName"), 22, 114, 174, MUTED);
        });
    drafts.putIfAbsent("wifiSsid", value("wifi"));
    field("wifiSsid", "Wi-Fi SSID", 20, 159, 180, 48);
    field("wifiPassword", "Wi-Fi password (blank = open)", 20, 189, 180, 128);
    hit(
        "Join Wi-Fi",
        20,
        220,
        180,
        25,
        () -> send("wifi", text("wifiSsid"), text("wifiPassword"), ""),
        null,
        BLUE);
    field("carrierName", "SIM carrier (e.g. Alyon Wireless)", 20, 267, 180, 48);
    hit(
        "Set SIM carrier",
        20,
        297,
        180,
        25,
        () -> send("carrier", text("carrierName"), "", ""),
        null,
        0xFF343C4B);
    art.add(
        g ->
            paragraph(
                g,
                "Towers need power and an Ethernet uplink. Routers need a gateway. Desktop Ethernet"
                    + " works with an adjacent cable.",
                24,
                343,
                172,
                MUTED));
  }

  private void packages() {
    title("Packages", () -> navigate("home"));
    var racks = array("racks");
    if (racks.isEmpty()) {
      empty(
          "No server rack",
          "Join a Wi-Fi or Ethernet LAN connected to a powered server rack.",
          "devapp");
      return;
    }
    var rack = racks.get(0).getAsJsonObject();
    for (var r : racks)
      if (r.getAsJsonObject().get("id").getAsString().equals(selected)) rack = r.getAsJsonObject();
    selected = rack.get("id").getAsString();
    final JsonObject current = rack;
    art.add(
        g -> {
          label(g, clip(current.get("name").getAsString(), 176), 22, 85, WHITE, 1);
          paragraph(
              g,
              "Files: "
                  + current.getAsJsonArray("files").asList().stream()
                      .map(JsonElement::getAsString)
                      .collect(java.util.stream.Collectors.joining(", ")),
              22,
              106,
              176,
              MUTED);
        });
    if (racks.size() > 1)
      hit(
          "Next rack",
          134,
          80,
          66,
          20,
          () -> {
            int index = 0;
            for (int i = 0; i < racks.size(); i++)
              if (racks.get(i).getAsJsonObject().get("id").getAsString().equals(selected))
                index = i;
            selected =
                racks.get((index + 1) % racks.size()).getAsJsonObject().get("id").getAsString();
            preserve();
            rebuildWidgets();
          },
          null,
          0xFF343C4B);
    field("packageName", "Package name", 20, 163, 180, 40);
    field("packagePassword", "Package passphrase (8+ characters)", 20, 193, 180, 128);
    multiline("packageBody", "App definition or package data", 20, 225, 180, 109, 2048);
    hit(
        "Download",
        20,
        345,
        86,
        24,
        () ->
            send("package_get", selected + "/" + text("packageName"), "", text("packagePassword")),
        null,
        BLUE);
    if (rack.get("mine").getAsBoolean()) {
      hit(
          "Upload",
          114,
          345,
          86,
          24,
          () ->
              send(
                  "package_put",
                  selected + "/" + text("packageName"),
                  text("packageBody"),
                  text("packagePassword")),
          null,
          0xFF343C4B);
      hit(
          "Export disk",
          20,
          376,
          86,
          24,
          () -> send("package_export", selected + "/" + text("packageName"), "", ""),
          null,
          0xFF343C4B);
      hit(
          "Import disk",
          114,
          376,
          86,
          24,
          () ->
              send(
                  "package_import",
                  selected + "/" + (text("packageName").isBlank() ? "import" : text("packageName")),
                  "",
                  ""),
          null,
          0xFF343C4B);
    }
    art.add(g -> label(g, "AES-GCM · encrypted storage", 24, 405, MUTED, .8F));
  }

  private void customApp() {
    JsonObject descriptor = null;
    for (var e : array("apps")) {
      var d = e.getAsJsonObject();
      if (d.get("id").getAsString().equals(app)) descriptor = d;
    }
    if (descriptor == null) {
      title("App unavailable", () -> navigate("home"));
      return;
    }
    var d = descriptor;
    title(d.get("title").getAsString(), () -> navigate("home"));
    art.add(g -> paragraph(g, d.get("text").getAsString(), 24, 87, 172, WHITE));
    String service = d.get("service").getAsString();
    if (!service.isBlank()) {
      field("arguments", "Arguments (JSON)", 20, 220, 180, 2048);
      hit(
          "Run",
          20,
          251,
          180,
          26,
          () -> send("service", service, text("arguments"), ""),
          null,
          BLUE);
      art.add(g -> paragraph(g, serviceOutput, 24, 294, 172, MUTED));
    }
  }

  private void pagination(int count, int size) {
    if (count <= size) return;
    hit(
        "<",
        20,
        395,
        35,
        20,
        () -> {
          preserve();
          page = Math.max(0, page - 1);
          rebuildWidgets();
        },
        null,
        0xFF343C4B);
    hit(
        ">",
        165,
        395,
        35,
        20,
        () -> {
          preserve();
          page = (page + 1) * size < count ? page + 1 : 0;
          rebuildWidgets();
        },
        null,
        0xFF343C4B);
    art.add(
        g ->
            centered(
                g, (page + 1) + " / " + Math.max(1, (count + size - 1) / size), 402, MUTED, .8F));
  }

  private void empty(String title, String text, String icon) {
    art.add(
        g -> {
          icon(g, icon, 82, 122, 56);
          centered(g, title, 203, WHITE, 1);
          paragraph(g, text, 29, 235, 162, MUTED);
        });
  }

  private void notice(String text) {
    toast = text;
    toastUntil = System.currentTimeMillis() + 5000;
  }

  private EditBox field(String key, String hint, int x, int y, int w, int max) {
    var b = new EditBox(font, x, y, w, 22, Component.literal(hint));
    b.setMaxLength(max);
    b.setHint(Component.literal(font.plainSubstrByWidth(hint, w - 8)));
    b.setValue(drafts.getOrDefault(key, ""));
    fields.put(key, b);
    addRenderableWidget(b);
    return b;
  }

  private void multiline(String key, String hint, int x, int y, int w, int h, int max) {
    multilineKey = key;
    multiline =
        new MultiLineEditBox(font, x, y, w, h, Component.literal(hint), Component.literal(hint)) {
          public void renderWidget(GuiGraphics g, int mx, int my, float p) {
            if (!visible) return;
            renderBackground(g);
            clipArea(g, getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1);
            g.pose().pushPose();
            g.pose().translate(0, -scrollAmount(), 0);
            renderContents(g, mx, my, p);
            g.pose().popPose();
            g.disableScissor();
            renderDecorations(g);
          }
        };
    multiline.setCharacterLimit(max);
    multiline.setValue(drafts.getOrDefault(key, ""));
    addRenderableWidget(multiline);
  }

  private void hit(
      String name, int x, int y, int w, int h, Runnable action, String icon, int color) {
    hit(name, x, y, w, h, action, icon, color, true);
  }

  private void hit(
      String name,
      int x,
      int y,
      int w,
      int h,
      Runnable action,
      String icon,
      int color,
      boolean drawLabel) {
    addRenderableWidget(
        new AbstractWidget(x, y, w, h, Component.literal(name)) {
          protected void renderWidget(GuiGraphics g, int mx, int my, float p) {
            if (color != 0) rounded(g, getX(), getY(), getWidth(), getHeight(), 6, color);
            if (isHoveredOrFocused())
              rounded(g, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 6, 0x44FFFFFF);
            if (icon != null) icon(g, icon, getX(), getY(), Math.min(getWidth(), getHeight()));
            else if (drawLabel && !name.equals("Home"))
              g.drawCenteredString(
                  font,
                  clip(name, getWidth() - 6),
                  getX() + getWidth() / 2,
                  getY() + (getHeight() - 8) / 2,
                  WHITE);
          }

          public void onClick(double x, double y) {
            action.run();
          }

          protected void updateWidgetNarration(NarrationElementOutput o) {
            defaultButtonNarrationText(o);
          }

          public boolean keyPressed(int k, int s, int m) {
            if (isFocused() && (k == 257 || k == 32)) {
              action.run();
              return true;
            }
            return super.keyPressed(k, s, m);
          }
        });
  }

  private String clip(String s, int width) {
    return font.plainSubstrByWidth(s.replace('\n', ' '), Math.max(1, width));
  }

  private void texture(GuiGraphics g, String p, int x, int y, int w, int h) {
    g.blit(ResourceLocation.fromNamespaceAndPath("sphone", p), x, y, 0, 0, w, h, w, h);
  }

  private void icon(GuiGraphics g, String p, int x, int y, int size) {
    texture(g, "textures/ui/icons/" + p + ".png", x, y, size, size);
  }

  private void label(GuiGraphics g, String s, float x, float y, int color, float zoom) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(zoom, zoom, 1);
    g.drawString(font, s, 0, 0, color, false);
    g.pose().popPose();
  }

  private void centered(GuiGraphics g, String s, int y, int color, float zoom) {
    label(g, s, (W - font.width(s) * zoom) / 2F, y, color, zoom);
  }

  private void paragraph(GuiGraphics g, String s, int x, int y, int width, int color) {
    for (var line : font.split(Component.literal(s), width)) {
      if (y > 408) break;
      g.drawString(font, line, x, y, color, false);
      y += 13;
    }
  }

  static void rounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
    for (int row = 0; row < h; row++) {
      int d = row < r ? r - row - 1 : row >= h - r ? row - (h - r) : 0;
      int inset = d > 0 ? r - (int) Math.sqrt(r * r - d * d) : 0;
      g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
    }
  }

  private void clipArea(GuiGraphics g, int x, int y, int right, int bottom) {
    g.enableScissor(
        Math.round(left + x * scale),
        Math.round(top + y * scale),
        Math.round(left + right * scale),
        Math.round(top + bottom * scale));
  }

  public void render(GuiGraphics g, int mx, int my, float p) {
    int vx = (int) ((mx - left) / scale), vy = (int) ((my - top) / scale);
    g.pose().pushPose();
    g.pose().translate(left, top, 0);
    g.pose().scale(scale, scale, 1);
    texture(
        g,
        "textures/ui/background/"
            + (value("wallpaper").isBlank() ? "b1" : value("wallpaper"))
            + ".png",
        0,
        0,
        W,
        H);
    if (!app.equals("home")) rounded(g, 8, 9, 204, 432, 18, 0xFF171C26);
    texture(g, "textures/ui/background/camera.png", 78, 15, 64, 19);
    label(g, new SimpleDateFormat("HH:mm").format(new Date()), 22, 22, WHITE, .8F);
    texture(g, "textures/ui/icons/other.png", 166, 24, 30, 9);
    if (data.has("battery")) {
      var b = data.getAsJsonObject("battery");
      int cap = b.get("capacity").getAsInt();
      label(
          g, cap == 0 ? "0%" : 100L * b.get("charge").getAsInt() / cap + "%", 173, 37, MUTED, .6F);
    }
    for (var d : art) d.render(g);
    for (var child : renderables) child.render(g, vx, vy, p);
    rounded(g, 72, 433, 76, 4, 2, 0xFFFFFFFF);
    if (System.currentTimeMillis() < toastUntil && !toast.isBlank()) {
      rounded(g, 14, 42, 192, 30, 7, 0xF038465C);
      label(g, clip(toast, 174), 21, 52, WHITE, .85F);
    }
    g.pose().popPose();
  }

  public void onClose() {
    PhoneGallery.clear(minecraft);
    super.onClose();
  }

  public boolean isPauseScreen() {
    return false;
  }

  public boolean mouseClicked(double x, double y, int b) {
    return super.mouseClicked((x - left) / scale, (y - top) / scale, b);
  }

  public boolean mouseReleased(double x, double y, int b) {
    return super.mouseReleased((x - left) / scale, (y - top) / scale, b);
  }

  public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
    return super.mouseDragged((x - left) / scale, (y - top) / scale, b, dx / scale, dy / scale);
  }

  public boolean mouseScrolled(double x, double y, double sx, double sy) {
    if (app.equals("conversation") && (y - top) / scale < 375) {
      chatScroll = Math.max(0, chatScroll + (int) (sy * 28));
      return true;
    }
    return super.mouseScrolled((x - left) / scale, (y - top) / scale, sx, sy);
  }

  public boolean keyPressed(int k, int s, int m) {
    if (k == 257 && app.equals("conversation") && fields.get("message").isFocused()) {
      sendMessage();
      return true;
    }
    if (k == 256 && !app.equals("home") && powered()) {
      navigate("home");
      return true;
    }
    return super.keyPressed(k, s, m);
  }
}
