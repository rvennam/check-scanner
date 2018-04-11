var AWS = require('ibm-cos-sdk');
var request = require('request');

var cosCredentials, cos;

if(process.env.OBJECTSTORAGE_CREDENTIALS) {
  console.log('Found Object Storage credentials in OBJECTSTORAGE_CREDENTIALS env var')
  cosCredentials = JSON.parse(process.env.OBJECTSTORAGE_CREDENTIALS);
} else {
  console.log('Missing env var OBJECTSTORAGE_CREDENTIALS, using credentials.json');
  cosCredentials = require('./credentials.json').OBJECTSTORAGE_CREDENTIALS
 }

module.exports = new Promise(function(resolve, reject){
  request(cosCredentials.endpoints, function (error, response, body) {
    if(error) reject(error);
    var endpoint = JSON.parse(body)['service-endpoints']['cross-region']['us']['public']['us-geo'];
    console.log('COS Endpoint: ' + endpoint)
    var config =   {
      'endpoint': endpoint,
      'apiKeyId': cosCredentials.apikey,
      'ibmAuthEndpoint': 'https://iam.ng.bluemix.net/oidc/token',
      'serviceInstanceId': cosCredentials.resource_instance_id
    };
  
    cos = new AWS.S3(config);
    resolve(cos)
  });
});