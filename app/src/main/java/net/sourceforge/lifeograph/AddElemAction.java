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


package net.sourceforge.lifeograph;

import android.content.Context;
import androidx.core.view.ActionProvider;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public class AddElemAction extends ActionProvider implements MenuItem.OnMenuItemClickListener
{
    public AddElemAction( Context context ) {
        super( context );
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public void onPrepareSubMenu( SubMenu menu ) {
        super.onPrepareSubMenu( menu );

        menu.clear();

        menu.add( 0, R.id.add_entry_today, 0, R.string.add_today )
            .setIcon( R.mipmap.ic_entry )
            .setOnMenuItemClickListener( this );

        menu.add( 0, R.id.add_entry_free, 0, R.string.entry_free )
            .setIcon( R.mipmap.ic_entry )
            .setOnMenuItemClickListener( this );

        menu.add( 0, R.id.add_entry_numbered, 0, R.string.entry_numbered )
            .setIcon( R.mipmap.ic_entry )
            .setOnMenuItemClickListener( this );
    }

    //@Override
    public boolean onMenuItemClick( MenuItem item ) {
        int itemId = item.getItemId();
        if( itemId == android.R.id.home ) {
            return true;
        }
        else
        if( itemId == R.id.add_entry_today ) {
            Lifeograph.goToToday();
            return true;
        }
        else
        if( itemId == R.id.add_entry_free ) {
            Lifeograph.addEntry( Diary.d.get_available_order_1st( true ), "" );
            return true;
        }
        else {
        if( itemId == R.id.add_entry_numbered ) {
            Lifeograph.addEntry( Diary.d.get_available_order_1st( false ), "" );
            return true;
        }
        }
        return true;
    }

    @Override
    public View onCreateActionView() {
        return null;
    }

    FragmentEditDiary mParent;
}
