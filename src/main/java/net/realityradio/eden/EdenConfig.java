package net.realityradio.eden;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class EdenConfig {
 public static final ModConfigSpec SPEC;
 public static final ModConfigSpec.BooleanValue REQUIRE_NETWORK,REQUIRE_POWER,REQUIRE_BACKHAUL,REQUIRE_BATTERY;
 public static final ModConfigSpec.IntValue TOWER_BASE_RANGE,TOWER_MAX_RANGE,TOWER_BASE_FE,ROUTER_MAX_RANGE,ROUTER_FE,GATEWAY_FE,MAX_DRAW,BUFFER_FE,CABLE_LIMIT;
 static {var b=new ModConfigSpec.Builder();REQUIRE_NETWORK=b.define("requireNetwork",true);REQUIRE_POWER=b.define("requirePower",true);REQUIRE_BACKHAUL=b.define("requireBackhaul",true);REQUIRE_BATTERY=b.define("requireBattery",true);
 TOWER_BASE_RANGE=b.defineInRange("towerBaseRange",128,1,8192);TOWER_MAX_RANGE=b.defineInRange("towerMaxRange",1024,1,16384);TOWER_BASE_FE=b.defineInRange("towerBaseFE",32,1,100000);
 ROUTER_MAX_RANGE=b.defineInRange("routerMaxRange",48,1,8192);ROUTER_FE=b.defineInRange("routerFE",8,1,100000);GATEWAY_FE=b.defineInRange("gatewayFE",16,1,100000);
 MAX_DRAW=b.defineInRange("maxPowerDraw",8192,1,1000000);BUFFER_FE=b.defineInRange("bufferFE",200000,1,200000000);CABLE_LIMIT=b.defineInRange("cableSearchLimit",1024,64,65536);SPEC=b.build();}
 private EdenConfig(){}
}
