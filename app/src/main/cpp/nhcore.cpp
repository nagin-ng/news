// NH Chat core: developer details live only here, scrambled, and are checked on every read.
#include <jni.h>
#include <stdint.h>

namespace {

const uint8_t F0[] = {0xD3, 0xC1};
const uint8_t F1[] = {0xD8, 0x02, 0xC5, 0x2F, 0xEE, 0x33, 0xB4, 0x29, 0x39, 0xA5, 0x46, 0x5A, 0x84, 0xB0, 0x97, 0x81, 0x5A, 0x76, 0xBE, 0xD4, 0x7C, 0x7F, 0x5F, 0x9D};
const uint8_t F2[] = {0x75, 0xEB, 0xC4, 0x85, 0xDF, 0x38, 0x3D, 0xAA, 0x80, 0x6E, 0xB5, 0x13, 0xFC, 0xFA, 0x7F, 0x0F, 0x27, 0xB6, 0xC0, 0xF9, 0xD2, 0xDE, 0x37};
const uint8_t F3[] = {0xE8, 0xDF, 0x1B, 0x88, 0x03, 0x7B, 0xA2, 0x17, 0xA6};
const uint8_t F4[] = {0x7F, 0x68, 0xB7, 0xB3, 0x85, 0x2F, 0x16, 0x7A, 0x26, 0x2A, 0xF9, 0xB6, 0xEA, 0x79, 0x35, 0xDF};
const uint8_t F5[] = {0xC0, 0xC9, 0x7D, 0x33, 0x0B, 0xF4, 0x44, 0x8C, 0x23, 0x38, 0x5C, 0x03, 0x96, 0x77, 0x46, 0x04, 0xE8, 0xA2, 0x35, 0xBD, 0x8D, 0x4A, 0xCA, 0x79};

struct Field {
    const uint8_t* d;
    int n;
    uint32_t chk;
};

const Field kFields[] = {
    {F0, 2, 0x9925622Cu},
    {F1, 24, 0xC88FED81u},
    {F2, 23, 0x0CFC4C0Du},
    {F3, 9, 0x92603CA3u},
    {F4, 16, 0x6B749CA1u},
    {F5, 24, 0xF34B690Au}
};
const int kCount = 6;

const uint64_t kImg = 0xCAAEB52732037541ULL;

// key bytes are rebuilt at run time from small constants
inline uint8_t kb(int i) {
    const uint32_t a = 0x9E3779B1u, b = 0x85EBCA6Bu, c = 0xC2B2AE35u, d = 0x27D4EB2Fu;
    uint32_t x = (a * (uint32_t)(i + 1)) ^ (b >> (i & 7)) ^ (c << ((i * 3) & 7)) ^ (d * (uint32_t)(i + 7));
    x ^= x >> 15;
    x *= 0x2C1B3C6Du;
    x ^= x >> 12;
    x *= 0x297A2D39u;
    x ^= x >> 15;
    return (uint8_t)(x & 0xFF);
}

bool decode(int id, char* out, int cap) {
    if (id < 0 || id >= kCount) return false;
    const Field& f = kFields[id];
    if (f.n + 1 > cap) return false;
    uint32_t h = 2166136261u;
    for (int j = 0; j < f.n; j++) {
        uint8_t ks = (uint8_t)(kb(j * 31 + id * 17 + 5) ^ (uint8_t)(id * 73 + j * 41 + 11));
        uint8_t p = (uint8_t)(f.d[j] ^ ks);
        out[j] = (char)p;
        h ^= p;
        h *= 16777619u;
    }
    out[f.n] = 0;
    if ((h ^ 0xA5C3E1F7u ^ ((uint32_t)id * 0x01000193u)) != f.chk) {
        out[0] = 0;
        return false;
    }
    return true;
}

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL Java_com_example_agylauncher_NhCore_get(JNIEnv* env, jclass, jint id) {
    char buf[96];
    if (!decode((int)id, buf, (int)sizeof(buf))) return env->NewStringUTF("");
    return env->NewStringUTF(buf);
}

JNIEXPORT jboolean JNICALL Java_com_example_agylauncher_NhCore_img(JNIEnv* env, jclass, jbyteArray arr) {
    if (arr == nullptr) return JNI_FALSE;
    jsize n = env->GetArrayLength(arr);
    jbyte* p = env->GetByteArrayElements(arr, nullptr);
    if (p == nullptr) return JNI_FALSE;
    uint64_t h = 1469598103934665603ULL;
    for (jsize i = 0; i < n; i++) {
        h ^= (uint8_t)p[i];
        h *= 1099511628211ULL;
    }
    env->ReleaseByteArrayElements(arr, p, JNI_ABORT);
    return ((h ^ 0x5DEECE66D1B4E3A7ULL) == kImg) ? JNI_TRUE : JNI_FALSE;
}

}  // extern "C"
