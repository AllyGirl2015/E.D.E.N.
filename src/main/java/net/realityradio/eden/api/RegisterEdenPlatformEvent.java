package net.realityradio.eden.api;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** Broadcast to every addon mod bus once during common setup after addon construction. */
public final class RegisterEdenPlatformEvent extends Event implements IModBusEvent {
  public void app(AppDefinition definition) {
    EdenPlatform.registerApp(definition);
  }

  public void service(ResourceLocation id, EdenService service) {
    EdenPlatform.registerService(id, service);
  }
}
