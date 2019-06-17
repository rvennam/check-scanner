/**
 * Copyright 2015-2018 IBM
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
 * (c) Copyright IBM Corp. 2015-2018
 */
package com.ibm.cloud.samples.pubsub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.objectstorage.SDKGlobalConfiguration;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.samples.pubsub.credentials.EventStreamsCredentials;
import com.ibm.cloud.samples.pubsub.credentials.ObjectStorageCredentials;

/**
 * Console-based sample interacting with Message Hub, authenticating with
 * SASL/PLAIN over an SSL connection.
 *
 * @author IBM
 */
public class EventStreamsConsole {

	private static final String WORK_TOPIC_NAME = "work-topic";
	private static final String RESULT_TOPIC_NAME = "result-topic";
	private static final Logger logger = Logger.getLogger(EventStreamsConsole.class);

	private static Thread consumerThread = null;
	private static WorkerRunnable consumerRunnable = null;
	private static String resourceDir;

	public static String bucketName;

	private static AmazonS3 cos;

	// add shutdown hooks (intercept CTRL-C etc.)
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.log(Level.WARN, "Shutdown received.");
				shutdown();
			}
		});
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.log(Level.ERROR, "Uncaught Exception on " + t.getName() + " : " + e, e);
				shutdown();
			}
		});
	}

	static final Properties getCommonConfigs(String boostrapServers, String apikey) {
		Properties configs = new Properties();
		configs.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
		configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
		configs.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
		configs.put(SaslConfigs.SASL_JAAS_CONFIG,
				"org.apache.kafka.common.security.plain.PlainLoginModule required username=\"token\" password=\""
						+ apikey + "\";");
		configs.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
		configs.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
		configs.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
		return configs;
	}

	static final Properties getAdminConfigs(String bootstrapServers, String apikey) {
		Properties configs = new Properties();
		configs.put(ConsumerConfig.CLIENT_ID_CONFIG, "pubsub-worker");
		configs.put(AdminClientConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
		configs.putAll(getCommonConfigs(bootstrapServers, apikey));
		return configs;
	}

	static final Properties getProducerConfigs(String bootstrapServers, String apikey) {
		Properties configs = new Properties();
		configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");
		configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");
		configs.put(ProducerConfig.CLIENT_ID_CONFIG, "pubsub-producer");
		configs.put(ProducerConfig.ACKS_CONFIG, "-1");
		configs.put(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
		configs.putAll(getCommonConfigs(bootstrapServers, apikey));
		return configs;
	}

	static final Properties getConsumerConfigs(String bootstrapServers, String apikey) {
		Properties configs = new Properties();
		configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		configs.put(ConsumerConfig.CLIENT_ID_CONFIG, "pubsub-consumer");
		configs.put(ConsumerConfig.GROUP_ID_CONFIG, "pubsub-group");
		configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		configs.put(ConsumerConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
		configs.putAll(getCommonConfigs(bootstrapServers, apikey));
		return configs;
	}

	public static void main(String args[]) {
		try {
			final String userDir = System.getProperty("user.dir");
			final String eventStreamsCredentials = System.getenv("EVENTSTREAMS_CREDENTIALS"); // JSON string with IBM
																								// Event Streams
																								// credentials
			final String objectStorageCredentials = System.getenv("OBJECTSTORAGE_CREDENTIALS"); // JSON string with IBM
																								// Object Storage
																								// credentials
			final Properties cosProperties = new Properties();
			resourceDir = userDir + File.separator + "resources";

			try {
				InputStream propsStream = new FileInputStream(resourceDir + File.separator + "cos.properties");
				cosProperties.load(propsStream);
				propsStream.close();
			} catch (IOException e) {
				logger.log(Level.ERROR, "Could not load properties from file");
				logger.log(Level.ERROR, e.getMessage());
				return;
			}
			String endpoint_url = cosProperties.getProperty("endpoint.url");
			String location = cosProperties.getProperty("location");
			bucketName = cosProperties.getProperty("bucket.name");

			SDKGlobalConfiguration.IAM_ENDPOINT = "https://iam.cloud.ibm.com/oidc/token";
			String cos_api_key = null;
			String service_instance_id = null;

			if (eventStreamsCredentials == null || objectStorageCredentials == null) {
				logger.log(Level.ERROR, "Credentials missing");
				System.exit(-1);
			}
			ObjectMapper mapper = new ObjectMapper();

			ObjectStorageCredentials cos_credentials = mapper.readValue(objectStorageCredentials,
					ObjectStorageCredentials.class);
			cos_api_key = cos_credentials.getApikey();
			service_instance_id = cos_credentials.getResourceInstanceId();

			// create the COS client
			cos = CosHelper.createClient(cos_api_key, service_instance_id, endpoint_url, location);

			EventStreamsCredentials credentials = mapper.readValue(eventStreamsCredentials,
					EventStreamsCredentials.class);
			AdminClient admin = AdminClient
					.create(getAdminConfigs(credentials.getBootstrapServers(), credentials.getApiKey()));
			logger.log(Level.INFO, "Kafka Endpoints: " + credentials.getBootstrapServers());

			System.out.println(getAdminConfigs(credentials.getBootstrapServers(), credentials.getApiKey()));

			try {
				NewTopic workTopic = new NewTopic(WORK_TOPIC_NAME, 1, (short) 3);
				CreateTopicsResult ctr = admin.createTopics(Collections.singleton(workTopic));
				ctr.all().get(10, TimeUnit.SECONDS);
			} catch (ExecutionException tee) {
				logger.log(Level.INFO, WORK_TOPIC_NAME + " already exists");
			}

			try {
				NewTopic resultTopic = new NewTopic(RESULT_TOPIC_NAME, 1, (short) 3);
				CreateTopicsResult ctr = admin.createTopics(Collections.singleton(resultTopic));
				ctr.all().get(10, TimeUnit.SECONDS);
			} catch (ExecutionException tee) {
				logger.log(Level.INFO, RESULT_TOPIC_NAME + " already exists");
			}

			Properties producerProperties = getProducerConfigs(credentials.getBootstrapServers(),
					credentials.getApiKey());
			Properties consumerProperties = getConsumerConfigs(credentials.getBootstrapServers(),
					credentials.getApiKey());
			consumerRunnable = new WorkerRunnable(producerProperties, consumerProperties, WORK_TOPIC_NAME,
					RESULT_TOPIC_NAME, cos);
			consumerThread = new Thread(consumerRunnable, "Consumer Thread");
			consumerThread.start();

			logger.log(Level.INFO, "EventStreamsConsole will run until interrupted.");
		} catch (Exception e) {
			logger.log(Level.ERROR, "Exception occurred, application will terminate", e);
			System.exit(-1);
		}
	}

	/*
	 * convenience method for cleanup on shutdown
	 */
	private static void shutdown() {
		if (consumerRunnable != null)
			consumerRunnable.shutdown();
		if (consumerThread != null)
			consumerThread.interrupt();
	}

}
