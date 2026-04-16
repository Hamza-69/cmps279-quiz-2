package lb.edu.aub.cmps279Spring26.hmr23.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookUpdate;
import lb.edu.aub.cmps279Spring26.hmr23.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookDetailViewModel extends ViewModel {

    public final MutableLiveData<Book> book = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public void loadBook(String id) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getBookApi()
                .getBook(id)
                .enqueue(new Callback<Book>() {
                    @Override
                    public void onResponse(Call<Book> call, Response<Book> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            book.setValue(response.body());
                        } else {
                            errorMessage.setValue("Book not found (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Book> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    public void deleteBook(String id) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getBookApi()
                .deleteBook(id)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful()) {
                            deleteSuccess.setValue(true);
                        } else {
                            errorMessage.setValue("Failed to delete book (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    public void updateBook(String id, BookUpdate update) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getBookApi()
                .updateBook(id, update)
                .enqueue(new Callback<Book>() {
                    @Override
                    public void onResponse(Call<Book> call, Response<Book> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            book.setValue(response.body());
                            updateSuccess.setValue(true);
                        } else {
                            errorMessage.setValue("Failed to update book (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Book> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    public void consumeUpdateSuccess() {
        updateSuccess.setValue(false);
    }
}
