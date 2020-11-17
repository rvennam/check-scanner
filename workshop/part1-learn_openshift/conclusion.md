# Conclusion

In this tutorial, you learned how you can use Kafka based Event Streams to implement a producer-consumer pattern. This allows the web application to be fast and offload the heavy processing to other applications. When work needs to be done, the producer (web application) creates messages and the work is load balanced between one or more workers who subscribe to the messages.