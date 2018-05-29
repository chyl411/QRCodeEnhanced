/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.common;

import android.util.Log;

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;

/**
 * This Binarizer implementation uses the old ZXing global histogram approach. It is suitable
 * for low-end mobile devices which don't have enough CPU or memory to use a local thresholding
 * algorithm. However, because it picks a global black point, it cannot handle difficult shadows
 * and gradients.
 * <p>
 * Faster mobile devices and all desktop applications should probably use HybridBinarizer instead.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class GlobalHistogramBinarizer extends Binarizer {

    private static final int LUMINANCE_BITS = 5;
    private static final int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
    private static final int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;
    private static final byte[] EMPTY = new byte[0];

    private byte[] luminances;
    private final int[] buckets;

    public GlobalHistogramBinarizer(LuminanceSource source) {
        super(source);
        luminances = EMPTY;
        buckets = new int[LUMINANCE_BUCKETS];
    }

    // Applies simple sharpening to the row data to improve performance of the 1D Readers.
    @Override
    public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
        LuminanceSource source = getLuminanceSource();
        int width = source.getWidth();
        if (row == null || row.getSize() < width) {
            row = new BitArray(width);
        } else {
            row.clear();
        }

        initArrays(width);
        byte[] localLuminances = source.getRow(y, luminances);
        int[] localBuckets = buckets;
        for (int x = 0; x < width; x++) {
            localBuckets[(localLuminances[x] & 0xff) >> LUMINANCE_SHIFT]++;
        }
        int blackPoint = estimateBlackPoint(localBuckets);

        if (width < 3) {
            // Special case for very small images
            for (int x = 0; x < width; x++) {
                if ((localLuminances[x] & 0xff) < blackPoint) {
                    row.set(x);
                }
            }
        } else {
            int left = localLuminances[0] & 0xff;
            int center = localLuminances[1] & 0xff;
            for (int x = 1; x < width - 1; x++) {
                int right = localLuminances[x + 1] & 0xff;
                // A simple -1 4 -1 box filter with a weight of 2.
                if (((center * 4) - left - right) / 2 < blackPoint) {
                    row.set(x);
                }
                left = center;
                center = right;
            }
        }
        return row;
    }

    // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
    //chyl411 delete
//  @Override
//  public BitMatrix getBlackMatrix() throws NotFoundException {
//    LuminanceSource source = getLuminanceSource();
//    int width = source.getWidth();
//    int height = source.getHeight();
//    BitMatrix matrix = new BitMatrix(width, height);
//
//    // Quickly calculates the histogram by sampling four rows from the image. This proved to be
//    // more robust on the blackbox tests than sampling a diagonal as we used to do.
//    initArrays(width);
//    int[] localBuckets = buckets;
//    for (int y = 1; y < 5; y++) {
//      int row = height * y / 5;
//      byte[] localLuminances = source.getRow(row, luminances);
//      int right = (width * 4) / 5;
//      for (int x = width / 5; x < right; x++) {
//        int pixel = localLuminances[x] & 0xff;
//        localBuckets[pixel >> LUMINANCE_SHIFT]++;
//      }
//    }
//    int blackPoint = estimateBlackPoint(localBuckets);
//
//    // We delay reading the entire image luminance until the black point estimation succeeds.
//    // Although we end up reading four rows twice, it is consistent with our motto of
//    // "fail quickly" which is necessary for continuous scanning.
//    byte[] localLuminances = source.getMatrix();
//    for (int y = 0; y < height; y++) {
//      int offset = y * width;
//      for (int x = 0; x < width; x++) {
//        int pixel = localLuminances[offset + x] & 0xff;
//        if (pixel < blackPoint) {
//          matrix.set(x, y);
//        }
//      }
//    }
//
//    return matrix;
//  }

    //chyl411 add
    @Override
    public BitMatrix getBlackMatrix() throws NotFoundException {
        LuminanceSource source = getLuminanceSource();
        int width = source.getWidth();
        int height = source.getHeight();
        BitMatrix matrix = new BitMatrix(width, height);


        // Quickly calculates the histogram by sampling four rows from the image. This proved to be
        // more robust on the blackbox tests than sampling a diagonal as we used to do.
        initArrays(width);


        Log.i("chyl411", "startedddd..." + System.currentTimeMillis());
        //chyl411 add -------------------------先增加图片的对比度，算法见https://www.codeproject.com/Tips/1172662/Histogram-Equalisation-in-Java
        //增加对比度，目前没什么效果 start
//        int totpix = width * height;
//        int[] histogram = new int[256];
//
//        for (int y = 0; y < height; y++) {
//            byte[] localLuminances = source.getRow(y, luminances);
//            for (int x = 0; x < width; x++) {
//                histogram[localLuminances[x] & 0xff]++;
//            }
//        }
//
//        int[] chistogram = new int[256];
//        chistogram[0] = histogram[0];
//        for (int i = 1; i < 256; i++) {
//            chistogram[i] = chistogram[i - 1] + histogram[i];
//        }
//
//        float[] arr = new float[256];
//        for (int i = 0; i < 256; i++) {
//            arr[i] = (float) ((chistogram[i] * 255.0) / (float) totpix);
//        }
//
//        for (int y = 0; y < height; y++) {
//            byte[] localLuminances = source.getRow(y, luminances);
//            for (int x = 0; x < width; x++) {
//                localLuminances[x] = (byte)arr[0xff & localLuminances[x]];
//            }
//
//            source.setRow(y, localLuminances);
//        }
        //chyl411 add end ------------------------  增加对比度，目前没什么效果 end

        //start----------- 自适应阀值化，感觉处理阴影效果还可以，明亮状态下会出现很多杂点
//        int sum_luminances, sum_distance, mean, count;
//        int maskSize = 3;
//        int C = 15;
//
//        byte[] localLuminances = source.getMatrix();
//        /** find mean and threshold the pixel */
//        for(int y = 0; y < height; y++){
//            for(int x = 0; x < width; x++){
//                sum_luminances = 0;
//                sum_distance = 0;
//                count = 0;
//
//                int currentOffset = y * width;
//                //当前点灰度值
//                int currentLuminance = localLuminances[currentOffset + x] & 0xff;
//
//                for(int r = y - (maskSize / 2); r <= y + (maskSize / 2); r++){
//                    for(int c = x - (maskSize / 2); c <= x + (maskSize / 2); c++){
//                        if(r < 0 || r >= height || c < 0 || c >= width){
//                            /** Some portion of the mask is outside the image. */
//                            continue;
//                        }else{
//                            try{
//                                int offset = r * width;
//                                //adaptive threshold - mean
//                                int tmp = localLuminances[offset + c] & 0xff;
//                                sum_luminances += (tmp & 0xff);
//                                sum_distance += (currentLuminance > tmp ? (currentLuminance - tmp) : (tmp - currentLuminance));
//                                count++;
//                            }catch(ArrayIndexOutOfBoundsException e){
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//
//                /** get mean pixel value */
//                mean = (int)((sum_luminances/count) - C);
//
//                //sum_distance 表示当前点灰度与周围点灰度的变化剧烈程度，越剧烈表示越可能是边缘
//                if((currentLuminance - sum_distance * 0.5) < (mean & 0xff)){
//                    matrix.set(x, y);
//                }
//            }
//        }

        //修改过的自适应阀值二值化算法，目前看效果不错
        int sum_luminances, mean, count;
        int maskSize = 10;
        int C = 0;

        byte[] localLuminances = source.getMatrix();
        /** find mean and threshold the pixel */
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sum_luminances = 0;
                count = 0;

                int currentOffset = y * width;
                //当前点灰度值
                int currentLuminance = localLuminances[currentOffset + x] & 0xff;

                for (int r = y - maskSize; r <= y + maskSize; r += 3) {
                    for (int c = x - maskSize; c <= x + maskSize; c += 3) {
                        if (r < 0 || r >= height || c < 0 || c >= width) {
                            /** Some portion of the mask is outside the image. */
                            continue;
                        } else {
                            try {
                                int offset = r * width;
                                //adaptive threshold - mean
                                int tmp = localLuminances[offset + c] & 0xff;
                                sum_luminances += (tmp & 0xff);
                                count++;
                            } catch (ArrayIndexOutOfBoundsException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                /** get mean pixel value */
                mean = (int) ((sum_luminances / count) - C);

                if (currentLuminance < (mean & 0xff)) {
                    matrix.set(x, y);
                }
            }
        }
        //end----------- 旧的自适应阀值化，感觉处理阴影效果还可以，明亮状态下会出现很多杂点，修改过的还行

        //chyl411 add start --------------------------一维梯度增量法
//        byte[] localLuminance = source.getMatrix();
//        byte[] log = new byte[width];
//        int threshold = 20;
//
//        for(int y = 0; y < height; y++){
//
//            int undetermindStart = 0;
//
//            //System.arraycopy(localLuminance, 0, log, y * width, width);
//
//            boolean shouldBeWhite = true;
//            for(int x = 0; x < width; x++){
//
//                int currentLineOffset = y * width;
//                //当前点灰度值
//                int currentLuminance = localLuminance[currentLineOffset + x] & 0xff;
//
//                //到达当前行末，未决的设定为最后一次梯度跳变后的值
//                if(x == width - 1)
//                {
//                    for(int cursor = undetermindStart; cursor <= x; cursor++)
//                    {
//                        if(shouldBeWhite == false){
//                            matrix.set(cursor, y);
//                        }
//                    }
//                    continue;
//                }
//                //下一点灰度值
//                int nextLuminance = localLuminance[currentLineOffset + x + 1] & 0xff;
//                int delta = nextLuminance - currentLuminance;//增量
//
//
//                //灰度值突然上升
//                if(delta > threshold){
//                    shouldBeWhite = true;
//
//                    //未决定部分应该是黑点
//                    for(int cursor = undetermindStart; cursor <= x; cursor++)
//                    {
//                        matrix.set(cursor, y);
//                    }
//
//                    undetermindStart = x + 1;
//                }
//                //灰度值突然下降
//                else if(delta < -threshold){
//                    shouldBeWhite = false;
//
//                    //未决定部分应该是白点不处理
//                    undetermindStart = x + 1;
//                }
//                else {
//
//                }
//            }
//        }
        //chyl411 add end --------------------------一维梯度增量法
        return matrix;
    }

    @Override
    public Binarizer createBinarizer(LuminanceSource source) {
        return new GlobalHistogramBinarizer(source);
    }

    private void initArrays(int luminanceSize) {
        if (luminances.length < luminanceSize) {
            luminances = new byte[luminanceSize];
        }
        for (int x = 0; x < LUMINANCE_BUCKETS; x++) {
            buckets[x] = 0;
        }
    }

    private static int estimateBlackPoint(int[] buckets) throws NotFoundException {
        // Find the tallest peak in the histogram.
        int numBuckets = buckets.length;
        int maxBucketCount = 0;
        int firstPeak = 0;
        int firstPeakSize = 0;
        for (int x = 0; x < numBuckets; x++) {
            if (buckets[x] > firstPeakSize) {
                firstPeak = x;
                firstPeakSize = buckets[x];
            }
            if (buckets[x] > maxBucketCount) {
                maxBucketCount = buckets[x];
            }
        }

        // Find the second-tallest peak which is somewhat far from the tallest peak.
        int secondPeak = 0;
        int secondPeakScore = 0;
        for (int x = 0; x < numBuckets; x++) {
            int distanceToBiggest = x - firstPeak;
            // Encourage more distant second peaks by multiplying by square of distance.
            int score = buckets[x] * distanceToBiggest * distanceToBiggest;
            if (score > secondPeakScore) {
                secondPeak = x;
                secondPeakScore = score;
            }
        }

        // Make sure firstPeak corresponds to the black peak.
        if (firstPeak > secondPeak) {
            int temp = firstPeak;
            firstPeak = secondPeak;
            secondPeak = temp;
        }

        // If there is too little contrast in the image to pick a meaningful black point, throw rather
        // than waste time trying to decode the image, and risk false positives.
        if (secondPeak - firstPeak <= numBuckets / 16) {
            throw NotFoundException.getNotFoundInstance();
        }

        // Find a valley between them that is low and closer to the white peak.
        int bestValley = secondPeak - 1;
        int bestValleyScore = -1;
        for (int x = secondPeak - 1; x > firstPeak; x--) {
            int fromFirst = x - firstPeak;
            int score = fromFirst * fromFirst * (secondPeak - x) * (maxBucketCount - buckets[x]);
            if (score > bestValleyScore) {
                bestValley = x;
                bestValleyScore = score;
            }
        }

        return bestValley << LUMINANCE_SHIFT;
    }

}
