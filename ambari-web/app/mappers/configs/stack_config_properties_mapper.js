/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.stackConfigPropertiesMapper = App.QuickDataMapper.create({
  model: App.StackConfigProperty,
  config: {
    id: 'id',
    name: 'StackConfigurations.property_name',
    display_name: 'StackConfigurations.property_display_name',
    file_name: 'StackConfigurations.type',
    description: 'StackConfigurations.property_description',
    value: 'StackConfigurations.property_value',
    default_value: 'StackConfigurations.property_value',
    type: 'StackConfigurations.property_type',
    service_name: 'StackConfigurations.service_name',
    stack_name: 'StackConfigurations.stack_name',
    stack_version: 'StackConfigurations.stack_version',
    property_depended_by: 'StackConfigurations.property_depended_by',
    value_attributes: 'StackConfigurations.property_value_attributes',
    is_final: 'default_is_final',
    default_is_final: 'default_is_final',
    supports_final: 'supports_final',
    widget: 'widget',
    /**** ui properties ***/
    display_type: 'display_type',
    category: 'category'
  },

  map: function (json) {
    console.time('stackConfigMapper execution time');
    if (json && json.items) {
      var configs = [];
      json.items.forEach(function(stackItem) {
        var configTypeInfo = Em.get(stackItem, 'StackServices.config_types');

        stackItem.configurations.forEach(function(config) {
          var configType = App.config.getConfigTagFromFileName(config.StackConfigurations.type);
          config.id = config.StackConfigurations.property_name + '_' + configType;
          config.default_is_final = config.StackConfigurations.final === "true";
          config.supports_final = !!configTypeInfo[configType] && configTypeInfo[configType].supports.final === "true";
          // Map from /dependencies to property_depended_by
          config.StackConfigurations.property_depended_by = [];
          if (config.dependencies && config.dependencies.length > 0) {
            config.dependencies.forEach(function(dep) {
              config.StackConfigurations.property_depended_by.push({
                type : dep.StackConfigurationDependency.dependency_type,
                name : dep.StackConfigurationDependency.dependency_name
              })
            });
          }
          /**
           * merging stack info with that is stored on UI
           * for now is not used; uncomment in will be needed
           * this.mergeWithUI(config);
           */
          this.mergeWithUI(config);
          configs.push(this.parseIt(config, this.get('config')));
        }, this);
      }, this);
      App.store.loadMany(this.get('model'), configs);
    }
    console.timeEnd('stackConfigMapper execution time');
  },

  /******************* METHODS TO MERGE STACK PROPERTIES WITH STORED ON UI (NOT USED FOR NOW)*********************************/

  /**
   * configs that are stored on UI
   * @type {Object[]};
   */
  preDefinedSiteProperties: function () {
    if (App.get('isHadoop23Stack')) {
      return require('data/HDP2.3/site_properties').configProperties;
    }
    if (App.get('isHadoop22Stack')) {
      return require('data/HDP2.2/site_properties').configProperties;
    }
    return require('data/HDP2/site_properties').configProperties;
  }.property('App.isHadoop22Stack', 'App.isHadoop23Stack'),

  /**
   * find UI config with current name and fileName
   * if there is such property - adds some info to config object
   * @param {Object} config
   * @method mergeWithUI
   */
  mergeWithUI: function(config) {
    var uiConfigProperty = this.getUIConfig(config.StackConfigurations.property_name, config.StackConfigurations.type);
    if (!config.StackConfigurations.property_display_name) {
      config.StackConfigurations.property_display_name = uiConfigProperty && uiConfigProperty.displayName ? uiConfigProperty.displayName : config.StackConfigurations.property_name;
    }
    config.category = uiConfigProperty ? uiConfigProperty.category : 'Advanced ' + App.config.getConfigTagFromFileName(config.StackConfigurations.type);
    config.display_type = uiConfigProperty ? uiConfigProperty.displayType : 'string';
  },

  /**
   * returns config with such name and fileName if there is such on UI
   * otherwise returns null
   * @param propertyName
   * @param siteName
   * @returns {Object|null}
   * @method getUIConfig
   */
  getUIConfig: function(propertyName, siteName) {
    return this.get('preDefinedSiteProperties').filterProperty('filename', siteName).findProperty('name', propertyName);
  }
});
