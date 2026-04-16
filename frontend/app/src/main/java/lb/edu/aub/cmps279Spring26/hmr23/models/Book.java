package lb.edu.aub.cmps279Spring26.hmr23.models;

import com.google.gson.annotations.SerializedName;

public class Book {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("author")
    private String author;

    @SerializedName("year")
    private int year;

    @SerializedName("category")
    private String category;

    @SerializedName("cover_image")
    private String coverImage;

    @SerializedName("is_borrowed")
    private boolean isBorrowed;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("borrow_date")
    private String borrowDate;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public String getCategory() { return category; }
    public String getCoverImage() { return coverImage; }
    public boolean isBorrowed() { return isBorrowed; }
    public String getDueDate() { return dueDate; }
    public String getBorrowDate() { return borrowDate; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthor(String author) { this.author = author; }
    public void setYear(int year) { this.year = year; }
    public void setCategory(String category) { this.category = category; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public void setBorrowed(boolean borrowed) { isBorrowed = borrowed; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }
}
