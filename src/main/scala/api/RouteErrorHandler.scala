package api

import core.{AlreadyExist, Unauthorized, NotFound}
import zio.http.{Body, Response, Status}


def RouteErrorHandler(error: Throwable | Response) =
  error match
    case AlreadyExist(error) => Response(status = Status.Conflict, body = Body.fromString(error))
    case Unauthorized(error) => Response.unauthorized(error)
    case NotFound(error) => Response.notFound(error)
    case e: Throwable => Response.internalServerError(s"Internal server error: ${e.getMessage}")
    case response: Response => response
