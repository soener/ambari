#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management.libraries.script import Script
from resource_management.libraries.functions import default, format
import status_params
import re
import json

def get_bare_principal(normalized_principal_name):
  """
  Given a normalized principal name (nimbus/c6501.ambari.apache.org@EXAMPLE.COM) returns just the
  primary component (nimbus)
  :param normalized_principal_name: a string containing the principal name to process
  :return: a string containing the primary component value or None if not valid
  """

  bare_principal = None

  if normalized_principal_name:
    match = re.match(r"([^/@]+)(?:/[^@])?(?:@.*)?", normalized_principal_name)

    if match:
      bare_principal = match.group(1)

  return bare_principal


# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

#hadoop params
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  rest_lib_dir = '/usr/hdp/current/storm-client/contrib/storm-rest'
  storm_bin_dir = "/usr/hdp/current/storm-client/bin"
  storm_lib_dir = "/usr/hdp/current/storm-client/lib"
else:
  rest_lib_dir = "/usr/lib/storm/contrib/storm-rest"
  storm_bin_dir = "/usr/bin"
  storm_lib_dir = "/usr/lib/storm/lib/"

storm_user = config['configurations']['storm-env']['storm_user']
log_dir = config['configurations']['storm-env']['storm_log_dir']
pid_dir = status_params.pid_dir
conf_dir = "/etc/storm/conf"
local_dir = config['configurations']['storm-site']['storm.local.dir']
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['hostLevelParams']['java_home']
jps_binary = format("{java64_home}/bin/jps")
nimbus_port = config['configurations']['storm-site']['nimbus.thrift.port']
nimbus_host = config['configurations']['storm-site']['nimbus.host']
rest_api_port = "8745"
rest_api_admin_port = "8746"
rest_api_conf_file = format("{conf_dir}/config.yaml")
storm_env_sh_template = config['configurations']['storm-env']['content']
jmxremote_port = config['configurations']['storm-env']['jmxremote_port']

if 'ganglia_server_host' in config['clusterHostInfo'] and \
                len(config['clusterHostInfo']['ganglia_server_host'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_host'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

security_enabled = config['configurations']['cluster-env']['security_enabled']

storm_ui_host = default("/clusterHostInfo/storm_ui_server_hosts", [])

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  _storm_principal_name = config['configurations']['storm-env']['storm_principal_name']
  storm_jaas_principal = _storm_principal_name.replace('_HOST',_hostname_lowercase)
  storm_keytab_path = config['configurations']['storm-env']['storm_keytab']

  if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
    storm_ui_keytab_path = config['configurations']['storm-env']['storm_ui_keytab']
    _storm_ui_jaas_principal_name = config['configurations']['storm-env']['storm_ui_principal_name']
    storm_ui_jaas_principal = _storm_ui_jaas_principal_name.replace('_HOST',_hostname_lowercase)

    storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)

    _nimbus_principal_name = config['configurations']['storm-env']['nimbus_principal_name']
    nimbus_jaas_principal = _nimbus_principal_name.replace('_HOST', _hostname_lowercase)
    nimbus_bare_jaas_principal = get_bare_principal(_nimbus_principal_name)
    nimbus_keytab_path = config['configurations']['storm-env']['nimbus_keytab']

if stack_is_hdp22_or_further:
  if security_enabled:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.secure.transport']
  else:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.nonsecure.transport']

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_report_interval = 60
  metric_collector_app_id = "nimbus"
metric_collector_sink_jar = "/usr/lib/storm/lib/ambari-metrics-storm-sink*.jar"

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  enable_ranger_storm = (config['configurations']['ranger-storm-plugin-properties']['ranger-storm-plugin-enabled'].lower() == 'yes')

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

#ranger storm properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_storm'

common_name_for_certificate = config['configurations']['ranger-storm-plugin-properties']['common.name.for.certificate']

storm_ui_port = config['configurations']['storm-site']['ui.port']

repo_config_username = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
repo_config_password = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-storm-plugin-properties']
policy_user = config['configurations']['ranger-storm-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
if has_ranger_admin:
  if xa_audit_db_flavor.lower() == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
  elif xa_audit_db_flavor.lower() == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
  elif nxa_audit_db_flavor.lower() == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
  elif xa_audit_db_flavor.lower() == 'sqlserver':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")
  
  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

storm_ranger_plugin_config = {
  'username': repo_config_username,
  'password': repo_config_password,
  'nimbus.url': 'http://' + storm_ui_host[0].lower() + ':' + str(storm_ui_port),
  'commonNameForCertificate': common_name_for_certificate
}

storm_ranger_plugin_repo = {
  'isActive': 'true',
  'config': json.dumps(storm_ranger_plugin_config),
  'description': 'storm repo',
  'name': repo_name,
  'repositoryType': 'storm',
  'assetType': '6'
}
