package soundcloud.api

import akka.actor.Actor
import akka.io.Tcp
import soundcloud.core.UserRepository
import soundcloud.core.entities.Event

/**
  * Each batch of messages undergoes basic parsing, and the resulting events are sorted and processed per-batch.
  * In an attempt to remain declarative, this handler simply acts as a controller and delegates the actual logic
  * to other entities.
  */
class EventHandler extends Actor {

	def receive = {
		case Tcp.Received(data) =>
			data.utf8String.split('\n').map(Event(_)).sorted.foreach { event =>
				event.messageType match {
					case 'F' =>
						val user = UserRepository.follow(event.fromUser.get, event.toUser.get)
						UserRepository.notify(user, event)
					case 'U' =>
						UserRepository.unfollow(event.toUser.get, event.fromUser.get)
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
				}
			}
		case Tcp.PeerClosed =>
			context.stop(self)
	}
}