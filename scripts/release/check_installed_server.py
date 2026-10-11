"""Load the exact inspected release jars with a normal NeoForge installation."""
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]
SERVER = ROOT / "release-server"
mods = SERVER / "mods"
mods.mkdir(exist_ok=True)
for path in (ROOT / "dist").glob("*.jar"):
    shutil.copyfile(path, mods / path.name)
assert len(list(mods.glob("*.jar"))) == 2, "Expected only the actual TiC/Mantle release jars"
(SERVER / "eula.txt").write_text("eula=true\n")
(SERVER / "user_jvm_args.txt").write_text("-Xms512M\n-Xmx3G\n")
(SERVER / "server.properties").write_text(
    "server-ip=127.0.0.1\nserver-port=25575\nview-distance=2\nsimulation-distance=2\n"
    "level-seed=12345\nmax-players=1\nmotd=Tinkers21 isolated release acceptance\n")
started = False
with (SERVER / "console.log").open("w") as output:
    process = subprocess.Popen(["bash", "run.sh", "--nogui"], cwd=SERVER,
                               stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)
    try:
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            log = SERVER / "logs/latest.log"
            text = log.read_text(errors="replace") if log.exists() else ""
            if "Done (" in text and 'For help, type "help"' in text:
                started = True
                process.stdin.write("list\nstop\n")
                process.stdin.flush()
                process.wait(timeout=45)
                break
            if process.poll() is not None:
                raise RuntimeError(f"Installed release server exited early: {process.returncode}")
            time.sleep(1)
        if not started:
            raise TimeoutError("Installed distribution jars did not complete actual world startup")
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
text = (SERVER / "logs/latest.log").read_text(errors="replace")
assert process.returncode == 0, f"Installed release server exit code {process.returncode}"
errors = [line for line in text.splitlines() if "/ERROR]" in line]
assert not errors, "Actual distribution server ERROR: " + "\n".join(errors)
assert "Loaded 3336 recipes" in text, "Actual distribution recipe reload did not match accepted core pack"
assert "Stopping server" in text and "Saving" in text, "Distribution world did not shut down/save cleanly"
assert not any("Mod loading has failed" in line or "Missing or unsupported mandatory dependencies" in line
               for line in text.splitlines()), "Native release dependency check failed"
print("Tinkers21 actual installed production jars world reload and clean shutdown PASS")
