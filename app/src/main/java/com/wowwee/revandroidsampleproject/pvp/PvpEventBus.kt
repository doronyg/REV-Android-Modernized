package com.wowwee.revandroidsampleproject.pvp

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Dedicated event source for PVP/session domain events.
 */
object PvpEventBus {
    private val subject = PublishSubject.create<PvpEvent>().toSerialized()

    @JvmStatic
    val events: Observable<PvpEvent> = subject.hide()

    @JvmStatic
    fun publish(event: PvpEvent) {
        subject.onNext(event)
    }
}

