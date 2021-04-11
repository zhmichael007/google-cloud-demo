This is a solution of migration and CDC from MySQL to Google Cloud BigQuery. It is using the opensource project debezium API, support Migratino and CDC from MySQL to BigQuery.
The customized debezium connector can work as a unified platform for histofic data migration and new data CDC, even the schema is changed or new table 
is added in MySQL. BigQuery will work as a data lake and collect the raw data, and transfer the raw data to table by using SQL script.  

# Installaton Steps
In Google Debain VM:
```java
sudo apt update
sudo apt install maven git -y
git clone https://github.com/zhmichael007/google-cloud-demo.git
cd google-cloud-demo/MySQL2BQ
mvn install
```
Modify the hostname, port, user, password in ChangeDataSender.java file
```java
mvn compile
mvn exec:java
```
