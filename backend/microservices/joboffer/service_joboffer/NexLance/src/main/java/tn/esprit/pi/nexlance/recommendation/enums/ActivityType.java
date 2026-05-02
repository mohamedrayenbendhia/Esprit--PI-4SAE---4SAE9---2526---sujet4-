package tn.esprit.pi.nexlance.recommendation.enums;

public enum ActivityType {
    CREATED("created"),
    SENT("sent"),
    VIEWED("viewed"),
    REMINDED("reminded"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    EXPIRED("expired"),
    UPDATED("updated");

    private final String value;

    ActivityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityType fromValue(String value) {
        for (ActivityType type : ActivityType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown activity type: " + value);
    }
}
