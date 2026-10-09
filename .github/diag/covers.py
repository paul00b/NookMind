"""Temporary diagnostic: which Google Books cover URLs come back as the "image not available" image."""
import hashlib
import json
import os
import struct
import urllib.parse
import urllib.request

KEY = os.environ.get("KEY", "")
QUERIES = [
    "Kafka sur le rivage Murakami",
    "Le Comte de Monte-Cristo Dumas",
    "La Croisee des chemins Thilliez",
    "Mes mains pleines de sang Askolovitch",
    "L'Anomalie Le Tellier",
    "Piranesi Susanna Clarke",
    "Les Bienveillantes Littell",
]


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=20) as r:
        return r.read(), r.headers.get("Content-Type")


def dims(b):
    if b[:8] == b"\x89PNG\r\n\x1a\n":
        return struct.unpack(">II", b[16:24])
    if b[:3] == b"GIF":
        return struct.unpack("<HH", b[6:10])
    if b[:2] == b"\xff\xd8":
        i = 2
        while i < len(b) - 9:
            if b[i] != 0xFF:
                i += 1
                continue
            m = b[i + 1]
            if m in (0xC0, 0xC1, 0xC2):
                h, w = struct.unpack(">HH", b[i + 5:i + 9])
                return (w, h)
            length = struct.unpack(">H", b[i + 2:i + 4])[0]
            i += 2 + length
    return None


out = []
os.makedirs("screenshots/diag", exist_ok=True)
for q in QUERIES:
    data, _ = get("https://www.googleapis.com/books/v1/volumes?" + urllib.parse.urlencode({"q": q, "maxResults": 3, "key": KEY}))
    for item in json.loads(data).get("items", []):
        vi = item["volumeInfo"]
        links = vi.get("imageLinks", {})
        line = {"q": q, "id": item["id"], "title": vi.get("title"), "links": links}
        base = links.get("thumbnail") or links.get("smallThumbnail")
        if base:
            base = base.replace("http://", "https://")
            for z in ["1", "2", "3"]:
                u = base.replace("zoom=1", "zoom=" + z).replace("zoom=5", "zoom=" + z)
                try:
                    b, ct = get(u)
                    ext = "png" if b[:4] == b"\x89PNG" else ("gif" if b[:3] == b"GIF" else "jpg")
                    with open("screenshots/diag/%s-z%s.%s" % (item["id"], z, ext), "wb") as f:
                        f.write(b)
                    line["z" + z] = {"size": len(b), "md5": hashlib.md5(b).hexdigest()[:12], "dims": dims(b), "type": ct}
                except Exception as e:  # noqa: BLE001
                    line["z" + z] = str(e)
        out.append(line)

with open("screenshots/diag/report.json", "w") as f:
    json.dump(out, f, indent=1, ensure_ascii=False)
print(json.dumps(out, indent=1, ensure_ascii=False))
