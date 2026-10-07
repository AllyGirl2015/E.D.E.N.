package net.realityradio.eden.client;
import java.nio.file.*;import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.texture.DynamicTexture;import net.minecraft.resources.ResourceLocation;import com.mojang.blaze3d.platform.NativeImage;
final class PhoneGallery {
 private static final Map<Path,ResourceLocation> CACHE=new HashMap<>();
 static List<Path> photos(Minecraft mc){Path dir=mc.gameDirectory.toPath().resolve("screenshots");if(!Files.isDirectory(dir))return List.of();try(var files=Files.list(dir)){return files.filter(p->Files.isRegularFile(p)&&p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).sorted(Comparator.comparingLong(PhoneGallery::modified).reversed()).limit(120).toList();}catch(Exception e){return List.of();}}
 private static long modified(Path p){try{return Files.getLastModifiedTime(p).toMillis();}catch(Exception e){return 0;}}
 static ResourceLocation texture(Minecraft mc,Path p){if(CACHE.containsKey(p))return CACHE.get(p);try(var input=Files.newInputStream(p)){if(Files.size(p)>32*1024*1024)return null;NativeImage img=NativeImage.read(input);if(img.getWidth()>8192||img.getHeight()>8192){img.close();return null;}var id=mc.getTextureManager().register("eden_gallery",new DynamicTexture(img));CACHE.put(p,id);return id;}catch(Exception e){return null;}}
 static void clear(Minecraft mc){CACHE.values().forEach(mc.getTextureManager()::release);CACHE.clear();}private PhoneGallery(){}
}
