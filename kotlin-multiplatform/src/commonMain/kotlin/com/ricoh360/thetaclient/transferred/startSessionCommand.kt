/*
 * [camera.startSession](https://github.com/ricohapi/theta-api-specs/blob/main/theta-web-api-v2.0/commands/camera.start_session.md)
 */
package com.ricoh360.thetaclient.transferred

import kotlinx.serialization.Serializable

/**
 * start capture request
 */
@Serializable
internal data class StartSessionRequest(
    override val name: String = "camera.startSession",
    override val parameters: EmptyParameter = EmptyParameter()
) : CommandApiRequest

/**
 * start capture response
 */
@Serializable
internal data class StartSessionResponse(
    /**
     * Executed command
     */
    override val name: String,

    /**
     * Command execution status
     * @see CommandState
     */
    override val state: CommandState,

    /**
     * Command ID used to check the execution status with
     * Commands/Status
     */
    override val id: String? = null,

    /**
     * Results when each command is successfully executed.  This
     * output occurs in state "done"
     */
    override val results: ResultStartSession? = null,

    /**
     * Error information (See Errors for details).  This output occurs
     * in state "error"
     */
    override val error: CommandError? = null,

    /**
     * Progress information.  This output occurs in state
     * "inProgress"
     */
    override val progress: CommandProgress? = null
) : CommandApiResponse

/**
 * start capture results
 */
@Serializable
internal data class ResultStartSession(
    /**
     * session identifier
     */
    val sessionId: String,

    /**
     * timeout value
     */
    val timeout: Int
)