package com.sistema.distribuido.network

/**
 * Singleton holder para CommandBroker, inicializado por la app maestro (MainActivity)
 */
object GlobalCommandBroker {
    @Volatile
    private var broker: CommandBroker? = null

    fun init(b: CommandBroker) {
        broker = b
    }

    fun getInstance(): CommandBroker {
        return broker ?: throw IllegalStateException("CommandBroker no inicializado. Llama a GlobalCommandBroker.init() desde MainActivity")
    }

    fun getInstanceOrNull(): CommandBroker? = broker
}

