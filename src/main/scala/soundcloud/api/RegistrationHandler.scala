package soundcloud.api

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}

/**
  * For each incoming connection, instantiates an appropriate handler and registers it to that connection's dispatcher.
  * Each connected client has his own handler, including the event-source, to enable fine-tuning of
  * each handler's performance.
  *
  * Dev-note: the separate client handlers allow tracking of each connection's dispatcher individually.
  * They could be replaced by a single actor with a synchronized map linking users to their dispatch-handlers,
  * but an actor is under a KB of memory and GC can collect them individually when the connection drops out.
  */

class RegistrationHandler extends Actor {

	import soundcloud.GlobalApp._

	private val hostname: String = config.getString("server.host")
	private val eventPort: Int = config.getInt("server.event.port")
	private val clientPort: Int = config.getInt("server.client.port")

	IO(Tcp) ! Tcp.Bind(
		handler = self,
		localAddress = new InetSocketAddress(hostname, eventPort),
		backlog = config.getInt("server.event.buffer")
	)
	IO(Tcp) ! Tcp.Bind(
		handler = self,
		localAddress = new InetSocketAddress(hostname, clientPort),
		backlog = config.getInt("server.client.buffer")
	)

	def receive: Receive = {
		case Tcp.CommandFailed(_: Tcp.Bind) =>
			context.stop(self)
		case binding@Tcp.Bound(localAddress) =>
			context.parent ! binding
			println(s"Server listening on $localAddress")
		case Tcp.Connected(remote, local) =>
			local.getPort match {
				case `eventPort` =>
					val actor = system.actorOf(Props[EventHandler].withDispatcher("akka.actor.eventDispatcher"), "eventHandler")
					sender() ! Tcp.Register(actor)
				case `clientPort` =>
					val actor = system.actorOf(Props(classOf[ClientHandler]), s"clientHandler:${remote.getPort.toString}")
					sender() ! Tcp.Register(actor)
			}
	}
}
