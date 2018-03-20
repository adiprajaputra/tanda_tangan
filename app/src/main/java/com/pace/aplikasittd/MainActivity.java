package com.pace.aplikasittd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Uri mCropImageUri;

    private ImageView imageView_awal;
    private ImageView imageView_grayscale;
    private ImageView imageView_filter;
    private ImageView imageView_biner;
    private ImageView imageView_segmentation;
    private ImageView imageView_thinning;
    //private ImageView imageView_pca;

    private TextView textView_gambarAwal;
    private TextView textView_gambarGrayscale;
    private TextView textView_gambarFilter;
    private TextView textView_gambarBiner;
    private TextView textView_gambarSegmentasi;
    private TextView textView_gambarThinning;
    //private TextView textView_pca;
    private TextView textView_hasil;
    private TextView textView_hasilAkhir;

    private ProgressDialog loading;

    private SeekBar sliderThresholding;

    private ScrollView mScrollView;

    Preprocessing preprocessing;

    Pca pca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cekPermissionKameraStorage();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


    }






    private void cekPermissionKameraStorage(){
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
            }, Env.MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else {
            pilihSumberGambar();
        }
    }

    private boolean adaFileWeightClass() {
        File file1 = new File(Env.pathWeight);
        File file2 = new File(Env.pathClass);
        return file1.exists() && file2.exists();
    }

    private void pilihSumberGambar() {
        CropImage.startPickImageActivity(this);
    }

    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.OFF).setMultiTouchEnabled(true).setActivityTitle("Select Signature").start(this);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle result of pick image chooser (startPickImageActivity)
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
            else {
                // no permissions required or already grunted, can start crop image activity
                startCropImageActivity(imageUri);
            }
        }

        // handle result of CropImageActivity
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                initLayout();

                ImageView imv1;
                try {
                    Bitmap tmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result.getUri());
                    textView_gambarAwal.setText("Input image ("+tmp.getWidth()+" x "+tmp.getHeight()+")");

                    imv1=(ImageView)findViewById(R.id.imageView_asli);
                    imv1.setImageBitmap(tmp);

                    prosesGambarGrayscale(result.getUri());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.i("requestCode", String.valueOf(requestCode));
        Log.i("permissions", permissions[0]);
        Log.i("grantResults", String.valueOf(grantResults[0]));

        switch (requestCode) {
            case Env.MY_PERMISSIONS_REQUEST_CAMERA: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        saveToPreferences(MainActivity.this, Env.ALLOW_KEY, true);
                    }
                }
                break;
            }

            case Env.MY_PERMISSIONS_REQUEST_STOTRAGE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        saveToPreferences(MainActivity.this, Env.ALLOW_KEY, true);
                    }
                }
                break;
            }
        }

        if((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "App need access Camera & Storage", Toast.LENGTH_LONG).show();
        }
        else {
            pilihSumberGambar();
        }
    }

    public static void saveToPreferences(Context context, String key, Boolean allowed) {
        SharedPreferences myPrefs = context.getSharedPreferences(Env.CAMERA_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(key, allowed);
        prefsEditor.apply();
    }




    private void initLayout() {
        imageView_awal = new ImageView(this);
        imageView_grayscale = new ImageView(this);
        imageView_filter = new ImageView(this);
        imageView_biner = new ImageView(this);
        imageView_segmentation = new ImageView(this);
        imageView_thinning = new ImageView(this);

        textView_gambarAwal = new TextView(this);
        textView_gambarGrayscale = new TextView(this);
        textView_gambarFilter = new TextView(this);
        textView_gambarBiner = new TextView(this);
        textView_gambarSegmentasi = new TextView(this);
        textView_gambarThinning = new TextView(this);
        textView_hasil = new TextView(this);
        textView_hasilAkhir = new TextView(this);

        sliderThresholding = new SeekBar(this);
        ShapeDrawable thumb = new ShapeDrawable(new OvalShape());
        thumb.setIntrinsicHeight(40);
        thumb.setIntrinsicWidth(40);
        sliderThresholding.setThumb(thumb);
        sliderThresholding.setProgress(1);
        sliderThresholding.setVisibility(View.GONE);
        sliderThresholding.setId(R.id.slider_thresholding);
        sliderThresholding.setMax(Env.MAX_VALUE_SEEKBAR);
        sliderThresholding.setProgress(Env.DEFAULT_VALUE_SEEKBAR);
        sliderThresholding.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                preprocessing.setThresholdBiner(i + Env.RATE_VALUE);
                textView_gambarBiner.setText("Binary image (threshold " + (i + Env.RATE_VALUE) + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //proses ulang
                prosesGambarBiner();
            }
        });

        mScrollView = (ScrollView)findViewById(R.id.scrool_view_utama);

        LinearLayout outerLinearLayout = (LinearLayout) findViewById(R.id.content_main);
        outerLinearLayout.removeAllViews();

        LinearLayout.LayoutParams params_layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams params_textview = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams params_textviewAkhir = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams params_textviewAkhirText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams params_slider = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        params_slider.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -16, getResources().getDisplayMetrics());
        params_slider.bottomMargin = 42;
        params_textview.bottomMargin = 42;
        params_textviewAkhir.bottomMargin = 134;
        params_textviewAkhirText.topMargin = 40;

        //gambar awal
        imageView_awal.setLayoutParams(params_layout);
        imageView_awal.setAdjustViewBounds(true);
        imageView_awal.setId(R.id.imageView_asli);
        outerLinearLayout.addView(imageView_awal);

        //text gambar awal
        textView_gambarAwal.setLayoutParams(params_textview);
        textView_gambarAwal.setGravity(Gravity.CENTER);
        textView_gambarAwal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarAwal.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarAwal);

        //gambar grayscale
        imageView_grayscale.setLayoutParams(params_layout);
        imageView_grayscale.setAdjustViewBounds(true);
        imageView_grayscale.setId(R.id.imageView_grayscale);
        outerLinearLayout.addView(imageView_grayscale);

        //text gambar grayscale
        textView_gambarGrayscale.setLayoutParams(params_textview);
        textView_gambarGrayscale.setGravity(Gravity.CENTER);
        textView_gambarGrayscale.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarGrayscale.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarGrayscale);

        //gambar filter
        imageView_filter.setLayoutParams(params_layout);
        imageView_filter.setAdjustViewBounds(true);
        imageView_filter.setId(R.id.imageView_filter);
        outerLinearLayout.addView(imageView_filter);

        //text gambar filter
        textView_gambarFilter.setLayoutParams(params_textview);
        textView_gambarFilter.setGravity(Gravity.CENTER);
        textView_gambarFilter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarFilter.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarFilter);

        //gambar biner
        imageView_biner.setLayoutParams(params_layout);
        imageView_biner.setAdjustViewBounds(true);
        imageView_biner.setId(R.id.imageView_biner);
        outerLinearLayout.addView(imageView_biner);

        //text gambar biner
        textView_gambarBiner.setLayoutParams(params_textview);
        textView_gambarBiner.setGravity(Gravity.CENTER);
        textView_gambarBiner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarBiner.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarBiner);

        //slider thresholding
        sliderThresholding.setLayoutParams(params_slider);
        outerLinearLayout.addView(sliderThresholding);

        //gambar segmentasi
        imageView_segmentation.setLayoutParams(params_layout);
        imageView_segmentation.setAdjustViewBounds(true);
        imageView_segmentation.setId(R.id.imageView_segmentation);
        outerLinearLayout.addView(imageView_segmentation);

        //text gambar segmentasi
        textView_gambarSegmentasi.setLayoutParams(params_textview);
        textView_gambarSegmentasi.setGravity(Gravity.CENTER);
        textView_gambarSegmentasi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarSegmentasi.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarSegmentasi);

        //gambar thinning
        imageView_thinning.setLayoutParams(params_layout);
        imageView_thinning.setAdjustViewBounds(true);
        imageView_thinning.setId(R.id.imageView_thinning);
        outerLinearLayout.addView(imageView_thinning);

        //text gambar thinning
        textView_gambarThinning.setLayoutParams(params_textview);
        textView_gambarThinning.setGravity(Gravity.CENTER);
        textView_gambarThinning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_gambarThinning.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_gambarThinning);

        textView_hasil.setLayoutParams(params_textviewAkhirText);
        textView_hasil.setGravity(Gravity.CENTER);
        textView_hasil.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textView_hasil.setTextColor(Color.BLACK);
        outerLinearLayout.addView(textView_hasil);

        textView_hasilAkhir.setLayoutParams(params_textviewAkhir);
        textView_hasilAkhir.setGravity(Gravity.CENTER);
        textView_hasilAkhir.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        textView_hasilAkhir.setTextColor(Color.BLACK);
        textView_hasilAkhir.setTypeface(null, Typeface.BOLD);
        textView_hasilAkhir.setMaxLines(4);
        textView_hasilAkhir.setBackgroundColor(Color.WHITE);
        textView_hasilAkhir.setVisibility(View.GONE);
        outerLinearLayout.addView(textView_hasilAkhir);
    }




    //==== AWAL =====

    private void prosesGambarGrayscale(Uri urigambarttd) {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Process 1 of 8", "Convert image to grayscale...", true, false);
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                ImageView imv;
                imv = (ImageView) findViewById(R.id.imageView_grayscale);
                if(s) {
                    imv.setImageBitmap(preprocessing.getImgHasil());
                    textView_gambarGrayscale.setText("Grayscale image");
                    scroolDownPage();
                    prosesGambarMedianFilter();
                } else {
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Failed to convert image to grayscale!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                try {
                    preprocessing = new Preprocessing(MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uris[0]));
                    return preprocessing.convertToGrayscale();

                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute(urigambarttd);
    }

    private void prosesGambarMedianFilter() {
        class ProcessInBackground extends AsyncTask<Void, Void, Bitmap> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(!loading.isShowing()) {
                    loading = ProgressDialog.show(MainActivity.this, "Process 2 of 8", "Filtering image...", true, false);
                } else {
                    loading.setTitle("Process 2 of 7");
                    loading.setMessage("Filtering image...");
                }            }

            @Override
            protected void onPostExecute(Bitmap s) {
                super.onPostExecute(s);

                ImageView imv;
                imv = (ImageView) findViewById(R.id.imageView_filter);
                imv.setImageBitmap(s);
                textView_gambarFilter.setText("Filtered image");
                scroolDownPage();
                prosesGambarBiner();
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                ImageFilter aa = new MedianFilter(preprocessing.getImgHasil());
                return aa.applyFilter();
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    private void prosesGambarBiner() {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(!loading.isShowing()) {
                    loading = ProgressDialog.show(MainActivity.this, "Process 3 of 8", "Convert image to binary...", true, false);
                } else {
                    loading.setTitle("Process 2 of 7");
                    loading.setMessage("Convert image to binary...");
                }
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                ImageView imv;
                imv = (ImageView) findViewById(R.id.imageView_biner);
                if(s) {
                    imv.setImageBitmap(preprocessing.getImgHasil());
                    textView_gambarBiner.setText("Binary image (threshold " + (sliderThresholding.getProgress() + Env.RATE_VALUE) + ")");
                    scroolDownPage();
                    prosesGambarSegmentasi();
                } else {
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Failed to convert image to binary!", Toast.LENGTH_SHORT).show();
                }
                sliderThresholding.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                return preprocessing.convertToBiner();
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    private void prosesGambarSegmentasi() {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading.setTitle("Process 4 of 8");
                loading.setMessage("Image segmentation...");
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                ImageView imv;
                imv = (ImageView) findViewById(R.id.imageView_segmentation);
                if(s) {
                    imv.setImageBitmap(preprocessing.getImgVisualisasiSegmentasi());
                    textView_gambarSegmentasi.setText("Image Segmentation");
                    scroolDownPage();
                    prosesGambarNormalisasi();
                } else {
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Failed to segment the image!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                if(Env.modeSmartSegmentation) {
                    return preprocessing.imageSegmentationSmart();
                } else {
                    return preprocessing.imageSegmentationNormal();
                }
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    private void prosesGambarNormalisasi() {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading.setTitle("Process 5 of 8");
                loading.setMessage("Normalize the image...");
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                if(s) {
                    prosesGambarThinning();
                } else {
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Failed to normalize the image!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                return preprocessing.imageNormalization();
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    private void prosesGambarThinning() {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading.setTitle("Process 6 of 8");
                loading.setMessage("Thinning image...");

                imageView_thinning.setVisibility(View.GONE);
                textView_gambarThinning.setVisibility(View.GONE);
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                if(s) {
                    //prosesPca();
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Selesai", Toast.LENGTH_SHORT).show();
                } else {
                    hideDialog();
                    Toast.makeText(MainActivity.this, "Failed to thinning the image!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                return preprocessing.thinningImage();
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    //PCA
//    private void prosesPca() {
//        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                loading.setTitle("Process 7 of 8");
//                loading.setMessage("PCA...");
//
//                //imageView_thinning.setVisibility(View.GONE);
//                //textView_gambarThinning.setVisibility(View.GONE);
//            }
//
//            @Override
//            protected void onPostExecute(Boolean s) {
//                super.onPostExecute(s);
//
//                if(s) {
//                    hideDialog();
//                    Toast.makeText(MainActivity.this, "Selesai", Toast.LENGTH_SHORT).show();
//                } else {
//                    hideDialog();
//                    Toast.makeText(MainActivity.this, "Failed to thinning the image!", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            protected Boolean doInBackground(Uri... uris) {
//                return pca.Pca();
//            }
//        }
//
//        ProcessInBackground ulc=new ProcessInBackground();
//        ulc.execute();
//    }







    private void showDialog() {
        if (!loading.isShowing())
            loading.show();
    }

    private void hideDialog() {
        if (loading.isShowing())
            loading.dismiss();
    }

    private void scroolDownPage() {
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }






























    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            coomingSoon();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_cameraGalery) {
            cekPermissionKameraStorage();
        } else if (id == R.id.nav_setings) {
            coomingSoon();
        } else if (id == R.id.nav_checkUpdate) {
            checkForUpdateDatabase();
        } else if (id == R.id.nav_about) {
            Intent about = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(about);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void coomingSoon(){
        // make snackbar
        Snackbar mSnackbar = Snackbar.make(this.findViewById(android.R.id.content), "ONGoing broo...", Snackbar.LENGTH_LONG);
        // get snackbar view
        View mView = mSnackbar.getView();
        // get textview inside snackbar view
        TextView mTextView = (TextView) mView.findViewById(android.support.design.R.id.snackbar_text);
        // set text to center
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            mTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        else
            mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        // show the snackbar
        mSnackbar.show();
    }

    private void checkForUpdateDatabase() {
        class ProcessInBackground extends AsyncTask<Uri, Void, Boolean> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Check for update", "Downloading update...", true, false);
            }

            @Override
            protected void onPostExecute(Boolean s) {
                super.onPostExecute(s);

                hideDialog();
                if(s) {
                    Toast.makeText(MainActivity.this, "Successfully download update", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed download update", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected Boolean doInBackground(Uri... uris) {
                try {
                    String server, user, pass, filenameWeight, filenameClass;
                    int port;
                    File localFileWeight, localFileClass, localFolder;

                    server = "www.etani.id";
                    user = "agussuarya@etani.id";
                    pass = "fjhFH__$vnbDD";
                    filenameWeight = "public_html/agussuarya/dataglvq/bobotGlvq.txt";
                    filenameClass = "public_html/agussuarya/dataglvq/kelasGlvq.txt";
                    port = 21;
                    localFolder = new File(Env.pathFolderData);
                    localFileWeight = new File(Env.pathWeight);
                    localFileClass = new File(Env.pathClass);

                    if (! localFolder.exists()){
                        localFolder.mkdir();
                    }

                    if(! localFileWeight.exists()) {
                        FileWriter writer = new FileWriter(localFileWeight);
                        writer.flush();
                        writer.close();
                    }
                    if(! localFileClass.exists()) {
                        FileWriter writer = new FileWriter(localFileClass);
                        writer.flush();
                        writer.close();
                    }

                    if(downloadAndSaveFile(server, port, user, pass, filenameClass, filenameWeight, localFileClass, localFileWeight)) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        ProcessInBackground ulc=new ProcessInBackground();
        ulc.execute();
    }

    private Boolean downloadAndSaveFile(String server, int portNumber, String user, String password, String filenameClass, String filenameWeight, File localFileClass, File localFileWeight) throws IOException {
        FTPClient ftp = null;

        try {
            ftp = new FTPClient();
            ftp.setConnectTimeout(5 * 1000);
            ftp.connect(server, portNumber);
            Log.d("downloadAndSaveFile : ", "Connected. Reply: " + ftp.getReplyString());

            ftp.login(user, password);
            Log.d("downloadAndSaveFile : ", "Logged in");
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            Log.d("downloadAndSaveFile : ", "Downloading");
            ftp.enterLocalPassiveMode();

            OutputStream outputStreamClass = null;
            OutputStream outputStreamWeight = null;

            boolean success = false;
            try {
                outputStreamClass = new BufferedOutputStream(new FileOutputStream(localFileClass));
                success = ftp.retrieveFile(filenameClass, outputStreamClass);
                outputStreamWeight = new BufferedOutputStream(new FileOutputStream(localFileWeight));
                success = ftp.retrieveFile(filenameWeight, outputStreamWeight);

            } finally {
                if (outputStreamClass != null) {
                    outputStreamClass.close();
                }
                if (outputStreamWeight != null) {
                    outputStreamWeight.close();
                }
            }

            return success;
        } finally {
            if (ftp != null) {
                ftp.logout();
                ftp.disconnect();
            }
        }
    }

}
