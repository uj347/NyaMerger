package com.uj.nyamerger.utils

import cliinterface.properties.AbstractProperty
import cliinterface.properties.SimpleProperty
import org.apache.tools.ant.taskdefs.optional.XMLValidateTask
import kotlin.reflect.KClass
import kotlin.reflect.cast



fun generateStateToken(initProps:Collection<AbstractProperty<*>>?):MutableSet<AbstractProperty<*>>{
    return initProps?.let{ (mutableSetOf<AbstractProperty<*>>()+it).toMutableSet() }?: mutableSetOf()
}

inline fun <reified T:Any> MutableSet<AbstractProperty<*>>.getValueByName(propertyName:String):T?{
        var returnValue:T?=null
        for (prop in this){
            if(prop.name==propertyName&&prop.type == T::class){
                returnValue=prop.value as T
            }
        }
        return returnValue
    }


fun MutableSet<AbstractProperty<*>>.getPropertyByName(propertyName:String):AbstractProperty<*>?{
    return this.firstOrNull{ it.name==propertyName }
}

fun  MutableSet<AbstractProperty<*>>.isPropertyPresent(propertyName:String):Boolean{
return  this.getPropertyByName(propertyName)!=null
}
fun MutableSet<AbstractProperty<*>>.getPropertyType(propertyName:String): KClass<*>? {
    return  this.getPropertyByName(propertyName)?.type
}

inline fun <reified T:Any> AbstractProperty<*>.getTypedValue():T?{
    if(this.type==T::class) {
        return this.value as T
    }else return null

}