package com.uj.nyamerger.pivotinput

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract.Companion.FF_PROBE_PROP
import com.uj.nyamerger.NyaContract.Companion.MULTI_MODE_PROP
import com.uj.nyamerger.NyaContract.Companion.PIVOT_INPUT_NAME
import com.uj.nyamerger.NyaContract.Companion.PIVOT_INPUT_PATH_PROP
import com.uj.nyamerger.audioinput.NumberedAddionalInputConfirmationStep
import com.uj.nyamerger.utils.*
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory


class PivotInputStep : SimpleStepWithBackOption(
    name = PIVOT_INPUT_NAME,
    parentStep = null,
    layoutBlock = {_,_,_-> println(LAYOUT_TEXT) },
    processingBlock = processingBlock,
    withQuitOption = true

    ) {
    companion object {
        private const val LAYOUT_TEXT = "Enter primary input, this could be single file or directory of homogenous mediafiles"

        private val processingBlock: suspend (String, AbstractStep, IStackController?) -> Unit =
            { stringInput: String, selfRef: AbstractStep, iStackController: IStackController? ->
                val stateToken = selfRef.stateToken!!
                val ffProbe: FFprobe = stateToken.getValueByName(FF_PROBE_PROP)!!
                val inputPath=Paths.get(stringInput)


                //AssertionStage
                kotlin.runCatching { assertPathIsReal(stringInput)

                //Assignment stage
                    selfRef.addAssociatedProperty(PIVOT_INPUT_PATH_PROP,inputPath)

               isMultimode(inputPath).let {
                    selfRef.addAssociatedProperty(MULTI_MODE_PROP,it)
                }


                val subtitleTracksPresent = isSubtitlesPresent(inputPath, ffProbe)
                val videoTracksPresent = isVideoPresent(inputPath, ffProbe)
                val audioTracksPresent = isAudioPresent(inputPath, ffProbe)



                selfRef.addNewChildStep(NumberedAddionalInputConfirmationStep(stateToken))

                    if (subtitleTracksPresent) selfRef.addNewChildStep(PivotInputSTracksTreatmentStep())
                    if(videoTracksPresent) selfRef.addNewChildStep(PivotInputVTracksTreatmentStep())
                    if(audioTracksPresent) selfRef.addNewChildStep(PivotInputATracksTreatmentStep())

                }.onFailure {
                    it.displayErrorMessage("enter appropriate input path")
                    selfRef.repeatSelf()
                }
            }





        private fun getConcreteFileOrSample(inputPath: Path): Path {
            return when (inputPath.isDirectory()) {
                true -> {
                    findMostCommonExtensionInDir(inputPath).let { mfqExt ->
                        takeSampleOfFileWithExtension(inputPath, mfqExt)
                    }
                }
                false -> {
                    inputPath
                }
            }
        }

        private fun isSubtitlesPresent(inputPath: Path, ffProbe: FFprobe): Boolean {
            val probePath = getConcreteFileOrSample(inputPath)
            val toReturn= ffProbe.probe(probePath.toString()).getStreams()
                .filter { it.codec_name == "subrip" }
                .isNotEmpty()
            return toReturn
        }


        private fun isVideoPresent(inputPath: Path, ffProbe: FFprobe): Boolean {
            val probePath = getConcreteFileOrSample(inputPath)
            val toReturn=ffProbe.probe(probePath.toString()).getStreams()
                .filter { it.codec_type == FFmpegStream.CodecType.VIDEO }
                .isNotEmpty()
            return toReturn
        }

        private fun isAudioPresent(inputPath: Path, ffProbe: FFprobe): Boolean {
            val probePath = getConcreteFileOrSample(inputPath)
            val toReturn= ffProbe.probe(probePath.toString()).getStreams()
                .filter { it.codec_type == FFmpegStream.CodecType.AUDIO }
                .isNotEmpty()
            return toReturn
        }

    }
}