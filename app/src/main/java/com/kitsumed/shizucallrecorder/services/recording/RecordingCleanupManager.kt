/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.kitsumed.shizucallrecorder.services.recording

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.kitsumed.shizucallrecorder.data.AppPreferences
import com.kitsumed.shizucallrecorder.system.storage.SafHelper
import com.kitsumed.shizucallrecorder.utils.AppLogger

/**
 * Deletes recordings older than the configured retention period from the user-selected recording folder.
 */
object RecordingCleanupManager {

    // Only files that carry the app's direction tag ("_in" / "_out") and an audio extension
    // used by this app are considered, so unrelated files in the folder are never touched.
    // The "_" after the tag is optional: anonymous calls produce names like "..._in.ogg".
    private val RECORDING_FILE_REGEX = Regex("^.*_(in|out)(_.*)?\\.(ogg|m4a|aac)$", RegexOption.IGNORE_CASE)

    /**
     * Removes recordings whose last-modified date is older than [days] from the selected folder.
     *
     * @param context App context used to resolve the recording folder URI.
     * @param days Retention period in days. Values <= 0 disable cleanup.
     * @return The number of deleted files.
     */
    fun cleanupOldRecordings(context: Context, days: Int): Int {
        if (days <= 0) return 0

        val folderUri = AppPreferences(context).getRecordingFolderUri()
        if (!SafHelper.isFolderValid(context, folderUri)) {
            AppLogger.w("Auto-delete skipped: recording folder is missing or not writable")
            return 0
        }

        val rootDir = DocumentFile.fromTreeUri(context, folderUri) ?: return 0
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        var deletedCount = 0

        for (file in rootDir.listFiles()) {
            if (!file.isFile) continue
            val fileName = file.name ?: continue
            if (!RECORDING_FILE_REGEX.matches(fileName)) continue
            val mtime = file.lastModified()
            if (mtime == 0L || mtime >= cutoff) continue

            if (file.delete()) {
                deletedCount++
                AppLogger.i("Auto-delete removed old recording: $fileName")
            } else {
                AppLogger.w("Auto-delete failed to remove: $fileName")
            }
        }

        AppLogger.i("Auto-delete finished: removed $deletedCount recording(s) older than $days day(s)")
        return deletedCount
    }
}