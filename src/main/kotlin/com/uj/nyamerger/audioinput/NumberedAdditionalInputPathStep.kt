package com.uj.nyamerger.audioinput

import cliinterface.properties.AbstractProperty
import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.NyaContract.Companion.MULTI_MODE_PROP
import com.uj.nyamerger.NyaContract.Companion.VARIABLE_ADDITIONAL_INPUT_PATH_STEP_NAME_CONST
import com.uj.nyamerger.utils.*
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import java.nio.file.Path
import java.nio.file.Paths

class NumberedAdditionalInputPathStep(index:Int): SimpleStepWithBackOption(
    name = generateName(index),
    parentStep = null,
    layoutBlock = {_,_,_-> println(generateLayoutMessage(index)) },
    processingBlock = generateProcessingBlock(index )
){
    companion object{
        private fun generateProcessingBlock(index: Int):suspend (String, AbstractStep, IStackController?) -> Unit{
            return {input,selfRef,stackCtrl->
                val scopeValueName=NyaContract.constructAdditionalInputPathPropName(index)
                val inputPath= Paths.get(input)
                val stateToken=selfRef.stateToken!!
                kotlin.runCatching {
                    //AssertionStage
                    assertPathIsReal(input)
                    assertMultimode(inputPath,stateToken)

                    //Assignment stage
                    selfRef.addAssociatedProperty(scopeValueName,inputPath)

                    //StackInflationStage
                    selfRef.addNewChildStep(NumberedAddionalInputConfirmationStep(stateToken))

                }.onFailure {
                   it.displayErrorMessage("enter appropriate path")
                    selfRef.repeatSelf()
                }
            }
        }
        private fun generateLayoutMessage(index: Int):String{
            return "Enter  appropriate additional input# $index"
        }
        private fun generateName(index:Int):String{
            return  VARIABLE_ADDITIONAL_INPUT_PATH_STEP_NAME_CONST+index
        }
        private fun assertMultimode(inputPath:Path,stateToken: MutableSet<AbstractProperty<*>>){
            val isAdditionalInputMultimode= isMultimode(inputPath)
            val additionalInputMode=if(isAdditionalInputMultimode){"multimode"}else{"single file mode"}
            val isPivotInputMultimode:Boolean=stateToken.getValueByName<Boolean>(MULTI_MODE_PROP)!!
            val pivotInputMode=if(isPivotInputMultimode){"multimode"}else{"single file mode"}
            if(isPivotInputMultimode!=isPivotInputMultimode)throw IllegalStateException(
                "Input modes doesn't match: primary input is $pivotInputMode " +
                        "and current additional input is $additionalInputMode"
            )

        }
    }
}