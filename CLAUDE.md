# 글자국 — 작업 메모 (Claude용)

- 앱 본체는 `src/geuljaguk.html` 한 파일. 기능 수정은 이 파일에서 하고, 버전 주석은 `v11.NNN:` 형식으로 남긴다 (현재 v11.126).
- 사용자용·운영자용 두 가지를 **항상 같이** 만든다. 차이는 `const ADMIN_BUILD = true/false;` 한 줄뿐이며 `tools/prepare_www.py --admin` 이 바꿔 준다. 원본 HTML은 `false`(사용자용)로 둔다.
- 배경음악 기능은 v11.123에서 제거했다(음악 탭·설정 줄·이용 팁·음악 파일 삭제, `bgmOn`은 항상 false). 되살리려면 v11.122의 git 기록에서 `bgmdata-*` 블록을 가져온다.
- 빌드: `npm ci`(postinstall로 patch-package가 카카오 플러그인의 옛 jcenter 저장소를 고침) → `bash tools/build.sh` → `out/글자국-사용자용.apk`, `out/글자국-운영자용.apk`. Android SDK·JDK 21 필요. Claude 작업 공간에서는 gradlew 다운로드가 막혀 `GRADLE=gradle bash tools/build.sh`, Maven Central 429가 나면 몇 번 다시 시도.
- 점검: `python3 tools/smoke_test.py` (브라우저 미리보기로 주요 화면 열어 보기).
- 안드로이드 고유 코드: `android/app/src/main/java/com/geuljaguk/app/` (MainActivity: 카카오 초기화·위젯 새로고침, widget/: 위젯 4종).
- 위젯 데이터: `android/app/src/main/res/raw/widget_data.json` (시간대별 한 줄 + 글귀). 명언을 바꾸면 이 파일도 같이 갱신해야 한다.
- 정확한 알람 권한(SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM)은 매니페스트에서 제거 유지 (플레이 정책).
- 서버 쪽(보안 규칙·함수·웹)을 바꾸면 사용자에게 **Firebase 재배포가 필요하다고 꼭 알린다**.
- `firebase/public/`은 공유 링크 웹페이지 결과물(웹 앱, 글귀별 q/번호, og 그림, assetlinks). 명언이 바뀌면 `python3 tools/build_web.py`로 다시 만든 뒤 Firebase 배포. (2026-11-12 복원본 스크립트 — 그림 모양이 예전과 거의 같지만 완전히 같진 않음)
- 2026-11-12 작업 공간 초기화 후 test48 APK와 v11.119 HTML에서 복원함. 위젯 자바 코드는 APK에서 되살려 다시 정리한 것.
- 사용자는 한국어로 소통, 5060 대상 앱. 맞춤법·띄어쓰기는 표준어 기준.
- Firebase 코드 묶음 `assets/firebase-bundle.js`는 `node tools/firebase-bundle/build.mjs`로 다시 만든다(firebase 11.10.0 + app-check 포함).
- v11.122 요금 줄이기: 댓글 30개씩·게시글 20개씩, 게시판 댓글 수는 보이는 글만 집계, 글쓰기는 `rate/{uid}`로 10초 간격(보안 규칙), 익명 계정은 글쓰기 불가. 댓글 최신순 조회에 복합 색인(`firebase/firestore.indexes.json`)이 필요하다(배포 스크립트가 함께 올림).
- App Check(Play Integrity)는 앱에 들어가 있지만 Firebase 콘솔에서 '적용'은 꺼 둔다. 켜면 스토어 설치본이 아닌 APK(테스트용·GitHub 빌드)는 서버 접속이 막힌다.
- v11.124: 홈의 '자유게시판'을 '나만의 명언'으로 바꿈(코드 이름은 board·posts 그대로). 글 = text(명언 200자)·by(누가 한 말 20자)·note(명언에 대한 이야기 300자), 보안 규칙 postExtrasOk가 길이 확인.
- v11.125: '누가 한 말' 칸 없앰(명언 아래에는 작성자 닉네임), 카드를 누르면 한 장씩 위로 넘기는 슬라이드 보기(openPostViewer), 나만의 명언은 한국 시간 하루 한 번(postDay/{uid}, 보안 규칙 daily()).
- v11.126: 슬라이드 보기에서 댓글을 열면 댓글 창이 아래 절반(.comment-sheet.half)에서 열리고 위쪽 명언 상자는 숨김, 명언은 위에 그대로 보임.
