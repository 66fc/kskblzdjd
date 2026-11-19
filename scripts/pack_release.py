"""打包完整项目源码为 zip，便于发送给项目组员。

排除：node_modules、编译中间产物、Git/工具目录、临时文件
保留：backend/target/badminton-admin-1.0.0.jar、frontend/dist（免构建即可运行）

用法：
    python scripts/pack_release.py
输出：
    release/羽球幻想管理系统-源码.zip
"""
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "release"
OUT_NAME = "羽毛球场地管理系统-源码.zip"
ARC_ROOT = "badminton-system"

# 目录级排除（相对项目根，匹配路径前缀）
EXCLUDE_DIRS = {
    "frontend/node_modules",
    "backend/target/classes",
    "backend/target/generated-sources",
    "backend/target/generated-test-sources",
    "backend/target/maven-status",
    "backend/target/maven-archiver",
    "backend/target/test-classes",
    "backend/target/surefire-reports",
    ".git",
    ".workbuddy",
    "release",
    ".tmp",
    ".idea",
    "frontend/dist/assets/__vite_ping",
}

# 文件级排除（后缀 / 名称）
EXCLUDE_SUFFIX = {".log", ".pid", ".original", ".zip"}
EXCLUDE_NAMES = {".DS_Store", "Thumbs.db"}

INCLUDE_HIDDEN = {".gitignore", ".npmrc", ".env.development", ".env.example"}


def skip(rel: Path) -> bool:
    posix = rel.as_posix()
    for d in EXCLUDE_DIRS:
        if posix == d or posix.startswith(d + "/"):
            return True
    if rel.name in EXCLUDE_NAMES:
        return True
    if rel.suffix.lower() in EXCLUDE_SUFFIX:
        return True
    # 打包脚本自身不进入发布包
    if posix == "scripts/pack_release.py":
        return True
    return False


def collect() -> list[Path]:
    files: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        cur = Path(dirpath)
        rel_dir = cur.relative_to(ROOT)
        # 剪枝：整目录跳过
        if rel_dir.as_posix() != "." and skip(rel_dir):
            dirnames[:] = []
            continue
        dirnames[:] = [d for d in dirnames if not skip(rel_dir / d)]
        for name in filenames:
            rel = rel_dir / name
            if skip(rel):
                continue
            if name.startswith(".") and name not in INCLUDE_HIDDEN:
                continue
            files.append(rel)
    return sorted(files)


def main() -> int:
    files = collect()
    if not files:
        print("没有可打包的文件")
        return 1

    OUT_DIR.mkdir(exist_ok=True)
    out_path = OUT_DIR / OUT_NAME
    if out_path.exists():
        out_path.unlink()

    total = 0
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for rel in files:
            src = ROOT / rel
            try:
                size = src.stat().st_size
            except OSError:
                continue
            zf.write(src, f"{ARC_ROOT}/{rel.as_posix()}")
            total += size

    size_mb = out_path.stat().st_size / 1024 / 1024
    print(f"打包完成：{out_path}")
    print(f"文件数：{len(files)}（原始 {total / 1024 / 1024:.1f} MB，压缩后 {size_mb:.1f} MB）")

    # 分类统计，便于核对
    groups: dict[str, int] = {}
    for rel in files:
        top = rel.as_posix().split("/")[0]
        groups[top] = groups.get(top, 0) + 1
    for k in sorted(groups):
        print(f"  {k:<28} {groups[k]:>5} 个文件")
    return 0


if __name__ == "__main__":
    sys.exit(main())
