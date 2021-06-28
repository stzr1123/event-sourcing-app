# Event Sourcing App
Following the instruction from the book *Practical Event Sourcing with Scala* by Denis Kalinin.

This is an application which experiments with the event sourcing pattern. 

Instead of implementing the traditional CRUD functionality through exposed APIs which directly act on a database, here we implement consumers and producers. These services publish events to Kafka and modify them asynchronously. 

Relevant backend state changes are propagated to the frontend through Server-Sent Events instead of being updated through traditional polling endpoints.

## Application Overview
* Backend uses Play 2.8
* Kafka as messaging and persistence layer
* Akka Streams Kafka client 
* React Frontend is served directly from Play
* Users are stored in Postgres
* Events are also being persisted to Postgres for querying purposes
* JSON as message serialization

## Development setup
```bash
$ cd stack && docker compose up
$ cd ui && npm run watch
$ sbt run
```
