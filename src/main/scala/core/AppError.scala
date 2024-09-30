package core

final case class NotFound(message: String) extends Exception

final case class AlreadyExist(message: String) extends Exception

final case class Unauthorized(message: String) extends Exception
