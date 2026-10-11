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
LIBRARY_VERSION = {"3.12.1-port.3": "1.11-port.1", "3.12.1-port.4": "1.11-port.2", "3.12.1-port.5": "1.11-port.3", "3.12.1-port.6": "1.11-port.3", "3.12.1-port.7": "1.11-port.4", "3.12.1-port.8": "1.11-port.5"}.get(VERSION, "1.11-port.0")


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
        if VERSION in ("3.12.1-port.7", "3.12.1-port.8"):
            dependencies = {d["modId"]: d for d in metadata["dependencies"][mod_id]}
            assert dependencies["neoforge"]["versionRange"] == "[21.1.249,)"
            if mod_id == "tconstruct":
                assert dependencies["mantle"]["versionRange"] == f"[{LIBRARY_VERSION},)"
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
    if VERSION in ("3.12.1-port.1", "3.12.1-port.2", "3.12.1-port.3"):
        # Server/datagen evidence is retained only for identical server/data sources.
        provenance["accepted_gates"] = provenance["accepted_gates"][:2]
        for gate in provenance["accepted_gates"]:
            gate["scope"] = "unchanged server/data baseline; hotfix changes only client rendering"
        provenance["accepted_gates"].append({"run": int(os.environ["GITHUB_RUN_ID"]),
            "sha": os.environ["GITHUB_SHA"], "gate": "strict graphical core/addon/crystal/fluid/container screen hotfix client"})
    if VERSION == "3.12.1-port.4":
        provenance["accepted_gates"] = [
            {"run": int(os.environ["GITHUB_RUN_ID"]), "sha": os.environ["GITHUB_SHA"],
             "gate": "26 actual GameTests including native faucet iron/gold basins; strict graphical book/core/addon/crystal/fluid/container; both production assemblies; installed distribution server"},
            {"run": 38110981948, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7",
             "gate": "native datagen", "scope": "unchanged generated data and datagen sources; current native recipe lookup covered by GameTests"}]
    if VERSION == "3.12.1-port.5":
        provenance["accepted_gates"] = [
            {"run": int(os.environ["GITHUB_RUN_ID"]), "sha": os.environ["GITHUB_SHA"],
             "gate": "strict actual client six loaded books Japanese encyclopedia icons preserved material/tool/slot data; previous graphical checks; both assemblies; installed distribution server"},
            {"run": 38119549799, "sha": "89e9b8139bf48640b350bbbcea0ace90ca4e5612",
             "gate": "26 actual GameTests", "scope": "identical server/recipe sources; hotfix only client book display reader and Japanese book text"},
            {"run": 38110981948, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7",
             "gate": "native datagen", "scope": "unchanged generated data/datagen"}]
    if VERSION == "3.12.1-port.6":
        provenance["accepted_gates"] = [
            {"run": int(os.environ["GITHUB_RUN_ID"]), "sha": os.environ["GITHUB_SHA"],
             "gate": "actual Japanese Gadgetry translated index; previous strict graphical gates; production assemblies and installed distribution server"},
            {"run": 38152426339, "sha": "71e495a06763602a600bc2d3a4f4ffe11f852275",
             "gate": "27 actual GameTests and complete Japanese/English six-book graphical exploration",
             "scope": "unchanged server sources; includes accepted legacy smeltery fluid fix"},
            {"run": 38110981948, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7",
             "gate": "native datagen", "scope": "unchanged generated data/datagen"}]
    if VERSION in ("3.12.1-port.7", "3.12.1-port.8"):
        provenance["minimum_neoforge"] = "21.1.249"
        provenance["accepted_gates"] = [
            {"run": int(os.environ["GITHUB_RUN_ID"]), "sha": os.environ["GITHUB_SHA"],
             "gate": "NeoForge 21.1.249: 27 actual GameTests and strict graphical core/addon/book checks; compile on pinned 21.1.251; exact distribution jars installed on both 249 and 251"},
            {"run": 38110981948, "sha": "01fa71cbaeddb4abe63c245b73a37f96c65255e7",
             "gate": "native datagen", "scope": "unchanged generated data/datagen sources"}]
    if VERSION == "3.12.1-port.8":
        provenance["accepted_gates"] = [
            {"run": int(os.environ["GITHUB_RUN_ID"]), "sha": os.environ["GITHUB_SHA"],
             "gate": "28 actual GameTests including restored native water/lava tags and real ammo/fishing-rod tip clearing; existing strict native client without viewers; both assemblies; shipping jars installed on 249 and 251"},
            {"run": int(os.environ["RECIPE_AUDIT_RUN"]), "sha": os.environ["RECIPE_AUDIT_SHA"],
             "gate": "actual native JEI layouts and EMI widgets/tooltips for all TiC viewer categories plus actual scorched-basin EMI screen",
             "scope": "same production Java/resources apart from release versions; known non-TiC tag ID collisions explicitly recorded"}]
    evidence = ROOT / "build/release-evidence"
    evidence.mkdir(parents=True, exist_ok=True)
    (evidence / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    notes_path = ROOT / f"docs/releases/{VERSION}.md"
    notes = notes_path.read_text().split("\n### SHA256\n", 1)[0].rstrip() + "\n"
    (evidence / "release-notes.md").write_text(notes)
    for name in ("provenance.json", "release-notes.md", "SHA256SUMS.txt"):
        (DIST / name).unlink(missing_ok=True)
    bundle = DIST / f"Tinkers21-1.21.1-neoforge-{VERSION}.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as archive:
        for jar in jars:
            archive.write(jar, f"mods/{jar.name}")
        archive.write(ROOT / "LICENSE", "LICENSE-Tinkers21.txt")
        archive.write(ROOT / "mantle-port/LICENSE", "LICENSE-Mantle.txt")
        archive.write(evidence / "release-notes.md", "INSTALL.md")
    assets = sorted(path for path in DIST.iterdir() if path.is_file() and path.name != "SHA256SUMS.txt")
    checksums = "".join(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n" for path in assets)
    (evidence / "SHA256SUMS.txt").write_text(checksums)
    notes_path.write_text(notes + "\n### SHA256\n\n```text\n" + checksums + "```\n")
    print(checksums, end="")
    print("Tinkers21 native distribution metadata licenses fixture exclusion and packaging PASS")


if __name__ == "__main__":
    main()
