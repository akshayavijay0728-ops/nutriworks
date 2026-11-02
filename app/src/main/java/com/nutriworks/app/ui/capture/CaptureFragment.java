package com.nutriworks.app.ui.capture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.nutriworks.app.R;
import com.nutriworks.app.databinding.FragmentCaptureBinding;
import com.nutriworks.app.util.ImageUtils;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capture screen providing CameraX live preview, flash control,
 * lens switching, image capture, gallery photo picking, and permission management.
 */
public class CaptureFragment extends Fragment {

    private static final String TAG = "CaptureFragment";

    private FragmentCaptureBinding binding;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    // Runtime Permission Launcher for Camera
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCameraLayout();
                    startCamera();
                } else {
                    handlePermissionDenied();
                }
            });

    // Modern Photo Picker Launcher
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    processSelectedImageUri(uri);
                } else {
                    Log.d(TAG, "Photo Picker cancelled by user");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCaptureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();

        setupClickListeners();
        checkAndRequestCameraPermissions();

        // Run background orphan image cleanup on initialization
        cameraExecutor.execute(() -> {
            if (isAdded() && getContext() != null) {
                ImageUtils.cleanupOrphanedImages(requireContext(), null);
            }
        });
    }

    private void setupClickListeners() {
        binding.btnShutter.setOnClickListener(v -> takePhoto());
        binding.btnFlash.setOnClickListener(v -> toggleFlashMode());
        binding.btnFlipCamera.setOnClickListener(v -> flipCamera());

        // Gallery picker from camera overlay
        binding.btnGalleryPicker.setOnClickListener(v -> launchPhotoPicker());

        // Gallery fallback when permission is denied
        binding.btnFallbackGallery.setOnClickListener(v -> launchPhotoPicker());
    }

    /**
     * Checks if camera hardware exists and verifies runtime permissions.
     */
    private void checkAndRequestCameraPermissions() {
        if (!hasCameraHardware()) {
            showNoCameraHardwareState();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showCameraLayout();
            startCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean hasCameraHardware() {
        return requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private void showNoCameraHardwareState() {
        binding.layoutPermissionDenied.setVisibility(View.VISIBLE);
        binding.textPermissionTitle.setText(R.string.permission_camera_rationale_title);
        binding.textPermissionMsg.setText(R.string.error_no_camera_hardware);
        binding.btnActionPermission.setVisibility(View.GONE);
    }

    private void showPermissionRationaleDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.permission_camera_rationale_title)
                .setMessage(R.string.permission_camera_rationale_msg)
                .setPositiveButton(R.string.permission_btn_grant, (dialog, which) -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton(R.string.permission_btn_cancel, (dialog, which) -> {
                    handlePermissionDenied();
                })
                .setCancelable(false)
                .show();
    }

    private void handlePermissionDenied() {
        if (binding == null) {
            return;
        }

        binding.layoutPermissionDenied.setVisibility(View.VISIBLE);

        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Permanently denied (user checked "Never ask again" or system policy)
            binding.textPermissionMsg.setText(R.string.permission_camera_settings_msg);
            binding.btnActionPermission.setText(R.string.permission_btn_settings);
            binding.btnActionPermission.setVisibility(View.VISIBLE);
            binding.btnActionPermission.setOnClickListener(v -> openAppSettings());
        } else {
            binding.textPermissionMsg.setText(R.string.permission_camera_rationale_msg);
            binding.btnActionPermission.setText(R.string.permission_btn_grant);
            binding.btnActionPermission.setVisibility(View.VISIBLE);
            binding.btnActionPermission.setOnClickListener(v ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void showCameraLayout() {
        if (binding != null) {
            binding.layoutPermissionDenied.setVisibility(View.GONE);
        }
    }

    /**
     * Initializes CameraX lifecycle-bound use cases (Preview & ImageCapture).
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                if (getContext() != null && binding != null) {
                    Snackbar.make(
                            binding.getRoot(),
                            getString(R.string.error_camera_init_failed, e.getMessage()),
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || binding == null) {
            return;
        }

        // Unbind previous use cases before rebinding
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build();

        try {
            camera = cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(),
                    cameraSelector,
                    preview,
                    imageCapture
            );
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Lens facing not supported on device: " + lensFacing, e);
            // Fall back to opposite lens if current lens is unavailable
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;
        }
    }

    /**
     * Captures a photo using CameraX and processes it via ImageUtils.
     */
    private void takePhoto() {
        if (imageCapture == null || binding == null) {
            return;
        }

        setCapturingState(true);

        File photoDir = ImageUtils.getAppPrivateImageDirectory(requireContext());
        File tempFile = new File(photoDir, "temp_capture_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(tempFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(tempFile);
                        processSelectedImageUri(savedUri);

                        // Delete temporary RAW capture after processing
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                setCapturingState(false);
                                if (binding != null) {
                                    Snackbar.make(
                                            binding.getRoot(),
                                            getString(R.string.error_image_capture_failed, exception.getMessage()),
                                            Snackbar.LENGTH_SHORT
                                    ).show();
                                }
                            });
                        }
                    }
                }
        );
    }

    /**
     * Cycles flash mode through OFF -> ON -> AUTO.
     */
    private void toggleFlashMode() {
        if (imageCapture == null || binding == null) {
            return;
        }

        switch (flashMode) {
            case ImageCapture.FLASH_MODE_OFF:
                flashMode = ImageCapture.FLASH_MODE_ON;
                binding.btnFlash.setIconResource(R.drawable.ic_flash_on);
                binding.textFlashStatus.setText(R.string.flash_mode_on);
                break;
            case ImageCapture.FLASH_MODE_ON:
                flashMode = ImageCapture.FLASH_MODE_AUTO;
                binding.btnFlash.setIconResource(R.drawable.ic_flash_auto);
                binding.textFlashStatus.setText(R.string.flash_mode_auto);
                break;
            case ImageCapture.FLASH_MODE_AUTO:
            default:
                flashMode = ImageCapture.FLASH_MODE_OFF;
                binding.btnFlash.setIconResource(R.drawable.ic_flash_off);
                binding.textFlashStatus.setText(R.string.flash_mode_off);
                break;
        }

        imageCapture.setFlashMode(flashMode);
    }

    /**
     * Toggles between front and back camera lenses.
     */
    private void flipCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    /**
     * Launches the Android Photo Picker contract.
     */
    private void launchPhotoPicker() {
        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Processes an image URI on a background thread: fixes EXIF rotation,
     * downsamples, creates 224x224 copy, saves to app-private storage,
     * and navigates to ConfirmImageFragment.
     */
    private void processSelectedImageUri(@NonNull Uri sourceUri) {
        if (getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(() -> setCapturingState(true));

        cameraExecutor.execute(() -> {
            ImageUtils.ImageProcessResult result =
                    ImageUtils.processAndSaveImage(requireContext(), sourceUri);

            if (getActivity() == null) {
                return;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    setCapturingState(false);

                    if (result != null && binding != null && isAdded() && getView() != null) {
                        Bundle args = new Bundle();
                        args.putString("image_path", result.getDisplayImagePath());
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_capture_to_confirmImage, args);
                    } else if (binding != null) {
                        Snackbar.make(
                                binding.getRoot(),
                                R.string.error_invalid_image,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    private void setCapturingState(boolean isCapturing) {
        if (binding == null) {
            return;
        }
        binding.progressCapture.setVisibility(isCapturing ? View.VISIBLE : View.GONE);
        binding.btnShutter.setEnabled(!isCapturing);
        binding.btnGalleryPicker.setEnabled(!isCapturing);
        binding.btnFlash.setEnabled(!isCapturing);
        binding.btnFlipCamera.setEnabled(!isCapturing);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }
}
