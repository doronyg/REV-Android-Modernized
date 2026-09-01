package com.wowwee.revandroidsampleproject.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong

object UdpGameEngine {
    private const val DEFAULT_PORT = 8888
    private const val BROADCAST_HOST = "255.255.255.255"
    private const val MAX_UDP_PACKET_SIZE = 8192

    private val gson = Gson()
    private val packetCounter = AtomicLong(1L)

    @Volatile
    private var socket: DatagramSocket? = null

    @Volatile
    private var networkScope: CoroutineScope? = null

    @Volatile
    private var listenJob: Job? = null

    @Volatile
    private var myRevId: String? = null

    @Volatile
    private var port: Int = DEFAULT_PORT

    @Volatile
    private var allowSelfLoopback: Boolean = false

    private val packetIdBySender = linkedMapOf<String, Long>()
    private val lock = Any()

    @JvmStatic
    @JvmOverloads
    fun start(myRevId: String, port: Int = DEFAULT_PORT, allowSelfLoopback: Boolean = false) {
        stop()

        this.myRevId = myRevId.trim()
        this.port = port
        this.allowSelfLoopback = allowSelfLoopback

        try {
            val createdSocket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(port))
            }
            socket = createdSocket
        } catch (bindError: BindException) {
            notifyNetworkError("Unable to bind UDP socket on port $port", bindError)
            return
        } catch (t: Throwable) {
            notifyNetworkError("Unable to start UDP engine on port $port", t)
            return
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        networkScope = scope
        listenJob = scope.launch { listenLoop() }
    }

    @JvmStatic
    fun stop() {
        listenJob?.cancel()
        listenJob = null

        networkScope?.cancel()
        networkScope = null

        socket?.close()
        socket = null

        synchronized(lock) {
            packetIdBySender.clear()
        }
    }

    @JvmStatic
    fun nextPacketId(): Long = packetCounter.getAndIncrement()

    @JvmStatic
    fun sendPacket(packet: GameStatePacket) {
        val currentSocket = socket ?: return
        val scope = networkScope ?: return

        scope.launch {
            try {
                val data = gson.toJson(packet).toByteArray(StandardCharsets.UTF_8)
                val destination = InetAddress.getByName(BROADCAST_HOST)
                val datagram = DatagramPacket(data, data.size, destination, port)
                currentSocket.send(datagram)
            } catch (t: Throwable) {
                notifyNetworkError("UDP send failed", t)
            }
        }
    }

    private fun listenLoop() {
        val buffer = ByteArray(MAX_UDP_PACKET_SIZE)
        while (networkScope?.isActive == true) {
            val currentSocket = socket ?: break
            try {
                val datagram = DatagramPacket(buffer, buffer.size)
                currentSocket.receive(datagram)
                val json = String(datagram.data, 0, datagram.length, StandardCharsets.UTF_8)
                processIncomingPacket(json)
            } catch (_: java.net.SocketException) {
                break
            } catch (t: Throwable) {
                notifyNetworkError("UDP receive failed", t)
            }
        }
    }

    private fun processIncomingPacket(json: String) {
        val packet = try {
            gson.fromJson(json, GameStatePacket::class.java)
        } catch (_: JsonSyntaxException) {
            return
        } catch (_: Throwable) {
            return
        }

        val localId = myRevId ?: return
        if (!allowSelfLoopback && packet.senderId == localId) {
            return
        }
        if (isDuplicate(packet)) {
            return
        }

        NetworkEventBus.publishPacket(packet)
    }

    private fun isDuplicate(packet: GameStatePacket): Boolean {
        synchronized(lock) {
            val dedupeKey = buildDedupeKey(packet)
            val previous = packetIdBySender[dedupeKey]
            if (previous != null && packet.packetId <= previous) {
                return true
            }
            packetIdBySender[dedupeKey] = packet.packetId
            if (packetIdBySender.size > 256) {
                val eldestKey = packetIdBySender.entries.firstOrNull()?.key
                if (eldestKey != null) {
                    packetIdBySender.remove(eldestKey)
                }
            }
            return false
        }
    }

    private fun buildDedupeKey(packet: GameStatePacket): String {
        val instanceId = packet.senderInstanceId?.trim().orEmpty()
        return if (instanceId.isEmpty()) {
            packet.senderId
        } else {
            "${packet.senderId}#$instanceId"
        }
    }

    private fun notifyNetworkError(message: String, throwable: Throwable?) {
        NetworkEventBus.publishError(message, throwable)
    }
}

