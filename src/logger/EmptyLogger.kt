package logger

class EmptyLogger(sectorCount: Long): ProgressLogger {

    override fun progress(sectorsWritten: Long, additional: String?) {}

    override fun error(msg: String, e: Exception?){}

    override fun complete(){}

}