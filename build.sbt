//
//  Author: Hari Sekhon
//  Date: 2016-06-06 22:51:45 +0100 (Mon, 06 Jun 2016)
//
//  vim:ts=4:sts=4:sw=4:et
//
//  https://github.com/harisekhon/nagios-plugin-kafka
//
//  License: see accompanying Hari Sekhon LICENSE file
//
//  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help improve or steer this or other code I publish
//
//  http://www.linkedin.com/in/harisekhon
//

name := "check_kafka"

version := "0.1.0"

scalaVersion := "2.10.6"

mainClass := Some("com.linkedin.harisekhon.kafka.CheckKafka")

// unmanagedBase := baseDirectory.value / "lib/target"

libraryDependencies ++= Seq (
    // try using non-assembly jar first
    "com.linkedin.harisekhon" %% "harisekhon-utils" % "1.17.0",
    "commons-cli" % "commons-cli" % "1.3.1",
    // 0.10.0.0 breaks, either hangs or gives this exception:
    // org.apache.kafka.common.protocol.types.SchemaException: Error reading field 'topic_metadata': Error reading array of size 1666657, only 53 bytes available
    "org.apache.kafka" %% "kafka" % "0.9.0.1",
    "log4j" % "log4j" % "1.2.17",
    //"net.sf.jopt-simple" % "jopt-simple" % "4.9"
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
