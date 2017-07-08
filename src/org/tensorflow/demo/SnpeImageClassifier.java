/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import static com.qualcomm.qti.snpe.NeuralNetwork.Runtime.CPU;
import static com.qualcomm.qti.snpe.NeuralNetwork.Runtime.DSP;
import static com.qualcomm.qti.snpe.NeuralNetwork.Runtime.GPU;

public class SnpeImageClassifier implements Classifier {

  private static final String TAG = "TensorFlowImageClassifier";

  // Only return this many results with at least this confidence.
  private static final int MAX_RESULTS = 3;
  private static final float THRESHOLD = 0.1f;

  // Config values.
  private String inputName;
  private String outputName;
  private int inputSize;
  private int imageMean;
  private float imageStd;

  // Pre-allocated buffers.
  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private float[] outputs;

  private boolean logStats = false;

  private NeuralNetwork inferenceInterface;

  private FloatTensor inputTensor;
  private String version;

  private SnpeImageClassifier() {}

  /**
   * Initializes a native TensorFlow session for classifying images.
   *
   * @param assetManager The asset manager to be used to load assets.
   * @param modelFilename The filepath of the model GraphDef protocol buffer.
   * @param labelFilename The filepath of label file for classes.
   * @param inputSize The input size. A square image of inputSize x inputSize is assumed.
   * @param imageMean The assumed mean of the image values.
   * @param imageStd The assumed std of the image values.
   * @param inputName The label of the image input node.
   * @param outputName The label of the output node.
   * @throws IOException
   */
  public static Classifier create(
          Application application,
          AssetManager assetManager,
          String modelFilename,
          String labelFilename,
          int inputSize,
          int imageMean,
          float imageStd,
          String inputName,
          String outputName,
          int numClasses,
          NeuralNetwork.Runtime... order) {
    SnpeImageClassifier c = new SnpeImageClassifier();
    c.inputName = inputName;
    c.outputName = outputName;
    c.version = SNPE.getRuntimeVersion(application);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(assetManager.open(labelFilename)));
      String line;
      while ((line = br.readLine()) != null) {
        c.labels.add(line);
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException("Problem reading label file!" , e);
    }

    final File file = new File(application.getCacheDir(), modelFilename);
    FileOutputStream fileOutputStream = null;
    if (!file.exists()) {
      InputStream model = null;
      try {
        model = assetManager.open(modelFilename);
        fileOutputStream = new FileOutputStream(file);
        byte[] chunk = new byte[100 * 1024];
        int read;
        while ((read = model.read(chunk)) != -1) {
          fileOutputStream.write(chunk, 0, read);
        }
      } catch (IOException e) {
        throw new RuntimeException("Problem reading model file!" , e);
      } finally {
        if (fileOutputStream != null) {
          try {
            fileOutputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (model != null) {
          try {
            model.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    try {
      c.inferenceInterface = new SNPE.NeuralNetworkBuilder(application)
              .setPerformanceProfile(NeuralNetwork.PerformanceProfile.HIGH_PERFORMANCE)
              .setRuntimeOrder(order)
              .setModel(file)
              .build();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
    // the placeholder node for input in the graphdef typically used does not specify a shape, so it
    // must be passed in as a parameter.
    c.inputSize = inputSize;
    c.imageMean = imageMean;
    c.imageStd = imageStd;

    // Pre-allocate buffers.
    c.intValues = new int[inputSize * inputSize];
    c.outputs = new float[numClasses];
    c.inputTensor = c.inferenceInterface.createFloatTensor(inputSize, inputSize, 3);

    return c;
  }

  @Override
  public InferenceResult recognizeImage(final Bitmap bitmap) {
    // Log this method so that it can be analyzed with systrace.
    InferenceResult result = new InferenceResult();
    Trace.beginSection("recognizeImage");

    Trace.beginSection("preprocessBitmap");
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    final float[] floatValues = new float[3];
    for (int y = 0; y < inputSize; y++) {
      for (int x = 0; x < inputSize; x++) {
        final int val = intValues[y * inputSize + x];
        floatValues[0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
        floatValues[1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
        floatValues[2] = ((val & 0xFF) - imageMean) / imageStd;
        inputTensor.write(floatValues, 0, floatValues.length, y, x);
      }
    }
    Trace.endSection();
    final long startTime = SystemClock.uptimeMillis();
    // Run the inference call.
    Trace.beginSection("run");
    final Map<String, FloatTensor> map = new HashMap<>();
    map.put(inputName, inputTensor);
    final Map<String, FloatTensor> output = inferenceInterface.execute(map);
    result.time = SystemClock.uptimeMillis() - startTime;
    Trace.endSection();
    output.get(outputName).read(this.outputs, 0, this.outputs.length);

    // Find the best classifications.
    PriorityQueue<Recognition> pq =
        new PriorityQueue<Recognition>(
            3,
            new Comparator<Recognition>() {
              @Override
              public int compare(Recognition lhs, Recognition rhs) {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
              }
            });
    for (int i = 0; i < outputs.length; ++i) {
      if (outputs[i] > THRESHOLD) {
        pq.add(
            new Recognition(
                "" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i], null));
      }
    }
    final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
    int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
    for (int i = 0; i < recognitionsSize; ++i) {
      recognitions.add(pq.poll());
    }
    Trace.endSection(); // "recognizeImage"
    result.result = recognitions;
    return result;
  }

  @Override
  public void enableStatLogging(boolean logStats) {
    this.logStats = logStats;
  }

  @Override
  public String getStatString() {
    return String.format("SNPE Version: %s, Runtime: %s", version,
            this.inferenceInterface.getRuntime().name());
  }

  @Override
  public void close() {
    inferenceInterface.release();
  }
}
