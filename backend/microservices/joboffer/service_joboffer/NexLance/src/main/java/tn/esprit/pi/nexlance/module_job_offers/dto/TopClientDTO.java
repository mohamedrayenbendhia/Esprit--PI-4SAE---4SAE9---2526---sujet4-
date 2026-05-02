package tn.esprit.pi.nexlance.module_job_offers.dto;

public class TopClientDTO {
    private String clientId;
    private String name;
    private long offers;
    private int completionRate;

    public TopClientDTO() {
    }

    public TopClientDTO(String clientId, String name, long offers, int completionRate) {
        this.clientId = clientId;
        this.name = name;
        this.offers = offers;
        this.completionRate = completionRate;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getOffers() {
        return offers;
    }

    public void setOffers(long offers) {
        this.offers = offers;
    }

    public int getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(int completionRate) {
        this.completionRate = completionRate;
    }
}
