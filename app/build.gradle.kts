    signingConfigs {
        // 用项目内的调试签名，避免依赖 ~/.android 下的默认 keystore
        getByName("debug") {
            storeFile = rootProject.file(".android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
