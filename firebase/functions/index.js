// 글자국 — 카카오 로그인 서버 함수
// 앱이 보낸 카카오 토큰이 '글자국 앱'에서 받은 진짜 토큰인지 카카오에 확인하고,
// 맞으면 Firebase 로그인용 맞춤 토큰을 돌려줍니다.
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
initializeApp();

const KAKAO_APP_ID = 1628078; // 카카오 개발자 콘솔 > 글자국 앱 ID

exports.kakaoLogin = onCall({ region: 'asia-northeast3' }, async (req) => {
  const token = req.data && req.data.accessToken;
  if (!token || typeof token !== 'string') throw new HttpsError('invalid-argument', '카카오 토큰이 없어요');

  const res = await fetch('https://kapi.kakao.com/v1/user/access_token_info', {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!res.ok) throw new HttpsError('unauthenticated', '카카오 토큰이 유효하지 않아요');

  const { id, app_id } = await res.json();
  if (Number(app_id) !== KAKAO_APP_ID) throw new HttpsError('permission-denied', '다른 앱의 카카오 토큰이에요');

  const customToken = await getAuth().createCustomToken(`kakao:${id}`, { provider: 'kakao' });
  return { customToken };
});

// ---------------------------------------------------------------------------
// v11.111: 회원 탈퇴 — 서버에서 계정과 데이터를 한 번에 지움
// (앱에서 직접 지우면 '최근 로그인'이 필요해 오래 로그인해 둔 사람은 탈퇴가 막힘)
// ---------------------------------------------------------------------------
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const REGION = 'asia-northeast3';

exports.deleteAccount = onCall({ region: REGION }, async (req) => {
  const uid = req.auth && req.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', '로그인이 필요해요');
  const db = getFirestore();
  const w = db.bulkWriter();

  // 1) 내가 쓴 댓글 삭제 + 글귀별 댓글 수 줄이기
  const mine = await db.collection('comments').where('authorUid', '==', uid).get();
  mine.forEach(d => {
    w.delete(d.ref);
    const q = d.get('quoteId');
    if (q !== undefined && q !== null) w.set(db.doc(`quoteStats/${q}`), { count: FieldValue.increment(-1) }, { merge: true });
  });
  // 2) 다른 사람 댓글에 남은 내 공감·신고 표시 지우기
  const liked = await db.collection('comments').where('likedBy', 'array-contains', uid).get();
  liked.forEach(d => { if (d.get('authorUid') !== uid) w.update(d.ref, { likedBy: FieldValue.arrayRemove(uid), likeCount: FieldValue.increment(-1) }); });
  const reported = await db.collection('comments').where('reportedBy', 'array-contains', uid).get();
  reported.forEach(d => { if (d.get('authorUid') !== uid) w.update(d.ref, { reportedBy: FieldValue.arrayRemove(uid), reportCount: FieldValue.increment(-1) }); });
  // 2-1) v11.114: 자유게시판 — 내 글 삭제, 남의 글에 남은 내 공감·신고 지우기
  const myPosts = await db.collection('posts').where('authorUid', '==', uid).get();
  myPosts.forEach(d => w.delete(d.ref));
  const likedP = await db.collection('posts').where('likedBy', 'array-contains', uid).get();
  likedP.forEach(d => { if (d.get('authorUid') !== uid) w.update(d.ref, { likedBy: FieldValue.arrayRemove(uid), likeCount: FieldValue.increment(-1) }); });
  const repP = await db.collection('posts').where('reportedBy', 'array-contains', uid).get();
  repP.forEach(d => { if (d.get('authorUid') !== uid) w.update(d.ref, { reportedBy: FieldValue.arrayRemove(uid), reportCount: FieldValue.increment(-1) }); });
  // 3) 내가 올린 신고 기록
  const reps = await db.collection('reports').where('uid', '==', uid).get();
  reps.forEach(d => w.delete(d.ref));
  // 4) 프로필·닉네임(다시 쓸 수 있게)·알림 주소
  const prof = await db.doc(`users/${uid}`).get();
  const nick = prof.exists ? String(prof.get('nickname') || '').trim().toLowerCase() : '';
  if (nick) {
    const n = await db.doc(`nicknames/${nick}`).get();
    if (n.exists && n.get('uid') === uid) w.delete(n.ref);
  }
  w.delete(db.doc(`users/${uid}`));
  w.delete(db.doc(`pushTokens/${uid}`));
  w.delete(db.doc(`rate/${uid}`));   // v11.122: 글쓰기 간격 기록
  w.delete(db.doc(`postDay/${uid}`));   // v11.125: 나만의 명언 하루 한 번 기록
  await w.close();

  // 5) 로그인 계정 삭제
  try { await getAuth().deleteUser(uid); } catch (e) { if (e.code !== 'auth/user-not-found') throw new HttpsError('internal', '계정을 지우지 못했어요'); }
  return { ok: true };
});

// ---------------------------------------------------------------------------
// v11.111: 댓글 알림 — 내 댓글에 답글이 달리거나 공감을 받으면 푸시 알림
// ---------------------------------------------------------------------------
const cut = (t, n) => { t = String(t || '').replace(/\s+/g, ' ').trim(); return t.length > n ? t.slice(0, n) + '…' : t; };

async function pushTo(uid, title, body, data) {
  if (!uid) return;
  const db = getFirestore();
  const ref = db.doc(`pushTokens/${uid}`);
  const snap = await ref.get();
  if (!snap.exists || snap.get('on') === false || !snap.get('token')) return;
  try {
    await getMessaging().send({
      token: snap.get('token'),
      notification: { title, body },
      data,
      android: { priority: 'high', notification: { channelId: 'comment_alerts', sound: 'geuljaguk_chime', icon: 'ic_stat_geuljaguk', color: '#2e4a3b', tag: `c-${data.cid}` } }
    });
  } catch (e) {
    // 앱을 지웠거나 주소가 바뀐 기기 → 주소 정리
    if (['messaging/registration-token-not-registered', 'messaging/invalid-registration-token', 'messaging/invalid-argument'].includes(e.code)) await ref.delete().catch(() => {});
    else console.warn('push failed', e.code || e.message);
  }
}

/* v11.114: Firestore 트리거(notifyReply·notifyLike) 대신, 앱이 답글·공감 직후 직접 부르는 함수로 바꿈
   (데이터베이스 위치와 상관없이 서울 리전에서 동작, 배포 실패 위험 없음)
   kind: 'comment' — 방금 내가 쓴 댓글/답글 (답글이면 원 댓글 주인, 게시판 글의 댓글이면 글쓴이에게)
         'like'    — 내가 공감한 댓글 (댓글 주인에게)
         'postLike'— 내가 공감한 게시판 글 (글쓴이에게)
   같은 사람이 같은 글에 공감을 여러 번 눌러도 알림은 한 번만 (notifLog) */
exports.notify = onCall({ region: REGION }, async (req) => {
  const uid = req.auth && req.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', '로그인이 필요해요');
  const kind = String((req.data && req.data.kind) || ''), id = String((req.data && req.data.id) || '');
  if (!/^[A-Za-z0-9_-]{1,64}$/.test(id)) throw new HttpsError('invalid-argument', 'bad id');
  const db = getFirestore();
  const once = async (key) => {   // 처음 한 번만 true
    const ref = db.doc(`notifLog/${key}`);
    try { await ref.create({ at: FieldValue.serverTimestamp() }); return true; } catch (e) { return false; }
  };
  if (kind === 'comment') {
    const snap = await db.doc(`comments/${id}`).get();
    if (!snap.exists) return { ok: false };
    const c = snap.data();
    if (c.authorUid !== uid) throw new HttpsError('permission-denied', 'not yours');
    const age = Date.now() - (c.createdAt && c.createdAt.toMillis ? c.createdAt.toMillis() : 0);
    if (age > 5 * 60 * 1000) return { ok: false };   // 방금 쓴 것만
    if (!(await once(`c_${id}`))) return { ok: false };
    const qid = String(c.quoteId);
    if (c.parentId) {
      const parent = await db.doc(`comments/${c.parentId}`).get();
      if (!parent.exists) return { ok: false };
      const to = parent.get('authorUid');
      if (!to || to === uid) return { ok: false };
      await pushTo(to, '💬 내 댓글에 답글이 달렸어요', `${cut(c.authorName, 10)}: ${cut(c.text, 40)}`, { qid, cid: String(c.parentId), type: 'reply' });
    } else if (qid.startsWith('p_')) {
      const post = await db.doc(`posts/${qid.slice(2)}`).get();
      if (!post.exists) return { ok: false };
      const to = post.get('authorUid');
      if (!to || to === uid) return { ok: false };
      await pushTo(to, '💬 내 한 줄에 댓글이 달렸어요', `${cut(c.authorName, 10)}: ${cut(c.text, 40)}`, { qid, cid: id, type: 'postComment' });
    }
    return { ok: true };
  }
  if (kind === 'like' || kind === 'postLike') {
    const col = kind === 'like' ? 'comments' : 'posts';
    const snap = await db.doc(`${col}/${id}`).get();
    if (!snap.exists) return { ok: false };
    const d = snap.data();
    if (!(d.likedBy || []).includes(uid) || d.authorUid === uid) return { ok: false };
    if (!(await once(`${kind === 'like' ? 'l' : 'pl'}_${id}_${uid}`))) return { ok: false };
    if (kind === 'like') await pushTo(d.authorUid, '❤️ 내 댓글에 공감이 달렸어요', `"${cut(d.text, 30)}" · 공감 ${d.likeCount || 1}개`, { qid: String(d.quoteId), cid: id, type: 'like' });
    else await pushTo(d.authorUid, '❤️ 내 한 줄에 공감이 달렸어요', `"${cut(d.text, 30)}" · 공감 ${d.likeCount || 1}개`, { qid: `p_${id}`, cid: id, type: 'postLike' });
    return { ok: true };
  }
  throw new HttpsError('invalid-argument', 'bad kind');
});
