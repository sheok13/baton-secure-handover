"""테스트 공용 준비물 (D 소유).
 
테스트용 PDF·DOCX를 실행할 때마다 코드로 만든다.
그래서 바이너리 파일을 저장소에 올리지 않아도 되고, 내용이 무엇인지 코드에서 바로 보인다.
"""
import io
import zipfile
 
import pytest