"""Use the verified native Recipe result lookup parameter without changing result logic."""
import pathlib
import re
import subprocess
import zipfile
from migrate_tic_resource_locations import TOKEN

NATIVE_PARAMETER = "net.minecraft.core.HolderLookup.Provider"

def migrate(source):
    tokens = [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(source)
              if not m.group().isspace() and not m.group().startswith(("//", "/*"))]
    code = " ".join(t[0] for t in tokens if not t[0].startswith((chr(34), chr(39))))
    if not re.search(r"import net \. minecraft \. core \. RegistryAccess ;", code):
        return source, 0
    if not re.search(r"import net \. minecraft \. world \. item \. ItemStack ;", code):
        return source, 0
    edits = []
    for i in range(1, len(tokens) - 5):
        if (tokens[i-1][0] == "ItemStack" and tokens[i][0] == "getResultItem"
            and tokens[i+1][0] == "(" and tokens[i+2][0] == "RegistryAccess"
            and re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*", tokens[i+3][0])
            and tokens[i+4][0] == ")" and tokens[i+5][0] in ("{", ";")):
            edits.append((tokens[i+2][1], tokens[i+2][2]))
    for start, end in reversed(edits):
        source = source[:start] + NATIVE_PARAMETER + source[end:]
    return source, len(edits)

def verify_native_api():
    cls = "net.minecraft.world.item.crafting.Recipe"
    for entry in pathlib.Path("/tmp/tinkers-port-classpath.txt").read_text().splitlines():
        jar = pathlib.Path(entry)
        if jar.suffix != ".jar" or not jar.is_file():
            continue
        with zipfile.ZipFile(jar) as archive:
            if cls.replace(".", "/") + ".class" not in archive.namelist():
                continue
        api = subprocess.run(["javap", "-classpath", str(jar), "-p", cls], check=True, capture_output=True, text=True).stdout
        if not re.search(r"getResultItem\(net\.minecraft\.core\.HolderLookup\$Provider\)", api):
            raise RuntimeError("Unexpected native Recipe result lookup signature")
        print("Resolved native Recipe result lookup parameter: PASS")
        return
    raise RuntimeError("Native Recipe API class was not found")

def check_parser():
    prefix = "import net.minecraft.core.RegistryAccess; import net.minecraft.world.item.ItemStack;\n"
    signature = "default ItemStack getResultItem(RegistryAccess access) { return result.get(); }"
    expected = signature.replace("RegistryAccess access", NATIVE_PARAMETER + " access")
    cases = [
        (prefix + signature, prefix + expected),
        (prefix + signature.replace("RegistryAccess", "/* retained */ RegistryAccess"), prefix + expected.replace(NATIVE_PARAMETER, "/* retained */ " + NATIVE_PARAMETER)),
        (prefix + 'String s = "ItemStack getResultItem(RegistryAccess access) {";', prefix + 'String s = "ItemStack getResultItem(RegistryAccess access) {";'),
        (prefix + "// " + signature, prefix + "// " + signature),
        (signature, signature),
        (prefix + "ItemStack other(RegistryAccess access) { return getResultItem(access); }", prefix + "ItemStack other(RegistryAccess access) { return getResultItem(access); }"),
    ]
    for original, expected in cases:
        actual, _ = migrate(original)
        if actual != expected or migrate(actual)[0] != actual:
            raise AssertionError("Recipe result lookup parser or idempotence changed")
    print("Recipe result lookup imports, comments, strings, body preservation and idempotence: PASS")

if __name__ == "__main__":
    check_parser()
    verify_native_api()
    files, references = 0, 0
    for path in sorted(pathlib.Path("src/main/java").rglob("*.java")):
        original = path.read_text()
        updated, count = migrate(original)
        if updated != original:
            path.write_text(updated)
            files += 1
            references += count
    print("Native recipe result lookup files:", files)
    print("Native recipe result lookup references:", references)
