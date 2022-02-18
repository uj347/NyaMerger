package com.uj.nyamerger.output

import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.addAssociatedProperty
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract.Companion.NEW_V_CODEC_PROP
import com.uj.nyamerger.NyaContract.Companion.SPECIFY_VIDEO_CODEC_NAME
import com.uj.nyamerger.NyaContract.Companion.supportedVCodecNames

class VideoCodecSpecificationStep:AbstractStep by OptionStep.create(
    name = SPECIFY_VIDEO_CODEC_NAME,
    parentStep = null,
    multiOptional = false,
    message = MESSAGE_TEXT,
    optionBundle = optionBundle


) {

    companion object{
        private  const val MESSAGE_TEXT="Select new video codec for the output"
        val optionBundle= stepOptionBundle {
            for (codec in supportedVCodecNames){
                option {
                    idName=codec
                    visibleText=codec
                    reaction={_,selfRef,_->
                        selfRef.addAssociatedProperty(NEW_V_CODEC_PROP,codec)
                    }
                }
            }
        }
    }
}