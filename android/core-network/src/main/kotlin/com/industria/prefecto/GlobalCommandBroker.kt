package com.industria.prefecto

/**
 * Singleton holder for CommandBroker, initialized by the master app (MainActivity)
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

