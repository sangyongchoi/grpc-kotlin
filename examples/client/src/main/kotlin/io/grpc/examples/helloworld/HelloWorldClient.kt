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

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import io.grpc.examples.helloworld.GreeterGrpcKt.GreeterCoroutineStub
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class HelloWorldClient(private val channel: ManagedChannel) : Closeable {
    private val stub: GreeterCoroutineStub = GreeterCoroutineStub(channel)

    suspend fun greet(name: String) {
        try {
            val request = helloRequest { this.name = name }
            val response = stub.sayHello(request)
            println("Received: ${response.message}")
        } catch (e: StatusRuntimeException) {
            println("error!!")
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

/**
 * Greeter, uses first argument as name to greet if present;
 * greets "world" otherwise.
 */
suspend fun main(args: Array<String>) {
    val port = 50051
    val config = getRetryingServiceConfig()
    println(config)
    val channel = ManagedChannelBuilder
        .forAddress("localhost", port)
        .usePlaintext()
        .defaultServiceConfig(config)
        .enableRetry()
        .build()

    val client = HelloWorldClient(channel)

    val user = args.singleOrNull() ?: "world"
    client.greet(user)
}

fun getRetryingServiceConfig(): Map<String?, *>? {
    val prefix = "/Users/sy.choi/Desktop/sy.choi/open-source/grpc-kotlin/examples/client/src/main/resources/helloworld/"
    return Gson()
        .fromJson(
            JsonReader(
                InputStreamReader(
                    File(prefix + "retrying_service_config.json").inputStream(),
                    StandardCharsets.UTF_8)),
            MutableMap::class.java)
}
