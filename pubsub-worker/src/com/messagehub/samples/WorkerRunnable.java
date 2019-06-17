/**
 * Copyright 2018 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corp. 2018
 */
package com.messagehub.samples;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;

public class WorkerRunnable implements Runnable {
	private static final Logger logger = Logger.getLogger(WorkerRunnable.class);

	private final KafkaConsumer<String, String> kafkaConsumer;
	private final KafkaProducer<String, String> kafkaProducer;
	private volatile boolean closing = false;
	private final String resultTopic;
	private final AmazonS3 cos;

	public WorkerRunnable(Properties producerProperties, Properties consumerProperties, String workTopic, String resultTopic, AmazonS3 cos) {
		this.cos = cos;
		this.resultTopic = resultTopic;

		// Create a Kafka consumer with the provided client configuration
		kafkaConsumer = new KafkaConsumer<String, String>(consumerProperties);
		// Create a Kafka producer with the provided client configuration
		kafkaProducer = new KafkaProducer<String, String>(producerProperties);

		// Checking for topic existence before subscribing
		List<PartitionInfo> partitions = kafkaConsumer.partitionsFor(workTopic);
		if (partitions == null || partitions.isEmpty()) {
			logger.log(Level.ERROR, "Topic '" + workTopic + "' does not exists - application will terminate");
			kafkaConsumer.close();
			throw new IllegalStateException("Topic '" + workTopic + "' does not exists - application will terminate");
		} else {
			logger.log(Level.INFO, partitions.toString());
		}

		kafkaConsumer.subscribe(Arrays.asList(workTopic));
	}

	@Override
	public void run() {
		logger.log(Level.INFO, WorkerRunnable.class.toString() + " is starting.");

		try {
			while (!closing) {
				try {
					// Poll on the Kafka consumer, waiting up to 3 secs if
					// there's nothing to consume.
					ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(3000));

					if (records.isEmpty()) {
						logger.log(Level.INFO, "No messages consumed.");
					} else {
						// Iterate through all the messages received and print
						// their content
						for (ConsumerRecord<String, String> record : records) {
							logger.log(Level.INFO, "Message consumed: " + record.value());
							processMessage(record.value());
						}
					}

				} catch (final WakeupException e) {
					logger.log(Level.WARN, "Consumer closing - caught exception: " + e);
				} catch (final KafkaException e) {
					logger.log(Level.ERROR, "Sleeping for 5s - Consumer has caught: " + e, e);
					try {
						Thread.sleep(5000); // Longer sleep before retrying
					} catch (InterruptedException e1) {
						logger.log(Level.WARN, "Consumer closing - caught exception: " + e);
					}
				}
			}
		} finally {
			kafkaConsumer.close();
			logger.log(Level.INFO, WorkerRunnable.class.toString() + " has shut down.");
		}
	}

	private void processMessage(String fileName) {
		System.out.println("processMessage: " + fileName);
		try {
			System.out.println("Getting Object from S3");
			System.out.println(cos.getObject(EventStreamsConsole.bucketName, fileName).toString());
			System.out.println("Simulating processing...");
			Thread.sleep(3000);
			// If a partition is not specified, the client will use the default
			// partitioner to choose one.

		} catch (Exception e) {
			System.out.println("Error getting \"" + fileName + "\" from Object Storage with BucketName " + EventStreamsConsole.bucketName);
			System.out.println(e.getMessage());
			return;
		}
		
		ObjectMapper mapper = new ObjectMapper();
		Message message = new Message();
		message.setName(fileName);
		message.setStatus("processed");
		if(System.getenv("MY_POD_NAME")!= null) {
			message.setWorkerID(System.getenv("MY_POD_NAME"));
		} else {
			message.setWorkerID(Long.toString(Thread.currentThread().getId()));
		}
		
		ProducerRecord<String, String> record;
		try {
			System.out.println("Producing result message in the topic: " + resultTopic);
			System.out.println(mapper.writeValueAsString(message));
			record = new ProducerRecord<String, String>(resultTopic, mapper.writeValueAsString(message));
			kafkaProducer.send(record);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		// Send record asynchronously

		System.out.println("processMessage Complete");

	}

	public void shutdown() {
		closing = true;
		kafkaConsumer.wakeup();
		logger.log(Level.INFO, WorkerRunnable.class.toString() + " is shutting down.");
	}
}
