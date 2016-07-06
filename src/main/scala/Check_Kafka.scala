//
//  Author: Hari Sekhon
//  Date: 2016-06-06 22:43:57 +0100 (Mon, 06 Jun 2016)
//
//  vim:ts=4:sts=4:sw=4:et
//
//  https://github.com/harisekhon/nagios-plugin-kafka
//
//  License: see accompanying Hari Sekhon LICENSE file
//
//  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help steer this or other code I publish
//
//  https://www.linkedin.com/in/harisekhon
//

package com.linkedin.harisekhon.kafka

//import com.google.common.io.Resources

import java.io.File
import java.nio.file.Paths

import com.linkedin.harisekhon.CLI
import com.linkedin.harisekhon.Utils._
import org.apache.kafka.common.KafkaException
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

//import org.apache.kafka.common.protocol.SecurityProtocol.PLAINTEXT
//import org.apache.kafka.common.protocol.SecurityProtocol.SASL_PLAINTEXT
//import org.apache.kafka.common.protocol.SecurityProtocol.SASL_SSL

import scala.util.Random

//import java.io.InputStream
import java.util.Properties
import java.util.Arrays
import org.apache.log4j.Level
import org.apache.log4j.Logger
import collection.JavaConversions._
import java.text.SimpleDateFormat

// TODO: temporary CLI args, replace with full CLI inheritence
object CheckKafka extends App {
    if(args.length < 2){
        println("usage: check_kafka <brokers> <topic>")
        System.exit(3)
    }
    // TODO: validate_hostport, must include port
    val brokers = args(0)
    val topic = args(1)
    try {
        // raises
        // Exception in thread "main" org.apache.kafka.common.KafkaException: Failed to construct kafka consumer
        // ...
        // org.apache.kafka.common.config.ConfigException: Invalid url in bootstrap.servers: 192.168.99.100
        val check_kafka = new CheckKafka(
            brokers = brokers, // "192.168.99.100:9092",
            topic = topic, // "nagios-plugin-kafka-test",
            partition = 0,
            acks = "-1",
            // TODO: SASL_PLAINTEXT, SASL_SSL protocol testing
            //        security_protocol = "SASL_PLAINTEXT",
            security_protocol = "PLAINTEXT",
            jaas_config = Option(null)
        )
        check_kafka.run()
    } catch {
        case e: org.apache.kafka.common.KafkaException => {
            println("Caught Kafka Exception: ")
            e.printStackTrace
            System.exit(2)
        }
        case _: Throwable => {
            println("Unexpected exception:")
            e.printStackTrace
            System.exit(2)
        }
    }
}

class CheckKafka(
                    val brokers: String = "localhost:9092",
                    val topic: String = "test",
                    val partition: Int = 0,
                    // ensure all ISRs have written msg
                    val acks: String = "-1",
                    val security_protocol: String = "PLAINTEXT",
                    var jaas_config: Option[String] = None
                ) {

    val log = Logger.getLogger("CheckKafka")
    // log.setLevel(Level.DEBUG)

    val DEFAULT_JAAS_FILE = "kafka_cli_jaas.conf"
    val HDP_JAAS_PATH = "/usr/hdp/current/kafka-broker/config/kafka_client_jaas.conf"

    val srcpath = new File(classOf[CheckKafka].getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
    val jar = if (srcpath.toString.contains("/target/")) {
        srcpath.getParentFile().getParentFile()
    } else {
        srcpath
    }
    val jaas_default_config = Paths.get(jar.getParentFile().getAbsolutePath(), "conf", DEFAULT_JAAS_FILE).toString
    val jaas_prop = System.getProperty("java.security.auth.login.config")
    if (!jaas_config.isEmpty) {
        log.info(s"using JAAS config file arg '$jaas_config'")
    } else if (jaas_prop != null) {
        val jaas_file = new File(jaas_prop)
        if (jaas_file.exists() && jaas_file.isFile()) {
            jaas_config = Option(jaas_prop)
            log.info(s"using JAAS config file from System property java.security.auth.login.config = '$jaas_config'")
        } else {
            log.warn(s"JAAS path specified in System property java.security.auth.login.config = '$jaas_prop' does not exist!")
        }
    }
    if (jaas_config.isEmpty) {
        val hdp_jaas_file = new File(HDP_JAAS_PATH)
        if (hdp_jaas_file.exists() && hdp_jaas_file.isFile()) {
            log.info(s"found HDP Kafka kerberos config '$HDP_JAAS_PATH'")
            jaas_config = Option(HDP_JAAS_PATH)
        }
    }
    if (jaas_config.isEmpty) {
        val jaas_default_file = new File(jaas_default_config)
        if (jaas_default_file.exists() && jaas_default_file.isFile()) {
            log.info(s"using default JaaS config file '$jaas_default_config'")
            jaas_config = Option(jaas_default_config)
        } else {
            log.warn("cannot find default JAAS file and none supplied")
        }
    }
    if (jaas_config.isDefined) {
        System.setProperty("java.security.auth.login.config", jaas_config.get)
    } else {
        log.warn("no JAAS config defined")
    }

    val uuid = java.util.UUID.randomUUID.toString
    val epoch = System.currentTimeMillis()
    // comes out the same whether specifying single, double or triple data digits
    val date = new SimpleDateFormat("yyyy-dd-MM HH:MM:ss.SSS Z").format(epoch)
    val id: String = s"Hari Sekhon check_kafka (scala) - random token=$uuid, $date"

    // enforce random group id
    //    if(props.getProperty("group.id") == null) {
    //        log.debug(s"group.id not set, creating random group id")
    //    }

    val msg = s"test message generated by $id"
    log.info(s"test message => '$msg'")

    val topic_partition = new TopicPartition(topic, partition)
    var last_offset: Long = 0

    val consumer_props = new Properties
    // old
    //    consumer_props.put("metadata.broker.list", broker_list)
    consumer_props.put("bootstrap.servers", brokers)
    consumer_props.put("security.protocol", security_protocol)

    // works without this
    val group_id: String = s"$uuid, $date"
    log.info(s"group id='$group_id'")
    //    consumer_props.put("group.id", group_id)

    consumer_props.put("enable.auto.commit", "true");
    consumer_props.put("auto.commit.interval.ms", "100");
    consumer_props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumer_props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

    // TODO: actually fail fast on connection refused etc
    // trying to fail fast
    consumer_props.put("request.timeout.ms", "1000")
    consumer_props.put("reconnect.backoff.ms", "0")
    consumer_props.put("retry.backoff.ms", "0")
    consumer_props.put("fetch.max.wait.ms", "900")
    // must be set lower than session timeout
    consumer_props.put("heartbeat.interval.ms", "100")
    consumer_props.put("session.timeout.ms", "900")
    // WARN  - The configuration consumer.timeout.ms = 1000 was supplied but isn't a known config.
    //    consumer_props.put("socket.timeout.ms", "1000")
    //    consumer_props.put("consumer.timeout.ms", "1000") // msg must be available within this window
    //    consumer_props.put("timeout.ms", "5000") // 5 secs for ISR acks
    //    consumer_props.put("metadata.fetch.timeout.ms", "1000") // 1 sec for metadata on topic connect

    // Cannot use 0.8 consumers as they don't support the
    log.info("creating Kafka consumer")
    // XXX: TODO: try switch to SimpleConsumer to be able to get rejected properly, also better to be backwards compatible with 0.8 systems
    val consumer = new KafkaConsumer[String, String](consumer_props)
    //    var consumer: KafkaConsumer[String, String] //= new KafkaConsumer[String, String](props)

    val producer_props = new Properties
    producer_props.put("bootstrap.servers", brokers)
    producer_props.put("client.id", "CheckKafka")
    producer_props.put("enable.auto.commit", "true");
    producer_props.put("auto.commit.interval.ms", "100");
    // old
    //    producer_props put("request.required.acks", required_acks)
    producer_props put("acks", acks)
    producer_props.put("security.protocol", security_protocol)
    producer_props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producer_props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    log.info("creating Kafka producer")
    //    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)
    val producer = new KafkaProducer[String, String](producer_props)

    def run(): Unit = {
        val start_time = System.currentTimeMillis()
        subscribe(topic)
        val start_write = System.currentTimeMillis()
        produce(topic, msg)
        // if clock gets reset and this become negative I'm not handling it as that should be a super rare one time occurrence
        // unless perhaps there are a lot of NTPd time steps to bring time back inline, but anyway that shouldn't be
        // a regular occurrence that affects this program
        val write_time = (System.currentTimeMillis() - start_write) / 1000.0
        val read_start_time = System.currentTimeMillis()
        consume(topic)
        val end_time = System.currentTimeMillis()
        val read_time = (end_time - read_start_time) / 1000.0
        val total_time = (end_time - start_time) / 1000.0
        val plural = if (brokers.split("\\s+,\\s+").length > 1) "s" else ""
        println(s"OK: Kafka broker${plural} successfully returned unique message, write_time=${write_time}s, read_time=${read_time}s, total_time=${total_time}s | write_time=${write_time}s, read_time=${read_time}s, total_time=${total_time}s")
    }

    def subscribe(topic: String = topic): Unit = {
        // conflicts with partition assignment
        //        log.debug(s"subscribing to topic $topic")
        //        consumer.subscribe(Arrays.asList(topic))
        log.info(s"consumer assigning topic '$topic' partition '$partition'")
        consumer.assign(Arrays.asList(topic_partition))
        //        consumer.assign(Arrays.asList(partition))
        // not connected to port so no conn refused at this point
        // loops from here indefinitely if connection refused
        last_offset = consumer.position(topic_partition)
    }

    def produce(topic: String = topic, msg: String = msg): Unit = {
        //        InputStream props = Resources.getResource("file.properties").openStream()
        //        try{
        log.info(s"sending message to topic $topic partition $partition")
        producer.send(new ProducerRecord[String, String](topic, partition, id, msg)) // key and partition optional
        log.info("flushing")
        producer.flush()
        log.info("closing producer")
        producer.close() // blocks until msgs are sent
        //        } catch(Throwable t){
        //            println("%s", t.getStackTrace)
        //        }
        //        finally {
        //            producer.close() // blocks until msgs are sent
        //        }
    }

    def consume(topic: String = topic): Unit = {
        log.info(s"seeking to last known offset $last_offset")
        consumer.seek(topic_partition, last_offset)
        log.info(s"consuming from offset $last_offset")
        val records: ConsumerRecords[String, String] = consumer.poll(200) // ms
        log.info("closing consumer")
        consumer.close()
        val consumed_record_count: Int = records.count()
        log.info(s"consumed record count = $consumed_record_count")
        assert(consumed_record_count != 0)
        var msg2: String = null
        for (record: ConsumerRecord[String, String] <- records) {
            val record_topic = record.topic()
            val value = record.value()
            log.info(s"found message, topic '$record_topic', value = '$value'")
            assert(topic.equals(record_topic))
            if (msg.equals(value)) {
                msg2 = value
            }
        }
        log.info(s"message returned: $msg2")
        log.info(s"message expected: $msg")
        if (msg2 == null) {
            println("CRITICAL: message not returned by Kafka")
            System.exit(2)
        } else if (!msg.equals(msg2)) {
            println("CRITICAL: message returned does not equal message sent!")
            System.exit(2)
        }
    }

}
