"""Migrate only FluidStack-copy sites confirmed by the Java compiler."""
import json, pathlib
from migrate_tic_resource_locations import TOKEN

def rewrite(line):
    tokens = [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(line)
              if not m.group().isspace() and not m.group().startswith(('//', '/*'))]
    starts = [i for i in range(len(tokens) - 2) if tokens[i][0] == 'new' and tokens[i + 1][0] == 'FluidStack' and tokens[i + 2][0] == '(']
    if len(starts) != 1:
        raise ValueError('Expected one diagnosed FluidStack constructor')
    start = starts[0]
    depth, brackets, braces = 1, 0, 0
    commas = []
    close = start + 3
    while close < len(tokens):
        value = tokens[close][0]
        if value == '(':
            depth += 1
        elif value == ')':
            depth -= 1
            if depth == 0:
                break
        elif value == '[':
            brackets += 1
        elif value == ']':
            brackets -= 1
        elif value == '{':
            braces += 1
        elif value == '}':
            braces -= 1
        elif value == ',' and depth == 1 and brackets == 0 and braces == 0:
            commas.append(close)
        close += 1
    if close == len(tokens) or len(commas) != 1:
        raise ValueError('Unsupported diagnosed FluidStack constructor')
    comma = commas[0]
    fluid = line[tokens[start + 2][2]:tokens[comma][1]].strip()
    amount = line[tokens[comma][2]:tokens[close][1]].strip()
    replacement = '(' + fluid + ').copyWithAmount(' + amount + ')'
    return line[:tokens[start][1]] + replacement + line[tokens[close][2]:]

def check_parser():
    examples = [
        ('new FluidStack(fluid, n)', '(fluid).copyWithAmount(n)'),
        ('return new FluidStack(result.get(), f(a,b));', 'return (result.get()).copyWithAmount(f(a,b));'),
        ('new FluidStack(fluid, f("a,b"))', '(fluid).copyWithAmount(f("a,b"))'),
        ('// new FluidStack(ignored)\nnew FluidStack(fluid, n)', '// new FluidStack(ignored)\n(fluid).copyWithAmount(n)'),
    ]
    for original, expected in examples:
        if rewrite(original) != expected:
            raise AssertionError((original, rewrite(original), expected))
    print('Diagnosed fluid-copy parser cases: PASS')

if __name__ == '__main__':
    check_parser()
    specs = json.loads(pathlib.Path('scripts/port/diagnosed_fluid_copies.json').read_text())
    changed, already, pending = 0, 0, []
    for spec in specs:
        path = pathlib.Path(spec['path'])
        old = path.read_text()
        lines = old.splitlines(keepends=True)
        expected = spec['source']
        replacement = rewrite(expected)
        index = spec['line'] - 1
        if index >= len(lines) or lines[index].strip() != expected.strip():
            candidates = [i for i, line in enumerate(lines) if line.strip() == expected.strip()]
            if len(candidates) != 1:
                if any(line.strip() == replacement.strip() for line in lines):
                    already += 1
                else:
                    pending.append(str(path) + ':' + str(spec['line']))
                continue
            index = candidates[0]
        ending = '\n' if lines[index].endswith('\n') else ''
        indent = lines[index][:len(lines[index]) - len(lines[index].lstrip())]
        lines[index] = indent + replacement.strip() + ending
        path.write_text(''.join(lines))
        changed += 1
    print('Diagnosed fluid-copy sites migrated:', changed)
    print('Already migrated fluid-copy sites:', already)
    print('Fluid-copy sites needing review:', len(pending))
    for site in pending:
        print('Pending fluid-copy site:', site)
