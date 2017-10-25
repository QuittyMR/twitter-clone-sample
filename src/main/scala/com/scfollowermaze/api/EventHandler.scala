package com.scfollowermaze.api

import akka.actor.Actor
import akka.io.Tcp
import com.scfollowermaze.core.entities.Event
import com.scfollowermaze.core.UserRepository

/**
  * Each batch of messages undergoes basic parsing, and the resulting events are sorted and processed per-batch.
  * In an attempt to remain declarative, this handler simply acts as a controller and delegates the actual logic
  * to other entities.
  *
  * Dev-note: creating a new notificationHandler actor could bring a performance increase for larger client-sets.
  */
class EventHandler extends Actor {

	import com.scfollowermaze.GlobalApp._

	def receive = {
		case Tcp.Received(data) =>
			data.utf8String.split('\n').map(Event(_)).sorted.foreach { event =>
				event.messageType match {
					case 'F' =>
						val user = UserRepository.follow(event.fromUser.get, event.toUser.get)
						UserRepository.notify(user, event)
					case 'U' =>
						UserRepository.unfollow(event.fromUser.get, event.toUser.get)
					case 'B' =>
						UserRepository.getAll.foreach(user => UserRepository.notify(user, event))
					case 'P' =>
						UserRepository.get(event.toUser.get).foreach(user =>
							UserRepository.notify(user, event)
						)
					case 'S' =>
						UserRepository.get(event.fromUser.get).foreach(user =>
							user.followers.flatMap(UserRepository.get).foreach(user =>
								UserRepository.notify(user, event)
							)
						)
					case _ =>
						log.warning(s"Invalid event $event received")
				}
			}
		case Tcp.PeerClosed =>
			context.stop(self)
	}
}