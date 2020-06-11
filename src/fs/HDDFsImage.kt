package fs

import logger.ProgressLogger
import utils.sectorAsPosition
import utils.toUInt32LittleEndian

@Suppress("MemberVisibilityCanBePrivate")
class HDDFsImage(pathToFile: String, private val debug: Boolean) : FileSeeker(pathToFile) {

    fun getSignatureSector() = readBytes(HDDFs.SYSCTR.first, HDDFs.SYSCTR.second.toInt())

    fun getFileMap() = readBytes(HDDFs.FB000.first, HDDFs.FB000.second.toInt())

    /**
     * Checks the SYSCTR Sector of the HDDFs image and validates it's signature
     * @return true if we found a valid HDDFs file system, otherwise false
     */
    fun isValidHDDFs(): Boolean {
        val signatureSector = getSignatureSector()
        for (i in 0 until HDDFs.HDDFS_SIGNATURE.size) {
            if (signatureSector[HDDFs.HDDFS_SIGNATURE_OFFSET + 1 + i] != HDDFs.HDDFS_SIGNATURE[i]) return false
        }
        return true
    }

    /**
     * Tries to locate the MPG file name in the FB000 Sector and returns the position
     * of the MPG Sector.
     * @return Pair containing the starting position (first) and length (second) of the MPG Sector
     */
    fun findMpgSectionPosition(): Pair<UInt, UInt> {
        val fileMap = getFileMap()
        val rawRelativePosition =
            findAndReturnPosition(fileMap, HDDFs.MPG_FILE_NAME)
        check(rawRelativePosition != -1) { "Could not locate MPG position" }

        val absolutePosition = rawRelativePosition - HDDFs.MPG_LOCATION_OFFSET_TO_NAME + HDDFs.FB000.first - 1
        val mpgStart = readBytes(absolutePosition, 4).toUInt32LittleEndian()
        val mpgLength = readBytes(absolutePosition + 4, 4).toUInt32LittleEndian()

        return Pair(mpgStart, mpgLength)
    }

    /**
     * Skipping through MPG_ALLOC_BLOCK_SECTORS_ANALYSE and see if we read empty (zero) bytes
     * @param position sector position + length of MPG sector (can be retrieved by calling [findMpgSectionPosition])
     * @return last block number with non empty data (approximation for speed)
     */
    fun getMpgBlockCount(position: Pair<UInt, UInt>): Long {
        for (i in 0 until position.second.toInt() / HDDFs.MPG_ALLOC_BLOCK_SECTORS_ANALYSE) {
            val absolutePosition = (position.first.toLong() + (i * HDDFs.MPG_ALLOC_BLOCK_SECTORS_ANALYSE)).sectorAsPosition
            val chunk = readBytes(absolutePosition, HDDFs.EMPTY.size)
            if (HDDFs.EMPTY.findIn(chunk, 0)) return (i - 1L) * (HDDFs.MPG_ALLOC_BLOCK_SECTORS_ANALYSE / HDDFs.MPG_ALLOC_BLOCK_SECTORS)
        }
        return position.second.toLong() / HDDFs.MPG_ALLOC_BLOCK_SECTORS
    }

    /**
     * Extracts all mpgs found in the MPG Sector
     * @param outPath Directory path on where to store output
     * @param position sector position + length of MPG sector (can be retrieved with [findMpgSectionPosition])
     * @param logger used only for progress bar, can be retrieved by calling [getMpgBlockCount]
     */
    fun extractAllMpgs(outPath: String, position: Pair<UInt, UInt>, logger: ProgressLogger, allowSequenceJumps: Boolean, recover: Boolean) {
        MpgScratcher(this, position, logger, allowSequenceJumps, recover, debug).extractTo(outPath)
    }

    companion object {
        fun findAndReturnPosition(byteArray: ByteArray, lookFor: ByteArray): Int {
            for (i in 0 until byteArray.size) {
                if (lookFor.findIn(byteArray, i)) {
                    return i
                }
            }
            return -1
        }

        /**
         * Pass byte array you are looking for as this and use
         * @param bytes to pass source array and
         * @param position at its current position
         * @return true if this was found in bytes at position, false if not
         */
        fun ByteArray.findIn(bytes: ByteArray, position: Int): Boolean {
            this.forEachIndexed { index, value ->
                if (index + position >= bytes.size || value != bytes[position + index]) return false
            }
            return true
        }
    }

}