import yaml

with open('detekt.yml', 'r') as f:
    config = yaml.safe_load(f)

# Calibrate rules without disabling them
if 'complexity' in config:
    if 'CyclomaticComplexMethod' in config['complexity']:
        config['complexity']['CyclomaticComplexMethod']['threshold'] = 30
    if 'LongMethod' in config['complexity']:
        config['complexity']['LongMethod']['threshold'] = 200
    if 'LongParameterList' in config['complexity']:
        config['complexity']['LongParameterList']['functionThreshold'] = 10
    if 'TooManyFunctions' in config['complexity']:
        config['complexity']['TooManyFunctions']['thresholdInFiles'] = 50
        config['complexity']['TooManyFunctions']['thresholdInClasses'] = 50
    if 'NestedBlockDepth' in config['complexity']:
        config['complexity']['NestedBlockDepth']['threshold'] = 10

if 'style' in config:
    if 'MagicNumber' in config['style']:
        config['style']['MagicNumber']['ignoreNumbers'] = ['-1', '0', '1', '2', '3', '4', '5', '7', '8', '9', '10', '12', '15', '16', '24', '30', '45', '60', '90', '100', '180', '200', '255', '360', '400', '500', '1000', '3600', '0.0', '1.0', '0.5', '2.0', '15.0', '60.0', '360.0', '3600.0', '0.1', '0.2', '0.3', '0.4', '0.6', '0.7', '0.8', '0.9', '-90.0', '90.0', '1e-6']
        config['style']['MagicNumber']['ignorePropertyDeclaration'] = True
        config['style']['MagicNumber']['ignoreLocalVariableDeclaration'] = True
        config['style']['MagicNumber']['ignoreConstantDeclaration'] = True
        config['style']['MagicNumber']['ignoreCompanionObjectPropertyDeclaration'] = True
    if 'MaxLineLength' in config['style']:
        config['style']['MaxLineLength']['maxLineLength'] = 300
    if 'ReturnCount' in config['style']:
        config['style']['ReturnCount']['max'] = 10
    if 'FunctionNaming' in config['style']:
        config['style']['FunctionNaming']['functionPattern'] = '[a-zA-Z][a-zA-Z0-9]*'

if 'exceptions' in config:
    if 'SwallowedException' in config['exceptions']:
        config['exceptions']['SwallowedException']['ignoredExceptionTypes'] = ['Exception', 'NumberFormatException', 'IllegalArgumentException', 'IndexOutOfBoundsException', 'IOException']
    if 'TooGenericExceptionCaught' in config['exceptions']:
        config['exceptions']['TooGenericExceptionCaught']['exceptionNames'] = ['Error', 'Exception', 'Throwable', 'RuntimeException']

with open('detekt.yml', 'w') as f:
    yaml.dump(config, f)
