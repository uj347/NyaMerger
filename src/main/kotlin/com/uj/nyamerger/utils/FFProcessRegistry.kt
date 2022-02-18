package com.uj.nyamerger.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.bramp.ffmpeg.ProcessFunction
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton
import javax.management.RuntimeMBeanException


class FFProcessRegistry  : ProcessFunction {
    private val repository = Collections.synchronizedCollection(HashSet<Process>())
    init{
        Runtime.getRuntime().addShutdownHook(Thread{terminateAllProcesses()})
    }
    fun terminateAllProcesses() {
        synchronized(repository) {
            repository.iterator().let {
                while (it.hasNext()) {
                    val scopedValue = it.next()
                    kotlin.runCatching { scopedValue.sendQToProcess() }
                        .onFailure { println("Error occurred on FFMpeg process termination: [${it.message}]") }
                    it.remove()
                }
            }
        }
    }

    override fun run(args: MutableList<String>?): Process {
        return ProcessBuilder(args).apply {redirectErrorStream(true)
        }.start().also {
            repository.add(it)
            it.onExit().whenComplete {proc,_->
                repository.remove(proc)
            }
        }
    }

    private fun Process.sendQToProcess() {
        OutputStreamWriter(outputStream).use {
            it.write("q")
        }
    }
}