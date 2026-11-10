// Stage 0: isolated Android ARM64 executable / build-chain validation.
// No Matter SDK, SUPLA Cloud, Zigbee or UART integration yet.
#include <cstdio>
#include <cstring>

int main(int argc, char** argv) {
    if (argc > 1 && std::strcmp(argv[1], "--version") == 0) {
        std::puts("matter-supla bootstrap 0.0.1 @lukasz06");
        return 0;
    }
    std::puts("matter-supla: Stage 0 bootstrap (no Matter or SUPLA functionality yet)");
    std::puts("Android ARM64 executable built successfully.");
    return 0;
}
