import zipfile
import xml.etree.ElementTree as ET

def extract_text_from_docx(docx_path):
    try:
        with zipfile.ZipFile(docx_path, 'r') as docx:
            xml_content = docx.read('word/document.xml')
            tree = ET.fromstring(xml_content)
            # The namespace for Word XML
            namespaces = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            paragraphs = tree.findall('.//w:p', namespaces)
            
            text = []
            for p in paragraphs:
                runs = p.findall('.//w:r/w:t', namespaces)
                text.append(''.join(run.text for run in runs if run.text))
            
            return '\n'.join(text)
    except Exception as e:
        return str(e)

if __name__ == '__main__':
    print("----- SRS -----")
    srs = extract_text_from_docx('/home/berard/disk2/DevOps/marketsphere/IRAKOZE MURASIRA Berard Submission Y3C /CSS-SRS-2026-001_v1.0.docx')
    print(srs[:2000]) # First 2000 chars
