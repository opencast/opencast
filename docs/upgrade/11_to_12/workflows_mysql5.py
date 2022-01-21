# Upgrade script for MySQL databases
# Requires Python3 to run
# Required packages:
#   $ pip install mysql-connector-python
# Set vars to point to your database
# Run on commandline: "python3 workflow_db_upgrade.py"
# WARNING: THIS SCRIPT DELETES DATA. CREATE A BACKUP BEFORE RUNNING

# Module Imports
import mysql.connector
from mysql.connector import Error
import sys
import xml.etree.ElementTree as ET
import distutils.util
from distutils.util import strtobool
from datetime import datetime

# Vars
user = "opencast"
password = "dbpassword"
host = "127.0.0.1"
database = "opencast"

# Constants
workflow_table_name = "oc_workflow"
workflow_configuration_table_name = "oc_workflow_configuration"
workflow_operation_table_name = "oc_workflow_operation"
workflow_operation_configuration_table_name = "oc_workflow_operation_configuration"

# DB functions
def create_connection(host_name, user_name, user_password, db_name):
    connection = None

    try:
        connection = mysql.connector.connect(
            host=host_name,
            user=user_name,
            passwd=user_password,
            database=db_name
        )
        connection.row_factory = lambda cursor, row: row[0]
        print("Connection to MySQL DB successful")
    except Error as e:
        print(f"The error '{e}' occurred")
    return connection

def execute_query(connection, query):
    cursor = connection.cursor()

    try:
        cursor.execute(query)
        connection.commit()
        print("Query executed successfully")
    except Error as e:
        print(f"The error '{e}' occurred")

def execute_query_with_data(connection, query, data):
    cursor = connection.cursor()

    try:
        cursor.execute(query, data)
        connection.commit()
        print("Query executed successfully")
    except Error as e:
        print(f"The error '{e}' occurred")

def execute_read_query(connection, query):
    cursor = connection.cursor()
    result = None
    try:
        cursor.execute(query)
        result = cursor.fetchall()
        # Turn list of tuples into a simple list
        result = [el[0] for el in result]
        return result
    except Error as e:
        print(f"The error '{e}' occurred")

def insert_parsed(sql, list_of_lists):
  vars = []
  for item in list_of_lists:
    vars.append(tuple(item))

  try:
    cursor = connection.cursor()
    cursor.executemany(sql, vars)
    connection.commit()
  except Error as e:
    print(f"The error '{e}' occurred")

# XML functions
def get_node_value(node, name):
  result = node.find(name)
  if result == None:
    return None
  else:
    return result.text

def get_attrib_from_node(node, attribute):
  #result = node.attrib[attribute]
  result = node.get(attribute)
  if result == None:
    return None
  else:
    return result

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

  return states.get(state, None)

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

  return states.get(state, None)

### Connect
print("Creating connection to database...")
connection = create_connection(host, user, password, database)

# Cleanup artifacts from previous runs of this script
print("Clearing out potential artifacts from previous runs...")
delete_workflow_table = f"DROP TABLE {workflow_table_name}"
delete_workflow_configuration_table = f"DROP TABLE {workflow_configuration_table_name}"
delete_workflow_operation_table = f"DROP TABLE {workflow_operation_table_name}"
delete_workflow_operation_configuration_table = f"DROP TABLE {workflow_operation_configuration_table_name}"
execute_query(connection, delete_workflow_table)
execute_query(connection, delete_workflow_configuration_table)
execute_query(connection, delete_workflow_operation_table)
execute_query(connection, delete_workflow_operation_configuration_table)

## Create new tables
#  Currently added indexes:
#  - Indexes for bidirectional relationships between Workflow <-> Operation <-> Configuration
#  - mediaPackageId, seriesId for oc_workflow
print("Create tables...")
create_workflow_table = f"""
CREATE TABLE IF NOT EXISTS {workflow_table_name} (
  id BIGINT(20),
  state INT(11),
  template VARCHAR(255),
  title VARCHAR(255),
  description VARCHAR(255),
  parent BIGINT(20),
  creatorId VARCHAR(255),
  organizationId VARCHAR(255),
  dateCreated DATETIME,
  dateCompleted DATETIME,
  mediaPackage LONGTEXT,
  mediaPackageId VARCHAR(128),
  seriesId VARCHAR(128),
  PRIMARY KEY (id),
  INDEX (mediaPackageId),
  INDEX (seriesId)
) ENGINE = InnoDB
"""

create_workflow_configuration_table = f"""
CREATE TABLE IF NOT EXISTS {workflow_configuration_table_name} (
  workflow_id BIGINT(20),
  key_part VARCHAR(255) NOT NULL,
  value_part LONGTEXT NOT NULL,
  INDEX (workflow_id)
) ENGINE = InnoDB
"""

# ??? Missing "executionHistory" maybe?
create_workflow_operation_table = f"""
CREATE TABLE IF NOT EXISTS {workflow_operation_table_name} (
  id BIGINT(20) AUTO_INCREMENT,
  template VARCHAR(255),
  job BIGINT(20),
  state INT(11),
  description VARCHAR(255),
  holdurl VARCHAR(255),
  holdActionTitle VARCHAR(255),
  failOnError TINYINT(1),
  if_condition VARCHAR(255),
  unless_condition VARCHAR(255),
  exceptionHandlerWorkflow VARCHAR(255),
  abortable TINYINT(1) DEFAULT 0,
  continuable TINYINT(1) DEFAULT 0,
  started DATETIME,
  completed DATETIME,
  timeInQueue BIGINT(20),
  maxAttempts INT(11),
  failedAttempts INT(11) DEFAULT 0,
  executionHost VARCHAR(255),
  retryStrategy INT(11),
  POSITION INT(11),
  INSTANCE_id BIGINT(20),
  operations_ORDER INT(11),
  PRIMARY KEY (id),
  INDEX (INSTANCE_id)
) ENGINE = InnoDB
"""

create_workflow_operation_configuration_table = f"""
CREATE TABLE IF NOT EXISTS {workflow_operation_configuration_table_name} (
  workflow_operation_id BIGINT(20),
  key_part VARCHAR(255),
  value_part LONGTEXT,
  INDEX (workflow_operation_id)
) ENGINE = InnoDB
"""

execute_query(connection, create_workflow_table)
execute_query(connection, create_workflow_configuration_table)
execute_query(connection, create_workflow_operation_table)
execute_query(connection, create_workflow_operation_configuration_table)

### Get information from database
print("Collect information from oc_job table...")
select_payload_from_job_table = """
SELECT payload FROM oc_job WHERE operation="START_WORKFLOW"
"""
select_date_created_from_job_table = """
SELECT date_created FROM oc_job WHERE operation="START_WORKFLOW"
"""
select_date_completed_from_job_table = """
SELECT date_completed FROM oc_job WHERE operation="START_WORKFLOW"
"""

payloads = execute_read_query(connection, select_payload_from_job_table)
date_createds = execute_read_query(connection, select_date_created_from_job_table)
date_completeds = execute_read_query(connection, select_date_completed_from_job_table)

### Parse information from XML
print("Put information from oc_job into the new tables...")
wf_items = []
wf_config = []
wf_operation = []
wf_operation_config = []
for (payload, date_created, date_completed) in zip(payloads, date_createds, date_completeds):
  try:
    root = ET.fromstring(payload)
  except:
    print("Payload was not XML, not parsing. Payload: " + payload)
    continue

  ### oc_workflow
  # Order is important
  items = []
  workflow_id = get_attrib_from_node(root, "id")
  items.append(workflow_id)
  items.append(parse_workflow_state(get_attrib_from_node(root, "state")))
  items.append(get_node_value(root, "{http://workflow.opencastproject.org}template"))
  items.append(get_node_value(root, "{http://workflow.opencastproject.org}title"))
  items.append(get_node_value(root, "{http://workflow.opencastproject.org}description"))
  items.append(get_node_value(root, "{http://workflow.opencastproject.org}parent"))
  items.append(get_node_value(root, "{http://org.opencastproject.security}creator-id"))
  items.append(get_node_value(root, "{http://org.opencastproject.security}organization-id"))
  items.append(date_created)
  items.append(date_completed)
  if root.find("{http://mediapackage.opencastproject.org}mediapackage"):
    ET.register_namespace('', 'http://mediapackage.opencastproject.org')
    items.append(
      ET.tostring(
        root.find("{http://mediapackage.opencastproject.org}mediapackage"),
        encoding="UTF-8", # Fix declaration
        xml_declaration=True).decode("utf-8"))
  else:
    items.append(None)
  if root.find("{http://mediapackage.opencastproject.org}mediapackage"):
    items.append(get_attrib_from_node(root.find("{http://mediapackage.opencastproject.org}mediapackage"), "id"))
  else:
    items.append(None)
  items.append(get_node_value(root.find("{http://mediapackage.opencastproject.org}mediapackage"), "{http://mediapackage.opencastproject.org}series"))

  wf_items.append(items)

  ### oc_workflow_configuration
  for configuration in root.find("{http://workflow.opencastproject.org}configurations"):
    configs = []
    configs.append(workflow_id)
    configs.append(get_attrib_from_node(configuration, "key"))
    configs.append(configuration.text)

    wf_config.append(configs)

  ### oc_workflow_operation
  operation_position = 0
  for operation in root.find("{http://workflow.opencastproject.org}operations"):
    operations = []
    operation_id = get_attrib_from_node(operation, "job")
    operations.append(operation_id)
    operations.append(get_attrib_from_node(operation, "id"))
    operations.append(get_attrib_from_node(operation, "job"))
    operations.append(parse_operation_state(get_attrib_from_node(operation, "state")))
    operations.append(get_attrib_from_node(operation, "description"))
    operations.append(get_node_value(operation, "{http://workflow.opencastproject.org}holdurl"))
    operations.append(get_node_value(operation, "{http://workflow.opencastproject.org}holdActionTitle"))
    if get_attrib_from_node(operation, "fail-on-error") == None:
      operations.append(None)
    else:
      operations.append(strtobool(get_attrib_from_node(operation, "fail-on-error")))
    operations.append(get_attrib_from_node(operation, "if"))
    operations.append(get_attrib_from_node(operation, "unless"))
    operations.append(get_attrib_from_node(operation, "exception-handler-workflow"))
    if get_attrib_from_node(operation, "abortable") == None:
      operations.append(None)
    else:
      operations.append(strtobool(get_attrib_from_node(operation, "abortable")))
    if get_attrib_from_node(operation, "continuable") == None:
      operations.append(None)
    else:
      operations.append(strtobool(get_attrib_from_node(operation, "continuable")))
    if get_node_value(operation, "{http://workflow.opencastproject.org}started") == None:
      operations.append(None)
    else:
      operations.append(datetime.fromtimestamp(int(get_node_value(operation, "{http://workflow.opencastproject.org}started")) / 1000.0))
    if get_node_value(operation, "{http://workflow.opencastproject.org}completed") == None:
      operations.append(None)
    else:
      operations.append(datetime.fromtimestamp(int(get_node_value(operation, "{http://workflow.opencastproject.org}completed")) / 1000.0))
    operations.append(get_node_value(operation, "{http://workflow.opencastproject.org}time-in-queue"))
    operations.append(get_attrib_from_node(operation, "max-attempts"))
    operations.append(get_attrib_from_node(operation, "failed-attempts"))
    operations.append(get_attrib_from_node(operation, "execution-host"))
    if get_attrib_from_node(operation, "retry-strategy") == "none":
      operations.append(0)
    elif get_attrib_from_node(operation, "retry-strategy") == "retry":
      operations.append(1)
    elif get_attrib_from_node(operation, "retry-strategy") == "hold":
      operations.append(2)
    else:
      operations.append(None)
    operations.append(operation_position)
    operations.append(workflow_id)
    operations.append(operation_position)

    wf_operation.append(operations)

    operation_position += 1

    ### oc_workflow_operation_configuration
    for op_config in operation.find("{http://workflow.opencastproject.org}configurations"):
      op_configs = []

      op_configs.append(operation_id)
      op_configs.append(get_attrib_from_node(op_config, "key"))
      op_configs.append(op_config.text)

      wf_operation_config.append(op_configs)

### Insert parsed information into the created tables
  create_workflow_sql = f"""
  INSERT INTO
    `{workflow_table_name}` (`id`, `state`, `template`, `title`, `description`, `parent`,
    `creatorId`, `organizationId`, `dateCreated`, `dateCompleted`, `mediaPackage`,
    `mediaPackageId`, `seriesId`)
  VALUES
    ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s )
  """

  create_workflow_configuration_sql = f"""
  INSERT INTO
    `{workflow_configuration_table_name}` (`workflow_id`, `key_part`, `value_part`)
  VALUES
    ( %s, %s, %s )
  """

  create_workflow_operation_sql = f"""
  INSERT INTO
    `{workflow_operation_table_name}` (`id`, `template`, `job`, `state`, `description`,
    `holdurl`, `holdActionTitle`, `failOnError`, `if_condition`, `unless_condition`,
    `exceptionHandlerWorkflow`, `abortable`, `continuable`, `started`, `completed`,
    `timeInQueue`, `maxAttempts`, `failedAttempts`, `executionHost`, `retryStrategy`,
    `POSITION`, `INSTANCE_id`, `operations_ORDER` )
  VALUES
    ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
      %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
      %s, %s, %s )
  """

  create_workflow_operation_configuration_sql = f"""
  INSERT INTO
    `{workflow_operation_configuration_table_name}` (`workflow_operation_id`, `key_part`, `value_part`)
  VALUES
    ( %s, %s, %s )
  """

  insert_parsed(create_workflow_sql, wf_items)
  insert_parsed(create_workflow_configuration_sql, wf_config)
  insert_parsed(create_workflow_operation_sql, wf_operation)
  insert_parsed(create_workflow_operation_configuration_sql, wf_operation_config)
  print(workflow_id)
  wf_items = []
  wf_config = []
  wf_operation = []
  wf_operation_config = []


# ### Delete workflow information from oc_job
# print("Delete information from oc_job table...")
# ### Get information from database
# select_id_from_job_table = """
# SELECT payload FROM oc_job WHERE operation="START_WORKFLOW"
# """
# ids = execute_read_query(connection, select_id_from_job_table)
#
# ### Remove workflow XML from oc_job
# sql_update_job_payload_query = """
# UPDATE oc_job SET payload = %s where id = %s
# """
# for id in ids:
#   execute_query_with_data(connection, sql_update_job_payload_query, (id, id))

print("Update complete!")