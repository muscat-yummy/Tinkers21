"""Inspect native distribution jars and package the first preview with provenance."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import shutil
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[2]
DIST = ROOT / "dist"
VERSION = os.environ.get("TINKERS21_RELEASE_VERSION", "3.12.1-port.0")
LIBRARY_VERSION = "1.11-port.0"


def inspect_jar(path: Path, mod_id: str, version: str, required_class: str, license_path: str) -> None:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), f"Duplicate entries in {path}"
        assert archive.testzip() is None, f"Invalid archive: {path}"
        assert required_class in names, f"Missing production implementation: {path}"
        assert license_path in names, f"Missing MIT notice: {path}"
        assert b"Copyright (c) 2022 SlimeKnights" in archive.read(license_path)
        assert b"Permission is hereby granted" in archive.read(license_path)
        assert "META-INF/mods.toml" not in names, f"Legacy Forge metadata retained: {path}"
        assert not any(name.startswith(("example/ticaddon/", "slimeknights/tconstruct/port/"))
                       or "portaddoncheck" in name for name in names), f"Verification fixture leaked: {path}"
        metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
        assert [(mod["modId"], mod["version"]) for mod in metadata["mods"]] == [(mod_id, version)]
        assert all("${" not in str(dependency) for dependencies in metadata["dependencies"].values()
                   for dependency in dependencies), f"Unexpanded dependency metadata: {path}"
        if mod_id == "tconstruct":
            assert metadata["mods"][0]["displayName"] == "Tinkers21 (Unofficial Fork)"
            assert "data/tconstruct/recipe/tools/materials/iron/ingot.json" in names
            assert "data/forge/tags/item/ingots/iron.json" in names
    assert path.stat().st_size > 200_000, f"Unexpected metadata-only jar: {path}"


def main() -> None:
    DIST.mkdir(exist_ok=True)
    tic = ROOT / "build/libs" / f"TinkersConstruct-{VERSION}.jar"
    mantle = ROOT / "mantle-port/build/libs" / f"Mantle-{LIBRARY_VERSION}.jar"
    inspect_jar(tic, "tconstruct", VERSION, "slimeknights/tconstruct/TConstruct.class",
                "META-INF/licenses/tinkers21/LICENSE")
    inspect_jar(mantle, "mantle", LIBRARY_VERSION, "slimeknights/mantle/Mantle.class",
                "META-INF/licenses/mantle/LICENSE")
    jars = []
    for source, name in [(tic, f"Tinkers21-1.21.1-neoforge-{VERSION}.jar"),
                         (mantle, f"Mantle21-1.21.1-neoforge-{LIBRARY_VERSION}.jar")]:
        destination = DIST / name
        shutil.copyfile(source, destination)
        jars.append(destination)
    provenance = {
        "project": "Tinkers21", "version": VERSION, "prerelease": VERSION == "3.12.1-port.0",
        "source_commit": os.environ["GITHUB_SHA"], "packaging_run": os.environ["GITHUB_RUN_ID"],
        "minecraft": "1.21.1", "neoforge": "21.1.251", "java": 21,
        "accepted_gates": [
            {"run": 38110981948, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7", "gate": "native datagen"},
            {"run": 38110981960, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7", "gate": "assembly and 25 actual GameTests"},
            {"run": 38111094698, "sha": "304b302cf0e016371979bc35f58bb9aef1857c16", "gate": "strict graphical core/addon client"},
        ],
        "jars": {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in jars},
        "fixtures_in_distribution": False,
    }
    if VERSION == "3.12.1-port.1":
        # Server/datagen evidence is retained only for identical server/data sources.
        provenance["accepted_gates"] = provenance["accepted_gates"][:2]
        for gate in provenance["accepted_gates"]:
            gate["scope"] = "unchanged server/data baseline; hotfix changes only client tint"
        provenance["accepted_gates"].append({"run": int(os.environ["GITHUB_RUN_ID"]),
            "sha": os.environ["GITHUB_SHA"], "gate": "strict graphical core/addon/crystal hotfix client"})
    (DIST / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    shutil.copyfile(ROOT / f"docs/releases/{VERSION}.md", DIST / "release-notes.md")
    bundle = DIST / f"Tinkers21-1.21.1-neoforge-{VERSION}.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as archive:
        for jar in jars:
            archive.write(jar, f"mods/{jar.name}")
        archive.write(ROOT / "LICENSE", "LICENSE-Tinkers21.txt")
        archive.write(ROOT / "mantle-port/LICENSE", "LICENSE-Mantle.txt")
        archive.write(DIST / "release-notes.md", "INSTALL.md")
        archive.write(DIST / "provenance.json", "provenance.json")
    assets = sorted(path for path in DIST.iterdir() if path.is_file() and path.name != "SHA256SUMS.txt")
    checksums = "".join(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n" for path in assets)
    (DIST / "SHA256SUMS.txt").write_text(checksums)
    print(checksums, end="")
    print("Tinkers21 native distribution metadata licenses fixture exclusion and packaging PASS")


if __name__ == "__main__":
    main()
