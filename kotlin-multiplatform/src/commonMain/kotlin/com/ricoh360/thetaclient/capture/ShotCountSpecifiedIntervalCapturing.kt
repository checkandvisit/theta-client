package com.ricoh360.thetaclient.capture

import com.ricoh360.thetaclient.ThetaApi
import com.ricoh360.thetaclient.ThetaRepository
import com.ricoh360.thetaclient.transferred.StopCaptureResponse
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * ShotCountSpecifiedIntervalCapturing
 *
 * @property endpoint URL of Theta web API endpoint
 * @property callback Success or failure of the call
 */
class ShotCountSpecifiedIntervalCapturing internal constructor(
    private val endpoint: String,
    private val callback: ShotCountSpecifiedIntervalCapture.StartCaptureCallback
) : Capturing() {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun cancelCapture() {
        stopCapture()
    }

    /**
     * Stops interval shooting with the shot count specified
     * When call stopCapture() then call property callback.
     */
    override fun stopCapture() {
        scope.launch {
            lateinit var response: StopCaptureResponse
            try {
                response = ThetaApi.callStopCaptureCommand(endpoint = endpoint)
                response.error?.let {
                    callback.onStopFailed(exception = ThetaRepository.ThetaWebApiException(message = it.message))
                    return@launch
                }
            } catch (e: JsonConvertException) {
                callback.onStopFailed(exception = ThetaRepository.ThetaWebApiException(message = e.message ?: e.toString()))
                return@launch
            } catch (e: ResponseException) {
                callback.onStopFailed(exception = ThetaRepository.ThetaWebApiException.create(exception = e))
                return@launch
            } catch (e: Exception) {
                callback.onStopFailed(exception = ThetaRepository.NotConnectedException(message = e.message ?: e.toString()))
                return@launch
            }
        }
    }
}