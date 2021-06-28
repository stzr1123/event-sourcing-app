package services

/*
  This class references our consumer classes so that they will be
  instantiated by the dependency injection library.
 */
class ConsumerAggregator(tagEventConsumer: TagEventConsumer,
                         logRecordConsumer: LogRecordConsumer)
