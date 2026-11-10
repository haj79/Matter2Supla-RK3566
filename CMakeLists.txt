cmake_minimum_required(VERSION 3.22)
project(matter_supla_rk3566 VERSION 0.0.1 LANGUAGES CXX)

add_executable(matter-supla src/main.cpp)
target_compile_features(matter-supla PRIVATE cxx_std_17)
target_compile_options(matter-supla PRIVATE -Wall -Wextra -Wpedantic)
