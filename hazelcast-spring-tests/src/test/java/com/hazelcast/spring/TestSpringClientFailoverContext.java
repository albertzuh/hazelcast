/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.spring;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientFailoverConfig;
import com.hazelcast.client.impl.clientside.HazelcastClientProxy;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(CustomSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"node-client-applicationContext-failover-hazelcast.xml"})
@Category(QuickTest.class)
public class TestSpringClientFailoverContext {

    @Resource
    private ApplicationContext applicationContext;

    @After
    public void teardown() {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    @Test
    public void testBlueGreenClient() {
        HazelcastClientProxy blueGreenClient = applicationContext.getBean("blueGreenClient", HazelcastClientProxy.class);
        ClientFailoverConfig failoverConfig = blueGreenClient.client.getFailoverConfig();
        List<ClientConfig> clientConfigs = failoverConfig.getClientConfigs();
        assertEquals(2, clientConfigs.size());
        assertEquals("spring-cluster", clientConfigs.get(0).getClusterName());
        assertEquals("alternativeClusterName", clientConfigs.get(1).getClusterName());
        assertEquals(5, failoverConfig.getTryCount());
        blueGreenClient.shutdown();
    }
}
