"""Rename native attribute operations only after verifying the resolved public enum."""
import pathlib, re, subprocess, zipfile
from migrate_tic_resource_locations import TOKEN

RENAMES = {"ADDITION": "ADD_VALUE", "MULTIPLY_BASE": "ADD_MULTIPLIED_BASE", "MULTIPLY_TOTAL": "ADD_MULTIPLIED_TOTAL"}
IMPORT = re.compile(r"import\s+(?:static\s+)?net\.minecraft\.world\.entity\.ai\.attributes\.AttributeModifier(?:\.Operation)?[.;]")

def migrate(source):
    tokens = [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(source)
              if not m.group().isspace() and not m.group().startswith(("//", "/*"))]
    code = " ".join(t[0] for t in tokens if not t[0].startswith(('"', "'")))
    # Check a real code import, not an import-looking string/comment.
    native = re.search(r"import\s+(?:static\s+)?net\s*\.\s*minecraft\s*\.\s*world\s*\.\s*entity\s*\.\s*ai\s*\.\s*attributes\s*\.\s*AttributeModifier\b", code)
    if not native:
        return source, 0
    edits = []
    for i in range(len(tokens) - 2):
        if tokens[i][0] == "Operation" and tokens[i+1][0] == "." and tokens[i+2][0] in RENAMES:
            _, start, end = tokens[i+2]
            edits.append((start, end, RENAMES[tokens[i+2][0]]))
    for start, end, replacement in reversed(edits):
        source = source[:start] + replacement + source[end:]
    return source, len(edits)

def verify_native_api():
    cls = "net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation"
    for entry in pathlib.Path("/tmp/tinkers-port-classpath.txt").read_text().splitlines():
        jar = pathlib.Path(entry)
        if jar.suffix != ".jar" or not jar.is_file():
            continue
        with zipfile.ZipFile(jar) as archive:
            found = cls.replace(".", "/") + ".class" in archive.namelist()
        if not found:
            continue
        result = subprocess.run(["javap", "-classpath", str(jar), "-p", cls], check=True, capture_output=True, text=True).stdout
        for name in RENAMES.values():
            if not re.search(r"public static final .*\b" + name + r";", result):
                raise RuntimeError("Missing native attribute operation: " + name)
        print("Resolved native attribute operation enum: PASS")
        return
    raise RuntimeError("Native attribute operation enum was not found")

def check_parser():
    prefix = "import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;\n"
    cases = [
        (prefix + "Operation.ADDITION", prefix + "Operation.ADD_VALUE"),
        (prefix + "Operation /* retained */ . MULTIPLY_TOTAL", prefix + "Operation /* retained */ . ADD_MULTIPLIED_TOTAL"),
        (prefix + '"Operation.ADDITION" // Operation.MULTIPLY_BASE', prefix + '"Operation.ADDITION" // Operation.MULTIPLY_BASE'),
        (prefix + 'String x = """Operation.ADDITION""";', prefix + 'String x = """Operation.ADDITION""";'),
        ('import other.Operation; Operation.ADDITION', 'import other.Operation; Operation.ADDITION'),
        ('// ' + prefix + 'Operation.ADDITION', '// ' + prefix + 'Operation.ADDITION'),
    ]
    for source, expected in cases:
        actual, _ = migrate(source)
        if actual != expected or migrate(actual)[0] != actual:
            raise AssertionError((source, actual, expected))
    print("Native attribute enum import/string/comment/idempotence cases: PASS")

if __name__ == "__main__":
    check_parser()
    verify_native_api()
    files, total = 0, 0
    for path in sorted(pathlib.Path("src/main/java").rglob("*.java")):
        original = path.read_text()
        updated, count = migrate(original)
        if updated != original:
            path.write_text(updated)
            files += 1
            total += count
    print("Native attribute operation files:", files)
    print("Native attribute operation references:", total)
