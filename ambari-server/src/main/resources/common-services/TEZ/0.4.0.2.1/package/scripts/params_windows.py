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

from resource_management import *
import os

config = Script.get_config()
tez_user = "hadoop"
tez_home_dir = None
tez_conf_dir = "conf"

hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"], ".."))

if os.environ.has_key("TEZ_HOME"):
  tez_home_dir = os.environ["TEZ_HOME"]
  tez_conf_dir = os.path.join(tez_home_dir, "conf")
