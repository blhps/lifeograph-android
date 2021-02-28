/* *********************************************************************************

    Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)

    This file is part of Lifeograph.

    Lifeograph is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lifeograph is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.

 ***********************************************************************************/

package net.sourceforge.lifeograph

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.sourceforge.lifeograph.DialogInquireText.InquireListener
import net.sourceforge.lifeograph.DialogPassword.DPAction
import net.sourceforge.lifeograph.helpers.Result
import java.io.File
import java.util.*

class FragmentListDiaries : Fragment(), RViewAdapterBasic.Listener, InquireListener,
                            DialogPassword.Listener {
    // VARIABLES ===================================================================================
    private val mColumnCount = 1
    private var mFlagOpenReady = false
    private var mPasswordAttemptNo = 0

    //private final List< String >        mPaths      = new ArrayList<>();
    private val mDiaryItems: MutableList<RViewAdapterBasic.Item> = ArrayList()

    // METHODS =====================================================================================
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set the adapter
        val recyclerView: RecyclerView = view.findViewById(R.id.list_diaries)
        val context = view.context
        if(mColumnCount <= 1) {
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
        else {
            recyclerView.layoutManager = GridLayoutManager(context, mColumnCount)
        }
        val adapter = RViewAdapterBasic(
                mDiaryItems, this)
        recyclerView.adapter = adapter
        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_diary)
        fab.setOnClickListener { createNewDiary() }
    }

    override fun onResume() {
        Log.d(Lifeograph.TAG, "FragmentDiaryList.onResume()")
        super.onResume()
        if(Diary.d.is_open) {
            Diary.d.writeAtLogout()
            Diary.d.remove_lock_if_necessary()
            Diary.d.clear()
        }
        val actionbar = (requireActivity() as AppCompatActivity).supportActionBar
        if(actionbar != null) {
            actionbar.subtitle = ""
        }
        (activity as FragmentHost?)!!.updateDrawerMenu(R.id.nav_diaries)
        populateDiaries()
    }

    // DIARY OPERATIONS ============================================================================
    private fun populateDiaries() {
        mDiaryItems.clear()
        val dir = diariesDir
        Log.d(Lifeograph.TAG, dir.path)
        if(!dir.exists()) {
            if(!dir.mkdirs()) Lifeograph.showToast("Failed to create the diary folder")
        }
        else {
            val dirs = dir.listFiles()
            if(dirs != null) {
                for(ff in dirs) {
                    if(!ff.isDirectory && !ff.path.endsWith(Diary.LOCK_SUFFIX)) {
                        mDiaryItems.add(RViewAdapterBasic.Item(ff.name, ff.path,
                                                               R.drawable.ic_diary))
                    }
                }
            }
        }
        mDiaryItems.add(
                RViewAdapterBasic.Item(Diary.sExampleDiaryName, Diary.sExampleDiaryPath,
                                       R.drawable.ic_diary))
    }

    private fun openDiary1(path: String) {
        mFlagOpenReady = false
        when(Diary.d.set_path(path, Diary.SetPathType.NORMAL)) {
            Result.SUCCESS -> mFlagOpenReady = true
            Result.FILE_NOT_FOUND -> Lifeograph.showToast("File is not found")
            Result.FILE_NOT_READABLE -> Lifeograph.showToast("File is not readable")
            Result.FILE_LOCKED -> Lifeograph.showConfirmationPrompt(
                    requireContext(),
                    R.string.continue_from_lock_prompt,
                    R.string.continue_from_lock,
                    { _: DialogInterface?, _: Int -> openDiary2() },
                    R.string.discard_lock
            ) { _: DialogInterface?, _: Int -> openDiary3() }
            else -> Lifeograph.showToast("Failed to open the diary")
        }
        if(mFlagOpenReady) openDiary3()
    }

    private fun openDiary2() {
        Diary.d.enableWorkingOnLockfile(true)
        openDiary3()
    }

    private fun openDiary3() {
        mFlagOpenReady = false
        when(Diary.d.read_header(requireContext().assets)) {
            Result.SUCCESS -> mFlagOpenReady = true
            Result.INCOMPATIBLE_FILE_OLD -> Lifeograph.showToast("Incompatible diary version (TOO OLD)")
            Result.INCOMPATIBLE_FILE_NEW -> Lifeograph.showToast("Incompatible diary version (TOO NEW)")
            Result.CORRUPT_FILE -> Lifeograph.showToast("Corrupt file")
            else -> Log.e(Lifeograph.TAG, "Unprocessed return value from read_header")
        }
        if(!mFlagOpenReady) return
        if(Diary.d.is_encrypted) askPassword() else readBody()
    }

    private val diariesDir: File
        get() {
            if(sStoragePref == "C") {
                return File(sDiaryPath)
            }
            else if(sStoragePref == "E") {
//                when(Environment.getExternalStorageState()) {
//                    Environment.MEDIA_MOUNTED -> {
//                        // We can read and write the media
//                        return File(context?.getExternalFilesDir(null), sDiaryPath)
//                    }
//                    Environment.MEDIA_MOUNTED_READ_ONLY -> {
//                        // We can only read the media (we may do something else here)
//                        Lifeograph.showToast(R.string.storage_not_available)
//                        Log.d(Lifeograph.TAG, "Storage is read-only")
//                    }
//                    else -> {
//                        // Something else is wrong. It may be one of many other states, but
//                        // all we need to know is we can neither read nor write
//                        Lifeograph.showToast(R.string.storage_not_available)
//                    }
//                }
                Lifeograph.showToast(
                        "External Storage Option is removed. Please use the custom path picker!")
            }
            return File(requireContext().filesDir, sDiaryPath)
        }

    private fun createNewDiary() {
        // ask for name
        DialogInquireText(requireContext(),
                          R.string.create_diary,
                          Lifeograph.getStr(R.string.new_diary),
                          R.string.create,
                          this).show()
    }

    private fun askPassword() {
        DialogPassword(requireContext(), Diary.d, DPAction.DPA_LOGIN, this).show()
        mPasswordAttemptNo++
    }

    private fun readBody() {
        when(Diary.d.read_body()) {
            Result.SUCCESS -> navigateToDiary()
            Result.WRONG_PASSWORD -> Lifeograph.showToast(R.string.wrong_password)
            Result.CORRUPT_FILE -> Lifeograph.showToast("Corrupt file")
            else -> {
            }
        }
        mPasswordAttemptNo = 0
    }

    private fun navigateToDiary() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_entries)
    }

    // INTERFACE METHODS ===========================================================================
    // RecyclerViewAdapterDiaries.DiaryItemListener INTERFACE METHODS
    override fun onItemClick(item: RViewAdapterBasic.Item) {
        openDiary1(item.mId)
        Log.d(Lifeograph.TAG, "Diary clicked")
    }

    // DialogPassword INTERFACE METHODS
    override fun onDPAction(action: DPAction) {
        if(action === DPAction.DPA_LOGIN) readBody()
    }

    // InquireListener INTERFACE METHODS
    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.create_diary) {
            if(Diary.d.init_new(Lifeograph.joinPath(diariesDir.path, text), "")
                    == Result.SUCCESS) {
                navigateToDiary()
            }
            // TODO else inform the user about the problem
        }
    }

    override fun onInquireTextChanged(id: Int, text: String): Boolean {
        if(id == R.string.create_diary) {
            val fp = File(diariesDir.path, text)
            return !fp.exists()
        }
        return true
    }

    companion object {
        @JvmField
        var sStoragePref = ""
        @JvmField
        var sDiaryPath = ""
    }
}
