#!/usr/bin/env python3
"""글자국 앱 HTML 한 파일 → 앱용 www 폴더

사용법:  python3 tools/prepare_www.py [--admin] [src/geuljaguk.html]
  - 배경음악(<script id="bgmdata-*">)을 꺼내 www/bgm/*.mp3 로 저장하고 HTML에서는 뺌
  - assets/fonts, assets/firebase-bundle.js 를 www 로 복사
  - --admin 이면 운영자용(ADMIN_BUILD = true), 아니면 사용자용(false)
"""
import base64, os, re, shutil, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
args = [a for a in sys.argv[1:] if not a.startswith('--')]
admin = '--admin' in sys.argv
src = args[0] if args else os.path.join(ROOT, 'src', 'geuljaguk.html')
www = os.path.join(ROOT, 'www')

html = open(src, encoding='utf-8').read()
if os.path.isdir(www):
    shutil.rmtree(www)
os.makedirs(os.path.join(www, 'bgm'))

pat = re.compile(r'<script type="text/plain" id="bgmdata-([a-z]+)">([^<]*)</script>\n?')
for name, data in pat.findall(html):
    with open(os.path.join(www, 'bgm', f'{name}.mp3'), 'wb') as f:
        f.write(base64.b64decode(data))
html = pat.sub('', html)

flag = 'const ADMIN_BUILD = true;' if admin else 'const ADMIN_BUILD = false;'
html, n = re.subn(r'^const ADMIN_BUILD = (true|false);', flag, html, count=1, flags=re.M)
if n != 1:
    sys.exit('ADMIN_BUILD 줄을 찾지 못했어요')

with open(os.path.join(www, 'index.html'), 'w', encoding='utf-8') as f:
    f.write(html)
shutil.copytree(os.path.join(ROOT, 'assets', 'fonts'), os.path.join(www, 'fonts'))
shutil.copy(os.path.join(ROOT, 'assets', 'firebase-bundle.js'), www)
print(f"www 준비 완료 ({'운영자용' if admin else '사용자용'}) — 배경음악 {len(os.listdir(os.path.join(www, 'bgm')))}곡")
