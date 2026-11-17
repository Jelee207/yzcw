# -*- coding: utf-8 -*-
"""
生成「苦情树 · 续缘相册」的图片占位插画。

数据来自 public/data/couples.json，输出 public/couples/<slug>.svg：
每人一张带有姓名与印章的古风「图位以待」图，作为相册的预留图片空间。
后台管理系统把对应记录的 image 改成真实图片地址后，前端会自动换图，
这些 SVG 依旧保留，作为图片加载失败时的兜底。

用法： python tools/gen_couple_svg.py
"""
import html
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "public" / "data" / "couples.json"
OUT = ROOT / "public" / "couples"
OUT.mkdir(parents=True, exist_ok=True)

# 每对情侣一点不同的色调，避免相册里十张占位图长得一模一样
PALETTE = [
    ("#b03a3a", "#e8b9b0"), ("#a8534a", "#f0c9b2"), ("#9a6b4f", "#e8c9a8"),
    ("#7d8a6a", "#cdd8b8"), ("#8e2c2c", "#e3aaa2"), ("#a26a86", "#ecc4d4"),
    ("#9c6b3f", "#f0d2a4"), ("#6f7f7a", "#c3d3cc"), ("#8a6a2c", "#e8d3a0"),
    ("#a4505e", "#ecc0c8"),
]


def esc(text):
    return html.escape(str(text), quote=True)


def frame(accent, soft):
    """画框、光晕、树影、花瓣等共用底图"""
    return f"""
<defs>
  <linearGradient id="paper" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#fcf7ea"/><stop offset=".55" stop-color="#f4e9d2"/><stop offset="1" stop-color="#e8d6b6"/>
  </linearGradient>
  <radialGradient id="halo"><stop offset="0" stop-color="{accent}" stop-opacity=".26"/>
    <stop offset=".7" stop-color="{accent}" stop-opacity=".07"/><stop offset="1" stop-color="{accent}" stop-opacity="0"/></radialGradient>
  <linearGradient id="gold" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#f7e8bb"/>
    <stop offset=".5" stop-color="#c9a24a"/><stop offset="1" stop-color="#8a6a2c"/></linearGradient>
</defs>
<rect width="480" height="360" fill="url(#paper)"/>
<rect x="12" y="12" width="456" height="336" rx="10" fill="none" stroke="#b8924a" stroke-opacity=".55" stroke-width="1.5"/>
<rect x="18" y="18" width="444" height="324" rx="8" fill="none" stroke="#b8924a" stroke-opacity=".28" stroke-width="1" stroke-dasharray="4 7"/>
<circle cx="240" cy="150" r="118" fill="url(#halo)"/>
<path d="M20 250 Q90 224 150 250 T280 250 T460 250" stroke="{accent}" stroke-opacity=".13" stroke-width="2" fill="none"/>
<path d="M20 262 Q90 236 150 262 T280 262 T460 262" stroke="{accent}" stroke-opacity=".08" stroke-width="1.5" fill="none"/>
<g fill="{accent}" opacity=".2"><circle cx="60" cy="70" r="3"/><circle cx="420" cy="64" r="2.5"/>
  <circle cx="404" cy="220" r="3"/><circle cx="76" cy="206" r="2.5"/><circle cx="112" cy="46" r="1.8"/></g>
<path d="M-10 44 Q60 24 108 48 M490 44 Q420 24 372 48" stroke="#7a5a38" stroke-width="6" fill="none" opacity=".28" stroke-linecap="round"/>
<g fill="{soft}" opacity=".8"><circle cx="76" cy="40" r="6"/><circle cx="102" cy="52" r="4"/>
  <circle cx="402" cy="40" r="6"/><circle cx="378" cy="52" r="4"/><circle cx="240" cy="34" r="3"/></g>
"""


def figures(accent, soft):
    """双人剪影 + 一缕姻缘红线"""
    return f"""
<g fill="{accent}" opacity=".5">
  <circle cx="181" cy="150" r="21"/>
  <circle cx="181" cy="125" r="8"/>
  <path d="M156 176 Q181 168 206 176 L220 248 L142 248 Z"/>
  <path d="M156 180 L132 218 L146 226 L166 196 Z"/>
  <path d="M206 180 L230 218 L216 226 L196 196 Z"/>
</g>
<g fill="{soft}" stroke="{accent}" stroke-opacity=".35" stroke-width="1.5">
  <path d="M275 128 Q299 112 323 128 L330 190 L268 190 Z" opacity=".8"/>
  <circle cx="299" cy="150" r="21"/>
  <path d="M276 176 Q299 168 322 176 L336 248 L258 248 Z"/>
  <path d="M276 180 L252 218 L266 226 L286 196 Z"/>
  <path d="M322 180 L346 218 L332 226 L312 196 Z"/>
</g>
<g stroke="#b03a3a" fill="none" stroke-linecap="round" opacity=".7">
  <path d="M206 210 Q240 244 274 210" stroke-width="2"/>
  <path d="M212 220 Q240 252 268 220" stroke-width="1.2" opacity=".6"/>
</g>
<circle cx="240" cy="231" r="5" fill="none" stroke="#b03a3a" stroke-width="2" opacity=".8"/>
<g fill="#fff" opacity=".75"><circle cx="128" cy="78" r="3"/><circle cx="352" cy="72" r="3"/>
  <circle cx="338" cy="56" r="2"/><circle cx="150" cy="60" r="2"/></g>
"""


def make_svg(name, mark, idx, generic=False):
    accent, soft = PALETTE[idx % len(PALETTE)]
    title = "此 间 留 白" if generic else esc(name)
    sub = "影 像 待 补 · 后 台 可 换" if not generic else "图 片 加 载 未 果 · 请 后 台 更 换"
    seal = "缘" if generic else esc(mark)
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 360" width="480" height="360" role="img" aria-label="{title} · 图位以待">
{frame(accent, soft)}
{figures(accent, soft)}
<text x="240" y="300" text-anchor="middle" font-family="STKaiti,KaiTi,STSong,serif" font-size="{'30' if generic else '25'}" letter-spacing="3" fill="#8e2c2c">{title}</text>
<text x="240" y="327" text-anchor="middle" font-family="STKaiti,KaiTi,STSong,serif" font-size="13" letter-spacing="3" fill="#8e2c2c" opacity=".68">{sub}</text>
<rect x="410" y="26" width="40" height="40" rx="4" fill="#b03a3a"/>
<text x="430" y="54" text-anchor="middle" font-family="STKaiti,KaiTi,STSong,serif" font-size="23" fill="#f5efe3">{seal}</text>
</svg>
"""


def main():
    couples = json.loads(DATA.read_text(encoding="utf-8"))
    for idx, c in enumerate(couples):
        (OUT / f"{c['slug']}.svg").write_text(
            make_svg(c["name"], c["mark"], idx), encoding="utf-8")
    (OUT / "placeholder.svg").write_text(
        make_svg("", "", 0, generic=True), encoding="utf-8")
    print(f"generated {len(couples) + 1} svg files in {OUT}")


if __name__ == "__main__":
    main()
