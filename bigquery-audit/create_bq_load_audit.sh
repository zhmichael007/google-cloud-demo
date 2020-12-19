#!/bin/bash

if [ -z "${PROJECT}" -o -z "${DATASET}" ]; then
  echo "Please set \$PROJECT and \$DATASET."
  exit 1
fi

IFS='' read -d '' Q <<EOF
#standardSQL
/*
 * Script: BQ Load Audit
 * Author: ryanmcdowell
 * Description: 
 * 
 * Creates a user friendly view for querying the
 * BigQuery load audit logs.
 */
/* Create a user friendly view. */
WITH load_audit AS (
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
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobStatistics.totalLoadOutputBytes,
    protopayload_auditlog.servicedata_v1_bigquery.jobCompletedEvent.job.jobConfiguration.load
  FROM
    \`${PROJECT}.${DATASET}.cloudaudit_googleapis_com_data_access_*\`
)
/* Query the audit */
SELECT
  principalEmail,
  callerIp,
  serviceName,
  methodName,
  eventName,
  projectId,
  jobId,
  errorCode,
  errorMessage,
  STRUCT(
    EXTRACT(MINUTE FROM startTime) as minuteOfDay,
    EXTRACT(HOUR FROM startTime) as hourOfDay,
    EXTRACT(DAYOFWEEK FROM startTime) - 1 as dayOfWeek,
    EXTRACT(DAYOFYEAR FROM startTime) as dayOfYear,
    EXTRACT(WEEK FROM startTime) as week,
    EXTRACT(MONTH FROM startTime) as month,
    EXTRACT(QUARTER FROM startTime) as quarter,
    EXTRACT(YEAR FROM startTime) as year
  ) as date,
  createTime,
  startTime,
  endTime,
  runtimeMs,
  TRUNC(runtimeMs / 1000.0, 2) as runtimeSecs,
  totalLoadOutputBytes,
  (totalLoadOutputBytes / pow(2,30)) as totalLoadOutputGigabytes,
  (totalLoadOutputBytes / pow(2,40)) as totalLoadOutputTerabytes,
  STRUCT(
    load.sourceUris,
    STRUCT(
      load.destinationTable.projectId,
      load.destinationTable.datasetId,
      load.destinationTable.tableId,
      CONCAT(load.destinationTable.datasetId, '.', load.destinationTable.tableId) as relativePath,
      CONCAT(load.destinationTable.projectId, '.', load.destinationTable.datasetId, '.', load.destinationTable.tableId) as absolutePath
    ) as destinationTable,
    load.createDisposition,
    load.writeDisposition,
    load.schemaJson
  ) as load,
  1 as numLoads
FROM
  load_audit
WHERE
  serviceName = 'bigquery.googleapis.com'
  AND methodName = 'jobservice.jobcompleted'
  AND eventName = 'load_job_completed'
EOF

bq mk --project_id $PROJECT --view="${Q}" $DATASET.bq_load_audit