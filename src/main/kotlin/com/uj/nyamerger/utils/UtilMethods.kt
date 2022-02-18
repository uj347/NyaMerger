package com.uj.nyamerger.utils

import cliinterface.properties.AbstractProperty
import com.uj.nyamerger.NyaContract
import com.uj.nyamerger.NyaContract.Companion.FF_MPEG_PROP
import com.uj.nyamerger.NyaContract.Companion.FF_PROBE_PROP
import com.uj.nyamerger.NyaContract.Companion.VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.regex.Pattern
import kotlin.NoSuchElementException
import kotlin.io.path.*

/** Finds most common extension in directory*/
fun findMostCommonExtensionInDir(path: Path):String{
    if(!path.isDirectory()) throw IllegalArgumentException("Not directory path")
    val countMap:MutableMap<String,Int> =HashMap()
    path.listDirectoryEntries()
        .filter {!it.isDirectory()}
        .forEach {
            val extension=it.extension
            val fromMapCount=countMap.get(extension)?:0
            countMap.put(extension,fromMapCount+1)
    }
    var maxEntry:Pair<String,Int>?=null
    countMap.forEach { ext,count->
    if((maxEntry?.second?:0)<count)
        maxEntry = ext to count
    }
    return maxEntry!!.first
}

/** Take a sample of file with most frequent extension in pointed dir*/
fun takeSampleOfFileWithExtension(fromDir:Path,extension: String):Path{
    if(!fromDir.isDirectory())throw IllegalArgumentException("You most pass directory to this method")
 return  fromDir.listDirectoryEntries().filter { it.extension==extension }.first()
}

fun countFilesForExtensionInDir(directory:Path, ext:String):Int{
    if(!directory.isDirectory())throw IllegalArgumentException("Argument pat most be a directory")
    return directory.listDirectoryEntries().map { it.extension }.filter { it==ext }.count()
}


fun findCommonPattern(fileNames:List<String>):String{
    val minStringLength=fileNames.map{it.length}.reduce { acc: Int, s: Int -> minOf(acc,s) }
    val sampleName=fileNames.first()
    var firstStaticOffset:Int=0
    for (i:Int in 0 until minStringLength){
        val checkSymb=fileNames.first().toCharArray().get(i)
        var breakTime:Boolean=false
        fileNames.forEach { if(it.toCharArray().get(i)!=checkSymb)breakTime=true }
        if(breakTime)break
        else firstStaticOffset=i
    }
    var secondStaticNegativeOffset:Int=0;
    for(i:Int in 0 until minStringLength){
        val checkSymb=fileNames.first().toCharArray().let{
            it.get(it.lastIndex-i)
        }
        var breakTime:Boolean=false
        fileNames.forEach { if(it.toCharArray().let{ it.get(it.lastIndex-i)}
            !=checkSymb)breakTime=true }
        if(breakTime)break
        else secondStaticNegativeOffset=i
    }
    val firstStaticPart:String=sampleName.take(firstStaticOffset)
    val secondStaticPart:String=sampleName.takeLast(secondStaticNegativeOffset)
    return firstStaticPart+"{N}"+secondStaticPart

}

fun assertPathIsReal(stringPath: String) {
    Paths.get(stringPath).let {
        if (!it.exists() || !it.isAbsolute) throw IllegalArgumentException("This path doesn't exist or isn't accessible, repeat path")
    }
}

fun isMultimode(inputPath: Path): Boolean = inputPath.isDirectory()

fun Throwable.displayErrorMessage(necessaryAction:String){
    println("${this.message}, $necessaryAction")
}

fun MutableSet<AbstractProperty<*>>.maxNumOfAdditionalInput():Int{
    var count:Int=0;
    this.filter{it.name.contains(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST)}
        .map { it.name.removePrefix(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST).trim()}
        .forEach { count=maxOf(it.toInt(),count) }
    return count
}
fun MutableSet<AbstractProperty<*>>.getAllAdditionalInputs():List<Path>{
    val result=mutableListOf<Path>()
    this.filter { it.name.contains(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST)}
        .sortedBy { it.name.removePrefix(VARIABLE_ADDITIONAL_INPUT_PATH_PROP_CONST).trim().toInt() }
        .map {it.getTypedValue<Path>()!! }
        .toCollection(result)

    return result
}
/** Time in milliseconds or negative number for infinite search */
 fun findFFMpegBinDirOrThrow(searchRootDir:Path, timeForSearchInMillis:Long=10000):Path{
     val timeStamp=System.currentTimeMillis()
    val ffBinsPattern = Pattern.compile("ffprobe|ffmpeg", Pattern.CASE_INSENSITIVE)
    var result: Path? = null
    if (!searchRootDir.isDirectory()) throw IllegalArgumentException("Pass directory to this method ")
    runCatching {
        Files.walkFileTree(searchRootDir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if(System.currentTimeMillis()-timeStamp>=timeForSearchInMillis&&timeForSearchInMillis>=0) {
                        return FileVisitResult.TERMINATE
                    }
                    val isThereFF = dir!!.listDirectoryEntries("ff*")
                        .filter { !it.isDirectory() && ffBinsPattern.matcher(it.nameWithoutExtension).matches() }
                        .count()==2
                    if (isThereFF) {
                        result = dir
                        return FileVisitResult.TERMINATE
                    } else return FileVisitResult.CONTINUE
                }
            }
        )
    }.onFailure {
        it.printStackTrace()
        throw NoSuchFileException("Unable to access ffmpeg dir in this root")
    }
    return result ?: throw NoSuchFileException("Unable to find ffmpeg dir in this root in specified time period")
}

/** Get pair of paths 1-st FFMPEG 2-nd FFPROBE*/
fun getFFBinsFromDirOrThrow(ffDir:Path):Pair<Path,Path>{
    if (!ffDir.isDirectory()||!ffDir.isAbsolute||!ffDir.exists())throw IllegalArgumentException("Not valid FFDIR")
    var ffMpeg:Path?=null
    var ffProbe:Path?=null
    ffDir.listDirectoryEntries()
        .filter { !it.isDirectory() }
        .forEach {
            when(it.nameWithoutExtension.lowercase()){
                "ffmpeg"->ffMpeg=it
                "ffprobe"->ffProbe=it
            }
        }
    if(ffMpeg!=null||ffProbe!=null){return ffMpeg!! to ffProbe!!}else{
        throw IllegalArgumentException("Not valid FFDIR")
    }


}

fun getJarLocation():Path{
    return Paths.get(NyaContract::class.java.getProtectionDomain().getCodeSource().getLocation().toURI()).parent
}

/** Returns pair <ffMpegPath, ffProbePath> */
fun getFFPathFromPropertiesOrThrow(workDir:Path):Pair<Path,Path>{
   var ffMpeg:Path?=null
    var ffProbe:Path?=null
    runCatching {
       BufferedInputStream(FileInputStream(workDir.resolve(NyaContract.PROP_FILE).toFile())).use {inStream->
           val nyaProps=Properties().apply {
               load(inStream)
           }
           ffMpeg=Paths.get(nyaProps.getProperty(FF_MPEG_PROP))
           ffProbe=Paths.get(nyaProps.getProperty(FF_PROBE_PROP))
       }
   }.getOrElse {IOException("Troubles in reading propfile")  }
    if (ffMpeg!=null||ffProbe!=null){
        return ffMpeg!! to ffProbe!!
    }else {
        throw NoSuchElementException("Cannot load all required props from file")
    }

}

fun writeFFPropertiesToFileOrThrow(workDir: Path, propsToWrite:Pair<Path,Path>){
    check(workDir.isDirectory())
    val properties=Properties().apply {
        setProperty(FF_MPEG_PROP,propsToWrite.first.toString())
        setProperty(FF_PROBE_PROP,propsToWrite.second.toString())
    }
    BufferedOutputStream(FileOutputStream(workDir.resolve(NyaContract.PROP_FILE).toFile())).use { outStream ->
        properties.store(outStream,"NyaMerger properties")
    }
}