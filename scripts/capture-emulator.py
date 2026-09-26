"""Capture actual app screens on a running test emulator, with no fixture metrics."""
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(['adb', *args])


def tap_label(label):
    for _ in range(6):
        adb('shell', 'uiautomator', 'dump', '/sdcard/ember-ui.xml')
        root = ET.fromstring(adb('shell', 'cat', '/sdcard/ember-ui.xml'))
        for node in root.iter('node'):
            if node.get('text', '').casefold() == label.casefold() or node.get('content-desc', '') == label:
                bounds = [int(n) for n in re.findall(r'\d+', node.attrib['bounds'])]
                x, y = (bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2
                adb('shell', 'input', 'tap', str(x), str(y))
                time.sleep(1)
                return
        time.sleep(1)
    raise RuntimeError(f'Could not find {label!r} in app UI')


adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
adb('shell', 'am', 'start', '-W', '-n', 'in.akhilesh.ember/.MainActivity')
time.sleep(2)
tap_label("Let's begin")
output = Path('app/build/screenshots')
output.mkdir(parents=True, exist_ok=True)
for tab in ['Launch', 'Network', 'Profiles', 'Journal', 'More']:
    tap_label(tab + ' tab')
    (output / (tab.lower() + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
print('Captured all five native screens.')
