"""Create a small original EPUB for the installed-reader smoke test."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED, ZIP_STORED
path = Path('verification-results/lirelia-test.epub').resolve()
path.parent.mkdir(exist_ok=True)
with ZipFile(path, 'w', ZIP_DEFLATED) as z:
    z.writestr('mimetype', 'application/epub+zip', compress_type=ZIP_STORED)
    z.writestr('META-INF/container.xml', '''<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="book.opf" media-type="application/oebps-package+xml"/></rootfiles></container>''')
    z.writestr('book.opf', '''<?xml version="1.0"?><package version="3.0" unique-identifier="id" xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">lirelia-native-double-click-test</dc:identifier><dc:title>Lirelia Test</dc:title><dc:language>fr</dc:language><meta property="dcterms:modified">2026-10-21T00:00:00Z</meta></metadata><manifest><item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/></manifest><spine><itemref idref="chapter"/></spine></package>''')
    z.writestr('nav.xhtml', '''<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><ol><li><a href="chapter.xhtml">Test</a></li></ol></nav></body></html>''')
    z.writestr('chapter.xhtml', '''<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Test</title></head><body><h1>Lirelia Test</h1><p>Bonjour ! Il <em>disait</em> toujours la vérité. Ensuite, il partait.</p></body></html>''')
print(path)
