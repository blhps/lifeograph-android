/* **************************************************************************************************

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

 **************************************************************************************************/

package net.sourceforge.lifeograph

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class FragmentChart : Fragment()
{
    // VARIABLES ===================================================================================
    companion object {
        lateinit var mChartElem: ChartElem
    }
    private val mMenu: Menu? = null
    private var mChartWidget: ViewChart? = null

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstState: Bundle?): View? {
        Log.d(Lifeograph.TAG, "FragmentEntryList.onCreateView()")
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mChartWidget = view.findViewById(R.id.chart_widget)

        mChartWidget!!.m_data = ChartData(Diary.d)
        mChartWidget!!.m_data.set_from_string(mChartElem._definition)

        mChartWidget?.calculate_points( 1.0 )
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        Log.d(Lifeograph.TAG, "FragmentEntryList.onResume()")
        super.onResume()
        //ActivityMain.mViewCurrent = this
    }
}
