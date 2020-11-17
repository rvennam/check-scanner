var express = require('express');
var router = express.Router();

var eventStreamsInstance = require('../eventstreams');
var cosInstance = require('../objectStorage');
var config = require('../config.js');

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

console.log("Bucket : " + bucketName)

/* GET files listing. */
router.get('/', function (req, res, next) {
  if(!cosInstance || !bucketName){
    res.send({message: "Object Storage not configured!"})
    return;
  }
  cosInstance.listObjects({
    Bucket: bucketName
  }, (err, data) => err ? console.log(err) : res.send(data.Contents));

});

router.delete('/', function (req,res,next){

  cosInstance.listObjects({Bucket: bucketName}, function (err, data) {
    if (err) {
        console.log("error listing bucket objects "+err);
        return;
    }
    var items = data.Contents;
    for (var i = 0; i < items.length; i += 1) {
        var deleteParams = {Bucket: bucketName, Key: items[i].Key};
        cosInstance.deleteObject(deleteParams, function (err, data) {
          if (err) {
              console.log("delete err " + deleteParams.Key);
          } else {
              console.log("deleted " + deleteParams.Key);
              res.end();
          }
      });
    }
});
})

/* POST new files */
router.post('/', function (req, res) {
  if (!req.files.uploadedFile) {
    res.send('Error, no file uploaded');
    return;
  }
  console.log('Uploading file : ' + req.files.uploadedFile.name);
  cosInstance
    .putObject({ Bucket: bucketName, Key: req.files.uploadedFile.name, Body: req.files.uploadedFile.data })
    .promise()
    .then(() => {
      console.log(req.files.uploadedFile.name + ' uploaded to Object Storage');
      eventStreamsInstance.onFileUploaded(req.files.uploadedFile.name);
    })
    .catch((error) => {
      console.log(`Did you create a bucket with name "${bucketName}"?`);
      console.log(error);
    });

  res.json({ name: req.files.uploadedFile.name, status: 'awaiting' });
  return;
});

module.exports = router;
