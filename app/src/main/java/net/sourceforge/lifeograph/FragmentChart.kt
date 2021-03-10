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

import android.os.Bundle
import android.view.*

class FragmentChart : FragmentDiaryEditor(), DialogInquireText.Listener
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_chart
    override val mMenuId: Int   = R.menu.menu_chart

    private lateinit var mChartWidget: ViewChart

    companion object {
        lateinit var mChartElem: ChartElem
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this

        mChartWidget = view.findViewById(R.id.chart_widget)

        mChartWidget.m_data = ChartData(Diary.d)
        mChartWidget.m_data.set_from_string(mChartElem._definition)

        mChartWidget.calculate_points( 1.0 )
    }

    override fun onResume() {
        super.onResume()

        Lifeograph.getActionBar().subtitle = mChartElem._title_str
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.rename -> {
                DialogInquireText(requireContext(),
                                  R.string.rename,
                                  mChartElem.m_name,
                                  R.string.apply,
                                  this).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val flagWritable = Diary.d.is_in_edit_mode
        mMenu.findItem(R.id.rename).isVisible = flagWritable
    }

    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.rename) {
            Diary.d.rename_chart( mChartElem.m_name, text)
            Lifeograph.getActionBar().subtitle = mChartElem._title_str
        }
    }
}
