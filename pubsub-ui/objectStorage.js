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
    'serviceInstanceId': cosCredentials.resource_instance_id
  };

var cos = new AWS.S3(config);


module.exports = cos;