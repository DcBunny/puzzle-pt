#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

BUILD_TYPE="${1:-release}"

case "$BUILD_TYPE" in
  debug)
    GRADLE_TASK="assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    GRADLE_TASK="assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    UNSIGNED_APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  *)
    echo "用法: $0 [debug|release]" >&2
    exit 1
    ;;
esac

./gradlew "$GRADLE_TASK"

if [[ "$BUILD_TYPE" == "release" && ! -f "$APK_PATH" && -f "$UNSIGNED_APK_PATH" ]]; then
  APK_PATH="$UNSIGNED_APK_PATH"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "构建完成，但未找到 APK: $APK_PATH" >&2
  exit 1
fi

echo "APK 已生成: $ROOT_DIR/$APK_PATH"
