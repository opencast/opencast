#!/usr/bin/env python3
"""
This script converts a workflow from XML to YAML by using a yaml parser.

This script does not guarantee a perfect conversion, it only aims at doing most
of the legwork for you. Therefore please check the output yourself and make
corrections as necessary!

wontfix: Newlines (e.g. between operations) are not preserved

Install dependencies: pip install lxml ruamel.yaml

Usage:
  Single file:   python workflowXMLtoYAML.py input.xml [output.yaml]
  Folder mode:   python workflowXMLtoYAML.py input_folder output_folder
"""

import os
import sys
import glob
from lxml import etree
from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap, CommentedSeq
from ruamel.yaml.scalarstring import LiteralScalarString

def smart_convert(value):
    """
    Convert common string representations: numbers and booleans.
    """
    if isinstance(value, str):
        val = value.strip()
        if val.isdigit():
            return int(val)
        if val.lower() == "true":
            return True
        if val.lower() == "false":
            return False
    return value

def process_state_mappings(sm_element):
    """
    Processes a <state-mappings> element into a list of dictionaries.
    For each <state-mapping> child element, extract the 'state' attribute and its text value.
    """
    mappings = []
    for sm in sm_element.findall("./{*}state-mapping"):
        mapping = {
            "state": sm.get("state"),
            "value": sm.text.strip() if sm.text else ""
        }
        mappings.append(mapping)
    return mappings


def get_preceding_comments(elem):
    """
    Returns a list of comment texts that immediately precede the given element.
    Iterates backward over preceding siblings until a non-comment is found.
    """
    comments = []
    prev = elem.getprevious()
    while prev is not None and isinstance(prev, etree._Comment):
        if prev.text:
            comments.append(prev.text.strip())
        prev = prev.getprevious()
    return list(reversed(comments))

def process_configurations(config_elem, op_id):
    """
    Processes the <configurations> element into a CommentedSeq where each
    <configuration> child is converted into a mapping with a single key-value pair.
    """
    new_configs = CommentedSeq()
    pending_comments = []
    for child in config_elem:
        # If the child is a comment, accumulate it.
        if isinstance(child, etree._Comment):
            if child.text:
                pending_comments.append(child.text.strip())
        # If it is a configuration element...
        elif isinstance(child.tag, str) and etree.QName(child).localname == "configuration":
            k = child.get("key")
            raw_text = child.text.strip() if child.text is not None else ""
            # If the raw text contains a newline, output it as literal block style,
            # so that quotes are not escaped.
            if "\n" in raw_text:
                value = LiteralScalarString(raw_text)
            else:
                value = smart_convert(raw_text)
            v = smart_convert(value)
            config_item = CommentedMap({k: v})
            # Attach any pending comments as inline (start) comments.
            if pending_comments:
                comment_str = "\n".join(pending_comments)
                config_item.yaml_set_start_comment(comment_str, indent=6)
                pending_comments = []  # reset after attaching comments
            new_configs.append(config_item)
        # Else, we ignore other nodes.
    return new_configs

def process_operation(op_element):
    """
    Processes an <operation> element

    Returns a tuple: (op_dict, comments_list)
    """
    op_dict = {}
    # Include all attributes (e.g., fail-on-error, if, max-attempts, etc.)
    for attr, val in op_element.attrib.items():
        op_dict[attr] = smart_convert(val)

    # Process child elements (other than configurations)
    for child in op_element:
        # Skip comment nodes; they are handled separately.
        if not isinstance(child.tag, str):
            continue
        tag_local = etree.QName(child).localname
        if tag_local != "configurations":
            if child.text:
                op_dict[tag_local] = smart_convert(child.text.strip())

    # Process configurations if present. Use wildcard to handle namespaces.
    conf_elem = op_element.find("./{*}configurations")
    if conf_elem is not None:
        op_id = op_dict.get("id", "")
        op_dict["configurations"] = process_configurations(conf_elem, op_id)

    # Get inline comments from preceding XML comments.
    comments = get_preceding_comments(op_element)
    return op_dict, comments

def process_operations(ops_parent):
    """
    Process all <operation> elements under the <operations> parent.
    Returns a ruamel.yaml CommentedSeq
    """
    operations_seq = CommentedSeq()
    for elem in ops_parent:
        # Skip non-element nodes.
        if not isinstance(elem.tag, str):
            continue
        if etree.QName(elem).localname == "operation":
            op_dict, comments = process_operation(elem)
            # Create a CommentedMap from the operation dictionary.
            op_cm = CommentedMap(op_dict)
            # Attach inline comment at the start.
            if comments:
                comment_str = "\n".join(comments)
                op_cm.yaml_set_start_comment(comment_str, indent=2)
            operations_seq.append(op_cm)

    # Set an end comment (newline) for each item in the sequence.
    for idx in range(len(operations_seq)):
        if operations_seq.ca.items.get(idx) is None:
            operations_seq.ca.items[idx] = [None, None, None, None]
        operations_seq.ca.items[idx][3] = "\n"

    return operations_seq

def get_child_text(elem, tag_name):
    """
    Returns the text of a child element (ignoring namespaces) or None.
    """
    child = elem.find(f".//{{*}}{tag_name}")
    if child is not None and child.text:
        return child.text.strip()
    return None

def convert_xml_to_yaml(input_path, output_path):
    """
    Reads the XML using lxml, converts the contents into a structured Python object,
    attaches inline YAML comments for operations, and then writes to a YAML file.
    """
    try:
        parser = etree.XMLParser(remove_blank_text=True, strip_cdata=False)
        tree = etree.parse(input_path, parser)
    except Exception as e:
        print(f"Error reading/parsing XML file '{input_path}': {e}")
        sys.exit(1)

    root = tree.getroot()

    output = CommentedMap()

    output["id"] = get_child_text(root, "id")
    title = get_child_text(root, "title")
    if title:
        output["title"] = title

    # Process tags
    tags_elem = root.find(f".//{{*}}tags")
    if tags_elem is not None:
        tags = tags_elem.findall(f".//{{*}}tag")
        output["tags"] = [t.text.strip() for t in tags if t.text]

    display_order = get_child_text(root, "displayOrder")
    if display_order is not None:
        output["displayOrder"] = smart_convert(display_order)

    # Process description
    description = get_child_text(root, "description")
    if description:
        output["description"] = LiteralScalarString(description)

    # Process state-mappings
    sm_elem = root.find(f".//{{*}}state-mappings")
    if sm_elem is not None:
        output["state-mappings"] = process_state_mappings(sm_elem)

    # Process configuration_panel_json
    config_panel_elem = root.find(f".//{{*}}configuration_panel_json")
    if config_panel_elem is not None and config_panel_elem.text:
        output["configuration_panel_json"] = LiteralScalarString(config_panel_elem.text)

    # Process operations
    ops_parent = root.find(f".//{{*}}operations")
    if ops_parent is not None:
        output["operations"] = process_operations(ops_parent)

    # Dump using ruamel.yaml so that inline comments are preserved
    yaml_instance = YAML()
    yaml_instance.indent(mapping=2, sequence=4, offset=2)
    try:
        with open(output_path, "w", encoding="utf-8") as outf:
            yaml_instance.dump(output, outf)
        return True
    except Exception as e:
        print(f"Error writing YAML file '{output_path}': {e}")
        return False

# --- Main logic ---

def main():
    # Usage:
    # Single file:   python convert.py input.xml [output.yaml]
    # Folder mode:   python convert.py input_folder output_folder
    if len(sys.argv) < 2 or len(sys.argv) > 3:
        print(__doc__)
        sys.exit(1)

    input_path = sys.argv[1]

    if os.path.isfile(input_path):
        # Single file mode.
        if len(sys.argv) == 3:
            output_path = sys.argv[2]
        else:
            base, _ = os.path.splitext(input_path)
            output_path = base + ".yaml"

        success = convert_xml_to_yaml(input_path, output_path)
        if success:
            print(f"Converted file: {input_path} → {output_path}")
        else:
            print(f"Failed to convert: {input_path}")
    elif os.path.isdir(input_path):
        # Folder mode
        if len(sys.argv) != 3:
            print("For folder mode, please provide an output folder as the second argument.")
            sys.exit(1)
        output_folder = sys.argv[2]
        if not os.path.exists(output_folder):
            os.makedirs(output_folder)

        xml_files = glob.glob(os.path.join(input_path, "*.xml"))
        if not xml_files:
            print("No XML files were found in the input folder.")
            sys.exit(0)

        for xml_file in xml_files:
            base_name = os.path.basename(xml_file)
            name, _ = os.path.splitext(base_name)
            output_file = os.path.join(output_folder, name + ".yaml")
            success = convert_xml_to_yaml(xml_file, output_file)
            if success:
                print(f"Converted: {xml_file} → {output_file}")
            else:
                print(f"Failed to convert: {xml_file}")
    else:
        print(f"Input path {input_path} is not a valid file or folder.")
        sys.exit(1)

if __name__ == '__main__':
    main()