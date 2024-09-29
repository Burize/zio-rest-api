package api

import core.{AlreadyExist, Unauthorized}
import zio.http.{Body, Response, Status}


def RouteErrorHandler(error: Throwable | Response) =
  error match
    case AlreadyExist(error) => Response(status = Status.Conflict, body = Body.fromString(error))
    case Unauthorized(error) => Response.unauthorized(error)
    case e: Throwable => Response.internalServerError(s"Internal server error: $e")
    case response: Response => response
