"""Read-only audit of mutable tool data boundaries before component binding."""
import pathlib
import re
import subprocess

files = subprocess.run(
    ['rg', '-l', r'\b(?:ModDataNBT|ToolDataNBT|RestrictedCompoundTag)\b', 'src/main/java'],
    check=False, capture_output=True, text=True)
if files.returncode not in (0, 1):
    raise RuntimeError(files.stderr)
total = 0
for filename in sorted(files.stdout.splitlines()):
    path = pathlib.Path(filename)
    source = path.read_text()
    aliases = set(re.findall(r'\b(?:ModDataNBT|ToolDataNBT|RestrictedCompoundTag)\s+(\w+)\b', source))
    if not aliases:
        continue
    receiver = r'(?:' + '|'.join(re.escape(alias) for alias in sorted(aliases)) + r')'
    getter = re.compile(r'\b' + receiver + r'\s*\.\s*(?:getCompound|getList|get|getData)\s*\(')
    for number, line in enumerate(source.splitlines(), 1):
        if getter.search(line):
            total += 1
            print(f'Tool data view: {path}:{number}: {line.strip()}')
print(f'Tool data alias getter sites requiring binding review: {total}')
