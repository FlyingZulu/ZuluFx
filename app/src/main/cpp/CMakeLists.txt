cmake_minimum_required(VERSION 3.18)
project(zulu CXX)
set(CMAKE_CXX_STANDARD 17)

find_package(oboe REQUIRED CONFIG)

add_library(zulu SHARED jni_bridge.cpp audio_engine.cpp)
target_include_directories(zulu PRIVATE dsp utils)
target_link_libraries(zulu oboe::oboe log)
target_compile_options(zulu PRIVATE -O2)
