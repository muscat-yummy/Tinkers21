from pathlib import Path
import shutil

root = Path('src/portClientCheck/java/slimeknights/tconstruct/port')
shutil.copyfile('scripts/port/qa/BookExplorationCheck.java', root / 'BookExplorationCheck.java')
path = root / 'ClientBootstrapCheck.java'
source = path.read_text()
old = '''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      minecraft.stop();
    }'''
new = '''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      return;
    }
    if (step == 23 && BookExplorationCheck.tick(minecraft)) {
      advance(24);
      minecraft.stop();
    }'''
assert source.count(old) == 1 and source.count('if (step == 23) return;') == 1
path.write_text(source.replace(old, new).replace('if (step == 23) return;', 'if (step == 24) return;'))
