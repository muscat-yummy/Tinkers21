from pathlib import Path
import shutil
root=Path('src/portClientCheck/java/slimeknights/tconstruct/port')
shutil.copyfile('scripts/port/recipe-qa/RecipeViewerCheck.java',root/'RecipeViewerCheck.java')
p=root/'ClientBootstrapCheck.java';t=p.read_text()
old='''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      minecraft.stop();
    }'''
new='''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      return;
    }
    if (step == 23 && RecipeViewerCheck.tick(minecraft)) {
      advance(24);
      minecraft.stop();
    }'''
assert t.count(old)==1
p.write_text(t.replace(old,new).replace('if (step == 23) return;','if (step == 24) return;'))
p=Path('build.gradle');p.write_text(p.read_text()+'''
// Isolated CI fixture only, never persisted in the production build configuration.
dependencies {
    runtimeOnly 'mezz.jei:jei-1.21.1-neoforge:19.51.0.420'
    portClientCheckImplementation('maven.modrinth:emi:5sIPA1To') { transitive = false }
}
''')
