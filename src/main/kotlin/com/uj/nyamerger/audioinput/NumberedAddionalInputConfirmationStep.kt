package com.uj.nyamerger.audioinput

import cliinterface.properties.AbstractProperty
import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract.Companion.VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST
import com.uj.nyamerger.NyaContract.Companion.constructAdditionalInputPathStepName
import com.uj.nyamerger.output.OutputConfigurationStep
import kotlin.math.max

class NumberedAddionalInputConfirmationStep(stateToken:MutableSet<AbstractProperty<*>>):AbstractStep by OptionStep.create(
    name = constructName(stateToken),
    parentStep = null,
    multiOptional = false,
    message = generateMessage(computeIndex(stateToken)),
    optionBundle= optionBundle
    ) {
    companion object{


        private val optionBundle= stepOptionBundle {
       option {
           idName="yes"
           visibleText="Yes"
           reaction={ strInput, selfRef, stackCtrl ->
               val index=computeIndex(selfRef.stateToken!!)
                selfRef.addNewChildStep(NumberedAdditionalInputPathStep(index))
           }
       }
            option {
                idName="no"
                visibleText="No"
                reaction={ strInput, selfRef, stackCtrl ->
                    selfRef.addNewChildStep(OutputConfigurationStep())
                }
            }
        }



        private fun generateMessage(index:Int):String{
            val message="Do you want to add additional input# $index ?"
            return message
        }


        private fun computeIndex(stateToken: MutableSet<AbstractProperty<*>>):Int{
            val allAudioPathProps:MutableSet<AbstractProperty<*>> =mutableSetOf()
            for (prop in stateToken){
                if (prop.name.contains(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST))allAudioPathProps.add(prop)
            }
            if(allAudioPathProps.isEmpty()) return 1
            var maxIndex=1
            for(prop in allAudioPathProps){
                val propIndex=prop.name.removePrefix(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST).let { Integer.valueOf(it) }
                maxIndex= max(maxIndex,propIndex)
            }
            return (maxIndex+1)
        }

        private fun constructName(stateToken:MutableSet<AbstractProperty<*>>):String{
            return constructAdditionalInputPathStepName( computeIndex(stateToken))
        }



    }

}