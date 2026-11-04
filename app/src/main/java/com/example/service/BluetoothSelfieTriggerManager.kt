package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.content.ContextCompat

data class BluetoothRemoteDeviceInfo(
    val name: String,
    val addressOrDescriptor: String,
    val isConnectedHid: Boolean,
    val isBleM3Model: Boolean = false,
    val deviceTypeLabel: String
)

data class RemoteKeySignal(
    val keyCode: Int,
    val keyName: String,
    val deviceName: String,
    val is5SecHoldSync: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class BleM3StartMode(val label: String, val description: String) {
    INSTANT_CLICK(
        "1-Klik Terus Mula (Instant Start)",
        "Tekan butang BLE-M3 sekali di Tab Timing ET untuk terus membunyikan Pistol Pelepas Sebenar & memulakan rakaman."
    ),
    FIVE_SEC_PRESS(
        "Set to Click Start (5 Sec Press)",
        "Tekan/tahan 5 saat pada BLE-M3 untuk Sync & melepaskan tembakan Pistol Pelepas + Rakaman Video."
    )
}

/**
 * Manages connection detection, BLE-M3 specific HID key/pointer synchronization,
 * strict Cursor & Trigger Locking to the Timing ET tab only (preventing cursor/focus from jumping to other tabs),
 * and START-ONLY trigger execution.
 */
class BluetoothSelfieTriggerManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("psyco_ble_m3_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SYNCED_KEYCODE = "synced_ble_m3_keycode"
        private const val KEY_SYNCED_DEVICE_NAME = "synced_ble_m3_device_name"
        private const val KEY_IS_BLE_M3_SYNCED = "is_ble_m3_synced"
        private const val KEY_START_MODE = "ble_m3_start_mode"
        private const val KEY_INTERCEPT_BLE_POINTER = "ble_m3_intercept_pointer"
        private const val KEY_LOCK_CURSOR_TIMING_TAB_ONLY = "ble_m3_lock_cursor_timing_only"

        val SUPPORTED_REMOTE_KEYCODES = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_SYSRQ,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2
        )

        fun getReadableKeyName(keyCode: Int): String {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> "BLE-M3 VOLUME_UP (Shutter #24)"
                KeyEvent.KEYCODE_VOLUME_DOWN -> "BLE-M3 VOLUME_DOWN (Shutter #25)"
                KeyEvent.KEYCODE_CAMERA -> "BLE-M3 CAMERA_SHUTTER (#27)"
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "BLE-M3 ENTER / CLICK (#66)"
                KeyEvent.KEYCODE_DPAD_CENTER -> "BLE-M3 CENTER_CLICK (#23)"
                KeyEvent.KEYCODE_DPAD_UP -> "BLE-M3 UP_BUTTON (#19)"
                KeyEvent.KEYCODE_DPAD_DOWN -> "BLE-M3 DOWN_BUTTON (#20)"
                KeyEvent.KEYCODE_DPAD_LEFT -> "BLE-M3 LEFT_BUTTON (#21)"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "BLE-M3 RIGHT_BUTTON (#22)"
                KeyEvent.KEYCODE_TAB -> "BLE-M3 TAB_LOCKED (#61)"
                KeyEvent.KEYCODE_PAGE_UP -> "BLE-M3 PAGE_UP (#92)"
                KeyEvent.KEYCODE_PAGE_DOWN -> "BLE-M3 PAGE_DOWN (#93)"
                KeyEvent.KEYCODE_SPACE -> "BLE-M3 SPACE_TRIGGER (#62)"
                KeyEvent.KEYCODE_HEADSETHOOK -> "BLE-M3 HEADSET_TRIGGER (#79)"
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> "BLE-M3 MEDIA_CLICK (#85)"
                -999 -> "BLE-M3 CURSOR LOCKED (Timing ET)"
                else -> "BLE-M3 ${KeyEvent.keyCodeToString(keyCode)} (#$keyCode)"
            }
        }
    }

    private var activeKeyDownCode: Int = -1
    private var activeKeyDownStartMs: Long = 0L

    fun isBleM3Synced(): Boolean = prefs.getBoolean(KEY_IS_BLE_M3_SYNCED, true)

    fun getSyncedKeyCode(): Int = prefs.getInt(KEY_SYNCED_KEYCODE, KeyEvent.KEYCODE_VOLUME_UP)

    fun getSyncedDeviceName(): String = prefs.getString(KEY_SYNCED_DEVICE_NAME, "BLE-M3 Remote") ?: "BLE-M3 Remote"

    fun getStartMode(): BleM3StartMode {
        val raw = prefs.getString(KEY_START_MODE, BleM3StartMode.INSTANT_CLICK.name)
        return try {
            BleM3StartMode.valueOf(raw ?: BleM3StartMode.INSTANT_CLICK.name)
        } catch (_: Exception) {
            BleM3StartMode.INSTANT_CLICK
        }
    }

    fun setStartMode(mode: BleM3StartMode) {
        prefs.edit().putString(KEY_START_MODE, mode.name).apply()
    }

    fun isBlePointerInterceptEnabled(): Boolean = prefs.getBoolean(KEY_INTERCEPT_BLE_POINTER, true)

    fun setBlePointerInterceptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTERCEPT_BLE_POINTER, enabled).apply()
    }

    fun isCursorLockedToTimingTabOnly(): Boolean = prefs.getBoolean(KEY_LOCK_CURSOR_TIMING_TAB_ONLY, true)

    fun setCursorLockedToTimingTabOnly(locked: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_CURSOR_TIMING_TAB_ONLY, locked).apply()
    }

    fun saveBleM3SyncConfiguration(keyCode: Int, deviceName: String) {
        prefs.edit()
            .putBoolean(KEY_IS_BLE_M3_SYNCED, true)
            .putInt(KEY_SYNCED_KEYCODE, keyCode)
            .putString(KEY_SYNCED_DEVICE_NAME, deviceName.ifBlank { "BLE-M3" })
            .apply()
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

    @SuppressLint("MissingPermission")
    fun getDetectedRemoteDevices(): List<BluetoothRemoteDeviceInfo> {
        val detected = mutableListOf<BluetoothRemoteDeviceInfo>()

        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            val deviceIds = inputManager?.inputDeviceIds ?: IntArray(0)
            for (id in deviceIds) {
                val dev = inputManager?.getInputDevice(id) ?: continue
                if (dev.isVirtual) continue
                val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dev.isExternal
                } else {
                    !dev.name.contains("gpio", ignoreCase = true) &&
                            !dev.name.contains("qpnp", ignoreCase = true) &&
                            !dev.name.contains("touch_dev", ignoreCase = true) &&
                            !dev.name.contains("mtk-tpd", ignoreCase = true) &&
                            !dev.name.contains("fts_ts", ignoreCase = true) &&
                            !dev.name.contains("goodix", ignoreCase = true) &&
                            !dev.name.contains("synaptics", ignoreCase = true)
                }

                val hasHidSource = (dev.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD ||
                        (dev.sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD ||
                        (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                        (dev.sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
                        (dev.sources and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD

                if (isExternal && hasHidSource) {
                    val isBleM3 = dev.name.contains("BLE-M3", ignoreCase = true) ||
                            dev.name.contains("BLE_M3", ignoreCase = true) ||
                            dev.name.contains("M3", ignoreCase = true) ||
                            dev.name.contains("BLE", ignoreCase = true)
                    val isLikelyShutter = isBleM3 ||
                            dev.name.contains("Shutter", ignoreCase = true) ||
                            dev.name.contains("Remote", ignoreCase = true) ||
                            dev.name.contains("Selfie", ignoreCase = true) ||
                            dev.name.contains("AB ", ignoreCase = true) ||
                            dev.name.contains("Camera", ignoreCase = true) ||
                            dev.name.contains("Bluetooth", ignoreCase = true) ||
                            dev.name.contains("Consumer", ignoreCase = true)

                    detected.add(
                        BluetoothRemoteDeviceInfo(
                            name = dev.name.ifBlank { "BLE-M3 / Bluetooth Trigger" },
                            addressOrDescriptor = dev.descriptor.take(18),
                            isConnectedHid = true,
                            isBleM3Model = isBleM3,
                            deviceTypeLabel = when {
                                isBleM3 -> "BLE-M3 Remote (Cursor Locked: Timing ET Sahaja)"
                                isLikelyShutter -> "Bluetooth Camera Selfie Trigger (HID Aktif)"
                                else -> "Peranti Input Bluetooth HID Luaran (Aktif)"
                            }
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (hasBluetoothConnectPermission()) {
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val gattDevices = try {
                    btManager?.getConnectedDevices(BluetoothProfile.GATT) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                val bonded = btManager?.adapter?.bondedDevices ?: emptySet()
                val allBtDevices = (gattDevices + bonded).distinctBy { it.address }

                for (btDev in allBtDevices) {
                    val devName = btDev.name ?: "BLE-M3 / Peranti Bluetooth"
                    val addr = btDev.address ?: ""
                    if (detected.none { it.name.equals(devName, ignoreCase = true) }) {
                        val isBleM3 = devName.contains("BLE-M3", ignoreCase = true) ||
                                devName.contains("BLE_M3", ignoreCase = true) ||
                                devName.contains("M3", ignoreCase = true)
                        val isShutterName = isBleM3 ||
                                devName.contains("Shutter", ignoreCase = true) ||
                                devName.contains("Remote", ignoreCase = true) ||
                                devName.contains("Selfie", ignoreCase = true) ||
                                devName.contains("AB ", ignoreCase = true) ||
                                devName.contains("BLE", ignoreCase = true) ||
                                devName.contains("Camera", ignoreCase = true)
                        detected.add(
                            BluetoothRemoteDeviceInfo(
                                name = devName,
                                addressOrDescriptor = addr,
                                isConnectedHid = isShutterName,
                                isBleM3Model = isBleM3,
                                deviceTypeLabel = when {
                                    isBleM3 -> "BLE-M3 Remote Starter (Cursor Locked: Timing ET)"
                                    isShutterName -> "Bluetooth Selfie Remote (Dipasangkan)"
                                    else -> "Peranti Bluetooth Dipasangkan"
                                }
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        return detected.sortedByDescending { it.isBleM3Model || it.isConnectedHid }
    }

    /**
     * Checks if a KeyEvent (ACTION_DOWN, ACTION_UP, or ACTION_MULTIPLE) comes from a Bluetooth Remote / BLE-M3.
     * CRITICAL: Consuming BOTH ACTION_DOWN and ACTION_UP prevents Android's FocusManager from moving the
     * cursor/focus highlight to the bottom NavigationBar tabs!
     */
    fun isRemoteKeyToConsume(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code == KeyEvent.KEYCODE_BACK ||
            code == KeyEvent.KEYCODE_HOME ||
            code == KeyEvent.KEYCODE_APP_SWITCH ||
            code == KeyEvent.KEYCODE_POWER
        ) {
            return false
        }

        val dev = event.device
        val devName = dev?.name ?: ""
        val isExternalBleDevice = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && dev?.isExternal == true) ||
                devName.contains("BLE", ignoreCase = true) ||
                devName.contains("M3", ignoreCase = true) ||
                devName.contains("Shutter", ignoreCase = true) ||
                devName.contains("Remote", ignoreCase = true) ||
                devName.contains("Keyboard", ignoreCase = true) ||
                devName.contains("Consumer", ignoreCase = true)

        val isKnownCode = code in SUPPORTED_REMOTE_KEYCODES || code == getSyncedKeyCode()
        return isKnownCode || isExternalBleDevice
    }

    /**
     * Checks if this specific KeyEvent should fire the Starter Gun & Start Timer (only on ACTION_DOWN).
     */
    fun isSupportedTriggerKey(event: KeyEvent): Boolean {
        if (!isRemoteKeyToConsume(event)) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode
            val devName = event.device?.name ?: ""
            if (event.repeatCount == 0) {
                activeKeyDownCode = code
                activeKeyDownStartMs = System.currentTimeMillis()
                return true
            } else {
                val heldDurationMs = System.currentTimeMillis() - activeKeyDownStartMs
                if (heldDurationMs in 4500L..5600L || (event.flags and KeyEvent.FLAG_LONG_PRESS) != 0) {
                    saveBleM3SyncConfiguration(code, devName.ifBlank { "BLE-M3" })
                    return true
                }
            }
        }
        return false
    }

    /**
     * Detects ANY MotionEvent (cursor move, hover, scroll, or click) coming from an external
     * Bluetooth HID device (such as BLE-M3's built-in mouse/touchpad cursor) so we can consume it
     * and lock the cursor strictly inside Timing ET without letting it click other bottom tabs!
     */
    fun isExternalBleHidMotionEvent(event: MotionEvent): Boolean {
        if (!isBlePointerInterceptEnabled()) return false
        val dev = event.device ?: return false
        if (dev.isVirtual) return false

        val devName = dev.name ?: ""
        val isNamedBleRemote = devName.contains("BLE-M3", ignoreCase = true) ||
                devName.contains("BLE_M3", ignoreCase = true) ||
                devName.contains("BLE", ignoreCase = true) ||
                devName.contains("Shutter", ignoreCase = true) ||
                devName.contains("Remote", ignoreCase = true) ||
                devName.contains("Selfie", ignoreCase = true) ||
                devName.contains("Consumer", ignoreCase = true)

        val isMouseOrTouchpadSource = (event.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
                (event.source and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD ||
                (dev.sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE

        val isExternalTouchFromBle = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && dev.isExternal) &&
                isNamedBleRemote

        return isNamedBleRemote || isMouseOrTouchpadSource || isExternalTouchFromBle
    }

    /**
     * Returns true only when the external BLE-M3 pointer event is a click/press action that should trigger Start.
     */
    fun isBleM3ExternalPointerTrigger(event: MotionEvent): Boolean {
        if (!isExternalBleHidMotionEvent(event)) return false
        return event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
    }

    fun extractSignal(event: KeyEvent): RemoteKeySignal {
        val deviceName = try {
            event.device?.name?.takeIf { it.isNotBlank() } ?: "BLE-M3 Bluetooth Trigger"
        } catch (_: Exception) {
            "BLE-M3 Bluetooth Trigger"
        }
        val heldMs = if (activeKeyDownStartMs > 0L) System.currentTimeMillis() - activeKeyDownStartMs else 0L
        val is5Sec = heldMs >= 4500L || event.repeatCount > 15
        if (is5Sec || !isBleM3Synced()) {
            saveBleM3SyncConfiguration(event.keyCode, deviceName)
        }
        return RemoteKeySignal(
            keyCode = event.keyCode,
            keyName = if (is5Sec) "${getReadableKeyName(event.keyCode)} [5s SYNC LOCK ✓]" else getReadableKeyName(event.keyCode),
            deviceName = deviceName,
            is5SecHoldSync = is5Sec
        )
    }

    fun extractPointerSignal(event: MotionEvent): RemoteKeySignal {
        val deviceName = try {
            event.device?.name?.takeIf { it.isNotBlank() } ?: "BLE-M3 HID Touch-Clicker"
        } catch (_: Exception) {
            "BLE-M3 HID Touch-Clicker"
        }
        saveBleM3SyncConfiguration(-999, deviceName)
        return RemoteKeySignal(
            keyCode = -999,
            keyName = getReadableKeyName(-999),
            deviceName = deviceName,
            is5SecHoldSync = true
        )
    }
}
