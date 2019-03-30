package com.treefuerza.pruebas.utils

import java.util.concurrent.*
import kotlin.coroutines.*
import java.util.concurrent.locks.*

fun <T> future(context: CoroutineContext = CommonPool, block: suspend () -> T): CompletableFuture<T> =
    CompletableFutureCoroutine<T>(context).also { block.startCoroutine(completion = it) }

class CompletableFutureCoroutine<T>(override val context: CoroutineContext) : CompletableFuture<T>(), Continuation<T> {
    override fun resumeWith(result: Result<T>) {
        result
            .onSuccess { complete(it) }
            .onFailure { completeExceptionally(it) }
    }
}

fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
    block.startCoroutine(Continuation(context) { result ->
        result.onFailure { exception ->
            val currentThread = Thread.currentThread()
            currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
        }
    })

fun <T> runBlocking(context: CoroutineContext, block: suspend () -> T): T =
    BlockingCoroutine<T>(context).also { block.startCoroutine(it) }.getValue()

private class BlockingCoroutine<T>(override val context: CoroutineContext) : Continuation<T> {
    private val lock = ReentrantLock()
    private val done = lock.newCondition()
    private var result: Result<T>? = null

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }

    private inline fun loop(block: () -> Unit): Nothing {
        while (true) {
            block()
        }
    }

    override fun resumeWith(result: Result<T>) = locked {
        this.result = result
        done.signal()
    }

    fun getValue(): T = locked<T> {
        loop {
            val result = this.result
            if (result == null) {
                done.awaitUninterruptibly()
            } else {
                return@locked result.getOrThrow()
            }
        }
    }
}

object CommonPool : Pool(ForkJoinPool.commonPool())

open class Pool(val pool: ForkJoinPool) : AbstractCoroutineContextElement(ContinuationInterceptor),
    ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        PoolContinuation(pool, continuation.context.fold(continuation) { cont, element ->
            if (element != this@Pool && element is ContinuationInterceptor)
                element.interceptContinuation(cont) else cont
        })

    // runs new coroutine in this pool in parallel (schedule to a different thread)
    fun runParallel(block: suspend () -> Unit) {
        pool.execute { launch(this, block) }
    }
}

private class PoolContinuation<T>(
    val pool: ForkJoinPool,
    val cont: Continuation<T>
) : Continuation<T> {
    override val context: CoroutineContext = cont.context

    override fun resumeWith(result: Result<T>) {
        pool.execute { cont.resumeWith(result) }
    }
}

suspend fun <T> CompletableFuture<T>.await(): T =
    suspendCoroutine<T> { cont: Continuation<T> ->
        whenComplete { result, exception ->
            if (exception == null) // the future has been completed normally
                cont.resume(result)
            else // the future has completed with an exception
                cont.resumeWithException(exception)
        }
    }