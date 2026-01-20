# TSPGlobal
Design and implement a service for keeping track of the last price for financial instruments.
Producers will use the service to publish prices and consumers will use it to obtain them.


The price data consists of records with the following fields:
* id: a string field to indicate which instrument this price refers to.
* asOf: a date time field to indicate when the price was determined.
* payload: the price data itself, which is a flexible data structure.

 

Producers should be able to provide prices in batch runs.
The sequence of uploading a batch run is as follows:
1. The producer indicates that a batch run is started
2. The producer uploads the records in the batch run in multiple chunks of 1000 records.
3. The producer completes or cancels the batch run.


Consumers can request the last price record for a given id.
The last value is determined by the asOf time, as set by the producer.
On completion, all prices in a batch run should be made available at the same time.
Batch runs which are cancelled can be discarded.

The service should be resilient against producers which call the service methods in an incorrect order,
or clients which call the service while a batch is being processed.

Technical requirements:

* Provide a working Java application which implements the business requirements
* The service interface should be defined as a Java API, so consumers and producers can be assumed to run in the same JVM.
* For the purpose of the exercise, we are looking for an in-memory solution (rather than one that relies on a database).
* Demonstrate the functionality of the application through unit tests
* You can use any open source libraries. Please include gradle or maven project definitions for the dependencies.

The purpose of this exercise is for you to demonstrate that you can analyse business requirements and convert them into clean code.
Please apply the same standards to the code which you would if this was a production system.
Comments in the code to illustrate design decisions are highly appreciated.
Bonus points can be scored by analysing performance characteristics and considering possible improvements.


Solution : --


                         ┌────────────────────────────────────────────┐
                         │           PriceRecordPublisher             │
                         └────────────────────────────────────────────┘
                                         ▲
                                         │
                         ┌────────────────────────────────────────────┐
                         │           PriceRecordConsumer              │
                         └────────────────────────────────────────────┘
                                         ▲
                                         │
                         ┌────────────────────────────────────────────┐
                         │          PriceRecordServiceAPI             │
                         │      (extends Publisher, Consumer)         │
                         └────────────────────────────────────────────┘
                                         ▲
                                         │ implements
                                         │
                         ┌────────────────────────────────────────────┐
                         │            PriceRecordService              │
                         ├────────────────────────────────────────────┤
                         │ - batches: ConcurrentHashMap<UUID, Batch>  │
                         │ - committedPrices: ConcurrentHashMap        │
                         │                                            │
                         │ + startBatch()                              │
                         │ + uploadChunk()                             │
                         │ + completeBatch()                           │
                         │ + cancelBatch()                             │
                         │ + getLastPrice()                            │
                         └──────────────────────────────┬─────────────┘
                                                        │ extends
                                                        │
                         ┌────────────────────────────────────────────────────┐
                         │               ThreadBasedPriceService              │
                         ├────────────────────────────────────────────────────┤
                         │ - instance: static final                           │
                         │ + getInstance()                                     │
                         │ + waitForBatch()                                    │
                         │ + getActiveBatchIds()                               │
                         └────────────────────────────────────────────────────┘


                                      ▲
                                      │ holds batches
                                      │
                            ┌─────────────────────┐
                            │        Batch        │
                            ├─────────────────────┤
                            │ - state: BatchState │
                            │ - latestPrices: Map │
                            │                     │
                            │ + addOrUpdate()     │
                            │ + isActive()        │
                            │ + getStatus()       │
                            │ + setStatus()       │
                            └─────────────────────┘


                                      ▲
                                      │
                          ┌──────────────────────────┐
                          │       PriceRecord        │
                          ├──────────────────────────┤
                          │ - id: String              │
                          │ - asOf: Instant           │
                          │ - payload: PriceRecord?   │
                          │                          │
                          │ + getters                │
                          └──────────────────────────┘

                 ┌─────────────────────┐                     ┌─────────────────────┐
                 │      Producer       │                     │      Consumer       │
                 ├─────────────────────┤                     ├─────────────────────┤
                 │ - publisherService  │                     │ - consumerService   │
                 │ + publishSampleBatch│                     │ + printLatestPrice()│ (private)  
                 └─────────────────────┘                     └─────────────────────┘

Overview:-

The system provides an in-memory service that tracks the latest price for financial instruments.
It supports:

Producers that publish prices in batch runs

Consumers that query the latest available price

Batch lifecycle: START → UPLOAD CHUNKS → COMPLETE or CANCEL

All data is stored in-memory (no database) and exposed as a Java API, meaning consumers and producers operate in the same JVM.


Key Requirements:-

-> Producers publish price records containing:

id: instrument identifier

asOf: timestamp

payload: flexible price structure

-> Producers publish records using batch runs:

Start a batch

Upload chunks (≤1000 records)

Complete or cancel the batch


 ┌──────────────┐       ┌──────────────────────────┐
 │   Producer    │ ----> │   PriceRecordPublisher   │
 └──────────────┘       └──────────────────────────┘
           │                           │
           ▼                           ▼
     ┌────────────────────────────────────────┐
     │        PriceRecordServiceAPI           │
     │   (Main entry point for system)        │
     └────────────────────────────────────────┘
           ▲                           ▲
           │                           │
 ┌──────────────┐       ┌──────────────────────────┐
 │   Consumer    │ <---- │   PriceRecordConsumer    │
 └──────────────┘       └──────────────────────────┘



Component Responsibilities:-

-> PriceRecordService (Core Service) :-
Provided implementation for PriceRecordPublisher, PriceRecordConsumer, PriceRecordServiceAPI

-> Batch Class
BatchState (IN_PROGRESS, COMPLETED, CANCELLED)
Map of priceRecordId → latest PriceRecord
Ensures only records with the latest asOf per ID are stored

-> ThreadBasedPriceService
Utilities specifically designed for multithreading scenarios
Thread-safe singleton providing: all producer and consumer threads operate on the same in-memory state, which is essential for concurrency testing.
Here, waitForBatch Blocks the calling thread until the specified batch has either completed or been cancelled via consumer threads waiting for data published by producers.
and getActiveBatchIds return set of batch IDs that are currently IN_PROGRESS.Basically monitoring batch lifecycle, coordinating multiple producer threads and verifying correctness during concurrency tests

->Producer
façade for producers to interact with the service that support Starting batch, Uploading chunks, Completing batch

-> Consumer
façade for clients querying prices Record that fetching last committed price record 

Processing Flow:-

-> Batch LifeCycle:-

START_BATCH → UPLOAD_CHUNKS → COMPLETE_BATCH → VISIBLE TO CONSUMERS
                                 OR
                               CANCEL_BATCH → DISCARDED

  Flow Diagram - 

  Producer calls startBatch()
       ▼
Service creates Batch object
       ▼
Producer sends chunks of PriceRecord
       ▼
Batch stores latest per-ID (asOf comparison)
       ▼
Producer calls completeBatch()
       ▼
All accumulated prices moved to global storage atomically
       ▼
Consumers can see latest price

-> Concurrency & Synchronization Approach

* Each Batch object is synchronized

* Prevents race conditions during chunk uploads

* PriceRecordService holds batches in a ConcurrentHashMap

* Global committed prices stored in ConcurrentHashMap

* Producers and consumers run from separate threads safely

* Invalid operations (uploading to completed batch) are ignored or throw clear errors

-> Patterns Used

Monitor locking (synchronized)

Immutable UUID for batch IDs

ConcurrentHashMap

Volatile state flag

-> Error & Misuse Handling

Invalid operations do NOT crash the system:

Uploading to completed batch → ignored

Completing unknown batch → ignored

Cancelling unknown batch → ignored

Uploading after cancellation → rejected

Multi-threaded producers writing same ID → latest timestamp wins

Unit Tests

✔ Validate batch lifecycle
✔ Validate last-price-wins
✔ Validate cancellation behavior
✔ Validate incorrect order does not break service

Integration Tests

✔ Multi-threaded producers uploading concurrently
✔ Multi-threaded consumers reading concurrently
✔ Private printLatestPrice method accessed via reflection
✔ Validation of final committed state


