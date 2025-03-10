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

package com.hazelcast.internal.util;

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;

import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Invokes all {@link JVMUtil} method to ensure no exception is thrown.
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class JVMUtilTest extends HazelcastTestSupport {

    @Test
    public void testConstructor() {
        assertUtilityConstructor(JVMUtil.class);
    }

    @Test
    public void testIs32bitJVM() {
        JVMUtil.is32bitJVM();
    }

    @Test
    public void testIsCompressedOops() {
        JVMUtil.isCompressedOops();
    }

    @Test
    public void testUsedMemory() {
        Assert.assertTrue(JVMUtil.usedMemory(Runtime.getRuntime()) > 0);
    }

    @Test
    public void testIsHotSpotCompressedOopsOrNull() {
        JVMUtil.isHotSpotCompressedOopsOrNull();
    }

    @Test
    public void testIsObjectLayoutCompressedOopsOrNull() {
        JVMUtil.isObjectLayoutCompressedOopsOrNull();
    }

    @Test
    public void testGetPid() {
       long legacyPidResult = getPidLegacy();

       assumeThat(legacyPidResult).isNotEqualTo(-1);
       assertEquals(legacyPidResult, JVMUtil.getPid());
    }

    /**
     * Returns the process ID. The algorithm does not guarantee it will be able
     * to get the correct process ID, in which case it returns {@code -1}.
     */
    private static long getPidLegacy() {
        String name = ManagementFactory.getRuntimeMXBean().getName();

        if (name == null) {
            return -1;
        }
        int separatorIndex = name.indexOf("@");
        if (separatorIndex < 0) {
            return -1;
        }
        String potentialPid = name.substring(0, separatorIndex);
        try {
            return Long.parseLong(potentialPid);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Prints the size of object reference as calculated by JVMUtil.
    // When running under Hotspot 64-bit:
    // - JDK 6u23+ should report 4 (CompressedOops enabled by default)
    // - JDK 7 with -Xmx <= 32G or without any -Xmx specified should report 4 (CompressedOops enabled), otherwise 8
    // - explicitly starting with -XX:+UseCompressedOops should report 4, otherwise 8
    public static void main(String[] args) {
        System.out.println("Size of reference: " + JVMUtil.REFERENCE_COST_IN_BYTES);
    }
}
