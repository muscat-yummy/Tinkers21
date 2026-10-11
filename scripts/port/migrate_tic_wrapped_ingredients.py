"""Wrap the ported BlockTagIngredient at existing native Ingredient call sites."""
import pathlib
from migrate_tic_resource_locations import TOKEN

def migrate(source):
    tokens = [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(source)
              if not m.group().isspace() and not m.group().startswith(('//', '/*'))]
    edits = []
    for index in range(len(tokens) - 2):
        if [t[0] for t in tokens[index:index + 3]] != ['new', 'BlockTagIngredient', '(']:
            continue
        depth, close = 1, index + 3
        while close < len(tokens):
            if tokens[close][0] == '(':
                depth += 1
            elif tokens[close][0] == ')':
                depth -= 1
                if depth == 0:
                    break
            close += 1
        if close == len(tokens):
            raise ValueError('Unclosed BlockTagIngredient constructor')
        following = [t[0] for t in tokens[close + 1:close + 3]]
        if following == ['.', 'toVanilla']:
            continue
        if following and following[0] == '{':
            raise ValueError('Anonymous BlockTagIngredient needs manual wrapper review')
        edits.append(tokens[close][2])
    for end in reversed(edits):
        source = source[:end] + '.toVanilla()' + source[end:]
    return source, len(edits)

def check_parser():
    examples = [
        ('new BlockTagIngredient(tag)', 'new BlockTagIngredient(tag).toVanilla()'),
        ('new BlockTagIngredient(f(a,b))', 'new BlockTagIngredient(f(a,b)).toVanilla()'),
        ('new BlockTagIngredient(tag).toVanilla()', 'new BlockTagIngredient(tag).toVanilla()'),
        ('"new BlockTagIngredient(tag)" // new BlockTagIngredient(tag)', '"new BlockTagIngredient(tag)" // new BlockTagIngredient(tag)'),
    ]
    for original, expected in examples:
        actual, _ = migrate(original)
        if actual != expected or migrate(actual)[0] != actual:
            raise AssertionError((original, actual, expected))
    print('Native ingredient wrapper parser/idempotence: PASS')

if __name__ == '__main__':
    check_parser()
    changed, calls = 0, 0
    own = pathlib.Path('src/main/java/slimeknights/tconstruct/library/recipe/ingredient/BlockTagIngredient.java')
    for path in sorted(pathlib.Path('src/main/java').rglob('*.java')):
        if path == own:
            continue
        old = path.read_text()
        text, count = migrate(old)
        if text != old:
            path.write_text(text)
            changed += 1
            calls += count
    print('Block-tag ingredient wrapper files:', changed)
    print('Block-tag ingredient wrapper calls:', calls)
