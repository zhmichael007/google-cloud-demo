#!/bin/bash

if [ -z "${PROJECT}" -o -z "${DATASET}" ]; then
  echo "Please set \$PROJECT and \$DATASET."
  exit 1
fi

IFS='' read -d '' Q <<EOF
#standardSQL
/*
 * Script: BQ Query Audit
 * Author: ryanmcdowell, vincegonzalez
 * Description: 
 * 
 * Creates a user friendly view for querying the
 * BigQuery query audit logs.
 */
/* Create a user friendly view. */
WITH query_audit AS (
  SELECT
    protopayload_auditlog.authenticationInfo.principalEmail,
    protopayload_auditlog.requestMetadata.callerIp,
    protopayload_auditlog.serviceName,
    protopayload_auditlog.methodName,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.eventName,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobName.projectId,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobName.jobId,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.createTime,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.startTime,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.endTime,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatus.error.code as errorCode,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatus.error.message as errorMessage,
    TIMESTAMP_DIFF(
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.endTime, 
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.startTime, MILLISECOND) as runtimeMs,
    TIMESTAMP_DIFF(
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.endTime, 
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.startTime, MILLISECOND) / 1000 as runtimeSecs,
    TIMESTAMP_DIFF(
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.startTime,
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.createTime, MILLISECOND) / 1000 as waitTimeSecs,
    CAST(CEIL((TIMESTAMP_DIFF(
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.endTime, 
      protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.startTime, MILLISECOND) / 1000) / 60) AS INT64) as executionMinuteBuckets,
    CASE 
      WHEN
        protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalProcessedBytes IS NULL  
        AND protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalSlotMs IS NULL
        AND protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatus.error.code IS NULL
      THEN true
      ELSE false
    END as cached,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalSlotMs,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalTablesProcessed,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalViewsProcessed,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalProcessedBytes,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalBilledBytes,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.billingTier,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobConfiguration.query,
    JSON_EXTRACT(REGEXP_EXTRACT(protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobConfiguration.query.query, r"--\\\\s+({.*})"), "\$['\#queryname']") as queryName,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.referencedTables,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.referencedViews
  FROM
    \`$PROJECT.$DATASET.cloudaudit_googleapis_com_data_access_*\`
)
/* Query the audit
 */
SELECT
  principalEmail,
  callerIp,
  serviceName,
  methodName,
  eventName,
  projectId,
  jobId,
  CASE
    WHEN REGEXP_CONTAINS(jobId, 'beam') THEN true
    ELSE false
  END as isBeamJob,
  CASE
    WHEN REGEXP_CONTAINS(query.query, 'cloudaudit_googleapis_com_data_access_') THEN true
    ELSE false
  END as isAuditDashboardQuery,
  errorCode,
  errorMessage,
  CASE
    WHEN errorCode IS NOT NULL THEN true
    ELSE false
  END as isError,
  CASE
    WHEN REGEXP_CONTAINS(errorMessage, 'timeout') THEN true
    ELSE false
  END as isTimeout,
  STRUCT(
    EXTRACT(MINUTE FROM createTime) as minuteOfDay,
    EXTRACT(HOUR FROM createTime) as hourOfDay,
    EXTRACT(DAYOFWEEK FROM createTime) - 1 as dayOfWeek,
    EXTRACT(DAYOFYEAR FROM createTime) as dayOfYear,
    EXTRACT(ISOWEEK FROM createTime) as week,
    EXTRACT(MONTH FROM createTime) as month,
    EXTRACT(QUARTER FROM createTime) as quarter,
    EXTRACT(YEAR FROM createTime) as year
  ) as date,
  createTime,
  startTime,
  endTime,
  runtimeMs,
  runtimeSecs,
  waitTimeSecs,
  cached,
  totalSlotMs,
  totalSlotMs / runtimeMs as avgSlots,
  
  /* The following statement breaks down the query into minute buckets 
   * and provides the average slot usage within that minute. This is a
   * crude way of making it so you can retrieve the average slot utilization
   * for a particular minute across multiple queries.
   */
  ARRAY(
    SELECT
      STRUCT(
        TIMESTAMP_TRUNC(TIMESTAMP_ADD(startTime, INTERVAL bucket_num MINUTE), MINUTE) as time,
        totalSlotMs / runtimeMs as avgSlotUsage
      )
    FROM
      UNNEST(GENERATE_ARRAY(1, executionMinuteBuckets)) as bucket_num
  ) as executionTimeline,
  
  totalTablesProcessed,
  totalViewsProcessed,
  totalProcessedBytes,
  totalBilledBytes,
  (totalBilledBytes / pow(2,30)) as totalBilledGigabytes,
  (totalBilledBytes / pow(2,40)) as totalBilledTerabytes,
  (totalBilledBytes / pow(2,40)) * 5 as estimatedCostUsd,
  billingTier,
  query,
  queryName,
  referencedTables,
  referencedViews,
  1 as queries
FROM
  query_audit
WHERE
  serviceName = 'bigquery.googleapis.com'
  AND methodName = 'jobservice.jobcompleted'
  AND eventName = 'query_job_completed'
EOF

bq mk --project_id $PROJECT --view="${Q}" $DATASET.bq_query_audit