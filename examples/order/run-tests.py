#!/usr/bin/env python3
"""Execute the example already compiled by kotlin-assay verify."""
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parent
lock = json.loads((root.parents[1] / 'toolchain.lock.json').read_text())
classpath = ':'.join([str(root / '.kotlinassay/compiled.jar'),
    str(Path(lock['compilerHome']) / 'lib/kotlin-stdlib.jar'), lock['persistentCollectionsJar']])
raise SystemExit(subprocess.run(['java', '-cp', classpath,
    'kotlinassay.examples.orders.tests.OrderTestsKt']).returncode)
