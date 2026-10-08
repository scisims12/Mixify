package com.mixify.innertube.utils

object Logger {
    fun d(message: String) {
        println("InnertubeD: $message")
    }
    fun d(t: Throwable?, message: String) {
        println("InnertubeD: $message")
        t?.printStackTrace()
    }
    fun w(message: String) {
        println("InnertubeW: $message")
    }
    fun w(t: Throwable?, message: String) {
        println("InnertubeW: $message")
        t?.printStackTrace()
    }
    fun e(message: String) {
        println("InnertubeE: $message")
    }
    fun e(t: Throwable?, message: String) {
        println("InnertubeE: $message")
        t?.printStackTrace()
    }
}
