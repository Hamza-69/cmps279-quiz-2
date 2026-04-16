package lb.edu.aub.cmps279Spring26.hmr23.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookListResponse;
import lb.edu.aub.cmps279Spring26.hmr23.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookListViewModel extends ViewModel {

    private static final int PAGE_SIZE = 20;

    // Exposed LiveData
    public final MutableLiveData<List<Book>> books = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Pagination state
    private String nextCursor = null;

    // Current filter state
    private String currentQuery = null;
    private String currentCategory = null;   // null means all
    private String currentSortedBy = "title";
    private String currentSortOrder = "asc";
    private Integer currentYearFrom = null;
    private Integer currentYearTo = null;

    // Guard against concurrent loads
    private boolean loading = false;

    public void setFilters(String query, String category, String sortedBy, String sortOrder,
                           Integer yearFrom, Integer yearTo) {
        currentQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        currentCategory = (category != null && category.equals("All Categories")) ? null : category;
        currentSortedBy = sortedBy;
        currentSortOrder = sortOrder;
        currentYearFrom = yearFrom;
        currentYearTo = yearTo;
    }

    /**
     * Load books. Pass reset=true to start a fresh query (clears list + cursor).
     * Pass reset=false to append the next page.
     */
    public void loadBooks(boolean reset) {
        if (loading) return;
        if (!reset && Boolean.FALSE.equals(hasMore.getValue())) return;

        if (reset) {
            nextCursor = null;
            books.setValue(new ArrayList<>());
            hasMore.setValue(true);
        }

        loading = true;
        isLoading.setValue(true);

        RetrofitClient.getInstance().getBookApi()
                .getBooks(currentQuery, nextCursor, PAGE_SIZE,
                        currentSortedBy, currentSortOrder, currentCategory,
                        currentYearFrom, currentYearTo)
                .enqueue(new Callback<BookListResponse>() {
                    @Override
                    public void onResponse(Call<BookListResponse> call, Response<BookListResponse> response) {
                        loading = false;
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            BookListResponse body = response.body();
                            nextCursor = body.getNextCursor();
                            hasMore.setValue(nextCursor != null);

                            List<Book> current = books.getValue();
                            if (current == null) current = new ArrayList<>();
                            List<Book> merged = new ArrayList<>(current);
                            if (body.getBooks() != null) {
                                merged.addAll(body.getBooks());
                            }
                            books.setValue(merged);
                        } else {
                            errorMessage.setValue("Failed to load books (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<BookListResponse> call, Throwable t) {
                        loading = false;
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }
}
