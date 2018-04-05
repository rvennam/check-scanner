var AWS = require('ibm-cos-sdk');

var cosCredentials;

if(process.env.OBJECTSTORAGE_CREDENTIALS) {
  console.log('Found Object Storage credentials in OBJECTSTORAGE_CREDENTIALS env var')
  cosCredentials = JSON.parse(process.env.OBJECTSTORAGE_CREDENTIALS);
} else {
  console.log('Missing env var OBJECTSTORAGE_CREDENTIALS, using credentials.json');
  cosCredentials = require('./credentials.json').OBJECTSTORAGE_CREDENTIALS
 }


 var config =   {
    'endpoint': 's3-api.us-geo.objectstorage.softlayer.net',
    'apiKeyId': cosCredentials.apikey,
    'ibmAuthEndpoint': 'https://iam.ng.bluemix.net/oidc/token',
    'serviceInstanceId': 'crn:v1:bluemix:public:cloud-object-storage:global:a/d50fb4700dcb8797f8f2efd18a775fb4:70a66f1f-46ec-4e73-acf4-a4ecc5a4be63::'
  };

var cos = new AWS.S3(config);


module.exports = cos;