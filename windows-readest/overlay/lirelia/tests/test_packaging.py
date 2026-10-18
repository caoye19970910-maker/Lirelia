import json
import unittest
from pathlib import Path


class PackagingTests(unittest.TestCase):
    def test_desktop_config_activates_native_learning_permissions(self):
        tauri = Path(__file__).resolve().parents[2] / 'apps/readest-app/src-tauri'
        base = json.loads((tauri / 'tauri.conf.json').read_text())
        overlay = json.loads((tauri / 'tauri.lirelia.conf.json').read_text())
        capability = json.loads((tauri / 'capabilities/lirelia.json').read_text())
        security = {**base['app']['security'], **overlay.get('app', {}).get('security', {})}
        self.assertIn(capability['identifier'], security['capabilities'],
                      'The installer must activate its sidecar permissions.')
        self.assertTrue(set(base['app']['security']['capabilities']).issubset(security['capabilities']))
