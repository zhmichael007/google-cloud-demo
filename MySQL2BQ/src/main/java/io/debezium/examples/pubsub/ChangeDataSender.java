package io.debezium.examples.pubsub;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.debezium.engine.format.Json;
import io.debezium.relational.HistorizedRelationalDatabaseConnectorConfig;
import io.debezium.relational.history.FileDatabaseHistory;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.relational.history.MemoryDatabaseHistory;
import io.debezium.util.Clock;

/**
 * Demo for using the Debezium Embedded API to send change events to Amazon Kinesis.
 */
public class ChangeDataSender implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDataSender.class);

    private static final String APP_NAME = "debezium-pubsub";

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;
    private final Properties props = new Properties();

    public ChangeDataSender() {
        props.setProperty("name", "engine");
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", "/tmp/offsets.dat");
        props.setProperty("database.history", "io.debezium.relational.history.FileDatabaseHistory");
        props.setProperty("database.history.file.filename", "/tmp/dbhistory.dat");
        props.setProperty("offset.flush.interval.ms", "1000");
        /* begin connector properties */
        props.setProperty("database.hostname", "192.168.0.128");
        props.setProperty("database.port", "3306");
        props.setProperty("database.user", "root");
        props.setProperty("database.password", "GoogleCloudDemo123*");
        props.setProperty("database.server.id", "1");
        props.setProperty("database.server.name", "test");
        props.setProperty("decimal.handling.mode", "string");

    }

    @Override
    public void run() {
        //https://debezium.io/documentation/reference/1.3/development/engine.html
        engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(props)
                .using(this.getClass().getClassLoader())
                .notifying(new MyChangeConsumer())
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Requesting embedded engine to shut down");
            //engine.close();
        }));

        // the submitted task keeps running, only no more new ones can be added
        executor.shutdown();

        awaitTermination(executor);

        cleanUp();

        LOGGER.info("Engine terminated");
    }

    private void awaitTermination(ExecutorService executor) {
        try {
            while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.info("Waiting another 10 seconds for the embedded engine to complete");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cleanUp() {
    }

    private void sendRecord(SourceRecord record) {
        // We are interested only in data events not schema change events
        if (record.topic().equals(APP_NAME)) {
            return;
        }

        Schema schema = null;

        if ( null == record.keySchema() ) {
            LOGGER.error("The keySchema is missing. Something is wrong.");
            return;
        }

        // For deletes, the value node is null
        if ( null != record.valueSchema() ) {
            schema = SchemaBuilder.struct()
                    .field("key", record.keySchema())
                    .field("value", record.valueSchema())
                    .build();
        }
        else {
            schema = SchemaBuilder.struct()
                    .field("key", record.keySchema())
                    .build();
        }

        Struct message = new Struct(schema);
        message.put("key", record.key());

        System.out.println("key:"+record.key()+", value:"+record.value());

        if ( null != record.value() )
            message.put("value", record.value());


    }

    private String streamNameMapper(String topic) {
        return topic;
    }

    public static void main(String[] args) {
        new ChangeDataSender().run();
    }
}
