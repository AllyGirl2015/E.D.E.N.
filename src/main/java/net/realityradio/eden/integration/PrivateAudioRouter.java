package net.realityradio.eden.integration;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import java.util.UUID;

public final class PrivateAudioRouter {
  public static void forward(MicrophonePacketEvent event, UUID recipient, UUID channel) {
    if (recipient == null || channel == null) return;
    event.cancel();
    var receiver = event.getVoicechat().getConnectionOf(recipient);
    if (receiver == null || !receiver.isConnected() || receiver.isDisabled()) return;
    var packet = event.getPacket().staticSoundPacketBuilder().channelId(channel).build();
    event.getVoicechat().sendStaticSoundPacketTo(receiver, packet);
  }

  private PrivateAudioRouter() {}
}
