package api

import zio.*
import zio.http.Middleware

import auth.api.{AuthRoutes, UserRoutes}
import product.api.ProductRoutes
import storage.api.StorageRoutes

val routes = (AuthRoutes() ++ UserRoutes() ++ StorageRoutes() ++ ProductRoutes()).handleError(RouteErrorHandler)
val middlewares = Middleware.requestLogging() ++ Middleware.debug

val apiRoutes = routes @@ middlewares
