package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import androidx.core.content.ContextCompat

data class BluetoothRemoteDeviceInfo(
    val name: String,
    val addressOrDescriptor: String,
    val isConnectedHid: Boolean,
    val deviceTypeLabel: String
)

data class RemoteKeySignal(
    val keyCode: Int,
    val keyName: String,
    val deviceName: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Manages connection detection, key-code calibration, and START-ONLY trigger execution
 * for Bluetooth Camera Selfie Remotes (e.g., AB Shutter 3, Bluetooth Remote Shutter, HID Selfie Triggers).
 */
class BluetoothSelfieTriggerManager(private val context: Context) {

    companion object {
        /**
         * Standard key codes emitted by Bluetooth Camera Selfie Remotes on Android:
         * - KEYCODE_VOLUME_UP (24): Primary shutter button on 95% of Bluetooth selfie triggers
         * - KEYCODE_VOLUME_DOWN (25): Secondary / Android shutter button
         * - KEYCODE_CAMERA (27): Dedicated camera shutter keycode
         * - KEYCODE_ENTER (66): AB Shutter 3 iOS/Keyboard button
         * - KEYCODE_DPAD_CENTER (23): Remote OK button
         * - KEYCODE_SPACE (62): HID keyboard trigger
         * - KEYCODE_HEADSETHOOK (79) / KEYCODE_MEDIA_PLAY_PAUSE (85): Bluetooth media button triggers
         */
        val SUPPORTED_REMOTE_KEYCODES = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        )

        fun getReadableKeyName(keyCode: Int): String {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> "VOLUME_UP (Shutter Utama #24)"
                KeyEvent.KEYCODE_VOLUME_DOWN -> "VOLUME_DOWN (Shutter Android #25)"
                KeyEvent.KEYCODE_CAMERA -> "CAMERA_SHUTTER (#27)"
                KeyEvent.KEYCODE_ENTER -> "ENTER / HID TRIGGER (#66)"
                KeyEvent.KEYCODE_DPAD_CENTER -> "CENTER_OK (#23)"
                KeyEvent.KEYCODE_SPACE -> "SPACE_TRIGGER (#62)"
                KeyEvent.KEYCODE_HEADSETHOOK -> "HEADSET_TRIGGER (#79)"
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "MEDIA_TRIGGER (#85)"
                else -> KeyEvent.keyCodeToString(keyCode)
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Scans both active external HID InputDevices (which work even without special permissions)
     * and bonded Bluetooth devices to detect connected Bluetooth Selfie Remotes.
     */
    @SuppressLint("MissingPermission")
    fun getDetectedRemoteDevices(): List<BluetoothRemoteDeviceInfo> {
        val detected = mutableListOf<BluetoothRemoteDeviceInfo>()

        // 1. Check active external InputDevices (HID Keyboards / Consumer Controls / Shutter Remotes)
        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            val deviceIds = inputManager?.inputDeviceIds ?: IntArray(0)
            for (id in deviceIds) {
                val dev = inputManager?.getInputDevice(id) ?: continue
                if (dev.isVirtual) continue
                // Check if external (Bluetooth / USB HID)
                val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dev.isExternal
                } else {
                    !dev.name.contains("gpio", ignoreCase = true) &&
                            !dev.name.contains("qpnp", ignoreCase = true) &&
                            !dev.name.contains("touch", ignoreCase = true) &&
                            !dev.name.contains("mtk-tpd", ignoreCase = true)
                }

                val hasKeys = (dev.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD ||
                        (dev.sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD ||
                        (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD

                if (isExternal && hasKeys) {
                    val isLikelyShutter = dev.name.contains("Shutter", ignoreCase = true) ||
                            dev.name.contains("Remote", ignoreCase = true) ||
                            dev.name.contains("Selfie", ignoreCase = true) ||
                            dev.name.contains("AB ", ignoreCase = true) ||
                            dev.name.contains("BLE", ignoreCase = true) ||
                            dev.name.contains("Camera", ignoreCase = true) ||
                            dev.name.contains("Bluetooth", ignoreCase = true)

                    detected.add(
                        BluetoothRemoteDeviceInfo(
                            name = dev.name.ifBlank { "Bluetooth Selfie Trigger" },
                            addressOrDescriptor = dev.descriptor.take(18),
                            isConnectedHid = true,
                            deviceTypeLabel = if (isLikelyShutter) {
                                "Bluetooth Camera Selfie Trigger (Aktif)"
                            } else {
                                "Peranti Input HID Luaran (Aktif)"
                            }
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Also check paired/bonded Bluetooth devices if permission is granted
        if (hasBluetoothConnectPermission()) {
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bonded = btManager?.adapter?.bondedDevices ?: emptySet()
                for (btDev in bonded) {
                    val devName = btDev.name ?: "Peranti Bluetooth"
                    val addr = btDev.address ?: ""
                    if (detected.none { it.name.equals(devName, ignoreCase = true) }) {
                        val isShutterName = devName.contains("Shutter", ignoreCase = true) ||
                                devName.contains("Remote", ignoreCase = true) ||
                                devName.contains("Selfie", ignoreCase = true) ||
                                devName.contains("AB ", ignoreCase = true) ||
                                devName.contains("Camera", ignoreCase = true)
                        detected.add(
                            BluetoothRemoteDeviceInfo(
                                name = devName,
                                addressOrDescriptor = addr,
                                isConnectedHid = isShutterName,
                                deviceTypeLabel = if (isShutterName) {
                                    "Bluetooth Selfie Remote (Dipasangkan)"
                                } else {
                                    "Peranti Bluetooth Dipasangkan"
                                }
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        return detected
    }

    /**
     * Checks if a KeyEvent comes from a supported Bluetooth Selfie Trigger button.
     */
    fun isSupportedTriggerKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false // Ignore key hold repeats
        return event.keyCode in SUPPORTED_REMOTE_KEYCODES
    }

    fun extractSignal(event: KeyEvent): RemoteKeySignal {
        val deviceName = try {
            event.device?.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Selfie Trigger"
        } catch (_: Exception) {
            "Bluetooth Selfie Trigger"
        }
        return RemoteKeySignal(
            keyCode = event.keyCode,
            keyName = getReadableKeyName(event.keyCode),
            deviceName = deviceName
        )
    }
}
