# Asynchronous data processing using object storage and pub/sub messaging

This sample application uses an Apache Kafka based messaging service to orchestrate long running workloads to applications running in a Kubernetes cluster. This pattern is used to decouple your application allowing greater control over scaling and performance. Event Streams can be used to queue up the work to be done without impacting the producer applications, making it an ideal system for long-running tasks.

Refer to [this tutorial](https://cloud.ibm.com/docs/tutorials?topic=solution-tutorials-pub-sub-object-storage) for instructions.

## License

See License.txt for license information.