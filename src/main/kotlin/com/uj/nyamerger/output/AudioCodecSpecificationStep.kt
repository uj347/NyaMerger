package com.uj.nyamerger.output

import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.addAssociatedProperty
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract

class AudioCodecSpecificationStep:AbstractStep by OptionStep.create (
    name = NyaContract.SPECIFY_AUDIO_CODEC_NAME,
    parentStep = null,
    multiOptional = false,
    message = MESSAGE_TEXT,
    optionBundle = optionBundle


) {

    companion object{
        private  const val MESSAGE_TEXT="Select new audio codec for the output"
        val optionBundle= stepOptionBundle {
            for (codec in NyaContract.supportedACodecNames){
                option {
                    idName=codec
                    visibleText=codec
                    reaction={_,selfRef,_->
                        selfRef.addAssociatedProperty(NyaContract.NEW_A_CODEC_PROP,codec)
                    }
                }
            }
        }
    }
}