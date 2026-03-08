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

// !!!!!! DO NOT SYNC THIS FILE WITH DESKTOP VERSION !!!!!!

#ifndef LIFEOGRAPH_LIFEOGRAPH_HEADER
#define LIFEOGRAPH_LIFEOGRAPH_HEADER

#include "settings.hpp"
#include "android_shim.hpp"

namespace LoG {

struct Icons {
    R2Pixbuf    diary_32;
    R2Pixbuf    entry_16;
    R2Pixbuf    entry_32;
    R2Pixbuf    entry_parent_16;
    R2Pixbuf    entry_plus_16;
    R2Pixbuf    entry_parent_32;
    R2Pixbuf    milestone_16;
    R2Pixbuf    milestone_32;
    R2Pixbuf    tag_16;
    R2Pixbuf    tag_32;
    R2Pixbuf    favorite_16;
    R2Pixbuf    trash_16;
    R2Pixbuf    todo_open_16;
    R2Pixbuf    todo_open_32;
    R2Pixbuf    todo_progressed_16;
    R2Pixbuf    todo_progressed_32;
    R2Pixbuf    todo_done_16;
    R2Pixbuf    todo_done_32;
    R2Pixbuf    todo_canceled_16;
    R2Pixbuf    todo_canceled_32;
    R2Pixbuf    entry_script_16;
    R2Pixbuf    entry_script_32;
    R2Pixbuf    filter;
    R2Pixbuf    filter_none;
    R2Pixbuf    table;
    R2Pixbuf    chart;
    R2Pixbuf    map_point;
    // below ones are regular Gtk icons but added due to the difficulty of getting them as Pixbuf
    R2Pixbuf    go_prev;
    R2Pixbuf    go_next;
    R2Pixbuf    go_home;
    R2Pixbuf    comment;
    R2Pixbuf    lock;
};


class Lifeograph {
public:
    inline static Settings settings;
    inline static Glib::RefPtr<Icons> icons;

    static String get_color_insensitive() { return "#AAAAAA"; }
    static const Glib::RefPtr<Gdk::Pixbuf>& get_todo_icon(int status) { static Glib::RefPtr<Gdk::Pixbuf> p; return p; }
    static const Glib::RefPtr<Gdk::Pixbuf>& get_todo_icon32(int status) { static Glib::RefPtr<Gdk::Pixbuf> p; return p; }

    inline static EnchantBroker* s_enchant_broker{ nullptr };
};

    class PyBindings {
    public:
        template<typename R, typename... Args>
        static R run_script_name_return(const std::string&, const std::string&, Args...) { return R(); }
    };

    class ChartSurface {
    public:
        static Glib::RefPtr<Gdk::Pixbuf> create_pixbuf(void*, int, const Pango::FontDescription&) { return nullptr; }
    };

    class TableSurface {
    public:
        static Glib::RefPtr<Gdk::Pixbuf> create_pixbuf(void*, int, const Pango::FontDescription&, bool) { return nullptr; }
    };
}

#endif
