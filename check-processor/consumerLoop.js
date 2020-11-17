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
 * © Copyright IBM Corp. 2015-2018
 */

var consumer;
var consumerLoop;

var exports = module.exports = {};
exports.consumerLoop = consumerLoop;

/**
 * Constructs a KafkaConsumer and registers listeners on the most common events
 * 
 * @param {object} Kafka - an instance of the node-rdkafka module
 * @param {object} consumer_opts - consumer configuration
 * @param {string} topicName - name of the topic to consumer from
 * @param {function} shutdown - shutdown function
 * @return {KafkaConsumer} - the KafkaConsumer instance
 */
exports.buildConsumer = function (Kafka, consumer_opts, topicName, shutdown) {
    var topicOpts = {
        'auto.offset.reset': 'latest'
    };

    // COS
    var cosInstance = require('./objectStorage');
    var bucketName = process.env.COSBUCKETNAME;;
    // Bucket name not set..just use the first available bucket
    if(!process.env.COSBUCKETNAME && cosInstance){
      cosInstance.listBuckets(function(err, data) {
        if (err) {
          console.log("Unable to find a bucket.", err);
        } else {
          if(data.Buckets.length >0){
            bucketName = data.Buckets[0].Name;
            console.log("COSBUCKETNAME env var not set, but found a bucket: " +  bucketName);
          }
        }
      });
    }
    
    console.log("Bucket : " + bucketName);

    //tesseract

    const tesseract = require("node-tesseract-ocr")
    var fs = require('fs');

    consumer = new Kafka.KafkaConsumer(consumer_opts, topicOpts);

    // Register listener for debug information; only invoked if debug option set in driver_options
    consumer.on('event.log', function (log) {
        console.log(log);
    });

    // Register error listener
    consumer.on('event.error', function (err) {
        console.error('Error from consumer:' + JSON.stringify(err));
    });

    var consumedMessages = []
    // Register callback to be invoked when consumer has connected
    consumer.on('ready', function () {
        console.log('The consumer has connected.');

        // request metadata for one topic
        consumer.getMetadata({
            topic: topicName,
            timeout: 10000
        },
            function (err, metadata) {
                if (err) {
                    console.error('Error getting metadata: ' + JSON.stringify(err));
                    shutdown(-1);
                } else {
                    console.log('Consumer obtained metadata: ' + JSON.stringify(metadata));
                    if (metadata.topics[0].partitions.length === 0) {
                        console.error('ERROR - Topic ' + topicName + ' does not exist. Exiting');
                        shutdown(-1);
                    }
                }
            });

        consumer.subscribe([topicName]);

        consumerLoop = setInterval(function () {
            if (consumer.isConnected()) {
                // The consume(num, cb) method can take a callback to process messages.
                // In this sample code we use the ".on('data')" event listener instead,
                // for illustrative purposes.
                consumer.consume(10);
            }

            if (consumedMessages.length === 0) {
                //console.log('No messages consumed');
            } else {
                for (var i = 0; i < consumedMessages.length; i++) {
                    var m = consumedMessages[i];
                    console.log('Received a new message: topic=' + m.topic + ', partition=' + m.partition + ', offset=' + m.offset + ', key=' + m.key + ', value=' + m.value.toString());
                    var fileName = m.value.toString();
                    console.log(fileName + ' downloaded from Object Storage');
                    cosInstance.getObject({
                        Bucket: bucketName,
                        Key: fileName
                    }, function (err, data) {
                        if (err) {
                            console.log(err)
                            return;
                        } else {
                            console.log("got image!", data)
                            fs.writeFileSync("images/" + fileName, data.Body);
                            tesseract.recognize("images/" + fileName, {
                                lang: "mcr2",
                                oem: 1,
                                psm: 3,
                            })
                                .then(text => {
                                    fs.unlink("images/" + fileName, (err) => {
                                        if (err) {
                                          console.error("Error deleting temp file: " + fileName)
                                        }
                                    })
                                    var newFileName;
                                    try {
                                        var split = text.match("\\[[0-9]{8}.*")[0].replace(/@/g, '').replace(/\[/g, '').split(' ')
                                        newFileName = `${split[0]}:${split[1]}::${fileName}`;
                                        console.log("Extracted Routing Number: " + split[0])
                                        console.log("Extracted Account Number: " + split[0])
                                        console.log("New File Name: " + newFileName);
                                    }
                                    catch(err) {
                                      console.log("Unable to process check");
                                      newFileName = `unknown:unknown::${fileName}`;
                                    }
                                    cosInstance
                                        .putObject({ Bucket: bucketName, Body: data.Body, Key: newFileName })
                                        .promise()
                                        .then(() => {
                                            console.log(newFileName + ' uploaded to Object Storage');
                                        })
                                        .catch((error) => {
                                            console.log(error);
                                        });

                                    cosInstance
                                        .deleteObject({ Bucket: bucketName, Key: fileName })
                                        .promise()
                                        .then(() => {
                                            console.log(fileName + ' deleted from Object Storage');
                                        })
                                        .catch((error) => {
                                            console.log(error);
                                        });

                                })
                                .catch(error => {
                                    console.log(error.message)
                                })
                        }
                    }
                    );

                }
                consumedMessages = [];
            }
        }, 2000);
    });

    // Register a listener to process received messages
    consumer.on('data', function (m) {
        consumedMessages.push(m);
    });
    return consumer;
}
