package api

import zio.*
import zio.http.Middleware
import auth.{ AuthRoutes, UserRoutes }
import product.ProductRoutes
import storage.StorageRoutes

val routes = (AuthRoutes() ++ UserRoutes() ++ StorageRoutes() ++ ProductRoutes()).handleError(RouteErrorHandler)

val middlewares = Middleware.requestLogging() ++ Middleware.debug

val apiRoutes = routes @@ middlewares
