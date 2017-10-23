package soundcloud.api

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import soundcloud.core.Event
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

	def receive: Receive = {
		case Tcp.CommandFailed(_: Tcp.Bind) =>
			context.stop(self)
		case binding@Tcp.Bound(localAddress) =>
			val parent = context.parent
			parent ! binding
			println(s"Server listening on $localAddress")
		case Tcp.Connected(remote, local) =>
			local.getPort match {
				case 9090 =>
					val actor = system.actorOf(Props[EventHandler], "eventHandler")
					sender() ! Tcp.Register(actor)
				case 9099 =>
					val actor = system.actorOf(Props(classOf[ClientHandler]), s"client-${remote.getPort.toString}")
					sender() ! Tcp.Register(actor)
			}
	}
}

class ClientHandler extends Actor {
	private var tcpHandler: ActorRef = null
	import soundcloud.globalApp._

	def receive = {
		case Tcp.Received(message) =>
			tcpHandler = sender()
			val userId = message.utf8String.stripLineEnd.toInt
			UserRepository.add(userId, Option(self))
			log.debug(s"Registered user $userId to ${self.path.name}")
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			tcpHandler ! Tcp.Write(ByteString(message))
	}
}

class EventHandler extends Actor {
	import soundcloud.globalApp._
	def receive = {
		case Tcp.Received(data) =>
			data.utf8String.split('\n').map(message => Event(message)).sorted.foreach { event =>
				event.messageType match {
					case 'F' =>
						val user = UserRepository.follow(event.toUser.get, event.fromUser.get)
						UserRepository.notify(user, event)
					case 'U' =>
						UserRepository.unfollow(event.toUser.get, event.fromUser.get)
					case 'B' =>
						UserRepository.getAll.foreach(user => UserRepository.notify(user, event))
					case 'P' =>
						log.info(s"Need user ${event.toUser.get}")
						UserRepository.get(event.toUser.get).foreach(user =>
							UserRepository.notify(user, event)
						)
					case 'S' =>
						UserRepository.get(event.fromUser.get).foreach(user =>
							user.followers.flatMap(UserRepository.get).foreach(user =>
								UserRepository.notify(user, event)
							)
						)
				}
			}
		case Tcp.PeerClosed =>
			context.stop(self)
	}
}