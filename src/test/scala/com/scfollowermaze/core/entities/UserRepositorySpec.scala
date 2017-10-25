package com.scfollowermaze.core.entities

import com.scfollowermaze.core.UserRepository
import com.scfollowermaze.core.handlers.BaseHandlerSpec
import org.scalatest.FlatSpecLike

class UserRepositorySpec extends BaseHandlerSpec with FlatSpecLike {


	"The user repository" should "be empty on initialization" in {
		assert(UserRepository.getAll.isEmpty)
	}

	it should "successfully register a user" in {
		import akka.actor.ActorRef
		UserRepository.add(1, Option(testActor))

		assert(UserRepository.get(1).get.isInstanceOf[User])
		assert(UserRepository.get(1).get.actor.get.isInstanceOf[ActorRef])
		assert(UserRepository.get(1).get.followers.isEmpty)
	}


	it should "successfully fabricate a user" in {
		UserRepository.add(2)
		assert(UserRepository.get(2).get.isInstanceOf[User])
		assert(UserRepository.get(2).get.actor.isEmpty)
		assert(UserRepository.get(2).get.followers.isEmpty)
	}


	it should "successfully add followers to an existing user" in {
		UserRepository.add(3)
		UserRepository.follow(4, 3)
		assert(UserRepository.get(3).get.followers.contains(4))
	}


	it should "successfully remove followers from an existing user" in {
		UserRepository.add(4)
		UserRepository.follow(5, 4)
		UserRepository.unfollow(5, 4)
		assert(UserRepository.get(4).get.followers.isEmpty)
	}

	it should "notify users" in {
		UserRepository.add(5, Option(testActor))
		UserRepository.notify(UserRepository.get(5).get, Event(1, 'F'))
		expectMsg("1|F\n")
	}
}