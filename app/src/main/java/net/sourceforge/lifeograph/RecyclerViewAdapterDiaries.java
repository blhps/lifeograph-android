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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class RecyclerViewAdapterDiaries
        extends RecyclerView.Adapter< RecyclerViewAdapterDiaries.ViewHolder >
{

    private final List< FragmentListDiaries.ListItemDiary > mItems;
    public ViewHolder mViewHolder;

    public RecyclerViewAdapterDiaries( List< FragmentListDiaries.ListItemDiary > items,
                                       DiaryItemListener listener ) {
        mItems = items;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder
    onCreateViewHolder( ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from( parent.getContext() )
                                  .inflate( R.layout.list_item_diary, parent, false );
        mViewHolder = new ViewHolder( view, mListener );

        return mViewHolder;
    }

    @Override
    public void
    onBindViewHolder( final ViewHolder holder, int position ) {
        holder.mItem = mItems.get( position );
        //holder.mImageView.setText( mValues.get( position ).id );
        holder.mTextView.setText( mItems.get( position ).getMName() );
    }

    @Override
    public int
    getItemCount() {
        return mItems.size();
    }

    interface DiaryItemListener
    {
        void onDiaryItemClick( String path );
    }

    DiaryItemListener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View mView;
        public final ImageView mImageView;
        public final TextView mTextView;
        public FragmentListDiaries.ListItemDiary mItem;

        public ViewHolder( View view, DiaryItemListener listener ) {
            super( view );
            mView      = view;
            mImageView = view.findViewById( R.id.icon );
            mTextView  = view.findViewById( R.id.title );

            view.setOnClickListener( v -> listener.onDiaryItemClick( mItem.getMPath() ) );
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }
}
