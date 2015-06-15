#include <jni.h>
#include "com_choochootrain_refocusing_activity_MainActivity.h"

JNIEXPORT jstring JNICALL Java_com_choochootrain_refocusing_activity_MainActivity_hello
  (JNIEnv * env, jobject obj){
    return (*env)->NewStringUTF(env, "Hello from JNI");
  }