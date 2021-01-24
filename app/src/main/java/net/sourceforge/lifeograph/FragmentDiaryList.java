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
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.sourceforge.lifeograph.helpers.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentDiaryList extends Fragment
        implements RecyclerViewAdapterDiaries.DiaryItemListener,
        DialogInquireText.InquireListener, DialogPassword.Listener
{
    public View
    onCreateView( @NonNull LayoutInflater inflater,
                  ViewGroup container,
                  Bundle savedInstanceState ) {

        return inflater.inflate( R.layout.fragment_list_diary, container, false );
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState ) {
        // Set the adapter
        if( view instanceof RecyclerView ) {
            Context context = view.getContext();
            RecyclerView recyclerView = ( RecyclerView ) view;
            if( mColumnCount <= 1 ) {
                recyclerView.setLayoutManager( new LinearLayoutManager( context ) );
            }
            else {
                recyclerView.setLayoutManager( new GridLayoutManager( context, mColumnCount ) );
            }
            RecyclerViewAdapterDiaries adapter =
                    new RecyclerViewAdapterDiaries( mDiaryItems, this );
            recyclerView.setAdapter( adapter );

            populateDiaries();
        }

    }

    @Override
    public void
    onResume() {
        super.onResume();

        ActionBar actionbar = ( ( AppCompatActivity ) requireActivity() ).getSupportActionBar();
        if( actionbar != null ) {
            actionbar.setSubtitle( "" );
        }

        ( ( FragmentHost ) getActivity() ).updateDrawerMenu( R.id.nav_diaries );
    }

    // DIARY OPERATIONS ============================================================================
    void
    populateDiaries() {
        mDiaryItems.clear();
        mPaths.clear();

        int index = 0;
        File dir = getDiariesDir();
        Log.d( Lifeograph.TAG, dir.getPath() );
        if( !dir.exists() ) {
            if( !dir.mkdirs() )
                Lifeograph.showToast( "Failed to create the diary folder" );
        }
        else {
            File[] dirs = dir.listFiles();
            if( dirs != null ) {
                for( File ff : dirs ) {
                    if( !ff.isDirectory() && !ff.getPath().endsWith( Diary.LOCK_SUFFIX ) ) {
                        mDiaryItems.add( new ListItemDiary( index, ff.getName(), ff.getPath() ) );
                        mPaths.add( ff.getPath() );
                        index++;
                    }
                }
            }
        }

        mDiaryItems.add( new ListItemDiary( index,
                                            Diary.sExampleDiaryName,
                                            Diary.sExampleDiaryPath ) );
        mPaths.add( Diary.sExampleDiaryPath );
    }

    private void
    openDiary1( String path ) {
        m_flag_open_ready = false;

        switch( Diary.diary.set_path( path, Diary.SetPathType.NORMAL ) ) {
            case SUCCESS:
                m_flag_open_ready = true;
                break;
            case FILE_NOT_FOUND:
                Lifeograph.showToast( "File is not found" );
                break;
            case FILE_NOT_READABLE:
                Lifeograph.showToast( "File is not readable" );
                break;
            case FILE_LOCKED:
                Lifeograph.showConfirmationPrompt(
                        getContext(),
                        R.string.continue_from_lock_prompt,
                        R.string.continue_from_lock,
                        ( a, b ) -> openDiary2(),
                        R.string.discard_lock,
                        ( a, b ) -> openDiary3() );
                break;
            default:
                Lifeograph.showToast( "Failed to open the diary" );
                break;
        }

        if( m_flag_open_ready )
            openDiary3();
    }
    private void
    openDiary2() {
        Diary.diary.enableWorkingOnLockfile( true );
        openDiary3();
    }
    private void
    openDiary3() {
        m_flag_open_ready = false;

        switch( Diary.diary.read_header( requireContext().getAssets() ) ) {
            case SUCCESS:
                m_flag_open_ready = true;
                break;
            case INCOMPATIBLE_FILE_OLD:
                Lifeograph.showToast( "Incompatible diary version (TOO OLD)" );
                break;
            case INCOMPATIBLE_FILE_NEW:
                Lifeograph.showToast( "Incompatible diary version (TOO NEW)" );
                break;
            case CORRUPT_FILE:
                Lifeograph.showToast( "Corrupt file" );
                break;
            default:
                Log.e( Lifeograph.TAG, "Unprocessed return value from read_header" );
                break;
        }

        if( !m_flag_open_ready ) return;

        if( Diary.diary.is_encrypted() )
            askPassword();
        else
            readBody();
    }

    private File
    getDiariesDir() {
        if( sExternalStorage.equals( "C" ) ) {
            return new File( sDiaryPath );
        }
        else if( sExternalStorage.equals( "E" ) ) {
            String state = Environment.getExternalStorageState();

            if( Environment.MEDIA_MOUNTED.equals( state ) ) {
                // We can read and write the media
                return new File( Environment.getExternalStorageDirectory(), sDiaryPath );
            }
            else if( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
                // We can only read the media (we may do something else here)
                Lifeograph.showToast( R.string.storage_not_available );
                Log.d( Lifeograph.TAG, "Storage is read-only" );
            }
            else {
                // Something else is wrong. It may be one of many other states, but
                // all we need to know is we can neither read nor write
                Lifeograph.showToast( R.string.storage_not_available );
            }
        }

        return new File( requireContext().getFilesDir(), sDiaryPath );
    }

    void
    createNewDiary() {
        // ask for name
        DialogInquireText dlg = new DialogInquireText( getContext(), R.string.create_diary,
                                                       Lifeograph.getStr( R.string.new_diary ),
                                                       R.string.create, this );
        dlg.show();
    }

    private void
    askPassword() {
        DialogPassword dlg = new DialogPassword( getContext(),
                                                 Diary.diary,
                                                 DialogPassword.DPAction.DPA_LOGIN,
                                                 this );
        dlg.show();
        m_password_attempt_no++;
    }

    private void
    readBody() {
        switch( Diary.diary.read_body() ) {
            case SUCCESS:
//                Intent i = new Intent( getContext(), FragmentEntryList.class );
//                startActivity( i );
                navigateToDiary();
                break;
            case WRONG_PASSWORD:
                Lifeograph.showToast( R.string.wrong_password );
                break;
            case CORRUPT_FILE:
                Lifeograph.showToast( "Corrupt file" );
                break;
            default:
                break;
        }

        m_password_attempt_no = 0;
    }

    private void
    navigateToDiary() {
//        ProductListDirections.NavigateToProductDetail directions =
//                ProductListDirections.navigateToProductDetail( productId );
        Navigation.findNavController( requireView() ).navigate( R.id.nav_entries );
    }


    // INTERFACE METHODS ===========================================================================
    // RecyclerViewAdapterDiaries.DiaryItemListener INTERFACE METHODS
    @Override
    public void
    onDiaryItemClick( int pos ) {
        openDiary1( mPaths.get( pos ) );
        Log.d( Lifeograph.TAG, "Diary clicked" );
    }

    // DialogPassword INTERFACE METHODS
    @Override
    public void
    onDPAction( DialogPassword.DPAction action ) {
        if( action == DialogPassword.DPAction.DPA_LOGIN )
            readBody();
    }

    // InquireListener INTERFACE METHODS
    @Override
    public void
    onInquireAction( int id, String text ) {
        if( id == R.string.create_diary ) {
            if( Diary.diary.init_new( Lifeograph.joinPath( getDiariesDir().getPath(), text ),
                                      "" )
                == Result.SUCCESS ) {
                Intent i = new Intent( getContext(), FragmentEditDiary.class );
                startActivity( i );
            }
            // TODO else inform the user about the problem
        }
    }
    @Override
    public boolean
    onInquireTextChanged( int id, String s ) {
        if( id == R.string.create_diary ) {
            File fp = new File( getDiariesDir().getPath(), s );
            return ( !fp.exists() );
        }
        return true;
    }

    // VARIABLES ===================================================================================
    private int          mColumnCount          = 1;
    public static String sExternalStorage      = "";
    public static String sDiaryPath;
    private boolean      m_flag_open_ready     = false;
    private int          m_password_attempt_no = 0;
    private final List< String >        mPaths      = new ArrayList<>();
    private final List< ListItemDiary > mDiaryItems = new ArrayList<>();

    // SUB CLASSES =================================================================================
    public static class ListItemDiary
    {
        public final int    mId;
        public final String mName;
        public final String mPath;

        public ListItemDiary( int id, String name, String path ) {
            mId   = id;
            mName = name;
            mPath = path;
        }

        @NonNull
        @Override
        public String toString() {
            return mName;
        }
    }
}
