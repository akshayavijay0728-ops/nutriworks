package com.nutriworks.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Image processing utilities for NutriWorks.
 * Provides memory-safe bitmap decoding, EXIF rotation correction,
 * center-cropped 224x224 ML scaling, app-private storage writing,
 * and orphan image directory cleanup.
 */
public final class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static final String IMAGE_DIR_NAME = "meal_photos";
    private static final int DISPLAY_MAX_DIMENSION = 1024;
    private static final int MODEL_INPUT_SIZE = 224;
    private static final int JPEG_QUALITY = 85;

    private ImageUtils() {
        // Prevent instantiation
    }

    /**
     * Value object holding paths and metadata of processed images.
     */
    public static class ImageProcessResult {
        private final String displayImagePath;
        private final String modelImagePath;
        private final int width;
        private final int height;
        private final long fileSizeBytes;

        public ImageProcessResult(String displayImagePath,
                                  String modelImagePath,
                                  int width,
                                  int height,
                                  long fileSizeBytes) {
            this.displayImagePath = displayImagePath;
            this.modelImagePath = modelImagePath;
            this.width = width;
            this.height = height;
            this.fileSizeBytes = fileSizeBytes;
        }

        public String getDisplayImagePath() {
            return displayImagePath;
        }

        public String getModelImagePath() {
            return modelImagePath;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }
    }

    /**
     * Processes an image from a content URI or local file:
     * decodes with memory downsampling, fixes EXIF orientation,
     * creates a high-res display copy and a 224x224 model copy,
     * and writes both into app-private storage.
     *
     * @param context Application context.
     * @param imageUri Source Uri (from CameraX output or PhotoPicker).
     * @return ImageProcessResult containing paths to saved files, or null if processing fails.
     */
    @Nullable
    public static ImageProcessResult processAndSaveImage(@NonNull Context context, @NonNull Uri imageUri) {
        Bitmap displayBitmap = null;
        Bitmap modelBitmap = null;

        try {
            // 1. Decode bounds and compute safe sample size to prevent OOM
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream inBounds = context.getContentResolver().openInputStream(imageUri)) {
                if (inBounds == null) {
                    Log.e(TAG, "Cannot open input stream for URI: " + imageUri);
                    return null;
                }
                BitmapFactory.decodeStream(inBounds, null, options);
            }

            options.inSampleSize = calculateInSampleSize(options, DISPLAY_MAX_DIMENSION, DISPLAY_MAX_DIMENSION);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // 2. Decode the sampled bitmap
            try (InputStream inData = context.getContentResolver().openInputStream(imageUri)) {
                if (inData == null) {
                    return null;
                }
                displayBitmap = BitmapFactory.decodeStream(inData, null, options);
            }

            if (displayBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + imageUri);
                return null;
            }

            // 3. Fix EXIF rotation
            displayBitmap = rotateBitmapIfRequired(context, displayBitmap, imageUri);

            // 4. Create center-cropped 224x224 bitmap for on-device ML model
            modelBitmap = createModelInputBitmap(displayBitmap, MODEL_INPUT_SIZE);

            // 5. Save display copy into app-private storage
            long timestamp = System.currentTimeMillis();
            File storageDir = getAppPrivateImageDirectory(context);
            File displayFile = new File(storageDir, "meal_display_" + timestamp + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(displayFile)) {
                displayBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
                fos.flush();
            }

            // 6. Save model copy into app-private storage
            File modelFile = new File(storageDir, "meal_model_" + timestamp + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(modelFile)) {
                modelBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }

            return new ImageProcessResult(
                    displayFile.getAbsolutePath(),
                    modelFile.getAbsolutePath(),
                    displayBitmap.getWidth(),
                    displayBitmap.getHeight(),
                    displayFile.length()
            );

        } catch (Exception e) {
            Log.e(TAG, "Error processing and saving image: " + e.getMessage(), e);
            return null;
        } finally {
            if (modelBitmap != null && !modelBitmap.isRecycled()) {
                modelBitmap.recycle();
            }
        }
    }

    /**
     * Calculates the power-of-two inSampleSize that keeps decoded dimensions
     * within the requested width and height.
     */
    public static int calculateInSampleSize(@NonNull BitmapFactory.Options options,
                                            int reqWidth,
                                            int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    /**
     * Inspects EXIF metadata of the source image and rotates the decoded bitmap
     * if orientation is not normal.
     */
    @NonNull
    public static Bitmap rotateBitmapIfRequired(@NonNull Context context,
                                                @NonNull Bitmap bitmap,
                                                @NonNull Uri imageUri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return bitmap;
            }

            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int rotationDegrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
                default:
                    return bitmap;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;

        } catch (IOException e) {
            Log.w(TAG, "Unable to read EXIF orientation: " + e.getMessage());
            return bitmap;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Center-crops and resizes a bitmap to target dimensions (e.g. 224x224),
     * maintaining square aspect ratio without distortion.
     */
    @NonNull
    public static Bitmap createModelInputBitmap(@NonNull Bitmap source, int targetSize) {
        int width = source.getWidth();
        int height = source.getHeight();

        int cropSize = Math.min(width, height);
        int cropX = (width - cropSize) / 2;
        int cropY = (height - cropSize) / 2;

        Bitmap croppedSquare = Bitmap.createBitmap(source, cropX, cropY, cropSize, cropSize);
        if (cropSize == targetSize) {
            return croppedSquare;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(croppedSquare, targetSize, targetSize, true);
        if (scaled != croppedSquare) {
            croppedSquare.recycle();
        }
        return scaled;
    }

    /**
     * Returns an app-private storage directory for storing meal photos.
     * Uses external files directory if available, otherwise internal files directory.
     */
    @NonNull
    public static File getAppPrivateImageDirectory(@NonNull Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            dir = new File(context.getFilesDir(), IMAGE_DIR_NAME);
        } else {
            dir = new File(dir, IMAGE_DIR_NAME);
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Cleans up orphaned meal photos that are neither in active usage nor linked to any meal entry.
     * Also purges unconfirmed temporary captures older than 2 hours.
     *
     * @param context Application context.
     * @param activeImagePaths Set of canonical file paths currently associated with meal records.
     */
    public static void cleanupOrphanedImages(@NonNull Context context,
                                             @Nullable Set<String> activeImagePaths) {
        File imageDir = getAppPrivateImageDirectory(context);
        File[] files = imageDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAgeMillis = 2 * 3600 * 1000; // 2 hours

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String absolutePath = file.getAbsolutePath();
            boolean isReferenced = (activeImagePaths != null && activeImagePaths.contains(absolutePath));

            // If not referenced and older than threshold, delete it
            if (!isReferenced && (now - file.lastModified() > maxAgeMillis)) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d(TAG, "Deleted orphaned temporary image: " + file.getName());
                }
            }
        }
    }
}
