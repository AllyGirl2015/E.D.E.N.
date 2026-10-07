package net.realityradio.eden.client;
import java.nio.file.Path;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.Screen;import net.minecraft.client.gui.components.Button;import net.minecraft.network.chat.Component;
final class PhotoScreen extends Screen {
 private final Screen parent;private final Path file;PhotoScreen(Screen parent,Path file){super(Component.literal(file.getFileName().toString()));this.parent=parent;this.file=file;}
 protected void init(){addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(10,10,60,20).build());}
 public void render(GuiGraphics g,int x,int y,float dt){g.fill(0,0,width,height,0xff0b1018);var id=PhoneGallery.texture(minecraft,file);if(id!=null)g.blit(id,20,45,0,0,width-40,height-70,width-40,height-70);super.render(g,x,y,dt);}
 public void onClose(){minecraft.setScreen(parent);}public boolean isPauseScreen(){return false;}
}
