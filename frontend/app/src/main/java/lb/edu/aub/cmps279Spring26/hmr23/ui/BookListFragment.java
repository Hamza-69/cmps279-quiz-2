package lb.edu.aub.cmps279Spring26.hmr23.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lb.edu.aub.cmps279Spring26.hmr23.R;
import lb.edu.aub.cmps279Spring26.hmr23.adapters.BookAdapter;
import lb.edu.aub.cmps279Spring26.hmr23.databinding.FragmentBookListBinding;
import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.BookListViewModel;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.SharedViewModel;

public class BookListFragment extends Fragment {

    private FragmentBookListBinding binding;
    private BookListViewModel listViewModel;
    private SharedViewModel sharedViewModel;
    private BookAdapter adapter;
    private LinearLayoutManager layoutManager;

    // Debounce search input by 500 ms
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Sort arrays matching strings.xml sort_options order
    private static final String[] SORT_BY_VALUES = {
            "title", "title", "author", "author", "year", "year"
    };
    private static final String[] SORT_ORDER_VALUES = {
            "asc", "desc", "asc", "desc", "desc", "asc"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewModel = new ViewModelProvider(this).get(BookListViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupSearchBar();
        setupSpinners();
        setupFab();
        observeViewModel();

        // Initial load
        listViewModel.loadBooks(true);
    }

    private void setupToolbar() {
        binding.toolbar.setTitle("Book Library");
    }

    private void setupRecyclerView() {
        adapter = new BookAdapter();
        layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerBooks.setLayoutManager(layoutManager);
        binding.recyclerBooks.setAdapter(adapter);

        // Infinite scroll
        binding.recyclerBooks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return; // only trigger on downward scroll
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                int total = adapter.getItemCount();
                Boolean loading = listViewModel.isLoading.getValue();
                Boolean more = listViewModel.hasMore.getValue();
                if (lastVisible >= total - 3
                        && Boolean.FALSE.equals(loading)
                        && Boolean.TRUE.equals(more)) {
                    listViewModel.loadBooks(false);
                }
            }
        });

        // Navigate to detail on item click
        adapter.setOnBookClickListener(book -> {
            sharedViewModel.setSelectedBookId(book.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_list_to_detail);
        });
    }

    private void setupSearchBar() {
        TextWatcher debounceWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = BookListFragment.this::applyFiltersAndReload;
                debounceHandler.postDelayed(debounceRunnable, 500);
            }
        };
        binding.editSearch.addTextChangedListener(debounceWatcher);
        binding.editYearFrom.addTextChangedListener(debounceWatcher);
        binding.editYearTo.addTextChangedListener(debounceWatcher);
    }

    private void setupSpinners() {
        // Category filter spinner
        String[] categoryItems = getResources().getStringArray(R.array.categories_filter);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, categoryItems);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(catAdapter);
        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyFiltersAndReload();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Sort spinner
        String[] sortItems = getResources().getStringArray(R.array.sort_options);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, sortItems);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSort.setAdapter(sortAdapter);
        binding.spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyFiltersAndReload();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void applyFiltersAndReload() {
        if (binding == null) return;
        String query = binding.editSearch.getText() != null
                ? binding.editSearch.getText().toString() : null;
        String category = (String) binding.spinnerCategory.getSelectedItem();
        int sortIdx = binding.spinnerSort.getSelectedItemPosition();
        String sortedBy = SORT_BY_VALUES[sortIdx];
        String sortOrder = SORT_ORDER_VALUES[sortIdx];

        Integer yearFrom = parseYear(binding.editYearFrom);
        Integer yearTo = parseYear(binding.editYearTo);

        listViewModel.setFilters(query, category, sortedBy, sortOrder, yearFrom, yearTo);
        listViewModel.loadBooks(true);
    }

    private Integer parseYear(com.google.android.material.textfield.TextInputEditText et) {
        if (et.getText() == null) return null;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_list_to_add));
    }

    private void observeViewModel() {
        listViewModel.books.observe(getViewLifecycleOwner(), this::onBooksUpdated);
        listViewModel.isLoading.observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        listViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Refresh triggered by Add/Edit/Delete success
        sharedViewModel.getListNeedsRefresh().observe(getViewLifecycleOwner(), needs -> {
            if (Boolean.TRUE.equals(needs)) {
                sharedViewModel.consumeRefresh();
                listViewModel.loadBooks(true);
            }
        });
    }

    private void onBooksUpdated(List<Book> books) {
        adapter.setBooks(books);
        binding.tvEmpty.setVisibility((books == null || books.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        binding = null;
    }
}
