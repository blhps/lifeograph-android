/***********************************************************************************

 Copyright (C) 2012-2016 Ahmet Öztürk (aoz_2@yahoo.com)

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


package net.sourceforge.lifeograph;


public abstract class DiaryElementChart extends DiaryElement
{
    public final static int DEFAULT_CHART_TYPE = ChartPoints.MONTHLY | ChartPoints.BOOLEAN;

    DiaryElementChart( Diary diary, String name, int status ) {
        super( diary, name, status );
    }
    DiaryElementChart( Diary diary, int id, int status ) {
        super( diary, id, status );
    }
    DiaryElementChart( Diary diary, String name, int status, int type ) {
        super( diary, name, status );
        m_chart_type = type;
    }

    int get_chart_type() {
        return m_chart_type;
    }

    void set_chart_type( int type ) {
        if( ( type & ChartPoints.PERIOD_MASK ) == 0 )
            type |= ( m_chart_type & ChartPoints.PERIOD_MASK );
        if( ( type & ChartPoints.VALUE_TYPE_MASK ) == 0 )
            type |= ( m_chart_type & ChartPoints.VALUE_TYPE_MASK );
        m_chart_type = type;
    }

    ChartPoints create_chart_data() {
        return null;
    }

    // thanks to multiple inheritance, the initialization is done in constructor in C++:
    int m_chart_type = DEFAULT_CHART_TYPE;
}
