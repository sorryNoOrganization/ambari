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

from functions import calc_xmn_from_xms
from resource_management import *
import status_params

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()

hbase_conf_dir = "/etc/hbase/conf"
daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
region_mover = "/usr/lib/hbase/bin/region_mover.rb"
region_drainer = "/usr/lib/hbase/bin/draining_servers.rb"
hbase_cmd = "/usr/lib/hbase/bin/hbase"
hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = default("/commandParams/mark_draining_only",False)
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
security_enabled = config['configurations']['cluster-env']['security_enabled']
user_group = config['configurations']['cluster-env']['user_group']

# this is "hadoop-metrics2-hbase.properties" for 2.x stacks
metric_prop_file_name = "hadoop-metrics.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']
hbase_env_sh_template = config['configurations']['hbase-env']['content']

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
master_heapsize = config['configurations']['hbase-env']['hbase_master_heapsize']

regionserver_heapsize = config['configurations']['hbase-env']['hbase_regionserver_heapsize']
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = config['configurations']['hbase-env']['hbase_regionserver_xmn_ratio']
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)

pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
ganglia_server_host = '' if len(ganglia_server_hosts) == 0 else ganglia_server_hosts[0]

rs_hosts = config['clusterHostInfo']['slave_hosts'] #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)

master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
else:
  kinit_cmd = ""

#log4j.properties
if (('hbase-log4j' in config['configurations']) and ('content' in config['configurations']['hbase-log4j'])):
  log4j_props = config['configurations']['hbase-log4j']['content']
else:
  log4j_props = None

#hdfs directories
hbase_hdfs_root_dir = config['configurations']['hbase-site']['hbase.rootdir']
hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local
)
