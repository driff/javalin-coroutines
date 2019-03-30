package com.treefuerza.pruebas.models

import java.util.concurrent.atomic.AtomicLong

data class Whisky(val id: Long = atomicLong.incrementAndGet(), val name: String, val origin: String){
    companion object {
        val atomicLong = AtomicLong()
    }

}