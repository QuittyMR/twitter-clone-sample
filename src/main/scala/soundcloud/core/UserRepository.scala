package soundcloud.core

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorRef
import soundcloud.core.entities.{Event, User}

import scala.collection.convert.decorateAsScala._
import scala.collection.mutable


/**
  * This singleton contains a thread-safe collection of registered users.
  *
  * Due to the fact that a registered user may follow an un-registered one and receive his status updates,
  * any attempt to follow an un-registered user adds a falsified, handler-less user to the collection.
  *
  * Dev-note: the mutable set used to store user followers is not thread-safe, but the serial nature of the
  * subscribe/unsubscribe process likely means synchronizing it isn't worth the performance impact.
  */
object UserRepository {

	import soundcloud.GlobalApp._

	private val users: mutable.Map[Int, User] = new ConcurrentHashMap[Int, User]().asScala

	def add(userId: Int, actor: Option[ActorRef]): User = {
		val user = User(userId, actor)
		users += (userId -> user)
		user
	}

	/**
	  * Returns a user to avoid retrieving the user twice when calling notify
	  */
	def follow(followerId: Int, userId: Int): User = {
		get(userId) match {
			case Some(user) =>
				user.followers += followerId
				user
			case _ =>
				log.debug(s"Falsifying record for $userId")
				add(userId, None)
				follow(followerId, userId)
		}
	}

	def unfollow(userId: Int, followerId: Int): Unit = {
		get(userId) match {
			case Some(user) =>
				user.followers -= followerId
			case _ =>
				None
		}
	}

	def get(userId: Int): Option[User] = users.get(userId)

	def getByHandler(actorRef: ActorRef): Option[User] = users.values.find(_.actor == actorRef)

	def getAll: Iterable[User] = users.values

	def notify(user: User, message: Event): Unit = {
		user.actor match {
			case Some(actor) =>
				actor ! (message.toString + "\n")
			case _ =>
				log.debug(s"User ${user.id} not registered")
		}
	}
}
