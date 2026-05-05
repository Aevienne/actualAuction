package me.angelique.actualAuction.input;

import java.util.UUID;

public record PendingPrompt(PromptType type, UUID auctionId, double priceContext) {
}
