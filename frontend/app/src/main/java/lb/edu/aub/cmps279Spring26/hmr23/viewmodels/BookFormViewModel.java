package lb.edu.aub.cmps279Spring26.hmr23.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookCreate;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookUpdate;
import lb.edu.aub.cmps279Spring26.hmr23.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookFormViewModel extends ViewModel {

    public final MutableLiveData<Book> result = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<String> imageBase64 = new MutableLiveData<>();

    public void setImageBase64(String base64) {
        imageBase64.setValue(base64);
    }

    public void clearImage() {
        imageBase64.setValue(null);
    }

    public void createBook(BookCreate book) {
        isLoading.setValue(true);
        RetrofitClient.getInstance().getBookApi()
                .createBook(book)
                .enqueue(new Callback<Book>() {
                    @Override
                    public void onResponse(Call<Book> call, Response<Book> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            errorMessage.setValue("Failed to create book (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<Book> call, Throwable t) {
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
                            result.setValue(response.body());
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

    
    public void consumeResult() {
        result.setValue(null);
    }
}
