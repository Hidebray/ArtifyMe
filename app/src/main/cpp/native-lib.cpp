#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_sevengroup_artifyme_utils_Keys_getRemoveBgKey(
        JNIEnv* env,
        jobject /* this */) {

    std::string apiKey = "tDfveJahWbMcD9gNQnaNFg7f";

    return env->NewStringUTF(apiKey.c_str());
}