package com.wowwee.revandroidsampleproject.network

sealed class NetworkEvent {
    data class PacketReceived(val packet: GameStatePacket) : NetworkEvent()
    data class TransportError(val message: String, val throwable: Throwable? = null) : NetworkEvent()
}

