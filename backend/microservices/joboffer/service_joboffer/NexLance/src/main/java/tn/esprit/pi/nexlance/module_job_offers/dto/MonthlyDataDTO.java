package tn.esprit.pi.nexlance.module_job_offers.dto;

public class MonthlyDataDTO {
    private String month;
    private long offers;
    private long applications;

    public MonthlyDataDTO() {
    }

    public MonthlyDataDTO(String month, long offers, long applications) {
        this.month = month;
        this.offers = offers;
        this.applications = applications;
    }

    // Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public long getOffers() {
        return offers;
    }

    public void setOffers(long offers) {
        this.offers = offers;
    }

    public long getApplications() {
        return applications;
    }

    public void setApplications(long applications) {
        this.applications = applications;
    }
}
