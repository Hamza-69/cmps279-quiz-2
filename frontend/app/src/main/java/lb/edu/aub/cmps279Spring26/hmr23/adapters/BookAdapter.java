package lb.edu.aub.cmps279Spring26.hmr23.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import lb.edu.aub.cmps279Spring26.hmr23.R;
import lb.edu.aub.cmps279Spring26.hmr23.databinding.ItemBookBinding;
import lb.edu.aub.cmps279Spring26.hmr23.models.Book;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private final List<Book> books = new ArrayList<>();
    private OnBookClickListener listener;

    public void setOnBookClickListener(OnBookClickListener listener) {
        this.listener = listener;
    }

    /** Replace the full list (used for fresh loads and search resets). */
    public void setBooks(List<Book> newBooks) {
        books.clear();
        if (newBooks != null) books.addAll(newBooks);
        notifyDataSetChanged();
    }

    /** Append more books (used for infinite scroll pagination). */
    public void appendBooks(List<Book> moreBooks) {
        if (moreBooks == null || moreBooks.isEmpty()) return;
        int start = books.size();
        books.addAll(moreBooks);
        notifyItemRangeInserted(start, moreBooks.size());
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookBinding binding = ItemBookBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        holder.bind(books.get(position));
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    class BookViewHolder extends RecyclerView.ViewHolder {

        private final ItemBookBinding binding;

        BookViewHolder(ItemBookBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Book book) {
            binding.tvTitle.setText(book.getTitle());
            binding.tvAuthor.setText(book.getAuthor());
            binding.tvYearCategory.setText(book.getYear() + " · " + book.getCategory());

            // Borrowed badge
            binding.chipBorrowed.setVisibility(book.isBorrowed() ? View.VISIBLE : View.GONE);

            // Cover image with Glide
            Context ctx = binding.getRoot().getContext();
            if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
                Glide.with(ctx)
                        .load(book.getCoverImage())
                        .placeholder(R.drawable.bg_book_cover_placeholder)
                        .error(R.drawable.bg_book_cover_placeholder)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivCover);
            } else {
                Glide.with(ctx).clear(binding.ivCover);
                binding.ivCover.setImageDrawable(null);
                binding.ivCover.setBackgroundResource(R.drawable.bg_book_cover_placeholder);
            }

            // Click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(book);
            });
        }
    }
}
