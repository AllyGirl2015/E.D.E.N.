package net.realityradio.eden.integration;
import de.maxhenkel.voicechat.api.*;import de.maxhenkel.voicechat.api.events.*;import net.realityradio.eden.network.PhoneCalls;
@ForgeVoicechatPlugin
public final class EdenVoicePlugin implements VoicechatPlugin {
 public String getPluginId(){return "eden_phone";}
 public void registerEvents(EventRegistration r){r.registerEvent(VoicechatServerStartedEvent.class,e->{var api=e.getVoicechat();PhoneCalls.voiceStarted(id->{var c=api.getConnectionOf(id);return c!=null&&c.isInstalled()&&c.isConnected()&&!c.isDisabled();});});r.registerEvent(VoicechatServerStoppedEvent.class,e->PhoneCalls.voiceStopped());r.registerEvent(MicrophonePacketEvent.class,e->{var sender=e.getSenderConnection();if(sender==null)return;var route=PhoneCalls.route(sender.getPlayer().getUuid());if(route!=null)PrivateAudioRouter.forward(e,route.recipient(),route.channel());});}
}
