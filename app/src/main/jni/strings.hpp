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


#ifndef LIFEOGRAPH_STRINGS_HEADER
#define LIFEOGRAPH_STRINGS_HEADER


#include "helpers.hpp"


namespace LoG
{

static constexpr char STRSEP[] = " | ";

// using KeyType = int;

// enum SI
// {
//     _VOID_,
//     ADD,
//     ALL,
//     ALL_ENTRIES,
//     ALL_PARAGRAPHS,
//     AUTO,
//     AVERAGE,
//     BACKGROUND_IMAGE,
//     CANCELED,
//     CATEGORY,
//     CHANGED,
//     CHART_INTERVALS,
//     CHART_UNDERLAY,
//     COLOR,
//     COMMENT_STYLE,
//     COMPLETION_TAG,
//     CREATION_DATE,
//     CUMULATIVE,
//     CURRENT_ENTRY,
//     CURRENT_ENTRY_TREE,
//     DEFINITION,
//     DELETED,
//     DONE,
//     EDIT_DATE,
//     FAVORITE,
//     FILTER_LIST,
//     FILTER_SEARCH,
//     FORMAT,
//     ID,
//     SI_IGNORE,
//     INTACT,
//     LANGUAGE,
//     LAST_ENTRY,
//     MAP_PATH,
//     MONTHLY,
//     NA,
//     NAME,
//     NEW,
//     NEW_COLUMN,
//     NEW_TABLE,
//     NO,
//     NONE,
//     NONTRASHED,
//     OPTIONS,
//     OTHER,
//     OVERWRITE,
//     PROGRESSED,
//     RIGHT_CLICK_PARA_EDIT,
//     STARTUP_ENTRY,
//     TITLE_STYLE,
//     TYPE_NAME,
//     THEME,
//     THEME_BASE,
//     THEME_FONT,
//     THEME_HEADING,
//     THEME_HIGHLIGHT,
//     THEME_SUBHEADING,
//     THEME_TEXT,
//     TODO,
//     TRASHED,
//     VALUE_TYPE,
//     YEARLY,
//     YES,
//
//     TAG,
//     PARAGRAPH,
//     // MONTHS
//     JANUARY,
//     FEBRUARY,
//     MARCH,
//     APRIL,
//     MAY,
//     JUNE,
//     JULY,
//     AUGUST,
//     SEPTEMBER,
//     OCTOBER,
//     NOVEMBER,
//     DECEMBER,
//     // DAYS
//     SUNDAY,
//     MONDAY,
//     TUESDAY,
//     WEDNESDAY,
//     THURSDAY,
//     FRIIDAY,
//     SATURDAY,
//
//     PROP_QUOT_TYPE,
//     PROP_COLOR,
//     PROP_IMG_SIZE,
//     PROP_LANGUAGE,
//     PROP_LOCATION,
//     PROP_LOCKED,
//     PROP_PATH,
//     PROP_READY,
//     PROP_REGISTER_SCRIPTS,
//     PROP_RELATED_ENTRY,
//     PROP_RELATED_PARA,
//     PROP_TAG_END_POS,
//     PROP_TAG_END_POS_CHANGED,
//     PROP_UNIT,
//     PROP_URI,
//     PROP_POPUP_NOTE,
//     PROP_POS_X,
//     PROP_POS_Y,
//     PROP_WIDTH,
//     PROP_HEIGHT,
//
//     EOENUM // fixed last item
// };


// typedef std::pair< const KeyType, const HELPERS::Ustring > StringKeyValuePair;
// typedef std::vector< StringKeyValuePair > SKVVec;
// typedef std::map< SI, const HELPERS::Ustring > StrMap;

// using StrMap = std::array< std::string_view, static_cast< size_t >( SI::EOENUM ) >;

// static constexpr StrMap STR0 =
// {{
//     "",
//     N_( "ADD" ),
//     N_( "All" ),
//     N_( "All Entries" ),
//     N_( "All Paragraphs" ),
//     N_( "Auto" ),
//     N_( "Average" ),
//     N_( "Background Image" ),
//     N_( "Canceled" ),
//     N_( "Category" ),
//     N_( "CHANGED" ),
//     N_( "Chart Intervals" ),
//     N_( "Chart Underlay" ),
//     N_( "Color" ),
//     N_( "Comment Style" ),
//     N_( "Completion Tag" ),
//     N_( "Creation Date" ),
//     N_( "Cumulative" ),
//     N_( "Current Entry" ),
//     N_( "Current Entry + Descendants" ),
//     N_( "Definition" ),
//     N_( "DELETED" ),
//     N_( "Done" ),
//     N_( "Edit Date" ),
//     N_( "Favorite" ),
//     N_( "List Filter" ),
//     N_( "Search Filter" ),
//     N_( "Format" ),
//     N_( "ID" ),
//     N_( "IGNORE" ),
//     N_( "INTACT" ),
//     N_( "Language" ),
//     N_( "Last Entry" ),
//     N_( "Map Path" ),
//     N_( "Monthly" ),
//     N_( "N/A" ),
//     N_( "Name" ),
//     N_( "NEW" ),
//     N_( "New Column" ),
//     N_( "New Table" ),
//     N_( "No" ),
//     N_( "None" ),
//     N_( "Non-Trashed" ),
//     N_( "Options" ),
//     N_( "OTHER" ),
//     N_( "OVERWRITE" ),
//     N_( "Progressed" ),
//     N_( "Right click to edit paragraph" ),
//     N_( "Startup Entry" ),
//     N_( "Title Style" ),
//     N_( "Type Name" ),
//     N_( "Theme" ),
//     N_( "Theme Base" ),
//     N_( "Theme Font" ),
//     N_( "Theme Heading" ),
//     N_( "Theme Highlight" ),
//     N_( "Theme Subheading" ),
//     N_( "Theme Text" ),
//     N_( "Todo" ),
//     N_( "Trashed" ),
//     N_( "Value Type" ),
//     N_( "Yearly" ),
//     N_( "Yes" ),
//
//     N_( "Tag" ),
//     N_( "Paragraph" ),
//
//     N_( "January" ),
//     N_( "February" ),
//     N_( "March" ),
//     N_( "April" ),
//     N_( "May" ),
//     N_( "June" ),
//     N_( "July" ),
//     N_( "August" ),
//     N_( "September" ),
//     N_( "October" ),
//     N_( "November" ),
//     N_( "December" ),
//
//     N_( "Sunday" ),
//     N_( "Monday" ),
//     N_( "Tuesday" ),
//     N_( "Wednesday" ),
//     N_( "Thursday" ),
//     N_( "Friday" ),
//     N_( "Saturday" ),
//
//     N_( "Quotation-Type" ),
//     N_( "Color" ),
//     N_( "Image-Size" ),
//     N_( "Language" ),
//     N_( "Location" ),
//     N_( "Locked" ),
//     N_( "Path" ),
//     N_( "Ready" ),
//     N_( "Register Scripts" ),
//     N_( "Related Entry" ),
//     N_( "Related Paragraph" ),
//     N_( "Tag End Pos" ),
//     N_( "Tag End Pos Chgd" ),
//     N_( "Unit" ),
//     N_( "URI" ),
//     "Popup Note",
//     "X Position",
//     "Y Position",
//     "Width",
//     "Height",
// }};

// TODO: 3.2: manage custom strings:
// static constexpr std::string_view get_custom_string( int id ) noexcept {
//     return STR0[ id ];
// }

// static const StrMap STR0 =
// {
//     { SI::_VOID_,               "" },
//     { SI::ADD,                  N_( "ADD" ) },
//     { SI::ALL_ENTRIES,          N_( "All Entries" ) },
//     { SI::ALL_PARAGRAPHS,       N_( "All Paragraphs" ) },
//     { SI::AUTO,                 N_( "Auto" ) },
//     { SI::AVERAGE,              N_( "Average" ) },
//     { SI::BACKGROUND_IMAGE,     N_( "Background Image" ) },
//     { SI::CANCELED,             N_( "Canceled" ) },
//     { SI::CATEGORY,             N_( "Category" ) },
//     { SI::CHANGED,              N_( "CHANGED" ) },
//     { SI::CHART_INTERVALS,      N_( "Chart Intervals" ) },
//     { SI::CHART_UNDERLAY,       N_( "Chart Underlay" ) },
//     { SI::COLOR,                N_( "Color" ) },
//     { SI::COMMENT_STYLE,        N_( "Comment Style" ) },
//     { SI::COMPLETION_TAG,       N_( "Completion Tag" ) },
//     { SI::CREATION_DATE,        N_( "Creation Date" ) },
//     { SI::CUMULATIVE,           N_( "Cumulative" ) },
//     { SI::CURRENT_ENTRY,        N_( "Current Entry" ) },
//     { SI::CURRENT_ENTRY_TREE,   N_( "Current Entry + Descendants" ) },
//     { SI::DEFINITION,           N_( "Definition" ) },
//     { SI::DELETED,              N_( "DELETED" ) },
//     { SI::DONE,                 N_( "Done" ) },
//     { SI::EDIT_DATE,            N_( "Edit Date" ) },
//     { SI::FAVORITE,             N_( "Favorite" ) },
//     { SI::FILTER_LIST,          N_( "List Filter" ) },
//     { SI::FILTER_SEARCH,        N_( "Search Filter" ) },
//     { SI::FORMAT,               N_( "Format" ) },
//     { SI::ID,                   N_( "ID" ) },
//     { SI::SI_IGNORE,            N_( "IGNORE" ) },
//     { SI::INTACT,               N_( "INTACT" ) },
//     { SI::LANGUAGE,             N_( "Language" ) },
//     { SI::LAST_ENTRY,           N_( "Last Entry" ) },
//     { SI::LOCATION,             N_( "Location" ) },
//     { SI::MAP_PATH,             N_( "Map Path" ) },
//     { SI::MONTHLY,              N_( "Monthly" ) },
//     { SI::NA,                   N_( "N/A" ) },
//     { SI::NAME,                 N_( "Name" ) },
//     { SI::NEW,                  N_( "NEW" ) },
//     { SI::NEW_COLUMN,           N_( "New Column" ) },
//     { SI::NEW_TABLE,            N_( "New Table" ) },
//     { SI::NO,                   N_( "No" ) },
//     { SI::NONE,                 N_( "None" ) },
//     { SI::OPTIONS,              N_( "Options" ) },
//     { SI::OTHER,                N_( "OTHER" ) },
//     { SI::OVERWRITE,            N_( "OVERWRITE" ) },
//     { SI::PROGRESSED,           N_( "Progressed" ) },
//     { SI::STARTUP_ENTRY,        N_( "Startup Entry" ) },
//     { SI::TITLE_STYLE,          N_( "Title Style" ) },
//     { SI::TYPE_NAME,            N_( "Type Name" ) },
//     { SI::THEME,                N_( "Theme" ) },
//     { SI::THEME_BASE,           N_( "Theme Base" ) },
//     { SI::THEME_FONT,           N_( "Theme Font" ) },
//     { SI::THEME_HEADING,        N_( "Theme Heading" ) },
//     { SI::THEME_HIGHLIGHT,      N_( "Theme Highlight" ) },
//     { SI::THEME_SUBHEADING,     N_( "Theme Subheading" ) },
//     { SI::THEME_TEXT,           N_( "Theme Text" ) },
//     { SI::TODO,                 N_( "Todo" ) },
//     { SI::TRASHED,              N_( "Trashed" ) },
//     { SI::UNIT,                 N_( "Unit" ) },
//     { SI::VALUE_TYPE,           N_( "Value Type" ) },
//     { SI::YEARLY,               N_( "Yearly" ) },
//     { SI::YES,                  N_( "Yes" ) },
//
//     { SI::TAG,                  N_( "Tag" ) },
//     { SI::PARAGRAPH,            N_( "Paragraph" ) }
// };
//
// static const StrMap STR_DATE =
// {
//     { SI::JANUARY,      N_( "January" ) },
//     { SI::FEBRUARY,     N_( "February" ) },
//     { SI::MARCH,        N_( "March" ) },
//     { SI::APRIL,        N_( "April" ) },
//     { SI::MAY,          N_( "May" ) },
//     { SI::JUNE,         N_( "June" ) },
//     { SI::JULY,         N_( "July" ) },
//     { SI::AUGUST,       N_( "August" ) },
//     { SI::SEPTEMBER,    N_( "September" ) },
//     { SI::OCTOBER,      N_( "October" ) },
//     { SI::NOVEMBER,     N_( "November" ) },
//     { SI::DECEMBER,     N_( "December" ) }
// };
//
// static const StrMap STR_DAYS =
// {
//     { SI::SUNDAY,      N_( "Sunday" ) },
//     { SI::MONDAY,      N_( "Monday" ) },
//     { SI::TUESDAY,     N_( "Tuesday" ) },
//     { SI::WEDNESDAY,   N_( "Wednesday" ) },
//     { SI::THURSDAY,    N_( "Thursday" ) },
//     { SI::FRIIDAY,     N_( "Friday" ) },
//     { SI::SATURDAY,    N_( "Saturday" ) }
// };

// TODO: 3.2: find a better operator
// inline std::string_view
// operator-( StrMap map, SI id )
// { return gettext( map.at( i ).c_str() ); }
// { return STR0[ static_cast< size_t >( id ) ]; }

// inline std::string_view
// operator-( StrMap map, KeyType id )
// { return gettext( map.at( i ).c_str() ); }
// { return STR0[ id ]; }

// inline HELPERS::Ustring
// operator/( StrMap map, SI id )
// { return gettext( map.at( i ).c_str() ); }
// { return std::string( STR0[ KeyType( id ) ] ); }

// inline HELPERS::Ustring
// operator/( StrMap map, KeyType id )
// { return gettext( map.at( i ).c_str() ); }
// { return std::string( STR0[ id ] ); }

namespace STRING
{

static const char SLOGAN[] =
        N_( "Personal, digital diary" );

static const char CANNOT_WRITE[] =
        N_( "Changes could not be written to diary!" );

static const char CANNOT_WRITE_SUB[] =
        N_( "Check if you have write permissions on "
            "the file and containing folder" );


static const char EMPTY_ENTRY_TITLE[] =
// TRANSLATORS: title of an empty diary entry
        N_( "<empty entry>" );

static const char ENTER_PASSWORD[] =
        N_( "Please enter password for selected diary..." );

static const char ENTER_PASSWORD_TIMEOUT[] =
        N_( "Program logged out to protect your privacy. "
            "Please re-enter password..." );

static const char INCOMPATIBLE_DIARY_OLD[] =
        N_( "Selected diary is in an older format which is "
            "not compatible with this version of Lifeograph. "
            "Please select another file..." );

static const char INCOMPATIBLE_DIARY_NEW[] =
        N_( "Selected diary is in a newer format which is "
            "not compatible with this version of Lifeograph. "
            "Please consider upgrading your program." );

static const char CORRUPT_DIARY[] =
        N_( "Selected file is not a valid "
            "Lifeograph diary. "
            "Please select another file..." );

static const char DIARY_NOT_FOUND[] =
        N_( "Selected file is not found. "
            "Please select another file..." );

static const char DIARY_NOT_READABLE[] =
        N_( "Selected file is not readable. "
            "Please check file permissions or select another file..." );

static const char DIARY_NOT_WRITABLE[] =
        N_( "You do not have permission to change this file." );

static const char DIARY_LOCKED[] =
        N_( "Selected file is locked which means either it is "
            "being edited by another instance of Lifeograph or "
            "the last session with it did not finish correctly. "
            "This file cannot be opened as long as the lock file (%1) "
            "is there." );

static const char UPGRADE_DIARY_CONFIRM[] =
        N_( "You are about to open an old diary that will be upgraded to the new format.\n\n"
            "Lifeograph will create a backup copy of the current diary for you "
            "in the same directory. Please keep that copy until you are sure that "
            "everything is all right." );

static const char FAILED_TO_OPEN_DIARY[] =
        N_( "Failed to open diary. "
            "Please select another file..." );

static const char DIARY_IS_NOT_ENCRYPTED[] =
        N_( "Press Open to continue" );

static const char COLHEAD_BY_LAST_ACCESS[] =
        N_( "Last Read" );

static const char COLHEAD_BY_LAST_SAVE[] =
        N_( "Last Saved" );

static const char DROP_TAG_TO_FILTER[] =
        N_( "(Drop a tag here to filter)" );

static const char CHANGE_PASSWORD[] =
        N_( "Change Password..." );

static const char ELEM_WITH_ENTRIES[] =
//  TRANSLATORS: e.g. Tag with 5 entries
        N_( "%1 with %2 entrie(s)" );

static const char TAG_WITH_ENTRIES_AVERAGE[] =
        N_( "Tag with %1 entrie(s). Average value = %2" );

static const char TAG_WITH_ENTRIES_CUMULATIVE[] =
        N_( "Tag with %1 entrie(s). Total value = %2" );

static const char NEW_THEME_NAME[] =
        N_( "New theme" );

static const char NEW_CHAPTER_NAME[] =
        N_( "New chapter" );

static const char NEW_CATEGORY_NAME[] =
        N_( "New category" );

static const char DEFAULT[] =
        N_( "Default" );

static const char NONDISPOSABLE_DEFAULT[] =
        N_( "<Default>" );

static const char DIARY_REPORT[] =
        N_( "Diary Report" );

} // namespace STRING

} // namespace LIFO

#endif
