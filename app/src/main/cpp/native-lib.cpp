#include <jni.h>
#include <vector>
#include <android/log.h>
#include "soundtouch/SoundTouch.h"

#define LOG_TAG "NativeSoundTouch"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace soundtouch;


extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_neptune_neptune_ui_sampler_NativeAudioProcessor_pitchShiftNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray inputSamples,
        jint semitones) {

    jsize arrayLength = env->GetArrayLength(inputSamples);
    jfloat* samplesPtr = env->GetFloatArrayElements(inputSamples, NULL);

    if (samplesPtr == nullptr) {
        return nullptr;
    }

    const int sampleRate = 44100;
    const int numChannels = 1;
    const int sampleSegment = 1024;

    SoundTouch soundTouch;

    soundTouch.setSampleRate(sampleRate);
    soundTouch.setChannels(numChannels);

    soundTouch.setPitchSemiTones(semitones);


    soundTouch.setSetting(SETTING_USE_QUICKSEEK, 0);
    soundTouch.setSetting(SETTING_USE_AA_FILTER, 1);

    const float* pcmBuffer = (const float*)samplesPtr;

    soundTouch.putSamples(pcmBuffer, arrayLength);

    soundTouch.flush();

    std::vector<float> outputData;
    SAMPLETYPE tempBuffer[sampleSegment];

    uint nSamples;
    do {
        nSamples = soundTouch.receiveSamples(tempBuffer, sampleSegment);

        if (nSamples > 0) {
            outputData.insert(
                    outputData.end(),
                    tempBuffer,
                    tempBuffer + nSamples
            );
        }
    } while (nSamples > 0);

    env->ReleaseFloatArrayElements(inputSamples, samplesPtr, JNI_ABORT);

    jsize outputLength = (jsize)outputData.size();
    jfloatArray outputArray = env->NewFloatArray(outputLength);

    if (outputArray != nullptr) {
        env->SetFloatArrayRegion(outputArray, 0, outputLength, outputData.data());
    }

    return outputArray;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_neptune_neptune_ui_sampler_NativeAudioProcessor_timeStretchNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray inputSamples,
        jfloat tempoRatio) {

    jsize arrayLength = env->GetArrayLength(inputSamples);
    jfloat* samplesPtr = env->GetFloatArrayElements(inputSamples, nullptr);

    if (samplesPtr == nullptr) {
        return nullptr;
    }

    const int sampleRate = 44100;
    const int numChannels = 1;
    const int sampleSegment = 1024;

    SoundTouch soundTouch;

    soundTouch.setSampleRate(sampleRate);
    soundTouch.setChannels(numChannels);

    soundTouch.setTempo(tempoRatio);

    soundTouch.setSetting(SETTING_USE_QUICKSEEK, 0);
    soundTouch.setSetting(SETTING_USE_AA_FILTER, 1);

    const float* pcmBuffer = reinterpret_cast<const float*>(samplesPtr);
    soundTouch.putSamples(pcmBuffer, arrayLength);
    soundTouch.flush();

    std::vector<float> outputData;
    SAMPLETYPE tempBuffer[sampleSegment];

    uint nSamples;
    do {
        nSamples = soundTouch.receiveSamples(tempBuffer, sampleSegment);

        if (nSamples > 0) {
            outputData.insert(
                    outputData.end(),
                    tempBuffer,
                    tempBuffer + nSamples
            );
        }
    } while (nSamples > 0);

    env->ReleaseFloatArrayElements(inputSamples, samplesPtr, JNI_ABORT);

    jsize outputLength = static_cast<jsize>(outputData.size());
    jfloatArray outputArray = env->NewFloatArray(outputLength);

    if (outputArray != nullptr) {
        env->SetFloatArrayRegion(outputArray, 0, outputLength, outputData.data());
    }

    return outputArray;
}
