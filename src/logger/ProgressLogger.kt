package logger

interface ProgressLogger {

    fun progress(sectorsWritten: Long, additional: String? = null)

    fun error(msg: String, e: Exception? = null)

    fun complete()
}