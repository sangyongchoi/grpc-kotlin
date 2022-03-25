/*
 * Copyright 2020 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

class HelloWorldServer(private val port: Int) {
    val server: Server = ServerBuilder
        .forPort(port)
        .addService(HelloWorldService())
//        .addService(HellWorldRetryService())
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@HelloWorldServer.stop()
                println("*** server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    internal class HellWorldRetryService : GreeterGrpc.GreeterImplBase() {

        var count = 0

        override fun sayHello(request: HelloRequest?, responseObserver: StreamObserver<HelloReply>?) {
            count++
            println("come in $count")
            responseObserver!!.onError(io.grpc.Status.UNAVAILABLE
                .withDescription("Greeter temporarily unavailable...").asRuntimeException())
        }
    }

    internal class HelloWorldService : GreeterGrpcKt.GreeterCoroutineImplBase() {

        private var count = 0

        override suspend fun sayHello(request: HelloRequest) = helloReply {
            println(count++)
            throw StatusRuntimeException(io.grpc.Status.UNAVAILABLE)
            message = "Hello ${request.name}"
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = HelloWorldServer(port)
    server.start()
    server.blockUntilShutdown()
}
