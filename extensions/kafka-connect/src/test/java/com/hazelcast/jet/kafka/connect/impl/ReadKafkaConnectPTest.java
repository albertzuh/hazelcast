/*
 * Copyright 2024 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.kafka.connect.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.metrics.MetricDescriptor;
import com.hazelcast.internal.metrics.MetricsRegistry;
import com.hazelcast.internal.metrics.impl.CapturingCollector;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.core.BroadcastKey;
import com.hazelcast.jet.core.test.TestOutbox;
import com.hazelcast.jet.core.test.TestProcessorContext;
import com.hazelcast.jet.impl.util.Util;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.hazelcast.jet.core.EventTimePolicy.noEventTime;
import static com.hazelcast.jet.kafka.connect.impl.DummySourceConnector.DummyTask.dummyRecord;
import static com.hazelcast.jet.kafka.connect.impl.DummySourceConnector.ITEMS_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastSerialClassRunner.class)
@Category({QuickTest.class})
public class ReadKafkaConnectPTest extends HazelcastTestSupport {

    private ReadKafkaConnectP<Integer> readKafkaConnectP;
    private TestOutbox outbox;
    private TestProcessorContext context;
    private HazelcastInstance hazelcastInstance;

    @Before
    public void setUp() {
        hazelcastInstance = createHazelcastInstance(smallInstanceConfig());
        outbox = new TestOutbox(new int[]{10}, 10);
        context = new TestProcessorContext();
        context.setHazelcastInstance(hazelcastInstance);
        SourceConnectorWrapper sourceConnectorWrapper = new SourceConnectorWrapper(minimalProperties(), 0, context);
        readKafkaConnectP = new ReadKafkaConnectP<>(noEventTime(), rec -> (Integer) rec.value());
        readKafkaConnectP.setSourceConnectorWrapper(sourceConnectorWrapper);
        readKafkaConnectP.setActive(true);
    }

    @After
    public void cleanup() {
        if (readKafkaConnectP != null) {
            readKafkaConnectP.close();
        }
        hazelcastInstance.shutdown();
    }

    @Test
    public void should_run_task() throws Exception {
        readKafkaConnectP.init(outbox, context);

        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));
        boolean complete = readKafkaConnectP.complete();
        assertFalse(complete);
        assertThat(new ArrayList<>(outbox.queue(0))).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void should_filter_items() throws Exception {
        SourceConnectorWrapper sourceConnectorWrapper = new SourceConnectorWrapper(minimalProperties(), 0, context);
        readKafkaConnectP = new ReadKafkaConnectP<>(noEventTime(), rec -> {
            Integer value = (Integer) rec.value();
            if (value % 2 == 0) {
                return null;
            } else {
                return value;
            }
        });
        readKafkaConnectP.setActive(true);
        readKafkaConnectP.setSourceConnectorWrapper(sourceConnectorWrapper);

        readKafkaConnectP.init(outbox, context);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));
        boolean complete = readKafkaConnectP.complete();

        assertFalse(complete);
        assertThat(new ArrayList<>(outbox.queue(0))).containsExactly(1, 3);
    }

    @Test
    public void should_require_eventTimePolicy() {
        var wrapper = new SourceConnectorWrapper(minimalProperties(), 0, context);
        assertThatThrownBy(() -> new ReadKafkaConnectP<>(null,
                rec -> (Integer) rec.value())
                .setSourceConnectorWrapper(wrapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventTimePolicy is required");
    }

    @Test
    public void should_require_projectionFn() {
        var wrapper = new SourceConnectorWrapper(minimalProperties(), 0, context);
        assertThatThrownBy(() -> new ReadKafkaConnectP<>(noEventTime(), null)
                .setSourceConnectorWrapper(wrapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("projectionFn is required");
    }

    @Test
    public void should_register_metrics() throws Exception {
        readKafkaConnectP.init(outbox, context);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));
        MetricsRegistry metricsRegistry = Util.getNodeEngine(hazelcastInstance).getMetricsRegistry();
        CapturingCollector collector = new CapturingCollector();
        metricsRegistry.collect(collector);
        List<String> metricNames = collector.captures().keySet().stream()
                .filter(metric -> metric.discriminatorValue() != null && metric.discriminatorValue().contains("some-"))
                .map(MetricDescriptor::metric)
                .collect(Collectors.toList());
        assertThat(metricNames).contains("sourceRecordPollTotal", "sourceRecordPollTotalAvgTime", "creationTime");
    }

    @Test
    public void should_not_emit_when_snapshotting_but_after() throws Exception {
        enableSnapshotting(context);
        readKafkaConnectP.init(outbox, context);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));
        readKafkaConnectP.saveToSnapshot();
        readKafkaConnectP.complete();

        assertThat(outbox.queue(0)).isEmpty();

        readKafkaConnectP.snapshotCommitFinish(true);

        readKafkaConnectP.complete();

        assertThat(new ArrayList<>(outbox.queue(0))).containsExactly(0, 1, 2, 3, 4);

    }

    @Test
    public void should_close_task_runner() throws Exception {
        readKafkaConnectP.setSourceConnectorWrapper(null);
        readKafkaConnectP.setPropertiesFromUser(minimalProperties());
        readKafkaConnectP.init(outbox, context);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));

        readKafkaConnectP.complete();

        assertTrueEventually(() -> assertThat(DummySourceConnector.DummyTask.INSTANCE.isStarted()).isTrue());
        readKafkaConnectP.close();
        assertThat(DummySourceConnector.DummyTask.INSTANCE.isStopped()).isTrue();
        readKafkaConnectP = null;

    }

    @Test
    public void should_create_snapshot() throws Exception {
        TestProcessorContext testProcessorContext = context;
        enableSnapshotting(testProcessorContext);
        testProcessorContext.setTotalParallelism(2);
        testProcessorContext.setGlobalProcessorIndex(1);
        readKafkaConnectP.init(outbox, testProcessorContext);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));

        readKafkaConnectP.complete();

        boolean snapshot = readKafkaConnectP.saveToSnapshot();
        assertTrue(snapshot);
        Map.Entry<Object, Object> lastSnapshot = outbox.snapshotQueue().peek();
        assertThat(lastSnapshot).isNotNull();
        assertThat(lastSnapshot.getKey()).isEqualTo(BroadcastKey.broadcastKey("snapshot-1"));
    }

    @Test
    public void should_restore_snapshot() throws Exception {
        TestProcessorContext testProcessorContext = context;
        enableSnapshotting(testProcessorContext);
        readKafkaConnectP.init(outbox, testProcessorContext);
        assertTrueEventually(() -> assertTrue(readKafkaConnectP.configurationReceived()));
        Map.Entry<Object, Object> lastSnapshot = outbox.snapshotQueue().peek();
        assertThat(lastSnapshot).isNull();

        readKafkaConnectP.restoreFromSnapshot(BroadcastKey.broadcastKey("snapshot-0"), stateWithOffset(42));

        readKafkaConnectP.saveToSnapshot();
        lastSnapshot = outbox.snapshotQueue().peek();
        assertThat(lastSnapshot).isNotNull();
        assertThat((State) lastSnapshot.getValue()).isEqualTo(stateWithOffset(42));

    }

    @Nonnull
    private static State stateWithOffset(int value) {
        Map<Map<String, ?>, Map<String, ?>> partitionsToOffset = new HashMap<>();
        SourceRecord lastRecord = dummyRecord(value);
        partitionsToOffset.put(lastRecord.sourcePartition(), lastRecord.sourceOffset());
        return new State(partitionsToOffset);
    }

    private static void enableSnapshotting(TestProcessorContext testProcessorContext) {
        testProcessorContext.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
    }

    @Nonnull
    private static Properties minimalProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "some-name");
        properties.setProperty("tasks.max", "2");
        properties.setProperty("connector.class", DummySourceConnector.class.getName());
        properties.setProperty(ITEMS_SIZE, "5");
        return properties;
    }
}
