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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapterElems
        extends RecyclerView.Adapter< RecyclerViewAdapterElems.ViewHolder >
{
    private final List< DiaryElement > mItems;
    private final List< Boolean >      mSelectionStatuses = new ArrayList<>();

    public RecyclerViewAdapterElems( List< DiaryElement > items,
                                     Listener listener ) {
        mItems = items;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder
    onCreateViewHolder( ViewGroup parent, int viewType ) {
        View view = LayoutInflater.from( parent.getContext() )
                                  .inflate( R.layout.list_item_element, parent, false );
        return new ViewHolder( view, this );
    }

    @Override
    public void
    onBindViewHolder( final ViewHolder holder, int position ) {
        holder.mItem = mItems.get( position );
        mSelectionStatuses.add( position, false );
        holder.mImageView.setImageResource( mItems.get( position ).get_icon() );
        holder.mTextView.setText( mItems.get( position ).get_list_str() );
    }

    @Override
    public int
    getItemCount() {
        return mItems.size();
    }

    void
    setChecked( int position, boolean isChecked ) {
        mSelectionStatuses.set( position, isChecked );
    }

    boolean
    isChecked( int position ) {
        return mSelectionStatuses.get( position );
    }

    interface Listener
    {
        void onElemClick( DiaryElement elem );
    }

    Listener mListener;

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View                mView;
        public final ImageView           mImageView;
        public final TextView            mTextView;
        public DiaryElement              mItem;
        private final RecyclerViewAdapterElems mAdapter;

        public ViewHolder( View view, RecyclerViewAdapterElems adapter ) {
            super( view );
            mView      = view;
            mImageView = view.findViewById( R.id.icon );
            mTextView  = view.findViewById( R.id.title );
            mAdapter   = adapter;

            view.setOnClickListener( v -> adapter.mListener.onElemClick( mItem ) );
            view.setOnLongClickListener( v -> {
                v.setActivated( !v.isActivated() );

                mAdapter.setChecked( getAdapterPosition(), v.isActivated() );
                return true; } );
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }
}
