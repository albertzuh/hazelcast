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

package com.hazelcast.spring.jet;

import com.hazelcast.config.Config;
import com.hazelcast.spring.CustomSpringJUnit4ClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;

@RunWith(CustomSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"instance-config-ignored-hazelcast.xml"})
@Category(QuickTest.class)
public class InstanceConfigIgnoredTest extends HazelcastTestSupport {

    @Resource(name = "config")
    Config config;

    @Test
    public void test() {
        Assert.assertEquals(4, config.getJetConfig().getBackupCount());
        Assert.assertEquals(4, config.getJetConfig().getInstanceConfig().getBackupCount());
    }
}
