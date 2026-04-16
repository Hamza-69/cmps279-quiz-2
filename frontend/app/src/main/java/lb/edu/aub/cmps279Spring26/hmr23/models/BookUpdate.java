package lb.edu.aub.cmps279Spring26.hmr23.models;

import com.google.gson.annotations.SerializedName;

public class BookUpdate {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("author")
    private String author;

    @SerializedName("year")
    private Integer year;

    @SerializedName("category")
    private String category;

    @SerializedName("cover_image")
    private String coverImage;

    @SerializedName("is_borrowed")
    private Boolean isBorrowed;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("borrow_date")
    private String borrowDate;

    

    public BookUpdate(String title, String description, String author, Integer year,
                      String category, String coverImage) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.year = year;
        this.category = category;
        this.coverImage = coverImage;
    }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthor(String author) { this.author = author; }
    public void setYear(Integer year) { this.year = year; }
    public void setCategory(String category) { this.category = category; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public void setBorrowed(Boolean borrowed) { isBorrowed = borrowed; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public Integer getYear() { return year; }
    public String getCategory() { return category; }
    public String getCoverImage() { return coverImage; }
}
