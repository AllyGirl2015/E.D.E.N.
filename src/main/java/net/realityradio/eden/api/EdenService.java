package net.realityradio.eden.api;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface EdenService {
    /** Validate permissions and arguments here. Never trust values coming from the client. */
    String execute(ServiceContext context, JsonObject arguments);
}
