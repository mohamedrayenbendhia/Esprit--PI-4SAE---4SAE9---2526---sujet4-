package tn.esprit.pi.nexlance.recommendation.enums;

public enum RecommendationStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    EXPIRED("expired");

    private final String value;

    RecommendationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RecommendationStatus fromValue(String value) {
        for (RecommendationStatus status : RecommendationStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
