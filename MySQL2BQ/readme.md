# Descripton:
This is a solution of migration and CDC from MySQL to Google Cloud BigQuery. It is using the opensource project debezium API, support Migratino and CDC from MySQL to BigQuery.
The customized debezium connector can work as a unified platform for history data migration and CDC, even the schema is changed or new table 
is added in MySQL. BigQuery will work as a data lake and collect the raw data, and transfer the raw data to table by using SQL script.  
The information about Debezium please refer to: [debezium]

# Authentication

See the [Authentication][authentication], you need to set Pub/Sub Publisher and BigQuery User priviledge in the Service Account. 

# Installaton Steps:

### Create BigQuery raw data table:
```
bq mk --table <project_id>:<dataset>.raw_data ./raw_data.json
```

### In a Google Debain VM:
```
sudo apt update
sudo apt install maven git wget -y
git clone https://github.com/zhmichael007/google-cloud-demo.git
cd google-cloud-demo/MySQL2BQ
```

### Install MySQL 5.7 or above, enable binlog with row mode
See the [Installation Guide][mysql installation] to install MySQL 5.7 or 8.0;  
sudo vi /etc/my.cnf, in the [mysqld] section, add the following:
```
[mysqld]
server-id=1
log_bin=ON
log_bin=/var/lib/mysql/mysql-bin
log_bin_index=/var/lib/mysql/mysql-bin.index
binlog-format=ROW
```
restart MySQL by using systemctl. 
Check the biglog setting in MySQL:
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/binlog.png" width="60%">

the value for binlog_format must be set to row or ROW.  
the value for binlog_row_image must be set to full or FULL.  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/binlog2.png" width="60%">

[mysql installation]: https://serverspace.io/support/help/how-to-install-mysql-on-debian-10/
[authentication]: https://github.com/googleapis/google-cloud-java#authentication
[debezium]: https://debezium.io/

### Modify the source code and run the application:
```
modify the hostname, port, user, password in ChangeDataSender.java file
set ingestion_mode, projectId, datasetName, tableName in MyChangeConsumer.java 
```
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/code1.png" width="60%">
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/code2.png" width="60%">


```
mvn install
mvn exec:java
```


# Demo
### Demo1: History Data Migration:
There are 2 databases, named testdb1 and testdb2:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/database_info.png" width="60%">

Start the Debezium application, will see the historic data is outputed, it will be inserted to BigQuery:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/debezium_start_app.png" width="80%">

Check the raw_data table in BigQuery:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/debezium_start_bq.png" width="60%">

You can use a view to parse the real table from raw_data:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/view.png" width="60%">

### Demo2: CDC
Execute Insert, Update and Delete operation in mysql client:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/cdc_mysql_op.png" width="60%">

Will see the output of the Debezium application with INSERT, UPDATE and DELETE:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/cdc_app_output.png" width="60%">

Check the raw_data table in BigQuery:  
<img src="https://github.com/zhmichael007/google-cloud-demo/blob/master/MySQL2BQ/img/cdc_bq.png" width="60%">  

You need to merge the data from raw_data table to destination table according to the operation INSERT, UPDATE and DELETE 
