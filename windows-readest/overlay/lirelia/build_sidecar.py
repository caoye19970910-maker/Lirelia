"""Build the sidecar for the current host. Windows EXEs must be built on Windows."""
from pathlib import Path
import platform
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
if shutil.which('rustc'):
    host = subprocess.check_output(['rustc', '-vV'], text=True)
    target = next(line.split(': ', 1)[1] for line in host.splitlines() if line.startswith('host:'))
else:
    architecture = 'aarch64' if platform.machine().lower() in ('arm64', 'aarch64') else 'x86_64'
    suffix = {'Windows': 'pc-windows-msvc', 'Darwin': 'apple-darwin', 'Linux': 'unknown-linux-gnu'}[platform.system()]
    target = f'{architecture}-{suffix}'
extension = '.exe' if platform.system() == 'Windows' else ''
output = root / 'lirelia' / 'build'
subprocess.run([sys.executable, '-m', 'PyInstaller', '--noconfirm', '--clean', '--onefile',
    '--name', 'lirelia-core', '--distpath', str(output / 'dist'), '--workpath', str(output / 'work'),
    '--specpath', str(output), '--collect-all', 'keyring', '--copy-metadata', 'fsrs',
    '--exclude-module', 'fsrs.optimizer', '--exclude-module', 'numpy', '--exclude-module', 'scipy',
    '--exclude-module', 'pandas', '--exclude-module', 'matplotlib',
    '--collect-all', 'gtts', '--paths', str(root / 'lirelia/core'),
    str(root / 'lirelia/core/bridge.py')], check=True, cwd=root)
destination = root / 'apps/readest-app/src-tauri/binaries' / f'lirelia-core-{target}{extension}'
destination.parent.mkdir(exist_ok=True)
shutil.copy2(output / 'dist' / f'lirelia-core{extension}', destination)
print(f'Built Lirelia sidecar: {destination}')
