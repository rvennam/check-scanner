var MessageHub = require('message-hub-rest');

var messageHubCredentials;

if(process.env.MESSAGEHUB_CREDENTIALS) {
  console.log('Found MessageHub credentials in MESSAGEHUB_CREDENTIALS env var')
  messageHubCredentials = JSON.parse(process.env.MESSAGEHUB_CREDENTIALS);
} else {
  console.log('Missing env var MESSAGEHUB_CREDENTIALS, using credentials.json');
  messageHubCredentials = require('./credentials.json').MESSAGEHUB_CREDENTIALS
 }


var messageHubService = {
  'messagehub': [
    {
      'label': 'messagehub',
      'credentials': messageHubCredentials
    }
  ]
};
var messageHubInstance = {};

messageHubInstance.producerInstance = new MessageHub(messageHubService);
console.log('Message Hub producer instance instantiated');

messageHubInstance
  .producerInstance
  .consume('my_consumer_group', 'my_consumer_instance', {'auto.offset.reset': 'largest'})
  .then(function (response) {
    console.log('Message Hub consumer instance instantiated');
    messageHubInstance.consumerInstance = response[0];
  })
  .fail(function (error) {
    console.log('ERROR creating consumer instance');
    throw new Error(error);
  });

module.exports = messageHubInstance;
