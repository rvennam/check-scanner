var COS_SDK = require('ibm-cos-sdk');

var cosCredentials, cos;

if(process.env.OBJECTSTORAGE_CREDENTIALS) {
  console.log('Found Object Storage credentials in OBJECTSTORAGE_CREDENTIALS env var')
  cosCredentials = JSON.parse(process.env.OBJECTSTORAGE_CREDENTIALS);
} else {
  console.log('Missing env var OBJECTSTORAGE_CREDENTIALS, trying credentials.json');
  try {
    cosCredentials = require('./credentials.json').OBJECTSTORAGE_CREDENTIALS
   }
   catch (e) {
    console.log('Object Storage credentials not found!')
    return;
   }
 }

 var cos_config =   {
  'endpoint': 's3.us.cloud-object-storage.appdomain.cloud',
  'apiKeyId': cosCredentials.apikey,
  'ibmAuthEndpoint': 'https://iam.cloud.ibm.com/oidc/token',
  'serviceInstanceId': cosCredentials.resource_instance_id
};


var cos = new COS_SDK.S3(cos_config);


module.exports = cos;