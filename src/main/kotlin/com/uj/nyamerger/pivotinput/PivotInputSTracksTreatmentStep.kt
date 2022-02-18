package com.uj.nyamerger.pivotinput

import cliinterface.properties.SimpleProperty
import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.addAssociatedProperty
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract

class PivotInputSTracksTreatmentStep : AbstractStep by OptionStep.create(
    name= NyaContract.SAVE_PIVOT_S_TRACKS_NAME,
    parentStep = null,
    multiOptional = false,
    message = MESSAGE,
    optionBundle= optionBundle
) {
    companion object{
        private const val MESSAGE="Do you want to save subtitles from main input?"

        val optionBundle= stepOptionBundle {
            option { idName="yes"
                visibleText="Yes"
                reaction= {input,selfRef,stackCtrl->
                    selfRef.addAssociatedProperty(NyaContract.SAVE_PIVOT_S_TRACKS_PROP,true)
                }
            }
            option { idName="no"
                visibleText="No"
                reaction= {input,selfRef,stackCtrl->
                    selfRef.addAssociatedProperty(NyaContract.SAVE_PIVOT_S_TRACKS_PROP,false)
                }
            }
        }
    }
}