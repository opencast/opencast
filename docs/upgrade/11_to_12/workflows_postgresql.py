# Upgrade script for PostgreSQL databases
# Requires Python3 to run
# Required packages:
#   $ pip install psycopg2-binary
# Set vars to point to your database
# Run on commandline: "python3 workflow_db_upgrade.py"
# WARNING: THIS SCRIPT DELETES DATA. CREATE A BACKUP BEFORE RUNNING

# Module Imports
import psycopg2
import xml.etree.ElementTree as ET
from datetime import datetime

# Vars
user = "opencast"
password = "dbpassword"
host = "127.0.0.1"
port = 5432
database = "opencast"

# Constants
workflow_table_name = "oc_workflow"
workflow_configuration_table_name = "oc_workflow_configuration"
workflow_operation_table_name = "oc_workflow_operation"
workflow_operation_configuration_table_name = "oc_workflow_operation_configuration"

WORKFLOW_NS = "{http://workflow.opencastproject.org}"
SECURITY_NS = "{http://org.opencastproject.security}"
MEDIAPACKAGE_NS = '{http://mediapackage.opencastproject.org}'

XML_DECLARATION = "<?xml version='1.0' encoding='UTF-8'?>"


# DB functions
def create_connection(host_name, port_number, user_name, user_password, db_name):
    connection = psycopg2.connect(
        host=host_name,
        port=port_number,
        user=user_name,
        password=user_password,
        database=db_name,
    )
    print("Connection to database successful")
    return connection


def execute_query(connection, query):
    cursor = connection.cursor()
    cursor.execute(query)
    connection.commit()
    print("Query executed successfully")


def execute_read_query(connection, query):
    cursor = connection.cursor()
    cursor.execute(query)
    return cursor.fetchall()


def insert_parsed(sql, list_of_lists):
    data = []
    for item in list_of_lists:
        data.append(tuple(item))

    cursor = connection.cursor()
    cursor.executemany(sql, data)
    connection.commit()


# XML functions
def get_node_value(node, name, ns=''):
    result = node.find(f'{ns}{name}')
    if result is None:
        return None
    return result.text


def get_attrib_from_node(node, attribute):
    return node.get(attribute)


def parse_workflow_state(state):
    states = {
        "INSTANTIATED": 0,
        "RUNNING": 1,
        "STOPPED": 2,
        "PAUSED": 3,
        "SUCCEEDED": 4,
        "FAILED": 5,
        "FAILING": 6,
    }
    return states.get(state)


def parse_operation_state(state):
    states = {
        "INSTANTIATED": 0,
        "RUNNING": 1,
        "PAUSED": 2,
        "SUCCEEDED": 3,
        "FAILED": 4,
        "SKIPPED": 5,
        "RETRY": 6,
    }
    return states.get(state)


def none_safe(value, fn):
    if value is None:
        return None
    return fn(value)


def parse_bool(value):
    if value is None:
        return None
    return value.lower() == 'true'


# Connect
print("Creating connection to database...")
connection = create_connection(host, port, user, password, database)

# Create new tables
#  Currently added indexes:
#  - Indexes for bidirectional relationships between Workflow <-> Operation <-> Configuration
#  - mediaPackageId, seriesId for oc_workflow
print("Create tables...")
create_workflow_table = f"""
CREATE TABLE {workflow_table_name} (
    id bigint PRIMARY KEY,
    creator_id character varying(255),
    date_completed timestamp without time zone,
    date_created timestamp without time zone,
    description text,
    mediapackage text,
    mediapackage_id character varying(128),
    organization_id character varying(255),
    series_id character varying(128),
    state integer,
    template character varying(255),
    title character varying(255)
)
"""

create_workflow_table_index_mediapackageId = f"""
CREATE INDEX IX_{workflow_table_name}_mediapackage_id ON {workflow_table_name} USING btree (mediapackage_id);
"""

create_workflow_table_index_seriesId = f"""
CREATE INDEX IX_{workflow_table_name}_series_id ON {workflow_table_name} USING btree (series_id);
"""

create_workflow_configuration_table = f"""
CREATE TABLE {workflow_configuration_table_name} (
    workflow_id bigint REFERENCES {workflow_table_name}(id),
    configuration_value text,
    configuration_key character varying(255)
)
"""

create_workflow_configuration_table_index_workflow_id = f"""
CREATE INDEX IX_{workflow_configuration_table_name}_workflow_id ON {workflow_configuration_table_name} USING btree (workflow_id);
"""

create_workflow_operation_table = f"""
CREATE TABLE {workflow_operation_table_name} (
    id bigint PRIMARY KEY,
    abortable boolean,
    continuable boolean,
    completed timestamp without time zone,
    started timestamp without time zone,
    description text,
    exception_handler_workflow character varying(255),
    if_condition character varying(65535),
    execution_host character varying(255),
    fail_on_error boolean,
    failed_attempts integer,
    job bigint,
    max_attempts integer,
    retry_strategy integer,
    state integer,
    template character varying(255),
    time_in_queue bigint,
    workflow_id bigint NOT NULL REFERENCES {workflow_table_name}(id),
    "position" integer
)
"""

create_workflow_operation_table_index_workflow_id = f"""
CREATE INDEX IX_{workflow_operation_table_name}_workflow_id ON {workflow_operation_table_name} USING btree (workflow_id);
"""

create_workflow_operation_configuration_table = f"""
CREATE TABLE {workflow_operation_configuration_table_name} (
    workflow_operation_id bigint REFERENCES {workflow_operation_table_name}(id),
    configuration_value text,
    configuration_key character varying(255) NOT NULL
)
"""

create_workflow_operation_configuration_table_index_workflow_operation_id = f"""
CREATE INDEX IX_{workflow_operation_configuration_table_name}_workflow_operation_id ON {workflow_operation_configuration_table_name} USING btree (workflow_operation_id);
"""

execute_query(connection, create_workflow_table)
execute_query(connection, create_workflow_table_index_mediapackageId)
execute_query(connection, create_workflow_table_index_seriesId)
execute_query(connection, create_workflow_configuration_table)
execute_query(connection, create_workflow_configuration_table_index_workflow_id)
execute_query(connection, create_workflow_operation_table)
execute_query(connection, create_workflow_operation_table_index_workflow_id)
execute_query(connection, create_workflow_operation_configuration_table)
execute_query(connection, create_workflow_operation_configuration_table_index_workflow_operation_id)

print("Collect information from oc_job table...")
select_workflow_count = """
select count(*)
from oc_job
where operation = 'START_WORKFLOW'

"""
select_workflow_from_job_table = """
SELECT id, payload, date_created, date_completed
FROM oc_job
WHERE operation = 'START_WORKFLOW'
ORDER BY date_created ASC
LIMIT 100
OFFSET {0}
"""

workflow_count = execute_read_query(connection, select_workflow_count)[0][0]
workflow_current = 0

# Parse information from XML
print("Put information from oc_job into the new tables...")
operation_id = 0
for offset in range(0, workflow_count, 100):
    workflow_sql = select_workflow_from_job_table.format(offset)
    workflow_jobs = execute_read_query(connection, workflow_sql)
    for (workflow_id, payload, date_created, date_completed) in workflow_jobs:
        root = ET.fromstring(payload)

        # oc_workflow
        workflow_current += 1
        print(f'Migrating workflow {workflow_id} ({workflow_current}/{workflow_count})')
        workflow_operations = []
        workflow_operation_config = []
        workflow_config = []
        workflow = [
            workflow_id,
            get_node_value(root, 'creator-id', SECURITY_NS),
            date_completed,
            date_created,
            get_node_value(root, 'description', WORKFLOW_NS),
            get_node_value(root, 'organization-id', SECURITY_NS),
            parse_workflow_state(get_attrib_from_node(root, "state")),
            get_node_value(root, 'template', WORKFLOW_NS),
            get_node_value(root, 'title', WORKFLOW_NS)]
        ET.register_namespace('', 'http://mediapackage.opencastproject.org')
        mediapackage = root.find(f"{MEDIAPACKAGE_NS}mediapackage")
        mediapackage_str = ET.tostring(mediapackage, encoding="UTF-8").decode("utf-8")
        # ET.tostring(…, xml_declaration=True) requires Python 3.8
        if not mediapackage_str.startswith(XML_DECLARATION):
            mediapackage_str = f'{XML_DECLARATION}\n{mediapackage_str}'
        workflow.append(mediapackage_str)
        workflow.append(get_attrib_from_node(mediapackage, "id"))
        workflow.append(get_node_value(mediapackage, f"{MEDIAPACKAGE_NS}series"))

        # oc_workflow_configuration
        for configuration in root.find(f"{WORKFLOW_NS}configurations"):
            value = configuration.text
            workflow_config.append([
                workflow_id,
                get_attrib_from_node(configuration, "key"),
                value if value is not None else ""])

        # oc_workflow_operation
        operation_position = 0
        for operation in root.find(f"{WORKFLOW_NS}operations"):
            workflow_operations.append([
                operation_id,
                parse_bool(get_attrib_from_node(operation, "abortable")),
                parse_bool(get_attrib_from_node(operation, "continuable")),
                none_safe(
                    get_node_value(operation, f"{WORKFLOW_NS}completed"),
                    lambda x: datetime.fromtimestamp(int(x) / 1000.0)),
                none_safe(
                    get_node_value(operation, f"{WORKFLOW_NS}started"),
                    lambda x: datetime.fromtimestamp(int(x) / 1000.0)),
                get_attrib_from_node(operation, "description"),
                get_attrib_from_node(operation, "exception-handler-workflow"),
                get_attrib_from_node(operation, "if"),
                get_attrib_from_node(operation, "execution-host"),
                parse_bool(get_attrib_from_node(operation, "fail-on-error")),
                get_attrib_from_node(operation, "failed-attempts"),
                get_attrib_from_node(operation, "job"),
                get_attrib_from_node(operation, "max-attempts"),
                {"none": 0, "retry": 1, "hold": 2}.get(
                    get_attrib_from_node(operation, "retry-strategy")),
                parse_operation_state(get_attrib_from_node(operation, "state")),
                get_attrib_from_node(operation, "id"),  # now named template
                get_node_value(operation, f"{WORKFLOW_NS}time-in-queue"),
                workflow_id,
                operation_position])

            # oc_workflow_operation_configuration
            for op_config in operation.find("{http://workflow.opencastproject.org}configurations"):
                value = op_config.text
                workflow_operation_config.append([
                    operation_id,
                    get_attrib_from_node(op_config, "key"),
                    value if value is not None else ""])

            # Generate ID and position for next operation
            operation_id += 1
            operation_position += 1

        # Insert parsed information into the created tables
        create_workflow_sql = f"""
        INSERT INTO {workflow_table_name} (
            id,
            creator_id,
            date_completed,
            date_created,
            description,
            organization_id,
            state,
            template,
            title,
            mediapackage,
            mediapackage_id,
            series_id
        ) VALUES
          ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s )
        """

        create_workflow_configuration_sql = f"""
        INSERT INTO {workflow_configuration_table_name} (
            workflow_id,
            configuration_key,
            configuration_value)
        VALUES
          ( %s, %s, %s )
        """

        create_workflow_operation_sql = f"""
        INSERT INTO {workflow_operation_table_name} (
            id,
            abortable,
            continuable,
            completed,
            started,
            description,
            exception_handler_workflow,
            if_condition,
            execution_host,
            fail_on_error,
            failed_attempts,
            job,
            max_attempts,
            retry_strategy,
            state,
            template,
            time_in_queue,
            workflow_id,
            "position"
        ) VALUES
        ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
          %s, %s, %s, %s, %s, %s, %s, %s, %s )
        """

        create_workflow_operation_configuration_sql = f"""
        INSERT INTO {workflow_operation_configuration_table_name} (
            workflow_operation_id,
            configuration_key,
            configuration_value
        ) VALUES
        ( %s, %s, %s )
        """

        insert_parsed(create_workflow_sql, [workflow])
        insert_parsed(create_workflow_configuration_sql, workflow_config)
        insert_parsed(create_workflow_operation_sql, workflow_operations)
        insert_parsed(create_workflow_operation_configuration_sql, workflow_operation_config)


# Delete workflow information from oc_job
print("Delete payloads from oc_job table...")
sql_update_job_payload_query = """
UPDATE oc_job
SET payload = NULL
WHERE operation = 'START_WORKFLOW'
"""
execute_query(connection, sql_update_job_payload_query)

print("Update complete!")
