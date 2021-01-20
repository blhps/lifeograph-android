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


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


class DialogTags extends Dialog
{
    DialogTags( Context context, DialogTagsHost host ) {
        super( context );

        mHost = host;

        mAdapterTags = new TagListAdapter( context,
                                           android.R.layout.simple_list_item_multiple_choice,
                                           android.R.id.text1,
                                           mTags,
                                           getLayoutInflater() );
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.dialog_tags );
        setTitle( R.string.edit_tags );
        setCancelable( true );
        setOnDismissListener( dialog -> invalidateOptionsMenu() );

        ListView listViewTags = findViewById( R.id.listViewTags );
        listViewTags.setAdapter( mAdapterTags );
        listViewTags.setItemsCanFocus( false );

        buttonAdd = findViewById( R.id.buttonAddTag );
        buttonAdd.setOnClickListener( v -> create_tag() );
        buttonAdd.setEnabled( false );

        editText = findViewById( R.id.editTextTag );

        if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            editText.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );

        editText.addTextChangedListener( new TextWatcher() {
            public void afterTextChanged( Editable s ) { }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                mFilterText = s.toString();
                update_list();
                if( s.length() > 0 )
                    buttonAdd.setEnabled( Diary.diary.m_entry_names.get( mFilterText ) == null );
                else
                    buttonAdd.setEnabled( false );
            }
        } );
        editText.setOnEditorActionListener( ( v, actionId, event ) -> {
            if( v.getText().length() > 0 ) {
                create_tag();
                return true;
            }
            return false;
        } );

        update_list();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHost.onDialogTagsClose();
    }

    private void create_tag() {
//        Entry tag = Diary.diary.create_entry( editText.getText().toString(), null );
//        mHost.addTag( tag );
//        editText.setText( "" );
    }

    private void update_list() {
        mAdapterTags.clear();
        for( Entry t : Diary.diary.m_entries.values() ) {
            if( ! mFilterText.isEmpty() )
                if( !t.get_name().contains( mFilterText ) )
                    continue;
            mTags.add( t );
        }
    }

    // VARIABLES
    private EditText editText;
    private String mFilterText = "";
    private Button buttonAdd;
    private java.util.List< Entry > mTags = new ArrayList<>();
    private TagListAdapter mAdapterTags;

    // TAG LIST ADAPTER CLASS ======================================================================
    class TagListAdapter extends ArrayAdapter< Entry > implements View.OnClickListener
    {
        TagListAdapter( Context context,
                        int resource,
                        int textViewResourceId,
                        java.util.List< Entry > objects,
                        LayoutInflater inflater ) {
            super( context, resource, textViewResourceId, objects );
            mInflater = inflater;
        }

        @Override
        public @NonNull View getView( int position, View convertView, @NonNull ViewGroup parent ) {
            ViewHolder holder;
            final Entry tag = getItem( position );

            if( convertView == null ) {
                View view = mInflater.inflate( R.layout.list_item_check, parent, false );
                holder = new ViewHolder( view, DiaryElement.Type.ENTRY );
                view.setTag( holder );
                convertView = view;
            }
            else {
                holder = ( ViewHolder ) convertView.getTag();
            }

            TextView title = holder.getName();
            assert tag != null;
            title.setText( tag.get_list_str() );

            holder.getIcon().setImageResource( tag.get_icon() );

            CheckBox checkBox = holder.getCheckBox();
            checkBox.setChecked( mHost.getTags().contains( tag ) );
            checkBox.setTag( R.id.tag, tag );
            checkBox.setOnClickListener( this );

            title.setTextColor( Color.BLACK );
            title.setBackgroundColor( Color.argb( 0, 0, 0, 0 ) );

            return convertView;
        }

        public void onClick( View view ) {
            switch( view.getId() ) {
                case R.id.checkBox: {
                    CheckBox cb = ( CheckBox ) view;
                    Entry tag = ( Entry ) cb.getTag( R.id.tag );

                    if( cb.isChecked() ) {
                        mHost.addTag( tag );
                    }
                    else {
                        mHost.removeTag( tag );
                    }

                    DialogTags.this.update_list();
                    break;
                }
            }
        }

        private final LayoutInflater mInflater;

        // VIEW HOLDER =========================================================================
        private class ViewHolder
        {
            private View mRow;
            private TextView mTitle = null;
            private ImageView mIcon = null;
            private CheckBox mCheckBox = null;

            private DiaryElement.Type mType;

            ViewHolder( View row, DiaryElement.Type type ) {
                mRow = row;
                mType = type;
            }

            public DiaryElement.Type getType() {
                return mType;
            }

            public TextView getName() {
                if( mTitle == null ) {
                    mTitle = mRow.findViewById( R.id.title );
                }
                return mTitle;
            }

            public ImageView getIcon() {
                if( mIcon == null ) {
                    mIcon = mRow.findViewById( R.id.icon );
                }
                return mIcon;
            }

            CheckBox getCheckBox() {
                if( mCheckBox == null ) {
                    mCheckBox = mRow.findViewById( R.id.checkBox );
                }
                return mCheckBox;
            }
        }
    }

    // INTERFACE WITH THE HOST ACTIVITY ============================================================
    interface DialogTagsHost
    {
        void onDialogTagsClose();
        List< Entry > getTags();
        void addTag( Entry t );
        void removeTag( Entry t );
    }

    private DialogTagsHost mHost;
}
