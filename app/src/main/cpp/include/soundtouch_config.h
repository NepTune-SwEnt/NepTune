//
// Created by Admin on 10/12/2025.
//

#ifndef NEPTUNE_SOUNDTOUCH_CONFIG_H
#define SOUNDTOUCH_CONFIG_H
#warning "SoundTouch config included!"

// Disable any x86-only optimizations (SSE, MMX)
#define SOUNDTOUCH_DISABLE_OPTIMIZATIONS 1
#define SOUNDTOUCH_DISABLE_X86_OPTIMIZATIONS 1

// Avoid exceptions (Android NDK prefers no exceptions in C++)
#define ST_NO_EXCEPTION_HANDLING 1

#define SOUNDTOUCH_DISABLE_NEON 1
#define SOUNDTOUCH_DISABLE_AVX 1
#define SOUNDTOUCH_DISABLE_FPU 1

#endif //NEPTUNE_SOUNDTOUCH_CONFIG_H
