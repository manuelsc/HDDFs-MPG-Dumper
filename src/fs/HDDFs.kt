package fs

import utils.sectorAsPosition

object HDDFs {

    /** MPG Allocation Block Size (MPGs are stored in Blocks) **/
    const val MPG_ALLOC_BLOCK_SECTORS = 512L

    const val MPG_ALLOC_BLOCK_SECTORS_ANALYSE = 8192L

    /** Same as MPG_ALLOC_BLOCK_SECTORS only in bytes instead of sectors.**/
    val MPG_ALLOC_BLOCK_SIZE = MPG_ALLOC_BLOCK_SECTORS.sectorAsPosition

    /** Contains the HDDFs signature that is found in SYSCTR  **/
    val HDDFS_SIGNATURE = byteArrayOf(0x48, 0x44, 0x44, 0x46, 0x73, 0x20, 0x30, 0x30, 0x2E, 0x30, 0x37)

    /** Offset from SYSCTR beginning until signature begins  **/
    const val HDDFS_SIGNATURE_OFFSET = 0x0F

    /** HDD Sector size in bytes **/
    const val HDD_SECTOR_SIZE = 512

    /** MPG Signature **/
    val MPG_SIGNATURE = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0xBA.toByte())

    /** This signature starts after the MPG_SIGNATURE (00 00 01 BA) and signals a new file start **/
    val MPG_NEW_SIGNATURE = byteArrayOf(0x44, 0x00, 0x04, 0x00, 0x04, 0x01)

    /** Empty array, used to check whether we reached end of MPG data **/
    val EMPTY = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /** AV001_0AV001_00.MPG, search for this file in the FB000 Section **/
    val MPG_FILE_NAME = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2E, 0x00, 0x41, 0x56, 0x30,
        0x30, 0x31, 0x5F, 0x30, 0x41, 0x56, 0x30, 0x30, 0x31, 0x5F, 0x30,
        0x30, 0x2E, 0x4D, 0x50, 0x47
    )

    /** AV001_0AV001_00.MPG, offset from the MPG location to its name (name comes after location) **/
    const val MPG_LOCATION_OFFSET_TO_NAME = 0x0F

    /** Specifies the SYSCTR Sector
     * Start sector: 2, length: 2
     * This sector contains the HDDFs Signature **/
    val SYSCTR = 2L.sectorAsPosition to 2L.sectorAsPosition // Pair(start, length)

    /** Specifies the File Map Sector
     * Start sector: 4, length: 3
     * This sectors holds the file map **/
    val FB000 = 4L.sectorAsPosition to 3L.sectorAsPosition // Pair(start, length)

}