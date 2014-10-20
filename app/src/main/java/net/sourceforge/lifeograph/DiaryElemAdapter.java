/***********************************************************************************

 Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

class DiaryElemAdapter extends ArrayAdapter< DiaryElement >
{
    public DiaryElemAdapter( Context context,
                             int resource,
                             int textViewResourceId,
                             java.util.List< DiaryElement > objects,
                             LayoutInflater inflater,
                             ListOperations listOperations ) {
        super( context, resource, textViewResourceId, objects );
        mInflater = inflater;
        mListOperations = listOperations;
    }

    private ViewHolder setHolder( DiaryElement elem, ViewGroup par ) {
        View view;
        ViewHolder holder;

        switch( elem.get_type() ) {
            case TAG_CTG:
                view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_TAG_CTG );
                break;
            case CHAPTER_CTG:
                view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_CHAPTER_CTG );
                break;
            case HEADER:
                view = mInflater.inflate( R.layout.list_section_simple, par, false );
                holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_SIMPLE );
                break;
            default:
                view = mInflater.inflate( R.layout.list_item_element, par, false );
                holder = new ViewHolder( view, DiaryElement.LayoutType.ELEMENT );
                break;
        }

        view.setTag( holder );
        return holder;
    }

    public void handleCollapse( DiaryElement elem ) {
        Log.d( Lifeograph.TAG, "handle collapse " + elem.get_name() );
        switch( elem.get_type().layout_type ) {
            case HEADER_TAG_CTG:
                Tag.Category tc = ( Tag.Category ) elem;
                tc.set_expanded( !tc.get_expanded() );
                break;
            case HEADER_CHAPTER_CTG:
                Chapter.Category cc = ( Chapter.Category ) elem;
                Diary.diary.set_current_chapter_ctg( cc );
                break;
        }

        mListOperations.updateList();
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        ViewHolder holder;
        TextView title;
        final DiaryElement elem = getItem( position );

        if( convertView == null ) {
            holder = setHolder( elem, parent );
            convertView = holder.getView();
        }
        else {
            holder = ( ViewHolder ) convertView.getTag();
        }

        if( holder.getLayoutType() != elem.get_type().layout_type ) {
            holder = setHolder( elem, parent );
            convertView = holder.getView();
        }

        title = holder.getName();
        title.setText( elem.get_list_str() );

        switch( holder.getLayoutType() ) {
            case HEADER_SIMPLE:
                break;
            case HEADER_TAG_CTG: {
                Tag.Category tc = ( Tag.Category ) elem;
                ImageButton iconCollapse = holder.getIconCollapse();
                iconCollapse.setImageResource(
                        tc.get_expanded() ? R.drawable.ic_expanded : R.drawable.ic_collapsed );
                iconCollapse.setOnClickListener( new View.OnClickListener()
                {
                    public void onClick( View v ) {
                        handleCollapse( elem );
                    }
                } );
                break;
            }
            case HEADER_CHAPTER_CTG: {
                Chapter.Category cc = ( Chapter.Category ) elem;
                ImageButton iconCollapse = holder.getIconCollapse();
                iconCollapse.setImageResource( cc == Diary.diary.m_ptr2chapter_ctg_cur ?
                                                       R.drawable.ic_radio_sel : R.drawable.ic_radio_empty );
                iconCollapse.setOnClickListener( new View.OnClickListener()
                {
                    public void onClick( View v ) {
                        handleCollapse( elem );
                    }
                } );
                break;
            }
            case ELEMENT: {
                TextView detail = holder.getDetail();
                detail.setText( elem.getListStrSecondary() );

                ImageView icon = holder.getIcon();
                icon.setImageResource( elem.get_icon() );

                ImageView icon2 = holder.getIcon2();
                icon2.setImageResource( R.drawable.ic_favorite );
                icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );
                break;
            }
            default:
                break;
        }

        return convertView;
    }

    private LayoutInflater mInflater;
    private ListOperations mListOperations;

    // INTERFACE WITH THE LIST IN THE RELATED FRAGMENT
    public interface ListOperations
    {
        public void updateList();
    }

    // VIEW HOLDER =================================================================================
    private class ViewHolder
    {
        private View mRow;
        private TextView mTitle = null;
        private TextView mDetail = null;
        private ImageView mIcon = null;
        private ImageView mIcon2 = null;

        private ImageButton mIconCollapse = null;

        private DiaryElement.LayoutType mLayoutType;

        public ViewHolder( View row, DiaryElement.LayoutType layoutType ) {
            mRow = row;
            mLayoutType = layoutType;
        }

        public DiaryElement.LayoutType getLayoutType() {
            return mLayoutType;
        }

        public View getView() {
            return mRow;
        }

        public TextView getName() {
            if( null == mTitle ) {
                mTitle = ( TextView ) mRow.findViewById( R.id.title );
            }
            return mTitle;
        }

        public TextView getDetail() {
            if( null == mDetail ) {
                mDetail = ( TextView ) mRow.findViewById( R.id.detail );
            }
            return mDetail;
        }

        public ImageView getIcon() {
            if( null == mIcon ) {
                mIcon = ( ImageView ) mRow.findViewById( R.id.icon );
            }
            return mIcon;
        }

        public ImageView getIcon2() {
            if( null == mIcon2 ) {
                mIcon2 = ( ImageView ) mRow.findViewById( R.id.icon2 );
            }
            return mIcon2;
        }

        public ImageButton getIconCollapse() {
            if( null == mIconCollapse ) {
                mIconCollapse = ( ImageButton ) mRow.findViewById( R.id.icon_collapse );
            }
            return mIconCollapse;
        }
    }
}
