package com.wowwee.revandroidsampleproject.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.BleDiscoveryScanner
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper
import java.util.Locale
import java.util.concurrent.LinkedBlockingDeque

class DiscoveryFragment : Fragment() {

    companion object {
        private const val TAG = "REV-DiscoveryFragment"
        private const val MAX_ROWS = 25
    }

    private val scanner = BleDiscoveryScanner.getInstance()
    private val rows = LinkedBlockingDeque<String>()

    private var tvDiscoveryStatus: TextView? = null
    private var tvDevices: TextView? = null

    private val scanListener = object : BleDiscoveryScanner.Listener {
        override fun onScanStateChanged(scanning: Boolean, message: String) {
            tvDiscoveryStatus?.text = message
        }

        override fun onDeviceDetected(device: BluetoothDevice, rssi: Int, connectable: Boolean, source: String) {
            val address = try {
                device.address
            } catch (_: SecurityException) {
                "<permission-denied-address>"
            }
            val name = try {
                device.name
            } catch (_: SecurityException) {
                "<permission-denied-name>"
            }?.takeIf { it.isNotBlank() } ?: "Unknown device"

            val row = String.format(Locale.US, "%s (%s)  RSSI %d", name, address, rssi)
            Log.d(TAG, "Detected device: $row")

            synchronized(rows) {
                rows.remove(row)
                rows.addFirst(row)
                while (rows.size > MAX_ROWS) {
                    rows.removeLast()
                }
            }
            renderDeviceList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (container == null) return null

        val view = inflater.inflate(R.layout.fragment_discovery, container, false)
        tvDiscoveryStatus = view.findViewById(R.id.tvDiscoveryStatus)
        tvDevices = view.findViewById(R.id.tvDiscoveryDevices)

        view.findViewById<Button>(R.id.btnDiscoveryRescan).setOnClickListener { startScan() }
        view.findViewById<Button>(R.id.btnDiscoveryBack).setOnClickListener {
            PermissionsFlowHelper.openScanFragment(activity)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        startScan()
    }

    override fun onPause() {
        super.onPause()
        scanner.stop()
    }

    private fun startScan() {
        synchronized(rows) {
            rows.clear()
        }
        renderDeviceList()
        scanner.start(activity, scanListener)
    }

    private fun renderDeviceList() {
        val target = tvDevices ?: return

        val snapshot = synchronized(rows) { rows.toList() }
        if (snapshot.isEmpty()) {
            target.text = getString(R.string.discovery_empty)
            return
        }

        val sb = StringBuilder()
            .append("Devices detected: ")
            .append(snapshot.size)
            .append('\n')
            .append('\n')

        snapshot.forEachIndexed { index, row ->
            sb.append(index + 1).append(". ").append(row).append('\n')
        }

        target.text = sb.toString()
    }
}

