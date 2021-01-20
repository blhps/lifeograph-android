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


import android.util.Log;

class NameAndValue
{
    final static int HAS_NAME = 0x1;
    final static int HAS_VALUE = 0x2;
    final static int HAS_UNIT = 0x4;
    final static int HAS_EQUAL = 0x10;

    NameAndValue() {
    }

    NameAndValue( String n, Double v ) {
        name = n;
        value = v;
    }

    String name = "";
    Double value = 0.0;
    String unit = "";
    int status = 0;

    static NameAndValue parse( String text ) {
        NameAndValue nav = new NameAndValue();
        char lf = '='; // =, \, #, $(unit)
        int divider = 0;
        int trim_length = 0;
        int trim_length_unit = 0;
        boolean negative = false;
        char c;

        for( int i = 0; i < text.length(); i++ ) {
            c = text.charAt( i );
            switch( c ) {
                case '\\':
                    if( lf == '#' || lf == '$' ) {
                        nav.unit += c;
                        trim_length_unit = 0;
                        lf = '$';
                    }
                    else if( lf == '\\' ) {
                        nav.name += c;
                        trim_length = 0;
                        lf = '=';
                    }
                    else // i.e. ( lf == '=' )
                        lf = '\\';
                    break;
                case '=':
                    if( nav.name.isEmpty() || lf == '\\' ) {
                        nav.name += c;
                        trim_length = 0;
                        lf = '=';
                    }
                    else if( lf == '#' || lf == '$' ) {
                        nav.unit += c;
                        trim_length_unit = 0;
                        lf = '$';
                    }
                    else // i.e. ( lf == '=' )
                    {
                        nav.status |= NameAndValue.HAS_EQUAL;
                        lf = '#';
                    }
                    break;
                case ' ':
                case '\t':
                    // if( lf == '#' ) just ignore
                    if( lf == '=' || lf == '\\' ) {
                        if( !nav.name.isEmpty() ) { // else ignore
                            nav.name += c;
                            trim_length++;
                        }
                    }
                    else if( lf == '$' ) {
                        nav.unit += c;
                        trim_length_unit++;
                    }
                    break;
                case ',':
                case '.':
                    if( divider != 0 || lf == '$' ) { // note that if divider, lf must be #
                        nav.unit += c;
                        trim_length_unit = 0;
                        lf = '$';
                    }
                    else if( lf == '#' )
                        divider = 1;
                    else {
                        nav.name += c;
                        trim_length = 0;
                        lf = '=';
                    }
                    break;
                case '-':
                    if( negative || lf == '$' ) { // note that if negative, lf must be #
                        nav.unit += c;
                        trim_length_unit = 0;
                        lf = '$';
                    }
                    else if( lf == '#' )
                        negative = true;
                    else {
                        nav.name += c;
                        trim_length = 0;
                        lf = '=';
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if( lf == '#' ) {
                        nav.status |= NameAndValue.HAS_VALUE;
                        nav.value *= 10;
                        nav.value += ( c - '0' );
                        if( divider != 0 )
                            divider *= 10;
                    }
                    else if( lf == '$' ) {
                        nav.unit += c;
                        trim_length_unit = 0;
                    }
                    else {
                        nav.name += c;
                        trim_length = 0;
                        lf = '='; // reset ( lf == \ ) case
                    }
                    break;
                default:
                    if( lf == '#' || lf == '$' ) {
                        nav.unit += c;
                        trim_length_unit = 0;
                        lf = '$';
                    }
                    else {
                        nav.name += c;
                        trim_length = 0;
                        lf = '=';
                    }
                    break;
            }
        }

        if( lf == '$' )
            nav.status |= ( HAS_NAME | HAS_EQUAL | HAS_UNIT );
        else if( lf == '#' )
            nav.status |= ( HAS_NAME | HAS_EQUAL );
        else if( !nav.name.isEmpty() )
            nav.status = HAS_NAME;

        if( trim_length != 0 )
            nav.name = nav.name.substring( 0, nav.name.length() - trim_length );
        if( trim_length_unit != 0 )
            nav.unit = nav.unit.substring( 0, nav.unit.length() - trim_length_unit );

        if( lf == '=' && !nav.name.isEmpty() ) // implicit boolean tag
            nav.value = 1.0;
        else {
            if( divider > 1 )
                nav.value /= divider;
            if( negative )
                nav.value *= -1;
        }

        Log.d( Lifeograph.TAG, "tag parsed | name: " + nav.name + "; value: " + nav.value + "; " +
                "unit: " + nav.unit );

        return nav;
    }
}
