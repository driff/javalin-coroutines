package com.treefuerza.pruebas.endpoints

import com.treefuerza.pruebas.models.Whisky
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

object Products{
    private fun createSomeData(): Map<Long, Whisky>{
        val products: MutableMap<Long, Whisky> = hashMapOf()
        val bowmore =
            Whisky(name = "Bowmore 15 Years Laimrig", origin = "Scotland, Islay")
        products.put(bowmore.id, bowmore)
        val talisker = Whisky(name = "Talisker 57° North", origin = "Scotland, Island")
        products.put(talisker.id, talisker)
        println(talisker)
        return products
    }

    suspend fun getAllWhisky(ctx: Context){
        val products = GlobalScope.async { createSomeData() }
        ctx.json(products.await())

    }

}