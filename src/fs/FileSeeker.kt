package fs

import java.io.RandomAccessFile

open class FileSeeker(pathToFile: String){

    private val raf = RandomAccessFile(pathToFile, "r")

    fun readBytes(start: Long, length: Int): ByteArray {
        raf.seek(start)
        val content = ByteArray(length)
        raf.readFully(content)
        return content
    }
}