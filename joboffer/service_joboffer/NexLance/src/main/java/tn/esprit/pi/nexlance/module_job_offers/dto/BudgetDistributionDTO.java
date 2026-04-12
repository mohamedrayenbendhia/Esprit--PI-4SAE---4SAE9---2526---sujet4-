package tn.esprit.pi.nexlance.module_job_offers.dto;

public class BudgetDistributionDTO {
    private String range;
    private long count;

    public BudgetDistributionDTO() {
    }

    public BudgetDistributionDTO(String range, long count) {
        this.range = range;
        this.count = count;
    }

    // Getters and Setters
    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
