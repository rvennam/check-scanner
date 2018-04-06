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
    res.send('Error, no file uploaded');
    return;
  }
  console.log('Uploading file : ' + req.files.uploadedFile.name);
  cosInstance
    .putObject({Bucket: bucketName, Key: req.files.uploadedFile.name, Body: req.files.uploadedFile.data})
    .promise()
    .then(() => {
      console.log(req.files.uploadedFile.name + ' uploaded to Object Storage');
      messageHubInstance.producerInstance.produce('work-topic', req.files.uploadedFile.name)
      .fail(function (error) {
        console.log('ERROR ', e);
        throw new Error(error);
        });
        console.log('Message sent to Message Hub');
      }
    );
  
  res.json({name: req.files.uploadedFile.name, status: 'awaiting'});
  return ;
});


var consumerInterval = setInterval(function() {
  messageHubInstance.consumerInstance.get('result-topic')
    .then(function(messages) {
      console.log('Received message: ', messages);
      if(!Array.isArray(messages)){
        console.log('Invalid message');
        return;
      }
      messages.forEach(function(message) {
        var fileStatus;
        try {
          fileStatus = JSON.parse(message);
        } catch(e) {
          console.log('Error parsing message');
        }
        console.log('Emitting socket.io message ' , fileStatus);
        io.emit('file-status', fileStatus);
      });
    })
    .fail(function(error) {
      console.log('Error!', error);
    });
}, 2000);


module.exports = router;
