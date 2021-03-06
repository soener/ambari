/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var hdp22properties = require('data/HDP2.2/site_properties').configProperties;

var excludedConfigs = [
    'DB_FLAVOR'
];

var hdp23properties = hdp22properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp23properties.push(
  {
    "id": "site property",
    "name": "DB_FLAVOR",
    "displayName": "DB FLAVOR",
    "value": "MYSQL",
    "defaultValue": "MYSQL",
    "isReconfigurable": true,
    "options": [
      {
        displayName: 'MYSQL'
      },
      {
        displayName: 'ORACLE'
      },
      {
        displayName: 'POSTGRES'
      },
      {
        displayName: 'SQLSERVER'
      }
    ],
    "displayType": "radio button",
    "radioName": "RANGER DB_FLAVOR",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings"
  },
  {
    "id": "site property",
    "name": "tez.am.view-acls",
    "displayName": "tez.am.view-acls",
    "isRequired": false,
    "serviceName": "TEZ",
    "filename": "tez-site.xml",
    "category": "Advanced tez-site"
  }
);

module.exports =
{
  "configProperties": hdp23properties
};
