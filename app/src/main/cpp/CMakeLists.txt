cmake_minimum_required(VERSION 3.22.1)
project("jarvis")

add_library(jarvis_llm SHARED jarvis_llm.cpp)

target_include_directories(jarvis_llm PRIVATE ${CMAKE_SOURCE_DIR})

target_link_libraries(jarvis_llm
        android
        log
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-base.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-cpu.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama-common.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmtmd.so)