"""Promote only the exact successful staging distribution; never rebuild or replace published jars."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile

REPO=os.environ['GITHUB_REPOSITORY']
RUN=38166181835
SOURCE='bc038bec18c9e48b611071c342d5fc12547e75ba'
AUDIT_RUN=38166095246
AUDIT_SOURCE='644bf6b8d517d1a411a4316ec18e9ccf93867afb'
VERSION='3.12.1-port.8'
TAG='v'+VERSION

def api(endpoint):
    return json.loads(subprocess.check_output(['gh','api','repos/'+REPO+'/'+endpoint],text=True))

def run(*args):
    subprocess.run(args,check=True)

for number,source in [(RUN,SOURCE),(AUDIT_RUN,AUDIT_SOURCE)]:
    accepted=api('actions/runs/'+str(number))
    assert accepted['conclusion']=='success' and accepted['head_sha']==source
run('git','fetch','--no-tags','origin',SOURCE)
run('git','diff','--exit-code',SOURCE,'HEAD','--','src/main','src/generated','mantle-port',
    'gradle.properties','build.gradle','settings.gradle','gradle','gradlew','gradlew.bat',
    'src/portAddonCheck','src/portGameTest','src/portClientCheck','scripts/port/recipe-qa')
destination=Path('accepted-distribution')
run('gh','run','download',str(RUN),'--repo',REPO,'--name','Tinkers21-'+VERSION+'-distribution','--dir',str(destination))
dist=destination/'dist'
expected={
    'Tinkers21-1.21.1-neoforge-'+VERSION+'.jar',
    'Mantle21-1.21.1-neoforge-1.11-port.5.jar',
    'Tinkers21-1.21.1-neoforge-'+VERSION+'.zip',
}
assert {p.name for p in dist.iterdir()}==expected
hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in dist.iterdir()}
evidence=json.loads((destination/'build/release-evidence/provenance.json').read_text())
assert evidence['source_commit']==SOURCE and int(evidence['packaging_run'])==RUN
assert evidence['version']==VERSION and evidence['minimum_neoforge']=='21.1.249'
assert all(hashes[name]==digest for name,digest in evidence['jars'].items())
with zipfile.ZipFile(dist/'Mantle21-1.21.1-neoforge-1.11-port.5.jar') as mantle:
    names=set(mantle.namelist())
    assert {'data/mantle/tags/fluid/'+name+'.json' for name in ['water','lava','soup']} <= names
    assert 'data/mantle/tags/block/gauges.json' in names
    assert not any(name.startswith(('data/mantle/tags/fluids/','data/mantle/tags/blocks/')) for name in names)
with zipfile.ZipFile(dist/('Tinkers21-1.21.1-neoforge-'+VERSION+'.jar')) as tic:
    config=json.loads(tic.read('tconstruct.mixins.json'))
    for name in ['EmiShapedRemainderMixin','EmiTextWidgetMixin','EmiScrollGridWidgetMixin','EmiRecipeIdentityMixin']:
        assert name in config['client'] and name not in config['mixins']
        assert 'slimeknights/tconstruct/mixin/'+name+'.class' in tic.namelist()
checksums=(destination/'build/release-evidence/SHA256SUMS.txt').read_text()
assert checksums==''.join(hashes[name]+'  '+name+'\n' for name in sorted(expected))
notes=destination/'docs/releases'/ (VERSION+'.md')
body=notes.read_text()
assert '### SHA256' in body and checksums in body and 'English' in body
bundle=dist/('Tinkers21-1.21.1-neoforge-'+VERSION+'.zip')
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    assert set(archive.namelist())=={'mods/'+name for name in expected if name.endswith('.jar')} | {'LICENSE-Tinkers21.txt','LICENSE-Mantle.txt','INSTALL.md'}
    for name in expected:
        if name.endswith('.jar'):
            assert hashlib.sha256(archive.read('mods/'+name)).hexdigest()==hashes[name]
# An existing published version must be identical. Never upload with --clobber.
matches=[release for release in api('releases?per_page=100') if release['tag_name']==TAG]
assert len(matches)<=1
if not matches:
    run('gh','release','create',TAG,*[str(dist/name) for name in sorted(expected)],'--repo',REPO,
        '--target',SOURCE,'--title','Tinkers21 '+VERSION,'--notes-file',str(notes),'--draft','--latest=false')
# The tag endpoint is unavailable until a draft's tag is created at publication.
# Authenticated release listing exposes the draft and lets us validate by release ID.
release=next(release for release in api('releases?per_page=100') if release['tag_name']==TAG)
assert {a['name'] for a in release['assets']}==expected
for asset in release['assets']:
    assert asset['digest']=='sha256:'+hashes[asset['name']]
assert release['body']==body and not release['prerelease']
assert release['target_commitish']==SOURCE
if release['draft']:
    subprocess.run(['gh','api','--method','PATCH','repos/'+REPO+'/releases/'+str(release['id']),'--input','-'],
        input=json.dumps({'draft':False,'make_latest':'true'}),text=True,stdout=subprocess.DEVNULL,check=True)
assert api('releases/latest')['id']==release['id']
print('Accepted staging distribution published as Latest with exactly three assets: '+TAG)
