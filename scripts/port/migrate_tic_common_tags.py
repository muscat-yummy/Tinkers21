"""Migrate reviewed common tag field names after checking the resolved native API."""
import pathlib, re, subprocess, zipfile
from migrate_tic_resource_locations import TOKEN

COMMON = {
    "GLASS": "GLASS_BLOCKS", "GLASS_COLORLESS": "GLASS_BLOCKS_COLORLESS",
    "GLASS_TINTED": "GLASS_BLOCKS_TINTED", "SAND": "SANDS",
    "SAND_COLORLESS": "SANDS_COLORLESS", "SAND_RED": "SANDS_RED",
    "COBBLESTONE": "COBBLESTONES", "STONE": "STONES", "GRAVEL": "GRAVELS",
    "SANDSTONE": "SANDSTONE_BLOCKS", "OBSIDIAN": "OBSIDIANS_NORMAL",
}
ITEM = {
    "STRING": "STRINGS", "LEATHER": "LEATHERS", "GUNPOWDER": "GUNPOWDERS",
    "DUSTS_PRISMARINE": "GEMS_PRISMARINE", "TOOLS_SHIELDS": "TOOLS_SHIELD",
    "TOOLS_BOWS": "TOOLS_BOW", "TOOLS_CROSSBOWS": "TOOLS_CROSSBOW",
    "TOOLS_FISHING_RODS": "TOOLS_FISHING_ROD", "TOOLS_TRIDENTS": "TOOLS_SPEAR",
    "SHEARS": "TOOLS_SHEAR",
}
VANILLA = {
    "HEADS": "SKULLS", "ARMORS_BOOTS": "FOOT_ARMOR",
    "ARMORS_LEGGINGS": "LEG_ARMOR", "ARMORS_CHESTPLATES": "CHEST_ARMOR",
    "ARMORS_HELMETS": "HEAD_ARMOR",
}
PREFIX = ["net", ".", "neoforged", ".", "neoforge", ".", "common", "."]

def migrate(source):
    tokens = [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(source)
              if not m.group().isspace() and not m.group().startswith(("//", "/*"))]
    code = " ".join(t[0] for t in tokens if not t[0].startswith(('"', "'")))
    native_import = re.search(r"import net \. neoforged \. neoforge \. common \. Tags ;", code)
    edits, count = [], 0
    for i in range(len(tokens) - 4):
        if [t[0] for t in tokens[i:i+2]] != ["Tags", "."] or tokens[i+2][0] not in ("Items", "Blocks") or tokens[i+3][0] != ".":
            continue
        qualified = i >= len(PREFIX) and [t[0] for t in tokens[i-len(PREFIX):i]] == PREFIX
        if i > 0 and tokens[i-1][0] == "." and not qualified:
            continue
        if not qualified and not native_import:
            continue
        group, name = tokens[i+2][0], tokens[i+4][0]
        renamed = COMMON.get(name) or (ITEM.get(name) if group == "Items" else None)
        if renamed:
            edits.append((tokens[i+4][1], tokens[i+4][2], renamed))
        elif group == "Items" and name in VANILLA:
            begin = i-len(PREFIX) if qualified else i
            edits.append((tokens[begin][1], tokens[begin][2], "net.minecraft.tags.ItemTags"))
            for j in range(begin+1, i+3):
                edits.append((tokens[j][1], tokens[j][2], ""))
            edits.append((tokens[i+4][1], tokens[i+4][2], VANILLA[name]))
        else:
            continue
        count += 1
    for start, end, replacement in sorted(edits, reverse=True):
        source = source[:start] + replacement + source[end:]
    return source, count

def verify_native_api():
    wanted = {
        "net.neoforged.neoforge.common.Tags$Blocks": set(COMMON.values()),
        "net.neoforged.neoforge.common.Tags$Items": set(COMMON.values()) | set(ITEM.values()),
        "net.minecraft.tags.ItemTags": set(VANILLA.values()),
    }
    for entry in pathlib.Path("/tmp/tinkers-port-classpath.txt").read_text().splitlines():
        jar = pathlib.Path(entry)
        if jar.suffix != ".jar" or not jar.is_file():
            continue
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
        for cls in list(wanted):
            if cls.replace(".", "/") + ".class" not in names:
                continue
            api = subprocess.run(["javap", "-classpath", str(jar), "-p", cls], check=True, capture_output=True, text=True).stdout
            for field in wanted[cls]:
                if not re.search(r"public static final .*\b" + field + r";", api):
                    raise RuntimeError("Missing resolved native tag field: " + cls + "." + field)
            del wanted[cls]
    if wanted:
        raise RuntimeError("Missing native tag classes: " + ", ".join(wanted))
    print("Resolved native common/vanilla tag fields: PASS")

def check_parser():
    prefix = "import net.neoforged.neoforge.common.Tags;\n"
    cases = [
        ("Tags.Items.STRING", "Tags.Items.STRINGS"),
        ("Tags.Blocks.GLASS", "Tags.Blocks.GLASS_BLOCKS"),
        ("Tags.Items.HEADS", "net.minecraft.tags.ItemTags.SKULLS"),
        ("Tags /* retained */ . Items . HEADS", "net.minecraft.tags.ItemTags /* retained */   . SKULLS"),
        ('"Tags.Items.STRING" // Tags.Items.HEADS', '"Tags.Items.STRING" // Tags.Items.HEADS'),
        ('String s = """Tags.Items.STRING""";', 'String s = """Tags.Items.STRING""";'),
        ("TinkerTags.Items.STRING", "TinkerTags.Items.STRING"),
        ("Other.Tags.Items.STRING", "Other.Tags.Items.STRING"),
        ("Tags.Items.GLASS_SILICA", "Tags.Items.GLASS_SILICA"),
    ]
    for before, after in cases:
        actual, _ = migrate(prefix + before)
        if actual != prefix + after or migrate(actual)[0] != actual:
            raise AssertionError((before, actual, after))
    if migrate("import other.Tags; Tags.Items.STRING")[1]:
        raise AssertionError("Unrelated tag import changed")
    if migrate("net.neoforged.neoforge.common.Tags.Items.HEADS")[0] != "net.minecraft.tags.ItemTags.SKULLS":
        raise AssertionError("Qualified native tag did not migrate")
    print("Common tag parser owned/unrelated/quoted/comment/qualified/idempotence cases: PASS")

if __name__ == "__main__":
    check_parser()
    verify_native_api()
    files, refs = 0, 0
    for path in sorted(pathlib.Path("src/main/java").rglob("*.java")):
        original = path.read_text()
        updated, count = migrate(original)
        if updated != original:
            path.write_text(updated)
            files += 1
            refs += count
    print("Native common tag migration files:", files)
    print("Native common tag migration references:", refs)
