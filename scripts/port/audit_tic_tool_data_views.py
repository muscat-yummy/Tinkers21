"""Read-only audit of mutable tool data boundaries before component binding."""
import pathlib
import re
import subprocess
import shutil

declaration = r'\b(?:ModDataNBT|ToolDataNBT|IModDataView|RestrictedCompoundTag)\b|getPersistentData|getRestrictedNBT'
if shutil.which('rg'):
    files = subprocess.run(['rg', '-l', declaration, 'src/main/java'],
                           check=False, capture_output=True, text=True)
    if files.returncode not in (0, 1):
        raise RuntimeError(files.stderr)
    filenames = files.stdout.splitlines()
else:
    # GitHub's runner image does not always provide ripgrep.
    match = re.compile(declaration)
    filenames = [str(path) for path in pathlib.Path('src/main/java').rglob('*.java')
                 if match.search(path.read_text())]
total = 0
for filename in sorted(filenames):
    path = pathlib.Path(filename)
    source = path.read_text()
    aliases = set(re.findall(r'\b(?:ModDataNBT|ToolDataNBT|IModDataView|RestrictedCompoundTag)\s+(\w+)\b', source))
    direct = re.compile(r'get(?:PersistentData|RestrictedNBT)\s*\(\s*\)\s*\.\s*(?:getCompound|getList|get)\s*\(')
    for match in direct.finditer(source):
        number = source.count('\n', 0, match.start()) + 1
        total += 1
        print(f'Direct tool data view: {path}:{number}: {source[match.start():source.find(chr(10), match.start())].strip()}')
    if not aliases:
        continue
    receiver = r'(?:' + '|'.join(re.escape(alias) for alias in sorted(aliases)) + r')'
    getter = re.compile(r'\b' + receiver + r'\s*\.\s*(?:getCompound|getList|get|getData)\s*\(')
    for number, line in enumerate(source.splitlines(), 1):
        if getter.search(line):
            total += 1
            print(f'Tool data view: {path}:{number}: {line.strip()}')
print(f'Tool data alias getter sites requiring binding review: {total}')
