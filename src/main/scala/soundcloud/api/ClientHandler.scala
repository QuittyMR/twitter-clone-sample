package soundcloud.api

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp
import akka.util.ByteString
import soundcloud.core.UserRepository

/**
  * Each client is assigned an instance of this handler, which registers a new user and stores the connection's
  * dispatcher on first encounter.
  *
  * Any message not wrapped in a Tcp type is written to the instance's dispatcher, which relays it to the client.
  */
class ClientHandler extends Actor {
	private var dispatchHandler: ActorRef = _

	import soundcloud.GlobalApp._

	def receive = {
		case Tcp.Received(message) =>
			dispatchHandler = sender()
			val userId = message.utf8String.stripLineEnd.toInt
			UserRepository.add(userId, Option(self))
			log.debug(s"Registered user $userId to ${self.path.name}")
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			dispatchHandler ! Tcp.Write(ByteString(message))
	}
}