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

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.HorizontalScrollView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.sourceforge.lifeograph.DialogPassword.DPAction
import net.sourceforge.lifeograph.helpers.FileUtil
import net.sourceforge.lifeograph.helpers.Result
import java.io.File
import java.util.*

class FragmentListDiaries : Fragment(), RViewAdapterBasic.Listener, DialogInquireText.Listener,
                            DialogPassword.Listener {
    // VARIABLES ===================================================================================
    private val mColumnCount = 1
    private var mPasswordAttemptNo = 0

    private val mDiaryUris: MutableList<String> = ArrayList()
    private val mDiaryItems: MutableList<RViewAdapterBasic.Item> = ArrayList()
    private val mSelectionStatuses: MutableList<Boolean> = ArrayList()

    private lateinit var mRVList: RecyclerView
    private lateinit var mAdapter: RViewAdapterBasic
    private lateinit var mToolbar: HorizontalScrollView

    // METHODS =====================================================================================
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list_diaries, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set the adapter
        mRVList = view.findViewById(R.id.list_diaries)
        val context = view.context
        if(mColumnCount <= 1) {
            mRVList.layoutManager = LinearLayoutManager(context)
        }
        else {
            mRVList.layoutManager = GridLayoutManager(context, mColumnCount)
        }
        mAdapter = RViewAdapterBasic(mDiaryItems, mSelectionStatuses, this)
        mRVList.adapter = mAdapter
        val fabNew: FloatingActionButton = view.findViewById(R.id.fab_add_new_diary)
        val fabExisting: FloatingActionButton = view.findViewById(R.id.fab_add_existing_diary)
        fabNew.setOnClickListener { createNewDiary() }
        fabExisting.setOnClickListener { openFile() }

        if(Lifeograph.mActivityMain.mStartUpPath != null) {
            Log.d(Lifeograph.TAG, Lifeograph.mActivityMain.mStartUpPath!!.path!!)
            val file = File(Lifeograph.mActivityMain.mStartUpPath!!.path!!)
            if(file.exists())
                openDiary1(file.path)
            else
                Lifeograph.showToast("File not found!")
        }
        mToolbar = view.findViewById(R.id.toolbar_elem)

        view.findViewById<Button>(R.id.remove).setOnClickListener { removeSelectedDiaries() }
        view.findViewById<Button>(R.id.cancel).setOnClickListener { cancelSelectionMode() }
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

        mToolbar.visibility = View.GONE

        (activity as FragmentHost?)!!.updateDrawerMenu(R.id.nav_diaries)
        readDiaryList()
        populateDiaries()
    }

    override fun onDestroyView() {
        if(mAdapter.hasSelection())
            mAdapter.clearSelection(mRVList.layoutManager!!)
        super.onDestroyView()
    }

//    override fun onStop() {
//        super.onStop()
//        Log.d(Lifeograph.TAG, "FragmentListDiaries.onStop()")
//        writeDiaryList()
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if(requestCode == 123 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the directory that the user selected
            val uri = resultData?.data!!
            val name = FileUtil.getFileName(uri, requireContext())
            Log.d(Lifeograph.TAG, "Name: $name")

            // ensure that the persmission is persistent
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

            if(mDiaryUris.contains(uri.toString()))
                Lifeograph.showToast("File is already in the list")
            else {
                mDiaryUris.add(uri.toString())
                writeDiaryList()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        startActivityForResult(intent, 123)
    }

    // DIARY OPERATIONS ============================================================================
    private fun populateDiaries() {
        mDiaryItems.clear()

        for(uriStr in mDiaryUris) {
            val uri = Uri.parse(uriStr)
            mDiaryItems.add(RViewAdapterBasic.Item(FileUtil.getFileName(uri, requireContext()),
                                                   uriStr,
                                                   R.drawable.ic_diary))
        }

        mDiaryItems.add(RViewAdapterBasic.Item(Diary.sExampleDiaryName,
                                               Diary.sExampleDiaryPath,
                                               R.drawable.ic_diary))

        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(mDiaryItems.size, false))

        mRVList.adapter?.notifyDataSetChanged()
    }

    private fun removeSelectedDiaries() {
        val list = mDiaryUris.toList()
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                if(i < list.size) {
                    mDiaryUris.remove(list[i])
                }
            }
        }

        mAdapter.clearSelection(mRVList.layoutManager!!)
        writeDiaryList()
        exitSelectionMode()
        populateDiaries()
    }

    fun cancelSelectionMode() {
        if(mAdapter.hasSelection()) {
            mAdapter.clearSelection(mRVList.layoutManager!!)
            exitSelectionMode()
        }
    }

    private fun readDiaryList() {
        mDiaryUris.clear()

        if(!File(requireContext().filesDir, "diaries.lst").exists()) {
            return
        }

        File(requireContext().filesDir, "diaries.lst").forEachLine {
            if(it.isNotEmpty()) {
                mDiaryUris.add(it)
            } }
    }

    private fun writeDiaryList() {
        var fileContent = String()
        for(uriStr in mDiaryUris) {
            fileContent += (uriStr + '\n')
        }

        File(requireContext().filesDir, "diaries.lst").bufferedWriter().use {
                out -> out.write(fileContent)
            }
    }

    private fun openDiary1(path: String) {
        when(Diary.d.set_path(requireContext(), path, Diary.SetPathType.NORMAL)) {
            Result.SUCCESS -> openDiary3()
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
    }

    private fun openDiary2() {
        Diary.d.enableWorkingOnLockfile(true)
        openDiary3()
    }

    private fun openDiary3() {
        when(Diary.d.read_header(requireContext())) {
            Result.SUCCESS -> if(Diary.d.is_encrypted) askPassword() else readBody()
            Result.INCOMPATIBLE_FILE_OLD -> Lifeograph.showToast("Incompatible diary version (TOO OLD)")
            Result.INCOMPATIBLE_FILE_NEW -> Lifeograph.showToast("Incompatible diary version (TOO NEW)")
            Result.CORRUPT_FILE -> Lifeograph.showToast("Corrupt file")
            else -> Log.e(Lifeograph.TAG, "Unprocessed return value from read_header")
        }
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
    // RViewAdapterBasic.Listener INTERFACE METHODS
    override fun onItemClick(item: RViewAdapterBasic.Item) {
        openDiary1(item.mId)
        Log.d(Lifeograph.TAG, "Diary clicked")
    }

    override fun enterSelectionMode(): Boolean {
        mToolbar.visibility = View.VISIBLE
        return true
    }

    override fun exitSelectionMode() {
        updateActionBarSubtitle()
        mToolbar.visibility = View.GONE
    }

    override fun updateActionBarSubtitle() {
        Lifeograph.getActionBar().subtitle =
                if( mAdapter.mSelCount > 0 ) "Selected ${mAdapter.mSelCount}/${mAdapter.itemCount}"
                else                         ""
    }

    // DialogPassword INTERFACE METHODS
    override fun onDPAction(action: DPAction) {
        if(action === DPAction.DPA_LOGIN) readBody()
    }

    // InquireListener INTERFACE METHODS
    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.create_diary) {
            if(Diary.d.init_new(requireContext(), Lifeograph.joinPath(diariesDir.path, text), "")
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
