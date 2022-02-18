package com.uj.nyamerger


import cliinterface.properties.AbstractProperty
import cliinterface.properties.SimpleProperty
import cliinterface.stack.SimpleStackController
import com.uj.nyamerger.compile.CompilerStep
import com.uj.nyamerger.prerequisites.PrerequisitesAssessmentStep
import com.uj.nyamerger.utils.FFProcessRegistry
import com.uj.nyamerger.utils.findFFMpegBinDirOrThrow
import com.uj.nyamerger.utils.getFFBinsFromDirOrThrow
import com.uj.nyamerger.utils.getJarLocation
import kotlinx.coroutines.runBlocking

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import org.apache.commons.io.FileUtils
import org.apache.commons.io.file.PathUtils
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.collections.HashSet
import kotlin.io.path.exists

fun maindfgdfgaaa(){
    runBlocking {
        val root=Paths.get("j:\\")
        runCatching {
            findFFMpegBinDirOrThrow(root).let {
                println("FFMpeg dir is: $it")
                getFFBinsFromDirOrThrow(it).let {bins->
                    println(
                        "FFMPEG  is : ${bins.first}\n" +
                                "FFPROBE IS: ${bins.second}"
                    )
                }
            }
        }.onFailure { println(it.message)}
        println("JAR location is : ${getJarLocation()}")

    }

}
fun main12333(){
    val ffBinsPattern= Pattern.compile("ffprobe|ffmpeg",Pattern.CASE_INSENSITIVE)
    val ffprobe=ffBinsPattern.matcher("ffprobe")
    val ffmpeg=ffBinsPattern.matcher("ffmpeg")
    val chaosProbe=ffBinsPattern.matcher("FfProbE")
    val chaosMpeg=ffBinsPattern.matcher("fFMpeG")
    val wrong=ffBinsPattern.matcher("BOobaffMpeg")
    println("case1: ${ffprobe.matches()}, case 2: ${ffmpeg.matches()}, case 3 : ${chaosProbe.matches()}, case 4: ${chaosMpeg.matches()}, wrong: ${wrong.matches()}")
}

fun main(){
    runBlocking {
        val stateToken= ConcurrentHashMap.newKeySet<AbstractProperty<*>>()


        val stacC= SimpleStackController(
            PrerequisitesAssessmentStep(),
            null,
            stateToken,
            coroutineContext
        )

        stacC.runChain().join()
    }
}

    fun mainasdasdasd(){
        println("Jar location is : ${getJarLocation()}")
    }


    fun main12422222(){
        runBlocking {
            val processRegistry = FFProcessRegistry()

            val ffMpegProp = FFmpeg("J:\\NyaM\\ffmpegdir\\bin\\ffmpeg.exe", processRegistry).let {
                SimpleProperty<FFmpeg>(NyaContract.FF_MPEG_PROP, null, it, FFmpeg::class)
            }
            val ffProbeProp =
                FFprobe("J:\\NyaM\\ffmpegdir\\bin\\ffprobe.exe", processRegistry).let {
                    SimpleProperty<FFprobe>(NyaContract.FF_PROBE_PROP, null, it, FFprobe::class)
                }

            val processRegistryProperty =
                SimpleProperty(NyaContract.PROCESS_REGISTRY_PROP, null, processRegistry, FFProcessRegistry::class)


            val stateToken = HashSet<AbstractProperty<*>>()
                .apply {
                    add(
                        SimpleProperty(
                            NyaContract.PIVOT_INPUT_PATH_PROP,
                            null,
                            Paths.get("j:\\testmultisource\\"),
                            Path::class
                        )
                    )
                    //add(SimpleProperty(NyaContract.VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST+1,null,Paths.get("j:\\testmultisource\\"), Path::class))
                    add(SimpleProperty(NyaContract.SAVE_PIVOT_V_TRACKS_PROP, null, true, Boolean::class))
                    add(SimpleProperty(NyaContract.OUTPUT_PATH_PROP, null, Paths.get("j:\\compilerTest"), Path::class))
                    add(SimpleProperty(NyaContract.NEW_V_CODEC_PROP, null, "copy", String::class))
                    add(SimpleProperty(NyaContract.MULTI_MODE_PROP, null, true, Boolean::class))
                    add(SimpleProperty(NyaContract.PARALLELISM_PROP, null, 2, Int::class))
                    add(SimpleProperty(NyaContract.NEW_VIDEO_RESOLUTION_PROP, null, 124 to 124, Pair::class))
                    add(processRegistryProperty)
                    addAll(listOf(ffMpegProp, ffProbeProp))
                }


//            val stacC = SimpleStackController(
//                CompilerStep(),
//                null,
//                stateToken,
//                coroutineContext
//            )
        val stacC= SimpleStackController(
            CompilerStep(),
            null,
            stateToken,
            coroutineContext
        )
            stacC.runChain().join()
            Scanner(System.`in`).apply {
                nextLine()
                val compPath=Paths.get("j:\\compilerTest")
                if(compPath.exists()){
                    PathUtils.cleanDirectory(compPath)
                }
            }
        }
    }







