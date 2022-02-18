package com.uj.nyamerger.compile

import cliinterface.properties.AbstractProperty
import cliinterface.properties.SimpleProperty
import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.SimpleStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract.Companion.COMPILER_STEP_NAME
import com.uj.nyamerger.NyaContract.Companion.PARALLELISM_PROP
import com.uj.nyamerger.NyaContract.Companion.CONVERSION_DEFERRED_PROP
import com.uj.nyamerger.NyaContract.Companion.CURRENT_CONVERSION_FILES_PROP
import com.uj.nyamerger.NyaContract.Companion.FF_MPEG_PROP
import com.uj.nyamerger.NyaContract.Companion.MULTI_MODE_PROP
import com.uj.nyamerger.NyaContract.Companion.NEW_A_CODEC_PROP
import com.uj.nyamerger.NyaContract.Companion.NEW_VIDEO_RESOLUTION_PROP
import com.uj.nyamerger.NyaContract.Companion.NEW_V_CODEC_PROP
import com.uj.nyamerger.NyaContract.Companion.OUTPUT_PATH_PROP
import com.uj.nyamerger.NyaContract.Companion.PIVOT_INPUT_PATH_PROP
import com.uj.nyamerger.NyaContract.Companion.PROCESSED_SUFFIX
import com.uj.nyamerger.NyaContract.Companion.PROCESS_REGISTRY_PROP
import com.uj.nyamerger.NyaContract.Companion.SAVE_PIVOT_A_TRACKS_PROP
import com.uj.nyamerger.NyaContract.Companion.SAVE_PIVOT_S_TRACKS_PROP
import com.uj.nyamerger.NyaContract.Companion.SAVE_PIVOT_V_TRACKS_PROP
import com.uj.nyamerger.NyaContract.Companion.VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST
import com.uj.nyamerger.utils.*
import jdk.jshell.spi.ExecutionControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*

class CompilerStep(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val compileContext: CoroutineContext = dispatcher + Job()
) : SimpleStep(
    name = COMPILER_STEP_NAME,
    parentStep = null,
    layoutBlock = generateLayoutBlock(compileContext, dispatcher),
    inputBlock = generateInputBlock(compileContext),
    processingBlock = generateProcessingBlock(compileContext),
) {

    companion object {

        private const val ABORT_LITERAL = "abort"
        private const val BACK_LITERAL = "back"
        private const val NORMAL_EXECUTION_LITERAL = "norm"
        private val abortPattern = Pattern.compile(ABORT_LITERAL, Pattern.CASE_INSENSITIVE)
        private val backPattern = Pattern.compile(BACK_LITERAL, Pattern.CASE_INSENSITIVE)
        private const val LAYOUT_MSG = "Conversion is started, " +
                "input \"${com.uj.nyamerger.compile.CompilerStep.Companion.BACK_LITERAL}\" " +
                "to go step back, input\"${com.uj.nyamerger.compile.CompilerStep.Companion.ABORT_LITERAL}\" " +
                "to abort conversion and terminate program"


        private fun generateLayoutBlock(context: CoroutineContext, dispatcher: CoroutineDispatcher):
                suspend (Unit, AbstractStep, IStackController?) -> Unit = { _, selfRef, _ ->
            println(LAYOUT_MSG)
            val conversionScope = CoroutineScope(context + SupervisorJob(context.job))
            val stateToken = selfRef.stateToken!!
            val isMultimode = stateToken.getValueByName<Boolean>(MULTI_MODE_PROP)!!
            val pivotInputPath = stateToken.getValueByName<Path>(PIVOT_INPUT_PATH_PROP)!!
            val outPutDir =
                stateToken.getValueByName<Path>(OUTPUT_PATH_PROP) ?: generateOutPutDirPath(pivotInputPath, isMultimode)
                    .also { newOutPath-> selfRef.addAssociatedProperty(OUTPUT_PATH_PROP,newOutPath)}
            val parallelism: Int =
                stateToken.getValueByName(PARALLELISM_PROP) ?: (Runtime.getRuntime().availableProcessors() / 2)
            val processRegistry = stateToken.getValueByName<FFProcessRegistry>(PROCESS_REGISTRY_PROP)!!
            val currentConversionsFiles=ConcurrentHashMap.newKeySet<Files>() as MutableSet<Files>
            selfRef.addAssociatedProperty(CURRENT_CONVERSION_FILES_PROP,currentConversionsFiles)
            when (isMultimode) {
                true -> {
                    conversionScope.runParallelConversion(pivotInputPath, outPutDir, parallelism, stateToken)
                        .let { def ->
                            stateToken.add(SimpleProperty(CONVERSION_DEFERRED_PROP, selfRef, def, Deferred::class))
                        }
                }
                false -> {
                    val additionalInputs = stateToken.getAllAdditionalInputs()
                    conversionScope.runOneFileConversion(pivotInputPath, additionalInputs, outPutDir, stateToken)
                        .let { def ->
                            stateToken.add(SimpleProperty(CONVERSION_DEFERRED_PROP, selfRef, def, Deferred::class))
                        }
                }
            }
        }


        private fun generateInputBlock(context: CoroutineContext):
                suspend (Unit, AbstractStep, IStackController?) -> String = { _, selfRef, _ ->

            var result: String = NORMAL_EXECUTION_LITERAL
            val stateToken = selfRef.stateToken!!
            val ffProcessRegistry: FFProcessRegistry = stateToken.getValueByName(PROCESS_REGISTRY_PROP)!!
            val conversionDeferred: Deferred<Unit> = stateToken.getValueByName(CONVERSION_DEFERRED_PROP)!!
            CoroutineScope(context).apply {
                //Job to wait for
                launch {

                    var userInputWaitingJob: Job? = null
                    //Wait for deferred
                    val deferredWaitingJob = launch {
                        kotlin.runCatching {
                            conversionDeferred.await()
                        }.onFailure {
                            if(it !is CancellationException) {
                                it.displayErrorMessage("take necessary actions to correct this")
                            }
                            userInputWaitingJob?.cancel()
                        }.onSuccess {
                            println("Conversion done successfully")
                            userInputWaitingJob?.cancel()
                        }
                    }
                    //Wait for user abort/back input
                    userInputWaitingJob = launch(Dispatchers.IO) {
                        val scannerThread=Thread{
                            val scn = Scanner(System.`in`)
                            while (scn.hasNext()) {
                                    val nextLine = scn.nextLine()
                                    this.ensureActive()
                                    when (nextLine.lowercase().trim()) {
                                        ABORT_LITERAL -> {
                                            println("Aborting conversion!")
                                            conversionDeferred.cancel()
                                            result = ABORT_LITERAL
                                            ffProcessRegistry.terminateAllProcesses()
                                            break
                                        }
                                        BACK_LITERAL -> {
                                            println("Aborting conversion and going one step back!")
                                            conversionDeferred.cancel()
                                            ffProcessRegistry.terminateAllProcesses()
                                            result = BACK_LITERAL
                                            break
                                        }
                                    }
                                }
                            }.apply {
                            isDaemon=true
                            start()
                            }
                    while (conversionDeferred.isActive&&this.isActive){
                        delay(1000)
                    }
                    scannerThread.interrupt()
                    }
                }.join()
            }
            result
        }


        private fun generateProcessingBlock(context: CoroutineContext):
                suspend (String, AbstractStep, IStackController?) -> Unit = { strInput, selfRef, _ ->
           val stateToken = selfRef.stateToken!!
            when (strInput) {
                BACK_LITERAL -> {
                    selfRef.parentStep?.let { selfRef.addAncestorStep(it) }
                    cleanUpAbortedMess(stateToken)
                }
                ABORT_LITERAL->{
                  cleanUpAbortedMess(stateToken)
                }
            }

        }

        private fun CoroutineScope.runParallelConversion(
            pivotPath: Path,
            outPutDir: Path,
            parallelism: Int,
            stateToken: MutableSet<AbstractProperty<*>>
        ): Deferred<Unit> {
            return async {
                flow {
                    generateInputMap(pivotPath, stateToken)
                        .forEach { emit(it) }
                }
                    .cancellable()
                    .flatMapMerge(parallelism) { (pivot, additionals) ->
                        flow<Unit> { runOneFileConversion(pivot, additionals, outPutDir, stateToken, true).await() }
                    }.collect()
            }
        }


        private fun CoroutineScope.runOneFileConversion(
            pivotPath: Path,
            additionalInputs: List<Path>,
            outPutDir: Path,
            stateToken: MutableSet<AbstractProperty<*>>,
            isMultimode: Boolean=false
        ): Deferred<Unit> {
            return async {
                println("Starting conversion of ${pivotPath.name}")
                val outputName = pivotPath.nameWithoutExtension + PROCESSED_SUFFIX + "." + pivotPath.extension
                val outputPath = outPutDir.resolve(outputName).also { it.addFileToCleanUpRegistry(stateToken) }
                val ffMpeg: FFmpeg = stateToken.getValueByName(FF_MPEG_PROP)!!
                if (!outPutDir.exists()) outPutDir.createDirectories()
                if (!outPutDir.isDirectory()) throw IllegalArgumentException("Output path most be directory")
                FFmpegBuilder().overrideOutputFiles(true).apply {
                    //Add single thread conversion for parsing in multimode
                    if(isMultimode)addExtraArgs("-threads","1")

                    addInput(pivotPath.toString())
                    for (inputPath in additionalInputs) {
                        addInput(inputPath.toString())
                    }
                    val outputPathString = outputPath.toString()
                    addOutput(outputPathString).customizeOutputFFBuilder(stateToken)
                }.let {builder->
                        kotlin.runCatching { ffMpeg.run(builder)}.onFailure {
                            cleanUpAbortedMess(stateToken)
                            throw IOException("Failed to convert $outputPath")
                        }
                }
            }
        }

        private fun MutableSet<AbstractProperty<*>>.getFFMapArgs(): List<Pair<String, String>> {
            val mapLiteral = "-map"
            val result = mutableListOf<Pair<String, String>>()
            this.forEach {
                when (it.name) {
                    SAVE_PIVOT_A_TRACKS_PROP -> {
                        it.getTypedValue<Boolean>()?.let { bool ->
                            if (bool) result.add(mapLiteral to "0:a")
                        }
                    }
                    SAVE_PIVOT_V_TRACKS_PROP -> {
                        it.getTypedValue<Boolean>()?.let { bool ->
                            if (bool) result.add(mapLiteral to "0:v")
                        }
                    }
                    SAVE_PIVOT_S_TRACKS_PROP -> {
                        it.getTypedValue<Boolean>()?.let { bool ->
                            if (bool) result.add(mapLiteral to "0:s")
                        }
                    }

                }
                if (it.name.contains(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST)) {
                    result.add(
                        mapLiteral to "${
                            it.name.trim().removePrefix(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST)
                        }:a"
                    )
                }
            }
            if (result.isEmpty()) throw IllegalStateException("Cannot extract any -map args from statetoken")
            return result
        }


        private fun FFmpegOutputBuilder.customizeOutputFFBuilder(stateToken: MutableSet<AbstractProperty<*>>) {
            var audioCodec: String? = null
            var videoCodec: String? = null
            var resolution: Pair<Int, Int>? = null

            stateToken.forEach {
                when (it.name) {
                    NEW_A_CODEC_PROP -> {
                        audioCodec = it.getTypedValue()
                    }
                    NEW_V_CODEC_PROP -> {
                        videoCodec = it.getTypedValue()
                    }
                    NEW_VIDEO_RESOLUTION_PROP -> {
                        resolution = it.getTypedValue()
                    }
                }
            }
            audioCodec ?: "copy".let {
                this.setAudioCodec(it)
            }
            videoCodec ?: "copy".let {
                this.setVideoCodec(it)
            }
            resolution?.let {
                this.setVideoResolution(it.first, it.second)
            }
            stateToken.getFFMapArgs().forEach {
                this.addExtraArgs(it.first, it.second)
            }
        }

        private fun generateInputMap(
            pivotInput: Path,
            stateToken: MutableSet<AbstractProperty<*>>
        ): Map<Path, List<Path>> {
            val totalResult = mutableMapOf<Path, List<Path>>()
            val mostCommonExtension: String = findMostCommonExtensionInDir(pivotInput)
            val processedPivot = pivotInput.listDirectoryEntries("*.$mostCommonExtension")
            for (path in processedPivot) {
                val scopeResult = mutableListOf<Path>()
                stateToken.getAllAdditionalInputs().forEach { scopeResult.add(findCompatibleInput(path, it)) }
                totalResult[path] = scopeResult
            }
            if (totalResult.isEmpty()) throw java.lang.IllegalStateException("Somehow inputMap can't be generated from provided inputs")
            return totalResult
        }

        private fun findCompatibleInput(pivotFile: Path, additionalDir: Path): Path {
            if (!additionalDir.isDirectory()) throw IllegalArgumentException("Additional input directory path is not directory")
            val result: Path? = additionalDir
                .listDirectoryEntries()
                .filter { it.fileName.nameWithoutExtension.contains(pivotFile.fileName.nameWithoutExtension) }
                .firstOrNull()
            return result
                ?: throw IllegalArgumentException("Additional input directory doesn't contains matching file ")
        }

        private fun generateOutPutDirPath(pivotInput: Path,isMultimode:Boolean):Path{
            return when(isMultimode){
                true->{
                    pivotInput.resolve(PROCESSED_SUFFIX)
                }
                false->{
                    pivotInput.parent.resolve(pivotInput.nameWithoutExtension+ PROCESSED_SUFFIX)
                }
            }
        }

        private fun Path.addFileToCleanUpRegistry(stateToken: MutableSet<AbstractProperty<*>>){
            val fileRegistry=stateToken.getValueByName<MutableSet<File>>(CURRENT_CONVERSION_FILES_PROP)!!
            this.toFile().let { fileRegistry.add(it)}
        }

      private suspend fun cleanUpAbortedMess(stateToken:MutableSet<AbstractProperty<*>>){
          delay(42)
          println("Cleaning up the mess")
            val fileRegistry=stateToken.getValueByName<MutableSet<File>>(CURRENT_CONVERSION_FILES_PROP)!!
            fileRegistry.forEach { file->
                kotlin.runCatching {
                    FileUtils.forceDelete(file)
                }.onFailure { println("Failed to clean up file: ${file.path}") }

            }

        }

    }

}