package io.debezium.examples;

import java.util.Map;

import org.apache.beam.vendor.grpc.v1p26p0.com.google.gson.Gson;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Maps;

public class SourceRecordTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRecordTranslator.class);
    
    public static final class Operation {
        public static final String INSERT = "INSERT";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String READ = "READ";
    }

    public class RecordBody {
        private String fullRecord;
        private String operation;
        private String primaryKey;
        private Long timestampMs;
        private String tableName;
        private int tableCode;
        private int tablePart;
        public void setOperation(String operation) {this.operation = operation;}
        public String getOperation() { return this.operation;}
        public void setPrimaryKey(Map<String, Object> primaryKey) {this.primaryKey = new Gson().toJson(primaryKey);}
        public String getPrimaryKey() {return this.primaryKey;}
        public void setTimestampMs(Long timestampMs) {this.timestampMs = timestampMs;}
        public Long getTimestampMs() {return this.timestampMs;}
        public void setFullRecord(Map<String, Object> fullRecord) {this.fullRecord = new Gson().toJson(fullRecord);}
        public String getFullRecord() {return this.fullRecord;}
        public void setTableName(String tableName) {this.tableName = tableName;}
        public String getTableName() {return this.tableName;}
        public void setTableCode(int tableCode) {this.tableCode = tableCode;}
        public int getTableCode() {return this.tableCode;}
        public void setTablePart(int tablePart) {this.tablePart = tablePart;}
        public int getTablePart() {return this.tablePart;}
        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    public RecordBody translate(SourceRecord record) {
        LOGGER.debug("Source Record from Debezium: {}", record);

        RecordBody body = new RecordBody();

        String qualifiedTableName = record.topic();

        Struct recordValue = (Struct) record.value();
        if (null == recordValue) {
            LOGGER.debug("Translator null recordValue is {}", record);
            return null;
        }

        Struct afterValue = recordValue.getStruct("after");
        Map<String, Object> result = null;
        if (null != afterValue) {
            LOGGER.debug("Translator null after is {}", recordValue);
            result = transfer(afterValue.schema(), afterValue);
        }
        else //DELETE operation
        {
            Struct beforeValue = recordValue.getStruct("before");
            if (null != beforeValue) {
                LOGGER.debug("Translator null before is {}", recordValue);
                result = transfer(beforeValue.schema(), beforeValue);
            }
        }


        if (null != record.key()) {
            body.setPrimaryKey(transfer(record.keySchema(), (Struct) record.key()));
        }


        String sourceRecordOp = recordValue.getString("op");
        if (null == sourceRecordOp) {
            LOGGER.debug("Translator null operation is {}", recordValue);
            return null;
        }

        Long timestampMs = recordValue.getInt64("ts_ms");

        body.setOperation(translateOperation(sourceRecordOp));
        body.setTimestampMs(timestampMs);
        if(result != null) {
            body.setFullRecord(result);
        }
        body.setTableName(qualifiedTableName);
        body.setTableCode(0);
        body.setTablePart(0);

        return body;
    }

    private static String translateOperation(String op) {
        switch (op) {
            case "c":
                return Operation.INSERT;
            case "u":
                return Operation.UPDATE;
            case "d":
                return Operation.DELETE;
            case "r":
                return Operation.READ;
            default:
                LOGGER.error("error op is {}", op);
                return null;
        }
    }

    private Map<String, Object> transfer(Schema schema, Struct value) {

        Map<String, Object> tmpMap = Maps.newHashMap();

        for (Field f : schema.fields()) {
            Schema.Type t = f.schema().type();
            switch (t) {
                case INT8:
                    tmpMap.put(f.name(), value.getInt8(f.name()));
                    break;
                case INT16:
                    tmpMap.put(f.name(), value.getInt16(f.name()));
                    break;
                case INT32:
                    tmpMap.put(f.name(), value.getInt32(f.name()));
                    break;
                case INT64:
                    tmpMap.put(f.name(), value.getInt64(f.name()));
                    break;
                case FLOAT32:
                    tmpMap.put(f.name(), value.getFloat32(f.name()));
                    break;
                case FLOAT64:
                    tmpMap.put(f.name(), value.getFloat64(f.name()));
                    break;
                case BOOLEAN:
                    tmpMap.put(f.name(), value.getBoolean(f.name()));
                    break;
                case STRING:
                    tmpMap.put(f.name(), value.getString(f.name()));
                    break;
                case BYTES:
                    tmpMap.put(f.name(), value.getBytes(f.name()));
                    break;
                case MAP:
                    LOGGER.error("{} types are not supported.", f.schema().type());
                    throw new DataException("Map types are not supported.");
                case ARRAY:
                    LOGGER.error("{} types are not supported.", f.schema().type());
                    throw new DataException("Array types are not supported.");
                case STRUCT:
                    LOGGER.error("{} types are not supported.", f.schema().type());
                    throw new DataException("Struct types are not supported.");
                default:
                    LOGGER.error("{} types are not supported.", f.schema().type());
                    throw new DataException(f.schema().type() + " types are not supported.");
            }
        }
        return tmpMap;
    }
}
