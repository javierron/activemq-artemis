# ActiveMQ Artemis

This file describes some minimum 'stuff one needs to know' to get started coding in this project.

## Source

For details about the modifying the code, building the project, running tests, IDE integration, etc. see
our [Hacking Guide](./docs/hacking-guide/en/SUMMARY.md).

## Building the ASYNC IO library

ActiveMQ Artemis provides two journal persistence types, NIO (which uses the Java NIO libraries), and ASYNCIO which interacts with the linux kernel libaio library.   The ASYNCIO journal type should be used where possible as it is far superior in terms of performance.

ActiveMQ Artemis does not ship with the Artemis Native ASYNCIO library in the source distribution.  These need to be built prior to running "mvn install", to ensure that the ASYNCIO journal type is available in the resulting build.  Don't worry if you don't want to use ASYNCIO or your system does not support libaio, ActiveMQ Artemis will check at runtime to see if the required libraries and system dependencies are available, if not it will default to using NIO.

To build the ActiveMQ Artemis ASYNCIO native libraries, please follow the instructions in the artemis-native/README.

## Documentation

Our documentation is always in sync with our releases at the [Apache ActiveMQ Artemis](http://activemq.apache.org/artemis/docs.html) website.

Or you can also look at the current master version on [github](https://github.com/apache/activemq-artemis/blob/master/docs/user-manual/en/SUMMARY.md).

## Examples

To run an example firstly make sure you have run

    $ mvn -Prelease install

If the project version has already been released then this is unnecessary.

then you will need to set the following maven options, on Linux by

    $ export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"

and the finally run the examples by

    $ mvn verify

You can also run individual examples by running the same command from the directory of which ever example you want to run.
NB for this make sure you have installed examples/common.

### Recreating the examples

If you are trying to copy the examples somewhere else and modifying them. Consider asking Maven to explicitly list all the dependencies:

    # if trying to modify the 'topic' example:
    cd examples/jms/topic && mvn dependency:list

## Artemis on Apache Karaf

feature:repo-add mvn:org.apache.activemq/artemis-features/1.1.1-SNAPSHOT/xml
feature:install artemis-core artemis-hornetq artemis-stomp artemis-mqtt artemis-amqp
