/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.ceres.app;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import java.net.InetSocketAddress;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * To be imported from a unit test that <code>@Testcontainers</code> activated. For example:
 * <pre>
 &#64;Container
 public static CassandraContainer&lt;?&gt; cassandraContainer = new CassandraContainer&lt;&gt;(
     CassandraContainerSetup#DOCKER_IMAGE);
 &#64;TestConfiguration
 &#64;Import(CassandraContainerSetup.class)
 public static class TestConfig {
   &#64;Bean
   CassandraContainer&lt;?&gt; cassandraContainer() {
     return cassandraContainer;
   }
 }
  * </pre>
 */
@TestConfiguration
public class CassandraContainerSetup {
  public static final DockerImageName DOCKER_IMAGE = DockerImageName.parse("cassandra:3.11");

  @Bean
  public CqlSessionBuilderCustomizer cqlSessionBuilderCustomizer(CassandraContainer<?> cassandraContainer) {
    return CassandraContainerSetup.setupSession(cassandraContainer);
  }

  public static CqlSessionBuilderCustomizer setupSession(CassandraContainer<?> cassandraContainer) {
    return cqlSessionBuilder -> {
      final Cluster cluster = cassandraContainer.getCluster();
      try (Session session = cluster.connect()) {
        session.execute("CREATE KEYSPACE IF NOT EXISTS ceres "
            + "    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
      }

      cqlSessionBuilder.addContactPoint(
          new InetSocketAddress(cassandraContainer.getHost(), cassandraContainer.getMappedPort(CassandraContainer.CQL_PORT))
      );
    };
  }
}
