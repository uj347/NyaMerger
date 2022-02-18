package com.uj.nyamerger.output

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract.Companion.NEW_VIDEO_RESOLUTION_PROP
import com.uj.nyamerger.NyaContract.Companion.SPECIFY_VIDEO_RESOLUTION_NAME
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import com.uj.nyamerger.utils.displayErrorMessage
import java.util.regex.Pattern

class VideoResolutionSpecificationStep: SimpleStepWithBackOption(
    name=SPECIFY_VIDEO_RESOLUTION_NAME,
    parentStep = null,
    layoutBlock = layoutBlock,
    processingBlock = processingBlock,
    ){
    companion object{
        private  val layoutBlock:suspend (Unit, AbstractStep, IStackController?) -> Unit={ _,_,_->
            println("Input new resolution for the output file in format {DDDDxDDDD}")
        }

        private val processingBlock:suspend (String, AbstractStep, IStackController?) ->Unit={input,selfRef,_->
            kotlin.runCatching {
                //Assertion stage
                input.performInputAssertions()
                //Assignment stage
                selfRef.addAssociatedProperty(NEW_VIDEO_RESOLUTION_PROP,input.splitIntoNumPair())

            }.onFailure {
                it.displayErrorMessage("repeat input of new resolution")
                selfRef.repeatSelf()
            }
        }

        private fun String.performInputAssertions(){
            val correctPatternMatcher=Pattern.compile("\\d{1,4}\\D+\\d{1,4}\\s*").matcher(this)
            if(!correctPatternMatcher.matches())throw IllegalArgumentException("Wrong input format")
        }

         private fun String.splitIntoNumPair():Pair<Int,Int>{
             val input = this.trim()
             val pattern=Pattern.compile("\\D+")
            input.split(pattern).map { it.trim().toInt()}.let{
                return it[0] to it[1]
            }
        }
    }

}