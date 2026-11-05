#!/bin/bash
# ONE TIME 构建脚本
# 用法：关掉浏览器/微信等大应用后，在「终端」里执行  ./build.sh
cd "$(dirname "$0")" || exit 1
export GRADLE_USER_HOME="$PWD/.gradle"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

# 1) 清掉上一次残留的守护进程（它们会占着几个 GB）
echo "清理残留 Gradle 进程…"
pkill -9 -f GradleDaemon 2>/dev/null
sleep 2

# 2) 内存检查
FREE=$(( $(vm_stat | awk '/Pages free/{gsub(/\./,"");print $3}') * 16384 / 1048576 ))
INAC=$(( $(vm_stat | awk '/Pages inactive/{gsub(/\./,"");print $3}') * 16384 / 1048576 ))
echo "空闲 ${FREE}MB，可回收 ${INAC}MB（编译需要约 3GB）"
if [ "$FREE" -lt 1500 ]; then
  echo "⚠️  空闲内存偏低，建议先关掉浏览器/微信等应用再运行"
fi

# 3) 构建（单进程、不开守护，避免多个 JVM 抢内存）
START=$(date +%s)
"./.toolchain/gradle-8.13/bin/gradle" :app:assembleDebug --no-daemon
CODE=$?
APK=app/build/outputs/apk/debug/app-debug.apk
FRESH=0
if [ -f "$APK" ] && [ "$(stat -f %m "$APK")" -ge "$START" ]; then FRESH=1; fi

if [ "$CODE" -eq 0 ] && [ "$FRESH" -eq 1 ]; then
  cp app/build/outputs/apk/debug/app-debug.apk dist/ONE-TIME-v1.0.apk
  echo "✅ 打包完成：dist/ONE-TIME-v1.0.apk"
else
  echo "❌ 构建失败，看上方日志；若又是 OutOfMemory，请重启 Mac 后重试"
fi
