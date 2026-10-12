from pathlib import Path
import os
import shutil
root=Path('src/portClientCheck/java/slimeknights/tconstruct/port')
shutil.copyfile('scripts/port/emi-native/NativeEmiCheck.java',root/'NativeEmiCheck.java')
p=root/'ClientBootstrapCheck.java';t=p.read_text()
old='''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      minecraft.stop();
    }'''
new='''    if (step == 22 && BookIconRenderingCheck.tick(minecraft)) {
      advance(23);
      return;
    }
    if (step == 23 && NativeEmiCheck.tick(minecraft)) {
      advance(24);
      minecraft.stop();
    }'''
assert t.count(old)==1
p.write_text(t.replace(old,new).replace('if (step == 23) return;','if (step == 24) return;'))
p=Path('build.gradle');p.write_text(p.read_text()+'''
// Isolated CI fixture only, never persisted in the production build configuration.
dependencies {
    portClientCheckImplementation('maven.modrinth:emi:5sIPA1To') { transitive = false }
}
''')

if os.environ.get('EMI_CHECK_JEI')=='true':
    p.write_text(p.read_text()+"\ndependencies { runtimeOnly 'mezz.jei:jei-1.21.1-neoforge:19.51.0.420' }\n")
    p.write_text(p.read_text()+"\nneoForge.runs.portClientCheck.systemProperty 'tconstruct.emiCheck.jei', 'true'\n")
