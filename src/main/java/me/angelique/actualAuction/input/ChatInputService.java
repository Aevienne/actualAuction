package me.angelique.actualAuction.input;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputService {
    private final Map<UUID, PendingPrompt> prompts = new ConcurrentHashMap<>();

    public void setPrompt(UUID playerId, PendingPrompt prompt) {
        prompts.put(playerId, prompt);
    }

    public PendingPrompt getPrompt(UUID playerId) {
        return prompts.get(playerId);
    }

    public PendingPrompt removePrompt(UUID playerId) {
        return prompts.remove(playerId);
    }

    public boolean hasPrompt(UUID playerId) {
        return prompts.containsKey(playerId);
    }
}
