cmake_minimum_required(VERSION 3.10.2)
project("nhcore")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections -Wl,--exclude-libs,ALL -s")

add_library(nhcore SHARED nhcore.cpp)

target_compile_options(nhcore PRIVATE
    -O2 -fvisibility=hidden -fno-exceptions -fno-rtti
    -ffunction-sections -fdata-sections)
