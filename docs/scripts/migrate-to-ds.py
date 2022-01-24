#!/usr/bin/env python3

import glob
import xml.etree.ElementTree as ET
import os

def _insert_position(position, list1, list2):
    return list1[:position] + list2 + list1[position:]

def _activate(lines, function):
    search_activate = 'void ' + function + '('
    l = 0
    for line in lines:
        if search_activate in line:
            print('activate', l, line)
            lines = _insert_position(l, lines, ['  @Activate\n'])
            break;
        l += 1
    if l == 0:
        print('MIGFAILED - ACTIVATE METHOD NOT FOUND!')
        quit()
    return lines

def _deactivate(lines, function):
    search_deactivate = 'void ' + function + '('
    l = 0
    for line in lines:
        if search_deactivate in line:
            print('deactivate', l, line)
            lines = _insert_position(l, lines, ['  @Deactivate\n'])
            break;
        l += 1
    if l == 0:
        print('MIGFAILED - DEACTIVATE METHOD NOT FOUND!')
        quit()
    return lines

def _modified(lines, function):
    search_deactivate = 'void ' + function + '('
    l = 0
    for line in lines:
        if search_deactivate in line:
            print('modified', l, line)
            lines = _insert_position(l, lines, ['  @Modified\n'])
            break;
        l += 1
    if l == 0:
        print('MIGFAILED - MODIFIED METHOD NOT FOUND!')
        quit()
    return lines

def _imports(lines, imports):
    # fix import order with your ide
    l = 0
    orgo = False
    for lin in lines:
        if 'package org.opencastproject' in lin:
            orgo = True
            l += 1
            continue

        if not orgo:
            l += 1
            continue

        lines = _insert_position(l,lines,['\n'])
        lines = _insert_position(l + 1, lines, imports)
        break

    return lines

def _references(lines, reference, imports):
    #<reference name="oaiPmhPublicationService"
    #interface="org.opencastproject.publication.api.OaiPmhPublicationService"
    #cardinality="1..1"
    #policy="static"
    #target="(artifact=feed)
    #bind="setOaiPmhPublicationService"/>
    #@Reference(name = "metadata", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, unbind = "removeMetadataService")

    search_method = reference.attrib['bind'] + '(' + reference.attrib['interface'].split('.')[-1]

    jref = ['  @Reference(\n']
    jref.append('      name = "' + reference.attrib['name'] + '",\n')

    if 'cardinality' in reference.attrib:
        car = reference.attrib['cardinality']
        if car == '0..1':
            jref.append('      cardinality = ReferenceCardinality.OPTIONAL,\n')
        elif car == '0..n':
            jref.append('      cardinality = ReferenceCardinality.MULTIPLE,\n')
        elif car == '1..n':
            jref.append('      cardinality = ReferenceCardinality.AT_LEAST_ONE,\n')
        elif car == '1..1':
            #default
            pass
        else:
            print('UNKNOWN CARDINALITY',pol)
            quit()
        del reference.attrib['cardinality']


    if 'policy' in reference.attrib:
        pol = reference.attrib['policy']
        if pol == 'static':
            jref.append('      policy = ReferencePolicy.STATIC,\n')
        elif pol == 'dynamic':
            jref.append('      policy = ReferencePolicy.DYNAMIC,\n')
        else:
            print('UNKNOWN POLICY',pol)
            quit()
        del reference.attrib['policy']

    del reference.attrib['bind']

    if 'unbind' in reference.attrib:
        jref.append('      unbind = "' + reference.attrib['unbind'] + '",\n')
        del reference.attrib['unbind']
    if 'target' in reference.attrib:
        jref.append('      target = "' + reference.attrib['target'] + '",\n')
        del reference.attrib['target']

    i = jref[-1].rfind(",")
    jref[-1] = jref[-1][:i] + jref[-1][i+1:]
    jref.append('  )\n')

    if len(jref) == 3:
        jref = [ jref[0].rstrip() + jref[1].strip() + jref[2].strip() + '\n']

    l = 0
    for line in lines:
        if search_method in line:
            print('method', l, line)
            lines = _insert_position(l, lines, jref)
            break;
        l += 1
    if l == 0:
        print('MIGFAILED - REFERENCE SET METHOD NOT FOUND!')
        quit()
    del reference.attrib['name']
    del reference.attrib['interface']
    return lines

def _component(component):
    tmp = ['@Component(\n']
    if 'immediate' in component.attrib:
        tmp.append('    immediate = ' + component.attrib['immediate'] + ',\n')
        del component.attrib['immediate']

    if component.attrib['name'] != component.find('implementation').attrib['class']:
        tmp.append('    name = "' + component.attrib['name'] + '",\n')

    del component.attrib['name']
    component.remove(component.find('implementation'))

    if component.find('service'):
        provide = []
        for child in component.find('service').findall('provide'):
            provide.append(child.attrib['interface'].split('.')[-1] + '.class')

        if len(provide) > 1:
            tmp.append('    service = { ' + ','.join(provide) + ' },\n')
        else:
            tmp.append('    service = ' + provide[0] + ',\n')
        component.remove(component.find('service'))

    tmp.append('    property = {\n')

    for prop in component.findall('property'):
        tmp.append('        "' + prop.attrib['name'] + '=' + prop.attrib['value'] + '",\n')
        component.remove(prop)

    i = tmp[-1].rfind(",")
    tmp[-1] = tmp[-1][:i] + tmp[-1][i+1:]

    tmp.append('    }\n')
    tmp.append(')\n')
    print(tmp)
    return tmp

def muddle_through(path, root, component):
    print(component.attrib)

    imports = [
        'import org.osgi.service.component.annotations.Activate;\n',
        'import org.osgi.service.component.annotations.Component;\n',
        'import org.osgi.service.component.annotations.Deactivate;\n',
        'import org.osgi.service.component.annotations.Modified;\n',
        'import org.osgi.service.component.annotations.Reference;\n',
        'import org.osgi.service.component.annotations.ReferenceCardinality;\n',
        'import org.osgi.service.component.annotations.ReferencePolicy;\n']

    implementation = component.find('implementation')
    source_path = os.path.join(os.path.dirname(path), '../../java/' + implementation.attrib['class'].replace('.','/') + '.java')
    if not os.path.isfile(source_path):
        # Dirty hack
        source_path = os.path.join(os.path.dirname(path), '../../../java/' + implementation.attrib['class'].replace('.','/') + '.java')

    print(source_path)

    lines = []
    with open(source_path, "r") as f:
        lines = f.readlines()
        # Component annotaiton
        search_class = ' class ' + component.find('implementation').attrib['class'].split('.')[-1]
        l = 0
        found_class = False
        for line in lines:
            if search_class in line:
                print('class', l, line)
                lines = _insert_position(l, lines, _component(component))
                found_class = True
                break
            l += 1

        if not found_class:
            print("CLASS NOT FOUND")
            quit()

        # References
        hasref = False
        for ref in component.findall('reference'):
            hasref = True
            lines = _references(lines, ref, imports)

        if not hasref:
            imports.remove('import org.osgi.service.component.annotations.Reference;\n')

        if 'activate' in component.attrib:
            lines = _activate(lines, component.attrib['activate'])
            del component.attrib['activate']

        if 'deactivate' in component.attrib:
            lines = _deactivate(lines, component.attrib['deactivate'])
            del component.attrib['deactivate']

        if 'modified' in component.attrib:
            lines = _modified(lines, component.attrib['modified'])
            del component.attrib['modified']
        # add imports
        lines = _imports(lines, imports)

        #for line in lines:
        #    print(line, end = '')

    with open(source_path, "w") as f:
        f.writelines(lines)

lookup_path = "../../modules/**/src/main/resources/OSGI-INF/**.xml"

xmlfiles = glob.glob(lookup_path, recursive = True)

for xml_path in xmlfiles:
    print("="*160)
    print("OSGI-INF:", xml_path)
    tree = ET.parse(xml_path)
    root = tree.getroot()
    if root.tag == '{http://www.osgi.org/xmlns/scr/v1.1.0}components':
        for element in root.findall('{http://www.osgi.org/xmlns/scr/v1.1.0}component'):
            muddle_through(xml_path, root, element)
    elif root.tag == '{http://www.osgi.org/xmlns/scr/v1.1.0}component':
        muddle_through(xml_path, None, root)
    else:
        print("FAILED:", xml_path)
    #tree.write(xml_path)
    #os.remove(xml_path)

