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
package com.messagehub.samples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.objectstorage.SDKGlobalConfiguration;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.messagehub.samples.bluemix.MessageHubCredentials;
import com.messagehub.samples.bluemix.ObjectStorageCredentials;
import com.messagehub.samples.rest.RESTAdmin;

/**
 * Console-based sample interacting with Message Hub, authenticating with
 * SASL/PLAIN over an SSL connection.
 *
 * @author IBM
 */
public class MessageHubConsoleSample {

	private static final String WORK_TOPIC_NAME = "work-topic";
	private static final String RESULT_TOPIC_NAME = "result-topic";
	private static final Logger logger = Logger.getLogger(MessageHubConsoleSample.class);

	private static Thread consumerThread = null;
	private static WorkerRunnable consumerRunnable = null;
	private static Thread producerThread = null;
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

	public static void main(String args[]) {
		try {
			final String userDir = System.getProperty("user.dir");
			final String messageHubCredentials = System.getenv("MESSAGEHUB_CREDENTIALS"); // JSON string with IBM Message Hub credentials
			final String objectStorageCredentials = System.getenv("OBJECTSTORAGE_CREDENTIALS"); // JSON string with IBM Object Storage credentials
			final Properties clientProperties = new Properties();
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

			String bootstrapServers = null;
			String adminRestURL = null;
			String apiKey = null;
			String user = null;
			String password = null;

			SDKGlobalConfiguration.IAM_ENDPOINT = "https://iam.bluemix.net/oidc/token";
			String cos_api_key = null;
			String service_instance_id = null;

 
			if (messageHubCredentials == null || objectStorageCredentials == null) {
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


			MessageHubCredentials credentials = mapper.readValue(messageHubCredentials, MessageHubCredentials.class);

			bootstrapServers = stringArrayToCSV(credentials.getKafkaBrokersSasl());
			adminRestURL = credentials.getKafkaRestUrl();
			apiKey = credentials.getApiKey();
			user = credentials.getUser();
			password = credentials.getPassword();

			// inject bootstrapServers in configuration, for both consumer and producer
			clientProperties.put("bootstrap.servers", bootstrapServers);

			logger.log(Level.INFO, "Kafka Endpoints: " + bootstrapServers);
			logger.log(Level.INFO, "Admin REST Endpoint: " + adminRestURL);

			createTopics(adminRestURL, apiKey);

			// create the Kafka client
			Properties consumerProperties = getClientConfiguration(clientProperties, "consumer.properties", user, password);
            Properties producerProperties = getClientConfiguration(clientProperties, "producer.properties", user, password);

			consumerRunnable = new WorkerRunnable(producerProperties, consumerProperties, WORK_TOPIC_NAME, RESULT_TOPIC_NAME, cos);
			consumerThread = new Thread(consumerRunnable, "Consumer Thread");
			consumerThread.start();

			logger.log(Level.INFO, "MessageHubConsoleSample will run until interrupted.");
		} catch (Exception e) {
			logger.log(Level.ERROR, "Exception occurred, application will terminate", e);
			System.exit(-1);
		}
	}

	private static void createTopics(String adminRestURL, String apiKey) {
		// Using Message Hub Admin REST API to create and list topics
		// If the topic already exists, creation will be a no-op
		try {
			logger.log(Level.INFO, "Creating the topic " + WORK_TOPIC_NAME);
			String restResponse = RESTAdmin.createTopic(adminRestURL, apiKey, WORK_TOPIC_NAME);
			logger.log(Level.INFO, "Admin REST response :" + restResponse);

			logger.log(Level.INFO, "Creating the topic " + RESULT_TOPIC_NAME);
			restResponse = RESTAdmin.createTopic(adminRestURL, apiKey, RESULT_TOPIC_NAME);
			logger.log(Level.INFO, "Admin REST response :" + restResponse);

			String topics = RESTAdmin.listTopics(adminRestURL, apiKey);
			logger.log(Level.INFO, "Admin REST Listing Topics: " + topics);
		} catch (Exception e) {
			logger.log(Level.ERROR, "Error occurred accessing the Admin REST API " + e, e);
			// The application will carry on regardless of Admin REST errors, as the topic
			// may already exist
		}

	}

	/*
	 * convenience method for cleanup on shutdown
	 */
	private static void shutdown() {
		if (consumerRunnable != null)
			consumerRunnable.shutdown();
		if (producerThread != null)
			producerThread.interrupt();
		if (consumerThread != null)
			consumerThread.interrupt();
	}

	/*
	 * Return a CSV-String from a String array
	 */
	private static String stringArrayToCSV(String[] sArray) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sArray.length; i++) {
			sb.append(sArray[i]);
			if (i < sArray.length - 1)
				sb.append(",");
		}
		return sb.toString();
	}

	/*
	 * Retrieve client configuration information, using a properties file, for
	 * connecting to Message Hub Kafka.
	 */
	static final Properties getClientConfiguration(Properties commonProps, String fileName, String user,
			String password) {
		Properties result = new Properties();
		InputStream propsStream;

		try {
			propsStream = new FileInputStream(resourceDir + File.separator + fileName);
			result.load(propsStream);
			propsStream.close();
		} catch (IOException e) {
			logger.log(Level.ERROR, "Could not load properties from file");
			return result;
		}

		result.putAll(commonProps);
		// Adding in credentials for MessageHub auth
		String saslJaasConfig = result.getProperty("sasl.jaas.config");
		saslJaasConfig = saslJaasConfig.replace("USERNAME", user).replace("PASSWORD", password);
		result.setProperty("sasl.jaas.config", saslJaasConfig);
		return result;
	}

}
