#!/usr/bin/env python3
"""校验 docs/错误码.md 与 backend 源码里的业务错误码是否一致。

规则（对应 docs/错误码.md 的「分段约定」）：
  * 源码里出现的码（字符串字面量）必须在文档里登记；
  * 文档里登记的码如果在源码里已不存在，允许出现在 STALE_ALLOW（文档中标注为「未采用」的码）。

退出码：0 = 一致；1 = 有未登记的码；2 = 运行环境问题（找不到文件）。

用法：
    python3 scripts/check-error-codes.py            # 在仓库根目录执行
    python3 scripts/check-error-codes.py --json     # 机器可读输出
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

CODE_RE = re.compile(r'"(?P<code>(?:ID|QA|REP|DIS|AUTH|SYS)-[0-9]{3,4}(?:-[A-Z]+)?)"')
DOC_CODE_RE = re.compile(r"(?:ID|QA|REP|DIS|AUTH|SYS)-[0-9]{3,4}(?:-[A-Z]+)?")

# 文档里明确写了「未采用」、源码中不存在属于正常情况（见「与指导书 3.7 建议码的映射」）
STALE_ALLOW = {"QA-1003", "QA-1014", "QA-1016", "QA-1017", "QA-1020"}

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "backend"
DOC = ROOT / "docs" / "错误码.md"


def scan_source() -> dict[str, set[str]]:
    found: dict[str, set[str]] = {}
    for path in BACKEND.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in CODE_RE.finditer(text):
            found.setdefault(match.group("code"), set()).add(str(path.relative_to(ROOT)))
    return found


def scan_doc() -> set[str]:
    text = DOC.read_text(encoding="utf-8")
    return set(DOC_CODE_RE.findall(text))


def main() -> int:
    if not BACKEND.is_dir() or not DOC.is_file():
        print(f"找不到 backend/ 或 {DOC}，请在仓库根目录执行", file=sys.stderr)
        return 2

    source = scan_source()
    documented = scan_doc()

    missing = sorted(set(source) - documented)
    stale = sorted(documented - set(source) - STALE_ALLOW)

    result = {
        "source_codes": len(source),
        "documented_codes": len(documented),
        "missing": {code: sorted(source[code]) for code in missing},
        "stale": stale,
    }

    if "--json" in sys.argv:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"源码错误码 {len(source)} 个，文档登记 {len(documented)} 个")
        if missing:
            print(f"\n[FAIL] 源码在用但文档未登记（{len(missing)}）：")
            for code in missing:
                where = ", ".join(sorted(source[code])[:2])
                print(f"  - {code}  ({where})")
        if stale:
            print(f"\n[WARN]  文档登记但源码已无引用（{len(stale)}）：")
            for code in stale:
                print(f"  - {code}")
        if not missing:
            print("\n[OK] 文档与实现一致")

    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
