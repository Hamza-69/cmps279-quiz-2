package lb.edu.aub.cmps279Spring26.hmr23.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import lb.edu.aub.cmps279Spring26.hmr23.R;
import lb.edu.aub.cmps279Spring26.hmr23.databinding.FragmentEditBookBinding;
import lb.edu.aub.cmps279Spring26.hmr23.models.Book;
import lb.edu.aub.cmps279Spring26.hmr23.models.BookUpdate;
import lb.edu.aub.cmps279Spring26.hmr23.network.RetrofitClient;
import lb.edu.aub.cmps279Spring26.hmr23.utils.ImageUtils;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.BookFormViewModel;
import lb.edu.aub.cmps279Spring26.hmr23.viewmodels.SharedViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditBookFragment extends Fragment {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int MAX_IMAGE_DIMENSION = 1024;

    private FragmentEditBookBinding binding;
    private BookFormViewModel formViewModel;
    private SharedViewModel sharedViewModel;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private boolean isGalleryMode = false;
    private boolean imageCaptured = false;
    private String currentBookId;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) onGalleryImageSelected(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        formViewModel = new ViewModelProvider(this).get(BookFormViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupToolbar();
        setupCategorySpinner();
        setupImageToggle();
        setupSubmitButton();
        observeViewModel();

        currentBookId = sharedViewModel.getSelectedBookId().getValue();
        if (currentBookId != null) {
            loadBookForEdit(currentBookId);
        } else {
            Toast.makeText(requireContext(), "No book selected", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }

        switchToCameraMode();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void loadBookForEdit(String id) {
        RetrofitClient.getInstance().getBookApi()
                .getBook(id)
                .enqueue(new Callback<Book>() {
                    @Override
                    public void onResponse(Call<Book> call, Response<Book> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            prefillForm(response.body());
                        } else {
                            Toast.makeText(requireContext(), "Failed to load book", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Book> call, Throwable t) {
                        Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void prefillForm(Book book) {
        binding.editTitle.setText(book.getTitle());
        binding.editDescription.setText(book.getDescription());
        binding.editAuthor.setText(book.getAuthor());
        binding.editYear.setText(String.valueOf(book.getYear()));

        // Select category spinner
        String[] categories = getResources().getStringArray(R.array.categories);
        List<String> catList = Arrays.asList(categories);
        int idx = catList.indexOf(book.getCategory());
        if (idx >= 0) binding.spinnerCategory.setSelection(idx);

        // Show existing cover image if any
        if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
            Glide.with(this)
                    .load(book.getCoverImage())
                    .into(binding.ivImagePreview);
            binding.ivImagePreview.setVisibility(View.VISIBLE);
            binding.cameraContainer.setVisibility(View.GONE);
            imageCaptured = true;
            binding.btnRetakeCamera.setVisibility(View.VISIBLE);
            binding.btnCapture.setVisibility(View.GONE);
        }
    }

    private void setupImageToggle() {
        binding.btnModeCamera.setOnClickListener(v -> {
            isGalleryMode = false;
            switchToCameraMode();
        });
        binding.btnModeGallery.setOnClickListener(v -> {
            isGalleryMode = true;
            switchToGalleryMode();
        });
        binding.btnCapture.setOnClickListener(v -> capturePhoto());
        binding.btnRetakeCamera.setOnClickListener(v -> {
            imageCaptured = false;
            formViewModel.clearImage();
            binding.ivImagePreview.setVisibility(View.GONE);
            binding.cameraContainer.setVisibility(View.VISIBLE);
            binding.btnCapture.setVisibility(View.VISIBLE);
            binding.btnRetakeCamera.setVisibility(View.GONE);
            startCamera();
        });
        binding.btnBrowseGallery.setOnClickListener(v ->
                galleryLauncher.launch("image/*"));
        binding.btnClearImage.setOnClickListener(v -> {
            formViewModel.clearImage();
            binding.ivImagePreview.setVisibility(View.GONE);
            binding.btnClearImage.setVisibility(View.GONE);
        });
    }

    private void updateToggleButtons(boolean cameraSelected) {
        int accentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent);
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.colorSurface);
        binding.btnModeCamera.setBackgroundTintList(
                ColorStateList.valueOf(cameraSelected ? accentColor : surfaceColor));
        binding.btnModeCamera.setTextColor(cameraSelected ? Color.WHITE : primaryColor);
        binding.btnModeGallery.setBackgroundTintList(
                ColorStateList.valueOf(cameraSelected ? surfaceColor : accentColor));
        binding.btnModeGallery.setTextColor(cameraSelected ? primaryColor : Color.WHITE);
    }

    private void switchToCameraMode() {
        updateToggleButtons(true);
        binding.layoutCameraActions.setVisibility(View.VISIBLE);
        binding.layoutGalleryActions.setVisibility(View.GONE);
        if (!imageCaptured) {
            binding.cameraContainer.setVisibility(View.VISIBLE);
            binding.ivImagePreview.setVisibility(View.GONE);
            startCamera();
        }
    }

    private void switchToGalleryMode() {
        updateToggleButtons(false);
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding.layoutCameraActions.setVisibility(View.GONE);
        binding.layoutGalleryActions.setVisibility(View.VISIBLE);
        binding.cameraContainer.setVisibility(View.GONE);
        String cur = formViewModel.imageBase64.getValue();
        if (cur != null) {
            binding.ivImagePreview.setVisibility(View.VISIBLE);
            binding.btnClearImage.setVisibility(View.VISIBLE);
        }
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }
        bindCamera();
    }

    private void bindCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(),
                        "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        File outputFile = new File(requireContext().getCacheDir(),
                "edit_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        imageCapture.takePicture(options, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                Bitmap bmp = android.graphics.BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                if (bmp == null) return;
                bmp = ImageUtils.scaleBitmap(bmp, MAX_IMAGE_DIMENSION);
                String base64 = ImageUtils.bitmapToBase64(bmp);
                formViewModel.setImageBase64(base64);

                imageCaptured = true;
                final Bitmap finalBmp = bmp;
                binding.ivImagePreview.setImageBitmap(finalBmp);
                binding.ivImagePreview.setVisibility(View.VISIBLE);
                binding.cameraContainer.setVisibility(View.GONE);
                binding.btnCapture.setVisibility(View.GONE);
                binding.btnRetakeCamera.setVisibility(View.VISIBLE);

                if (cameraProvider != null) cameraProvider.unbindAll();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(requireContext(),
                        "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onGalleryImageSelected(Uri uri) {
        Bitmap bmp = ImageUtils.uriToBitmap(requireContext(), uri);
        if (bmp == null) {
            Toast.makeText(requireContext(), "Could not read image", Toast.LENGTH_SHORT).show();
            return;
        }
        bmp = ImageUtils.scaleBitmap(bmp, MAX_IMAGE_DIMENSION);
        String base64 = ImageUtils.bitmapToBase64(bmp);
        formViewModel.setImageBase64(base64);

        binding.ivImagePreview.setImageBitmap(bmp);
        binding.ivImagePreview.setVisibility(View.VISIBLE);
        binding.btnClearImage.setVisibility(View.VISIBLE);
    }

    private void setupSubmitButton() {
        binding.btnSubmit.setOnClickListener(v -> submitForm());
    }

    private void submitForm() {
        String title = getTextOrNull(binding.editTitle);
        String description = getTextOrNull(binding.editDescription);
        String author = getTextOrNull(binding.editAuthor);
        String yearStr = getTextOrNull(binding.editYear);

        if (title == null) { binding.layoutTitle.setError(getString(R.string.error_title_required)); return; }
        binding.layoutTitle.setError(null);
        if (description == null) { binding.layoutDescription.setError(getString(R.string.error_description_required)); return; }
        binding.layoutDescription.setError(null);
        if (author == null) { binding.layoutAuthor.setError(getString(R.string.error_author_required)); return; }
        binding.layoutAuthor.setError(null);
        if (yearStr == null) { binding.layoutYear.setError(getString(R.string.error_year_required)); return; }
        binding.layoutYear.setError(null);

        int year;
        try { year = Integer.parseInt(yearStr); }
        catch (NumberFormatException e) { binding.layoutYear.setError("Enter a valid year"); return; }

        String category = (String) binding.spinnerCategory.getSelectedItem();
        String coverImage = formViewModel.imageBase64.getValue();

        BookUpdate update = new BookUpdate(title, description, author, year, category, coverImage);
        formViewModel.updateBook(currentBookId, update);
    }

    private void observeViewModel() {
        formViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setEnabled(!loading);
        });

        formViewModel.result.observe(getViewLifecycleOwner(), book -> {
            if (book != null) {
                formViewModel.consumeResult();
                sharedViewModel.requestListRefresh();
                // Pop back to detail (which will reload), or all the way to list
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        formViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindCamera();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getTextOrNull(com.google.android.material.textfield.TextInputEditText et) {
        if (et.getText() == null) return null;
        String s = et.getText().toString().trim();
        return s.isEmpty() ? null : s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) cameraProvider.unbindAll();
        binding = null;
    }
}
