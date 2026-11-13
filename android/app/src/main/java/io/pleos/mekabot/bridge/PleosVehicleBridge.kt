package io.pleos.mekabot.bridge

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.CruiseControlStateListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener
import ai.pleos.playground.vehicle.listener.TirePressureListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Pleos Playground Vehicle SDK v2.3.2 Bridge
 *
 * Binds real Pleos Connect Vehicle hardware events (Gear, Wiper, HVAC, Tire, EvBattery,
 * Odometer, DrivingMode, CarInfo, Light, Seat) to the In-Car WebGL/HTML Battle Engine.
 * Enforces Zero-Distraction compliance and Capability Fallback rules.
 */
@SuppressLint("MissingPermission")
class PleosVehicleBridge(
    private val context: Context,
    private val webView: WebView,
    private val onGearStateChanged: (String) -> Unit
) {
    companion object {
        private const val TAG = "PleosVehicleBridge"
    }

    private val vehicle = Vehicle(context.applicationContext)
    private var isSdkConnected = false
    private var supportsAmbientColor = false

    private val frontWindshield = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val fourWheels = listOf(
        WheelArea(WheelValue.LEFT_FRONT),
        WheelArea(WheelValue.RIGHT_FRONT),
        WheelArea(WheelValue.LEFT_REAR),
        WheelArea(WheelValue.RIGHT_REAR)
    )
    private val wheelPressures = mutableMapOf<WheelValue, Float>()

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            val gearStr = mapGear(gear)
            onGearStateChanged(gearStr)
            emitToWeb("onGearChanged", """{"gear":"$gearStr","source":"PleosVehicleSDK"}""")
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "GearSelectionListener failed: ${e.message}")
        }
    }

    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            val isRaining = (state == WiperState.ON || state == WiperState.SERVICE)
            emitToWeb("onWiperChanged", """{"isRaining":$isRaining,"state":"${state?.name ?: "OFF"}"}""")
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "WiperStateListener failed: ${e.message}")
        }
    }

    private val hvacTempListener = object : HvacTemperatureValueListener {
        override fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?) {
            celsius?.let { temp ->
                emitToWeb("onExteriorTempChanged", """{"temp":$temp}""")
            }
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "HvacTemperatureValueListener failed: ${e.message}")
        }
    }

    private val cruiseListener = object : CruiseControlStateListener {
        override fun onCruiseControlStateUpdated(state: CruiseControl?) {
            val isCruise = (state == CruiseControl.ACTIVATED || state == CruiseControl.ENABLED)
            emitToWeb("onCruiseChanged", """{"isCruise":$isCruise,"state":"${state?.name ?: "OFF"}"}""")
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "CruiseControlStateListener failed: ${e.message}")
        }
    }

    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) {
            value?.let { km ->
                emitToWeb("onOdometerChanged", """{"km":$km}""")
            }
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "DistanceDrivenListener failed: ${e.message}")
        }
    }

    private val evSocListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
            value?.let { soc ->
                emitToWeb("onEvBatteryChanged", """{"soc":$soc}""")
            }
        }
        override fun onFailed(e: Exception) {
            Log.w(TAG, "EvBatteryStateOfChargeListener failed: ${e.message}")
        }
    }

    fun startListening() {
        try {
            vehicle.initialize()
            isSdkConnected = true
            Log.i(TAG, "Pleos Vehicle SDK v2.3.2 initialized successfully.")
            emitToWeb("onSdkConnected", """{"connected":true,"version":"2.3.2"}""")

            bindCarInfoAndCapabilities()
            bindGearStream()
            bindEnvironmentStreams()
            bindTireAndBatteryStreams()
        } catch (e: Exception) {
            isSdkConnected = false
            Log.w(TAG, "Pleos Vehicle Service unavailable, running in Hybrid Mock mode: ${e.message}")
            emitToWeb("onSdkConnected", """{"connected":false,"reason":"${e.message ?: "Emulator Fallback"}"}""")
        }
    }

    private fun bindCarInfoAndCapabilities() {
        val carInfo = vehicle.getCarInfo()
        carInfo.checkVINInfoCapabilities(
            onComplete = { cap ->
                if (cap.accessType != AccessType.NONE) {
                    carInfo.getVINInfo(
                        onComplete = { vin ->
                            emitToWeb("onVinIdentified", """{"vin":"${vin ?: "KMH-IONIQ6-PLEOS"}"}""")
                        },
                        onFailed = {}
                    )
                }
            },
            onFailed = {}
        )

        val light = vehicle.getLight()
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { cap, areas ->
                supportsAmbientColor = (cap.accessType != AccessType.NONE && areas.isNotEmpty())
                emitToWeb(
                    "onAmbientCapabilityChecked",
                    """{"supported":$supportsAmbientColor,"accessType":"${cap.accessType.name}"}"""
                )
            },
            onFailed = {
                supportsAmbientColor = false
                emitToWeb("onAmbientCapabilityChecked", """{"supported":false,"accessType":"FALLBACK"}""")
            }
        )
    }

    private fun bindGearStream() {
        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState(
            onComplete = { currentGear ->
                val gearStr = mapGear(currentGear)
                onGearStateChanged(gearStr)
                emitToWeb("onGearChanged", """{"gear":"$gearStr","source":"PleosVehicleSDK"}""")
            },
            onFailed = {}
        )
        try {
            gear.registerGearSelection(gearListener)
        } catch (e: Exception) {
            Log.w(TAG, "registerGearSelection error: ${e.message}")
        }
    }

    private fun bindEnvironmentStreams() {
        // 1. Wiper (Water Element)
        val wiper = vehicle.getWiper()
        wiper.checkWiperStateCapabilities(
            onComplete = { cap, _ ->
                if (cap.accessType != AccessType.NONE) {
                    wiper.getCurrentWiperState(
                        area = frontWindshield,
                        onComplete = { state ->
                            val isRaining = (state == WiperState.ON || state == WiperState.SERVICE)
                            emitToWeb("onWiperChanged", """{"isRaining":$isRaining,"state":"${state?.name ?: "OFF"}"}""")
                        },
                        onFailed = {}
                    )
                    wiper.registerWiperState(frontWindshield, wiperListener)
                }
            },
            onFailed = {}
        )

        // 2. HVAC Temperature (Fire / Ice Elements)
        val hvac = vehicle.getHVAC()
        hvac.checkHvacTemperatureCurrentCapabilities(
            onComplete = { cap, _ ->
                if (cap.accessType != AccessType.NONE) {
                    hvac.getHvacTemperatureCurrent(
                        area = driverSeat,
                        unit = TemperatureUnit.CELSIUS,
                        onComplete = { temp ->
                            temp?.let { emitToWeb("onExteriorTempChanged", """{"temp":$it}""") }
                        },
                        onFailed = {}
                    )
                    hvac.registerHvacTemperatureCurrent(driverSeat, hvacTempListener)
                }
            },
            onFailed = {}
        )

        // 3. DrivingMode Cruise Control (Wind Element)
        val drivingMode = vehicle.getDrivingMode()
        drivingMode.checkCruiseControlStateCapabilities(
            onComplete = { cap ->
                if (cap.accessType != AccessType.NONE) {
                    drivingMode.getCruiseControlState(
                        onComplete = { state ->
                            val isCruise = (state == CruiseControl.ACTIVATED || state == CruiseControl.ENABLED)
                            emitToWeb("onCruiseChanged", """{"isCruise":$isCruise}""")
                        },
                        onFailed = {}
                    )
                    drivingMode.registerCruiseControlState(cruiseListener)
                }
            },
            onFailed = {}
        )

        // 4. Odometer (Earth Element & Anti-Spoofing)
        val odometer = vehicle.getOdometer()
        odometer.checkTripCumulativeDrivingDistanceCapabilities(
            onComplete = { cap ->
                if (cap.accessType != AccessType.NONE) {
                    odometer.getTripCumulativeDrivingDistance(
                        onComplete = { km ->
                            km?.let { emitToWeb("onOdometerChanged", """{"km":$it}""") }
                        },
                        onFailed = {}
                    )
                    odometer.registerDistanceDriven(odometerListener)
                }
            },
            onFailed = {}
        )
    }

    private fun bindTireAndBatteryStreams() {
        // 1. 4-Wheel Tire Pressure (Full Armor Buff vs Bluehands Maintenance Alert)
        val tire = vehicle.getTire()
        tire.checkTirePressureCapabilities(
            onComplete = { cap, supportedWheels ->
                if (cap.accessType != AccessType.NONE) {
                    val targetWheels = if (supportedWheels.isNotEmpty()) supportedWheels else fourWheels
                    targetWheels.forEach { wheelArea ->
                        val wheelVal = wheelArea.values.firstOrNull() ?: WheelValue.LEFT_FRONT
                        tire.getTirePressure(
                            area = wheelArea,
                            onComplete = { pressure ->
                                pressure?.let {
                                    wheelPressures[wheelVal] = it
                                    evaluateFullArmor()
                                }
                            },
                            onFailed = {}
                        )
                        tire.registerTirePressure(
                            wheelArea,
                            object : TirePressureListener {
                                override fun onTirePressureUpdated(pressure: Float?) {
                                    pressure?.let {
                                        wheelPressures[wheelVal] = it
                                        evaluateFullArmor()
                                    }
                                }
                                override fun onFailed(e: Exception) {}
                            }
                        )
                    }
                }
            },
            onFailed = {}
        )

        // 2. EV Battery State & Charge Switch (EV Hyper-Recharge & E-pit Boss Raid)
        val evBattery = vehicle.getEvBattery()
        evBattery.checkEvBatteryStateOfChargeCapabilities(
            onComplete = { cap ->
                if (cap.accessType != AccessType.NONE) {
                    evBattery.getEvBatteryStateOfCharge(
                        onComplete = { soc ->
                            soc?.let { emitToWeb("onEvBatteryChanged", """{"soc":$it}""") }
                        },
                        onFailed = {}
                    )
                    evBattery.registerEvBatteryStateOfCharge(evSocListener)
                }
            },
            onFailed = {}
        )
        evBattery.checkEvChargeSwitchCapabilities(
            onComplete = { cap ->
                if (cap.accessType != AccessType.NONE) {
                    evBattery.getEvChargeSwitchState(
                        onComplete = { isCharging ->
                            emitToWeb("onEvChargingChanged", """{"charging":${isCharging == true}}""")
                        },
                        onFailed = {}
                    )
                }
            },
            onFailed = {}
        )
    }

    private fun evaluateFullArmor() {
        if (wheelPressures.isEmpty()) return
        val allNormal = wheelPressures.values.all { it >= 30.0f }
        emitToWeb("onTireStatusChanged", """{"fullArmor":$allNormal,"pressures":${wheelPressures.values}}""")
    }

    private fun mapGear(gear: VehicleGear?): String = when (gear) {
        VehicleGear.GEAR_PARK -> "P"
        VehicleGear.GEAR_REVERSE -> "R"
        VehicleGear.GEAR_NEUTRAL -> "N"
        VehicleGear.GEAR_DRIVE,
        VehicleGear.GEAR_1,
        VehicleGear.GEAR_2,
        VehicleGear.GEAR_3,
        VehicleGear.GEAR_4,
        VehicleGear.GEAR_5,
        VehicleGear.GEAR_6,
        VehicleGear.GEAR_7,
        VehicleGear.GEAR_8,
        VehicleGear.GEAR_9 -> "D"
        else -> "P"
    }

    @JavascriptInterface
    fun syncAmbientLightColor(argbHex: String) {
        if (!isSdkConnected || !supportsAmbientColor) return
        try {
            val colorInt = android.graphics.Color.parseColor(argbHex)
            val light = vehicle.getLight()
            light.setAmbientLightSwitch(driverSeat, LightSwitch.ON, { _, _ -> }, {})
            light.setAmbientLightBrightnessColor(driverSeat, colorInt, { _, _ -> }, {})
        } catch (e: Exception) {
            Log.w(TAG, "syncAmbientLightColor fallback: ${e.message}")
        }
    }

    @JavascriptInterface
    fun triggerSeatHapticFeedback(durationMs: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "triggerSeatHapticFeedback error: ${e.message}")
        }
    }

    @JavascriptInterface
    fun requestVehicleRefresh() {
        if (isSdkConnected) {
            bindGearStream()
            bindEnvironmentStreams()
            bindTireAndBatteryStreams()
        }
    }

    fun release() {
        if (!isSdkConnected) return
        try {
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getWiper().unregisterWiperState(frontWindshield, wiperListener)
            vehicle.getHVAC().unregisterHvacTemperatureCurrent(driverSeat, hvacTempListener)
            vehicle.getDrivingMode().unregisterCruiseControlState(cruiseListener)
            vehicle.getOdometer().unregisterDistanceDriven(odometerListener)
            vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(evSocListener)
            vehicle.release()
        } catch (e: Exception) {
            Log.w(TAG, "release error: ${e.message}")
        }
        isSdkConnected = false
    }

    private fun emitToWeb(event: String, jsonPayload: String) {
        webView.post {
            webView.evaluateJavascript(
                "window.PleosEventBus && window.PleosEventBus.dispatch('$event', $jsonPayload);",
                null
            )
        }
    }
}
