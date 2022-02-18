package com.uj.nyamerger.output

import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.addAssociatedProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.utils.customsteps.SimpleStepWithBackOption
import com.uj.nyamerger.utils.displayErrorMessage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.*

class OutputPathSpecificationStep: SimpleStepWithBackOption(
    name= NyaContract.SPECIFY_OUTPUT_PATH_NAME,
    parentStep = null,
    layoutBlock = layoutBlock,
    processingBlock = processingBlock,
){
    companion object{
        private  val layoutBlock:suspend (Unit, AbstractStep, IStackController?) -> Unit={ _, _, _->
            println("Input output directory")
        }

        private val processingBlock:suspend (String, AbstractStep, IStackController?) ->Unit={ input, selfRef, _->
            kotlin.runCatching {
                //Assertion stage
                val outPath=input.performInputAssertions()
                //Assignment stage
                selfRef.addAssociatedProperty(NyaContract.OUTPUT_PATH_PROP,outPath)

            }.onFailure {
                it.displayErrorMessage("repeat input of the output directory")
                selfRef.repeatSelf()
            }
        }



        private fun String.performInputAssertions():Path {
            val path = Paths.get(this.trim())
            kotlin.runCatching{

                if (!path.exists()) path.createDirectories()
                if (!path.isDirectory()) throw IllegalArgumentException("You pointed to non-directory")
            }.onFailure {
                path.deleteExisting()
                throw it
            }
            return path
        }

    }
}


