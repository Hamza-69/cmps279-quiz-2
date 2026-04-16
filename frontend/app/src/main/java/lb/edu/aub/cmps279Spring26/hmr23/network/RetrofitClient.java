package lb.edu.aub.cmps279Spring26.hmr23.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://cmps-279.fly.dev";
    private static RetrofitClient instance;
    private final BookApi bookApi;

    private RetrofitClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        bookApi = retrofit.create(BookApi.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public BookApi getBookApi() {
        return bookApi;
    }
}
