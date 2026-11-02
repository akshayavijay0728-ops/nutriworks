package com.nutriworks.app.ui.capture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.nutriworks.app.R;
import com.nutriworks.app.databinding.FragmentConfirmImageBinding;
import com.nutriworks.app.util.ImageUtils;

import java.io.File;
import java.util.Locale;

/**
 * Confirmation screen presenting the captured or selected meal photo
 * before advancing to ML recognition and nutrition estimation.
 */
public class ConfirmImageFragment extends Fragment {

    private static final String TAG = "ConfirmImageFragment";
    public static final String ARG_IMAGE_PATH = "image_path";

    private FragmentConfirmImageBinding binding;
    private String imagePath;
    private Bitmap displayBitmap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConfirmImageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }

        loadImageAndDisplayMetadata();
        setupActionButtons();
    }

    /**
     * Decodes the image safely to avoid OOM and updates UI with file dimensions and size.
     */
    private void loadImageAndDisplayMetadata() {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            handleMissingImage();
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.canRead()) {
            handleMissingImage();
            return;
        }

        // Measure bounds first
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), boundsOptions);

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            handleCorruptedImage();
            return;
        }

        // Decode sampled bitmap safely
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = ImageUtils.calculateInSampleSize(boundsOptions, 800, 800);
        decodeOptions.inJustDecodeBounds = false;

        displayBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), decodeOptions);
        if (displayBitmap == null) {
            handleCorruptedImage();
            return;
        }

        binding.imgPreview.setImageBitmap(displayBitmap);

        // Display photo resolution and size metadata
        long fileSizeBytes = imageFile.length();
        double fileSizeKb = fileSizeBytes / 1024.0;
        String metaString = String.format(
                Locale.getDefault(),
                "%dx%d px  •  %.1f KB  •  Stored Privately",
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                fileSizeKb
        );
        binding.textImageMeta.setText(metaString);
    }

    private void setupActionButtons() {
        binding.btnRetake.setOnClickListener(v -> {
            if (isAdded() && getView() != null) {
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        binding.btnConfirm.setOnClickListener(v -> {
            if (imagePath != null && !imagePath.isEmpty()) {
                if (isAdded() && getView() != null) {
                    Bundle args = new Bundle();
                    args.putString(ResultFragment.ARG_IMAGE_PATH, imagePath);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_confirmImage_to_result, args);
                }
            } else {
                Snackbar.make(binding.getRoot(), R.string.error_invalid_image, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void handleMissingImage() {
        if (binding == null) {
            return;
        }
        binding.textImageMeta.setText(R.string.error_invalid_image);
        binding.btnConfirm.setEnabled(false);
        Snackbar.make(binding.getRoot(), R.string.error_invalid_image, Snackbar.LENGTH_LONG).show();
    }

    private void handleCorruptedImage() {
        if (binding == null) {
            return;
        }
        binding.textImageMeta.setText(R.string.error_invalid_image);
        binding.btnConfirm.setEnabled(false);
        Snackbar.make(binding.getRoot(), R.string.error_invalid_image, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.imgPreview.setImageBitmap(null);
        }
        if (displayBitmap != null && !displayBitmap.isRecycled()) {
            displayBitmap.recycle();
            displayBitmap = null;
        }
        binding = null;
    }
}
