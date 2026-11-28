# 글자국 — 작업 메모 (Claude용)

- 앱 본체는 `src/geuljaguk.html` 한 파일. 기능 수정은 이 파일에서 하고, 버전 주석은 `v11.NNN:` 형식으로 남긴다 (현재 v11.150).
- **APK·HTML 파일은 사용자가 요청할 때만 만들어 보낸다**(2026-11-28 사용자 지시). 평소 수정은 원본 고치기 → 점검 → 미리보기 업데이트 → git 커밋까지만. versionCode는 APK를 만들 때만 올린다.
- APK를 만들 때는 사용자용·운영자용 두 가지를 **항상 같이** 만든다. 차이는 `const ADMIN_BUILD = true/false;` 한 줄뿐이며 `tools/prepare_www.py --admin` 이 바꿔 준다. 원본 HTML은 `false`(사용자용)로 둔다.
- 배경음악 기능은 v11.123에서 제거했다(음악 탭·설정 줄·이용 팁·음악 파일 삭제, `bgmOn`은 항상 false). 되살리려면 v11.122의 git 기록에서 `bgmdata-*` 블록을 가져온다.
- 빌드: `npm ci`(postinstall로 patch-package가 카카오 플러그인의 옛 jcenter 저장소를 고침) → `bash tools/build.sh` → `out/글자국-사용자용.apk`, `out/글자국-운영자용.apk`. Android SDK·JDK 21 필요. Claude 작업 공간에서는 gradlew 다운로드가 막혀 `GRADLE=gradle bash tools/build.sh`, Maven Central 429가 나면 몇 번 다시 시도.
- 점검: `python3 tools/smoke_test.py` (브라우저 미리보기로 주요 화면 열어 보기).
- 안드로이드 고유 코드: `android/app/src/main/java/com/geuljaguk/app/` (MainActivity: 카카오 초기화·위젯 새로고침, widget/: 위젯 4종).
- 위젯 데이터: `android/app/src/main/res/raw/widget_data.json` (시간대별 한 줄 + 글귀). 명언을 바꾸면 이 파일도 같이 갱신해야 한다.
- 정확한 알람 권한(SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM)은 매니페스트에서 제거 유지 (플레이 정책).
- 서버 쪽(보안 규칙·함수·웹)을 바꾸면 사용자에게 **Firebase 재배포가 필요하다고 꼭 알린다**.
- `firebase/public/`은 공유 링크 웹페이지 결과물(웹 앱, 글귀별 q/번호, og 그림, assetlinks). 명언이 바뀌면 `python3 tools/build_web.py`로 다시 만든 뒤 Firebase 배포(그림까지 10분 넘게 걸림). 명언 문장은 그대로이고 화면·안내 글만 바뀌면 `python3 tools/build_web.py --app-only`(웹 앱만). (2026-11-12 복원본 스크립트 — 그림 모양이 예전과 거의 같지만 완전히 같진 않음)
- 2026-11-12 작업 공간 초기화 후 test48 APK와 v11.119 HTML에서 복원함. 위젯 자바 코드는 APK에서 되살려 다시 정리한 것.
- 사용자는 한국어로 소통, 5060 대상 앱. 맞춤법·띄어쓰기는 표준어 기준.
- Firebase 코드 묶음 `assets/firebase-bundle.js`는 `node tools/firebase-bundle/build.mjs`로 다시 만든다(firebase 11.10.0 + app-check 포함).
- App Check(Play Integrity)는 앱에 들어가 있지만 Firebase 콘솔에서 '적용'은 꺼 둔다. 켜면 스토어 설치본이 아닌 APK(테스트용·GitHub 빌드)는 서버 접속이 막힌다.
- `firebase/deploy-windows.bat`는 반드시 CRLF 줄바꿈 유지(.gitattributes). 파이썬 등으로 고칠 때 LF로 바뀌면 윈도우에서 명령이 깨진다. zip을 보내기 전 `file`로 'CRLF line terminators' 확인.

## 작업 방식 (2026-11-28 사용자 지시 — 속도)
- APK·HTML·Firebase zip은 **요청할 때만** 만든다. 서버·웹이 바뀌면 "재배포 필요"만 알리고, zip은 재배포 직전 요청 시 한 번.
- 점검: 문구·색만 바꾼 경우 `python3 tools/smoke_test.py`만. 기능·화면 구조를 바꾼 경우에만 전체 기능 점검(Playwright 시나리오).
- 미리보기 댓글이 연달아 오면 모아서 한 번에 고치고, 버전도 한 번만 올린다.
- 버전별 상세 기록은 `docs/CHANGELOG.md`에 남긴다(이 파일엔 원칙만).
- 지금 꼭 지킬 문구 원칙: 설정이 바뀌는 안내는 '…되었습니다/했습니다', 그 밖은 원래 말투. 게시판 이름은 '명언 제작소'(코드 이름 board·posts).
- `out/`에는 최근 APK만 남긴다(예전 test APK는 지움).

