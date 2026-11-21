import os
from docx import Document

# txt 파일이 있는 디렉토리
doc_dir = "infra/seed/docs"

for filename in os.listdir(doc_dir):
    if filename.endswith(".txt"):
        txt_path = os.path.join(doc_dir, filename)
        docx_path = os.path.join(doc_dir, filename.replace(".txt", ".docx"))
        
        doc = Document()
        with open(txt_path, "r", encoding="utf-8") as f:
            for line in f:
                doc.add_paragraph(line.strip())
        
        doc.save(docx_path)
        print(f"✅ 변환 완료: {docx_path}")