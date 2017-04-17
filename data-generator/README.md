# Data generator

The mining data from printers. For that task datd generator used SNMP lib and retrive info about printer status (toner lvl, drum usage and etc).

## Build

mvn clean package

## Run

java -jar target/data-generator-1.0-SNAPSHOT-fat.jar