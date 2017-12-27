Kafka Scala API Tester - Advanced Nagios Plugin / CLI Tool with Kerberos support (CMM customized)
==============================
Ported from HariSekhon/nagios-plugin-kafka. For detail documentation, please refer to https://github.com/HariSekhon/nagios-plugin-kafka
---
## Intro

This project builds a single self-contained Java jar file with all dependencies included and can simply be run on the command line with full switch option support:
```
java -jar check_kafka.jar --help
```

and there is an optional convenience shell wrapper script at the top level to make commands shorter:
```
./check_kafka --help
```

Run against one or more Kafka brokers, comma separated:
```
./check_kafka --brokers localhost:9092 --topic test
OK: Kafka broker successfully returned unique message via topic 'test' partition '0', write time = 0.185s, read time = 0.045s, total time = 1.729s | write_time=0.185s read_time=0.045s total_time=1.729s
```

Use the ```--verbose``` switch to also show the brokers list that were tested. If you have specified one of the kerberos switches (or edited the consumer/producer properties files to do so) then the output will additionally contain the marker ```with sasl authentication``` to let you know that it was a secure configuration that was tested (originally I called this ```with kerberos``` but technically it may not be in future).
```
OK: Kafka broker '<hortonworks_host>:6667' successfully returned unique message via topic 'topic3' partition '0' with sasl authentication, write time = 0.148s, read time = 0.043s, total time = 0.691s | write_time=0.148s read_time=0.043s total_time=0.691s
```

#### Keberos Support

See the ```conf/``` directory for JAAS kerberos configurations.

### Build

Maven, Gradle and SBT automated builds are all provided.

A self-contained jar file with all dependencies will be created and symlinked to ```check_kafka.jar``` at the top level.

The Maven and Gradle builds are best as they will auto bootstap and run with no prior installed dependencies other than Java and ```make``` to kick it off.

#### Pre-build Steps
Update following files with correct JAAS configurations:
```
- conf/kafka_cli_jaas.conf
- conf/kafka_client_jaas.conf
- conf/kafka_server_jaas.conf
```

Files to update if you have higher version kafka:
```
- build.gradle
- build.sbt
- pom.xml

Update kafka and scala version string in these files.
Look for change logs for details
```

#### Automated Build from Source
The default ```make``` build will trigger a Gradle bootstrap from scratch with has an embedded checksum for security:

```
make
```

You can call any one of the 3 major build systems explicitly instead, which will recurse to build the library submodule using the same mechanism:

Maven:

```
make mvn
```

Gradle:

```
make gradle
```

SBT:

```
make sbt
```


