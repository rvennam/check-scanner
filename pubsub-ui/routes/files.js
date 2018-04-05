var express = require('express');
var router = express.Router();

var messageHubInstance = require('../messagehub');
var cosInstance = require('../objectStorage');

var bucketName = 'mywebsite';

/* GET files listing. */
router.get('/', function (req, res, next) {
  cosInstance.listObjects({
    Bucket: bucketName
  }, (err, data) => err ? console.log(err) : res.send(data));

});

/* POST new files */
router.post('/', function (req, res) {
  if(!req.files.uploadedFile){
    res.send('Error');
    return;
  }
  messageHubInstance.producerInstance
    .produce('work-topic', req.files.uploadedFile.name)
    .fail(function (error) {
      throw new Error(error);
    });
  res.json({name: req.files.uploadedFile.name, status: 'awaiting'});
  return cosInstance
    .putObject({Bucket: bucketName, Key: req.files.uploadedFile.name, Body: req.files.uploadedFile.data})
    .promise();
});


var consumerInterval = setInterval(function() {
  messageHubInstance.consumerInstance.get('result-topic')
    .then(function(data) {
      console.log('Data from MH', data);
      var messages = [];
      Array.isArray(data) ? messages = data : messages.push(data);
      messages.forEach(function(message) {
        var fileStatus = JSON.parse(message);
        console.log('Message from MessageHub: ', fileStatus)
        if(fileStatus.name) io.emit('file-status', fileStatus);
      });
    })
    .fail(function(error) {
      throw new Error(error);
    });
}, 2000);


module.exports = router;
