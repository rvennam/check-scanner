var express = require('express');
var router = express.Router();

var messageHubInstance = require('../messagehub');
var cosInstance = require('../objectStorage');
var config = require('../config.js');

var bucketName = config.COSBucketName;

/* GET files listing. */
router.get('/', function (req, res, next) {
  console.log(cosInstance);
  cosInstance.listObjects({
    Bucket: bucketName
  }, (err, data) => err ? console.log(err) : res.send(data));

});

/* POST new files */
router.post('/', function (req, res) {
  if(!req.files.uploadedFile){
    res.send('Error, no file uploaded');
    return;
  }
  console.log('Uploading file : ' + req.files.uploadedFile.name);
  cosInstance
    .putObject({Bucket: bucketName, Key: req.files.uploadedFile.name, Body: req.files.uploadedFile.data})
    .promise()
    .then(() => {
      console.log(req.files.uploadedFile.name + ' uploaded to Object Storage');
      messageHubInstance.onFileUploaded(req.files.uploadedFile.name);      
    })
    .catch((error) => {
      console.log(`Did you create a bucket with name "${bucketName}"?`);
      console.log(error);
    });
  
  res.json({name: req.files.uploadedFile.name, status: 'awaiting'});
  return ;
});

module.exports = router;
