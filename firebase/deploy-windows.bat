@echo off
chcp 65001 > nul
echo.
echo ===== 글자국 Firebase 설정 올리기 (서버 함수 + 보안 규칙 + 공유 웹페이지) =====
echo.
echo [1/4] Firebase 도구 설치 중... (처음 한 번만 오래 걸려요)
call npm install -g firebase-tools
echo.
echo [2/4] Firebase 로그인 - 브라우저가 열리면 글자국 Firebase를 만든 구글 계정으로 로그인하고 '허용'을 누르세요.
call firebase login
echo.
echo [3/4] 함수 준비 중...
cd /d "%~dp0functions"
call npm install
cd /d "%~dp0"
echo.
echo [4/4] 서버에 올리는 중... (3~5분)
call firebase deploy --only functions,firestore:rules,hosting --force
echo.
echo 위에 "Deploy complete!" 가 보이면 성공이에요. 이 창을 캡처해서 Claude에게 보내주세요.
pause
