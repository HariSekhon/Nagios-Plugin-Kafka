//
//  Author: Hari Sekhon
//  Date: 2016-06-06 22:43:57 +0100 (Mon, 06 Jun 2016)
//
//  vim:ts=2:sts=2:sw=2:et
//
//  https://github.com/harisekhon/nagios-plugin-kafka
//
//  License: see accompanying Hari Sekhon LICENSE file
//
//  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help steer this or other code I publish
//
//  https://www.linkedin.com/in/harisekhon
//

package com.linkedin.harisekhon

//import com.google.common.io.Resources

//import com.linkedin.harisekhon.Utils._

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition

import scala.util.Random
//import java.io.InputStream
import java.util.Properties
import java.util.Arrays
import org.apache.log4j.Level
import org.apache.log4j.Logger
import collection.JavaConversions._

object CheckKafka extends App {
    val check_kafka = new CheckKafka(
                                     broker_list = "192.168.99.100:9092",
                                     topic = "nagios-plugin-kafka-test"
                                     )
//    check_kafka.subscribe()
//    check_kafka.produce()
//    check_kafka.consume()
}

class CheckKafka(
        val broker_list: String = "localhost:9092",
        val topic: String = "test",
        val partition: Int = 0,
        // change default to -1 to ensure all ISRs have written msg
        val required_acks: String = "1"){

    val log = Logger.getLogger("CheckKafka")
     // set in log4j.properties
//    log.setLevel(Level.DEBUG)
    // TODO: split consumer + producer values
    val props = new Properties
//    props.put("metadata.broker.list", broker_list)
    props.put("bootstrap.servers", broker_list)
    props.put("client.id", "CheckKafka")
    props put("request.required.acks", required_acks)
    props.put("key.serializer",   "org.apache.kafka.common.serialization.StringSerializer")
    props.put("key.deserializer",   "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    //    val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)
//    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)
    val msg = "test message generated by nagios-plugin-kafka"

//    def subscribe(topic: String = topic): Unit = {
        if(props.getProperty("group.id") == null){
            val group_id: String = "group-id-random-" + new Random().nextInt(100000)
            props.setProperty("group.id", group_id)
            log.debug(s"group.id not set, creating random group id $group_id")
        }
        log.debug("creating Kafka consumer")
        val consumer = new KafkaConsumer[String, String](props)
//        log.debug(s"subscribing to topic $topic")
//        consumer.subscribe(Arrays.asList(topic))
        val topic_partition = new TopicPartition(topic, partition)
        log.debug(s"assigning partition $partition")
        consumer.assign(Arrays.asList(topic_partition))
//        consumer.assign(Arrays.asList(partition))
        val latest_offset = consumer.position(topic_partition)
//    }

//    def produce(topic: String = topic, msg: String = msg): Unit = {
        //        InputStream props = Resources.getResource("file.properties").openStream()
        log.debug("creating Kafka producer")
//        try{
        val producer = new KafkaProducer[String, String](props)
        log.debug("sending message")
        producer.send(new ProducerRecord[String, String](topic, msg)) // key and partition optional
        log.debug("flushing")
        producer.flush()
        log.debug("closing producer")
        producer.close() // blocks until msgs are sent
//        } catch(Throwable t){
//            println("%s", t.getStackTrace)
//        }
//        finally {
//            producer.close() // blocks until msgs are sent
//        }
//    }
//    }

//    def consume(topic: String = topic): Unit = {
        log.debug("consuming")
        val records: ConsumerRecords[String, String] = consumer.poll(200) // ms
        log.debug("closing consumer")
        consumer.close()
        val consumed_record_count: Int = records.count()
        log.debug(s"consumed record count = $consumed_record_count")
        assert(consumed_record_count != 0)
        var msg2: String = null
        for(record: ConsumerRecord[String, String] <- records){
            val record_topic = record.topic()
            log.debug(s"found message with topic $record_topic")
            assert(topic.equals(record_topic))
            if(msg.equals(record.value())){
                msg2 = record.value()
            }
        }
         log.debug(s"message returned: $msg2")
         log.debug(s"message expected: $msg")
        assert(msg.equals(msg2))
        println("OK: successfully returned msg by Kafka")
//    }

}