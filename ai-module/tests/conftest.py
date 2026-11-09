"""테스트 공용 준비물 (D 소유).
 
테스트용 PDF·DOCX를 실행할 때마다 코드로 만든다.
그래서 바이너리 파일을 저장소에 올리지 않아도 되고, 내용이 무엇인지 코드에서 바로 보인다.
"""
import io
import zipfile
 
import pytest


def build_pdf(pages: list[str]) -> bytes:
    """페이지마다 영어 한 줄이 들어간 아주 작은 PDF를 만든다. 빈 문자열이면 글자 없는 페이지."""
    objs: list[bytes] = []
    n_pages = len(pages)
    page_ids = [4 + i * 2 for i in range(n_pages)]  # 1 catalog, 2 pages, 3 font, 그다음 (page, content) 쌍
 
    objs.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    kids = " ".join(f"{pid} 0 R" for pid in page_ids)
    objs.append(f"<< /Type /Pages /Kids [{kids}] /Count {n_pages} >>".encode())
    objs.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    for i, text in enumerate(pages):
        content_id = page_ids[i] + 1
        objs.append(
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] "
            f"/Resources << /Font << /F1 3 0 R >> >> /Contents {content_id} 0 R >>".encode()
        )
        stream = f"BT /F1 12 Tf 20 100 Td ({text}) Tj ET".encode() if text else b""
        objs.append(b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream")
 
    out = io.BytesIO()
    out.write(b"%PDF-1.4\n")
    offsets = []
    for i, body in enumerate(objs, start=1):
        offsets.append(out.tell())
        out.write(f"{i} 0 obj\n".encode() + body + b"\nendobj\n")
    xref = out.tell()
    out.write(f"xref\n0 {len(objs) + 1}\n0000000000 65535 f \n".encode())
    for off in offsets:
        out.write(f"{off:010d} 00000 n \n".encode())
    out.write(f"trailer\n<< /Size {len(objs) + 1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode())
    return out.getvalue()
 