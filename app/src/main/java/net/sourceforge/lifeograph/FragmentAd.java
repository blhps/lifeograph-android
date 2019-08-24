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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class FragmentAd extends Fragment
{
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        return inflater.inflate( R.layout.fragment_ad, container, false );
    }

    @Override
    public void onActivityCreated( Bundle bundle ) {
        super.onActivityCreated( bundle );

        if( Lifeograph.getAddFreeNotPurchased() ) {
            AdView adView = getView().findViewById( R.id.adView );

            Bundle extras = new Bundle();
            extras.putString( "max_ad_content_rating", "G" );

            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle( AdMobAdapter.class, extras )
                    .tagForChildDirectedTreatment( true )
                    //TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE but how???
                    .build();

            adView.loadAd( adRequest );
        }
    }
}
