package net.realityradio.eden.core;
import java.util.*;
/** Removable battery; one FE per elapsed world game tick while installed. */
public final class BatteryPack {
 public final UUID id; public final String type; public int charge; public long lastTick;
 public BatteryPack(UUID id,String type,int charge,long tick){this.id=Objects.requireNonNull(id);this.type=type;this.charge=Math.max(0,Math.min(charge,capacity(type)));lastTick=tick;}
 public static int days(String type){return switch(type){case "coal"->8;case "copper"->15;case "iron"->30;case "gold"->40;case "diamond"->60;case "netherite"->80;case "creative"->Integer.MAX_VALUE;default->throw new IllegalArgumentException("Unknown battery type");};}
 public static int capacity(String type){return type.equals("creative")?Integer.MAX_VALUE:days(type)*24000;}
 public boolean creative(){return type.equals("creative");}
 public boolean advance(long tick){long elapsed=Math.max(0,tick-lastTick);lastTick=tick;if(creative())return false;int old=charge;charge=(int)Math.max(0,(long)charge-elapsed);return old!=charge;}
 public boolean powered(){return creative()||charge>0;}
}
