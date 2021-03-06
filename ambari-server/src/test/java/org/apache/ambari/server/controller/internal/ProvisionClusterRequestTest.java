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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.RequiredPasswordValidator;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.topology.TopologyValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

/**
 * Unit tests for ProvisionClusterRequest.
 */
@SuppressWarnings("unchecked")
public class ProvisionClusterRequestTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";

  private static final BlueprintFactory blueprintFactory = createStrictMock(BlueprintFactory.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final Configuration blueprintConfig = new Configuration(
      Collections.<String, Map<String, String>>emptyMap(),
      Collections.<String, Map<String, Map<String, String>>>emptyMap());

  @Before
  public void setUp() throws Exception {
    ProvisionClusterRequest.init(blueprintFactory);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).once();
    expect(blueprint.getConfiguration()).andReturn(blueprintConfig).anyTimes();

    replay(blueprintFactory, blueprint);
  }

  @After
  public void tearDown() {
    reset(blueprintFactory, blueprint);
  }

  @Test
  public void test_basic() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    TopologyRequest provisionClusterRequest = new ProvisionClusterRequest(properties);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(2, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get("group1");
    assertEquals("group1", group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains("host1.myDomain.com"));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
    // configuration
    Configuration group1Configuration = group1Info.getConfiguration();
    assertNull(group1Configuration.getParentConfiguration());
    assertEquals(1, group1Configuration.getProperties().size());
    Map<String, String> group1TypeProperties = group1Configuration.getProperties().get("foo-type");
    assertEquals(2, group1TypeProperties.size());
    assertEquals("prop1Value", group1TypeProperties.get("hostGroup1Prop1"));
    assertEquals("prop2Value", group1TypeProperties.get("hostGroup1Prop2"));
    assertTrue(group1Configuration.getAttributes().isEmpty());

    // group2
    HostGroupInfo group2Info = hostGroupInfo.get("group2");
    assertEquals("group2", group2Info.getHostGroupName());
    assertTrue(group2Info.getHostNames().isEmpty());
    assertEquals(5, group2Info.getRequestedHostCount());
    assertNotNull(group2Info.getPredicate());
    // configuration
    Configuration group2Configuration = group2Info.getConfiguration();
    assertNull(group2Configuration.getParentConfiguration());
    assertEquals(1, group2Configuration.getProperties().size());
    Map<String, String> group2TypeProperties = group2Configuration.getProperties().get("foo-type");
    assertEquals(1, group2TypeProperties.size());
    assertEquals("prop1Value", group2TypeProperties.get("hostGroup2Prop1"));
    //attributes
    Map<String, Map<String, Map<String, String>>> group2Attributes = group2Configuration.getAttributes();
    assertEquals(1, group2Attributes.size());
    Map<String, Map<String, String>> group2Type1Attributes = group2Attributes.get("foo-type");
    assertEquals(1, group2Type1Attributes.size());
    Map<String, String> group2Type1Prop1Attributes = group2Type1Attributes.get("hostGroup2Prop10");
    assertEquals(1, group2Type1Prop1Attributes.size());
    assertEquals("attribute1Prop10-value", group2Type1Prop1Attributes.get("attribute1"));

    // cluster scoped configuration
    Configuration clusterScopeConfiguration = provisionClusterRequest.getConfiguration();
    assertSame(blueprintConfig, clusterScopeConfiguration.getParentConfiguration());
    assertEquals(1, clusterScopeConfiguration.getProperties().size());
    Map<String, String> clusterScopedProperties = clusterScopeConfiguration.getProperties().get("someType");
    assertEquals(1, clusterScopedProperties.size());
    assertEquals("someValue", clusterScopedProperties.get("property1"));
    // attributes
    Map<String, Map<String, Map<String, String>>> clusterScopedAttributes = clusterScopeConfiguration.getAttributes();
    assertEquals(1, clusterScopedAttributes.size());
    Map<String, Map<String, String>> clusterScopedTypeAttributes = clusterScopedAttributes.get("someType");
    assertEquals(1, clusterScopedTypeAttributes.size());
    Map<String, String> clusterScopedTypePropertyAttributes = clusterScopedTypeAttributes.get("property1");
    assertEquals(1, clusterScopedTypePropertyAttributes.size());
    assertEquals("someAttributePropValue", clusterScopedTypePropertyAttributes.get("attribute1"));

    verify(blueprintFactory, blueprint);
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_NoHostGroupInfo() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection)properties.get("host_groups")).clear();

    // should result in an exception
    new ProvisionClusterRequest(properties);
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_GroupInfoMissingName() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection<Map<String, Object>>)properties.get("host_groups")).iterator().next().remove("name");

    // should result in an exception
    new ProvisionClusterRequest(properties);
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_NoHostsInfo() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection<Map<String, Object>>)properties.get("host_groups")).iterator().next().remove("hosts");

    // should result in an exception
    new ProvisionClusterRequest(properties);
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void test_NoHostNameOrHostCount() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    // remove fqdn property for a group that contains fqdn not host_count
    for (Map<String, Object> groupProps : (Collection<Map<String, Object>>) properties.get("host_groups")) {
      Collection<Map<String, Object>> hostInfo = (Collection<Map<String, Object>>) groupProps.get("hosts");
      Map<String, Object> next = hostInfo.iterator().next();
      if (next.containsKey("fqdn")) {
        next.remove("fqdn");
        break;
      }
    }

    // should result in an exception
    new ProvisionClusterRequest(properties);
  }

  @Test
  public void testGetValidators_noDefaultPassword() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    //properties.put("default_password", "pwd");
    TopologyRequest request = new ProvisionClusterRequest(properties);
    List<TopologyValidator> validators = request.getTopologyValidators();

    assertEquals(1, validators.size());
    TopologyValidator pwdValidator = validators.get(0);

    TopologyValidator noDefaultPwdValidator = new RequiredPasswordValidator(null);
    assertEquals(pwdValidator, noDefaultPwdValidator);
  }

  @Test
  public void testGetValidators_defaultPassword() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    properties.put("default_password", "pwd");
    TopologyRequest request = new ProvisionClusterRequest(properties);
    List<TopologyValidator> validators = request.getTopologyValidators();

    assertEquals(1, validators.size());
    TopologyValidator pwdValidator = validators.get(0);

    TopologyValidator defaultPwdValidator = new RequiredPasswordValidator("pwd");
    assertEquals(pwdValidator, defaultPwdValidator);
  }


  public static Map<String, Object> createBlueprintRequestProperties(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);

    Collection<Map<String, Object>> hostGroups = new ArrayList<Map<String, Object>>();
    properties.put("host_groups", hostGroups);

    // host group 1
    Map<String, Object> hostGroup1Properties = new HashMap<String, Object>();
    hostGroups.add(hostGroup1Properties);
    hostGroup1Properties.put("name", "group1");
    Collection<Map<String, String>> hostGroup1Hosts = new ArrayList<Map<String, String>>();
    hostGroup1Properties.put("hosts", hostGroup1Hosts);
    Map<String, String> hostGroup1HostProperties = new HashMap<String, String>();
    hostGroup1HostProperties.put("fqdn", "host1.myDomain.com");
    hostGroup1Hosts.add(hostGroup1HostProperties);
    // host group 1 scoped configuration
    // version 1 configuration syntax
    Collection<Map<String, String>> hostGroup1Configurations = new ArrayList<Map<String, String>>();
    hostGroup1Properties.put("configurations", hostGroup1Configurations);
    Map<String, String> hostGroup1Configuration1 = new HashMap<String, String>();
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop1", "prop1Value");
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop2", "prop2Value");
    hostGroup1Configurations.add(hostGroup1Configuration1);

    // host group 2
    Map<String, Object> hostGroup2Properties = new HashMap<String, Object>();
    hostGroups.add(hostGroup2Properties);
    hostGroup2Properties.put("name", "group2");
    Collection<Map<String, String>> hostGroup2Hosts = new ArrayList<Map<String, String>>();
    hostGroup2Properties.put("hosts", hostGroup2Hosts);
    Map<String, String> hostGroup2HostProperties = new HashMap<String, String>();
    hostGroup2HostProperties.put("host_count", "5");
    hostGroup2HostProperties.put("host_predicate", "Hosts/host_name=myTestHost");
    hostGroup2Hosts.add(hostGroup2HostProperties);
    // host group 2 scoped configuration
    // version 2 configuration syntax
    Collection<Map<String, String>> hostGroup2Configurations = new ArrayList<Map<String, String>>();
    hostGroup2Properties.put("configurations", hostGroup2Configurations);
    Map<String, String> hostGroup2Configuration1 = new HashMap<String, String>();
    hostGroup2Configuration1.put("foo-type/properties/hostGroup2Prop1", "prop1Value");
    hostGroup2Configuration1.put("foo-type/properties_attributes/attribute1/hostGroup2Prop10", "attribute1Prop10-value");
    hostGroup2Configurations.add(hostGroup2Configuration1);

    // cluster scoped configuration
    Collection<Map<String, String>> clusterConfigurations = new ArrayList<Map<String, String>>();
    properties.put("configurations", clusterConfigurations);

    Map<String, String> clusterConfigurationProperties = new HashMap<String, String>();
    clusterConfigurations.add(clusterConfigurationProperties);
    clusterConfigurationProperties.put("someType/properties/property1", "someValue");
    clusterConfigurationProperties.put("someType/properties_attributes/attribute1/property1", "someAttributePropValue");

    return properties;
  }
}
