package io.debezium.examples.pubsub;

import com.google.cloud.bigquery.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.pubsub.v1.PubsubMessage;
import com.google.api.gax.rpc.ApiException;


public class MyChangeConsumer implements DebeziumEngine.ChangeConsumer<RecordChangeEvent<SourceRecord>> {
    private SourceRecordTranslator sourceRecordTranslator = null;
    private String ingestion_mode = "bigquery"; //bigquery or pubsub, case sensitive
    private String projectId = "zhmichael1";
    private String topicId = "topic_debezium";
    private String datasetName = "debezium_cdc";
    private String tableName = "raw_data";
    private int batchSize = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(MyChangeConsumer.class);

    public MyChangeConsumer() {
        sourceRecordTranslator = new SourceRecordTranslator();
    }

    private void SendPubSub(List<RecordChangeEvent<SourceRecord>> records, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> recordCommitter)
            throws InterruptedException {
        List<ApiFuture<String>> messageIdFutures = new ArrayList<>();
        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = null;

        for (RecordChangeEvent record : records) {
            SourceRecord sourceRecord = (SourceRecord) record.record();
            if (-1 == sourceRecord.topic().indexOf('.') || null == sourceRecord.value()) continue;
            //System.out.println(record.toString());
            //System.out.println("Key = '" + sourceRecord.key() + "' topic = '" + sourceRecord.topic() + "'"+ "' value = '" + sourceRecord.value() + "'");
            SourceRecordTranslator.RecordBody recordBody = sourceRecordTranslator.translate(sourceRecord);
            if (null == recordBody) {
                LOGGER.debug("null message is = {}", record);
                continue;
            }
            System.out.println(recordBody.toString());

            try {
                ByteString data = ByteString.copyFromUtf8(recordBody.toString());
                publisher = Publisher.newBuilder(topicName).build();
                PubsubMessage message = PubsubMessage
                        .newBuilder()
                        .setData(data)
                        .build();
                ApiFuture<String> future = publisher.publish(message);
                messageIdFutures.add(future);
                ApiFutures.addCallback(
                        future,
                        new ApiFutureCallback<String>() {
                            @Override
                            public void onFailure(Throwable throwable) {
                                if (throwable instanceof ApiException) {
                                    ApiException apiException = ((ApiException) throwable);
                                    // details on the API exception
                                    System.out.println(apiException.getStatusCode().getCode());
                                    System.out.println(apiException.isRetryable());
                                }
                                System.out.println("Error publishing message : " + message);
                            }

                            @Override
                            public void onSuccess(String messageId) {
                                // Once published, returns server-assigned message ids (unique within the topic)
                                //System.out.println("Published message ID: " + messageId);
                            }
                        },
                        MoreExecutors.directExecutor());

            } catch (IOException e) {
                throw new InterruptedException();
            }

            recordCommitter.markProcessed(record);
        }

        try {
            //Let PubSub message from async to sync, and also can force the Callback to be executed before recordCommitter.markBatchFinished();
            List<String> messageIds = ApiFutures.allAsList(messageIdFutures).get();

            //Message ID from PubSub server side
            for (int i = 0; i < messageIds.size(); i++) {
                //System.out.println("message id: " + messageIds.get(i));
            }

            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (ExecutionException e) {
            throw new InterruptedException();
        }

        recordCommitter.markBatchFinished();
    }

    private void SendBigQuery(List<RecordChangeEvent<SourceRecord>> records, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> recordCommitter)
            throws InterruptedException {

        List<InsertAllRequest.RowToInsert> rowContentArray = new ArrayList<>();

        for (RecordChangeEvent record : records) {
            SourceRecord sourceRecord = (SourceRecord) record.record();
            if (-1 == sourceRecord.topic().indexOf('.') || null == sourceRecord.value()) continue;
            //System.out.println(record.toString());
            //System.out.println("Key = '" + sourceRecord.key() + "' topic = '" + sourceRecord.topic() + "'"+ "' value = '" + sourceRecord.value() + "'");
            SourceRecordTranslator.RecordBody recordBody = sourceRecordTranslator.translate(sourceRecord);
            if (null == recordBody) {
                LOGGER.debug("null message is = {}", record);
                continue;
            }
            System.out.println(recordBody.toString());

            Map<String, Object> rowContent = new HashMap<>();

            rowContent.put("fullRecord", recordBody.getFullRecord());
            rowContent.put("operation", recordBody.getOperation());
            rowContent.put("primaryKey", recordBody.getPrimaryKey());
            rowContent.put("timestampMs", recordBody.getTimestampMs());
            rowContent.put("tableName", recordBody.getTableName());
            rowContent.put("tableCode", recordBody.getTableCode());
            rowContent.put("tablePart", recordBody.getTablePart());


            rowContentArray.add(InsertAllRequest.RowToInsert.of(rowContent));
            if(rowContentArray.size() == this.batchSize) {
                tableInsertRowsWithoutRowIds(datasetName, tableName, rowContentArray);
                rowContentArray.clear();
            }
            recordCommitter.markProcessed(record);
        }

        if(rowContentArray.size() > 0) {
            tableInsertRowsWithoutRowIds(datasetName, tableName, rowContentArray);
            rowContentArray.clear();
        }
        recordCommitter.markBatchFinished();
    }

    private void tableInsertRowsWithoutRowIds(
            String datasetName, String tableName, Iterable<InsertAllRequest.RowToInsert> rows)
            throws InterruptedException {
        try {
            // Initialize client that will be used to send requests. This client only needs to be created
            // once, and can be reused for multiple requests.
            BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

            // Get table
            TableId tableId = TableId.of(datasetName, tableName);

            // Inserts rowContent into datasetName:tableId.
            InsertAllResponse response =
                    bigquery.insertAll(InsertAllRequest.newBuilder(tableId).setRows(rows).build());

            if (response.hasErrors()) {
                // If any of the insertions failed, this lets you inspect the errors
                for (Map.Entry<Long, List<BigQueryError>> entry : response.getInsertErrors().entrySet()) {
                    System.out.println("Response error: \n" + entry.getValue());
                    throw new InterruptedException();
                }
            }
            System.out.println("Rows successfully inserted into table without row ids");
        } catch (BigQueryException e) {
            System.out.println("Insert operation not performed \n" + e.toString());
            throw new InterruptedException();
        }
    }

    public void handleBatch(List<RecordChangeEvent<SourceRecord>> records, 
                            DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> recordCommitter)
            throws InterruptedException {
        if("pubsub" == ingestion_mode) {
            LOGGER.debug("Send data to PubSub");
            SendPubSub(records, recordCommitter);
        }
        else if("bigquery" == ingestion_mode) {
            LOGGER.debug("Send data to BigQuery");
            SendBigQuery(records, recordCommitter);
        }
        else
        {
            LOGGER.error("Incorrect ingestion mode: {}", ingestion_mode);
        }
    }
}
