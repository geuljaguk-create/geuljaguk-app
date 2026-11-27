#!/bin/bash
cd "$(dirname "$0")"
echo "===== 글자국 Firebase 설정 올리기 (서버 함수 + 보안 규칙 + 공유 웹페이지) ====="
echo "[1/4] Firebase 도구 설치 (암호를 물으면 맥 로그인 암호 입력)"
sudo npm install -g firebase-tools
echo "[2/4] Firebase 로그인 - 브라우저에서 글자국 Firebase 구글 계정으로 로그인"
firebase login
echo "[3/4] 함수 준비"
(cd functions && npm install)
echo "[4/4] 서버에 올리는 중 (3~5분)"
firebase deploy --only functions,firestore:rules,firestore:indexes,hosting --force
echo "\"Deploy complete!\" 가 보이면 성공이에요. 이 창을 캡처해서 Claude에게 보내주세요."
read -p "엔터를 누르면 닫혀요"
