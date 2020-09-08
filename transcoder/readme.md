# Set Authentication Environment:
Add a service account in IAM with Cloud Stroage Admin and Transcoder Admin, download the json file and activate it in the client machine:
gcloud auth activate-service-account --key-file your-service-key


# Post and get a template id:

TOKEN=`gcloud auth print-access-token`

curl -X POST "https://transcoder.googleapis.com/v1beta1/projects/280272749669/locations/us-central1/jobTemplates?jobTemplateId=vp9-bilibili" -H "Authorization: Bearer ${TOKEN?}" -H "Content-Type: application/json"  -d @transcode-template-bilibili-vp9.json

curl -X GET https://transcoder.googleapis.com/v1beta1/projects/280272749669/locations/us-central1/jobTemplates/vp9-bilibili -H "Authorization: Bearer ${TOKEN?}"


# View Job Status:
curl -X GET https://transcoder.googleapis.com/v1beta1/projects/280272749669/locations/us-central1/jobs/244a653b91bda8393e9737bdea28d343 -H "Authorization: Bearer `gcloud auth print-access-token`"

# Count the total frames:
https://stackoverflow.com/questions/2017843/fetch-frame-count-with-ffmpeg

ffprobe -v error -count_frames -select_streams v:0  -show_entries stream=nb_read_frames -of default=nokey=1:noprint_wrappers=1 2020082501/Bbasketballdrive1920x1080x50x500x420p8/4200-kbps-mp4.mp4
