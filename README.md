# 글자국

하루 한 줄, 마음에 새기는 문장 — 5060을 위한 명언 앱 (Android · iOS 예정)

## 폴더 구성

| 폴더·파일 | 내용 |
| --- | --- |
| `src/geuljaguk.html` | **앱 본체** — 화면·기능·명언이 모두 들어 있는 HTML 한 파일. 수정은 대부분 여기서 |
| `assets/` | 글꼴(fonts), Firebase 연결 파일(firebase-bundle.js) |
| `android/` | 안드로이드 앱 (홈 화면 위젯 4종, 알림, 카카오·구글 로그인 설정) |
| `firebase/` | 서버 — 보안 규칙, 서버 함수(알림·회원 탈퇴·카카오 로그인), 공유 링크 웹페이지 |
| `tools/prepare_www.py` | HTML 한 파일 → 앱용 `www` 폴더 (사용자용 / `--admin` 운영자용) |
| `tools/build.sh` | 사용자용·운영자용 APK를 한 번에 만들기 |
| `.github/workflows/build.yml` | GitHub 자동 빌드 |

## APK 받기 (GitHub)

1. 저장소 → **Actions** 탭
2. 맨 위(가장 최근) **앱 빌드** 항목 클릭
3. 아래 **Artifacts** 에서 `글자국-사용자용` 또는 `글자국-운영자용` 내려받기 → 압축 풀고 설치

직접 다시 만들려면 Actions → 앱 빌드 → **Run workflow**.

## 출시용(AAB) 만들기

저장소 → Settings → Secrets and variables → Actions 에 아래 4개를 한 번 등록한 뒤,
Actions → 앱 빌드 → Run workflow 에서 **출시용 AAB도 만들기**를 켜고 실행.

| 이름 | 값 |
| --- | --- |
| `GJ_KEYSTORE_BASE64` | 업로드 키 파일(geuljaguk-upload.jks)을 base64로 바꾼 글자 |
| `GJ_KEYSTORE_PASSWORD` | 키 비밀번호 |
| `GJ_KEY_ALIAS` | `geuljaguk` |
| `GJ_KEY_PASSWORD` | 키 비밀번호 (위와 같음) |

> 업로드 키 파일과 비밀번호는 **절대 저장소에 올리지 않습니다.**

## 서버(Firebase) 배포

`firebase/` 폴더에서 `deploy-windows.bat` (맥은 `deploy-mac.command`) 실행.
보안 규칙·서버 함수·웹페이지가 함께 배포됩니다.
