package soundcloud.api

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp.{CommandFailed, Connect}
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
//					log.debug(s"Registered a handler for $actorName")
//					println(s"Registering ${sender()}")
					val actor = system.actorOf(Props(classOf[ClientHandler], remote), actorName)
					val register = Tcp.Register(actor, useResumeWriting = false)
					import akka.pattern.ask
					(tcpManager ? Connect(remote)).onSuccess {
						case something =>
							println(something)
					}
					sender() ! register

			}

		case Tcp.Received(message) =>
			println("Reached here")
	}
}

class ClientHandler(remote: InetSocketAddress) extends Actor {
	import soundcloud.globalApp._
	import context.system

	private var tcpClient:ActorRef = null

	def receive = {
		case Tcp.Received(message) =>
			tcpClient = sender()
			if (message.utf8String.stripLineEnd == "283") {
				println(s"Actor is ${tcpClient.path.name}")
			}
			UserRepository.add(message.utf8String.stripLineEnd.toInt, self)
			log.debug(UserRepository.get(message.utf8String.stripLineEnd.toInt).toString)
		case Tcp.PeerClosed =>
			context.stop(self)
		case message: String =>
			println(s"Messaging $message to $tcpClient")
			tcpClient ! Tcp.Write(ByteString(message))
	}
}

class EventHandler extends Actor {

	import soundcloud.globalApp._

	def receive = {
		case Tcp.Received(data) =>
			data.utf8String.split('\n').map(message => Event(message)).sorted.foreach { event =>
				var logMessage = ""
				event.messageType match {
					case 'F' =>
						UserRepository.follow(event.toUser.get, event.fromUser.get) match {
							case Some(user) =>
//								UserRepository.notify(user, event.toString)
								logMessage = s"User ${event.fromUser.get} follows ${event.toUser.get}"
							case _ =>
								logMessage = "User does not exist"
						}
					case 'U' =>
						UserRepository.unfollow(event.toUser.get, event.fromUser.get)
						logMessage = s"User ${event.fromUser.get} un-follows ${event.toUser.get}"
					case 'B' =>
						logMessage = s"This is a public service announcement!"
					case 'P' =>
						logMessage = s"User ${event.fromUser.get} sends his regards to ${event.toUser.get}"
						UserRepository.get(event.toUser.get) match {
							case Some(user) =>
//								UserRepository.notify(user, event.toString)
								println(s"User actor is ${user.actor}")
								user.actor ! event.toString
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