var Kafka = require('node-rdkafka');
var fs = require('fs');

var eventStreamsCredentials;

if (process.env.EVENTSTREAMS_CREDENTIALS) {
  console.log('Found Event Streams credentials in EVENTSTREAMS_CREDENTIALS env var')
  eventStreamsCredentials = JSON.parse(process.env.EVENTSTREAMS_CREDENTIALS);
} else {
  console.log('Missing env var EVENTSTREAMS_CREDENTIALS, trying credentials.json');
  try {
    eventStreamsCredentials = require('./credentials.json').EVENTSTREAMS_CREDENTIALS
  }
  catch (e) {
    console.log('Event Streams credentials not found!')
    return;
  }
}


var ca_location = '/etc/ssl/certs';
if (process.platform === "darwin") {
  ca_location = '/usr/local/etc/openssl@1.1/cert.pem'
} else if (fs.existsSync("/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem")) {
  ca_location = '/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem'
}

// Config options common to all clients
var driver_options = {
  'metadata.broker.list': eventStreamsCredentials.kafka_brokers_sasl.join(','),
  'security.protocol': 'sasl_ssl',
  'ssl.ca.location': process.env.CA_LOCATION || ca_location,
  'sasl.mechanisms': 'PLAIN',
  'sasl.username': 'token',
  'sasl.password': eventStreamsCredentials.api_key,
  'broker.version.fallback': '0.10.0',  // still needed with librdkafka 0.11.6 to avoid fallback to 0.9.0
  'log.connection.close': false
};
var consumer_opts = {
  'client.id': 'pubsub-consumer',
  'group.id': 'pubsub-group'
};

var producer_opts = {
  'client.id': 'pubsub-producer',
  'dr_msg_cb': true  // Enable delivery reports with message payload
};

// Add the common options to client and producer
for (var key in driver_options) {
  consumer_opts[key] = driver_options[key];
  producer_opts[key] = driver_options[key];
}


const producerStream = Kafka.Producer.createWriteStream(producer_opts, {}, {
  topic: 'work-topic',
});
producerStream.on('error', function (err) {
  // Here's where we'll know if something went wrong sending to Kafka
  console.error('Error in our kafka stream');
  console.error(err);
});

// Use the producer to send new work requests
// and dispatch processed events to clients
var eventStreamsInstance = {
  onFileUploaded: (filename) => {
    console.log(`Uploaded ${filename}`);
    if (producerStream.write(Buffer.from(filename))) {
      console.log('Notified of new uploaded file');
    } else {
      console.log('Failed to send message');
    }
  },
  onFileProcessed: (fileStatus) => {
    console.log('Emitting socket.io message', fileStatus);
    io.emit('file-status', fileStatus);
  }
};

var topicOpts = {
  'auto.offset.reset': 'latest'
};
console.log(consumer_opts);
const consumer = new Kafka.KafkaConsumer(consumer_opts, topicOpts);
consumer.on('event.log', function (log) {
  console.log(log);
});
// Register error listener
consumer.on('event.error', function (err) {
  console.error('Error from consumer:' + JSON.stringify(err));
});
const topicName = 'result-topic';
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
        console.log('Message consumed: topic=' + m.topic + ', partition=' + m.partition + ', offset=' + m.offset + ', key=' + m.key + ', value=' + m.value.toString());
        eventStreamsInstance.onFileProcessed(JSON.parse(m.value.toString()));
      }
      consumedMessages = [];
    }
  }, 3000);
});

// Register a listener to process received messages
consumer.on('data', function (m) {
  consumedMessages.push(m);
});

consumer.connect();

module.exports = eventStreamsInstance;
