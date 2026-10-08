package com.mixify.innertube.pages

import com.mixify.innertube.YouTube
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HomeItemsTest {
    @Test
    fun printHomeItems() {
        runBlocking {
            val result = YouTube.home()
            if (result.isSuccess) {
                val page = result.getOrNull()
                println("Successfully fetched Home page with ${page?.sections?.size ?: 0} sections:")
                page?.sections?.take(3)?.forEach { section ->
                    println("Section: ${section.title}")
                    section.items.take(3).forEach { item ->
                        val typeName = item::class.simpleName ?: "Unknown"
                        println(" - Item: '${item.title}' [Type: $typeName, ID: ${item.id}]")
                    }
                }
            } else {
                println("Failed to fetch Home page: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
