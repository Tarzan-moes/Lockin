package com.lockin.app.watch.connection

/**
 * WatchConnector – Manages the Bluetooth/Wi-Fi connection to a paired smartwatch.
 *
 * Supports Galaxy Watch (Samsung Wearable SDK) and generic Wear OS devices.
 * For Part 1 this is a stub; real implementation uses MessageClient / DataClient.
 */
interface WatchConnector {
    suspend fun isWatchConnected(): Boolean
    suspend fun connectToWatch(): Boolean
    suspend fun disconnectWatch()
    suspend fun getWatchBatteryLevel(): Int?
}

