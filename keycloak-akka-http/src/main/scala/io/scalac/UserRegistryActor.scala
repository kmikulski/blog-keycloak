package io.scalac

import akka.actor.{ Actor, ActorLogging, Props }

final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: Seq[User])

object UserRegistryActor {
  final case class ActionPerformed(description: String)
  final case object GetUsers
  final case class CreateUser(user: User)
  final case class GetUser(name: String)
  final case class DeleteUser(name: String)

  def props: Props = Props[UserRegistryActor]
}

class UserRegistryActor extends Actor with ActorLogging {
  import UserRegistryActor._

  var users = Set(
    User("Grzegorz", 31, "Poland"),
    User("Thomas", 28, "United Kingdom"))

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(users.toSeq)
  }
}