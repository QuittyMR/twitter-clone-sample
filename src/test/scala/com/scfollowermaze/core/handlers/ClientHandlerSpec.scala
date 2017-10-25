package com.scfollowermaze.core.handlers

import akka.actor.{ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import com.scfollowermaze.api.ClientHandler
import com.scfollowermaze.core.UserRepository


class ClientHandlerSpec extends BaseHandlerSpec {

	private val handler: ActorRef = system.actorOf(Props(new ClientHandler()))

	"The client-handler" should "register a new client on first encounter" in {
			handler ! Tcp.Received(ByteString("1\n"))
			Thread.sleep(200)
			assert(UserRepository.get(1).isDefined)
	}

	it should "write any received string back to the registered user's dispatcher" in {
			val message: String = "I am your dispatcher!"
			handler ! Tcp.Received(ByteString("1\n"))
			Thread.sleep(200)

			handler ! message
			expectMsg(Tcp.Write(ByteString(message)))
		}
}