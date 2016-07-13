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

import com.linkedin.harisekhon.CLI
import com.linkedin.harisekhon.Utils._

import java.io.{File, InputStream, OutputStream, PipedInputStream, PipedOutputStream}
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.{Arrays, Properties}

import org.apache.kafka.common.KafkaException
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

import org.apache.log4j.Logger

import scala.collection.JavaConversions._

// TODO: temporary CLI args, replace with full CLI class inheritance
object CheckKafka extends App {
    if(args.length < 2){
        println("usage: check_kafka <brokers> <topic> [<partition>]")
        System.exit(3)
    }
    // TODO: validate_hostport, must include port
    // TODO: validate_int partition
    val consumer_props = new Properties
    val producer_props = new Properties
    consumer_props.put("bootstrap.servers", args(0))
    producer_props.put("bootstrap.servers", args(0))
    val topic = args(1)
    val partition: Int =
        if(args.length > 2) try {
            Integer.parseInt(args(2))
        } catch {
            case e: NumberFormatException => {
                println("Invalid argument for partition, must be an integer")
                System.exit(3)
                0 // appease type system
            }
        } else {
            0
        }
    try {
        // without port suffix raises the following exception, which we intend to catch and print nicely
        // Exception in thread "main" org.apache.kafka.common.KafkaException: Failed to construct kafka consumer
        // ...
        // org.apache.kafka.common.config.ConfigException: Invalid url in bootstrap.servers: 192.168.99.100
        val check_kafka = new CheckKafka(
            topic = topic,
            partition = partition,
            producer_props = producer_props,
            consumer_props = consumer_props,
            jaas_config = None
        )
        check_kafka.run()
    } catch {
        case e: KafkaException => {
            println("Caught Kafka Exception: ")
            e.printStackTrace()
            System.exit(2)
        }
        case e: Throwable => {
            println("Caught unexpected Exception: ")
            e.printStackTrace()
            System.exit(2)
        }
    }
}

class CheckKafka(
                    val topic: String = "test",
                    val partition: Int = 0,
                    val consumer_props: Properties = new Properties,
                    val producer_props: Properties = new Properties,
                    var jaas_config: Option[String] = None
                ) {

    val log = Logger.getLogger("CheckKafka")

    if(consumer_props eq producer_props){
        throw new IllegalArgumentException("Consumer + Producer props should not be the same object")
    }

    val DEFAULT_JAAS_FILE = "kafka_cli_jaas.conf"
    val HDP_JAAS_PATH = "/usr/hdp/current/kafka-broker/config/kafka_client_jaas.conf"

    val srcpath = new File(classOf[CheckKafka].getProtectionDomain.getCodeSource.getLocation.toURI.getPath)
    val jar = if (srcpath.toString.contains("/target/")) {
        srcpath.getParentFile.getParentFile
    } else {
        srcpath
    }
    val jaas_default_config = Paths.get(jar.getParentFile.getAbsolutePath, "conf", DEFAULT_JAAS_FILE).toString
    val jaas_prop = System.getProperty("java.security.auth.login.config")
    if (jaas_config.nonEmpty) {
        log.info(s"using JAAS config file arg '$jaas_config'")
    } else if (jaas_prop != null) {
        val jaas_file = new File(jaas_prop)
        if (jaas_file.exists() && jaas_file.isFile()) {
            jaas_config = Some(jaas_prop)
            log.info(s"using JAAS config file from System property java.security.auth.login.config = '$jaas_config'")
        } else {
            log.warn(s"JAAS path specified in System property java.security.auth.login.config = '$jaas_prop' does not exist!")
        }
    }
    if (jaas_config.isEmpty) {
        val hdp_jaas_file = new File(HDP_JAAS_PATH)
        if (hdp_jaas_file.exists() && hdp_jaas_file.isFile()) {
            log.info(s"found HDP Kafka kerberos config '$HDP_JAAS_PATH'")
            jaas_config = Some(HDP_JAAS_PATH)
        }
    }
    if (jaas_config.isEmpty) {
        val jaas_default_file = new File(jaas_default_config)
        if (jaas_default_file.exists() && jaas_default_file.isFile()) {
            log.info(s"using default JaaS config file '$jaas_default_config'")
            jaas_config = Some(jaas_default_config)
        } else {
            log.warn("cannot find default JAAS file and none supplied")
        }
    }
    if (jaas_config.nonEmpty) {
        System.setProperty("java.security.auth.login.config", jaas_config.get)
    } else {
        log.warn("no JAAS config defined")
    }

    val uuid = java.util.UUID.randomUUID.toString
    val epoch = System.currentTimeMillis()
    val date = new SimpleDateFormat("yyyy-dd-MM HH:MM:ss.SSS Z").format(epoch)
    val id: String = s"Hari Sekhon check_kafka (scala) - random token=$uuid, $date"

    val msg = s"test message generated by $id"
    log.info(s"test message => '$msg'")

    val topic_partition = new TopicPartition(topic, partition)
    var last_offset: Long = 0

    val consumer_properties: InputStream = getClass.getResourceAsStream("/consumer.properties")
    if(consumer_properties == null) {
        log.error("could not find consumer.properties file")
        System.exit(2)
    }
    val consumer_props_args = consumer_props.clone().asInstanceOf[Properties]
    consumer_props.load(consumer_properties)
    if(log.isDebugEnabled){
        log.debug("Loaded Consumer Properties from resource file:")
        consumer_props.foreach({case (k,v) => log.debug(s"  $k = $v")})
        log.debug("Loading Consumer Property args:")
        consumer_props_args.foreach({case (k,v) => log.debug(s"  $k = $v")})
    }
    val consumer_in = new PipedInputStream
    val consumer_out = new PipedOutputStream(consumer_in)
    new Thread(
        new Runnable(){
            def run(): Unit = {
                consumer_props_args.store(consumer_out, "")
                consumer_out.close()
            }
        }
    ).start()
    consumer_props.load(consumer_in)

    // enforce unique group to make sure we are guaranteed to received our unique message back
    val group_id: String = s"$uuid, $date"
    log.info(s"group id='$group_id'")
    consumer_props.put("group.id", group_id)

    // Cannot use 0.8 consumers as only new 0.9 API supports Kerberos
    log.info("creating Kafka consumer")
    val consumer = new KafkaConsumer[String, String](consumer_props)

    val producer_properties: InputStream = getClass.getResourceAsStream("/producer.properties")
    if(producer_properties == null){
        log.error("could not find producer.properties file")
        System.exit(2)
    }
    val producer_props_args = producer_props.clone().asInstanceOf[Properties]
    producer_props.load(producer_properties)
    if(log.isDebugEnabled){
        log.debug("Loaded Producer Properties from resource file:")
        producer_props.foreach({case (k,v) => log.debug(s"  $k = $v")})
        log.debug("Loading Producer Property args:")
        producer_props_args.foreach({case (k,v) => log.debug(s"  $k = $v")})
    }
    val producer_in = new PipedInputStream()
    val producer_out = new PipedOutputStream(producer_in)
    new Thread(
        new Runnable(){
            def run(): Unit = {
                producer_props_args.store(producer_out, "")
                producer_out.close()
            }
        }
    ).start()
    producer_props.load(producer_in)

    log.info("creating Kafka producer")
    val producer = new KafkaProducer[String, String](producer_props)

    def run(): Unit = {
        val start_time = System.currentTimeMillis()
        subscribe(topic)
        val start_write = System.currentTimeMillis()
        produce(topic, msg)
        val write_time = (System.currentTimeMillis() - start_write) / 1000.0
        val read_start_time = System.currentTimeMillis()
        consume(topic)
        val end_time = System.currentTimeMillis()
        val read_time = (end_time - read_start_time) / 1000.0
        val total_time = (end_time - start_time) / 1000.0
        val plural =
            if (consumer_props.get("bootstrap.servers").isInstanceOf[String] &&
                consumer_props.get("bootstrap.servers").asInstanceOf[String].split("\\s+,\\s+").length > 1)
            {
                "s"
            } else {
                ""
            }
        println(s"OK: Kafka broker$plural successfully returned unique message, write_time=${write_time}s, read_time=${read_time}s, total_time=${total_time}s | write_time=${write_time}s, read_time=${read_time}s, total_time=${total_time}s")
    }

    def subscribe(topic: String = topic): Unit = {
        // conflicts with partition assignment
        // log.debug(s"subscribing to topic $topic")
        // consumer.subscribe(Arrays.asList(topic))
        log.info(s"consumer assigning topic '$topic' partition '$partition'")
        consumer.assign(Arrays.asList(topic_partition))
        // consumer.assign(Arrays.asList(partition))
        // not connected to port so no conn refused at this point
        // TODO: loops from here indefinitely if connection refused, find way to timeout or fail fast
        last_offset = consumer.position(topic_partition)
    }

    def produce(topic: String = topic, msg: String = msg): Unit = {
        log.info(s"sending message to topic $topic partition $partition")
        producer.send(new ProducerRecord[String, String](topic, partition, id, msg)) // key and partition optional
        log.info("flushing")
        producer.flush()
        log.info("closing producer")
        producer.close() // blocks until msgs are sent
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
