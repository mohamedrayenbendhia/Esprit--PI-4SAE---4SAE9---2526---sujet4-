package tn.esprit.pi.nexlance.module_job_offers.dto;

public class JobOfferStatsDTO {
    private long totalOffers;
    private long totalApplications;
    private double conversionRate;
    private double avgApplicationsPerOffer;
    private double avgResponseTime;

    public JobOfferStatsDTO() {
    }

    public JobOfferStatsDTO(long totalOffers, long totalApplications, double conversionRate, 
                           double avgApplicationsPerOffer, double avgResponseTime) {
        this.totalOffers = totalOffers;
        this.totalApplications = totalApplications;
        this.conversionRate = conversionRate;
        this.avgApplicationsPerOffer = avgApplicationsPerOffer;
        this.avgResponseTime = avgResponseTime;
    }

    // Getters
    public long getTotalOffers() {
        return totalOffers;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public double getAvgApplicationsPerOffer() {
        return avgApplicationsPerOffer;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    // Setters
    public void setTotalOffers(long totalOffers) {
        this.totalOffers = totalOffers;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public void setAvgApplicationsPerOffer(double avgApplicationsPerOffer) {
        this.avgApplicationsPerOffer = avgApplicationsPerOffer;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }
}
