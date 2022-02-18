package com.uj.nyamerger.pivotinput

import cliinterface.step.AbstractStep
import cliinterface.step.OptionStep
import cliinterface.step.addAssociatedProperty
import cliinterface.step.stepoptions.option
import cliinterface.step.stepoptions.stepOptionBundle
import com.uj.nyamerger.NyaContract

class PivotInputVTracksTreatmentStep : AbstractStep by OptionStep.create(
    name= NyaContract.SAVE_PIVOT_V_TRACKS_NAME,
    parentStep = null,
    multiOptional = false,
    message = MESSAGE,
    optionBundle= optionBundle
) {
    companion object{
        private const val MESSAGE="Do you want to save video tracks from main input?"

        val optionBundle= stepOptionBundle {
            option { idName="yes"
                visibleText="Yes"
                reaction= {input,selfRef,stackCtrl->
                    selfRef.addAssociatedProperty(NyaContract.SAVE_PIVOT_V_TRACKS_PROP,true)
                }
            }
            option { idName="no"
                visibleText="No"
                reaction= {input,selfRef,stackCtrl->
                    selfRef.addAssociatedProperty(NyaContract.SAVE_PIVOT_V_TRACKS_PROP,false)
                }
            }
        }
    }
}