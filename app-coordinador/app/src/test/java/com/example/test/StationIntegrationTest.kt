package com.example.test

import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import org.junit.Test
import org.junit.Assert.*

/**
 * STATION TESTS: Manufactura (Robot + Laser)
 */
class ManufacturaStationTest {

    @Test
    fun testRobotHome() {
        val broker = CommandBroker(allowOfflineSend = true)

        val homeCmd = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "ROBOT:HOME"
        )

        broker.send(homeCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testRobotPosition() {
        val broker = CommandBroker(allowOfflineSend = true)

        val posCmd = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "ROBOT:POSITION|100|200|50"
        )

        broker.send(posCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testRobotSequences() {
        val broker = CommandBroker(allowOfflineSend = true)

        val sequences = listOf(
            "ROBOT:SEQ_1",
            "ROBOT:SEQ_2",
            "ROBOT:SEQ_3"
        )

        sequences.forEach { seq ->
            val cmd = CimMessage(
                sourceApp = AppType.MANUFACTURA,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.START_SEQUENCE,
                payload = seq
            )
            broker.send(cmd)
        }

        val stats = broker.getStats()
        assertEquals(3, stats.logSize)
    }

    @Test
    fun testLaserStart() {
        val broker = CommandBroker(allowOfflineSend = true)

        val laserCmd = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "LASER:START|250W"
        )

        broker.send(laserCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testLaserOffset() {
        val broker = CommandBroker(allowOfflineSend = true)

        val offsetCmd = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "LASER:OFFSET|+10|+5"
        )

        broker.send(offsetCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testLaserStop() {
        val broker = CommandBroker(allowOfflineSend = true)

        val stopCmd = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "LASER:STOP"
        )

        broker.send(stopCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testManufacturaIdentification() {
        val broker = CommandBroker(allowOfflineSend = true)

        val identify = CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "MANUFACTURA_STATION_01"
        )

        broker.send(identify)

        val identified = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.MANUFACTURA,
            commandType = CommandType.IDENTIFIED,
            payload = "OK"
        )

        broker.send(identified)

        val stats = broker.getStats()
        assertEquals(2, stats.logSize)
    }

    @Test
    fun testManufacturaCompleteFlow() {
        val broker = CommandBroker(allowOfflineSend = true)

        // 1. Identify
        broker.send(CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "MANUFACTURA"
        ))

        // 2. Robot commands
        broker.send(CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "ROBOT:HOME"
        ))

        broker.send(CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "ROBOT:POSITION|100|200|50"
        ))

        // 3. Laser commands
        broker.send(CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "LASER:START|250W"
        ))

        broker.send(CimMessage(
            sourceApp = AppType.MANUFACTURA,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "LASER:STOP"
        ))

        val stats = broker.getStats()
        assertEquals(5, stats.logSize)
    }
}

/**
 * STATION TESTS: Calidad (ArUco + Tracking)
 */
class CalidadStationTest {

    @Test
    fun testGenerateAruco() {
        val broker = CommandBroker(allowOfflineSend = true)

        val generateCmd = CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "GENERATE_ARUCO|5|50mm"
        )

        broker.send(generateCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testStartTracking() {
        val broker = CommandBroker(allowOfflineSend = true)

        val startCmd = CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.START_SEQUENCE,
            payload = "TRACKING_START|PALLET_01"
        )

        broker.send(startCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testTrackingStatusUpdates() {
        val broker = CommandBroker(allowOfflineSend = true)

        val startCmd = CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.START_SEQUENCE,
            payload = "TRACKING_START"
        )
        broker.send(startCmd)

        repeat(5) { i ->
            val statusCmd = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.CALIDAD,
                commandType = CommandType.STATUS_RESPONSE,
                payload = "TRACKING_UPDATE|P_$i|X:${i*100}|Y:${i*150}"
            )
            broker.send(statusCmd)
        }

        val stopCmd = CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.STOP_SEQUENCE,
            payload = "TRACKING_STOP"
        )
        broker.send(stopCmd)

        val stats = broker.getStats()
        assertEquals(7, stats.logSize) // 1 start + 5 updates + 1 stop
    }

    @Test
    fun testCalidadIdentification() {
        val broker = CommandBroker(allowOfflineSend = true)

        val identify = CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "CALIDAD_STATION_01"
        )

        broker.send(identify)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testCalidadCompleteFlow() {
        val broker = CommandBroker(allowOfflineSend = true)

        // 1. Identify
        broker.send(CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "CALIDAD"
        ))

        // 2. Generate ArUco
        broker.send(CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "GENERATE_ARUCO|4|100mm"
        ))

        // 3. Start tracking
        broker.send(CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.START_SEQUENCE,
            payload = "TRACKING_START"
        ))

        // 4. Stop tracking
        broker.send(CimMessage(
            sourceApp = AppType.CALIDAD,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.STOP_SEQUENCE,
            payload = "TRACKING_STOP"
        ))

        val stats = broker.getStats()
        assertEquals(4, stats.logSize)
    }
}

/**
 * STATION TESTS: Almacén (Storage Grid)
 */
class AlmacenStationTest {

    @Test
    fun testStoreCommand() {
        val broker = CommandBroker(allowOfflineSend = true)

        val storeCmd = CimMessage(
            sourceApp = AppType.ALMACEN,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "STORE|ROW:2|COL:3|PRODUCT_ID:P001"
        )

        broker.send(storeCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testRetrieveCommand() {
        val broker = CommandBroker(allowOfflineSend = true)

        val retrieveCmd = CimMessage(
            sourceApp = AppType.ALMACEN,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "RETRIEVE|ROW:2|COL:3"
        )

        broker.send(retrieveCmd)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testStorageGridOperations() {
        val broker = CommandBroker(allowOfflineSend = true)

        // 6x3 storage grid = 18 posiciones
        repeat(6) { row ->
            repeat(3) { col ->
                val storeCmd = CimMessage(
                    sourceApp = AppType.ALMACEN,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "STORE|ROW:$row|COL:$col|PRODUCT:P${row*3+col}"
                )
                broker.send(storeCmd)
            }
        }

        val stats = broker.getStats()
        assertEquals(18, stats.logSize)
    }

    @Test
    fun testAlmacenIdentification() {
        val broker = CommandBroker(allowOfflineSend = true)

        val identify = CimMessage(
            sourceApp = AppType.ALMACEN,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "ALMACEN_STATION_01"
        )

        broker.send(identify)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testAlmacenCompleteFlow() {
        val broker = CommandBroker(allowOfflineSend = true)

        // 1. Identify
        broker.send(CimMessage(
            sourceApp = AppType.ALMACEN,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "ALMACEN"
        ))

        // 2. Store operations (3 items)
        repeat(3) { i ->
            broker.send(CimMessage(
                sourceApp = AppType.ALMACEN,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = "STORE|ROW:$i|COL:0|PRODUCT:P$i"
            ))
        }

        // 3. Retrieve operations (2 items)
        repeat(2) { i ->
            broker.send(CimMessage(
                sourceApp = AppType.ALMACEN,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = "RETRIEVE|ROW:$i|COL:0"
            ))
        }

        val stats = broker.getStats()
        assertEquals(6, stats.logSize) // 1 identify + 3 store + 2 retrieve
    }

    @Test
    fun testStorageGridFull() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Llenar todo el grid 6x3
        var commandCount = 0
        repeat(6) { row ->
            repeat(3) { col ->
                val cmd = CimMessage(
                    sourceApp = AppType.ALMACEN,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "STORE|ROW:$row|COL:$col|PRODUCT:P${row*3+col}"
                )
                broker.send(cmd)
                commandCount++
            }
        }

        assertEquals(18, commandCount)

        val stats = broker.getStats()
        assertEquals(18, stats.logSize)
    }

    @Test
    fun testStorageRetrievalPattern() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Store en patrón FIFO
        repeat(5) { i ->
            broker.send(CimMessage(
                sourceApp = AppType.ALMACEN,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = "STORE|ROW:0|COL:$i|PRODUCT:P$i"
            ))
        }

        // Retrieve en orden FIFO (primero en, primero fuera)
        repeat(5) { i ->
            broker.send(CimMessage(
                sourceApp = AppType.ALMACEN,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = "RETRIEVE|ROW:0|COL:$i"
            ))
        }

        val stats = broker.getStats()
        assertEquals(10, stats.logSize) // 5 store + 5 retrieve
    }
}

