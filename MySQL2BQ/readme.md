# Descripton:
This is a solution of migration and CDC from MySQL to Google Cloud BigQuery. It is using the opensource project debezium API, support Migratino and CDC from MySQL to BigQuery.
The customized debezium connector can work as a unified platform for histofic data migration and new data CDC, even the schema is changed or new table 
is added in MySQL. BigQuery will work as a data lake and collect the raw data, and transfer the raw data to table by using SQL script.  

# Authentication

See the [Authentication][authentication] section in the base directory's README. You need to set Pub/Sub Publisher and BigQuery User priviledge in the Service Account. 

# Installaton Steps:
## In a Google Debain VM:
```java
sudo apt update
sudo apt install maven git -y
git clone https://github.com/zhmichael007/google-cloud-demo.git
cd google-cloud-demo/MySQL2BQ
modify the hostname, port, user, password in ChangeDataSender.java file
mvn install
mvn exec:java
```
[authentication]: https://github.com/googleapis/google-cloud-java#authentication
## Create BigQuery raw data table:
```java
bq mk --table zhmichael1:debezium_cdc.raw_data ./raw_data.json
```
