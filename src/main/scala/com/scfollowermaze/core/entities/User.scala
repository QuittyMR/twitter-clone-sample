package com.scfollowermaze.core.entities

import akka.actor.ActorRef

import scala.collection.mutable

case class User(id: Int, actor: Option[ActorRef], followers: mutable.Set[Int] = mutable.Set())