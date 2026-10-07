package net.realityradio.eden;
import com.mojang.logging.LogUtils;
import net.minecraft.core.UUIDUtil;import net.minecraft.core.component.DataComponentType;import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;import net.neoforged.fml.common.Mod;import net.neoforged.fml.*;import net.neoforged.fml.config.ModConfig;import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;import net.neoforged.neoforge.registries.*;import net.neoforged.neoforge.capabilities.*;
import net.realityradio.eden.api.*;import net.realityradio.eden.device.*;import net.realityradio.eden.network.*;import net.realityradio.eden.infrastructure.*;
import java.util.*;import org.slf4j.Logger;
@Mod(Eden.MODID)
public final class Eden {
 public static final String MODID="eden";public static final Logger LOGGER=LogUtils.getLogger();
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MODID);public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(MODID);
 public static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,MODID);
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MODID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MODID);
 private static DeferredHolder<DataComponentType<?>,DataComponentType<UUID>> uuid(String name){return COMPONENTS.register(name,()->DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());}
 public static final DeferredHolder<DataComponentType<?>,DataComponentType<UUID>> DEVICE_ID=uuid("device_id"),SIM_ID=uuid("sim_id"),BATTERY_ID=uuid("battery_id");
 public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> BATTERY_ENERGY=COMPONENTS.register("battery_energy",()->DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT).build());
 public static final Map<String,DeferredItem<BatteryItem>> BATTERIES=registerBatteries();
 private static Map<String,DeferredItem<BatteryItem>> registerBatteries(){var m=new LinkedHashMap<String,DeferredItem<BatteryItem>>();for(var t:List.of("coal","copper","iron","gold","diamond","netherite","creative"))m.put(t,ITEMS.register(t+"_battery",()->new BatteryItem(new Item.Properties(),t)));return m;}
 public static final DeferredItem<DeviceItem> PHONE=ITEMS.register("phone",()->new DeviceItem(new Item.Properties(),"phone")),TABLET=ITEMS.register("tablet",()->new DeviceItem(new Item.Properties(),"tablet")),LAPTOP=ITEMS.register("laptop",()->new DeviceItem(new Item.Properties(),"laptop"));
 public static final DeferredItem<Item> SIM=ITEMS.registerSimpleItem("sim",new Item.Properties().stacksTo(1)),DATA_DISK=ITEMS.registerSimpleItem("data_disk",new Item.Properties().stacksTo(1));
 public static final DeferredBlock<DeviceBlock> TERMINAL=BLOCKS.register("desktop",()->new DeviceBlock(BlockBehaviour.Properties.of().strength(2)));
 public static final DeferredItem<BlockItem> TERMINAL_ITEM=ITEMS.registerSimpleBlockItem("desktop",TERMINAL);
 private static DeferredBlock<NetworkNodeBlock> node(String name){return BLOCKS.register(name,()->new NetworkNodeBlock(BlockBehaviour.Properties.of().strength(3)));}
 public static final DeferredBlock<NetworkNodeBlock> TOWER=node("cell_tower"),ROUTER=node("wifi_router"),GATEWAY=node("internet_gateway"),RACK=node("server_rack"),CHARGER=node("charging_station");
 public static final DeferredBlock<EthernetBlock> CABLE=BLOCKS.register("ethernet_cable",()->new EthernetBlock(BlockBehaviour.Properties.of().strength(.5F).noOcclusion())),WAN_CABLE=BLOCKS.register("wan_cable",()->new EthernetBlock(BlockBehaviour.Properties.of().strength(.5F).noOcclusion()));
 public static final DeferredItem<BlockItem> TOWER_ITEM=ITEMS.registerSimpleBlockItem("cell_tower",TOWER),ROUTER_ITEM=ITEMS.registerSimpleBlockItem("wifi_router",ROUTER),GATEWAY_ITEM=ITEMS.registerSimpleBlockItem("internet_gateway",GATEWAY),RACK_ITEM=ITEMS.registerSimpleBlockItem("server_rack",RACK),CHARGER_ITEM=ITEMS.registerSimpleBlockItem("charging_station",CHARGER),CABLE_ITEM=ITEMS.registerSimpleBlockItem("ethernet_cable",CABLE),WAN_CABLE_ITEM=ITEMS.registerSimpleBlockItem("wan_cable",WAN_CABLE);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<NetworkNodeEntity>> NETWORK_ENTITY=ENTITIES.register("network_node",()->BlockEntityType.Builder.of(NetworkNodeEntity::new,TOWER.get(),ROUTER.get(),GATEWAY.get(),RACK.get(),CHARGER.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DeviceBlockEntity>> TERMINAL_ENTITY=ENTITIES.register("desktop",()->BlockEntityType.Builder.of(DeviceBlockEntity::new,TERMINAL.get()).build(null));
 public static final DeferredHolder<CreativeModeTab,CreativeModeTab> DEVICES_TAB=TABS.register("devices",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.eden")).icon(()->PHONE.get().getDefaultInstance()).displayItems((p,o)->{
  for(var i:List.of(PHONE,TABLET,LAPTOP,SIM,DATA_DISK,TERMINAL_ITEM,TOWER_ITEM,ROUTER_ITEM,GATEWAY_ITEM,RACK_ITEM,CHARGER_ITEM,CABLE_ITEM,WAN_CABLE_ITEM))o.accept(i.get());BATTERIES.values().forEach(i->o.accept(i.get()));}).build());
 public Eden(IEventBus bus,ModContainer container){container.registerConfig(ModConfig.Type.SERVER,EdenConfig.SPEC);COMPONENTS.register(bus);ITEMS.register(bus);BLOCKS.register(bus);ENTITIES.register(bus);TABS.register(bus);bus.addListener(EdenNetworking::register);
  bus.addListener((RegisterCapabilitiesEvent e)->{for(var b:BATTERIES.values())e.registerItem(Capabilities.EnergyStorage.ITEM,(s,c)->((BatteryItem)s.getItem()).energy(s),b.get());e.registerItem(Capabilities.EnergyStorage.ITEM,(s,c)->BatteryPower.itemEnergy(s),PHONE.get(),TABLET.get(),LAPTOP.get());e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,NETWORK_ENTITY.get(),(n,c)->n.energy);e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,NETWORK_ENTITY.get(),(n,c)->Set.of("rack","charger").contains(n.kind())?n.inventory:null);});
  bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{String[][] apps={{"messages","Messages"},{"contacts","Contacts"},{"notes","Notes"},{"weather","Weather"},{"calculator","Calculator"},{"settings","Settings"},{"studio","App Studio"},{"phone","Phone"},{"camera","Camera"},{"gallery","Gallery"},{"appstore","App Store"},{"plusplus","++ Game"},{"network","Network"},{"packages","Packages"},{"battery","Battery"}};for(var a:apps)EdenPlatform.registerApp(new AppDefinition(id(a[0]),a[1],"",null));ModLoader.postEvent(new RegisterEdenPlatformEvent());EdenPlatform.freeze();}));
  NeoForge.EVENT_BUS.addListener(ServerDevices::logout);NeoForge.EVENT_BUS.addListener(ServerDevices::stopped);NeoForge.EVENT_BUS.addListener(PhoneCalls::tick);
 }
 public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(MODID,path);}
}
