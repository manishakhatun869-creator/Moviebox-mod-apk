#!/usr/bin/env python3
"""Static consistency checks for the Towfik Music Android sources.

This is NOT a substitute for a Gradle build (which needs the Android SDK); it is
a fast lint that executes against the real files and catches the classes of
error that break an Android build:

  1. every res/*.xml and the manifest are well-formed XML
  2. every @type/name reference in XML resolves to a defined resource
  3. every R.type.name reference in Kotlin resolves to a defined resource
  4. every view-binding property used in Kotlin exists as an id in the layout
     (or included layout) that binding class is generated from
  5. package declaration matches the source directory
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
MAIN_JAVA = ROOT / "app" / "src" / "main" / "java"
TEST_JAVA = ROOT / "app" / "src" / "test" / "java"
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"

errors = []


def err(msg):
    errors.append(msg)


def snake(name):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


# ---------- collect defined resources ----------
defined = {
    "string": set(), "color": set(), "dimen": set(), "style": set(),
    "drawable": set(), "layout": set(), "menu": set(), "mipmap": set(),
    "id": set(),
}

def collect_values():
    for f in RES.glob("values*/*.xml"):
        try:
            tree = ET.parse(f)
        except ET.ParseError as e:
            err(f"XML parse {f}: {e}")
            continue
        for el in tree.getroot():
            tag = el.tag.split('}')[-1]
            name = el.get("name")
            if not name:
                continue
            if tag in ("string", "color", "dimen", "style", "id"):
                defined[tag].add(name)

def collect_files():
    for d in ("drawable", "layout", "menu", "mipmap"):
        base = RES / d
        if base.exists():
            for f in base.iterdir():
                if f.suffix == ".xml" or f.suffix == ".png":
                    defined[d].add(f.stem)
    anydpi = RES / "mipmap-anydpi-v26"
    if anydpi.exists():
        for f in anydpi.glob("*.xml"):
            defined["mipmap"].add(f.stem)

def collect_ids():
    for f in RES.glob("layout/*.xml"):
        try:
            tree = ET.parse(f)
        except ET.ParseError as e:
            err(f"XML parse {f}: {e}")
            continue
        for el in tree.iter():
            i = el.get("{http://schemas.android.com/apk/res/android}id")
            if i:
                defined["id"].add(i.replace("@+id/", "").replace("@id/", ""))
    # menu item ids (referenced from Kotlin as R.id.nav_*)
    for f in RES.glob("menu/*.xml"):
        try:
            tree = ET.parse(f)
        except ET.ParseError as e:
            err(f"XML parse {f}: {e}")
            continue
        for el in tree.iter():
            i = el.get("{http://schemas.android.com/apk/res/android}id")
            if i:
                defined["id"].add(i.replace("@+id/", "").replace("@id/", ""))


# ---------- XML @references ----------
REF_RE = re.compile(r"@(string|color|dimen|style|drawable|layout|menu|mipmap|id)/([A-Za-z0-9_.]+)")

def check_xml_refs():
    for f in list(RES.rglob("*.xml")) + [MANIFEST]:
        text = f.read_text()
        for m in REF_RE.finditer(text):
            typ, name = m.group(1), m.group(2)
            if typ == "id":
                continue  # ids are declared via @+id elsewhere
            if name.startswith("android:"):
                continue
            if typ == "style" and ("Material" in name or "AppCompat" in name):
                continue  # provided by the Material / AppCompat libraries
            if name not in defined.get(typ, set()):
                err(f"{f.name}: @{typ}/{name} not defined")


# ---------- Kotlin R.* references ----------
R_RE = re.compile(r"\bR\.(string|color|dimen|style|drawable|layout|menu|mipmap|id)\.([A-Za-z0-9_]+)")

def check_kotlin_refs():
    for f in list(MAIN_JAVA.rglob("*.kt")) + list(TEST_JAVA.rglob("*.kt")):
        text = f.read_text()
        for m in R_RE.finditer(text):
            typ, name = m.group(1), m.group(2)
            if name not in defined.get(typ, set()):
                err(f"{f.name}: R.{typ}.{name} not defined")


# ---------- view binding property checks ----------
BIND_IMPORT_RE = re.compile(r"import com\.towfik\.music\.databinding\.([A-Za-z0-9_]+)Binding")
CHAIN_RE = re.compile(r"\b(?:binding|b)\.([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)")

def layout_ids(layout_stem):
    ids = set()
    includes = {}
    f = RES / "layout" / f"{layout_stem}.xml"
    if not f.exists():
        return ids, includes
    tree = ET.parse(f)
    for el in tree.iter():
        i = el.get("{http://schemas.android.com/apk/res/android}id")
        if i:
            ids.add(i.replace("@+id/", "").replace("@id/", ""))
        if el.tag.split('}')[-1] == "include":
            inc_id = el.get("{http://schemas.android.com/apk/res/android}id")
            inc_layout = el.get("layout")
            if inc_id and inc_layout:
                includes[inc_id.replace("@+id/", "")] = inc_layout.replace("@layout/", "")
    return ids, includes

def check_binding():
    for f in MAIN_JAVA.rglob("*.kt"):
        text = f.read_text()
        m = BIND_IMPORT_RE.search(text)
        if not m:
            continue
        binding_class = m.group(1)
        layout_stem = snake(binding_class)
        ids, includes = layout_ids(layout_stem)
        for chain_m in CHAIN_RE.finditer(text):
            parts = chain_m.group(1).split(".")
            first = parts[0]
            if first in ("root",):
                continue
            if first in ids:
                continue
            if first in includes:
                # remaining parts must be in the included layout (or root)
                sub_ids, _ = layout_ids(includes[first])
                rest = parts[1:]
                if rest and rest[0] != "root" and rest[0] not in sub_ids:
                    err(f"{f.name}: binding.{chain_m.group(1)} -> '{rest[0]}' not in {includes[first]}")
                continue
            err(f"{f.name}: binding.{first} not an id in {layout_stem}.xml")


# ---------- package/dir match ----------
PKG_RE = re.compile(r"^\s*package\s+([a-zA-Z0-9_.]+)", re.M)

def check_package():
    for base in (MAIN_JAVA, TEST_JAVA):
        for f in base.rglob("*.kt"):
            m = PKG_RE.search(f.read_text())
            if not m:
                err(f"{f}: no package decl")
                continue
            pkg = m.group(1)
            rel = f.parent.relative_to(base)
            expect = ".".join(rel.parts)
            if pkg != expect:
                err(f"{f}: package '{pkg}' != dir '{expect}'")


def main():
    # XML wellformedness
    for f in RES.rglob("*.xml"):
        try:
            ET.parse(f)
        except ET.ParseError as e:
            err(f"XML parse {f}: {e}")
    try:
        ET.parse(MANIFEST)
    except ET.ParseError as e:
        err(f"XML parse manifest: {e}")

    collect_values()
    collect_files()
    collect_ids()
    check_xml_refs()
    check_kotlin_refs()
    check_binding()
    check_package()

    if errors:
        print(f"FAIL: {len(errors)} problem(s)")
        for e in sorted(set(errors)):
            print(" -", e)
        sys.exit(1)
    print("verify.py: all checks passed")


if __name__ == "__main__":
    main()
