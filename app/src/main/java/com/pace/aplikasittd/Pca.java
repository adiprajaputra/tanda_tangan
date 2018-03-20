package com.pace.aplikasittd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;

/**
 * Created by Pace on 2/28/2018.
 */

public class Pca {

//    private int sizeNormalizationImage, sizeAreaNormalizationImage;
//    private File pathImgThinning, pathImgGrayscale;
//
//    public Pca(File pathImgThinning, File pathImgGrayscale, int sizeNormalizationImage) {
//        this.sizeNormalizationImage = sizeNormalizationImage;
//        this.sizeAreaNormalizationImage = sizeNormalizationImage*sizeNormalizationImage;
//        this.pathImgThinning = pathImgThinning;
//        this.pathImgGrayscale = pathImgGrayscale;
//    }

    /*public boolean Pca(){
        return true;
    }
    public boolean extractFeature() {
        File path = new File(Env.pathImgSegmenNormBiner);
        try {
            if (path.exists()) {
                String[] fileNames = path.list();
                for (String fileName : fileNames) {
                    Log.i("PCA Filename : ", fileName);

                    //Bitmap mBitmap = BitmapFactory.decodeFile(path.getPath() + "/" + fileName);
                    //mBitmap = thinningImageStentiford(mBitmap);
                    //storeImageThinning(mBitmap, fileName);
                }
            } else {
                Log.e("Error: ", "Path not found");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
        return true;
    }*/
}
