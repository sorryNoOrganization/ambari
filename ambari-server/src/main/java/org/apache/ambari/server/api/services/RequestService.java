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


import org.apache.ambari.server.api.resources.RequestResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


/**
 * Service responsible for request resource requests.
 */
public class RequestService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;


  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public RequestService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/requests/{requestID}
   * Get a specific request.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param requestId  request id
   *
   * @return request resource representation
   */
  @GET
  @Path("{requestId}")
  @Produces("text/plain")
  public Response getRequest(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("requestId") String requestId) {

    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(requestId, m_clusterName));
  }

  /**
   * Handles URL: /clusters/{clusterId}/requests
   * Get all requests for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return request collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getRequests(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(null, m_clusterName));
  }

  /**
   * Gets the tasks sub-resource.
   */
  @Path("{requestId}/tasks")
  public TaskService getTaskHandler(@PathParam("requestId") String requestId) {
    return new TaskService(m_clusterName, requestId);
  }

  /**
   * Create a request resource definition.
   *
   * @param requestId    request id
   * @param clusterName  cluster name
   *
   * @return a request resource definition
   */
  ResourceDefinition createResourceDefinition(String requestId, String clusterName) {
    return new RequestResourceDefinition(requestId, clusterName);
  }
}
