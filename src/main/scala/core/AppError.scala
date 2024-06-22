package core

sealed trait AppError(message: String) extends Throwable

final case class NotFoundError(message: String) extends AppError(message: String)
