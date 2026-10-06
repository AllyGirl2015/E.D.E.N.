package net.realityradio.eden.testing;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.realityradio.eden.Eden;
import net.realityradio.eden.api.RegisterEdenPlatformEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.realityradio.eden.core.DeviceRecord;
import net.realityradio.eden.device.DeviceBlockEntity;
import net.realityradio.eden.storage.EdenSavedData;
import net.realityradio.eden.storage.NetworkJson;

/** Headless Minecraft integration checks; enabled only by NeoForge's GameTest harness. */
@EventBusSubscriber(modid = Eden.MODID, bus = EventBusSubscriber.Bus.MOD)
@GameTestHolder(Eden.MODID)
@PrefixGameTestTemplate(false)
public final class EdenGameTests {
    private static boolean sdkEventSeen;
    @SubscribeEvent
    public static void observeSdkRegistration(RegisterEdenPlatformEvent event) {
        sdkEventSeen = true;
    }
    @GameTest(template = "empty")
    public static void deviceRegistriesAndTerminalEntity(GameTestHelper helper) {
        helper.assertTrue(sdkEventSeen, "Addon registration event was broadcast to subscriber mod buses");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Eden.id("phone")), "Phone item registered");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Eden.id("sim")), "SIM item registered");
        var stack = new ItemStack(Eden.PHONE.get()); var id = UUID.randomUUID();
        stack.set(Eden.DEVICE_ID.get(), id);
        helper.assertTrue(id.equals(stack.copy().get(Eden.DEVICE_ID.get())), "Native component survives item copy");
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Eden.TERMINAL.get().defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof DeviceBlockEntity, "Desktop block entity created");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void savedDataStoresNativeWorldNetwork(GameTestHelper helper) {
        var saved = EdenSavedData.get(helper.getLevel().getServer());
        var id = UUID.randomUUID(); saved.network.devices.put(id, new DeviceRecord(id, "phone"));
        var sim = saved.network.issueSim("Alyon Wireless"); saved.network.insert(id, sim.id);
        saved.network.send(id, sim.number, "Integration SMS", 42);
        saved.setDirty();
        var tag = saved.save(new CompoundTag(), helper.getLevel().registryAccess());
        var restored = NetworkJson.decode(tag.getString("network"));
        helper.assertTrue(restored.account(id).messages.getFirst().body().equals("Integration SMS"), "SavedData stores complete message state");
        helper.assertTrue(saved.isDirty(), "Native world storage marks changed state dirty");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void breakingDesktopPreservesIdentityAndEjectsSim(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, Eden.TERMINAL.get().defaultBlockState());
        var block = (DeviceBlockEntity) level.getBlockEntity(pos);
        var data = EdenSavedData.get(level.getServer());
        var record = new DeviceRecord(block.deviceId, "desktop");
        record.notes = "Moving my computer";
        data.network.devices.put(record.id, record);
        var sim = data.network.issueSim("Alyon Wireless");
        data.network.insert(record.id, sim.id); data.setDirty();
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Eden.SIM.get()) && sim.id.equals(e.getItem().get(Eden.SIM_ID.get()))), "Breaking desktop ejects the exact SIM");
        var desktop = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(Eden.TERMINAL_ITEM.get())).findFirst().orElseThrow();
        helper.assertTrue(record.id.equals(desktop.get(Eden.DEVICE_ID.get())), "Desktop drop keeps identity");
        helper.assertTrue(record.sim == null && sim.installedDevice == null, "World SIM binding is cleared");
        level.setBlockAndUpdate(pos, Eden.TERMINAL.get().defaultBlockState());
        Eden.TERMINAL.get().setPlacedBy(level, pos, Eden.TERMINAL.get().defaultBlockState(), null, desktop);
        var replaced = (DeviceBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(replaced.deviceId.equals(record.id), "Replaced desktop restores identity");
        helper.assertTrue(data.network.device(replaced.deviceId).notes.equals("Moving my computer"), "Notes survive relocation");
        helper.succeed();
    }

}
