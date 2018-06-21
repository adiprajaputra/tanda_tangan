package com.pace.aplikasittd;

import android.os.Environment;

/**
 * Created by Pace on 18/01/2018.
 */

final class Env {
    static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    static final int MY_PERMISSIONS_REQUEST_STOTRAGE = 101;
    static final String ALLOW_KEY = "ALLOWED";
    static final String CAMERA_PREF = "camera_pref";

    static int MAX_VALUE_SEEKBAR = 80;
    static int DEFAULT_VALUE_SEEKBAR = MAX_VALUE_SEEKBAR/2;
    static int thresholdBiner = 127; /*threshold min = 40, max = 255 berdasarkan dari MAX_VALUE_SEEKBAR*/
    static int RATE_VALUE = thresholdBiner - DEFAULT_VALUE_SEEKBAR;
    static int sizeNormalizationImage = 64;

    static boolean modeSmartSegmentation = false; /*false = normal,  true = smart*/

    static final String pathFolderData = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/dataGlvq/";

    static final String pathWeight = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/dataGlvq/weightGlvq.txt";

    static final String pathClass = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/dataGlvq/classGlvq.txt";

    static final String pathImgGray = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/Grayscale";

    static final String pathImgThinning = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/Thinning";

    static final String pathImgSegmenGray = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/SegmentationGrayscale";

    static final String pathImgSegmenNormGray = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/SegmentationNormalGrayscale";

    static final String pathImgSegmenBiner = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/SegmentationBiner";

    static final String pathImgSegmenNormBiner = Environment.getExternalStorageDirectory()
            + "/Android/data/"
            + BuildConfig.APPLICATION_ID
            + "/SegmentationNormalBiner";

    static final String UPLOAD_URL = "http://192.168.43.175/upload";
}
