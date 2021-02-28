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

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import net.sourceforge.lifeograph.Lifeograph.DiaryEditor

class ActivityMain : AppCompatActivity(), FragmentHost {
    // VARIABLES ===================================================================================
    private var mNavController: NavController? = null
    private var mAppBarConfiguration: AppBarConfiguration? = null

    var mActionBar: ActionBar? = null

    companion object {
        var mViewCurrent: DiaryEditor? = null
    }

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lifeograph.mActivityMain = this
        Lifeograph.updateScreenSizes(this)
        if(Diary.d == null) Diary.d = Diary()

        // PREFERENCES
        PreferenceManager.setDefaultValues(applicationContext, R.xml.pref_general, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(
                applicationContext)
        FragmentListDiaries.sStoragePref = prefs.getString(
                Lifeograph.getStr(R.string.pref_DIARY_STORAGE_key), "N/A")!!
        FragmentListDiaries.sDiaryPath = prefs.getString(
                Lifeograph.getStr(R.string.pref_DIARY_PATH_key), "N/A")!!
        Date.s_format_order = prefs.getString(
                Lifeograph.getStr(R.string.pref_DATE_FORMAT_ORDER_key), "N/A")
        Date.s_format_separator = prefs.getString(
                Lifeograph.getStr(R.string.pref_DATE_FORMAT_SEPARATOR_key), ".")!![0]
        Lifeograph.sOptImperialUnits = prefs.getString(
                Lifeograph.getStr(R.string.pref_UNIT_TYPE_key), "M") == "I"

        // CONTENT
        setContentView(R.layout.home)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        mActionBar = supportActionBar
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        mAppBarConfiguration = AppBarConfiguration.Builder(
                R.id.nav_diaries, R.id.nav_settings, R.id.nav_about)
                .setOpenableLayout(drawerLayout)
                .build()
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment?)!!
        mNavController = navHostFragment.navController

//        AppBarConfiguration appBarConfiguration =
//                new AppBarConfiguration.Builder( navController.getGraph() )
//                        .setOpenableLayout( drawerLayout )
//                        .build();


//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // Show and Manage the Drawer and Back Icon
        NavigationUI.setupActionBarWithNavController(this, mNavController!!,
                mAppBarConfiguration!!)

        // Handle Navigation item clicks
        // This works with no further action on your part if the menu and destination id’s match.
        NavigationUI.setupWithNavController(navigationView, mNavController!!)
    }

    override fun onResume() {
        super.onResume()
        Log.d(Lifeograph.TAG, "ActivityLogin.onResume()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Lifeograph.TAG, "ActivityLogin.onDestroy()")
    }

    //    @Override
    //    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
    //    }
    //    @Override
    //    public void onBackPressed() {
    //        Log.d( Lifeograph.TAG, "BACK PRESSED!!!!" );
    //        if( mViewCurrent == null || !mViewCurrent.handleBack() )
    //            super.onBackPressed();
    //    }
    override fun onSupportNavigateUp(): Boolean {
        Log.d(Lifeograph.TAG, "NAVIGATE UP!!!!")
        return ((mViewCurrent != null && mViewCurrent!!.handleBack())
                || NavigationUI.navigateUp(mNavController!!, mAppBarConfiguration!!)
                || super.onSupportNavigateUp())
    }

    override fun updateDrawerMenu(id: Int) {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val itemEntries = navigationView.menu.findItem(R.id.nav_entries)
        val itemCharts = navigationView.menu.findItem(R.id.nav_charts)
        val itemFilters = navigationView.menu.findItem(R.id.nav_filters)
        val itemThemes = navigationView.menu.findItem(R.id.nav_themes)
        if(id == R.id.nav_diaries) {
            itemEntries.isVisible = false
            itemCharts.isVisible = false
            itemFilters.isVisible = false
            itemThemes.isVisible = false
        }
        else {
            itemEntries.isVisible = true
            itemCharts.isVisible = true
            itemFilters.isVisible = true
            itemThemes.isVisible = true
        }
    }

    fun showElem(elem: DiaryElement) {
        when(elem._type) {
            DiaryElement.Type.ENTRY, DiaryElement.Type.CHAPTER -> {
                FragmentEntry.mEntry = elem as Entry
                if(mViewCurrent is FragmentEntry)
                    (mViewCurrent as FragmentEntry).show(true)
                else
                    mNavController!!.navigate(R.id.nav_entry_editor)
            }
            DiaryElement.Type.THEME -> {
                FragmentTheme.mTheme = elem as Theme
                mNavController!!.navigate(R.id.nav_theme_editor)
            }
            DiaryElement.Type.FILTER -> {
                FragmentFilter.mFilter = elem as Filter
                mNavController!!.navigate(R.id.nav_filter_editor)
            }
            DiaryElement.Type.CHART -> {
                FragmentChart.mChartElem = (elem as ChartElem)
                mNavController!!.navigate(R.id.nav_chart_editor)
            }
            else -> {

            }
        }
    }
}
