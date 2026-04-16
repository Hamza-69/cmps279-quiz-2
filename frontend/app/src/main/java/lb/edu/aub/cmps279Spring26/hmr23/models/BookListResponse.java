package lb.edu.aub.cmps279Spring26.hmr23.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookListResponse {

    @SerializedName("books")
    private List<Book> books;

    @SerializedName("next_cursor")
    private String nextCursor;

    public List<Book> getBooks() { return books; }
    public String getNextCursor() { return nextCursor; }
}
