package com.uj.nyamerger.output

import cliinterface.properties.AbstractProperty
import cliinterface.stack.IStackController
import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.compile.CompilerStep
import com.uj.nyamerger.utils.generateStateToken
import com.uj.nyamerger.utils.getValueByName
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext

class OutputConfigurationStep: AbstractStep by OptionStep.create(
    name = NyaContract.OUTPUT_CONFIGURATION_NAME,
    parentStep = null,
    multiOptional = true,
    message = MESSAGE_TEXT,
    optionBundle = optionBundle,
    firstAction = lastAction
    ) {
    companion object{
        private const val MESSAGE_TEXT = "Select necessary manipulations on output (options separated by whitespace):"

        private val lastAction:
                suspend (String, AbstractStep, IStackController?) -> Unit ={_,selfRef,_->
                    selfRef.addNewChildStep(CompilerStep())
                    selfRef.stateToken?.
                    getValueByName<Boolean>(NyaContract.MULTI_MODE_PROP)?.
                    let {
                        if (it){
                            selfRef.addNewChildStep(ParallelismSpecificationStep())
                        }
                    }

                }

        private val optionBundle= stepOptionBundle {

            option{
                idName="justMerge"
                visibleText="Just merge inputs"
                reaction={_,_,_->}
            }

            option {
                idName="changeVCodec"
                visibleText="Change video codec"
                reaction={_,selfRef,_->
                    selfRef.addNewChildStep(VideoCodecSpecificationStep())
                }
            }

            option {
                idName="changeACodec"
                visibleText="Change audio codec"
                reaction={_,selfRef,_->
                    selfRef.addNewChildStep(AudioCodecSpecificationStep())
                }
            }
            option {
                idName="changeVRes"
                visibleText="Change video resolution"
                reaction={_,selfRef,_->
                    selfRef.addNewChildStep(VideoResolutionSpecificationStep())
                }
            }

            option {
                idName="specifyOutputDir"
                visibleText="Specify output dir (by default new directory will be generated in directory containing pivot input)"
                reaction={_,selfRef,_->
                   selfRef.addNewChildStep(OutputPathSpecificationStep())
                }
            }


        }

}
}