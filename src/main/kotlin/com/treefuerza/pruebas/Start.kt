package com.treefuerza.pruebas

import com.treefuerza.pruebas.endpoints.Products
import io.javalin.Javalin
import kotlinx.coroutines.runBlocking
import sun.plugin2.util.PojoUtil.toJson
import io.javalin.json.FromJsonMapper
import io.javalin.json.ToJsonMapper


suspend fun main(){


    val app = Javalin.create().start(8080)
    app.get("/") { ctx -> ctx.result("Hello World") }
    app.get("/products") {ctx ->
        runBlocking { Products.getAllWhisky(ctx) }
    }
}

