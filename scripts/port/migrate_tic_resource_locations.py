"""Move complex ResourceLocation constructor calls to validated vanilla factories."""
import pathlib, re
TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:(?!""").)*"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|\s+|[A-Za-z_$][\w$]*|.', re.DOTALL)

def migrate(source):
    tokens = [(match.group(), match.start(), match.end()) for match in TOKEN.finditer(source)
              if not match.group().isspace() and not match.group().startswith(('//', '/*'))]
    edits = []
    pending = 0
    for index, (token, start, end) in enumerate(tokens):
        if token != 'new':
            continue
        cursor = index + 1
        if cursor >= len(tokens):
            continue
        name = tokens[cursor][0]
        if name == 'net':
            qualified = ''.join(token[0] for token in tokens[cursor:cursor + 7])
            if qualified != 'net.minecraft.resources.ResourceLocation':
                continue
            cursor += 7
            name = qualified
        elif name == 'ResourceLocation':
            cursor += 1
        else:
            continue
        if cursor >= len(tokens) or tokens[cursor][0] != '(':
            continue
        prefix_end = tokens[cursor - 1][2]
        parens, brackets, braces, commas = 1, 0, 0, 0
        close = cursor + 1
        while close < len(tokens):
            value = tokens[close][0]
            if value == '(':
                parens += 1
            elif value == ')':
                parens -= 1
                if parens == 0:
                    break
            elif value == '[':
                brackets += 1
            elif value == ']':
                brackets -= 1
            elif value == '{':
                braces += 1
            elif value == '}':
                braces -= 1
            elif value == ',' and parens == 1 and brackets == 0 and braces == 0:
                commas += 1
            close += 1
        arguments = commas + 1 if close > cursor + 1 else 0
        if close == len(tokens) or arguments not in (1, 2) or (close + 1 < len(tokens) and tokens[close + 1][0] == '{'):
            pending += 1
            continue
        method = 'parse' if arguments == 1 else 'fromNamespaceAndPath'
        edits.append((start, prefix_end, name + '.' + method))
    for start, end, replacement in reversed(edits):
        source = source[:start] + replacement + source[end:]
    return source, len(edits), pending

def check_parser():
    examples = [
        ('new ResourceLocation("id")', 'ResourceLocation.parse("id")'),
        ('new ResourceLocation("ns", f(a,b))', 'ResourceLocation.fromNamespaceAndPath("ns", f(a,b))'),
        ('new ResourceLocation("ns", "path,with,commas")', 'ResourceLocation.fromNamespaceAndPath("ns", "path,with,commas")'),
        ('"new ResourceLocation(ignored)" // new ResourceLocation("skip")', '"new ResourceLocation(ignored)" // new ResourceLocation("skip")'),
        ('new net.minecraft.resources.ResourceLocation("ns", f(new ResourceLocation("inner"), b))', 'net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ns", f(ResourceLocation.parse("inner"), b))'),
        ('String s = """new ResourceLocation("text")""";', 'String s = """new ResourceLocation("text")""";'),
    ]
    for source, expected in examples:
        actual, _, _ = migrate(source)
        if actual != expected:
            raise AssertionError((source, actual, expected))
    print('ResourceLocation parser nested/quoted/comment cases: PASS')

if __name__ == '__main__':
    check_parser()
    changed, calls, pending = 0, 0, 0
    for path in sorted(pathlib.Path('src/main/java').rglob('*.java')):
        old = path.read_text()
        text, count, manual = migrate(old)
        calls += count
        pending += manual
        if text != old:
            path.write_text(text)
            changed += 1
    print('ResourceLocation factory migration files:', changed)
    print('ResourceLocation factory migration calls:', calls)
    print('Constructor forms requiring manual review:', pending)
