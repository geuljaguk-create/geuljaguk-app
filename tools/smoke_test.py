#!/usr/bin/env python3
"""글자국 기본 점검 — 브라우저 미리보기로 주요 화면을 차례로 열어 오류가 없는지 확인

사용법:  python3 tools/smoke_test.py [src/geuljaguk.html]
(필요: pip install playwright && playwright install chromium)
"""
import asyncio, os, sys
from playwright.async_api import async_playwright

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'src', 'geuljaguk.html')

STEPS = [
    ('홈 → 첫 글귀로 넘기기', "document.getElementById('appContainer').scrollTop = document.getElementById('appContainer').clientHeight"),
    ('읽기 설정 열기', "openReadSettings()"),
    ('읽기 설정 닫기', "closeSettingsPanel()"),
    ('댓글 창 열기', "openComments(baseQuotes[0].id)"),
    ('댓글 쓰기', "document.getElementById('commentInput').value='점검용 댓글'; submitComment()"),
    ('댓글 창 닫기', "closeComments()"),
    ('홈으로', "scrollToHome()"),
    ('오늘의 마음 열기', "openMoodCheck()"),
    ('오늘의 마음 고르기', "chooseMood(moodQ.opts[0].k)"),
    ('설정 열기', "openSettingsModal('main')"),
    ('알림 설정', "openSettingsModal('noti')"),
    ('설정 닫기', "closeSettingsPanel()"),
    ('자유게시판 열기', "openBoard()"),
    ('자유게시판 닫기', "closeBoard()"),
    ('검색 열기', "openSearchModal()"),
    ('보관함 열기', "closeModal(); openLibraryModal('likes')"),
]

async def main():
    errors = []
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await (await b.new_context(viewport={'width': 360, 'height': 740})).new_page()
        pg.on('pageerror', lambda e: errors.append(f'[페이지 오류] {e}'))
        await pg.route('**/images.unsplash.com/**', lambda r: r.abort())
        await pg.goto('file://' + os.path.abspath(SRC))
        await pg.evaluate("localStorage.clear(); localStorage.setItem('coachDone2','1'); localStorage.setItem('communityTermsAgreed','1'); localStorage.setItem('nickname','점검')")
        await pg.reload(); await pg.wait_for_timeout(2500)
        for name, js in STEPS:
            try:
                await pg.evaluate(f"(async () => {{ {js} }})()")
                await pg.wait_for_timeout(700)
                print(f'  ✓ {name}')
            except Exception as e:
                errors.append(f'[{name}] {str(e).splitlines()[0]}')
                print(f'  ✗ {name}')
        await pg.wait_for_timeout(2500)
        await b.close()
    print(f'\n오류 {len(errors)}개')
    for e in errors: print(' -', e)
    sys.exit(1 if errors else 0)

asyncio.run(main())
