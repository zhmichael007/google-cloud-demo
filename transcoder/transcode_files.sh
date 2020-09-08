#!/bin/bash

PROJECT_ID=youzhi-lab
BUCKET_NAME=hk-publish
TOKEN=`gcloud auth print-access-token`
GCS_INPUT_VIDEO=video/G01animation1920x1080x24x833x420p8.mp4
GCS_OUTPUT_FOLDER=transcode_output/2020072801/G01animation1920x1080x24x833x420p8/


for file in $(gsutil ls gs://hk-publish/video/bilibili/); do # Not recommended, will break on whitespace
	filename="${file##*/}"
	filename_no_ext="${filename%%.*}"
    curl -X POST "https://transcoder.googleapis.com/v1beta1/projects/${PROJECT_ID?}/locations/asia-east1/jobs" \
      -H "Authorization: Bearer ${TOKEN?}" \
      -H "Content-Type: application/json" \
      -d \
      '{
        "inputUri": "'${file?}'",
        "outputUri": "gs://'${BUCKET_NAME?}'/transcode_output/2020072801/'${filename_no_ext?}'/",
        "templateId": "vp9-bilibili"
      }'
done