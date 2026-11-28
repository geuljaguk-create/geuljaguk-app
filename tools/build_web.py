#!/usr/bin/env python3
"""공유 링크 웹페이지 만들기 → firebase/public

사용법:  python3 tools/build_web.py
  1) tools/prepare_www.py(사용자용)로 www 를 만든 뒤 웹 앱(index.html·글꼴·Firebase)을 public 에 복사
  2) 명언마다 q/<번호>/index.html (카카오톡 미리보기용 OG 태그 + 앱/웹으로 이동) 생성
  3) 명언마다 og/<번호>.png (1200×630 미리보기 그림) 생성
  4) firebase/pages (개인정보처리방침·계정 삭제 안내·기본 그림) 복사
  5) .well-known/assetlinks.json (앱 링크용 서명 지문)
"""
import html, io, json, os, re, shutil, subprocess, sys
from PIL import Image, ImageDraw, ImageFont
from fontTools.ttLib import TTFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'firebase', 'public')
BASE = 'https://geuljaguk-e0920.web.app/'
# 앱 링크 지문: 디버그 키, 업로드 키 (Play 앱 서명 키가 생기면 여기에 추가)
SHA256 = [
    '3B:55:83:D7:79:EB:45:E0:15:A5:C4:8D:DA:DA:BF:9E:71:CB:AD:2C:6E:E1:EB:C8:C0:88:B6:41:C5:20:AD:ED',
    'D7:E3:0B:A7:93:0C:AB:40:6F:72:1C:0D:BE:41:D2:8F:D4:23:51:7E:A5:2B:F6:63:3A:78:63:43:56:4B:AB:A7',
]

def quotes_from_html(h):
    a = h.index('const rawQuotesData = ['); b = h.index('];', a)
    out = []
    for line in h[a:b].split('\n')[1:]:
        line = line.strip().rstrip(',')
        if not line.startswith('{'):
            continue
        try:
            out.append(json.loads('{' + re.sub(r'(\w+): ', r'"\1": ', line[1:].strip()[:-1]) + '}'))
        except Exception:
            pass
    return out

def font(name, size, bold=False):
    p = os.path.join(ROOT, 'assets', 'fonts', name)
    f = TTFont(p); f.flavor = None; buf = io.BytesIO(); f.save(buf); buf.seek(0)
    ft = ImageFont.truetype(buf, size)
    if bold:
        try: ft.set_variation_by_axes([700])
        except Exception: pass
    return ft

def wrap(draw, text, fnt, maxw):
    lines = []
    for para in text.split('\n'):
        cur = ''
        for word in para.split(' '):
            t = (cur + ' ' + word).strip()
            if draw.textlength(t, font=fnt) <= maxw or not cur:
                cur = t
            else:
                lines.append(cur); cur = word
        lines.append(cur)
    return lines

def og_image(q, icon, path):
    W, H = 1200, 630
    im = Image.new('RGB', (W, H))
    px = im.load()
    for y in range(H):              # 옅은 크림 → 연둣빛 대각선 그라데이션
        for x in range(0, W):
            t = (x / W + y / H) / 2
            px[x, y] = (round(251 - 13 * t), round(247 - 5 * t), round(238 - 4 * t))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((28, 28, W - 28, H - 28), radius=30, outline=(214, 218, 210), width=2)
    text = q['text'].replace('\n', ' ')
    for size in (60, 54, 48, 42, 36, 32):
        fq = font('GowunBatang-Bold.woff2', size)
        lines = wrap(d, '“' + text + '”', fq, 720)
        if len(lines) <= 3:
            break
    lh = int(size * 1.55)
    top = 300 - (len(lines) * lh) // 2 - 20
    for i, ln in enumerate(lines):
        w = d.textlength(ln, font=fq)
        d.text(((W - w) / 2, top + i * lh), ln, font=fq, fill=(29, 42, 34))
    fa = font('PretendardVariable.woff2', 28, bold=True)
    au = '— ' + q['author']
    d.text(((W - d.textlength(au, font=fa)) / 2, top + len(lines) * lh + 22), au, font=fa, fill=(78, 110, 90))
    fb = font('PretendardVariable.woff2', 30, bold=True)
    bw = 46 + 12 + d.textlength('글자국', font=fb)
    x0 = (W - bw) / 2
    im.paste(icon, (int(x0), 532), icon)
    d.text((x0 + 58, 539), '글자국', font=fb, fill=(29, 42, 34))
    im.save(path, optimize=True)

ROOT_OG = """<meta property="og:type" content="website">
<meta property="og:site_name" content="글자국">
<meta property="og:title" content="글자국">
<meta property="og:description" content="매일 한 걸음, 마음에 새기는 문장">
<meta property="og:image" content="https://geuljaguk-e0920.web.app/og.png">
<meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:url" content="https://geuljaguk-e0920.web.app/">
<meta name="twitter:card" content="summary_large_image">
<meta name="description" content="매일 한 걸음, 마음에 새기는 문장">
"""

def copy_app(www):
    """웹 앱(index.html·Firebase 묶음·글꼴)을 public 에 복사. 첫 화면 링크 미리보기용 OG 태그를 <title> 뒤에 넣음"""
    os.makedirs(OUT, exist_ok=True)
    h = open(os.path.join(www, 'index.html'), encoding='utf-8').read()
    if 'og:title' not in h:
        h = h.replace('</title>\n', '</title>\n' + ROOT_OG, 1)
    open(os.path.join(OUT, 'index.html'), 'w', encoding='utf-8').write(h)
    shutil.copy(os.path.join(www, 'firebase-bundle.js'), OUT)
    if os.path.isdir(os.path.join(OUT, 'fonts')):
        shutil.rmtree(os.path.join(OUT, 'fonts'))
    shutil.copytree(os.path.join(www, 'fonts'), os.path.join(OUT, 'fonts'))

def main():
    subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'prepare_www.py')], check=True)
    www = os.path.join(ROOT, 'www')
    if '--app-only' in sys.argv:   # v11.131: 명언 문장은 그대로이고 앱 화면·안내 글만 바뀐 경우 — q/·og/ 그림은 두고 웹 앱만 바꿈 (그림 만들기는 10분 넘게 걸림)
        copy_app(www)
        print('웹 앱만 바꿈 (q/·og/ 그대로)')
        return
    pages_only = '--pages-only' in sys.argv   # v11.136: q/ 페이지(제목·설명)만 다시 쓰고 og/ 그림은 그대로 (그림 만들기는 10분 넘게 걸림)
    if pages_only:
        shutil.rmtree(os.path.join(OUT, 'q'), ignore_errors=True)
    else:
        if os.path.isdir(OUT):
            shutil.rmtree(OUT)
        os.makedirs(OUT)
    copy_app(www)
    for name in os.listdir(os.path.join(ROOT, 'firebase', 'pages')):
        s = os.path.join(ROOT, 'firebase', 'pages', name)
        d = os.path.join(OUT, name)
        if os.path.isdir(s): shutil.copytree(s, d, dirs_exist_ok=True)
        else: shutil.copy(s, d)
    os.makedirs(os.path.join(OUT, '.well-known'), exist_ok=True)
    with open(os.path.join(OUT, '.well-known', 'assetlinks.json'), 'w') as f:
        json.dump([{'relation': ['delegate_permission/common.handle_all_urls'],
                    'target': {'namespace': 'android_app', 'package_name': 'com.geuljaguk.app', 'sha256_cert_fingerprints': SHA256}}], f, indent=2)

    h = open(os.path.join(ROOT, 'src', 'geuljaguk.html'), encoding='utf-8').read()
    quotes = quotes_from_html(h)
    icon = Image.open(os.path.join(ROOT, 'android', 'app', 'src', 'main', 'res', 'mipmap-xxxhdpi', 'ic_launcher.png')).convert('RGBA').resize((46, 46), Image.LANCZOS)
    m = Image.new('L', (46, 46), 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, 45, 45), radius=11, fill=255); icon.putalpha(m)
    os.makedirs(os.path.join(OUT, 'og'), exist_ok=True); os.makedirs(os.path.join(OUT, 'q'), exist_ok=True)
    for q in quotes:
        i = q['id']; t = q['text'].replace('\n', ' ')
        title = '글자국'; desc = '친구가 보낸 오늘의 한 줄'   # v11.136: 카톡·문자 미리보기 제목은 '글자국', 설명에 작가 이름 넣지 않음 (명언은 그림에 있음)
        os.makedirs(os.path.join(OUT, 'q', str(i)))
        with open(os.path.join(OUT, 'q', str(i), 'index.html'), 'w', encoding='utf-8') as f:
            f.write(f'''<!doctype html><html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title>
<meta property="og:type" content="website">
<meta property="og:site_name" content="글자국">
<meta property="og:title" content="{title}">
<meta property="og:description" content="{desc}">
<meta property="og:image" content="{BASE}og/{i}.png">
<meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:url" content="{BASE}q/{i}/">
<meta name="twitter:card" content="summary_large_image">
<meta name="description" content="{desc}">
<meta http-equiv="refresh" content="0; url=/?s={i}">
<script>location.replace('/?s={i}' + (location.search ? '&' + location.search.slice(1) : ''));</script>
</head><body style="background:#f4efe4"></body></html>
''')
        if not pages_only:
            og_image(q, icon, os.path.join(OUT, 'og', f'{i}.png'))
    print(f'웹 준비 완료 — 명언 {len(quotes)}개 (q/ 페이지, og/ 그림)')

if __name__ == '__main__':
    main()
