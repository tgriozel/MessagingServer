# What is this?

This is a very simple messaging server where users can send (queued) messages to each other.
Basic authentication is implemented. Access is done through a RESTful API.

# Building and running

You will just need sbt for that:  
`sbt assembly`  
`sbt run`  

To run the tests:  
`sbt test`

# TODO

Mandatory:
- Setting up SSL

Optional:
- Add anonymous message recipients based on properties only
- Add persistence
- Implement the message queuing with something like Amazon SQS
- Whatever you would like to add (Kafka, etc)
