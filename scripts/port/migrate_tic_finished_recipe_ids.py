"""Give raw finished-recipe output the explicit ID removed from native Recipe."""
import pathlib
from migrate_tic_resource_locations import TOKEN

def tokens_for(source):
    return [(m.group(), m.start(), m.end()) for m in TOKEN.finditer(source)
            if not m.group().isspace() and not m.group().startswith(("//", "/*"))]

def arguments(tokens, opening):
    depth = 0
    start = opening + 1
    result = []
    for i in range(opening, len(tokens)):
        value = tokens[i][0]
        if value in ("(", "[", "{"):
            depth += 1
        elif value in (")", "]", "}"):
            depth -= 1
            if depth == 0:
                result.append((start, i))
                return result, i
        elif value == "," and depth == 1:
            result.append((start, i))
            start = i + 1
    return None, None

def migrate(source):
    tokens = tokens_for(source)
    edits, pending = [], 0
    for i, (value, start, end) in enumerate(tokens):
        if value != "LoadableFinishedRecipe" or i == 0 or tokens[i-1][0] != "new":
            continue
        opening = i + 1
        if opening < len(tokens) and tokens[opening][0] == "<":
            while opening < len(tokens) and tokens[opening][0] != ">":
                opening += 1
            opening += 1
        if opening >= len(tokens) or tokens[opening][0] != "(":
            pending += 1
            continue
        args, close = arguments(tokens, opening)
        if args is None:
            pending += 1
            continue
        if len(args) == 4:
            continue
        if len(args) != 3:
            pending += 1
            continue
        first_start, first_end = args[0]
        # Only adapt observed builders whose concrete recipe constructor already receives the same explicit id.
        first = tokens[first_start:first_end]
        if not first or not (first[0][0] == "new" or (len(first) > 2 and first[1][0] == "." and first[2][0] == "apply")):
            pending += 1
            continue
        inner_open = next((n for n in range(first_start, first_end) if tokens[n][0] == "("), None)
        if inner_open is None:
            pending += 1
            continue
        inner_args, inner_close = arguments(tokens, inner_open)
        if not inner_args:
            pending += 1
            continue
        id_start, id_end = inner_args[0]
        if id_end - id_start != 1 or tokens[id_start][0] != "id":
            pending += 1
            continue
        edits.append((tokens[first_start][1], "id, "))
    for position, text in reversed(edits):
        source = source[:position] + text + source[position:]
    return source, len(edits), pending

def check_parser():
    cases = [
        ("new LoadableFinishedRecipe<>(new Recipe(id, inputs), LOADER, advancementId)",
         "new LoadableFinishedRecipe<>(id, new Recipe(id, inputs), LOADER, advancementId)"),
        ("new LoadableFinishedRecipe<>(constructor.apply(id, f(a,b)), LOADER, advancementId)",
         "new LoadableFinishedRecipe<>(id, constructor.apply(id, f(a,b)), LOADER, advancementId)"),
        ("new LoadableFinishedRecipe<>(id, new Recipe(id, inputs), LOADER, advancementId)",
         "new LoadableFinishedRecipe<>(id, new Recipe(id, inputs), LOADER, advancementId)"),
        ('"new LoadableFinishedRecipe<>(new Recipe(id), LOADER, null)"',
         '"new LoadableFinishedRecipe<>(new Recipe(id), LOADER, null)"'),
        ("// new LoadableFinishedRecipe<>(new Recipe(id), LOADER, null)",
         "// new LoadableFinishedRecipe<>(new Recipe(id), LOADER, null)"),
        ("new LoadableFinishedRecipe<>(recipe, LOADER, advancementId)",
         "new LoadableFinishedRecipe<>(recipe, LOADER, advancementId)"),
    ]
    for source, expected in cases:
        updated, _, _ = migrate(source)
        if updated != expected or migrate(updated)[0] != updated:
            raise AssertionError((source, updated, expected))
    print("Explicit finished recipe ID nested-call/string/comment/idempotence cases: PASS")

if __name__ == "__main__":
    check_parser()
    files, total, pending = 0, 0, 0
    for path in sorted(pathlib.Path("src/main/java").rglob("*.java")):
        original = path.read_text()
        updated, count, manual = migrate(original)
        pending += manual
        if updated != original:
            path.write_text(updated)
            files += 1
            total += count
    print("Explicit finished recipe ID files:", files)
    print("Explicit finished recipe ID calls:", total)
    print("Finished recipe ID calls requiring review:", pending)
