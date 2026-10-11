"""Reject runtime or dependency drift from the accepted native CI sources."""
from pathlib import Path
import subprocess

BASE = "01fa71cbaeddb4abe63c245b73a37f96c65255e7"
CLIENT = "304b302cf0e016371979bc35f58bb9aef1857c16"
subprocess.run(["git", "diff", "--exit-code", BASE, "HEAD", "--", "src/main", "src/generated",
                "mantle-port/src", "src/portAddonCheck", "src/portGameTest", "settings.gradle",
                "gradle", "gradlew", "gradlew.bat"], check=True)
subprocess.run(["git", "diff", "--exit-code", CLIENT, "HEAD", "--", "src/portClientCheck"], check=True)
for filename, suffix in [
    ("build.gradle", "\n// Keep upstream and fork notices in the standalone distribution jar.\ntasks.named('jar', Jar).configure {\n    from('LICENSE') { into('META-INF/licenses/tinkers21') }\n}\n"),
    ("mantle-port/build.gradle", "\n// The independently distributed library must retain its upstream MIT notice.\ntasks.named('jar', Jar).configure {\n    from('LICENSE') { into('META-INF/licenses/mantle') }\n}\n"),
]:
    original = subprocess.check_output(["git", "show", f"{BASE}:{filename}"], text=True)
    assert Path(filename).read_text() == original + suffix, f"Build/dependency drift: {filename}"
original = subprocess.check_output(["git", "show", f"{BASE}:gradle.properties"], text=True)
assert Path("gradle.properties").read_text() == original.replace(
    "mod_name=Tinkers' Construct (Unofficial Port)", "mod_name=Tinkers21 (Unofficial Fork)")
print("Accepted runtime source, toolchain and distribution-only build changes PASS")
