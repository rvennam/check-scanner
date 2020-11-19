# Check Scanner using OpenShift, Object Storage and Event Streams

This sample application uses an Apache Kafka based messaging service to orchestrate long running workloads to applications. This pattern is used to decouple your application allowing greater control over scaling and performance. Event Streams can be used to queue up the work to be done without impacting the producer applications, making it an ideal system for long-running tasks.

You will simulate this pattern using a check processing example. First you will create a UI application which will be used to upload checks to Object Storage and generate messages indicating work to be done. Next, you will create a separate worker application which will asynchronously process the user uploaded checks when it receives messages.

**Check Scanner** is composed of:
- **Check Upload** UI microservice
- **Check Processor** worker microservice 
- **Object Storage** for storing files
- **Event Streams** as a work queue


![](./assets/Architecture.png)

1. The user uploads an image of a check using the **Check Upload** UI application
2. File is saved in IBM Cloud Object Storage
3. Message is sent to IBM Cloud Event Streams topic indicating the new file is awaiting processing.
4. When ready, **Check Processor** application receives the message and begins processing the new image.
5. When complete, it updates the image in IBM Cloud Object Storage.
