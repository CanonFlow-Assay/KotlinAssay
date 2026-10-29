#!/usr/bin/env python3
"""Run the same expanded acceptance harness compiled by KotlinAssay verify."""
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parent
lock = json.loads((root.parents[1] / "toolchain.lock.json").read_text())
classpath = ":".join((str(root / ".kotlinassay/compiled.jar"),
    str(Path(lock["compilerHome"]) / "lib/kotlin-stdlib.jar")))
raise SystemExit(subprocess.run(["java", "-cp", classpath,
    "playground.expenseguard.tests.MainKt", "expanded"]).returncode)
