Kafka API 0.9+ CLI Tester & Advanced Nagios Plugin (Scala + native Kafka 0.9+ Java API)
==============================
[![Build Status](https://travis-ci.org/HariSekhon/nagios-plugin-kafka.svg?branch=master)](https://travis-ci.org/HariSekhon/nagios-plugin-kafka)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/de500bf4f90d401ba5c98ed903c8a612)](https://www.codacy.com/app/harisekhon/nagios-plugin-kafka)
[![Dependency Status](https://www.versioneye.com/user/projects/57616d340a82b200276f6669/badge.svg)](https://www.versioneye.com/user/projects/57616d340a82b200276f6669)
[![Platform](https://img.shields.io/badge/platform-Linux%20%7C%20OS%20X-blue.svg)](https://github.com/harisekhon/nagios-plugin-kafka)
[![DockerHub](https://img.shields.io/badge/docker-available-blue.svg)](https://hub.docker.com/r/harisekhon/nagios-plugins/)

Kafka 0.9+ API CLI Tester & Advanced Nagios Plugin with Kerberos support. See the [Advanced Nagios PLugins Collection](https://github.com/harisekhon/nagios-plugins) for more related testing and enterprise monitoring programs.

## Quick Start

This project builds a single self-contained Java jar file with all dependencies included and can simply be run on the command line with full switch option support:
```
java -jar check_kafka.jar --help
```

and there is an optional convenience shell wrapper script at the top level to make commands shorter:
```
./check_kafka --help
```

Run against a broker
```
./check_kafka --brokers kafka_broker:9092 --topic test
OK: ...
```

#### Keberos Support

See the ```conf/``` directory for JaaS kerberos configurations.

### Build

#### Docker

A Dockerized pre-built version is available on [DockerHub](https://hub.docker.com/r/harisekhon/nagios-plugin-kafka).

If you have docker installed this one command will download and run it:
```
docker run harisekhon/nagios-plugin-kafka check_kafka --help
```

#### Compile Yourself

Builds with any one of Maven, Gradle or SBT. The Maven and Gradle builds are best as they will auto-download their own build systems of the correct compatible version for you without you having to pre-install them.

The default build will trigger a Maven build:
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

### Kafka 0.8 support - Alternative Perl & Python Kafka API Nagios Plugins

The [Advanced Nagios Plugins Collection](https://github.com/harisekhon/nagios-plugins) which has both Perl and Python predecessors to this program which work with Kafka 0.8+. The main differenitator with this Scala version is that it uses the native Java API and has full Kerberos support, which the dynamic language versions do not.

### See Also

- [Python Tools](https://github.com/harisekhon/pytools) - 30+ Hadoop, Spark, Pig, Ambari Blueprints, AWS CloudFormation, Linux, Data Converters & Validators (Avro/Parquet/JSON/CSV/XML/YAML), Elasticsearch, Solr, IPython - CLI tools
- [Perl Tools](https://github.com/harisekhon/tools) - 25+ Hadoop, Hive, Solr, Linux, SQL, Ambari, Datameer, Web and various Linux CLI Tools
- [Spotify Tools](https://github.com/harisekhon/spotify-tools) - Backup & Play Automation: Spotify Lookup - converts Spotify URIs to 'Artist - Track' form by querying the Spotify Metadata API. Spotify Cmd - command line control of Spotify on Mac via AppleScript for automation, auto timed track flick through etc.
- [Spark Apps](https://github.com/harisekhon/spark-apps) - Spark Scala Apps (Spark => Elasticsearch indexer etc)
