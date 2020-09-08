PROJECT_ID=youzhi-lab
BUCKET_NAME=hk-publish
TOKEN=`gcloud auth print-access-token`
GCS_INPUT_VIDEO=video/bilibili/Bbasketballdrive1920x1080x50x500x420p8.mp4
GCS_OUTPUT_FOLDER=transcode_output/2020081101/Bbasketballdrive1920x1080x50x500x420p8/

curl -X POST "https://transcoder.googleapis.com/v1beta1/projects/${PROJECT_ID?}/locations/asia-east1/jobTemplates?jobTemplateId=h265-bilibili-2020081101" \
  -H "Authorization: Bearer ${TOKEN?}" \
  -H "Content-Type: application/json" \
  -d \
'{
  "config": {
    "inputs": [
      {
        "key": "input0",
      }
    ],
    "editList": [
      {
        "key": "atom0",
        "inputs": [
          "input0"
        ]
      }
    ],
    "elementaryStreams": [
      {
        "key": "video-stream0",
        "videoStream": {
          "codec": "h265",
          "heightPixels": 1080,
          "widthPixels": 1920,
          "bitrateBps": 400000,
          "frameRate": 50,
          "enableTwoPass": true,
          "gopDuration": "5s",
          "vbvSizeBits": 18000000,
        }
      },
      {
        "key": "video-stream1",
        "videoStream": {
          "codec": "h265",
          "heightPixels": 1080,
          "widthPixels": 1920,
          "bitrateBps": 700000,
          "frameRate": 50,
          "enableTwoPass": true,
          "gopDuration": "5s",
          "vbvSizeBits": 18000000,
        }
      },
      {
        "key": "video-stream2",
        "videoStream": {
          "codec": "h265",
          "heightPixels": 1080,
          "widthPixels": 1920,
          "bitrateBps": 1400000,
          "frameRate": 50,
          "enableTwoPass": true,
          "gopDuration": "5s",
          "vbvSizeBits": 18000000,
        }
      },
      {
        "key": "video-stream3",
        "videoStream": {
          "codec": "h265",
          "heightPixels": 1080,
          "widthPixels": 1920,
          "bitrateBps": 2100000,
          "frameRate": 50,
          "enableTwoPass": true,
          "gopDuration": "5s",
          "vbvSizeBits": 18000000,
        }
      },
      {
        "key": "video-stream4",
        "videoStream": {
          "codec": "h265",
          "heightPixels": 1080,
          "widthPixels": 1920,
          "bitrateBps": 4200000,
          "frameRate": 50,
          "enableTwoPass": true,
          "gopDuration": "5s",
          "vbvSizeBits": 18000000,
        }
      },
    ],
    "muxStreams": [
      {
        "key": "400-kbps-mp4",
        "container": "mp4",
        "elementaryStreams": [
          "video-stream0"
        ]
      },
      {
        "key": "700-kbps-mp4",
        "container": "mp4",
        "elementaryStreams": [
          "video-stream1"
        ]
      },
      {
        "key": "1400-kbps-mp4",
        "container": "mp4",
        "elementaryStreams": [
          "video-stream2"
        ]
      },
      {
        "key": "2100-kbps-mp4",
        "container": "mp4",
        "elementaryStreams": [
          "video-stream3"
        ]
      },
      {
        "key": "4200-kbps-mp4",
        "container": "mp4",
        "elementaryStreams": [
          "video-stream4"
        ]
      },
    ],
    "manifests": [
      
    ],
    "output": {
    }
  }
}'

curl -X POST "https://transcoder.googleapis.com/v1beta1/projects/${PROJECT_ID?}/locations/asia-east1/jobs" \
  -H "Authorization: Bearer ${TOKEN?}" \
  -H "Content-Type: application/json" \
  -d \
    '{
      "inputUri": "gs://'${BUCKET_NAME?}/${GCS_INPUT_VIDEO?}'",
      "outputUri": "gs://'${BUCKET_NAME?}/${GCS_OUTPUT_FOLDER?}'",
      "templateId": "h265-bilibili-2020081101"
    }'



JOB_NAME=projects/247839977271/locations/asia-east1/jobs/ccf20ec8733349a37a1e2f9c1c6ef88a

curl "https://transcoder.googleapis.com/v1beta1/${JOB_NAME?}" \
-H "Authorization: Bearer ${TOKEN?}"