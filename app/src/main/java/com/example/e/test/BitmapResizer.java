
package com.example.e.test;

//Taken from raywenderlich android intents tutorial "memeify"
/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;




public class BitmapResizer {

    public static Bitmap shrinkBitmap(Context ctx, Uri uri, int width, int height) {
        InputStream input;
        try {
            input = ctx.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }

        if (!input.markSupported()) { // InputStream doesn't support mark(). so wrap it into BufferedInputStream & use that
            input = new BufferedInputStream(input);
        }

        try {
            input.mark(input.available()); // input.isavailable() gives size of input stream
        } catch (IOException e) {
            e.printStackTrace();
        }


        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;

        //Need to decodestream . else bmpFactoryOptions will be zero. so insamplesize will be zero
        BitmapFactory.decodeStream(input, null, bmpFactoryOptions);

        int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
        int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);

        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) {
                bmpFactoryOptions.inSampleSize = heightRatio;
            } else {
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        bmpFactoryOptions.inJustDecodeBounds = false;

        try {
            input.reset(); // Resetting input stream
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmpFactoryOptions);

        // Code to fix orientation issue on some devices
        bitmap=rotateBitmapIfRequired(ctx,bitmap);

        return bitmap;
    }

    private static Bitmap rotateBitmapIfRequired(Context ctx, Bitmap bitmap) {
        // captured image is saved as "default_image.jpg" inside "images" folder. "imgFile.getAbsolutePath()" will contain that path

        File imagePath = new File(ctx.getFilesDir(), "images");
        File imgFile = new File(imagePath, "default_image.jpg");

        ExifInterface exif = null;
        try {
            //  exif = new ExifInterface(imgFile.getAbsolutePath());
            exif = new ExifInterface(imgFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                bitmap=rotateImage(bitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                bitmap=rotateImage(bitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                bitmap=rotateImage(bitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:

            default:
                break;
        }
        return  bitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}