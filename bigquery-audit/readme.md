在Cloud Shell里或者自己设置的GCP Command Line环境里执行以下语句，直接用Project Admin权限：

export PROJECT=zhmichael1

export DATASET=audit2

（以上两个变量为project名称和数据集的名称，可以根据自己的环境修改）

bq --project_id $PROJECT mk $DATASET

gcloud --project $PROJECT logging sinks create bigquery-audit-2 bigquery.googleapis.com/projects/$PROJECT/datasets/$DATASET --log-filter resource.type="bigquery_resource"

可以按照上面命令行输出的service account的名称，加到该dataset的Edit permission里面


bash create_bq_query_audit.sh

bash create_bq_load_audit.sh

顺利的话，可以在audit2这个dataset里看到以下table和view，稍微等会表格中会自动导入数据

打开https://datastudio.google.com/reporting/5a574063-6f27-4938-a4eb-67a7edc0bc3f
（需要把账户邮箱给我添加view权限=）

点击创建副本

在弹出的对话框里设置Data Source，按照对应的名字，设置audit2里的那两个View

最后会显示这样的Dashboard
