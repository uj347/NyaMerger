package com.uj.nyamerger.prerequisites

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.SimpleStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.pivotinput.PivotInputStep
import com.uj.nyamerger.utils.*
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import java.nio.file.Path

class PrerequisitesAssessmentStep : SimpleStep(
    name= NyaContract.PRE_REQUISITES_ASSESSMENT_STEP_NAME,
    parentStep = null,
    layoutBlock = layoutBlock,
    inputBlock={_,_,_->""},
    processingBlock = processingBlock,
){
    companion object{
        private  val layoutBlock:suspend (Unit, AbstractStep, IStackController?) -> Unit={ _, _, _->

        }

        private val processingBlock:suspend (String, AbstractStep, IStackController?) ->Unit={ input, selfRef, _->
            val processRegistry=FFProcessRegistry()
            selfRef.addAssociatedProperty(NyaContract.PROCESS_REGISTRY_PROP,processRegistry)
            var ffMpegPath: Path?=null
            var ffProbePath:Path?=null
                kotlin.runCatching {
                    //IF JAR LOCATION UNAVAILABLE -ONLY OPTION IS TO ASK USER
                    val workDir= getJarLocation()
                    kotlin.runCatching {
                        //LOOKUP CACHED PROP FILE
                        getFFPathFromPropertiesOrThrow(workDir).let { (mpeg,probe)->
                            ffMpegPath=mpeg
                            ffProbePath=probe
                        }
                    }.onFailure {
                        //LOOKUP WORKDIR FOR PATHS
                        //todo
                        findFFMpegBinDirOrThrow(workDir).let { ffBinP->
                        getFFBinsFromDirOrThrow(ffBinP).let { pair->
                            ffMpegPath=pair.first
                            ffProbePath=pair.second
                            kotlin.runCatching { writeFFPropertiesToFileOrThrow(workDir,pair) }
                        }
                        }
                    }
                }.onFailure {
                //ASK USER
                    selfRef.addNewChildStep(FFDirSpecficationStep())
                }.onSuccess {
                kotlin.runCatching {
                   checkNotNull(ffMpegPath)
                   checkNotNull(ffProbePath)
                    selfRef.addAssociatedProperty(NyaContract.FF_MPEG_PROP,
                                                    FFmpeg(ffMpegPath!!.toString(),processRegistry))
                    selfRef.addAssociatedProperty(NyaContract.FF_PROBE_PROP,
                                                    FFprobe(ffProbePath!!.toString(),processRegistry))
                }.onFailure {
                    //ASK USER
                    selfRef.addNewChildStep(FFDirSpecficationStep())
                }.onSuccess {
                    //HERE IS SUCCESS SO GO ON TO MEDIA BUSINESS LOGIC
                    selfRef.addNewChildStep(PivotInputStep())
                }
                }
        }
        }
}



