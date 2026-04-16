package lb.edu.aub.cmps279Spring26.hmr23.network;

import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookCreate;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookListResponse;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookUpdate;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookApi {

    @POST("books/")
    Call<Book> createBook(@Body BookCreate book);

    @GET("books/")
    Call<BookListResponse> getBooks(
            @Query("query") String query,
            @Query("cursor") String cursor,
            @Query("limit") int limit,
            @Query("sorted_by") String sortedBy,
            @Query("sort_order") String sortOrder,
            @Query("category_filter_by") String categoryFilter,
            @Query("year_from") Integer yearFrom,
            @Query("year_to") Integer yearTo
    );

    @GET("books/{id}")
    Call<Book> getBook(@Path("id") String id);

    @PUT("books/{id}")
    Call<Book> updateBook(@Path("id") String id, @Body BookUpdate update);

    @DELETE("books/{id}")
    Call<Void> deleteBook(@Path("id") String id);
}
