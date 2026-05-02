package tn.esprit.pi.nexlance.recommendation.enums;

public enum UserType {
    CLIENT("client"),
    FREELANCE("freelance"),
    ADMIN("admin"),
    SYSTEM("system");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserType fromValue(String value) {
        for (UserType type : UserType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type: " + value);
    }
}
