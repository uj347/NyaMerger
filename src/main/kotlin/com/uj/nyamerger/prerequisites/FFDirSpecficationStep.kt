package com.uj.nyamerger.prerequisites

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.pivotinput.PivotInputStep
import com.uj.nyamerger.utils.*
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class FFDirSpecficationStep: SimpleStepWithBackOption(
    name= NyaContract.FF_MPEG_DIR_SPECIFICATION_NAME,
    parentStep = null,
    layoutBlock = layoutBlock,
    processingBlock = processingBlock,
    withQuitOption = true
){
    companion object{
        private  val layoutBlock:suspend (Unit, AbstractStep, IStackController?) -> Unit={ _, _, _->
            println("Input full path to directory containing ffMpeg binaries")
        }

        private val processingBlock:suspend (String, AbstractStep, IStackController?) ->Unit={ input, selfRef, _->
            kotlin.runCatching {
                //Assertion stage
                val ffPath=input.performInputAssertions()
                val ffBinsPaths= getFFBinsFromDirOrThrow(findFFMpegBinDirOrThrow(ffPath))
                val processRegistry:FFProcessRegistry=selfRef.stateToken!!.getValueByName(NyaContract.PROCESS_REGISTRY_PROP)!!
                //Assignment stage
                kotlin.runCatching { writeFFPropertiesToFileOrThrow(getJarLocation(),ffBinsPaths) }.onFailure {
                    println("Failed to save FFMpeg binaries paths to properties file")
                }
                val ffMpeg=FFmpeg(ffBinsPaths.first.toString())
                    val ffProbe=FFprobe(ffBinsPaths.second.toString())
                selfRef.addAssociatedProperty(NyaContract.FF_MPEG_PROP,ffMpeg)
                selfRef.addAssociatedProperty(NyaContract.FF_PROBE_PROP,ffProbe)
                selfRef.addNewChildStep(PivotInputStep())

            }.onFailure {
                it.displayErrorMessage("repeat input of the output directory")
                selfRef.repeatSelf()
            }
        }



        private fun String.performInputAssertions(): Path {
            val path = Paths.get(this.trim())
            kotlin.runCatching{
                check(path.isAbsolute)
                check (path.exists(),)
                check(path.isDirectory())
            }.onFailure {
                throw IllegalArgumentException("You entered incorrect path")
            }
            return path
        }

    }
}
