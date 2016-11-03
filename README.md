###Maven###
Build and run a deployable jar:
```
mvn clean package

java -Djava.net.peferIPv4Stack=true -jar api-2.0.jar -http.port=:80 -admin.port=:90 -local.doc.root=/root/asserts/
-dsc.hosts=cassandra1,cassandra2 -ex.host=gw.api.taobao.com -com.twitter.server.resolverMap=ex=gw.api.taobao.com:80
-mysql.host=cassandra1 -mysql.port=3306 -mysql.username=root -mysql.password=123456 -mysql.database=cpdailyspace
```
