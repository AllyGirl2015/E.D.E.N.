package net.realityradio.eden.integration;

import static org.junit.jupiter.api.Assertions.*;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrivateAudioRouterTest {
  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> cls, java.util.function.Function<String, Object> f) {
    return (T)
        Proxy.newProxyInstance(
            cls.getClassLoader(), new Class[] {cls}, (p, m, a) -> f.apply(m.getName()));
  }

  @Test
  void phoneAudioIsCancelledBeforeOfflinePeerLookup() {
    var order = new ArrayList<String>();
    var api =
        proxy(
            VoicechatServerApi.class,
            n -> {
              order.add(n);
              return null;
            });
    var event =
        proxy(
            MicrophonePacketEvent.class,
            n -> {
              order.add(n);
              return n.equals("getVoicechat") ? api : n.equals("cancel") ? true : null;
            });
    PrivateAudioRouter.forward(event, UUID.randomUUID(), UUID.randomUUID());
    assertEquals("cancel", order.getFirst());
    assertTrue(order.contains("getConnectionOf"));
  }

  @Test
  void ordinaryProximityAudioIsUntouched() {
    var names = new ArrayList<String>();
    var event =
        proxy(
            MicrophonePacketEvent.class,
            n -> {
              names.add(n);
              return null;
            });
    PrivateAudioRouter.forward(event, null, null);
    assertTrue(names.isEmpty());
  }
}
