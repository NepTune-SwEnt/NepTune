cmake_minimum_required(VERSION 3.22.1)

set(SOUNDTOUCH_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/soundtouch)

# ---------- DEFINITIONS ----------
add_compile_definitions(
        SOUNDTOUCH_DISABLE_X86_OPTIMIZATIONS=1
        SOUNDTOUCH_DISABLE_NEON=1
        SOUNDTOUCH_DISABLE_AVX=1
        SOUNDTOUCH_DISABLE_SSE=1
        SOUNDTOUCH_DISABLE_FPU=1
        ST_NO_EXCEPTION_HANDLING=1
)


# ---------- FLAGS ----------
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -O3")

# ---------- SOURCES ----------
set(SOUNDTOUCH_SOURCES
        ${SOUNDTOUCH_SRC_DIR}/AAFilter.cpp
        ${SOUNDTOUCH_SRC_DIR}/FIFOSampleBuffer.cpp
        ${SOUNDTOUCH_SRC_DIR}/PeakFinder.cpp
        ${SOUNDTOUCH_SRC_DIR}/RateTransposer.cpp
        ${SOUNDTOUCH_SRC_DIR}/SoundTouch.cpp
        ${SOUNDTOUCH_SRC_DIR}/TDStretch.cpp
        ${SOUNDTOUCH_SRC_DIR}/FIRFilter.cpp
        ${SOUNDTOUCH_SRC_DIR}/InterpolateCubic.cpp
        ${SOUNDTOUCH_SRC_DIR}/InterpolateLinear.cpp
        ${SOUNDTOUCH_SRC_DIR}/InterpolateShannon.cpp
        ${SOUNDTOUCH_SRC_DIR}/cpu_detect_stub.cpp
)

list(REMOVE_ITEM SOUNDTOUCH_SOURCES
        ${SOUNDTOUCH_SRC_DIR}/cpu_detect_x86.cpp
        ${SOUNDTOUCH_SRC_DIR}/cpu_detect_x86_64.cpp
        ${SOUNDTOUCH_SRC_DIR}/cpu_detect_arm.cpp
        ${SOUNDTOUCH_SRC_DIR}/cpu_detect_arm64.cpp
        ${SOUNDTOUCH_SRC_DIR}/mmx_optimized.cpp
        ${SOUNDTOUCH_SRC_DIR}/sse_optimized.cpp
)
# ---------- LIB STATIC ----------
add_library(
        soundtouch-static
        STATIC
        ${SOUNDTOUCH_SOURCES}
)

target_include_directories(
        soundtouch-static
        PRIVATE ${SOUNDTOUCH_SRC_DIR}
)

# ---------- JNI SHARED LIB ----------
add_library(
        sampler_jni
        SHARED
        native-lib.cpp
)

target_include_directories(
        sampler_jni
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
        PRIVATE ${SOUNDTOUCH_SRC_DIR}
)

target_link_libraries(
        sampler_jni
        soundtouch-static
        log
        m
)
