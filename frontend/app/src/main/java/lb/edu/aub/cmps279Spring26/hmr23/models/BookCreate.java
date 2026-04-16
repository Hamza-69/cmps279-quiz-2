package lb.edu.aub.cmps279Spring26.hmr23.models;

import com.google.gson.annotations.SerializedName;

public class BookCreate {

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

    public BookCreate(String title, String description, String author, int year,
                      String category, String coverImage) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.year = year;
        this.category = category;
        this.coverImage = coverImage;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public String getCategory() { return category; }
    public String getCoverImage() { return coverImage; }
}
