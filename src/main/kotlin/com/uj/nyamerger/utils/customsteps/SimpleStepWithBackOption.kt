package com.uj.nyamerger.utils.customsteps

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.SimpleStep
import cliinterface.step.oneLineCliInputBlock
import kotlinx.coroutines.flow.FlowCollector

abstract class SimpleStepWithBackOption(
    name: String,
    parentStep: AbstractStep?,
    layoutBlock: suspend (Unit, AbstractStep, IStackController?) ->Unit,
inputBlock: suspend (Unit, AbstractStep, IStackController?) -> String =::oneLineCliInputBlock,
processingBlock: suspend (String, AbstractStep, IStackController?) ->Unit,
onCompletionBlock: suspend FlowCollector<Unit>.(Throwable?) -> Unit= { },
    withQuitOption:Boolean=false

):SimpleStep(
    name=name,
    parentStep=parentStep,
    layoutBlock= formBackLayout(layoutBlock,withQuitOption),
    inputBlock=inputBlock,
    processingBlock= formBackReaction(processingBlock,withQuitOption),
    onCompletionBlock=onCompletionBlock
){

    companion object{

       private const val BACK_STR="back"
        private const val QUIT_STR="quit"
        private const val BACK_MESSAGE_WITH_PARENT="Enter \"back\" to go one step back"
        private const val BACK_MESSAGE_WITHOUT_PARENT ="Enter \"back\" to repeat this step "
        private const val QUIT_MESSAGE="Enter \"quit\" to exit"

       private fun formBackReaction(originalBlock:suspend (String, AbstractStep, IStackController?) ->Unit,withQuitOption: Boolean)
        : suspend (String, AbstractStep, IStackController?) ->Unit {
            return {originStrIput:String, originSelfRef:AbstractStep, originStackController:IStackController? ->
                val parentOfCurrentStep=originSelfRef.parentStep
                when(originStrIput.lowercase()){
                    BACK_STR ->{
                        parentOfCurrentStep?.let {
                            originSelfRef.addAncestorStep(parentOfCurrentStep)
                        }?:originSelfRef.repeatSelf()
                    }
                    QUIT_STR->{
                        if(withQuitOption) {
                            System.exit(0)
                        }else {
                        originalBlock.invoke(originStrIput, originSelfRef, originStackController)
                        }
                    }
                    else->{originalBlock.invoke(originStrIput,originSelfRef,originStackController)}
                }
            }
        }

     private  fun formBackLayout (originLayout:suspend (Unit, AbstractStep, IStackController?) ->Unit,withQuitOption: Boolean):suspend (Unit, AbstractStep, IStackController?) ->Unit{
           return {input, selfRef:AbstractStep,stckCtrl->
               originLayout.invoke(input,selfRef,stckCtrl)
               selfRef.parentStep?.let{ println(BACK_MESSAGE_WITH_PARENT)}
                   ?: println(BACK_MESSAGE_WITHOUT_PARENT)
               if(withQuitOption) println(QUIT_MESSAGE)



           }
        }




    }
}