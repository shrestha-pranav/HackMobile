## Description

This is a fork of the Tensorflow Android demo application ported to SNPE.

## Current samples:

1. [TF Classify](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/ClassifierActivity.java):
        Uses the [Google Inception](https://arxiv.org/abs/1409.4842)
        model to classify camera frames in real-time, displaying the top results
        in an overlay on the camera image.
2. [TF Detect](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/DetectorActivity.java):
        Demonstrates a model based on [Scalable Object Detection
        using Deep Neural Networks](https://arxiv.org/abs/1312.2249) to
        localize and track people in the camera preview in real-time.
3. [TF Stylize](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/StylizeActivity.java):
        Uses a model based on [A Learned Representation For Artistic Style]
        (https://arxiv.org/abs/1610.07629) to restyle the camera preview image
        to that of a number of different artists.