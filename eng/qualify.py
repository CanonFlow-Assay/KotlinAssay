#!/usr/bin/env python3
"""Execute bounded adversarial qualification on generated specimens only."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--nalan', type=Path, default=ROOT.parent.parent / 'Nalan/assay/dogfood', help='synthetic Nalan specimen root')
parser.add_argument('--evidence-dir', type=Path, default=ROOT / 'evidence', help='destination for fresh receipts; use a separate directory to retain historical evidence')
args = parser.parse_args()
NALAN = args.nalan.resolve()
CASES = ROOT / 'build/qualification'
EVIDENCE = args.evidence_dir.resolve()
if not (NALAN / '.kotlinassay.json').is_file():
    parser.error('Nalan specimen requires .kotlinassay.json; supply --nalan')
CASES.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(parents=True, exist_ok=True)
BASE_POLICY = json.loads((NALAN / '.kotlinassay.json').read_text())

def fresh(name, addition='', config=None):
    target = CASES / name
    target.mkdir(exist_ok=True)
    shutil.copytree(NALAN / 'domain', target / 'domain', dirs_exist_ok=True)
    shutil.copytree(NALAN / 'tests', target / 'tests', dirs_exist_ok=True)
    (target / 'domain/Custodian.kt').write_text((NALAN / 'domain/Custodian.kt').read_text() + addition)
    (target / '.kotlinassay.json').write_text(json.dumps(BASE_POLICY if config is None else config, indent=2) + '\n')
    return target

def run(name, mode, target, expected, rule=None):
    process = subprocess.run([str(ROOT / 'kotlin-assay'), mode, str(target)], capture_output=True, text=True, timeout=120)
    try:
        receipt = json.loads(process.stdout.strip().splitlines()[-1])
    except (ValueError, IndexError):
        raise AssertionError(name + ': no valid receipt\n' + process.stderr[-1500:])
    assert process.returncode == expected, (name, process.returncode, expected, receipt)
    if rule:
        assert rule in [f['rule'] for f in receipt.get('findings', [])], (name, rule, receipt)
    (EVIDENCE / (name + '.json')).write_text(json.dumps(receipt, sort_keys=True, indent=2) + '\n')
    print(name + ': ' + receipt['verdict'], flush=True)
    return {'name': name, 'exitCode': process.returncode, 'verdict': receipt['verdict'], 'receiptSha256': hashlib.sha256((EVIDENCE / (name + '.json')).read_bytes()).hexdigest()}

RESULTS = [run('nalan-domain', 'verify', NALAN, 0), run('tool-core', 'verify', ROOT, 0)]
RESULTS += [run('scan-not-verification', 'scan', NALAN, 2)]
RESULTS += [run('repeatable-scan', 'scan', NALAN, 2)]
assert (EVIDENCE / 'scan-not-verification.json').read_bytes() == (EVIDENCE / 'repeatable-scan.json').read_bytes()
RESULTS += [run('reject-mutation', 'verify', fresh('mutation', '\nvar hiddenState = 0\n'), 1, 'KFA001')]
RESULTS += [run('reject-oop', 'verify', fresh('oop', '\nclass CustodianService { fun next() = 1 }\n'), 1, 'KFA004')]
RESULTS += [run('reject-loop', 'verify', fresh('loop', '\nfun imperative(): Int { for (i in 1..3) println(i); return 0 }\n'), 1, 'KFA003')]
RESULTS += [run('reject-hidden-effects', 'verify', fresh('effects', '\nfun disguised(x: Int) = x.also { println(it) }\n'), 1, 'KFA008')]
RESULTS += [run('reject-boundary-call', 'verify', fresh('boundary', '\nfun leak() = nalan.assay.tests.main()\n'), 1, 'KFA015')]
bad_source = fresh('invalid-source', '\nfun broken( =\n')
RESULTS += [run('invalid-source-not-pass', 'verify', bad_source, 2)]
unclassified = fresh('unclassified')
(unclassified / 'Forgotten.kt').write_text('class Forgotten\n')
RESULTS += [run('unclassified-not-pass', 'verify', unclassified, 2)]
empty = dict(BASE_POLICY, requiredCommands=[])
RESULTS += [run('missing-test-not-pass', 'verify', fresh('missing-test', config=empty), 2)]
failed = dict(BASE_POLICY, requiredCommands=[['python3', '-c', 'raise SystemExit(7)']])
RESULTS += [run('failed-command-fails', 'verify', fresh('failed-command', config=failed), 1)]
unavailable = dict(BASE_POLICY, requiredCommands=[['missing-kotlinassay-command']])
RESULTS += [run('unavailable-command-not-pass', 'verify', fresh('unavailable', config=unavailable), 2)]
drift = dict(BASE_POLICY, requiredCommands=[['python3', '-c', "from pathlib import Path; p=Path('domain/Custodian.kt'); p.write_text(p.read_text()+'\\n// changed after inspection\\n')"]])
RESULTS += [run('source-drift-not-pass', 'verify', fresh('source-drift', config=drift), 2)]
bad_policy = fresh('invalid-policy', config=dict(BASE_POLICY, profile='unreviewed'))
RESULTS += [run('invalid-policy-tool-failure', 'verify', bad_policy, 3)]
missing = CASES / 'no-policy'
missing.mkdir(exist_ok=True)
RESULTS += [run('missing-policy-not-pass', 'verify', missing, 2)]

summary={'status':'Passed','scope':'Local strict-syntax profile and synthetic Nalan domain only','cases':RESULTS,'nonclaims':['No Android application/device/alarm qualification','No Bluetooth radio/background qualification','No Garmin/Tapo history analyzed','No universal purity or semantic effect proof']}
(EVIDENCE / 'qualification.json').write_text(json.dumps(summary, indent=2, sort_keys=True) + '\n')
print('Qualification cases passed:',len(RESULTS),flush=True)
