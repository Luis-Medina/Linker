package com.luismedinaweb.linker

import java.io.Serializable

/**
 * Created by luiso on 4/1/2017.
 */
class NetworkPacket(private val mType: TYPE?, val content: String?) : Serializable {

    val isValid: Boolean
        get() = mType != TYPE.UNKNOWN && content != null

    fun getType(): TYPE {
        return mType ?: TYPE.UNKNOWN
    }

    enum class TYPE {
        ACK,
        NACK,
        TERMINATE,
        CLIENT_HELLO,
        SERVER_HELLO,
        LINK,
        UNKNOWN
    }

    companion object {

        private const val serialVersionUID = 1L
    }

}
