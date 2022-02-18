package com.uj.nyamerger.output

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import com.uj.nyamerger.utils.displayErrorMessage

class ParallelismSpecificationStep : SimpleStepWithBackOption(
    name= NyaContract.SPECIFY_PARALLELISM_NAME,
    parentStep = null,
    layoutBlock = layoutBlock,
    processingBlock = processingBlock,
){
    companion object{
        private  val layoutBlock:suspend (Unit, AbstractStep, IStackController?) -> Unit={ _, _, _->
            println("Specify number of threads to run conversion. [Your CPU have ${Runtime.getRuntime().availableProcessors()} cores]")
        }

        private val processingBlock:suspend (String, AbstractStep, IStackController?) ->Unit={ input, selfRef, _->
            kotlin.runCatching {
                //Assertion stage
                val parallelism=input.performInputAssertions()
                //Assignment stage
                selfRef.addAssociatedProperty(NyaContract.PARALLELISM_PROP,parallelism)

            }.onFailure {
                it.displayErrorMessage("enter number.")
                selfRef.repeatSelf()
            }
        }



        private fun String.performInputAssertions(): Int {
            return kotlin.runCatching {
               this.trim().toInt()
            }.getOrElse {throw IllegalArgumentException("Invalid input") }
        }
    }
}