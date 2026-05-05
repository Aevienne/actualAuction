package me.angelique.actualAuction.service;

public record BidResult(boolean success, String message) {
    public static BidResult success(String message) {
        return new BidResult(true, message);
    }

    public static BidResult error(String message) {
        return new BidResult(false, message);
    }
}
