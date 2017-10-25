package com.scfollowermaze.api

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp
import akka.util.ByteString
import com.scfollowermaze.core.UserRepository

/**
  * Each client is assigned an instance of this handler, which registers a new user and stores the connection's
  * dispatcher on first encounter.
  * Any message not wrapped in a Tcp type is written to the instance's dispatcher, which relays it to the client.
  *
  * Dev-note: This actor has a state. While this is generally a very bad idea, the main concern is that the mutability
  * tightly-couples this actor to something else, preventing it from being garbage-collected -
  * but the coupling is to the dispatcher, to which this actor is already coupled regardless.
  */
class ClientHandler extends Actor {
	var dispatchHandler: ActorRef = _

	import com.scfollowermaze.GlobalApp._

	def receive = {
		case Tcp.Received(message) =>
			dispatchHandler = sender()
			val userId = message.utf8String.stripLineEnd.toInt
			UserRepository.add(userId, Option(self))
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			dispatchHandler ! Tcp.Write(ByteString(message))
		case otherType @ _ =>
			log.warning(s"Client sent an invalid message: ${otherType.toString}")
	}
}