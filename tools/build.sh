#!/usr/bin/env bash
# 사용자용·운영자용 APK를 한 번에 만들기
#   bash tools/build.sh            → out/글자국-사용자용.apk, out/글자국-운영자용.apk (테스트용 디버그 빌드)
#   bash tools/build.sh release    → out/글자국-출시용.aab (서명 필요: android/keystore.properties)
set -euo pipefail
# 작업 공간에서 gradlew 다운로드가 막히면: GRADLE=gradle bash tools/build.sh
cd "$(dirname "$0")/.."
mkdir -p out
export NO_PROXY="${NO_PROXY:-localhost,127.0.0.1}"
if [ "${1:-}" = "release" ]; then
  python3 tools/prepare_www.py
  npx cap sync android
  (cd android && ${GRADLE:-./gradlew} bundleRelease --max-workers=2 -q)
  cp android/app/build/outputs/bundle/release/app-release.aab "out/글자국-출시용.aab"
  echo "완료: out/글자국-출시용.aab"; exit 0
fi
for kind in admin user; do
  if [ "$kind" = admin ]; then python3 tools/prepare_www.py --admin; name=운영자용; else python3 tools/prepare_www.py; name=사용자용; fi
  npx cap sync android >/dev/null
  (cd android && ${GRADLE:-./gradlew} assembleDebug --max-workers=2 -q)
  cp android/app/build/outputs/apk/debug/app-debug.apk "out/글자국-$name.apk"
  echo "완료: out/글자국-$name.apk"
done
