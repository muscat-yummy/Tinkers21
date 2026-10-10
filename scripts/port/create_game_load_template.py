"""Create a real empty structure fixture for the isolated bootstrap GameTest."""
import gzip
import pathlib
import struct

def name(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded

# Empty structure: one air palette entry, no blocks or entities, one cubic cell.
# DataVersion is omitted deliberately: vanilla accepts unversioned structures.
root = b"\x0a" + name("")
root += b"\x09" + name("size") + b"\x03" + struct.pack(">i", 3) + struct.pack(">iii", 1, 1, 1)
root += b"\x09" + name("palette") + b"\x0a" + struct.pack(">i", 1)
root += b"\x08" + name("Name") + name("minecraft:air") + b"\x00"
root += b"\x09" + name("blocks") + b"\x0a" + struct.pack(">i", 0)
root += b"\x09" + name("entities") + b"\x0a" + struct.pack(">i", 0)
root += b"\x00"
path = pathlib.Path("build/generated/portGameTestResources/data/tconstruct/structure/bootstrap.nbt")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(root, mtime=0))
print("Generated actual empty bootstrap GameTest structure")

# A separate bounded machine fixture isolates real ticking machines from bootstrap.
machine_root = root.replace(struct.pack(">iii", 1, 1, 1), struct.pack(">iii", 5, 4, 5), 1)
machine_path = path.with_name("smeltery.nbt")
machine_path.write_bytes(gzip.compress(machine_root, mtime=0))
print("Generated actual empty smeltery GameTest structure")

# Island names describe shape variants, not dimensions: the largest retained
# template is 32x13x32. Reserve its whole footprint plus foliage headroom so
# placement and fluid ticks cannot spill into neighboring GameTests.
island_root = root.replace(struct.pack(">iii", 1, 1, 1), struct.pack(">iii", 35, 32, 35), 1)
path.with_name("island.nbt").write_bytes(gzip.compress(island_root, mtime=0))
print("Generated actual empty island GameTest structure")
