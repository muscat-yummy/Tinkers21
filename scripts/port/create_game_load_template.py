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
