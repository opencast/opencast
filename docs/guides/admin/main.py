import pathlib

def define_env(env):
    'Hook function'

    @env.macro
    def opencast_major_version():
        '''Get Opencast version from main pom.xml
        '''
        pom_file = str(pathlib.Path(__file__).parent.resolve()) + '/../../../pom.xml'
        line = ''
        with open(pom_file, 'r') as pom:
            while not line.startswith('  <version>'):
                line = pom.readline()
        return line.split('>')[1].split('<')[0].split('.')[0].split('-')[0]
