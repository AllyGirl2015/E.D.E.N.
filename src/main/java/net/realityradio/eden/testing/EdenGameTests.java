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


    @GameTest(template = "empty")
    public static void looseBatteryAndWholePhoneCharge(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));
        level.setBlockAndUpdate(pos,Eden.CHARGER.get().defaultBlockState());
        var station=(net.realityradio.eden.infrastructure.NetworkNodeEntity)level.getBlockEntity(pos);
        var coal=new ItemStack(Eden.BATTERIES.get("coal").get());
        var cell=coal.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(cell!=null&&cell.getMaxEnergyStored()==192000,"Coal has eight days of FE capacity");
        helper.assertTrue(cell.receiveEnergy(500,true)==500&&cell.getEnergyStored()==0,"Loose battery simulation is read only");
        station.inventory.setStackInSlot(0,coal);station.energy.receiveEnergy(4096,false);
        net.realityradio.eden.infrastructure.NetworkNodeEntity.tick(level,pos,station.getBlockState(),station);
        helper.assertTrue(cell.getEnergyStored()==1024&&station.energy.getEnergyStored()==3070,"Charger transfers FE and pays idle draw");
        station.inventory.setStackInSlot(0,ItemStack.EMPTY);
        var id=UUID.randomUUID();var record=new DeviceRecord(id,"phone");
        record.battery=new net.realityradio.eden.core.BatteryPack(UUID.randomUUID(),"copper",0,level.getServer().overworld().getGameTime());
        EdenSavedData.get(level.getServer()).network.devices.put(id,record);
        var phone=new ItemStack(Eden.PHONE.get());phone.set(Eden.DEVICE_ID.get(),id);station.inventory.setStackInSlot(0,phone);
        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,pos,null)!=null,"Automation can insert a whole phone");
        net.realityradio.eden.infrastructure.NetworkNodeEntity.tick(level,pos,station.getBlockState(),station);
        helper.assertTrue(record.battery.charge==1024,"Phone charges its installed battery");
        var phoneEnergy=phone.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(phoneEnergy.receiveEnergy(500,true)==500&&record.battery.charge==1024,"Other mod chargers can simulate whole-phone charging");
        helper.assertTrue(phoneEnergy.receiveEnergy(500,false)==500&&record.battery.charge==1524,"Direct FE charging reaches the installed battery");
        record.battery=null;helper.assertTrue(!phoneEnergy.canReceive()&&phoneEnergy.receiveEnergy(500,false)==0,"Removing the battery stops charging");helper.succeed();
    }
    @GameTest(template = "empty")
    public static void cableCutsAndPowerLossDisconnectBackhaul(GameTestHelper helper) {
        var level=helper.getLevel();var routerPos=helper.absolutePos(new BlockPos(1,1,1));var cablePos=routerPos.east();var gatewayPos=cablePos.east();
        level.setBlockAndUpdate(routerPos,Eden.ROUTER.get().defaultBlockState());level.setBlockAndUpdate(cablePos,Eden.CABLE.get().defaultBlockState());level.setBlockAndUpdate(gatewayPos,Eden.GATEWAY.get().defaultBlockState());
        var router=(net.realityradio.eden.infrastructure.NetworkNodeEntity)level.getBlockEntity(routerPos);var gateway=(net.realityradio.eden.infrastructure.NetworkNodeEntity)level.getBlockEntity(gatewayPos);
        router.powered=true;gateway.powered=true;
        helper.assertTrue(net.realityradio.eden.infrastructure.Connectivity.uplink(level,router),"Powered router reaches cabled gateway");
        level.removeBlock(cablePos,false);helper.assertTrue(!net.realityradio.eden.infrastructure.Connectivity.uplink(level,router),"Cable cut disconnects WAN");
        level.setBlockAndUpdate(cablePos,Eden.WAN_CABLE.get().defaultBlockState());gateway.powered=false;helper.assertTrue(!net.realityradio.eden.infrastructure.Connectivity.uplink(level,router),"Unpowered gateway stops WAN");
        var fe=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,gatewayPos,null);
        helper.assertTrue(fe!=null&&fe.receiveEnergy(1000,true)==1000&&fe.getEnergyStored()==0,"Standard block FE simulates cleanly");fe.receiveEnergy(1000,false);helper.assertTrue(fe.getEnergyStored()==1000,"Standard FE powers node");helper.succeed();
    }
    @GameTest(template = "empty")
    public static void rackPreservesInventoryAndEncryptedCartridge(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));level.setBlockAndUpdate(pos,Eden.RACK.get().defaultBlockState());
        var rack=(net.realityradio.eden.infrastructure.NetworkNodeEntity)level.getBlockEntity(pos);rack.owner=UUID.randomUUID();rack.powered=true;
        rack.packages.put("shared",net.realityradio.eden.infrastructure.PackageVault.seal("Hello LAN","strong-password"));rack.inventory.setStackInSlot(0,new ItemStack(Eden.DATA_DISK.get()));
        helper.assertTrue(rack.exportDisk("shared"),"Encrypted package exports to physical cartridge");var disk=rack.inventory.getStackInSlot(0).copy();
        var tag=rack.saveWithFullMetadata(level.registryAccess());var owner=rack.owner;
        level.removeBlock(pos,false);level.setBlockAndUpdate(pos,Eden.RACK.get().defaultBlockState());rack=(net.realityradio.eden.infrastructure.NetworkNodeEntity)level.getBlockEntity(pos);rack.loadWithComponents(tag,level.registryAccess());rack.powered=true;
        helper.assertTrue(owner.equals(rack.owner)&&rack.inventory.getStackInSlot(0).is(Eden.DATA_DISK.get()),"Rack restores owner and inventory");rack.packages.clear();rack.inventory.setStackInSlot(0,disk);
        helper.assertTrue(rack.importDisk(),"Cartridge imports encrypted package");helper.assertTrue(net.realityradio.eden.infrastructure.PackageVault.open(rack.packages.get("shared"),"strong-password").equals("Hello LAN"),"Imported ciphertext decrypts with correct password");helper.succeed();
    }
}
