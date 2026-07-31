import os
import re

def fix_file(path):
    with open(path, 'r') as f:
        content = f.read()
    
    # Fix NewLineAtEndOfFile
    if not content.endswith('\n'):
        content += '\n'
    
    # Fix WildcardImport
    if 'org.junit.Assert.*' in content:
        content = content.replace('import org.junit.Assert.*', 'import org.junit.Assert.assertEquals\nimport org.junit.Assert.assertNotNull\nimport org.junit.Assert.assertNull')
        
    with open(path, 'w') as f:
        f.write(content)

for root, dirs, files in os.walk('app/src'):
    for file in files:
        if file.endswith('.kt') or file.endswith('.java'):
            fix_file(os.path.join(root, file))
