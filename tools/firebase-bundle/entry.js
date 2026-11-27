// 앱에 같이 넣는 Firebase 코드 (assets/firebase-bundle.js 원본)
// 다시 만들기: node tools/firebase-bundle/build.mjs
export * as appM from 'firebase/app';
export * as authM from 'firebase/auth';
export * as fsM from 'firebase/firestore';
export * as fnM from 'firebase/functions';
export * as acM from 'firebase/app-check';   // v11.122: App Check (글자국 앱에서 온 요청인지 확인)
