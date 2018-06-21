package com.pace.aplikasittd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by root on 28/05/17.
 */

public class Preprocessing {
    private  Bitmap imgMasukan, imgHasil, imgVisualisasiSegmentasi;
    private int widthImg, heightImg, thresholdBiner, sizeNormalizationImage;
    private boolean sudahGrayscale, sudahBiner, sudahSegmentasi, sudahNormalisasi;
    private static final String TAG = MainActivity.class.getSimpleName();
    private List<Integer> histogramX;

    public Preprocessing(Bitmap img) {
        this.imgMasukan = Bitmap.createBitmap(img);
        this.imgHasil = Bitmap.createBitmap(imgMasukan);
        this.imgVisualisasiSegmentasi = Bitmap.createBitmap(imgMasukan);
        this.widthImg = imgMasukan.getWidth();
        this.heightImg = imgMasukan.getHeight();
        this.sudahGrayscale = false;
        this.sudahBiner = false;
        this.sudahSegmentasi = false;
        this.sudahNormalisasi = false;
        this.thresholdBiner = Env.thresholdBiner;
        this.sizeNormalizationImage = Env.sizeNormalizationImage;
    }

    public void setThresholdBiner(int thresholdBiner) {
        this.thresholdBiner = thresholdBiner;
    }

    public Bitmap getImgHasil() {
        return imgHasil;
    }

    public Bitmap getImgVisualisasiSegmentasi() {
        return imgVisualisasiSegmentasi;
    }




    // <!-- PROSES PREPROSESING

    public boolean convertToGrayscale() {
        try {
            Bitmap bmp_hasil = Bitmap.createBitmap(widthImg, heightImg, Bitmap.Config.ARGB_8888);
            int p,r,g,b;
            double piksel;

            for (int i = 0; i < widthImg; i++) {
                for (int j = 0; j < heightImg; j++) {
                    p = imgMasukan.getPixel(i, j);
                    r = Color.red(p);
                    g = Color.green(p);
                    b = Color.blue(p);
                    piksel = ((r*0.299)+(g*0.587)+(b*0.114));

                    r= (int) piksel;

                    bmp_hasil.setPixel(i, j, Color.argb(Color.alpha(p), r, r, r));
                }
            }
            sudahGrayscale = true;
            imgHasil = bmp_hasil;
            imgMasukan = bmp_hasil;

            storeImageGrayscale(Bitmap.createBitmap(imgHasil));

            return true;
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    public boolean convertToBiner(){
        try {
            if(sudahGrayscale) {
                Bitmap bmp_hasil = Bitmap.createBitmap(widthImg, heightImg, Bitmap.Config.ARGB_8888);
                int p,r;

                for (int i = 0; i < widthImg; i++) {
                    for (int j = 0; j < heightImg; j++) {
                        p = imgMasukan.getPixel(i, j);
                        r = Color.red(p);
                        if(r < thresholdBiner){
                            r=0;
                        }
                        else{
                            r=255;
                        }

                        bmp_hasil.setPixel(i, j, Color.argb(Color.alpha(p), r, r, r));
                    }
                }
                sudahBiner = true;
                imgHasil = bmp_hasil;
                return true;
            } else {
                Log.e("Error: ", "Img belum melalui proses grayscale");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    public boolean imageSegmentationSmart() {
        try {
            if(sudahBiner) {
                return imageSegmentationProcessSmart();
            } else {
                Log.e("Error: ", "Img belum melalui proses biner");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    public boolean imageSegmentationNormal() {
        try {
            if(sudahBiner) {
                return imageSegmentationProcessNormal();
            } else {
                Log.e("Error: ", "Img belum melalui proses biner");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    public boolean imageNormalization() {
        try {
            if(sudahSegmentasi) {
                saveImagesSegmentationNormal();
                sudahNormalisasi = true;
                return true;
            } else {
                Log.e("Error: ", "Img belum melalui proses segmentasi");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    public boolean thinningImage() {
        File path = new File(Env.pathImgSegmenNormBiner);

        try {
            if(path.exists()) {
                String[] fileNames = path.list();
                for (String fileName : fileNames) {
                    Log.i("Thinning image : ", fileName);

                    Bitmap mBitmap = BitmapFactory.decodeFile(path.getPath() + "/" + fileName);
                    mBitmap = thinningImageStentiford(mBitmap);
                    storeImageThinning(mBitmap, fileName);
                }
                return true;
            } else {
                Log.e("Error: ", "Path not found");
                return false;
            }
        } catch (Exception err) {
            Log.e("Error: ", err.getMessage());
            return false;
        }
    }

    //PROSES PREPROSESING -->

    // <!-- coba median filter


   // proses median filter -->




    // <!-- PROSES SEGMENTASI

    private boolean imageSegmentationProcessSmart() {
        int thresholdHistogram, jumlahSegmentasi, penguranganThreshold, prefix;
        float tinggiAksara1, tinggiAksara2;
        boolean statusSegmentasi;
        List<Integer> coordCropX, histogramTmp1, histogramTmp2, coordHasilX, coordHasilY, coordTopBottom, coordTmp;

        coordHasilX = new ArrayList<>();
        statusSegmentasi = false;
        prefix = 0;

        deleteAllImage();
        createHistogramX();
        coordCropX = findCoordinatesX(histogramX);

        for(int i=0;i<coordCropX.size();i+=2) {
            histogramTmp1 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1));
            coordTopBottom = cropTopBottom(histogramTmp1);
            thresholdHistogram = createThresholdHistogramY(histogramTmp1);
            histogramTmp2 = histogramTmp1;
            jumlahSegmentasi = countSegmentsHistogramY(histogramTmp2);
            penguranganThreshold = thresholdHistogram/5;
            if(penguranganThreshold==0) {
                penguranganThreshold = 1;
            }

            if(jumlahSegmentasi == 3) {
                // langsung simpan -> 0 = threshold (tidak ada pengurangan histogram)
                histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, 0);

            } else if(jumlahSegmentasi == 2) {
                coordHasilY = findCoordinatesY(histogramTmp2);
                tinggiAksara1 = countHeightAksara(coordHasilY.get(0), coordHasilY.get(1));
                tinggiAksara2 = countHeightAksara(coordHasilY.get(2), coordHasilY.get(3));

                if(tinggiAksara1 * 1.8 < tinggiAksara2) {
                    while (jumlahSegmentasi != 3) {
                        if(thresholdHistogram <= 0) {
                            //jika tidak bisa dibagi 3, langsung aja
                            histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, 0);
                            break;
                        }
                        histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, coordHasilY.get(2), thresholdHistogram);
                        jumlahSegmentasi = countSegmentsHistogramY(histogramTmp2);
                        thresholdHistogram -= penguranganThreshold;
                    }
                } else {
                    // langsung simpan -> 0 = threshold (tidak ada pengurangan histogram)
                    histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, 0);
                }
            } else if (jumlahSegmentasi == 1) {
                jumlahSegmentasi = 3;
                penguranganThreshold *= 3; //speedboost wkwk
                while (jumlahSegmentasi > 2) {
                    if(thresholdHistogram <= 0) {
                        //jika tidak bisa dibagi 3
                        //langsun aja
                        histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, 0);
                        break;
                    }
                    //histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, thresholdHistogram);
                    histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, coordTopBottom.get(0), thresholdHistogram);
                    jumlahSegmentasi = countSegmentsHistogramY(histogramTmp2);
                    thresholdHistogram -= penguranganThreshold;
                }
            } else {
                break;
            }

            coordHasilX.clear();
            coordHasilY = findCoordinatesY(histogramTmp2);
            coordHasilY = normalizationCoordinatesHistogram(coordHasilY, coordTopBottom.get(0));

            for(int j=0;j<coordHasilY.size();j+=2) {
                coordTmp = findCoordinatesFitX(coordCropX.get(i), coordHasilY.get(j), coordCropX.get(i+1), coordHasilY.get(j+1));
                coordHasilX.add(coordTmp.get(0));
                coordHasilX.add(coordTmp.get(1));
            }

            if(i==0)imgVisualisasiSegmentasi = Bitmap.createBitmap(imgHasil);
            for(int j=0;j<coordHasilX.size();j+=2) {
                tesArea(coordHasilX.get(j), coordHasilY.get(j), coordHasilX.get(j+1), coordHasilY.get(j+1));
            }

            prefix += 1;
            saveAllImageSegmentations(coordHasilX, coordHasilY, prefix);

            if(i==coordCropX.size()-2) {
                statusSegmentasi = true;
            }
        }

        if(statusSegmentasi) {
            sudahSegmentasi = true;
            return true;
        } else {
            sudahSegmentasi = false;
            return false;
        }
    }

    private boolean imageSegmentationProcessNormal() {
        int prefix;
        boolean statusSegmentasi;
        List<Integer> coordCropX, histogramTmp1, histogramTmp2, coordHasilX, coordHasilY, coordTopBottom, coordTmp;

        coordHasilX = new ArrayList<>();
        statusSegmentasi = false;
        prefix = 0;

        deleteAllImage();
        createHistogramX();
        coordCropX = findCoordinatesX(histogramX);

        for(int i=0;i<coordCropX.size();i+=2) {
            histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1));
            coordTopBottom = cropTopBottom(histogramTmp2);

            // langsung simpan -> 0 = threshold (tidak ada pengurangan histogram)
            histogramTmp2 = createHistogramY(coordCropX.get(i), coordCropX.get(i+1), coordTopBottom, 0);

            coordHasilX.clear();
            coordHasilY = findCoordinatesY(histogramTmp2);
            coordHasilY = normalizationCoordinatesHistogram(coordHasilY, coordTopBottom.get(0));

            for(int j=0;j<coordHasilY.size();j+=2) {
                coordTmp = findCoordinatesFitX(coordCropX.get(i), coordHasilY.get(j), coordCropX.get(i+1), coordHasilY.get(j+1));
                coordHasilX.add(coordTmp.get(0));
                coordHasilX.add(coordTmp.get(1));
            }

            if(i==0)imgVisualisasiSegmentasi = Bitmap.createBitmap(imgHasil);
            for(int j=0;j<coordHasilX.size();j+=2) {
                tesArea(coordHasilX.get(j), coordHasilY.get(j), coordHasilX.get(j+1), coordHasilY.get(j+1));
            }

            prefix += 1;
            saveAllImageSegmentations(coordHasilX, coordHasilY, prefix);

            if(i==coordCropX.size()-2) {
                statusSegmentasi = true;
            }
        }

        if(statusSegmentasi) {
            sudahSegmentasi = true;
            return true;
        } else {
            sudahSegmentasi = false;
            return false;
        }
    }


    private void deleteAllImage() {
        File mediaStorageDirSegmentationBiner = new File(Env.pathImgSegmenBiner);

        File mediaStorageDirSegmentationGrayscale = new File(Env.pathImgSegmenGray);

        File mediaStorageDirSegmentationNormalBiner = new File(Env.pathImgSegmenNormBiner);

        File mediaStorageDirSegmentationNormalGrayscale = new File(Env.pathImgSegmenNormGray);

        File mediaStorageDirThinning = new File(Env.pathImgThinning);

        if (mediaStorageDirSegmentationBiner.isDirectory()) {
            String[] children = mediaStorageDirSegmentationBiner.list();
            for (String aChildren : children) {
                new File(mediaStorageDirSegmentationBiner, aChildren).delete();
            }
        }

        if (mediaStorageDirSegmentationGrayscale.isDirectory()) {
            String[] children = mediaStorageDirSegmentationGrayscale.list();
            for (String aChildren : children) {
                new File(mediaStorageDirSegmentationGrayscale, aChildren).delete();
            }
        }

        if (mediaStorageDirSegmentationNormalBiner.isDirectory()) {
            String[] children = mediaStorageDirSegmentationNormalBiner.list();
            for (String aChildren : children) {
                new File(mediaStorageDirSegmentationNormalBiner, aChildren).delete();
            }
        }

        if (mediaStorageDirSegmentationNormalGrayscale.isDirectory()) {
            String[] children = mediaStorageDirSegmentationNormalGrayscale.list();
            for (String aChildren : children) {
                new File(mediaStorageDirSegmentationNormalGrayscale, aChildren).delete();
            }
        }

        if (mediaStorageDirThinning.isDirectory()) {
            String[] children = mediaStorageDirThinning.list();
            for (String aChildren : children) {
                new File(mediaStorageDirThinning, aChildren).delete();
            }
        }
    }

    private int countHeightAksara(int koordY1, int koordY2) {
        int hasil;
        hasil = koordY2 - koordY1;
        if(hasil < 0) { hasil = 0; }
        return hasil;
    }

    private void tesArea(int koorX1, int koorY1, int koorX2, int koorY2) {
        int colorSegmentation = Color.parseColor("#4285F4");

        //atas
        for(int i=koorX1;i<=koorX2;i++) {
            imgVisualisasiSegmentasi.setPixel(i, koorY1, colorSegmentation);
        }

        //bawah
        for(int i=koorX1;i<=koorX2;i++) {
            imgVisualisasiSegmentasi.setPixel(i, koorY2, colorSegmentation);
        }

        //kiri
        for(int i=koorY1;i<=koorY2;i++) {
            imgVisualisasiSegmentasi.setPixel(koorX1, i, colorSegmentation);
        }

        //kanan
        for(int i=koorY1;i<=koorY2;i++) {
            imgVisualisasiSegmentasi.setPixel(koorX2, i, colorSegmentation);
        }
    }

    private void createHistogramX() {
        histogramX = new ArrayList<>();

        for (int i = 0; i < widthImg; i++) {
            histogramX.add(0);
        }

        for (int i = 0; i < widthImg; i++) {
            for (int j = 0; j < heightImg; j++) {
                if(Color.red(imgHasil.getPixel(i,j)) == 0) {
                    histogramX.set(i, histogramX.get(i)+1);
                }
            }
        }
    }

    private List<Integer> cropTopBottom(List<Integer> histogram) {
        List<Integer> hasil;
        int PUTIH, panjangMin, panjangSekarang, tmpKoord;
        boolean mulai;

        hasil = new ArrayList<>();
        PUTIH = 0;
        panjangMin = 3;
        panjangSekarang = 0;
        mulai = false;
        tmpKoord = 0;

        for(int i=0;i<histogram.size();i++) {
            if(histogram.get(i) > PUTIH) {
                if(mulai) {
                    panjangSekarang += 1;
                } else {
                    mulai = true;
                    panjangSekarang = 1;
                    tmpKoord = i;
                }

                if(panjangSekarang == panjangMin) {
                    hasil.add(tmpKoord);
                    break;
                }
            } else {
                mulai = false;
            }
        }

        mulai = false;
        for(int i=histogram.size()-1;i>=0;i--) {
            if(histogram.get(i) > PUTIH) {
                if(mulai) {
                    panjangSekarang += 1;
                } else {
                    mulai = true;
                    panjangSekarang = 1;
                    tmpKoord = i;
                }

                if(panjangSekarang == panjangMin) {
                    hasil.add(tmpKoord);
                    break;
                }
            } else {
                mulai = false;
            }
        }

        //0 = awal
        //1 = akhir
        return hasil;
    }

    private List<Integer> createHistogramY(int koordX, int koordY) {
        int HITAM = 0;
        List<Integer> histogramHasil = new ArrayList<>();
        for (int j = 0; j < heightImg; j++) {
            histogramHasil.add(0);
        }

        for (int i = koordX; i < koordY; i++) {
            for (int j = 0; j < heightImg; j++) {
                if(Color.red(imgHasil.getPixel(i,j)) == HITAM) {
                    histogramHasil.set(j, histogramHasil.get(j)+1);
                }
            }
        }

        return histogramHasil;
    }

    private List<Integer> createHistogramY(int koordX1, int koordX2, List<Integer> coordTopBottom, int threshold) {
        int HITAM, coordTOP, coordBOTTOM, index;
        List<Integer> histogramHasil;

        histogramHasil = new ArrayList<>();
        HITAM = 0;
        coordTOP = coordTopBottom.get(0);
        coordBOTTOM = coordTopBottom.get(1);

        for (int j = coordTOP; j <= coordBOTTOM; j++) {
            histogramHasil.add(0);
        }

        for (int i = koordX1; i <= koordX2; i++) {
            for (int j = coordTOP; j <= coordBOTTOM; j++) {
                if(Color.red(imgHasil.getPixel(i,j)) == HITAM) {
                    index = j-coordTOP;
                    histogramHasil.set(index, histogramHasil.get(index)+1);
                }
            }
        }

        for (int j = coordTOP; j <= coordBOTTOM; j++) {
            index = j-coordTOP;
            histogramHasil.set(index, histogramHasil.get(index)-threshold);
            if(histogramHasil.get(index) < 0) {
                histogramHasil.set(index, 0);
            }
        }

        return histogramHasil;
    }

    //untuk 2 segmen yang akan jadi 3 segmen
    private List<Integer> createHistogramY(int koordX1, int koordX2, List<Integer> coordTopBottom, int coordStart,int threshold) {
        int HITAM, coordTOP, coordBOTTOM, index;
        List<Integer> histogramHasil;

        histogramHasil = new ArrayList<>();
        HITAM = 0;
        coordTOP = coordTopBottom.get(0);
        coordBOTTOM = coordTopBottom.get(1);

        for (int j = coordTOP; j <= coordBOTTOM; j++) {
            histogramHasil.add(0);
        }

        for (int i = koordX1; i <= koordX2; i++) {
            for (int j = coordTOP; j <= coordBOTTOM; j++) {
                if(Color.red(imgHasil.getPixel(i,j)) == HITAM) {
                    index = j-coordTOP;
                    histogramHasil.set(index, histogramHasil.get(index)+1);
                }
            }
        }

        for (int j = coordTOP; j <= coordBOTTOM; j++) {
            index = j-coordTOP;
            if(j >= coordStart) {
                histogramHasil.set(index, histogramHasil.get(index)-threshold);
            }
            if(histogramHasil.get(index) < 0) {
                histogramHasil.set(index, 0);
            }
        }

        //biar tidak ter crop aksara bali bagian atasnya
        for (int j = coordStart; j <= coordBOTTOM; j++) {
            index = j-coordTOP;
            if(histogramHasil.get(index) == 0) {
                histogramHasil.set(index, 1);
            } else {
                break;
            }
        }
        //biar tidak ter crop aksara bali bagian bawahnya
        for (int j = coordBOTTOM; j >= coordStart; j--) {
            index = j-coordTOP;
            if(histogramHasil.get(index) == 0) {
                histogramHasil.set(index, 1);
            } else {
                break;
            }
        }

        return histogramHasil;
    }

    private int createThresholdHistogramY(List<Integer> histogram) {
        int jumlah;
        jumlah = 0;

        for(int i = 0; i < histogram.size(); i++) {
            jumlah+=histogram.get(i);
        }

        try {
            return jumlah/histogram.size();
        } catch (Exception err) {
            Log.e("Error : ", err.getMessage());
            return 0;
        }
    }

    private int countSegmentsHistogramY(List<Integer> histogram) {
        int countSegments, ruangKosong;
        boolean awal;

        countSegments = 0;
        ruangKosong = 0;
        awal =  true;

        for (int j = 0; j < histogram.size(); j++) {

            if(histogram.get(j) != ruangKosong) {
                if(awal && (j == 0 || histogram.get(j-1) == ruangKosong)) {
                    countSegments+=1;
                    awal = false;
                } else if(!awal && (j==histogram.size()-1 || histogram.get(j+1) == ruangKosong)){
                    awal = true;
                }
            }
        }

        return countSegments;
    }

    private List<Integer> findCoordinatesY(List<Integer> histogram) {
        List<Integer> kumpulanKoordinatY;
        int ruangKosong, k;
        boolean awal;

        kumpulanKoordinatY = new ArrayList<>();
        ruangKosong = 0;
        awal =  true;
        k = -1;

        for (int j = 0; j < histogram.size(); j++) {

            if(histogram.get(j) != ruangKosong) {
                if(awal && (j == 0 || histogram.get(j-1) == ruangKosong)) {
                    k+=1;
                    kumpulanKoordinatY.add(k, j);
                    awal = false;
                } else if(!awal && (j==histogram.size()-1 || histogram.get(j+1) == ruangKosong)){
                    k+=1;
                    kumpulanKoordinatY.add(k, j);
                    awal = true;
                }
            }
        }

        return kumpulanKoordinatY;
    }

    private List<Integer> findCoordinatesX(List<Integer> histogram) {
        List<Integer> kumpulanKoordinatX;
        int ruangKosong, k;
        boolean awal;

        kumpulanKoordinatX = new ArrayList<>();
        ruangKosong = 0;
        awal =  true;
        k = -1;

        for (int j = 0; j < histogram.size(); j++) {

            if(histogram.get(j) != ruangKosong) {
                if(awal && (j == 0 || histogram.get(j-1) == ruangKosong)) {
                    k+=1;
                    kumpulanKoordinatX.add(k, j);
                    awal = false;
                } else if(!awal && (j==histogram.size()-1 || histogram.get(j+1) == ruangKosong)){
                    k+=1;
                    kumpulanKoordinatX.add(k, j);
                    awal = true;
                }
            }
        }

        return kumpulanKoordinatX;
    }

    private List<Integer> findCoordinatesFitX(int x1, int y1, int x2, int y2) {
        int lengthX, HITAM, panjangMin, panjangSekarang, index, tmpKoord;
        List<Integer> koordHasilBeginEnd, histogram;
        boolean mulai;

        lengthX = x2-x1+1;
        HITAM = 0;
        koordHasilBeginEnd = new ArrayList<>();
        histogram = new ArrayList<>();
        mulai = false;
        panjangMin = 3;
        panjangSekarang = 0;
        tmpKoord = 0;

        for(int i=0;i<lengthX;i++) {
            histogram.add(0);
        }

        for(int i=x1;i<=x2;i++) {
            for(int j=y1;j<=y2;j++) {
                if(Color.red(imgHasil.getPixel(i,j)) == HITAM) {
                    index = i-x1;
                    histogram.set(index, histogram.get(index)+1);
                }
            }
        }

        for(int i=0;i<histogram.size();i++) {
            if(histogram.get(i) > HITAM) {
                if(mulai) {
                    panjangSekarang += 1;
                } else {
                    mulai = true;
                    panjangSekarang = 1;
                    tmpKoord = i;
                }

                if(panjangSekarang == panjangMin) {
                    koordHasilBeginEnd.add(tmpKoord+x1);
                    break;
                }
            } else {
                mulai = false;
            }
        }

        mulai = false;
        for(int i=histogram.size()-1;i>=0;i--) {
            if(histogram.get(i) > HITAM) {
                if(mulai) {
                    panjangSekarang += 1;
                } else {
                    mulai = true;
                    panjangSekarang = 1;
                    tmpKoord = i;
                }

                if(panjangSekarang == panjangMin) {
                    koordHasilBeginEnd.add(tmpKoord+x1);
                    break;
                }
            } else {
                mulai = false;
            }
        }
        // 0 = x1 awal
        // 1 = x2 akhir
        return koordHasilBeginEnd;
    }

    private List<Integer> normalizationCoordinatesHistogram(List<Integer> histogram, int increament) {
        List<Integer> hasil;

        hasil = new ArrayList<>();
        for(int i=0;i<histogram.size();i++) {
            hasil.add(i, histogram.get(i)+increament);
        }
        return hasil;
    }

    private boolean saveAllImageSegmentations(List<Integer> coordX, List<Integer> coordY, int prefix) {
        int sizeeX;
        int penamaanY, tmpKoordX, tmpKoordY, tmpImgWidth, tmpImgHeight;
        Bitmap hasilImg;

        sizeeX = coordX.size();
        penamaanY=0;

        try {
            for(int i=0;i<sizeeX;i+=2) {
                penamaanY+=1;
                tmpKoordX = coordX.get(i);
                tmpKoordY = coordY.get(i);
                tmpImgWidth = coordX.get(i+1) - tmpKoordX;
                tmpImgHeight = coordY.get(i+1) - tmpKoordY;

                hasilImg = Bitmap.createBitmap(imgHasil, tmpKoordX, tmpKoordY, tmpImgWidth, tmpImgHeight);
                storeImageSegmentationBiner(hasilImg, prefix, penamaanY);
                hasilImg = Bitmap.createBitmap(imgMasukan, tmpKoordX, tmpKoordY, tmpImgWidth, tmpImgHeight);
                storeImageSegmentationGrayscale(hasilImg, prefix, penamaanY);
            }

            return true;
        } catch (Exception err) {
            return false;
        }
    }

    // PROSES SEGMENTASI -->





    // <!-- METHOD NORMALISASI GAMBAR

    private void saveImagesSegmentationNormal() {
        try {
            File path = new File(Env.pathImgSegmenGray);

            if(path.exists()) {
                String[] fileNames = path.list();
                for (String fileName : fileNames) {
                    Log.i("Normalization image : ", fileName);

                    Bitmap mBitmap = BitmapFactory.decodeFile(path.getPath() + "/" + fileName);
                    mBitmap = normalizationImage(mBitmap, sizeNormalizationImage, sizeNormalizationImage);
                    storeImageSegmentationNormalGrayscale(mBitmap, fileName);
                }
            }

            path = new File(Env.pathImgSegmenBiner);

            if(path.exists()) {
                String[] fileNames = path.list();
                for (String fileName : fileNames) {
                    Log.i("Normalization image : ", fileName);

                    Bitmap mBitmap = BitmapFactory.decodeFile(path.getPath() + "/" + fileName);
                    mBitmap = normalizationImage(mBitmap, sizeNormalizationImage, sizeNormalizationImage);
                    storeImageSegmentationNormalBiner(mBitmap, fileName);
                }
            }

        } catch (Exception err) {
            Log.e("Error : ", err.getMessage());
        }
    }

    private Bitmap normalizationImage(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    //METHOD NORMALISASI GAMBAR -->






    // <!-- ALGORITMA THINNING STENTIFORD

    private Bitmap thinningImageStentiford(Bitmap imgInput) {
        int jumlah_piksel_terhapus = 1;
        List<Integer> koor_x = new ArrayList<>();
        List<Integer> koor_y = new ArrayList<>();

        Bitmap gambar_tmp;
        Bitmap gambar_hasil_tmp;
        gambar_hasil_tmp = imgInput.copy(Bitmap.Config.ARGB_8888, true);

        int T1=0, T2=0, T3=0, T4=0;

        while(jumlah_piksel_terhapus != 0) {
            koor_x.clear();
            koor_y.clear();
            jumlah_piksel_terhapus = 0;

            gambar_tmp = gambar_hasil_tmp.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = 1; i < gambar_tmp.getHeight() - 1; i++) {
                for (int j = 1; j < gambar_tmp.getWidth() - 1; j++) {
                    //sing manggo asane
                    gambar_hasil_tmp.setPixel(j ,i, gambar_tmp.getPixel(j, i));

                    //jika putih, hitam, hitam (T1)
                    if ((Color.green(gambar_tmp.getPixel(j, i - 1)) == 255) && (Color.green(gambar_tmp.getPixel(j, i)) == 0) && (Color.green(gambar_tmp.getPixel(j, i + 1)) == 0)) {
                        if (cek_jumlah_tetangga_objek(gambar_tmp, j, i) >= 2 && cek_jumlah_tetangga_objek(gambar_tmp, j, i) <= 7) {
                            if (cek_jumlah_konektivitas(gambar_tmp, j, i) == 1) {
                                gambar_hasil_tmp.setPixel(j, i, Color.WHITE);
                                jumlah_piksel_terhapus += 1;
                                T1 += 1;
                            }
                        }
                    }
                }
            }

            gambar_tmp = gambar_hasil_tmp.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = 1; i < gambar_tmp.getWidth() - 1; i++) {
                for (int j = gambar_tmp.getHeight() - 2; j >= 1; j--) {
                    gambar_hasil_tmp.setPixel(i, j, gambar_tmp.getPixel(i, j));//set sesuai image awal

                    //jika putih, hitam, hitam (T2)
                    if ((Color.green(gambar_tmp.getPixel(i - 1, j)) == 255) && (Color.green(gambar_tmp.getPixel(i, j)) == 0) && (Color.green(gambar_tmp.getPixel(i + 1, j)) == 0)) {
                        if (cek_jumlah_tetangga_objek(gambar_tmp, i, j) >= 2 && cek_jumlah_tetangga_objek(gambar_tmp, i, j) <= 7) {
                            if (cek_jumlah_konektivitas(gambar_tmp, i, j) == 1) {
                                gambar_hasil_tmp.setPixel(i, j, Color.WHITE); //set putih
                                jumlah_piksel_terhapus += 1;
                                T2 += 1;
                            }
                        }
                    }
                }
            }

            gambar_tmp = gambar_hasil_tmp.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = gambar_tmp.getHeight() - 2; i >= 1; i--) {
                for (int j = gambar_tmp.getWidth() - 2; j >= 1; j--) {
                    gambar_hasil_tmp.setPixel(j, i, gambar_tmp.getPixel(j, i)); //set sesuai image awal

                    //jika hitam, hitam, putih (T3)
                    if ((Color.green(gambar_tmp.getPixel(j, i - 1)) == 0) && (Color.green(gambar_tmp.getPixel(j, i)) == 0) && (Color.green(gambar_tmp.getPixel(j, i + 1)) == 255)) {
                        if (cek_jumlah_tetangga_objek(gambar_tmp, j, i) >= 2 && cek_jumlah_tetangga_objek(gambar_tmp, j, i) <= 7) {
                            if (cek_jumlah_konektivitas(gambar_tmp, j, i) == 1) {
                                gambar_hasil_tmp.setPixel(j, i, Color.WHITE); //set putih
                                jumlah_piksel_terhapus += 1;
                                T3 += 1;
                            }
                        }
                    }
                }
            }

            gambar_tmp = gambar_hasil_tmp.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = gambar_tmp.getWidth() - 2; i >= 1; i--) {
                for (int j = 1; j <= gambar_tmp.getHeight() - 2; j++) {
                    gambar_hasil_tmp.setPixel(i, j, gambar_tmp.getPixel(i, j)); //set sesuai image awal

                    //jika hitam, hitam, putih(T4)
                    if ((Color.green(gambar_tmp.getPixel(i - 1, j)) == 0) && (Color.green(gambar_tmp.getPixel(i, j)) == 0) && (Color.green(gambar_tmp.getPixel(i + 1, j)) == 255)) {
                        if (cek_jumlah_tetangga_objek(gambar_tmp, i, j) >= 2 && cek_jumlah_tetangga_objek(gambar_tmp, i, j) <= 7) {
                            if (cek_jumlah_konektivitas(gambar_tmp, i, j) == 1) {
                                gambar_hasil_tmp.setPixel(i, j, Color.WHITE); //set putih
                                jumlah_piksel_terhapus += 1;
                                T4 += 1;
                            }
                        }
                    }
                }
            }

            gambar_tmp = Bitmap.createBitmap(gambar_hasil_tmp);

            T1 = 0;
            T2 = 0;
            T3 = 0;
            T4 = 0;

        }

        return gambar_hasil_tmp;
        //gambar_hasil_tmp sebegai hasilnya......
    }

    private int cek_jumlah_tetangga_objek(Bitmap gambar, int x, int y) {
        int hasil = 0;

        if(Color.green(gambar.getPixel(x+1,y))==0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x+1, y-1)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x, y-1)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x-1, y-1)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x-1, y)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x-1, y+1)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x, y+1)) == 0) {
            hasil += 1;
        }
        if (Color.green(gambar.getPixel(x+1, y+1)) == 0) {
            hasil += 1;
        }

        return hasil;
    }

    private int cek_jumlah_konektivitas(Bitmap gambar, int x, int y) {
        int hasil = 0;
        //int[] N = new int[9];

        List<Integer> NN = new ArrayList<>();
        for(int i=0;i<9;i++) {
            NN.add(0);
        }

        if (Color.green(gambar.getPixel(x+1, y)) == 255) {
            //N[0] = 0;
            //N[8] = 0;
            NN.set(0, 0);
            NN.set(8, 0);
        } else {
            NN.set(0, 1);
            NN.set(8, 1);
        }

        if (Color.green(gambar.getPixel(x+1, y-1)) == 255) {
            NN.set(1, 0);
        } else {
            NN.set(1, 1);
        }

        if (Color.green(gambar.getPixel(x, y-1)) == 255) {
            NN.set(2, 0);
        } else {
            NN.set(2, 1);
        }

        if (Color.green(gambar.getPixel(x-1, y-1)) == 255) {
            NN.set(3, 0);
        } else {
            NN.set(3, 1);
        }

        if (Color.green(gambar.getPixel(x-1, y)) == 255) {
            NN.set(4, 0);
        } else {
            NN.set(4, 1);
        }

        if (Color.green(gambar.getPixel(x-1, y+1)) == 255) {
            NN.set(5, 0);
        } else {
            NN.set(5, 1);
        }

        if (Color.green(gambar.getPixel(x, y+1)) == 255) {
            NN.set(6, 0);
        } else {
            NN.set(6, 1);
        }

        if (Color.green(gambar.getPixel(x+1, y+1)) == 255) {
            NN.set(7, 0);
        } else {
            NN.set(7, 1);
        }

        for (int i = 0; i < 7; i+=2) {
            hasil += (NN.get(i) - (NN.get(i) * NN.get(i+1) * NN.get(i+2)));
        }

        return hasil;
    }

    //ALGORITMA THINNING STENTIFORD -->

















    // <!-- SAVE FILE

    private void storeImageGrayscale(Bitmap image) {
        File pictureFile = getOutputMediaFileGrayscale();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileGrayscale(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgGray);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;
        String mImageName="ttd.jpg";

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


    private void storeImageSegmentationGrayscale(Bitmap image, int prefix, int postfix) {
        File pictureFile = getOutputMediaFileSegmentationGrayscale(prefix, postfix);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileSegmentationGrayscale(int prefix, int postfix){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgSegmenGray);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;
        String mImageName="ttd_"+ prefix +"_"+ postfix +".jpg";

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


    private void storeImageSegmentationBiner(Bitmap image, int prefix, int postfix) {
        File pictureFile = getOutputMediaFileSegmentationBiner(prefix, postfix);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileSegmentationBiner(int prefix, int postfix){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgSegmenBiner);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;
        String mImageName="ttd_"+ prefix +"_"+ postfix +".jpg";

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


    private void storeImageSegmentationNormalGrayscale(Bitmap image, String nameFile) {
        File pictureFile = getOutputMediaFileSegmentationNormalGrayscale(nameFile);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileSegmentationNormalGrayscale(String nameFile){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgSegmenNormGray);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + nameFile);
        return mediaFile;
    }


    private void storeImageSegmentationNormalBiner(Bitmap image, String nameFile) {
        File pictureFile = getOutputMediaFileSegmentationNormalBiner(nameFile);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileSegmentationNormalBiner(String nameFile){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgSegmenNormBiner);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + nameFile);
        return mediaFile;
    }


    private void storeImageThinning(Bitmap image, String nameFile) {
        File pictureFile = getOutputMediaFileThinning(nameFile);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        image.recycle();
    }

    private  File getOutputMediaFileThinning(String nameFile){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Env.pathImgThinning);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + nameFile);
        return mediaFile;
    }

    //SAVE FILE -->




}
