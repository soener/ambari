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
package org.apache.ambari.server.api.services;

import com.sun.jersey.core.util.Base64;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Widget Service
 */
public class WidgetService extends BaseService {

  private final String clusterName;

  public WidgetService(String clusterName) {
    this.clusterName = clusterName;
  }

  @GET
  @Path("{widgetId}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("widgetId") String widgetId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(getUserName(headers), widgetId));
  }

  /**
   * Handles URL: /widget
   * Get all instances for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return instance collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(getUserName(headers), null));
  }

  @POST
  @Path("{widgetId}")
  @Produces("text/plain")
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetId") String widgetId) {
    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(getUserName(headers), widgetId));
  }

  @POST
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(getUserName(headers), null));
  }

  @PUT
  @Path("{widgetId}")
  @Produces("text/plain")
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetId") String widgetId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(getUserName(headers), widgetId));
  }

  @PUT
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(getUserName(headers), null));
  }

  @DELETE
  @Path("{widgetId}")
  @Produces("text/plain")
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetId") String widgetId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(getUserName(headers), widgetId));
  }

  private ResourceInstance createResource(String userName, String widgetId) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Widget, widgetId);
    mapIds.put(Resource.Type.User, userName);
    return createResource(Resource.Type.Widget, mapIds);
  }

  private String getUserName(HttpHeaders headers) {
    List<String> authorizationHeaders = headers.getRequestHeaders().get("Authorization");
    if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
      String authorizationString = authorizationHeaders.get(0);
      if (authorizationString != null && authorizationString.startsWith("Basic")) {
        String base64Credentials = authorizationString.substring("Basic".length()).trim();
        String clearCredentials = new String(Base64.decode(base64Credentials), Charset.forName("UTF-8"));
        return clearCredentials.split(":", 2)[0];
      }
    }
    return null;
  }
}
