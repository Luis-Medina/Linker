package com.luismedinaweb.linker

sealed class SearchStatus {

    object NotStarted : SearchStatus()

    data class Searching(val message: String) : SearchStatus()

    object Cancelled : SearchStatus()

    data class Finished(val serverData: ServerData?) : SearchStatus()
}