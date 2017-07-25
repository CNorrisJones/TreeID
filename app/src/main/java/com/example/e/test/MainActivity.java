package com.example.e.test;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;



import com.example.e.test.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.ListIterator;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiImage;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import clarifai2.dto.prediction.Prediction;

import static android.support.v4.content.FileProvider.getUriForFile;


//camera methods adapted from Android Studio tutorial at https://developer.android.com/training/camera/photobasics.html#TaskPath
//and raywenderlich "memeify" tutorial

//TODO: transfer all bitmap handling to Glide
//TODO: put all strings in res/strings
//TODO: dagger, butterknife
//TODO: IDMap
//TODO: IDHistory
public class MainActivity extends AppCompatActivity {

    private static final int TAKE_PHOTO_REQUEST_CODE = 1;
    private static final int CHOOSE_PHOTO_REQUEST_CODE = 2;
    private File newFile;
    private Uri selectedPhotoPath;
    private Bitmap bitmap;
    private TextView predictionTextView;
    ClarifaiClient client;
    private String info = ("Select 'Choose picture from gallery' or 'Get photo from camera' to load a new image. \n\nTap the picture to get an ID.");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        predictionTextView = (TextView) findViewById(R.id.predictionTextView);
        predictionTextView.setText(info);
        client = new ClarifaiBuilder(getResources().getString(R.string.clarifai_api_key)).buildSync();
    }

    public void viewIDHistory(View view) {
        Intent intent = new Intent(this, IDHistoryActivity.class);
        startActivity(intent);
    }


    //TODO: send photos to external storage
    //TODO: generate date/time filename
    public void dispatchTakePictureIntent(View view) {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagePath = new File(getFilesDir(), "images");
        newFile = new File(imagePath, "default_image.jpg");
        if (newFile.exists()) {
            newFile.delete();
        } else {
            newFile.getParentFile().mkdirs();
        }
        selectedPhotoPath = getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile);

        captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedPhotoPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            ClipData clip = ClipData.newUri(getContentResolver(), "A photo", selectedPhotoPath);
            captureIntent.setClipData(clip);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        startActivityForResult(captureIntent, TAKE_PHOTO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            setImageViewWithImage();
        }
        if (requestCode == CHOOSE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("clarifai", "made it to Result");
            selectedPhotoPath = data.getData();

            setImageViewWithImage();
        }
    }

    //makes use of 'BitmapResizer' provided in the
    //starter code for raywenderlich intents2 tutorial.
    //TODO: make resizing consistent between default image and new images
    private void setImageViewWithImage() {
        predictionTextView.setText(info);
        Log.d("clarifai", "made it to setImage");
        bitmap = com.example.e.test.BitmapResizer.shrinkBitmap(
                MainActivity.this,
                selectedPhotoPath,
                (findViewById(R.id.imageView)).getWidth(),
                (findViewById(R.id.imageView)).getHeight()
        );
        ((ImageView) findViewById(R.id.imageView)).setImageBitmap(bitmap);
    }

    public void getImageFromGallery(View view) {

//adapted from codetheory http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedPhotoPath);
        startActivityForResult(Intent.createChooser(intent, "Choose picture"), CHOOSE_PHOTO_REQUEST_CODE);


        Log.d("clarifai", "gallery button is clicked");
    }

    //gets prediction.
    //adapted from code in clarifai android starter app.
    //TODO: put the error messages in res/strings
    public void submitImage(View view) {
        Log.d("clarifai", "image is clicked");
        predictionTextView.setText("\nPlease wait");

        new AsyncTask<Object, Object, ClarifaiResponse<List<ClarifaiOutput<Prediction>>>>() {
            @Override
            protected ClarifaiResponse<List<ClarifaiOutput<Prediction>>> doInBackground(Object... params) {
                Log.d("clarifai", "async start");


                //user is calling Clarifai with the default image, for which a Uri is not available
                if (selectedPhotoPath == null) {

//hat tip   https://randula.wordpress.com/2012/04/01/android-drawable-to-byte-array/
                    Resources res = getResources();

                    Drawable drawable;
                    //inserted api level check and deprecated method to cover apis below 21, down to 17
                    if (Build.VERSION.SDK_INT >= 21) {
                        drawable = res.getDrawable(R.drawable.cedar,null);
                    }
                    else {
                        drawable = res.getDrawable(R.drawable.cedar);
                    }


                    bitmap = ((BitmapDrawable) drawable).getBitmap();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                    byte[] bitMapData = stream.toByteArray();
//end android-drawable-to-byte-array

                    return client.predict("pspbark")
                            .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(bitMapData)))
                            .executeSync();
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] bitMapData = stream.toByteArray();
                return client.predict("pspbark")
                        .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(bitMapData)))
                        .executeSync();
            }

            @Override
            protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Prediction>>> response) {
                if (!response.isSuccessful()) {
                    predictionTextView.setText("\nCall to Clarifai was unsuccessful");
                    return;
                }
                final List<ClarifaiOutput<Prediction>> predictions = response.get();
                if (predictions.isEmpty()) {
                    predictionTextView.setText("\nNo predictions were returned");
                    return;
                }
                ListIterator iter = predictions.get(0).data().listIterator();
                String prediction = "\n";

                while (iter.hasNext()) {
                    Concept c = (Concept) iter.next();
                    String name = c.name();
                    String value = String.format("%.4f", c.value());
                    String report = name + ": " + value;
                    prediction += report + "\n";
                    Log.d("clarifai", report);
                }
                predictionTextView.setText(prediction);
                Log.d("clarifai", "made it to onPostExecute: done printing predictions");
            }
        }.execute();
    }


}
