package soundcloud.api

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import soundcloud.core.entities.UserRepository

/**
  * Responsible for registering connected clients to their respective handlers
  */

class registrationServer extends Actor {

	import soundcloud.globalApp._

	private val tcpManager: ActorRef = IO(Tcp)

	tcpManager ! Tcp.Bind(
		handler = self,
		localAddress = new InetSocketAddress(
			config.getString("server.events.host"),
			config.getInt("server.events.port")
		),
		backlog = config.getInt("server.clients.buffer")
	)
	tcpManager ! Tcp.Bind(
		handler = self,
		localAddress = new InetSocketAddress(
			config.getString("server.clients.host"),
			config.getInt("server.clients.port")
		),
		backlog = config.getInt("server.clients.buffer")
	)

	var currentSequence = 1

	private val clientHandler = system.actorOf(Props[ClientHandler])

	def receive: Receive = {
		case binding@Tcp.Bound(localAddress) =>
			context.parent ! binding
			println(s"Server listening on $localAddress")
		case Tcp.CommandFailed(_: Tcp.Bind) =>
			context.stop(self)
		case Tcp.Connected(remote, local) =>
			local.getPort match {
				case 9090 =>
				//					originalSender ! Tcp.Register(clientHandler)
				case 9099 =>
					val actorName = remote.toString.substring(1)
					log.info(s"Registered a handler for $actorName")
					sender() ! Tcp.Register(context.actorOf(Props[ClientHandler], actorName))

			}
	}
}

class ClientHandler extends Actor {
	import soundcloud.globalApp._

	def receive = {
		case Tcp.Received(message) =>
			UserRepository.add(message.utf8String.stripLineEnd.toInt, self)
			log.debug(UserRepository.get(message.utf8String.stripLineEnd.toInt).toString)
		case Tcp.PeerClosed =>
			context.stop(self)
	}
}
