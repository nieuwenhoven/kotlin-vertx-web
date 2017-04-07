package {{ cookiecutter.package_name }}

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import java.time.LocalTime

val indexHtml = """<html>
    <head>
        <title>Vertx Web Demo</title>
    </head>
    <body>
        <a href="/static/servertime.html">View the server time</a>
    </body>
</html>
"""

fun main(args: Array<String>) {

    val svr = Server(8080)
    svr.start()
}


class Server(val port: Int) : AbstractVerticle() {

    init {
        vertx = Vertx.vertx()
    }

    @Throws(Exception::class)
    override fun start() {

        println("Starting server on port: $port ")

        val server = vertx.createHttpServer()

        val router = Router.router(vertx)

        //------------
        // Allow outbound traffic to the time-feed addres
        val options = BridgeOptions().addOutboundPermitted(PermittedOptions().setAddress("time-feed"))
        router.route("/eventbus/*").handler(
                // You can also optionally provide a handler like this which will be passed any events that occur on the bridge
                // You can use this for monitoring or logging, or to change the raw messages in-flight.
                // It can also be used for fine grained access control.
                SockJSHandler.create(vertx).bridge(options, { event ->
                    if (event.type() == BridgeEventType.SOCKET_CREATED) {
                        System.out.println("A socket was created")
                    }
                    event.complete(true)
                })
        )

        // Serve the static resources
        router.route("/static/*").handler(StaticHandler.create().setWebRoot("webroot/static"))

        router.route("/").handler({ routingContext ->
            routingContext.response()
                    .putHeader("content-type", "text/html")
                    .end(indexHtml)
        })

        // Publish a message to the address "time-feed" every second
        vertx.setPeriodic(500, { _ -> vertx.eventBus().publish("time-feed", LocalTime.now().toString()) })

        server.requestHandler({ router.accept(it) }).listen(port)
    }
}
