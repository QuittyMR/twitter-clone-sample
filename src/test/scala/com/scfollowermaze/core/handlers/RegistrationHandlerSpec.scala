package com.scfollowermaze.core.handlers

import akka.actor.{ActorRef, Props}
import akka.io.Tcp
import com.scfollowermaze.api.RegistrationHandler


class RegistrationHandlerSpec extends BaseHandlerSpec {

	private val handler: ActorRef = system.actorOf(Props(new RegistrationHandler()))

	"The registration-handler" should "bind to tcp ports on request" in {
		val binding: Tcp.Bound = Tcp.Bound(socketAddress("local"))
		handler ! binding
		expectMsg(binding)
	}

	it should "Register a handler on the client and event ports" in {
		List(
			socketAddress("client"),
			socketAddress("event")
		).foreach { socket =>
			handler ! Tcp.Connected(socketAddress("remote"), socket)
			expectMsgType[Tcp.Register]
		}
	}
}