package utils

import fs.HDDFs

/**
 * Convert given sector to an absolute position
 */
val Long.sectorAsPosition
    get() = this * HDDFs.HDD_SECTOR_SIZE

/**
 * Converts mpg blocknumber to size in bytes
 */
val Long.blocksToMpgSize
    get() = (this * HDDFs.MPG_ALLOC_BLOCK_SIZE)

/**
 * Converts ByteArray of size 4 to an unsigned int
 */
fun ByteArray.toUInt32LittleEndian(): UInt {
    return ((this[0].toUInt() and 0xFFu)) or
            ((this[1].toUInt() and 0xFFu) shl 8) or
            ((this[2].toUInt() and 0xFFu) shl 16) or
            (this[3].toUInt() and 0xFFu shl 24)
}

/**
 * Converts ByteArray of size 4 to an unsigned Int (special case for after MPG signature)
 * Used only for comparing if higher
 */
fun ByteArray.toUInt32BigEndian(): UInt {
    return ((this[0].toUInt() and 0xFFu shl 24)) or
            ((this[1].toUInt() and 0xFFu) shl 16) or
            ((this[2].toUInt() and 0xFFu) shl 8) or
            (this[3].toUInt() and 0xFFu)
}

/**
 * Make big numbers more readable
 */
fun ULong.format(): String {
    return String.format("%,d", this.toLong())
}