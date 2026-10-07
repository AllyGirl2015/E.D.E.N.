package net.realityradio.eden.core;
import java.util.*;
public final class DeviceRecord {
 public final UUID id; public final String kind; public UUID sim; public BatteryPack battery;
 public String notes="",theme="magenta",wallpaper="b1",wifiSsid="",wifiKey="";
 public Map<String,PhoneNote> notebook=new LinkedHashMap<>();public Set<String> installedApps=defaultApps();
 public static Set<String> defaultApps(){return new LinkedHashSet<>(List.of("eden:messages","eden:contacts","eden:notes","eden:weather","eden:settings","eden:phone","eden:camera","eden:gallery","eden:appstore","eden:network","eden:packages"));}
 public DeviceRecord(UUID id,String kind){this.id=Objects.requireNonNull(id);this.kind=Objects.requireNonNull(kind);}
}
