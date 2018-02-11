# Twitter clone (massively concurrent pub/sub)

## Overview

This app uses Akka actors to provide a massively concurrent socket server.
It was done in response to a technical challenge issued by Soundcloud, 
who also graciously supplied the virtual clients.
Using actors as small, decoupled work-units allows this server to maintain
a constant memory signature and pretty decent performance, while being
easily expandable and hopefully maintainable as well.
As an added bonus, it's very fault tolerant. Try crashing it, i dare you.
Sort of.

## Deployment

* ```$ ./run.sh``` to copmile, run all tests and start the server -
 then launch your follower-maze jar.
* ```$ sbt assembly``` to run all tests and generate a fat jar (8.8MB total) in target/scala-2.11
* Tested on Ubuntu 14.04 with Oracle JDK7
## Design

The GlobalApp class is both an entry point and a place to store
all the implicit "magic" i use globally.
The app follows an MVC pattern in broad strokes; handlers
are controllers, entities store modeled data and the UserRepository does
most of the business logic.

Beyond that, the app uses 4 primary actor types;

* TCP handlers are instantiated implicitly and serve as message
dispatchers for connected clients
* The RegistrationHandler instantiates the appropriate handler for each
new connection and binds connected clients to their dispatchers (TCP handlers)
* The EventHandler accepts events and serves as a controller for the
business logic, routing them to where they need to go
* A ClientHandler is assigned for each connection and notifies the
clients when needed.

## Dev notes

* There's documentation for each class or object in the code - hopefully
these will provide necessary context and explain what the h*** was i thinking.
* Some... "brave" decisions have been made with the ClientHandler.
See the doc-tags in the class.
* I like declarative code as much as the next guy, but it's easy to
overdo in Scala. I'd rather write more rows with less implicits
than spend hours looking for a magical conversion method.
The more Scala i write, the stronger i feel about this.
* The code could be more idiomatic in places, and i'm sure some more
 thought can be given to my choice of collections -
 or maybe i just love micro-optimizing stuff.
* On my old 4-core i7 laptop, this code uses 600MB of RAM and 8.5% of
the CPU, processing all events in 258 seconds. That's ~39K events/sec,
and with that resource usage, AWS will let this run on free-tier. Akka actors are awesome.

## Tests
The best integration or stress test i could possibly think
up for this application is what was provided by you as a test case in the
follower-maze jar.

What's left for me was simple integration/unit tests, more to provide
usage examples than to actually test anything.

However, the testing of actors, along with the fact that one of my actors is not
 stateless, did provide some nice puzzles - you may have noted the
 old-school thread sleep i had to employ because the tests run on a
 separate actor-system, causing race-conditions.

The EventHandler is not tested because it does nothing that isn't
covered by either the event entity tests or the user repository ones.
