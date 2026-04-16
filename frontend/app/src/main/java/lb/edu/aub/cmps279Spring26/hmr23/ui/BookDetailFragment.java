package lb.edu.aub.cmps279Spring26.hmr23.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lb.edu.aub.cmps279Spring26.hmr23.R;
import lb.edu.aub.cmps279Spring26.hmr23.databinding.FragmentBookDetailBinding;
import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookUpdate;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.BookDetailViewModel;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.SharedViewModel;

public class BookDetailFragment extends Fragment {

    private FragmentBookDetailBinding binding;
    private BookDetailViewModel detailViewModel;
    private SharedViewModel sharedViewModel;
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailViewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupToolbar();
        observeViewModel();

        String bookId = sharedViewModel.getSelectedBookId().getValue();
        if (bookId != null) {
            detailViewModel.loadBook(bookId);
        } else {
            Toast.makeText(requireContext(), "No book selected", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void bindBookToView(Book book) {
        binding.toolbar.setTitle(book.getTitle());
        binding.tvTitle.setText(book.getTitle());
        binding.tvAuthor.setText(book.getAuthor());
        binding.tvYear.setText(String.valueOf(book.getYear()));
        binding.tvCategory.setText(book.getCategory());
        binding.tvDescription.setText(book.getDescription());

        // Borrowed status
        if (book.isBorrowed()) {
            binding.chipBorrowed.setVisibility(View.VISIBLE);
            binding.tvStatus.setText(getString(R.string.borrowed));
            String normalizedDueDate = normalizeDate(book.getDueDate());
            if (normalizedDueDate != null) {
                binding.tvDueDate.setVisibility(View.VISIBLE);
                binding.tvDueDate.setText(getString(R.string.due_date_label, normalizedDueDate));
            } else {
                binding.tvDueDate.setVisibility(View.GONE);
            }
            String normalizedBorrowDate = normalizeDate(book.getBorrowDate());
            if (normalizedBorrowDate != null) {
                binding.tvBorrowDate.setVisibility(View.VISIBLE);
                binding.tvBorrowDate.setText(getString(R.string.borrow_date_label, normalizedBorrowDate));
            } else {
                binding.tvBorrowDate.setVisibility(View.GONE);
            }
            int overdueDays = getOverdueDays(book.getDueDate());
            binding.tvLateFee.setVisibility(View.VISIBLE);
            if (overdueDays > 0) {
                binding.tvLateFee.setText(getString(R.string.late_fee_label, overdueDays * 2, overdueDays));
            } else {
                binding.tvLateFee.setText(getString(R.string.no_late_fee));
            }
        } else {
            binding.chipBorrowed.setVisibility(View.GONE);
            binding.tvStatus.setText(getString(R.string.available));
            binding.tvDueDate.setVisibility(View.GONE);
            binding.tvBorrowDate.setVisibility(View.GONE);
            binding.tvLateFee.setVisibility(View.GONE);
        }

        // Cover image
        if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
            Glide.with(this)
                    .load(book.getCoverImage())
                    .placeholder(R.drawable.bg_book_cover_placeholder)
                    .error(R.drawable.bg_book_cover_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivCover);
        } else {
            binding.ivCover.setImageDrawable(null);
            binding.ivCover.setBackgroundResource(R.drawable.bg_book_cover_placeholder);
        }

        // Edit button
        binding.btnEdit.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_detail_to_edit));

        // Delete button
        binding.btnDelete.setOnClickListener(v -> confirmDelete(book.getId()));

        // Borrow / Return button
        binding.btnBorrowReturn.setText(book.isBorrowed() ? R.string.return_book : R.string.take_book);
        binding.btnBorrowReturn.setOnClickListener(v -> {
            if (book.isBorrowed()) {
                confirmReturn(book);
            } else {
                showDueDatePickerAndBorrow(book);
            }
        });
    }

    private void confirmDelete(String bookId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) ->
                        detailViewModel.deleteBook(bookId))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDueDatePickerAndBorrow(Book book) {
        Calendar today = Calendar.getInstance();
        DatePickerDialog pickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar dueDate = Calendar.getInstance();
                    dueDate.set(year, month, dayOfMonth);
                    clearTimePart(dueDate);

                    Calendar minAllowedDate = Calendar.getInstance();
                    clearTimePart(minAllowedDate);
                    if (dueDate.before(minAllowedDate)) {
                        Toast.makeText(requireContext(), getString(R.string.due_date_invalid), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BookUpdate update = buildUpdateFromBook(book);
                    update.setBorrowed(true);
                    update.setBorrowDate(formatDate(Calendar.getInstance()));
                    update.setDueDate(formatDate(dueDate));
                    detailViewModel.updateBook(book.getId(), update);
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
        pickerDialog.setTitle(getString(R.string.select_due_date_title));
        Calendar minDate = Calendar.getInstance();
        clearTimePart(minDate);
        pickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        pickerDialog.show();
    }

    private void confirmReturn(Book book) {
        int overdueDays = getOverdueDays(book.getDueDate());
        int fee = overdueDays * 2;
        String message = overdueDays > 0
                ? getString(R.string.return_confirm_message, fee, overdueDays)
                : getString(R.string.return_confirm_message_no_fee);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.return_confirm_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.return_book), (dialog, which) -> {
                    BookUpdate update = buildUpdateFromBook(book);
                    update.setBorrowed(false);
                    detailViewModel.updateBook(book.getId(), update);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private BookUpdate buildUpdateFromBook(Book book) {
        BookUpdate update = new BookUpdate(
                book.getTitle(),
                book.getDescription(),
                book.getAuthor(),
                book.getYear(),
                book.getCategory(),
                book.getCoverImage()
        );
        update.setBorrowed(book.isBorrowed());
        update.setDueDate(book.getDueDate());
        update.setBorrowDate(book.getBorrowDate());
        return update;
    }

    private int getOverdueDays(String rawDueDate) {
        Date parsedDueDate = parseDate(rawDueDate);
        if (parsedDueDate == null) {
            return 0;
        }
        Calendar dueDate = Calendar.getInstance();
        dueDate.setTime(parsedDueDate);
        clearTimePart(dueDate);

        Calendar today = Calendar.getInstance();
        clearTimePart(today);

        long diffMillis = today.getTimeInMillis() - dueDate.getTimeInMillis();
        if (diffMillis <= 0) {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis);
    }

    private Date parseDate(String rawDate) {
        String normalized = normalizeDate(rawDate);
        if (normalized == null || normalized.length() != 10) {
            return null;
        }
        try {
            apiDateFormat.setLenient(false);
            return apiDateFormat.parse(normalized);
        } catch (ParseException e) {
            return null;
        }
    }

    private String normalizeDate(String rawDate) {
        if (rawDate == null) return null;
        String trimmed = rawDate.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() >= 10 && trimmed.charAt(4) == '-' && trimmed.charAt(7) == '-') {
            return trimmed.substring(0, 10);
        }
        return trimmed;
    }

    private String formatDate(Calendar calendar) {
        clearTimePart(calendar);
        return apiDateFormat.format(calendar.getTime());
    }

    private void clearTimePart(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void observeViewModel() {
        detailViewModel.book.observe(getViewLifecycleOwner(), book -> {
            if (book != null) bindBookToView(book);
        });

        detailViewModel.isLoading.observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        detailViewModel.deleteSuccess.observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                sharedViewModel.requestListRefresh();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        detailViewModel.updateSuccess.observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                sharedViewModel.requestListRefresh();
                detailViewModel.consumeUpdateSuccess();
            }
        });

        detailViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
