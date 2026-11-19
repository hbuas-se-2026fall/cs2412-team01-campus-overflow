#!/usr/bin/env python3
"""把 CSV 画成燃尽图（PNG）。只依赖 Pillow。

CSV 格式：
    date,remaining,note
    2026-10-21,43,冲刺开始
    2026-10-22,19,

用法：
    python scripts/po/burndown.py docs/scrum/burndown/sprint-1.csv
    python scripts/po/burndown.py docs/scrum/burndown/sprint-1.csv -o out.png --title "Sprint 1 燃尽图"
"""
import argparse
import csv
from datetime import date
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\simhei.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
]


def font(size):
    for p in FONT_CANDIDATES:
        if Path(p).exists():
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                continue
    return ImageFont.load_default()


def load(path):
    rows = []
    with open(path, encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            if not (r.get("date") or "").strip():
                continue
            rem = (r.get("remaining") or "").strip()
            if rem == "":
                continue
            rows.append((date.fromisoformat(r["date"].strip()), float(rem)))
    rows.sort(key=lambda x: x[0])
    return rows


def draw(rows, out, title):
    W, H = 980, 520
    L, R, T, B = 90, 40, 70, 80
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)
    f_title, f_axis, f_small = font(26), font(16), font(14)

    pw, ph = W - L - R, H - T - B
    n = len(rows)
    total = max(v for _, v in rows) or 1
    ideal = [total * (1 - i / (n - 1)) if n > 1 else 0 for i in range(n)]

    def x(i):
        return L + (pw * i / (n - 1) if n > 1 else pw / 2)

    def y(v):
        return T + ph * (1 - v / total)

    # 网格
    steps = 5
    for k in range(steps + 1):
        val = total * k / steps
        yy = y(val)
        d.line([(L, yy), (L + pw, yy)], fill=(228, 232, 238), width=1)
        d.text((L - 12, yy), f"{val:.0f}", font=f_axis, fill=(90, 96, 105), anchor="rm")
    # 日期标签（最多 8 个）
    every = max(1, n // 8)
    for i in range(0, n, every):
        d.text((x(i), T + ph + 14), rows[i][0].strftime("%m-%d"), font=f_small,
               fill=(90, 96, 105), anchor="ma")

    # 坐标轴
    d.line([(L, T), (L, T + ph)], fill=(120, 126, 135), width=2)
    d.line([(L, T + ph), (L + pw, T + ph)], fill=(120, 126, 135), width=2)

    # 理想线（虚线）
    for i in range(n - 1):
        if i % 2 == 0:
            d.line([(x(i), y(ideal[i])), (x(i + 1), y(ideal[i + 1]))], fill=(160, 168, 180), width=2)

    # 实际线
    pts = [(x(i), y(v)) for i, (_, v) in enumerate(rows)]
    d.line(pts, fill=(220, 68, 68), width=3)
    for px, py in pts:
        d.ellipse([px - 4, py - 4, px + 4, py + 4], fill=(220, 68, 68))

    d.text((L, 26), title, font=f_title, fill=(32, 36, 44))
    d.text((L + pw - 10, T - 26), "剩余故事点 / 工作日", font=f_small, fill=(120, 126, 135), anchor="ra")

    legend_y = H - 34
    d.line([(L, legend_y), (L + 28, legend_y)], fill=(220, 68, 68), width=3)
    d.text((L + 36, legend_y - 9), "实际剩余", font=f_axis, fill=(50, 55, 62))
    d.line([(L + 140, legend_y), (L + 168, legend_y)], fill=(160, 168, 180), width=2)
    d.text((L + 176, legend_y - 9), "理想剩余（线性）", font=f_axis, fill=(50, 55, 62))
    d.text((W - R, legend_y - 9),
           f"{rows[0][0]:%m-%d} ~ {rows[-1][0]:%m-%d}｜完成 {total - rows[-1][1]:.0f}/{total:.0f} 点",
           font=f_small, fill=(120, 126, 135), anchor="ra")

    img.save(out)
    print(f"saved {out}  ({n} 个数据点, {total:.0f} 点)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("csv_path")
    ap.add_argument("-o", "--out")
    ap.add_argument("--title")
    a = ap.parse_args()

    csv_path = Path(a.csv_path)
    rows = load(csv_path)
    if len(rows) < 2:
        raise SystemExit(f"{csv_path}: 至少需要 2 行有 remaining 的数据才能画图")

    out = Path(a.out) if a.out else csv_path.with_name(f"burndown-{csv_path.stem}.png")
    title = a.title or f"燃尽图 · {csv_path.stem}"
    draw(rows, out, title)


if __name__ == "__main__":
    main()