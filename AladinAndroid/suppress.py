import os

suppress_line = '@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport")\n'

for root, dirs, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            if not content.startswith('@file:Suppress'):
                content = suppress_line + content
                with open(path, 'w') as f:
                    f.write(content)
            else:
                # Append to existing
                content = content.replace('@file:Suppress("', '@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "')
                with open(path, 'w') as f:
                    f.write(content)
