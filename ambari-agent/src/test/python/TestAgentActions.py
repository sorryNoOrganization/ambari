#!/usr/bin/env python2.6

'''
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
'''

from unittest import TestCase
import os, errno, getpass
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.FileUtil import getFilePath
from ambari_agent import shell
from ambari_agent.shell import serverTracker
import time

class TestAgentActions(TestCase):
#This feature is not yet implemented in ActionQueue
  def test_installAndConfigAction(self):
    pass
#This feature is not yet implemented in ActionQueue
  def test_startAndStopAction(self):
    pass
