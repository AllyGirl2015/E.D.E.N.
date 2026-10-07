package net.realityradio.eden.core;

import java.util.*;

public final class CallBook {
  public record Call(
      UUID caller,
      UUID callee,
      UUID callerDevice,
      UUID calleeDevice,
      String from,
      String to,
      long created,
      long answered) {
    public boolean active() {
      return answered >= 0;
    }

    public UUID peer(UUID p) {
      return caller.equals(p) ? callee : caller;
    }

    public UUID device(UUID p) {
      return caller.equals(p) ? callerDevice : calleeDevice;
    }
  }

  private final Map<UUID, Call> calls = new HashMap<>();

  public Call get(UUID id) {
    return calls.get(id);
  }

  public Call dial(UUID from, UUID to, UUID fd, UUID td, String fn, String tn, long now) {
    if (from.equals(to) || fn.equals(tn))
      throw new IllegalArgumentException("You cannot call yourself");
    if (calls.containsKey(from)) throw new IllegalArgumentException("End your current call first");
    if (calls.containsKey(to)) throw new IllegalArgumentException("That number is busy");
    var c = new Call(from, to, fd, td, fn, tn, now, -1);
    calls.put(from, c);
    calls.put(to, c);
    return c;
  }

  public Call answer(UUID p, UUID d, long now) {
    var c = calls.get(p);
    if (c == null || !c.callee.equals(p) || !c.calleeDevice.equals(d) || c.active())
      throw new IllegalArgumentException("No incoming call on this device");
    var next =
        new Call(c.caller, c.callee, c.callerDevice, c.calleeDevice, c.from, c.to, c.created, now);
    calls.put(next.caller, next);
    calls.put(next.callee, next);
    return next;
  }

  public Call end(UUID p) {
    var c = calls.remove(p);
    if (c != null) {
      calls.remove(c.caller);
      calls.remove(c.callee);
    }
    return c;
  }

  public List<Call> all() {
    return calls.values().stream().distinct().toList();
  }

  public void clear() {
    calls.clear();
  }
}
