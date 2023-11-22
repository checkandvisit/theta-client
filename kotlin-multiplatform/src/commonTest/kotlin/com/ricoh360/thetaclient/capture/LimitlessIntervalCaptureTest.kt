package com.ricoh360.thetaclient.capture

import com.goncalossilva.resources.Resource
import com.ricoh360.thetaclient.CheckRequest
import com.ricoh360.thetaclient.MockApiClient
import com.ricoh360.thetaclient.ThetaRepository
import com.ricoh360.thetaclient.transferred.CaptureMode
import io.ktor.client.network.sockets.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LimitlessIntervalCaptureTest {
    private val endpoint = "http://192.168.1.1:80/"

    @BeforeTest
    fun setup() {
        MockApiClient.status = HttpStatusCode.OK
    }

    @AfterTest
    fun teardown() {
        MockApiClient.status = HttpStatusCode.OK
    }

    /**
     * call startCapture.
     */
    @Test
    fun startCaptureTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/LimitlessIntervalCapture/start_capture_done.json").readText(),
            Resource("src/commonTest/resources/LimitlessIntervalCapture/stop_capture_done.json").readText()
        )
        var counter = 0
        MockApiClient.onRequest = { request ->
            val index = counter++

            var response = ""
            // check request
            when (index) {
                0 -> {
                    CheckRequest.checkSetOptions(request = request, captureMode = CaptureMode.IMAGE)
                    response = responseArray[0]
                }

                1 -> {
                    CheckRequest.checkSetOptions(request = request, captureNumber = 0)
                    response = responseArray[1]
                }

                2 -> {
                    CheckRequest.checkCommandName(request, "camera.startCapture")
                    response = responseArray[2]
                }

                else -> {
                    if (CheckRequest.getCommandName(request) == "camera.stopCapture") {
                        CheckRequest.checkCommandName(request, "camera.stopCapture")
                        response = responseArray[3]
                    } else if (request.url.encodedPath == "/osc/state") {
                        response =
                            Resource("src/commonTest/resources/LimitlessIntervalCapture/state_shooting.json").readText()
                    }
                }
            }

            ByteReadChannel(response)
        }
        val deferred = CompletableDeferred<Unit>()

        // execute
        val thetaRepository = ThetaRepository(endpoint)
        val capture = thetaRepository.getLimitlessIntervalCaptureBuilder().build()

        assertNull(capture.getCaptureInterval(), "set option captureInterval")

        var files: List<String>? = null
        val capturing = capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "error stop capture")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "error capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                files = fileUrls
                deferred.complete(Unit)
            }
        })
        runBlocking {
            delay(100)
        }
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // check result
        assertTrue(files?.firstOrNull()?.startsWith("http://") ?: false, "start capture limitless interval")
    }

    /**
     * call startCapture.
     */
    @Test
    fun cancelStartCaptureTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/LimitlessIntervalCapture/start_capture_done.json").readText(),
        )
        var counter = 0
        var idleCount = 0
        MockApiClient.onRequest = { request ->
            val index = counter++

            var response = ""
            // check request
            when (index) {
                0 -> {
                    CheckRequest.checkSetOptions(request = request, captureMode = CaptureMode.IMAGE)
                    response = responseArray[0]
                }

                1 -> {
                    CheckRequest.checkSetOptions(request = request, captureNumber = 0)
                    response = responseArray[1]
                }

                2 -> {
                    CheckRequest.checkCommandName(request, "camera.startCapture")
                    response = responseArray[2]
                }

                else -> {
                    if (request.url.encodedPath == "/osc/state") {
                        idleCount += 1
                        response =
                            Resource("src/commonTest/resources/LimitlessIntervalCapture/state_idle.json").readText()
                    } else {
                        assertTrue(false, "error capture limitless interval")
                    }
                }
            }

            ByteReadChannel(response)
        }
        val deferred = CompletableDeferred<Unit>()

        // execute
        val thetaRepository = ThetaRepository(endpoint)
        val capture = thetaRepository.getLimitlessIntervalCaptureBuilder().build()

        assertNull(capture.getCaptureInterval(), "set option captureInterval")

        var files: List<String>? = null
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "error stop capture")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "error capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                files = fileUrls
                deferred.complete(Unit)
            }
        })
        runBlocking {
            delay(100)
        }

        runBlocking {
            withTimeout(5000) {
                deferred.await()
            }
        }

        // check result
        assertNull(files, "cancel capture limitless interval")
        assertTrue(idleCount >= 2, "cancel capture limitless interval")
    }

    /**
     * Setting captureInterval.
     */
    @Test
    fun settingCaptureIntervalTest() = runTest {
        val interval = 5

        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText()
        )
        var counter = 0

        MockApiClient.onRequest = { request ->
            val index = counter++

            // check request
            when (index) {
                0 -> {
                    CheckRequest.checkSetOptions(request = request, captureMode = CaptureMode.IMAGE)
                }

                1 -> {
                    CheckRequest.checkSetOptions(
                        request = request,
                        captureInterval = interval
                    )
                }
            }

            ByteReadChannel(responseArray[index])
        }

        // execute
        val thetaRepository = ThetaRepository(endpoint)
        val capture = thetaRepository.getLimitlessIntervalCaptureBuilder()
            .setCaptureInterval(interval)
            .build()

        // check result
        assertEquals(capture.getCaptureInterval(), interval, "set option captureInterval $interval")
    }

    /**
     * Error response to build call.
     */
    @Test
    fun buildErrorResponseTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_error.json").readText(), // set captureMode error
            "Not json" // json error
        )
        var counter = 0

        MockApiClient.onRequest = { _ ->
            val index = counter++
            ByteReadChannel(responseArray[index])
        }

        // execute
        val thetaRepository = ThetaRepository(endpoint)

        var exceptionSetCaptureMode = false
        try {
            thetaRepository.getLimitlessIntervalCaptureBuilder().build()
        } catch (e: ThetaRepository.ThetaWebApiException) {
            assertTrue(e.message!!.indexOf("UnitTest", 0, true) >= 0, "")
            exceptionSetCaptureMode = true
        }
        assertTrue(exceptionSetCaptureMode, "setOptions captureMode error response")

        // execute not json response
        var exceptionNotJson = false
        try {
            thetaRepository.getLimitlessIntervalCaptureBuilder().build()
        } catch (e: ThetaRepository.ThetaWebApiException) {
            assertTrue(
                e.message!!.indexOf("json", 0, true) >= 0 || e.message!!.indexOf(
                    "Illegal",
                    0,
                    true
                ) >= 0,
                "setOptions option not json error response"
            )
            exceptionNotJson = true
        }
        assertTrue(exceptionNotJson, "setOptions option error response")
    }

    /**
     * Error exception to build call.
     */
    @Test
    fun buildExceptionTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_error.json").readText(), // status error & error json
            "Status error UnitTest", // status error not json
            "timeout UnitTest" // timeout
        )
        var counter = 0

        MockApiClient.onRequest = { _ ->
            val index = counter++
            when (index) {
                0 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                1 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                2 -> throw ConnectTimeoutException("timeout")
            }
            ByteReadChannel(responseArray[index])
        }

        // execute
        val thetaRepository = ThetaRepository(endpoint)

        // execute status error and json response
        var exceptionStatusJson = false
        try {
            thetaRepository.getLimitlessIntervalCaptureBuilder()
                .build()
        } catch (e: ThetaRepository.ThetaWebApiException) {
            assertTrue(
                e.message!!.indexOf("UnitTest", 0, true) >= 0,
                "status error and json response"
            )
            exceptionStatusJson = true
        }
        assertTrue(exceptionStatusJson, "status error and json response")

        // execute status error and not json response
        var exceptionStatus = false
        try {
            thetaRepository.getLimitlessIntervalCaptureBuilder().build()
        } catch (e: ThetaRepository.ThetaWebApiException) {
            assertTrue(e.message!!.indexOf("503", 0, true) >= 0, "status error")
            exceptionStatus = true
        }
        assertTrue(exceptionStatus, "status error")

        // execute timeout exception
        var exceptionOther = false
        try {
            thetaRepository.getLimitlessIntervalCaptureBuilder()
                .build()
        } catch (e: ThetaRepository.NotConnectedException) {
            assertTrue(e.message!!.indexOf("time", 0, true) >= 0, "timeout exception")
            exceptionOther = true
        }
        assertTrue(exceptionOther, "other exception")
    }

    /**
     * Error response to startCapture call
     */
    @Test
    fun startCaptureErrorResponseTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/LimitlessIntervalCapture/start_capture_error.json").readText(), // startCapture error
            "Not json" // json error
        )
        var counter = 0
        MockApiClient.onRequest = { _ ->
            val index = counter++
            ByteReadChannel(responseArray[index])
        }

        val thetaRepository = ThetaRepository(endpoint)
        val capture = thetaRepository.getLimitlessIntervalCaptureBuilder().build()

        // execute error response
        var deferred = CompletableDeferred<Unit>()
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(
                    exception.message!!.indexOf("UnitTest", 0, true) >= 0,
                    "capture limitless interval error response"
                )
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }
        })

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute json error response
        deferred = CompletableDeferred()
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(exception.message!!.length >= 0, "capture limitless interval json error response")
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }
        })

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }
    }

    /**
     * Error exception to startCapture call
     */
    @Test
    fun startCaptureExceptionTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/setOptions/set_options_done.json").readText(),
            Resource("src/commonTest/resources/LimitlessIntervalCapture/start_capture_error.json").readText(), // startCapture error
            "Status error UnitTest", // status error not json
            "timeout UnitTest" // timeout
        )
        var counter = 0
        MockApiClient.onRequest = { _ ->
            val index = counter++
            when (index) {
                0 -> MockApiClient.status = HttpStatusCode.OK
                1 -> MockApiClient.status = HttpStatusCode.OK
                2 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                3 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                4 -> throw ConnectTimeoutException("timeout")
            }
            ByteReadChannel(responseArray[index])
        }

        val thetaRepository = ThetaRepository(endpoint)
        val capture = thetaRepository.getLimitlessIntervalCaptureBuilder()
            .build()

        // execute status error and json response
        var deferred = CompletableDeferred<Unit>()
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(
                    exception.message!!.indexOf("UnitTest", 0, true) >= 0,
                    "status error and json response"
                )
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }
        })

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute status error and not json response
        deferred = CompletableDeferred()
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(exception.message!!.indexOf("503", 0, true) >= 0, "status error")
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }
        })

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute timeout exception
        deferred = CompletableDeferred()
        capture.startCapture(object : LimitlessIntervalCapture.StartCaptureCallback {
            override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }

            override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                assertTrue(exception.message!!.indexOf("time", 0, true) >= 0, "timeout exception")
                deferred.complete(Unit)
            }

            override fun onCaptureCompleted(fileUrls: List<String>?) {
                assertTrue(false, "capture limitless interval")
                deferred.complete(Unit)
            }
        })

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }
    }

    /**
     * Error response to stopCapture call
     */
    @Test
    fun stopCaptureErrorResponseTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/LimitlessIntervalCapture/stop_capture_error.json").readText(), // stopCapture error
            "Not json" // json error
        )
        var counter = 0
        MockApiClient.onRequest = { _ ->
            val index = counter++
            ByteReadChannel(responseArray[index])
        }

        // execute error response
        var deferred = CompletableDeferred<Unit>()
        var capturing = LimitlessIntervalCapturing(
            endpoint,
            object : LimitlessIntervalCapture.StartCaptureCallback {
                override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(
                        exception.message!!.indexOf("UnitTest", 0, true) >= 0,
                        "stop capture error response"
                    )
                    deferred.complete(Unit)
                }

                override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }

                override fun onCaptureCompleted(fileUrls: List<String>?) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }
            }
        )
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute json error response
        deferred = CompletableDeferred()
        capturing = LimitlessIntervalCapturing(
            endpoint,
            object : LimitlessIntervalCapture.StartCaptureCallback {
                override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(exception.message!!.length >= 0, "stop capture json error response")
                    deferred.complete(Unit)
                }

                override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }

                override fun onCaptureCompleted(fileUrls: List<String>?) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }
            }
        )
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }
    }

    /**
     * Error exception to stopCapture call
     */
    @Test
    fun stopCaptureExceptionTest() = runTest {
        // setup
        val responseArray = arrayOf(
            Resource("src/commonTest/resources/LimitlessIntervalCapture/stop_capture_error.json").readText(), // status error & error json
            "Status error UnitTest", // status error not json
            "timeout UnitTest" // timeout
        )
        var counter = 0
        MockApiClient.onRequest = { _ ->
            val index = counter++
            when (index) {
                1 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                2 -> MockApiClient.status = HttpStatusCode.ServiceUnavailable
                3 -> throw ConnectTimeoutException("timeout")
            }
            ByteReadChannel(responseArray[index])
        }

        // execute status error and json response
        var deferred = CompletableDeferred<Unit>()
        var capturing = LimitlessIntervalCapturing(
            endpoint,
            object : LimitlessIntervalCapture.StartCaptureCallback {
                override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(
                        exception.message!!.indexOf("UnitTest", 0, true) >= 0,
                        "status error and json response"
                    )
                    deferred.complete(Unit)
                }

                override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }

                override fun onCaptureCompleted(fileUrls: List<String>?) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }
            }
        )
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute status error and not json response
        deferred = CompletableDeferred()
        capturing = LimitlessIntervalCapturing(
            endpoint,
            object : LimitlessIntervalCapture.StartCaptureCallback {
                override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(exception.message!!.indexOf("503", 0, true) >= 0, "status error")
                    deferred.complete(Unit)
                }

                override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }

                override fun onCaptureCompleted(fileUrls: List<String>?) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }
            }
        )
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }

        // execute timeout exception
        deferred = CompletableDeferred()
        capturing = LimitlessIntervalCapturing(
            endpoint,
            object : LimitlessIntervalCapture.StartCaptureCallback {
                override fun onStopFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(
                        exception.message!!.indexOf("time", 0, true) >= 0,
                        "timeout exception"
                    )
                    deferred.complete(Unit)
                }

                override fun onCaptureFailed(exception: ThetaRepository.ThetaRepositoryException) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }

                override fun onCaptureCompleted(fileUrls: List<String>?) {
                    assertTrue(false, "stop capture")
                    deferred.complete(Unit)
                }
            }
        )
        capturing.stopCapture()

        runBlocking {
            withTimeout(1000) {
                deferred.await()
            }
        }
    }
}
