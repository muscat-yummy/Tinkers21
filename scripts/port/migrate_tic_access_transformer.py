"""Remap legacy access-transformer members only when the mapped descriptor verifies."""
import pathlib, re, subprocess, zipfile
class_jars = {}
for entry in pathlib.Path('/tmp/tinkers-port-classpath.txt').read_text().splitlines():
    jar = pathlib.Path(entry)
    if not jar.is_file() or jar.suffix != '.jar':
        continue
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.endswith('.class') and name.startswith('net/minecraft/'):
                class_jars.setdefault(name[:-6].replace('/', '.'), jar)

cache = {}
def members(cls):
    if cls in cache:
        return cache[cls]
    jar = class_jars.get(cls)
    if jar is None:
        cache[cls] = set()
        return cache[cls]
    result = subprocess.run(['javap', '-classpath', str(jar), '-p', '-s', cls], capture_output=True, text=True, check=True)
    found, declaration = set(), ''
    for line in result.stdout.splitlines():
        line = line.strip()
        if line.startswith('descriptor:'):
            descriptor = line.split(':', 1)[1].strip()
            if '(' in declaration:
                name = declaration.split('(', 1)[0].split()[-1]
                if name == cls:
                    name = '<init>'
            else:
                name = declaration.rstrip(';').split()[-1]
            found.add((name, descriptor))
        elif line.endswith(';'):
            declaration = line
    cache[cls] = found
    return found

path = pathlib.Path('src/main/resources/META-INF/accesstransformer.cfg')
lines, changed, pending = [], 0, 0
for line in path.read_text().splitlines():
    code, separator, comment = line.partition('#')
    parts = code.split()
    if len(parts) != 3 or not separator:
        lines.append(line)
        continue
    access, cls, target = parts
    if not re.match(r'(?:m_|f_|func_|field_)\w+', target):
        lines.append(line)
        continue
    candidate = comment.strip()
    if not re.fullmatch(r'[A-Za-z_$]\w*', candidate):
        lines.append(line)
        pending += 1
        continue
    descriptor = target[target.index('('):] if '(' in target else None
    found = members(cls)
    verified = (candidate, descriptor) in found if descriptor else any(name == candidate and not desc.startswith('(') for name, desc in found)
    if verified:
        lines.append(access + ' ' + cls + ' ' + candidate + (descriptor or '') + ' # ' + candidate)
        changed += 1
    else:
        lines.append(line)
        pending += 1
path.write_text('\n'.join(lines) + '\n')
print('Descriptor-verified mapped AT members:', changed)
print('Legacy AT entries requiring manual review:', pending)
