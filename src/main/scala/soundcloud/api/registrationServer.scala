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
	import context.system

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
					sender() ! Tcp.Register(system.actorOf(Props[EventHandler], "eventHandler"))
				case 9099 =>
					val actorName = remote.toString.substring(1)
					val actor = system.actorOf(Props(classOf[ClientHandler], remote), actorName)
					val register = Tcp.Register(actor, useResumeWriting = false)
					sender() ! register
			}

		case Tcp.Received(message) =>
			println("Reached here")
	}
}

class ClientHandler(remote: InetSocketAddress) extends Actor {
	private var tcpHandler:ActorRef = null

	def receive = {
		case Tcp.Received(message) =>
			tcpHandler = sender()
			UserRepository.add(message.utf8String.stripLineEnd.toInt, self)
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			tcpHandler ! Tcp.Write(ByteString(message))
	}
}

class EventHandler extends Actor {
	def receive = {
		case Tcp.Received(data) =>
			data.utf8String.split('\n').map(message => Event(message)).sorted.foreach { event =>
				var logMessage = ""
				event.messageType match {
					case 'F' =>
						UserRepository.follow(event.toUser.get, event.fromUser.get) match {
							case Some(user) =>
								UserRepository.notify(user, event.toString)
							case _ =>
								logMessage = "User does not exist"
						}
					case 'U' =>
						UserRepository.unfollow(event.toUser.get, event.fromUser.get)
					case 'B' =>
						logMessage = s"This is a public service announcement!"
					case 'P' =>
						logMessage = s"User ${event.fromUser.get} sends his regards to ${event.toUser.get}"
						UserRepository.get(event.toUser.get) match {
							case Some(user) =>
								UserRepository.notify(user, event.toString)
							case _ =>
						}

					case 'S' =>
						logMessage = s"User ${event.fromUser.get} sends a status update"
				}
//				log.info(s"${event.sequenceId} $logMessage")
			}
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			println("Fuck me")
	}
}