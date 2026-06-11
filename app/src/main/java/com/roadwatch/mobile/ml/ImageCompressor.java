package com.roadwatch.mobile.ml;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Downscales + JPEG-compresses photos taken by CameraX before upload.
 *
 * Why this exists:
 *  - Raw camera output on modern phones is 8-15 MB. The backend caps multipart
 *    uploads at 15 MB and our YOLO model only needs ~1280px on the long edge.
 *  - Running on cellular, sending 10 MB per complaint is wasteful and slow.
 *
 * The compressor:
 *  - Reads the JPEG dimensions without decoding the full bitmap (inJustDecodeBounds).
 *  - Computes an inSampleSize so the long edge ends up ~MAX_DIMENSION_PX.
 *  - Honours EXIF rotation so portraits don't end up sideways on the admin website.
 *  - Re-encodes at JPEG quality 80, which is visually indistinguishable from the
 *    original for road photos but typically 5-10x smaller.
 *  - Overwrites the file in place so the rest of the upload pipeline doesn't
 *    need to know about it.
 */
public final class ImageCompressor {

    private static final String TAG = "ImageCompressor";

    /** Long-edge target in pixels €” keeps detail for AI but cuts size dramatically. */
    private static final int MAX_DIMENSION_PX = 1600;

    /** Files smaller than this are left untouched (already small enough). */
    private static final long SKIP_BELOW_BYTES = 800L * 1024L; // 800 KB

    /** JPEG quality (0-100). 80 is the sweet spot for size vs. visual quality. */
    private static final int JPEG_QUALITY = 80;

    private ImageCompressor() {
    }

    /**
     * Compresses {@code file} in place. Safe to call on any error path €”
     * if compression fails the original file is left untouched.
     *
     * @return {@code true} if the file was rewritten, {@code false} if skipped.
     */
    public static boolean compressInPlace(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        long originalSize = file.length();
        if (originalSize <= SKIP_BELOW_BYTES) {
            return false;
        }

        Bitmap scaled = null;
        Bitmap rotated = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return false;
            }

            BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight);
            decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
            scaled = BitmapFactory.decodeFile(file.getAbsolutePath(), decode);
            if (scaled == null) {
                return false;
            }

            rotated = applyExifRotation(scaled, file);

            try (FileOutputStream out = new FileOutputStream(file)) {
                rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
                out.flush();
            }
            Log.d(TAG, "Compressed " + file.getName() + " from " + originalSize
                    + " to " + file.length() + " bytes");
            return true;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Compression failed for " + file.getName() + ", uploading original", e);
            return false;
        } finally {
            if (rotated != null && rotated != scaled) {
                rotated.recycle();
            }
            if (scaled != null) {
                scaled.recycle();
            }
        }
    }

    private static int computeInSampleSize(int width, int height) {
        int longEdge = Math.max(width, height);
        int sampleSize = 1;
        while (longEdge / (sampleSize * 2) >= MAX_DIMENSION_PX) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static Bitmap applyExifRotation(Bitmap bitmap, File file) {
        int degrees = readExifRotationDegrees(file);
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static int readExifRotationDegrees(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
