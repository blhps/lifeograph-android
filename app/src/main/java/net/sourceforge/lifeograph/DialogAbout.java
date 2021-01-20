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

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

// ABOUT DIALOG ====================================================================================
public class DialogAbout extends DialogFragment
{
    public DialogAbout() {}

    @Override
    public View
    onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                  Bundle savedInstanceState ) {
        return inflater.inflate(R.layout.dialog_about, container);
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        Objects.requireNonNull( getDialog() ).setTitle( R.string.program_name );
        setCancelable( true );

        TextView tv = view.findViewById( R.id.textViewWebsite );
        tv.setMovementMethod( LinkMovementMethod.getInstance() );

        tv = view.findViewById( R.id.textViewVersion );
        tv.setText( String.format( "%s\n\"%s\"", BuildConfig.VERSION_NAME,
                                   Lifeograph.LIFEOGRAPH_RELEASE_CODENAME ) );
    }
}
