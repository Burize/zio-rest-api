package core

final case class NotFoundError(message: String) extends Exception

final case class AlreadyExist(message: String) extends Exception

final case class Unauthorized(message: String) extends Exception
