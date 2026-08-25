package com.wowwee.revandroidsampleproject.network

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Dedicated event source for UDP/network events.
 */
object NetworkEventBus {
    private val subject = PublishSubject.create<NetworkEvent>().toSerialized()

    @JvmStatic
    val events: Observable<NetworkEvent> = subject.hide()

    @JvmStatic
    fun publishPacket(packet: GameStatePacket) {
        subject.onNext(NetworkEvent.PacketReceived(packet))
    }

    @JvmStatic
    fun publishError(message: String, throwable: Throwable? = null) {
        subject.onNext(NetworkEvent.TransportError(message, throwable))
    }
}

