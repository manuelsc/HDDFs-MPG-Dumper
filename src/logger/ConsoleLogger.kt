package logger

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle

class ConsoleLogger(sectorCount: Long): ProgressLogger {

    private val pb = ProgressBar("Extracting MPGs", sectorCount, ProgressBarStyle.ASCII)

    override fun progress(sectorsWritten: Long, additional: String?) {
        pb.stepTo(sectorsWritten)
        if(additional != null) pb.extraMessage = additional
    }

    override fun error(msg: String, e: Exception?){
        closeProgress()
        println("Something went wrong\n")
        e?.printStackTrace()
    }

    override fun complete(){
        closeProgress()
        println("Done!")
    }

    private fun closeProgress(){
        try { pb.close() } catch (e: Exception) {}
    }
}