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

App.WidgetWizardStep3Controller = Em.Controller.extend({
  name: "widgetWizardStep3Controller",

  /**
   * @type {string}
   */
  widgetName: '',

  /**
   * @type {string}
   */
  widgetAuthor: '',

  /**
   * @type {boolean}
   */
  isSharedChecked: false,

  /**
   * @type {string}
   */
  widgetScope: function () {
    return this.get('isSharedChecked')? 'Cluster': 'User';
  }.property('isSharedChecked'),

  /**
   * @type {string}
   */
  widgetDescription: '',

  /**
   * actual values of properties in API format
   * @type {object}
   */
  widgetProperties: {},

  /**
   * @type {Array}
   */
  widgetValues: [],

  /**
   * @type {Array}
   */
  widgetMetrics: [],

  /**
   * restore widget data set on 2nd step
   */
  initPreviewData: function () {
    this.set('widgetProperties', this.get('content.widgetProperties'));
    this.set('widgetValues', this.get('content.widgetValues'));
    this.set('widgetMetrics', this.get('content.widgetMetrics'));
    this.set('widgetAuthor', App.router.get('loginName'));
    this.set('widgetName', this.get('content.widgetName'));
    this.set('widgetDescription', this.get('content.widgetDescription'));
    this.set('isSharedChecked', this.get('content.widgetScope') == 'CLUSTER');
  },

  isSubmitDisabled: function () {
    return !(this.get('widgetName'));
  }.property('widgetName'),

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {cluster_name: *, widget_name: *, widget_type: *, description: *, scope: string, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    return {
      WidgetInfo: {
        widget_name: this.get('widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('widgetDescription') || " ",
        scope: this.get('widgetScope').toUpperCase(),
        "metrics": this.get('widgetMetrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.serviceName,
            "component_name": metric.componentName,
            "metric_path": metric.metricPath,
            "host_component_criteria": metric.hostComponentCriteria
          }
        }),
        values: this.get('widgetValues'),
        properties: this.get('widgetProperties')
      }
    };
  },

  complete: function () {
    App.router.send('complete', this.collectWidgetData());
  }
});
