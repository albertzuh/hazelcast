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

package com.hazelcast.client.impl.protocol.task.dynamicconfig;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.DynamicConfigAddMapConfigCodec;
import com.hazelcast.config.CacheDeserializedValues;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapPartitionLostListenerConfig;
import com.hazelcast.config.MetadataPolicy;
import com.hazelcast.config.PartitioningStrategyConfig;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.dynamicconfig.DynamicConfigurationAwareConfig;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.partition.PartitioningStrategy;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.UserCodeNamespacePermission;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class AddMapConfigMessageTask
        extends AbstractAddConfigMessageTask<DynamicConfigAddMapConfigCodec.RequestParameters> {

    public AddMapConfigMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected DynamicConfigAddMapConfigCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return DynamicConfigAddMapConfigCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return DynamicConfigAddMapConfigCodec.encodeResponse();
    }

    @Override
    @SuppressWarnings({"checkstyle:npathcomplexity", "checkstyle:cyclomaticcomplexity", "checkstyle:MethodLength"})
    protected IdentifiedDataSerializable getConfig() {
        MapConfig config = new MapConfig(parameters.name);
        config.setAsyncBackupCount(parameters.asyncBackupCount);
        config.setBackupCount(parameters.backupCount);
        config.setCacheDeserializedValues(CacheDeserializedValues.valueOf(parameters.cacheDeserializedValues));
        if (parameters.listenerConfigs != null && !parameters.listenerConfigs.isEmpty()) {
            config.setEntryListenerConfigs(
                    (List<EntryListenerConfig>) adaptListenerConfigs(parameters.listenerConfigs, parameters.userCodeNamespace));
        }
        if (parameters.merkleTreeConfig != null) {
            config.setMerkleTreeConfig(parameters.merkleTreeConfig);
        }
        if (parameters.eventJournalConfig != null) {
            config.setEventJournalConfig(parameters.eventJournalConfig);
        }
        if (parameters.hotRestartConfig != null) {
            config.setHotRestartConfig(parameters.hotRestartConfig);
        }

        config.setInMemoryFormat(InMemoryFormat.valueOf(parameters.inMemoryFormat));
        config.setAttributeConfigs(parameters.attributeConfigs);
        config.setReadBackupData(parameters.readBackupData);
        config.setStatisticsEnabled(parameters.statisticsEnabled);
        config.setPerEntryStatsEnabled(parameters.perEntryStatsEnabled);
        config.setIndexConfigs(parameters.indexConfigs);
        if (parameters.mapStoreConfig != null) {
            config.setMapStoreConfig(parameters.mapStoreConfig.asMapStoreConfig(serializationService,
                    parameters.userCodeNamespace));
        }
        config.setTimeToLiveSeconds(parameters.timeToLiveSeconds);
        config.setMaxIdleSeconds(parameters.maxIdleSeconds);
        if (parameters.evictionConfig != null) {
            config.setEvictionConfig(parameters.evictionConfig.asEvictionConfig(serializationService));
        }
        if (parameters.mergePolicy != null) {
            config.setMergePolicyConfig(mergePolicyConfig(parameters.mergePolicy, parameters.mergeBatchSize));
        }
        if (parameters.nearCacheConfig != null) {
            config.setNearCacheConfig(parameters.nearCacheConfig.asNearCacheConfig(serializationService));
        }
        config.setPartitioningStrategyConfig(getPartitioningStrategyConfig());
        if (parameters.partitionLostListenerConfigs != null && !parameters.partitionLostListenerConfigs.isEmpty()) {
            config.setPartitionLostListenerConfigs(
                    (List<MapPartitionLostListenerConfig>) adaptListenerConfigs(parameters.partitionLostListenerConfigs,
                            parameters.userCodeNamespace));
        }
        config.setSplitBrainProtectionName(parameters.splitBrainProtectionName);
        if (parameters.queryCacheConfigs != null && !parameters.queryCacheConfigs.isEmpty()) {
            List<QueryCacheConfig> queryCacheConfigs = new ArrayList<>(parameters.queryCacheConfigs.size());
            for (QueryCacheConfigHolder holder : parameters.queryCacheConfigs) {
                queryCacheConfigs.add(holder.asQueryCacheConfig(serializationService, parameters.userCodeNamespace));
            }
            config.setQueryCacheConfigs(queryCacheConfigs);
        }
        config.setWanReplicationRef(parameters.wanReplicationRef);
        config.setMetadataPolicy(MetadataPolicy.getById(parameters.metadataPolicy));
        if (parameters.isDataPersistenceConfigExists) {
            config.setDataPersistenceConfig(parameters.dataPersistenceConfig);
        }
        if (parameters.isTieredStoreConfigExists) {
            config.setTieredStoreConfig(parameters.tieredStoreConfig);
        }
        if (parameters.isPartitioningAttributeConfigsExists) {
            config.setPartitioningAttributeConfigs(parameters.partitioningAttributeConfigs);
        }
        if (parameters.isUserCodeNamespaceExists) {
            config.setUserCodeNamespace(parameters.userCodeNamespace);
        }
        return config;
    }

    private PartitioningStrategyConfig getPartitioningStrategyConfig() {
        if (parameters.partitioningStrategyClassName != null) {
            return new PartitioningStrategyConfig(parameters.partitioningStrategyClassName);
        } else if (parameters.partitioningStrategyImplementation != null) {
            PartitioningStrategy<?> partitioningStrategy =
                    serializationService.toObject(parameters.partitioningStrategyImplementation);
            return new PartitioningStrategyConfig(partitioningStrategy);
        } else {
            return null;
        }
    }

    @Override
    public Permission getUserCodeNamespacePermission() {
        return parameters.userCodeNamespace != null
                ? new UserCodeNamespacePermission(parameters.userCodeNamespace, ActionConstants.ACTION_USE) : null;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_MAP_CONFIG;
    }

    @Override
    protected boolean checkStaticConfigDoesNotExist(IdentifiedDataSerializable config) {
        DynamicConfigurationAwareConfig nodeConfig = (DynamicConfigurationAwareConfig) nodeEngine.getConfig();
        MapConfig mapConfig = (MapConfig) config;
        return DynamicConfigurationAwareConfig.checkStaticConfigDoesNotExist(
                nodeConfig.getStaticConfig().getMapConfigs(),
                mapConfig.getName(), mapConfig);
    }
}
