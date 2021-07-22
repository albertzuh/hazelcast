/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.internal.cluster.impl.operations;

import com.hazelcast.internal.cluster.impl.ClusterDataSerializerHook;
import com.hazelcast.internal.cluster.impl.ClusterServiceImpl;

/**
 * Requests member list publish from master node
 */
public class TriggerMemberListPublishOp extends AbstractClusterOperation {

    public TriggerMemberListPublishOp() {
    }

    @Override
    public void run() throws Exception {
        final ClusterServiceImpl clusterService = getService();
        clusterService.getMembershipManager().sendMemberListToMember(getAllKnownAliases(getCallerAddress()));
    }

    @Override
    public int getClassId() {
        return ClusterDataSerializerHook.TRIGGER_MEMBER_LIST_PUBLISH;
    }
}
