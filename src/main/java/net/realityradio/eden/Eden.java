package net.realityradio.eden;

import com.mojang.logging.LogUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import net.realityradio.eden.api.*;
import net.realityradio.eden.device.*;
import net.realityradio.eden.network.*;
import java.util.UUID;
import org.slf4j.Logger;

@Mod(Eden.MODID)
public final class Eden {
    public static final String MODID = "eden";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> DEVICE_ID = COMPONENTS.register("device_id",
        () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SIM_ID = COMPONENTS.register("sim_id",
        () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
    public static final DeferredItem<DeviceItem> PHONE = ITEMS.register("phone", () -> new DeviceItem(new Item.Properties(), "phone"));
    public static final DeferredItem<DeviceItem> TABLET = ITEMS.register("tablet", () -> new DeviceItem(new Item.Properties(), "tablet"));
    public static final DeferredItem<DeviceItem> LAPTOP = ITEMS.register("laptop", () -> new DeviceItem(new Item.Properties(), "laptop"));
    public static final DeferredItem<Item> SIM = ITEMS.registerSimpleItem("sim", new Item.Properties().stacksTo(1));
    public static final DeferredBlock<DeviceBlock> TERMINAL = BLOCKS.register("desktop", () -> new DeviceBlock(BlockBehaviour.Properties.of().strength(2)));
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ITEMS.registerSimpleBlockItem("desktop", TERMINAL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeviceBlockEntity>> TERMINAL_ENTITY = ENTITIES.register("desktop",
        () -> BlockEntityType.Builder.of(DeviceBlockEntity::new, TERMINAL.get()).build(null));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEVICES_TAB = TABS.register("devices", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.eden")).icon(() -> PHONE.get().getDefaultInstance())
        .displayItems((params, out) -> { out.accept(PHONE); out.accept(TABLET); out.accept(LAPTOP); out.accept(SIM); out.accept(TERMINAL_ITEM); }).build());

    public Eden(IEventBus modBus) {
        COMPONENTS.register(modBus); ITEMS.register(modBus); BLOCKS.register(modBus); ENTITIES.register(modBus); TABS.register(modBus);
        modBus.addListener(EdenNetworking::register);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            String[][] builtins = {{"messages","Messages"},{"contacts","Contacts"},{"notes","Notes"},{"weather","Weather"},
                {"calculator","Calculator"},{"settings","Settings"},{"studio","App Studio"}};
            for (var app : builtins) EdenPlatform.registerApp(new AppDefinition(id(app[0]), app[1], "", null));
            ModLoader.postEvent(new RegisterEdenPlatformEvent());
            EdenPlatform.freeze();
        }));
        NeoForge.EVENT_BUS.addListener(ServerDevices::logout);
        NeoForge.EVENT_BUS.addListener(ServerDevices::stopped);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }
}
