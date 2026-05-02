package tn.esprit.pi.nexlance.module_job_offers.dto;

public class CategoryDistributionDTO {
    private String category;
    private long count;
    private String color;

    public CategoryDistributionDTO() {
    }

    public CategoryDistributionDTO(String category, long count, String color) {
        this.category = category;
        this.count = count;
        this.color = color;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
