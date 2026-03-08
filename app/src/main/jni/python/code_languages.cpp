/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

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


#include "code_languages.hpp"


namespace LoG
{

Glib::RefPtr< Glib::Regex >
get_code_lang_regex( int lang_feature )
{
    static std::unordered_map< int, Glib::RefPtr< Glib::Regex > > regex_storage;

    auto kv = regex_storage.find( lang_feature );
    if( kv != regex_storage.end() )
        return kv->second;
    else
    {
        auto kv { CODE_LANGUAGES.find( lang_feature ) };
        if( kv == CODE_LANGUAGES.end() )
            return {};
        else
        {
            auto new_regex { Glib::Regex::create( kv->second.expr, kv->second.flags ) };
            regex_storage.emplace( lang_feature, new_regex );
            return new_regex;
        }
    };
}

} // namespace LoG

